/********************************************************************************/
/*										*/
/*		PybaseDebugManager.java 					*/
/*										*/
/*	Manager for debugger configurations and information			*/
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

import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseException;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class PybaseDebugManager implements PybaseDebugConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private PybaseMain				pybase_main;
private Map<String,PybaseLaunchConfig>		config_map;
private File					config_file;
private Map<String,PybaseDebugBreakpoint>	break_map;
private File					break_file;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseDebugManager(PybaseMain pm)
{
   pybase_main = pm;

   config_map = new ConcurrentHashMap<String,PybaseLaunchConfig>();
   config_file = new File(pm.getWorkSpaceDirectory(),CONFIG_FILE);
   break_map = new ConcurrentHashMap<String,PybaseDebugBreakpoint>();
   break_file = new File(pm.getWorkSpaceDirectory(),BREAKPOINT_FILE);

   loadConfigurations();
   loadBreakpoints();
}




/********************************************************************************/
/*										*/
/*	Methods to load/store configurations					*/
/*										*/
/********************************************************************************/

private void loadConfigurations()
{
   Element xml = IvyXml.loadXmlFromFile(config_file);
   for (Element le : IvyXml.children(xml,"LAUNCH")) {
      PybaseLaunchConfig plc = new PybaseLaunchConfig(le);
      config_map.put(plc.getId(),plc);
    }
}


private void saveConfigurations()
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(config_file);
      xw.begin("CONFIGS");
      for (PybaseLaunchConfig plc : config_map.values()) {
	 if (plc.isSaved()) {
	    plc.outputSaveXml(xw);
	  }
       }
      xw.end("CONFIGS");
      xw.close();
    }
   catch (IOException e) {
      PybaseMain.logE("Problem writing out configurations",e);
    }
}



/********************************************************************************/
/*										*/
/*	Configuration management methods					*/
/*										*/
/********************************************************************************/

public void getRunConfigurations(IvyXmlWriter xw)
{
   for (PybaseLaunchConfig plc : config_map.values()) {
      plc.outputBubbles(xw);
    }
}


public void getNewRunConfiguration(String proj,String nm,String clone,IvyXmlWriter xw)
   throws PybaseException
{
   PybaseLaunchConfig plc = null;
   if (clone != null) {
      PybaseLaunchConfig orig = config_map.get(clone);
      if (orig == null) throw new PybaseException("Configuration to clone not found: " + clone);
      if (nm == null) nm = getUniqueName(orig.getName());
      plc = new PybaseLaunchConfig(nm,orig);
    }
   else {
      if (nm == null) nm = getUniqueName("Python Launch");
      plc = new PybaseLaunchConfig(nm);
    }

   if (plc != null) {
      if (proj != null) plc.setAttribute(ATTR_PROJECT,proj);
      plc.outputBubbles(xw);
      handleLaunchNotify(plc,"ADD");
    }
}



private String getUniqueName(String nm)
{
   int idx = nm.lastIndexOf("(");
   if (idx >= 0) nm = nm.substring(0,idx).trim();
   for (int i = 1; ; ++i) {
      String nnm = nm + " (" + i + ")";
      boolean fnd = false;
      for (PybaseLaunchConfig plc : config_map.values()) {
	 if (plc.getName().equals(nnm)) fnd = true;
       }
      if (!fnd) return nnm;
    }
}



public void editRunConfiguration(String lid,String prop,String val,IvyXmlWriter xw)
	throws PybaseException
{
   PybaseLaunchConfig cfg = config_map.get(lid);
   if (cfg == null) throw new PybaseException("Launch configuration " + lid + " not found");
   if (prop.equals("NAME")) {
      cfg.setName(val);
    }
   else {
      cfg.setAttribute(prop,val);
    }

   if (xw != null) cfg.outputBubbles(xw);

   handleLaunchNotify(cfg,"CHANGE");
}



public void saveRunConfiguration(String lid,IvyXmlWriter xw) throws PybaseException
{
   PybaseLaunchConfig cfg = config_map.get(lid);
   if (cfg == null) throw new PybaseException("Launch configuration " + lid + " not found");
   cfg.setSaved(true);
   saveConfigurations();

   handleLaunchNotify(cfg,"CHANGE");
}


public void deleteRunConfiguration(String lid,IvyXmlWriter xw) throws PybaseException
{
   PybaseLaunchConfig cfg = config_map.remove(lid);
   if (cfg == null) return;
   cfg.setSaved(false);
   saveConfigurations();

   handleLaunchNotify(cfg,"REMOVE");
}



private void handleLaunchNotify(PybaseLaunchConfig plc,String reason)
{
   IvyXmlWriter xw = pybase_main.beginMessage("LAUNCHCONFIGEVENT");
   xw.begin("LAUNCH");
   xw.field("REASON",reason);
   xw.field("ID",plc.getId());
   if (!reason.equals("REMOVE")) plc.outputBubbles(xw);
   xw.end("LAUNCH");
   pybase_main.finishMessage(xw);
}



/********************************************************************************/
/*										*/
/*	Load and save breakpoints						*/
/*										*/
/********************************************************************************/

private void loadBreakpoints()
{
}


private void saveBreakpoints()
{
}



/********************************************************************************/
/*										*/
/*	Methods to manage breakpoints						*/
/*										*/
/********************************************************************************/

public void getAllBreakpoints(IvyXmlWriter xw)
{
}



public void setLineBreakpoint(String proj,String id,String file,int line,boolean susvm,boolean trace)
{
}


public void editBreakpoint(String id,String p1,String v1,String p2,String v2,String p3,String v3)
{
}


public void clearLineBreakpoints(String proj,String file,int line)
{
}

}	// end of class PybaseDebugManager




/* end of PybaseDebugManager.java */
