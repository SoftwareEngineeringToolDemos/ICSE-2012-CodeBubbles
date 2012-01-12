/********************************************************************************/
/*										*/
/*		BoppOptionBase.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.swing.*;

import org.w3c.dom.*;

import java.util.*;
import java.util.regex.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;


abstract class BoppOptionBase implements BoppConstants.BoppOptionNew, BoppConstants
{


/********************************************************************************/
/*										*/
/*	Static constructors							*/
/*										*/
/********************************************************************************/

static BoppOptionBase getOption(String pkgname,Element ox)
{
   OptionType otyp = IvyXml.getAttrEnum(ox,"TYPE",OptionType.NONE);

   switch (otyp) {
      case NONE :
	 break;
      case BOOLEAN :
	 return new OptionBoolean(pkgname,ox);
      case COLOR :
	 return new OptionColor(pkgname,ox);
      case COMBO :
	 return new OptionChoice(pkgname,ox);
      case DIMENSION :
	 return new OptionDimension(pkgname,ox);
      case DIVIDER :
	 return new OptionDivider(pkgname,ox);
      case DOUBLE  :
	 break;
      case FONT :
	 return new OptionFont(pkgname,ox);
      case INTEGER :
	 return new OptionInteger(pkgname,ox);
      case STRING :
	 return new OptionString(pkgname,ox);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected String	option_name;
protected String	option_description;
protected String	package_name;
protected List<String>	option_tabs;
private String		option_keywords;
private BoppOptionSet	option_set;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppOptionBase(String pkgname,Element ox)
{
   package_name = pkgname;
   option_set = null;

   option_name = IvyXml.getAttrString(ox,"NAME");
   option_description = IvyXml.getAttrString(ox,"DESCRIPTION");
   if (option_description == null) option_description = "";
   option_tabs = new ArrayList<String>();
   for (Element tx : IvyXml.children(ox,"TAB")) {
      String nm = IvyXml.getAttrString(tx,"NAME");
      if (nm != null) option_tabs.add(nm);
    }
   option_keywords = "";
   for (Element kx : IvyXml.children(ox,"KEYWORD")) {
      String nm = IvyXml.getAttrString(kx,"TEXT");
      if (nm != null) option_keywords += " " + nm;
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getOptionName() 		{ return option_name; }

@Override public abstract OptionType getOptionType();

void setOptionSet(BoppOptionSet os)		{ option_set = os; }

@Override public Collection<String> getOptionTabs()	{ return option_tabs; }

protected BoardProperties getProperties()
{
   return BoardProperties.getProperties(package_name);
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void noteChange()
{
}

void noteChange(String ... props)
{
   for (String prop : props) {
      if (prop == null) continue;
      option_set.noteChange(package_name,prop);
    }
}



void finishChanges()
{
   option_set.finishChanges();
}





/********************************************************************************/
/*										*/
/*	Search methods								*/
/*										*/
/********************************************************************************/

@Override public boolean search(Pattern [] pats)
{
   if (getOptionType() == OptionType.DIVIDER) return false;

   for (Pattern p : pats) {
      if (p != null) {
	 Matcher m1 = p.matcher(option_name);
	 Matcher m2 = p.matcher(option_description);
	 Matcher m3 = p.matcher(option_keywords);
	 if (!(m1.find() || m2.find() || m3.find())) return false;
       }
    }
	
   return true;
}



/********************************************************************************/
/*										*/
/*	Boolean Options 							*/
/*										*/
/********************************************************************************/

private static class OptionBoolean extends BoppOptionBase implements ActionListener {

   OptionBoolean(String pkgname,Element ox) {
      super(pkgname,ox);
    }

   @Override public OptionType getOptionType()		{ return OptionType.BOOLEAN; }

   @Override public void addButton(SwingGridPanel pnl) {
      pnl.addBoolean(option_description,getValue(),this);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      JCheckBox cbx = (JCheckBox) evt.getSource();
      noteChange();
      setValue(cbx.isSelected());
      finishChanges();
    }

   boolean getValue() {
      BoardProperties bp = getProperties();
      return bp.getBoolean(option_name);
    }

   void setValue(boolean v) {
      BoardProperties bp = getProperties();
      bp.setProperty(option_name,v);
    }

}	// end of inner class OptionBoolean




/********************************************************************************/
/*										*/
/*	Color Options								*/
/*										*/
/********************************************************************************/

private static class OptionColor extends BoppOptionBase implements ActionListener {

   private String from_name;
   private String to_name;

   OptionColor(String pkgname,Element ox) {
      super(pkgname,ox);
      Element px = IvyXml.getChild(ox,"PROPERTIES");
      if (px == null) {
	 from_name = option_name;
	 to_name = null;
       }
      else {
	 from_name = IvyXml.getAttrString(px,"FROM");
	 to_name = IvyXml.getAttrString(px,"TO");
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.COLOR; }

   @Override public void addButton(SwingGridPanel pnl) {
      if (to_name == null) {
	 pnl.addColorField(option_description,getFromValue(),true,this);
       }
      else {
	 pnl.addColorRangeField(option_description,getFromValue(),getToValue(),this);
       }
    }

   @Override public void actionPerformed(ActionEvent evt) {
      noteChange(from_name,to_name);
      if (to_name == null) {
	 SwingColorButton scb = (SwingColorButton) evt.getSource();
	 setFromValue(scb.getColor());
       }
      else {
	 SwingColorRangeChooser scr = (SwingColorRangeChooser) evt.getSource();
	 setFromValue(scr.getFirstColor());
	 setToValue(scr.getSecondColor());
       }
      finishChanges();
    }

   private Color getFromValue() {
      return getProperties().getColor(from_name);
    }

   private Color getToValue() {
      return getProperties().getColor(to_name);
    }

   private void setFromValue(Color c) {
      getProperties().setProperty(from_name,c);
    }

   private void setToValue(Color c) {
      getProperties().setProperty(to_name,c);
    }

}	// end of inner class OptionColor



/********************************************************************************/
/*										*/
/*	Choice options								*/
/*										*/
/********************************************************************************/

private static class OptionChoice extends BoppOptionBase implements ActionListener {

   private Map<String,String> choice_map;
   private Map<String,String> lookup_map;

   OptionChoice(String pkgname,Element ox) {
      super(pkgname,ox);
      choice_map = new LinkedHashMap<String,String>();
      lookup_map = new HashMap<String,String>();
      for (Element ce : IvyXml.children(ox,"COMBO")) {
	 String k = IvyXml.getAttrString(ce,"TEXT");
	 String v = IvyXml.getAttrString(ce,"VALUE");
	 if (k != null && v != null) {
	    choice_map.put(k,v);
	    lookup_map.put(v,k);
	  }
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.COMBO; }

   @Override public void addButton(SwingGridPanel pnl) {
      pnl.addChoice(option_description,choice_map.keySet(),getValue(),this);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox cbx = (JComboBox) evt.getSource();
      String v = (String) cbx.getSelectedItem();
      noteChange();
      setValue(v);
      finishChanges();
    }

   String getValue() {
      String v = getProperties().getProperty(option_name);
      return lookup_map.get(v);
    }

   void setValue(String v) {
      v = choice_map.get(v);
      getProperties().setProperty(option_name,v);
    }

}	// end of inner class OptionChoice




/********************************************************************************/
/*										*/
/*	Dimension Options							*/
/*										*/
/********************************************************************************/

private static class OptionDimension extends BoppOptionBase implements ActionListener {

   private String width_prop;
   private String height_prop;

   OptionDimension(String pkgname,Element ox) {
      super(pkgname,ox);
      Element px = IvyXml.getChild(ox,"PROPERTIES");
      if (px == null) {
	 width_prop = option_name;
	 height_prop = null;
       }
      else {
	 width_prop = IvyXml.getAttrString(px,"WIDTH");
	 height_prop = IvyXml.getAttrString(px,"HEIGHT");
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.DIMENSION; }

   @Override public void addButton(SwingGridPanel pnl) {
      if (height_prop == null) {
	 pnl.addNumericField(option_description,10,1024,getWidthValue(),this);
       }
      else {
	 pnl.addDimensionField(option_description,getWidthValue(),getHeightValue(),this);
       }
    }

   @Override public void actionPerformed(ActionEvent evt) {
      noteChange(width_prop,height_prop);
      if (height_prop == null) {
	 SwingNumericField fld = (SwingNumericField) evt.getSource();
	 setWidthValue((int) fld.getValue());
       }
      else {
	 SwingDimensionChooser dim = (SwingDimensionChooser) evt.getSource();
	 setWidthValue(dim.getWidthValue());
	 setHeightValue(dim.getHeightValue());
       }
      finishChanges();
    }


   private int getWidthValue() {
      return getProperties().getInt(width_prop,0);
    }

   private int getHeightValue() {
      return getProperties().getInt(height_prop,0);
    }

   private void setWidthValue(int v) {
      if (width_prop == null) return;
      if (v == 0) getProperties().remove(width_prop);
      else getProperties().setProperty(width_prop,v);
    }

   private void setHeightValue(int v) {
      if (height_prop == null) return;
      if (v == 0) getProperties().remove(height_prop);
      else getProperties().setProperty(height_prop,v);
    }
}	// end of inner class OptionDimension




/********************************************************************************/
/*										*/
/*	Divider Options 							*/
/*										*/
/********************************************************************************/

private static class OptionDivider extends BoppOptionBase {

   OptionDivider(String pkgname,Element ox) {
      super(pkgname,ox);
    }

   @Override public OptionType getOptionType()		{ return OptionType.DIVIDER; }

   @Override public void addButton(SwingGridPanel pnl) {
      pnl.addSeparator();
      pnl.addSectionLabel(option_name);
    }

}	// end of inner clss OptionDivider



/********************************************************************************/
/*										*/
/*	Font Options								*/
/*										*/
/********************************************************************************/

private static class OptionFont extends BoppOptionBase implements ActionListener {

   private String font_prop;
   private String family_prop;
   private String size_prop;
   private String style_prop;
   private String color_prop;

   OptionFont(String pkgname,Element ox) {
      super(pkgname,ox);
      Element fx = IvyXml.getChild(ox,"PROPERTIES");
      if (fx == null) {
	 font_prop = option_name;
	 family_prop = size_prop = style_prop = null;
       }
      else {
	 font_prop = null;
	 family_prop = IvyXml.getAttrString(fx,"FAMILY");
	 size_prop = IvyXml.getAttrString(fx,"SIZE");
	 style_prop = IvyXml.getAttrString(fx,"STYLE");
	 color_prop = IvyXml.getAttrString(fx,"COLOR");
       }
    }

   @Override public OptionType getOptionType()		{ return OptionType.FONT; }

   @Override public void addButton(SwingGridPanel pnl) {
      int sts = 0;
      if (font_prop == null) {
	 if (family_prop == null) sts |= SwingFontChooser.FONT_FIXED_FAMILY;
	 if (size_prop == null) sts |= SwingFontChooser.FONT_FIXED_SIZE;
	 if (style_prop == null) sts |= SwingFontChooser.FONT_FIXED_STYLE;
	 if (color_prop == null) sts |= SwingFontChooser.FONT_FIXED_COLOR;
       }
      pnl.addFontField(option_description,getFontValue(),getColorValue(),sts,this);
    }

   @Override public void actionPerformed(ActionEvent e) {
      noteChange(font_prop,family_prop,size_prop,style_prop,color_prop);
      SwingFontChooser sfc = (SwingFontChooser) e.getSource();
      setFont(sfc.getFont());
      setColor(sfc.getFontColor());
      finishChanges();
    }

   private Font getFontValue() {
      if (font_prop != null) {
         return getProperties().getFont(font_prop);
       }
      
      String fam = "Serif";
      if (family_prop != null) fam = getProperties().getProperty(family_prop,fam);
      int sz = 12;
      if (size_prop != null) sz = getProperties().getInt(size_prop,sz);
      int sty = Font.PLAIN;
      if (style_prop != null) sty = getProperties().getInt(style_prop,sty);
      return BoardFont.getFont(fam,sty,sz);
    }

   private Color getColorValue() {
      if (color_prop == null) return null;
      return getProperties().getColor(color_prop);
    }

   private void setFont(Font ft) {
      if (font_prop != null) {
	 getProperties().setProperty(font_prop,ft);
       }
      else {
	 if (family_prop != null) getProperties().setProperty(family_prop,ft.getFamily());
	 if (size_prop != null) getProperties().setProperty(size_prop,ft.getSize());
	 if (style_prop != null) getProperties().setProperty(style_prop,ft.getStyle());
       }
    }

   private void setColor(Color c) {
      if (color_prop != null && c != null)
	 getProperties().setProperty(color_prop,c);
    }

}	// end of inner class OptionFont




/********************************************************************************/
/*										*/
/*	Numeric Options 							*/
/*										*/
/********************************************************************************/

private static class OptionInteger extends BoppOptionBase implements ChangeListener, ActionListener {

   private int min_value;
   private int max_value;
   private boolean range_ok;

   OptionInteger(String pkgname,Element ox) {
      super(pkgname,ox);
      min_value = IvyXml.getAttrInt(ox,"MIN",0);
      max_value = IvyXml.getAttrInt(ox,"MAX",0);
      if (min_value >= max_value) range_ok = false;
      else if (IvyXml.getAttrBool(ox,"SLIDER")) range_ok = true;
      else if (max_value - min_value < 10 && max_value - min_value > 2) range_ok = true;
      else range_ok = false;
    }

   @Override public OptionType getOptionType()		{ return OptionType.INTEGER; }

   @Override public void addButton(SwingGridPanel pnl) {
      if (range_ok) {
	 int dec = (max_value - min_value)/100;
	 if (dec < 0) dec = 1;
	 pnl.addRange(option_description,min_value,max_value,dec,getValue(),this);
       }
      else {
	 pnl.addNumericField(option_description,min_value,max_value,getValue(),this);
       }
    }

   @Override public void actionPerformed(ActionEvent evt) {
      noteChange();
      SwingNumericField snf = (SwingNumericField) evt.getSource();
      setValue((int) snf.getValue());
      finishChanges();
    }

   @Override public void stateChanged(ChangeEvent evt) {
      SwingRangeSlider rs = (SwingRangeSlider) evt.getSource();
      setValue((int) rs.getScaledValue());
      noteChange();
    }

   private int getValue() {
      return getProperties().getInt(option_name);
    }

   private void setValue(int v) {
      getProperties().setProperty(option_name,v);
    }
}	// end of inner class OptionInteger



/********************************************************************************/
/*										*/
/*	String options								*/
/*										*/
/********************************************************************************/

private static class OptionString extends BoppOptionBase implements ActionListener {

   OptionString(String pkgname,Element ox) {
      super(pkgname,ox);
    }

   @Override public OptionType getOptionType()		{ return OptionType.STRING; }

   @Override public void addButton(SwingGridPanel pnl) {
      pnl.addTextField(option_description,getValue(),12,this,null);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      noteChange();
      JTextField tf = (JTextField) evt.getSource();
      setValue(tf.getText());
      finishChanges();
    }

   private String getValue() {
      return getProperties().getProperty(option_name);
    }

   private void setValue(String v) {
      getProperties().setProperty(option_name,v);
    }

}	// end of inner class OptionString


}	// end of class BoppOptionBase




/* end of BoppOptionBase.java */

