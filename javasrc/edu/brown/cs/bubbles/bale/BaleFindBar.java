/********************************************************************************/
/*										*/
/*		BaleFindBar.java						*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor find bar		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Arman Uguray 		      */
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

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;


class BaleFindBar extends JPanel implements BaleConstants, ActionListener, CaretListener, ItemListener//, BudaConstants.NoBubble
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane editor_pane;
private BaleDocument for_document;
private JTextField text_field;
private JCheckBox is_case_sensitive; // toggles whether the search should be case sensitive
private JLabel number_label; // shows how many occurences of the search text have been found
private String	search_for;
private String searched_for; // stores the most recent search text. used to determine whether it is necessary to run a new search
private java.util.List<Position> _occurrences; // stores the locations of occurrences of the search text.
private int current_index; // stores which occurrence was last highlighted - used to facilitate the arrow functions
private int current_caret_position;
private Highlighter _highlighter;
private Object my_highlight_tag;
private int last_dir;

private static Image cancel_image, next_image, prev_image;

static {
   cancel_image = BoardImage.getImage("button_cancel");
   cancel_image = cancel_image.getScaledInstance(10,10,Image.SCALE_SMOOTH);
   next_image = BoardImage.getImage("2dowarrow");
   next_image = next_image.getScaledInstance(10,10,Image.SCALE_SMOOTH);
   prev_image = BoardImage.getImage("2uparrow");
   prev_image = prev_image.getScaledInstance(10,10,Image.SCALE_SMOOTH);
}

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFindBar(BaleEditorPane edt)
{
   super(new BorderLayout());

   setOpaque(true);
   setBackground(new Color(0,0,0,0));
   setBorder(new LineBorder(Color.darkGray, 1, true));

   editor_pane = edt;
   for_document = (BaleDocument) edt.getDocument();
   search_for = null;
   searched_for = null;
   _occurrences = null;
   current_index = 0;
   last_dir = 1;
   current_caret_position = edt.getCaretPosition();

   editor_pane.addCaretListener(new HighlightCanceler());

   Box topbox = new Box(BoxLayout.X_AXIS);
   topbox.add(Box.createHorizontalStrut(3));

   Dimension bdim = new Dimension(12,10);

   JButton b1 = new JButton(new ImageIcon(cancel_image));//BoardImage.getIcon("button_cancel"));
   b1.setActionCommand("DONE");
   b1.addActionListener(this);
   b1.setMaximumSize(bdim);
   b1.setPreferredSize(bdim);
   b1.setSize(bdim);
   b1.setFocusPainted(false);
   b1.setContentAreaFilled(false);
   b1.setBorderPainted(false);
   //topbox.add(b1);

   JButton b2 = new JButton("Prev", new ImageIcon(prev_image));
   b2.setActionCommand("LAST");
   b2.addActionListener(this);
   b2.setBorder(null);
   //b2.setMaximumSize(bdim);
   //b2.setPreferredSize(bdim);
   //b2.setSize(bdim);
   b2.setFocusPainted(false);
   b2.setContentAreaFilled(false);
   b2.setBorderPainted(false);
   //topbox.add(b2);

   JButton b3 = new JButton("Next", new ImageIcon(next_image));
   b3.setActionCommand("NEXT");
   b3.addActionListener(this);
   b3.setBorder(null);
   //b3.setMaximumSize(bdim);
   // b3.setPreferredSize(bdim);
   //b3.setSize(bdim);
   b3.setFocusPainted(false);
   b3.setContentAreaFilled(false);
   b3.setBorderPainted(false);
   //topbox.add(b3);

   //topbox.add(Box.createHorizontalStrut(2));

   text_field = new JTextField(10);
   text_field.setFont(BALE_PROPERTIES.getFont(BALE_CRUMB_FONT));
   text_field.addCaretListener(this);
   text_field.setAction(new SearchAction());
   text_field.addKeyListener(new CloseListener());
   Dimension sz = text_field.getPreferredSize();
   text_field.setMaximumSize(sz);
   text_field.setPreferredSize(sz);
   topbox.add(text_field);

   topbox.add(b3);
   topbox.add(Box.createHorizontalStrut(2));
   topbox.add(b2);
   topbox.add(Box.createHorizontalStrut(2));
   topbox.add(b1);
   //topbox.add(new JSeparator(SwingConstants.VERTICAL));

   add(topbox, BorderLayout.NORTH);

   Box bottombox = new Box(BoxLayout.X_AXIS);
   bottombox.add(Box.createHorizontalGlue());

   is_case_sensitive = new JCheckBox("Case Sensitive?");
   is_case_sensitive.addItemListener(this);
   is_case_sensitive.setSelected(true);
   is_case_sensitive.setHorizontalTextPosition(SwingConstants.LEFT);
   is_case_sensitive.setBackground(new Color(0,0,0,0));
   is_case_sensitive.setBorder(null);
   bottombox.add(is_case_sensitive);

   bottombox.add(Box.createHorizontalStrut(3));

   number_label = new JLabel("Matches:  ");
   number_label.setBackground(new Color(0,0,0,0));
   bottombox.add(number_label);

   bottombox.add(Box.createHorizontalGlue());

   add(bottombox, BorderLayout.CENTER);

   // topbox.add(Box.createHorizontalStrut(2));
   //topbox.add(is_case_sensitive);
   //topbox.add(Box.createHorizontalStrut(2));
   //topbox.add(number_label);
   //add(topbox, BorderLayout.CENTER);

   _highlighter = editor_pane.getHighlighter();
   try {
      my_highlight_tag = _highlighter.addHighlight(0, 0, BaleHighlightContext.getPainter(BaleHighlightType.FIND));
    }
   catch (BadLocationException e) {
      my_highlight_tag = new Object();
    }

   Dimension xdim = getPreferredSize();
   setMaximumSize(xdim);
   setMinimumSize(xdim);
   setPreferredSize(xdim);
   setSize(xdim);
}




/********************************************************************************/
/*										*/
/*	Activate methods							*/
/*										*/
/********************************************************************************/

@Override public void setVisible(boolean fg)
{
   super.setVisible(fg);

   if (fg) {
      text_field.requestFocus();
    }
   else {
      if (getParent() != null && !(getParent() instanceof BudaBubbleArea)) getParent().setVisible(false);
      editor_pane.requestFocus();
    }
}



/********************************************************************************/
/*										*/
/*	Search routines 							*/
/*										*/
/********************************************************************************/

void find(int dir,boolean next)
{
   if (BudaRoot.findBudaBubble(editor_pane) == null) {
      BudaBubble my_bub = BudaRoot.findBudaBubble(this);
      if (my_bub != null) my_bub.setVisible(false);
      return;
    }
   BaleElement currentelement = null;
   for_document.baleWriteLock();
   try{
      BowiFactory.startTask(BowiTaskType.CTRL_F_SEARCH);
      last_dir = dir == 0 ? last_dir : dir;
      // if the text field is empty then don't search but also reset the look
      if (search_for == null || search_for.length() == 0) {
	 clearHighlights();
	 searched_for = null;
	 return;
       }
      // if the user changed the text then run a new search
      if (searched_for == null || (is_case_sensitive.isSelected() && !search_for.equals(searched_for))
	     || (!is_case_sensitive.isSelected() && !search_for.equalsIgnoreCase(searched_for))
	     || current_caret_position != editor_pane.getCaretPosition()) {
	 clearHighlights();
	 searched_for = search_for;
	 // find and store the indices of all the occurrences so that going back and forth doesn't require a new search
	 findAllOccurences(search_for, dir);
	 number_label.setText("Matches: "+_occurrences.size());
	 //current_index = -1;
       }
      if (_occurrences == null || _occurrences.size() == 0) {
	 clearHighlights();
	 return;
       }

      // depending on the find direction, either navigate to the next or previous occurrence.
      // wrap around if necessary
      int found = 0;
      if (dir > 0) {
	 current_index++;
	 if (current_index >= _occurrences.size()) current_index = 0;
       }
      else if (dir < 0) {
	 current_index--;
	 if (current_index < 0) current_index = _occurrences.size() - 1;
       }
      else if (dir == 0) {
	 current_index = 0;
       }
      found = _occurrences.get(current_index).getOffset();
      int len = search_for.length();

      try {
	 current_caret_position = found+len;
	 editor_pane.setCaretPosition(found);
	 editor_pane.moveCaretPosition(found+len);
	 _highlighter.changeHighlight(my_highlight_tag,found,found+len);
	 editor_pane.scrollRectToVisible(editor_pane.modelToView(found+len));
	 currentelement = for_document.getCharacterElement(found);
	 if (currentelement == null) return;
	 BoardMetrics.noteCommand("BALE","Find");

	 if (currentelement.isElided()){
	    currentelement.setElided(false);
	    for_document.handleElisionChange();
	    editor_pane.increaseSizeForElidedElement(currentelement);
	    BoardMetrics.noteCommand("BALE","FindUnElision");
	    BaleEditorBubble.noteElision(editor_pane);
	  }
       }
      catch (BadLocationException e) { }
    }
   finally {
      for_document.baleWriteUnlock();
      BowiFactory.stopTask(BowiTaskType.CTRL_F_SEARCH);
    }
}



private void findAllOccurences(String text, int dir)
{
   try {
      int carpos = editor_pane.getCaretPosition();
      int soff = 0;
      int eoff = for_document.getLength();
      int len = text.length();
      java.util.List<Position> occurrences = new Vector<Position>();
      int tlen = eoff-soff;
      try {
	 boolean search = true;
	 Segment segment = new Segment();
	 Position found;
	 Position bestfound = for_document.createPosition(0);
	 int bestdist = for_document.getLength();
	 while (search) {
	    for_document.getText(soff,tlen,segment);
	    int finalloc = tlen-len;
	    if (finalloc < 0) break;
	    for (int i = 0; i <= tlen-len; ++i) {
	       boolean fnd = true;
	       for (int j = 0; fnd && j < len; ++j) {
		  char x = segment.charAt(i+j);
		  char y = search_for.charAt(j);
		  if (is_case_sensitive.isSelected()) fnd = x == y;
		  else{
		     x = Character.toLowerCase(x);
		     y = Character.toLowerCase(y);
		     fnd = x == y;
		   }
		}
	       if (fnd) {
		  found = for_document.createPosition(i+soff);
		  occurrences.add(found);
		  soff = found.getOffset() + len;
		  if (found.getOffset() - carpos < bestdist && found.getOffset() - carpos > 0 && dir > 0) {
		     bestfound = found;
		     bestdist = found.getOffset() - carpos;
		   }
		  else if (carpos - soff < bestdist && carpos - soff > 0 && dir < 0) {
		     bestfound = found;
		     bestdist = carpos - soff;
		   }
		  tlen = eoff - soff;
		  break;
		}
	       else if (i >= tlen-len) {
		  search = false;
		}
	     }
	  }
	 _occurrences = occurrences;
	 if (dir == 0 || bestfound.getOffset() == 0)  current_index = -1;
	 else if (dir > 0) current_index = _occurrences.indexOf(bestfound)-1;
	 else current_index = _occurrences.indexOf(bestfound)+1;
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem with search: " + e);
       }
    }
   finally {}
}



private void clearHighlights()
{
   try {
      _highlighter.changeHighlight(my_highlight_tag, 0, 0);
    }
   catch (BadLocationException ble) {}
}


/********************************************************************************/
/*										*/
/*	Action Handlers 							*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();

   if (cmd.equals("DONE")) {
      try {
	 _highlighter.changeHighlight(my_highlight_tag, 0, 0);
       } catch (BadLocationException ble) {}
	 setVisible(false);
    }
   else if (cmd.equals("NEXT")) {
      find(1,true);
      text_field.grabFocus();
    }
   else if (cmd.equals("LAST")) {
      find(-1,true);
      text_field.grabFocus();
    }
   else BoardLog.logD("BALE","SEARCH ACTION: " + cmd);
}




@Override public void caretUpdate(CaretEvent e)
{
   JTextField tfld = (JTextField) e.getSource();

   String txt = tfld.getText();
   if (txt.equals(search_for)) return;

   search_for = txt;

   //find(last_direction,false);
}

@Override public void itemStateChanged(ItemEvent e) {
   Object source = e.getItemSelectable();
   if (source == is_case_sensitive) {
      searched_for = null;
    }
}


@Override protected void paintComponent(Graphics g0) {
   super.paintComponent(g0);
   Graphics2D g = (Graphics2D) g0.create();
   Paint p = new GradientPaint(0f, 0f, Color.white, 0f, getHeight(), new Color(192,192,192));

   g.setPaint(p);
   g.fillRect(0, 0, getWidth() , getHeight());
}



private class SearchAction extends AbstractAction {

   private static final long serialVersionUID = 1;


   SearchAction() {
      super("SearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      find(last_dir,true);
    }

}	// end of inner class SearchAction




private class CloseListener extends KeyAdapter {

   private int modifier;

   CloseListener() {
      modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }

   @Override public void keyPressed(KeyEvent e) {
      if (KeyEvent.getKeyText(e.getKeyCode()).equals("F") && e.getModifiers() == modifier){
	 clearHighlights();
	 setVisible(false);
	 if (editor_pane.isVisible()) editor_pane.grabFocus();
       }
    }

   @Override public void keyReleased(KeyEvent e) {
      if (!KeyEvent.getKeyText(e.getKeyCode()).equals("Enter")) find(0, true);
    }

}	// end of inner class CloseListener




private class HighlightCanceler implements CaretListener {

   @Override public void caretUpdate(CaretEvent e) {
      if (current_caret_position != editor_pane.getCaretPosition()) clearHighlights();
    }

}	// end of inner class HighlightCanceler



}	// end of class BaleFindBar





/* end of BaleFindBar.java */
