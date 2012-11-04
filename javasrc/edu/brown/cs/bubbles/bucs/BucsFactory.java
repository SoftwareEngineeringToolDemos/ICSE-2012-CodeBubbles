/********************************************************************************/
/*										*/
/*		BucsFactory.java						*/
/*										*/
/*	Bubbles Code Search factory class					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bucs;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bale.*;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.batt.*;
import edu.brown.cs.bubbles.bump.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

import java.util.*;


public class BucsFactory implements BucsConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BucsFactory the_factory = null;


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{ }


public static void initialize(BudaRoot br)
{
   getFactory().setupCallbacks();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BucsFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BucsFactory();
    }
   return the_factory;
}



private BucsFactory()
{
}



private void setupCallbacks()
{
   BaleFactory.getFactory().addContextListener(new BucsContexter());
}




/********************************************************************************/
/*										*/
/*	Create new test bubbles 						*/
/*										*/
/********************************************************************************/

private boolean createTestCaseBubble(BaleContextConfig cfg,BattConstants.NewTestMode md)
{
   String mnm = cfg.getMethodName();

   List<BumpLocation> locs = BumpClient.getBump().findMethod(null,mnm,false);
   if (locs == null || locs.size() == 0) return false;
   BumpLocation loc = locs.get(0);

   BudaBubble bb = new BucsTestCaseBubble(loc,cfg.getEditor());
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(cfg.getEditor());
   if (bba == null) return false;
   bba.addBubble(bb,cfg.getEditor(),null,
	    BudaConstants.PLACEMENT_RIGHT|BudaConstants.PLACEMENT_MOVETO|BudaConstants.PLACEMENT_NEW);

   return true;
}







/********************************************************************************/
/*										*/
/*	Handle context clicks in the editor					*/
/*										*/
/********************************************************************************/

private class BucsContexter implements BaleConstants.BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      switch (cfg.getTokenType()) {
	 case METHOD_DECL_ID :
	    menu.add(new BucsAction(cfg));
	    break;
	 default :
	    break;
       }
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

}	// end of inner class BpareContexter



/********************************************************************************/
/*										*/
/*	Code search action							*/
/*										*/
/********************************************************************************/

private class BucsAction extends AbstractAction {

   private BaleContextConfig start_config;

   private static final long serialVersionUID = 1;


   BucsAction(BaleContextConfig cfg) {
      super("Code Search for Method Implementation");
      start_config = cfg;
    }

   @Override public void actionPerformed(ActionEvent e) {
       createTestCaseBubble(start_config,BattConstants.NewTestMode.INPUT_OUTPUT);
    }

}	// end of inner class BpareAction



}	// end of class BucsFactory




/* end of BucsFactory.java */

