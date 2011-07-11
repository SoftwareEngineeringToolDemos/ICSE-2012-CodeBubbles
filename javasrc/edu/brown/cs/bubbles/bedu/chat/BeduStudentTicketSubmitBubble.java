/********************************************************************************/
/*										                                                  */
/*		BeduStudentTicketSubmitBubble.java 	                                      */
/*										     															  */
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
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.brown.cs.bubbles.bgta.*;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

class BeduStudentTicketSubmitBubble extends BudaBubble {
private static Color		  GRADIENT_BOTTOM_COLOR = Color.white;
private static Color		  GRADIENT_TOP_COLOR	   = new Color(0xC4, 0x32, 0x1F);
private static Dimension	 DEFAULT_DIMENSION	= new Dimension(250, 200);
private static final String NEW_LOGIN_STR			= "Log in to an XMPP account";

private static final long	serialVersionUID		= 1L;

private TicketPanel			panel;
private String				   ta_jid;



public BeduStudentTicketSubmitBubble(String a_jid) {
	HashMap<String, BgtaManager> chat_logins = new HashMap<String, BgtaManager>();
	for (Iterator<BgtaManager> it = BgtaFactory.getManagers(); it.hasNext();) {
		BgtaManager man = it.next();
		chat_logins.put(man.getUsername(), man);
	}

	ta_jid = a_jid;
	panel = new TicketPanel(chat_logins);
	setContentPane(panel);
}

private class TicketPanel extends JPanel implements ItemListener, ActionListener 
{
   private Map<String, BgtaManager> chat_logins;
   private JComboBox					 login_box;
   private JTextArea					 ticket_area;
   
   
   
   private TicketPanel(Map<String, BgtaManager> some_chat_logins) {
   	setOpaque(false);
   	chat_logins = some_chat_logins;
   	setPreferredSize(DEFAULT_DIMENSION);
   	setLayout(new GridBagLayout());
   
   	GridBagConstraints c = new GridBagConstraints();
   	JLabel l = new JLabel("Choose a chat login: ");
   	c.gridx = 0;
   	c.gridy = 0;
   	c.fill = GridBagConstraints.NONE;
   	c.weightx = 0.5;
   	c.weighty = 0.1;
   	add(l, c);
   
   	login_box = new JComboBox(chat_logins.keySet().toArray(new String[1]));
   	login_box.addItem(NEW_LOGIN_STR);
   	login_box.addItemListener(this);
   
   	c.gridx = 1;
   	c.gridy = 0;
   	c.fill = GridBagConstraints.NONE;
   	c.weightx = 0.5;
   	c.weighty = 0;
   	add(login_box, c);
   
   	JLabel ticket_area_label = new JLabel("Describe your question or problem: ");
   	c.gridx = 0;
   	c.gridy = 1;
   	c.gridwidth = 2;
   	c.gridheight = 1;
   	c.weightx = 0;
   	c.anchor = GridBagConstraints.PAGE_START;
   	c.insets = new Insets(0, 0, 10, 0);
   	// c.fill = GridBagConstraints.HORIZONTAL;
   	add(ticket_area_label, c);
   	c.insets = new Insets(0, 0, 0, 0);
   
   	ticket_area = new JTextArea();
   	ticket_area.setOpaque(false);
   	ticket_area.setText("Blah blah");
   	ticket_area.setLineWrap(true);
   	JScrollPane scroll = new JScrollPane(ticket_area);
   	scroll.setOpaque(false);
   	scroll.getViewport().setOpaque(false);
   	// scroll.setBorder(null);
   	c.anchor = GridBagConstraints.PAGE_START;
   	c.gridx = 0;
   	c.gridy = 2;
   	c.gridwidth = 2;
   	c.gridheight = 1;
   	c.fill = GridBagConstraints.BOTH;
   	c.weightx = 1;
   	c.weighty = 1;
   	add(scroll, c);
   
   	JButton submit_button = new JButton("Submit");
   	submit_button.addActionListener(this);
   	c.anchor = GridBagConstraints.PAGE_END;
   	c.gridx = 1;
   	c.gridy = 3;
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
   
   
   
   @Override public void itemStateChanged(ItemEvent e) {
   	if (e.getItem() instanceof String
   			&& ((String) (e.getItem())).equals(NEW_LOGIN_STR)) {
   		// TODO: figure out how to pull up login bubble, which is tricky
   		// because it has a package-private constructor with a lot of
   		// bgta-internal required args
   	}
   }
   
   
   
   @Override public void actionPerformed(ActionEvent e) {
   	BgtaManager man = panel.chat_logins.get(panel.login_box.getSelectedItem());
   
   	man.subscribeToUser(ta_jid);
   	BgtaBubble chat_b = BgtaFactory.createReceivedChatBubble(ta_jid, man);
   	BudaBubbleArea bba = BudaRoot
   			.findBudaBubbleArea(BeduStudentTicketSubmitBubble.this);
   	chat_b.sendMessage("TICKET:" + panel.ticket_area.getText());
   	bba.addBubble(chat_b, BeduStudentTicketSubmitBubble.this, null,
   			PLACEMENT_LOGICAL | PLACEMENT_GROUPED);
   }
}
}
