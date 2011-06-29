package edu.brown.cs.bubbles.bgta.educhat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.brown.cs.bubbles.bgta.*;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

class EduchatTicketSubmitBubble extends BudaBubble {
   private static Color GRADIENT_BOTTOM_COLOR = Color.white;
   private static Color GRADIENT_TOP_COLOR = new Color(0xC4,0x32,0x1F);
   private static Dimension DEFAULT_DIMENSION = new Dimension(250, 200);
   
   
   private static final long serialVersionUID = 1L;
   
   public EduchatTicketSubmitBubble(String ta_jid)
   {
      SubmitListener l = new SubmitListener(ta_jid, this);
      TicketPanel p = new TicketPanel();
      setContentPane(p);
   }

   private class TicketPanel extends JPanel
   {
      private TicketPanel()
      {
         setOpaque(false); 
         
         setPreferredSize(DEFAULT_DIMENSION);
         setLayout(new GridBagLayout());
         
         GridBagConstraints c = new GridBagConstraints();	 
         JLabel l = new JLabel("Choose a chat login: ");
         c.gridx = 0;
         c.gridy = 0;
         c.fill = GridBagConstraints.NONE;
         c.weightx = 0.5;
         c.weighty = 0.1;
         add(l, c);
         
         JComboBox comboBox = new JComboBox();
         comboBox.addItem("Test");
         c.gridx = 1;
         c.gridy = 0;
         c.fill = GridBagConstraints.NONE;
         c.weightx = 0.5;
         c.weighty = 0;
         add(comboBox, c);
         
         JLabel ticket_area_label = new JLabel("Describe your question or problem: ");
         c.gridx = 0;
         c.gridy = 1;
         c.gridwidth = 2;
         c.gridheight = 1;
         c.weightx = 0;
         c.anchor = GridBagConstraints.PAGE_START;
         c.insets = new Insets(0, 0, 10, 0);
        // c.fill = GridBagConstraints.HORIZONTAL;
         add(ticket_area_label, c);
         c.insets = new Insets(0,0,0,0);
         
         
         JTextArea ticket_pane = new JTextArea();
         ticket_pane.setOpaque(false);
         ticket_pane.setText("Blah blah");
         ticket_pane.setLineWrap(true);
         JScrollPane scroll = new JScrollPane(ticket_pane);
         scroll.setOpaque(false);
         scroll.getViewport().setOpaque(false);
         //scroll.setBorder(null);
         c.anchor = GridBagConstraints.PAGE_START;
         c.gridx = 0;
         c.gridy = 2;
         c.gridwidth = 2;
         c.gridheight = 1;
         c.fill = GridBagConstraints.BOTH;
         c.weightx = 1;
         c.weighty = 1;
         add(scroll, c);
         
         JButton submit_button = new JButton("Submit");
         
         c.anchor = GridBagConstraints.PAGE_END;
         c.gridx = 1;
         c.gridy = 3;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.weightx = .4;
         c.weighty = 0;
         c.fill = GridBagConstraints.NONE;
         add(submit_button, c);
      }
      
      @Override public void paintComponent(Graphics g) {
         Graphics2D g2 = (Graphics2D) g.create();
         Dimension sz = getSize();
         Paint p = new GradientPaint(0f,0f,GRADIENT_BOTTOM_COLOR,0f,sz.height, GRADIENT_TOP_COLOR);
             
         Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
         g2.setColor(Color.orange);
         g2.fill(r);
         g2.setPaint(p);
         g2.fill(r);
      
         super.paintComponent(g);
      }
   }
private class SubmitListener implements ActionListener{
   private String ta_jid;
   private EduchatTicketSubmitBubble bubble;
   
   public SubmitListener(String jid, EduchatTicketSubmitBubble a_bubble)
   {
       bubble = a_bubble;
       ta_jid = jid;
   }
   
   @Override public void actionPerformed(ActionEvent e)
   {
      //we need to give the student the chance to choose an account, for now we'll pick arbitrarily just to try it out
    //  BgtaManager man = BgtaFactory.getFactory().getManagers().next();
      
     // BgtaBubble chat_b = BgtaFactory.createRecievedChatBubble(ta_jid, man);
    //  BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bubble);
      
     // bba.addBubble(chat_b, bubble, null, PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
   }

}
  
}
