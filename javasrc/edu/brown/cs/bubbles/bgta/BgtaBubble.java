/********************************************************************************/
/*										*/
/*		BgtaBubble.java 						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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


import edu.brown.cs.bubbles.bgta.BgtaManager.*;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.*;

import org.jivesoftware.smack.XMPPException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;



class BgtaBubble extends BudaBubble implements BgtaConstants, DocumentListener {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BgtaManager	  the_manager;
private String		  chat_username;
private BoardProperties   my_props;
private BgtaLoggingArea   logging_area;
private BgtaDraftingArea  draft_area;
private BgtaConversation		  the_chat;
private boolean 	  is_saved;
private boolean 	  alt_color;
private boolean 	  alt_color_is_on;
private int		  username_id;
private boolean 	  is_preview;
private boolean		  is_listener;

private static final long serialVersionUID = 1L;
private static HashMap<String, Integer> current_id;

static {
	current_id = new HashMap<String, Integer>();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaBubble(String username,BgtaManager man)
{
   this(username,man,false);
}



BgtaBubble(String username,BgtaManager man,boolean preview)
{
   the_manager = man;
   is_saved = the_manager.isBeingSaved();
   is_preview = preview;
   is_listener = false;
   chat_username = username;
   my_props = BgtaFactory.getBgtaProperties();
   alt_color = my_props.getBoolean(BGTA_ALT_COLOR_UPON_RECIEVE);
   alt_color_is_on = alt_color;
   ChatPanel pan = new ChatPanel();
   GridBagLayout lay = (GridBagLayout) pan.getLayout();
   GridBagConstraints c = new GridBagConstraints();

   Document doc = null;
   logging_area = new BgtaLoggingArea(this);
   if (!the_manager.hasBubble(chat_username)) {
	  if (!the_manager.hasChat(chat_username)) {
//	 the_chat = the_manager.startChat(chat_username, logging_area, this);
	     doc = the_manager.startChat(chat_username,this);
	     logging_area.setDocument(doc);
	 current_id.put(chat_username, 1);
     username_id = 1;
     is_listener = true;
	   }
	  else {
//	 the_chat = the_manager.getExistingChat(chat_username);
//	 the_chat.increaseUseCount();
	     doc = the_manager.getExistingDoc(chat_username);
	     logging_area.setDocument(doc);
	 Integer id = current_id.remove(chat_username);
	 current_id.put(chat_username, id.intValue() + 1);
	 username_id = id.intValue() + 1;
	 the_manager.addDuplicateBubble(this);
	   }
    }
   else {
      BgtaBubble existingBubble = the_manager.getExistingBubble(chat_username);
      BgtaLoggingArea existingLog = null;
      if (existingBubble != null) {
	 existingLog = existingBubble.getLog();
	 if (existingLog != null) {
//	    doc = existingLog.getDocument();
	    doc = the_manager.getExistingDoc(chat_username);
//	    the_chat = existingLog.getChat();
//	    the_chat.increaseUseCount();
	    if (doc != null/* && the_chat != null*/) {
	       logging_area.setDocument(doc);
	       Integer id = current_id.remove(chat_username);
		   current_id.put(chat_username, id.intValue() + 1);
		   username_id = id.intValue() + 1;
	       the_manager.addDuplicateBubble(this);
	     }
	  }
       }
    }

   // Register bubble as document listener.
   if (doc != null)
      doc.addDocumentListener(this);
   draft_area = new BgtaDraftingArea(/*the_chat*/the_manager.getChat(chat_username),logging_area,this);
   JScrollPane log_pane = new JScrollPane(logging_area,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

   log_pane.setCursor(new Cursor(Cursor.HAND_CURSOR));
   log_pane.setOpaque(false);
   log_pane.getViewport().setOpaque(false);
   log_pane.setBorder(new EmptyBorder(0,0,0,0));

   BgtaLabel lab = new BgtaLabel(chat_username,the_manager.getRoster());
   the_manager.addPresenceListener(lab);

   c.fill = GridBagConstraints.BOTH;
   c.gridwidth = GridBagConstraints.REMAINDER;
   c.weighty = 0.0;
   c.weightx = 1.0;

   lay.setConstraints(lab, c);

   c.weighty = 1.0;

   lay.setConstraints(log_pane, c);

   c.weighty = 0.0;

   JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
   lay.setConstraints(sep, c);

   lay.setConstraints(draft_area, c);

   pan.add(lab);
   pan.add(log_pane);
   pan.add(sep);
   pan.add(draft_area);

   pan.setFocusable(true);
   pan.addMouseListener(new BudaConstants.FocusOnEntry(draft_area));
   logging_area.addMouseListener(new BudaConstants.FocusOnEntry(draft_area));
   draft_area.setFocusable(true);
   draft_area.addMouseListener(new BudaConstants.FocusOnEntry(draft_area));

   setContentPane(pan, draft_area);
}



@Override public void setVisible(boolean vis)
{
   super.setVisible(vis);
   if (!vis)
	  the_manager.removeBubble(this);
   else
	  the_manager.updateBubble(this);
}



@Override protected void localDispose()
{
   // Remove the chat.
//   the_manager.removeChat(the_chat,logging_area);
   the_manager.removeChat(chat_username);
   
   // TODO: there should only be one, right?
   // If that was the last one, clear the id for the user.
//   if (!the_manager.hasConversation(chat_username))
   if (!the_manager.hasChat(chat_username))
	  current_id.remove(chat_username);
   the_chat = null;
}



void makeActive()
{
   BgtaConversation chat = logging_area.getChat();
   if (chat.isListener(logging_area))
	  is_listener = true;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getUsername()				{ return chat_username; }

int getUsernameID()				{ return username_id; }

BgtaLoggingArea getLog()			{ return logging_area; }

boolean isPreview()				{ return is_preview; }

boolean isListener()			{ return is_listener; }

BgtaManager getManager()			{ return the_manager; }



/********************************************************************************/
/*										*/
/*	Messaging methods							*/
/*										*/
/********************************************************************************/

void recieveMessage(BgtaMessage mess)
{
   if (mess instanceof BgtaXMPPMessage)
	  logging_area.processMessage(((BgtaXMPPConversation) the_chat).getChat(), ((BgtaXMPPMessage) mess).getMessage());
   else
	  logging_area.logMessage(mess.getBody());
}



void sendMessage(String mess)
{
   draft_area.send(mess);
}



void sendMetadata(String metadata)
{
   try {
      the_chat.sendMessage(metadata);
    }
   catch (XMPPException e) {}

   logging_area.logMessage("Sent the data", " ");
}



/********************************************************************************/
/*										*/
/*	Alt color methods							*/
/*										*/
/********************************************************************************/

void setAltColorIsOn(boolean ison)
{
   if (!alt_color) return;
   alt_color_is_on = ison;
   repaint();
}



boolean reloadAltColor()
{
   alt_color = my_props.getBoolean(BGTA_ALT_COLOR_UPON_RECIEVE);
   if (!alt_color) alt_color_is_on = false;
   return alt_color;
}



boolean getAltColorIsOn()
{
   return alt_color_is_on;
}



/********************************************************************************/
/*                            */
/* Document listener methods                           */
/*                            */
/********************************************************************************/
@Override
public void changedUpdate(DocumentEvent e)
{
    if (isVisible())
        repaint();
    logging_area.setCaretPosition(logging_area.getDocument().getLength());
}



@Override
public void insertUpdate(DocumentEvent e)
{
   if (isVisible())
      repaint();
   logging_area.setCaretPosition(logging_area.getDocument().getLength());
}



@Override
public void removeUpdate(DocumentEvent e)
{
    if (isVisible())
        repaint();
    logging_area.setCaretPosition(logging_area.getDocument().getLength());
}



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (!(o instanceof BgtaBubble)) return false;
   BgtaBubble b = (BgtaBubble) o;

   return chat_username.equals(b.getUsername()) && username_id == b.getUsernameID();
}



@Override public int hashCode()
{
   return chat_username.hashCode() + username_id;
}



@Override public String toString()
{
   return "BgtaBubble Username: " + chat_username + ", UsernameID: " + username_id;
}



/********************************************************************************/
/*										*/
/*	Chat Panel implementation						*/
/*										*/
/********************************************************************************/

private class ChatPanel extends JPanel implements BgtaConstants,
BudaConstants.BudaBubbleOutputer {

   private static final long serialVersionUID = 1L;

   ChatPanel() {
      super(new GridBagLayout());
      setOpaque(false);
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Paint p;
      if (!alt_color_is_on) p = new GradientPaint(0f,0f,BGTA_BUBBLE_TOP_COLOR,0f,sz.height,
						     BGTA_BUBBLE_BOTTOM_COLOR);
      else p = new GradientPaint(0f,0f,BGTA_ALT_BUBBLE_TOP_COLOR,0f,sz.height,
				    BGTA_ALT_BUBBLE_BOTTOM_COLOR);
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setColor(Color.white);
      g2.fill(r);
      g2.setPaint(p);
      g2.fill(r);

      super.paintComponent(g);
    }

   @Override public String getConfigurator()		{ return "BGTA"; }

   @Override public void outputXml(BudaXmlWriter xw) {
      if (!is_saved) return;
      xw.field("TYPE", "CHAT");
      xw.field("NAME", chat_username);
      xw.field("USERNAME", the_manager.getUsername());
      xw.field("PASSWORD", the_manager.getPassword());
      xw.field("SERVER", the_manager.getServer());
    }

}	// end of private class ChatPanel



}	// end of class BgtaBubble



/* end of BgtaBubble.java */
