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


import static junit.framework.Assert.*;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import edu.brown.cs.bubbles.bedu.chat.BeduCourse.TACourse;
import edu.brown.cs.bubbles.bgta.BgtaChat;

public class BeduTAXMPPClientTest {
private static BeduTAXMPPClient ta_client;
private static BeduTAXMPPClient ta_client2;
private static BeduTAXMPPClient ta_client3;
private static XMPPConnection	student_conn1;
private static XMPPConnection student_conn2;

// login names
private static String			  ta_login		= "codebubbles@jabber.org";
private static String			  student_login = "codebubbles2";
private static String           student2_login = "codebubbles3";

//result bools for the routing test that have
//to be members so we can set them inside
//anonymous message listeners
private boolean s1ToT2Received = false;
private boolean t2ToS1Received = false;
private boolean t1ToS2Received = false;
private boolean s2ToT1Received = false;


@BeforeClass public static void setUpOnce() throws XMPPException {
   //BeduChatFactory.DEBUG = true;
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
	
	BgtaChat bc = ta_client.acceptTicketAndAlertPeers(t);
	ta_client.endChatSession(bc);
	assertTrue(ta_client.getTickets().size() == 0);
}

@Test public void testTicketForwardAndAccept() throws XMPPException, InterruptedException
{
   System.out.println("Testing forward and accept");
   Chat c = student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null);
   ta_client2.connectAndLogin("TA2");
   
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);
   
   c.sendMessage("TICKET:tick");
   Thread.sleep(5000);
   assertTrue(ta_client.getTickets().size() == 1);
   assertTrue(ta_client2.getTickets().size() == 1);
   assertEquals(ta_client.getTickets().get(0),ta_client2.getTickets().get(0));
   
   BgtaChat bc = ta_client2.acceptTicketAndAlertPeers(ta_client2.getTickets().get(0));
   Thread.sleep(5000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);
   ta_client2.endChatSession(bc);

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
   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 2);
   assertTrue(ta_client3.getTickets().size() == 2);
   BgtaChat bc1 = ta_client.acceptTicketAndAlertPeers(ta_client.getTickets().get(0));
   BgtaChat bc3 = ta_client3.acceptTicketAndAlertPeers(ta_client.getTickets().get(0));
   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client3.getTickets().size() == 0);
   ta_client.endChatSession(bc1);
   ta_client3.endChatSession(bc3);
   
}

@Test public void testMessageRouting() throws Exception
{
   /**
    * With 2 TAs and 2 student connections have the two students
    * send tickets to different TAs, have TAs accept tickets from students
    * from whom they didn't receive the intial ticket and then try to send messages
    * in both directions once the sessions are established and check if the 
    * messages reach their destinations 
    */
   System.out.println("Testing correct routing of messages");
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);
   
   ConnectionConfiguration config = new ConnectionConfiguration("jabber.org", 5222);
   config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
   config.setSendPresence(true);

   student_conn2 = new XMPPConnection(config);
   student_conn2.connect();
   student_conn2.login(student2_login, "brownbears");

   
   
   Chat s1ToT2 = student_conn1.getChatManager().createChat("codebubbles@jabber.org", new MessageListener()
   {
      @Override
      public void processMessage(Chat c, Message m)
      {
         if(m.getBody().equals("t2ToS1"))
             t2ToS1Received = true;   
      }  
   });
   
   Chat s2ToT1 = student_conn2.getChatManager().createChat("codebubbles@jabber.org", new MessageListener(){
      @Override
      public void processMessage(Chat c, Message m)
      {
         if(m.getBody().equals("t1ToS2"))
            t1ToS2Received = true;
      }
   });
   
   student_conn1.getChatManager().createChat("codebubbles@jabber.org/TA1", null).sendMessage("TICKET:s1ToT2"); //hashcode = -954750633
   Thread.sleep(1000);
   student_conn2.getChatManager().createChat("codebubbles@jabber.org/TA2",null).sendMessage("TICKET:s2ToT1"); //hash = -953827113

   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 2);
   assertTrue(ta_client2.getTickets().size() == 2);
   
   BgtaChat t1ToS2 = ta_client.acceptTicketAndAlertPeers(new BeduStudentTicket("s2ToT1", new Date(), student2_login + "@jabber.org"));   
   BgtaChat t2ToS1 = ta_client2.acceptTicketAndAlertPeers(new BeduStudentTicket("s1ToT2", new Date(), student_login + "@jabber.org"));
   
   Thread.sleep(3000);
   assertTrue(ta_client.getTickets().size() == 0);
   assertTrue(ta_client2.getTickets().size() == 0);
   
   PipedOutputStream pipeErr = new PipedOutputStream();
   PipedInputStream pipeIn = new PipedInputStream(pipeErr);
   System.setErr(new PrintStream(pipeErr));

   
   t2ToS1.sendMessage("t2ToS1");
   Thread.sleep(1000);
   t1ToS2.sendMessage("t1ToS2");
   Thread.sleep(1000);
   s1ToT2.sendMessage("s1toT2");
   Thread.sleep(1000);
   s2ToT1.sendMessage("s2toT1");
   Thread.sleep(1000);
   
   byte[] errbuf = new byte[300];
   pipeIn.read(errbuf);
   
   assertEquals("BEDU:codebubbles@jabber.org/TA2:Student message:codebubbles2@jabber.org/Smack:s1toT2\n"
       + "BEDU:codebubbles@jabber.org/TA1:Student message:codebubbles3@jabber.org:s2toT1",
       new String(errbuf).trim());
   assertTrue(t2ToS1Received);
   assertTrue(t1ToS2Received);
}
}
