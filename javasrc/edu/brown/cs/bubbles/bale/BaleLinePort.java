/********************************************************************************/
/*										*/
/*		BaleLinePort.java						*/
/*										*/
/*	Bubble Annotated Language Editor port at a given line number		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.buda.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;



class BaleLinePort implements BaleConstants, BudaConstants, BudaConstants.LinkPort {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane for_editor;
private Position       file_position;
private PortAnnotation port_annot;
private String	       port_description;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleLinePort(Component c,Position where,String desc)
{
   BaleFragmentEditor bfe = null;

   for (Component p = c; p != null; p = p.getParent()) {
      if (p instanceof BaleFragmentEditor) {
	 bfe = (BaleFragmentEditor) p;
	 break;
      }
   }
   if (bfe == null) {
      BudaBubble bb = BudaRoot.findBudaBubble(c);
      if (bb != null) bfe = (BaleFragmentEditor) bb.getContentPane();
   }

   if (bfe != null) for_editor = bfe.getEditor();
   port_description = desc;

   file_position = where;

   port_annot = null;
   if (for_editor != null) {
      BaleAnnotationArea baa = for_editor.getAnnotationArea();
      if (baa != null) {
	 port_annot = new PortAnnotation();
	 baa.addAnnotation(port_annot);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Point getLinkPoint(BudaBubble bb,Rectangle tgt)
{
   int y0 = computeLinePosition(bb);

   Rectangle r = bb.getBounds();

   int x0 = r.x;

   if (tgt.getX() + tgt.getWidth() > r.x) x0 = r.x + r.width - 1;

   return new Point(x0,y0 + r.y);
}




@Override public Point getLinkPoint(BudaBubble bb,Point2D tgt)
{
   int y0 = computeLinePosition(bb);

   Rectangle r = bb.getBounds();

   int x0 = r.x;
   y0 += r.y;

   if (tgt.getX() > r.x + r.width) x0 = r.x + r.width - 1;

   return new Point(x0,y0);
}




/********************************************************************************/
/*										*/
/*	Actual computation methods						*/
/*										*/
/********************************************************************************/

private int computeLinePosition(BudaBubble bb)
{
   Rectangle r;

   int len = for_editor.getDocument().getLength();
   int fpos = file_position.getOffset();
   if (fpos >= len) fpos = len - 1;
   if (fpos < 0) return -1;

   try {
      r = for_editor.modelToView(fpos);
      if (r == null) return -1;
   }
   catch (BadLocationException e) {
      return -1;
   }

   Rectangle br = bb.getBounds();

   int x0 = r.x + r.width / 2;
   int y0 = r.y + r.height / 2;
   Point p = SwingUtilities.convertPoint(for_editor, x0, y0, bb);
   if (p.y < 0 || p.y > br.height) {
      // Need to adjust size
      if (p.y < 0) p.y = 10;
      else p.y = br.height - 5;
   }

   return p.y;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw)
{
   BaleDocument bd = (BaleDocument) for_editor.getDocument();
   int lno = bd.findLineNumber(file_position.getOffset());

   xw.begin("PORT");
   xw.field("CONFIG", "BALE");
   xw.field("LINE", lno);
   xw.end("PORT");
}



/********************************************************************************/
/*										*/
/*	Removal methods 							*/
/*										*/
/********************************************************************************/

@Override public void noteRemoved()
{
   if (port_annot != null) {
      BaleAnnotationArea baa = for_editor.getAnnotationArea();
      if (baa != null) baa.removeAnnotation(port_annot);
      port_annot = null;
   }
}



/********************************************************************************/
/*										*/
/*	Line port annotation							*/
/*										*/
/********************************************************************************/

private class PortAnnotation implements BaleAnnotation {

   PortAnnotation()						{}

   @Override public File getFile()				{ return for_editor.getBaleDocument().getFile(); }

   @Override public int getDocumentOffset() {
      return for_editor.getBaleDocument().getDocumentOffset(file_position.getOffset());
    }

   @Override public Icon getIcon()				{ return null; }

   @Override public String getToolTip() 			{ return port_description; }

   @Override public Color getLineColor()			{ return BALE_PORT_ANNOT_COLOR; }

   @Override public Color getBackgroundColor()			{ return null; }

   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }

   @Override public int getPriority()				{ return 1; }

   @Override public void addPopupButtons(Component c,JPopupMenu m) { }

}	// end of inner class PortAnnotation


}	// end of class BaleLinePort




/* end of BaleLinePort.java */
