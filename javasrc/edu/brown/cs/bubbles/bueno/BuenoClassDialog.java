/********************************************************************************/
/*										*/
/*		BuenoClassDialog.java						*/
/*										*/
/*	BUbbles Environment New Objects creator new method dialog		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpContractType;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.File;
import java.util.List;



public class BuenoClassDialog extends BuenoAbstractDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoClassDialog(BudaBubble source,Point locale,BuenoType typ,
			    BuenoProperties known,BuenoLocation insert,
			    BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,typ);

   String prj = property_set.getStringProperty(BuenoKey.KEY_PROJECT);
   if (prj == null) prj = insertion_point.getProject(); 

   BumpContractType bct = BumpClient.getBump().getContractType(prj);

   if (bct == null) return;

   if (bct.useContractsForJava()) {
      String imp = "import com.google.java.contract.*;";
      property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp);
    }

   if (bct.useJunit()) {
      String imp = "import org.junit.*;";
      property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp);
    }
}



/********************************************************************************/
/*										*/
/*	Class dialog panel setup						*/
/*										*/
/********************************************************************************/

protected void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_SIGNATURE);
   pnl.addRawComponent("Class Signature",sfld);
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
   bf.createNew(create_type,insertion_point,property_set);

   String proj = insertion_point.getProject();
   File f = insertion_point.getInsertionFile();
   if (f == null) return;

   if (bubble_creator != null) {
      String nm = getClassName();
      bubble_creator.createBubble(proj,nm,bba,p);
    }
}



/********************************************************************************/
/*										*/
/*	Signature parsing methods						*/
/*										*/
/********************************************************************************/

protected boolean checkParsing()
{
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

   String prj = property_set.getStringProperty(BuenoKey.KEY_PROJECT);
   if (prj == null) prj = insertion_point.getProject();
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (pkg == null) pkg = insertion_point.getPackage();
   String nm = property_set.getStringProperty(BuenoKey.KEY_NAME);
   if (pkg != null) nm = pkg + "." + nm;
   BumpClient bc = BumpClient.getBump();
   List<BumpLocation> locs = bc.findClassDefinition(prj,nm);
   if (locs != null && locs.size() > 0) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Get the full name of the new method					*/
/*										*/
/********************************************************************************/

private String getClassName()
{
   StringBuffer buf = new StringBuffer();
   String pkg = insertion_point.getPackage();
   if (pkg != null) {
      buf.append(pkg);
      buf.append(".");
    }
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   return buf.toString();
}





}	// end of class BuenoClassDialog



/* end of BuenoClassDialog.java */

