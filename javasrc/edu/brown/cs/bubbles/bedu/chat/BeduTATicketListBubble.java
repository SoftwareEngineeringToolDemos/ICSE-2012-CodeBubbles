/********************************************************************************/
/*										                                                  */
/*		BeduTATicketListBubble.java 	                                            */
/*		This Bubble is for listing the available tickets                          */
/*    so a TA can select them and open up chat with a student						  */
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
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;

class BeduTATicketListBubble extends BudaBubble {
private static final long serialVersionUID = 1L;
private static Color	  GRADIENT_BOTTOM_COLOR  = Color.white;
private static Color	  GRADIENT_TOP_COLOR	 	 = new Color(0x33, 0x00, 0x99);
private static Dimension DEFAULT_DIMENSION    = new Dimension(200, 200);



BeduTATicketListBubble(BeduTATicketList list, BeduTAXMPPClient a_ta_client) {
	TicketListPanel p = new TicketListPanel(list, this, a_ta_client);
	setContentPane(p);
}

private class TicketListPanel extends JPanel implements MouseListener {
	private static final long serialVersionUID = 1L;
	private JTable			    table;
   private BeduTATicketList ticket_list;
   private BudaBubble		 parent;
   private BeduTAXMPPClient ta_client;
   
   
   
   TicketListPanel(BeduTATicketList list, BudaBubble a_parent,
   		BeduTAXMPPClient a_ta_client) {
   	super(new BorderLayout());
   	parent = a_parent;
   	ta_client = a_ta_client;
   	ticket_list = list;
   	setOpaque(false);
   	setPreferredSize(DEFAULT_DIMENSION);
   	JLabel l = new JLabel("Tickets submitted by students:");
   	add(l, BorderLayout.NORTH);
   	table = new JTable(list);
   
   	table.getColumnModel().getColumn(1).setPreferredWidth(50);
   
   	table.getColumnModel().getColumn(0).setPreferredWidth(150);
   
   	table.setFillsViewportHeight(true);
   	JScrollPane p = new JScrollPane(table);
   	p.setPreferredSize(new Dimension(table.getPreferredSize().width, table
   			.getRowHeight() * 2));
   	p.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
   	table.setOpaque(false);
   	p.setOpaque(false);
   	table.addMouseListener(this);
   	add(p, BorderLayout.CENTER);
   }
   
   
   
   @Override public void mouseClicked(MouseEvent e) {
   	if (e.getClickCount() == 2) {
   		if (table.rowAtPoint(e.getPoint()) != -1) {
   			BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   			BudaBubble ticket_view_bubble = new BeduTATicketViewBubble((BeduStudentTicket)ticket_list.get(table.rowAtPoint(e.getPoint())), ta_client);
   			bba.addBubble(ticket_view_bubble, parent, null, PLACEMENT_LOGICAL
   					| PLACEMENT_GROUPED);
   		}
   	}
   }
   
   
   
   @Override public void mouseEntered(MouseEvent e) {
   	// TODO Auto-generated method stub
   
   }
   
   
   
   @Override public void mouseExited(MouseEvent e) {
   	// TODO Auto-generated method stub
   
   }
   
   
   
   @Override public void mousePressed(MouseEvent e) {
   	// TODO Auto-generated method stub
   
   }
   
   
   
   @Override public void mouseReleased(MouseEvent e) {
   	// TODO Auto-generated method stub
   
   }
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
