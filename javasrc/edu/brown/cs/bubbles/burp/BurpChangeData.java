/********************************************************************************/
/*										*/
/*		BurpChangeData.java						*/
/*										*/
/*	Bubble Undo/Redo Processor information for a particular edit		*/
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

import javax.swing.undo.UndoableEdit;
import javax.swing.text.Document;

import java.util.*;



class BurpChangeData implements BurpConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BurpHistory for_history;
private UndoableEdit base_edit;
private BurpChangeData next_global;
private BurpChangeData prior_global;
private Map<BurpEditorData,BurpChangeData> next_editor;
private Map<BurpEditorData,BurpChangeData> prior_editor;
private List<BurpChangeData> depend_upons;
private boolean is_significant;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BurpChangeData(BurpHistory bh,UndoableEdit ue,BurpChangeData prior)
{
   for_history = bh;
   base_edit = ue;
   if (prior == null) next_global = null;
   else next_global = prior.next_global;
   prior_global = prior;
   if (prior_global != null) prior_global.next_global = this;
   next_editor = new HashMap<BurpEditorData,BurpChangeData>(2);
   prior_editor = new HashMap<BurpEditorData,BurpChangeData>(2);
   depend_upons = null;
   is_significant = ue.isSignificant();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BurpChangeData getPriorChange()
{
   return prior_global;
}



Document getBaseEditDocument()
{
   if (base_edit instanceof BurpSharedEdit) {
      BurpSharedEdit bde = (BurpSharedEdit) base_edit;
      return bde.getBaseEditDocument();
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Methods for getting next edit and associated editors			*/
/*										*/
/********************************************************************************/

BurpChangeData getNext(BurpEditorData ed)
{
   if (ed == null) return next_global;
   if (next_editor == null) return null;
   return next_editor.get(ed);
}


void addEditor(BurpEditorData ed)
{
   BurpChangeData cd = ed.getCurrentChange();
   if (cd != null && cd.next_editor != null) cd.next_editor.put(ed,this);
   next_editor.put(ed,null);
   prior_editor.put(ed,cd);
}



boolean removeEditor(BurpEditorData ed)
{
   if (next_editor == null) return false;
   if (!next_editor.containsKey(ed)) return false;
   next_editor.remove(ed);
   prior_editor.remove(ed);
   if (next_editor.size() == 0) return true;	     // should be removed globally
   return false;
}



void removeGlobal()
{
   if (prior_global != null) prior_global.next_global = next_global;
   if (next_global != null) next_global.prior_global = prior_global;
   next_editor = null;
   prior_editor = null;
   base_edit = null;
}



/********************************************************************************/
/*										*/
/*	Methods for managing dependencies					*/
/*										*/
/********************************************************************************/

void addDependencies(List<BurpChangeData> dep0)
{
   depend_upons = null;
   if (dep0 != null && !dep0.isEmpty()) depend_upons = new ArrayList<BurpChangeData>(dep0);
   Set<BurpEditorData> eddeps = new HashSet<BurpEditorData>();

   if (next_editor != null) {
      for (BurpEditorData ed : next_editor.keySet()) {
	 if (ed.getCurrentChange() != this) eddeps.add(ed);
       }
    }

   for (BurpChangeData cd = next_global; 
         cd != null && cd != this && !eddeps.isEmpty(); 
         cd = cd.next_global) {
      Set<BurpEditorData> rem = new HashSet<BurpEditorData>();
      for (BurpEditorData ed : eddeps) {
	 if (cd.next_editor.containsKey(ed)) rem.add(ed);
       }
      boolean usefg = !rem.isEmpty();

      if (usefg) {
	 eddeps.removeAll(rem);
	 if (depend_upons == null) depend_upons = new ArrayList<BurpChangeData>();
	 if (!depend_upons.contains(cd)) depend_upons.add(cd);
	 cd.addDependencies(null);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Significant event determination 					*/
/*										*/
/********************************************************************************/

void setSignificant(boolean fg) 	     { is_significant = fg; }

boolean isSignificant()
{
   if (is_significant) return true;
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (cd.isSignificant()) return true;
       }
    }
   return false;
}



/********************************************************************************/
/*										*/
/*	Methods to handle undo/redo						*/
/*										*/
/********************************************************************************/

boolean canUndo()
{
   if (base_edit == null) return false;
   if (!base_edit.canUndo()) return false;
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (!cd.canUndo()) return false;
       }
    }
   return true;
}



boolean canRedo()
{
   if (!base_edit.canRedo()) return false;
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (!cd.canRedo()) return false;
       }
    }
   return true;
}



void undo()
{
   if (depend_upons != null) {
      for (BurpChangeData cd : depend_upons) {
	 if (cd.canUndo()) cd.undo();
       }
    }
   
   if (base_edit.canUndo()) base_edit.undo();
   
   for_history.resetCurrentChange(this,prior_global,false);
   for (Map.Entry<BurpEditorData,BurpChangeData> ent : prior_editor.entrySet()) {
      BurpEditorData ed = ent.getKey();
      BurpChangeData cd = ent.getValue();
      ed.setCurrentChange(cd);
    }
}



void redo()
{
   base_edit.redo();
   
   for_history.resetCurrentChange(prior_global,this,true);
   for (Map.Entry<BurpEditorData,BurpChangeData> ent : next_editor.entrySet()) {
      BurpEditorData ed = ent.getKey();
      ed.setCurrentChange(this);
    }
   
   if (depend_upons != null) {
      for (int i = depend_upons.size()-1; i >= 0; --i) {
	 BurpChangeData cd = depend_upons.get(i);
	 if (cd.canRedo()) cd.redo();
       }
    }
}



}	// end of class BurpChangeData




/* end of BurpChangeData.java */
