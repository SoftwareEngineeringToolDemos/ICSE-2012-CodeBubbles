/********************************************************************************/
/*										*/
/*		BoppOption.java 						*/
/*										*/
/*	General superclass for the various option types 			*/
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

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



abstract class BoppOption extends JPanel implements BoppConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected String	   opt_name;
protected String	   opt_pack;
protected BoardProperties  b_props;

private String	     opt_description;
private OptionType	 opt_type;
private JLabel	     descript_text;
private JLabel	     example_text;
private String	     search_keywords  = "";
private ArrayList<TabName> containing_tabs;

private static final long  serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 * Constructor for dividers
 */
protected BoppOption(String n,OptionType t)
{
   containing_tabs = new ArrayList<TabName>();
   addToTab(TabName.ALL);
   opt_name = n;
   opt_type = t;
}


/**
 * Constructor for options
 *
 * @param n
 *	     Option name (e.g. Beam.note.width)
 * @param tn
 *	     List of tabs the option belongs to
 * @param d
 *	     Description of the option
 * @param p
 *	     Package name (e.g. Beam)
 * @param t
 *	     Type of option
 */
protected BoppOption(String n,ArrayList<TabName> tn,String d,String p,OptionType t)
{
   if (tn != null) containing_tabs = tn;
   else containing_tabs = new ArrayList<TabName>();
   addToTab(TabName.ALL);
   opt_name = n;
   opt_description = d;
   opt_pack = p;
   opt_type = t;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

ArrayList<TabName> getContainingTabs()
{
   return containing_tabs;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

protected void addBasicInfo()
{
   descript_text = new JLabel(opt_description);
   descript_text.setHorizontalAlignment(JLabel.LEFT);
   descript_text.setVerticalAlignment(JLabel.TOP);
   descript_text.setFont(OPTION_PLAIN_FONT);
   descript_text.setMaximumSize(descript_text.getPreferredSize());
   descript_text.setMinimumSize(descript_text.getPreferredSize());
   JPanel p = new JPanel();
   p.setLayout(new BoxLayout(p,BoxLayout.PAGE_AXIS));
   p.setMaximumSize(SEPARATOR_SIZE);
   p.setMinimumSize(SEPARATOR_SIZE);
   p.setPreferredSize(SEPARATOR_SIZE);
   JSeparator sep = new JSeparator();
   p.add(sep);
   sep.setVisible(false);
   this.add(p);
   this.add(Box.createRigidArea(new Dimension(5,0)));
   this.add(descript_text);
}

/**
 * Sets up the option with its default value, should be overriden by subclasses
 * to add features
 */

void setup()
{
   this.setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
   this.setOpaque(false);
}


/**
 * Adds an example label under the option
 */
void addExample(String s)
{
   example_text = new JLabel(s + " ");
   example_text.setFont(OPTION_EXAMPLE_FONT);
   this.add(example_text);
   Dimension d = this.getMinimumSize();
   d = new Dimension(d.width,d.height + LABEL_SIZE);
   this.setMinimumSize(d);
   this.setMaximumSize(d);
   this.setPreferredSize(d);
}


/**
 * Adds a warning under the option (red text)
 */
void addWarning(String s)
{
   JLabel warning = new JLabel(s + " ");
   warning.setFont(OPTION_WARNING_FONT);
   warning.setForeground(WARNING_COLOR);
   this.add(warning);
   Dimension d = this.getMinimumSize();
   d = new Dimension(d.width,d.height + LABEL_SIZE);
   this.setMinimumSize(d);
   this.setMaximumSize(d);
   this.setPreferredSize(d);
}


void addKeyword(String s)
{
   search_keywords += s;
}



/**
 * Saves the option
 */
void save(BoardProperties bp)
{
   try {
      bp.save();
    }
   catch (IOException e) {
      e.printStackTrace();
    }
}



/********************************************************************************/
/*										*/
/*	Methods for interacting with the gui					*/
/*										*/
/********************************************************************************/

/**
 * Adds the given option to a tab (will not duplicate)
 */
void addToTab(TabName t)
{
   if (!containing_tabs.contains(t)) containing_tabs.add(t);
}



boolean search(Pattern[] patterns)
{
   if (opt_type == OptionType.DIVIDER) {
      return false;
    }
   for (Pattern p : patterns) {
      if (p != null) {
	 Matcher namematch = p.matcher(opt_name);
	 Matcher descriptionmatch = p.matcher(opt_description);
	 Matcher keywordmatch = p.matcher(search_keywords);
	 if (!(keywordmatch.find() || namematch.find() || descriptionmatch.find())) return false;
       }
    }
   return true;
}



/********************************************************************************/
/*										*/
/*	Methods for saving and setting options					*/
/*										*/
/********************************************************************************/

/**
 * Resets the option to the value in the users preferences
 */
abstract void resetOption();

/**
 * Sets the option in the user's preferences to the current option value
				  */
abstract void setOption();

void incOption(String name,String pack)
{
   BoppPanelHandler.optionChanged(pack, name);
   BoardProperties updated = BoardProperties.getProperties(BOPP_FILE_NAME);
   int current = updated.getIntOption(pack + name);
   updated.setProperty(pack + name, ++current);
}

String getOptionName()
{
   return opt_name;
}

String getPack()
{
   return opt_pack;
}

OptionType getType()
{
   return opt_type;
}



}	// end of class BoppOption



/* end of BoppOption.java */

