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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   
   private String resource_name;
   private String service;
   private Course.TACourse course;
   private Map<String, Chat> chats; //maps bare jids to Chat objects 
   private Set<Chat> active_chats;
   
   private TicketList ticket_list;
   
   /**
    *  Logs in with the given username/password at the XMPP service 
    * at service (i.e. "jabber.org")
    * @param aUsername
    * @param an_xmpp_password
    * @param service
    * @param authPassword the password that TAs will use to register with the bot 
    */
   public TAXMPPClient(Course.TACourse a_course)
   {
      chats = new HashMap<String, Chat>();
      active_chats = new HashSet<Chat>();
      course = a_course;
      config = new ConnectionConfiguration(course.getXMPPServer());
      config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
      config.setSendPresence(true); 
      ticket_list = course.getTicketList();
      
      conn = new XMPPConnection(config);
   }
   
   public Course getCourse()
   {
      return course;
   }

      
   /**
    * Connect the bot to the xmpp service
    * @throws XMPPException
    */
   public void connectAndLogin(String name) throws XMPPException
   {
      resource_name = name;
      conn.connect();
      conn.login(course.getTAJID().split("@")[0], course.getXMPPPassword(), resource_name);
      conn.getChatManager().addChatListener(new ChatManagerListener(){
      @Override
      public void chatCreated(Chat c, boolean createdLocally) {
    	  
           //TODO: figure out if excluding chats that are createdLocally is actually useful/correct
           if(!createdLocally)
           {
                  String jid = StringUtils.parseBareAddress(c.getParticipant());
        	  if(chats.get(jid) == null) 
        	  {
        	      System.out.println("Chat created: " + c + " with " + c.getParticipant());
     		      chats.put(jid, conn.getChatManager().createChat(jid, new StudentXMPPBotMessageListener()));
     		    //  c.addMessageListener();
        	  }
           }
         }
      });
      
      conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
      System.out.println(conn.getRoster().getEntries());
      
      //if(!conn.getRoster().contains(getMyBareJID()))
      //{
      //   conn.getRoster().createEntry(getMyBareJID(), "Me", null);
      //}
   
   }
  
   boolean isLoggedIn()
   {
      return conn.isAuthenticated();
   }
   
   Chat getChatForJID(String jid)
   {
      Chat chat = chats.get(StringUtils.parseBareAddress(jid));
      
      if(chat == null)
      {
        chat = conn.getChatManager().createChat(jid, null);
      }
      
      return chat;
   }
   
   public void disconnect() throws XMPPException
   {
      conn.disconnect();
   }
   
   /**
    * End the current chat session with a student
    * and ignore further messages from the student
    * until another ticket is accepted
    */
   public void endChatSession(Chat c)
   {
      active_chats.remove(c);
   }
   
   public void acceptTicketAndAlertPeers(StudentTicket t)
   {
      //should i determine if the ticket is actually in the list?
      
      //we need to alert all the other TAs that we're accepting this ticket
      sendMessageToOtherResources("ACCEPTING:" + t.textHash());
      
      ticket_list.remove(t);
      
      active_chats.add(chats.get(StringUtils.parseBareAddress(t.getStudentJID())));
      
      //now we need to open up a chat window or maybe this should be 
      //done on the outside, better figure that out and then this is called
   }


   public TicketList getTickets()
   {
     return ticket_list;
   }
   XMPPConnection getConnection()
   {
      return conn;
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
             System.out.println("TAClient received message: " + m.getBody() + " from " + c);
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
        else if(active_chats.contains(c))
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
