/********************************************************************************/
/*										*/
/*		BuenoMethodDialog.java						*/
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

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.List;



public class BuenoMethodDialog extends BuenoAbstractDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoMethodDialog(BudaBubble source,Point locale,
			    BuenoProperties known,BuenoLocation insert,
			    BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_METHOD);
}



/********************************************************************************/
/*										*/
/*	Method dialog panel setup						*/
/*										*/
/********************************************************************************/

protected void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_SIGNATURE);
   pnl.addRawComponent("Signature",sfld);
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
      String nm = getMethodName();
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
	 parseMethodSignature(sgn);
       }
      catch (BuenoException e) {
	 return false;
       }
    }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   return true;
}



private void parseMethodSignature(String txt) throws BuenoException
{
   StreamTokenizer tok = new StreamTokenizer(new StringReader(txt));

   parseModifiers(tok);
   parseReturnType(tok);

   if (checkNextToken(tok,'(')) {
      tok.pushBack();
      String rtyp = property_set.getStringProperty(BuenoKey.KEY_RETURNS);
      String cnm = insertion_point.getClassName();
      int idx1 = cnm.indexOf("<");
      if (idx1 >= 0) cnm = cnm.substring(0,idx1);
      int idx2 = cnm.lastIndexOf(".");
      if (idx2 >= 0) cnm = cnm.substring(idx2+1);
      if (cnm.equals(rtyp)) {
	 create_type = BuenoType.NEW_CONSTRUCTOR;
       }
      property_set.remove(BuenoKey.KEY_RETURNS);
      property_set.put(BuenoKey.KEY_NAME,rtyp);
    }
   else {
      parseName(tok);
    }

   parseArguments(tok);
   parseExceptions(tok);
   parseEnd(tok);
}




private void parseReturnType(StreamTokenizer tok) throws BuenoException
{
   String tnm = parseType(tok);

   property_set.put(BuenoKey.KEY_RETURNS,tnm);
}



private void parseArguments(StreamTokenizer stok) throws BuenoException
{
   if (!checkNextToken(stok,'(')) throw new BuenoException("Parameter list missing");
   List<String> parms = new ArrayList<String>();

   int anum = 1;
   for ( ; ; ) {
      if (checkNextToken(stok,')')) break;
      String typ = parseType(stok);
      String anm = "a" + anum;
      ++anum;
      if (checkNextToken(stok,',') || checkNextToken(stok,')')) {
	 stok.pushBack();
       }
      else if (nextToken(stok) == StreamTokenizer.TT_WORD) {
	 anm = stok.sval;
	 while (checkNextToken(stok,'[')) {
	    if (!checkNextToken(stok,']')) throw new BuenoException("Bad array parameter");
	    typ += "[]";
	  }
       }
      else throw new BuenoException("Expected agrument name");

      parms.add(typ + " " + anm);

      if (checkNextToken(stok,')')) break;
      else if (!checkNextToken(stok,',')) throw new BuenoException("Illegal argument name");
    }

   property_set.put(BuenoKey.KEY_PARAMETERS,parms);
}



private void parseExceptions(StreamTokenizer stok) throws BuenoException
{
   property_set.remove(BuenoKey.KEY_THROWS);

   if (!checkNextToken(stok,"throws")) return;

   List<String> rslt = new ArrayList<String>();

   for ( ; ; ) {
      String typ = parseType(stok);
      rslt.add(typ);
      if (!checkNextToken(stok,',')) break;
    }

   property_set.put(BuenoKey.KEY_THROWS,rslt);
}



/********************************************************************************/
/*										*/
/*	Get the full name of the new method					*/
/*										*/
/********************************************************************************/

private String getMethodName()
{
   StringBuffer buf = new StringBuffer();
   String cls = insertion_point.getClassName();
   buf.append(cls);
   buf.append(".");
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   buf.append("(");
   String [] params = property_set.getParameters();
   for (int i = 0; i < params.length; ++i) {
      if (i > 0) buf.append(",");
      int idx = params[i].lastIndexOf(" ");
      String typ = params[i].substring(0,idx);
      buf.append(typ);
    }
   buf.append(")");

   return buf.toString();
}




}	// end of class BuenoMethodDialog



/* end of BuenoMethodDialog.java */
