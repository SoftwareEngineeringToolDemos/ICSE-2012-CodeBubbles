/********************************************************************************/
/*                                                                              */
/*              BnoteDatabase.java                                              */
/*                                                                              */
/*      Database interface for storing programmers notebook                     */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bnote;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.sql.*;
import java.util.*;
import java.io.*;


class BnoteDatabase implements BnoteConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Connection      note_conn;

private static boolean          database_okay;

private static final String DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
// private static final String DB_DRIVER = "org.apache.derby.jdbc.ClientDriver";
private static final String DB_PROTOCOL = "jdbc:derby:";
// private static final String DB_PROTOCOL = "jdbc:derby://localhost:1527/";

private static final String NOTEBOOK_DBNAME = "BNote_Data";


private static Set<String>      ignore_fields;

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
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
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
   
}




/********************************************************************************/
/*                                                                              */
/*      Define the database if necessary                                        */
/*                                                                              */
/********************************************************************************/

private void setupDatabase() throws SQLException
{
   Statement s = note_conn.createStatement();
   s.execute("CREATE TABLE Entry (id int generated always as identity," +
         "project varchar(64),type varchar(16)," +
         "username varchar(64),time timestamp default CURRENT TIMESTAMP)");
   s.execute("CREATE TABLE Prop (entry int,id varchar(32),value long varchar)");
}




/********************************************************************************/
/*                                                                              */
/*      Add to the database                                                     */
/*                                                                              */
/********************************************************************************/

void addEntry(String proj,BnoteEntryType type,Map<String,Object> values)
{
   if (!database_okay) return;
   
   if (proj == null) proj = (String) values.get("PROJECT");
   String unm = values.get("USER").toString();
   if (unm == null) unm = System.getProperty("user.name");
   
   try {
      PreparedStatement s = note_conn.prepareStatement("INSERT INTO Entry VALUES (DEFAULT,?,?,?,DEFAULT)");
      s.setString(1,proj);
      s.setString(2,type.toString());
      s.setString(3,unm);
      s.executeUpdate();
      
      for (Map.Entry<String,Object> ent : values.entrySet()) {
         if (ignore_fields.contains(ent.getKey())) continue;
         s = note_conn.prepareStatement("INSERT INTO Prop VALUES (" +
               "IDENTIFY_VAL_LOCAL(),?,?)");
         s.setString(1,ent.getKey());
         s.setString(2,ent.getValue().toString());
         s.executeUpdate();
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem saving notebook entry",e);
    }
}


void addEntry(Element xml)
{
   if (!database_okay) return;
   
   try {
      PreparedStatement s = note_conn.prepareStatement("INSERT INTO Entry VALUES (DEFAULT,?,?,DEFAULT)");
      s.setString(1,IvyXml.getAttrString(xml,"PROJECT"));
      s.setString(2,IvyXml.getAttrString(xml,"TYPE"));
      s.setString(3,IvyXml.getTextElement(xml,"USER"));
      s.executeUpdate();
      
      for (Element kv : IvyXml.children(xml,"DATA")) {
         s = note_conn.prepareStatement("INSERT INTO Prop VALUES (" +
               "IDENTIFY_VAL_LOCAL(),?,?)");
         String k = IvyXml.getAttrString(kv,"KEY");
         String v = IvyXml.getAttrString(kv,"VALUE");
         if (v == null) v = IvyXml.getText(kv);
         s.setString(1,k);
         s.setString(2,v);
         s.executeUpdate();
       }
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem saving notebook entry",e);
    }
}   




/********************************************************************************/
/*                                                                              */
/*      Standard query methods                                                  */
/*                                                                              */
/********************************************************************************/

List<String> getTasksForProject(String proj)
{
   List<String> rslt = new ArrayList<String>();
   
   if (note_conn == null) return rslt;
   
   String q = "SELECT P.value, E.time " +
      "FROM Entry E, Prop P " +
      "WHERE E.id = P.entry AND E.type = 'TASK' AND " +
      "E.project = ? AND P.id = 'TASK' " +
      "ORDER BY E.time DESC";
   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setString(1,proj);
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
         String v = rs.getString(1);
         if (v != null) rslt.add(v);
       }
      rs.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem accessing project tasks",e);
    }
   
   return rslt;
}







}       // end of class BnoteDatabase




/* end of BnoteDatabase.java */

