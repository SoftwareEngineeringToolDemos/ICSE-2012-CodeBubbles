/********************************************************************************/
/*										*/
/*		BgtaManager.java						*/
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

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.text.Document;

import java.util.*;




class BgtaManager implements PacketListener {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private XMPPConnection		the_connection;
private static XMPPConnection	stat_con;

private String			user_name;
private String			user_password;
private String			user_server;
private boolean 		being_saved;

private Vector<BgtaBubble>	existing_bubbles;
private RosterListener		roster_listener;
private BgtaRepository		the_repository;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaManager(String username,String password,String server,BgtaRepository repo)
	throws XMPPException
{
   this(username,password,server);
   the_repository = repo;
}



BgtaManager(String username,String password,String server) throws XMPPException
{
   login(username, password, server);
   user_name = username;
   user_password = password;
   user_server = server;
   existing_bubbles = new Vector<BgtaBubble>();
   being_saved = false;
   roster_listener = null;
}



BgtaManager(String username,String password) throws XMPPException
{
   login(username, password);
   user_name = username;
   user_password = password;
   user_server = "";
   existing_bubbles = new Vector<BgtaBubble>();
   being_saved = false;
   roster_listener = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Roster getRoster()				{ return the_connection.getRoster(); }

String getUsername()				{ return user_name; }

String getPassword()				{ return user_password; }

String getServer()				{ return user_server; }

boolean isBeingSaved()				{ return being_saved; }


void setBeingSaved(boolean bs)			{ being_saved = bs; }



boolean isEquivalent(String un,String pa,String se)
{
   return un.equals(user_name) && pa.equals(user_password) && se.equals(user_server);
}


/********************************************************************************/
/*										*/
/*	Presence listener							*/
/*										*/
/********************************************************************************/

void addPresenceListener(PacketListener p)
{
   Presence pr = new Presence(Presence.Type.available);
   the_connection.addPacketListener(p, new PacketTypeFilter(pr.getClass()));
}



/********************************************************************************/
/*										*/
/*	Login methods								*/
/*										*/
/********************************************************************************/

void login(String username,String password) throws XMPPException
{
   login(username, password, "gmail.com");
}


void login() throws XMPPException
{
   login("codebubbles4tester@gmail.com", "bubbles4");
}


void login(String username,String password,String server) throws XMPPException
{
   // XMPPConnection.DEBUG_ENABLED = true;
   String serv, host;
   if (server.equals("gmail.com")) {
      serv = server;
      host = "talk.google.com";
    }
   else if (server.equals("chat.facebook.com")) {
      serv = server;
      host = "chat.facebook.com";
    }
   else if (server.equals("jabber.org")) {
      serv = server;
      host = server;
    }
   else {
      serv = "gmail.com";
      host = "talk.google.com";
    }
   ConnectionConfiguration config = null;
   if (serv.equals("chat.facebook.com")) {
      SASLAuthentication.registerSASLMechanism("DIGEST-MD5",
						  BgtaSASLDigestMD5Mechanism.class);
      config = new ConnectionConfiguration("chat.facebook.com",5222);
      config.setSASLAuthenticationEnabled(true);
    }
   else {
      config = new ConnectionConfiguration(host,5222,serv);
      config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
    }
   the_connection = new XMPPConnection(config);
   stat_con = the_connection;

   the_connection.connect();
   the_connection.login(username, password);

   Message m = new Message();
   the_connection.addPacketListener(this, new PacketTypeFilter(m.getClass()));
}



/********************************************************************************/
/*										*/
/*	Connection methods							*/
/*										*/
/********************************************************************************/

Chat startChat(String username,MessageListener list,BgtaBubble using)
{
   Chat ch = the_connection.getChatManager().createChat(username, list);
   existing_bubbles.add(using);
   return ch;
}



void addDuplicateBubble(BgtaBubble dup)
{
   existing_bubbles.add(dup);
}



void updateBubble(BgtaBubble up)
{
   if (hasBubble(up.getUsername())) {
      String text = null;
      Document doc = up.getLog().getDocument();
      try {
	 text = doc.getText(0, doc.getLength());
	 doc = getExistingBubble(up.getUsername()).getLog().getDocument();
	 String curr = doc.getText(0, doc.getLength());
	 if (curr.indexOf(text) < 0) {
	    doc.insertString(0, text, null);
	  }
       }
      catch (Throwable t) {
	 BoardLog.logE("BGTA", "problem updating chat bubble", t);
       }
      up.getLog().setDocument(doc);
    }
   existing_bubbles.add(up);
}



boolean hasBubble(String username)
{
   for (BgtaBubble tbb : existing_bubbles) {
      String s = tbb.getBuddy();
      if (s.equals(username)) return true;
    }
   return false;
}



BgtaBubble getExistingBubble(String username)
{
   for (BgtaBubble tbb : existing_bubbles) {
      String s = tbb.getBuddy();
      if (s.equals(username)) return tbb;
    }
   return null;
}



void removeBubble(BgtaBubble bub)
{
   existing_bubbles.removeElement(bub);
}



void disconnect()
{
   the_connection.disconnect();
}



/********************************************************************************/
/*										*/
/*	Presence methods							*/
/*										*/
/********************************************************************************/

static Presence getPresence(String conname)
{
   return stat_con.getRoster().getPresence(conname);
}


static Icon iconFor(Presence pres)
{
   if (pres == null) return new ImageIcon();
   if (pres.getType() == Presence.Type.available) {
      if (pres.getMode() == null || pres.getMode() == Presence.Mode.available) return BoardImage
	 .getIcon("greenled");
      switch (pres.getMode()) {
	 case away:
	 case xa:
	    return BoardImage.getIcon("yahoo_idle");
	 case dnd:
	    return BoardImage.getIcon("mix_record");
	 case chat:
	    return BoardImage.getIcon("greenled");
	 default:
	    return BoardImage.getIcon("greenled");
       }
    }
   else if (pres.getType() == Presence.Type.unavailable) {
      return BoardImage.getIcon("mini_circle");
    }
   else {
      return new ImageIcon();
    }
}



/********************************************************************************/
/*										*/
/*	Packet Listener 							*/
/*										*/
/********************************************************************************/

@Override public void processPacket(Packet pack)
{
   if (pack instanceof Message) {
      String with = pack.getFrom();
      if (with.lastIndexOf("/") != -1) with = with.substring(0, with.lastIndexOf("/"));
      if (with.equals(user_name)) return;
      boolean receive = true;
      for (BgtaBubble tbb : existing_bubbles) {
	 if (with.equals(tbb.getBuddy())) {
	    if (!tbb.isPreview()) return;
	    receive = false;
	  }
       }
      BgtaBubble bb = BgtaFactory.createRecievedChatBubble(with, this);
      if (receive) bb.recieveMessage((Message) pack);
    }
}



/********************************************************************************/
/*										*/
/*	Service methods 							*/
/*										*/
/********************************************************************************/

private String getGateway(String service)
{
   try {
      ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(the_connection);
      // jabber.yeahnah.co.nz is a free server which provides gateways to several legacy
      // IM services
      DiscoverItems items = sdm.discoverItems("jabber.yeahnah.co.nz");
      Iterator<DiscoverItems.Item> itemsIterator = items.getItems();
      while (itemsIterator.hasNext()) {
	 DiscoverItems.Item item = itemsIterator.next();
	 DiscoverInfo info = sdm.discoverInfo(item.getEntityID(), item.getNode());
	 if (info.containsFeature("jabber:iq:gateway")) {
	    if (item.getEntityID().contains(service.toLowerCase())) {
	       return item.getEntityID();
	     }
	  }
       }
      return null;
    }
   catch (Throwable t) {
      BoardLog.logE("BGTA", "Failed to find gateway for legacy service: " + service, t);
      return null;
    }
}



/********************************************************************************/
/*										*/
/*	Registration management 						*/
/*										*/
/********************************************************************************/

void register(String username,String password,String service) throws XMPPException
{
   if (roster_listener == null) {
      roster_listener = new BgtaRosterListener(this);
      the_connection.getRoster().addRosterListener(roster_listener);
    }
   String gateway = getGateway(service);
   Registration registration = new Registration();
   registration.addExtension(new GatewayRegistrationExtension());
   registration.setType(IQ.Type.SET);
   registration.setTo(gateway);
   registration.setFrom(the_connection.getUser());

   Map<String, String> attributes = new HashMap<String, String>();
   attributes.put("username", username);
   attributes.put("password", password);
   registration.setAttributes(attributes);

   PacketCollector collector = the_connection.createPacketCollector(new PacketIDFilter(
								       registration.getPacketID()));
   the_connection.sendPacket(registration);

   IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
   collector.cancel();
   if (response == null) throw new XMPPException("Server timed out.");
   if (response.getType() == IQ.Type.ERROR) throw new XMPPException(
      "Error registering user",response.getError());
   if (!the_connection.getRoster().contains(gateway)) the_connection.getRoster().createEntry(
      gateway, gateway, null);

   Presence pres = new Presence(Presence.Type.subscribe);
   pres.setTo(gateway);
   pres.setFrom(the_connection.getUser());
   the_connection.sendPacket(pres);
   pres.setType(Presence.Type.available);
   the_connection.sendPacket(pres);
}



void unregister(String username,String service) throws XMPPException
{
   String gateway = getGateway(service);
   Registration registration = new Registration();
   registration.addExtension(new GatewayRegistrationExtension());
   registration.setType(IQ.Type.SET);
   registration.setTo(gateway);
   registration.setFrom(the_connection.getUser());
   Map<String, String> attributes = new HashMap<String, String>();
   attributes.put("remove", null);
   registration.setAttributes(attributes);

   PacketCollector collector = the_connection.createPacketCollector(new PacketIDFilter(
								       registration.getPacketID()));
   the_connection.sendPacket(registration);
   IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
   collector.cancel();
   if (response == null) throw new XMPPException("Server timed out.");
   if (response.getType() == IQ.Type.ERROR) throw new XMPPException(
      "Error unregistering user",response.getError());
   if (the_connection.getRoster().contains(gateway)) the_connection.getRoster().removeEntry(
      the_connection.getRoster().getEntry(gateway));
   Roster roster = the_connection.getRoster();
   Collection<RosterEntry> buddies = roster.getEntries();
   for (RosterEntry buddy : buddies) {
      if (buddy.getUser().contains(gateway)) {
	 roster.removeEntry(buddy);
       }
    }
   roster.removeRosterListener(roster_listener);
   roster_listener = null;
}




private class GatewayRegistrationExtension implements PacketExtension {

   @Override public String getElementName() {
      return "x";
    }

   @Override public String getNamespace() {
      return "jabber:iq:gateway:register";
    }

   @Override public String toXML() {
      return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\"/>";
    }

}	// end of inner class GatewayRegistrationExtension



private class BgtaRosterListener implements RosterListener {

   private BgtaManager _manager;

   private BgtaRosterListener(BgtaManager man) {
      _manager = man;
    }

   @Override public void presenceChanged(Presence pres) 	{ }

   @Override public void entriesAdded(Collection<String> arg0) {
      the_repository.removeManager(_manager);
      the_repository.addNewRep(new BgtaBuddyRepository(_manager));
    }

   @Override public void entriesDeleted(Collection<String> arg0) {
      the_repository.removeManager(_manager);
      the_repository.addNewRep(new BgtaBuddyRepository(_manager));
    }

   @Override public void entriesUpdated(Collection<String> arg0) { }

}	// end of inner class BgtaRosterListener



}	// end of class BgtaManager




/* end of BgtaManager.java */
