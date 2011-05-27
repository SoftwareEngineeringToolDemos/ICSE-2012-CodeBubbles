/********************************************************************************/
/*										*/
/*		BconPackageDisplay .java					*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.banal.BanalConstants;
import edu.brown.cs.bubbles.board.BoardThreadPool;

import edu.brown.cs.ivy.petal.*;

import javax.swing.*;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.*;



class BconPackageDisplay extends JPanel implements BconConstants, BanalConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BconPackageGraph	package_graph;
private boolean 		show_labels;
private boolean 		arc_labels;
private boolean 		reset_graphics;
private LayoutType		layout_type;
private ComputeGraph		compute_graph;

private boolean 		layout_needed;
private boolean 		freeze_layout;
private double			user_scale;
private double			prior_scale;

private PetalEditor		petal_editor;
private GraphModel		petal_model;
private PetalLayoutMethod	layout_method;

private Map<BconGraphNode,BconPetalNode> node_map;
private Map<BconGraphArc,BconPetalArc>	 arc_map;

private static Map<NodeType,CompShape> shape_map;

private final double SCALE_FACTOR = 1.125;

enum CompShape {
   SQUARE,
   TRIANGLE,
   CIRCLE,
   DIAMOND,
   PENTAGON
}

static {
   shape_map = new HashMap<NodeType,CompShape>();
   shape_map.put(NodeType.CLASS,CompShape.DIAMOND);
   shape_map.put(NodeType.INTERFACE,CompShape.TRIANGLE);
   shape_map.put(NodeType.ENUM,CompShape.CIRCLE);
   shape_map.put(NodeType.THROWABLE,CompShape.PENTAGON);
   shape_map.put(NodeType.ANNOTATION,CompShape.PENTAGON);
   shape_map.put(NodeType.PACKAGE,CompShape.SQUARE);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconPackageDisplay(BconPackageGraph pg)
{
   super(new BorderLayout());

   package_graph = pg;
   show_labels = false;
   arc_labels = false;
   layout_needed = true;
   freeze_layout = false;
   reset_graphics = false;
   user_scale = 1.0;
   prior_scale = 1.0;
   compute_graph = new ComputeGraph();

   node_map = new HashMap<BconGraphNode,BconPetalNode>();
   arc_map = new HashMap<BconGraphArc,BconPetalArc>();

   // setupGraphModel();
   BoardThreadPool.start(compute_graph);
   PetalUndoSupport undo = PetalUndoSupport.getSupport();
   undo.blockCommands();

   petal_model = new GraphModel();
   petal_editor = new PetalEditor(petal_model);
   setLayoutType(LayoutType.GENERAL);

   petal_editor.addMouseWheelListener(new Wheeler());

   JScrollPane jsp = new JScrollPane(petal_editor);
   // jsp.setWheelScrollingEnabled(false);
   jsp.addMouseWheelListener(new Wheeler());

   add(jsp,BorderLayout.CENTER);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean getShowLabels() 			{ return show_labels; }
void setShowLabels(boolean fg)
{
   if (show_labels == fg) return;
   show_labels = fg;
   reset_graphics = true;
   relayout();
}




boolean getShowArcLabels()			{ return arc_labels; }
void setShowArcLabels(boolean fg)		{ arc_labels = fg; }

LayoutType getLayoutType()			{ return layout_type; }
void setLayoutType(LayoutType t)
{
   if (layout_type == t) return;

   layout_type = t;

   switch (layout_type) {
      case FORCE :
	 PetalRelaxLayout prl = new PetalRelaxLayout(petal_editor);
	 layout_method = prl;
	 break;
      case SPRING :
	 prl = new PetalRelaxLayout(petal_editor);
	 prl.setArcLength(50);
	 prl.setDistance(100);
	 layout_method = prl;
	 break;
      default :
      case GENERAL :
	 PetalLevelLayout pll = new PetalLevelLayout(petal_editor);
	 pll.setOptimizeLevels(true);
	 pll.setWhiteFraction(1.0);
	 layout_method = pll;
	 break;
      case TREE :
	 pll = new PetalLevelLayout(petal_editor);
	 pll.setWhiteFraction(1.0);
	 layout_method = pll;
	 break;
      case MAP :
	 pll = new PetalLevelLayout(petal_editor);
	 pll.setTwoWay(true);
	 pll.setWhiteFraction(1.0);
	 layout_method = pll;
	 break;
      case CIRCLE :
	 PetalCircleLayout pcl = new PetalCircleLayout(petal_editor);
	 layout_method = pcl;
	 break;
   }

   petal_editor.commandLayout(layout_method);
}



void relayout()
{
   layout_needed = true;
   BoardThreadPool.start(compute_graph);
   // setupGraphModel();
   // repaint();
}



void updateGraph()
{
   BoardThreadPool.start(compute_graph);
   // setupGraphModel();
   // repaint();
}


void zoom(int amt)
{
   for (int i = 0; i < Math.abs(amt); ++i) {
      if (amt < 0) user_scale /= SCALE_FACTOR;
      else user_scale *= SCALE_FACTOR;
   }
   if (Math.abs(user_scale - 1.0) < 0.001) user_scale = 1;
   if (user_scale < 1/128.0) user_scale = 1/128.0;
   // if (user_scale > 2048) user_scale = 2048;

   if (petal_editor != null) {
      double sf = petal_editor.getScaleFactor();
      if (sf * user_scale / prior_scale > 2) {
	 user_scale = 2 * prior_scale / sf;
      }
      sf = sf * user_scale / prior_scale;
      petal_editor.setScaleFactor(sf);
      prior_scale = user_scale;
      petal_editor.repaint();
   }
}




void handlePopupMenu(MouseEvent e)
{
   Point p0 = e.getLocationOnScreen();
   SwingUtilities.convertPointFromScreen(p0,petal_editor);
   PetalNode pn0 = petal_editor.findNode(p0);
   if (pn0 == null) return;

   BconPetalNode bpn = (BconPetalNode) pn0;
   bpn.handlePopupMenu(e);
}




/********************************************************************************/
/*										*/
/*	Graph model setup							*/
/*										*/
/********************************************************************************/

private void setupGraphModel()
{
   Collection<BconGraphNode> nodes = package_graph.getNodes();
   boolean chng = false;

   Set<BconGraphNode> nodeset = new HashSet<BconGraphNode>(nodes);
   for (Iterator<Map.Entry<BconGraphNode,BconPetalNode>> it = node_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<BconGraphNode,BconPetalNode> ent = it.next();
      if (!nodeset.contains(ent.getKey())) {
	 it.remove();
	 chng = true;
       }
      else if (reset_graphics) {
	 ent.getValue().resetGraphics();
      }
    }
   reset_graphics = false;

   for (BconGraphNode gn : nodes) {
      BconPetalNode pn = node_map.get(gn);
      if (pn != null) {
	 // possibly update the petal node if necessary
       }
      else {
	 pn = new BconPetalNode(gn);
	 node_map.put(gn,pn);
	 chng = true;
	 layout_needed = true;
       }
    }

   Set<BconGraphArc> arcs = new HashSet<BconGraphArc>();
   for (BconGraphNode gn : nodes) {
      if (gn.getOutArcs() != null)
	 arcs.addAll(gn.getOutArcs());
    }
   for (Iterator<Map.Entry<BconGraphArc,BconPetalArc>> it = arc_map.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<BconGraphArc,BconPetalArc> ent = it.next();
      if (!arcs.contains(ent.getKey())) {
	 it.remove();
	 chng = true;
       }
    }
   for (BconGraphArc ga : arcs) {
      BconPetalArc pa = arc_map.get(ga);
      if (pa != null) {
	 // possibly update the petal arc if necessary
       }
      else {
	 pa = new BconPetalArc(ga);
	 arc_map.put(ga,pa);
	 chng = true;
       }
    }

   if (chng && !freeze_layout) layout_needed = true;

   if (petal_model != null) {
      petal_model.invalidate();
   }

   if (layout_needed && petal_editor != null) {
      petal_editor.commandDeselectAll();
      petal_editor.commandLayout(layout_method);
      layout_needed = false;
    }

   if (petal_editor != null) {
      Dimension d1 = petal_editor.getPreferredSize();
      Dimension d2 = getSize();
      if (d1.width != 0 && d2.width != 0 && d1.height != 0 && d2.height != 0) {
	 double dx = d2.getWidth() / d1.getWidth();
	 double dy = d2.getHeight() / d1.getHeight();
	 double da = Math.min(dx,dy);
	 double db = petal_editor.getScaleFactor() / prior_scale;
	 petal_editor.setScaleFactor(da*db*0.95*user_scale);
	 prior_scale = user_scale;
      }
   }

   // scale as needed
}



/********************************************************************************/
/*										*/
/*	Petal node and arc implementations					*/
/*										*/
/********************************************************************************/

private Component getDisplayComponent(BconGraphNode gn)
{
   JComponent comp = null;

   if (show_labels) {
      String nm = gn.getFullName();
      int idx = nm.lastIndexOf(".");
      if (idx >= 0) nm = nm.substring(idx+1);
      JLabel lbl = new JLabel(nm);
      Font ft = lbl.getFont();
      ft = ft.deriveFont(9f);
      lbl.setFont(ft);
      lbl.setBorder(new LineBorder(Color.BLACK,2));
      lbl.setMinimumSize(lbl.getPreferredSize());
      lbl.setSize(lbl.getPreferredSize());
      lbl.setMaximumSize(new Dimension(400,400));
      lbl.setOpaque(true);
      comp = lbl;
   }
   else {
      CompShape cs = shape_map.get(gn.getNodeType());
      GraphComponent gc = new GraphComponent(cs);
      comp = gc;
   }

   switch (gn.getNodeType()) {
      case CLASS :
	 comp.setForeground(Color.RED);
	 break;
      case INTERFACE :
	 comp.setForeground(Color.GREEN);
	 break;
      case ENUM :
	 comp.setForeground(Color.ORANGE);
	 break;
      case THROWABLE :
	 comp.setForeground(Color.ORANGE);
	 break;
      case ANNOTATION :
	 comp.setForeground(Color.MAGENTA);
	 break;
      case PACKAGE :
	 comp.setForeground(Color.BLUE);
	 break;
   }

   return comp;
}




private class BconPetalNode extends PetalNodeDefault {

   private BconGraphNode graph_node;

   BconPetalNode(BconGraphNode gn) {
      graph_node = gn;
      setComponent(getDisplayComponent(graph_node));
    }

   @Override public String getToolTip(Point at) {
      String nm = graph_node.getFullName();
      switch (graph_node.getNodeType()) {
	 case CLASS :
	    return "Class " + nm;
	 case INTERFACE :
	    return "Interface " + nm;
	 case ENUM :
	    return "Enum " + nm;
	 case ANNOTATION :
	    return "Annotation " + nm;
	 case PACKAGE :
	    return "Package " + nm;
	 case THROWABLE :
	    return "Throwable " + nm;
      }
      return null;
   }

   @Override public boolean handleMouseClick(MouseEvent e)	{ return false; }
   @Override public boolean handleKeyInput(KeyEvent e)		{ return false; }

   String getSortName() 			{ return graph_node.getFullName(); }
   int getArcCount()				{ return graph_node.getArcCount(); }

   void resetGraphics() {
      setComponent(getDisplayComponent(graph_node));
   }

   void handlePopupMenu(MouseEvent evt) {
      Point p0 = evt.getLocationOnScreen();
      SwingUtilities.convertPointFromScreen(p0,BconPackageDisplay.this);
      JPopupMenu m = new JPopupMenu();
      if (package_graph.getCollapsedType(graph_node) != ArcType.NONE) {
	 m.add(new ExpandAction(graph_node));
      }
      String pnm = graph_node.getFullName();
      if (graph_node.isInnerClass()) {
	 m.add(new CompactAction(graph_node,ArcType.INNERCLASS));
	 int idx = pnm.lastIndexOf(".");
	 if (idx >= 0) pnm = pnm.substring(0,idx);
      }
      if (pnm.contains(".")) {
	 m.add(new CompactAction(graph_node,ArcType.PACKAGE));
      }
      // add goto buttons
      m.show(BconPackageDisplay.this,p0.x,p0.y);
   }

   @Override public Point findPortPoint(Point at,Point from) {
      if (getComponent() instanceof GraphComponent) {
	 GraphComponent gc = (GraphComponent) getComponent();
	 return gc.findPortPoint(at,from);
       }

      return super.findPortPoint(at,from);
    }

}	// end of inner class BconPetalNode



private class BconPetalArc extends PetalArcDefault {

   BconGraphArc for_arc;

   BconPetalArc(BconGraphArc ga) {
      super(node_map.get(ga.getFromNode()),node_map.get(ga.getToNode()));
      for_arc = ga;

      int mxn = ga.getRelationTypes().getRelationshipCount();
      ArcType prt = ga.getRelationTypes().getPrimaryRelationship();
      if (mxn > 0) {
	 float wd = (float)(1 + Math.log(mxn));
	 Stroke s = new BasicStroke(wd);
	 setStroke(s);
      }

      Color c = null;
      switch (prt) {
	 default :
	    c = Color.CYAN;
	    break;
	 case CALLS :
	    c = Color.MAGENTA;
	    break;
	 case SUBCLASS :
	 case IMPLEMENTED_BY :
	 case EXTENDED_BY :
	    c = Color.BLACK;
	    break;
	 case INNERCLASS :
	    c = Color.GRAY;
	    break;
      }
      if (c != null) setColor(c);

      if (ga.useTargetArrow())
	 setTargetEnd(new PetalArcEndDefault(PETAL_ARC_END_ARROW,4,c));
      if (ga.useSourceArrow())
	 setSourceEnd(new PetalArcEndDefault(PETAL_ARC_END_ARROW,4,c));
      // create label if needed
    }

   @Override public boolean handleMouseClick(MouseEvent evt)	{ return false; }

   @Override public String getToolTip() {
      return for_arc.getLabel();
    }

}	// end of inner clas BconPetalArc




/********************************************************************************/
/*										*/
/*	Graph Components							*/
/*										*/
/********************************************************************************/

private class GraphComponent extends JPanel {

   private CompShape	use_shape;
   private Polygon	poly_shape;

   GraphComponent(CompShape sh) {
      Dimension d = new Dimension(20,20);
      setSize(d);
      setPreferredSize(d);
      setMinimumSize(d);
      if (sh == null) sh = CompShape.DIAMOND;
      use_shape = sh;
      setupShape();
   }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(getForeground());
      Rectangle r = getBounds();

      Shape s = null;
      switch (use_shape) {
	 case SQUARE :
	 default :
	    r.x = r.y = 0;
	    s = r;
	    break;
	 case TRIANGLE :
	 case DIAMOND :
	 case PENTAGON :
	    s = poly_shape;
	    break;
	 case CIRCLE :
	    Ellipse2D el = new Ellipse2D.Double();
	    el.setFrame(0,0,r.width,r.height);
	    s = el;
	    break;
       }

      if (s != null) {
	 g2.fill(s);
       }
    }

   void setupShape() {
      Rectangle r = getBounds();
      switch (use_shape) {
	 case SQUARE :
	 case CIRCLE :
	 default :
	    break;
	 case TRIANGLE :
	    poly_shape = new Polygon();
	    poly_shape.addPoint(r.width/2,0);
	    poly_shape.addPoint(0,r.height);
	    poly_shape.addPoint(r.width,r.height);
	    break;
	 case DIAMOND :
	    poly_shape = new Polygon();
	    poly_shape.addPoint(r.width/2,0);
	    poly_shape.addPoint(0,r.height/2);
	    poly_shape.addPoint(r.width/2,r.height);
	    poly_shape.addPoint(r.width,r.height/2);
	    break;
	 case PENTAGON :
	    poly_shape = new Polygon();
	    double a1 = Math.tan(Math.toRadians(54));
	    int h1 = (int)(r.width / 2.0 / a1);
	    double a2 = Math.tan(Math.toRadians(18));
	    int h2 = (int)(a2 * (r.height - h1));
	    poly_shape.addPoint(r.width/2,0);
	    poly_shape.addPoint(r.width,h1);
	    poly_shape.addPoint(r.width - h2,r.height);
	    poly_shape.addPoint(h2,r.height);
	    poly_shape.addPoint(0,h1);
	    break;
       }
   }

   Point  findPortPoint(Point at,Point from) {
      switch (use_shape) {
	 case SQUARE :
	    return PetalHelper.findPortPoint(getBounds(),at,from);
	 case CIRCLE :
	    return PetalHelper.findOvalPortPoint(getBounds(),at,from);
	 case DIAMOND :
	 case TRIANGLE :
	 case PENTAGON :
	    if (poly_shape != null)
	       return PetalHelper.findShapePortPoint(this,poly_shape,at,from);

       }

      return PetalHelper.findPortPoint(getBounds(),at,from);
    }

}	// end of inner class GraphComponent



/********************************************************************************/
/*										*/
/*	PetalModel definitions							*/
/*										*/
/********************************************************************************/

private class GraphModel extends PetalModelBase {

   private PetalNode [] node_array;
   private PetalArc []	arc_array;

   GraphModel() {
      node_array = null;
      arc_array = null;
    }

   void invalidate() {
      node_array = null;
      arc_array = null;
    }

   @Override public PetalNode [] getNodes() {
      if (node_array == null) {
	 node_array = new PetalNode[node_map.size()];
	 node_array = node_map.values().toArray(node_array);
	 Arrays.sort(node_array,new NodeSorter());
       }
      return node_array;
    }

   @Override public PetalArc [] getArcs() {
      if (arc_array == null) {
	 arc_array = new PetalArc[arc_map.size()];
	 arc_array = arc_map.values().toArray(arc_array);
       }
      return arc_array;
    }

   // disable editing
   @Override public void createArc(PetalNode frm,PetalNode to)			{ }
   @Override public boolean dropNode(Object o,Point p,PetalNode pn,PetalArc pa) { return false; }
   @Override public void removeArc(PetalArc pa) 				{ }
   @Override public void removeNode(PetalNode pn)				{ }

}	// end of inner class GraphModel



private static class NodeSorter implements Comparator<PetalNode> {

   @Override public int compare(PetalNode p1,PetalNode p2) {
      int d1 = ((BconPetalNode) p1).getArcCount() - ((BconPetalNode) p2).getArcCount();
      if (d1 != 0) return -d1;
      String s1 = ((BconPetalNode) p1).getSortName();
      String s2 = ((BconPetalNode) p2).getSortName();
      return s1.compareTo(s2);
    }

}	// end of inner class NodeSorter



/********************************************************************************/
/*										*/
/*	Mouse wheel scrolling							*/
/*										*/
/********************************************************************************/

private class Wheeler extends MouseAdapter {

   @Override public void mouseWheelMoved(MouseWheelEvent e) {
      int mods = e.getModifiersEx();
      if ((mods & MouseEvent.CTRL_DOWN_MASK) == 0) return;
      int ct = e.getWheelRotation();
      zoom(ct);
      e.consume();
   }

}	// end of inner class Wheeler




/********************************************************************************/
/*										*/
/*	Key/button actions							*/
/*										*/
/********************************************************************************/

private class ExpandAction extends AbstractAction  {

   private BconGraphNode for_node;

   ExpandAction(BconGraphNode gn) {
      super("Expand node");
      for_node = gn;
   }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.expandNode(for_node);
      node_map.clear();
      relayout();
   }

}	// end of inner class ExapandAction




private class CompactAction extends AbstractAction {

   private BconGraphNode for_node;
   private ArcType compact_type;

   CompactAction(BconGraphNode gn,ArcType at) {
      super("Compact node for " + at.toString());
      for_node = gn;
      compact_type = at;
   }

   @Override public void actionPerformed(ActionEvent e) {
      package_graph.collapseNode(for_node,compact_type);
      relayout();
   }

}	// end of inner class CompactAction




/********************************************************************************/
/*										*/
/*     Delayed Evaluation							*/
/*										*/
/********************************************************************************/

private class ComputeGraph implements Runnable {

   private boolean need_recompute;
   private boolean doing_compute;

   ComputeGraph() {
      need_recompute = false;
      doing_compute = false;
   }

   @Override public void run() {
      synchronized (this) {
	 if (doing_compute) {
	    need_recompute = true;
	    return;
	 }
	 doing_compute = true;
      }

      for ( ; ; ) {
	 package_graph.getNodes();
	 synchronized (this) {
	    if (!need_recompute) {
	       doing_compute = false;
	       break;
	    }
	    need_recompute = false;
	 }
      }

      SwingUtilities.invokeLater(new SetupGraph());
   }

}	// end of inner class ComputeGraph



private class SetupGraph implements Runnable {

   @Override public void run() {
      setupGraphModel();
      repaint();
    }

}	// end of inner class SetupGraph




}	// end of class BconPackageDisplay




/* end of BconPackageDisplay.java */

