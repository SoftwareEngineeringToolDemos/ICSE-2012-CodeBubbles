package edu.brown.cs.bubbles.bedu.chat;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPConnection;

import edu.brown.cs.bubbles.bgta.BgtaChat;

public class BeduProxyChat extends BgtaChat
{
   static ArrayList<BeduProxyChat> instances;
   
   static {
      instances = new ArrayList<BeduProxyChat>();
   }
   private XMPPConnection my_conn;
   private String recip;
   private Chat chat;
   private String newest_msg;
   
   public BeduProxyChat(XMPPConnection conn, Chat c)
   {
      super(conn.getUser(), c.getParticipant(), null, ChatServer.fromServer(conn.getServiceName()), c, null);
      newest_msg = "";
      my_conn = conn;
      chat = c;
      instances.add(this);
   }
   public Chat getChat()
   {
      return chat;
   }
   
   void logOutsideMessage(String msg)
   {
      if(!msg.equals(newest_msg))
         logMessage(msg);
   }
}
