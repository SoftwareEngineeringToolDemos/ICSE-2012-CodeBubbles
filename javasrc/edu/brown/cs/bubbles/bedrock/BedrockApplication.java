/********************************************************************************/
/*										*/
/*		BedrockApplication.java 					*/
/*										*/
/*	Main class for the Eclipse RCP interface				*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.application.*;

import java.io.*;
import java.util.*;


public class BedrockApplication implements IApplication, BedrockConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean exit_ok;
private Display base_display;
private int	exit_ctr;
private boolean is_setup;

private Location base_location;

private static boolean	use_display = false;
private static boolean	tiny_display = false;
private static boolean	hide_display = false;
private static boolean	exit_atend = true;
private static boolean	show_atend = false;

private static BedrockApplication      the_app;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BedrockApplication()
{
   exit_ok = false;
   exit_ctr = 0;
   base_display = null;
   is_setup = false;

   the_app = this;
}



/********************************************************************************/
/*										*/
/*	Static methods								*/
/*										*/
/********************************************************************************/

static void enterApplication()
{
   BedrockPlugin.logD("BEDROCK: ENTER ");
}


static void stopApplication()
{
   BedrockPlugin.logD("BEDROCK: STOP APP REQUEST " + the_app);

   if (the_app == null) return;

   synchronized (the_app) {
      the_app.exit_ok = true;
      ++the_app.exit_ctr;
      the_app.notifyAll();
    }

   BedrockPlugin.logD("BEDROCK STOP CHECK " + PlatformUI.isWorkbenchRunning() + " " + the_app.exit_ctr);

   if (PlatformUI.isWorkbenchRunning()) {
      int ctr = the_app.exit_ctr;
      the_app.checkActive();
      BedrockPlugin.logD("BEDROCK STOP NEXT " + ctr + " " + the_app.exit_ctr + " " + the_app.exit_ok);
      if (ctr == the_app.exit_ctr && the_app.exit_ok) {
	 the_app.base_display.syncExec(new Shutdown());
       }
    }
}



private static class Shutdown implements Runnable {

   @Override public void run() {
      BedrockPlugin.logD("BEDROCK SHUTDOWN " + show_atend + " " + exit_atend);
      if (show_atend) {
	 for (Shell sh1 : the_app.base_display.getShells()) {
	    BedrockPlugin.logD("SHELL1 " + sh1.isVisible() + " " + sh1.getText());
	    sh1.setVisible(true);
	  }
	 Shell sh = the_app.base_display.getActiveShell();
	 if (sh != null) sh.setVisible(true);
       }
      if (exit_atend) {
	 try {
	    IWorkbench wb = PlatformUI.getWorkbench();
	    wb.close();
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logD("Problem closing workbench: " + t);
	  }
       }
    }

}	// end of inner class Shutdown




static Display getDisplay()
{
   if (the_app == null) return null;

   the_app.waitForSetup();

   return the_app.base_display;
}


private synchronized void waitForSetup()
{
   while (!is_setup) {
      System.err.println("BEDROCK: wait for setup");
      try {
	 wait();
       }
      catch (InterruptedException e) { }
    }
}



private synchronized void noteSetup()
{
   if (is_setup) return;

   System.err.println("BEDROCK: note setup");
   is_setup = true;
   notifyAll();
}




/********************************************************************************/
/*										*/
/*	Start/stop methods							*/
/*										*/
/********************************************************************************/

@Override public Object start(IApplicationContext ctx) throws Exception
{
   exit_ok = false;
   ++exit_ctr;

   base_location = Platform.getInstanceLocation();
   if (base_location != null) {
      boolean fg = base_location.lock();
      if (!fg) {
	 System.err.println("BEDROCK: eclipse/bedrock already running");
	 base_location = null;
	 return IApplication.EXIT_OK;
       }
    }

   BedrockPlugin.logI("APP START");

   Map<?,?> argm = ctx.getArguments();
   String [] args = (String []) argm.get(IApplicationContext.APPLICATION_ARGS);
   if (args.length == 0) hide_display = true;
   for (String s : args) {
      BedrockPlugin.logI("APP ARG: " + s);
      if (s.startsWith("-bdisplay")) use_display = true;
      else if (s.startsWith("-btiny")) tiny_display = true;
      else if (s.startsWith("-bhide")) hide_display = true;
      else if (s.startsWith("-bnone")) {
	 use_display = tiny_display = hide_display = false;
       }
      else hide_display = true; 		// default
    }

   if ((use_display || tiny_display || hide_display) &&
	  !PlatformUI.isWorkbenchRunning()) {
      int sts = PlatformUI.RETURN_UNSTARTABLE;
      BedrockPlugin.logI("DISPLAY START");
      try {
	 if (base_display == null) base_display = PlatformUI.createDisplay();
	 BedrockPlugin.logI("DISPLAY = " + base_display);
	 EndChecker ec = new EndChecker();
	 ec.start();
	 if (org.eclipse.jface.preference.PreferenceConverter.FONTDATA_DEFAULT_DEFAULT != null);
	 is_setup = false;
	 sts = PlatformUI.createAndRunWorkbench(base_display,new WbAdvisor());
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Start status: " + t);
	 BedrockPlugin.logE("Bad start",t);
	 t.printStackTrace();
       }
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      if (ws != null) {
	 IWorkspaceRoot wr = ws.getRoot();
	 IPath wp = wr.getFullPath();
	 File wf = wp.toFile();
	 File cf = new File(wf,".clone");
	 try {
	    FileWriter fw = new FileWriter(cf);
	    fw.close();
	  }
	 catch (IOException e) { }
       }
      if (base_display == null) {
	 BedrockPlugin.logI("Alternative Start");
	 try {
	    IWorkbench wb = PlatformUI.getWorkbench();
	    base_display = wb.getDisplay();
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logE("Start1 status:",t);
	  }
	 if (base_display != null) {
	    try {
	       // Class<?> c1 = org.eclipse.jface.resource.ColorRegistry.class;
	       DebugUITools.getPreferenceStore();
	       JavaUI.getColorManager();
	     }
	    catch (Throwable t) {
	       BedrockPlugin.logE("Start1a status",t);
	     }
	  }
	 noteSetup();
       }

      BedrockPlugin.logD("SETUP STATUS " + sts);

      // this fails with RETURN_UNSTARTABLE
      if (sts == PlatformUI.RETURN_OK) return IApplication.EXIT_OK;
      else if (sts == PlatformUI.RETURN_RESTART || base_display == null)
	 return IApplication.EXIT_RELAUNCH;
      else {
	 exit_ok = false;
	 BedrockPlugin.logD("BEDROCK: START STATUS = " + sts);
	 if (the_app != null && the_app.base_display != null) {
	    for (Shell sh1 : the_app.base_display.getShells()) {
	       BedrockPlugin.logD("SHELL2 " + sh1.isVisible() + " " + sh1.getText());
	       sh1.setVisible(false);
	     }
	  }
	 Shell sh = base_display.getActiveShell();
	 if (sh != null) sh.setVisible(false);
	 exit_atend = true;
	 show_atend = false;
	 // forceLoads();
	 noteSetup();
       }
    }
   else {
      base_display = null;
    }

   for ( ; ; ) {
      int ctr = exit_ctr;
      BedrockPlugin.logD("WAIT ON " + this);
      synchronized (this) {		   // wait until exit request
	 while (!exit_ok) {
	    try {
	       wait(60000);
	     }
	    catch (InterruptedException e) { }
	    if (!exit_ok) {
	       BedrockPlugin bp = BedrockPlugin.getPlugin();
	       IvyXmlWriter xw = bp.beginMessage("PING");
	       String resp = bp.finishMessageWait(xw);
	       if (resp == null) exit_ok = true;
	     }
	    BedrockPlugin.logD("PRECHECK " + exit_ok + " " + exit_ctr + " " + ctr);
	  }
       }
      checkActive();
      BedrockPlugin.logD("EXIT CHECK " + exit_ok + " " + exit_ctr + " " + ctr);
      if (ctr == exit_ctr && exit_ok) break;
    }

   BedrockPlugin.logI("BEDROCK: EXITING");

   return IApplication.EXIT_OK;
}




@Override public void stop()
{
   synchronized (this) {
      exit_ok = true;
      ++exit_ctr;
      notifyAll();
    }
}



private void checkActive()
{
   BedrockPlugin bp = BedrockPlugin.getPlugin();
   IvyXmlWriter xw = bp.beginMessage("PING");
   String resp = bp.finishMessageWait(xw,5000);
   synchronized (this) {
      if (resp != null) {
	 exit_ok = false;
	 ++exit_ctr;
       }
    }
}



static String getOptions()
{
   String opts = "";
   if (use_display) opts += " Use_Display";
   if (tiny_display) opts += " Tiny_Display";
   if (hide_display) opts += " Hide_display";
   if (exit_atend) opts += " Exit_atend";
   if (show_atend) opts += " Show_atend";

   return opts;
}



private void forceLoads()
{
   try {
      // Class<?> c1 = org.eclipse.jface.resource.ColorRegistry.class;
      DebugUITools.getPreferenceStore();
      JavaUI.getColorManager();
      new org.eclipse.jface.resource.FontRegistry();
    }
   catch (Throwable t) { }
}



/********************************************************************************/
/*										*/
/*	Workbench advisor							*/
/*										*/
/********************************************************************************/

private class WbAdvisor extends WorkbenchAdvisor {

   @Override public String getInitialWindowPerspectiveId() {
      return null;
    }

   @Override public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer c) {
      return new WbWindowAdvisor(c);
    }

   @Override public boolean openWindows() {
      if (hide_display) return true;
      return super.openWindows();
    }

   @Override public void postStartup() {
      BedrockPlugin.logD("BEDROCK POST STARTUP " + use_display + " " + hide_display);

      // super.postStartup();

      forceLoads();
      noteSetup();

      if (use_display || hide_display) {
	 for (Shell sh1 : the_app.base_display.getShells()) {
	    BedrockPlugin.logD("BEDROCK: SHELL4 " + sh1.isVisible() + " " + sh1.getText());
	    sh1.setVisible(false);
	  }
	 Shell sh = base_display.getActiveShell();
	 if (sh != null) {
	    BedrockPlugin.logD("BEDROCK: SHELL5 " + sh.isVisible() + " " + sh.getText());
	    sh.setVisible(false);
	    // sh.close();
	  }
       }
      if (tiny_display) {
	 Shell sh = base_display.getActiveShell();
	 sh.setMinimumSize(1,1);
	 sh.setSize(1,1);
       }
    }

}	// end of inner class WbAdvisor



private class WbWindowAdvisor extends WorkbenchWindowAdvisor {

   WbWindowAdvisor(IWorkbenchWindowConfigurer cfg) {
      super(cfg);
    }

   @Override public void preWindowOpen() {
      // remove the window
      super.preWindowOpen();

      BedrockPlugin.logD("PRE WINDOW OPEN " + use_display + " " + hide_display);

      hideWindows();
    }

   @Override public void postWindowOpen() {
      // remove the window
      super.postWindowOpen();

      BedrockPlugin.logD("POST WINDOW OPEN " + use_display + " " + hide_display);

      hideWindows();
    }

   private void hideWindows() {
      if (use_display || hide_display) {
	 Set<Shell> done = new HashSet<Shell>();
	 for (Shell sh1 : base_display.getShells()) {
	    hideShell(sh1,done);
	  }
	 Shell sh = the_app.base_display.getActiveShell();
	 hideShell(sh,done);
	 IWorkbenchWindowConfigurer cfg = getWindowConfigurer();
	 IWorkbenchWindow win = cfg.getWindow();
	 sh = win.getShell();
	 hideShell(sh,done);
       }
    }

   private void hideShell(Shell sh,Set<Shell> done) {
      if (sh == null) return;
      if (done.contains(sh)) return;
      BedrockPlugin.logD("SHELLX " + sh.isVisible() + " " + sh + " " + sh.getText());
      sh.setVisible(false);
      sh.setEnabled(false);
      sh.setMinimized(true);
      sh.addShellListener(new WbShellRemover());
      done.add(sh);
      for (Composite c = sh.getParent(); c != null; c = c.getParent()) {
	 c.setVisible(false);
       }
    }

}	// end of inner class WbWindowAdvisor


private class WbShellRemover extends ShellAdapter {

   @Override public void shellDeiconified(ShellEvent e) {
      BedrockPlugin.logD("BEDROCK: DEICON SHELL");
      Shell sh = (Shell) e.getSource();
      sh.setVisible(false);
    }

   @Override public void shellActivated(ShellEvent e) {
      BedrockPlugin.logD("BEDROCK: ACTIVE SHELL");
      Shell sh = (Shell) e.getSource();
      sh.setVisible(false);
    }

}	// end of WbShellRemover


/********************************************************************************/
/*										*/
/*	Thread to check for termination 					*/
/*										*/
/********************************************************************************/

private class EndChecker extends Thread {

   EndChecker() {
      super("Bedrock_Exit_Checker");
      setDaemon(true);
    }

   @Override public void run() {
      int ctr = 0;
      for ( ; ; ) {
	 try {
	    sleep(60000);
	  }
	 catch (InterruptedException e) { }
	 if (PlatformUI.isWorkbenchRunning()) {
	    BedrockPlugin bp = BedrockPlugin.getPlugin();
	    IvyXmlWriter xw = bp.beginMessage("PING");
	    String resp = bp.finishMessageWait(xw,600000);
	    if (resp != null) ctr = 0;
	    else if (++ctr >= 2) {
	       System.err.println("BEDROCK: End checker stopping");
	       xw = bp.beginMessage("STOP");
	       bp.finishMessage(xw);
	       bp.forceExit();
	     }
	  }
       }
    }

}	// end of inner class EndChecker



}	// end of class BedrockApplication




/* end of BedrockApplication.java */
