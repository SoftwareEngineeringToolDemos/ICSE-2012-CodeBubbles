/********************************************************************************/
/*										*/
/*		BnoteDatabase.java						*/
/*										*/
/*	Database interface for storing programmers notebook			*/
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

import java.sql.*;
import java.util.*;
import java.io.*;


class BnoteDatabase implements BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Connection	note_conn;
private List<TaskImpl>	all_tasks;

private static boolean		database_okay;

private static final String DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
// private static final String DB_DRIVER = "org.apache.derby.jdbc.ClientDriver";
private static final String DB_PROTOCOL = "jdbc:derby:";
// private static final String DB_PROTOCOL = "jdbc:derby://localhost:1527/";

private static final String NOTEBOOK_DBNAME = "BNote_Data";


private static Set<String>	ignore_fields;

static {
   ignore_fields = new HashSet<String>();
   ignore_fields.add("PROJECT");
   ignore_fields.add("TYPE");
   ignore_fields.add("USER");
   ignore_fields.add("TIME");
}


static {
   database_okay = false;
   try {
      Class.forName(DB_DRIVER).newInstance();
      BoardLog.logD("BNOTE","Derby database driver loaded");
      database_okay = true;
    }
   catch (Throwable t) {
      BoardLog.logD("BNOTE","Can't load derby database driver",t);
    }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BnoteDatabase()
{
   note_conn = null;
   Properties props = new Properties();
   boolean create = false;

   File pfx = BoardSetup.getBubblesPluginDirectory();
   File dbf = new File(pfx,NOTEBOOK_DBNAME);
   if (!dbf.exists()) {
      props.put("create","true");
      create = true;
    }

   String dbn = dbf.getPath();

   if (!database_okay) return;

   try {
      note_conn = DriverManager.getConnection(DB_PROTOCOL + dbn,props);
      if (create) setupDatabase();
    }
   catch (SQLException e) {
      database_okay = false;
      BoardLog.logE("BNOTE","Unable to connect to database",e);
      return;
    }

   loadTasks();
}




/********************************************************************************/
/*										*/
/*	Define the database if necessary					*/
/*										*/
/********************************************************************************/

private void setupDatabase() throws SQLException
{
   Statement s = note_conn.createStatement();
   s.execute("CREATE TABLE Entry (id int generated always as identity," +
		"project varchar(64),taskid int,type varchar(16)," +
		"username varchar(64),time timestamp default CURRENT TIMESTAMP)");
   s.execute("CREATE TABLE Prop (entry int,id varchar(32),value long varchar)");
   s.execute("CREATE TABLE Task (id int generated always as identity," +
		"name long varchar,description long varchar," +
		"project varchar(64))");
}




/********************************************************************************/
/*										*/
/*	Add to the database							*/
/*										*/
/********************************************************************************/

BnoteTask addEntry(String proj,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (!database_okay) return null;

   if (proj == null) proj = (String) values.get("PROJECT");
   String unm = null;
   if (values.get("USER") != null) unm = values.get("USER").toString();
   if (unm == null) unm = System.getProperty("user.name");

   switch (type) {
      case NONE :
	 return null;
      case NEW_TASK :
	 String nm = (String) values.remove("NAME");
	 String ds = (String) values.remove("DESCRIPTION");
	 task = defineTask(nm,proj,ds);
	 break;
    }

   try {
      PreparedStatement s = note_conn.prepareStatement("INSERT INTO Entry VALUES (DEFAULT,?,?,?,?,DEFAULT)");
      s.setString(1,proj);
      s.setString(2,getValue(task));
      s.setString(3,type.toString());
      s.setString(4,unm);
      s.executeUpdate();

      for (Map.Entry<String,Object> ent : values.entrySet()) {
	 if (ignore_fields.contains(ent.getKey())) continue;
	 s = note_conn.prepareStatement("INSERT INTO Prop VALUES (" +
	       "IDENTITY_VAL_LOCAL(),?,?)");
	 s.setString(1,ent.getKey());
	 s.setString(2,ent.getValue().toString());
	 s.executeUpdate();
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem saving notebook entry",e);
    }

   return task;
}




private String getValue(Object o)
{
   if (o == null) return null;

   if (o instanceof BnoteValue) {
      BnoteValue bv = (BnoteValue) o;
      return bv.getDatabaseValue();
    }

   return o.toString();
}



/********************************************************************************/
/*										*/
/*	Task methods								*/
/*										*/
/********************************************************************************/

TaskImpl defineTask(String name,String proj,String desc)
{
   if (!database_okay) return null;

   TaskImpl ti = null;

   String q = "INSERT INTO Task VALUES ( DEFAULT,?,?,?)";
   String q1 = "VALUES IDENTITY_VAL_LOCAL()";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setString(1,name);
      s.setString(2,desc);
      s.setString(3,proj);
      s.executeUpdate();
      s = note_conn.prepareStatement(q1);
      ResultSet rs = s.executeQuery();
      int id = -1;
      while (rs.next()) {
	 id = rs.getInt(1);
	 break;
       }
      if (id > 0) {
	 ti = new TaskImpl(id,name,proj,desc);
	 // if server, need to send new task message
	 all_tasks.add(ti);
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem defining new task",e);
    }

   return ti;
}



private void loadTasks()
{
   all_tasks = new ArrayList<TaskImpl>();

   String q = "SELECT T.id,T.name,T.project,T.description FROM Task T";
   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 int id = rs.getInt(1);
	 String nm = rs.getString(2);
	 String pr = rs.getString(3);
	 String d = rs.getString(4);
	 TaskImpl ti = new TaskImpl(id,nm,pr,d);
	 // if server, need to send new task message
	 all_tasks.add(ti);
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem loading tasks",e);
    }
}




List<BnoteTask> getTasksForProject(String proj)
{
   List<BnoteTask> rslt = new ArrayList<BnoteTask>();

   for (TaskImpl ti : all_tasks) {
      if (proj == null || proj.equals(ti.getProject())) {
	 rslt.add(ti);
       }
    }

   return rslt;
}



List<String> getUsersForTask(String proj,BnoteTask task)
{
   List<String> rslt = new ArrayList<String>();
   
   String q = "SELECT DISTINCT E.username FROM Entry E";
   if (proj != null || task != null) {
      q += " WHERE ";
      if (proj != null) q += "E.project = ?";
      if (task != null) {
         if (proj != null) q += " AND ";
         q += "E.taskid = ?";
       }
    }
   q += " ORDER BY E.username";
   
   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
         s.setString(1,proj);
         if (task != null) s.setInt(2,task.getTaskId());
       }
      else if (task != null) {
         s.setInt(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
         String unm = rs.getString(1);
         rslt.add(unm);
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting user set",e);
    }
   
   return rslt;
}



List<String> getNamesForTask(String proj,BnoteTask task)
{
   Set<String> rslt = new TreeSet<String>();
   
   String q = "SELECT P.value FROM Prop P";
   if (proj != null || task != null) q += ", Entry E";
   q += " WHERE P.id = 'NAME'";
   if (proj != null || task != null) {
      q += " AND E.id = P.entry";
      if (proj != null) q += " AND E.project = ?";
      if (task != null) {
         q += " AND E.taskid = ?";
       }
    }
   
   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
         s.setString(1,proj);
         if (task != null) s.setInt(2,task.getTaskId());
       }
      else if (task != null) {
         s.setInt(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
         String unm = rs.getString(1);
         rslt.add(unm);
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting name set",e);
    }
   
   return new ArrayList<String>(rslt);
}



BnoteTask findTaskById(int id)
{
   if (id <= 0) return null;

   for (TaskImpl ti : all_tasks) {
      if (ti.getTaskId() == id) return ti;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Task implementation							*/
/*										*/
/********************************************************************************/

private class TaskImpl implements BnoteTask, BnoteValue {

   private int task_id;
   private String task_name;
   private String task_project;
   private String task_description;

   TaskImpl(int tid,String nm,String p,String d) {
      task_id = tid;
      task_name = nm;
      task_project = p;
      task_description = d;
    }

   @Override public int getTaskId()			{ return task_id; }
   @Override public String getName()			{ return task_name; }
   @Override public String getProject() 		{ return task_project; }
   @Override public String getDescription()		{ return task_description; }

   @Override public String toString()			{ return task_name; }

   @Override public String getDatabaseValue() {
      return Integer.toString(task_id);
    }

}	// end of inner class TaskImpl




}	// end of class BnoteDatabase




/* end of BnoteDatabase.java */

