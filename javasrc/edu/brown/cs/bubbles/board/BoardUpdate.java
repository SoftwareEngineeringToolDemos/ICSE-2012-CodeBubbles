/********************************************************************************/
/*										*/
/*		BoardUpdate.java						*/
/*										*/
/*	Bubbles attribute and property management automatic update manager	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* SVN: $Id$ */



package edu.brown.cs.bubbles.board;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.*;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;



/**
 *	This class is used to check and perform automatic updates of the bubbles
 *	binary (jar file) as they become available.  It is important that this
 *	class contains no nested classes and uses nothing other than the standard
 *	Java library classes so it can easily be put in a jar file to download
 *	to do the update.
 **/

public class BoardUpdate {


/********************************************************************************/
/*										*/
/*	Constant definitions for update 					*/
/*										*/
/********************************************************************************/

/**
 *	URL prefix indicating where bubbles information is web-accessible.
 **/

private static final String BUBBLES_DIR = "http://www.cs.brown.edu/people/spr/bubbles/";



/**
 *	URL for the currently available version relative to BUBBLES_DIR
 **/
private static final String VERSION_URL = "/version.xml";


/**
 *	File path for the current version information.	This only has to work at
 *	Brown since we will be the only one releasing versions.  One might want to
 *	make this machine independent.	Running this main with -version will cause
 *	the minor version number to be updated by one in the given file.  This allows
 *	a script to generate a new version easily.
 **/

private static final String VERSION_FILE = "../../../../../../lib/version.xml";


/**
 *	File path for version description in readable English
 **/

private static final String VERSION_DESC = "../../../../../../lib/version.txt";



/**
 *	URL for the jar version of this file.  The updater will download this file
 *	if the current version of bubbles is out of date and then run it with the
 *	path of the bubbles jar as the argument. This is relative to BUBBLES_DIR.
 **/

private static final String UPDATER_URL = "/updater.jar";


/**
 *	URL for the newest version of code bubbles.  The updater will download this
 *	jar and replace the original with it and the reexecute it.  This is relative
 *	to BUBBLES_DIR.
 **/

private static final String BUBBLES_URL = "/bubbles.jar";


/**
 *	Path in the jar file of the current version file
 **/

private static final String VERSION_RESOURCE = "version.xml";



/**
 *	Arguments to File.createTempFile for updater file
 **/

private static final String	UPDATER_PREFIX = "bubblesupdater";
private static final String	UPDATER_SUFFIX = ".jar";




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static String	bubbles_dir = BUBBLES_DIR;
private String		jar_file;
private List<String>	java_args;
private long		max_memory;

private static String	version_data = "Build_" + System.getProperty("user.name");



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

/**
 *	Main entry point.  This is invoked by bubbles when an update is required.
 **/

public static void main(String [] args)
{
   if (args.length == 0) badArgs();
   if (args[0].startsWith("-v")) updateVersionFile();
   else {
      BoardUpdate bu = new BoardUpdate(args);
      bu.setupUpdate();
    }

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Static method to check if an update is needed				*/
/*										*/
/********************************************************************************/

static void checkUpdate(String jarfile,List<String> javaargs)
{
   try {
      InputStream vins = BoardUpdate.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE);
      Element ve = getVersionXml(vins);
      version_data = getMajor(ve) + "." + getMinor(ve) + " @ " + getBubblesDir(ve);
      vins.close();
      bubbles_dir = getBubblesDir(ve);

      URL u = new URL(bubbles_dir + VERSION_URL);
      InputStream uins = u.openStream();
      Element ue = getVersionXml(uins);
      uins.close();

      if (sameVersion(ue,ve)) return;		// we are up to date

      BoardUpdate bu = new BoardUpdate(jarfile,javaargs);
      bu.startUpdate();
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem checking for update: " + t);
    }
}



static String getVersionData()
{
   return version_data;
}



static void setVersion()
{
   try {
      InputStream vins = BoardUpdate.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE);
      Element ve = getVersionXml(vins);
      version_data = getMajor(ve) + "." + getMinor(ve) + " @ " + getBubblesDir(ve);
      vins.close();
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem setting version: " + t);
    }
}



/********************************************************************************/
/*										*/
/*	Static method to update the version file				*/
/*										*/
/********************************************************************************/

static void updateVersionFile()
{
   try {
      InputStream ins = new FileInputStream(VERSION_FILE);
      Element v = getVersionXml(ins);
      ins.close();
      int mj = getMajor(v);
      int mn = getMinor(v);
      String newminor = Integer.toString(mn+1);
      v.setAttribute("MINOR",newminor);
      saveVersionXml(VERSION_FILE,v);

      PrintWriter pw = new PrintWriter(new FileWriter(VERSION_DESC));
      pw.println("Version " + mj + "." + newminor + " of BUBBLES is now available");
      pw.close();
    }
   catch (Throwable t) {
      System.err.println("BOARD: Problem updating version number: " + t);
      System.exit(1);
    }
}




/********************************************************************************/
/*										*/
/*	Methods for managing the XML version file				*/
/*										*/
/********************************************************************************/

private static Element getVersionXml(InputStream ins) throws IOException
{
   try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(ins);
      return doc.getDocumentElement();
    }
   catch (ParserConfigurationException e) {
      System.err.println("BOARD: Problem creating XML parser for versioning");
      throw new IOException("Can't parse xml",e);
    }
   catch (SAXException e) {
      System.err.println("BOARD: Bad xml version file: " + e);
      throw new IOException("Can't parse xml",e);
    }
}




private static void saveVersionXml(String file,Element e) throws IOException
{
   Writer w = new FileWriter(file);
   addXml(e,w);
   w.write("\n");
   w.close();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardUpdate(String [] args)
{
   // used when run to do the actual update
   jar_file = null;
   java_args = new ArrayList<String>();
   max_memory = 0;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m")) {
	    try {
	       max_memory = Long.parseLong(args[i].substring(2));
	     }
	    catch (NumberFormatException e) {
	       max_memory = 0;
	     }
	  }
	 else {
	    System.err.println("BOARDUPDATE: Illegal argument: " + args[i]);
	  }
       }
      else if (jar_file == null) jar_file = args[i];
      else java_args.add(args[i]);
    }

   if (jar_file == null) badArgs();
}



private BoardUpdate(String jarfile,List<String> javaargs)
{
   // used when run to setup the update
   jar_file = jarfile;
   java_args = new ArrayList<String>();
   if (javaargs != null) java_args.addAll(javaargs);
   max_memory = 0;
}



private static void badArgs()
{
   System.err.println("BOARDUPDATE: boardupdate [-version]");
   System.err.println("BOARDUPDATE: boardupdate <jar file>");
}



/********************************************************************************/
/*										*/
/*	Methods to start the update by downloading and starting the updater	*/
/*										*/
/********************************************************************************/

private void startUpdate() throws IOException
{
   File f = File.createTempFile(UPDATER_PREFIX,UPDATER_SUFFIX);
   OutputStream ots = new BufferedOutputStream(new FileOutputStream(f));
   URL u = new URL(bubbles_dir + UPDATER_URL);
   InputStream ins = u.openStream();
   copyFile(ins,ots);

   System.err.println("BUBBLES: Starting update");

   if (java_args == null) java_args = new ArrayList<String>();
   java_args.add(0,"java");
   java_args.add(1,"-jar");
   java_args.add(2,f.getPath());
   java_args.add(3,jar_file);
   java_args.add(4,"-m" + Runtime.getRuntime().maxMemory());
   ProcessBuilder pb = new ProcessBuilder(java_args);
   pb.start();

   System.exit(0);
}



/********************************************************************************/
/*										*/
/*	Methods to actually do the update					*/
/*										*/
/********************************************************************************/

private void setupUpdate()
{
   // In case java locks the running jar file, wait for the current process
   // to go away

   JPanel pnl = new JPanel(new BorderLayout());
   pnl.setBackground(new Color(211,232,248));
   JTextField prog = new JTextField();
   pnl.add(prog,BorderLayout.SOUTH);
   JLabel lbl = new JLabel("<html><h1><c>Updating Code Bubbles</c></h1>" +
			      "<br><h2><c>(This could take some time)</c></h2>"+
			      "<br>");
   lbl.setBackground(new Color(211,232,248));
   pnl.add(lbl,BorderLayout.CENTER);
   JFrame splash = new JFrame();
   splash.setContentPane(pnl);
   splash.setUndecorated(true);
   splash.setResizable(false);
   splash.setAlwaysOnTop(false);
   splash.pack();
   Toolkit tk = Toolkit.getDefaultToolkit();
   Dimension ssz = tk.getScreenSize();
   Dimension wsz = splash.getSize();
   int xpos = ssz.width/2 - wsz.width/2;
   int ypos = ssz.height/2 - wsz.height/2;
   splash.setLocation(xpos,ypos);
   splash.setVisible(true);

   prog.setText("Checking jar file is unlocked");

   for (int i = 0; i < 10; ++i) {
      try {
	 FileInputStream fins = new FileInputStream(jar_file);
	 fins.close();
	 break;
       }
      catch (IOException e) { }
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
    }

   boolean usebackup = false;
   File jf0 = new File(jar_file);
   File jf1 = new File(jar_file + ".backup");

   try {
      // first create a backup of the current jar in case we need it
      prog.setText("Saving a backup of the jar file");
      InputStream ins = new FileInputStream(jf0);
      OutputStream ots = new FileOutputStream(jf1);
      jf1.deleteOnExit();
      copyFile(ins,ots);
      usebackup = true;

      prog.setText("Downloading new version of bubbles");
      URL u = new URL(bubbles_dir + BUBBLES_URL);
      ins = u.openStream();
      ots = new FileOutputStream(jf0);
      copyFile(ins,ots);
    }
   catch (Throwable t) {
      System.err.println("BOARDUPDATE: Problem doing update: " + t);

      prog.setText("Update Problem: restoring old version");

      if (usebackup) {
	 try {
	    InputStream ins = new FileInputStream(jf1);
	    OutputStream ots = new FileOutputStream(jf0);
	    copyFile(ins,ots);
	  }
	 catch (IOException e) {
	    System.err.println("BOARDUPDATE: problem restoring backup " + jf1);
	    System.exit(1);
	  }
       }
      // This is here to prevent infinite loops in the event of update failure
      System.exit(1);
    }

   try {
      prog.setText("Restarting bubbles");
      List<String> args = new ArrayList<String>();
      args.add("java");
      if (max_memory > 0) args.add("-Xmx" + max_memory);
      args.add("-cp");
      args.add(jf0.getPath());
      args.add("edu.brown.cs.bubbles.board.BoardSetup");
      args.add("-update");

      ProcessBuilder pb = new ProcessBuilder(args);

      Process p = pb.start();
      for ( ; ; ) {
	 try {
	    p.waitFor();
	    break;
	  }
	 catch (InterruptedException e) { }
       }
    }
   catch (IOException e) {
      System.err.println("BOARD: Problem restarting bubbles: " + e);
      System.exit(1);
    }
}



/********************************************************************************/
/*										*/
/*	File management methods 						*/
/*										*/
/********************************************************************************/

private static void copyFile(InputStream ins,OutputStream ots) throws IOException
{
   byte [] buf = new byte[8192];
   for ( ; ; ) {
      int ln = ins.read(buf);
      if (ln < 0) break;
      ots.write(buf,0,ln);
    }
   ins.close();
   ots.close();
}




/********************************************************************************/
/*										*/
/*	Version management							*/
/*										*/
/********************************************************************************/

private static int getMajor(Element e1)
{
   String v = e1.getAttribute("MAJOR");
   if (v == null) return -1;
   try {
      return Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }
   return -1;
}



private static int getMinor(Element e1)
{
   String v = e1.getAttribute("MINOR");
   if (v == null) return -1;
   try {
      return Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }
   return -1;
}




private static String getBubblesDir(Element e1)
{
   String v = e1.getAttribute("URL");
   if (v == null) {
      System.err.println("BOARD UPDATER: Can't find URL in version xml");
      v = BUBBLES_DIR;
    }

   return v;
}




private static boolean sameVersion(Element e1,Element e2)
{
   if (getMajor(e1) != getMajor(e2)) return false;
   if (getMinor(e1) != getMinor(e2)) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	XML output methods (copied from IvyXML) 				*/
/*										*/
/********************************************************************************/

private static void addXml(Node n,Writer w) throws IOException
{
   if (n instanceof Element) {
      Element e = (Element) n;
      w.write("<" + e.getNodeName());
      NamedNodeMap nnm = e.getAttributes();
      for (int i = 0; ; ++i) {
	 Node na = nnm.item(i);
	 if (na == null) break;
	 Attr a = (Attr) na;
	 if (a.getSpecified()) {
	    w.write(" " + a.getName() + "='");
	    outputXmlString(a.getValue(),w);
	    w.write("'");
	  }
       }
      if (e.getFirstChild() == null) w.write(" />");
      else {
	 w.write(">");
	 for (Node cn = n.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
	    addXml(cn,w);
	  }
	 w.write("</" + e.getNodeName() + ">");
       }
    }
   else if (n instanceof CDATASection) {
      w.write("<![CDATA[");
      w.write(n.getNodeValue());
      w.write("]]>");
    }
   else if (n instanceof Text) {
      String s = n.getNodeValue();
      if (s != null) outputXmlString(s,w);
    }
}



private static void outputXmlString(String s,Writer pw)
{
   if (s == null) return;

   try {
      for (int i = 0; i < s.length(); ++i) {
	 char c = s.charAt(i);
	 switch (c) {
	    case '&' :
	       pw.write("&amp;");
	       break;
	    case '<' :
	       pw.write("&lt;");
	       break;
	    case '>' :
	       pw.write("&gt;");
	       break;
	    case '"' :
	       pw.write("&quot;");
	       break;
	    case '\'' :
	       pw.write("&apos;");
	       break;
	    default :
	       pw.write(c);
	       break;
	  }
       }
    }
   catch (IOException e) { }
}





}	// end of class BoardUpdate




/* end of BoardUpdate.java */
