/********************************************************************************/
/*										*/
/*		BaleFragmentEditor.java 					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor widget 		*/
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

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubblePosition;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.*;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;


class BaleFragmentEditor extends SwingGridPanel implements CaretListener, BaleConstants,
	BudaConstants.BudaBubbleOutputer, BumpConstants.BumpProblemHandler,
	BumpConstants.BumpBreakpointHandler, BaleConstants.BaleAnnotationListener
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private EditorPane	editor_pane;
private BaleViewport	editor_viewport;
private BaleAnnotationArea annot_area;
private BaleCrumbBar	crumb_bar;
private BaleFindBar	find_bar;
private BaleDocumentIde base_document;
private BaleFragmentType fragment_type;
private List<BaleRegion> fragment_regions;
private String		fragment_name;
private int		start_cline;
private int		end_cline;
private Map<BumpProblem,ProblemAnnot> problem_annotations;
private Map<BumpBreakpoint,BreakpointAnnot> breakpoint_annotations;
private Set<BaleAnnotation> document_annotations;
private boolean 	check_annotations;


private static final long serialVersionUID = 1;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFragmentEditor(String proj,File file,String name,BaleDocumentIde fdoc,BaleFragmentType typ,
		      List<BaleRegion> regions)
{
   base_document = fdoc;
   fdoc.checkProjectName(proj);
   fragment_type = typ;
   fragment_regions = new ArrayList<BaleRegion>(regions);
   fragment_name = name;
   check_annotations = true;

   setInsets(0);

   editor_pane = new EditorPane();
   editor_pane.addMouseListener(new BudaConstants.FocusOnEntry());

   crumb_bar = new BaleCrumbBar(editor_pane, fragment_name);
   annot_area = new BaleAnnotationArea(editor_pane);
   editor_viewport = new BaleViewport(editor_pane,annot_area);

   find_bar = new BaleFindBar(editor_pane);
   find_bar.setVisible(false);

   problem_annotations = new HashMap<BumpProblem,ProblemAnnot>();
   breakpoint_annotations = new HashMap<BumpBreakpoint,BreakpointAnnot>();
   document_annotations = new HashSet<BaleAnnotation>();

   addGBComponent(crumb_bar,0,0,3,1,1,0);
   addGBComponent(editor_viewport,1,1,2,1,10,10);

   setComponentZOrder(find_bar,0);

   editor_pane.addCaretListener(this);

   BumpClient.getBump().addProblemHandler(fdoc.getFile(),this);
   BumpClient.getBump().addBreakpointHandler(fdoc.getFile(),this);
   BaleFactory.getFactory().addAnnotationListener(this);

   for (BumpProblem bp : BumpClient.getBump().getProblems(fdoc.getFile())) {
      handleProblemAdded(bp);
    }

   for (BumpBreakpoint bb : BumpClient.getBump().getBreakpoints(fdoc.getFile())) {
      handleBreakpointAdded(bb);
    }

   System.err.println("CHECK ANNOTATIONS");
   for (BaleAnnotation ba : BaleFactory.getFactory().getAnnotations(getDocument())) {
      annotationAdded(ba);
    }
}



void dispose()
{
   editor_pane.dispose();
   editor_pane.removeCaretListener(this);
   BumpClient.getBump().removeProblemHandler(this);
   BumpClient.getBump().removeBreakpointHandler(this);
   BaleFactory.getFactory().removeAnnotationListener(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BaleDocument getDocument()		{ return editor_pane.getBaleDocument(); }

BaleEditorPane getEditor()		{ return editor_pane; }

BaleFragmentType getFragmentType()	{ return fragment_type; }




/********************************************************************************/
/*										*/
/*	Handle initial sizing							*/
/*										*/
/********************************************************************************/

void setInitialSize(Dimension d)
{
   Dimension eps = editor_pane.getPreferredSize();
   BaleDocument bd = getDocument();

   if (eps.height > d.height || eps.width > d.width) {
      // bd.recheckElisions();
      editor_pane.setSize(d);
      eps = editor_pane.getPreferredSize();
   }

   eps.width += BALE_ANNOT_WIDTH;

   Dimension cbs = crumb_bar.getPreferredSize();
   if (cbs.width > eps.width) {
      cbs.width = eps.width;
      crumb_bar.setSize(cbs);
      crumb_bar.setPreferredSize(cbs);
    }

   editor_viewport.setPreferredSize(eps);
   invalidate();
   Dimension xps = getPreferredSize();
   setSize(xps);
   bd.fixElisions();
}




/********************************************************************************/
/*										*/
/*	Handle caret events							*/
/*										*/
/********************************************************************************/

@Override public void caretUpdate(CaretEvent evt)
{
   int pos0 = evt.getDot();
   int pos1 = evt.getMark();
   if (pos0 > pos1) {
      int t = pos0;
      pos0 = pos1;
      pos1 = t;
    }

   BaleDocumentFragment bd = (BaleDocumentFragment) editor_pane.getDocument();

   int ln0 = bd.findLineNumber(pos0);
   int ln1 = bd.findLineNumber(pos1);
   if (ln0 == start_cline && ln1 == end_cline) return;

   int soff = bd.findLineOffset(ln0);
   int eoff = bd.findLineOffset(ln1+1)-1;
   bd.setCursorRegion(soff,eoff-soff);
}



/********************************************************************************/
/*										*/
/*	Handle context menu							*/
/*										*/
/********************************************************************************/

void handleContextMenu(MouseEvent e)
{
   Point p = new Point(e.getXOnScreen(),e.getYOnScreen());
   SwingUtilities.convertPointFromScreen(p,this);
   Component c = SwingUtilities.getDeepestComponentAt(this,p.x,p.y);
   while (c != null && c != this) {
      if (c == crumb_bar) {
	 convertMouseEvent(e,p,c);
	 crumb_bar.handleContextMenu(e);
	 break;
       }
      else if (c == find_bar) {
	 convertMouseEvent(e,p,c);
	 break;
       }
      else if (c == annot_area) {
	 convertMouseEvent(e,p,c);
	 annot_area.handleContextMenu(e);
	 break;
       }
      else if (c == editor_pane) {
	 convertMouseEvent(e,p,c);
	 editor_pane.handleContextMenu(e);
	 break;
       }
      c = c.getParent();
    }
}


private void convertMouseEvent(MouseEvent e,Point p,Component c)
{
   Point pt = SwingUtilities.convertPoint(this,p,c);
   e.translatePoint(pt.x - e.getX(),pt.y - e.getY());
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   if (check_annotations) checkInitialAnnotations();

   super.paint(g);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BALE"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","FRAGMENT");
   xw.field("FRAGTYPE",fragment_type);
   xw.field("FILE",base_document.getFile().getPath());
   xw.field("PROJECT",base_document.getProjectName());
   String fnm = getDocument().getFragmentName();
   if (fnm != null) fragment_name = fnm;
   xw.field("NAME",fragment_name);
}



/********************************************************************************/
/*										*/
/*	Methods to handle problems relevant to this fragment			*/
/*										*/
/********************************************************************************/

@Override public void handleProblemAdded(BumpProblem bp)
{
   BaleDocument bd = getDocument();
   int soff = bd.mapOffsetToJava(bp.getStart());

   if (soff >= 0) {
      try {
	 ProblemAnnot pa = new ProblemAnnot(bp,bd,bd.createPosition(soff));
	 problem_annotations.put(bp,pa);
	 annot_area.addAnnotation(pa);
       }
      catch (BadLocationException e) { }
    }
}



@Override public void handleProblemRemoved(BumpProblem bp)
{
   ProblemAnnot pa = problem_annotations.get(bp);
   if (pa != null) annot_area.removeAnnotation(pa);
}



@Override public void handleProblemsDone()
{ }




/********************************************************************************/
/*										*/
/*	Methods to handle breakpoints for the fragment				*/
/*										*/
/********************************************************************************/

@Override public void handleBreakpointAdded(BumpBreakpoint bb)
{
   BaleDocument bd = getDocument();
   int lno = bb.getLineNumber();
   if (lno < 0) return;

   int soff = bd.findLineOffset(lno);

   if (soff >= 0) {
      try {
	 BreakpointAnnot ba = new BreakpointAnnot(bb,bd,bd.createPosition(soff));
	 breakpoint_annotations.put(bb,ba);
	 annot_area.addAnnotation(ba);
       }
      catch (BadLocationException e) { }
    }
}



@Override public void handleBreakpointRemoved(BumpBreakpoint bb)
{
   BreakpointAnnot ba = breakpoint_annotations.get(bb);
   if (ba != null) annot_area.removeAnnotation(ba);
}



@Override public void handleBreakpointChanged(BumpBreakpoint bb)
{
   BreakpointAnnot ba = breakpoint_annotations.get(bb);
   if (ba != null) {
      annot_area.removeAnnotation(ba);
      annot_area.addAnnotation(ba);
    }
}




/********************************************************************************/
/*										*/
/*	Editor Pane specializations						*/
/*										*/
/********************************************************************************/

private class EditorPane extends BaleEditorPane implements BaleEditor {

   private static final long serialVersionUID = 1;

   EditorPane() { }

   protected EditorKit createDefaultEditorKit() {
      return new BaleEditorKit.FragmentKit(base_document,fragment_type,fragment_regions);
    }

   @Override public BaleFindBar getFindBar()			{ return find_bar; }
   @Override public BaleAnnotationArea getAnnotationArea()	{ return annot_area; }

   @Override void toggleFindBar() {
      if (find_bar.isVisible()) {
	 BudaBubble bb = BudaRoot.findBudaBubble(find_bar);
	 if (bb != null && bb.isVisible()) {
	    find_bar.setVisible(false);
	    return;
	 }
       }

      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      Rectangle bounds = BudaRoot.findBudaBubble(this).getBounds();
      int findwidth = find_bar.getWidth();
      bounds.x = bounds.x + (bounds.width/2) - (findwidth/2);
      bba.add(find_bar, new BudaConstraint(BudaBubblePosition.FIXED, bounds.x, bounds.y+bounds.height));
      bba.setLayer(BudaRoot.findBudaBubble(find_bar), 1);
      find_bar.setVisible(true);
   }

}	// end of inner class EditorPane




/********************************************************************************/
/*										*/
/*	FindBar methods 							*/
/*										*/
/********************************************************************************/

void hideFindBar() {
   if (find_bar.isVisible()) editor_pane.toggleFindBar();
}



void relocateFindBar()
{
   if (find_bar.isVisible()) {
      BudaBubble bb = BudaRoot.findBudaBubble(this);
      if (bb == null) return;
      Rectangle bounds = bb.getBounds();
      int findwidth = find_bar.getWidth();
      bounds.x = bounds.x + (bounds.width/2) - (findwidth/2);
      BudaBubble findbubble = BudaRoot.findBudaBubble(find_bar);
      if (findbubble == null) return;
      findbubble.setLocation(bounds.x, bounds.y + bounds.height);
   }
}



/********************************************************************************/
/*										*/
/*	Annotation methods for outside (document) annotations			*/
/*										*/
/********************************************************************************/

@Override public void annotationAdded(BaleAnnotation ba)
{
   System.err.println("ADD ANNOTATION " + ba);
   if (ba.getFile() == null) return;
   if (!ba.getFile().equals(getDocument().getFile())) return;

   int fragoffset = getDocument().getFragmentOffset(ba.getDocumentOffset());
   if (fragoffset < 0) return;

   synchronized (document_annotations) {
      document_annotations.add(ba);
    }
   annot_area.addAnnotation(ba);
   BudaBubble bb = BudaRoot.findBudaBubble(this);

   if (ba.getForceVisible(bb)) {
      try {
	 Position p0 = getDocument().createPosition(fragoffset);
	 SwingUtilities.invokeLater(new ForceVisible(p0));
       }
      catch (BadLocationException e) { }
    }

   if (ba.getLineColor() != null) repaint();
}



@Override public void annotationRemoved(BaleAnnotation ba)
{
   synchronized (document_annotations) {
      if (!document_annotations.contains(ba)) return;
    }
   annot_area.removeAnnotation(ba);
}



void checkInitialAnnotations()
{
   if (!check_annotations) return;
   check_annotations = false;

   BudaBubble bb = BudaRoot.findBudaBubble(this);

   synchronized (document_annotations) {
      for (BaleAnnotation ba : document_annotations) {
	 int fragoffset = getDocument().getFragmentOffset(ba.getDocumentOffset());
	 if (fragoffset < 0) continue;
	 if (ba.getForceVisible(bb)) {
	    try {
	       Position p0 = getDocument().createPosition(fragoffset);
	       SwingUtilities.invokeLater(new ForceVisible(p0));
	     }
	    catch (BadLocationException e) { }
	  }
       }
    }
}



private class ForceVisible implements Runnable {

   private Position for_position;

   ForceVisible(Position p) {
      for_position = p;
    }

   @Override public void run() {
      int fragoffset = for_position.getOffset();
      BoardLog.logD("BALE","Force visible " + fragoffset);
      if (fragoffset < 0) return;

      boolean chng = false;
      for ( ; ; ) {
	 BaleElement be = getDocument().getCharacterElement(fragoffset);
	 if (be == null || !be.isElided()) break;
	 be.setElided(false);
	 BoardLog.logD("BALE","Unelide");
	 chng = true;
       }
      if (chng) {
	 BaleDocument bd = getDocument();
	 bd.handleElisionChange();
	 repaint();
	 SwingUtilities.invokeLater(this);
	 return;
       }

      try {
	 Rectangle r = editor_pane.modelToView(fragoffset);
	 if (r != null) {
	    editor_pane.scrollRectToVisible(r);
	    BoardLog.logD("BALE","Scroll to visible " + r);
	  }
       }
      catch (BadLocationException e) { }
      repaint();
    }

}	// end of inner class ForceVisible



/********************************************************************************/
/*										*/
/*	Debugging Routines							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return super.toString() + " " + fragment_name + " " + hashCode();
}




/********************************************************************************/
/*										*/
/*	Problem annotations							*/
/*										*/
/********************************************************************************/

private static class ProblemAnnot implements BaleAnnotation {

   private BumpProblem for_problem;
   private BaleDocument for_document;
   private Position error_pos;

   ProblemAnnot(BumpProblem bp,BaleDocument bd,Position p) {
      for_problem = bp;
      for_document = bd;
      error_pos = p;
    }

   @Override public File getFile()		{ return for_document.getFile(); }
   @Override public int getDocumentOffset() {
      return for_document.getDocumentOffset(error_pos.getOffset());
    }

   @Override public Icon getIcon() {
      switch (for_problem.getErrorType()) {
	 case FATAL :
	 case ERROR :
	    return BoardImage.getIcon("error");
	 case WARNING :
	    return BoardImage.getIcon("warning");
	 case NOTICE :
	    return BoardImage.getIcon("notice");
       }
      return null;
    }

   @Override public String getToolTip() {
      return IvyXml.xmlSanitize(for_problem.getMessage());
    }

   @Override public Color getLineColor()			{ return null; }
   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }
   @Override public int getPriority()				{ return 10; }
   @Override public void addPopupButtons(JPopupMenu m)		{ }

}	// end of inner class ProblemAnnot




/********************************************************************************/
/*										*/
/*	Breakpoint annotations							*/
/*										*/
/********************************************************************************/

private static class BreakpointAnnot implements BaleAnnotation {

   private BumpBreakpoint for_breakpoint;
   private BaleDocument for_document;
   private Position break_pos;

   BreakpointAnnot(BumpBreakpoint bb,BaleDocument bd,Position p) {
      for_breakpoint = bb;
      for_document = bd;
      break_pos = p;
    }

   @Override public File getFile()		{ return for_document.getFile(); }
   @Override public int getDocumentOffset() {
      return for_document.getDocumentOffset(break_pos.getOffset());
    }

   @Override public Icon getIcon() {
      // TODO: different images if enabled/disabled/conditional/...
      boolean enable = for_breakpoint.getBoolProperty("ENABLED");
      boolean trace = for_breakpoint.getBoolProperty("TRACEPOINT");
      if (!enable) return BoardImage.getIcon("breakdisable");
      else if (trace) return BoardImage.getIcon("trace");

      return BoardImage.getIcon("break");
    }

   @Override public String getToolTip() {
      // TODO: this should be more meaningful
      boolean enable = for_breakpoint.getBoolProperty("ENABLED");
      boolean trace = for_breakpoint.getBoolProperty("TRACEPOINT");
      int line = for_breakpoint.getLineNumber();
      String id = (trace ? "Tracepoint" : "Breakpoint");
      if (!enable) id = "Disabled " + id;
      if (line > 0) id += " at line " + line;
      return id;
    }

   @Override public Color getLineColor()			{ return null; }
   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }
   @Override public int getPriority()				{ return 5; }
   @Override public void addPopupButtons(JPopupMenu m)		{ }

}	// end of inner class BreakpointAnnot



}	// end of class BaleFragmentEditor




/* end of BaleFragmentEditor.java */



