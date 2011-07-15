/********************************************************************************/
/*										                                                  */
/*		BeduTATicketListBubble.java 	                                            */
/*		This Bubble is a TA can view a ticket and respond                         */
/*    by opening a chat session with the student         							  */
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.bubbles.buda.BudaBubble;

class BeduTATicketViewBubble extends BudaBubble {
private static final long serialVersionUID = 1L;
private static Color		  GRADIENT_BOTTOM_COLOR = Color.white;
private static Color		  GRADIENT_TOP_COLOR	 = new Color(0x33, 0x00, 0x99);
private static Dimension  DEFAULT_DIMENSION	 = new Dimension(250, 200);

private BeduStudentTicket ticket;



BeduTATicketViewBubble(BeduStudentTicket t, BeduTAXMPPClient a_client) {
	ticket = t;
	setContentPane(new TicketViewPanel(t, this, new ChatStartListener(this,
			a_client)));
}

private class TicketViewPanel extends JPanel 
{
	private static final long serialVersionUID = 1L;

	private TicketViewPanel(BeduStudentTicket t, BeduTATicketViewBubble a_bubble,
      ChatStartListener listener) {
   	// add(new JLabel(t.getText()));

   	setOpaque(false);
   
   	setPreferredSize(DEFAULT_DIMENSION);
   	setLayout(new GridBagLayout());
   
   	GridBagConstraints c = new GridBagConstraints();
   
   	JLabel ticket_area_label = new JLabel("Problem description: ");
   	c.gridx = 0;
   	c.gridy = 0;
   	c.gridwidth = 2;
   	c.gridheight = 1;
   	c.weightx = 0;
   	c.anchor = GridBagConstraints.PAGE_START;
   	c.insets = new Insets(0, 0, 10, 0);
   	//c.fill = GridBagConstraints.HORIZONTAL;
   	add(ticket_area_label, c);
   	c.insets = new Insets(0, 0, 0, 0);
   
   	JTextArea ticket_pane = new JTextArea();
   	ticket_pane.setOpaque(false);
   	ticket_pane.setText(t.getText());
   	ticket_pane.setLineWrap(true);
   	JScrollPane scroll = new JScrollPane(ticket_pane);
   	scroll.setOpaque(false);
   	scroll.getViewport().setOpaque(false);
   	//scroll.setBorder(null);
   	c.anchor = GridBagConstraints.PAGE_START;
   	c.gridx = 0;
   	c.gridy = 1;
   	c.gridwidth = 2;
   	c.gridheight = 1;
   	c.fill = GridBagConstraints.BOTH;
   	c.weightx = 1;
   	c.weighty = 1;
   	add(scroll, c);
   
   	JButton submit_button = new JButton("Chat with student");
   	submit_button.addActionListener(listener);
   	c.anchor = GridBagConstraints.PAGE_END;
   	c.gridx = 1;
   	c.gridy = 2;
   	c.gridwidth = 1;
   	c.gridheight = 1;
   	c.weightx = .4;
   	c.weighty = 0;
   	c.fill = GridBagConstraints.NONE;
   	add(submit_button, c);
   }
   
   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Paint p = new GradientPaint(0f, 0f, GRADIENT_BOTTOM_COLOR, 0f, sz.height,
            GRADIENT_TOP_COLOR);
   
      Shape r = new Rectangle2D.Float(0, 0, sz.width, sz.height);
      g2.setColor(Color.orange);
      g2.fill(r);
      g2.setPaint(p);
      g2.fill(r);
   
      super.paintComponent(g);
   }
}

private class ChatStartListener implements ActionListener 
{
   private BeduTATicketViewBubble bubble;
   private BeduTAXMPPClient		 client;
   
   
   
   private ChatStartListener(BeduTATicketViewBubble a_bubble,
   		BeduTAXMPPClient a_client) {
   	bubble = a_bubble;
   	client = a_client;
   }
   
   
   
   @Override public void actionPerformed(ActionEvent e) {
   	client.acceptTicketAndAlertPeers(bubble.ticket);
   	BudaBubble chat_bub = new BeduChatBubble.TABubble(client,
   			client.getChatForJID(bubble.ticket.getStudentJID()));
   	BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bubble);
   	bba.addBubble(chat_bub, bubble, null, PLACEMENT_LOGICAL | PLACEMENT_GROUPED);
   }
}
}
