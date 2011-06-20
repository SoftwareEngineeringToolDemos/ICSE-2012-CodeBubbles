/********************************************************************************/
/*                            */
/*    StudentXMPPBot.java                 */
/*                            */
/* Bubbles chat for students                 */
/* This class implements an XMPP chat bot that is used to         */
/*    facilitate help requests and chatting between students         */
/*    and TAs in a course                    */
/********************************************************************************/
/* Copyright 2011 Brown University -- Andrew Kovacs         */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

package edu.brown.cs.bubbles.bgta.educhat;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import edu.brown.cs.bubbles.bgta.BgtaUtil;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class TAXMPPClient {
   private ConnectionConfiguration config;
   private XMPPConnection conn;
   
   private String username;
   private String resource_name; //this will be used to identify the TA uniquely
   private String xmpp_password;
   private String cur_student;
   private String service;
   
   //using a LinkedHashMap so we can keep the tickets in order 
   private LinkedHashMap<Integer, StudentTicket> ticket_map;
   
   /**
    *  Logs in with the given username/password at the XMPP service 
    * at service (i.e. "jabber.org")
    * @param aUsername
    * @param an_xmpp_password
    * @param service
    * @param authPassword the password that TAs will use to register with the bot 
    */
   public TAXMPPClient(String aUsername, String an_xmpp_password, String a_service, String a_resource_name)
   {
      config = new ConnectionConfiguration("jabber.org");
      config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
      config.setSendPresence(true); 
      
      ticket_map = new LinkedHashMap<Integer, StudentTicket>();

      username = aUsername;
      xmpp_password = an_xmpp_password;
      resource_name = a_resource_name;
      service = a_service;
   }
   
   /**
    * Connect the bot to the xmpp service
    * @throws XMPPException
    */
   public void connect() throws XMPPException
   {
      conn = new XMPPConnection(config);
      conn.connect();
      conn.login(username, xmpp_password, resource_name);
      conn.getChatManager().addChatListener(new ChatManagerListener(){
      @Override
      public void chatCreated(Chat c, boolean createdLocally) {
           //TODO: figure out if excluding chats that are createdLocally is actually useful/correct
           if(!createdLocally)
              c.addMessageListener(new StudentXMPPBotMessageListener());
           }
      });
      
      conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
      System.out.println(conn.getRoster().getEntries());
      if(!conn.getRoster().contains(username + "@" + service))
      {
         conn.getRoster().createEntry(username + "@" + service, "Me", null);
      }
      System.out.println(conn.getRoster().getEntries());

   }
   
   public void disconnect() throws XMPPException
   {
      conn.disconnect();
   }
   
   public void removeTicketAndAlertPeers(StudentTicket t)
   {
      //should i determine if the ticket is actually in the list?
      
      //we need to alert all the other TAs that we're accepting this ticket
      sendMessageToOtherResources("ACCEPTING:" + t.textHash());
      
      ticket_map.remove(t.textHash());
      
      //now we need to open up a chat window or maybe this should be 
      //done on the outside, better figure that out and then this is called
   }


   public List<StudentTicket> getTickets()
   {
      ArrayList<StudentTicket> l = new ArrayList<StudentTicket>();
      for(Integer i : ticket_map.keySet())
      {
	l.add(ticket_map.get(i)); 
      }
      
      return l;
   }
   
   private void sendMessageToOtherResources(String msg)
   {
      for(String full_jid : BgtaUtil.getFullJIDsForRosterEntry(conn.getRoster(), username+"@"+service))
      {
         Chat other_ta_chat = conn.getChatManager().createChat(full_jid, new MessageListener() {
            @Override
            public void processMessage(Chat c, Message m)
            {
               //do nothing
            }
           });

         try {
	       other_ta_chat.sendMessage(msg);
	    } catch (XMPPException e) {
	       // TODO Auto-generated catch block
	       e.printStackTrace();
	    }
      }
   }
   
   private class StudentXMPPBotMessageListener implements MessageListener {
      @Override
      public void processMessage(Chat c, Message m) {
	     System.out.println("TAClient received message: " + m.getBody());
        String[] args = m.getBody().split(":");

        String cmd = args[0];
        if(cmd.equals("TICKET"))
        {
           //comes in the form "TICKET:<message>"
           StudentTicket t = new StudentTicket(args[1], new Date(System.currentTimeMillis()), m.getFrom());
           ticket_map.put(args[1].hashCode(), t);
           sendMessageToOtherResources("TICKET-FORWARD:" + m.getFrom() + ":" + args[1]);
        }
        else if(cmd.equals("TICKET-FORWARD"))
        {
           //comes in the form "TICKET-FORWARD:<student-jid>:<message>"
           StudentTicket t = new StudentTicket(args[2], new Date(System.currentTimeMillis()), args[1]);
           ticket_map.put(args[2].hashCode(), t);
        }
        else if(cmd.equals("ACCEPTING"))
        {
           //form: "ACCEPTING:<string hash>"
           int id = Integer.valueOf(args[1]);
           ticket_map.remove(id);
        }
      }
   }
}
