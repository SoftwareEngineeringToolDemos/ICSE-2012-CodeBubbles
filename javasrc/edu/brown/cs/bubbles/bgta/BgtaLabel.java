/********************************************************************************/
/*										*/
/*		BgtaLabel.java							*/
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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.awt.Cursor;




class BgtaLabel extends JLabel implements PacketListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ImageIcon	my_icon;
private String		user_name;
private Presence	my_presence;


private static final long serialVersionUID = 1L;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaLabel(String username,Roster rost)
{
   super(username,BgtaManager.iconFor(rost.getPresence(username)),JLabel.LEFT);
   my_presence = rost.getPresence(username);
   my_icon = (ImageIcon) getIcon();
   if (my_presence.getType() == Presence.Type.available && my_presence.getMode() != null) {
      setToolTipText(my_presence.getMode().toString());
   }
   else if (my_presence.getType() == Presence.Type.unavailable) {
      setToolTipText("Unavailable");
   }
   user_name = username;
   setHorizontalTextPosition(JLabel.RIGHT);
   setCursor(Cursor.getDefaultCursor());
   createToolTip();
}



/********************************************************************************/
/*										*/
/*	PacketListener								*/
/*										*/
/********************************************************************************/

@Override public void processPacket(Packet p)
{
   if (p instanceof Presence) {
      Presence pr = (Presence) p;
      String fromuser = pr.getFrom().substring(0, pr.getFrom().indexOf("/"));
      if (fromuser.equals(user_name)) {
	 my_presence = pr;
	 if (pr.getType() == Presence.Type.available) {
	    my_icon = (ImageIcon) BgtaManager.iconFor(my_presence);
	    setIcon(my_icon);
	    setToolTipText(my_presence.getMode().toString());
	 }
	 else if (pr.getType() == Presence.Type.unavailable) {
	    my_icon = (ImageIcon) BgtaManager.iconFor(my_presence);
	    setIcon(my_icon);
	    setToolTipText("unavailable");
	 }
      }
   }
}



}	// end of class BgtaLabel



/* end of BgtaLabel.java */
