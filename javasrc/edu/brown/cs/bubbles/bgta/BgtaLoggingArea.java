/********************************************************************************/
/*										*/
/*		BgtaLoggingArea.java						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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

import edu.brown.cs.bubbles.bgta.BgtaManager.BgtaXMPPConversation;
import edu.brown.cs.ivy.xml.IvyXml;

import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationListener;
import net.kano.joustsim.oscar.oscar.service.icbm.MessageInfo;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class BgtaLoggingArea extends JTextPane implements MessageListener, ConversationListener, BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BgtaConversation		my_chat;
private String		buddy_name;
private Document	my_doc;
private AttributeSet	is_bolded;
private AttributeSet	is_unbolded;
private AttributeSet	in_use;
// private int		last_focused_caret_pos;
private BgtaBubble	my_bubble;

private static final long serialVersionUID = 1L;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaLoggingArea(BgtaBubble bub)
{
   // super(7, 25);
   super();
   setContentType("text/html");
   setText("");
   // last_focused_caret_pos = 0;
   Dimension d = new Dimension(BGTA_LOG_WIDTH,BGTA_LOG_HEIGHT);
   setPreferredSize(d);
   setSize(d);
   putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
   my_chat = null;
   buddy_name = "";
   // setLineWrap(true);
   // setWrapStyleWord(true);
   // setContentType("text/html");
   setEditable(false);
   setOpaque(false);
   setCursor(new Cursor(Cursor.TEXT_CURSOR));
   setBorder(new EmptyBorder(0,5,0,0));

   my_doc = getDocument();
   StyleContext sc = StyleContext.getDefaultStyleContext();
   is_unbolded = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, false);
   is_bolded = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, true);
   in_use = null;// is_unbolded;

   my_bubble = bub;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean getScrollableTracksViewportWidth()
{
   return true;
}

void setChat(BgtaConversation ch)
{
   my_chat = ch;
   buddy_name = my_chat.getUser();
   if (my_bubble.getManager().getRoster().getEntry(buddy_name).getName() != null)
	  buddy_name = my_bubble.getManager().getRoster().getEntry(buddy_name).getName() + ": ";
   else
      buddy_name = buddy_name.substring(0, buddy_name.lastIndexOf("@")) + ": ";
}



@Override public void setDocument(Document doc)
{
   super.setDocument(doc);
   my_doc = doc;
}

BgtaConversation getChat()
{
   return my_chat;
}



/********************************************************************************/
/*										*/
/*	Message logging methods 						*/
/*										*/
/********************************************************************************/

void logMessage(Message recieved)
{
   logMessage(recieved.getBody());
}


void logMessage(String recieved)
{
   logMessage(recieved, buddy_name);
}



void logMessage(String recieved,String from)
{
   setEditable(true);
   setCaretPosition(getDocument().getLength());
//   try {
//	  if (!from.equals("Me: ")) {
//	if (my_bubble.getManager().getRoster().getEntry(from).getName() != null) 
//	   from = my_bubble.getManager().getRoster().getEntry(from).getName();
//	   }
//      getDocument().insertString(my_doc.getLength(), from + recieved + "\n", null);
//    }
//   catch (BadLocationException e) {
      //System.out.println("bad loc");
//    }
   if (my_bubble.reloadAltColor() && in_use == is_bolded) {
      my_bubble.setAltColorIsOn(true);
    }
   setEditable(false);
   setCaretPosition(getDocument().getLength());
}



/********************************************************************************/
/*										*/
/*	Bolding methods (currently not in use, could be added back later)	*/
/*										*/
/********************************************************************************/

void bold()
{
   // last_focused_caret_pos = getDocument().getLength();
   // in_use = is_bolded;
}


void unbold()
{
   // int len;
   in_use = is_unbolded;
   /*
      try{
      len = getDocument().getLength() - last_focused_caret_pos;
      String tochange = getDocument().getText(last_focused_caret_pos, len);
      getDocument().remove(last_focused_caret_pos, len);
      getDocument().insertString(getDocument().getLength(), tochange, in_use);
    }catch(BadLocationException e){}*/
}



/********************************************************************************/
/*										*/
/*	Message listener							*/
/*										*/
/********************************************************************************/

@Override public void processMessage(Chat ch,Message received)
{
//   if (ch != ((BgtaXMPPConversation) my_chat).getChat()) return;
//   if (received.getType() == Message.Type.chat) {
//      String tolog = received.getBody();
//      if (tolog.startsWith(BGTA_METADATA_START) && tolog.endsWith(BGTA_METADATA_FINISH)) {
//	 tolog = tolog.substring(BGTA_METADATA_START.length(), tolog.length()
//				    - BGTA_METADATA_FINISH.length());
//	 logMessage("Click the button below to load the data", "");
//	 JButton accept = new JButton("Load Task to Task Shelf");
//	 Dimension d = new Dimension(BGTA_DATA_BUTTON_WIDTH,BGTA_DATA_BUTTON_HEIGHT);
//	 accept.setPreferredSize(d);
//	 accept.setSize(d);
//	 accept.setMinimumSize(d);
//	 Element xml = IvyXml.loadXmlFromURL(tolog);
//	 accept.addActionListener(new XMLListener(xml));
//	 setCaretPosition(my_doc.getLength());
//	 insertComponent(accept);
//       }
//      else 
//    	logMessage(tolog);
//    }
}



/********************************************************************************/
/*										*/
/*	Button press methods							*/
/*										*/
/********************************************************************************/

void pressedButton()
{
   try {
      my_doc.insertString(my_doc.getLength(), BGTA_TASK_DESCRIPTION, in_use);
    }
   catch (BadLocationException e) {}
}



private class XMLListener implements ActionListener {

   private Element _xml;

   private XMLListener(Element xml) {
      _xml = xml;
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      BgtaFactory.addTaskToRoot(_xml);
      pressedButton();
    }

}	// end of private class XMLListener



/********************************************************************************/
/*										*/
/*	ConversationListener							*/
/*										*/
/********************************************************************************/

@Override public void canSendMessageChanged(Conversation conv, boolean cansend) { }



@Override public void conversationClosed(Conversation conv) { }



@Override public void conversationOpened(Conversation conv) { }



@Override public void gotMessage(Conversation conv, MessageInfo minfo) {
	logMessage(minfo.getMessage().getMessageBody().replaceAll("<.*?>",""));
}



@Override public void gotOtherEvent(Conversation conv, ConversationEventInfo cinfo) { }



@Override public void sentMessage(Conversation conv, MessageInfo minfo) { }



@Override public void sentOtherEvent(Conversation conv, ConversationEventInfo cinfo) { }



}	// end of class BgtaLoggingArea




/* end of BgtaLoggingArea.java */
