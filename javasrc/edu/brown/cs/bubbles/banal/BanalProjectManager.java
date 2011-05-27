/********************************************************************************/
/*										*/
/*		BanalProjectManager.java					*/
/*										*/
/*	Bubbles ANALysis package project/class data manager			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.*;
import java.util.*;


class BanalProjectManager implements BanalConstants, BumpConstants.BumpChangeHandler {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// project -> classname -> ClassData
private Map<String,Map<String,ClassData>> user_classes;

private boolean 	classes_valid;
private boolean 	accessing_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BanalProjectManager()
{
   user_classes = new HashMap<String,Map<String,ClassData>>();
   classes_valid = false;
   accessing_data = false;

   BumpClient bc = BumpClient.getBump();
   bc.addChangeHandler(this);

   //TODO: register for update notifications from Eclipse
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Collection<ClassData> getClassData()
{
   synchronized (this) {
      checkClassData();

      Collection<ClassData> rslt = new ArrayList<ClassData>();
      for (Map<String,ClassData> mcd : user_classes.values()) {
	 rslt.addAll(mcd.values());
       }

      return rslt;
    }
}




/********************************************************************************/
/*										*/
/*	Methods to get class information from eclipse				*/
/*										*/
/********************************************************************************/

private void checkClassData()
{
   synchronized (this) {
      while (accessing_data) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
      if (classes_valid) return;
      accessing_data = true;
      classes_valid = true;
    }

   for ( ; ; ) {
      BumpClient bump = BumpClient.getBump();

      Element projs = bump.getAllProjects();
      if (projs == null) {
	 BoardLog.logD("BANAL","No projects found");
	 return;
       }

      for (Element pe : IvyXml.children(projs,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 if (user_classes.get(pnm) != null) continue;

	 Element pinfo = bump.openProject(pnm);
	 BoardLog.logD("BANAL","GET PROJECT " + pnm);
	 if (pinfo == null) continue;
	 Map<String,ClassData> mcd = new HashMap<String,ClassData>();

	 Element cinfo = IvyXml.getChild(pinfo,"CLASSES");
	 for (Element ce : IvyXml.children(cinfo,"TYPE")) {
	    String cnm = IvyXml.getAttrString(ce,"NAME");
	    String bnm = IvyXml.getAttrString(ce,"BINARY");
	    String snm = IvyXml.getAttrString(ce,"SOURCE");
	    ClassData cd = new ClassData(pnm,cnm,bnm,snm);
	    mcd.put(cnm,cd);
	  }
	 user_classes.put(pnm,mcd);

	 // Element hinfo = bump.getTypeHierarchy(project_name,null,null);
	 // System.err.println("HIERARCHY DATA: " + IvyXml.convertXmlToString(hinfo));
       }

      synchronized (this) {
	 if (classes_valid) {
	    accessing_data = false;
	    notifyAll();
	    break;
	  }
	 classes_valid = true;
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods for handling file changes					*/
/*										*/
/********************************************************************************/

@Override public void handleFileChanged(String proj,String file)
{
   invalidate(proj);
}


@Override public void handleFileAdded(String proj,String file)
{
   invalidate(proj);
}



@Override public void handleFileRemoved(String proj,String file)
{
   invalidate(proj);
}


private void invalidate(String proj)
{
   synchronized (this) {
      if (accessing_data) return;
      classes_valid = false;
      user_classes.remove(proj);
    }
}




/********************************************************************************/
/*										*/
/*	Class information							*/
/*										*/
/********************************************************************************/

private static class ClassData implements BanalClassData {

   private String class_name;
   private String class_file;
   private String source_file;
   private String class_project;

   ClassData(String pnm,String cnm,String bnm,String snm) {
      class_project = pnm;
      class_name = cnm;
      class_file = bnm;
      source_file = snm;
    }

   @Override public String getName()		{ return class_name; }
   @Override public String getSourceFile()	{ return source_file; }
   @Override public String getProject() 	{ return class_project; }

   @Override public InputStream getClassStream() {
      if (class_file == null) return null;
      try {
	 return new FileInputStream(class_file);
       }
      catch (IOException e) { }
      return null;
    }

   @Override public boolean equals(Object o) {
      if (o instanceof ClassData) {
	 ClassData cd = (ClassData) o;
	 return class_name.equals(cd.class_name);
       }
      return false;
    }

   @Override public int hashCode() {
      return class_name.hashCode();
    }

}	// end of inner class ClassData




}	// end of class BanalProjectManager



/* end of BanalProjectManager.java */
