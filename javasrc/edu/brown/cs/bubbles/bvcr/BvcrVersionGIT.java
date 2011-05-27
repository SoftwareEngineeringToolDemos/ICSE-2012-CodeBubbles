/********************************************************************************/
/*										*/
/*		BvcrVersionGIT.java						*/
/*										*/
/*	Bubble Version Collaboration Repository interface to GIT		*/
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

import java.io.*;
import java.util.*;
import java.text.*;



class BvcrVersionGIT extends BvcrVersionManager implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		git_root;
private String		git_command;
private String		git_name;

private static BoardProperties bvcr_properties = BoardProperties.getProperties("Bvcr");

private static SimpleDateFormat GIT_DATE = new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy ZZZZZ");

private static String GIT_LOG_FORMAT = "\"%H%x09%an%x09%ad%x09%s\"";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrVersionGIT(BvcrProject bp)
{
   super(bp);

   git_command = bvcr_properties.getProperty("bvcr.git.command","git");

   findGitRoot();
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BvcrVersionManager getRepository(BvcrProject bp,File srcdir)
{
   if (srcdir == null) return null;

   File fp = srcdir;
   while (fp != null && fp.exists() && fp.isDirectory()) {
      File f2 = new File(fp,".git");
      if (f2.exists() && f2.isDirectory()) {
	 System.err.println("BVCR: HANDLE GIT REPOSITORY " + srcdir);
	 return new BvcrVersionGIT(bp);
       }
      fp = fp.getParentFile();
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getRootDirectory() 		{ return git_root; }

String getRepositoryName()
{
   if (git_name != null) return git_name;
   return super.getRepositoryName();
}


void getDifferences(BvcrDifferenceSet ds)
{
   String cmd = git_command  + "-diff -r -- ";

   List<File> diffs = ds.getFilesToCompute();
   if (diffs == null) {
      cmd += " " + git_root.getPath();
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
/*	Find the top of the GIT repository					*/
/*										*/
/********************************************************************************/

private void findGitRoot()
{
   File f = new File(for_project.getSourceDirectory());
   for ( ; ; ) {
      File fp = f.getParentFile();
      File fp1 = new File(fp,".git");
      if (!fp1.exists()) break;
      f = fp;
    }
   git_root = f;
}



/********************************************************************************/
/*										*/
/*	History gathering for a file						*/
/*										*/
/********************************************************************************/

void findHistory(File f,IvyXmlWriter xw)
{
   StringCommand cmd = new StringCommand(git_command + " log --pretty=format:" + GIT_LOG_FORMAT);

   BvcrFileVersion prior = null;
   Collection<BvcrFileVersion> fvs = new ArrayList<BvcrFileVersion>();
   StringTokenizer tok = new StringTokenizer(cmd.getContent(),"\n\r");
   while (tok.hasMoreTokens()) {
      String ent = tok.nextToken();
      try {
	 StringTokenizer ltok = new StringTokenizer(ent,"\t");
	 String rev = ltok.nextToken();
	 String auth = ltok.nextToken();
	 Date d = GIT_DATE.parse(ltok.nextToken());
	 String msg = ltok.nextToken("\n");
	 BvcrFileVersion fv = new BvcrFileVersion(f,rev,d,auth,msg);
	 if (prior != null) prior.addPriorVersion(fv);
	 prior = fv;
	 fvs.add(fv);
       }
      catch (Throwable e) {
	 System.err.println("BVCR: Problem parsing log entry: " + e);
       }
    }

   xw.begin("HISTORY");
   xw.field("FILE",f.getPath());
   for (BvcrFileVersion fv : fvs) {
      fv.outputXml(xw);
    }
   xw.end("HISTORY");
}




}	// end of class BvcrVersionGIT



/* end of BvcrVersionGIT.java */
