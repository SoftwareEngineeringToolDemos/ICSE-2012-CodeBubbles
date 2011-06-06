/********************************************************************************/
/*										*/
/*		BattFactory.java						*/
/*										*/
/*	Bubble Automated Testing Tool factory class for bubbles integration	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* SVN: $Id$ */


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;

import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.exec.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;
import java.awt.Point;
import java.util.*;
import java.io.*;


public class BattFactory implements BattConstants, BudaConstants.ButtonListener,
		MintConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties 	batt_props;
private boolean 		server_running;
private BattModeler		batt_model;

private static BattFactory	the_factory;



static {
   the_factory = new BattFactory();
   BudaRoot.addBubbleConfigurator("BATT",new BattConfigurator());
   BudaRoot.registerMenuButton(TEST_BUTTON,the_factory);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static BattFactory getFactory()		{ return the_factory; }



private BattFactory()
{
   batt_props = BoardProperties.getProperties("Batt");
   batt_model = new BattModeler();
   server_running = false;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   // work is done by the static initializer
}



public static void initialize(BudaRoot br)
{
}




/********************************************************************************/
/*										*/
/*	Menu button handleing							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;

   if (id.equals(TEST_BUTTON)) {
      bb = createStatusBubble();
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb,bc);
      bb.grabFocus();
    }
}



/********************************************************************************/
/*										*/
/*	BattBubble setup							*/
/*										*/
/********************************************************************************/

BudaBubble createStatusBubble()
{
   return new BattStatusBubble(batt_model);
}



/********************************************************************************/
/*										*/
/*	Batt agent setup							*/
/*										*/
/********************************************************************************/

void updateTests()
{
   BumpClient bc = BumpClient.getBump();
   MintControl mc = bc.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   mc.send("<BATT DO='SHOWALL' />",rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();
   if (e != null) batt_model.updateTestModel(e);
}



void runTests(RunType rt)
{
   BumpClient bc = BumpClient.getBump();
   MintControl mc = bc.getMintControl();
   mc.send("<BATT DO='RUNTESTS' TYPE='" + rt.toString() + "' />");
}


void stopTest()
{
   BumpClient bc = BumpClient.getBump();
   MintControl mc = bc.getMintControl();
   mc.send("<BATT DO='STOPTEST' />");
}


TestMode getTestMode()
{
   BumpClient bc = BumpClient.getBump();
   MintControl mc = bc.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   mc.send("<BATT DO='MODE' />",rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();
   if (IvyXml.isElement(e,"RESULT")) {
      String md = IvyXml.getText(e);
      if (md.equals("CONTINUOUS")) return TestMode.CONTINUOUS;
      else if (md.equals("DEMAND")) return TestMode.ON_DEMAND;
      else if (md.equals("ON_DEMAND")) return TestMode.ON_DEMAND;
    }
   return null;
}



void setTestMode(TestMode md)
{
   if (md == null) return;

   BumpClient bc = BumpClient.getBump();
   MintControl mc = bc.getMintControl();
   mc.send("<BATT DO='SETMODE' VALUE='" + md.toString() + "' />");
}



void startBattServer()
{
   BumpClient bc = BumpClient.getBump();
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bc.getMintControl();

   synchronized (this) {
      if (server_running) return;

      mc.register("<BATT TYPE='_VAR_0' />",batt_model);

      List<String> args = new ArrayList<String>();
      args.add("java");
      args.add("-cp");
      args.add(System.getProperty("java.class.path"));
      args.add("edu.brown.cs.bubbles.batt.BattMain");
      if (batt_props.getBoolean("Batt.server.continuous",false)) args.add("-C");
      else args.add("-S");
      args.add("-m");
      args.add(bc.getMintName());

      String s1 = bs.getLibraryPath("battjunit.jar");
      String s2 = bs.getLibraryPath("battagent.jar");
      if (s1 == null || s2 == null) {
	 BoardProperties sp = BoardProperties.getProperties("System");
	 String s3 = sp.getProperty("edu.brown.cs.bubbles.jar");
	 BoardLog.logX("BATT","Missing batt file " + s1 + " " + s2 + " " + s3);
	 server_running = true;
	 return;
       }
      args.add("-u");
      args.add(s1);
      args.add("-a");
      args.add(s2);

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BATT DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString();
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       IvyExec exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	       BoardLog.logD("BATT","Run " + exec.getCommand());
	    }
	    catch (IOException e) {
	       break;
	     }
	  }
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      if (!server_running) {
	 BoardLog.logE("BATT","Unable to start batt server");
	 server_running = true; 	// don't try again
       }
    }
}




}	// end of class BattFactory




/* end of BattFactory.java */


