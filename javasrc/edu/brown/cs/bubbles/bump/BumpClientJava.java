/********************************************************************************/
/*										*/
/*		BumpClientJava.java						*/
/*										*/
/*	BUblles Mint Partnership main class for using Eclipse/Java		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko	*/
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



package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.exec.IvyExec;

import javax.swing.JOptionPane;

import java.io.*;
import java.util.*;



/**
 *	This class provides an interface between Code Bubbles and the back-end
 *	IDE.  This particular implementation works for ECLIPSE.
 *
 *	At some point, this should be converted into an interface that can then
 *	be implemented by an appropriate class for each back end IDE.
 *
 **/

public class BumpClientJava extends BumpClient
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	eclipse_starting;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpClientJava()
{
   eclipse_starting = false;

   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*										*/
/*	Eclipse interaction methods						*/
/*										*/
/********************************************************************************/

/**
 *	Return the name of the back end.
 **/

@Override public String getName()		{ return "Eclipse"; }




/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

@Override void localStartIDE()
{
   synchronized (this) {
      if (eclipse_starting) return;
      eclipse_starting = true;
    }

   ensureRunning();
}




private void ensureRunning()
{
   if (tryPing()) return;
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no eclipse found");
      JOptionPane.showMessageDialog(null,
				       "Server must be running and accessible before client can be run",
				       "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

   String eclipsedir = board_properties.getProperty(BOARD_PROP_ECLIPSE_DIR);
   String ws = board_properties.getProperty(BOARD_PROP_ECLIPSE_WS);

   File ef = new File(eclipsedir);
   File ef1 = null;
   for (String s : BOARD_ECLIPSE_START) {
      ef1 = new File(ef,s);
      if (ef1.exists() && ef1.canExecute()) break;
    }
   if (ef1 == null || !ef1.exists() || !ef1.canExecute()) ef1 = new File(ef,"eclipse");
   String efp = ef1.getPath();
   if (efp.endsWith(".app") || efp.endsWith(".exe")) efp = efp.substring(0,efp.length()-4);

   String cmd = "'" + efp + "'";
   if (!board_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,false)) {
      cmd += " -application edu.brown.cs.bubbles.bedrock.application -nosplash";
    }
   else {
      cmd += " -nosplash";
    }
   if (ws != null) cmd += " -data '" + ws + "'";

   String eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_OPTIONS);
   if (eopt != null) cmd += " " + eopt;
   if (board_properties.getBoolean(BOARD_PROP_ECLIPSE_CLEAN)) {
      if (!cmd.contains("-clean")) cmd += " -clean";
      board_properties.remove(BOARD_PROP_ECLIPSE_CLEAN);
      try {
	 board_properties.save();
       }
      catch (IOException e) { }
    }

   cmd += " -vmargs '-Dedu.brown.cs.bubbles.MINT=" + mint_name + "'";
   eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
   if (eopt != null) cmd += " " + eopt;

   try {
      IvyExec ex = new IvyExec(cmd);
      boolean eok = false;
      for (int i = 0; i < 200; ++i) {
	 synchronized (this) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (tryPing()) {
	    BoardLog.logI("BUMP","Eclipse started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting eclipse");
	    JOptionPane.showMessageDialog(null,
					     "Eclipse could not be started. Check the eclipse log",
					     "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Eclipse doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running eclipse: " + e);
      System.exit(1);
    }
}





/********************************************************************************/
/*										*/
/*	Java Search queries							*/
/*										*/
/********************************************************************************/

/**
 *	Find the loction of a method given the project and name.  If the name is
 *	ambiguous, mutliple locations may be returned.	The name can contain a
 *	parenthesized argument list, i.e. package.method(int,java.lang.String).
 **/

public List<BumpLocation> findMethod(String proj,String name,boolean system)
{
   boolean cnstr = false;
   name = name.replace('$','.');

   String nm0 = name;
   int idx2 = name.indexOf("(");
   String args = "";
   if (idx2 >= 0) {
      nm0 = name.substring(0,idx2);
      args = name.substring(idx2);
      if (args.contains("<")) {
	 StringBuffer buf = new StringBuffer();
	 int lvl = 0;
	 for (int i = 0; i < args.length(); ++i) {
	    char c = args.charAt(i);
	    if (c == '<') ++lvl;
	    else if (c == '>') --lvl;
	    else if (lvl == 0) buf.append(c);
	  }
	 args = buf.toString();
	 name = nm0 + args;
       }
    }

   int idx0 = nm0.lastIndexOf(".");
   if (idx0 >= 0) {
      String mthd = nm0.substring(idx0+1);
      int idx1 = nm0.lastIndexOf(".",idx0-1);
      String clsn = nm0.substring(idx1+1,idx0);
      if (mthd.equals(clsn) || mthd.equals("<init>")) {
	 cnstr = true;
	 nm0 = nm0.substring(0,idx0);
	 if (idx2 > 0) nm0 += args;
	 name = nm0;
       }
    }

   if (name == null) {
      BoardLog.logI("BUMP","Empty name provided to java search");
      return null;
    }

   List<BumpLocation> locs = findMethods(proj,name,false,true,cnstr,system);

   if (locs == null || locs.isEmpty()) {
      int x1 = name.indexOf('(');
      if (cnstr && x1 >= 0) {				// check for nested constructor with extra argument
	 String mthd = name.substring(0,x1);
	 int x2 = mthd.lastIndexOf('.');
	 if (x2 >= 0) {
	    String pfx = name.substring(0,x2);
	    if (args.startsWith("(" + pfx + ",") || args.startsWith("(" + pfx + ")")) {
	       int ln = pfx.length();
	       args = "(" + args.substring(ln+2);
	       name = mthd + args;
	       locs = findMethods(proj,name,false,true,cnstr,system);
	    }
	 }
      }
   }

   return locs;
}



@Override protected String localFixupName(String nm)
{
   nm = nm.replace('$','.');
   return nm;
}



}	// end of class BumpClientJava




/* end of BumpClientJava.java */
