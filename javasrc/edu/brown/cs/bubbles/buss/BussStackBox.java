/********************************************************************************/
/*										*/
/*		BussStackBox.java						*/
/*										*/
/*	BUbble Stack Strategies bubble stack contents				*/
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


package edu.brown.cs.bubbles.buss;

import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BubbleViewCallback;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaWorkingSet;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

class BussStackBox extends JTree implements BussConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BussBubble buss_bubble;
private BussTreeModel tree_model;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BussStackBox(BussTreeModel mdl, int contentWidth, BussBubble bussbubble)
{
   super(mdl);

   tree_model = mdl;
   buss_bubble = bussbubble;

   BoardProperties props = BoardProperties.getProperties("Buss");
   Font ft = props.getFont(BUSS_STACK_FONT_PROP,BUSS_STACK_FONT);
   Font ftb = ft.deriveFont(Font.BOLD);

   TreeSelectionModel smdl = getSelectionModel();
   smdl.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
   setRootVisible(false);
   setScrollsOnExpand(true);
   setShowsRootHandles(false);
   setVisibleRowCount(15);
   setFont(ftb);
   setDragEnabled(true);
   setTransferHandler(new Transferer());
   BudaCursorManager.setCursor(this,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   setCellRenderer(new BussCellRenderer(contentWidth, bussbubble));
   setRowHeight(0);
   setOpaque(false);

   addTreeSelectionListener(new Selector());

   registerKeyAction(new ShowAllAction(),"SHOW_ALL",KeyStroke.getKeyStroke(KeyEvent.VK_F4,0));
   registerKeyAction(new ShowSelectedAction(),"SHOW_SELECTED",KeyStroke.getKeyStroke(KeyEvent.VK_F3,0));
   registerKeyAction(new DeleteSelectedAction(),"DELETE_SELECTED",KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
   
   BasicTreeUI tui = (BasicTreeUI) getUI();
   tui.setCollapsedIcon(null);
   tui.setExpandedIcon(null);
   tui.setLeftChildIndent(BUSS_TREE_INDENT);
   tui.setRightChildIndent(BUSS_TREE_INDENT);

   expandAll();

   BudaRoot.addBubbleViewCallback(new BussBubbleCallback(buss_bubble));
}




/********************************************************************************/
/*										*/
/*	Tree actions								*/
/*										*/
/********************************************************************************/

private void expandAll()
{
   for (int i = 0; i < getRowCount(); ++i) {
      expandRow(i);
    }

   if (getRowCount() > 0) expandRow(0);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","STACK");
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

protected void paintComponent(Graphics g)
{
   Graphics2D g2 = (Graphics2D) g.create();
   Dimension sz = getSize();
   Paint p = new GradientPaint(0f,0f,BUSS_STACK_TOP_COLOR,0f,sz.height,BUSS_STACK_BOTTOM_COLOR);
   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   g2.setPaint(p);
   g2.fill(r);

   super.paintComponent(g);
}



/********************************************************************************/
/*										*/
/*	Drag/drop handler							*/
/*										*/
/********************************************************************************/

private class Transferer extends TransferHandler {

   private static final long serialVersionUID = 1;


   @Override protected Transferable createTransferable(JComponent c) {
      BussStackBox bsb = (BussStackBox) c;
      TreePath [] tp = bsb.getSelectionPaths();
      if (tp == null) return null;
      TransferBubble tb = new TransferBubble(tp);
      if (!tb.isValid()) return null;
      return tb;
    }

   @Override public int getSourceActions(JComponent c) {
      return COPY;
    }

   @Override protected void exportDone(JComponent c,Transferable t,int action) {
      if (t instanceof TransferBubble) {
         TransferBubble tb = (TransferBubble) t;
         for (BussEntry be : tb.getEntries()) {
            if (be.getBubble() != null && be.getBubble().getParent() != buss_bubble.getLayeredPane()) {
               be.getBubble().setFixed(false);
               tree_model.removeEntry(be);
             }
          }
       }
    }
}	// end of inner class Transferer



private static class TransferBubble implements Transferable, BudaConstants.BudaDragBubble {

   private List<BussEntry> tree_entries;

   TransferBubble(TreePath [] pths) {
      tree_entries = new ArrayList<BussEntry>();
      for (int i = 0; i < pths.length; ++i) {
         BussTreeNode tn = (BussTreeNode) pths[i].getLastPathComponent();
         BussEntry be = tn.getEntry();
         if (be != null) tree_entries.add(be);
       }
    }

   boolean isValid()			{ return tree_entries.size() > 0; }
   List<BussEntry> getEntries() 	{ return tree_entries; }


   @Override public Object getTransferData(DataFlavor df) {
      if (df == BudaRoot.getBubbleTransferFlavor()) return this;
      return null;
    }

   @Override public DataFlavor [] getTransferDataFlavors() {
       return new DataFlavor [] { BudaRoot.getBubbleTransferFlavor() };
     }

   @Override public boolean isDataFlavorSupported(DataFlavor f) {
      if (f.equals(BudaRoot.getBubbleTransferFlavor())) return true;
      return false;
    }

   @Override public BudaBubble [] createBubbles() {
      BudaBubble [] rslt = new BudaBubble[tree_entries.size()];
      int i = 0;
      for (BussEntry ent : tree_entries) {
	 BudaBubble bb = ent.getBubble();
	 if (bb != null) {
	    bb.setFixed(false);
	    rslt[i++] = bb;
	 }
       }
      return rslt;
    }

}	// end of inner class TransferBubble




/********************************************************************************/
/*										*/
/*	Selection change							*/
/*										*/
/********************************************************************************/

private class Selector implements TreeSelectionListener
{
   @Override public void valueChanged(TreeSelectionEvent e) {
      TreePath tp = e.getNewLeadSelectionPath();
      if (tp == null){
         return;
       }
      BussTreeNode tn = (BussTreeNode) tp.getLastPathComponent();
      tree_model.setSelection(tn);
    }

}	// end of inner class Selector



/********************************************************************************/
/*										*/
/*	Keyboard action methods 						*/
/*										*/
/********************************************************************************/

private void registerKeyAction(Action act,String cmd,KeyStroke k)
{
   getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(k,cmd);
   getActionMap().put(cmd,act);
}



private void createAllBubbles()
{
   Rectangle loc = BudaRoot.findBudaLocation(this);
   BudaRoot broot = BudaRoot.findBudaRoot(this);
   BudaBubble bbl = BudaRoot.findBudaBubble(this);

   if (bbl != null) bbl.setVisible(false);

   createAllBubbles((TreeNode) tree_model.getRoot(),broot,loc);
}



private void createSelectedBubble()
{
   TreePath tp = getSelectionPath();
   if (tp == null) return;

   TreeNode tn = (TreeNode) tp.getLastPathComponent();

   Rectangle loc = BudaRoot.findBudaLocation(this);
   BudaRoot broot = BudaRoot.findBudaRoot(this);
   BudaBubble bbl = BudaRoot.findBudaBubble(this);

   if (bbl != null) bbl.setVisible(false);

   createAllBubbles(tn,broot,loc);
}


private void deleteSelected()
{
   TreePath tp = getSelectionPath();
   if (tp == null) return;
   
   tree_model.setSelection(null);
   BudaBubble bbl = buss_bubble.getEditorBubble();
   if (bbl != null) bbl.setVisible(false);
   buss_bubble.removeEditorBubble();
   buss_bubble.setPreviewBubble(null);
   
   TreeNode tn = (TreeNode) tp.getLastPathComponent();
   if (tn == null) return;
   
   BussEntry be = ((BussTreeNode) tn).getEntry();
   if (be != null) {
      tree_model.removeEntry(be);
    }
}




private void createAllBubbles(TreeNode tn,BudaRoot br,Rectangle loc)
{
   if (br == null) return;

   BussEntry ent = ((BussTreeNode) tn).getEntry();

   if (ent != null) {
      createEntryBubble(ent,br,loc);
    }
   else {
      for (Enumeration<?> e = tn.children(); e.hasMoreElements(); ) {
	 TreeNode ctn = (TreeNode) e.nextElement();
	 createAllBubbles(ctn,br,loc);
       }
    }
}




private void createEntryBubble(BussEntry be,BudaRoot root,Rectangle loc)
{
   BudaBubble bb = be.getBubble();
   BussBubble bbl = (BussBubble) BudaRoot.findBudaBubble(this);
   if (bb == null || bbl == null) return;

   BudaBubble sbb = bbl.getSourceBubble();
   if (sbb != null && sbb.getContentName() != null && sbb.getContentFile() != null &&
	    sbb.getContentName().equals(bb.getContentName()) &&
	    sbb.getContentFile().equals(bb.getContentFile()))
      return;

   if (root != null && loc != null) {
      bb.setVisible(true);
      bb.setFixed(false);
      root.add(bb,new BudaConstraint(loc.x,loc.y));
      Dimension d = bb.getSize();
      loc.y += d.height + 25;
      bbl.addLinks(bb);
      bb.markBubbleAsNew();
    }
}



private class ShowAllAction extends AbstractAction implements ActionListener {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BUSS","ShowAll");
      createAllBubbles();
    }

}	// end of inner class ShowAllAction




private class ShowSelectedAction extends AbstractAction implements ActionListener {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BUSS","ShowSelected");
      createSelectedBubble();
    }

}	// end of inner class ShowSelectedAction




private class DeleteSelectedAction extends AbstractAction implements ActionListener {
   
   private static final long serialVersionUID = 1;
   
   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BUSS","DeleteSelected");
      deleteSelected();
    }
   
}	// end of inner class DeleteSelectedAction




/********************************************************************************/
/*										*/
/*	Classes for maintaining bubble windows as part of the bubble stack	*/
/*										*/
/********************************************************************************/

private static class BussBubbleCallback implements BubbleViewCallback {

   private static final double DISTANCE_LIMIT = 100;
   private BussBubble buss_bubble;

   BussBubbleCallback(BussBubble bussBubble){
      buss_bubble = bussBubble;
    }

   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }
   @Override public void bubbleAdded(BudaBubble bb)			{ }
   @Override public void bubbleRemoved(BudaBubble bb)			{ }
   @Override public void workingSetAdded(BudaWorkingSet ws)		{ }
   @Override public void workingSetRemoved(BudaWorkingSet ws)		{ }
   @Override public void doneConfiguration()				{ }
   @Override public void copyFromTo(BudaBubble f,BudaBubble t)		{ }

   @Override public boolean bubbleActionDone(BudaBubble bb) {
      if (buss_bubble.getEditorBubble() != bb) return false;
   
      Point editorloc = bb.getLocation();
      Point originallocation = buss_bubble.getEditorBubbleLocation();
   
      if (editorloc == null || originallocation == null) return false;
   
      double distance = Point.distance(editorloc.x, editorloc.y, originallocation.x, originallocation.y);
   
      if (distance <= DISTANCE_LIMIT) {
         bb.setFixed(true);
         buss_bubble.updateEditorBubbleLocation();
         return true;
       }
      else {
         buss_bubble.setPreviewBubble(null);
         bb.setFixed(false);
         buss_bubble.removeEditorBubble();
       }
   
      return false;
   }

}	// end of inner class EditorBubbleCallback



}	// end of class BussStackBox




/* end of BussStackBox.java */
