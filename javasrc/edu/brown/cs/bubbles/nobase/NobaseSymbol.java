/********************************************************************************/
/*										*/
/*		NobaseSymbol.java						*/
/*										*/
/*	Representation of a javascript symbol (identifier			*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.*;


class NobaseSymbol implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		symbol_name;
private NobaseValue	symbol_value;
private NobaseScope	symbol_scope;
private int		num_assignment;
private boolean 	is_explicit;
private String		bubbles_name;
private NobaseProject	for_project;
private NobaseFile	for_file;
private NobaseAst.NobaseAstNode   def_node;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseSymbol(NobaseProject proj,NobaseFile file,NobaseAst.NobaseAstNode def,
      String name,boolean exp)
{
   symbol_name = name;
   symbol_value = NobaseValue.createUndefined();
   symbol_scope = null;
   num_assignment = 0;
   is_explicit = exp;
   bubbles_name = null;
   for_project = proj;
   for_file = file;
   def_node = def;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return symbol_name; }
NobaseValue getValue()			{ return symbol_value; }
NobaseType getType()
{
   if (symbol_value == null) {
      symbol_value = NobaseValue.createUndefined();
    }
   return symbol_value.getType();
}
String getBubblesName() 		{ return bubbles_name; }
NobaseProject getProject()		{ return for_project; }
NobaseFile getFileData()		{ return for_file; }
NobaseAst.NobaseAstNode getDefNode()	{ return def_node; }

NameType getNameType()
{
   String qnm = bubbles_name;
   if (symbol_value != null && symbol_value.isFunction()) {
     return NameType.FUNCTION;
    }
   else {
      int idx = qnm.indexOf(".");
      if (idx < 0) return NameType.MODULE;
      idx = qnm.indexOf(".",idx+1);
      if (idx >= 0) return NameType.LOCAL;
    }
   return NameType.VARIABLE;
}
String getHandle() {
   if (symbol_value != null && symbol_value.isFunction())
      return bubbles_name + "()";
   return bubbles_name;
}

void setValue(NobaseValue typ)		{ symbol_value = typ; }
void setExplicit()			{ is_explicit = true; }
void addAssignment()			{ ++num_assignment; }
void setBubblesName(String nm)		{ bubbles_name = nm; }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputNameData(NobaseFile rf,IvyXmlWriter xw)
{
   NobaseAstNode an = getDefNode();
   if (an == null) return;

   xw.begin("ITEM");
   xw.field("PROJECT",for_project.getName());
   xw.field("PATH",rf.getFile().getPath());
   xw.field("QNAME",getBubblesName());
   xw.field("JSTYPE",symbol_value.getType().getName());
   xw.field("TYPE",getExternalTypeName());
   int spos = an.getStartPosition();
   int epos = an.getEndPosition();
   xw.field("STARTOFFSET",spos);
   xw.field("LENGTH",epos-spos);
   xw.field("ENDOFFSET",epos);
   String hdl = getHandle();
   if (hdl != null) xw.field("HANDLE",hdl);
   xw.end("ITEM");
}


void outputFullName(IvyXmlWriter xw)
{
   xw.begin("FULLYQUALIFIEDNAME");
   xw.field("NAME",getBubblesName());
   xw.field("JSTYPE",symbol_value.getType().getName());
   xw.field("TYPE",getExternalTypeName());
   xw.end("FULLYQUALIFIEDNAME");
}


private String getExternalTypeName()
{
   String rslt = "Variable";
   if (symbol_value != null) {
      String tnm = symbol_value.getType().getName();
      if (tnm.equalsIgnoreCase("function")) rslt = "Function";
    }
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   if (bubbles_name != null) buf.append(bubbles_name);
   else buf.append(symbol_name);
   if (symbol_value != null) {
      buf.append("=");
      if (symbol_value.getType() == null) buf.append("???");
      else buf.append(symbol_value.getType().getName());
      Object o = symbol_value.getKnownValue();
      if (o != null) {
	 buf.append("[");
	 buf.append(o.toString());
	 buf.append("]");
       }
    }
   return buf.toString();
}



}	// end of class NobaseSymbol




/* end of NobaseSymbol.java */
