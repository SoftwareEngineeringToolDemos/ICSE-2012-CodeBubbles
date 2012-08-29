/********************************************************************************/
/*										*/
/*		PybaseDebugBreakpoint.java					*/
/*										*/
/*	Handle debugging breakpoint for Bubbles from Python			*/
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

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.ivy.xml.IvyXml;


import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.BadLocationException;

import org.w3c.dom.Element;

import java.io.File;


public class PybaseDebugBreakpoint implements PybaseDebugConstants, PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private IFileData       file_data;
private Position        file_position;
private String		debug_condition;
private boolean 	condition_enabled;
private String		function_name;
private long		last_modified;
private boolean         is_enabled;
private String          break_id;
private int             break_number;
private boolean         is_tracepoint;

private static IdCounter break_counter = new IdCounter();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseDebugBreakpoint(IFileData fd,int line) throws PybaseException
{
   file_data = fd;
   IDocument d = fd.getDocument();
   try {
      int off = d.getLineOffset(line);
      file_position = new Position(off);
      d.addPosition(file_position);
    }
   catch (BadLocationException ex) {
      throw new PybaseException("Bad breakpoint location",ex);
    }
   
   break_number = break_counter.nextValue();
   break_id = "BREAK_" + Integer.toString(break_number);
   debug_condition = null;
   condition_enabled = false;
   function_name = null;
   last_modified = 0;
   is_enabled = true;
   is_tracepoint = false;
}


PybaseDebugBreakpoint(PybaseMain pm,Element xml) throws PybaseException
{
   break_number = IvyXml.getAttrInt(xml,"ID");
   break_counter.noteValue(break_number);
   break_id = "BREAK_" + Integer.toString(break_number);
   
   break_id = IvyXml.getAttrString(xml,"ID");
   String fnm = IvyXml.getTextElement(xml,"FILE");
   file_data = pm.getFileData(fnm,null);
   int line = IvyXml.getAttrInt(xml,"LINE");
   IDocument d = file_data.getDocument();
   try {
      int off = d.getLineOffset(line);
      file_position = new Position(off);
      d.addPosition(file_position);
    }
   catch (BadLocationException ex) {
      throw new PybaseException("Bad breakpoint location",ex);
    }
   
   debug_condition = IvyXml.getTextElement(xml,"CONDITION");
   condition_enabled = IvyXml.getAttrBool(xml,"CONDENABLED");;
   function_name = null;
   last_modified = 0;
   is_enabled = IvyXml.getAttrBool(xml,"ENABLED");      
   is_tracepoint = IvyXml.getAttrBool(xml,"TRACEPOINT");
}


 

/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getId()                                  { return break_id; }

public File getFile()				{ return file_data.getFile(); }

public int getLine()
{
   int off = file_position.getOffset();
   try {
      return file_data.getDocument().getLineOfOffset(off);
    }
   catch (BadLocationException ex) {
      return -1;
    }
}

public boolean isEnabled()                      { return is_enabled; }
public String getCondition()			{ return debug_condition; }
public boolean isConditionEnabled()		{ return condition_enabled; }

public void setConditionEnabled(boolean e)	{ condition_enabled = e; }
public void setCondition(String c)
{
   if (c != null && c.trim().length() == 0) c = null;
   debug_condition = c;
}

void setProperty(String p,String v)
{
   if (p == null) return;
   if (p.equals("ENABLE") || p.equals("ENABLED")) {
      if (v == null) is_enabled = true;
      else is_enabled = Boolean.parseBoolean(v);
    }
   else if (p.equals("DISABLE") || p.equals("DISABLED")) {
      is_enabled = false;
    }
}



public String getFunctionName()
{
   File file = getFile();
   if (file == null || !file.exists()) {
      return "None";
    }

   if (file.lastModified() == last_modified) {
      return function_name;
    }

/********************
   try {
      IPythonNature nature = getPythonNature();
      if(nature == null){
	 lastModifiedTimeCached = 0;
	 return "None";
       }
      ICodeCompletionASTManager astManager = nature.getAstManager();
      if(astManager == null){
	 lastModifiedTimeCached = 0;
	 return "None";
       }
      //Only mark it as found if we were able to get the python nature (otherwise, this could change later
      //if requesting during a setup)
      if(nature.startRequests()){ //start requests, as we'll ask for resolve and get module.
	 SourceModule sourceModule = null;
	 try{
	    String modName = nature.resolveModule(fileStr);
	    if(modName != null){
	       //when all is set up, this is the most likely path we're going to use
	       //so, we shouldn't have delays when the module is changed, as it's already
	       //ok for use.
	       IModule module = astManager.getModule(modName, nature, true);
	       if(module instanceof SourceModule){
		  sourceModule = (SourceModule) module;
		}
	     }
	  }
	 finally{
	    nature.endRequests();
	  }
	 lastModifiedTimeCached = file.lastModified();
		
	 if(sourceModule == null){
	    //the text for the breakpoint requires the function name, and it may be requested before
	    //the ast manager is actually restored (so, modName is None, and we have little alternative
	    //but making a parse to get the function name)
	    IDocument doc = getDocument();
	    sourceModule = (SourceModule) AbstractModule.createModuleFromDoc("", null, doc, nature, -1);
	  }
	
	 int lineToUse = getLineNumber() - 1;
	
	 if(sourceModule == null || sourceModule.getAst() == null || lineToUse < 0){
	    functionName = "None";
	    return functionName;
	  }
	
	 SimpleNode ast = sourceModule.getAst();
	
	 functionName = NodeUtils.getContextName(lineToUse, ast);
	 if(functionName == null){
	    functionName = ""; //global context
	  }
	 return functionName;
	
       }
      //If it was found, it would've already returned. So, match anything as we couldn't determine it.
      functionName = "None";
	
    }
   catch (Exception e) {
      //Some error happened determining it. Match anything.
      Log.log("Error determining breakpoint context. Breakpoint at: "+file+" will match any context.", e);
      functionName = "None";
    }
********************/
   return function_name;
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(IvyXmlWriter xw) 
{
   xw.begin("BREAKPOINT");
   xw.field("ID",break_number);
   xw.field("FILE",file_data.getFile());
   xw.field("FUNCTION",getFunctionName());
   xw.field("LINE",getLine());
   xw.field("OFFSET",file_position.getOffset());
   xw.field("ENABLED",is_enabled);
   xw.field("CONDENABLED",condition_enabled);
   xw.field("TRACEPOINT",is_tracepoint);
   if (debug_condition != null) xw.cdataElement("CONDITION",debug_condition);
   xw.end("BREAKPOINT");
}



void outputBubbles(IvyXmlWriter xw)
{
   xw.begin("BREAKPOINT");
   xw.field("ENABLED",is_enabled);
   xw.field("ID",break_id);
   xw.field("LINE",getLine());
   xw.field("STARTPOS",file_position.getOffset());
   xw.field("ENDPOS",file_position.getOffset() + file_position.getLength());
   xw.field("TRACEPOINT",is_tracepoint);
   if (debug_condition != null) {
      xw.begin("CONDITION");
      xw.field("ENABLED",condition_enabled);
      xw.text(debug_condition);
      xw.end("CONDITION");
    }
   xw.end("BREAKPOINT");
}


}	// end of class PybaseDebugBreakpoint




/* end of PybaseDebugBreakpoint.java */
