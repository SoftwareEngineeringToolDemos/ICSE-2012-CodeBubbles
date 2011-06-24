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
import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.Collection;
import java.util.Vector;
import java.util.Date;

import java.text.SimpleDateFormat;

import java.io.File;
import java.io.IOException;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import org.w3c.dom.*;

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
private BgtaManager the_manager;
private ChatHistory the_history;
private File history_file;
private boolean is_open;
private static final String HISTORY_TIMESTAMP_FORMAT = "MM/dd/yyyy HH:mm:ss";
private static final SimpleDateFormat date_format = new SimpleDateFormat(HISTORY_TIMESTAMP_FORMAT);

// used for XMPP chats
private Chat the_chat;
private MessageListener chat_listener;
private Message last_message;

// used for AIM (OSCAR) conversations
private Conversation the_conversation;
private ConversationListener conversation_listener;
private MessageInfo last_minfo;

// wrapper members
private boolean is_xmpp;

/********************************************************************************/
/*                            */
/* Constructors                           */
/*                            */
/********************************************************************************/

BgtaChat(String username,String displayName,ChatServer server,Object chat,Document doc,BgtaManager man)
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
   the_manager = man;
   is_open = true;
   last_message = null;
   last_minfo = null;
   
   // Determine the protocol and set up members appropriately.
   if (!server.equals(ChatServer.AIM)) {
      is_xmpp = true;
      the_chat = (Chat) chat;
      chat_listener = new XMPPChatListener();
      the_chat.addMessageListener(chat_listener);
    }
   else {
      the_conversation = (Conversation) chat;
      conversation_listener = new AIMChatListener();
      the_conversation.addConversationListener(conversation_listener);
   }
   
   // Create a new Document for this user if one doesn't exist.
   // Otherwise, use the old one.
   user_document = doc;
   if (user_document == null) {
      user_document = new HTMLDocument();
    }
   
   // Create a new ChatHistory for this chat.
   the_history = new ChatHistory(this);
   history_file = null;
}



/********************************************************************************/
/*                            */
/* Access methods                           */
/*                            */
/********************************************************************************/

String getUsername()    { return user_name; }

ChatServer getServer()  { return user_server; }

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

Object getLastMessage()
{
    if (!is_xmpp)
        return last_minfo;
    return last_message;
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
   String message = null;
   if (is_xmpp) {
      if (msg == last_message)
         return;
      message = ((Message) msg).getBody();
      last_message = (Message) msg;
    }
   else {
       if (msg == last_minfo)
          return;
       message = ((MessageInfo) msg).getMessage().getMessageBody().replaceAll("<.*?>","");
       last_minfo = (MessageInfo) msg;
    }
   if (message == null || message.equals(""))
      return;
   if (!is_open) {
       BgtaFactory.createRecievedChatBubble(user_name,the_manager);
       is_open = true;
    }
   if (message.startsWith(BGTA_METADATA_START) && message.endsWith(BGTA_METADATA_FINISH)) {
       logMessage("Working set received.","Bubbles");
       Collection<BgtaBubble> bubbles = the_manager.getExistingBubbles(user_name);
       if (bubbles != null) {
           for (BgtaBubble bb : the_manager.getExistingBubbles(user_name)) {
               bb.processMetadata(message);
            }
        }
    }
   else
      logMessage(message);
}

void logMessage(String message)
{
   logMessage(message,user_display);
}

void logMessage(String message,String from)
{
   if (!from.equals(user_display) && !from.equals("Me") && !from.equals("Error") && !from.equals("Bubbles"))
      return;
   try {
      user_document.insertString(user_document.getLength(),from + ": " + message + "\n",null);
    } catch (BadLocationException e) {
       //System.out.println("bad loc");
    }
    String to = the_manager.getUsername();
    if (from.equals("Me"))
        to = user_name;
   the_history.addHistoryItem(new ChatHistoryItem(from,to,message,date_format.format(new Date())));
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

private void createHistoryFile()
{
   if (history_file != null) return;
   
   File dir = BoardSetup.getBubblesPluginDirectory();
   if (dir != null) {
      try {
         for (int i = 0; i < 5; ++i) {
            String fnm = "history_" + the_manager.getUsername().toLowerCase() + "_" + replace(user_name," ","").toLowerCase() + ".xml";
            File f = new File(dir,fnm);
            if (f.createNewFile()) {
               history_file = f;
               break;
             }
            try {
               Thread.sleep(1000);
             }
            catch (InterruptedException e) { }
          }
       }
      catch (IOException e) {
         BoardLog.logE("BGTA","Problem created chat history file",e);
       }
    }
}

private synchronized void saveHistory()
{
   if (history_file == null) createHistoryFile();
   if (history_file == null) {
      BoardLog.logE("BGTA","Problem saving chat history");
      return;
    }
   
   try {
      IvyXmlWriter xw = new IvyXmlWriter(history_file);
      the_history.outputToXML(xw);
      xw.close();
    }
   catch (IOException e) {
      BoardLog.logE("BGTA","Problem writing chat history file",e);
      history_file = null;
    }
}


/********************************************************************************/
/*                            */
/* Management methods                           */
/*                            */
/********************************************************************************/

void close()
{
   is_open = false;
   saveHistory();   
}


/**
 * A class for processing received XMPP messages.
 * 
 * @author Sumner Warren
 *
 */
private class XMPPChatListener implements MessageListener {

@Override public void processMessage(Chat ch,Message msg) {
    if (!ch.equals(the_chat))
        return;
    if (msg.getType() != Message.Type.chat)
        return;
    messageReceived(msg);
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

   @Override public void gotOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

   @Override public void sentMessage(Conversation arg0, MessageInfo arg1) { }

   @Override public void sentOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

}  // end of inner class AIMChatListener



private class ChatHistory {
    
    private Vector<ChatHistoryItem> my_items;
    
    ChatHistory(BgtaChat ch) {
        my_items = new Vector<ChatHistoryItem>();
    }
    
    public void addHistoryItem(ChatHistoryItem item) {
        my_items.add(item);
    }
    
    public void outputToXML(IvyXmlWriter xw) {
        xw.begin("HISTORY");
        xw.field("FROM",user_name);
        xw.field("TO",the_manager.getUsername());
        xw.field("SERVER",user_server.server());
        for (ChatHistoryItem item : my_items) {
            item.outputToXML(xw);
         }
        xw.end("HISTORY");
    }

}  // end of inner class ChatHistory



private class ChatHistoryItem {
    
    private String item_from;
    private String item_to;
    private String item_text;
    private String item_time;
    
    ChatHistoryItem(String from,String to,String text,String time) {
        item_from = from;
        item_to = to;
        item_text = text;
        item_time = time;
    }
    
    public String getFrom() { return item_from; }
    
    public String getTo() { return item_to; }
    
    public String getText() { return item_text; }
    
    public String getTime() { return item_time; }
    
    public void outputToXML(IvyXmlWriter xw) {
        xw.begin("MESSAGE");
        xw.textElement("FROM",item_from);
        xw.textElement("TO",item_to);
        xw.textElement("TEXT",item_text);
        xw.textElement("TIMESTAMP",item_time);
        xw.end("MESSAGE");
    }
    
}  // end of inner class ChatHistoryItem



}
