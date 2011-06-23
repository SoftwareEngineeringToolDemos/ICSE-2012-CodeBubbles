package edu.brown.cs.bubbles.bgta.educhat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import java.util.Date;

import edu.brown.cs.bubbles.buda.BudaBubble;

/**
 * This Bubble is for listing the available tickets 
 * so a TA can select them and open up chat with a student
 * @author akovacs
 *
 */
class EduchatTicketListBubble extends BudaBubble {
   private static Color GRADIENT_BOTTOM_COLOR = Color.white;
   private static Color GRADIENT_TOP_COLOR = new Color(0x33,0x00,0x99);
   private static Dimension DEFAULT_DIMENSION = new Dimension(200, 300);
   
   
   public EduchatTicketListBubble()
   {
      TicketListPanel p = new TicketListPanel();
      setContentPane(p);
   }
   
   private class TicketListPanel extends JPanel
   {
         public TicketListPanel()
      {
         super(new BorderLayout());
         setOpaque(false);
         setPreferredSize(DEFAULT_DIMENSION);
         
         String[] columns = {"Ticket", "Student"};
         Object[][] data = {{"How do you use println??",new Date(0)},{"I can't get my quadtree to work", new Date(13371337)}};
         
         TicketList tl = new TicketList();
         tl.add(new StudentTicket("lol", new Date(3454353), "Sdf"));
         tl.add(new StudentTicket("ldsfol", new Date(546254353), "Sdsdff"));
         JTable table = new JTable(tl);
         //table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
         table.getColumnModel().getColumn(1).setPreferredWidth(50);
         table.getColumnModel().getColumn(0).setPreferredWidth(150);
         table.setFillsViewportHeight(true);
         JScrollPane p = new JScrollPane(table);
         p.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         table.setOpaque(false);
         p.setOpaque(false);
         add(p, BorderLayout.CENTER);
      }
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
