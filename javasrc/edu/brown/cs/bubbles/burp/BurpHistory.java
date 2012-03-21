/********************************************************************************/
/*										*/
/*		BurpHistory.java						*/
/*										*/
/*	Bubble Undo/Redo Processor history maintainer				*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.burp;

import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoableEdit;
import javax.swing.event.DocumentEvent;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 *	This class implements command history and undo/redo in the bubbles
 *	framework.
 *
 *	It actually supports intelligent undo/redo on a bubble-by-bubble
 *	basis.	Where commands affect multiple bubbles (which can happen
 *	for various reasons, e.g. a global name change or a simple edit
 *	where multiple bubbles share the same buffer), it ensures that all
 *	bubbles are kept consistent by undoing/redoing any dependent
 *	commands at the same time.
 *
 *	For example if I edit A, then do a global name change, then edit B,
 *	then do an undo in A, the global name change will be undone as will
 *	the edit to B.	A second undo in A will undo the initial edit to A.
 *
 **/

public class BurpHistory implements BurpConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BurpChangeData			current_change;
private BurpChangeData			first_change;
private Map<JTextComponent,BurpEditorData>  editor_map;
private Map<UndoableEdit,BurpChangeData>    change_map;

private static BurpHistory		the_history = null;
private static UndoRedoAction		undo_action = new UndoRedoAction(-1);
private static UndoRedoAction		redo_action = new UndoRedoAction(1);



/********************************************************************************/
/*										*/
/*	Static access methods							*/
/*										*/
/********************************************************************************/

/**
 *	Return the singular instance of the history module for all bubbles.
 **/

public synchronized static BurpHistory getHistory()
{
   if (the_history == null) {
      the_history = new BurpHistory();
    }
   return the_history;
}




/**
 *	Return the singluar instance of an UNDO editor action that can be
 *	associated with key strokes or invoked on mouse action.
 **/

public static Action getUndoAction()		{ return undo_action; }


/**
 *	Return the singular instance of a REDO editor action.
 **/

public static Action getRedoAction()		{ return redo_action; }




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BurpHistory()
{
   current_change = null;
   editor_map = new HashMap<JTextComponent,BurpEditorData>();
   change_map = new HashMap<UndoableEdit,BurpChangeData>();

   BumpClient.getBump().addChangeHandler(new ChangeHandler());
}



/********************************************************************************/
/*										*/
/*	Editor management methods						*/
/*										*/
/********************************************************************************/

/**
 *	Register an editor with the history mechanism.	This should be done when the
 *	editor is created.  UNDO/REDO is only supported in registered editors.
 **/

public void addEditor(JTextComponent be)
{
   BurpEditorData ed = new BurpEditorData(this,be);
   synchronized (editor_map) {
      editor_map.put(be,ed);
    }
}


/**
 *	Unregister an editor with the history mechanism.  This should be done when
 *	the editor is removed.
 *
 *	This should actually be done automatically when the editor is freed, but this
 *	can be problematic given all the links that might exist here and elsewhere to
 *	the editor.
 **/

public void removeEditor(JTextComponent be)
{
   synchronized (editor_map) {
      BurpEditorData ed = editor_map.remove(be);
      if (ed != null) ed.remove();
    }
}



/**
 *	Determine if the given registered editor is dirty, i.e. the underlying text
 *	has been edited, since the last save.
 **/

public boolean isDirty(JTextComponent be)
{
   BurpEditorData ed = editor_map.get(be);

   if (ed == null) return false;

   return ed.isDirty();
}



/**
 *	Determine if undo is possible for a component
 **/

public boolean canUndo(JTextComponent be)
{
   BurpEditorData ed = editor_map.get(be);

   if (ed == null) return false;

   BurpChangeData cd = ed.getCurrentChange();
   if (cd == null) return false;

   return cd.canUndo();
}




/**
 *	Determine if redo is possible for a component
 **/

public boolean canRedo(JTextComponent be)
{
   BurpEditorData ed = editor_map.get(be);

   if (ed == null) return false;

   BurpChangeData cd = ed.getCurrentChange();
   if (cd == null)  cd = ed.getFirstChange();
   else cd = cd.getNext(ed);
   if (cd == null) return false;

   return cd.canRedo();
}




/********************************************************************************/
/*										*/
/*	Undo/Redo request methods						*/
/*										*/
/********************************************************************************/

/**
 *	Undo the last edit in the registered editor.
 **/

public void undo(JTextComponent be)
{
   BoardMetrics.noteCommand("BURP","undo");

   BurpEditorData ed = null;
   if (be != null) {
      ed = editor_map.get(be);
      if (ed == null) return;
    }

   boolean havesig = false;
   while (!havesig) {
      BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
      if (cd == null) break;
      cd.addDependencies();
      if (!cd.canUndo()) break;
      cd.undo();
      if (cd.isSignificant()) havesig = true;
    }
}



/**
 *	Redo the last undone edit in the registered editor if that is possible.  If an
 *	intervening command was executed, the undo can not be redone.
 **/

public void redo(JTextComponent be)
{
   BoardMetrics.noteCommand("BURP","undo");

   BurpEditorData ed = null;
   if (be != null) {
      ed = editor_map.get(be);
      if (ed == null) return;
    }

   boolean havesig = false;
   while (!havesig) {
      BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
      if (cd == null) {
	 if (ed == null) cd = first_change;
	 else cd = ed.getFirstChange();
       }
      else cd = cd.getNext(ed);
      if (cd == null) break;
      if (!cd.canRedo()) break;
      cd.redo();
      if (cd.isSignificant()) havesig = true;
    }
}




/********************************************************************************/
/*										*/
/*	Methods for maintaing the current_change				*/
/*										*/
/********************************************************************************/

void resetCurrentChange(BurpChangeData when,BurpChangeData to,boolean fwd)
{
   if (current_change == when) current_change = to;
   else if (fwd && current_change == null) current_change = to;
}



/********************************************************************************/
/*										*/
/*	Internal list management methods					*/
/*										*/
/********************************************************************************/

synchronized void handleNewEdit(BurpEditorData ed,UndoableEdit ue,boolean evt,boolean sig)
{
   UndoableEdit bed = getBaseEdit(ue);

   BoardMetrics.noteCommand("BURP",getEditCommandName(ed,ue));

   BurpChangeData cd = change_map.get(bed);
   if (cd == null) {
      removeForward(null);
      cd = new BurpChangeData(this,ue,current_change);
      if (current_change == null) first_change = cd;
      current_change = cd;
      change_map.put(bed,cd);
    }

   if (evt) cd.setSignificant(sig);

   if (ed != null) {
      removeForward(ed);
      ed.addChange(cd);
    }
}



private UndoableEdit getBaseEdit(UndoableEdit ed)
{
   if (ed instanceof BurpSharedEdit) {
      BurpSharedEdit bde = (BurpSharedEdit) ed;
      ed = bde.getBaseEdit();
    }

   return ed;
}



private void removeForward(BurpEditorData ed)
{
   // remove any changes after cd for the given editor
   // if ed is null, remove all changes after ed
   // assumes that any forward changes have been undone

   BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
   if (cd == null) return;

   BurpChangeData next = null;
   for (BurpChangeData ncd = cd.getNext(ed); ncd != null; ncd = next) {
      next = ncd.getNext(ed);
      if (ed == null || ncd.removeEditor(ed)) ncd.removeGlobal();
    }
}



/***********************
private void removeAll(BurpEditorData ed)
{
   // remove any changes for the given editor

   BurpChangeData cd = (ed == null ? current_change : ed.getCurrentChange());
   if (cd == null) return;

   BurpChangeData next = null;
   for (BurpChangeData ncd = cd.getNext(ed); ncd != null; ncd = next) {
      next = ncd.getNext(ed);
      if (ed == null || ncd.removeEditor(ed)) ncd.removeGlobal();
    }
}
**************************/


private String getEditCommandName(BurpEditorData be,UndoableEdit ed)
{						
   String rslt = "edit_" + ed.getPresentationName();
   if (be != null) {
      String id = be.getBubbleId();
      if (id != null) rslt += "_" + id;
    }

   if (ed instanceof DocumentEvent) {
      DocumentEvent de = (DocumentEvent) ed;
      rslt += "_" + de.getLength() + "_" + de.getOffset();
    }


   return rslt;
}



/********************************************************************************/
/*										*/
/*	Undo/redo action for text editors					*/
/*										*/
/********************************************************************************/

private static class UndoRedoAction extends AbstractAction {

   private int history_direction;

   private static final long serialVersionUID = 1;

   UndoRedoAction(int dir) {
      super(dir > 0 ? "Redo" : "Undo");
      history_direction = dir;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BurpHistory hist = BurpHistory.getHistory();
      if (e.getSource() instanceof JTextComponent) {
	 JTextComponent bed = (JTextComponent) e.getSource();
	 if (history_direction < 0) hist.undo(bed);
	 else hist.redo(bed);
       }
    }

}	// end of inner class UndoRedoAction



/********************************************************************************/
/*										*/
/*	Change handler for detecting saves					*/
/*										*/
/********************************************************************************/

private void noteSave(File file)
{
   synchronized (editor_map) {
      for (Map.Entry<JTextComponent,BurpEditorData> ent : editor_map.entrySet()) {
	 JTextComponent tc = ent.getKey();
	 BudaBubble bb = BudaRoot.findBudaBubble(tc);
	 if (bb != null) {
	    File bf = bb.getContentFile();
	    if (bf != null && bf.equals(file)) ent.getValue().noteSave();
	  }
       }
    }
}



private class ChangeHandler implements BumpConstants.BumpChangeHandler {

   @Override public void handleFileAdded(String proj,String file)		{ }
   @Override public void handleFileRemoved(String proj,String file)		{ }
   @Override public void handleFileStarted(String proj,String file)             { }

   @Override public void handleFileChanged(String proj,String file) {
      noteSave(new File(file));
    }

}	// end of inner class ChangeHandler



}	// end of class BurpHistory



/* end of BurpHistory.java */

