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
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class TAXMPPClient {
   private ConnectionConfiguration config;
   private XMPPConnection conn;
   
   private String username;
   private String resource_name; //this will be used to identify the TA uniquely
   private String xmpp_password;
   private String cur_student_jid;
   private String service;
   
   //using a LinkedHashMap so we can keep the tickets in order 
   //private LinkedHashMap<Integer, StudentTicket> ticket_map;
   private TicketList ticket_list;
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
      ticket_list = new TicketList();
      //ticket_map = new LinkedHashMap<Integer, StudentTicket>();
   
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
      //if(!conn.getRoster().contains(getMyBareJID()))
      //{
      //   conn.getRoster().createEntry(getMyBareJID(), "Me", null);
      //}
      System.out.println(conn.getRoster().getEntries());

   }
   
   public void disconnect() throws XMPPException
   {
      conn.disconnect();
   }
   
   public void acceptTicketAndAlertPeers(StudentTicket t)
   {
      //should i determine if the ticket is actually in the list?
      
      //we need to alert all the other TAs that we're accepting this ticket
      sendMessageToOtherResources("ACCEPTING:" + t.textHash());
      
      ticket_list.remove(t);
      
      cur_student_jid = t.getStudentJID();
      
      //now we need to open up a chat window or maybe this should be 
      //done on the outside, better figure that out and then this is called
   }


   public TicketList getTickets()
   {
     return ticket_list;
   }
   
   private void sendMessageToOtherResources(String msg)
   {
      for(String full_jid : BgtaUtil.getFullJIDsForRosterEntry(conn.getRoster(), getMyBareJID()))
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
   
   private String getMyBareJID()
   {
      return StringUtils.parseBareAddress(conn.getConnectionID());
   }
   
   private class StudentXMPPBotMessageListener implements MessageListener {
      @Override
      public void processMessage(Chat c, Message m) {
             System.out.println("TAClient received message: " + m.getBody());
        String[] chat_args = m.getBody().split(":");
   
        String cmd = chat_args[0];
        if(cmd.equals("TICKET"))
        {
           //comes in the form "TICKET:<message>"
           StudentTicket t = new StudentTicket(chat_args[1], new Date(System.currentTimeMillis()), m.getFrom());
           ticket_list.add(t);
           sendMessageToOtherResources("TICKET-FORWARD:" + m.getFrom() + ":" + chat_args[1]);
        }
        else if(StringUtils.parseBareAddress(c.getParticipant()).equals(getMyBareJID()) && cmd.equals("TICKET-FORWARD"))
        {
           //comes in the form "TICKET-FORWARD:<student-jid>:<message>"
           StudentTicket t = new StudentTicket(chat_args[2], new Date(System.currentTimeMillis()), chat_args[1]);
           ticket_list.add(t);
        }
        else if(StringUtils.parseBareAddress(c.getParticipant()).equals(getMyBareJID()) && cmd.equals("ACCEPTING"))
        {
           //form: "ACCEPTING:<string hash>"
           int hash = Integer.valueOf(chat_args[1]);
           for(StudentTicket t : ticket_list)
           {
              if(t.hashCode() == hash)
              {
        	 ticket_list.remove(t);
              }
           }
        }
        else if(StringUtils.parseBareAddress(c.getParticipant()).equals(StringUtils.parseBareAddress(cur_student_jid)))
        {
           //let the message go through to the UI
           System.out.println("Student message: " + c.getParticipant() + " : " + m.getBody());
        }
        else
        {
           try{
              c.sendMessage("Please submit a ticket to chat with a TA");
           } catch(XMPPException e)
           {
              //this exception doesn't really matter because this 
              //person shouldn't be chatting with us anyway
           }
        }
      }
   }
}
