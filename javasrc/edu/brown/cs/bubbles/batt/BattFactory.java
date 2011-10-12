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
import edu.brown.cs.bubbles.bale.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.exec.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;
import java.awt.Point;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.swing.*;


public class BattFactory implements BattConstants, BudaConstants.ButtonListener,
		MintConstants, BaleConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties 	batt_props;
private boolean 		server_running;
private BattModeler		batt_model;

private static BattFactory	the_factory = null;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BattFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BattFactory();
    }
   return the_factory;
}



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
   BudaRoot.addBubbleConfigurator("BATT",new BattConfigurator());
   BudaRoot.registerMenuButton(TEST_BUTTON,getFactory());
}



public static void initialize(BudaRoot br)
{
   BattFactory bf = getFactory();

   switch (BoardSetup.getSetup().getRunMode()) {
      case SERVER :
	 bf.startBattServer();
	 break;
    }

   BaleFactory.getFactory().addContextListener(new BattContexter(bf));
}




/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaBubble bb = null;

   if (id.equals(TEST_BUTTON)) {
      bb = createStatusBubble();
    }

   if (bba != null && bb != null) {
      bba.addBubble(bb,null,pt,0);
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
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   mc.send("<BATT DO='SHOWALL' />",rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();
   if (e != null) batt_model.updateTestModel(e);
}



void runTests(RunType rt)
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='RUNTESTS' TYPE='" + rt.toString() + "' />");
}


void stopTest()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='STOPTEST' />");
}


TestMode getTestMode()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
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

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='SETMODE' VALUE='" + md.toString() + "' />");
}



void startBattServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();

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
      args.add(bs.getMintName());

      String s0 = bs.getLibraryPath("junit.jar");
      String s1 = bs.getLibraryPath("battjunit.jar");
      String s2 = bs.getLibraryPath("battagent.jar");
      if (s0 == null || s1 == null || s2 == null) {
	 BoardProperties sp = BoardProperties.getProperties("System");
	 String s3 = sp.getProperty("edu.brown.cs.bubbles.jar");
	 BoardLog.logX("BATT","Missing batt file " + s0 + " " + s1 + " " + s2 + " " + s3);
	 server_running = true;
	 return;
       }
      args.add("-u");
      args.add(s1);
      args.add("-a");
      args.add(s2);
      args.add("-l");
      args.add(s0);

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BATT DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt;
	 if (i == 0) rslt = rply.waitForString(1000);
	 else rslt = rply.waitForString();
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




/********************************************************************************/
/*										*/
/*	Batt editor context listener						*/
/*										*/
/********************************************************************************/

private static class BattContexter implements BaleContextListener {

   BattModeler batt_model;

   BattContexter(BattFactory bf) {
      batt_model = bf.batt_model;
    }

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      String mthd = cfg.getMethodName();
      if (mthd == null) return;
      List<BattTestCase> direct = new ArrayList<BattTestCase>();
      List<BattTestCase> all = new ArrayList<BattTestCase>();
      for (BattTestCase btc : batt_model.getAllTests()) {
	 UseMode um = btc.usesMethod(mthd);
	 switch (um) {
	    case DIRECT :
	       direct.add(btc);
	       all.add(btc);
	       break;
	    case INDIRECT :
	       all.add(btc);
	       break;
	  }
	}

       if (all.size() > 0) {
	  if (direct.size() > 0) {
	     TestDisplayAction tda = new TestDisplayAction("Show Test Cases",direct,cfg.getEditor());
	     menu.add(tda);
	   }
	  if (all.size() != direct.size()) {
	     TestDisplayAction tda = new TestDisplayAction("Show Tests Using Method",all,cfg.getEditor());
	     menu.add(tda);
	   }
	}

       String mnm = mthd;
       int idx = mnm.indexOf("(");
       if (idx >= 0) mnm = mnm.substring(0,idx);
       idx = mnm.lastIndexOf(".");
       if (idx >= 0) mnm = mnm.substring(idx+1);

       menu.add(new NewTestAction(mthd,NewTestMode.USER_CODE,mnm,cfg.getEditor()));

       /**************
       JMenu newmenu = new JMenu("Create New Test ...");
       newmenu.add(new NewTestAction(mthd,NewTestMode.INPUT_OUTPUT,null,cfg.getEditor()));
       newmenu.add(new NewTestAction(mthd,NewTestMode.CALL_SEQUENCE,null,cfg.getEditor()));
       newmenu.add(new NewTestAction(mthd,NewTestMode.USER_CODE,null,cfg.getEditor()));
       menu.add(newmenu);
       *************/
    }

}	// end of inner class BattContexter




private static class TestDisplayAction extends AbstractAction {

   private List<BattTestCase> test_cases;
   private JComponent source_area;

   TestDisplayAction(String id,List<BattTestCase> cases,JComponent src) {
      super(id);
      test_cases = cases;
      source_area = src;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT",getValue(Action.NAME).toString());
      List<BumpLocation> locs = new ArrayList<BumpLocation>();
      BumpClient bc = BumpClient.getBump();
      for (BattTestCase btc : test_cases) {
	 String c = btc.getClassName();
	 String m = btc.getMethodName();
	 m = c + "." + m;
	 List<BumpLocation> fl = bc.findMethod(null,m,false);
	 if (fl == null) continue;
	 locs.addAll(fl);
       }
      if (locs.size() > 0) {
	 BaleFactory.getFactory().createBubbleStack(source_area,null,null,false,locs,BudaConstants.BudaLinkStyle.NONE);
       }
    }

}	// end of inner class TestDisplayAction




private static class NewTestAction extends AbstractAction {

   private JComponent source_area;
   private NewTestMode test_mode;
   private String method_name;

   NewTestAction(String mthd,NewTestMode mode,String nm,JComponent src) {
      super(getButtonName(mode,nm));
      source_area = src;
      test_mode = mode;
      method_name = mthd;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","NewTest_" + test_mode);
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> locs = bc.findMethod(null,method_name,false);
      if (locs == null || locs.size() == 0) return;
      BumpLocation loc = null;
      for (BumpLocation bl : locs) {
	 // check if bl is relvant to the context of the action
	 loc = bl;
	 break;
       }

      BattNewTestBubble ntb = new BattNewTestBubble(method_name,loc,test_mode);
      BudaBubble bb = ntb.createNewTestBubble();
      if (bb == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_area);
      if (bba == null) return;
      bba.addBubble(bb,source_area,null,PLACEMENT_RIGHT|PLACEMENT_MOVETO|PLACEMENT_NEW);
    }

   private static String getButtonName(NewTestMode mode,String nm) {
      String typ = null;
      switch (mode) {
	 case INPUT_OUTPUT :
	    typ = "Input-Output Test";
	    break;
	 case CALL_SEQUENCE :
	    typ = "Call Sequence Test";
	    break;
	 case USER_CODE :
	    typ = "Test Method";
	    break;
       }
      if (nm != null) {
	 typ = "Create New " + typ;
	 // typ += " for " + nm;
       }

      return typ;
    }

}	// end of inner class NewTestAction




}	// end of class BattFactory




/* end of BattFactory.java */


