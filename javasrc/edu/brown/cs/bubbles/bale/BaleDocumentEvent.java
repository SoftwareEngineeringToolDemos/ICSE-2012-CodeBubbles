/********************************************************************************/
/*										*/
/*		BaleDocumentEvent.java						*/
/*										*/
/*	Bubble Annotated Language Editor document event class			*/
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

import edu.brown.cs.bubbles.burp.BurpConstants;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.undo.UndoableEdit;




class BaleDocumentEvent implements DocumentEvent, UndoableEdit, BaleConstants,
	BurpConstants.BurpSharedEdit {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument base_document;
private int doc_offset;
private int edit_length;
private EventType event_type;
private UndoableEdit the_edit;
private BaleElementEvent element_event;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleDocumentEvent(BaleDocument d,int off,int len,EventType et,UndoableEdit ed,BaleElementEvent ee)
{
   base_document = d;
   doc_offset = off;
   edit_length = len;
   event_type = et;
   the_edit = ed;
   element_event = ee;
}



BaleDocumentEvent(DocumentEvent e,BaleElementEvent ee)
{
   base_document = (BaleDocument) e.getDocument();
   doc_offset = e.getOffset();
   edit_length = e.getLength();
   event_type = e.getType();

   if (e instanceof BaleDocumentEvent) {
      BaleDocumentEvent bde = (BaleDocumentEvent) e;
      the_edit = bde.the_edit;
    }
   else the_edit = null;

   element_event = ee;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Document getDocument() 			{ return base_document; }
@Override public int getLength()				{ return edit_length; }
@Override public int getOffset()				{ return doc_offset; }
@Override public DocumentEvent.EventType getType()		{ return event_type; }

UndoableEdit getEdit()
{
   if (the_edit == null) return null;

   return the_edit;
}


@Override public DocumentEvent.ElementChange getChange(Element e)
{
   if (element_event == null) return null;

   if (element_event.getElement() == e) return element_event;

   return null;
}



@Override public UndoableEdit getBaseEdit()
{
   if (the_edit == null) return null;

   if (the_edit instanceof BaleDocumentEvent) {
      BaleDocumentEvent bde = (BaleDocumentEvent) the_edit;
      return bde.getBaseEdit();
    }

   return the_edit;
}



/********************************************************************************/
/*										*/
/*	Undoable Edit methods							*/
/*										*/
/********************************************************************************/

@Override public boolean addEdit(UndoableEdit ed)
{
   if (the_edit == null) {
      the_edit = ed;
      return true;
    }
   else return the_edit.addEdit(ed);
}


@Override public boolean canRedo()
{
   if (the_edit == null) return false;
   return the_edit.canRedo();
}



@Override public boolean canUndo()
{
   if (the_edit == null) return false;
   return the_edit.canUndo();
}



@Override public void die()
{
   if (the_edit != null) {
      the_edit.die();
      the_edit = null;
    }
}


@Override public String getPresentationName()
{
   if (the_edit == null) return null;
   return the_edit.getPresentationName();
}



@Override public String getRedoPresentationName()
{
   if (the_edit == null) return null;
   return the_edit.getRedoPresentationName();
}



@Override public String getUndoPresentationName()
{
   if (the_edit == null) return null;
   return the_edit.getUndoPresentationName();
}



@Override public boolean isSignificant()
{
   if (the_edit == null) return false;
   return the_edit.isSignificant();
}



@Override public void redo()
{
   if (the_edit != null) {
      base_document.baleWriteLock();
      try {
	 the_edit.redo();
       }
      finally { base_document.baleWriteUnlock(); }
    }
}


@Override public boolean replaceEdit(UndoableEdit ed)
{
   if (the_edit == null) return false;
   return the_edit.replaceEdit(ed);
}


@Override public void undo()
{
   if (the_edit != null) {
      base_document.baleWriteLock();
      try {
	 the_edit.undo();
       }
      finally { base_document.baleWriteUnlock(); }
    }
}


}	// end of class BaleDocumentEvent




/* end of BaleDocumentEvent.java */
