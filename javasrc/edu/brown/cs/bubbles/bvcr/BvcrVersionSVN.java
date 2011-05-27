/********************************************************************************/
/*										*/
/*		BvcrVersionSVN.java						*/
/*										*/
/*	Bubble Version Collaboration Repository interface to SVN		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;
import java.io.*;
import java.util.*;
import java.text.*;



class BvcrVersionSVN extends BvcrVersionManager implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		svn_root;
private String		svn_command;
private String		svn_name;

private static BoardProperties bvcr_properties = BoardProperties.getProperties("Bvcr");

private static SimpleDateFormat SVN_DATE = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS");





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrVersionSVN(BvcrProject bp)
{
   super(bp);

   svn_command = bvcr_properties.getProperty("bvcr.svn.command","svn");

   findSvnRoot();
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BvcrVersionManager getRepository(BvcrProject bp,File srcdir)
{
   if (srcdir == null) return null;

   File f1 = new File(srcdir,".svn");
   if (f1.exists() && f1.isDirectory()) {
      System.err.println("BVCR: HANDLE SVN REPOSITORY " + srcdir);
      return new BvcrVersionSVN(bp);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getRootDirectory() 		{ return svn_root; }

String getRepositoryName()
{
   if (svn_name != null) return svn_name;
   return super.getRepositoryName();
}


void getDifferences(BvcrDifferenceSet ds)
{
   String cmd = svn_command  + " diff";

   List<File> diffs = ds.getFilesToCompute();
   if (diffs == null) {
      cmd += " " + svn_root.getPath();
    }
   else if (diffs.size() == 0) return;
   else {
      for (File f : diffs) {
	 cmd += " " + f.getPath();
       }
    }

   DiffAnalyzer da = new DiffAnalyzer(ds);
   runCommand(cmd,da);
}




/********************************************************************************/
/*										*/
/*	Find the top of the SVN repository					*/
/*										*/
/********************************************************************************/

private void findSvnRoot()
{
   File f = new File(for_project.getSourceDirectory());
   for ( ; ; ) {
      File fp = f.getParentFile();
      File fp1 = new File(fp,".svn");
      if (!fp1.exists()) break;
      f = fp;
    }
   svn_root = f;

   XmlCommand cmd = new XmlCommand(svn_command + " --xml info " + svn_root.getPath());
   Element x = cmd.getXml();

   // check for .uid file before using svn id
   Element ee = IvyXml.getChild(x,"entry");
   Element re = IvyXml.getChild(ee,"repository");

   if (getRepositoryId() == null) repository_id = IvyXml.getTextElement(re,"uuid");
   svn_name = IvyXml.getTextElement(re,"root");
}



/********************************************************************************/
/*										*/
/*	History gathering for a file						*/
/*										*/
/********************************************************************************/

void findHistory(File f,IvyXmlWriter xw)
{
   XmlCommand cmd = new XmlCommand(svn_command + " --xml log -g " + f.getPath());
   Element x = cmd.getXml();
   BvcrFileVersion prior = null;
   Collection<BvcrFileVersion> fvs = new ArrayList<BvcrFileVersion>();
   for (Element le : IvyXml.children(x,"logentry")) {
      //TODO: this doesn't handle merged or labeled versions
      String rev = IvyXml.getAttrString(le,"revision");
      String auth = IvyXml.getTextElement(le,"author");
      String msg = IvyXml.getTextElement(le,"message");
      Date d = null;
      try {
	 d = SVN_DATE.parse(IvyXml.getTextElement(le,"date"));
       }
      catch (ParseException e) {
	 System.err.println("BVCR: Problem parsing date: " + e);
       }
      BvcrFileVersion fv = new BvcrFileVersion(f,rev,d,auth,msg);
      if (prior != null) prior.addPriorVersion(fv);
      prior = fv;
      fvs.add(fv);
    }

   xw.begin("HISTORY");
   xw.field("FILE",f.getPath());
   for (BvcrFileVersion fv : fvs) {
      fv.outputXml(xw);
    }
   xw.end("HISTORY");
}




}	// end of class BvcrVersionSVN



/* end of BvcrVersionSVN.java */
