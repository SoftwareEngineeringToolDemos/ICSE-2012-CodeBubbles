/********************************************************************************/
/*										*/
/*		BoppDividerOption.java						*/
/*										*/
/*	Divider between option sets						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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


package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardImage;

import javax.swing.*;

import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.*;
import java.util.ArrayList;


class BoppDividerOption extends BoppOption {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppDividerOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
{
   super(n,tn,d,p,t);
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setup()
{
   super.setup();
   JLabel n = new JLabel("   " + opt_name);
   n.setFont(DIVIDER_LARGE_FONT);
   n.setHorizontalAlignment(JLabel.CENTER);
   n.setVerticalAlignment(JLabel.CENTER);
   JPanel p = new BackgroundPanel(BoardImage.getImage("dividerbg"),n);
   // p.add(n);
   this.add(p);
   this.setMinimumSize(DIVIDER_OPTION_SIZE);
   this.setMaximumSize(DIVIDER_OPTION_SIZE);
   this.setPreferredSize(DIVIDER_OPTION_SIZE);
}



/********************************************************************************/
/*										*/
/*	Option set and get methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{
   // do nothing
}



@Override void setOption()
{
   // do nothing
}



/********************************************************************************/
/*										*/
/*	Panel for the separator 						*/
/*										*/
/********************************************************************************/

private static class BackgroundPanel extends JPanel {

   private Image  background_image;
   private static final long serialVersionUID = 1;

   public BackgroundPanel(Image i,JLabel l) {
      background_image = i;
      BufferedImage bimage = new BufferedImage(DIVIDER_OPTION_SIZE.width,
						  DIVIDER_OPTION_SIZE.height,BufferedImage.TYPE_4BYTE_ABGR);
      Graphics2D g = bimage.createGraphics();
      adjustGraphics(g);
      TextLayout textLayout = new TextLayout(l.getText(),l.getFont(),
						g.getFontRenderContext());
      g.setPaint(Color.white);
      textLayout.draw(g, 1, 24);
      g.dispose();

      float ninth = 1.0f / 9.0f;
      float[] kernel = { ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth, ninth };
      ConvolveOp op = new ConvolveOp(new Kernel(3,3,kernel),ConvolveOp.EDGE_NO_OP,null);
      BufferedImage image2 = op.filter(bimage, null);

      Graphics2D g2 = image2.createGraphics();
      adjustGraphics(g2);
      g2.setPaint(Color.BLACK);
      textLayout.draw(g2, 0, 23);
      JLabel lab = new JLabel(new ImageIcon(image2));
      lab.setOpaque(false);
      this.setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
      this.setOpaque(false);
      this.add(lab);
      this.setMinimumSize(DIVIDER_OPTION_SIZE);
      this.setMaximumSize(DIVIDER_OPTION_SIZE);
      this.setPreferredSize(DIVIDER_OPTION_SIZE);
    }

   private void adjustGraphics(Graphics2D g) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
			    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

   @Override protected void paintComponent(Graphics g) {
      g.drawImage(background_image, 0, 0, this);
      super.paintComponent(g);
    }

}	// end of inner class BackgroundPanel



}	// end of class BoppDividerOption



/* end of BoppDividerOption.java */
