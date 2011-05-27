/********************************************************************************/
/*										*/
/*		BuenoPackageDialog.java 					*/
/*										*/
/*	BUbbles Environment New Objects creator new package dialog		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class BuenoPackageDialog extends BuenoAbstractDialog implements BuenoConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Pattern package_pattern = Pattern.compile("[A-Za-z_]\\w*(\\.[A-Za-z_]\\w*)*");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoPackageDialog(BudaBubble source,Point locale,
			    BuenoProperties known,BuenoLocation insert,
			    BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_PACKAGE);
}



/********************************************************************************/
/*										*/
/*	Method dialog panel setup						*/
/*										*/
/********************************************************************************/

protected void setupPanel(SwingGridPanel pnl)
{
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (pkg == null) {
      pkg = insertion_point.getPackage();
      if (pkg != null) {
	 int idx = pkg.lastIndexOf(".");
	 if (idx >= 0) property_set.put(BuenoKey.KEY_PACKAGE,pkg.substring(0,idx+1));
       }
    }

   StringField sfld = new StringField(BuenoKey.KEY_PACKAGE);
   pnl.addRawComponent("Package Name",sfld);
   sfld.addActionListener(this);

   sfld = new StringField(BuenoKey.KEY_SIGNATURE);
   pnl.addRawComponent("Initial Class Signature",sfld);
   sfld.addActionListener(this);
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

@Override protected void doCreate(BudaBubbleArea bba,Point p)
{
   BuenoFactory bf = BuenoFactory.getFactory();
   bf.createNew(BuenoType.NEW_PACKAGE,insertion_point,property_set);

   // create the initial class
   bf.createNew(create_type,insertion_point,property_set);

   String proj = insertion_point.getProject();
   File f = insertion_point.getInsertionFile();
   if (f == null) return;

   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   String cls = pkg + "." + property_set.getStringProperty(BuenoKey.KEY_NAME);

   if (bubble_creator != null) {
      bubble_creator.createBubble(proj,cls,bba,p);
    }
}



/********************************************************************************/
/*										*/
/*	Package parsing methods 						*/
/*										*/
/********************************************************************************/

protected boolean checkParsing()
{
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);

   if (pkg == null || pkg.length() == 0) return false;

   Matcher m = package_pattern.matcher(pkg);
   if (!m.matches()) return false;

   String sgn = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
   if (sgn != null) {
      try {
	 parseClassSignature(sgn);
       }
      catch (BuenoException e) {
	 return false;
       }
    }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   return true;
}



}	// end of class BuenoPackageDialog



/* end of BuenoPackageDialog.java */
