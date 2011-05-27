/********************************************************************************/
/*										*/
/*		BddtExceptionBreakpointBubble.java				*/
/*										*/
/*	Bubble to handle the creation of exception breakpoings			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bass.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.swing.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import javax.swing.*;
import java.awt.event.*;

import java.util.*;


class BddtExceptionBreakpointBubble extends BudaBubble implements BddtConstants,
		BumpConstants, BudaConstants, BassConstants, ActionListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JComboBox		exception_box;
private JCheckBox		caught_button;
private JCheckBox		uncaught_button;
private JCheckBox		suspendvm_button;

private static boolean use_hierarchy = false;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtExceptionBreakpointBubble()
{
   setupPanel();
   BoardThreadPool.start(new ExceptionSet());
}



/********************************************************************************/
/*										*/
/*	Panel layout								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   pnl.beginLayout();
   pnl.addBannerLabel("Exception Breakpoint Creator");
   pnl.addSeparator();
   exception_box = pnl.addChoice("EXCEPTION",(new Object [] { "Computing List of Exceptions ..." }),null,null);
   exception_box.setEditable(true);
   caught_button = pnl.addBoolean("CAUGHT",true,null);
   uncaught_button = pnl.addBoolean("UNCAUGHT",true,null);
   suspendvm_button = pnl.addBoolean("SUSPEND VM",false,null);

   pnl.addBottomButton("CANCEL","CANCEL",this);
   pnl.addBottomButton("APPLY","APPLY",this);
   pnl.addBottomButton("DEFINE","DEFINE",this);
   pnl.addSeparator();
   pnl.addBottomButtons();

   setContentPane(pnl);
}



/********************************************************************************/
/*										*/
/*	Action handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();

   if (cmd.equals("CANCEL")) {
      setVisible(false);
    }
   else if (cmd.equals("APPLY")) {
      createBreakpoint();
    }
   else if (cmd.equals("ACCEPT")) {
      createBreakpoint();
      setVisible(false);
    }
}




private void createBreakpoint()
{
   String cls = (String) exception_box.getSelectedItem();
   if (cls == null || cls.length() == 0) return;
   BumpExceptionMode md = BumpExceptionMode.DEFAULT;
   boolean caught = caught_button.isSelected();
   boolean uncaught = uncaught_button.isSelected();
   if (caught & uncaught) md = BumpExceptionMode.ALL;
   else if (caught) md = BumpExceptionMode.CAUGHT;
   else if (uncaught) md = BumpExceptionMode.UNCAUGHT;
   boolean suspendvm = suspendvm_button.isSelected();
   BumpBreakMode bmd = BumpBreakMode.DEFAULT;
   if (suspendvm) bmd = BumpBreakMode.SUSPEND_VM;


   BumpClient bc = BumpClient.getBump();
   bc.getBreakModel().addExceptionBreakpoint(null,cls,md,bmd);
}




/********************************************************************************/
/*										*/
/*	Methods to get the set of exceptions					*/
/*										*/
/********************************************************************************/

private class ExceptionSet implements Runnable {

   @Override public void run() {
      Set<String> etypes = new TreeSet<String>();

      if (use_hierarchy) {
	 BumpClient bc = BumpClient.getBump();
	 Element e = bc.getTypeHierarchy(null,null,"java.lang.Throwable",true);
	 for (Element te : IvyXml.children(e,"TYPE")) {
	    // actually one needs to be more selective here and avoid supertypes
	    etypes.add(IvyXml.getAttrString(te,"NAME"));
	  }
       }
      else {
	 BumpClient bc = BumpClient.getBump();
	 List<BumpLocation> locs = bc.findAllClasses("*Exception");
	 if (locs != null) {
	    for (BumpLocation bl : locs) {
	       etypes.add(bl.getSymbolName());
	     }
	  }
       }

      DefaultComboBoxModel mdl = (DefaultComboBoxModel) exception_box.getModel();
      mdl.removeAllElements();
      for (String s : etypes) mdl.addElement(s);
    }

}	// end of inner class ExceptionSet




}	// end of class BddtExceptionBreakpointBubble




/* end of BddtExceptionBreakpointBubble.java */

