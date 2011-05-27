/********************************************************************************/
/*										*/
/*		BoppColorOption.java						*/
/*										*/
/*	Handle color choice options						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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


package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardProperties;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


class BoppColorOption extends BoppOption {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private ColorOption	  c_opt;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppColorOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
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
   addBasicInfo();
   b_props = BoardProperties.getProperties(opt_pack);
   c_opt = new ColorOption(b_props.getColorOption(opt_name));
   this.add(c_opt);
   this.setMinimumSize(COLOR_OPTION_SIZE);
   this.setMaximumSize(COLOR_OPTION_SIZE);
   this.setPreferredSize(COLOR_OPTION_SIZE);
}



/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   c_opt.setColor(b_props.getColorOption(opt_name));
   repaint();
}



@Override void setOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   Color currentColor = b_props.getColorOption(opt_name);
   Color optcolor = c_opt.getColor();
   if ((currentColor.getRed() != optcolor.getRed())
	    || (currentColor.getGreen() != optcolor.getGreen())
	    || (currentColor.getBlue() != optcolor.getBlue())) {
      incOption(opt_name, opt_pack);
      b_props.setProperty(opt_name, c_opt.getColor());
   }
   save(b_props);
}




/********************************************************************************/
/*										*/
/*	Option class for normal options 					*/
/*										*/
/********************************************************************************/

private class ColorOption extends JPanel implements ActionListener {

   private Color	color;
   private static final long serialVersionUID = 1L;

   private ColorOption(Color c) {
      setLayout(null);
      color = c;
      JButton b = new JButton();
      b.setText("Pick a color");
      b.addActionListener(this);
      this.add(b);
      Insets insets = getInsets();
      Dimension size = b.getPreferredSize();
      b.setBounds(COLOR_BUTTON_POSITION_X + insets.left, COLOR_BUTTON_POSITION_Y
		     + insets.top, size.width, size.height);
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setPaint(Color.black);
      Rectangle2D rect = new Rectangle2D.Float(COLOR_SWATCH_X - 5,COLOR_SWATCH_Y - 5,
						  COLOR_SWATCH_WIDTH + 5,COLOR_SWATCH_HEIGHT + 5);
      g2.fill(rect);
      g2.setPaint(color);
      rect = new Rectangle2D.Float(COLOR_SWATCH_X,COLOR_SWATCH_Y,COLOR_SWATCH_WIDTH,
				      COLOR_SWATCH_HEIGHT);
      g2.fill(rect);
    }

   private void setColor(Color c) {
      color = c;
    }

   private Color getColor() {
      return color;
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      int alpha = color.getAlpha();
      Color c = JColorChooser.showDialog(this, "Pick a color", color);
      if (c != null) {
	 color = new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
       }
      JButton b = (JButton) arg0.getSource();
      b.getParent().repaint();
    }

}	// end of inner class ColorOption


}	// end of class BoppColorOption



/* end of BoppColorOption.java */
