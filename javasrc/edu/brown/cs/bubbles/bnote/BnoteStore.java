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

import java.util.*;
import java.io.*;


public class BnoteStore implements BnoteConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BnoteDatabase	note_db;

private static BnoteStore	the_store = null;

private static Set<String>	field_strings;


static {
   field_strings = new HashSet<String>();
   field_strings.add("PROJECT");
   field_strings.add("TYPE");
   field_strings.add("USER");
   field_strings.add("TIME");
   field_strings.add("TASK");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BnoteStore()
{
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
   // handle non-local access

   return note_db.getTasksForProject(proj);
}


List<String> getUsersForTask(String proj,BnoteTask task)
{
   // handle non-local access

   return note_db.getUsersForTask(proj,task);
}


List<Date> getDatesForTask(String proj,BnoteTask task)
{
   // handle non-local access

   return note_db.getDatesForTask(proj,task);
}


List<String> getNamesForTask(String proj,BnoteTask task)
{
   // handle non-local access

   return note_db.getNamesForTask(proj,task);
}



List<BnoteEntry> getEntriesForTask(String proj,BnoteTask task)
{
   // handle non-local access

   return note_db.getEntriesForTask(proj,task);
}



File getAttachment(String aid)
{
   // handle non-local access

   return note_db.getAttachment(aid);
}


String getAttachmentAsString(String aid)
{
   // handle non-local access
   
   return note_db.getAttachmentAsString(aid);
}


BnoteTask findTaskById(int id)
{
   // handle non-local access

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
   if (the_store == null) return null;

   Map<String,Object> values = new HashMap<String,Object>();
   for (int i = 0; i < args.length-1; i += 2) {
      values.put(args[i].toString(),args[i+1]);
    }

   return log(project,task,type,values);
}



public static boolean attach(BnoteTask task,File file)
{
   if (the_store == null || task == null || file == null) return false;

   long aid = the_store.saveAttachment(file);
   if (aid == 0) return false;

   log(task.getProject(),task,BnoteEntryType.ATTACHMENT,"SOURCE",file,"ATTACHID",aid);

   return true;
}


/********************************************************************************/
/*										*/
/*	Internal logging entries						*/
/*										*/
/********************************************************************************/

private BnoteTask enter(String project,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   return note_db.addEntry(project,task,type,values);

   /*********************
   // this code will be useful for export
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
 * ***************/
}



/********************************************************************************/
/*										*/
/*	Attachment methods							*/
/*										*/
/********************************************************************************/

private long saveAttachment(File f)
{
   long len = f.length();
   if (len == 0 || len > MAX_ATTACHMENT_SIZE) return 0;

   try {
      InputStream ins = new FileInputStream(f);
      return note_db.saveAttachment(f.getPath(),ins,(int) len);
    }
   catch (IOException e) {
      BoardLog.logD("BNOTE","Problem reading attachment: " + e);
    }
   return 0;
}




}	// end of class BnoteStore




/* end of BnoteStore.java */

