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


import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;

import org.jivesoftware.smack.XMPPException;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.Vector;



class BgtaLoginBubble extends BudaBubble implements BgtaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Vector<BgtaManager> manager_list;
private JButton 	    sub_button;
private JButton 	    logout_button;
private JTextField	    user_field;
private JPasswordField	    pass_field;
private JLabel		    server_field;
private BgtaRepository	    my_repository;
private BgtaLoginName	    my_name;
private boolean 	    rem_user;

private static final long   serialVersionUID = 1L;



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
   server_field = new JLabel("@gmail.com");
   server_field.setHorizontalAlignment(SwingConstants.RIGHT);
   server_field.setVerticalAlignment(SwingConstants.TOP);
   server_field.setFont(BoardFont.getFont(server_field.getFont().getFontName(),Font.PLAIN,10));

   user_field = new JTextField(15);
   pass_field = new JPasswordField(15);
   String[] serverStrings = { "Gmail", "Brown Gmail", "Facebook", "AIM", "Jabber" };
   JComboBox serverchoice = new JComboBox(serverStrings);
   serverchoice.setFont(BoardFont.getFont(serverchoice.getFont().getFontName(),Font.PLAIN,10));

   user_field.addActionListener(new EnterListener(pass_field));
   pass_field.addActionListener(new EnterListener(null));
   serverchoice.addActionListener(new ServerListener(server_field));

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



private class LogoutListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
	  // attempt to log out of the chosen server
      String username = user_field.getText();
      String server = server_field.getText();
      if (server.equals("@gmail.com")) {
	 if (!username.contains("@gmail.com")) username += "@gmail.com";
	 server = "gmail.com";
       }
      else if (server.equals("@brown.edu")) {
	 if (!username.contains("@brown.edu")) username += "@brown.edu";
	 server = "gmail.com";
       }
      else if (server.equals("@jabber.org")) {
	 server = "jabber.org";
       }
      if (BgtaFactory.logoutAccount(username, new String(pass_field.getPassword()), server)) removeBubble();
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
     for (BgtaManager man : manager_list) {
        if (man.isEquivalent(user_field.getText(), new String(pass_field.getPassword()),
    			    server_field.getText())) putin = false;
      }
     if (putin) {
        String username = user_field.getText();
        String server = server_field.getText();
        String password = new String(pass_field.getPassword());
        if (server.equals("@gmail.com")) {
           if (!username.contains("@gmail.com")) username += "@gmail.com";
           server = "gmail.com";
         }
        else if (server.equals("@brown.edu")) {
           if (!username.contains("@brown.edu")) username += "@brown.edu";
           server = "gmail.com";
         }
        else if (server.equals("chat.facebook.com")) {
           server = "chat.facebook.com";
         }
        else if (server.equals("@jabber.org")) {
           server = "jabber.org";
         }
        if (server.equals("AIM")) {
           newman = new BgtaAimManager(username,password,server);
         }
        else {
           newman = new BgtaManager(username,password,server,my_repository);
         }
        newman.setBeingSaved(rem_user);
        manager_list.add(newman);
        my_repository.addNewRep(new BgtaBuddyRepository(newman));
        if (rem_user) BgtaFactory.addManagerProperties(username, password, server);
        removeBubble();
      }
     else {
        user_field.setText("already logged in");
        pass_field.setText("");
      }
       }
      catch (XMPPException xmppe) {
     user_field.setText("invalid login");
     pass_field.setText("");
     pass_field.setToolTipText(xmppe.getMessage());
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

   private JLabel serverlabel;

   private ServerListener(JLabel example) {
      serverlabel = example;
    }

   @Override public void actionPerformed(ActionEvent e) {
      JComboBox cb = (JComboBox) e.getSource();
      String server = (String) cb.getSelectedItem();
      if (server.equals("Gmail")) {
	 serverlabel.setText("@gmail.com");
	 serverlabel.setVisible(true);
       }
      else if (server.equals("Brown Gmail")) {
	 serverlabel.setText("@brown.edu");
	 serverlabel.setVisible(true);
       }
      else if (server.equals("Facebook")) {
	 serverlabel.setText("chat.facebook.com");
	 serverlabel.setVisible(false);
       }
      else if (server.equals("AIM")) {
	 serverlabel.setText("AIM");
	 serverlabel.setVisible(false);
       }
      else if (server.equals("Jabber")) {
	 serverlabel.setText("@jabber.org");
	 serverlabel.setVisible(true);
       }
    }

}	// end of inner class ServerListener



}	// end of class BgtaLoginBubble



/* end of BgtaLoginBubble.java */