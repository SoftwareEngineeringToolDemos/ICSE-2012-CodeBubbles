/********************************************************************************/
/*										*/
/*		BudaHelp.java							*/
/*										*/
/*	BUblles Display Area help mechanism					*/
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;




/**
 *	This class provides a top level window for doing bubble management.  It
 *	handles setting up the various subwindows and the communications among
 *	those windows.
 **/

public class BudaHelp extends BudaHover implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Component		for_component;
private BudaHelpClient		help_client;
private HelpArea		help_area;
private JScrollPane		scroll_area;
private boolean 		mouse_inside;
private Element 		help_xml;
private File			help_file;
private long			help_time;
private boolean 		force_help;

private static final String	HELP_WIDTH = "Buda.help.width";
private static final String	HELP_HEIGHT = "Buda.help.height";
private static final String	HELP_FONT_PROP = "Buda.help.font";
private static final Font	HELP_FONT = BoardFont.getFont(Font.SERIF,Font.PLAIN,12);
private static final String	HELP_COLOR_PROP = "Buda.help.color";
private static final Color	HELP_COLOR = new Color(245,222,179);
private static final String	HELP_BORDER_PROP = "Buda.help.border.color";
private static final Color	HELP_BORDER = new Color(165,42,42);
private static final String	HELP_RESOURCE = "helpdoc.xml";





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaHelp(Component c,BudaHelpClient helper)
{
   super(c);

   for_component = c;
   help_client = helper;

   setHoverTime(2000);
   help_area = null;
   scroll_area = null;
   mouse_inside = false;
   force_help = false;

   String fn = BoardSetup.getSetup().getLibraryPath(HELP_RESOURCE);
   if (fn == null) {
      help_file = null;
      help_xml = null;
      help_time = 0;
    }
   else {
      help_file = new File(fn);
      help_xml = IvyXml.loadXmlFromFile(help_file);
      help_time = help_file.lastModified();
    }
}



/********************************************************************************/
/*										*/
/*	Hover callback methods							*/
/*										*/
/********************************************************************************/


void simulateHover(MouseEvent e)
{
   force_help = true;
   super.simulateHover(e);
   force_help = false;
   mouse_inside = true;
}



@Override public void handleHover(MouseEvent e)
{
   if (!force_help && !BudaRoot.showHelpTips()) return;

   String txt = getHelpText(e);
   if (txt == null) return;

   if (help_area == null) {
      Mouser mm = new Mouser();
      help_area = new HelpArea(txt);
      help_area.addMouseListener(mm);
      help_area.addKeyListener(mm);
      scroll_area = new JScrollPane(help_area);
      scroll_area.addMouseListener(mm);
      scroll_area.addKeyListener(mm);
   }
   else {
      help_area.setText(txt);
   }

   int dx = BUDA_PROPERTIES.getInt("Buda.help.delta.x");
   int dy = BUDA_PROPERTIES.getInt("Buda.help.delta.y");

   BudaRoot root = BudaRoot.findBudaRoot(for_component);
   if (root == null) return;
   Container rootpanel = root.getLayeredPane();
   Point pt = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),rootpanel);
   pt.x -= dx;
   pt.y -= dy;

   rootpanel.remove(scroll_area);
   rootpanel.add(scroll_area);
   int w = BUDA_PROPERTIES.getInt(HELP_WIDTH,300);
   int h = BUDA_PROPERTIES.getInt(HELP_HEIGHT,200);
   // Dimension d1 = help_area.getPreferredSize();
   Dimension d = new Dimension(w,h);
   scroll_area.setPreferredSize(d);
   scroll_area.setSize(d);
   scroll_area.setLocation(pt.x,pt.y);
   scroll_area.setVisible(true);
   scroll_area.validate();
   help_area.setCaretPosition(0);
   mouse_inside = false;
}




@Override public void endHover(MouseEvent e)
{
   if (scroll_area == null) return;
   if (e != null && mouse_inside) return;
   if (e != null) {
      Point p0 = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),scroll_area);
      if (p0 == null) return;
      // System.err.println("POINT " + p0 + " " + scroll_area.getWidth() + " " + scroll_area.getHeight());
      if (p0.x >= 0 && p0.x < scroll_area.getWidth() && p0.y >= 0 && p0.y < scroll_area.getHeight()) return;
    }
   scroll_area.setVisible(false);
}



private String getHelpText(MouseEvent e)
{
   String txt = null;

   String lbl = help_client.getHelpLabel(e);
   if (lbl != null) {
      if (help_file != null && help_file.exists() && help_file.lastModified() > help_time) {
	 help_time = help_file.lastModified();
	 help_xml = IvyXml.loadXmlFromFile(help_file);
       }
      else if (help_xml == null) {
	 help_xml = IvyXml.loadXmlFromStream(BoardProperties.getLibraryFile(HELP_RESOURCE));
       }
      for (Element xml : IvyXml.children(help_xml,"HELP")) {
	 String key = IvyXml.getAttrString(xml,"KEY");
	 if (key.equals(lbl)) {
	    txt = IvyXml.getTextElement(xml,"TEXT");
	    if (txt != null && txt.length() > 0) {
	       break;
	    }
	  }
       }
    }

   if (txt == null) {
      txt = help_client.getHelpText(e);
    }

   if (txt != null) {
      txt = txt.trim();
      if (!txt.startsWith("<html>")) txt = "<html>" + txt;
    }

   return txt;
}




/********************************************************************************/
/*										*/
/*	Editor component for help						*/
/*										*/
/********************************************************************************/

private class HelpArea extends JEditorPane {

   private static final long serialVersionUID = 1;

   HelpArea(String cnts) {
      super("text/html",cnts);
      initialize();
      setText(cnts);
    }

   private void initialize() {
      setEditable(false);

      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setFont(BUDA_PROPERTIES.getFont(HELP_FONT_PROP,HELP_FONT));
      addHyperlinkListener(new HyperListener());
      Color c = BUDA_PROPERTIES.getColor(HELP_COLOR_PROP,HELP_COLOR);
      setBackground(c);
      Color bc = BUDA_PROPERTIES.getColor(HELP_BORDER_PROP,HELP_BORDER);
      setBorder(new LineBorder(bc));
    }

}	// end of inner class HelpArea




/********************************************************************************/
/*										*/
/*	Hyper link listener							*/
/*										*/
/********************************************************************************/

private class HyperListener implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	 URL u = e.getURL();
	 if (u == null) {
	    endHover(null);
	    String d = e.getDescription();
	    int idx = d.indexOf(":");
	    if (idx < 0) return;
	    String proto = d.substring(0,idx);
	    HyperlinkListener hl = BudaRoot.getListenerForProtocol(proto);
	    if (hl != null) {
	       hl.hyperlinkUpdate(e);
	    }
	   return;
	 }

	 try {
	    Desktop.getDesktop().browse(u.toURI());
	 }
	 catch (IOException ex) { }
	 catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class HyperListener


private class Mouser extends MouseAdapter implements KeyListener {

   @Override public void mouseEntered(MouseEvent e) {
      // System.err.println("MOUSE ENTERED " + (e.getSource() == scroll_area) + " " + (e.getSource() == help_area) + " " + e);
      mouse_inside = true;
   }

   @Override public void mouseExited(MouseEvent e) {
      // Point p0 = e.getPoint();
      Point p1 = scroll_area.getMousePosition();
      // System.err.println("POINT " + p1 + " " + p0 + " " + scroll_area.getWidth() + " " + scroll_area.getHeight());
      if (p1 != null) return;
      mouse_inside = false;
      // System.err.println("MOUSE EXITED");
      scroll_area.setVisible(false);
   }

   @Override public void mouseClicked(MouseEvent e) { }
   @Override public void mousePressed(MouseEvent e) { }
   @Override public void mouseReleased(MouseEvent e) { }

   @Override public void keyPressed(KeyEvent e) 		{ scroll_area.setVisible(false); }
   @Override public void keyTyped(KeyEvent e)			{ scroll_area.setVisible(false); }
   @Override public void keyReleased(KeyEvent e)		{ }

}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Main program to generate a HTML To-Do page from the help file		*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   File helpfile = new File(BoardSetup.getSetup().getLibraryPath(HELP_RESOURCE));
   Element xml = IvyXml.loadXmlFromFile(helpfile);
   List<WhenInfo> whens = new ArrayList<WhenInfo>();
   List<TodoInfo> todos = new ArrayList<TodoInfo>();
   HTMLEditorKit.Parser par = new ParserDelegator();
   for (Element he : IvyXml.children(xml,"HELP")) {
      String key = IvyXml.getAttrString(he,"KEY");
      String when = IvyXml.getAttrString(he,"WHEN");
      String tag = "when_" + key;
      WhenInfo wi = new WhenInfo(key,when,tag);
      whens.add(wi);
      String htmltext = IvyXml.getTextElement(he,"TEXT");
      ParseHandler pp = new ParseHandler(wi);
      try {
	 par.parse(new StringReader(htmltext),pp,true);
	 todos.addAll(pp.getTodoItems());
       }
      catch (IOException e) { }
    }

   PrintWriter pw = null;
   try {
      pw = new PrintWriter(new FileWriter("helptext.html"));
      pw.println("<html><head>");
      pw.println("<title>The Code Bubbles How-To Page</title>");
      pw.println("<script type='text/javascript'>");
      pw.println("function demo(x) {");
      pw.println("var xmlhttp = new XMLHttpRequest();");
      pw.println("xmlhttp.open('GET','http://localhost:19888/' + x,false);");
      pw.println("xmlhttp.send(null); }");
      pw.println("</script>");
      pw.println("</head>");
      pw.println("<body>");
      pw.println("<h1 align='center'>The Code Bubbles How-To Page</h1>");
      for (WhenInfo wi : whens) {
	 pw.print("<p align='left'><a href='#" + wi.getTag() + "'>");
	 pw.println(wi.getDescription() + "</a>");
	 pw.println("<ul>");
	 for (TodoInfo ti : todos) {
	    if (ti.getWhen() == wi) {
	       pw.print("<li>To <a href='#" + ti.getTag() + "'>");
	       pw.println(ti.getName() + "</a></li>");
	     }
	  }
	 pw.println("</ul></p>");
       }
      for (WhenInfo wi : whens) {
	 pw.println("<hr />");
	 pw.print("<a name='" + wi.getTag() + "'></a>");
	 pw.println("<h2>" + wi.getDescription() + ":</h2>");
	 for (TodoInfo ti : todos) {
	    if (ti.getWhen() == wi) {
	       pw.print("<a name='" + ti.getTag() + "'></a>");
	       pw.println("<h3>To " + ti.getName() + "</h3>");
	       pw.println("<blockquote><p>");
	       pw.println(ti.getHtml());
	       pw.println("</p></blockquote>");
	     }
	  }
       }
      pw.println("</body></html>");
      pw.close();
    }
   catch (IOException e) {
    }
}




private static class WhenInfo {

   private String when_desc;
   private String when_tag;

   WhenInfo(String id,String desc,String tag) {
      when_desc = desc;
      when_tag = tag;
    }

   String getTag()		{ return when_tag; }
   String getDescription()	{ return when_desc; }

}	// end of inner class WhenInfo


private static class TodoInfo {

   private WhenInfo todo_when;
   private String todo_name;
   private String todo_html;
   private String todo_tag;
   private static int todo_counter = 0;

   TodoInfo(WhenInfo wi,String name,String html) {
      todo_when = wi;
      todo_name = name;
      todo_html = html;
      todo_tag = "todo_"+ (++todo_counter);
    }

   WhenInfo getWhen()			{ return todo_when; }
   String getTag()			{ return todo_tag; }
   String getName()			{ return todo_name; }
   String getHtml()			{ return todo_html; }

}	// end of inner class TodoInfo


private static class ParseHandler extends HTMLEditorKit.ParserCallback {

   enum ParseState { START, KEY, PREKEY, POSTKEY, SHOWME, END };


   private WhenInfo when_item;
   private List<TodoInfo> todo_items;
   private StringBuffer todo_buffer;
   private StringBuffer html_buffer;
   private ParseState parse_state;



   ParseHandler(WhenInfo wi) {
      when_item = wi;
      todo_items = new ArrayList<TodoInfo>();
      todo_buffer = new StringBuffer();
      html_buffer = new StringBuffer();
      parse_state = ParseState.START;
    }

   List<TodoInfo> getTodoItems()		{ return todo_items; }

   @Override public void handleSimpleTag(HTML.Tag t,MutableAttributeSet a,int pos) {
      handleStartTag(t,a,pos);
    }

   @Override public void handleStartTag(HTML.Tag t,MutableAttributeSet a,int pos) {
      if (parse_state == ParseState.START) {
	 if (t == HTML.Tag.BODY) parse_state = ParseState.PREKEY;
	 return;
       }
      if (parse_state == ParseState.END || parse_state == ParseState.SHOWME) return;
      if (t == HTML.Tag.P) {
	 finishItem();
	 return;
       }
      else if (t == HTML.Tag.A) {
	 String v = a.getAttribute(HTML.Attribute.HREF).toString();
	 if (v.startsWith("showme:")) {
	    int idx = v.indexOf(":");
	    String what = v.substring(idx+1);
	    html_buffer.append("<form><input onclick='demo(\"" + what + "\");' type='button' value='Show Me' /></form>");
	    parse_state = ParseState.SHOWME;
	    return;
	  }
       }
      StringBuffer buf = new StringBuffer();
      buf.append("<" + t.toString());
      for (Enumeration<?> e = a.getAttributeNames(); e.hasMoreElements(); ) {
	 Object k = e.nextElement();
	 Object v = a.getAttribute(k);
	 buf.append(" " + k.toString() + "='" + v.toString() + "'");
       }
      buf.append(">");
      if (parse_state == ParseState.KEY) todo_buffer.append(buf);
      html_buffer.append(buf);
      if (parse_state == ParseState.PREKEY && t == HTML.Tag.B) parse_state = ParseState.KEY;
    }

   @Override public void handleEndTag(HTML.Tag t,int pos) {
      String v = "</" + t.toString() + ">";
      switch (parse_state) {
	 case START :
	 case END :
	    return;
	 case PREKEY :
	 case POSTKEY :
	    if (t == HTML.Tag.P) {
	       finishItem();
	     }
	    else {
	       html_buffer.append(v);
	     }
	    if (t == HTML.Tag.BODY) parse_state = ParseState.END;
	    break;
	 case KEY :
	    if (t == HTML.Tag.B) {
	       parse_state = ParseState.POSTKEY;
	     }
	    else todo_buffer.append(v);
	    html_buffer.append(v);
	    break;
	 case SHOWME :
	    if (t == HTML.Tag.A) {
	       parse_state = ParseState.POSTKEY;
	     }
	    break;
       }
    }

   @Override public void handleText(char [] data,int pos) {
      switch (parse_state) {
	 case START :
	 case END :
	 case SHOWME :
	    break;
	 case PREKEY :
	 case POSTKEY :
	    html_buffer.append(data);
	    break;
	 case KEY :
	    html_buffer.append(data);
	    todo_buffer.append(data);
	    break;
       }
    }

   private void finishItem() {
      if (todo_buffer.length() > 0) {
	 TodoInfo ii = new TodoInfo(when_item,todo_buffer.toString(),html_buffer.toString());
	 todo_items.add(ii);
       }
      todo_buffer = new StringBuffer();
      html_buffer = new StringBuffer();
      parse_state = ParseState.PREKEY;
    }

}	// end of inner class Parser



}	// end of class BudaHelp



/* end of BudaHelp.java */