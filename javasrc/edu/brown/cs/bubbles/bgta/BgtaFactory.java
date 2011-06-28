/********************************************************************************/
/*										*/
/*		BgtaFactory.java						*/
/*										*/
/*	Bubbles attribute and property management main setup routine		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Ian Strickman		      */
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



package edu.brown.cs.bubbles.bgta;



import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bgta.educhat.CourseRepository;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;

import org.jivesoftware.smack.XMPPException;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;




/**
 * This class sets up the chat interface and provides calls to define chat bubbles
 **/

public class BgtaFactory implements BgtaConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static BgtaFactory		the_factory = null;
private static Vector<BgtaManager>	chat_managers;
private static BgtaRepository		buddy_list;
private static BoardProperties		login_properties;
private static BudaRoot 		my_buda_root;
private static boolean			rec_dif_back;
private static JMenu			metadata_menu;


static {
   chat_managers = new Vector<BgtaManager>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   login_properties = BoardProperties.getProperties("Bgta");

   // TODO: Needs to be altered to match spr's guidelines for property storage
   for (int i = 0; i < login_properties.getInt(BGTA_NUM_ACCOUNTS); i++) {
      try {
	 BgtaManager man = new BgtaManager(
		  login_properties.getProperty(BGTA_USERNAME_PREFIX + i),
		  login_properties.getProperty(BGTA_PASSWORD_PREFIX + i),
		  login_properties.getProperty(BGTA_PASSWORD_PREFIX + i));
	 man.setBeingSaved(true);
	 chat_managers.add(man);
      }
      catch (XMPPException e) {
	 System.err.println("BGTA: COULDN'T LOAD ACCOUNT FOR "
		  + login_properties.getProperty(BGTA_USERNAME_PREFIX + i));
      }

   }
   buddy_list = new BgtaRepository(chat_managers);
   rec_dif_back = login_properties.getBoolean(BGTA_ALT_COLOR_UPON_RECIEVE);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_PEOPLE, buddy_list);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER, buddy_list);
   BudaRoot.addBubbleConfigurator("BGTA", new BgtaConfigurator());
   
   CourseRepository.setup();
}


public static synchronized BgtaFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BgtaFactory();
    }
   return the_factory;
}


public static void initialize(BudaRoot br)
{
   my_buda_root = br;
   getFactory().addMetadataMenuItem();
}



/********************************************************************************/
/*										*/
/*	Property adding methods 						*/
/*										*/
/********************************************************************************/

static void addManagerProperties(String usnm,String psswd,String svr)
{
   int newnum = login_properties.getInt(BGTA_NUM_ACCOUNTS);
   login_properties.setProperty(BGTA_USERNAME_PREFIX + newnum, usnm);
   login_properties.setProperty(BGTA_PASSWORD_PREFIX + newnum, psswd);
   login_properties.setProperty(BGTA_SERVER_PREFIX + newnum, svr);
   ++newnum;
   login_properties.setProperty(BGTA_NUM_ACCOUNTS, newnum);
   try {
      login_properties.save();
   }
   catch (IOException e) {}
}


static void clearManagerProperties()
{
   login_properties.clear();
   login_properties.setProperty(BGTA_NUM_ACCOUNTS, 0);
   try {
      login_properties.save();
   }
   catch (IOException e) {}
}


static void altColorUponRecieve(boolean b)
{
   login_properties.setProperty(BGTA_ALT_COLOR_UPON_RECIEVE, b);
   rec_dif_back = b;
   try {
      login_properties.save();
   }
   catch (IOException e) {}
}


static void logoutAllAccounts()
{ }


static boolean logoutAccount(String username,String password,String server)
{
   for (BgtaManager man : chat_managers) {
      if (man.isEquivalent(username, password, server)) {
	 buddy_list.removeManager(man);
	 man.disconnect();
	 chat_managers.remove(man);
	 BassFactory.reloadRepository(buddy_list);
	 return true;
      }
   }
   return false;
}


@SuppressWarnings("deprecation")
static void registerUserViaGateway(String username,String password,String server)
{
   for (BgtaManager man : chat_managers) {
      if (man.isEquivalent("codebubbles4tester@gmail.com", "bubbles4", "gmail.com")) {
	 try {
	    man.register(username, password, server);
	    BassFactory.reloadRepository(buddy_list);
	 }
	 catch (Throwable t) {
	    BoardLog.logE("BGTA", "Problem registering user with legacy service: "
		     + server, t);
	 }
      }
   }
}


@SuppressWarnings("deprecation")
static void unregisterUserViaGateway(String username,String server)
{
   for (BgtaManager man : chat_managers) {
      if (man.isEquivalent("codebubbles4tester@gmail.com", "bubbles4", "gmail.com")) {
	 try {
	    man.unregister(username, server);
	    BassFactory.reloadRepository(buddy_list);
	 }
	 catch (Throwable t) {
	    BoardLog.logE("BGTA", "Problem unregistering user with legacy service: "
		     + server, t);
	 }
      }
   }
}


static BoardProperties getBgtaProperties()
{
   return login_properties;
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BgtaFactory()
{ }


/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

BudaBubble createChatBubble(String friendname,String myname,String password,String server)
{
   BgtaManager newman;
   BgtaBubble bb = null;
   try {
      newman = new BgtaManager(myname,password,server);
      chat_managers.add(newman);
      bb = new BgtaBubble(friendname,newman);
   }
   catch (XMPPException e) {}
   return bb;
}

public static List<String> getChatters()
{
   return buddy_list.getAllBuddyNames();
}


private BudaBubble createMetadataChatBubble(String friendname,String url)
{
   boolean isknownmanager = false;
   BgtaManager man = buddy_list.getBuddyInfo(friendname).getManager();
   for (BgtaManager m : chat_managers) {
      if (man == m) {
	 isknownmanager = true;
	 break;
      }
   }
   if (!isknownmanager) return null;
   BgtaBubble bb = new BgtaBubble(friendname,man);
   Rectangle vp = my_buda_root.getViewport();
   my_buda_root.add(bb, new BudaConstraint(vp.x,vp.y));
   bb.sendMessage("Here's my Data!");
   bb.sendMetadata(BGTA_METADATA_START + url + BGTA_METADATA_FINISH);
   return bb;
}


static BgtaBubble createRecievedChatBubble(String username,BgtaManager man)
{
   BgtaBubble bb = new BgtaBubble(username,man);
   if (bb != null) {
      rec_dif_back = login_properties.getBoolean(BGTA_ALT_COLOR_UPON_RECIEVE);
      if (rec_dif_back) {
	 bb.setAltColorIsOn(true);
      }
      Rectangle vp = my_buda_root.getViewport();
      my_buda_root.add(bb, new BudaConstraint(vp.x,vp.y));
   }
   return bb;
}



/********************************************************************************/
/*										*/
/*	Metadata adding methods 						*/
/*										*/
/********************************************************************************/

static void addTaskToRoot(Element xml)
{
   my_buda_root.addTask(xml);
}

private void addMetadataMenuItem()
{
   metadata_menu = new JMenu("Send Working Set over Chat");
   metadata_menu.addMenuListener(new SendMetadataChatListener());
   my_buda_root.addTopBarMenuItem(metadata_menu, true);
}

private void addChatButton(JMenu menu,String id,String tt)
{
   JMenuItem itm = new JMenuItem(id);
   itm.addActionListener(new ChatterListener());
   if (tt != null) {
      itm.setToolTipText(tt);
      ToolTipManager.sharedInstance().registerComponent(itm);
   }
   menu.add(itm);
}


/********************************************************************************/
/*										*/
/*	Listeners for chat							*/
/*										*/
/********************************************************************************/

private class SendMetadataChatListener implements MenuListener {

   @Override public void menuSelected(MenuEvent e) {
      List<String> chatters = BgtaFactory.getChatters();
      metadata_menu.removeAll();
      for (String name : chatters) {
	 addChatButton(metadata_menu, name, null);
       }

    }

   @Override public void menuCanceled(MenuEvent e)		{ }

   @Override public void menuDeselected(MenuEvent e)		{ }

}	// end of inner class SendMetadataChatListener


private class ChatterListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      String url = "";
      try {
	 File f = my_buda_root.findCurrentWorkingSet().getDescription();
	 BoardUpload bup = new BoardUpload(f);
	 url = bup.getFileURL();
       }
      catch (IOException e1) {}
      createMetadataChatBubble(cmd, url);
    }

}	// End of inner class ChatterListener



}	// end of class BgtaFactory



/* end of BgtaFactory.java */
