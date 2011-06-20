/********************************************************************************/
/*                            */
/*    BgtaChat.java                 */
/*                            */
/* Bubbles chat management      */
/*                            */
/********************************************************************************/
/* Copyright 2011 Brown University -- Sumner Warren            */
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


package edu.brown.cs.bubbles.bgta;

import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import net.kano.joustsim.oscar.oscar.service.icbm.*;

public class BgtaChat implements BgtaConstants {



/********************************************************************************/
/*                            */
/* Private storage                           */
/*                            */
/********************************************************************************/

// used for all chats
private String user_name;
private String user_display;
private ChatServer user_server;
private Document user_document;

// used for XMPP chats
private Chat the_chat;
private MessageListener chat_listener;

// used for AIM (OSCAR) conversations
private Conversation the_conversation;
private ConversationListener conversation_listener;

// wrapper members
private boolean is_xmpp;

/********************************************************************************/
/*                            */
/* Constructors                           */
/*                            */
/********************************************************************************/

BgtaChat(String username,String displayName,ChatServer server,Object chat,Document doc)
{
   user_name = username;
   user_display = displayName;
   if (user_display == null) {
      user_display = getName(user_name);
   }
   // Fix display name to not have the server ending.
   
   
   user_server = server;
   the_chat = null;
   the_conversation = null;
   is_xmpp = false;
   
   // Determine the protocol and set up members appropriately.
   if (!server.equals(ChatServer.AIM)) {
      is_xmpp = true;
      the_chat = (Chat) chat;
      the_chat.addMessageListener(new XMPPChatListener());
    }
   else {
      the_conversation = (Conversation) chat;
      //TODO: fix AIM listener
//      conversation_listener = (ConversationListener) listener;
      the_conversation.addConversationListener(new AIMChatListener());
   }
   
   // Create a new Document for this user if one doesn't exist.
   // Otherwise, use the old one.
   user_document = doc;
   if (user_document == null) {
      user_document = new HTMLDocument();
    }   
}



/********************************************************************************/
/*                            */
/* Access methods                           */
/*                            */
/********************************************************************************/

String getUsername()    { return user_name; }

ChatServer getServer()      { return user_server; }

Document getDocument()  { return user_document; }

boolean isXMPP()        { return is_xmpp; }

/**
 * Preferred method of setting the document is by passing
 * it to the constructor.
 */
@Deprecated
void setDocument(Document doc) { user_document = doc; }

Chat getXMPPChat()
{
   if (is_xmpp)
      return the_chat;
   return null;
}

Conversation getAIMConversation()
{
   if (!is_xmpp)
      return the_conversation;
   return null;
}

Object getChat()
{
   if (is_xmpp)
      return the_chat;
   return the_conversation;
}



/********************************************************************************/
/*                            */
/* Comparison methods                           */
/*                            */
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (!(o instanceof BgtaChat)) return false;
   BgtaChat chat = (BgtaChat) o;
   
   return user_name.equals(chat.getUsername()) && user_server.equals(chat.getServer()) && user_document.equals(chat.getDocument());
}

@Override public int hashCode()
{
   return user_name.hashCode() + user_server.hashCode() + user_document.hashCode();
}

@Override public String toString()
{
   return user_name + ", " + user_server + ", " + user_document.toString();
}



/********************************************************************************/
/*                            */
/* Messaging methods                           */
/*                            */
/********************************************************************************/

void messageReceived(Object msg)
{
   Message message = null;
   MessageInfo minfo = null;
   if (is_xmpp) {
      message = (Message) msg;
      logMessage(message.getBody());
    }
   else {
      minfo = (MessageInfo) msg;
      logMessage(minfo.getMessage().getMessageBody().replaceAll("<.*?>",""));
    }
}



void logMessage(String message)
{
   logMessage(message,user_display);
}



void logMessage(String message,String from)
{
   if (!from.equals(user_display) && !from.equals("Me") && !from.equals("Error"))
      return;
   try {
      user_document.insertString(user_document.getLength(), from + ": " + message + "\n", null);
    } catch (BadLocationException e) {
   //System.out.println("bad loc");
    }
}



boolean sendMessage(String message)
{
   boolean sent = true;
   try {
      if (is_xmpp && the_chat != null)
         the_chat.sendMessage(message);
      if (!is_xmpp && the_conversation != null)
         the_conversation.sendMessage(new SimpleMessage(wrapHTML(message)));
    } catch (XMPPException e) {
      sent = false;
      BoardLog.logE("BGTA","Error sending message: " + message + " to: " + user_name);
      logMessage("Message not sent. Please try again.", "Error");
    }
   if (sent)
      logMessage(message, "Me");
   return sent;
}



/********************************************************************************/
/*                            */
/* Helper methods                           */
/*                            */
/********************************************************************************/

/**
 * A method which creates a more display-able version of a username. This
 * is accomplished by tearing off anything after an @, and replacing periods
 * and underscores with whitespace.
 */
private String getName(String username)
{
   String name = username;
   int idx = name.indexOf("@");
   if (idx > 0)
       name = name.substring(0, idx);
   name = whiteSpaceAwareReplace(name,".");
   name = whiteSpaceAwareReplace(name,"_");
   return name;
}



private String whiteSpaceAwareReplace(String input,String toreplace)
{
   String current = new String(input);
   while (current.indexOf(toreplace) != -1) {
      if (current.charAt(current.indexOf(toreplace) + 1) != ' ') {
         String back = current.substring(current.indexOf(toreplace) + 1);
         String front = current.substring(0, current.indexOf(toreplace));
         current = front + " " + back;
       }
      else {
         String back = current.substring(current.indexOf(toreplace) + 1);
         String front = current.substring(0, current.indexOf(toreplace));
         current = front + back;
       }   
    }
   return current;
}



private String wrapHTML(String text)
{
   String temp = text;
   temp = replace(temp,"&","&amp;");
   temp = replace(temp,"<","&lt;");
   temp = replace(temp,">","&gt;");
   temp = replace(temp,"\"","&qout;");
   temp = replace(temp,"\n","<br>");
   return "<html><body>" + temp + "</body></html>";
}



private String replace(String input,String toreplace,String replacewith)
{
   String current = new String(input);
   int pos = current.indexOf(toreplace);
   if (pos != -1) {
      current = current.substring(0,pos) + replacewith + replace(current.substring(pos + toreplace.length()),toreplace,replacewith);
    }
   return current;
}



/********************************************************************************/
/*                            */
/* Management methods                           */
/*                            */
/********************************************************************************/

void close()
{
   if (is_xmpp) {
      if (the_chat != null && chat_listener != null)
         the_chat.removeMessageListener(chat_listener);
    }
   else {
      if (the_conversation != null && conversation_listener != null)
         the_conversation.removeConversationListener(conversation_listener);
    }
}


/**
 * A class for processing received XMPP messages.
 * TODO: handle metadata messages.
 * 
 * @author Sumner Warren
 *
 */
private class XMPPChatListener implements MessageListener {

@Override public void processMessage(Chat ch,Message msg) {
   if (msg.getType() == Message.Type.chat) {
//      String tolog = msg.getBody();
//      if (tolog.startsWith(BGTA_METADATA_START) && tolog.endsWith(BGTA_METADATA_FINISH)) {
//    tolog = tolog.substring(BGTA_METADATA_START.length(), tolog.length()
//                - BGTA_METADATA_FINISH.length());
//    logMessage("Click the button below to load the data", "");
//    JButton accept = new JButton("Load Task to Task Shelf");
//    Dimension d = new Dimension(BGTA_DATA_BUTTON_WIDTH,BGTA_DATA_BUTTON_HEIGHT);
//    accept.setPreferredSize(d);
//    accept.setSize(d);
//    accept.setMinimumSize(d);
//    Element xml = IvyXml.loadXmlFromURL(tolog);
//    accept.addActionListener(new XMLListener(xml));
//    setCaretPosition(my_doc.getLength());
//    insertComponent(accept);
//       }
//      else 
      if (!ch.equals(the_chat))
         return;
      messageReceived(msg);
    }
 }

}  // end of inner class XMPPChatListener



private class AIMChatListener implements ConversationListener {

   @Override public void canSendMessageChanged(Conversation arg0, boolean arg1) { }

   @Override public void conversationClosed(Conversation arg0) { }

   @Override public void conversationOpened(Conversation arg0) { }

   @Override public void gotMessage(Conversation con, MessageInfo msg) {
      if (!con.equals(the_conversation))
          return;
      messageReceived(msg);
    }

   @Override public void gotOtherEvent(Conversation arg0, ConversationEventInfo arg1) {
		// TODO: metadata?
    }

   @Override public void sentMessage(Conversation arg0, MessageInfo arg1) { }

   @Override public void sentOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

}  // end of inner class AIMChatListener



}
