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

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class BgtaLoggingArea extends JTextPane implements BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Document	my_doc;
// private AttributeSet	is_bolded;
private AttributeSet	is_unbolded;
private AttributeSet	in_use;
private int		last_focused_caret_pos;

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
   // is_bolded = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, true);
   in_use = null;// is_unbolded;
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


@Override public void setDocument(Document doc)
{
   super.setDocument(doc);
   my_doc = doc;
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
/*	Metadata processing							*/
/*										*/
/********************************************************************************/

void processMetadata(String data)
{
    JButton accept = new JButton("Load Task to Task Shelf");
    Dimension d = new Dimension(BGTA_DATA_BUTTON_WIDTH,BGTA_DATA_BUTTON_HEIGHT);
    accept.setPreferredSize(d);
    accept.setSize(d);
    accept.setMinimumSize(d);
    Element xml = IvyXml.loadXmlFromURL(data);
    accept.addActionListener(new XMLListener(xml));
    setCaretPosition(my_doc.getLength());
    last_focused_caret_pos = getCaretPosition();
    insertComponent(accept);
}



/********************************************************************************/
/*										*/
/*	Button press methods							*/
/*										*/
/********************************************************************************/

void pressedButton(JButton button)
{
   try {
      my_doc.insertString(my_doc.getLength(), BGTA_TASK_DESCRIPTION, in_use);
    }
   catch (BadLocationException e) {}
   removeAll();
}



private class XMLListener implements ActionListener {

   private Element _xml;

   private XMLListener(Element xml) {
      _xml = xml;
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      BgtaFactory.addTaskToRoot(_xml);
      pressedButton((JButton) arg0.getSource());
    }

}	// end of private class XMLListener



}	// end of class BgtaLoggingArea




/* end of BgtaLoggingArea.java */
