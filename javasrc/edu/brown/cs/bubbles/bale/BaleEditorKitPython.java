/********************************************************************************/
/*										*/
/*		BaleEditorKitPython.java					*/
/*										*/
/*	Bubble Annotated Language Editor editor kit for Python-specific cmds	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.bale;


import javax.swing.Action;
import javax.swing.text.Keymap;



class BaleEditorKitPython implements BaleConstants, BaleConstants.BaleLanguageKit
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKitPython()
{
}



/********************************************************************************/
/*                                                                              */
/*      Action Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Action [] getActions()
{
   return new Action[0];
}


@Override public Keymap getKeymap(Keymap base) 
{
   return base;
}


}	// end of class BaleEditorKitPython



/* end of BaleEditorKitPython.java */

