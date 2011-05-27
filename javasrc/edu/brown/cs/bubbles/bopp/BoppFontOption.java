/********************************************************************************/
/*										*/
/*		BoppFontOption.java						*/
/*										*/
/*	Buttons of for letting user choose fonts				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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





package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardProperties;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;


/**
 * NOT FUNCTIONAL YET
 *
 * @author ahills
 *
 */

class BoppFontOption extends BoppOption {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JLabel	    font_label;
private Font	current_font;
private FontChooser font_chooser;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppFontOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
{
   super(n,tn,d,p,t);
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setup()
{
   super.setup();
   addBasicInfo();
   makeFontPanel();
   validate();
}



void makeFontPanel()
{
   JPanel fontPanel = new JPanel();
   fontPanel.setLayout(new BoxLayout(fontPanel,BoxLayout.PAGE_AXIS));
   font_label = new JLabel("Example text");
   resetOption();
   fontPanel.add(font_label);
   JButton b = new JButton("Choose a font");
   b.addActionListener(new ChooseFontListener());
   fontPanel.add(b);
   Dimension d = new Dimension(FONT_OPTION_SIZE.width,FONT_OPTION_SIZE.height
				  + (int) font_label.getPreferredSize().getHeight()
				  + (int) b.getPreferredSize().getHeight());
   this.setMaximumSize(d);
   this.setMinimumSize(d);
   this.setPreferredSize(d);
   this.add(fontPanel);
   JFrame f = new JFrame("Choose a font");
   font_chooser = new FontChooser(f);
   f.setVisible(false);
   font_chooser.setVisible(false);
}



/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   current_font = b_props.getFont(opt_name);
   font_label.setFont(current_font);
}

@Override void setOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   b_props.setProperty(opt_name, current_font);
   save(b_props);
}



/********************************************************************************/
/*										*/
/*	Dialog for choosing font						*/
/*										*/
/********************************************************************************/

private class FontChooser extends JDialog {

   private Font    result_font;
   private String  result_name;
   private int	   result_size;
   private boolean is_bold;
   private boolean is_italic;

   private String  font_list[];
   private List    font_name_choice;
   private List    font_size_choice;
   Checkbox	   bold_btn, italic_btn;
   private JLabel  preview_area;

   private static final long serialVersionUID = 1;


   FontChooser(Frame f) {
      super(f,"Font Chooser",true);
    }

   void setup() {
      Container cp = getContentPane();
      Panel top = new Panel();
      top.setLayout(new FlowLayout());

      font_name_choice = new List(8);
      top.add(font_name_choice);

      font_list = GraphicsEnvironment.getLocalGraphicsEnvironment()
	 .getAvailableFontFamilyNames();

      for (int i = 0; i < font_list.length; i++)
	 font_name_choice.add(font_list[i]);
      // System.out.println(current_font);
      font_name_choice.select(grabFont(current_font.getFamily()));

      font_size_choice = new List(8);
      top.add(font_size_choice);

      for (int i = 1; i < 41; i++)
	 font_size_choice.add(Integer.toString(i));
      font_size_choice.select(current_font.getSize() - 1);

      cp.add(top, BorderLayout.NORTH);

      Panel attrs = new Panel();
      top.add(attrs);
      attrs.setLayout(new GridLayout(0,1));
      attrs.add(bold_btn = new Checkbox("Bold",current_font.isBold()));
      attrs.add(italic_btn = new Checkbox("Italic",current_font.isItalic()));

      preview_area = new JLabel(FONT_PREVIEW_TEXT,JLabel.CENTER);
      preview_area.setSize(200, 50);
      cp.add(preview_area, BorderLayout.CENTER);

      Panel bot = new Panel();

      JButton okButton = new JButton("Apply");
      bot.add(okButton);
      okButton.addActionListener(new ApplyListener());

      JButton canButton = new JButton("Cancel");
      bot.add(canButton);
      canButton.addActionListener(new CancelListener());
      cp.add(bot, BorderLayout.SOUTH);

      previewFont(); // ensure view is up to date!
      pack();
      setLocation(100, 100);
    }

   private int grabFont(String s) {
      for (int i = 0; i < font_list.length; i++) {
	 if (s.equals(font_list[i])) return i;
       }
      return 0;
    }

   private class CancelListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
	 result_font = null;
	 result_name = null;
	 result_size = 0;
	 is_bold = false;
	 is_italic = false;
	 dispose();
	 setVisible(false);
       }
    }

   @SuppressWarnings("unused") private class PreviewListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
       }
    }

   private class ApplyListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
	 previewFont();
	 dispose();
	 setVisible(false);
       }
    }

   /**
     * Called from the action handlers to get the font info, build a font, and set
     * it.
     */
   protected void previewFont() {
      result_name = font_name_choice.getSelectedItem();
      String resultsizename = font_size_choice.getSelectedItem();
      int resultsize = Integer.parseInt(resultsizename);
      is_bold = bold_btn.getState();
      is_italic = italic_btn.getState();
      int attrs = Font.PLAIN;
      if (is_bold) attrs = Font.BOLD;
      if (is_italic) attrs |= Font.ITALIC;
      result_font = new Font(result_name,attrs,resultsize);
      current_font = result_font;
      font_label.setFont(current_font);
      font_label.repaint();
      preview_area.setFont(result_font);
      // System.out.println("Name: " + result_name + " Size: " + resultsizename
      // + " Bold: " + is_bold);
      pack(); // ensure Dialog is big enough.
    }

   /** Retrieve the selected font name. */
   @SuppressWarnings("unused") String getSelectedName() {
      return result_name;
    }

   /** Retrieve the selected size */
    @SuppressWarnings("unused") int getSelectedSize() {
      return result_size;
    }

   /** Retrieve the selected font, or null */
   @SuppressWarnings("unused") Font getSelectedFont() {
      return result_font;
    }

}	// end of class FontChooser


private class ChooseFontListener implements ActionListener {

   public void actionPerformed(ActionEvent e) {
      font_chooser.setup();
      font_chooser.setVisible(true);
    }

}	// end of class ChooseFontListener



}	// end of class BoppFontOption


/* end of BoppFontOption.java */
