/********************************************************************************/
/*										*/
/*		PybaseLaunchConfig.java 					*/
/*										*/
/*	Launch configuration representation for Python Bubbles			*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.util.Map;
import java.util.HashMap;




public class PybaseLaunchConfig implements PybaseDebugConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String  config_name;
private String  config_id;
private int     config_number;
private File    base_file;
private boolean is_saved;
private Map<String,String> config_attrs;

private static IdCounter launch_counter = new IdCounter();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseLaunchConfig(String nm)
{
   config_name = nm;
   config_number = launch_counter.nextValue();
   config_id = "LAUNCH_" + Integer.toString(config_number);
   config_attrs = new HashMap<String,String>();
   base_file = null;
   is_saved = false;
}


PybaseLaunchConfig(Element xml) 
{
   config_name = IvyXml.getAttrString(xml,"NAME");
   config_number = IvyXml.getAttrInt(xml,"ID");
   launch_counter.noteValue(config_number);
   config_id = "LAUNCH+" + Integer.toString(config_number);
   
   config_attrs = new HashMap<String,String>();
   for (Element ae : IvyXml.children(xml,"ATTR")) {
      config_attrs.put(IvyXml.getAttrString(ae,"KEY"),IvyXml.getAttrString(ae,"VALUE"));
    }
   String fn = IvyXml.getTextElement(xml,"FILE");
   if (fn == null) base_file = null;
   else base_file = new File(fn);
   is_saved = true;
}



PybaseLaunchConfig(String nm,PybaseLaunchConfig orig)
{
   config_name = nm;
   config_number = launch_counter.nextValue();
   config_id = "LAUNCH_" + Integer.toString(config_number);
   config_attrs = new HashMap<String,String>(orig.config_attrs);
   base_file = orig.base_file;
   is_saved = false;
}
   
/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return config_name; }
String getId()			        	{ return config_id; }
void setName(String nm)                         { config_name = nm; }

public File getFileToRun()			{ return base_file; }
public void setFileToRun(File f)		{ base_file = f; }


void setAttribute(String k,String v)
{
   if (k == null) return;
   if (v == null) config_attrs.remove(k);
   else config_attrs.put(k,v);
}

boolean isSaved()                               { return is_saved; }
void setSaved(boolean fg)                       { is_saved = fg; }

public String [] getEnvironment()		{ return null; }
public File getWorkingDirectory()		{ return null; }
public String getEncoding()			{ return null; }
public String getPySrcPath()			{ return null; }

public String [] getCommandLine()
{
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      OutputMethods                                                           */
/*                                                                              */
/********************************************************************************/

void outputSaveXml(IvyXmlWriter xw)
{
   xw.begin("CONFIG");
   xw.field("NAME",config_name);
   xw.field("ID",config_number);
   xw.textElement("FILE",base_file);
   for (Map.Entry<String,String> ent : config_attrs.entrySet()) {
      xw.begin("ATTR");
      xw.field("KEY",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("ATTR");
    }
   xw.end("CONFIG");
}


void outputBubbles(IvyXmlWriter xw)
{
   xw.begin("CONFIGURATION");
   xw.field("ID",config_id);
   xw.field("NAME",config_name);
   xw.field("WORKING",!is_saved);
   xw.field("DEBUG",true);
   for (Map.Entry<String,String> ent : config_attrs.entrySet()) {
      xw.begin("ATTRIBUTE");
      xw.field("NAME",ent.getKey());
      xw.field("TYPE","java.lang.String");
      xw.cdata(ent.getValue());
      xw.end("ATTRIBUTE");
    }
   xw.begin("TYPE");
   xw.field("NAME","PYTHON");
   xw.end("TYPE");
   xw.end("CONFIGURATION");
}
   
   

}	// end of class PybaseLaunchConfig




/* end of PybaseLaunchConfig.java */
