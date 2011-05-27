/********************************************************************************/
/*										*/
/*		BconPackageGraph.java						*/
/*										*/
/*	Bubbles Environment Context Viewer package graph holder 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.banal.BanalConstants;
import edu.brown.cs.bubbles.banal.BanalFactory;

import java.util.*;



class BconPackageGraph implements BconConstants, BanalConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		for_project;
private String		for_package;
private Set<ClassType>	class_options;
private Set<ArcType>	arc_options;
private boolean 	include_children;

private Collection<BanalPackageClass> cur_classes;
private Map<String,GraphNode> active_nodes;

private Collection<BconGraphNode> cur_nodes;
private boolean 	need_recompute;
private boolean 	dont_reuse;
private boolean 	auto_collapse;

private Map<String,ArcType> collapse_nodes;


static Map<PackageRelationType,ArcType> relation_map;

static Map<ArcType,Integer> node_priority;

static {
   relation_map = new HashMap<PackageRelationType,ArcType>();

   relation_map.put(PackageRelationType.SUPERCLASS,ArcType.SUBCLASS);
   relation_map.put(PackageRelationType.IMPLEMENTS,ArcType.IMPLEMENTED_BY);
   relation_map.put(PackageRelationType.EXTENDS,ArcType.EXTENDED_BY);
   relation_map.put(PackageRelationType.INNERCLASS,ArcType.INNERCLASS);
   relation_map.put(PackageRelationType.ALLOCATES,ArcType.ALLOCATES);
   relation_map.put(PackageRelationType.CALLS,ArcType.CALLS);
   relation_map.put(PackageRelationType.CATCHES,ArcType.CATCHES);
   relation_map.put(PackageRelationType.ACCESSES,ArcType.ACCESSES);
   relation_map.put(PackageRelationType.WRITES,ArcType.WRITES);
   relation_map.put(PackageRelationType.CONSTANT,ArcType.CONSTANT);
   relation_map.put(PackageRelationType.FIELD,ArcType.FIELD);
   relation_map.put(PackageRelationType.LOCAL,ArcType.LOCAL);
   relation_map.put(PackageRelationType.PACKAGE,ArcType.PACKAGE);

   node_priority = new HashMap<ArcType,Integer>();
   node_priority.put(ArcType.SUBCLASS,-1000);
   node_priority.put(ArcType.IMPLEMENTED_BY,-1000);
   node_priority.put(ArcType.EXTENDED_BY,-1000);
   node_priority.put(ArcType.INNERCLASS,-500);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconPackageGraph(String proj,String pkg)
{
   for_project = proj;
   for_package = pkg;

   class_options = EnumSet.allOf(ClassType.class);
   arc_options = EnumSet.allOf(ArcType.class);
   include_children = false;

   cur_classes = new HashSet<BanalPackageClass>();
   // TODO: register for file changes to know when to update cur_classes
   //	need to clear cur_classes in that case
   need_recompute = true;
   dont_reuse = false;
   auto_collapse = true;
   cur_nodes = null;

   active_nodes = new HashMap<String,GraphNode>();

   collapse_nodes = new HashMap<String,ArcType>();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean getClassOption(ClassType cty)
{
   return class_options.contains(cty);
}


void setClassOption(ClassType cty,boolean fg)
{
   if (fg) class_options.add(cty);
   else class_options.remove(cty);
}


boolean getArcOption(ArcType prt)
{
   return arc_options.contains(prt);
}



void setArcOption(ArcType prt,boolean fg)
{
   if (fg) arc_options.add(prt);
   else arc_options.remove(prt);
}


boolean getIncludeChildren()			{ return include_children; }
void setIncludeChildren(boolean fg)		{ include_children = fg; }



void collapseNode(BconGraphNode nd,ArcType prt)
{
   // should find node for parent and save it here
   String nm = nd.getFullName();
   int idx = nm.lastIndexOf(".");
   int idx1 = nm.lastIndexOf("$");
   if (idx1 > 0 && prt == ArcType.INNERCLASS) idx = idx1;
   if (idx < 0) return;
   nm = nm.substring(0,idx);
   need_recompute = true;
   collapse_nodes.put(nm,prt);
}


void expandNode(BconGraphNode nd)
{
   need_recompute = true;
   collapse_nodes.put(nd.getFullName(),ArcType.NONE);
   dont_reuse = true;
}


ArcType getCollapsedType(BconGraphNode nd)
{
   ArcType prt = collapse_nodes.get(nd.getFullName());
   if (prt == null) return ArcType.NONE;
   return prt;
}




/********************************************************************************/
/*										*/
/*	Methods to get access to the induced graph				*/
/*										*/
/********************************************************************************/

Collection<BconGraphNode> getNodes()
{
   if (cur_classes == null || cur_classes.isEmpty()) need_recompute = true;

   synchronized (this) {
      if (!need_recompute) return cur_nodes;
      if (cur_classes == null || cur_classes.isEmpty()) {
	 cur_classes = BanalFactory.getFactory().computePackageGraph(for_project,for_package);
       }
      cur_nodes = new ArrayList<BconGraphNode>();
      Map<String,GraphNode> priors = active_nodes;
      if (dont_reuse) priors.clear();
      dont_reuse = false;
      active_nodes = new HashMap<String,GraphNode>();

      for (BanalPackageClass bpc : cur_classes) {
	 if (useClass(bpc)) createNode(bpc,priors);
       }

      if (auto_collapse) {
	 collapseNodes();
	 auto_collapse = false;
	 return getNodes();
      }
    }

   return cur_nodes;
}


private GraphNode createNode(BanalPackageClass bpc,Map<String,GraphNode> priors)
{
   String nm = bpc.getName();
   GraphNode gn = active_nodes.get(nm);
   if (gn != null) return gn;
   GraphNode pgn = null;
   String pnm = null;

   if (bpc.getTypes().contains(ClassType.INNER)) {
      int idx = nm.lastIndexOf("$");
      int idx1 = nm.lastIndexOf(".");
      if (idx1 > idx) idx = idx1;
      pnm = nm.substring(0,idx);
      if (collapse_nodes.get(pnm) == ArcType.INNERCLASS) {
	 pgn = active_nodes.get(pnm);
	 if (pgn == null) {
	    for (BanalPackageClass pcn : cur_classes) {
	       if (pcn.getName().equals(pnm)) {
		  pgn = createNode(pcn,priors);
		  break;
	       }
	    }
	 }
	 if (pgn != null && !pgn.getIncludeChildren()) {
	    active_nodes.put(nm,pgn);
	    pgn.addClass(bpc);
	    return pgn;
	 }
      }
    }

   int idx2 = nm.lastIndexOf(".");
   if (idx2 < 0) pnm = "<Default Package>";
   else pnm = nm.substring(0,idx2);
   if (collapse_nodes.get(pnm) == ArcType.PACKAGE) {
      pgn = active_nodes.get(pnm);
      if (pgn == null) {
	 pgn = createPackageNode(pnm,priors);
      }
      if (pgn != null && !pgn.getIncludeChildren()) {
	 active_nodes.put(nm,pgn);
	 pgn.addClass(bpc);
	 return pgn;
      }
   }

   gn = priors.get(bpc.getName());
   if (gn == null) gn = new ClassNode(bpc);
   else gn.invalidate();

   active_nodes.put(gn.getFullName(),gn);
   cur_nodes.add(gn);
   if (pgn != null) pgn.addChild(gn);

   return gn;
}



PackageNode createPackageNode(String nm,Map<String,GraphNode> priors)
{
   GraphNode pnd = null;
   int idx = nm.lastIndexOf(".");
   if (idx > 0) {
      String pnm = nm.substring(0,idx);
      if (collapse_nodes.get(pnm) == ArcType.PACKAGE) {
	 pnd = active_nodes.get(pnm);
	 if (pnd != null && !pnd.getIncludeChildren()) return null;
      }
   }

   PackageNode pn = null;

   GraphNode pgn = priors.get(nm);
   if (pgn != null && pgn instanceof PackageNode) {
      pn = (PackageNode) pgn;
      pn.invalidate();
    }
   else pn = new PackageNode(nm);

   active_nodes.put(nm,pn);
   cur_nodes.add(pn);
   if (pnd != null) pnd.addChild(pn);

   return pn;
}



/********************************************************************************/
/*										*/
/*	Handle automatic collapsing at the start				*/
/*										*/
/********************************************************************************/

private void collapseNodes()
{
   int ct = cur_nodes.size();
   if (ct < 10) return;

   // first remove all inner classes
   int ict = 0;
   for (BconGraphNode gn : cur_nodes) {
      if (gn.isInnerClass()) {
	 ++ict;
	 collapseNode(gn,ArcType.INNERCLASS);
	 need_recompute = true;
      }
   }
   ct -= ict;
   if (ct < 10) return;

   Map<String,Integer> pmap = new HashMap<String,Integer>();
   for (BconGraphNode gn : cur_nodes) {
      if (gn.isInnerClass()) continue;
      String nm = gn.getFullName();
      String pnm = "<Default Package>";
      int idx = nm.lastIndexOf(".");
      if (idx > 0) pnm = nm.substring(0,idx);
      if (pmap.get(pnm) == null) pmap.put(pnm,1);
      else pmap.put(pnm,pmap.get(pnm) + 1);
   }
   for (Iterator<Map.Entry<String,Integer>> it = pmap.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String,Integer> ent = it.next();
      if (ent.getValue() <= 1) it.remove();
   }

   while (ct > 10 && !pmap.isEmpty()) {
      String pkg = null;
      int bct = 0;
      int bdep = 0;
      for (Map.Entry<String,Integer> ent : pmap.entrySet()) {
	 String pnm = ent.getKey();
	 int dep = getDepth(pnm);
	 boolean use = false;
	 if (pkg == null) use = true;
	 if (!use && dep > bdep) use = true;
	 if (!use && dep == bdep && ent.getValue() < bct) use = true;
	 if (use) {
	    pkg = pnm;
	    bct = ent.getValue();
	    bdep = dep;
	 }
      }
     if (ct - bct >= 5) {
	collapse_nodes.put(pkg,ArcType.PACKAGE);
	ct -= (bct -1);
	need_recompute = true;
     }
     pmap.remove(pkg);
   }
}




private int getDepth(String nm)
{
   int dep = 0;
   int idx = -1;
   for ( ; ; ) {
      idx = nm.indexOf(".",idx+1);
      if (idx < 0) break;
      ++dep;
   }
   return dep;
}




/********************************************************************************/
/*										*/
/*	Node/arc use checking methods						*/
/*										*/
/********************************************************************************/

private boolean inPackage(String nm)
{
   if (for_package == null) return true;
   if (!nm.startsWith(for_package)) return false;
   if (nm.equals(for_package)) return true;

   int pln = for_package.length();
   if (nm.charAt(pln) != '.') return false;

   return true;
}


private boolean useClass(BanalPackageClass c)
{
   if (!inPackage(c.getName())) return false;

   Set<ClassType> ctyps = c.getTypes();

   for (ClassType cty : ctyps) {
      if (ctyps.contains(cty) && !class_options.contains(cty)) return false;
   }

   return true;
}



private boolean useEdge(BanalPackageLink lnk)
{
  Map<PackageRelationType,Integer> typs = lnk.getTypes();
  for (Map.Entry<PackageRelationType,Integer> ent : typs.entrySet()) {
     PackageRelationType prt = ent.getKey();
     ArcType at = relation_map.get(prt);

     if (arc_options.contains(at) && ent.getValue() > 0) {
	if (!useClass(lnk.getFromClass())) return false;
	if (!useClass(lnk.getToClass())) return false;
	return true;
      }
   }

  return false;
}



/********************************************************************************/
/*										*/
/*	Naming methods								*/
/*										*/
/********************************************************************************/

private String getClassDisplayName(BanalPackageClass cls)
{
   String nm = cls.getName();
   if (for_package != null && nm.startsWith(for_package)) {
      int pln = for_package.length();
      if (nm.charAt(pln) == '.') nm = nm.substring(pln+1);
   }

   nm = nm.replace("$",".");

   return nm;
}



private String getHtmlText(BanalPackageLink lnk,boolean flip)
{
   StringBuffer buf = new StringBuffer();
   String frm = lnk.getFromClass().getName();
   String to = lnk.getToClass().getName();

   buf.append("[");
   buf.append(frm);
   buf.append("&mdash;(");
   int ctr = 0;
   for (Map.Entry<PackageRelationType,Integer> ent : lnk.getTypes().entrySet()) {
      if (ent.getValue() > 0) {
	 if (ctr++ > 0) buf.append(",");
	 buf.append(ent.getKey());
       }
    }
   buf.append(")&mdash;&rarr;");
   buf.append(to);
   buf.append("]");
   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Node representation							*/
/*										*/
/********************************************************************************/

private abstract class GraphNode implements BconGraphNode {

   private boolean node_children;
   private Set<GraphNode> child_nodes;
   private GraphNode parent_node;
   private Map<GraphNode,BconGraphArc> out_arcs;
   private Map<GraphNode,BconGraphArc> prior_arcs;
   private boolean check_arcs;
   private Set<BanalPackageClass> class_set;

   GraphNode() {
      node_children = false;
      child_nodes = null;
      parent_node = null;
      out_arcs = new HashMap<GraphNode,BconGraphArc>();
      prior_arcs = null;
      check_arcs = true;
      class_set = null;
    }

   @Override public String getDisplayName()	       { return getFullName(); }

   @Override public boolean getIncludeChildren()	{ return node_children; }
   @Override public void setIncludeChildren(boolean fg) { node_children = fg; }

   @Override public boolean hasChildren() {
      return child_nodes != null && child_nodes.size() > 0;
    }
   @Override public Set<BconGraphNode> getChildren()	{ return new HashSet<BconGraphNode>(child_nodes); }
   @Override public BconGraphNode getParent()		{ return parent_node; }

   void invalidate() {
      check_arcs = true;
      child_nodes = null;
      node_children = false;
    }

   void addChild(GraphNode gn) {
      gn.parent_node = this;
      if (child_nodes == null) child_nodes = new HashSet<GraphNode>();
      child_nodes.add(gn);
    }

   @Override public int getArcCount() {
      return getOutArcs().size();
   }

   @Override public Collection<BconGraphArc> getOutArcs() {
      if (check_arcs) {
	 if (prior_arcs == null) {
	    prior_arcs = out_arcs;
	    out_arcs = new HashMap<GraphNode,BconGraphArc>();
	  }
	 addOutArcs();
	 check_arcs = false;
	 prior_arcs = null;
       }
      return out_arcs.values();
   }

   protected void addOutArcs() {
      addLocalArcs();
      if (hasChildren()) {
	 for (GraphNode cn : child_nodes) {
	    cn.addOutArcs();
	 }
      }
   }

   void addClass(BanalPackageClass cls) {
      if (class_set == null) class_set = new HashSet<BanalPackageClass>();
      class_set.add(cls);
    }

   protected void addLocalArcs() {
      if (class_set == null) return;
      for (BanalPackageClass cls : class_set)
	 addLocalLinks(cls);
   }


   protected void addLocalLinks(BanalPackageClass cls) {
      for (BanalPackageLink bpl : cls.getOutLinks()) {
	 if (useEdge(bpl)) {
	    String nm = bpl.getToClass().getName();
	    GraphNode tgn = active_nodes.get(nm);
	    if (tgn.linkExists(this)) tgn.addLinkData(bpl,this,false);
	    else addLinkData(bpl,tgn,true);
	  }
       }
    }

   private boolean linkExists(GraphNode tgt) {
      if (prior_arcs != null) return prior_arcs.containsKey(tgt);
      else return out_arcs.containsKey(tgt);
    }

   private void addLinkData(BanalPackageLink bpl,GraphNode tgn,boolean fwd) {
      if (tgn == null || tgn == this) return;
      GraphArc ga = (GraphArc) out_arcs.get(tgn);
      if (ga != null) ga.addLink(bpl,fwd);
      else {
	 if (prior_arcs != null) ga = (GraphArc) prior_arcs.get(tgn);
	 else if (check_arcs) {
	    prior_arcs = out_arcs;
	    out_arcs = new HashMap<GraphNode,BconGraphArc>();
	  }
	 if (ga == null) ga = new GraphArc(this,tgn,bpl);
	 else ga.invalidate(bpl);
	 out_arcs.put(tgn,ga);
       }
    }

}	// end of inner abstract class GraphNode



private class ClassNode extends GraphNode {

   private BanalPackageClass for_class;

   ClassNode(BanalPackageClass cls) {
      addClass(cls);
      for_class = cls;
    }

   @Override public String getFullName()		{ return for_class.getName(); }
   @Override public String getDisplayName()		{ return getClassDisplayName(for_class); }
   @Override public Set<ClassType> getClassType()	{ return for_class.getTypes(); }
   @Override public NodeType getNodeType() {
      Set<ClassType> ct = getClassType();
      if (ct.contains(ClassType.THROWABLE)) return NodeType.THROWABLE;
      if (ct.contains(ClassType.CLASS)) return NodeType.CLASS;
      if (ct.contains(ClassType.ENUM)) return NodeType.ENUM;
      if (ct.contains(ClassType.INTERFACE)) return NodeType.INTERFACE;
      if (ct.contains(ClassType.ANNOTATION)) return NodeType.ANNOTATION;
      return NodeType.CLASS;
    }

   @Override public boolean isInnerClass() {
      return for_class.getTypes().contains(ClassType.INNER);
   }

}	// end of inner class ClassNode



private class PackageNode extends GraphNode {

   private String package_name;

   PackageNode(String nm) {
      package_name = nm;
    }

   @Override public String getFullName()		{ return package_name; }
   @Override public NodeType getNodeType()		{ return NodeType.PACKAGE; }
   @Override public boolean isInnerClass()		{ return false; }
   @Override public Set<ClassType> getClassType()	{ return EnumSet.noneOf(ClassType.class); }

}	// end of inner class PackageNode



/********************************************************************************/
/*										*/
/*	Arc representation							*/
/*										*/
/********************************************************************************/

private class GraphArc implements BconGraphArc {

   private List<BanalPackageLink> forward_links;
   private List<BanalPackageLink> backward_links;
   private RelationData relation_data;
   private GraphNode from_node;
   private GraphNode to_node;
   boolean flip_flop;

   GraphArc(GraphNode frm,GraphNode to,BanalPackageLink lnk) {
      forward_links = new ArrayList<BanalPackageLink>();
      backward_links = new ArrayList<BanalPackageLink>();
      if (lnk != null) forward_links.add(lnk);
      relation_data = null;
      from_node = frm;
      to_node = to;
      flip_flop = false;
    }

   void invalidate(BanalPackageLink lnk) {
      forward_links.clear();
      backward_links.clear();
      if (lnk != null) forward_links.add(lnk);
      relation_data = null;
    }

   void addLink(BanalPackageLink lnk,boolean fwd) {
      if (fwd) forward_links.add(lnk);
      else backward_links.add(lnk);
      relation_data = null;
   }

   @Override public BconRelationData getRelationTypes() {
      if (relation_data != null) return relation_data;

      RelationData rslt = new RelationData();
      for (BanalPackageLink bpl : forward_links) {
	 for (Map.Entry<PackageRelationType,Integer> ent : bpl.getTypes().entrySet()) {
	    PackageRelationType k = ent.getKey();
	    ArcType at = relation_map.get(k);
	    if (at != null) {
	       rslt.addLink(at,ent.getValue(),false);
	     }
	  }
       }
      for (BanalPackageLink bpl : backward_links) {
	 for (Map.Entry<PackageRelationType,Integer> ent : bpl.getTypes().entrySet()) {
	    PackageRelationType k = ent.getKey();
	    ArcType at = relation_map.get(k);
	    if (at != null) {
	       rslt.addLink(at,ent.getValue(),true);
	     }
	  }
       }
      relation_data = rslt;
      flip_flop = relation_data.shouldFlip();
      return relation_data;
    }

   @Override public boolean isInnerArc() {
      if (forward_links.size() != 0) {
	 BanalPackageLink lnk = forward_links.get(0);
	 if (!lnk.getFromClass().getName().equals(from_node.getFullName())) return true;
       }
      return false;
    }

   @Override public BconGraphNode getFromNode() {
      if (relation_data == null) getRelationTypes();
      return (flip_flop ? to_node : from_node);
    }
   @Override public BconGraphNode getToNode() {
      if (relation_data == null) getRelationTypes();
      return (flip_flop ? from_node : to_node);
    }

   @Override public boolean useSourceArrow() {
      if (relation_data == null) getRelationTypes();
      return (flip_flop ? relation_data.useTargetArrow() : relation_data.useSourceArrow());
   }

   @Override public boolean useTargetArrow() {
      if (relation_data == null) getRelationTypes();
      return (flip_flop ? relation_data.useSourceArrow() : relation_data.useTargetArrow());
   }

   @Override public String getLabel() {
      String rslt = "<html>";
      int ct = 0;
      for (BanalPackageLink lnk : forward_links) {
	 if (ct++ > 0) rslt += "<br>";
	 rslt += getHtmlText(lnk,flip_flop);
       }
      for (BanalPackageLink lnk : backward_links) {
	 if (ct++ > 0) rslt += "<br>";
	 rslt += getHtmlText(lnk,!flip_flop);
       }
      return rslt;
    }

}	// end of inner class GraphArc



/********************************************************************************/
/*										*/
/*	Hold information about a relationship					*/
/*										*/
/********************************************************************************/


private static class RelationData implements BconRelationData {

   private Map<ArcType,Integer> forward_counts;
   private Map<ArcType,Integer> backward_counts;

   RelationData() {
      forward_counts = new HashMap<ArcType,Integer>();
      backward_counts = new HashMap<ArcType,Integer>();
    }

   void addLink(ArcType at,int ct,boolean bkwd) {
      Map<ArcType,Integer> mp = (bkwd ? backward_counts : forward_counts);
      Integer v = mp.get(at);
      if (v == null) mp.put(at,ct);
      else mp.put(at,ct + v);
    }

   @Override public ArcType getPrimaryRelationship() {
      int mxn = 0;
      ArcType prt = ArcType.NONE;
      for (Map.Entry<ArcType,Integer> ent : forward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer rv = backward_counts.get(ent.getKey());
	 if (rv != null) vl += rv;
	 if (vl > mxn) {
	    mxn = ent.getValue();
	    prt = ent.getKey();
	  }
       }
      for (Map.Entry<ArcType,Integer> ent : backward_counts.entrySet()) {
	 int vl = ent.getValue();
	 if (vl > mxn) {
	    mxn = ent.getValue();
	    prt = ent.getKey();
	  }
       }
      return prt;
    }

   @Override public int getRelationshipCount() {
      int ct = 0;
      for (Integer v : forward_counts.values()) ct += v;
      for (Integer v : backward_counts.values()) ct += v;
      return ct;
    }

   boolean shouldFlip() {
      int ct = 0;
      for (Map.Entry<ArcType,Integer> ent : forward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer pr = node_priority.get(ent.getKey());
	 if (pr != null) vl *= pr;
	 ct += vl;
       }
      for (Map.Entry<ArcType,Integer> ent : backward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer pr = node_priority.get(ent.getKey());
	 if (pr != null) vl *= pr;
	 ct -= vl;
       }
      if (ct < 0) return true;
      return false;
    }

   boolean useTargetArrow() {
      for (Map.Entry<ArcType,Integer> ent : forward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer pr = node_priority.get(ent.getKey());
	 if (pr != null) vl *= pr;
	 if (vl > 0) return true;
       }
      for (Map.Entry<ArcType,Integer> ent : backward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer pr = node_priority.get(ent.getKey());
	 if (pr != null) vl *= pr;
	 if (vl > 0) return true;
       }
      return false;
    }

   boolean useSourceArrow() {
      for (Map.Entry<ArcType,Integer> ent : forward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer pr = node_priority.get(ent.getKey());
	 if (pr != null) vl *= pr;
	 if (vl < 0) return true;
       }
      for (Map.Entry<ArcType,Integer> ent : backward_counts.entrySet()) {
	 int vl = ent.getValue();
	 Integer pr = node_priority.get(ent.getKey());
	 if (pr != null) vl *= pr;
	 if (vl <0) return true;
       }
      return false;
   }

}	// end of inner class RelationData

}	// end of class BconPackageGraph



/* end of BconPackageGraph.java */

