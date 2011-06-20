/********************************************************************************/
/*										*/
/*		BemaMain.java							*/
/*										*/
/*	Bubbles Environment Main Application main program			*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bema;


import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bdoc.BdocFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import java.io.*;
import java.lang.reflect.Method;



/**
 *	Bubbles main program.
 **/

public class BemaMain implements BemaConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Starting point for the bubbles environment.
 **/

public static void main(String [] args)
{
   BemaMain bm = new BemaMain(args);

   if (System.getProperty("java.vm.vendor").startsWith("Apple")) {
      try {
	 UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
       }
      catch (Throwable t) {
	 System.err.println("BEMA: Problem setting l&f: " + t);
       }
    }
   
   bm.start();
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	restore_session;
private boolean 	save_session;
private boolean 	force_setup;
private boolean 	skip_setup;
private boolean 	skip_splash;
private boolean 	allow_debug;
private boolean 	use_lila;
private boolean 	use_web;
private String		use_workspace;
private Element 	load_config;
private String []	java_args;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BemaMain(String [] args)
{
   restore_session = true;
   save_session = true;
   force_setup = false;
   skip_setup = false;
   skip_splash = false;
   allow_debug = false;
   use_workspace = null;
   use_web = false;
   java_args = args;
   use_lila = false;

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   int ln = args.length;

   for (int i = 0; i < ln; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-nosetup")) {                  // -nosetup
	    skip_setup = true;
	  }
	 else if (args[i].startsWith("-nos")) {                 // -nosave
	    save_session = false;
	  }
	 else if (args[i].startsWith("-nor")) {                 // -norestore
	    restore_session = false;
	  }
	 else if (args[i].startsWith("-s")) {                   // -save
	    save_session = true;
	  }
	 else if (args[i].startsWith("-r")) {                   // -restore
	    restore_session = true;
	  }
	 else if (args[i].startsWith("-f")) {                   // -force
	    force_setup = true;
	  }
	 else if (args[i].startsWith("-w") && i+1 < ln) {       // -workspace <ws>
	    use_workspace = args[++i];
	  }
	 else if (args[i].startsWith("-nosp")) {                // -nosplash
	    skip_splash = true;
	  }
	 else if (args[i].startsWith("-p") && i+1 < ln) {       // -prop <propdir>
	    BoardProperties.setPropertyDirectory(args[++i]);
	  }
	 else if (args[i].startsWith("-Debug")) {               // -Debug
	    allow_debug = true;
	  }
	 else if (args[i].startsWith("-m") && i+1 < ln) {       // -msg <id>
	    System.setProperty("edu.brown.cs.bubbles.MINT",args[++i]);
	  }
	 else if (args[i].startsWith("-W")) {                   // -WEB
	    use_web = true;
	  }
	 else if (args[i].startsWith("-lila")) {
	    use_lila = true;
	  }
	 else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("BUBBLES: bubbles [-nosave] [-noresotre] [-force] [-workspace <workspace>]");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Methods to actually run bubbles 					*/
/*										*/
/********************************************************************************/

private void start()
{
   BoardProperties bp = BoardProperties.getProperties("Bema");

   // first setup the environment
   BoardSetup bs = BoardSetup.getSetup();
   if (skip_setup) bs.setSkipSetup();
   if (force_setup) bs.setForceSetup(true);
   if (skip_splash) bs.setSkipSplash();
   if (allow_debug) bs.setAllowDebug();
   if (use_lila) bs.setUseLila();
   if (use_workspace != null) bs.setDefaultWorkspace(use_workspace);
   bs.setJavaArgs(java_args);
   bs.doSetup();

   bs.setSplashTask("Setting up Metrics and Logging");
   BoardMetrics.setupMetrics(force_setup);

   if (use_web) {
      String url = bp.getProperty("Bema.web.url");
      if (url == null) {
	 BoardLog.logE("BEMA","Bema.web.url property not defined");
	 use_web = false;
       }
      else {
	 File f = BoardSetup.getPropertyBase();
	 File f1 = new File(f,"webkey");
	 if (!f1.exists()) {
	    BoardLog.logE("BEMA","Can't find webkey file in property directory");
	    use_web = false;
	  }
	 else {
	    try {
	       BufferedReader br = new BufferedReader(new FileReader(f1));
	       String ln = br.readLine();
	       if (ln == null) {
		  BoardLog.logE("BEMA","No key defined in webkey file");
		  use_web = false;
		}
	       else {
		  ln = ln.trim();
		  BumpClient bc = BumpClient.getBump();
		  bc.useWebMint(ln,url);
		}
	       br.close();
	     }
	    catch (IOException e) {
	       BoardLog.logE("BEMA","Unable to read webkey file");
	       use_web = false;
	     }
	  }
       }
    }

   // next start Messaging
   bs.setSplashTask("Setting up messaging");
   BumpClient bc = BumpClient.getBump();

   // next start Eclipse
   bs.setSplashTask("Starting IDE (" + bc.getName() + ") and Updating Projects");
   bc.waitForIDE();


   // ensure various components are setup

   bs.setSplashTask("Initializing components");
   BaleFactory.setup();
   BassFactory.setup();
   BdocFactory.setup();

   bs.setSplashTask("Loading Project Symbols");
   BassFactory.waitForNames();

   for (String s : bp.stringPropertyNames()) {
      if (s.startsWith("Bema.plugin.")) {
	 String nm = bp.getProperty(s);
	 String ld = bp.getProperty(s + ".load");
	 if (nm != null) setupPackage(nm,ld);
       }
    }
   String pinf = bp.getProperty("Bema.pluginfolder");
   if (pinf != null) {
      //TODO: Load items in plugins folder
    }

   bs.setSplashTask("Loading and Caching JavaDoc");
   BdocFactory.getFactory().waitForReady();

   // setup top level session from backup

   bs.setSplashTask("Restoring configuration");
   load_config = null;
   if (restore_session) {
      File cf = BoardSetup.getConfigurationFile();
      load_config = IvyXml.loadXmlFromFile(cf);
    }

   ToolTipManager ttm = ToolTipManager.sharedInstance();
   ttm.setDismissDelay(Integer.MAX_VALUE);

   // now start windows
   BudaRoot root = new BudaRoot(load_config);

   initializePackage("edu.brown.cs.bubbles.bale.BaleFactory",root);
   initializePackage("edu.brown.cs.bubbles.bass.BassFactory",root);

   for (int i = 0; ; ++i) {
      String nm = bp.getProperty("Bema.plugin." + i);
      if (nm != null) initializePackage(nm,root);
      else if (i >= 10) break;
    }

   root.pack();

   root.restoreConfiguration(load_config);

   root.setVisible(true);
   bs.removeSplash();

   if (save_session) {
      Runtime.getRuntime().addShutdownHook(new SaveSession(root));
    }
}



/********************************************************************************/
/*										*/
/*	Initialization by name							*/
/*										*/
/********************************************************************************/

private void setupPackage(String nm,String load)
{
   if (load != null) {
      // setup class loader for load jar file
      // use this instead of Class.forName();
      // see taiga.core.CoreClassLoader
    }

   try {
      Class<?> c = Class.forName(nm);
      Method m = c.getMethod("setup");
      m.invoke(null);
    }
   catch (ClassNotFoundException e) {
      BoardLog.logE("BEMA","Can't find initialization package " + nm);
    }
   catch (Exception e) {
      // missing setup method is okay
    }
}



private void initializePackage(String nm,BudaRoot br)
{
   try {
      Class<?> c = Class.forName(nm);
      Method m = c.getMethod("initialize",BudaRoot.class);
      m.invoke(null,br);
    }
   catch (Throwable e) {
      // missing initialize method is okay, missing class already caught
    }
}




/********************************************************************************/
/*										*/
/*	Methods to save session at the end					*/
/*										*/
/********************************************************************************/

private static class SaveSession extends Thread {

   private BudaRoot for_root;

   SaveSession(BudaRoot br) {
      super("SaveSessionAtEnd");
      for_root = br;
    }

   @Override public void run() {
      File cf = BoardSetup.getConfigurationFile();
      try {
	 // for_root.handleSaveAllRequest();
	 for_root.handleCheckpointAllRequest();
	 for_root.saveConfiguration(cf);
       }
      catch (IOException e) {
	 BoardLog.logE("BEMA","Problem saving session: " + e);
       }
    }

}	// end of inner class SaveSession



}	// end of class BemaMain




/* end of BemaMain.java */



