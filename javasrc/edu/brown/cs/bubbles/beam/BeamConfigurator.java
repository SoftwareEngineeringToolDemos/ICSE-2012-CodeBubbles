/********************************************************************************/
/*										*/
/*		BeamConfigurator.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items bubble configurator	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;



class BeamConfigurator implements BeamConstants, BudaConstants.BubbleConfigurator
{



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");

   BudaBubble bb = null;

   if (typ.equals("NOTE")) {
      String cnts = IvyXml.getTextElement(cnt,"TEXT");
      String name = IvyXml.getAttrString(cnt,"NAME");
      BeamNoteAnnotation annot = null;
      Element anx = IvyXml.getChild(cnt,"ANNOT");
      if (anx != null) {
	 annot = new BeamNoteAnnotation(anx);
	 if (annot.getDocumentOffset() < 0) annot = null;
       }
      bb = new BeamNoteBubble(name,cnts,annot);
    }
   else if (typ.equals("FLAG")) {
      String path = IvyXml.getTextElement(cnt,"IMGPATH");
      bb = new BeamFlagBubble(path);
    }
   else if (typ.equals("PROBLEMS")) {
      String typs = IvyXml.getAttrString(cnt,"ERRORTYPES");
      boolean tasks = IvyXml.getAttrBool(cnt,"FORTASKS");
      bb = new BeamProblemBubble(typs,tasks);
    }

   return bb;
}




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
@Override public void loadXml(Element root)				{ }




}	// end of class BeamConfigurator




/* end of BeamConfigurator.java */
