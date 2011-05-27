/********************************************************************************/
/*										*/
/*		BoppDoubleOption.java						*/
/*										*/
/*	Option panel for floating point values					*/
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

import java.util.ArrayList;


/**
 * Not implemented yet (no options requiring a double)
 * @author ahills
 *
 */
class BoppDoubleOption extends BoppOption {


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

BoppDoubleOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
{
   super(n,tn,d,p,t);
}


/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{ }

@Override void setOption()
{ }


}	// end of class BoppDoubleOption



/* end of BoppDoubleOption.java */
