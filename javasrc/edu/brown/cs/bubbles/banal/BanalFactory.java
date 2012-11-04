/********************************************************************************/
/*										*/
/*		BanalFactory.java						*/
/*										*/
/*	Bubbles ANALysis package factory for use within bubbles 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.exec.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;
import java.io.IOException;


public class BanalFactory implements BanalConstants, MintConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 		server_running;

private static BanalFactory	the_factory;


static {
   the_factory = new BanalFactory();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BanalFactory()
{
   server_running = false;
}


public static BanalFactory getFactory() 	{ return the_factory; }




/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

public static void setup()
{ }



public static void initialize(BudaRoot br)
{
   the_factory.startBanalServer();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Collection<BanalPackageNode> computePackageGraph(String proj,String pkg,boolean usemethods)
{
   BanalPackageGraph pg = null;

   if (server_running) {
      BoardSetup bs = BoardSetup.getSetup();
      MintControl mc = bs.getMintControl();
      MintDefaultReply rply = new MintDefaultReply();
      String cmd = "<BANAL DO='PACKAGEGRAPH'";
      if (proj != null) cmd += " PROJECT='" + proj + "'";
      if (pkg != null) cmd += " PACKAGE='" + pkg + "'";
      cmd += " METHODS='" + usemethods + "' />";
      mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
      Element e = rply.waitForXml();
      if (IvyXml.isElement(e,"RESULT")) {
	 BoardLog.logD("BANAL","Graph reply: " + IvyXml.convertXmlToString(e));
	 pg = new BanalPackageGraph(IvyXml.getChild(e,"GRAPH"));
       }
    }

   if (pg == null) return new ArrayList<BanalPackageNode>();

   return pg.getAllNodes();
}




/********************************************************************************/
/*										*/
/*	Server methods								*/
/*										*/
/********************************************************************************/

void startBanalServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;

   synchronized (this) {
      if (server_running) return;

      long mxmem = Runtime.getRuntime().maxMemory();
      mxmem = Math.min(512*1024*1024L,mxmem);

      List<String> args = new ArrayList<String>();
      args.add("java");
      args.add("-Xmx" + Long.toString(mxmem));
      args.add("-cp");
      args.add(System.getProperty("java.class.path"));
      args.add("edu.brown.cs.bubbles.banal.BanalMain");
      args.add("-S");
      args.add("-m");
      args.add(bs.getMintName());

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BANAL DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString(1000);
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	       BoardLog.logD("BANAL","Run " + exec.getCommand());
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
	 BoardLog.logE("BANAL","Unable to start banal server");
	 server_running = true; 	// don't try again
       }
    }
}


}	// end of class BanalFactory




/* end of BanalFactory.java */
