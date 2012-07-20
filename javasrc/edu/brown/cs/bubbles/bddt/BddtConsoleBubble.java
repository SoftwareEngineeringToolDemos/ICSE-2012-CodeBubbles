/********************************************************************************/
/*										*/
/*		BddtConsoleBubble.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool console bubble implementation */
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* SVN: $Id$ */

package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;



class BddtConsoleBubble extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextPane text_pane;
private JScrollPane scroll_pane;
private boolean   auto_scroll;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtConsoleBubble(BumpProcess bp,StyledDocument doc)
{
   this(doc);
}



BddtConsoleBubble(BddtLaunchControl ctrl,StyledDocument doc)
{
   this(doc);
}



private BddtConsoleBubble(StyledDocument doc)
{
   text_pane = new JTextPane(doc);
   text_pane.setEditable(false);
   text_pane.setBackground(Color.black);
   text_pane.setFont(BDDT_CONSOLE_FONT);
   text_pane.setForeground(Color.white);

   scroll_pane = new JScrollPane(text_pane);
   scroll_pane.setBorder(null);
   scroll_pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
   scroll_pane.setWheelScrollingEnabled(true);
   Dimension d = new Dimension(BDDT_CONSOLE_WIDTH,BDDT_CONSOLE_HEIGHT);
   auto_scroll = true;

   text_pane.setPreferredSize(d);

   setContentPane(scroll_pane,text_pane);

   doc.addDocumentListener(new EndScroll());
   text_pane.addMouseListener(new FocusOnEntry());
}




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());

   JCheckBoxMenuItem sitm = new JCheckBoxMenuItem("Auto Scroll");
   sitm.setState(auto_scroll);
   sitm.addActionListener(new AutoScrollAction());
   menu.add(sitm);

   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*										*/
/*	handle automatic scrolling to end					*/
/*										*/
/********************************************************************************/

private class EndScroll implements DocumentListener, Runnable {

   private boolean	is_queued;

   EndScroll() {
      is_queued = false;
    }

   @Override public void changedUpdate(DocumentEvent e) { }

   @Override public void removeUpdate(DocumentEvent e) { }

   @Override public void insertUpdate(DocumentEvent e) {
      if (!auto_scroll) return;

      synchronized (this) {
	 if (!is_queued) {
	    SwingUtilities.invokeLater(this);
	    is_queued = true;
	  }
       }
    }

   @Override public void run() {
      if (!auto_scroll) return;

      synchronized (this) {
	 is_queued = false;
       }

      AbstractDocument d = (AbstractDocument) text_pane.getDocument();
      d.readLock();
      try {
	 int len = d.getLength();
	 try {
	    Rectangle r = text_pane.modelToView(len-1);
	    if (r != null) {
	       Dimension sz = text_pane.getSize();
	       r.x = 0;
	       r.y += 20;
	       if (r.y + r.height > sz.height) r.y = sz.height;
	       text_pane.scrollRectToVisible(r);
	     }
	 }
	 catch (BadLocationException ex) {
	    BoardLog.logE("BDDT","Problem scrolling to end of console: " + ex);
	 }
       }
      finally {
	 d.readUnlock();
       }
    }

}	// end of inner class EndScroll




/********************************************************************************/
/*										*/
/*	Action									*/
/*										*/
/********************************************************************************/

private class AutoScrollAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JCheckBoxMenuItem itm = (JCheckBoxMenuItem) evt.getSource();
      auto_scroll = itm.getState();
    }

}	// end of inner class AutoScrollAction




}	// end of class BddtConsoleBubble




/* end of BddtConsoleBubble.java */

