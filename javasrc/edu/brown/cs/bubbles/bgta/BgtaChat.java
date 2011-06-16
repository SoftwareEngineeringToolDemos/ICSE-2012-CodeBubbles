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

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;

import org.jivesoftware.smack.Chat;

import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;

public class BgtaChat implements BgtaConstants {



/********************************************************************************/
/*                            */
/* Private storage                           */
/*                            */
/********************************************************************************/

// used for all chats
private String user_name;
private String user_server;
private Document the_document;

// used for XMPP chats
private Chat the_chat;

// used for AIM (OSCAR) conversations
private Conversation the_conversation;

// wrapper members
private boolean is_xmpp;

/********************************************************************************/
/*                            */
/* Constructors                           */
/*                            */
/********************************************************************************/

/**
 * Convenience constructor for BgtaChat(username,server,chat,null).
 */
BgtaChat(String username,String server,Object chat)
{
   this(username,server,chat,null);
}

BgtaChat(String username,String server,Object chat,Document doc)
{
   user_name = username;
   user_server = server;
   the_chat = null;
   the_conversation = null;
   is_xmpp = false;
   if (!server.equals("aim")) {
      is_xmpp = true;
      the_chat = (Chat) chat;
   }
   else
      the_conversation = (Conversation) chat;
   the_document = doc;
   if (the_document == null) {
      the_document = new DefaultStyledDocument();
   }
}



/********************************************************************************/
/*                            */
/* Access methods                           */
/*                            */
/********************************************************************************/

String getUsername() { return user_name; }

String getServer() { return user_server; }

Document getDocument() { return the_document; }

boolean isXMPP() { return is_xmpp; }

/**
 * Preferred method of setting the document is by passing
 * it to the constructor.
 */
@Deprecated
void setDocument(Document doc) { the_document = doc; }

Chat getXMPPChat() {
   if (is_xmpp)
      return the_chat;
   return null;
}

Conversation getAIMConversation() {
   if (!is_xmpp)
      return the_conversation;
   return null;
}

Object getChat() {
   if (is_xmpp)
      return the_chat;
   return the_conversation;
}



/********************************************************************************/
/*                            */
/* Management methods                           */
/*                            */
/********************************************************************************/





}
