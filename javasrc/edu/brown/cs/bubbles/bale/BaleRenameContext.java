/********************************************************************************/
/*										*/
/*		BaleRenameContext.java						*/
/*										*/
/*	Bubble Annotated Language Editor context for renaming			*/
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
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bump.BumpClient;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class BaleRenameContext implements BaleConstants, CaretListener, BuenoConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane	for_editor;
private BaleDocument	for_document;
private BaleElement	for_id;
private String		start_name;

private JDialog 	cur_menu;
private JTextField	rename_field;
private JButton 	accept_button;
private EditMouser	edit_mouser;
private EditKeyer	edit_keyer;
private RenamePanel	the_panel;

private static final int	X_DELTA = 0;
private static final int	Y_DELTA = 0;
private static final Pattern	ID_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleRenameContext(BaleEditorPane edt,int soff)
{
   for_editor = edt;
   for_document = edt.getBaleDocument();
   cur_menu = null;
   the_panel = null;
   start_name = null;

   for_id = for_document.getCharacterElement(soff);
   if (for_id == null || !for_id.isIdentifier()) return;

   int esoff = for_id.getStartOffset();
   int eeoff = for_id.getEndOffset();
   try {
      start_name = for_document.getText(esoff,eeoff-esoff);
    }
   catch (BadLocationException e) { return; }

   for_editor.addCaretListener(this);
   edit_mouser = new EditMouser();
   for_editor.addMouseListener(edit_mouser);
   edit_keyer = new EditKeyer();
   for_editor.addKeyListener(edit_keyer);
   for_editor.setRenameContext(this);

   handleShow();
}



/********************************************************************************/
/*										*/
/*	Methods to handle editing						*/
/*										*/
/********************************************************************************/

private synchronized void removeContext()
{
   if (for_editor == null) return;

   BoardLog.logD("BALE","Remove rename");

   for_editor.setRenameContext(null);
   for_editor.removeCaretListener(this);
   for_editor.removeMouseListener(edit_mouser);
   for_editor.removeKeyListener(edit_keyer);
   for_editor = null;
   for_document = null;
   if (cur_menu != null) {
      cur_menu.setVisible(false);
      cur_menu = null;
    }
}



@Override public void caretUpdate(CaretEvent e)
{
   removeContext();
}



private class EditMouser extends MouseAdapter {

   @Override public void mousePressed(MouseEvent e) {
      removeContext();
    }

}	// end of inner class EditMouser




private class EditKeyer extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      removeContext();
    }

}	// end of inner class EditKeyer




/********************************************************************************/
/*										*/
/*	Classes to handle dialog box						*/
/*										*/
/********************************************************************************/

private synchronized void handleShow()
{
   BaleEditorPane be = for_editor;

   if (be == null) return;	// no longer relevant

   the_panel = new RenamePanel();

   if (for_editor == null) return;

   try {
      int soff = be.getCaretPosition();
      Rectangle r = be.modelToView(soff);
      Window w = null;
      for (Component c = be; c != null; c = c.getParent()) {
	 if (w == null && c instanceof Window) {
	    w = (Window) c;
	    break;
	  }
       }
      cur_menu = new JDialog(w);
      cur_menu.setUndecorated(true);
      cur_menu.setContentPane(the_panel);
      Point p0 = be.getLocationOnScreen();
      cur_menu.setLocation(p0.x + r.x + X_DELTA,p0.y + r.y + r.height + Y_DELTA);
      cur_menu.pack();
      cur_menu.setVisible(true);
      rename_field.grabFocus();
      BoardLog.logD("BALE","Show rename");
    }
   catch (BadLocationException e) {
      removeContext();
    }
}



/********************************************************************************/
/*										*/
/*	Validity checking methods						*/
/*										*/
/********************************************************************************/

private boolean isRenameValid()
{
   String ntext = rename_field.getText();
   if (!isValidId(ntext)) return false;
   if (ntext.equals(start_name)) return false;

   return true;
}



private static boolean isValidId(String text)
{
   if (text == null) return false;

   Matcher m = ID_PATTERN.matcher(text);

   return m.matches();
}



/********************************************************************************/
/*										*/
/*	Class to hold the renaming dialog					*/
/*										*/
/********************************************************************************/

private class RenamePanel extends JPanel {

   private static final long serialVersionUID = 1;

   RenamePanel() {
      setFocusable(false);
      int len = start_name.length() + 4;
      rename_field = new JTextField(start_name,len);
      RenameListener rl = new RenameListener();
      rename_field.addActionListener(rl);
      rename_field.addCaretListener(rl);
      add(rename_field);
      accept_button = new JButton(new AcceptAction());
      accept_button.setEnabled(false);
      add(accept_button);
      JButton cb = new JButton(new CancelAction());
      add(cb);
      setBorder(new MatteBorder(5,5,5,5,new Color(200,200,200,0)));
    }

}	// end of inner class CompletionPanel




/********************************************************************************/
/*										*/
/*	Handle the renaming							*/
/*										*/
/********************************************************************************/

private void rename()
{
   String ntext = rename_field.getText();

   if (for_id == null) return;

   int soff = for_document.mapOffsetToEclipse(for_id.getStartOffset());
   int eoff = for_document.mapOffsetToEclipse(for_id.getEndOffset());

   BudaRoot br = BudaRoot.findBudaRoot(for_editor);
   if (br != null) br.handleSaveAllRequest();

   BumpClient bc = BumpClient.getBump();
   Element edits = bc.rename(for_document.getProjectName(),for_document.getFile(),soff,eoff,ntext);

   removeContext();

   if (edits == null) return;

   BaleApplyEdits bae = new BaleApplyEdits();
   bae.applyEdits(edits);

   if (br != null) br.handleSaveAllRequest();
}



/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

private class RenameListener implements ActionListener, CaretListener {

   @Override public void actionPerformed(ActionEvent e) {
      if (!isRenameValid()) return;
      rename();
    }

   @Override public void caretUpdate(CaretEvent e) {
      accept_button.setEnabled(isRenameValid());
    }

}	// end of inner class RenameListener



private class AcceptAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   AcceptAction() {
      super(null,BoardImage.getIcon("accept"));
      putValue(LONG_DESCRIPTION,"Accept the rename");
    }

   @Override public void actionPerformed(ActionEvent e) {
      rename();
    }

}	// end of inner class AcceptAction




private class CancelAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   CancelAction() {
      super(null,BoardImage.getIcon("cancel"));
      putValue(LONG_DESCRIPTION,"Cancel the rename");
    }

   @Override public void actionPerformed(ActionEvent e) {
      removeContext();
    }

}	// end of inner class CancelAction




}	// end of class BaleRenameContext




/* end of BaleRenameContext.java */
