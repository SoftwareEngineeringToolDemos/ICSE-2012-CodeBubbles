/********************************************************************************/
/*										                                                  */
/*		BeduTAXMPPClient.java			   													  */
/*    Bubbles for Education                                                     */
/*                                                                              */
/* This class implements an XMPP chat bot that is used to       					  */
/*    facilitate help requests and chatting between students         			  */
/*    and TAs in a course                    											  */
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs									  */
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

package edu.brown.cs.bubbles.bedu.chat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.bubbles.bgta.BgtaUtil;
import edu.brown.cs.bubbles.bgta.BgtaChat;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

public class BeduTAXMPPClient {
private ConnectionConfiguration config;
private XMPPConnection			  conn;

private String						  resource_name;
private BeduCourse.TACourse	  course;
private Map<String, BeduTAChat> chats;		  // maps bare jids to Chat objects
private Map<String, String>     ta_sessions; //maps student jids to ta jids for active sessions this instance knows about 
private Set<String>				  permitted_jids;

private BeduTATicketList		  ticket_list;



BeduTAXMPPClient(BeduCourse.TACourse a_course) {
   if(BeduChatFactory.DEBUG)
      XMPPConnection.DEBUG_ENABLED = true;
	chats = new HashMap<String, BeduTAChat>();
	permitted_jids = new HashSet<String>();
	ta_sessions = new HashMap<String, String>();
	course = a_course;
	config = new ConnectionConfiguration(course.getXMPPServer());
	config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
	config.setSendPresence(true);
	ticket_list = course.getTicketList();

	conn = new XMPPConnection(config);
}



BeduCourse getCourse() {
	return course;
}



/**
 * Connect the bot to the xmpp service
 * with the given name as the resource name 
 * @throws XMPPException
 */
void connectAndLogin(String name) throws XMPPException {
	resource_name = name;
	conn.connect();
	conn.login(course.getTAJID().split("@")[0], course.getXMPPPassword(), resource_name);

	
	conn.getChatManager().addChatListener(new ChatManagerListener() {
		@Override public void chatCreated(Chat c, boolean createdLocally) {
		   if(BeduChatFactory.DEBUG)
		      System.out.println("Chat created with: " + c.getParticipant());
			c.addMessageListener(new StudentXMPPBotMessageListener());
		}
	});


	conn.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
	try {
      Thread.sleep(1000);
   } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
   }
   ArrayList<String> my_full_jids = new ArrayList<String>();
   
   int minPriority = 128;
   for(Iterator<Presence> it = conn.getRoster().getPresences(getMyBareJID()); it.hasNext();)
   {
      Presence p = it.next();
      my_full_jids.add(p.getFrom());
      if(p.getPriority() < minPriority && p.getPriority() >= -128)
      {
         minPriority = p.getPriority();
      }
   }

   Presence avail_p = new Presence(Presence.Type.available, "Answering questions", minPriority - 1, Presence.Mode.available);
   conn.sendPacket(avail_p);
   
	//if(!conn.getRoster().contains(getMyBareJID()))
	//{
	// conn.getRoster().createEntry(getMyBareJID(), "Me", null);
	//}
	//BgtaUtil.getFullJIDsForRosterEntry(conn.getRoster(), getMyBareJID());

   boolean foundFull = false;
   for(String full_jid : my_full_jids)
   {
     // System.out.println(conn.getRoster().getPresence(full_jid).getPriority());
      if(!StringUtils.parseResource(full_jid).equals(name))
      {
         conn.getChatManager().createChat(full_jid, new StudentXMPPBotMessageListener()).sendMessage("REQUEST-TICKETS");
         foundFull = true;
         break;
      }
   }
	 
	if(!foundFull)
	{
	   conn.getChatManager().createChat(getMyBareJID(), new StudentXMPPBotMessageListener()).sendMessage("REQUEST-TICKETS");
	}
	
}



boolean isLoggedIn() {
	return conn.isAuthenticated();
}



Chat getChatForJID(String jid) {
	return null;
}



void disconnect() throws XMPPException {
	conn.disconnect();
}



/**
 * End the current chat session with a student and ignore further messages from
 * the student until another ticket is accepted
 */
void endChatSession(BgtaChat c) {
	permitted_jids.remove(c.getUsername());
	sendMessageToOtherResources("COMPLETED:" + c.getUsername());
}



BgtaChat acceptTicketAndAlertPeers(BeduStudentTicket t) {
	//TODO: should i determine if the ticket is actually in the list?
	sendMessageToOtherResources("ACCEPTING:" + t.textHash());
	ticket_list.remove(t);
        BeduTAChat c = new BeduTAChat(conn, conn.getChatManager().createChat(t.getStudentJID(), null));
        chats.put(t.getStudentJID(), c);
	permitted_jids.add(t.getStudentJID());
        return c;
}



public BeduTATicketList getTickets() {
	return ticket_list;
}


XMPPConnection getConnection() {
	return conn;
}



private void sendMessageToOtherResources(String msg) {
	for (String full_jid : BgtaUtil.getFullJIDsForRosterEntry(conn.getRoster(), getMyBareJID())) {
	   if(StringUtils.parseResource(full_jid).equals(resource_name))
	      continue;
		Chat other_ta_chat = conn.getChatManager().createChat(full_jid,
				new MessageListener() {
					@Override public void processMessage(Chat c, Message m) {
						// do nothing
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



private String getMyBareJID() {
	return StringUtils.parseBareAddress(conn.getUser());
}

private class StudentXMPPBotMessageListener implements MessageListener {
@Override public void processMessage(Chat c, Message m) {
   if(BeduChatFactory.DEBUG)
      System.out.println(conn.getUser() + "  received message: " + m.getBody() + " from " + c.getParticipant());
   
	String[] chat_args = m.getBody().split(":");
	String cmd = chat_args[0];
	
	if (cmd.equals("TICKET")) {
		// comes in the form "TICKET:<message>"
		BeduStudentTicket t = new BeduStudentTicket(chat_args[1], new Date(
				System.currentTimeMillis()), m.getFrom());
		ticket_list.add(t);
		try {
         c.sendMessage("Ticket received. A TA will respond soon.");
      } catch (XMPPException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
		sendMessageToOtherResources("TICKET-FORWARD:" + m.getFrom() + ":"
				+ chat_args[1]);
	} 
	
	else if (StringUtils.parseBareAddress(c.getParticipant()).equals(
			getMyBareJID())
			&& cmd.equals("TICKET-FORWARD")) {
		// comes in the form "TICKET-FORWARD:<student-jid>:<message>"
		BeduStudentTicket t = new BeduStudentTicket(chat_args[2], new Date(
				System.currentTimeMillis()), chat_args[1]);
		if(!ticket_list.contains(t))
			ticket_list.add(t);
	}
	
	else if (StringUtils.parseBareAddress(c.getParticipant()).equals(
			getMyBareJID())
			&& cmd.equals("ACCEPTING")) {
		// form: "ACCEPTING:<string hash>"

      int hash = Integer.valueOf(chat_args[1]);
      for (BeduStudentTicket t : ticket_list) {
              if (t.textHash() == hash) {
            ta_sessions.put(t.getStudentJID(), c.getParticipant());
                      ticket_list.remove(t);
            break;
              }
      }	
	}
	
	else if (StringUtils.parseBareAddress(c.getParticipant()).equals(
         getMyBareJID())
         && cmd.equals("MSG-FORWARD")) {
      // form: "ACCEPTING:<string hash>"

      String jid = chat_args[1];
      String msg = chat_args[2];
      
      chats.get(jid).logOutsideMessage(msg);
   }
	
	else if (StringUtils.parseBareAddress(c.getParticipant()).equals(
         getMyBareJID())
         && cmd.equals("COMPLETED")) {
      // form: "COMPLETED:<student this ta is done talking to >"

      String jid = chat_args[1];

      ta_sessions.remove(jid);
   }
	
	 else if (cmd.equals("REQUEST-TICKETS")) {
      for (BeduStudentTicket t : ticket_list) {
         try {
            c.sendMessage("TICKET-FORWARD:" + t.getStudentJID() + ":"
                  + t.getText());
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         } catch (XMPPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
   }
	else if (permitted_jids.contains(c.getParticipant())) {
	   chats.get(c.getParticipant()).logOutsideMessage(m.getBody());
	   if(BeduChatFactory.DEBUG)
	      System.err.println("BEDU: Student message: " + c.getParticipant() + " : "+ m.getBody());
	}
	else if(ta_sessions.keySet().contains(c.getParticipant()))
	{
	   try {
         conn.getChatManager().createChat(ta_sessions.get(c.getParticipant()), null)
         .sendMessage("MSG-FORWARD:" + c.getParticipant() + ":" + m.getBody());
      } catch (XMPPException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
	}
	else {
		try {
			c.sendMessage("Please submit a ticket to chat with a TA");
		} catch (XMPPException e) {
			// this exception doesn't really matter because this
			// person shouldn't be chatting with us anyway
		}	
	}
}
}

private class BeduTAChat extends BgtaChat
{
   private String newest_msg;
   
   public BeduTAChat(XMPPConnection conn, Chat c)
   {
      super(conn.getUser(), c.getParticipant(), null, ChatServer.fromServer(conn.getServiceName()), c, null);
      newest_msg = "";
   }
   
   void logOutsideMessage(String msg)
   {
      if(!msg.equals(newest_msg))
         logMessage(msg);
   }
}

}
