/********************************************************************************/
/*										*/
/*		BvcrVersionCVS.java						*/
/*										*/
/*	Bubble Version Collaboration Repository interface to CVS		*/
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



class BvcrVersionCVS extends BvcrVersionManager implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		cvs_root;
private String		cvs_command;
private String		cvs_name;

private static BoardProperties bvcr_properties = BoardProperties.getProperties("Bvcr");

private static SimpleDateFormat CVS_DATE = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss ZZZZZ");





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrVersionCVS(BvcrProject bp)
{
   super(bp);

   cvs_command = bvcr_properties.getProperty("bvcr.cvs.command","cvs");

   findCvsRoot();
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static BvcrVersionManager getRepository(BvcrProject bp,File srcdir)
{
   if (srcdir == null) return null;

   File f1 = new File(srcdir,"CVS");
   if (f1.exists() && f1.isDirectory()) {
      System.err.println("BVCR: HANDLE CVS REPOSITORY " + srcdir);
      return new BvcrVersionCVS(bp);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getRootDirectory() 		{ return cvs_root; }

String getRepositoryName()
{
   if (cvs_name != null) return cvs_name;
   return super.getRepositoryName();
}


void getDifferences(BvcrDifferenceSet ds)
{
   String cmd = cvs_command  + " diff";

   List<File> diffs = ds.getFilesToCompute();
   if (diffs == null) {
      cmd += " " + cvs_root.getPath();
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
/*	Find the top of the CVS repository					*/
/*										*/
/********************************************************************************/

private void findCvsRoot()
{
   File f = new File(for_project.getSourceDirectory());
   for ( ; ; ) {
      File fp = f.getParentFile();
      File fp1 = new File(fp,"CVS");
      if (!fp1.exists()) break;
      f = fp;
    }
   cvs_root = f;
}



/********************************************************************************/
/*										*/
/*	History gathering for a file						*/
/*										*/
/********************************************************************************/

void findHistory(File f,IvyXmlWriter xw)
{
   StringCommand cmd = new StringCommand(cvs_command + " log " + f.getPath());
   String rslt = cmd.getContent();

   StringTokenizer tok = new StringTokenizer(rslt,"\n");
   String rev = null;
   Date d = null;
   String auth = null;
   String msg = null;
   BvcrFileVersion prior = null;
   Collection<BvcrFileVersion> fvs = new ArrayList<BvcrFileVersion>();

   while (tok.hasMoreTokens()) {
      String ln = tok.nextToken();
      if (rev == null) {
	 if (ln.startsWith("revision ")) rev = ln.substring(9).trim();
       }
      else {
	 if (ln.startsWith("date: ")) {
	    StringTokenizer ltok = new StringTokenizer(ln,";");
	    while (ltok.hasMoreTokens()) {
	       String itm = ltok.nextToken();
	       int idx = itm.indexOf(":");
	       if (idx >= 0) {
		  String what = itm.substring(0,idx).trim();
		  String cnts = itm.substring(idx+1).trim();
		  if (what.equals("date")) {
		     try {
			d = CVS_DATE.parse(cnts);
		      }
		     catch (ParseException e) { }
		   }
		  else if (what.equals("author")) auth = cnts;
		}
	     }
	  }
	 else if (ln.startsWith("----------------------------")) {
	    if (auth != null && d != null) {
	       BvcrFileVersion fv = new BvcrFileVersion(f,rev,d,auth,msg);
	       if (prior != null) prior.addPriorVersion(fv);
	       prior = fv;
	       fvs.add(fv);
	     }
	    rev = null;
	    d = null;
	    msg = null;
	    auth = null;
	  }
	 else if (msg == null) msg = ln;
	 else msg = msg + "\n" + ln;
       }
    }

   xw.begin("HISTORY");
   xw.field("FILE",f.getPath());
   for (BvcrFileVersion fv : fvs) {
      fv.outputXml(xw);
    }
   xw.end("HISTORY");
}




}	// end of class BvcrVersionCVS



/* end of BvcrVersionCVS.java */

