/********************************************************************************/
/*										*/
/*		BaleApplyEdits.java						*/
/*										*/
/*	Bubble Annotated Language Editor context for applying eclipse edits	*/
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

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;

import java.io.File;
import java.util.*;



class BaleApplyEdits implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument	for_document;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleApplyEdits()
{
   for_document = null;
}


BaleApplyEdits(BaleDocument bd)
{
   for_document = bd;
}



/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

void applyEdits(Element xml)
{
   if (IvyXml.isElement(xml,"EDITS") || IvyXml.isElement(xml,"RESULT")) {
      for (Element c : IvyXml.children(xml)) applyEdits(c);
    }
   else if (IvyXml.isElement(xml,"CHANGE")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return;
      else if (typ.equals("COMPOSITE")) {
	 for (Element c : IvyXml.children(xml)) applyEdits(c);
       }
      else if (typ.equals("EDIT")) {
	 Element r = IvyXml.getChild(xml,"RESOURCE");
	 if (r != null) {
	    String proj = IvyXml.getAttrString(r,"PROJECT");
	    File fil = new File(IvyXml.getAttrString(r,"LOCATION"));
	    BaleDocumentIde bde = BaleFactory.getFactory().getDocument(proj,fil);
	    bde.flushEdits();
	    for_document = bde;
	  }
	 Set<Element> edits = new TreeSet<Element>(new EditSorter());
	 for (Element ed : IvyXml.children(xml,"EDIT")) {
	    edits.add(ed);
	  }
	 for (Element ed : edits) {
	    applyEdit(ed);
	  }
       }
    }
   else if (IvyXml.isElement(xml,"EDIT")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return;
      if (typ.equals("MULTI")) {
	 Set<Element> edits = new TreeSet<Element>(new EditSorter());
	 for (Element ed : IvyXml.children(xml,"EDIT")) {
	    edits.add(ed);
	  }
	 for (Element ed : edits) {
	    applyEdit(ed);
	  }
       }
      else applyEdit(xml);
    }
}




/********************************************************************************/
/*										*/
/*	Sorter to do edits from bottom to top					*/
/*										*/
/********************************************************************************/

private static class EditSorter implements Comparator<Element> {

   @Override public int compare(Element e1,Element e2) {
      int off1 = IvyXml.getAttrInt(e1,"OFFSET");
      int off2 = IvyXml.getAttrInt(e2,"OFFSET");
      return off2 - off1;
    }

}	// end of inner class EditSorter




/********************************************************************************/
/*										*/
/*	Method to do an actual edit						*/
/*										*/
/********************************************************************************/

private void applyEdit(Element ed)
{
   int off = IvyXml.getAttrInt(ed,"OFFSET");
   int len = IvyXml.getAttrInt(ed,"LENGTH");
   int eoff = off+len;
   int off1 = for_document.mapOffsetToJava(off);
   int off2 = for_document.mapOffsetToJava(eoff);
   int len1 = off2-off1;

   String typ = IvyXml.getAttrString(ed,"TYPE");

   if (typ.equals("INSERT") || typ.equals("REPLACE") || typ.equals("DELETE")) {
      for_document.nextEditCounter();
      String txt = IvyXml.getTextElement(ed,"TEXT");
      try {
	 for_document.replace(off1,len1,txt,null);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem applying Eclipse text edit",e);
       }
    }
   else {
      for (Element ce : IvyXml.children(ed,"EDIT")) {
	 applyEdit(ce);
       }
    }
}



}	// end of class BaleApplyEdits




/* end of BaleApplyEdits.java */
