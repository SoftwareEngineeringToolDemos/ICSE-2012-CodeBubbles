/********************************************************************************/
/*										*/
/*		BvcrFileVersion.java						*/
/*										*/
/*	Bubble Version Collaboration Repository representation of a file version*/
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


import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;
import java.io.*;
import java.util.*;



class BvcrFileVersion implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File				for_file;
private String				version_id;
private Date				version_time;
private String				version_author;
private String				version_message;
private Collection<String>		alternative_ids;
private Collection<BvcrFileVersion>	prior_versions;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrFileVersion(File f,String id,Date when,String auth,String msg)
{
   for_file = f;
   version_id = id;
   version_time = when;
   version_author = auth;
   version_message = msg;

   alternative_ids = null;
   prior_versions = new HashSet<BvcrFileVersion>();
}



BvcrFileVersion(Element xml)
{
   for_file = new File(IvyXml.getAttrString(xml,"FILE"));
   version_id = IvyXml.getAttrString(xml,"ID");
   version_time = new Date(IvyXml.getAttrLong(xml,"TIME"));
   version_author = IvyXml.getAttrString(xml,"AUTHOR");
   version_message = IvyXml.getTextElement(xml,"MESSAGE");

   alternative_ids = null;
   prior_versions = new HashSet<BvcrFileVersion>();

   // set prior versions

   for (Element e : IvyXml.children(xml,"ALTERNATIVE")) {
      addAlternativeId(IvyXml.getText(e));
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addPriorVersion(BvcrFileVersion v)
{
   prior_versions.add(v);
}



void addAlternativeId(String id)
{
   if (alternative_ids == null) alternative_ids = new HashSet<String>();
   alternative_ids.add(id);
}



String getVersionId()			{ return version_id; }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("VERSION");
   xw.field("FILE",for_file);
   xw.field("ID",version_id);
   xw.field("TIME",version_time.getTime());
   xw.field("AUTHOR",version_author);

   for (BvcrFileVersion fv : prior_versions) {
      xw.begin("PRIOR");
      xw.field("ID",fv.getVersionId());
      xw.end("PRIOR");
    }

   if (alternative_ids != null) {
      for (String s : alternative_ids) {
	 xw.textElement("ALTERNATIVE",s);
       }
    }

   xw.cdataElement("MESSAGE",version_message);

   xw.end("VERSION");
}





}	// end of class BvcrFileVersion




/* end of BvcrFileVersion.java */
