/********************************************************************************/
/*										*/
/*		BwizFocusTextField.java 					*/
/*										*/
/*	Text field that selects all of contents when focused			*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 UCF -- Jared Bott				      */
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

import javax.swing.*;
import java.awt.event.*;



class BwizFocusTextField extends JTextField
{


/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

//Creates a BwizFocusTextField with the default styling and the parameter text as the tooltip
static BwizFocusTextField getStyledField(String text)
{
   return getStyledField(text, text);
}



//Creates a BwizFocusTextField with the default styling and a tooltip as specified
static BwizFocusTextField getStyledField(String text, String tooltip)
{
   BwizFocusTextField field = new BwizFocusTextField(text);
   field.setToolTipText(tooltip);

   return field;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizFocusTextField()
{
   super();

   addListener();
}



BwizFocusTextField(String text)
{
   super(text);

   addListener();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void addListener()
{
   this.addFocusListener(new Focuser());
}


private void focusSelection()
{
   selectAll();
}


/********************************************************************************/
/*										*/
/*	Focus manager								*/
/*										*/
/********************************************************************************/

private class Focuser extends FocusAdapter {

   @Override public void focusGained(FocusEvent e) {
      focusSelection();
    }

}	// end of inner class Focuser




}	// end of class BwizFocusTextField



/* end of BwizFocusTextField.java */
