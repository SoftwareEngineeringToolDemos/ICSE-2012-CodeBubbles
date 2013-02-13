/********************************************************************************/
/*										*/
/*		BattStatusBubble.java						*/
/*										*/
/*	Bubbles Automated Testing Tool status display bubble			*/
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



package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bale.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.bddt.*;

import edu.brown.cs.ivy.swing.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;



class BattStatusBubble extends BudaBubble implements BattConstants, ActionListener,
	BattConstants.BattModelListener, BudaConstants.BudaBubbleOutputer, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BattModeler	batt_model;
private SwingGridPanel	display_panel;
private DisplayTable	display_table;
private JPanel		display_bar;
private TestMode	current_mode;
private RunType 	current_runtype;

private Set<DisplayMode> ALL_MODE = EnumSet.of(DisplayMode.ALL);
private Set<DisplayMode> FAIL_MODE = EnumSet.of(DisplayMode.FAIL);
private Set<DisplayMode> PENDING_MODE = EnumSet.of(DisplayMode.PENDING,DisplayMode.NEEDED);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattStatusBubble(BattModeler bm)
{
   batt_model = bm;
   setupPanel();
   current_mode = null;
   current_runtype = RunType.ALL;

   setContentPane(display_panel);
   bm.addBattModelListener(this);

   BoardThreadPool.start(new SetupBatt());
}



@Override protected void localDispose()
{
   batt_model.removeBattModelListener(this);
}



/********************************************************************************/
/*										*/
/*	Display setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   display_panel = new SwingGridPanel();
   display_panel.beginLayout();
   display_bar = new DisplayBar();
   display_panel.addLabellessRawComponent("BAR",display_bar,true,false);
   display_panel.addSeparator();
   display_table = new DisplayTable();
   JScrollPane jsp = new JScrollPane(display_table);
   display_panel.addLabellessRawComponent("TABLE",jsp,true,true);
   display_panel.addSeparator();

   display_panel.addBottomButton("ALL","ALL",this);
   display_panel.addBottomButton("PENDING","PENDING",this);
   display_panel.addBottomButton("FAIL","FAIL",this);
   display_panel.addBottomButton("RUN","RUN",this);
   display_panel.addBottomButtons();
}



/********************************************************************************/
/*										*/
/*	Action handlers 							*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();
   if (cmd == null) return;
   if (cmd.equals("ALL")) {
      batt_model.setDisplayMode(ALL_MODE);
    }
   else if (cmd.equals("PENDING")) {
      batt_model.setDisplayMode(PENDING_MODE);
    }
   else if (cmd.equals("FAIL")) {
      batt_model.setDisplayMode(FAIL_MODE);
    }
   else if (cmd.equals("RUN")) {
      BumpClient bc = BumpClient.getBump();
      bc.saveAll();
      BattFactory.getFactory().runTests(current_runtype);
    }
   else if (cmd.equals("STOP")) {
      BattFactory.getFactory().stopTest();
    }
   else {
      BoardLog.logE("BATT","Unknown action " + cmd);
    }
}



@Override public void battModelUpdated(BattModeler bm)
{
   if (display_bar != null) {
      // display_bar.repaint();
      repaint();
    }
}




/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   Point pt = new Point(e.getXOnScreen(),e.getYOnScreen());
   SwingUtilities.convertPointFromScreen(pt,display_table);

   BattTestCase btc = display_table.findTestCase(pt);

   if (btc != null) {
      menu.add(new SourceAction(btc));
      menu.add(new DebugAction(btc));
    }

   if (current_mode == null) {
      current_mode = BattFactory.getFactory().getTestMode();
    }
   JMenu m2 = new JMenu("Test Running Mode");
   m2.add(new ModeAction(TestMode.ON_DEMAND));
   m2.add(new ModeAction(TestMode.CONTINUOUS));
   menu.add(m2);

   JMenu m1 = new JMenu("Run Option");
   m1.add(new RunAction(RunType.ALL));
   m1.add(new RunAction(RunType.FAIL));
   m1.add(new RunAction(RunType.PENDING));
   menu.add(m1);

   menu.add(new StopAction());
   menu.add(new UpdateAction());

   menu.add(getFloatBubbleAction());

   // TODO: provide rerun, debug, ... options

   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Launch configuration methods						*/
/*										*/
/********************************************************************************/

private BumpLaunchConfig getLaunchConfigurationForTest(BattTestCase btc)
{
   BumpClient bc = BumpClient.getBump();
   BumpRunModel brm = bc.getRunModel();

   for (BumpLaunchConfig blc : brm.getLaunchConfigurations()) {
      if (!blc.isWorkingCopy() && blc.getConfigType() == BumpLaunchConfigType.JUNIT_TEST) {
	 if (blc.getTestName() != null && blc.getTestName().equals(btc.getMethodName()) &&
		  btc.getClassName().equals(blc.getMainClass()))
	    return blc;
       }
    }

   String nm = btc.getName();
   int idx = nm.indexOf("(");
   if (idx >= 0) nm = nm.substring(0,idx);

   String pnm = null;
   String cnm = btc.getClassName();
   List<BumpLocation> locs = bc.findAllClasses(cnm);
   if (locs != null && locs.size() > 0) {
      BumpLocation loc = locs.get(0);
      pnm = loc.getProject();
    }

   BumpLaunchConfig blc = brm.createLaunchConfiguration(nm,BumpLaunchConfigType.JUNIT_TEST);
   if (blc == null) return null;

   BumpLaunchConfig blc1 = blc;
   if (pnm != null) blc1 = blc1.setProject(pnm);
   blc1 = blc1.setMainClass(btc.getClassName());
   blc1 = blc1.setTestName(btc.getMethodName());
   blc1 = blc1.setJunitKind("junit4");
   blc = blc1.save();

   return blc;
}



private class DebugAction extends AbstractAction {

   private BattTestCase test_case;

   DebugAction(BattTestCase tc) {
      super("Debug " + tc.getName());
      test_case = tc;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BumpLaunchConfig blc = getLaunchConfigurationForTest(test_case);
      if (blc == null) return;

      BddtFactory.getFactory().newDebugger(blc);
    }

}	// end of inner class DebugAction




/********************************************************************************/
/*										*/
/*	Outputer methods							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BATT"; }

@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","TESTSTATUS");
}



/********************************************************************************/
/*										*/
/*	Display bar class							*/
/*										*/
/********************************************************************************/

enum BarType {
   SUCCESS,
   NEED_SUCCESS,
   RUNNING,
   PENDING,
   CANT_RUN,
   STOPPED,
   NEED_FAILURE,
   FAILURE
}

private static final int NUM_BAR_TYPES = 8;
private static final int BAR_INSET = 3;

private static final Paint [] bar_colors = new Color [] {
   Color.GREEN,
   new Color(0,255,255),
   Color.YELLOW,
   Color.GRAY,
   Color.BLACK,
   new Color(255,192,64),
   new Color(255,0,255),
   Color.RED
};


private BarType getTestType(BattTestCase btc)
{
   switch (btc.getState()) {
      case STOPPED :
	 return BarType.STOPPED;
      case CANT_RUN :
	 return BarType.CANT_RUN;
      case EDITED :
      case NEEDS_CHECK :
	 switch (btc.getStatus()) {
	    case SUCCESS :
	       return BarType.NEED_SUCCESS;
	    case FAILURE :
	       return BarType.NEED_FAILURE;
	    default :
	       return BarType.PENDING;
	  }
      default :
      case PENDING :
	 return BarType.PENDING;
      case RUNNING :
	 return BarType.RUNNING;
      case UP_TO_DATE :
	 switch (btc.getStatus()) {
	    case SUCCESS :
	       return BarType.SUCCESS;
	    case FAILURE :
	       return BarType.FAILURE;
	    default :
	       return BarType.PENDING;
	  }
    }
}



private class DisplayBar extends JPanel {

   DisplayBar() {
      setPreferredSize(new Dimension(300,30));
      setMinimumSize(new Dimension(100,16));
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      List<BattTestCase> tests = batt_model.getAllTests();

      int counts[] = new int[NUM_BAR_TYPES];
      Arrays.fill(counts,0);
      double total = 0;

      for (BattTestCase btc :tests) {
	 BarType bt = getTestType(btc);
	 counts[bt.ordinal()]++;
	 total++;
       }

      Dimension d = getSize();
      double y0 = BAR_INSET;
      double y1 = d.height - BAR_INSET;
      double x0 = BAR_INSET;
      double x1 = d.width - BAR_INSET;
      double x = x0;
      Rectangle2D r2 = new Rectangle2D.Double(x0,y0,x1-x0,y1-y0);
      g2.setColor(Color.WHITE);
      g2.fill(r2);
      for (int i = 0; i < NUM_BAR_TYPES; ++i) {
	 if (counts[i] == 0) continue;
	 double w = (x1-x0)*counts[i]/total;
	 g2.setPaint(bar_colors[i]);
	 r2.setRect(x,y0,w,y1-y0);
	 g2.fill(r2);
	 x += w;
       }
    }

}	// end of inner class DisplayBar





/********************************************************************************/
/*										*/
/*	Table for displaying tests						*/
/*										*/
/********************************************************************************/

private class DisplayTable extends JTable implements MouseListener {

   DisplayTable() {
      super(batt_model.getTableModel());
      setShowGrid(true);
      setPreferredScrollableViewportSize(new Dimension(350,100));
      setAutoCreateRowSorter(true);
      setColumnSelectionAllowed(false);
      setDragEnabled(false);
      setFillsViewportHeight(true);

      getColumnModel().getColumn(0).setMinWidth(BATT_STATUS_COL_MIN_WIDTH);
      getColumnModel().getColumn(0).setMaxWidth(BATT_STATUS_COL_MAX_WIDTH);

      getColumnModel().getColumn(1).setMinWidth(BATT_STATE_COL_MIN_WIDTH);
      getColumnModel().getColumn(1).setMaxWidth(BATT_STATE_COL_MAX_WIDTH);

      getColumnModel().getColumn(2).setPreferredWidth(BATT_CLASS_COL_PREF_WIDTH);
      getColumnModel().getColumn(3).setPreferredWidth(BATT_NAME_COL_PREF_WIDTH);

      setCellSelectionEnabled(false);
      setRowSelectionAllowed(true);
      setColumnSelectionAllowed(false);
      addMouseListener(this);
    }

   @Override public boolean getScrollableTracksViewportWidth()		{ return true; }

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() < 2) return;
      BattTestCase btc = findTestCase(e.getPoint());
      if (btc == null) return;
      // show bubble for that test case
      SourceAction sa = new SourceAction(btc);
      sa.actionPerformed(null);
    }

   @Override public void mouseEntered(MouseEvent _e)			{ }
   @Override public void mouseExited(MouseEvent _e)			{ }
   @Override public void mouseReleased(MouseEvent e)			{ }
   @Override public void mousePressed(MouseEvent e)			{ }

   @Override public boolean isCellEditable(int r,int c) 		{ return false; }

   @Override public String getToolTipText(MouseEvent e) {
      BattTestCase btc = findTestCase(e.getPoint());
      if (btc == null) return null;
      return btc.getToolTip();
    }

   BattTestCase getActualTestCase(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      return batt_model.getTestCase(row);
    }

   BattTestCase findTestCase(Point p) {
      int row = rowAtPoint(p);
      if (row < 0) return null;
      return getActualTestCase(row);
    }

}	// end of inner class DisplayTable



/********************************************************************************/
/*										*/
/*	Runner to set up batt							*/
/*										*/
/********************************************************************************/

private static class SetupBatt implements Runnable {

   @Override public void run() {
      BattFactory.getFactory().startBattServer();
      BattFactory.getFactory().updateTests();
    }

}	// end of inner class SetupBatt



/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

private class SourceAction extends AbstractAction {

   private BattTestCase test_case;

   SourceAction(BattTestCase btc) {
      super("Goto " + btc.getMethodName());
      test_case = btc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String nm = test_case.getClassName() + "." + test_case.getMethodName();
      BaleFactory bf = BaleFactory.getFactory();
      BudaBubble bb = bf.createMethodBubble(null,nm);
      if (bb == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BattStatusBubble.this);
      if (bba != null) {
	 bba.addBubble(bb,BattStatusBubble.this,null,PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class SourceAction


private class ModeAction extends JRadioButtonMenuItem implements ActionListener {

   private TestMode test_mode;

   ModeAction(TestMode md) {
      super(md.toString(),(md == current_mode));
      addActionListener(this);
      test_mode = md;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().setTestMode(test_mode);
      current_mode = test_mode;
    }

}	// end of inner class ModeAction



private class UpdateAction extends JRadioButtonMenuItem implements ActionListener {


   UpdateAction() {
      super("Update Test Set");
      addActionListener(this);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().findNewTests();
    }

}	// end of inner class ModeAction



private class RunAction extends JRadioButtonMenuItem implements ActionListener {

   private RunType run_type;

   RunAction(RunType typ) {
      super(typ.toString(),(typ == current_runtype));
      addActionListener(this);
      run_type = typ;
    }

   @Override public void actionPerformed(ActionEvent e) {
      current_runtype = run_type;
    }

}	// end of inner class RunAction




private static class StopAction extends AbstractAction {

   StopAction() {
      super("Stop current test");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BattFactory.getFactory().stopTest();
    }

}	// end of inner class RunAction




}	// end of class BattStatusBubble




/* end of BattStatusBubble.java */

