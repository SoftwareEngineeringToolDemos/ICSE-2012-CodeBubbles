/********************************************************************************/
/*										*/
/*		BbookDisplayBuilder.java					*/
/*										*/
/*	Programmers log book display generator					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bbook;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bnote.*;

import edu.brown.cs.ivy.xml.*;

import javax.activation.*;

import java.util.*;



class BbookDisplayBuilder implements BbookConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String			for_project;
private BnoteTask		for_task;
private String			for_user;
private String			for_class;
private String			for_method;

private Orderings		order_by;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookDisplayBuilder(Map<String,Object> props)
{
   for_project = (String) props.get("PROJECT");
   for_task = (BnoteTask) props.get("TASK");
   for_user = (String) props.get("USER");
   for_class = (String) props.get("CLASS");
   for_method = (String) props.get("METHOD");

   order_by = (Orderings) props.get("ORDERBY");
   if (order_by == null) order_by = Orderings.TIME;
}



/********************************************************************************/
/*										*/
/*	Generation methods							*/
/*										*/
/********************************************************************************/

String generateHtml()
{
   String bkg = BoardSetup.getSetup().getLibraryPath("bbookbkg.gif");

   StringBuffer html = new StringBuffer();

   html.append("<html><body");
   if (bkg != null) html.append(" background='file://" + bkg + "'");
   html.append(" style='margin-left: 24px'>");
   html.append("<h1 align='center' style='font-family: Arial, Helvetica, sans-serif; " +
		  "font-size: 24px'>Programmer's Log Book</h1>");

   generateHtmlOverview(html);

   generateContents(html);

   html.append("</body></html>");

   return html.toString();
}



/********************************************************************************/
/*										*/
/*	Overview table								*/
/*										*/
/********************************************************************************/

private void generateHtmlOverview(StringBuffer buf)
{
   Date start = null;
   Date end = null;

   List<Date> dts = BnoteFactory.getFactory().getDatesForTask(for_project,for_task);
   if (dts != null) {
      for (Date d1 : dts) {
	 if (start == null || start.compareTo(d1) > 0) start = d1;
	 if (end == null || end.compareTo(d1) < 0) end = d1;
       }
    }

   buf.append("<table>");
   if (for_task != null) {
      buf.append("<tr><td>Task:</td><td>");
      buf.append(fixText(for_task.getName()));
      buf.append("</td></tr>");
    }
   if (for_user != null) {
      buf.append("<tr><td>User:</td><td>");
      buf.append(fixText(for_user));
      buf.append("</td></tr>");
    }

   if (for_class != null || for_method != null) {
      buf.append("<tr><td>Code:</td><td>");
      if (for_class == null) buf.append("*.");
      else if (for_class.endsWith(".")) buf.append(for_class + "*.");
      else buf.append(fixText(for_class));
      if (for_method == null) buf.append("*");
      else buf.append(fixText(for_method));
      buf.append("</td></tr>");
    }

   if (start != null) {
      buf.append("<tr><td>Start:</td><td>");
      buf.append(start.toString());
      buf.append("</td></tr>");
    }
   if (end != null) {
      buf.append("<tr><td>Finish:</td><td>");
      buf.append(end.toString());
      buf.append("</td></tr>");
    }

   buf.append("</table>");
   buf.append("<hr />");
}



/********************************************************************************/
/*										*/
/*	Entry generation							*/
/*										*/
/********************************************************************************/

private void generateContents(StringBuffer buf)
{
   List<BnoteEntry> ents = BnoteFactory.getFactory().getEntriesForTask(for_project,for_task);

   for (Iterator<BnoteEntry> it = ents.iterator(); it.hasNext(); ) {
      BnoteEntry ent = it.next();
      if (!isRelevant(ent)) it.remove();
    }

   switch (order_by) {
      case TIME :
	 break;
      case TASK :
	 ents = orderByTask(ents);
	 break;
      case METHOD :
	 ents = orderByMethod(ents);
	 break;
    }

   GenContext ctx = new GenContext();

   for (BnoteEntry be : ents) {
      BnoteTask bt = be.getTask();
      if (ctx.getTask() != bt) {
	 outputTaskHeader(bt,ctx,buf);
       }
      outputEntryData(be,ctx,buf);
    }
}



private boolean isRelevant(BnoteEntry ent)
{
   // project and task already accounted for

   if (for_user != null) {
      String u = ent.getUser();
      if (!for_user.equals(u)) return false;
    }

   if (for_class != null || for_method != null) {
      String enm = ent.getProperty("NAME");
      if (enm == null) return false;
      if (for_class != null) {
	 if (!enm.startsWith(for_class)) return false;
       }
      if (for_method != null) {
	 int idx1 = enm.indexOf("(");
	 int idx;
	 if (idx1 < 0) idx = enm.lastIndexOf(".");
	 else idx = enm.lastIndexOf(".",idx1);
	 String mnm = enm.substring(idx+1);
	 if (!for_method.equals(mnm)) return false;
       }
    }

   return true;
}


private List<BnoteEntry> orderByTask(List<BnoteEntry> ents)
{
   List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();

   while (ents.size() > 0) {
      BnoteTask task = null;
      for (Iterator<BnoteEntry> it = ents.iterator(); it.hasNext(); ) {
	 BnoteEntry ent = it.next();
	 if (task == null) task = ent.getTask();
	 else if (ent.getTask() != task) continue;
	 it.remove();
	 rslt.add(ent);
       }
    }

   return rslt;
}



private List<BnoteEntry> orderByMethod(List<BnoteEntry> ents)
{
   List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();

   while (ents.size() > 0) {
      String method = null;
      for (Iterator<BnoteEntry> it = ents.iterator(); it.hasNext(); ) {
	 BnoteEntry ent = it.next();
	 String mnm = ent.getProperty("NAME");
	 if (method == null) method = mnm;
	 else if (!method.equals(mnm)) continue;
	 it.remove();
	 rslt.add(ent);
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Specific output methods 						*/
/*										*/
/********************************************************************************/

private void outputTaskHeader(BnoteTask bt,GenContext ctx,StringBuffer buf)
{
   setState(ctx,ContextState.TASK,buf);

   boolean full = ctx.setTask(bt);
   buf.append("<h2>Task: " + fixText(bt.getName()) + "</h2>");
   if (full && bt.getDescription() != null) {
      buf.append("<blockquote><p>");
      buf.append(fixTextBlock(bt.getDescription()));
      buf.append("</blockquote>");
    }
}




private void outputEntryData(BnoteEntry ent,GenContext ctx,StringBuffer buf)
{
   // if the date has changed significantly, output that
   // if the user has changed, output that

   switch (ent.getType()) {
      case NONE :
      case TASK :
      case NEW_TASK :
	 return;
      case OPEN :
      case CLOSE :
      case EDIT :
      case SAVE :
	 String nm = ent.getProperty("NAME");
	 if (nm == null) return;
	 setState(ctx,ContextState.WORK,buf);
	 buf.append("<li>");
	 buf.append(ent.getType().toString());
	 buf.append(" ");
	 buf.append(fixText(nm));
	 buf.append("</li>");
	 break;
      case NOTE :
	 setState(ctx,ContextState.NOTE,buf);
	 String nt = ent.getProperty("NOTE");
	 buf.append("<blockquote><p>");
	 buf.append(fixTextBlock(nt));
	 buf.append("</blockquote>");
	 break;
      case ATTACHMENT :
         String fnm = ent.getProperty("SOURCE");
         String aid = ent.getProperty("ATTACHID");
         MimetypesFileTypeMap  mtm = new MimetypesFileTypeMap();
         String mtname = mtm.getContentType(fnm);
         if (mtname == null) break;
         MimeType mt = null;
         try {
            mt = new MimeType(mtname);
          }
         catch (MimeTypeParseException e) {
            break;
          }
         String ptyp = mt.getPrimaryType();
         if (ptyp == null) break;
         if (ptyp.equals("text")) {
          }
         else if (ptyp.equals("audio")) {
          }
         else if (ptyp.equals("image")) {
          }
         else if (ptyp.equals("video")) {
          }
	 break;
    }
}



private void setState(GenContext ctx,ContextState st,StringBuffer buf)
{
   if (st == ctx.getState()) return;

   // handle closing current items within a task
   switch (ctx.getState()) {
      case WORK :
	 buf.append("</ul>");
	 ctx.setState(ContextState.TASK);
	 break;
      case NOTE :
	 ctx.setState(ContextState.TASK);
	 break;
    }

   switch (st) {
      case NONE :
	 buf.append("<hr />");
	 break;
      case TASK :
	 break;
      case WORK :
	 buf.append("<h3>Work on Task:</h3>");
	 buf.append("<ul>");
	 break;
      case NOTE :
	 buf.append("<h3>Notes:</h3>");
	 break;
    }

   ctx.setState(st);
}




/********************************************************************************/
/*										*/
/*	String management							*/
/*										*/
/********************************************************************************/

private String fixText(String s)
{
   if (s == null) return null;

   return IvyXml.xmlSanitize(s);
}


private String fixTextBlock(String s)
{
   if (s == null) return null;

   String s1 = IvyXml.xmlSanitize(s,false);
   s1 = s1.replace("\n\n","\n<p>");
   return s1;
}



/********************************************************************************/
/*										*/
/*	Output context management						*/
/*										*/
/********************************************************************************/

enum ContextState {
   NONE,
   TASK,
   WORK,
   NOTE
}



private static class GenContext {

   private BnoteTask	current_task;
   private Set<BnoteTask> done_tasks;
   private ContextState current_state;

   GenContext() {
      current_task = null;
      done_tasks = new HashSet<BnoteTask>();
      current_state = ContextState.NONE;
    }

   BnoteTask getTask()				{ return current_task; }
   boolean setTask(BnoteTask t) {
      current_task = t;
      return done_tasks.add(t);
    }

   ContextState getState()			{ return current_state; }
   void setState(ContextState st)		{ current_state = st; }

}	// end of inner class GenContext




}	// end of class BbookDisplayBuilder




/* end of BbookDisplayBuilder.java */

