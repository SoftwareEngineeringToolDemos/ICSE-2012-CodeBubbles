/********************************************************************************/
/*										*/
/*		BgtaLoginBubble.java						*/
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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.jivesoftware.smack.XMPPException;

import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.buda.BudaBubble;



class BgtaLoginBubble extends BudaBubble implements BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Vector<BgtaManager>            manager_list;
private JButton                        sub_button;
private JButton                        logout_button;
private JTextField                     user_field;
private JPasswordField                 pass_field;
private JLabel                         server_field;
private BgtaRepository                 my_repository;
private BgtaLoginName                  my_name;
private ChatServer                     selected_server;
private boolean                        rem_user;
private static Collection<BgtaManager> all_managers;

private static final long   serialVersionUID = 1L;

static {
   all_managers = new Vector<BgtaManager>();
}

/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BgtaLoginBubble(Vector<BgtaManager> mans,BgtaRepository repo,BgtaLoginName name)
{
   super();
   manager_list = mans;
   my_repository = repo;
   my_name = name;
   selected_server = ChatServer.GMAIL;

   LoginPanel lpan = new LoginPanel();
   JLabel userlabel = new JLabel("Username: ");
   userlabel.setHorizontalAlignment(SwingConstants.LEFT);
   userlabel.setVerticalAlignment(SwingConstants.BOTTOM);
   userlabel.setFont(BoardFont.getFont(userlabel.getFont().getFontName(),Font.PLAIN,10));
   JLabel passlabel = new JLabel("Password: ");
   passlabel.setHorizontalAlignment(SwingConstants.LEFT);
   passlabel.setVerticalAlignment(SwingConstants.BOTTOM);
   passlabel.setFont(BoardFont.getFont(passlabel.getFont().getFontName(),Font.PLAIN,10));
   JLabel serverlabel = new JLabel("Service: ");
   serverlabel.setHorizontalAlignment(SwingConstants.LEFT);
   serverlabel.setVerticalAlignment(SwingConstants.BOTTOM);
   serverlabel.setFont(BoardFont.getFont(serverlabel.getFont().getFontName(),Font.PLAIN,10));
   server_field = new JLabel(selected_server.display());
   server_field.setHorizontalAlignment(SwingConstants.RIGHT);
   server_field.setVerticalAlignment(SwingConstants.TOP);
   server_field.setFont(BoardFont.getFont(server_field.getFont().getFontName(),Font.PLAIN,10));

   user_field = new JTextField(15);
   pass_field = new JPasswordField(15);
   int servers = ChatServer.values().length;
   String[] serverStrings = new String[servers];
   servers = 0;
   for (ChatServer server : ChatServer.values()) {
   	serverStrings[servers++] = server.selector();
   }
   JComboBox serverchoice = new JComboBox(serverStrings);
   serverchoice.setFont(BoardFont.getFont(serverchoice.getFont().getFontName(),Font.PLAIN,10));

   user_field.addActionListener(new EnterListener(pass_field));
   user_field.addFocusListener(new FocusSelectionListener());
   pass_field.addActionListener(new EnterListener(null));
   pass_field.addFocusListener(new FocusSelectionListener());
   serverchoice.addActionListener(new ServerListener());

   JCheckBox rembox = new JCheckBox("Keep me signed in");
   rembox.setOpaque(false);
   rembox.setFont(BoardFont.getFont(rembox.getFont().getFontName(),Font.PLAIN,10));
   rembox.setSelected(BGTA_INITIAL_REM_SETTING);
   rembox.addItemListener(new RememberListener());

   sub_button = new JButton("Login");
   sub_button.addActionListener(new LoginListener());
   sub_button.setFont(BoardFont.getFont(sub_button.getFont().getFontName(),Font.PLAIN,10));
   logout_button = new JButton("Logout");
   logout_button.addActionListener(new LogoutListener());
   logout_button.setFont(BoardFont.getFont(logout_button.getFont().getFontName(),Font.PLAIN,10));

   lpan.setLayout(new GridBagLayout());
   GridBagConstraints c = new GridBagConstraints();
   c.fill = GridBagConstraints.NONE;
   c.anchor = GridBagConstraints.LINE_START;
   c.gridx = 0;
   c.gridy = 0;
   c.weighty = 0.5;
   c.gridwidth = 1;
   c.gridheight = 1;
   c.insets = new Insets(5,10,0,0);
   lpan.add(serverlabel, c);
   c.anchor = GridBagConstraints.LINE_END;
   c.gridx = 1;
   c.insets = new Insets(5,0,0,10);
   lpan.add(serverchoice, c);
   c.anchor = GridBagConstraints.LAST_LINE_START;
   c.gridx = 0;
   c.gridy = 1;
   c.gridwidth = 2;
   c.weighty = 0.5;
   c.insets = new Insets(0,10,0,10);
   lpan.add(userlabel, c);
   c.anchor = GridBagConstraints.CENTER;
   c.gridy = 2;
   lpan.add(user_field, c);
   c.anchor = GridBagConstraints.LAST_LINE_END;
   c.gridy = 3;
   lpan.add(server_field, c);
   c.anchor = GridBagConstraints.LAST_LINE_START;
   c.gridy = 4;
   lpan.add(passlabel, c);
   c.anchor = GridBagConstraints.CENTER;
   c.gridy = 5;
   lpan.add(pass_field, c);
   c.gridy = 6;
   lpan.add(rembox, c);
   c.anchor = GridBagConstraints.LINE_START;
   c.gridy = 7;
   c.gridwidth = 1;
   c.insets = new Insets(0,10,5,0);
   lpan.add(sub_button, c);
   c.anchor = GridBagConstraints.LINE_END;
   c.gridx = 1;
   c.insets = new Insets(0,0,5,10);
   lpan.add(logout_button, c);

   setContentPane(lpan);
}




/********************************************************************************/
/*										*/
/*	Bubble management methods						*/
/*										*/
/********************************************************************************/

void removeBubble()
{
   setVisible(false);
}



@Override public void setVisible(boolean vis)
{
   super.setVisible(vis);
   if (vis == false) my_name.setHasBubble(false);
}



/********************************************************************************/
/*										*/
/*	Interaction Listeners									*/
/*										*/
/********************************************************************************/
private class LogoutListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
	  // attempt to log out of the chosen server
      String username = user_field.getText();
      String servername = selected_server.server();
      if (selected_server.hasEnding() && !username.contains(selected_server.ending()))
      	username += selected_server.ending();
      if (BgtaFactory.logoutAccount(username,servername)) removeBubble();
      // if not logged in in the first place, display a message saying so
      else {
	 user_field.setText("weren't logged in to begin with");
	 pass_field.setText("");
       }
    }

}	// end of inner class LogoutListener



private class RememberListener implements ItemListener {

   @Override public void itemStateChanged(ItemEvent e) {
      rem_user = !rem_user;
    }

}	// end of inner class RememberListener



private class LoginListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask(BowiTaskType.LOGIN_TO_CHAT);
      if (user_field.getText().equals("") || pass_field.getPassword().equals("")) {
     BowiFactory.stopTask(BowiTaskType.LOGIN_TO_CHAT);
     return;
       }
      BgtaManager newman = null;
      try {
     boolean putin = true;
     String username = user_field.getText();
     String password = new String(pass_field.getPassword());
     String servername = selected_server.server();
     if (selected_server.hasEnding() && !username.contains(selected_server.ending()))
        username += selected_server.ending();
     for (BgtaManager man : all_managers) {
        if (man.isEquivalent(username,servername)
      		  && man.getPassword().equals(new String(password))) {
           newman = man;
      	  putin = false;
         }
      }
     if (putin) {
        if (selected_server == ChatServer.AIM) {
           newman = new BgtaAimManager(username,password,servername);
         }
        else {
           newman = new BgtaManager(username,password,servername,my_repository);
         }
        newman.setBeingSaved(rem_user);
        manager_list.add(newman);
        all_managers.add(newman);
        my_repository.addNewRep(new BgtaBuddyRepository(newman));
        if (rem_user)
           BgtaFactory.addManagerProperties(username, password, servername);
        removeBubble();
      }
     else {
        if (newman.isLoggedIn()) {
           user_field.setText("already logged in");
           pass_field.setText("");
         }
        else {
           newman.login();
           newman.setBeingSaved(rem_user);
           manager_list.add(newman);
           my_repository.addNewRep(new BgtaBuddyRepository(newman));
           if (rem_user)
              BgtaFactory.addManagerProperties(username, password, servername);
           removeBubble();
         }
      }
       }
      catch (XMPPException xmppe) {
     user_field.setText("incorrect login information");
     pass_field.setText("");
     pass_field.setToolTipText(xmppe.getMessage());
//     user_field.setText(xmppe.getClass() + xmppe.getMessage());
       }
      BowiFactory.stopTask(BowiTaskType.LOGIN_TO_CHAT);
    }

}	// end of inner class LoginListener



private class EnterListener implements ActionListener {

   private JTextField next_field;

   private EnterListener(JTextField next) {
      next_field = next;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (next_field != null) {
	 next_field.requestFocusInWindow();
       }
      else {
	 sub_button.doClick();
       }
    }

}	// end of inner class EnterListener



private class FocusSelectionListener extends FocusAdapter {
	
	@Override public void focusGained(FocusEvent e) {
		((JTextField) e.getSource()).selectAll();
	}
	
	@Override public void focusLost(FocusEvent e) {
		((JTextField) e.getSource()).setCaretPosition(0);
		((JTextField) e.getSource()).moveCaretPosition(0);
	}
	
} // end of inner class FocusSelectionListener



/********************************************************************************/
/*										*/
/*	Login panel implementation						*/
/*										*/
/********************************************************************************/

private class LoginPanel extends JPanel implements BgtaConstants {

   private static final long serialVersionUID = 1L;

   LoginPanel() {
      super(new GridBagLayout());
      setOpaque(false);
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Paint p = new GradientPaint(0f,0f,BGTA_BUBBLE_TOP_COLOR,0f,sz.height,
				     BGTA_BUBBLE_BOTTOM_COLOR);
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setColor(Color.white);
      g2.fill(r);
      g2.setPaint(p);
      g2.fill(r);

      super.paintComponent(g);
    }

}	// end of inner class LoginPanel



/********************************************************************************/
/*										*/
/*	Server listener 							*/
/*										*/
/********************************************************************************/

private class ServerListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      JComboBox cb = (JComboBox) e.getSource();
      String selection = (String) cb.getSelectedItem();
      selected_server = ChatServer.GMAIL;
      if (selection.equals(ChatServer.BROWN.selector())) {
      	selected_server = ChatServer.BROWN;
       }
      else if (selection.equals(ChatServer.FACEBOOK.selector())) {
      	selected_server = ChatServer.FACEBOOK;
       }
      else if (selection.equals(ChatServer.AIM.selector())) {
      	selected_server = ChatServer.AIM;
       }
      else if (selection.equals(ChatServer.JABBER.selector())) {
      	selected_server = ChatServer.JABBER;
       }
      server_field.setText(selected_server.display());
      if (selected_server == ChatServer.FACEBOOK || selected_server == ChatServer.AIM)
      	server_field.setVisible(false);
      else
      	server_field.setVisible(true);
    }

}	// end of inner class ServerListener



}	// end of class BgtaLoginBubble



/* end of BgtaLoginBubble.java */