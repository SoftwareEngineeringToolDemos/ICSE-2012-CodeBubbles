/********************************************************************************/
/*										*/
/*		BoppIntOption.java						*/
/*										*/
/*	Panel for choosing an integral value					*/
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

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.regex.Pattern;


class BoppIntOption extends BoppOption {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextField	int_field;
private JSlider    int_slider;
private BoppDoubleSlider  double_slider;

private Node	      xml_node;


private int	       dec_precision;
private double	    input_min;
private double	    input_max;
private boolean    negative_allowed;
private boolean    decimal_allowed;
private boolean    has_max;
private boolean    has_min;

private boolean    field_changing;
private boolean    slider_changing;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppIntOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t,Node node)
{
   super(n,tn,d,p,t);
   xml_node = node;
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void setup()
{
   super.setup();
   field_changing = false;
   slider_changing = false;
   negative_allowed = IvyXml.getAttrBool(xml_node, "NEGATIVEALLOWED", false);
   decimal_allowed = IvyXml.getAttrBool(xml_node, "DECIMALALLOWED", false);
   dec_precision = IvyXml.getAttrInt(xml_node, "PRECISION", 2);
   String s = IvyXml.getAttrString(xml_node, "MAX");
   if (s == null) {
      has_max = false;
    }
   else {
      try {
	 input_max = Double.parseDouble(s);
	 has_max = true;
       }
      catch (Exception e) {
	 has_max = false;
       }
    }
   s = IvyXml.getAttrString(xml_node, "MIN");
   if (s == null) {
      has_min = false;
    }
   else {
      try {
	 input_min = Double.parseDouble(s);
	 has_min = true;
       }
      catch (Exception e) {
	 has_min = false;
       }
    }
   addBasicInfo();
   int_field = new JTextField(new NumericDocument(),"",INT_LABEL_WIDTH);
   int_field.setMaximumSize(int_field.getPreferredSize());
   int_field.getDocument().addDocumentListener(new IntFieldListener());
   resetOption();
   this.add(int_field);
   this.setMinimumSize(INT_OPTION_SIZE);
   this.setMaximumSize(INT_OPTION_SIZE);
   this.setPreferredSize(INT_OPTION_SIZE);
   if (has_min && has_max && IvyXml.getAttrBool(xml_node, "SLIDER", false)) {
      addSlider();
    }
}



void addSlider()
{
   if (input_min >= input_max) {
      return;
    }
   if (input_min > getDoubleValue() || input_max < getDoubleValue()) {
      return;
    }
   Dimension d = getPreferredSize();
   JSlider slider;
   if (decimal_allowed) {
      double_slider = new BoppDoubleSlider(input_min,input_max,getDoubleValue(),
					      dec_precision);
      slider = double_slider;
    }
   else {
      int_slider = new JSlider((int) input_min,(int) input_max,getIntValue());
      slider = int_slider;
    }

   slider.setOpaque(false);
   slider.setPreferredSize(SLIDER_SIZE);
   slider.setMaximumSize(SLIDER_SIZE);
   slider.setMinimumSize(SLIDER_SIZE);
   slider.setAlignmentX(LEFT_ALIGNMENT);
   slider.addChangeListener(new SliderListener());
   JPanel p = new JPanel();
   p.setAlignmentX(LEFT_ALIGNMENT);
   p.setOpaque(false);
   p.setPreferredSize(SLIDER_SIZE);
   p.add(slider);
   this.add(p);
   d = new Dimension(d.width,d.height + SLIDER_INT_SIZE);
   this.setMinimumSize(d);
   this.setMaximumSize(d);
   this.setPreferredSize(d);
}



/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

void resetOption()
{
   BoardProperties bp = BoardProperties.getProperties(opt_pack);
   int_field.setText(bp.getString(opt_name));
}



void setOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   String current = b_props.getString(opt_name);
   if (!current.equals(int_field.getText())) {
      incOption(opt_name, opt_pack);
      b_props.setProperty(opt_name, int_field.getText());
    }
   save(b_props);

}


private double getDoubleValue()
{
   try {
      return Double.parseDouble(int_field.getText());
    }
   catch (NumberFormatException e) {
      return 0;
    }
}



private int getIntValue()
{
   try {
      return Integer.parseInt(int_field.getText());
    }
   catch (NumberFormatException e) {
      return 0;
    }
}


/********************************************************************************/
/*										*/
/*	Interfacing between text field and slider				*/
/*										*/
/********************************************************************************/

private class NumericDocument extends PlainDocument {

   private static final long serialVersionUID = 1;
   private Pattern    digit_pattern;

   public NumericDocument() {
      super();
      String pattern = "^";

      if (negative_allowed) {
	 pattern += "(\\d|-)?(\\d)*";
       }
      else {
	 pattern += "(\\d)*";
       }

      if (decimal_allowed) {
	 pattern += "\\.?(\\d{0," + dec_precision + "})$";
       }
      else {
	 pattern += "$";
       }

      digit_pattern = Pattern.compile(pattern);
    }


   public void insertString(int offset,String str,AttributeSet attr) throws BadLocationException {
      if (str != null) {
	 StringBuffer s = new StringBuffer(getText(0, getLength()));
	 String updated = s.insert(offset, str).toString();
	 if (digit_pattern.matcher(updated).matches()) {
	    super.insertString(offset, str, attr);
	  }
       }
    }

}	// end of inner class NumericDocument



private class IntFieldListener implements DocumentListener {

   @Override public void changedUpdate(DocumentEvent arg0) {
      fieldChanged();
    }

   @Override public void insertUpdate(DocumentEvent arg0) {
      fieldChanged();
    }

   @Override public void removeUpdate(DocumentEvent arg0) {
      fieldChanged();
    }

}	// end of inner class IntFieldListener




private void fieldChanged()
{
   field_changing = true;

   if (!slider_changing) {
      updateSlider();
    }

   slider_changing = false;
   field_changing = false;
}



private class SliderListener implements ChangeListener {

   public void stateChanged(ChangeEvent arg0) {
      slider_changing = true;
      if (!field_changing) {
	 updateField();
       }
      field_changing = false;
      slider_changing = false;
    }

}	// end of inner class SliderListener



private void updateSlider()
{
   if (int_slider != null) {
      int val;
      try {
	 val = getIntValue();
       }
      catch (NumberFormatException e) {
	 return;
       }
      if (val < int_slider.getMinimum() || val > int_slider.getMaximum()) {
	 return;
       }
      else {
	 int_slider.setValue(val);
	 int_slider.repaint();
       }
    }
   if (double_slider != null) {
      double val;
      try {
	 val = getDoubleValue();
       }
      catch (NumberFormatException e) {
	 return;
       }
      if (val < double_slider.getDoubleMinimum()
	     || val > double_slider.getDoubleMaximum()) {
	 return;
       }
      else {
	 double_slider.setDoubleValue(val);
	 double_slider.repaint();
       }
    }
}



private void updateField()
{
   if (int_slider != null) {
      int_field.setText(Integer.toString(int_slider.getValue()));
    }
   if (double_slider != null) {
      int_field.setText(Double.toString(double_slider.getDoubleValue()));
    }
}



}	// end of class BoppIntOption



/* end of BoppIntOption.java */
