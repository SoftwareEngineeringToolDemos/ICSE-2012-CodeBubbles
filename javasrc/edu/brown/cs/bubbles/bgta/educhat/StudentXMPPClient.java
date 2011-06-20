package edu.brown.cs.bubbles.bgta.educhat;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
public class StudentXMPPClient {
   XMPPConnection conn;
   
   public StudentXMPPClient(XMPPConnection a_connection) throws XMPPException
   { 
      conn = a_connection;
      if(!conn.isConnected() || !conn.isAuthenticated())
      {
         throw new XMPPException("Tried to initiate Student chat XMPPConnection that was not logged in");
       }
   } 
}
