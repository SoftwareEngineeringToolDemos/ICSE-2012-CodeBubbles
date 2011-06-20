/********************************************************************************/
/*										*/
/*		BumpProblemImpl.java						*/
/*										*/
/*	BUblles Mint Partnership problem description holder			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.*;



class BumpProblemImpl implements BumpConstants.BumpProblem, BumpConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	problem_id;
private int	problem_msgid;
private String	problem_message;
private String	for_project;
private File	file_name;
private int	line_number;
private int	start_position;
private int	end_position;
private BumpErrorType error_type;
private int	edit_id;
private List<BumpFix> problem_fixes;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpProblemImpl(Element d,String id,int eid,String proj)
{
   problem_id = id;
   problem_msgid = IvyXml.getAttrInt(d,"MSGID");
   problem_message = IvyXml.getTextElement(d,"MESSAGE");
   for_project = proj;
   String fnm = IvyXml.getTextElement(d,"FILE");
   if (fnm == null) file_name = null;
   else file_name = new File(fnm);
   line_number = IvyXml.getAttrInt(d,"LINE",0);
   start_position = IvyXml.getAttrInt(d,"START");
   end_position = IvyXml.getAttrInt(d,"END");
   error_type = BumpErrorType.NOTICE;
   if (IvyXml.getAttrBool(d,"ERROR")) error_type = BumpErrorType.ERROR;
   else if (IvyXml.getAttrBool(d,"WARNING")) error_type = BumpErrorType.WARNING;
   edit_id = eid;

   setupFixes(d);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProblemId()				{ return problem_id; }
@Override public String getMessage()				{ return problem_message; }
@Override public File getFile() 				{ return file_name; }
@Override public int getLine()					{ return line_number; }
@Override public int getStart() 				{ return start_position; }
@Override public int getEnd()					{ return end_position; }
@Override public BumpErrorType getErrorType()			{ return error_type; }
@Override public int getEditId()				{ return edit_id; }
@Override public String getProject()				{ return for_project; }

@Override public List<BumpFix> getFixes()			{ return problem_fixes; }

int getMessageId()						{ return problem_msgid; }



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void setEditId(int eid) 				{ edit_id = eid; }

void update(Element e)
{
   problem_message = IvyXml.getTextElement(e,"MESSAGE");
   end_position = IvyXml.getAttrInt(e,"END");
   setupFixes(e);
}




private void setupFixes(Element d)
{
   problem_fixes = null;

   for (Element e : IvyXml.children(d,"FIX")) {
      FixImpl fi = new FixImpl(e);
      if (fi.getType() != BumpFixType.NONE) {
	 if (problem_fixes == null) problem_fixes = new ArrayList<BumpFix>();
	 problem_fixes.add(fi);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "PROBLEM:" + getProblemId() + ":" + getMessage() + "@" + getFile() + ":" + getLine();
}



/********************************************************************************/
/*										*/
/*	Problem Fix reprsentation						*/
/*										*/
/********************************************************************************/

private class FixImpl implements BumpConstants.BumpFix {

   private BumpFixType fix_type;
   private Map<String,String> fix_attrs;

   FixImpl(Element e) {
      fix_type = IvyXml.getAttrEnum(e,"TYPE",BumpFixType.NONE);
      fix_attrs = new HashMap<String,String>(4);
      if (for_project != null) fix_attrs.put("PROJECT",for_project);
      for (String s : FIX_PARAMETERS) {
	 String v = IvyXml.getTextElement(e,s);
	 if (v != null) fix_attrs.put(s,v);
       }
    }

   @Override public BumpFixType getType()		{ return fix_type; }
   @Override public String getParameter(String id)	{ return fix_attrs.get(id); }

}	// end of inner class FixImpl



}	// end of class BumpProblemImpl




/* end of BumpProblemImpl.java */



