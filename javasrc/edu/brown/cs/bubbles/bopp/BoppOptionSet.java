/********************************************************************************/
/*										*/
/*		BoppOptionSet.java						*/
/*										*/
/*	Hold the various options and provide access to them			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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

package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 *
 * This class is the options panel, displaying all the preferences that can be
 * changed by the user.
 *
 **/

class BoppOptionSet implements BoppConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,List<BoppOptionNew>> tab_map;
private List<BoppOptionNew> all_options;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoppOptionSet()
{
   tab_map = new HashMap<String,List<BoppOptionNew>>();
   all_options = new ArrayList<BoppOptionNew>();
   
   Element xml = IvyXml.loadXmlFromStream(BoardProperties.getLibraryFile(PREFERENCES_XML_FILENAME_NEW));
   for (Element op : IvyXml.children(xml,"PACKAGE")) {
      loadXmlPackage(op);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

Collection<String> getTabNames()
{
   return new ArrayList<String>(tab_map.keySet());
}


List<BoppOptionNew> getOptionsForTab(String tab)
{
   return tab_map.get(tab);
}



/********************************************************************************/
/*                                                                              */
/*      Loading methods                                                         */
/*                                                                              */
/********************************************************************************/

private void loadXmlPackage(Element px)
{
   String pname = IvyXml.getAttrString(px,"NAME");
   for (Element op : IvyXml.children(px,"OPT")) {
      loadXmlOption(op,pname);
    }
}



private void loadXmlOption(Element ox,String pkgname)
{
   BoppOptionNew bopt = BoppOptionBase.getOption(pkgname,ox);
   if (bopt == null) return;
   all_options.add(bopt);
   for (String tnm : bopt.getOptionTabs()) {
      List<BoppOptionNew> lopt = tab_map.get(tnm);
      if (lopt == null) {
         lopt = new ArrayList<BoppOptionNew>();
         tab_map.put(tnm,lopt);
       }
      lopt.add(bopt);
    }
}




}	// end of class BoppOptionSet




/* end of BoppOptionSet.java */
