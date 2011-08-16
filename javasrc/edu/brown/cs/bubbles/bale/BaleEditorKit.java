/********************************************************************************/
/*										*/
/*		BaleEditorKit.java						*/
/*										*/
/*	Bubble Annotated Language Editor editor kit				*/
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


package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.burp.BurpHistory;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;



class BaleEditorKit extends DefaultEditorKit implements BaleConstants, ViewFactory,
		BudaConstants, BuenoConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final static long serialVersionUID = 1;

private Action []	bale_actions;

private static final Action undo_action = BurpHistory.getUndoAction();
private static final Action redo_action = BurpHistory.getRedoAction();
private static final Action tab_action = new TabAction();
private static final Action newline_action = new NewlineAction();
private static final Action default_key_action = new DefaultKeyAction();
private static final Action backspace_action = new BackspaceAction();
private static final Action toggle_insert_action = new ToggleInsertAction();
private static final Action find_action = new FindAction();
private static final Action find_next_action = new FindNextAction(1);
private static final Action find_prev_action = new FindNextAction(-1);
private static final Action delete_line_action = new DeleteLineAction();
private static final Action delete_to_eol_action = new DeleteToEolAction();
private static final Action insert_line_above_action = new InsertLineAboveAction();
private static final Action insert_line_below_action = new InsertLineBelowAction();
private static final Action indent_lines_action = new IndentLinesAction();
private static final Action join_lines_action = new JoinLinesAction();
private static final Action comment_lines_action = new CommentLinesAction();
private static final Action smart_paste_action = new SmartPasteAction();
private static final Action move_lines_down_action = new MoveLinesAction(1);
private static final Action move_lines_up_action = new MoveLinesAction(-1);
private static final Action start_line_action = new StartLineAction(false);
private static final Action start_line_select_action = new StartLineAction(true);
private static final Action begin_line_action = new BeginLineAction(false);
private static final Action begin_line_select_action = new BeginLineAction(true);
private static final Action end_line_action = new EndLineAction(false);
private static final Action end_line_select_action = new EndLineAction(true);
private static final Action select_line_action = new SelectLineAction();
private static final Action select_word_action = new SelectWordAction();
private static final Action select_paragraph_action = new SelectParagraphAction();
private static final Action save_action = new SaveAction();
private static final Action save_all_action = new SaveAllAction();
private static final Action revert_action = new RevertAction();
private static final Action explicit_elision_action = new ExplicitElisionAction();
private static final Action redo_elision_action = new RedoElisionAction();
private static final Action remove_elision_action = new RemoveElisionAction();
private static final Action autocomplete_action = new AutoCompleteAction();
private static final Action rename_action = new RenameAction();
private static final Action extract_method_action = new ExtractMethodAction();
private static final Action format_action = new FormatAction();
private static final Action expand_action = new ExpandAction();

private static final Action goto_definition_action = new GotoDefinitionAction();
private static final Action goto_implementation_action = new GotoImplementationAction();
private static final Action goto_reference_action = new GotoReferenceAction();
private static final Action goto_doc_action = new GotoDocAction();
private static final Action goto_search_action = new GotoSearchAction();
private static final Action goto_type_action = new GotoTypeAction();
private static final Action class_search_action = new ClassSearchAction();
private static final Action bud_action;
private static final Action fragment_action;
private static final Action open_eclipse_editor_action = new OpenEclipseEditorAction();
private static final Action quick_fix_action = new QuickFixAction();
private static final Action marquis_comment_action = new CommentAction("MarquisComment",BuenoType.NEW_MARQUIS_COMMENT);
private static final Action block_comment_action = new CommentAction("BlockComment",BuenoType.NEW_BLOCK_COMMENT);
private static final Action javadoc_comment_action = new CommentAction("JavadocComment",BuenoType.NEW_JAVADOC_COMMENT);




static {
   BaleBudder bb = new BaleBudder();
   bud_action = bb.getBuddingAction();
   fragment_action = bb.getFragmentAction();
}


private static final Action [] local_actions = {
   undo_action,
   redo_action,
   tab_action,
   newline_action,
   default_key_action,
   backspace_action,
   toggle_insert_action,
   find_action,
   find_next_action,
   find_prev_action,
   delete_line_action,
   delete_to_eol_action,
   insert_line_above_action,
   insert_line_below_action,
   indent_lines_action,
   join_lines_action,
   comment_lines_action,
   start_line_action,
   start_line_select_action,
   begin_line_action,
   begin_line_select_action,
   end_line_action,
   end_line_select_action,
   select_line_action,
   select_word_action,
   select_paragraph_action,
   save_action,
   save_all_action,
   revert_action,
   explicit_elision_action,
   redo_elision_action,
   remove_elision_action,
   smart_paste_action,
   move_lines_up_action,
   move_lines_down_action,
   autocomplete_action,
   rename_action,
   extract_method_action,
   format_action,
   expand_action,

   goto_definition_action,
   goto_implementation_action,
   goto_reference_action,
   goto_doc_action,
   goto_search_action,
   goto_type_action,
   class_search_action,
   bud_action,
   fragment_action,
   open_eclipse_editor_action,

   quick_fix_action,
   marquis_comment_action,
   block_comment_action,
   javadoc_comment_action
};


private static final String	MENU_KEY = "menu";
private static final String	menu_keyname;

static {
   int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
   if (mask == Event.META_MASK) menu_keyname = "meta";
   else if (mask == Event.CTRL_MASK) menu_keyname = "ctrl";
   else menu_keyname = "ctrl";
}




private static final KeyItem [] key_defs = new KeyItem[] {
   new KeyItem("menu Z",undo_action),
      new KeyItem("menu Y",redo_action),
      new KeyItem("BACK_SPACE",backspace_action),
      new KeyItem("TAB",tab_action),
      new KeyItem("shift TAB",tab_action),      // should be separate action
      new KeyItem("ENTER",newline_action),
      new KeyItem("INSERT",toggle_insert_action),
      new KeyItem("F14",toggle_insert_action),          // for the mac keyboard
      new KeyItem("menu F",find_action),
      new KeyItem("menu K",find_next_action),
      new KeyItem("menu shift K",find_prev_action),
      new KeyItem("menu D",delete_line_action),
      new KeyItem("menu shift DELETE",delete_to_eol_action),
      new KeyItem("menu shift ENTER",insert_line_above_action),
      new KeyItem("shift ENTER",insert_line_below_action),
      new KeyItem("menu I",indent_lines_action),
      new KeyItem("menu alt J",join_lines_action),
      new KeyItem("menu SLASH",comment_lines_action),
      new KeyItem("HOME",start_line_action),
      new KeyItem("shift HOME",start_line_select_action),
      new KeyItem("END",end_line_action),
      new KeyItem("shift END",end_line_select_action),
      new KeyItem("menu S",save_all_action),
      new KeyItem("menu shift S",save_action),
      new KeyItem("alt shift menu S",revert_action),            // temporary for now
      new KeyItem("menu E",explicit_elision_action),
      new KeyItem("menu shift E",redo_elision_action),
      new KeyItem("menu alt E",remove_elision_action),
      new KeyItem("menu V",smart_paste_action),
      new KeyItem("menu shift V",pasteAction),
      new KeyItem("alt DOWN",move_lines_down_action),
      new KeyItem("alt UP",move_lines_up_action),
      new KeyItem("alt shift R",rename_action),
      new KeyItem("alt shift M",extract_method_action),
      new KeyItem("alt W",select_word_action),
      new KeyItem("alt shift W",select_line_action),
      new KeyItem("menu B",select_paragraph_action),
      new KeyItem("menu shift F",format_action),
      new KeyItem("alt B",expand_action),

      new KeyItem("menu SPACE",autocomplete_action),

      new KeyItem("F3",goto_implementation_action),
      new KeyItem("menu shift F3",goto_definition_action),
      new KeyItem("shift F3",goto_doc_action),
      new KeyItem("F4",goto_reference_action),
      new KeyItem("menu shift C",goto_type_action),
      new KeyItem("menu shift G",goto_reference_action),
      new KeyItem("shift F4",goto_search_action),
      new KeyItem("menu shift T",class_search_action),
      new KeyItem("menu F4",goto_search_action),
      new KeyItem("alt X",bud_action),
      new KeyItem("alt C",fragment_action),

      new KeyItem("shift alt C",block_comment_action),
      new KeyItem("shift alt Q",marquis_comment_action),
      new KeyItem("shift alt J",javadoc_comment_action),

      new KeyItem("menu 1",quick_fix_action)
};



private static Keymap	bale_keymap;

static {
   Keymap dflt = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
   bale_keymap = JTextComponent.addKeymap("BALE",dflt);
   for (KeyItem ka : key_defs) {
      ka.addToKeyMap(bale_keymap);
    }
   bale_keymap.setDefaultAction(default_key_action);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleEditorKit()
{
   Action [] supact = super.getActions();
   bale_actions = new Action[supact.length + local_actions.length];
   int ct = 0;
   for (int i = 0; i < supact.length; ++i) bale_actions[ct++] = supact[i];
   for (int j = 0; j < local_actions.length; ++j) bale_actions[ct++] = local_actions[j];
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Action [] getActions() 	{ return bale_actions; }




/********************************************************************************/
/*										*/
/*	Keymap actions								*/
/*										*/
/********************************************************************************/

void setupKeyMap(JTextComponent tc)
{
   tc.setKeymap(bale_keymap);
}



static Action findAction(String name)
{
   if (local_actions != null) {
      for (int i = 0; i < local_actions.length; ++i) {
	 String nm = (String) local_actions[i].getValue(Action.NAME);
	 if (nm != null && nm.equals(name)) return local_actions[i];
       }
    }

   DefaultEditorKit ek = new DefaultEditorKit();
   Action [] supact = ek.getActions();
   for (int i = 0; i < supact.length; ++i) {
      String nm = (String) supact[i].getValue(Action.NAME);
      if (nm != null && nm.equals(name)) return supact[i];
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	View Factory methods							*/
/*										*/
/********************************************************************************/

@Override public ViewFactory getViewFactory()	{ return this; }



@Override public View create(Element elem)
{
   if (elem == null) return null;
   if (!(elem instanceof BaleElement)) return new ParagraphView(elem);

   // BoardLog.logD("BALE","Create view for " + elem.getName());

   BaleElement be = (BaleElement) elem;
   View rslt = null;
   switch (be.getViewType()) {
      case NONE :
	 break;
      case TEXT :
	 rslt = new BaleViewText(be);
	 break;
      case BLOCK :
	 rslt = new BaleViewBlock(be);
	 break;
      case CODE :
	 // note: not currently used
	 rslt = new BaleViewCode(be);
	 break;
      case LINE :
	 rslt = new BaleViewLine(be);
	 break;
      case ORPHAN :
	 rslt = new BaleViewOrphan(be);
	 break;
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Subclass for Fragment Editors						*/
/*										*/
/********************************************************************************/

static class FragmentKit extends BaleEditorKit {

   private BaleDocumentIde base_document;
   private BaleFragmentType fragment_type;
   private List<BaleRegion> fragment_regions;

   private final static long serialVersionUID = 1;

   FragmentKit(BaleDocumentIde base,BaleFragmentType typ,List<BaleRegion> rgns) {
      base_document = base;
      fragment_type = typ;
      fragment_regions = rgns;
    }

   @Override public Document createDefaultDocument() {
      return new BaleDocumentFragment(base_document,fragment_type,fragment_regions);
    }

}	// end of inner class FragmentKit




/********************************************************************************/
/*										*/
/*	Utilities for action management 					*/
/*										*/
/********************************************************************************/

private static BaleEditorPane getBaleEditor(ActionEvent e)
{
   if (e == null) return null;

   return (BaleEditorPane) e.getSource();
}



private static boolean checkEditor(BaleEditorPane e)
{
   if (e == null) return false;
   if (!e.isEditable()) return false;
   if (!e.isEnabled()) return false;

   BaleDocument bd = e.getBaleDocument();
   if (bd.isOrphan()) {
      bd.baleWriteLock();
      try {
	 e.setEditable(false);
       }
      finally { bd.baleWriteUnlock(); }
      return false;
    }

   return true;
}



private static boolean checkReadEditor(BaleEditorPane e)
{
   if (e == null) return false;
   if (!e.isEnabled()) return false;
   BaleDocument bd = e.getBaleDocument();
   if (bd.isOrphan()) {
      bd.baleWriteLock();
      try {
	 if (e.isEditable()) e.setEditable(false);
       }
      finally { bd.baleWriteUnlock(); }
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Text character actions :: default keys					*/
/*										*/
/********************************************************************************/

private static class DefaultKeyAction extends TextAction {

   private static final long serialVersionUID = 1;

   DefaultKeyAction() {
      super("DefaultKeyAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      try {
	 Dimension d0 = target.getPreferredSize();

	 String content = e.getActionCommand();
	 int mod = e.getModifiers();
	 if ((content != null) && (content.length() > 0) &&
		((mod & ActionEvent.ALT_MASK) == (mod & ActionEvent.CTRL_MASK))) {
	    char c = content.charAt(0);

	    if ((c >= 0x20) && (c != 0x7F)) {
	       int sel = target.getSelectionStart();
	       if (target.getOverwriteMode()) {
		  if (sel == target.getSelectionEnd()) {
		     String prev = null;
		     try {
			prev = target.getText(sel,1);
		      }
		     catch (BadLocationException ex) { }
		     if (prev == null || prev.equals("\n"));
		     else if (prev.equals("\t")) {
			int cpos = bd.getColumnPosition(sel);
			int npos = bd.getNextTabPosition(cpos);
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < npos-cpos; ++i) buf.append(" ");
			target.setSelectionEnd(sel+1);
			target.replaceSelection(buf.toString());
			target.setSelectionStart(sel);
			target.setSelectionEnd(sel+1);
		      }
		     else target.setSelectionEnd(sel+1);
		   }
		}

	       target.replaceSelection(content);
	       if (content != null && shouldAutoIndent(target,content,sel)) {
		  // TODO: check that this is the only thing on the line
		  indent_lines_action.actionPerformed(e);
		}
	       BaleCompletionContext ctx = target.getCompletionContext();
	       if (ctx == null && isCompletionTrigger(c) && !target.getOverwriteMode()) {
		  new BaleCompletionContext(target,sel,c);
		}

	       Dimension d1 = target.getPreferredSize();
	       if (d1.height > d0.height) {
		  target.increaseSize(1);
		}
	     }
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class DefaultKeyAction




private static boolean shouldAutoIndent(BaleEditorPane target, String content, int pos)
{
   // TODO: Really want to look at the line with content and check for only {, }, case, ...
   // when content is the last character of that

   if (content.equals("{")) return isFirstCharacter(target,pos);
   if (content.equals("}")) return isFirstCharacter(target,pos);
   if (content.equals("e")) return matchesText(target,pos,"case");
   if (content.equals("t")) return matchesText(target,pos,"default");

   return false;
}



private static boolean isFirstCharacter(BaleEditorPane target,int pos)
{
   if (pos == 0) return true;

   int delta = Math.min(80,pos);
   String txt;
   try {
      txt = target.getText(pos-delta,delta);
    }
   catch (BadLocationException e) { return false; }

   for (int i = txt.length()-1; i >= 0; --i) {
      char ch = txt.charAt(i);
      if (ch == '\n') return true;
      if (!Character.isWhitespace(ch)) return false;
    }
   return true;
}



private static boolean matchesText(BaleEditorPane target,int pos,String match)
{
   if (pos < match.length()) return false;

   int delta = Math.min(80,pos);
   String txt;
   try {
      txt = target.getText(pos-delta,delta+1);
    }
   catch (BadLocationException e) { return false; }

   for (int i = txt.length()-1; i >= 0; --i) {
      char ch = txt.charAt(i);
      if (ch == '\n') {
	 String line = txt.substring(i+1).trim();
	 return line.equals(match);
       }
    }

   return false;
}



private static boolean isCompletionTrigger(char c)
{
   return c == '(' || c == '.';
}



/********************************************************************************/
/*										*/
/*	Text character actions :: other actions 				*/
/*										*/
/********************************************************************************/

private static class BackspaceAction extends TextAction {

   private Action delete_prev_action;
   private Action backward_action;

   private static final long serialVersionUID = 1;

   BackspaceAction() {
      super("BackspaceAction");
      delete_prev_action = findAction(deletePrevCharAction);
      backward_action = findAction(backwardAction);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target == null) ;
      else if (target.getOverwriteMode()) backward_action.actionPerformed(e);
      else delete_prev_action.actionPerformed(e);
    }

}	// end of inner class BackspaceAction



private static class ToggleInsertAction extends TextAction {

   private static final long serialVersionUID = 1;

   ToggleInsertAction() {
      super("ToggleInsertAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target != null) {
	 target.setOverwriteMode(!target.getOverwriteMode());
       }
    }

}	// end of inner class ToggleInsertAction




private static class TabAction extends TextAction {

   private static final long serialVersionUID = 1;

   TabAction() {
      super("TabAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane tgt = getBaleEditor(e);
      if (!checkEditor(tgt)) return;

      BaleDocument bd = tgt.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = tgt.getSelectionStart();
	 int eoff = tgt.getSelectionEnd();
	 if (soff != eoff) {
	    int slno = bd.findLineNumber(soff);
	    int elno = bd.findLineNumber(eoff);
	    if (slno != elno) {
	       if (elno < slno) {
		  int x = elno;
		  elno = slno;
		  slno = x;
		}
	       for (int i = slno; i <= elno; ++i) {
		  bd.fixLineIndent(i);
		}
	       return;
	     }
	  }

	 int cpos = bd.getColumnPosition(soff);
	 int pos = bd.getNextTabPosition(cpos);
	 if (tgt.getOverwriteMode()) {
	    int mod = e.getModifiers();
	    if (eoff != soff || ((mod & ActionEvent.SHIFT_MASK) != 0)) {
	       tgt.setSelectionEnd(pos);
	     }
	    else tgt.setSelectionStart(pos);
	  }
	 else {
	    StringBuffer buf = new StringBuffer();
	    for (int i = cpos; i < pos; ++i) buf.append(" ");
	    tgt.replaceSelection(buf.toString());
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class TabAction




private static class NewlineAction extends TextAction {

   private static final long serialVersionUID = 1;

   NewlineAction() {
      super("NewlineAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;

      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      try {
	 String text = "\n";
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 BaleElement elt = bd.getCharacterElement(eoff);
	 if (elt != null && elt.isComment()) {
	    switch (elt.getEndTokenState()) {
	       case IN_COMMENT :
	       case IN_FORMAL_COMMENT :
		  text += " * ";
		  break;
	     }
	  }

	 boolean grow = true;
	 boolean rep = true;
	 boolean ind = true;

	 if (soff != eoff) {
	    int slno = bd.findLineNumber(soff);
	    int elno = bd.findLineNumber(eoff);
	    if (elno != slno) grow = false;
	  }
	 else {
	    if (target.getOverwriteMode()) {
	       rep = false;
	       grow = false;
	       int nlno = bd.findLineNumber(soff) + 1;
	       int pos = bd.getFirstNonspace(nlno);
	       if (pos >= 0) ind = false;
	       else pos = -pos;
	       if (pos < bd.getEndPosition().getOffset()) target.setCaretPosition(pos);
	       soff = pos-1;
	     }
	  }

	 if (rep) target.replaceSelection(text);
	 if (ind) {
	    int lno = bd.findLineNumber(soff+1);
	    bd.fixLineIndent(lno);
	  }
	 if (grow) {
	    target.increaseSize(1);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class NewlineAction




/********************************************************************************/
/*										*/
/*	Text editing actions							*/
/*										*/
/********************************************************************************/

private static class DeleteLineAction extends TextAction {

   private static final long serialVersionUID = 1;

   DeleteLineAction() {
      super("DeleteLineAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;

      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int slno = bd.findLineNumber(soff);
	 int sdel = bd.findLineOffset(slno);
	 int eoff = be.getSelectionEnd();
	 int elno = (eoff == soff ? slno : bd.findLineNumber(eoff));
	 int edel = bd.findLineOffset(elno+1);

	 if (edel != sdel) {
	    be.setSelectionStart(sdel);
	    be.setSelectionEnd(edel);
	    be.cut();
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class DeleteLineAction





private static class DeleteToEolAction extends TextAction {

   private static final long serialVersionUID = 1;

   DeleteToEolAction() {
      super("DeleteToEolAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int sdel = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();
	 int elno = bd.findLineNumber(eoff);
	 int edel = bd.findLineOffset(elno+1) - 1;

	 try {
	    if (edel != sdel) bd.remove(sdel,edel-sdel);
	  }
	 catch (BadLocationException ex) { }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class DeleteToEolAction





private static class InsertLineAboveAction extends TextAction {

   private static final long serialVersionUID = 1;

   InsertLineAboveAction() {
      super("InsertLineAboveAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int slno = bd.findLineNumber(soff);
	 int sins = bd.findLineOffset(slno);
	 try {
	    bd.insertString(sins,"\n",null);
	  }
	 catch (BadLocationException ex) { }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class InsertLineAboveAction





private static class InsertLineBelowAction extends TextAction {

   private static final long serialVersionUID = 1;

   InsertLineBelowAction() {
      super("InsertLineBelowAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionEnd();
	 int slno = bd.findLineNumber(soff);
	 int sins = bd.findLineOffset(slno+1);
	 try {
	    bd.insertString(sins,"\n",null);
	  }
	 catch (BadLocationException ex) { }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class InsertLineBelowAction





private static class IndentLinesAction extends TextAction {

   private static final long serialVersionUID = 1;


   IndentLinesAction() {
      super("IndentLinesAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();

	 int slno = bd.findLineNumber(soff);
	 int elno = slno;
	 if (eoff != soff) elno = bd.findLineNumber(eoff);
	 if (elno < slno) {
	    int x = elno;
	    elno = slno;
	    slno = x;
	  }

	 for (int i = slno; i <= elno; ++i) {
	    bd.fixLineIndent(i);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class IndentLinesAction




private static class JoinLinesAction extends TextAction {

   private static final long serialVersionUID = 1;

   JoinLinesAction() {
      super("JoinLinesAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();

	 int slno = bd.findLineNumber(soff);
	 int elno = slno;
	 if (eoff != soff) elno = bd.findLineNumber(eoff);
	 if (elno < slno) {
	    int x = elno;
	    elno = slno;
	    slno = x;
	  }
	 if (elno == slno) elno = slno+1;

	 for (int i = slno; i < elno; ++i) {
	    joinLineWithNext(be,bd,i);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

   private void joinLineWithNext(BaleEditorPane be,BaleDocument bd,int lno) {
      int soff = bd.findLineOffset(lno+1);
      int bdel = soff-1;		// delete the newline
      int edel = bd.getFirstNonspace(lno+1);
      if (edel < 0) edel = soff;
      try {
	 bd.remove(bdel,edel-bdel);
	 bd.insertString(bdel," ",null);
       }
      catch (BadLocationException e) { }
    }

}	// end of inner class JoinLinesAction



private static class SmartPasteAction extends TextAction {

   private static final long serialVersionUID = 1;

   SmartPasteAction() {
      super("SmartPasteAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.baleWriteLock();
      try {
	 int p0 = target.getSelectionStart();
	 int p1 = target.getSelectionEnd();
	 Position epos = null;
	 try {
	    epos = bd.createPosition(p1+1);
	 }
	 catch (BadLocationException ex) { }

	 target.paste();

	 int slno = bd.findLineNumber(p0);
	 if (epos != null) p1 = epos.getOffset();
	 else p1 = p0;
	 int elno = bd.findLineNumber(p1);
	 for (int i = slno; i <= elno; ++i) {
	    bd.fixLineIndent(i);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class SmartPasteAction




private static class MoveLinesAction extends TextAction {

   private int move_direction;

   private static final long serialVersionUID = 1;

   MoveLinesAction(int dir) {
      super((dir > 0 ? "MoveLinesDownAction" : "MoveLinesUpAction"));
      move_direction = dir;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int spos = target.getSelectionStart();
	 int epos = target.getSelectionEnd();
	 int slno = bd.findLineNumber(spos);
	 spos = bd.findLineOffset(slno);
	 int elno = bd.findLineNumber(epos);
	 epos = bd.findLineOffset(elno+1);

	 try {
	    String txt = bd.getText(spos,epos-spos);
	    bd.remove(spos,epos-spos);
	    int nlno = slno + move_direction;
	    int ipos = bd.findLineOffset(nlno);
	    bd.insertString(ipos,txt,null);
	    target.setSelectionStart(ipos);
	    target.setSelectionEnd(ipos + txt.length());
	  }
	 catch (BadLocationException ex) {
	    return;
	  }
	 //TODO: Should we indent here?
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class MoveLinesAction




private static class CommentLinesAction extends TextAction {

   private static final long serialVersionUID = 1;


   CommentLinesAction() {
      super("CommentLinesAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;
      BaleDocument bd = be.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = be.getSelectionStart();
	 int eoff = be.getSelectionEnd();

	 int slno = bd.findLineNumber(soff);
	 int elno = slno;
	 if (eoff != soff) elno = bd.findLineNumber(eoff);
	 if (elno < slno) {
	    int x = elno;
	    elno = slno;
	    slno = x;
	  }

	 LinkedList<Integer> fixups = new LinkedList<Integer>();
	 for (int i = elno; i >= slno; --i) {
	    int loff = bd.findLineOffset(i);
	    BaleElement ce = bd.getCharacterElement(loff);
	    while (ce.isEmpty() && !ce.isComment() && !ce.isEndOfLine()) {
	       ce = ce.getNextCharacterElement();
	     }
	    int noff = ce.getStartOffset();

	    try {
	       if (ce != null && ce.getName().equals("LineComment")) {
		  bd.remove(noff,2);
		  fixups.addFirst(i);
		}
	       else {
		  bd.insertString(noff,"// ",null);
		}
	     }
	    catch (BadLocationException ex) {
	       return;
	     }
	  }
	 for (Integer iv : fixups) {
	    bd.fixLineIndent(iv);
	 }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class CommentLinesAction




/********************************************************************************/
/*										*/
/*	Search actions								*/
/*										*/
/********************************************************************************/

private static class FindAction extends TextAction {

   private static final long serialVersionUID = 1;

   FindAction() {
      super("FindAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleFindBar fb = target.getFindBar();
      target.toggleFindBar();
      fb.repaint();
    }

}	// end of inner class FindAction




private static class FindNextAction extends TextAction {

   private int find_direction;

   private static final long serialVersionUID = 1;

   FindNextAction(int dir) {
      super(dir > 0 ? "FindNextAction" : "FindPrevAction");
      find_direction = dir;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleFindBar fb = target.getFindBar();
      fb.find(find_direction,true);
    }

}	// end of inner class FindNextAction




/********************************************************************************/
/*										*/
/*	Cursor actions								*/
/*										*/
/********************************************************************************/

private static class StartLineAction extends TextAction {

   private boolean do_select;

   private static final long serialVersionUID = 1;

   StartLineAction(boolean select) {
      super(select ? "StartLineSelectAction" : "StartLineAction");
      do_select = select;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = Math.abs(bd.getFirstNonspace(lno));
      int boff = bd.findLineOffset(lno);
      if (off == soff && soff != boff) soff = boff;
      if (do_select) target.moveCaretPosition(soff);
      else target.setCaretPosition(soff);
    }

}	// end of inner class StartLineAction






private static class BeginLineAction extends TextAction {

   private boolean do_select;

   private static final long serialVersionUID = 1;

   BeginLineAction(boolean select) {
      super(select ? selectionBeginLineAction : beginLineAction);
      do_select = select;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = bd.findLineOffset(lno);
      if (do_select) target.moveCaretPosition(soff);
      else target.setCaretPosition(soff);
    }

}	// end of inner class BeginLineAction





private static class EndLineAction extends TextAction {

   private boolean do_select;

   private static final long serialVersionUID = 1;

   EndLineAction(boolean select) {
      super(select ? selectionEndLineAction : endLineAction);
      do_select = select;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = bd.findLineOffset(lno+1)-1;
      if (soff < 0) return;
      if (do_select) target.moveCaretPosition(soff);
      else target.setCaretPosition(soff);
    }

}	// end of inner class EndLineAction



/********************************************************************************/
/*										*/
/*	Selection actions							*/
/*										*/
/********************************************************************************/

private static class SelectLineAction extends TextAction {

   private static final long serialVersionUID = 1;

   SelectLineAction() {
      super(selectLineAction);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int off = target.getCaretPosition();
      int lno = bd.findLineNumber(off);
      int soff = bd.findLineOffset(lno);
      int eoff = bd.findLineOffset(lno+1)-1;
      if (soff >= 0) {
	 target.setCaretPosition(soff);
	 target.moveCaretPosition(eoff);
       }
    }

}	// end of inner class SelectLineAction



private static class SelectWordAction extends TextAction {

   private static final long serialVersionUID = 1;

   SelectWordAction() {
      super(selectWordAction);
    }

   public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      BaleElement be = bd.getCharacterElement(soff);
      if (be == null) return;
      int esoff = be.getStartOffset();
      if (esoff < 0) return;
      target.setCaretPosition(esoff);
      int eeoff = be.getEndOffset();
      if (eeoff < 0) return;
      target.moveCaretPosition(eeoff);
    }

}	// end of inner class SelectWordAction



private static class SelectParagraphAction extends TextAction {

   private static final long serialVersionUID = 1;

   SelectParagraphAction() {
      super(selectParagraphAction);
    }

   public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      BaleElement be = bd.getCharacterElement(soff);
      while (be != null && !be.isLeaf() && !be.isLineElement()) {
	 be = be.getBaleParent();
       }
      if (be == null) return;
      target.setCaretPosition(be.getStartOffset());
      target.moveCaretPosition(be.getEndOffset());
    }

}	// end of inner class SelectParagraphAction



/********************************************************************************/
/*										*/
/*	File actions								*/
/*										*/
/********************************************************************************/

private static class SaveAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveAction() {
      super("SaveAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.save();
      BoardMetrics.noteCommand("BALE","Save");
    }

}	// end of inner class SaveAction





private static class RevertAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RevertAction() {
      super("RevertAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      bd.revert();
      BoardMetrics.noteCommand("BALE","Revert");
    }

}	// end of inner class SaveAction




private static class SaveAllAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveAllAction() {
      super("SaveAllAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (target == null) return;
      BudaRoot br = BudaRoot.findBudaRoot(target);

      BowiFactory.startTask(BowiTaskType.SAVE);
      try {
	 // bc.saveAll();
	 br.handleSaveAllRequest();
	 try {
	    br.saveConfiguration(null);
	  }
	 catch (IOException ex) { }
       }
      finally { BowiFactory.stopTask(BowiTaskType.SAVE); }

      BoardMetrics.noteCommand("BALE","SaveAll");
    }

}	// end of inner class SaveAllAction




/********************************************************************************/
/*										*/
/*	Elision actions 							*/
/*										*/
/********************************************************************************/

private static class RedoElisionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RedoElisionAction() {
      super("RedoElisionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      BowiFactory.startTask(BowiTaskType.EXPAND_ELLISONS);
      try {
	 bd.baleWriteLock();
	 try {
	    bd.redoElision();
	    bd.handleElisionChange();
	    target.getPreferredSize();
	    bd.fixElisions();
	  }
	 finally { bd.baleWriteUnlock(); }
       }
      finally { BowiFactory.stopTask(BowiTaskType.EXPAND_ELLISONS); }
      BoardMetrics.noteCommand("BALE","RedoElision");
      BaleEditorBubble.noteElision(target);
    }

}	// end of inner class RedoElisionAction




private static class RemoveElisionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RemoveElisionAction() {
      super("RemoveElisionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask(BowiTaskType.EXPAND_ELLISONS);
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkEditor(target)) return;
	 BaleDocument bd = target.getBaleDocument();
	 bd.baleWriteLock();
	 try {
	    BaleElideMode em = bd.getElideMode();
	    bd.removeElision();
	    bd.handleElisionChange();
	    bd.setElideMode(em);
	  }
	 finally { bd.baleWriteUnlock(); }
	 BoardMetrics.noteCommand("BALE","RemoveElision");
	 BaleEditorBubble.noteElision(target);
       }
      finally { BowiFactory.stopTask(BowiTaskType.EXPAND_ELLISONS); }
    }

}	// end of inner class RemoveElisionAction




private static class ExplicitElisionAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ExplicitElisionAction() {
      super("ExplicitElisionAction");
   }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      BowiFactory.startTask(BowiTaskType.EXPAND_ELLISONS);
      try {
	 bd.baleWriteLock();
	 try {
	    int off = target.getCaretPosition();
	    BaleElement.Branch br = bd.getParagraphElement(off);
	    BaleElement ebr = null;
	    if (br.isElided()) {
	       br.setElided(false);
	       ebr = br;
	     }
	    else {
	       while (br != null && !br.canElide()) {
		  br = br.getBaleParent();
		}
	       if (br == null) return;
	       br.setElided(true);
	     }
	    bd.handleElisionChange();
	    if (ebr != null) target.increaseSizeForElidedElement(ebr);
	  }
	 finally { bd.baleWriteUnlock(); }
       }
      finally { BowiFactory.stopTask(BowiTaskType.EXPAND_ELLISONS); }
      BoardMetrics.noteCommand("BALE","ExplicitElision");
      BaleEditorBubble.noteElision(target);
    }

}	// end of inner class ExplicitElisionAction





/********************************************************************************/
/*										*/
/*	Completion actions							*/
/*										*/
/********************************************************************************/

private static class AutoCompleteAction extends TextAction  {

   private static final long serialVersionUID = 1;

   AutoCompleteAction() {
      super("AutoCompleteAction");
   }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleCompletionContext ctx = target.getCompletionContext();
      if (ctx == null) {
	 int sel = target.getSelectionStart();
	 new BaleCompletionContext(target,sel-1,'1');
	 BoardMetrics.noteCommand("BALE","AutoCompleteIt");
       }
      else {
	 ctx.handleSelected();
	 BoardMetrics.noteCommand("BALE","AutoComplete");
       }
   }
}



/********************************************************************************/
/*										*/
/*	Refactoring actions							*/
/*										*/
/********************************************************************************/

private static class RenameAction extends TextAction {

   private static final long serialVersionUID = 1;

   RenameAction() {
      super("RenameAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      BaleDocument bd = target.getBaleDocument();
      if (!checkEditor(target)) return;
      int soff = target.getSelectionStart();
      int eoff = target.getSelectionEnd();

      BaleElement be = bd.getCharacterElement(soff);
      if (be == null) return;
      if (!be.isIdentifier()) return;
      if (eoff != soff) {
	 BaleElement xbe = bd.getCharacterElement(eoff);
	 if (xbe != be) return;
       }

      BaleRenameContext ctx = target.getRenameContext();
      if (ctx == null) {
	 int sel = target.getSelectionStart();
	 new BaleRenameContext(target,sel);
       }

      BoardMetrics.noteCommand("BALE","Rename");
    }

}	// end of inner class RenameAction



private static class ExtractMethodAction extends TextAction implements BuenoConstants.BuenoBubbleCreator {

   private static final long serialVersionUID = 1;

   ExtractMethodAction() {
      super("ExtractMethodAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      BaleDocument bd = target.getBaleDocument();
      if (!checkEditor(target)) return;

      int spos = target.getSelectionStart();
      int epos = target.getSelectionEnd();
      int slno = bd.findLineNumber(spos);
      spos = bd.findLineOffset(slno);
      int elno = bd.findLineNumber(epos);
      epos = bd.findLineOffset(elno+1)-1;
      String cnts;
      Point p;
      try {
	 cnts = bd.getText(spos,epos-spos);
	 Rectangle r = target.modelToView(spos);
	 p = new Point(r.x,r.y);
       }
      catch (BadLocationException ex) {
	 BoardLog.logE("BALE","Problem getting extract text",ex);
	 return;
       }

      BudaBubble bbl = BudaRoot.findBudaBubble(target);

      String fnm = bd.getFragmentName();
      String aft = null;
      String cls = null;
      switch (bd.getFragmentType()) {
	 case FILE :
	 case NONE :
	    return;
	 case METHOD :
	    aft = fnm;
	    cls = fnm;
	    int idx1 = cls.indexOf("(");
	    if (idx1 >= 0) cls = cls.substring(0,idx1);
	    idx1 = cls.lastIndexOf(".");
	    if (idx1 >= 0) cls = cls.substring(0,idx1);
	    break;
	 case FIELDS :
	 case STATICS :
	 case HEADER :
	    cls = fnm;
	    int idx2 = cls.lastIndexOf(".");
	    cls = cls.substring(0,idx2);
	    break;
       }

      if (cls == null) return;

      BuenoProperties props = new BuenoProperties();
      props.put(BuenoConstants.BuenoKey.KEY_CONTENTS,cnts);
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(bd.getProjectName(),cls,aft,true);

      BuenoMethodDialog bmd = new BuenoMethodDialog(bbl,p,props,loc,this);
      bmd.setLabel("Enter Signature of Extracted Method");
      bmd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb == null) return;
      bba.addBubble(bb,null,p,PLACEMENT_MOVETO);
      // bba.add(bb,new BudaConstraint(p));
      bb.markBubbleAsNew();
   }

}	// end of inner class ExtractMethodAction




/********************************************************************************/
/*										*/
/*	Formatting Actions							*/
/*										*/
/********************************************************************************/

private static class FormatAction extends TextAction {

   private static final long serialVersionUID = 1;

   FormatAction() {
      super("FormatAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();

      bd.baleWriteLock();
      try {
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 if (soff == eoff) {
	    soff = 0;
	    eoff = bd.getLength();
	  }
	 if (soff == eoff) return;
	 int dsoff = bd.mapOffsetToEclipse(soff);
	 int deoff = bd.mapOffsetToEclipse(eoff);

	 BumpClient bc = BumpClient.getBump();
	 org.w3c.dom.Element edits = bc.format(bd.getProjectName(),bd.getFile(),dsoff,deoff);

	 if (edits != null) {
	    BaleApplyEdits bae = new BaleApplyEdits(bd);
	    bae.applyEdits(edits);
	  }
       }
      finally { bd.baleWriteUnlock(); }
    }

}	// end of inner class FormatAction




private static class ExpandAction extends TextAction {

   private static final long serialVersionUID = 1;

   ExpandAction() {
      super("ExpandAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      javax.swing.plaf.TextUI tui = target.getUI();
      View root = tui.getRootView(target);
      View v1 = root.getView(0);
      int vx = (int)(Math.ceil(v1.getMaximumSpan(View.X_AXIS)+0.5));
      Dimension d = target.getSize();
      if (vx <= d.width) return;
      BudaBubble bb = BudaRoot.findBudaBubble(target);
      Dimension d1 = bb.getSize();
      d1.width += vx - d.width + 10;
      bb.setSize(d1);
    }

}	// end of inner class ExpandAction




/********************************************************************************/
/*										*/
/*	Goto Actions								*/
/*										*/
/********************************************************************************/

private static class GotoDefinitionAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoDefinitionAction() {
      super("GotoDefinitionAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask(BowiTaskType.GO_TO_DEFINITION);
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;

	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();

	 BumpClient bc = BumpClient.getBump();
	 Collection<BumpLocation> locs;
	 BalePosition sp;
	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findDefinition(bd.getProjectName(),
					bd.getFile(),
					bd.mapOffsetToEclipse(soff),
					bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    if (e.getActionCommand() == null) {
	       e = new ActionEvent(target,e.getID(),"GotoDefinitionAction",e.getWhen(),e.getModifiers());
	     }
	    goto_doc_action.actionPerformed(e);
	    return;
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,true,locs,BudaLinkStyle.STYLE_SOLID);
       }
      finally { BowiFactory.stopTask(BowiTaskType.GO_TO_DEFINITION); }
      BoardMetrics.noteCommand("BALE","GoToDefinition");
    }

}	// end of inner class GotoDefinitionAction




private static class GotoImplementationAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoImplementationAction() {
      super("GotoImplementationAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask(BowiTaskType.GO_TO_IMPLEMENTATION);
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;
	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();

	 BumpClient bc = BumpClient.getBump();
	 Collection<BumpLocation> locs;
	 BalePosition sp;
	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findImplementations(bd.getProjectName(),
					     bd.getFile(),
					     bd.mapOffsetToEclipse(soff),
					     bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    if (e.getActionCommand() == null) {
	       e = new ActionEvent(target,e.getID(),"GotoImplementationAction",e.getWhen(),e.getModifiers());
	     }
	    goto_doc_action.actionPerformed(e);
	    return;
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,true,locs,BudaLinkStyle.STYLE_SOLID);
       }
      finally { BowiFactory.stopTask(BowiTaskType.GO_TO_IMPLEMENTATION); }
      BoardMetrics.noteCommand("BALE","GoToImplementation");
    }

}	// end of inner class GotoImplementationAction




private static class GotoReferenceAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoReferenceAction() {
      super("GotoReferenceAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask(BowiTaskType.FIND_ALL_REFERENCES);
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;
	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();
	 BaleElement be = bd.getCharacterElement(soff);

	 BumpClient bc = BumpClient.getBump();
	 Collection<BumpLocation> locs;
	 BalePosition sp;

	 String fullnm = null;

	 fullnm  = bc.getFullyQualifiedName(bd.getProjectName(),bd.getFile(),
					       bd.mapOffsetToEclipse(soff),
					       bd.mapOffsetToEclipse(eoff));


	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findReferences(bd.getProjectName(),
					bd.getFile(),
					bd.mapOffsetToEclipse(soff),
					bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (fullnm == null) {
	    BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.NOIDENTIFIER, sp);
	    Action act = findAction(beepAction);
	    if (act != null) act.actionPerformed(e);
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.REF, sp);
	    Action act = findAction(beepAction);
	    if (act != null) act.actionPerformed(e);
	    return;
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,false,locs,BudaLinkStyle.STYLE_REFERENCE);
       }
      finally { BowiFactory.stopTask(BowiTaskType.FIND_ALL_REFERENCES); }
      BoardMetrics.noteCommand("BALE","GoToReference");
    }

}	// end of inner class GotoReferenceAction




private static class GotoDocAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoDocAction() {
      super("GotoDocAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      int eoff = target.getSelectionEnd();
      BumpClient bc = BumpClient.getBump();

      String fullnm = null;

      fullnm  = bc.getFullyQualifiedName(bd.getProjectName(),bd.getFile(),
					    bd.mapOffsetToEclipse(soff),
					    bd.mapOffsetToEclipse(eoff));

      BalePosition sp = null;
      try {
	 sp = (BalePosition) bd.createPosition(soff);
       }
      catch (BadLocationException ex) { }

      BaleElement be = bd.getCharacterElement(soff);

      if (fullnm != null) {
	 // BoardLog.logD("BALE","FIND DOCUMENTATION FOR " + fullnm);
	 BudaBubble bb = BudaRoot.createDocumentationBubble(fullnm);
	 if (bb != null) {
	    BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(target);
	    BudaBubble obbl = BudaRoot.findBudaBubble(target);

	    Point lp = null;
	    BudaConstants.LinkPort port0 = null;
	    if (sp != null) {
	       port0 = new BaleLinePort(target,sp,"Find Link");
	       lp = port0.getLinkPoint(obbl,obbl.getLocation());
	     }
	    else port0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);

	    bba.addBubble(bb,target,lp,PLACEMENT_RIGHT|PLACEMENT_MOVETO);

	    /****************
	    BudaRoot root = BudaRoot.findBudaRoot(target);
	    Rectangle loc = BudaRoot.findBudaLocation(target);
	    if (lp != null) loc.y = lp.y;
	    root.add(bb,new BudaConstraint(loc.x+loc.width+BUBBLE_CREATION_SPACE,loc.y));
	    ****************/

	    BudaConstants.LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	    BudaBubbleLink lnk = new BudaBubbleLink(obbl,port0,bb,port1);
	    bba.addLink(lnk);
	    bb.markBubbleAsNew();
	    BoardMetrics.noteCommand("BALE","GoToDocumentation");
	    return;
	  }
	 else {
	    if (e.getActionCommand() == null) {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.DOC, sp);
	     }
	    else if (e.getActionCommand().equals("GotoImplementationAction")) {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.IMPLDOC, sp);
	     }
	    else if (e.getActionCommand().equals("GotoDefinitionAction")) {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.DEFDOC, sp);
	     }
	    else {
	       BaleInfoBubble.createInfoBubble(target, be.getName(),BaleInfoBubbleType.DOC, sp);
	     }
	  }
       }
      else {
	 BaleInfoBubble.createInfoBubble(target, be.getName(), BaleInfoBubbleType.NOIDENTIFIER, sp);
       }

      Action act = findAction(beepAction);
      if (act != null) act.actionPerformed(e);
    }

}	// end of inner class GotoDocAction




private static class GotoSearchAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoSearchAction() {
      super("GotoSearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      int eoff = target.getSelectionEnd();

      BumpClient bc = BumpClient.getBump();
      Collection<BumpLocation> locs = bc.findDefinition(bd.getProjectName(),
							   bd.getFile(),
							   bd.mapOffsetToEclipse(soff),
							   bd.mapOffsetToEclipse(eoff));
      if (locs == null || locs.size() == 0) return;

      BumpLocation bloc = null;
      for (BumpLocation bl : locs) {
	 bloc = bl;
	 break;
       }

      if (bloc == null) return;

      String nm = bloc.getSymbolName();
      int idx = nm.lastIndexOf(".");

      switch (bloc.getSymbolType()) {
	 case ENUM_CONSTANT :
	 case FIELD :
	 case FUNCTION :
	 case CONSTRUCTOR :
	    nm = nm.substring(0,idx);
	    break;
	 default :
	    return;
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	    int idx2 = nm.indexOf("<");
	    if (idx2 >= 0) nm = nm.substring(0,idx2);
	    break;
	 case PACKAGE :
	    break;
       }

      BudaRoot br = BudaRoot.findBudaRoot(target);
      Rectangle r = BudaRoot.findBudaLocation(target);
      Point pt = new Point(r.x+r.width+BUBBLE_CREATION_SPACE,r.y);
      br.createMergedSearchBubble(pt,bd.getProjectName(),nm);
      BoardMetrics.noteCommand("BALE","GoToSearch");
    }

}	// end of inner class GotoSearchAction




private static class GotoTypeAction extends TextAction {

   private static final long serialVersionUID = 1;

   GotoTypeAction() {
      super("GotoTypeAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BowiFactory.startTask(BowiTaskType.GO_TO_DEFINITION);
      try {
	 BaleEditorPane target = getBaleEditor(e);
	 if (!checkReadEditor(target)) return;

	 BaleDocument bd = target.getBaleDocument();
	 int soff = target.getSelectionStart();
	 int eoff = target.getSelectionEnd();

	 BumpClient bc = BumpClient.getBump();
	 Collection<BumpLocation> locs;
	 BalePosition sp;
	 try {
	    sp = (BalePosition) bd.createPosition(soff);
	    locs = bc.findTypeDefinition(bd.getProjectName(),
					    bd.getFile(),
					    bd.mapOffsetToEclipse(soff),
					    bd.mapOffsetToEclipse(eoff));
	  }
	 catch (BadLocationException ex) {
	    return;
	  }

	 if (doClassSearchAction(locs)) {
	    goto_search_action.actionPerformed(e);
	    return;
	  }

	 if (locs == null || locs.size() == 0) {
	    if (e.getActionCommand() == null) {
	       e = new ActionEvent(target,e.getID(),"GotoTypeAction",e.getWhen(),e.getModifiers());
	     }
	    goto_doc_action.actionPerformed(e);
	    return;
	  }

	 BaleBubbleStack.createBubbles(target,sp,null,true,locs,BudaLinkStyle.STYLE_SOLID);
       }
      finally { BowiFactory.stopTask(BowiTaskType.GO_TO_DEFINITION); }
      BoardMetrics.noteCommand("BALE","GoToType");
    }

}	// end of inner class GotoTypeAction




private static class ClassSearchAction extends TextAction {

   private static final long serialVersionUID = 1;

   ClassSearchAction() {
      super("ClassSearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;

      String nm = "C:";

      BudaRoot br = BudaRoot.findBudaRoot(target);
      Rectangle r = BudaRoot.findBudaLocation(target);
      Point pt = new Point(r.x+r.width+BUBBLE_CREATION_SPACE,r.y);
      br.createMergedSearchBubble(pt,null,nm);
      BoardMetrics.noteCommand("BALE","ClassSearch");
    }

}	// end of inner class ClassSearchAction




private static boolean doClassSearchAction(Collection<BumpLocation> locs)
{
   if (locs == null || locs.size() == 0) return false;

   BumpSymbolType bst = null;
   BumpLocation baseloc = null;

   for (BumpLocation bl : locs) {
      BumpSymbolType nst = bl.getSymbolType();
      if (nst != BumpSymbolType.CLASS && nst != BumpSymbolType.ENUM &&
		  nst != BumpSymbolType.THROWABLE &&
		  nst != BumpSymbolType.INTERFACE &&
		  nst != BumpSymbolType.PACKAGE) return false;
      if (bst == null) bst = nst;
      else if (bst != nst) return false;
      baseloc = bl;
    }

   if (bst == null) return false;

   if (locs.size() == 2) {
      BumpLocation fnd = null;
      for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
	 BumpLocation xl = it.next();
	 if (fnd != null) {
	    if (fnd.getKey().equals(xl.getKey())) {
	       it.remove();
	     }
	  }
	 fnd = xl;
       }
    }

   if (locs.size() > 1) return true;

   int len = baseloc.getDefinitionEndOffset() - baseloc.getDefinitionOffset();

   switch (bst) {
      case CLASS :
      case THROWABLE :
	 if (len < 1000) return false;
	 break;
      case PACKAGE :
	 break;
      case INTERFACE :
	 if (len < 1500) return false;
	 break;
      case ENUM :
	 if (len < 2000) return false;
	 break;
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Actions to talk to Eclipse						*/
/*										*/
/********************************************************************************/

private static class OpenEclipseEditorAction extends TextAction {

   private static final long serialVersionUID = 1;

   OpenEclipseEditorAction() {
      super("OpenEclipseEditorAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkReadEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();

      BumpClient bc = BumpClient.getBump();
      BoardMetrics.noteCommand("BALE","JumpToEclipse");

      bc.openEclipseEditor(bd.getProjectName(),
			      bd.getFile(),
			      bd.findLineNumber(soff));
    }

}	// end of inner class OpenEclipseEditorAction




/********************************************************************************/
/*										*/
/*	Error handling actions							*/
/*										*/
/********************************************************************************/

private static class QuickFixAction extends TextAction {

   private static final long serialVersionUID = 1;

   QuickFixAction() {
      super("QuickFixAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane target = getBaleEditor(e);
      if (!checkEditor(target)) return;
      BaleDocument bd = target.getBaleDocument();
      int soff = target.getSelectionStart();
      List<BumpProblem> probs = bd.getProblemsAtLocation(soff);
      if (probs == null) return;

      List<BaleFixer> fixes = new ArrayList<BaleFixer>();
      for (BumpProblem bp : probs) {
	 if (bp.getFixes() != null) {
	    for (BumpFix bf : bp.getFixes()) {
	       BaleFixer fixer = new BaleFixer(bp,bf);
	       if (fixer.isValid()) fixes.add(fixer);
	     }
	  }
       }
      if (fixes.isEmpty()) return;

      BaleFixer fix = null;
      if (fixes.size() == 1) fix = fixes.get(0);
      else {
	 Object [] fixalts = fixes.toArray();
	 fix = (BaleFixer) JOptionPane.showInputDialog(target,"Select Quick Fix","Quick Fix Selector",
							  JOptionPane.QUESTION_MESSAGE,
							  null,fixalts,fixes.get(0));
       }
      if (fix == null) return;

      fix.actionPerformed(e);
      BoardMetrics.noteCommand("BALE","QuickFix");
    }

}	// end of inner class QuickFixAction




/********************************************************************************/
/*										*/
/*	Comment insertion actions						*/
/*										*/
/********************************************************************************/

private static class CommentAction extends TextAction {

   private BuenoType new_type;

   private static final long serialVersionUID = 1;

   CommentAction(String nm,BuenoType newtype) {
      super(nm);
      new_type = newtype;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BaleEditorPane be = getBaleEditor(e);
      if (!checkEditor(be)) return;

      BuenoFactory bf = BuenoFactory.getFactory();
      BuenoLocation bl = new CommentLocation(be,be.getSelectionStart());
      bf.createNew(new_type,bl,null);
    }

}	// end of inner class CommentAction



private static class CommentLocation extends BuenoLocation {

   BaleEditorPane editor_pane;
   int cur_offset;

   CommentLocation(BaleEditorPane be,int offset) {
      editor_pane = be;
      cur_offset = editor_pane.getBaleDocument().getDocumentOffset(offset);
    }

   @Override public String getProject() {
      return editor_pane.getBaleDocument().getProjectName();
    }
   @Override public File getFile() {
      return editor_pane.getBaleDocument().getFile();
    }

   @Override public int getOffset()		{ return cur_offset; }

}	// end of inner class CommentLocation




/********************************************************************************/
/*										*/
/*	Key designator class							*/
/*										*/
/********************************************************************************/

private static class KeyItem {

   private KeyStroke key_stroke;
   private Action key_action;

   KeyItem(String key,Action a) {
      key = fixKey(key);
      key_stroke = KeyStroke.getKeyStroke(key);
      if (key_stroke == null) {
	 BoardLog.logE("BALE","Bad key definition: " + key);
       }
      key_action = a;
    }

   KeyItem(String key,String name) {
      key = fixKey(key);
      key_stroke = KeyStroke.getKeyStroke(key);
      if (key_stroke == null) BoardLog.logE("BALE","Bad key definition: " + key);
      Action a = findAction(name);
      if (a == null) BoardLog.logE("BALE","Bad action name: " + name);
      key_action = a;
    }

   void addToKeyMap(Keymap kmp) {
      if (key_stroke != null && key_action != null) {
	 kmp.addActionForKeyStroke(key_stroke,key_action);
       }
    }

   private String fixKey(String key) {
      return key.replace(MENU_KEY,menu_keyname);
    }

}	// end of inner class KeyItem




}	// end of class BaleEditorKit





/* end of BaleEditorKit.java */
