/********************************************************************************/
/*										*/
/*		BgtaAimManager .java							*/
/*										*/
/*	description of class							*/
/*										*/
/*	Written by								*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bgta;

import edu.brown.cs.bubbles.bgta.BgtaConstants.*;
import edu.brown.cs.bubbles.board.BoardLog;

import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.AimSession;
import net.kano.joustsim.oscar.DefaultAppSession;
import net.kano.joustsim.oscar.AimConnectionProperties;
import net.kano.joustsim.oscar.OpenedServiceListener;
import net.kano.joustsim.oscar.State;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.ssi.Buddy;
import net.kano.joustsim.oscar.BuddyInfo;
import net.kano.joustsim.oscar.oscar.service.ssi.MutableBuddyList;
import net.kano.joustsim.oscar.oscar.service.ssi.Group;
import net.kano.joustsim.oscar.oscar.service.ssi.SsiService;
import net.kano.joustsim.oscar.oscar.service.icbm.*;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


class BgtaAimManager extends BgtaManager
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private AimConnection	       the_connection;
private IcbmListener		   conversation_listener;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaAimManager(String username,String password,String server) throws XMPPException
{
   login(username, password);
   user_name = username;
   user_password = password;
   user_server = server;
   existing_bubbles = new Vector<BgtaBubble>();
   existing_chats = new Vector<BgtaChat>();
   being_saved = false;
}



@Override void login(String username,String password) throws XMPPException
{
   Screenname screenname = new Screenname(username);
   AimSession aimSession = new DefaultAppSession().openAimSession(screenname);
   AimConnectionProperties props = new AimConnectionProperties(screenname,password);
   props.setLoginHost("login.messaging.aol.com");
   props.setLoginPort(5190);
   the_connection = aimSession.openConnection(props);
   the_connection.addOpenedServiceListener(
      new OpenedServiceListener() {

	 @Override
	    public void closedServices(AimConnection arg0,
					  Collection<? extends Service> arg1) { }

	 @Override
	    public void openedServices(AimConnection arg0,
					  Collection<? extends Service> arg1) { }
	
       });
   the_connection.connect();
   if (the_connection.getState() == State.FAILED) {
      BoardLog.logE("BGTA", "Error connecting to AIM via OSCAR protocol.");
      throw new XMPPException("Error connecting to AIM server.");
    }
   try {
      Thread.sleep(2000);
    }
   catch (InterruptedException e) {
      //do nothing
    }
   IcbmService icbm = the_connection.getIcbmService();
   if (icbm == null) {
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) {
	 //do nothing
       }
    }
   if (icbm == null) {
      BoardLog.logE("BGTA", "Icbm service not available.");
      throw new XMPPException("Error connecting to AIM server.");
    }
   if (!icbm.isReady()) {
      BoardLog.logE("BGTA", "Icbm service is not ready.");
      throw new XMPPException("Error connecting to AIM server.");
    }
   icbm.removeIcbmListener(conversation_listener);
   conversation_listener = new AIMServiceListener();
   icbm.addIcbmListener(conversation_listener);
   OscarConnection con = the_connection.getInfoService().getOscarConnection();
   con.addGlobalServiceListener(
      new ServiceListener() {

	 @Override
	    public void handleServiceFinished(Service arg0) { }

	 @Override
	    public void handleServiceReady(Service arg0) { }
		
       });
   SsiService ssi = the_connection.getSsiService();
   if (ssi == null) {
      BoardLog.logE("BGTA", "Ssi service not available.");
      throw new XMPPException("Error connecting to AIM server.");
    }
   if (!ssi.isReady()) {
      BoardLog.logE("BGTA", "Ssi service not ready.");
      throw new XMPPException("Error connecting to AIM server.");
    }
   the_roster = new BgtaAIMRoster(ssi.getBuddyList());
}



@Override void disconnect()
{
   the_connection.disconnect();
   for (BgtaBubble bub : existing_bubbles) {
      bub.disposeBubble();
    }
   existing_bubbles.clear();
   existing_chats.clear();
}



@Override void removeChat(BgtaChat chat,MessageListener list) {
   if (chat.close())
      existing_chats.removeElement(chat);
}



@Override void addPresenceListener(PacketListener p) { }



@Override BgtaRoster getRoster() { return the_roster; }



@Override BgtaChat startChat(String username,MessageListener list,BgtaBubble using)
{
   if (!hasChat(username)) {
      Conversation con = the_connection.getIcbmService().getImConversation(new Screenname(username));
      AIMConversationListener listener = new AIMConversationListener();
      con.addConversationListener(listener);
      existing_bubbles.add(using);
      BgtaAIMChat chat = new BgtaAIMChat(con, listener);
      existing_chats.add(chat);
      return chat;
    }
   else
      return getExistingChat(username);
}



class AIMServiceListener implements IcbmListener {

   @Override public void buddyInfoUpdated(IcbmService service, Screenname sn,
					     IcbmBuddyInfo arg2) { }

   @Override public void newConversation(IcbmService service, Conversation conv) {
      if (!hasChat(conv.getBuddy().getFormatted())) {
	 conv.addConversationListener(new AIMConversationListener());
	 BgtaFactory.createRecievedChatBubble(conv.getBuddy().getFormatted(), BgtaAimManager.this);
       }
    }

   @Override public void sendAutomaticallyFailed(IcbmService service, Message message,
						    Set<Conversation> conv) { }

}	// end of inner class AIMServiceListener



class AIMConversationListener implements ConversationListener {

   @Override public void gotMessage(Conversation conv, MessageInfo minfo) {
      BgtaBubble bubble = getExistingBubble(conv.getBuddy().getFormatted());
      if (bubble == null) return;
      bubble.recieveMessage(new BgtaAIMMessage(minfo.getMessage()));
    }

   @Override public void sentMessage(Conversation conv,MessageInfo minfo) {
      //System.out.println("Message sent:" + minfo.getMessage().getMessageBody());
    }

   @Override
      public void canSendMessageChanged(Conversation arg0, boolean arg1) { }

   @Override
      public void conversationClosed(Conversation arg0) { }

   @Override
      public void conversationOpened(Conversation arg0) { }

   @Override
      public void gotOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

   @Override
      public void sentOtherEvent(Conversation arg0, ConversationEventInfo arg1) { }

}	// end of inner class AIMConversationAdapter



class BgtaAIMRoster implements BgtaRoster {

   private Map<String, BgtaAIMRosterEntry>  aim_buddies;

   BgtaAIMRoster(MutableBuddyList buddy_list) {
      aim_buddies = new ConcurrentHashMap<String, BgtaAIMRosterEntry>();
      for (Group group: buddy_list.getGroups()) {
	 for (Buddy buddy : group.getBuddiesCopy()) {
	    aim_buddies.put(buddy.getScreenname().getNormal(), new BgtaAIMRosterEntry(buddy));
	  }
       }
    }

   @Override public BgtaRosterEntry getEntry(String username) {
      boolean contains = aim_buddies.containsKey(username);
      if (!contains)
	 return null;
      return aim_buddies.get(username);
    }

   @Override public Collection<BgtaAIMRosterEntry> getEntries() {
      return aim_buddies.values();
    }

   @Override public Presence getPresence(String username) {
      BuddyInfo buddyInfo = the_connection.getBuddyInfoManager().getBuddyInfo(aim_buddies.get(username).getScreenname());
      if (buddyInfo.isOnline()) {
	 Presence pr = new Presence(Presence.Type.available);
	 pr.setMode(Presence.Mode.chat);
	 if (buddyInfo.isAway())
	    pr.setMode(Presence.Mode.away);
	 return pr;
       }
      else {
	 return new Presence(Presence.Type.unavailable);
       }
    }

}	// end of inner class BgtaAIMRoster



class BgtaAIMRosterEntry implements BgtaRosterEntry {

   private Buddy       the_entry;

   BgtaAIMRosterEntry(Buddy buddy) { the_entry = buddy; }

   @Override public String getName() {
      String name = null;
      if (the_entry.getAlias() != null)
	 name = the_entry.getAlias();
      else
	 name = the_entry.getScreenname().getFormatted();
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
      return the_entry.getScreenname().getNormal();
    }

   Screenname getScreenname() {
      return the_entry.getScreenname();
    }

}	// end of inner class BgtaAIMRosterEntry



class BgtaAIMChat implements BgtaChat {

   private Conversation 		the_chat;
   private ConversationListener the_listener;
   private int					current_uses;

   BgtaAIMChat(Conversation con,ConversationListener list) {
      the_chat = con;
      the_listener = list;
      current_uses = 1;
    }

   @Override public String getUser() {
      return the_chat.getBuddy().getFormatted();
    }

   @Override public void sendMessage(String message) throws XMPPException {
      the_chat.sendMessage(new SimpleMessage(makeHTML(message), false));
    }

   @Override public boolean close() {
      if (--current_uses < 1) {
	 current_uses = 0;
	 the_chat.close();
	 the_chat.removeConversationListener(the_listener);
	 return true;
       }
      return false;
    }

   @Override public void increaseUseCount() { current_uses++; }

   @Override public boolean isListener(Object list) {
      return list.equals((ConversationListener) the_listener);
    }

   @Override public void exchangeListeners(Object list) {
      ConversationListener listener = (ConversationListener) list;
      the_chat.addConversationListener(listener);
      the_chat.removeConversationListener(the_listener);
      the_listener = listener;
    }

   Conversation getConversation() { return the_chat; }

   private String replace(String input,String toreplace,String replacewith) {
      String current = input;
      int pos = current.indexOf(toreplace);
      if (pos != -1) {
	 current = current.substring(0,pos) + replacewith + replace(current.substring(pos + toreplace.length()),toreplace,replacewith);
       }
      return current;
    }

   private String makeHTML(String text) {
      String temp = text;
      temp = replace(temp,"&","&amp;");
      temp = replace(temp,"<","&lt;");
      temp = replace(temp,">","&gt;");
      temp = replace(temp,"\"","&qout;");
      temp = replace(temp,"\n","<br>");
      return "<html><body>" + temp + "</body></html>";
    }

}	// end of inner class BgtaAIMChat



class BgtaAIMMessage implements BgtaMessage {

   private Message		   the_message;

   BgtaAIMMessage(Message mess) { the_message = mess; }

   @Override public String getBody() {
      return stripHTML(the_message.getMessageBody());
    }

   @Override public String getFrom() { return ""; }

   @Override public String getTo() { return ""; }

   private String stripHTML(String text) {
      // "<.*?>" is a regular expression which should match individual HTML tags
      return text.replaceAll("<.*?>", "");
    }

}	// end of inner class BgtaAIMMessage



}	// end of class BgtaAimManager



/* end of BgtaAimManager.java */
