/********************************************************************************/
/*										*/
/*		BoppComboOption.java						*/
/*										*/
/*	Handle enumeration choice options					*/
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

import java.awt.Color;
import java.util.ArrayList;


public class BoppComboOption extends BoppOption {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JComboBox	 combo_box;
private Node	      xml_node;
private String[]	  string_options;
private String[]	  value_options;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppComboOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t,Node node)
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
   addBasicInfo();
   setupList();
   combo_box = new JComboBox(string_options);
   combo_box.setAlignmentX(LEFT_ALIGNMENT);
   combo_box.setBackground(Color.white);
   JPanel p = new JPanel();
   p.setOpaque(false);
   p.setMaximumSize(COMBO_BOX_SIZE);
   p.setMinimumSize(COMBO_BOX_SIZE);
   p.setPreferredSize(COMBO_BOX_SIZE);
   p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
   p.add(combo_box);
   p.add(Box.createHorizontalGlue());
   p.setAlignmentX(LEFT_ALIGNMENT);
   this.add(p);
   this.setMinimumSize(COMBO_OPTION_SIZE);
   this.setMaximumSize(COMBO_OPTION_SIZE);
   this.setPreferredSize(COMBO_OPTION_SIZE);
   resetOption();
}


private void setupList()
{
   ArrayList<String> opts = new ArrayList<String>();
   ArrayList<String> values = new ArrayList<String>();
   for (Node node : IvyXml.children(xml_node)) {
      String text = null;
      String prefname = null;
      if (node.getNodeName().equals("COMBO")) {
	 text = IvyXml.getAttrString(node, "TEXT");
	 prefname = IvyXml.getAttrString(node, "VALUE");
      }
      if (text != null && prefname != null) {
	 opts.add(text);
	 values.add(prefname);
      }
   }
   string_options = opts.toArray(new String[opts.size()]);
   value_options = values.toArray(new String[values.size()]);
}



/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   String s = b_props.getString(opt_name);
   for (int i = 0; i < string_options.length; i++) {
      if (s.equals(value_options[i])) {
	 combo_box.setSelectedIndex(i);
	 return;
      }
   }
}

@Override void setOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   String s = value_options[combo_box.getSelectedIndex()];
   b_props.setProperty(opt_name, s);
   save(b_props);
}




}	// end of class BoppComboOption



/* end of BoppComboOption.java */
