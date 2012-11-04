/********************************************************************************/
/*										*/
/*		BaleBubbleStack.java						*/
/*										*/
/*	Bubble Annotated Language Editor bubble stack for bale editors		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.*;

import javax.swing.text.Position;

import java.awt.*;
import java.util.*;
import java.util.List;

class BaleBubbleStack implements BaleConstants, BudaConstants, BussConstants
{


/********************************************************************************/
/*										*/
/*	Methods to create new bubbles or bubble stack from location set 	*/
/*										*/
/********************************************************************************/

static void createBubbles(Component src,Position p,Point pt,boolean near,
			     Collection<BumpLocation> locs,BudaLinkStyle link)
{
   if (locs == null) return;

   // remove duplicate locations
   Map<String,List<BumpLocation>> keys = new HashMap<String,List<BumpLocation>>();
   for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
      BumpLocation bl = it.next();
      String key = null;
      switch (bl.getSymbolType()) {
	 default :
	    break;
	 case FUNCTION :
	 case CONSTRUCTOR :
	    key = bl.getKey();
	    break;
	 case FIELD :
	 case ENUM_CONSTANT :
	    key = bl.getSymbolName();
	    int idx = key.lastIndexOf(".");
	    key = key.substring(0,idx+1) + ".<FIELDS>";
	    break;
	 case STATIC_INITIALIZER :
	    key = bl.getSymbolName();
	    idx = key.lastIndexOf(".");
	    key = key.substring(0,idx+1) + ".<INITIALIZER>";
	    break;
         case MAIN_PROGRAM :
            key = bl.getSymbolName();
            idx = key.lastIndexOf(".");
            key = key.substring(0,idx+1) + ".<MAIN>";
            break;
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	 case MODULE :
	    key = bl.getSymbolName();
	    key = key + ".<PREFIX>";
	    break;
       }
      if (key != null) {
	 List<BumpLocation> lbl = keys.get(key);
	 if (lbl != null) it.remove();
	 else {
	    lbl = new ArrayList<BumpLocation>();
	    keys.put(key,lbl);
	  }
	 lbl.add(bl);
       }
    }

   if (locs.size() > 1) {
      BaleBubbleStack bs = new BaleBubbleStack(src,p,pt,near,link,keys);
      bs.setupStack();
      return;
    }

   for (BumpLocation bl : locs) {
      createBubble(src,p,pt,near,bl,true,link);
    }
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Component			source_bubble;
private Position			source_position;
private Point				source_point;
private boolean 			place_near;
private int				title_width;
private Map<String,List<BumpLocation>>	location_set;
private BudaLinkStyle			link_style;

private static final int DEFAULT_TITLE_WIDTH = 150;
private static final int DEFAULT_CONTENT_WIDTH = 300;
private static final int MAX_ENTRIES = 40;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BaleBubbleStack(Component src,Position p,Point pt,boolean near,BudaLinkStyle link,
			   Map<String,List<BumpLocation>> locs)
{
   source_bubble = src;
   source_position = p;
   source_point = pt;
   location_set = locs;
   title_width = 0;
   place_near = near;
   link_style = link;
}




/********************************************************************************/
/*										*/
/*	Methods to setup the bubble stack					*/
/*										*/
/********************************************************************************/

private void setupStack()
{
   List<BussEntry> entries = new ArrayList<BussEntry>();

   title_width = 0;
   int contentwidth = Integer.MIN_VALUE;

   title_width = DEFAULT_TITLE_WIDTH;

   for (List<BumpLocation> locs : location_set.values()) {
      BumpLocation loc0 = locs.get(0);
      if (entries.size() > MAX_ENTRIES) continue;
      switch (loc0.getSymbolType()) {
	 case FUNCTION :
	 case CONSTRUCTOR :
	    MethodStackEntry se = new MethodStackEntry(locs);
	    entries.add(se);
	    break;
	 case FIELD :
	 case ENUM_CONSTANT :
	    FieldStackEntry fe = new FieldStackEntry(locs);
	    entries.add(fe);
	    break;
	 case STATIC_INITIALIZER :
	    InitializerStackEntry ie = new InitializerStackEntry(locs);
	    entries.add(ie);
	    break;
         case MAIN_PROGRAM :
	    MainProgramStackEntry me = new MainProgramStackEntry(locs);
	    entries.add(me);
	    break;
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	 case MODULE :
	    TypeStackEntry te = new TypeStackEntry(locs);
	    entries.add(te);
	    break;
	 default :
	    createBubble(source_bubble,source_position,source_point,false,loc0,true,BudaLinkStyle.STYLE_SOLID);
	    break;
       }
    }

   contentwidth = DEFAULT_CONTENT_WIDTH;
   for(BussEntry entry : entries){
      BaleCompactFragment component = (BaleCompactFragment) entry.getCompactComponent();
      component.init(contentwidth);
    }

   if (entries.size() == 0) return;
   BussFactory bussf = BussFactory.getFactory();
   BussBubble bb = bussf.createBubbleStack(entries, contentwidth + title_width);

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_bubble);
   if (bba == null) return;
   int place = PLACEMENT_RIGHT|PLACEMENT_MOVETO|PLACEMENT_NEW;
   if (place_near) place |= PLACEMENT_GROUPED;
   bba.addBubble(bb,source_bubble,source_point,place);

   if (source_bubble != null && source_position != null) {
      BudaConstants.LinkPort p0 = new BaleLinePort(source_bubble,source_position,null);
      BudaConstants.LinkPort p1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
      BudaBubble obbl = BudaRoot.findBudaBubble(source_bubble);
      if (obbl != null) {
	 BudaBubbleLink lnk = new BudaBubbleLink(obbl,p0,bb,p1,true,link_style);
	 bba.addLink(lnk);
	 bb.setSourceBubbleInfomation(obbl, p0);
       }
    }

   // BudaRoot.addBubbleViewCallback(new EditorBubbleCallback(bb));
}




/********************************************************************************/
/*										*/
/*	Methods to create a bubble for a location				*/
/*										*/
/********************************************************************************/

private static BudaBubble createBubble(Component src,Position p,Point pt,boolean near,
					  BumpLocation bl,boolean add,BudaLinkStyle link)
{
   if (link == BudaLinkStyle.NONE)
      return BaleFactory.getFactory().createLocationEditorBubble(src,p,pt,near,bl,false,add,true);
   else
      return BaleFactory.getFactory().createLocationEditorBubble(src,p,pt,near,bl,true,add,true);
}




/********************************************************************************/
/*										*/
/*	Stack entry representation						*/
/*										*/
/********************************************************************************/

private abstract class GenericStackEntry implements BussEntry {

   protected List<BumpLocation> entry_locations;
   protected BumpLocation def_location;
   protected BaleFragmentEditor full_fragment;
   protected BaleCompactFragment compact_fragment;
   protected BudaBubble item_bubble;

   GenericStackEntry(List<BumpLocation> locs) {
      entry_locations = new ArrayList<BumpLocation>(locs);
      def_location = locs.get(0);
      full_fragment = null;
      BaleFactory bf = BaleFactory.getFactory();
      BaleDocumentIde bd = bf.getDocument(def_location.getSymbolProject(),def_location.getFile());
      compact_fragment = new BaleCompactFragment(bd,entry_locations,title_width);
    }

   @Override public Component getCompactComponent()	{ return compact_fragment; }


   @Override public Component getExpandComponent() {
      if (full_fragment == null) full_fragment = createFullFragment();
      return full_fragment;
    }
   @Override public String getExpandText()		{ return null; }

   @Override public BudaBubble getBubble() {
      if (item_bubble == null) {
	 def_location.update();
	 item_bubble = createBubble(source_bubble,source_position,source_point,false,def_location,false,BudaLinkStyle.NONE);
       }
      if (item_bubble != null && item_bubble.getContentPane() != null) {
	 item_bubble.getContentPane().repaint();
       }
      return item_bubble;
    }

   @Override abstract public String getEntryName();
   abstract protected BaleFragmentEditor createFullFragment();

   @Override public void dispose() {
      if (item_bubble != null) item_bubble.disposeBubble();
    }

}	// end of inner class GenericMethodEntry




private class MethodStackEntry extends GenericStackEntry {

   MethodStackEntry(List<BumpLocation> locs) {
      super(locs);
    }

   @Override public String getEntryName() {
      String nm = def_location.getSymbolName();
      nm = nm.replace('$','.');
      return nm;
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createMethodFragmentEditor(def_location);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class MethodStackEntry




private class FieldStackEntry extends GenericStackEntry {

   private String class_name;

   FieldStackEntry(List<BumpLocation> locs) {
      super(locs);
      String nm = def_location.getSymbolName();
      int idx = nm.lastIndexOf(".");
      class_name = nm.substring(0,idx);
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<FIELDS>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createFieldFragmentEditor(
	 def_location.getSymbolProject(),class_name);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class FieldStackEntry




private class InitializerStackEntry extends GenericStackEntry {

   private String class_name;

   InitializerStackEntry(List<BumpLocation> locs) {
      super(locs);
      String nm = def_location.getSymbolName();
      int idx = nm.lastIndexOf(".");
      class_name = nm.substring(0,idx);
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<STATICS>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createStaticsFragmentEditor(
	 def_location.getSymbolProject(),class_name,def_location.getFile());
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class InitializerStackEntry



private class MainProgramStackEntry extends GenericStackEntry {
   
   private String class_name;
   
   MainProgramStackEntry(List<BumpLocation> locs) {
      super(locs);
      String nm = def_location.getSymbolName();
      int idx = nm.lastIndexOf(".");
      class_name = nm.substring(0,idx);
    }
   
   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<MAIN>";
    }
   
   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createStaticsFragmentEditor(
            def_location.getSymbolProject(),class_name,def_location.getFile());
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }
   
}	// end of inner class MainProgramStackEntry



private class TypeStackEntry extends GenericStackEntry {

   private String class_name;

   TypeStackEntry(List<BumpLocation> locs) {
      super(locs);
      class_name = def_location.getSymbolName();
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<PREFIX>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createClassPrefixFragmentEditor(
	 def_location.getSymbolProject(),class_name);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class TypeStackEntry




/********************************************************************************/
/*										*/
/*	Classes for maintaining bubble windows as part of the bubble stack	*/
/*										*/
/********************************************************************************/
/**************** now done in BussStackBox
 * 
private static class EditorBubbleCallback implements BubbleViewCallback {

   private static final double DISTANCE_LIMIT = 100;
   private BussBubble buss_bubble;

   EditorBubbleCallback(BussBubble bussBubble){
      buss_bubble = bussBubble;
    }

   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }
   @Override public void bubbleAdded(BudaBubble bb)			{ }
   @Override public void bubbleRemoved(BudaBubble bb)			{ }
   @Override public void workingSetAdded(BudaWorkingSet ws)		{ }
   @Override public void workingSetRemoved(BudaWorkingSet ws)		{ }
   @Override public void doneConfiguration()				{ }
   @Override public void copyFromTo(BudaBubble f,BudaBubble t)		{ }

   @Override public boolean bubbleActionDone(BudaBubble bb) {
      if (!(bb instanceof BaleEditorBubble)) return false;

      BaleEditorBubble editorbubble = (BaleEditorBubble) bb;

      if (buss_bubble.getEditorBubble() != editorbubble) return false;

      Point editorloc = editorbubble.getLocation();
      Point originallocation = buss_bubble.getEditorBubbleLocation();

      if (editorloc == null || originallocation == null) return false;

      double distance = Point.distance(editorloc.x, editorloc.y, originallocation.x, originallocation.y);

      if (distance <= DISTANCE_LIMIT) {
	 buss_bubble.updateEditorBubbleLocation();
	 return true;
       }
      else {
	 buss_bubble.setPreviewBubble(null);

	 editorbubble.setFixed(false);

	 buss_bubble.removeEditorBubble();
       }

      return false;
   }

}	// end of inner class EditorBubbleCallback

***************************/


}	// end of class BaleBubbleStack



/* end of BaleBubbleStack.java */

