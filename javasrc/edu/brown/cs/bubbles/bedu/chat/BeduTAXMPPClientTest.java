/********************************************************************************/
/*																										  */
/*		StudentXMPPBotTest.java																	  */
/*																										  */
/*	Bubbles for education																	     */
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

import java.util.Scanner;

import junit.framework.TestCase;
import static junit.framework.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.After;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;

import edu.brown.cs.bubbles.bedu.chat.BeduCourse.TACourse;

public class BeduTAXMPPClientTest {
private static BeduTAXMPPClient ta_client;
private static BeduTAXMPPClient ta_client2;
private static BeduTAXMPPClient ta_client3;
private static XMPPConnection	student_conn1;

// login names
private static String			  ta_login		= "codebubbles@jabber.org";
private static String			  student_login = "codebubbles2";



@BeforeClass public static void setUpOnce() throws XMPPException {
	System.out.println("Setting up once");
	//BeduCourse.TACourse c = new TACourse("testcourse", ta_login, "brownbears", "jabber.org");
	ta_client = new BeduTAXMPPClient(new TACourse("testcourse", ta_login, "brownbears", "jabber.org"));
	ta_client.connectAndLogin("TA1");

	ta_client2 = new BeduTAXMPPClient(new TACourse("testcourse", ta_login, "brownbears", "jabber.org"));
	//ta_client2.connectAndLogin("TA2");

	XMPPConnection.DEBUG_ENABLED = true;
	ConnectionConfiguration config = new ConnectionConfiguration("jabber.org", 5222);
	config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
	config.setSendPresence(true);

	student_conn1 = new XMPPConnection(config);
	student_conn1.connect();
	student_conn1.login(student_login, "brownbears");
}



@AfterClass public static void staticTearDown() throws XMPPException {
	 student_conn1.disconnect();
	 ta_client.disconnect();
	 ta_client2.disconnect();
	 ta_client3.disconnect();
}



/**
 * Tests the ability to receive a ticket via chat and store it as a
 * StudentTicket
 */
@Test public void testTicketReceiveAndAccept() throws XMPPException {
	System.out.println("Testing ticket receipt");
	Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", new MessageListener() {
		@Override public void processMessage(Chat c, Message m) {

		}
	});

	assertTrue(ta_client.getTickets().size() == 0);
	try {
		Thread.sleep(2000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	c.sendMessage("TICKET:this is a ticket");
	try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	assertTrue(ta_client.getTickets().size() == 1);
	BeduStudentTicket t = ta_client.getTickets().get(0);
	assertEquals(t.getText(), "this is a ticket");
	//assertEquals(t.getStudentJID(), student_login + "@jabber.org/Smack");
	
	ta_client.acceptTicketAndAlertPeers(t);
	assertTrue(ta_client.getTickets().size() == 0);
}

@Test public void testTicketForwardAndAccept() throws XMPPException, InterruptedException
{
   Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null);
   ta_client2.connectAndLogin("TA2");
   
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);
   
   c.sendMessage("TICKET:derp");
   Thread.sleep(5000);
   assertTrue(ta_client.getTickets().size() == 1);
   assertTrue(ta_client2.getTickets().size() == 1);
   assertEquals(ta_client.getTickets().get(0),ta_client2.getTickets().get(0));
   
   ta_client2.acceptTicketAndAlertPeers(ta_client2.getTickets().get(0));
   Thread.sleep(5000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);

}

@Test public void testInitialTicketForwards() throws XMPPException, InterruptedException
{
   System.out.println("Testing initial ticket forwarding...");
   ta_client3 = new BeduTAXMPPClient(new TACourse("testcourse", ta_login, "brownbears", "jabber.org"));
   Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null);

   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client3.getTickets().size() == 0);
   c.sendMessage("TICKET:1");
   c.sendMessage("TICKET:2");

   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 2);
   assertTrue(ta_client3.getTickets().size() == 0);
   
   ta_client3.connectAndLogin("TA3");
   Thread.sleep(2000);
   assertTrue(ta_client.getTickets().size() == 2);
   assertTrue(ta_client3.getTickets().size() == 2);
   ta_client.acceptTicketAndAlertPeers(ta_client.getTickets().get(0));
   ta_client3.acceptTicketAndAlertPeers(ta_client.getTickets().get(0));
   Thread.sleep(2000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client3.getTickets().size() == 0);
   
}


}
