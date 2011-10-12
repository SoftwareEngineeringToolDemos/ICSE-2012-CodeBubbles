/********************************************************************************/
/*										*/
/*		BtedBubble.java 						*/
/*										*/
/*	Bubble Environment text editor bubble					*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook 			*/
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



package edu.brown.cs.bubbles.bted;

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubbleOutputer;
import edu.brown.cs.bubbles.burp.BurpHistory;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.PlainDocument;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.*;

import jsyntaxpane.DefaultSyntaxKit;


class BtedBubble extends BudaBubble implements BtedConstants, BudaBubbleOutputer {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BtedEditorPane		 text_editor;
private BtedFactory		 the_factory;
private File			 current_file;
private RedoAction		 redo_action;
private UndoAction		 undo_action;
private JLabel			 name_label;
private BtedFindBar		 search_bar;
private JScrollPane		 scroll_pane;
private JPanel			 main_panel;
private BurpHistory		 burp_history;
private BtedUndoableEditListener edit_listener;

private static BoardProperties	 bted_props	  = BoardProperties.getProperties("Bted");

private static File		last_directory = null;

private static final long	serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BtedBubble(String path,boolean newFile)
{

   DefaultSyntaxKit.initKit(bted_props.getBoolean(WRAPPING));
   text_editor = new BtedEditorPane();
   text_editor.setOpaque(false);
   edit_listener = new BtedUndoableEditListener();
   name_label = new JLabel();

   scroll_pane = new JScrollPane(text_editor);
   scroll_pane.setOpaque(false);
   if (bted_props.getBoolean(WRAPPING)) {
      scroll_pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
   else {
      scroll_pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

   burp_history = BurpHistory.getHistory();

   the_factory = BtedFactory.getFactory();

   main_panel = new JPanel(new BorderLayout());
   this.setPreferredSize(new Dimension(bted_props.getInt(INITIAL_WIDTH),bted_props
					  .getInt(INITIAL_HEIGHT)));
   main_panel.setPreferredSize(new Dimension(bted_props.getInt(INITIAL_WIDTH) - 10,
						bted_props.getInt(INITIAL_HEIGHT) - 10));
   main_panel.add(scroll_pane, BorderLayout.CENTER);

   this.setupGui();

   if (path == null && !newFile) {
      if (!this.openFileFromStart()) {
	 this.newFile();
       }
    }
   else if (path == null && newFile) {
      this.newFile();
    }
   else {
      current_file = new File(path);
      the_factory.loadFileIntoEditor(current_file, text_editor, edit_listener);
      name_label.setText(current_file.getName());
    }

   this.setContentPane(main_panel, text_editor);

}



/**
 * Overrided to close the document when the bubble is closed
 */
@Override protected void localDispose()
{
   this.onClose();
   super.localDispose();
}



/********************************************************************************/
/*										*/
/*	Window setup								*/
/*										*/
/********************************************************************************/

private void setupGui()
{
   JToolBar toolBar = new JToolBar();
   toolBar.setFloatable(false);

   AbstractAction newFileAction = new NewFileAction();
   AbstractAction openFileAction = new OpenFileAction();
   AbstractAction saveFileAction = new SaveFileAction();
   AbstractAction saveFileAsAction = new SaveFileAsAction();

   JButton newButton = new JButton(newFileAction);
   JButton openButton = new JButton(openFileAction);
   JButton saveButton = new JButton(saveFileAction);
   JButton saveAsButton = new JButton(saveFileAsAction);

   newButton.setIcon(new ImageIcon(BoardImage.getImage("filenew.png")));
   openButton.setIcon(new ImageIcon(BoardImage.getImage("fileopen.png")));
   saveButton.setIcon(new ImageIcon(BoardImage.getImage("filesave.png")));
   saveAsButton.setIcon(new ImageIcon(BoardImage.getImage("filesaveas.png")));

   newButton.setMargin(BUTTON_MARGIN);
   openButton.setMargin(BUTTON_MARGIN);
   saveButton.setMargin(BUTTON_MARGIN);
   saveAsButton.setMargin(BUTTON_MARGIN);

   toolBar.add(newButton);
   toolBar.add(openButton);
   toolBar.add(saveButton);
   toolBar.add(saveAsButton);

   undo_action = new UndoAction();
   redo_action = new RedoAction();

   JButton undoButton = new JButton(undo_action);
   JButton redoButton = new JButton(redo_action);

   undoButton.setIcon(new ImageIcon(BoardImage.getImage("undo.png")));
   redoButton.setIcon(new ImageIcon(BoardImage.getImage("redo.png")));

   undoButton.setMargin(BUTTON_MARGIN);
   redoButton.setMargin(BUTTON_MARGIN);

   toolBar.add(undoButton);
   toolBar.add(redoButton);

   search_bar = new BtedFindBar(text_editor);
   search_bar.setVisible(false);

   InputMap inputMap = main_panel.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
   ActionMap actionMap = main_panel.getActionMap();

   int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

   KeyStroke newFile = KeyStroke.getKeyStroke(KeyEvent.VK_N, mask);
   KeyStroke openFile = KeyStroke.getKeyStroke(KeyEvent.VK_O, mask);
   KeyStroke saveFile = KeyStroke.getKeyStroke(KeyEvent.VK_S, mask);
   KeyStroke undo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask);
   KeyStroke redo = KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask);
   KeyStroke find = KeyStroke.getKeyStroke(KeyEvent.VK_F, mask);
   KeyStroke next = KeyStroke.getKeyStroke("ENTER");

   inputMap.put(newFile, "New File");
   actionMap.put("New File", newFileAction);
   inputMap.put(openFile, "Open File");
   actionMap.put("Open File", openFileAction);
   inputMap.put(saveFile, "Save File");
   actionMap.put("Save File", saveFileAction);
   inputMap.put(undo, "Undo");
   actionMap.put("Undo", undo_action);
   inputMap.put(redo, "Redo");
   actionMap.put("Redo", redo_action);
   inputMap.put(find, "Find");
   actionMap.put("Find", new FindAction());
   search_bar.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(next, "Next");
   search_bar.getActionMap().put("Next", new NextFindAction());

   JPanel topPanel = new JPanel(new BorderLayout());
   topPanel.add(name_label, BorderLayout.NORTH);
   topPanel.add(toolBar, BorderLayout.CENTER);

   scroll_pane.setCursor(Cursor.getDefaultCursor());

   main_panel.add(topPanel, BorderLayout.NORTH);
   main_panel.add(search_bar, BorderLayout.SOUTH);
}



@Override public void setBounds(Rectangle r)
{
   if (bted_props.getBoolean(WRAPPING)) {
      text_editor.setSize(main_panel.getSize());
    }
   super.setBounds(r);
}




/********************************************************************************/
/*										*/
/*	File handling methods							*/
/*										*/
/********************************************************************************/

/**
 * @return the current file
 */
public File getFile()
{
   return current_file;
}



/**
 * Opens a file when the bubble was just opened.
 * @return true if successful
 */

private boolean openFileFromStart()
{
   JFileChooser chooser = new JFileChooser(last_directory);
   int returnVal = chooser.showOpenDialog(this);
   if (returnVal == JFileChooser.APPROVE_OPTION) {
      current_file = chooser.getSelectedFile();
      last_directory = current_file;
      the_factory.loadFileIntoEditor(current_file, text_editor, edit_listener);
      name_label.setText(current_file.getName());
      return true;
    }
   return false;
}



/**
 * Opens a file when the bubble has already been open
 */
private void openFileFromMenu()
{
   JFileChooser chooser = new JFileChooser(last_directory);
   int returnVal = chooser.showOpenDialog(this);
   if (returnVal == JFileChooser.APPROVE_OPTION) {
      this.onClose();
      current_file = chooser.getSelectedFile();
      last_directory = current_file;
      the_factory.reopenBubble(current_file.getPath(), this);
      name_label.setText(current_file.getName());
    }
}



/**
 * Saves the file as the current_file unless it is null, in which case
 * the user is prompted for a location.
 */
private void saveFile()
{
   if (current_file == null) {
      saveAsFile();
    }
   else {
      try {
	 OutputStream os = new BufferedOutputStream(new FileOutputStream(current_file));
	 text_editor.setEditable(false);
	 BudaCursorManager.setGlobalCursorForComponent(this,
							  Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	 Writer w = new OutputStreamWriter(os);
	 text_editor.write(w);
	 w.close();
       }
      catch (IOException e) {
	 e.printStackTrace();
       }
      text_editor.setEditable(true);
      BudaCursorManager.resetDefaults(this);// setCursor(Cursor.getDefaultCursor());
    }
}



/**
 * Asks the user for a location and then calls saveFile() to save at this location
 */
private void saveAsFile()
{
   JFileChooser chooser = new JFileChooser(last_directory);
   int returnVal = chooser.showSaveDialog(this);
   if (returnVal == JFileChooser.APPROVE_OPTION
	  && !the_factory.isFileOpen(chooser.getSelectedFile())) {
      this.onClose();
      current_file = chooser.getSelectedFile();
      last_directory = current_file;
      this.saveFile();
      the_factory.reopenBubble(current_file.getPath(), this);
    }
   else {
      // could not save
    }
}



/**
 * Creates a new plain text document
 */
private void newFile()
{
   this.onClose();
   current_file = null;
   name_label.setText("New File");
   burp_history.addEditor(text_editor);
}



/**
 * Decreases the document count when closed
 */
protected void onClose()
{
   if (current_file != null) {
      the_factory.decreaseCount(current_file);
    }
}




/********************************************************************************/
/*										*/
/*	BudaBubbleOutputer methods						*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()
{
   return "BTED";
}

@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE", "EDITORBUBBLE");
   if (current_file != null) xw.field("PATH", current_file.getPath());
}



/********************************************************************************/
/*										*/
/*	Listeners and actions							*/
/*										*/
/********************************************************************************/


/**
 * Listens for edits to the document
 */
private class BtedUndoableEditListener implements UndoableEditListener {

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      undo_action.updateUndoState();
      redo_action.updateRedoState();
    }

} // end of class BtedUndoableEditListener



/**
 * Undoes the previous edit
 */
private class UndoAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   public UndoAction() {
      this.setEnabled(false);
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      burp_history.undo(text_editor);
      this.updateUndoState();
      redo_action.updateRedoState();
    }

   public void updateUndoState() {
      if (burp_history.canUndo(text_editor)) {
	 this.setEnabled(true);
       }
      else {
	 this.setEnabled(false);
       }
    }

} // end of class UndoAction



/**
 * Redoes the previous undo
 */
private class RedoAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   public RedoAction() {
      this.setEnabled(false);
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      burp_history.redo(text_editor);
      this.updateRedoState();
      undo_action.updateUndoState();
    }

   public void updateRedoState() {
      if (burp_history.canRedo(text_editor)) {
	 this.setEnabled(true);
       }
      else {
	 this.setEnabled(false);
       }
    }

} // end of class RedoAction




private class OpenFileAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      openFileFromMenu();
    }

} // end of class OpenFileAction



private class NewFileAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      newFile();
    }

} // end of class NewFileAction



private class SaveFileAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      saveFile();
    }

} // end of class saveFileAction



private class SaveFileAsAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      saveAsFile();
    }

} // end of class SaveFileAsAction



/**
 * Makes the search bar visible if it was not visible
 * and hides it if it was visible.
 */
private class FindAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   public FindAction() {
      super("Find");
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (search_bar.isVisible()) {
	 search_bar.setVisible(false);
	 text_editor.grabFocus();
       }
      else {
	 search_bar.setVisible(true);
	 search_bar.grabFocus();
       }
    }

} // end of class FindAction



/**
 * If the search bar is visible, it will search for the
 * next item in the text box.
 */
private class NextFindAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   public NextFindAction() {
      super("Find Next");
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      if (search_bar.isVisible()) {
	 search_bar.search(SearchMode.NEXT);
       }
    }

} // end of class NextFindAction




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

/**
 * Subclasses JEditorPane to allow gradient painting.
 */

private static class BtedEditorPane extends JEditorPane {

   private static final long serialVersionUID = 1;

   BtedEditorPane() {
      super("text/plain",null);
      int tvl = bted_props.getIntOption("Bted.tabsize");
      if (tvl > 0) {
	 getDocument().putProperty(PlainDocument.tabSizeAttribute,tvl);
       }
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Paint p = new GradientPaint(0f,0f,bted_props.getColor(TOP_COLOR),0f,sz.height,
				     bted_props.getColor(BOTTOM_COLOR));
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);
      super.paintComponent(g);
    }

} // end of class BtedEditorPane




/**
 * Paints the bubble on the overview panel
 */
@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();
   g.setColor(bted_props.getColor(BtedConstants.OVERVIEW_COLOR));
   g.fillRect(0, 0, sz.width, sz.height);
}



}	// end of class BtedBubble




/* end of BtedBubble.java */
