/********************************************************************************/
/*										*/
/*		BaleViewport.java						*/
/*										*/
/*	Bubble Annotated Language Editor viewport for fragment editors		*/
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
import edu.brown.cs.bubbles.buda.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.awt.event.*;



class BaleViewport extends JScrollPane implements CaretListener, MouseWheelListener,
		BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JEditorPane	editor_pane;
private JPanel		annot_area;
private CombinedPanel	combined_panel;
private int		last_height;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleViewport(JEditorPane editor,JPanel annot)
{
   super(VERTICAL_SCROLLBAR_NEVER,HORIZONTAL_SCROLLBAR_NEVER);

   editor_pane = editor;
   annot_area = annot;
   combined_panel = null;

   if (annot == null) setViewportView(editor);
   else if (editor == null) setViewportView(annot);
   else {
      // TODO: might want to use a gridbag layout to allow empty space at bottom
      combined_panel = new CombinedPanel();
      setViewportView(combined_panel);
    }
   setViewportBorder(BorderFactory.createEmptyBorder());
   setBorder(BorderFactory.createEmptyBorder());

   last_height = 0;

   if (editor != null) editor.addCaretListener(this);
   addComponentListener(new ViewportManager());
   getViewport().addChangeListener(new ViewManager());
   setWheelScrollingEnabled(false);
   addMouseWheelListener(this);

   JScrollBar sb = getVerticalScrollBar();
   sb.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
}
		


/********************************************************************************/
/*										*/
/*	Sizing logic								*/
/*										*/
/********************************************************************************/

private void handleViewportSized()
{
   Dimension sz = getViewport().getSize();
   BaleDocument bd = (BaleDocument) editor_pane.getDocument();
   Dimension vsz = getViewport().getViewSize();

   if (vsz.height > sz.height) {
      vsz.height = sz.height;
      getViewport().setViewSize(vsz);
    }

   if (sz.height > last_height && last_height != 0) {
      Dimension psz = editor_pane.getPreferredSize();	// size for current elisions
      Dimension tsz = editor_pane.getMaximumSize();	// size without elisions
      // TODO: get max size here doesn't work
      if (sz.height > tsz.height && tsz.height != psz.height) {
	 bd.recheckElisions();
       }
    }

   last_height = sz.height;

   int soff = editor_pane.getCaretPosition();
   try {
      Rectangle r = editor_pane.modelToView(soff);
      Component c1 = getViewport().getView();
      if (r == null || c1 == null) return;
      r = SwingUtilities.convertRectangle(editor_pane,r,c1);
      getViewport().scrollRectToVisible(r);
    }
   catch (BadLocationException ex) { }
}



private void checkScrollBars()
{
   Dimension vsz = getViewport().getViewSize();
   Dimension xsz = getSize();

   int ht0 = vsz.height;
   int ht1 = xsz.height;
   int pct = BALE_SCROLL_MINIMUM;
   if (pct <= 100 || ht1 == 0 || vsz.width == 0 || combined_panel == null) return;
   if (ht0 < BALE_SCROLL_MIN_HT) return;

   if (ht1 * pct / 100 * 0.80 > ht0) {
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
    }
   else if (ht1 * pct / 100 < ht0) {
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
    }
}




/********************************************************************************/
/*										*/
/*	Scrolling logic 							*/
/*										*/
/********************************************************************************/

private void handleScroll(int nline,boolean lr)
{
   BudaRoot.findBudaBubbleArea(this).invalidateLinks();
   Point p0 = getViewport().getViewPosition();
   Dimension sz = getViewport().getExtentSize();
   Dimension vsz = getViewport().getViewSize();
   BaleDocument bd = (BaleDocument) editor_pane.getDocument();

   int linetop = bd.findLineNumber(0);
   int off0 = editor_pane.viewToModel(p0);
   int line0 = bd.findLineNumber(off0);
   int line1 = -1;

   BaleElement be = bd.getCharacterElement(off0);
   while (be != null && !be.isLineElement() && !be.isElided())
      be = be.getBaleParent();
   int delta = Math.abs(nline);
   while (be != null && delta > 0) {
      int off1;
      if (nline > 0) off1 = be.getEndOffset() + 1;
      else off1 = be.getStartOffset() - 1;
      --delta;
      be = bd.getCharacterElement(off1);
      while (be != null && !be.isLineElement() && !be.isElided())
	 be = be.getBaleParent();
    }
   if (be != null) {
      int off2 = be.getStartOffset();
      line1 = bd.findLineNumber(off2);
    }

   if (line1 < 0) {
      line1 = line0 + nline;
      if (line1 < linetop) line1 = linetop;
      if (line1 == line0) return;
    }

   try {
      Rectangle r0;
      for ( ; ; ) {
	 int off1 = bd.findLineOffset(line1);
	 if (off1 < 0) return;
	 if (off1 >= bd.getLength()) return;
	 r0 = editor_pane.modelToView(off1);
	 int ybottom = r0.y + sz.height;
	 if (ybottom < vsz.height + r0.height) break;
	 line1 -= 1;
	 if (line1 < linetop) return;
	 if (line1 == line0) break;
       }
      p0.y = r0.y;
      getViewport().setViewPosition(p0);
      repaint();
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Scrolling problem: " + e);
    }
}



/********************************************************************************/
/*										*/
/*	Event handling methods and classes					*/
/*										*/
/********************************************************************************/

@Override public void caretUpdate(CaretEvent evt)
{
   return;
}



private class ViewportManager extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      handleViewportSized();
    }

}	// end of inner class ViewportManager



@Override public void mouseWheelMoved(MouseWheelEvent e)
{
   int nclk = e.getWheelRotation();
   int mods = e.getModifiers();

   if (nclk != 0) {
      BaleDocument bd = (BaleDocument) editor_pane.getDocument();
      bd.baleReadLock();
      try {
	 handleScroll(nclk,(mods & MouseWheelEvent.SHIFT_MASK) != 0);
       }
      finally { bd.baleReadUnlock(); }
    }
}



private class ViewManager implements ChangeListener {

   @Override public void stateChanged(ChangeEvent e) {
      checkScrollBars();
      BudaBubble bbl = BudaRoot.findBudaBubble(BaleViewport.this);
      if (bbl != null) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
	 if (bba != null) bba.forceReroute(bbl);
       }
    }

}	// end of inner class ViewManager




/********************************************************************************/
/*										*/
/*	Class to handle combining annotation and editor for viewport		*/
/*										*/
/********************************************************************************/

private class CombinedPanel extends JPanel implements Scrollable {

   private static final long serialVersionUID = 1;

   CombinedPanel() {
      super(new BorderLayout());

      add(annot_area,BorderLayout.WEST);
      add(editor_pane,BorderLayout.CENTER);
    }

   @Override public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

   @Override public int getScrollableBlockIncrement(Rectangle r,int o,int d) {
      return editor_pane.getScrollableBlockIncrement(r,o,d);
    }

   @Override public int getScrollableUnitIncrement(Rectangle r,int o,int d) {
      return editor_pane.getScrollableUnitIncrement(r,o,d);
    }

   @Override public boolean getScrollableTracksViewportHeight() { return false; }
   @Override public boolean getScrollableTracksViewportWidth()	{ return true; }

}	// end of inner class Combined Panel



}	// end of class BaleViewport



/* end of BaleViewport.java */
