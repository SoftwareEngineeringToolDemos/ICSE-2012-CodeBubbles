/********************************************************************************/
/*										*/
/*		BeamNoteBubble.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items note bubble		*/
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


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


class BeamNoteBubble extends BudaBubble implements BeamConstants,
	BudaConstants.BudaBubbleOutputer
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NoteArea		note_area;
private File			note_file;
private BeamNoteAnnotation	note_annot;

private static BoardProperties	beam_properties = BoardProperties.getProperties("Beam");

private static Map<String,Document> file_documents;


private static final String	MENU_KEY = "menu";
private static final String	menu_keyname;
private static SimpleDateFormat file_dateformat = new SimpleDateFormat("yyMMddHHmmss");

static {
   int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
   if (mask == Event.META_MASK) menu_keyname = "meta";
   else if (mask == Event.CTRL_MASK) menu_keyname = "ctrl";
   else menu_keyname = "ctrl";

   file_documents = new HashMap<String,Document>();
}




private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Keymap for								*/
/*										*/
/********************************************************************************/

private static Keymap		note_keymap;

private static final KeyItem [] key_defs = new KeyItem[] {
   new KeyItem("menu B",new NoteEditorKit.BoldAction()),
      new KeyItem("menu I",new NoteEditorKit.ItalicAction()),
      new KeyItem("menu U",new NoteEditorKit.UnderlineAction()),
      new KeyItem("ctrl 1",new NoteEditorKit.NoteFontSizeAction("font_size_10",10)),
      new KeyItem("ctrl 2",new NoteEditorKit.NoteFontSizeAction("font_size_12",12)),
      new KeyItem("ctrl 3",new NoteEditorKit.NoteFontSizeAction("font_size_16",14)),
      new KeyItem("ctrl 4",new NoteEditorKit.NoteFontSizeAction("font_size_20",16)),
      new KeyItem("ctrl 5",new NoteEditorKit.NoteFontSizeAction("font_size_24",24)),
      new KeyItem("ctrl shift 1",new NoteEditorKit.NoteColorAction("foreground_black",Color.BLACK)),
      new KeyItem("ctrl shift 2",new NoteEditorKit.NoteColorAction("foreground_red",Color.RED)),
      new KeyItem("ctrl shift 3",new NoteEditorKit.NoteColorAction("foreground_green",Color.GREEN)),
      new KeyItem("ctrl shift 4",new NoteEditorKit.NoteColorAction("foreground_blue",Color.BLUE)),
      new KeyItem("ctrl shift 5",new NoteEditorKit.AlignmentAction("align_left",StyleConstants.ALIGN_LEFT)),
      new KeyItem("ctrl shift 6",new NoteEditorKit.AlignmentAction("align_center",StyleConstants.ALIGN_CENTER)),
      new KeyItem("ctrl shift 7",new NoteEditorKit.AlignmentAction("align_right",StyleConstants.ALIGN_RIGHT)),
      new KeyItem("ctrl shift 8",new NoteEditorKit.AlignmentAction("align_justified",StyleConstants.ALIGN_JUSTIFIED)),
      new KeyItem("menu Z",BurpHistory.getUndoAction()),
      new KeyItem("menu Y",BurpHistory.getRedoAction())
};


static {
   Keymap dflt = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
   SwingText.fixKeyBindings(dflt);
   note_keymap = JTextComponent.addKeymap("NOTE",dflt);
   for (KeyItem ka : key_defs) {
      ka.addToKeyMap(note_keymap);
    }
}






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamNoteBubble()
{
   this(null,null,null);
}


BeamNoteBubble(String file,String cnts,BeamNoteAnnotation annot)
{
   super(null,BudaBorder.RECTANGLE);

   if (file != null) {
      try {
	 FileReader fr = new FileReader(file);
	 StringBuffer cbuf = new StringBuffer();
	 char [] buf = new char[1024];
	 for ( ; ; ) {
	    int ln = fr.read(buf);
	    if (ln < 0) break;
	    cbuf.append(buf,0,ln);
	  }
	 cnts = cbuf.toString();
       }
      catch (IOException e) {
	 BoardLog.logE("BEAM","Problem reading note file",e);
	 file = null;
       }
    }

   Document d = null;
   if (file != null) d = file_documents.get(file);

   note_area = null;
   if (d != null) note_area = new NoteArea(d);
   else note_area = new NoteArea(cnts);

   if (file != null) note_file = new File(file);
   else createFileName();

   if (note_file != null) file_documents.put(note_file.getPath(),note_area.getDocument());

   if (annot != null && annot.getDocumentOffset() < 0) annot = null;
   note_annot = annot;
   if (annot != null) {
      BaleFactory.getFactory().addAnnotation(annot);
      annot.setAnnotationFile(note_file);
    }

   // if contents are null, then set the header part of the html with information about
   // the source of this bubble, date, dlm, title, etc.

   setContentPane(note_area,note_area);
}



@Override protected void localDispose()
{
   if (note_file != null) {
      if (note_area.getText().length() == 0 || note_annot == null) note_file.delete();
      note_file = null;
      note_annot = null;
    }
}



/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/**********************************************a**********************************/

@Override protected void paintContentOverview(Graphics2D g)
{
   Dimension sz = getSize();

   g.setColor(NOTE_OVERVIEW_COLOR);
   g.fillRect(0,0,sz.width,sz.height);
}




/********************************************************************************/
/*										*/
/*	Save interface								*/
/*										*/
/********************************************************************************/

@Override public void handleSaveRequest()
{
   saveNote();
}



@Override public void handleCheckpointRequest()
{
   saveNote();
}



private synchronized void saveNote()
{
   if (note_file == null) createFileName();
   if (note_file == null) return;

   try {
      FileWriter fw = new FileWriter(note_file);
      String txt = note_area.getText();
      fw.write(txt);
      fw.close();
    }
   catch (IOException e) {
      BoardLog.logE("BEAM","Problem writing note file",e);
      note_file = null;
    }
}


private void createFileName()
{
   if (note_file != null) return;

   File dir = BoardSetup.getBubblesPluginDirectory();
   if (dir != null) {
      try {
	 for (int i = 0; i < 5; ++i) {
	    String fnm = "Note_" + file_dateformat.format(new Date()) + ".html";
	    File f = new File(dir,fnm);
	    if (f.createNewFile()) {
	       note_file = f;
	       break;
	     }
	    try {
	       Thread.sleep(1000);
	     }
	    catch (InterruptedException e) { }
	  }
       }
      catch (IOException e) {
	 BoardLog.logE("BEAM","Problem creating note file",e);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Configurator interface							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BEAM"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","NOTE");
   if (note_file != null) xw.field("FILE",note_file);
   xw.cdataElement("TEXT",note_area.getText());
   if (note_annot != null) note_annot.saveAnnotation(xw);
}



/********************************************************************************/
/*										*/
/*	Note area implementation						*/
/*										*/
/********************************************************************************/

private static class NoteArea extends JEditorPane implements BeamConstants
{
   private static final long serialVersionUID = 1;


   NoteArea(String cnts) {
      super("text/html",cnts);
      initialize();
      setText(cnts);
    }

   NoteArea(Document d) {
      setContentType("text/html");
      initialize();
      setDocument(d);
    }

   private void initialize() {
      setEditorKit(new NoteEditorKit());
      setKeymap(note_keymap);
      Dimension d = new Dimension(beam_properties.getInt(NOTE_WIDTH),beam_properties.getInt(NOTE_HEIGHT));
      setPreferredSize(d);
      setSize(d);
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setFont(beam_properties.getFont(NOTE_FONT_PROP,NOTE_FONT));
      addMouseListener(new BudaConstants.FocusOnEntry());

      if (beam_properties.getColor(NOTE_TOP_COLOR).getRGB() == beam_properties.getColor(NOTE_BOTTOM_COLOR).getRGB()) {
	 setBackground(beam_properties.getColor(NOTE_TOP_COLOR));
       }
      else setBackground(new Color(0,true));

      BurpHistory.getHistory().addEditor(this);
    }

   @Override protected void finalize() throws Throwable {
      try {
	 BurpHistory.getHistory().removeEditor(this);
       }
      finally { super.finalize(); }
    }

   @Override protected void paintComponent(Graphics g0) {
      if (beam_properties.getColor(NOTE_TOP_COLOR).getRGB() != beam_properties.getColor(NOTE_BOTTOM_COLOR).getRGB()) {
	 Graphics2D g2 = (Graphics2D) g0.create();
	 Dimension sz = getSize();
	 Paint p = new GradientPaint(0f,0f,beam_properties.getColor(NOTE_TOP_COLOR),0f,sz.height,beam_properties.getColor(NOTE_BOTTOM_COLOR));
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setPaint(p);
	 g2.fill(r);
       }
      super.paintComponent(g0);
    }

}	// end of inner class NoteArea



/********************************************************************************/
/*										*/
/*	Editor Kit for notes							*/
/*										*/
/********************************************************************************/

private static class NoteEditorKit extends HTMLEditorKit
{

   private static final long serialVersionUID = 1;

   NoteEditorKit() {
      setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

   private static class NoteFontSizeAction extends FontSizeAction {

      private static final long serialVersionUID = 1;

      NoteFontSizeAction(String nm,int sz) {
	 super(nm,sz);
	 putValue(ACTION_COMMAND_KEY,nm);
       }

    }	// end of inner class NoteFontSizeAction


   private static class NoteColorAction extends ForegroundAction {

      private static final long serialVersionUID = 1;

      NoteColorAction(String nm,Color c) {
	 super(nm,c);
	 putValue(ACTION_COMMAND_KEY,nm);
       }

    }	// end of inner class NoteColorAction


}	// end of inner class NoteEditorKit



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
      if (key_stroke == null) BoardLog.logE("BEAM","Bad key definition: " + key);
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




}	// end of class BeamNoteBubble



/* end of BeamNoteBubble.java */
