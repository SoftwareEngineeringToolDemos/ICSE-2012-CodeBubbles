package edu.brown.cs.bubbles.bgta.educhat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import java.util.Date;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

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
      TicketListPanel p = new TicketListPanel(this);
      setContentPane(p);
   }
   
   private class TicketListPanel extends JPanel implements MouseListener
   {
      private JTable table;
      private TicketList tl;
      
      public TicketListPanel(BudaBubble parent)
      {
         super(new BorderLayout());
         setOpaque(false);
         setPreferredSize(DEFAULT_DIMENSION);
         
         String[] columns = {"Ticket", "Student"};
         Object[][] data = {{"How do you use println??",new Date(0)},{"I can't get my quadtree to work", new Date(13371337)}};
         
         tl = new TicketList();
         tl.add(new StudentTicket("lol", new Date(3454353), "Sdf"));
         tl.add(new StudentTicket("ldsfol", new Date(546254353), "Sdsdff"));
      
         table = new JTable(tl);
        
         table.getColumnModel().getColumn(1).setPreferredWidth(50);
         table.getColumnModel().getColumn(0).setPreferredWidth(150);
       
         //this.set
         table.setFillsViewportHeight(true);
         System.out.println(table.getPreferredSize() + " " + table.getRowHeight());
         JScrollPane p = new JScrollPane(table);
         p.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * 2));
         p.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         table.setOpaque(false);
         p.setOpaque(false);
         table.addMouseListener(this);
         add(p, BorderLayout.CENTER);
      }

	 @Override
	 public void mouseClicked(MouseEvent e) {
	    if(e.getClickCount() == 2){
	       if(table.rowAtPoint(e.getPoint()) != -1)
	       {
		  BudaRoot.findBudaBubbleArea(this).add(new TicketViewBubble(tl.get(table.rowAtPoint(e.getPoint()))));
	       }
	    }
	      
	 }

	 @Override
	 public void mouseEntered(MouseEvent e) {
	    // TODO Auto-generated method stub
	    
	 }

	 @Override
	 public void mouseExited(MouseEvent e) {
	    // TODO Auto-generated method stub
	    
	 }

	 @Override
	 public void mousePressed(MouseEvent e) {
	    // TODO Auto-generated method stub
	    
	 }

	 @Override
	 public void mouseReleased(MouseEvent e) {
	    // TODO Auto-generated method stub
	    
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
