/********************************************************************************/ /*										   */
/*		BddtFactory.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool factory and setup class	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;


/**
 *	This class provides the entries for setting up and providing access to
 *	the various debugging bubbles and environment.
 **/

public class BddtFactory implements BddtConstants, BudaConstants.ButtonListener,
					BumpConstants, BaleConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaChannelSet		debug_channels;
private BumpLaunchConfig	current_configuration;
private JLabel			launch_label;

private static BddtConsoleController console_controller;
private static BddtHistoryController history_controller;
private static BddtFactory	the_factory;
private static BoardProperties	bddt_properties;



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is called automatically at startup to initialize the module.
 **/

public static void setup()
{
   the_factory = new BddtFactory();
   console_controller = new BddtConsoleController();
   history_controller = new BddtHistoryController();

   bddt_properties = BoardProperties.getProperties("Bddt");

   BudaRoot.addBubbleConfigurator("BDDT",new BddtConfigurator());

   BudaRoot.registerMenuButton(BDDT_BREAKPOINT_BUTTON, the_factory);
   BudaRoot.registerMenuButton(BDDT_CONFIG_BUTTON,the_factory);
   BudaRoot.registerMenuButton(BDDT_PROCESS_BUTTON,the_factory);

   BudaRoot.addToolbarButton(BDDT_TOOLBAR_MENU_BUTTON, the_factory.new SaveButton(),
	 "Save all", BoardImage.getImage("save"));

   BudaRoot.addToolbarButton(BDDT_TOOLBAR_MENU_BUTTON, the_factory.new BuildButton(),
				"Build all", BoardImage.getImage("build"));
   BudaRoot.addToolbarButton(BDDT_TOOLBAR_MENU_BUTTON, the_factory.new RefreshButton(),
				"Refresh", BoardImage.getImage("refresh"));

   BddtRepository rep = new BddtRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_LAUNCH_CONFIG,rep);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,rep);
}


/**
 *	Return the singleton instance of the ddttext viewer factory.
 **/

public static BddtFactory getFactory()
{
   return the_factory;
}



public static void initialize(BudaRoot br)
{
   if (the_factory.current_configuration == null) {
      the_factory.setCurrentLaunchConfig(null);
    }

   the_factory.setupDebugging(br);
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BddtFactory()
{
   debug_channels = null;
   // status_label = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BddtConsoleController getConsoleControl()		{ return console_controller; }
BddtHistoryController getHistoryControl()		{ return history_controller; }



/********************************************************************************/
/*										*/
/*	Methods to setup up a debugging process 				*/
/*										*/
/********************************************************************************/

public void newDebugger(BumpLaunchConfig blc)
{
   BudaBubbleArea bba = null;

   String label = blc.getProject() + " : " + blc.getConfigName();

   if (debug_channels.getNumChannels() == 1 && debug_channels.isChannelEmpty()) {
      bba = debug_channels.getBubbleArea();
      debug_channels.setChannelName(label);
    }
   else bba = debug_channels.addChannel(label);

   setCurrentLaunchConfig(blc);

   BddtLaunchControl ctrl = new BddtLaunchControl(blc);
   console_controller.setupConsole(ctrl);

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_LAUNCH_CONTROL)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,BDDT_LAUNCH_CONTROL_X,BDDT_LAUNCH_CONTROL_Y);
    }
   else {
      bc = new BudaConstraint(BDDT_LAUNCH_CONTROL_X,BDDT_LAUNCH_CONTROL_Y);
    }
   bba.add(ctrl,bc);

   BudaRoot br = BudaRoot.findBudaRoot(bba);
   br.setCurrentChannel(ctrl);

   ctrl.setupKeys();
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

BumpLaunchConfig getCurrentLaunchConfig()
{
   return current_configuration;
}


void setCurrentLaunchConfig(BumpLaunchConfig blc)
{
   if (blc == null) {
      BumpRunModel brm = BumpClient.getBump().getRunModel();
      for (BumpLaunchConfig xlc : brm.getLaunchConfigurations()) {
	 if (!xlc.isWorkingCopy()) blc = xlc;
	 break;
       }
    }

   current_configuration = blc;
   if (launch_label != null && blc != null) {
      launch_label.setText(blc.getConfigName());
   }
}




private void setupDebugging(BudaRoot br)
{
   if (debug_channels != null) return;

   debug_channels = new BudaChannelSet(br,BDDT_CHANNEL_TOP_COLOR,BDDT_CHANNEL_BOTTOM_COLOR);

   SwingGridPanel pnl = new DebuggingPanel();

   JLabel top = new JLabel("Debug",JLabel.CENTER);
   pnl.addGBComponent(top,0,0,0,1,1,0);

   JButton btn = defineButton("debug","Switch to the debugging perspective");
   pnl.addGBComponent(btn,1,1,1,1,0,0);

   btn = defineButton("new","<html>Create a new debugging channel for current configuration" +
	 " or switch configurations (right click)");
   pnl.addGBComponent(btn,2,1,1,1,0,0);
   btn.addMouseListener(new ConfigSelector());

   launch_label = new JLabel();
   if (current_configuration != null) {
      launch_label.setText(current_configuration.getConfigName());
    }

   pnl.addGBComponent(launch_label,0,3,0,0,1,1);

   br.addPanel(pnl);
}



/********************************************************************************/
/*										*/
/*	Debugging panel 							*/
/*										*/
/********************************************************************************/

private class DebuggingPanel extends SwingGridPanel
{
   private static final long serialVersionUID = 1;

   DebuggingPanel() {
      super();
    }

   protected void paintComponent(Graphics g0) {
      super.paintComponent(g0);
      Graphics2D g = (Graphics2D) g0.create();
      if (BDDT_PANEL_TOP_COLOR.equals(BDDT_PANEL_BOTTOM_COLOR)) {
	 g.setColor(BDDT_PANEL_TOP_COLOR);
       }
      else {
	 Paint p = new GradientPaint(0f, 0f, BDDT_PANEL_TOP_COLOR, 0f, this.getHeight(),
					BDDT_PANEL_BOTTOM_COLOR);
	 g.setPaint(p);
       }
      g.fillRect(0, 0, this.getWidth() , this.getHeight());
    }

}	// end of inner class DebuggingPanel



private JButton defineButton(String name,String info)
{
   JButton btn = new JButton(BoardImage.getIcon("debug/" + name + ".png"));
   btn.setToolTipText(info);
   btn.setActionCommand(name.toUpperCase());
   btn.setMargin(new Insets(0,1,0,1));
   btn.setOpaque(false);
   btn.setBackground(new Color(0,true));
   btn.addActionListener(new PanelHandler());

   return btn;
}



private class PanelHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      JButton btn = (JButton) e.getSource();
      BudaRoot br = BudaRoot.findBudaRoot(btn);
      String cmd = e.getActionCommand();
      if (cmd.equals("DEBUG")) {
	 BoardMetrics.noteCommand("BDDT","GotoDebug");
	 if (br.getChannelSet() == debug_channels) br.setChannelSet(null);
	 else br.setChannelSet(debug_channels);
       }
      else if (cmd.equals("NEW")) {
	 if (current_configuration == null) setCurrentLaunchConfig(null);

	 if (current_configuration != null) {
	    BoardMetrics.noteCommand("BDDT","NewDebug");
	    newDebugger(current_configuration);
	  }
	 else {
	    // TOOD: Put up error message at this point
	  }
       }
    }

}	// end of inner class PanelHandler



/********************************************************************************/
/*										*/
/*	Bubble making methods							*/
/*										*/
/********************************************************************************/

BudaBubble makeThreadBubble(BudaBubble pview,BddtLaunchControl ctrl)
{
   BudaRoot br = BudaRoot.findBudaRoot(pview);
   BudaBubble bb = null;
   bb = new BddtThreadView(ctrl);
   Rectangle r = pview.getBounds();
   int x = r.x;
   int y = r.y + r.height + BDDT_CONSOLE_HEIGHT + 20 + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_THREADS)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   br.add(bb,bc);
   BudaDefaultPort from = new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true);
   BudaDefaultPort to = new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true);
   BudaBubbleLink lk = new BudaBubbleLink(pview, from, bb, to);
   br.addLink(lk);

   return bb;
}



BudaBubble makeConsoleBubble(BudaBubble src,BddtLaunchControl ctrl)
{
   if (ctrl == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   BudaBubble bb = null;
   bb = console_controller.createConsole(ctrl);
   Rectangle r = src.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_CONSOLE)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   bba.add(bb,bc);

   return bb;
}




BudaBubble makeConsoleBubble(BudaBubble src,BumpProcess proc)
{
   if (proc == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   BudaBubble bb = null;
   bb = console_controller.createConsole(proc);
   Rectangle r = src.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_CONSOLE)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   bba.add(bb,bc);

   return bb;
}




BudaBubble makeHistoryBubble(BudaBubble src,BddtLaunchControl ctrl)
{
   if (ctrl == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   BudaBubble bb = null;
   bb = history_controller.createHistory(ctrl);
   if (bb == null) return null;

   Rectangle r = src.getBounds();
   int x = r.x;
   int y = r.y + r.height + BDDT_CONSOLE_HEIGHT + 20 + BDDT_STACK_HEIGHT + 20 + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_HISTORY)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   bba.add(bb,bc);

   return bb;
}



BudaBubble makePerformanceBubble(BudaBubble src,BddtLaunchControl ctrl)
{
   if (ctrl == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   BudaBubble bb = null;
   // bb = new BddtPerfView(ctrl);
   bb = new BddtPerfViewTable(ctrl);

   Rectangle r = src.getBounds();
   int x = r.x + BDDT_HISTORY_WIDTH + 20;
   int y = r.y + r.height + BDDT_CONSOLE_HEIGHT + 20 + BDDT_STACK_HEIGHT + 20 + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_HISTORY)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   bba.add(bb,bc);

   return bb;
}




/********************************************************************************/
/*										*/
/*	Button handling 							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;

   if (id.equals(BDDT_CONFIG_BUTTON)) {
      bb = new BddtConfigView();
    }
   else if (id.equals(BDDT_PROCESS_BUTTON)) {
      bb = new BddtProcessView();
   }
   else if (id.equals(BDDT_BREAKPOINT_BUTTON)) {
      bb = new BddtBreakpointBubble();
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb,bc);
      bb.grabFocus();
    }
}



/********************************************************************************/
/*										*/
/*	Button action routines for save/compile/etc				*/
/*										*/
/********************************************************************************/

private class SaveButton implements ActionListener, Runnable
{
   private BudaRoot buda_root;


   @Override public void actionPerformed(ActionEvent e)  {
      buda_root = BudaRoot.findBudaRoot((Component) e.getSource());
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BoardMetrics.noteCommand("BDDT","SaveAll");
      BowiFactory.startTask(BowiTaskType.SAVE);
      BumpClient bc = BumpClient.getBump();
      bc.saveAll();
      buda_root.handleSaveAllRequest();
      BowiFactory.stopTask(BowiTaskType.SAVE);
   }

}	// end of inner class SaveButton




private class BuildButton implements ActionListener, Runnable
{
   @Override public void actionPerformed(ActionEvent e)  {
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BoardMetrics.noteCommand("BDDT","Build");
      BowiFactory.startTask(BowiTaskType.BUILD);
      BumpClient bc = BumpClient.getBump();
      bc.compile(true, false, false);
      BowiFactory.stopTask(BowiTaskType.BUILD);
   }

}	// end of inner class BuildButton



private class RefreshButton implements ActionListener, Runnable
{
   @Override public void actionPerformed(ActionEvent e)  {
      BoardThreadPool.start(this);
      // BumpClient bc = BumpClient.getBump();
      // bc.compile(false, false, true);
   }

   @Override public void run() {
      BoardMetrics.noteCommand("BDDT","Refresh");
      BowiFactory.startTask(BowiTaskType.REFRESH);
      BumpClient bc = BumpClient.getBump();
      bc.compile(false, false, true);
      BowiFactory.stopTask(BowiTaskType.REFRESH);
    }

}	// end of inner class RefreshButton



/********************************************************************************/
/*										*/
/*	Button actions for selecting configurator				*/
/*										*/
/********************************************************************************/

void addNewConfigurationActions(JPopupMenu menu)
{
   menu.add(new CreateConfigAction(BumpLaunchConfigType.JAVA_APP));
   menu.add(new CreateConfigAction(BumpLaunchConfigType.REMOTE_JAVA));
}



private class ConfigSelector extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON3) {
	 JPopupMenu menu = new JPopupMenu();
	 BumpClient bc = BumpClient.getBump();
	 BumpRunModel bm = bc.getRunModel();
	 Collection<BumpLaunchConfig> blcs = new TreeSet<BumpLaunchConfig>(new ConfigComparator());
	 for (BumpLaunchConfig blc : bm.getLaunchConfigurations()) {
	    if (!blc.isWorkingCopy()) blcs.add(blc);
	  }
	 for (BumpLaunchConfig blc : blcs) {
	    menu.add(new ConfigAction(blc));
	  }
	 addNewConfigurationActions(menu);
	 menu.show((Component) e.getSource(),e.getX(),e.getY());
       }
    }

}	// end of inner class ConfigSelector




private class ConfigAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   ConfigAction(BumpLaunchConfig blc) {
      super(blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      setCurrentLaunchConfig(for_config);
      BoardMetrics.noteCommand("BDDT","GoToDebug");
      newDebugger(for_config);
    }

}	// end of inner class ConfigAction




private static class CreateConfigAction extends AbstractAction {

   private BumpLaunchConfigType config_type;

   CreateConfigAction(BumpLaunchConfigType typ) {
      super("Create New " + typ.getEclipseName() + " Configuration");
      config_type = typ;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient bc = BumpClient.getBump();
      BumpRunModel brm = bc.getRunModel();
      BumpLaunchConfig blc = brm.createLaunchConfiguration(null,config_type);
      if (blc != null) blc.save();
    }

}	// end of inner class CreateConfigAction



private static class ConfigComparator implements Comparator<BumpLaunchConfig> {

   @Override public int compare(BumpLaunchConfig l1,BumpLaunchConfig l2) {
      return l1.getConfigName().compareTo(l2.getConfigName());
    }

}	// end of inner class ConfigComparator





}	// end of class BddtFactory



/* end of BddtFactory.java */
