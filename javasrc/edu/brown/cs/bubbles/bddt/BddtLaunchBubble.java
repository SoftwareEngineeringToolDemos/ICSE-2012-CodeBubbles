/********************************************************************************/
/*										*/
/*		BddtLaunchBubble.java						*/
/*										*/
/*	Bubble Environment launch configuration bubble				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook, Steven P. Reiss	*/
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
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubbleOutputer;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


class BddtLaunchBubble extends BudaBubble implements BddtConstants, BudaConstants, BassConstants,
	BumpConstants, BudaBubbleOutputer, ActionListener, UndoableEditListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient		bump_client;
private BumpLaunchConfig	launch_config;
private BumpLaunchConfig	edit_config;
private JTextComponent		arg_area;
private JTextComponent		vmarg_area;
private JButton 		debug_button;
private JButton 		save_button;
private JButton 		revert_button;
private JButton 		clone_button;
private JComboBox		start_class;



private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Cnostructors								*/
/*										*/
/********************************************************************************/

BddtLaunchBubble(BumpLaunchConfig cfg)
{
   bump_client = BumpClient.getBump();
   launch_config = cfg;
   edit_config = null;

   setupPanel();
}




/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   pnl.addTextField("Launch Name",launch_config.getConfigName(),this,null);

   String lp = launch_config.getProject();
   Element pxml = bump_client.getAllProjects();
   if (pxml != null) {
      List<String> pnms = new ArrayList<String>();
      for (Element pe : IvyXml.children(pxml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 pnms.add(pnm);
       }
      if (pnms.size() > 1) {
	 pnl.addChoice("Project",pnms,lp,this);
       }
    }

   List<String> starts = new ArrayList<String>();
   BassRepository br = BassFactory.getRepository(BassConstants.SearchType.SEARCH_CODE);
   for (BassName bn : br.getAllNames()) {
      if (bn.getProject().equals(lp) && bn.getName().endsWith(".main") &&
	       bn.getNameType() == BassNameType.METHOD &&
	       Modifier.isPublic(bn.getModifiers()) &&
	       Modifier.isStatic(bn.getModifiers())) {
	 String cn = bn.getPackageName() + "." + bn.getClassName();
	 starts.add(cn);
      }
   }
   start_class = pnl.addChoice("Start Class",starts,launch_config.getMainClass(),this);

   arg_area = pnl.addTextArea("Arguments",launch_config.getArguments(),2,24,this);
   vmarg_area = pnl.addTextArea("VM Arguments",launch_config.getVMArguments(),1,24,this);
   pnl.addSeparator();
   debug_button = pnl.addBottomButton("Debug","DEBUG",this);
   save_button = pnl.addBottomButton("Save","SAVE",this);
   revert_button = pnl.addBottomButton("Revert","REVERT",this);
   clone_button = pnl.addBottomButton("Clone","CLONE",this);
   pnl.addBottomButtons();
   fixButtons();

   setContentPane(pnl,arg_area);
}



private void fixButtons()
{
   if (edit_config == launch_config) edit_config = null;

   if (debug_button == null) return;

   debug_button.setEnabled(edit_config == null);
   save_button.setEnabled(edit_config != null);
   revert_button.setEnabled(edit_config != null);
   clone_button.setEnabled(true);
}


private void reload()
{
   arg_area.setText(launch_config.getArguments());
   vmarg_area.setText(launch_config.getVMArguments());
   start_class.setSelectedItem(launch_config.getMainClass());
}


private String getNewName()
{
   return launch_config.getConfigName() + " (2)";
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g)
{
   Dimension sz = getSize();
   g.setColor(BDDT_LAUNCH_OVERVIEW_COLOR);
   g.fillRect(0, 0, sz.width, sz.height);
}




/********************************************************************************/
/*										*/
/*	Bubble outputer methods 						*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()
{
   return "BDDT";
}



@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","LAUNCHBUBBLE");
   xw.field("CONFIG",launch_config.getConfigName());
}



/********************************************************************************/
/*										*/
/*	Button handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();

   if (cmd.equals("DEBUG")) {
      if (edit_config != null) {
	 BumpLaunchConfig blc = edit_config.save();
	 if (blc != null) {
	    launch_config = blc;
	    edit_config = null;
	  }
	 reload();
       }
      BddtFactory bf = BddtFactory.getFactory();
      bf.newDebugger(launch_config);
    }
   else if (cmd.equals("SAVE")) {
      if (edit_config != null) {
	 BumpLaunchConfig blc = edit_config.save();
	 if (blc != null) {
	    launch_config = blc;
	    edit_config = null;
	  }
	 reload();
       }
    }
   else if (cmd.equals("REVERT")) {
      if (edit_config != null) {
	 edit_config = null;
	 reload();
       }
    }
   else if (cmd.equals("CLONE")) {
      BumpLaunchConfig blc = launch_config.clone(getNewName());
      if (edit_config != null) {
	 blc.setArguments(edit_config.getArguments());
	 blc.setVMArguments(edit_config.getVMArguments());
	 blc.setMainClass(edit_config.getMainClass());
       }
      BddtLaunchBubble bbl = new BddtLaunchBubble(blc);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      Rectangle loc = BudaRoot.findBudaLocation(this);
      bba.addBubble(bbl,loc.x + loc.width + 25,loc.y);
    }
   else if (cmd.equals("Start Class")) {
      if (edit_config == null) edit_config = launch_config;
      if (edit_config != null && start_class != null) {
	 edit_config = edit_config.setMainClass((String) start_class.getSelectedItem());
       }
    }
   else if (cmd.equals("Project")) {
      if (edit_config == null) edit_config = launch_config;
      // set project accordingly
      // update selection of start classes
      // set main class to default value
    }
   else System.err.println("ACTION: " + cmd);

   fixButtons();
}


@Override public void undoableEditHappened(UndoableEditEvent e)
{
   JTextComponent ted = (JTextComponent) e.getSource();
   if (ted == arg_area) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setArguments(ted.getText());
    }
   else if (ted == vmarg_area) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setVMArguments(ted.getText());	  }

   fixButtons();
}




}	// end of class BddtLaunchBubble




/* end of BddtLaunchBubble.java */
