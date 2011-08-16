/********************************************************************************/
/*										*/
/*		BnoteStore.java 						*/
/*										*/
/*	Main access to notebook store						*/
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



package edu.brown.cs.bubbles.bnote;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.util.*;


public class BnoteStore implements BnoteConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	is_local;
private BnoteDatabase	note_db;

private static BnoteStore	the_store = null;

private static Set<String>	field_strings;
private static Set<String>	cdata_strings;


static {
   field_strings = new HashSet<String>();
   field_strings.add("PROJECT");
   field_strings.add("TYPE");
   field_strings.add("USER");
   field_strings.add("TIME");
   field_strings.add("TASK");

   cdata_strings = new HashSet<String>();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BnoteStore()
{
   BoardSetup bs = BoardSetup.getSetup();

   switch (bs.getRunMode()) {
      case CLIENT :
	 is_local = false;
	 // need to handle task notifications and build up task set
	 break;
      case SERVER :
	 is_local = true;
	 MintControl mc = bs.getMintControl();
	 mc.register("<BNOTE TYPE='LOG'><_VAR_0 /></BNOTE>",new LogServer());
	 break;
      default :
	 is_local = true;
	 break;
    }

   note_db = new BnoteDatabase();
}



static BnoteStore createStore()
{
   if (the_store == null) {
      the_store = new BnoteStore();
    }
   return the_store;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BnoteTask> getTasksForProject(String proj)
{
   return note_db.getTasksForProject(proj);
}


List<String> getUsersForTask(String proj,BnoteTask task)
{
   return note_db.getUsersForTask(proj,task);
}


List<String> getNamesForTask(String proj,BnoteTask task)
{
   return note_db.getNamesForTask(proj,task);
}



BnoteTask findTaskById(int id)
{
   return note_db.findTaskById(id);
}




/********************************************************************************/
/*										*/
/*	Static logging entries							*/
/*										*/
/********************************************************************************/

public static BnoteTask defineTask(String name,String proj,String desc)
{
   if (the_store == null) return null;

   return log(proj,null,BnoteEntryType.NEW_TASK,"NAME",name,"DESCRIPTION",desc);
}




public static BnoteTask log(String project,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (the_store == null) return null;

   return the_store.enter(project,task,type,values);
}



public static BnoteTask log(String project,BnoteTask task,BnoteEntryType type,Object ... args)
{
   Map<String,Object> values = new HashMap<String,Object>();
   for (int i = 0; i < args.length-1; i += 2) {
      values.put(args[i].toString(),args[i+1]);
    }

   return log(project,task,type,values);
}



/********************************************************************************/
/*										*/
/*	Internal logging entries						*/
/*										*/
/********************************************************************************/

private BnoteTask enter(String project,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (is_local) {
      return note_db.addEntry(project,task,type,values);
    }

   IvyXmlWriter xw = new IvyXmlWriter();

   if (project != null) values.put("PROJECT",project);
   if (type != null) values.put("TYPE",type);
   if (!values.containsKey("USER")) values.put("USER",System.getProperty("user.name"));
   values.put("TIME",System.currentTimeMillis());
   if (task != null) values.put("TASK",task);

   xw.begin("BNOTE");

   for (Map.Entry<String,Object> ent : values.entrySet()) {
      String k = ent.getKey();
      if (!field_strings.contains(k)) continue;
      Object v = ent.getValue();
      xw.field(k,v.toString());
    }

   for (Map.Entry<String,Object> ent : values.entrySet()) {
      String k = ent.getKey();
      if (field_strings.contains(k)) continue;
      Object v = ent.getValue();
      xw.begin("DATA");
      xw.field("KEY",k);
      if (cdata_strings.contains(k)) xw.cdata(v.toString());
      else xw.text(v.toString());
    }

   xw.end("BNOTE");

   sendEntry(xw.toString());

   return null;
}



private BnoteTask sendEntry(String ent)
{
   MintControl mc = BoardSetup.getSetup().getMintControl();
   String msg = "<BNOTE TYPE='LOG'>" + ent + "</BNOTE>";
   mc.send(msg);

   // should get task id if a new task
   // then need to get the actual task

   return null;
}



/********************************************************************************/
/*										*/
/*	Handler for log entries from client					*/
/*										*/
/********************************************************************************/

private class LogServer implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      Element entry = args.getXmlArgument(0);

      Map<String,Object> vals = new HashMap<String,Object>();
      String proj = IvyXml.getAttrString(entry,"PROJECT");
      BnoteEntryType typ = IvyXml.getAttrEnum(entry,"TYPE",BnoteEntryType.NONE);
      int tid = IvyXml.getAttrInt(entry,"TASK");

      for (String s : field_strings) {
	 String v = IvyXml.getAttrString(entry,s);
	 if (v != null) vals.put(s,v);
       }
      for (Element e : IvyXml.children(entry,"DATA")) {
	 String k = IvyXml.getAttrString(e,"KEY");
	 String v = IvyXml.getText(e);
	 vals.put(k,v);
       }

      BnoteTask task = null;
      task = note_db.findTaskById(tid);

      vals.remove("PROJECT");
      vals.remove("TYPE");
      vals.remove("TASK");

      log(proj,task,typ,vals);
    }

}	// end of inner class LogServer



}	// end of class BnoteStore




/* end of BnoteStore.java */

