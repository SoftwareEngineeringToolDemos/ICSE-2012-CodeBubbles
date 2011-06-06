/********************************************************************************/
/*										*/
/*		BoardSetup.java 						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
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



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingSetup;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.filechooser.FileFilter;

import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;



/**
 *	This class handles ensuring that bubbles is set up correctly.  If it isn't
 *	(i.e. bubbles is being run for the first time), then it interactively queries
 *	the user for the proper directories and does the appropriate setup.  It can
 *	be run either stand-alone or directly from any bubbles application.
 *
 **/

public class BoardSetup implements BoardConstants {




/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Entry point for running bubbles setup stand-alone.
 **/

public static void main(String [] args)
{
   new SwingSetup();

   BoardSetup bs = new BoardSetup(args);

   bs.doSetup();

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties system_properties;
private boolean 	force_setup;
private boolean 	ask_workspace;
private boolean 	run_foreground;
private boolean 	has_changed;
private String		install_path;
private boolean 	install_jar;
private String		jar_file;
private String		jar_directory;
private String		eclipse_directory;
private String		default_workspace;
private boolean 	auto_update;
private boolean 	do_uninstall;
private int		setup_count;
private boolean 	update_setup;
private boolean 	must_restart;
private boolean 	show_splash;
private boolean 	allow_debug;
private boolean 	use_lila;
private List<String>	java_args;
private BoardSplash	splash_screen;
private long		run_size;


private static String	       prop_base;


private static BoardSetup	board_setup = null;

private static final long MIN_MEMORY = 1024*1024*1024;


private static String [] ivy_props = new String [] {
   "IVY", "edu.brown.cs.IVY", "edu.brown.cs.ivy.IVY"
};

private static String [] ivy_env = new String [] {
   "IVY", "BROWN_IVY_IVY", "BROWN_IVY"
};

static {
   prop_base = System.getProperty("edu.brown.cs.bubbles.BASE");
   if (prop_base == null) prop_base = BOARD_PROP_BASE;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Constructor for setting up an instance of the setup module with default values.
 *	This is the setup that should be done for various bubbles applications.
 **/

public static synchronized BoardSetup getSetup()
{
   if (board_setup == null) {
      board_setup = new BoardSetup();
    }
   return board_setup;
}



private BoardSetup()
{
   system_properties = BoardProperties.getProperties("System");

   force_setup = false;

   install_path = system_properties.getProperty(BOARD_PROP_INSTALL_DIR);
   auto_update = system_properties.getBoolean(BOARD_PROP_AUTO_UPDATE,true);
   install_jar = false;
   jar_directory = null;
   jar_file = null;
   do_uninstall = false;
   must_restart = false;
   update_setup = false;
   show_splash = true;
   splash_screen = null;
   allow_debug = false;
   use_lila = false;

   eclipse_directory = system_properties.getProperty(BOARD_PROP_ECLIPSE_DIR);
   default_workspace = system_properties.getProperty(BOARD_PROP_ECLIPSE_WS);
   ask_workspace = system_properties.getBoolean(BOARD_PROP_ECLIPSE_ASK_WS,true);
   run_foreground = system_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,true);

   if (!checkWorkspace()) default_workspace = null;

   run_size = Runtime.getRuntime().maxMemory();
   run_size = system_properties.getLong(BOARD_PROP_JAVA_VM_SIZE,run_size);
   if (run_size < MIN_MEMORY) run_size = MIN_MEMORY;

   java_args = null;

   has_changed = false;

   setup_count = 0;
}



private BoardSetup(String [] args)
{
   this();

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-f")) {                                // -force
	    force_setup = true;
	  }
	 else if (args[i].startsWith("-E") && i+1 < args.length) {      // -Eclipse <eclipse install directory>
	    eclipse_directory = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-B") && i+1 < args.length) {      // -Bubbles <bubbles install directory>
	    install_path = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-w") && i+1 < args.length) {      // -workspace <workspace>
	    default_workspace = args[++i];
	    has_changed = true;
	  }
	 else if (args[i].startsWith("-u")) {                           // -update
	    update_setup = true;
	  }
	 else if (args[i].startsWith("-U")) {                           // -Uninstall
	    do_uninstall = true;
	  }
	 else if (args[i].startsWith("-nosp")) {                        // -nosplash
	    show_splash = false;
	  }
	 else badArgs();
       }
      else badArgs();
    }
}




private void badArgs()
{
   System.err.println("BOARD: setup [-E eclipse_dir] [-B bubbles_dir] [-w workspace]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 *	Set the flag to force a new setup.
 **/

public void setForceSetup(boolean fg)
{
   force_setup = fg;
}



/**
 *	Set the flag to skip setup.  This should only be done if the caller knows
 *	that eclipse, the workspace, and the bubbles directory are all defined
 *	correctly.  It is used primarily for restarting bubbles.
 **/

public void setSkipSetup()
{
   setup_count = -1;
}


/**
 *	Skip the splash screen.
 **/

public void setSkipSplash()
{
   show_splash = false;
}


/**
 *	Allow remote debugging of bubbles
 **/

public void setAllowDebug()
{
   allow_debug = true;
}



/**
 *	Runs with lila monitor
 **/

public void setUseLila()
{
   use_lila = true;
}



/**
 *	Set the default Eclipse workspace.  This is used to let the user specify
 *	the workspace on the command line
 **/

public void setDefaultWorkspace(String ws)
{
   default_workspace = ws;
   if (!checkWorkspace()) default_workspace = null;
}



/**
 *	Set the java arguments to be used when restarting the system.  This is used
 *	to ensure that restart uses the same arguments as the original start.
 **/

public void setJavaArgs(Collection<String> args)
{
   java_args = new ArrayList<String>();
   if (args != null) java_args.addAll(args);
}



/**
 *	Set the java arguments to be used when restarting the system.  This is used
 *	to ensure that restart uses the same arguments as the original start.
 **/

public void setJavaArgs(String [] args)
{
   java_args = new ArrayList<String>();
   if (args != null) for (String s : args) java_args.add(s);
}



/**
 *	Returns the file location of the bubbles configuration file which is generally
 *	stored in the eclipse workspace.
 **/

public static File getConfigurationFile()
{
   BoardSetup bs = getSetup();

   File wsd = new File(bs.default_workspace);

   // TODO: This should be in the plugin subdirectory

   return new File(wsd,BOARD_CONFIGURATION_FILE);
}



/**
 *	Returns the file location of the bubbles history configuration file which is
 *	usually stored in the eclipse workspace.  This file contains history information
 *	such as prior group memberships and the task shelf.
 **/

public static File getHistoryFile()
{
   BoardSetup bs = getSetup();

   File wsd = new File(bs.default_workspace);

   // If null is returned, history is kept with the configuration

   // TODO: This should be in the plugin subdirectory

   return new File(wsd,BOARD_HISTORY_FILE);
}



/**
 *	Returns the file location of the bubbles documentation configruation file that is
 *	usually stored in the eclipse workspace.  This file contains history information
 *	such as prior group memberships and the task shelf.
 **/

public static File getDocumentationFile()
{
   BoardSetup bs = getSetup();

   File wsd = new File(bs.default_workspace);

   if (!wsd.exists() || !wsd.isDirectory()) {
      BoardLog.logX("BOARD","Bad board setup " + bs.default_workspace + " " + bs.eclipse_directory + " " +
		       bs.jar_file + " " + bs.jar_directory + " " + bs.install_path + " " +
		       bs.install_jar);
    }

   return new File(wsd,BOARD_DOCUMENTATION_FILE);
}



/**
 *	Returns the file location of the bubbles plugin directory.  This is where we should
 *	be saving various files and auxilliary information.
 **/

public static File getBubblesPluginDirectory()
{
   BoardSetup bs = getSetup();

   File wsd = new File(bs.default_workspace);

   if (!wsd.exists() || !wsd.isDirectory()) {
      BoardLog.logX("BOARD","Bad board setup " + bs.default_workspace + " " + bs.eclipse_directory + " " +
		       bs.jar_file + " " + bs.jar_directory + " " + bs.install_path + " " +
		       bs.install_jar);
    }

   File f1 = new File(wsd,".metadata");
   File f2 = new File(f1,".plugins");
   File f3 = new File(f2,"edu.brown.cs.bubbles.bedrock");
   if (!f3.exists()) f3.mkdirs();
   if (!f3.exists() || !f3.isDirectory()) {
      BoardLog.logE("BOARD","Bad plugin directory " + f3);
    }

   return f3;
}



/**
 *	Get a string describing this version of bubbles.
 **/

public static String getVersionData()
{
   return BoardUpdate.getVersionData();
}



/**
 *	Get property base directory (~/.bubbles)
 **/

public static File getPropertyBase()
{
   return new File(prop_base);
}



void setRunSize(long sz)
{
   run_size = sz;
}



/**
 *	Return the library resource path
 **/

public String getLibraryPath(String item)
{
   File f = null;

   if (install_jar && jar_directory != null) {
      f = new File(jar_directory);
    }
   else if (install_path != null) {
      f = new File(install_path);
    }
   else return null;

   f = new File(f,BOARD_INSTALL_LIBRARY);
   f = new File(f,item);
   if (!f.exists()) return null;

   return f.getPath();
}



/********************************************************************************/
/*										*/
/*	Splash screen methods							*/
/*										*/
/********************************************************************************/

/**
 *	Set the current task to be displayed at the bottom of the splash dialog.
 **/

public void setSplashTask(String id)
{
   if (splash_screen != null) splash_screen.setCurrentTask(id);
}



/**
 *	Set the % done to be displayed at the bottom of the splash dialog.  This
 *	does nothing for now.
 **/

public void setSplashPercent(int v)
{
   if (splash_screen != null) splash_screen.setPercentDone(v);
}



/**
 *	Indicate that setup is complete by removing the splash dialog.
 **/

public void removeSplash()
{
   if (splash_screen != null) {
      splash_screen.remove();
      splash_screen = null;
    }
}




/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

void resetProperties()
{
   File ivv = new File(prop_base);
   if (!ivv.exists()) ivv.mkdir();

   for (String s : BOARD_RESOURCE_PROPS) {
      File f1 = new File(ivv,s);
      f1.delete();
    }

   if (install_jar) checkJarResources();
   else if (install_path != null) checkLibResources();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Entry point for doing setup for a bubbles applications.  Any application should
 *	first consruct an instance of BoardSetup and then call this method.  Once the
 *	method returns, bubbles should be ready to use.
 **/

public boolean doSetup()
{
  if (do_uninstall) {
      uninstall();
      return false;
    }

   boolean firsttime = checkDefaultInstallation();

   if (show_splash && splash_screen == null && !firsttime) {
      splash_screen = new BoardSplash();
      splash_screen.start();
    }

   if (setup_count != 0 && default_workspace != null && eclipse_directory != null) {
      BoardLog.setup();
      if (setup_count < 0) BoardUpdate.setVersion();
      return false;
    }

   setup_count = 1;

   setSplashTask("Checking configuration");

   boolean needsetup = force_setup;
   needsetup |= !checkEclipse();
   needsetup |= !checkPlugin();
   needsetup |= !checkInstall() && !install_jar;
   needsetup |= !checkWorkspace();

   if (install_jar && !update_setup) {
      setSplashTask("Checking for newer version");
      BoardUpdate.checkUpdate(jar_file,java_args);
    }
   else if (install_jar || jar_directory != null) {
      BoardUpdate.setVersion();
    }

   if (!update_setup) {
      setSplashTask("Getting configuration information");
      if (needsetup) handleSetup();
      else if (ask_workspace || has_changed) {
	 WorkspaceDialog wd = new WorkspaceDialog();
	 if (!wd.process()) {
	    BoardLog.logE("BOARD","BUBBLES: Setup aborted");
	    System.exit(1);
	  }
	 if (has_changed || wd.hasChanged()) saveProperties();
       }
    }

   boolean needupdate = force_setup | update_setup;
   needupdate |= !checkDates();

   if (needupdate) {
      setSplashTask("Updating Eclipse plugin");
      updatePlugin();
    }

   setSplashTask("Checking messaging configuration");
   setupIvy();

   if (default_workspace != null) {
      system_properties.setProperty(BOARD_PROP_ECLIPSE_WS,default_workspace);
    }

   BoardLog.setup();

   setSplashTask("Checking libraries");
   String cp = System.getProperty("java.class.path");
   for (String s : BOARD_LIBRARY_FILES) {
      if (!s.endsWith(".jar")) continue;
      s = s.replace('/',File.separatorChar);
      if (!cp.contains(s)) must_restart = true;
    }

   if (must_restart) {
      restartBubbles();
    }

   return must_restart;
}





private void setupIvy()
{
   File ivv = new File(System.getProperty("user.home"),".ivy");
   File f4 = new File(ivv,"Props");
   if (ivv.exists() && ivv.isDirectory() && f4.exists()) {
      if (IvySetup.setup(ivv)) {
	 return;
       }
    }

   boolean ivyfound = false;
   for (int i = 0; !ivyfound && i < ivy_props.length; ++i) {
      if (System.getProperty(ivy_props[i]) != null) ivyfound = true;
    }
   for (int i = 0; !ivyfound && i < ivy_env.length; ++i) {
      if (System.getenv(ivy_env[i]) != null) ivyfound = true;
    }
   if (ivyfound) return;

   String ivydir = null;
   if (install_jar) ivydir = jar_directory;
   else {
      File fi = new File(install_path);
      File fi2 = new File(fi.getParentFile(),"ivy");
      if (fi2.exists() && fi2.isDirectory()) ivydir = fi2.getAbsolutePath();
      else ivydir = install_path;
    }
   System.setProperty("edu.brown.cs.IVY",ivydir);

   Properties p = new Properties();

   try {
      Registry rmireg = LocateRegistry.getRegistry("valerie");
      Object o = rmireg.lookup("edu.brown.cs.ivy.mint.registry");
      if (o != null) {
	 p.setProperty("edu.brown.cs.ivy.mint.registryhost","valerie.cs.brown.edu");
       }
    }
   catch (Exception e) { }

   p.setProperty("BROWN_IVY_IVY",ivydir);
   p.setProperty("edu.brown.cs.ivy.IVY",ivydir);

   if (!ivv.exists()) {
      if (!ivv.mkdir()) {
	 BoardLog.logE("BOARD","Unable to create directory " + ivv);
	 reportError("Unable to create directory " + ivv);
	 System.exit(1);
       }
    }

   try {
      FileOutputStream os = new FileOutputStream(f4);
      p.storeToXML(os,"SETUP on " + new Date().toString() + " by BOARD");
      os.close();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem saving ivy: " + e);
      reportError("Problem saving ivy: " + e);
      System.exit(1);
    }
}



private void uninstall()
{
   if (checkPlugin()) {
      File pins = getPluginDirectory();
      if (pins == null) return;
      File pin = new File(pins,BOARD_BUBBLES_PLUGIN);
      pin.delete();
    }

   File bdir = new File(BOARD_PROP_BASE);
   if (bdir.exists()) deleteAll(bdir);
}



private void deleteAll(File f)
{
   if (!f.exists()) return;

   if (f.isDirectory()) {
      for (File fd : f.listFiles()) {
	 deleteAll(fd);
       }
    }

   f.delete();
}




/********************************************************************************/
/*										*/
/*	Setup Dialog								*/
/*										*/
/********************************************************************************/

private void handleSetup()
{
   SetupDialog sd = new SetupDialog();
   if (!sd.process()) {
      BoardLog.logE("BOARD","Setup aborted by user");
      System.exit(1);
    }

   WorkspaceDialog wd = new WorkspaceDialog();
   if (!wd.process()) {
      BoardLog.logE("BOARD","Setup aborted by user/workspace dialog");
      System.exit(1);
    }

   saveProperties();
}



private void saveProperties()
{
   if (install_path != null) system_properties.setProperty(BOARD_PROP_INSTALL_DIR,install_path);
   system_properties.setProperty(BOARD_PROP_AUTO_UPDATE,auto_update);
   system_properties.setProperty(BOARD_PROP_ECLIPSE_DIR,eclipse_directory);
   system_properties.setProperty(BOARD_PROP_ECLIPSE_WS,default_workspace);
   system_properties.setProperty(BOARD_PROP_ECLIPSE_ASK_WS,ask_workspace);
   system_properties.setProperty(BOARD_PROP_ECLIPSE_FOREGROUND,run_foreground);
   String logl = system_properties.getProperty(BOARD_PROP_LOG_LEVEL);
   if (logl == null || logl.length() == 0) {
      if (install_jar) logl = "WARNING";
      else logl = "DEBUG";
      system_properties.setProperty(BOARD_PROP_LOG_LEVEL,logl);
    }

   if (jar_directory != null) system_properties.setProperty(BOARD_PROP_JAR_DIR,jar_directory);

   String vmargs = system_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
   if (vmargs == null) {
      system_properties.setProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS,"-Xmx512m");
    }

   try {
      system_properties.save();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Unable to save system properties: " + e);
      reportError("Unable to save system properties: " + e);
      System.exit(2);
    }
}




/********************************************************************************/
/*										*/
/*	Check if eclipse is setup correctly					*/
/*										*/
/********************************************************************************/

private boolean checkEclipse()
{
   if (eclipse_directory == null) return false;
   File ed = new File(eclipse_directory);
   if (!checkEclipseDirectory(ed)) return false;

   return true;
}



private boolean checkPlugin()
{
   if (!checkEclipse()) return false;

   File pins = getPluginDirectory();
   File pin = new File(pins,BOARD_BUBBLES_PLUGIN);
   if (!pin.exists() || !pin.canRead()) return false;

   return true;
}



private static boolean checkEclipseDirectory(File ed)
{
   if (!ed.exists() || !ed.isDirectory()) return false;

   boolean execfnd = false;
   for (String s : BOARD_ECLIPSE_START) {
      File binf = new File(ed,s);
      if (binf.exists() && binf.canExecute()) {
	 execfnd = true;
	 break;
       }
    }
   if (!execfnd) return false;

   File pdf = new File(ed,BOARD_ECLIPSE_PLUGINS);
   if (!pdf.exists() || !pdf.isDirectory()) {
      File ddf = new File(ed,BOARD_ECLIPSE_DROPINS);
      if (!ddf.exists() || !ddf.isDirectory()) return false;
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Check if bubbles is installed correctly 				*/
/*										*/
/********************************************************************************/

private boolean checkInstall()
{
   // first check if we are running from a complete jar
   boolean ok = true;
   try {
      InputStream ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN);
      if (ins == null) ok = false;
      else ins.close();
      for (String s : BOARD_RESOURCE_PROPS) {
	 ins = getClass().getClassLoader().getResourceAsStream(s);
	 if (ins == null) ok = false;
	 else ins.close();
       }
      install_jar = ok;
      if (install_jar) {
	 URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
	 String file = url.toString();
	 if (file.startsWith("jar:file:/")) file = file.substring(9);
	 if (file.length() >= 3 && file.charAt(0) == '/' &&
		Character.isLetter(file.charAt(1)) && file.charAt(2) == ':' &&
		File.separatorChar == '\\')
	    file = file.substring(1);
	 int idx = file.lastIndexOf('!');
	 if (idx >= 0) file = file.substring(0,idx);
	 if (File.separatorChar != '/') file = file.replace('/',File.separatorChar);
	 file = file.replace("%20"," ");
	 File f = new File(file);
	 jar_file = f.getPath();
	 jar_directory = f.getParent();
	 checkJarResources();
	 checkJarLibraries();
       }
    }
   catch (IOException e) {
      install_jar = false;
    }

   // otherwise check for a valid installation
   if (install_path == null) return false;
   File ind = new File(install_path);
   if (!checkInstallDirectory(ind)) return false;

   if (!install_jar) {
      checkLibResources();
    }

   return true;
}



private static boolean checkInstallDirectory(File ind)
{
   if (!ind.exists() || !ind.isDirectory()) return false;
   int fct = 0;
   for (String s : BOARD_BUBBLES_START) {
      File ex = new File(ind,s);
      if (ex.exists() && ex.canExecute() && !ex.isDirectory()) ++fct;
    }
   if (fct == 0) return false;

   File libb = new File(ind,BOARD_INSTALL_LIBRARY);
   if (!libb.exists() || !libb.isDirectory()) return false;
   File inf = new File(libb,BOARD_RESOURCE_PLUGIN);
   if (!inf.exists() || !inf.canRead()) return false;
   for (String s : BOARD_RESOURCE_PROPS) {
      inf = new File(libb,s);
      if (!inf.exists() || !inf.canRead()) return false;
    }
   for (String s : BOARD_LIBRARY_FILES) {
      inf = new File(libb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing library file " + s);
       }
    }
   for (String s : BOARD_LIBRARY_EXTRAS) {
      inf = new File(libb,s);
      if (!inf.exists() || !inf.canRead()) {
	 BoardLog.logX("BOARD","Missing library file " + s);
       }
    }

   return true;
}



private void checkJarResources()
{
   File ivv = new File(prop_base);
   if (!ivv.exists()) ivv.mkdir();

   for (String s : BOARD_RESOURCE_PROPS) {
      try {
	 File f1 = new File(ivv,s);
	 InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream(s);
	 if (f1.exists()) {
	    ensurePropsDefined(s,ins);
	  }
	 else {
	    FileOutputStream ots = new FileOutputStream(f1);
	    copyFile(ins,ots);
	  }
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","Problem setting up jar resource " + s + ": " + e);
	 reportError("Problem setting up jar resource " + s + ": " + e);
	 System.exit(1);
       }
    }
}




private void checkJarLibraries()
{
   File root = new File(jar_directory);
   File libd = new File(root,BOARD_INSTALL_LIBRARY);
   if (!libd.exists()) libd.mkdir();

   for (String s : BOARD_LIBRARY_FILES) {
      must_restart |= extractLibraryResource(s,libd,false);
    }

   for (String s : BOARD_LIBRARY_EXTRAS) {
      extractLibraryResource(s,libd,true);
    }
}


private boolean extractLibraryResource(String s,File libd,boolean force)
{
   boolean upd = false;

   String xs = s;
   int idx = s.lastIndexOf("/");
   if (idx >= 0) {
      xs = s.replace('/',File.separatorChar);
      String hd = xs.substring(0,idx);
      File fd = new File(libd,hd);
      if (!fd.exists()) fd.mkdirs();
    }

   try {
      File f1 = new File(libd,xs);
      if (!force && f1.exists()) return false;
      upd = true;
      InputStream ins = BoardSetup.class.getClassLoader().getResourceAsStream(s);
      if (ins != null) {
	 FileOutputStream ots = new FileOutputStream(f1);
	 copyFile(ins,ots);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem setting up jar lib resource " + s + ": " + e,e);
      reportError("Problem setting up jar lib resource " + s + ": " + e);
      System.exit(1);
    }

   return upd;
}



private void checkLibResources()
{
   File ivv = new File(prop_base);
   if (!ivv.exists()) ivv.mkdir();
   File lbv = new File(install_path,BOARD_INSTALL_LIBRARY);

   for (String s : BOARD_RESOURCE_PROPS) {
      try {
	 File f1 = new File(ivv,s);
	 File f2 = new File(lbv,s);
	 InputStream ins = new FileInputStream(f2);
	 if (f1.exists()) {
	    ensurePropsDefined(s,ins);
	  }
	 else {
	    OutputStream ots = new FileOutputStream(f1);
	    copyFile(ins,ots);
	  }
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","Problem setting up lib resource " + s + " (" + lbv + "," + install_path + ")",e);
	 reportError("Problem setting up lib resource " + s + ": " + e);
	 System.exit(1);
       }
    }
}



private void ensurePropsDefined(String nm,InputStream ins) throws IOException
{
   BoardProperties ups = BoardProperties.getProperties(nm);
   BoardProperties dps = new BoardProperties(ins);

   boolean chng = false;

   for (String pnm : dps.stringPropertyNames()) {
      String v = dps.getProperty(pnm);
      if (v != null && !ups.containsKey(pnm)) {
	 ups.setProperty(pnm,v);
	 chng = true;
       }
    }

   if (chng) ups.save();
}



/********************************************************************************/
/*										*/
/*	Check for a valid eclipse workspace					*/
/*										*/
/********************************************************************************/

private boolean checkWorkspace()
{
   if (default_workspace == null) return false;
   File wsd = new File(default_workspace);
   if (!checkWorkspaceDirectory(wsd)) return false;

   default_workspace = wsd.getAbsolutePath();
   return true;
}



private static boolean checkWorkspaceDirectory(File wsd)
{
   if (wsd == null) return false;
   if (!wsd.exists() || !wsd.isDirectory()) return false;
   File df = new File(wsd,BOARD_ECLIPSE_WS_DATA);

   if (!df.exists() || !df.canRead()) return false;

   return true;
}



private boolean checkDates()
{
   long dlm = 0;

   if (install_jar) {
      try {
	 URL u = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
	 if (u == null) return false;
	 URLConnection uc = u.openConnection();
	 dlm = uc.getLastModified();
	 // TODO: this is the overall jar file, not the bedrock jar file
       }
      catch (IOException e) {
	 BoardLog.logE("BOARD","PROBLEM LOADING URL: " + e);
       }
    }
   else {
      File ind = new File(install_path);
      File libb = new File(ind,BOARD_INSTALL_LIBRARY);
      File inf = new File(libb,BOARD_RESOURCE_PLUGIN);
      dlm = inf.lastModified();
    }

   File pdf = getPluginDirectory();
   File bdf = new File(pdf,BOARD_BUBBLES_PLUGIN);
   long edlm = bdf.lastModified();

   if (dlm > 0 && edlm > 0 && edlm >= dlm) return true;
   if (edlm > 0 && !auto_update) return true;

   return false;
}




/********************************************************************************/
/*										*/
/*	File methods								*/
/*										*/
/********************************************************************************/

private void copyFile(InputStream ins,OutputStream ots) throws IOException
{
   byte [] buf = new byte[8192];

   for ( ; ; ) {
      int ct = ins.read(buf,0,buf.length);
      if (ct < 0) break;
      ots.write(buf,0,ct);
    }

   ins.close();
   ots.close();
}



/********************************************************************************/
/*										*/
/*	Plugin update methods							*/
/*										*/
/********************************************************************************/

private void updatePlugin()
{
   // ideally, should check if eclipse is currently running?

   try {
      InputStream ins = null;
      if (install_jar) {
	 ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN);
       }
      else {
	 File ind = new File(install_path);
	 File libb = new File(ind,BOARD_INSTALL_LIBRARY);
	 File inf = new File(libb,BOARD_RESOURCE_PLUGIN);
	 ins = new FileInputStream(inf);
       }
      File pdf = getPluginDirectory();
      File bdf = new File(pdf,BOARD_BUBBLES_PLUGIN);
      OutputStream ots = new FileOutputStream(bdf);

      copyFile(ins,ots);

      system_properties.setProperty(BOARD_PROP_ECLIPSE_CLEAN,true);
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem updating bubble eclipse plugin: " + e);
      reportError("Problem updating bubble eclipse plugin: " + e);
      System.exit(3);
    }
}



private File getPluginDirectory()
{
   File edf = new File(eclipse_directory);

   File pdf = new File(edf,BOARD_ECLIPSE_PLUGINS);

   if (!pdf.exists() || !pdf.isDirectory()) {
      File ddf = new File(edf,BOARD_ECLIPSE_DROPINS);
      if (ddf.exists() && ddf.isDirectory()) pdf = ddf;
    }

   return pdf;
}




/********************************************************************************/
/*										*/
/*	Restart methods 							*/
/*										*/
/********************************************************************************/

private void restartBubbles()
{
   if (jar_file == null) return;

   saveProperties();

   setSplashTask("Restarting with new configuration");

   File dir1 = new File(jar_directory);
   File dir = new File(dir1,BOARD_INSTALL_LIBRARY);

   StringBuffer cp = new StringBuffer();
   cp.append(jar_file);
   for (String s : BOARD_LIBRARY_FILES) {
      if (!s.endsWith(".jar")) continue;
      s = s.replace('/',File.separatorChar);
      File f = new File(dir,s);
      cp.append(File.pathSeparator);
      cp.append(f.getPath());
    }

   List<String> args = new ArrayList<String>();
   if (java_args != null) args.addAll(java_args);

   BoardLog.logD("BOARD","RESTART: java -Xmx" + run_size + " -cp " + cp.toString() + " " +
		    BOARD_RESTART_CLASS + " -nosetup");
   boolean dorun = true;

   try {
      int idx = 0;
      args.add(idx++,"java");
      if (use_lila) {
	 File lf = new File(dir,"LagHunter-4.jar");
	 if (lf.exists()) {
	    File f2 = new File(dir1,"LiLaConfiguration.ini");
	    File f3 = new File(dir,"LiLaConfiguration.ini");
	    if (!f2.exists() && f3.exists()) {
	       try {
		  FileInputStream fis = new FileInputStream(f3);
		  FileOutputStream fos = new FileOutputStream(f2);
		  copyFile(fis,fos);
		}
	       catch (IOException e) { }
	     }
	    String lc = "-javaagent:" + lf.getPath() + "=/useLiLaConfigurationFile";
	    args.add(idx++,lc);
	    BoardLog.logD("BOARD","Use lila: " + lc);
	  }
       }
      args.add(idx++,"-Xmx" + run_size);
      args.add(idx++,"-cp");
      args.add(idx++,cp.toString());
      if (allow_debug) {
	 args.add(idx++,"-Xdebug");
	 args.add(idx++,"-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n");
       }
      args.add(idx++,BOARD_RESTART_CLASS);
      args.add(idx++,"-nosetup");
      ProcessBuilder pb = new ProcessBuilder(args);
      if (dorun) pb.start();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem restarting bubbles: " + e);
      reportError("Problem restarting bubbles: " + e);
      System.exit(1);
    }

   if (dorun) System.exit(0);

   BoardLog.logE("BOARD","RESTART FAILED");
}




/********************************************************************************/
/*										*/
/*	Default installation checks						*/
/*										*/
/********************************************************************************/

private boolean checkDefaultInstallation()
{
   boolean firsttime = (system_properties.size() == 0);
   if (!firsttime) return false;

   boolean jarok = true;
   try {
      InputStream ins = getClass().getClassLoader().getResourceAsStream(BOARD_RESOURCE_PLUGIN);
      if (ins == null) jarok = false;
      else ins.close();
    }
   catch (IOException e) {
      jarok = false;
    }
   if (!jarok) return firsttime;

   URL url = getClass().getClassLoader().getResource(BOARD_RESOURCE_PLUGIN);
   String file = url.toString();
   if (file.startsWith("jar:file:/")) file = file.substring(9);
   if (file.length() >= 3 && file.charAt(0) == '/' &&
	  Character.isLetter(file.charAt(1)) && file.charAt(2) == ':' &&
	  File.separatorChar == '\\')
      file = file.substring(1);
   int idx = file.lastIndexOf('!');
   if (idx >= 0) file = file.substring(0,idx);
   if (File.separatorChar != '/') file = file.replace('/',File.separatorChar);
   file = file.replace("%20"," ");
   File f = new File(file);
   if (!f.exists()) return firsttime;

   jar_file = f.getPath();
   jar_directory = f.getParent();
   install_jar = true;

   File fe = new File(jar_directory,"eclipse");
   if (checkEclipseDirectory(fe)) {
      eclipse_directory = f.getPath();
      firsttime = false;
    }

   has_changed = true;

   return firsttime;
}




/********************************************************************************/
/*										*/
/*	Setup dialog management 						*/
/*										*/
/********************************************************************************/

private class SetupDialog implements ActionListener, CaretListener {

   private JButton accept_button;
   private JButton install_button;
   private JDialog working_dialog;
   private boolean result_status;
   private JLabel eclipse_warning;
   private JLabel bubbles_warning;

   SetupDialog() {
      SwingGridPanel pnl = new SwingGridPanel();

      pnl.beginLayout();
      pnl.addBannerLabel("Bubbles Environment Setup");

      pnl.addSeparator();

      pnl.addFileField("Eclipse Installation Directory",eclipse_directory,
	       JFileChooser.DIRECTORIES_ONLY,
	       new EclipseDirectoryFilter(),this,this,null);

      eclipse_warning = new JLabel("Warning!");  //edited by amc6
      eclipse_warning.setToolTipText("Not a valid Eclipse installation directory");
      eclipse_warning.setForeground(WARNING_COLOR);
      pnl.add(eclipse_warning);

      bubbles_warning = new JLabel("Warning!");
      bubbles_warning.setToolTipText("Not a valid Code Bubbles installation directory");
      bubbles_warning.setForeground(WARNING_COLOR);

      pnl.addSeparator();

      if (!install_jar) {
	 pnl.addFileField("Bubbles Installation Directory",install_path,
		  JFileChooser.DIRECTORIES_ONLY,
		  new InstallDirectoryFilter(),this,null);
	 pnl.add(bubbles_warning);
	 pnl.addSeparator();
       }

      pnl.addBoolean("Automatically Update Bubbles",auto_update,this);
      pnl.addBoolean("Run Eclipse in Foreground",run_foreground,this);

      pnl.addSeparator();
      install_button = pnl.addBottomButton("INSTALL BUBBLES","INSTALL",this);
      accept_button = pnl.addBottomButton("OK","OK",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();

      working_dialog = new JDialog((JFrame) null,"Bubbles Environment Setup",true);
      working_dialog.setContentPane(pnl);
      working_dialog.pack();
    }

   boolean process() {
      checkStatus();
      result_status = false;
      working_dialog.setVisible(true);
      return result_status;
    }

   private void checkStatus() { //edited by amc6
      if (checkEclipse() && checkPlugin() && (install_jar || checkInstall())) {
	 accept_button.setEnabled(true);
       }
      else {
	 accept_button.setEnabled(false);
       }

      if (checkEclipse() && !checkPlugin() && (install_jar || checkInstall())) {
	 install_button.setEnabled(true);
       }
      else {
	 install_button.setEnabled(false);
       }

      if (checkEclipse()) {
	 eclipse_warning.setVisible(false);
       }
      else {
	 eclipse_warning.setVisible(true);
       }

      if (checkInstall()) {
	 bubbles_warning.setVisible(false);
       }
      else {
	 bubbles_warning.setVisible(true);
       }
   }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("Eclipse Installation Directory")) {
	 JTextField tf = (JTextField) e.getSource();
	 File ef = new File(tf.getText());
	 eclipse_directory = ef.getAbsolutePath();
	 has_changed = true;
       }
      else if (cmd.equals("Bubbles Installation Directory")) {
	 JTextField tf = (JTextField) e.getSource();
	 File inf = new File(tf.getText());
	 install_path = inf.getPath();
	 has_changed = true;
       }
      else if (cmd.equals("Automatically Update Bubbles")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 auto_update = cbx.isSelected();
	 has_changed = true;
       }
      else if (cmd.equals("Run Eclipse in Foreground")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 run_foreground = cbx.isSelected();
	 has_changed = true;
       }
      else if (cmd.equals("INSTALL")) {
	 updatePlugin();
	 force_setup = false;
       }
      else if (cmd.equals("OK")) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
      else if (cmd.equals("CANCEL")) {
	 result_status = false;
	 working_dialog.setVisible(false);
       }
      else {
	 BoardLog.logE("BOARD","Unknown SETUP DIALOG command: " + cmd);
       }
      checkStatus();
    }

   @Override public void caretUpdate(CaretEvent e) {
      checkStatus();
    }

}	// end of inner class SetupDialog




private static class EclipseDirectoryFilter extends FileFilter {

   public boolean accept(File f) {
      //return checkEclipseDirectory(f);  //edited by amc6
      return true;
    }

   public String getDescription()	{ return "Eclipse Installation Directory"; }

}	// end of inner class EclipseDirectoryFilter




private static class InstallDirectoryFilter extends FileFilter {

   public boolean accept(File f) {
      //return checkInstallDirectory(f);  //edited by amc6
      return true;
    }

   public String getDescription()	{ return "Bubbles Installation Directory"; }

}	// end of inner class EclipseDirectoryFilter




/********************************************************************************/
/*										*/
/*	Eclipse workspace dialog management					*/
/*										*/
/********************************************************************************/

private class WorkspaceDialog implements ActionListener, KeyListener {

   private JButton accept_button;
   private JDialog working_dialog;
   private boolean result_status;
   private boolean ws_changed;
   private JLabel workspace_warning;


   WorkspaceDialog() {
      SwingGridPanel pnl = new SwingGridPanel();

      pnl.beginLayout();
      pnl.addBannerLabel("Bubbles Workspace Setup");

      pnl.addSeparator();

      JTextField textfield = pnl.addFileField("Eclipse Workspace",default_workspace,JFileChooser.DIRECTORIES_ONLY,
	       new WorkspaceDirectoryFilter(),this,null);

      textfield.addKeyListener(this);

      workspace_warning = new JLabel("Warning");//added by amc6
      workspace_warning.setToolTipText("Not a vaid Eclipse Workspace");
      workspace_warning.setForeground(WARNING_COLOR);

      pnl.add(workspace_warning);

      pnl.addSeparator();

      pnl.addBoolean("Always Ask for Workspace",ask_workspace,this);

      pnl.addSeparator();
      accept_button = pnl.addBottomButton("OK","OK",this);
      pnl.addBottomButton("CANCEL","CANCEL",this);
      pnl.addBottomButtons();

      working_dialog = new JDialog((JFrame) null,"Bubbles Workspace Setup",true);
      working_dialog.setContentPane(pnl);
      working_dialog.pack();
    }

   boolean process() {
      ws_changed = false;
      checkStatus();
      result_status = false;
      working_dialog.setVisible(true);
      return result_status;
    }

   boolean hasChanged() 			{ return ws_changed; }

   private void checkStatus() {
      if (checkWorkspace()) {
	 accept_button.setEnabled(true);
	 workspace_warning.setVisible(false);
       }
      else {
	 accept_button.setEnabled(false);
	 workspace_warning.setVisible(true);
       }
    }

   public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("Eclipse Workspace")) {
	 JTextField tf = (JTextField) e.getSource();
	 File ef = new File(tf.getText());
	 String np = ef.getPath();
	 if (!np.equals(default_workspace)) ws_changed = true;
	 default_workspace = np;
       }
      else if (cmd.equals("Always Ask for Workspace")) {
	 JCheckBox cbx = (JCheckBox) e.getSource();
	 if (ask_workspace != cbx.isSelected()) ws_changed = true;
	 ask_workspace = cbx.isSelected();
       }
      else if (cmd.equals("OK")) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
      else if (cmd.equals("CANCEL")) {
	 result_status = false;
	 working_dialog.setVisible(false);
       }
      else {
	 BoardLog.logE("BOARD","Unknown WORKSPACE DIALOG command: " + cmd);
       }
      checkStatus();
    }

   public void keyPressed(KeyEvent e) {
      BoardLog.logD("BOARD", "KeyEvent handled: " + KeyEvent.getKeyText(e.getKeyCode()));
      if(accept_button.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
	 result_status = true;
	 working_dialog.setVisible(false);
       }
   }

   public void keyTyped(KeyEvent e) {

   }

   public void keyReleased(KeyEvent e) {

   }

}	// end of inner class SetupDialog



private static class WorkspaceDirectoryFilter extends FileFilter {

   public boolean accept(File f) {
      //return checkWorkspaceDirectory(f); //edited by amc6
      return true;
    }

   public String getDescription()	{ return "Eclipse Workspace Directory"; }

}	// end of inner class WorkspaceDirectoryFilter




/********************************************************************************/
/*										*/
/*	Method to report an error to the user					*/
/*										*/
/********************************************************************************/

private void reportError(String msg)
{
   JOptionPane.showMessageDialog(null,msg,"Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
}



}	// end of class BoardSetup




/* end of BoardSetup.java */



