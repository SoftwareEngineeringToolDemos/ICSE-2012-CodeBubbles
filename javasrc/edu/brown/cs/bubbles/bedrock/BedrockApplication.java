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

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.*;

import java.util.Map;



public class BedrockApplication implements IApplication, BedrockConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean exit_ok;
private Display base_display;
private int	exit_ctr;

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

   return the_app.base_display;
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

   System.err.println("BEDROCK: APP START");

   Map<?,?> argm = ctx.getArguments();
   String [] args = (String []) argm.get(IApplicationContext.APPLICATION_ARGS);
   if (args.length == 0) hide_display = true;
   for (String s : args) {
      System.err.println("BEDROCK: APP ARG: " + s);
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
      try {
	 if (base_display == null) base_display = PlatformUI.createDisplay();
	 System.err.println("BEDROCK: DISPLAY = " + base_display);
	 EndChecker ec = new EndChecker();
	 ec.start();
	 sts = PlatformUI.createAndRunWorkbench(base_display,new WbAdvisor());
       }
      catch (Throwable t) { }
      if (base_display == null) {
	 try {
	    IWorkbench wb = PlatformUI.getWorkbench();
	    base_display = wb.getDisplay();
	  }
	 catch (Throwable t) { }
       }

      // this fails with RETURN_UNSTARTABLE
      if (sts == PlatformUI.RETURN_OK) return IApplication.EXIT_OK;
      else if (sts == PlatformUI.RETURN_RESTART) return IApplication.EXIT_RELAUNCH;
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
       }
    }
   else {
      base_display = null;
    }

   for ( ; ; ) {
      int ctr = exit_ctr;
      BedrockPlugin.logD("BEDROCK: WAIT ON " + this);
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
	    BedrockPlugin.logD("BEDROCK: PRECHECK " + exit_ok + " " + exit_ctr + " " + ctr);
	  }
       }
      checkActive();
      BedrockPlugin.logD("BEDROCK: EXIT CHECK " + exit_ok + " " + exit_ctr + " " + ctr);
      if (ctr == exit_ctr && exit_ok) break;
    }

   System.err.println("BEDROCK: EXITING");

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
      System.err.println("BEDROCK POST STARTUP");
      BedrockPlugin.logD("BEDROCK POST STARTUP");

      if (use_display || hide_display) {
	 for (Shell sh1 : the_app.base_display.getShells()) {
	    BedrockPlugin.logD("SHELL4 " + sh1.isVisible() + " " + sh1.getText());
	    sh1.setVisible(false);
	  }
	 Shell sh = base_display.getActiveShell();
	 if (sh != null) sh.setVisible(false);
	 // sh.close();
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

   @Override public void postWindowOpen() {
      // remove the window
      super.postWindowOpen();
      BedrockPlugin.logD("POST WINDOW OPEN");

      if (use_display || hide_display) {
	 for (Shell sh1 : base_display.getShells()) {
	    BedrockPlugin.logD("SHELL3 " + sh1.isVisible() + " " + sh1.getText());
	    sh1.setVisible(false);
	  }
	 Shell sh = the_app.base_display.getActiveShell();
	 if (sh != null) sh.setVisible(false);
       }
    }

}	// end of inner class WbWindowAdvisor



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
            String resp = bp.finishMessageWait(xw);
            if (resp != null) ctr = 0;
            else if (++ctr >= 2) {
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
