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
import java.util.Date;



class BnoteDatabase implements BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Connection	note_conn;
private List<TaskImpl>	all_tasks;

private long		id_count;
private long		next_id;
private long		id_request;
private Boolean 	use_begin;
private boolean 	use_streams;

private static Set<String>	ignore_fields;

static {
   ignore_fields = new HashSet<String>();
   ignore_fields.add("PROJECT");
   ignore_fields.add("TYPE");
   ignore_fields.add("USER");
   ignore_fields.add("TIME");
}


// TODO: Add entry to dump the database, add entry to merge a dumped database
//	needing to track what has been added before
//	needing to change IDs



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BnoteDatabase()
{
   id_count = 0;
   next_id = 0;
   id_request = 32;
   use_begin = null;
   all_tasks = new ArrayList<TaskImpl>();
   use_streams = false;

   BnoteConnect bcn = new BnoteConnect();

   note_conn = bcn.getLogDatabase();

   if (note_conn == null) return;

   try {
      Statement st = note_conn.createStatement();
      st.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE");
      st.close();
    }
   catch (SQLException e) { }

   loadTasks();
}




/********************************************************************************/
/*										*/
/*	Add to the database							*/
/*										*/
/********************************************************************************/

BnoteTask addEntry(String proj,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (note_conn == null) return null;

   long eid = getNextId();
   if (eid == 0) return null;

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

   if (task != null) {
      TaskImpl ti = (TaskImpl) task;
      ti.noteUse();
    }

   try {
      PreparedStatement s = note_conn.prepareStatement("INSERT INTO Entry VALUES (?,?,?,?,?,DEFAULT)");
      s.setLong(1,eid);
      s.setString(2,proj);
      if (task != null) s.setLong(3,task.getTaskId());
      else s.setLong(3,0);
      s.setString(4,type.toString());
      s.setString(5,unm);
      s.executeUpdate();
      s.close();

      for (Map.Entry<String,Object> ent : values.entrySet()) {
	 if (ignore_fields.contains(ent.getKey())) continue;
	 s = note_conn.prepareStatement("INSERT INTO Prop VALUES (?,?,?)");
	 s.setLong(1,eid);
	 s.setString(2,ent.getKey());
	 s.setString(3,ent.getValue().toString());
	 s.executeUpdate();
	 s.close();
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem saving notebook entry",e);
    }

   return task;
}




/********************************************************************************/
/*										*/
/*	Attachment methods							*/
/*										*/
/********************************************************************************/

long saveAttachment(String anm,InputStream ins,int len)
{
   long id = getNextId();

   if (id == 0 || len > MAX_ATTACHMENT_SIZE) return 0;

   try {
      PreparedStatement s = note_conn.prepareStatement("INSERT INTO Attachment VALUES (?,?,?)");
      s.setLong(1,id);
      s.setString(2,anm);
      if (!use_streams) {
	 try {
	    if (len > 0) s.setBlob(3,ins,len);
	    else s.setBlob(3,ins);
	  }
	 catch (SQLException e) {
	    use_streams = true;
	  }
       }
      if (use_streams) {
	 if (len > 0) s.setBinaryStream(3,ins,len);
	 else s.setBinaryStream(3,ins);
       }
      s.executeUpdate();
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem saving attachment",e);
    }

   return id;
}



File getAttachment(String aid)
{
   File outf = null;

   long id = 0;

   try {
      id = Long.parseLong(aid);
    }
   catch (NumberFormatException e) {
      return null;
    }

   try {
      String q = "SELECT A.source,A.data FROM Attachment A WHERE A.id = ?";
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setLong(1,id);
      ResultSet rs = s.executeQuery();
      if (!rs.next()) return null;
      String snm = rs.getString(1);
      InputStream ins = null;
      if (!use_streams) {
	 try {
	    Blob data = rs.getBlob(2);
	    ins = data.getBinaryStream();
	  }
	 catch (SQLException e) {
	    use_streams = true;
	  }
       }
      if (use_streams) ins = rs.getBinaryStream(2);
      int idx = snm.lastIndexOf(".");
      String kind = "";
      if (idx > 0) kind = snm.substring(idx);
      outf = File.createTempFile("BnoteBlob",kind);
      outf.deleteOnExit();
      OutputStream fos = new FileOutputStream(outf);
      byte [] buf = new byte[16384];
      for ( ; ; ) {
	 int ln = ins.read(buf);
	 if (ln < 0) break;
	 fos.write(buf,0,ln);
       }
      ins.close();
      fos.close();
      s.close();
    }
   catch (SQLException e) {
      outf = null;
      BoardLog.logE("BNOTE","Problem accessing attachment",e);
    }
   catch (IOException e) {
      outf = null;
      BoardLog.logE("BNOTE","Problem accessing attachment",e);
    }

   return outf;
}



String getAttachmentAsString(String aid)
{
   long id = 0;

   try {
      id = Long.parseLong(aid);
    }
   catch (NumberFormatException e) {
      return null;
    }

   try {
      String q = "SELECT A.data FROM Attachment A WHERE A.id = ?";
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setLong(1,id);
      ResultSet rs = s.executeQuery();
      if (!rs.next()) return null;
      Blob data = rs.getBlob(1);
      byte [] bytes = data.getBytes(0,(int) data.length());
      String rslt = new String(bytes);
      s.close();
      return rslt;
    }
   catch (SQLException e) {
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Task methods								*/
/*										*/
/********************************************************************************/

TaskImpl defineTask(String name,String proj,String desc)
{
   if (note_conn == null) return null;

   TaskImpl ti = null;

   long tid = getNextId();
   if (tid == 0) return null;

   String q = "INSERT INTO Task VALUES (?,?,?,?)";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setLong(1,tid);
      s.setString(2,name);
      s.setString(3,desc);
      s.setString(4,proj);
      s.executeUpdate();
      s.close();
      ti = new TaskImpl(tid,name,proj,desc);
      // if server, need to send new task message
      all_tasks.add(ti);
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
      s.close();
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
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 String unm = rs.getString(1);
	 rslt.add(unm);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting user set",e);
    }

   return rslt;
}



List<Date> getDatesForTask(String proj,BnoteTask task)
{
   List<Date> rslt = new ArrayList<Date>();

   String q = "SELECT DISTINCT E.time FROM Entry E";
   if (proj != null || task != null) {
      q += " WHERE ";
      if (proj != null) q += "E.project = ?";
      if (task != null) {
	 if (proj != null) q += " AND ";
	 q += "E.taskid = ?";
       }
    }
   q += " ORDER BY E.time";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
	 s.setString(1,proj);
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 Date unm = rs.getTimestamp(1);
	 rslt.add(unm);
       }
      s.close();
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
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 String unm = rs.getString(1);
	 rslt.add(unm);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting name set",e);
    }

   return new ArrayList<String>(rslt);
}



List<BnoteEntry> getEntriesForTask(String proj,BnoteTask task)
{
   List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();

   String q = "SELECT * FROM Entry E";
   if (proj != null || task != null) {
      q += " WHERE ";
      if (proj != null) q += "E.project = ?";
      if (task != null) {
	 if (proj != null) q += " AND ";
	 q += "E.taskid = ?";
       }
    }
   q += " ORDER BY time";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
	 s.setString(1,proj);
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 EntryImpl ei = new EntryImpl(rs);
	 rslt.add(ei);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting name set",e);
    }

   return rslt;
}



BnoteTask findTaskById(int id)
{
   if (id <= 0 || note_conn == null) return null;

   for (TaskImpl ti : all_tasks) {
      if (ti.getTaskId() == id) return ti;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Id methods								*/
/*										*/
/********************************************************************************/

private long getNextId()
{
   if (note_conn == null) return 0;

   if (id_count <= 0) {
      if (use_begin == null) {
	 try {
	    Statement st = note_conn.createStatement();
	    st.execute("BEGIN");
	    st.execute("COMMIT");
	    st.close();
	    use_begin = true;
	  }
	 catch (SQLException e) {
	    use_begin = false;
	  }
       }
      try {
	 Statement st = note_conn.createStatement();
	 if (use_begin) {
	    st.executeUpdate("BEGIN");
	  }
	 ResultSet rs = st.executeQuery("SELECT nextid FROM IdNumber");
	 if (rs.next()) next_id = rs.getLong(1);
	 else next_id = 0;
	 id_count = id_request;
	 long next = next_id + id_request;
	 String upd = "UPDATE IdNumber SET nextid = " + next;
	 st.executeUpdate(upd);
	 if (use_begin) {
	    st.executeUpdate("COMMIT");
	  }
	 st.close();
       }
      catch (SQLException e) {
	 BoardLog.logE("BNOTE","Problem getting more ids: ",e);
       }
    }

   if (id_count == 0) return 0;

   --id_count;
   return next_id++;
}



/********************************************************************************/
/*										*/
/*	Task implementation							*/
/*										*/
/********************************************************************************/

private class TaskImpl implements BnoteTask, BnoteValue {

   private long task_id;
   private String task_name;
   private String task_project;
   private String task_description;
   private Date start_date;
   private Date end_date;

   TaskImpl(long tid,String nm,String p,String d) {
      task_id = tid;
      task_name = nm;
      task_project = p;
      task_description = d;
      start_date = null;
      end_date = null;
    }

   @Override public long getTaskId()			{ return task_id; }
   @Override public String getName()			{ return task_name; }
   @Override public String getProject() 		{ return task_project; }
   @Override public String getDescription()		{ return task_description; }

   @Override public String toString()			{ return task_name; }

   @Override public String getDatabaseValue() {
      return Long.toString(task_id);
    }

   @Override public Date getFirstTime() {
      if (start_date == null) loadDates();
      return start_date;
    }

   @Override public Date getLastTime() {
      if (end_date == null) loadDates();
      return end_date;
    }

   void noteUse()					{ end_date = new Date(); }

   private void loadDates() {
      List<Date> dts = getDatesForTask(task_project,this);
      if (dts == null || dts.size() == 0) start_date = end_date = new Date();
      else {
	 start_date = dts.get(0);
	 end_date = dts.get(dts.size()-1);
       }
    }

}	// end of inner class TaskImpl



/********************************************************************************/
/*										*/
/*	Entry implementation							*/
/*										*/
/********************************************************************************/

private class EntryImpl implements BnoteEntry {

   private int	entry_id;
   private String entry_project;
   private BnoteTask entry_task;
   private BnoteEntryType entry_type;
   private String entry_user;
   private Date entry_time;
   private Map<String,String> prop_set;

   EntryImpl(ResultSet rs) throws SQLException {
      entry_id = rs.getInt("id");
      entry_project = rs.getString("project");
      int tid = rs.getInt("taskid");
      entry_task = findTaskById(tid);
      entry_user = rs.getString("username");
      entry_time = rs.getTimestamp("time");
      String typ = rs.getString("type");
      entry_type = BnoteEntryType.NONE;
      if (typ != null) {
	 try {
	    entry_type = Enum.valueOf(BnoteEntryType.class,typ);
	  }
	 catch (IllegalArgumentException e) { }
       }
    }

   @Override public String getProject() 		{ return entry_project; }
   @Override public BnoteTask getTask() 		{ return entry_task; }
   @Override public BnoteEntryType getType()		{ return entry_type; }
   @Override public String getUser()			{ return entry_user; }
   @Override public Date getTime()			{ return entry_time; }

   @Override public String getProperty(String id) {
      loadProperties();
      return prop_set.get(id);
    }

   @Override public Set<String> getPropertyNames() {
      loadProperties();
      return prop_set.keySet();
    }

   private synchronized void loadProperties() {
      if (prop_set != null) return;
      prop_set = new HashMap<String,String>();
      try {
	 String q = "SELECT P.id, P.value FROM Prop P WHERE P.entry = ?";
	 PreparedStatement s = note_conn.prepareStatement(q);
	 s.setInt(1,entry_id);
	 ResultSet rs = s.executeQuery();
	 while (rs.next()) {
	    String k = rs.getString(1);
	    String v = rs.getString(2);
	    prop_set.put(k,v);
	  }
	 s.close();
       }
      catch (SQLException e) {
	 BoardLog.logE("BNOTE","Problem getting properties",e);
       }
    }

}	// end of inner class EntryImpl



}	// end of class BnoteDatabase




/* end of BnoteDatabase.java */


