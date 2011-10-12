/********************************************************************************/
/*										*/
/*		BvcrFactory.java						*/
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


package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.exec.*;

import org.w3c.dom.*;
import java.util.*;
import java.io.*;


public class BvcrFactory implements BvcrConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 		server_running;

private static BvcrFactory	the_factory = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BvcrFactory getFactory()
{
   if (the_factory == null) the_factory = new BvcrFactory();
   return the_factory;
}


private BvcrFactory()
{
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
   getFactory().startBvcrServer();
}




/********************************************************************************/
/*										*/
/*	Bvcr agent setup							*/
/*										*/
/********************************************************************************/

Element getChangesForFile(String proj,String file)
{
   if (!server_running) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='FINDCHANGES'";
   cmd += " PROJECT='" + proj + "'";
   cmd += " FILE='" + file + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();

   // This should use e to get the set of lines changed in the original version
   // and then map these to lines changed in the users version
   // It should return a change structure that encompasses these changes

   return e;
}



Element getHistoryForFile(String proj,String file)
{
   if (!server_running) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='HISTORY'";
   cmd += " PROJECT='" + proj + "'";
   cmd += " FILE='" + file + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();

   return e;
}




/********************************************************************************/
/*										*/
/*	Server code								*/
/*										*/
/********************************************************************************/

void startBvcrServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;

   synchronized (this) {
      if (server_running) return;

      List<String> args = new ArrayList<String>();
      args.add("java");
      args.add("-cp");
      args.add(System.getProperty("java.class.path"));
      args.add("edu.brown.cs.bubbles.bvcr.BvcrMain");
      args.add("-S");
      args.add("-m");
      args.add(bs.getMintName());

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BVCR DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString(1000);
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	       BoardLog.logD("BVCR","Run " + exec.getCommand());
	    }
	    catch (IOException e) {
	       break;
	     }
	  }
	 else {
	    try {
	       if (exec != null) {
		  // check if process exited (nothing to do)
		  exec.exitValue();
		  break;
		}
	     }
	    catch (IllegalThreadStateException e) { }
	  }

	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      if (!server_running) {
	 BoardLog.logE("BVCR","Unable to start bvcr server");
	 server_running = true; 	// don't try again
       }
    }
}




}	// end of class BvcrFactory




/* end of BvcrFactory.java */



