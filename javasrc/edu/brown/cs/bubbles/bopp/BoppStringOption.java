/********************************************************************************/
/*										*/
/*		BoppStringOption.java						*/
/*										*/
/*	Panel for a string value option 					*/
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

import javax.swing.JTextField;

import java.util.ArrayList;


class BoppStringOption extends BoppOption {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextField string_field;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppStringOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
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
   b_props = BoardProperties.getProperties(opt_pack);
   string_field = new JTextField(45);
   string_field.setText(b_props.getStringOption(opt_name));
   string_field.setMaximumSize(string_field.getPreferredSize());
   this.add(string_field);
   this.setMinimumSize(STRING_OPTION_SIZE);
   this.setMaximumSize(STRING_OPTION_SIZE);
   this.setPreferredSize(STRING_OPTION_SIZE);
}



/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   string_field.setText(b_props.getStringOption(opt_name));
}



@Override void setOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   String currentstr = b_props.getStringOption(opt_name);
   if (!currentstr.equals(string_field.getText())) {
      incOption(opt_name, opt_pack);
      b_props.setProperty(opt_name, string_field.getText());
    }
   save(b_props);
}



}	// end of class BoppStringOption



/* end of BoppStringOption.java */
