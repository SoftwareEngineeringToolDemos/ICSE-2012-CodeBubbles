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

import java.util.*;
import java.io.*;


public class BnoteStore implements BnoteConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	is_local;
private BnoteDatabase	note_db;
private PrintWriter	output_file;

private static BnoteStore	the_store = null;

private static Set<String>	field_strings;
private static Set<String>	cdata_strings;

static {
   field_strings = new HashSet<String>();
   field_strings.add("PROJECT");
   field_strings.add("TYPE");
   field_strings.add("USER");
   field_strings.add("TIME");

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
	 break;
      case SERVER :
	 is_local = true;
	 MintControl mc = bs.getMintControl();
	 mc.register("<BNOTE TYPE='LOG'><_VAR_0></BNOTE>",
	       new LogServer());
	 break;
      default :
	 is_local = true;
	 break;
    }

   output_file = null;
   if (is_local) {
      File dir = BoardSetup.getBubblesPluginDirectory();
      File logf = new File(dir,BNOTE_LOG_FILE_NAME);
      try {
	 FileWriter fw = new FileWriter(logf);
	 output_file = new PrintWriter(fw);
       }
      catch (IOException e) {
	 BoardLog.logE("BNOTE","Problem setting up log file",e);
       }
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

List<String> getTasksForProject(String proj)
{
   if (proj == null) return null;

   return note_db.getTasksForProject(proj);
}




/********************************************************************************/
/*										*/
/*	Static logging entries							*/
/*										*/
/********************************************************************************/

public static void log(String project,BnoteEntryType type,Map<String,Object> values)
{
   if (the_store == null) return;

   the_store.enter(project,type,values);
}



public static void log(String project,BnoteEntryType type,Object ... args)
{
   Map<String,Object> values = new HashMap<String,Object>();
   for (int i = 0; i < args.length-1; i += 2) {
      values.put(args[i].toString(),args[i+1]);
    }
   log(project,type,values);
}



/********************************************************************************/
/*										*/
/*	Internal logging entries						*/
/*										*/
/********************************************************************************/

private void enter(String project,BnoteEntryType type,Map<String,Object> values)
{
   IvyXmlWriter xw = new IvyXmlWriter();

   if (project != null) values.put("PROJECT",project);
   if (type != null) values.put("TYPE",type);
   if (!values.containsKey("USER")) values.put("USER",System.getProperty("user.name"));
   values.put("TIME",System.currentTimeMillis());

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

   saveEntry(xw.toString());
}



private void saveEntry(String ent)
{
   if (!is_local) {
      MintControl mc = BoardSetup.getSetup().getMintControl();
      String msg = "<BNOTE TYPE='LOG'>" + ent + "</BNOTE>";
      mc.send(msg);
      return;
    }

   if (output_file == null) return;

   synchronized (this) {
      output_file.println(ent);
      output_file.flush();
    }
}



/********************************************************************************/
/*										*/
/*	Handler for log entries from client					*/
/*										*/
/********************************************************************************/

private class LogServer implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String entry = args.getArgument(0);
      saveEntry(entry);
    }

}	// end of inner class LogServer



}	// end of class BnoteStore




/* end of BnoteStore.java */

