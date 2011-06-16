/********************************************************************************/
/*										*/
/*		BgtaDraftingArea.java						*/
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




import org.jivesoftware.smack.XMPPException;

import edu.brown.cs.bubbles.bgta.BgtaConstants.*;

import javax.swing.JTextArea;

import java.awt.event.*;



class BgtaDraftingArea extends JTextArea {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BgtaConversation		my_chat;
private BgtaLoggingArea my_log;
private BgtaBubble	my_bubble;

private static final long serialVersionUID = 1L;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaDraftingArea(BgtaConversation ch,BgtaLoggingArea bla,BgtaBubble mybub)
{
   super(1,25);
   my_chat = ch;
   my_log = bla;
   my_bubble = mybub;
   setLineWrap(true);
   setWrapStyleWord(true);
   setOpaque(false);
   addKeyListener(new DraftingListener());
   addFocusListener(new FocusForLogListener());
}




/********************************************************************************/
/*										*/
/*	Sending methods 							*/
/*										*/
/********************************************************************************/

void send()
{
   boolean sent = true;
   try {
      my_chat.sendMessage(getText());
      my_log.logMessage(getText(), "Me: ");
   }
   catch (Exception e) {
	  //System.out.println(e.getMessage());
      sent = false;
   }
   if (sent) setText("");
   else setText("Message was not sent");
}



void send(String message)
{
   try {
      my_chat.sendMessage(message);
      my_log.logMessage(message, "Me: ");
      my_log.setCaretPosition(my_log.getDocument().getLength());
   }
   catch (XMPPException e) {}
}




/********************************************************************************/
/*										*/
/*	 Listeners for handling input						*/
/*										*/
/********************************************************************************/

private class DraftingListener extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	 send();
	 // Necessary because otherwise sending doesn't look good.
	 e.setKeyCode(KeyEvent.VK_LEFT);
       }
    }

}	// end of inner class DraftingListener




private class FocusForLogListener implements FocusListener {

   @Override public void focusGained(FocusEvent e) {
      my_log.unbold();
      my_bubble.setAltColorIsOn(false);
    }

   @Override public void focusLost(FocusEvent e) {
      if (my_bubble.reloadAltColor()) {
	 my_log.bold();
	 // my_bubble.setAltColorIsOn(true);
       }
    }

}	// end of inner class FocusForLogListener




}	// end of class BgtaDraftingArea



/* end of BgtaDraftingArea.java */
