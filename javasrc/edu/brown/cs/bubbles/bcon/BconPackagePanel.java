/********************************************************************************/
/*										*/
/*		BconPackagePanel.java						*/
/*										*/
/*	Bubbles Environment Context Viewer package information viewer		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.banal.BanalConstants;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;

import java.awt.Dimension;
import java.awt.event.*;



class BconPackagePanel implements BconConstants, BconConstants.BconPanel, BanalConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		for_project;
private String		for_package;
private SwingGridPanel	package_panel;
private BconPackageDisplay graph_panel;
private BconPackageGraph package_graph;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconPackagePanel(String proj,String pkg)
{
   for_project = proj;
   for_package = pkg;

   package_graph = new BconPackageGraph(proj,pkg);

   Dimension sz = new Dimension(300,200);
   graph_panel = new BconPackageDisplay(package_graph);
   graph_panel.setSize(sz);
   graph_panel.setPreferredSize(sz);

   graph_panel.addComponentListener(new Sizer());

   PackagePanel pnl = new PackagePanel();
   JLabel ttl = new JLabel(for_package);
   pnl.addGBComponent(ttl,0,0,2,1,10,0);

   pnl.addGBComponent(graph_panel,0,1,1,1,10,10);
   JTextField tfld = new JTextField();
   pnl.addGBComponent(tfld,0,2,1,1,10,0);
   JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM,JTabbedPane.SCROLL_TAB_LAYOUT);
   pnl.addGBComponent(tabs,1,1,1,1,0,1);
   tabs.add("Nodes",new NodeTab());
   tabs.add("Edges",new EdgeTab());
   tabs.add("Layout",new LayoutTab());

   package_panel = pnl;
}




@Override public void dispose() 			{ }




/********************************************************************************/
/*										*/
/*	Methods to build the graph model using current settings 		*/
/*										*/
/********************************************************************************/

private void resetGraph()
{
   graph_panel.updateGraph();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public JComponent getComponent()		{ return package_panel; }




/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   graph_panel.handlePopupMenu(e);
}




/********************************************************************************/
/*										*/
/*	Sizing methods								*/
/*										*/
/********************************************************************************/

private class Sizer extends ComponentAdapter {

   @Override public void componentResized(ComponentEvent e) {
      resetGraph();
    }

}	// end of inner class Sizer



/********************************************************************************/
/*										*/
/*	Tab for node options							*/
/*										*/
/********************************************************************************/

private class NodeTab extends JPanel implements ActionListener {

   NodeTab() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      addClassButton("Public",ClassType.PUBLIC);
      addClassButton("Proteced",ClassType.PROTECTED);
      addClassButton("Package",ClassType.PACKAGE_PROTECTED);
      addClassButton("Private",ClassType.PRIVATE);
      addClassButton("Inner Classes",ClassType.INNER);
      addClassButton("Classes",ClassType.CLASS);
      addClassButton("Interfaces",ClassType.INTERFACE);
      addClassButton("Static",ClassType.STATIC);
      addClassButton("Abstract",ClassType.ABSTRACT);
      addClassButton("Enumerations",ClassType.ENUM);
      addClassButton("Exceptions",ClassType.THROWABLE);
      addClassButton("Methods",ClassType.METHOD);
      // add(new JSeparator()); -- make it fixed size
      JCheckBox cbx = new JCheckBox("Show Labels",graph_panel.getShowLabels());
      cbx.setToolTipText("Show node labels");
      cbx.addActionListener(this);
      add(cbx);
    }

   private void addClassButton(String nm,ClassType cty) {
      boolean fg = package_graph.getClassOption(cty);
      JCheckBox btn = new JCheckBox(nm,fg);
      btn.addActionListener(new ClassAction(cty));
      btn.setToolTipText("Show " + nm.toLowerCase() + " types");
      add(btn);
    }

   public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();
      AbstractButton ab = (AbstractButton) e.getSource();

      if (btn == null) ;
      else if (btn.equals("Show Labels")) {
	 graph_panel.setShowLabels(ab.isSelected());
       }
    }

}	// end of inner class NodeTab




private class ClassAction implements ActionListener {

   private ClassType class_type;

   ClassAction(ClassType cty) {
      class_type = cty;
   }

   @Override public void actionPerformed(ActionEvent e) {
      AbstractButton cbx = (AbstractButton) e.getSource();
      package_graph.setClassOption(class_type,cbx.isSelected());
      resetGraph();
   }

}	// end of inner class ClassAction






/********************************************************************************/
/*										*/
/*	Tab for edge options							*/
/*										*/
/********************************************************************************/

private class EdgeTab extends JPanel implements ActionListener {

   EdgeTab() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      addRelationButton("Superclass",ArcType.SUBCLASS);
      addRelationButton("Implements",ArcType.IMPLEMENTED_BY);
      addRelationButton("Extends",ArcType.EXTENDED_BY);
      addRelationButton("Inner class",ArcType.INNERCLASS);
      addRelationButton("Allocates",ArcType.ALLOCATES);
      addRelationButton("Calls",ArcType.CALLS);
      addRelationButton("Catches",ArcType.CATCHES);
      addRelationButton("Accesses",ArcType.ACCESSES);
      addRelationButton("Writes",ArcType.WRITES);
      addRelationButton("Constant",ArcType.CONSTANT);
      addRelationButton("Field",ArcType.FIELD);
      addRelationButton("Local",ArcType.LOCAL);
      addRelationButton("Members",ArcType.MEMBER_OF);

      // add(new JSeparator()); -- make it fixed size
      JCheckBox cbx = new JCheckBox("Show Labels",graph_panel.getShowArcLabels());
      cbx.setToolTipText("Show edge labels");
      cbx.addActionListener(this);
      add(cbx);
    }

   private void addRelationButton(String nm,ArcType rtyp) {
      boolean fg = package_graph.getArcOption(rtyp);
      JCheckBox btn = new JCheckBox(nm,fg);
      btn.addActionListener(new RelationAction(rtyp));
      btn.setToolTipText("Show " + nm.toLowerCase() + " relationships");
      add(btn);
    }

   public void actionPerformed(ActionEvent e) {
      String btn = e.getActionCommand();
      AbstractButton ab = (AbstractButton) e.getSource();

      if (btn == null) ;
      else if (btn.equals("Show Labels")) {
	 graph_panel.setShowArcLabels(ab.isSelected());
       }
    }

}	// end of inner class EdgeTab




private class RelationAction implements ActionListener {

   private ArcType rel_type;

   RelationAction(ArcType rtyp) {
      rel_type = rtyp;
   }

   @Override public void actionPerformed(ActionEvent e) {
      AbstractButton cbx = (AbstractButton) e.getSource();
      package_graph.setArcOption(rel_type,cbx.isSelected());
      resetGraph();
   }
}




/********************************************************************************/
/*										*/
/*	Tab for layout options							*/
/*										*/
/********************************************************************************/

private class LayoutTab extends JPanel implements ActionListener {

   LayoutTab() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

      ButtonGroup bg = new ButtonGroup();
      addLayoutButton(bg,"Circle",LayoutType.CIRCLE);
      addLayoutButton(bg,"Force",LayoutType.FORCE);
      addLayoutButton(bg,"Map",LayoutType.MAP);
      addLayoutButton(bg,"General",LayoutType.GENERAL);
      addLayoutButton(bg,"Spring",LayoutType.SPRING);
      addLayoutButton(bg,"Tree",LayoutType.TREE);
      addLayoutButton(bg,"Stack",LayoutType.STACK);
      addLayoutButton(bg,"Parallel",LayoutType.PSTACK);
      addLayoutButton(bg,"EdgeLabel",LayoutType.ESTACK);
      addLayoutButton(bg,"Partition",LayoutType.PARTITION);
    }

   private void addLayoutButton(ButtonGroup grp,String nm,LayoutType lty) {
      boolean fg = (graph_panel.getLayoutType() == lty);
      JRadioButton itm = new JRadioButton(nm,fg);
      grp.add(itm);
      itm.addActionListener(new LayoutAction(lty));
      itm.setToolTipText("Use " + nm.toLowerCase() + " layout");
      add(itm);
    }

   public void actionPerformed(ActionEvent e) {
    }

}	// end of inner class LayoutTab




private class LayoutAction implements ActionListener {

    private LayoutType for_type;

    LayoutAction(LayoutType typ) {
       for_type = typ;
     }

    @Override public void actionPerformed(ActionEvent e) {
       AbstractButton cbx = (AbstractButton) e.getSource();
       if (cbx.isSelected() && graph_panel.getLayoutType() != for_type) {
	  graph_panel.setLayoutType(for_type);
	  resetGraph();
	}
     }

}	// end of inner class LayoutAction




/********************************************************************************/
/*										*/
/*     Class for bubble contents						*/
/*										*/
/********************************************************************************/

private class PackagePanel extends SwingGridPanel
      implements BudaConstants.BudaBubbleOutputer {

   @Override public String getConfigurator()		{ return "BCON"; }

   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","PACKAGE");
      xw.field("PROJECT",for_project);
      xw.field("PACKAGE",for_package);
   }


}




}	// end of class BconPackagePanel





/* end of BconPackagePanel.java */







