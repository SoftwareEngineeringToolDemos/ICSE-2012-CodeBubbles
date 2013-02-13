/********************************************************************************/
/*										*/
/*		BhelpFactory.java						*/
/*										*/
/*	Factory for Bubbles help demonstrations 				*/
/*										*/
/********************************************************************************/
/*	Copyright 2012 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2012, Brown University, Providence, RI.				 *
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



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.io.*;
import java.util.*;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;



public class BhelpFactory implements BhelpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BhelpDemo>	demo_map;
private BudaRoot                buda_root;
private BhelpWebServer          web_server;

private static BhelpFactory	the_factory = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BhelpFactory getFactory()
{
   return the_factory;
}



private BhelpFactory(BudaRoot br)
{
   buda_root = br;
   demo_map = new HashMap<String,BhelpDemo>();
   InputStream ins = BoardProperties.getLibraryFile(HELP_RESOURCE);
   if (ins != null) {
      Element xml = IvyXml.loadXmlFromStream(ins);
      for (Element ex : IvyXml.children(xml,"DEMO")) {
	 BhelpDemo bd = new BhelpDemo(ex);
	 demo_map.put(bd.getName(),bd);
       }
    }
   BudaRoot.addHyperlinkListener("showme",new Hyperlinker());
   
   try {
      web_server = new BhelpWebServer();
      web_server.process();
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*										*/
/*	Setup methods (called by BEAM)						*/
/*										*/
/********************************************************************************/

public static void setup()
{
   // work is done by the static initializer
}



public static void initialize(BudaRoot br)
{
   the_factory = new BhelpFactory(br);

   br.registerKeyAction(new TestAction(),"Test Help Sequence",
         KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,
               InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK));
}




/********************************************************************************/
/*										*/
/*	Help demonstration start						*/
/*										*/
/********************************************************************************/

public void startDemonstration(Component comp,String name)
{
   if (comp == null) {
      comp = buda_root.getCurrentBubbleArea();
    }
   
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(comp);
   if (bba == null) {
      BudaRoot br = BudaRoot.findBudaRoot(comp);
      if (br == null) return;
      bba = br.getCurrentBubbleArea();
      if (bba == null) return;
    }
   BhelpDemo demo = demo_map.get(name);
   if (demo == null) return;
   demo.executeDemo(bba);
}



/********************************************************************************/
/*                                                                              */
/*      Hyperlink actions                                                       */
/*                                                                              */
/********************************************************************************/

private class Hyperlinker implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      String d = e.getDescription();
      int idx = d.indexOf(":");
      if (idx < 0) return;
      String what = d.substring(idx+1);
      startDemonstration((Component) e.getSource(),what);
   }
   
}	// end of inner class Hyperlinker



/********************************************************************************/
/*                                                                              */
/*      Test actions                                                            */
/*                                                                              */
/********************************************************************************/

private static class TestAction extends AbstractAction {
   
   @Override public void actionPerformed(ActionEvent e) {
      BhelpFactory bf = BhelpFactory.getFactory();
      Component c = (Component) e.getSource();
      bf.startDemonstration(c,"testaction");
    }
   
}       // end of inner class TestAction


}	// end of class BhelpFactory




/* end of BhelpFactory.java */

