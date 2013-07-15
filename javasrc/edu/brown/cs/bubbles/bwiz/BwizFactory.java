/********************************************************************************/
/*										*/
/*		BwizFactory.java						*/
/*										*/
/*	Factory to set up wizards as part of bubbles				*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Annalia Sunderland		      */
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bwiz;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoBubbleCreator;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoMethodCreatorInstance;
import edu.brown.cs.bubbles.board.*;

import java.awt.*;




public class BwizFactory implements BwizConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot	buda_root;

private static BwizFactory the_factory;




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   the_factory = new BwizFactory();
}



public static void initialize(BudaRoot br)
{
   the_factory.setupWizards(br);
}



public static BwizFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BwizFactory()
{
   buda_root = null;
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupWizards(BudaRoot br)
{
   buda_root = br;

   BwizStartWizard pnl = new BwizStartWizard();

   br.addPanel(pnl,false);

   BoardProperties bp = BoardProperties.getProperties("Bwiz");
   if (bp.getBoolean("Bwiz.use.method.dialog")) {
      BuenoFactory.getFactory().setMethodDialog(new MethodCreator());
    }
}



/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

void createBubble(Component c,Component fc)
{
   WizardBubble bb = new WizardBubble(c,fc);
   BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
   bba.addBubble(bb,null,null,PLACEMENT_LOGICAL|PLACEMENT_USER|PLACEMENT_MOVETO);
}



private class WizardBubble extends BudaBubble
{

   WizardBubble(Component c,Component fc) {
      setContentPane(c,fc);
    }

}	// end of inner class WizardBubble




/********************************************************************************/
/*										*/
/*	Methods for using new method wizard dialog				*/
/*										*/
/********************************************************************************/

private class MethodCreator implements BuenoMethodCreatorInstance {


   @Override public void showMethodDialogBubble(BudaBubble src,Point loc,
						   BuenoProperties known,
						   BuenoLocation insert,
						   String lbl,
						   BuenoBubbleCreator newer) {
      BwizNewWizard bcwiz = new BwizNewWizard(CreateType.METHOD,
						 insert.getClassName(),
						 insert.getProject(),
						 insert.getPackage());
      bcwiz.setInsertLocation(insert);
      bcwiz.setBubbleCreator(newer);
      WizardBubble bb = new WizardBubble(bcwiz,bcwiz.getFocus());
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
      if (bba == null) bba = buda_root.getCurrentBubbleArea();
      bba.addBubble(bb,src,loc,PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_MOVETO|
		       PLACEMENT_LOGICAL);
    }
   
}	// end of inner class MethodCreator




}	// end of class BwizFactory




/* end of BwizFactory.java */
