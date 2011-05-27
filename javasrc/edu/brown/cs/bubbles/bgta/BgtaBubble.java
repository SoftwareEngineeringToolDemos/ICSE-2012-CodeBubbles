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


import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.*;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;

import java.awt.*;
import java.awt.geom.Rectangle2D;



class BgtaBubble extends BudaBubble implements BgtaConstants {



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
private Chat		  chat_panel;
private boolean 	  is_saved;
private boolean 	  alt_color;
private boolean 	  alt_color_is_on;
private int		  username_id;
private boolean 	  is_preview;

private static final long serialVersionUID = 1L;



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
   chat_username = username;
   my_props = BgtaFactory.getBgtaProperties();
   alt_color = my_props.getBoolean(BGTA_ALT_COLOR_UPON_RECIEVE);
   alt_color_is_on = alt_color;
   ChatPanel pan = new ChatPanel();
   GridBagLayout lay = (GridBagLayout) pan.getLayout();
   GridBagConstraints c = new GridBagConstraints();

   logging_area = new BgtaLoggingArea(this);
   if (!the_manager.hasBubble(chat_username)) {
      chat_panel = the_manager.startChat(chat_username, logging_area, this);
      username_id = 1;
    }
   else {
      BgtaBubble existingBubble = the_manager.getExistingBubble(chat_username);
      BgtaLoggingArea existingLog = null;
      Document doc = null;
      if (existingBubble != null) {
	 existingLog = existingBubble.getLog();
	 if (existingLog != null) {
	    doc = existingLog.getDocument();
	    chat_panel = existingLog.getChat();
	    if (doc != null && chat_panel != null) {
	       logging_area.setDocument(doc);
	       username_id = existingBubble.getUsernameID() + 1;
	       the_manager.addDuplicateBubble(this);
	     }
	  }
       }
    }
   logging_area.setChat(chat_panel);

   draft_area = new BgtaDraftingArea(chat_panel,logging_area,this);
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



@Override protected void localDispose()
{
   the_manager.removeBubble(this);
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



/********************************************************************************/
/*										*/
/*	Manager methods 							*/
/*										*/
/********************************************************************************/

String getBuddy()				{ return chat_username; }



/********************************************************************************/
/*										*/
/*	Messaging methods							*/
/*										*/
/********************************************************************************/

void recieveMessage(Message mess)
{
   logging_area.processMessage(chat_panel, mess);
}



void sendMessage(String mess)
{
   draft_area.send(mess);
}



void sendMetadata(String metadata)
{
   try {
      chat_panel.sendMessage(metadata);
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
