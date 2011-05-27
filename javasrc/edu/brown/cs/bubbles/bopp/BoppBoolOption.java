/********************************************************************************/
/*										*/
/*		BoppBoolOption.java						*/
/*										*/
/*	Hanel boolean buttons							*/
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

import javax.swing.JCheckBox;

import java.util.ArrayList;


class BoppBoolOption extends BoppOption {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JCheckBox	 boolean_box;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppBoolOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
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
   BoardProperties bp = BoardProperties.getProperties(opt_pack);
   boolean_box = new JCheckBox();
   boolean_box.setSelected(bp.getBooleanOption(opt_name, false));
   boolean_box.setOpaque(false);
   this.add(boolean_box);
   this.setMinimumSize(BOOLEAN_OPTION_SIZE);
   this.setMaximumSize(BOOLEAN_OPTION_SIZE);
   this.setPreferredSize(BOOLEAN_OPTION_SIZE);
}




/********************************************************************************/
/*										*/
/*	Set and get option methods						*/
/*										*/
/********************************************************************************/

@Override void resetOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   boolean_box.setSelected(b_props.getBooleanOption(opt_name));
}




@Override void setOption()
{
   b_props = BoardProperties.getProperties(opt_pack);
   boolean currentbool = b_props.getBooleanOption(opt_name);
   if (currentbool != boolean_box.isSelected()) {
      incOption(opt_name, opt_pack);
      b_props.setProperty(opt_name, boolean_box.isSelected());
    }
   save(b_props);
}


}	// end of class BoppBoolOption




/* end of BoppBoolOption.java */
