/********************************************************************************/
/*										*/
/*		BaleConfigurator.java						*/
/*										*/
/*	Bubble Annotated Language Editor bubble configuration creator		*/
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


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;



class BaleConfigurator implements BaleConstants, BudaConstants.BubbleConfigurator,
	BudaConstants.PortConfigurator
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
   BaleFactory bf = BaleFactory.getFactory();

   if (typ.equals("FRAGMENT")) {
      BaleFragmentType ftyp = IvyXml.getAttrEnum(cnt,"FRAGTYPE",BaleFragmentType.NONE);
      String proj = IvyXml.getAttrString(cnt,"PROJECT");
      String name = IvyXml.getAttrString(cnt,"NAME");
      int idx = name.lastIndexOf(".");
      if (name.length() == 0) name = null;
      String head = name;
      if (name != null && idx > 0) head = name.substring(0,idx);
      switch (ftyp) {
	 case NONE :
	    break;
	 case FILE :
	    bb = bf.createFileBubble(proj,head);
	    break;
	 case METHOD :
	    if (name != null) bb = bf.createMethodBubble(proj,name);
	    break;
	 case FIELDS :
	    bb = bf.createFieldsBubble(proj,head);
	    break;
	 case STATICS :
	    bb = bf.createStaticsBubble(proj,head);
	    break;
	 case HEADER :
	    bb = bf.createClassPrefixBubble(proj,head);
	    break;
	 case CLASS :
	    bb = bf.createClassBubble(proj,name);
	    break;
       }
    }

   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   
   if (typ.equals("FRAGMENT") && bb instanceof BaleEditorBubble) {
      BaleEditorBubble eb = (BaleEditorBubble) bb;
      String proj = IvyXml.getAttrString(cnt,"PROJECT");
      String file = IvyXml.getAttrString(cnt,"FILE");
      String name = IvyXml.getAttrString(cnt,"NAME");
      BaleDocument bd = (BaleDocument) eb.getContentDocument();
      if (bd.getFile().getPath().equals(file) &&
            bd.getProjectName().equals(proj) &&
            name.equals(bd.getFragmentName())) 
         return true;
    }
   
   return false;
}
      
/********************************************************************************/
/*										*/
/*	Port creation methods							*/
/*										*/
/********************************************************************************/

@Override public BudaConstants.LinkPort createPort(BudaBubble bb,Element xml)
{
   if (bb == null || !(bb instanceof BaleEditorBubble)) return null;

   BaleEditorBubble beb = (BaleEditorBubble) bb;
   BaleFragmentEditor bfe = (BaleFragmentEditor) beb.getContentPane();
   BaleDocument bd = bfe.getDocument();

   int lno = IvyXml.getAttrInt(xml,"LINE");
   int off = bd.findLineOffset(lno);

   try {
      return new BaleLinePort(beb,bd.createPosition(off),null);
    }
   catch (BadLocationException e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
@Override public void loadXml(BudaBubbleArea bba,Element root)		{ }





}	// end of class BaleConfigurator




/* end of BaleConfigurator.java */
