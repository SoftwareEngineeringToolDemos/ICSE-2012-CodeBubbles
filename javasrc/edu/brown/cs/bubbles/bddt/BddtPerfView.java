/********************************************************************************/
/*										*/
/*		BddtPerfView.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool performance bubble		*/
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

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingTreeTable;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



class BddtPerfView extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	launch_control;
private PerfTable		perf_table;
private PerfModel		perf_model;
private PerfEventHandler	event_handler;
private Expander		tree_expander;
private double			base_samples;
private double			total_samples;
private double			base_time;

private static final String [] column_names = new String[] {
   "Location", "Base Time", "Base %", "Total Time", "Total %" };

private static final Class<?> [] column_class = new Class<?>[] {
   String.class, Double.class, Double.class, Double.class, Double.class };





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtPerfView(BddtLaunchControl ctrl)
{
   launch_control = ctrl;
   base_samples = 1;
   total_samples = 1;
   base_time = 0;

   setupBubble();
}



@Override protected void localDispose()
{
   if (event_handler != null) {
      BumpClient bc = BumpClient.getBump();
      BumpRunModel rm = bc.getRunModel();
      rm.removeRunEventHandler(event_handler);
      event_handler = null;
    }
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupBubble()
{
   BumpClient bc = BumpClient.getBump();
   BumpRunModel rm = bc.getRunModel();
   event_handler = new PerfEventHandler();
   rm.addRunEventHandler(event_handler);

   perf_model = new PerfModel();
   perf_table = new PerfTable(perf_model);

   perf_table.addMouseListener(new ClickHandler());

   tree_expander = new Expander(perf_table.getTree());
   perf_table.addTreeExpansionListener(tree_expander);
   perf_model.addTreeModelListener(tree_expander);

   JScrollPane sp = new JScrollPane(perf_table);
   sp.setPreferredSize(new Dimension(BDDT_PERF_WIDTH,BDDT_PERF_HEIGHT));

   JPanel pnl = new JPanel(new BorderLayout());
   pnl.add(sp,BorderLayout.CENTER);

   setContentPane(pnl,null);
   perf_table.addMouseListener(new FocusOnEntry());
}




/********************************************************************************/
/*										*/
/*	Popup menu and mouse handlers						*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   // option of goto source
   // reset option
}



private class ClickHandler extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
	 // handle goto source
       }
    }

}	// end of inner class ClickHandler




/********************************************************************************/
/*										*/
/*	Run Event Handler							*/
/*										*/
/********************************************************************************/

private class PerfEventHandler implements BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)		{ }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      if (evt.getProcess() != launch_control.getProcess()) return;
      switch (evt.getEventType()) {
	 case PROCESS_PERFORMANCE :
	    Element xml = (Element) evt.getEventData();
	    base_samples = IvyXml.getAttrDouble(xml,"ACTIVE");
	    total_samples = IvyXml.getAttrDouble(xml,"SAMPLES");
	    base_time = IvyXml.getAttrDouble(xml,"TIME");
	    System.err.println("PERF: " + base_samples + " " + total_samples + " " + base_time);
	    for (Element itm : IvyXml.children(xml,"ITEM")) {
	       PerfNode pn = findNode(IvyXml.getAttrString(itm,"NAME"));
	       pn.update(itm);
	    }
	    break;
       }
    }

   @Override public void handleThreadEvent(BumpRunEvent evt)		{ }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,String msg)	{ }

}	// end of inner class PerfEventHandler




/********************************************************************************/
/*										*/
/*	Table implementation							*/
/*										*/
/********************************************************************************/

private class PerfTable extends SwingTreeTable implements BudaConstants.BudaBubbleOutputer {

   private CellDrawer [] cell_drawer;

   PerfTable(TreeTableModel mdl) {
      super(mdl);
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      for (Enumeration<TableColumn> e = getColumnModel().getColumns(); e.hasMoreElements(); ) {
	 TableColumn tc = e.nextElement();
	 tc.setHeaderRenderer(new HeaderDrawer(getTableHeader().getDefaultRenderer()));
       }
      cell_drawer = new CellDrawer[getColumnModel().getColumnCount()];
      JTree tr = getTree();
      tr.setCellRenderer(new TreeCellRenderer());
    }

   @Override public TableCellRenderer getCellRenderer(int r,int c) {
      if (cell_drawer[c] == null) {
	 cell_drawer[c] = new CellDrawer(super.getCellRenderer(r,c));
       }
      return cell_drawer[c];
    }

   @Override protected void paintComponent(Graphics g) {
      Dimension sz = getSize();
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      Graphics2D g2 = (Graphics2D) g.create();
      Color top = BDDT_PERF_TOP_COLOR;
      Color bot = BDDT_PERF_BOTTOM_COLOR;
      if (top.getRGB() != bot.getRGB()) {
	 Paint p = new GradientPaint(0f,0f,top,0f,sz.height,bot);
	 g2.setPaint(p);
       }
      else {
	 g2.setColor(top);
       }
      g2.fill(r);
      perf_model.lock();
      try {
	 super.paintComponent(g);
       }
      finally { perf_model.unlock(); }
    }

   @Override public String getConfigurator()		{ return "BDDT"; }

   @Override public void outputXml(BudaXmlWriter xw)	{ }

}	// end of inner class PerfTable




/********************************************************************************/
/*										*/
/*	Renderers								*/
/*										*/
/********************************************************************************/

private static class HeaderDrawer implements TableCellRenderer {

   private TableCellRenderer default_renderer;
   private Font bold_font;

   HeaderDrawer(TableCellRenderer dflt) {
      default_renderer = dflt;
      bold_font = null;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      if (bold_font == null) {
	 bold_font = cmp.getFont();
	 bold_font = bold_font.deriveFont(Font.BOLD);
       }
      cmp.setFont(bold_font);
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of inner class HeaderRenderer




private static class CellDrawer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   CellDrawer(TableCellRenderer dflt) {
      default_renderer = dflt;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setOpaque(false);
      return cmp;
    }

}	// end of innerclass ErrorRenderer



private static class TreeCellRenderer extends DefaultTreeCellRenderer {

   private static final long serialVersionUID = 1;

   TreeCellRenderer() {
      setBackgroundNonSelectionColor(null);
      // setBackgroundSelectionColor(null);
      setBackground(new Color(0,0,0,0));
    }

   @Override public Component getTreeCellRendererComponent(JTree tree,
							      Object value,
							      boolean sel,
							      boolean expanded,
							      boolean leaf,
							      int row,
							      boolean hasfocus) {
      return super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasfocus);
   }

}	// end of inner class TreeCellRenderer




/********************************************************************************/
/*										*/
/*	Tree Expansion maintenance						*/
/*										*/
/********************************************************************************/

private static class Expander implements TreeExpansionListener, TreeModelListener {

   private Set<String> expand_set;
   private JTree       for_tree;

   Expander(JTree tr) {
      expand_set = new HashSet<String>();
      for_tree = tr;
    }

   @Override public void treeCollapsed(TreeExpansionEvent evt) {
      PerfNode tn = (PerfNode) evt.getPath().getLastPathComponent();
      expand_set.remove(tn.getFullName());
    }

   @Override public void treeExpanded(TreeExpansionEvent evt) {
      PerfNode tn = (PerfNode) evt.getPath().getLastPathComponent();
      expand_set.add(tn.getFullName());
    }

   @Override public void treeNodesChanged(TreeModelEvent e)	{ }
   @Override public void treeNodesInserted(TreeModelEvent e)	{ }
   @Override public void treeNodesRemoved(TreeModelEvent e)	{ }
   @Override public void treeStructureChanged(TreeModelEvent e) {
      SwingUtilities.invokeLater(new ExpandNodes(this));
    }

   private void checkNodes() {
      checkNode(null,(PerfNode) for_tree.getModel().getRoot());
    }

   private void checkNode(TreePath tp,PerfNode vn) {
      boolean exp = false;

      if (tp == null) exp = true;
      else if (vn.getParent() == null) exp = true;
      else if (expand_set.contains(vn.getFullName())) exp = true;

      if (exp) {
	 if (tp == null) tp = new TreePath(vn);
	 else tp = tp.pathByAddingChild(vn);
	 for_tree.expandPath(tp);
	 for (int i = 0; i < vn.getChildCount(); ++i) {
	    checkNode(tp,(PerfNode) vn.getChild(i));
	  }
       }
    }

}	// end of inner class Expander



private static class ExpandNodes implements Runnable {

   private Expander for_expander;

   ExpandNodes(Expander ex) {
      for_expander = ex;
    }

   @Override public void run() {
      for_expander.checkNodes();
    }

}	// end of inner class ExpandNodes



/********************************************************************************/
/*										*/
/*	Tree model								*/
/*										*/
/********************************************************************************/

private class PerfModel extends SwingTreeTable.AbstractTreeTableModel {

   private Lock 	model_lock;


   PerfModel() {
      super(new PerfNode(null,"<ROOT>")); // specify root
      model_lock = new ReentrantLock();
    }

   void lock()					{ model_lock.lock(); }
   void unlock()				{ model_lock.unlock(); }

   @Override public Object getChild(Object par,int idx) {
      PerfNode pn = (PerfNode) par;
      return pn.getChild(idx);
    }

   @Override public int getChildCount(Object par) {
      PerfNode pn = (PerfNode) par;
      return pn.getChildCount();
    }

   @Override public int getColumnCount() {
      return column_names.length;
   }

   @Override public String getColumnName(int col) {
      return column_names[col];
    }

   @Override public Class<?> getColumnClass(int col) {
      if (col == 0) return SwingTreeTable.TreeTableModel.class;
      return column_class[col];
    }

   @Override public Object getValueAt(Object node,int col) {
      PerfNode pn = (PerfNode) node;
      switch (col) {
	 case 0 :			// Location
	    return pn.toString();
	 case 1 :
	    return pn.getBaseCount()* base_time/base_samples;
	 case 2 :
	    return pn.getBaseCount() / base_samples * 100;
	 case 3 :
	    return pn.getTotalCount() * base_time/base_samples;
	 case 4 :
	    return pn.getTotalCount() / total_samples * 100;
      }
      return null;
    }

   void handleInsert(PerfNode cn) {
      PerfNode pn = cn.getParent();
      Object [] path = pn.getPath();
      int idx = getIndexOfChild(pn,cn);
      int [] idxs = new int[] { idx };
      Object [] chld = new Object[] { pn };
      fireTreeNodesInserted(this,path,idxs,chld);
    }

   void handleChange(PerfNode cn) {
      PerfNode pn = cn.getParent();
      Object [] path = pn.getPath();
      int idx = getIndexOfChild(pn,cn);
      int [] idxs = new int[] { idx };
      Object [] chld = new Object[] { pn };
      fireTreeNodesChanged(this,path,idxs,chld);
    }

}	// end of inner class PerfModel




/********************************************************************************/
/*										*/
/*	Performance nodes							*/
/*										*/
/********************************************************************************/

private PerfNode findNode(String nm)
{
   StringTokenizer tok = new StringTokenizer(nm,".@");
   PerfNode pn = (PerfNode) perf_model.getRoot();
   while (tok.hasMoreTokens()) {
      String tk = tok.nextToken();
      pn = pn.findChild(tk);
   }
   return pn;
}




private class PerfNode implements Comparable<PerfNode> {

   private PerfNode parent_node;
   private Set<PerfNode> child_nodes;
   private PerfNode [] child_array;
   private String node_name;
   private int base_count;
   private int total_count;

   PerfNode(PerfNode par,String nm) {
      parent_node = par;
      node_name = nm;
      child_nodes = null;
      child_array = null;
      base_count = 0;
      total_count = 0;
   }

   void update(Element xml) {
      int bc = IvyXml.getAttrInt(xml,"BASE",0);
      int tc = IvyXml.getAttrInt(xml,"TOTAL",0);
      if (parent_node != null) {
	 parent_node.update(bc-base_count,tc-total_count);
      }
      base_count = bc;
      total_count = tc;
      perf_model.handleChange(this);
   }

   void update(int bd,int td) {
      base_count += bd;
      total_count += td;
      if (parent_node != null) parent_node.update(bd,td);
   }

   @Override public String toString() {
      return node_name;
   }

   @Override public int compareTo(PerfNode pn) {
      return node_name.compareTo(pn.node_name);
   }

   int getBaseCount()			{ return base_count; }
   int getTotalCount()			{ return total_count; }
   String getName()			{ return node_name; }
   PerfNode getParent() 		{ return parent_node; }

   void addChild(PerfNode pn) {
      if (child_nodes == null) {
	 child_nodes = new TreeSet<PerfNode>();
      }
      child_nodes.add(pn);
      child_array = null;
      perf_model.handleInsert(pn);
    }

   int getChildCount() {
      if (child_nodes == null) return 0;
      return child_nodes.size();
   }

   PerfNode getChild(int idx) {
      if (child_nodes == null) return null;
      if (child_array == null) {
	 child_array = new PerfNode[child_nodes.size()];
	 child_array = child_nodes.toArray(child_array);
      }
      if (idx < 0 || idx >= child_array.length) return null;
      return child_array[idx];
   }

   PerfNode findChild(String nm) {
      if (child_nodes != null) {
	 for (PerfNode pn : child_nodes) {
	    if (pn.getName().equals(nm)) return pn;
	 }
      }
      PerfNode pn = new PerfNode(this,nm);
      addChild(pn);
      return pn;
    }

   Object [] getPath() {
      List<PerfNode> pars = new LinkedList<PerfNode>();
      for (PerfNode xn = this; xn != null; xn = xn.parent_node) {
	 pars.add(0,xn);
       }
      Object [] path = new Object[pars.size()];
      return pars.toArray(path);
    }

   String getFullName() {
      String name = "";
      if (parent_node != null) name = parent_node.getFullName() + ".";
      return name + node_name;
   }

}	// end of inner class PerfNode



}	// end of class BddtPerfView




/* end of BddtPerfView.java */















