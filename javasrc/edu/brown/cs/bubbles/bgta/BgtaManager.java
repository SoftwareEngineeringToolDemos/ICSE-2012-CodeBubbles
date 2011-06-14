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

import edu.brown.cs.bubbles.bgta.BgtaConstants.*;
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

import java.util.*;






class BgtaManager implements PacketListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private XMPPConnection		the_connection;
private static XMPPConnection	stat_con;

protected String		user_name;
protected String		user_password;
protected String		user_server;
protected boolean 		being_saved;

protected Vector<BgtaBubble>	existing_bubbles;
protected Vector<BgtaChat>		existing_chats;
protected BgtaRoster			the_roster;
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
   existing_chats = new Vector<BgtaChat>();
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
   existing_chats = new Vector<BgtaChat>();
   being_saved = false;
   roster_listener = null;
}

BgtaManager() { }



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BgtaRoster getRoster()				{ return the_roster; }

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
   String serv, host;
   if (server.equals("gmail.com")) {
      serv = server;
      host = "talk.google.com";
    }
   else if (server.equals("chat.facebook.com")) {
      serv = server;
      host = server;
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
      config = new ConnectionConfiguration(serv,5222);
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
   if (!the_connection.isAuthenticated()) throw new XMPPException("Could not login to server.");

   Message m = new Message();
   the_connection.addPacketListener(this, new PacketTypeFilter(m.getClass()));
   roster_listener = new BgtaRosterListener();
   the_connection.getRoster().addRosterListener(roster_listener);
   the_roster = new BgtaXMPPRoster(the_connection.getRoster());
}


/********************************************************************************/
/*										*/
/*	Connection methods							*/
/*										*/
/********************************************************************************/

BgtaChat startChat(String username,MessageListener list,BgtaBubble using)
{
   if (!hasChat(username)) {
	  Chat ch = the_connection.getChatManager().createChat(username, list);
	  existing_bubbles.add(using);
	  BgtaXMPPChat chat = new BgtaXMPPChat(ch,list);
	  existing_chats.add(chat);
	  return chat;
    }
   else
	  return getExistingChat(username);
}



void addDuplicateBubble(BgtaBubble dup)
{
   existing_bubbles.add(dup);
   return;
}



void updateBubble(BgtaBubble up)
{
//   if (hasBubble(up.getUsername())) {
//      String text = null;
//      Document doc = up.getLog().getDocument();
//      try {
//	 text = doc.getText(0, doc.getLength());
//	 doc = getExistingBubble(up.getUsername()).getLog().getDocument();
//	 String curr = doc.getText(0, doc.getLength());
//	 if (curr.indexOf(text) < 0) {
//	    doc.insertString(0, text, null);
//	  }
//       }
//      catch (Throwable t) {
//	 BoardLog.logE("BGTA", "problem updating chat bubble", t);
//       }
//      up.getLog().setDocument(doc);
//    }
   if (hasBubble(up.getUsername())) {
	  if (!up.isListener())
	 up.getLog().setDocument(getExistingBubble(up.getUsername()).getLog().getDocument());
	  else {
	 for (BgtaBubble bub : existing_bubbles) {
		bub.getLog().setDocument(up.getLog().getDocument()); 
	  }
	   }
    }
   existing_bubbles.add(up);
}



boolean hasBubble(String username)
{
   for (BgtaBubble tbb : existing_bubbles) {
      String s = tbb.getUsername();
      if (s.equals(username)) return true;
    }
   return false;
}



BgtaBubble getExistingBubble(String username)
{
   for (BgtaBubble tbb : existing_bubbles) {
      String s = tbb.getUsername();
      if (s.equals(username)) return tbb;
    }
   return null;
}



void removeBubble(BgtaBubble bub)
{
   existing_bubbles.removeElement(bub);
   return;
}



boolean hasChat(String username)
{
   for (BgtaChat chat : existing_chats) {
	  if (chat.getUser().equals(username))
		 return true;
    }
   return false;
}



BgtaChat getExistingChat(String username)
{
   for (BgtaChat chat : existing_chats) {
	  if (chat.getUser().equals(username))
		 return chat;
    }
   return null;
}



void removeChat(BgtaChat chat,MessageListener list)
{
   if (((BgtaXMPPChat) chat).isListener(list) && hasBubble(chat.getUser())) {
	  BgtaBubble bub = getExistingBubble(chat.getUser());
      ((BgtaXMPPChat) chat).exchangeListeners(bub.getLog());
      bub.makeActive();
    }
   if (chat.close())
      existing_chats.removeElement(chat);
}



void disconnect()
{
   the_connection.disconnect();
//   for (BgtaBubble bub : existing_bubbles) {
//	  bub.disposeBubble();
//    }
//   existing_bubbles.clear();
   existing_chats.clear();
   roster_listener = null;
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
	 if (with.equals(tbb.getUsername())) {
	    if (!tbb.isPreview()) return;
	    receive = false;
	  }
       }
      BgtaBubble bb = BgtaFactory.createRecievedChatBubble(with, this);
      if (receive) bb.recieveMessage(new BgtaXMPPMessage((Message) pack));
    }
}



/********************************************************************************/
/*										*/
/*	Service methods 							*/
/*										*/
/********************************************************************************/
@Deprecated
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
@Deprecated
void register(String username,String password,String service) throws XMPPException
{
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


@Deprecated
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
}


@Deprecated
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

   @Override public void presenceChanged(Presence pres) { }

   @Override public void entriesAdded(Collection<String> arg0) {
      the_repository.removeManager(BgtaManager.this);
      the_repository.addNewRep(new BgtaBuddyRepository(BgtaManager.this));
    }

   @Override public void entriesDeleted(Collection<String> arg0) {
      the_repository.removeManager(BgtaManager.this);
      the_repository.addNewRep(new BgtaBuddyRepository(BgtaManager.this));
    }

   @Override public void entriesUpdated(Collection<String> arg0) { }

}	// end of inner class BgtaRosterListener



class BgtaXMPPRoster implements BgtaRoster {
	
   private Roster       our_roster;
   
   BgtaXMPPRoster(Roster roster) {
      this.our_roster = roster;
    }
   
   @Override public BgtaRosterEntry getEntry(String username) {
      return new BgtaXMPPRosterEntry(this.our_roster.getEntry(username));
    }
   
   @Override public Collection<BgtaXMPPRosterEntry> getEntries() {
	  Collection<RosterEntry> entries = null;
	  Collection<RosterGroup> groups = this.our_roster.getGroups();
	  List<BgtaXMPPRosterEntry> toreturn = new Vector<BgtaXMPPRosterEntry>();
	  if (!groups.isEmpty()) {
		  for (RosterGroup group : groups) {
			entries = group.getEntries();
			for (RosterEntry entry : entries) {
				toreturn.add(new BgtaXMPPRosterEntry(entry));
			 }
		   }
	   }
	  else {
		 entries = this.our_roster.getEntries();
		 for (RosterEntry entry : entries) {
			toreturn.add(new BgtaXMPPRosterEntry(entry));
		  }
	   }
      return toreturn.subList(0, toreturn.size());
    }
   
   @Override public Presence getPresence(String username) {
       return this.our_roster.getPresence(username);
    }
   
}   // end of inner class BgtaXMPPRoster



class BgtaXMPPRosterEntry implements BgtaRosterEntry {
	
   private RosterEntry  the_entry;
   
   BgtaXMPPRosterEntry(RosterEntry entry) {
      the_entry = entry;
    }
   
   @Override public String getName() {
      String name = null;
	  if (the_entry.getName() != null)
         name = the_entry.getName();
	  else
	     name = the_entry.getUser();
      int idx = name.indexOf("@");
      if (idx > 0)
          name = name.substring(0, idx);
      while (name.indexOf(".") != -1) {
         if (name.charAt(name.indexOf(".") + 1) != ' ') {
            String back = name.substring(name.indexOf(".") + 1);
         	String front = name.substring(0, name.indexOf("."));
         	name = front + " " + back;
          }
         else {
            String back = name.substring(name.indexOf(".") + 1);
            String front = name.substring(0, name.indexOf("."));
            name = front + back;
          }   
       }
      return name;
    }
   
   @Override public String getUser() {
      return the_entry.getUser();
    }
   
}   // end of inner class BgtaXMPPRosterEntry



class BgtaXMPPChat implements BgtaChat {
	
   private Chat				the_chat;
   private MessageListener  the_listener;
   private int				current_uses;
   
   BgtaXMPPChat(Chat ch,MessageListener list) {
	  the_chat = ch;
	  the_listener = list;
	  current_uses = 1;
    }
	
   @Override public String getUser() {
	  return the_chat.getParticipant();
	}
   
   @Override public void sendMessage(String message) throws XMPPException {
	  the_chat.sendMessage(message);
    }
   
   @Override public boolean close() {
	   if (--current_uses < 1) {
		  current_uses = 0;
		  the_chat.removeMessageListener(the_listener);
		  return true;
	   }
	   return false;
    }
   
   @Override public void increaseUseCount() { current_uses++; }
   
   @Override public boolean isListener(Object list) {
	  return list.equals((MessageListener) the_listener);
    }
   
   @Override public void exchangeListeners(Object list) {
	  MessageListener listener = (MessageListener) list;
	  the_chat.addMessageListener(listener);
	  the_chat.removeMessageListener(the_listener);
	  the_listener = listener;
    }
   
   Chat getChat() { return the_chat; }
   
}   // end of inner class BgtaXMPPChat



class BgtaXMPPMessage implements BgtaMessage {
	
   private Message			the_message;
	
   BgtaXMPPMessage(Message mess) { the_message = mess;	}
	
   @Override public String getBody() {
	  return the_message.getBody();
    }
   
   @Override public String getFrom() {
	  return the_message.getFrom();
    }
   
   @Override public String getTo() {
	  return the_message.getTo();
    }
   
   Message getMessage() {
	  return the_message;
    }
   
}   // end of inner class BgtaXMPPMessage



}	// end of class BgtaManager



/* end of BgtaManager.java */