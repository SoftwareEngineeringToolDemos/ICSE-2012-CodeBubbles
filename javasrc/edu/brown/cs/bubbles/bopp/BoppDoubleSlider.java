/********************************************************************************/
/*										*/
/*		BoppDoubleSlider.java						*/
/*										*/
/*	Slider panel for floating point values					*/
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

/**
 * <b>Programm:</b> WaveGradient<br>
 * <b>Copyright:</b> 2002 Andreas Gohr, Frank Schubert<br>
 * <b>License:</b> GPL2 or higher<br>
 * <br>
 * <b>Info:</b> This JSlider uses doubles for its values
 */


package edu.brown.cs.bubbles.bopp;


import javax.swing.JSlider;


class BoppDoubleSlider extends JSlider {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int decimal_precision;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 * Constructor - initializes with 0.0,100.0,50.0
 */
BoppDoubleSlider()
{
   this(0,100,50,100);
}


BoppDoubleSlider(double min,double max,double val,int precision)
{
   super();
   decimal_precision = (int) Math.pow(10, precision);
   setDoubleMinimum(min);
   setDoubleMaximum(max);
   setDoubleValue(val);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 * returns Maximum in double precision
 */
double getDoubleMaximum()
{
   return ((double) getMaximum() / decimal_precision);
}

/**
 * returns Minimum in double precision
 */
double getDoubleMinimum()
{
   return ((double) getMinimum() / decimal_precision);
}

/**
 * returns Value in double precision
 */
double getDoubleValue()
{
   return ((double) getValue() / decimal_precision);
}

/**
 * sets Maximum in double precision
 */
void setDoubleMaximum(double max)
{
   setMaximum((int) (max * decimal_precision));
}

/**
 * sets Minimum in double precision
 */
void setDoubleMinimum(double min)
{
   setMinimum((int) (min * decimal_precision));
}

/**
 * sets Value in double precision
 */
void setDoubleValue(double val)
{
   setValue((int) (val * decimal_precision));
   setToolTipText(Double.toString(val));
}



}	// end of class BoppDoubleSlider



/* end of BoppDoubleSlider.java */
