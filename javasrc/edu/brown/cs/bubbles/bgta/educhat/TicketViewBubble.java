package edu.brown.cs.bubbles.bgta.educhat;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.brown.cs.bubbles.buda.BudaBubble;

class TicketViewBubble extends BudaBubble {
   private static Color GRADIENT_BOTTOM_COLOR = Color.white;
   private static Color GRADIENT_TOP_COLOR = new Color(0x33,0x00,0x99);
   private static Dimension DEFAULT_DIMENSION = new Dimension(200, 300);
   
   private StudentTicket ticket;
   
   public TicketViewBubble(StudentTicket t)
   {
      ticket = t;
      setContentPane(new TicketViewPanel(t));
   }
   
   private class TicketViewPanel extends JPanel
   {
      public TicketViewPanel(StudentTicket t)
      {
	 add(new JLabel(t.getText()));
      }
   }

}
