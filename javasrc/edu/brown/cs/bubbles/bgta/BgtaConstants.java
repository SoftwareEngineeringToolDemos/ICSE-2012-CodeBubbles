/********************************************************************************/
/*										*/
/*		BgtaConstants.java						*/
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

import edu.brown.cs.bubbles.bass.BassConstants;

import java.awt.Color;
import java.util.Collection;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;




interface BgtaConstants extends BassConstants {



/********************************************************************************/
/*										*/
/*	Constants for gradient background					*/
/*										*/
/********************************************************************************/

Color	BGTA_BUBBLE_TOP_COLOR	= Color.white;

Color	BGTA_BUBBLE_BOTTOM_COLOR     = new Color(126,206,250);

Color	BGTA_ALT_BUBBLE_TOP_COLOR    = Color.white;

Color	BGTA_ALT_BUBBLE_BOTTOM_COLOR = new Color(46,139,87);




/********************************************************************************/
/*										*/
/*	Constants for properties						*/
/*										*/
/********************************************************************************/

String	BGTA_NUM_ACCOUNTS	    = "Bgta.mans.num";

String	BGTA_USERNAME_PREFIX	 = "Bgta.mans.username.";

String	BGTA_PASSWORD_PREFIX	 = "Bgta.mans.password.";

String	BGTA_SERVER_PREFIX	   = "Bgta.mans.server.";

String	BGTA_ALT_COLOR_UPON_RECIEVE  = "Bgta.altcolor";



/********************************************************************************/
/*										*/
/*	Constants for sort priority						*/
/*										*/
/********************************************************************************/

int	BGTA_CHATTY_PRIORITY	 = BASS_DEFAULT_SORT_PRIORITY - 8;

int	BGTA_AVAIL_PRIORITY	  = BASS_DEFAULT_SORT_PRIORITY - 7;

int	BGTA_IDLE_PRIORITY	   = BASS_DEFAULT_SORT_PRIORITY - 6;

int	BGTA_XA_PRIORITY	     = BASS_DEFAULT_SORT_PRIORITY - 5;

int	BGTA_DND_PRIORITY	    = BASS_DEFAULT_SORT_PRIORITY - 4;

int	BGTA_OFFLINE_PRIORITY	= BASS_DEFAULT_SORT_PRIORITY - 3;

int	BGTA_SPEC_ACCOUNT_PRIORITY   = BASS_DEFAULT_SORT_PRIORITY - 2;

int	BGTA_GEN_ACCOUNT_PRIORITY    = BASS_DEFAULT_SORT_PRIORITY - 1;



/********************************************************************************/
/*										*/
/*	Constants for remembering account information				*/
/*										*/
/********************************************************************************/

boolean BGTA_INITIAL_REM_SETTING     = false;



/********************************************************************************/
/*										*/
/*	Constants for metadata interpretation					*/
/*										*/
/********************************************************************************/

String	BGTA_METADATA_START	  = "///({";

String	BGTA_METADATA_FINISH	 = "})///";



/********************************************************************************/
/*										*/
/*	Constants for Buddy List Prefices					*/
/*										*/
/********************************************************************************/

String	BGTA_BUDDY_PREFIX	    = "@people.";



/********************************************************************************/
/*										*/
/*	Constants for Logging area						*/
/*										*/
/********************************************************************************/

int	BGTA_LOG_WIDTH	       = 275;

int	BGTA_LOG_HEIGHT       = (int) (0.75 * BGTA_LOG_WIDTH);

int	BGTA_DATA_BUTTON_WIDTH	     = 5;

int	BGTA_DATA_BUTTON_HEIGHT      = 3 * BGTA_DATA_BUTTON_WIDTH;



/********************************************************************************/
/*										*/
/*	Constants for task loading						*/
/*										*/
/********************************************************************************/

String	BGTA_TASK_DESCRIPTION	= "To open the new data, right click on the top bar and select \"Load Task\".\n";



/********************************************************************************/
/*										*/
/*	Enum for server values									*/
/*										*/
/********************************************************************************/
enum ChatServer {
	GMAIL("Gmail", "gmail.com", "@gmail.com", "talk.google.com", true),
	BROWN("Brown Gmail", "gmail.com", "@brown.edu", "talk.google.com", true),
	FACEBOOK("Facebook", "chat.facebook.com", "chat.facebook.com", "", false),
	JABBER("Jabber", "jabber.org", "@jabber.org", "", false),
	AIM("AIM", "aim", "", "", false);
	
	private String selector;
	private String server;
	private String display;
	private String host;
	private boolean ending;
	
	private ChatServer(String selector,String server,String display,String host, boolean ending) {
		this.selector = selector;
		this.server = server;
		if (display.equals(""))
			this.display = server;
		else
			this.display = display;
		if (host.equals(""))
			this.host = server;
		else
			this.host = host;
		this.ending = ending;
	}
	
	public String selector() { return selector; }
	
	public String server() { return server; }
	
	public String display() { return display; }
	
	public String host() { return host; }
	
	public boolean hasEnding() {
		return ending;
	}
	
	public String ending() {
		if (ending)
			return display;
		return "";
	}
	
	@Override public String toString() { return selector + " - " + display; }
}



/********************************************************************************/
/*										*/
/*	Interfaces for XMPP and OSCAR compatibility						*/
/*										*/
/********************************************************************************/

interface BgtaRoster {
	
   BgtaRosterEntry getEntry(String username);
   Collection<? extends BgtaRosterEntry> getEntries();
   Presence getPresence(String username);
   
}   // end of inner interface BgtaRoster



interface BgtaRosterEntry {
	
   String getName();
   String getUser();
   
}   // end of inner interface BgtaRosterEntry



interface BgtaConversation {
	
	String getUser();
	void sendMessage(String message) throws XMPPException;
	boolean close();
	void increaseUseCount();
	boolean isListener(Object list);
	void exchangeListeners(Object list);
	
}   // end of inner interface BgtaChat



interface BgtaMessage {
	
	String getBody();
	String getFrom();
	String getTo();
}



}	// end of inner interface BgtaConstants




/* end of BgtaConstants.java */
