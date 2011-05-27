/********************************************************************************/
/*										*/
/*		BoppFactory.java						*/
/*										*/
/*	Factory for setting up the option panel 				*/
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

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.buda.*;

import org.w3c.dom.Node;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


/**
 * Factory for options panel
 **/

public class BoppFactory implements BoppConstants, BudaConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static BoppFactory the_factory = new BoppFactory();
private static BudaRoot    buda_root;



/**
 * returns the factory
 *
 */

public static BoppFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

/**
 * setup method (currently does nothing)
 */

public static void setup()
{
   // do nothing
}


/**
 *	Setup method called after buda is setup
 **/

public static void initialize(BudaRoot br)
{
   Icon chevron = BoardImage.getIcon("dropdown_chevron");
   chevron = BoardImage.resizeIcon(((ImageIcon) chevron).getImage(),
				      BUDA_BUTTON_RESIZE_WIDTH, BUDA_BUTTON_RESIZE_HEIGHT);
   JButton btn1 = new JButton("Options",chevron);
   btn1.setIconTextGap(0);

   // JButton btn1 = new JButton("Options");
   btn1.setMargin(BOPP_BUTTON_INSETS);
   Font ft = btn1.getFont();
   ft = ft.deriveFont(10f);
   btn1.setHorizontalTextPosition(AbstractButton.LEADING);
   btn1.setFont(ft);
   btn1.setOpaque(false);
   btn1.setBackground(new Color(0,true));
   btn1.addActionListener(new OptionsListener(br));
   btn1.setToolTipText("Options for Code Bubbles");

   br.addButtonPanelButton(btn1);
   buda_root = br;
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

static void repaintBubbleArea()
{
   buda_root.getCurrentBubbleArea().repaint();
}

/**
 * Returns a new options panel
 */

public static JPanel getBoppPanel(BudaBubbleArea area)
{
   BoppPanelHandler b = new BoppPanelHandler(area);
   return b.getUIPanel();
}


/**
 * Makes an option
 *
 * @param n
 *	     Option name (e.g. Beam.note.width)
 * @param tn
 *	     List of tabs that contain the option
 * @param d
 *	     Description that will be visible to users
 * @param p
 *	     Package name (e.g. Beam)
 * @param t
 *	     Option type
 * @return
 */

static BoppOption makeOption(String n,ArrayList<TabName> tn,String d,String p,
				OptionType t,Node node)
{
   if (t == null) return null;

   switch (t) {
      case INTEGER:
	 return new BoppIntOption(n,tn,d,p,t,node);
      case BOOLEAN:
	 return new BoppBoolOption(n,tn,d,p,t);
      case STRING:
	 return new BoppStringOption(n,tn,d,p,t);
      case COLOR:
	 return new BoppColorOption(n,tn,d,p,t);
      case DIVIDER:
	 return new BoppDividerOption(n,tn,d,p,t);
      case COMBO:
	 return new BoppComboOption(n,tn,d,p,t,node);
      case FONT:
	 // return new BoppFontOption(n, tn, d, p, t);
    }
   return null;
}



/********************************************************************************/
/*										*/
/*	Handler for options button						*/
/*										*/
/********************************************************************************/

private static class OptionsListener implements ActionListener {

   private BudaRoot		       for_root;
   private Map<BudaBubbleArea, JPanel> options_panel;

   OptionsListener(BudaRoot br) {
      for_root = br;
      options_panel = new HashMap<BudaBubbleArea, JPanel>();
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaBubbleArea bba = for_root.getCurrentBubbleArea();
      if (bba == null) return;

      JPanel pnl = options_panel.get(bba);
      if (pnl == null) {
	 pnl = BoppFactory.getBoppPanel(bba);
	 options_panel.put(bba, pnl);
       }
      else if (pnl.getParent() != null && pnl.getParent().isVisible()) {
	 pnl.getParent().setVisible(false);
	 return;
       }

      Rectangle r = bba.getViewport();
      Dimension d = pnl.getPreferredSize();
      BudaConstraint bc = new BudaConstraint(BudaConstants.BudaBubblePosition.STATIC,r.x
						+ r.width - d.width,r.y);
      pnl.setSize(d);
      bba.add(pnl, bc, 0);
      pnl.setVisible(true);
    }

}	// end of inner class OptionsListener


}	// end of class BoppFactory



/* end of BoppFactory.java */
