/********************************************************************************/
/*										*/
/*		BudaPlacement.java						*/
/*										*/
/*	BUblles Display Area common code for locating bubbles			*/
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

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;



import java.awt.*;
import java.util.*;
import java.util.List;


/**
 *	This class holds a variety of enumeration types and constants that define
 *	the basic properties of the bubble display.  It can be changed and then
 *	the system recompiled.	Most of these constants should be made into
 *	properties and read from a property file so they can be changed by the
 *	user without recompiling.
 *
 **/

class BudaPlacement implements BudaConstants, BudaConstants.BubbleViewCallback {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea			bubble_area;
private BudaBubble			last_placement;
private Map<BudaBubble,List<BudaBubble>> related_bubbles;
private BudaBubble			last_focus;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaPlacement(BudaBubbleArea bba)
{
   bubble_area = bba;
   last_placement = null;
   related_bubbles = new HashMap<BudaBubble,List<BudaBubble>>();
   last_focus = null;
   BudaRoot.addBubbleViewCallback(this);
}



/********************************************************************************/
/*										*/
/*	Actually placement routines							     */
/*										*/
/********************************************************************************/

void placeBubble(BudaBubble bbl,Component rcom,Point relpt,int place,BudaBubblePosition pos)
{
   BudaBubble rel = null;
   BudaBubble grpb = null;
   if (rcom != null) rel = BudaRoot.findBudaBubble(rcom);


   Rectangle r = new Rectangle();
   if (rel != null) {
      r = BudaRoot.findBudaLocation(rel);
      grpb = rel;
    }
   else if (relpt != null) {
      r.setLocation(relpt);
    }
   else if (last_focus != null) {
      r = BudaRoot.findBudaLocation(last_focus);
      grpb = last_focus;
    }
   else if (last_placement != null) {
      r = BudaRoot.findBudaLocation(last_placement);
      grpb = last_placement;
    }

   if ((place & PLACEMENT_ADJACENT) == 0) {
      expandForGroup(r,grpb);
    }


   Rectangle r1 = null;
   if (rel != null) {
      synchronized (related_bubbles) {
	 List<BudaBubble> ls = related_bubbles.get(rel);
	 if (ls != null && !ls.isEmpty()) {
	    BudaBubble bbx = ls.get(ls.size()-1);
	    r1 = BudaRoot.findBudaLocation(bbx);
	  }
       }
    }

   if ((place & PLACEMENT_LOGICAL) != 0) {
      BudaRoot br = BudaRoot.findBudaRoot(bubble_area);
      Rectangle r2 = br.getViewport();
      if (r.x - BUBBLE_CREATION_SPACE < r2.x) {
	 if (r.x + r.width + BUBBLE_CREATION_SPACE < r2.x + r2.width)
	    place |= PLACEMENT_RIGHT;
       }
      if (r.x + r.width + BUBBLE_CREATION_SPACE > r2.x + r2.width) {
	 if (r.x - BUBBLE_CREATION_SPACE > r2.x)
	    place |= PLACEMENT_LEFT;
       }
      if (r.y - BUBBLE_CREATION_SPACE < r2.y) {
	 if (r.y + r.height + BUBBLE_CREATION_SPACE < r2.y + r2.height)
	    place |= PLACEMENT_BELOW;
       }
      if (r.y + r.height + BUBBLE_CREATION_SPACE > r2.y + r2.height) {
	 if (r.y - BUBBLE_CREATION_SPACE > r2.y)
	    place |= PLACEMENT_ABOVE;
       }
    }

   Dimension sz = bbl.getSize();
   int x0;
   int y0;
   int delta;

   if ((place & PLACEMENT_GROUPED) != 0) delta = BUBBLE_CREATION_NEAR_SPACE;
   else delta = BUBBLE_CREATION_SPACE;

   if (rel == null) {
      x0 = r.x;
      y0 = r.y;
    }
   else if ((place & PLACEMENT_RIGHT) != 0) {
      x0 = r.x + r.width + delta;
      y0 = r.y;
      if (relpt != null) y0 = relpt.y;
    }
   else if ((place & PLACEMENT_LEFT) != 0) {
      x0 = r.x - sz.width - delta;
      y0 = r.y;
      if (relpt != null) y0 = relpt.y;
    }
   else if ((place & PLACEMENT_BELOW) != 0) {
      x0 = r.x;
      y0 = r.y + r.height + delta;
      if (relpt != null) x0 = relpt.x;
    }
   else if ((place & PLACEMENT_ABOVE) != 0) {
      x0 = r.x;
      y0 = r.y - sz.height - delta;
      if (relpt != null) x0 = relpt.x;
    }
   else {
      x0 = r.x + r.width + delta;
      y0 = r.y;
      if (relpt != null) y0 = relpt.y;
    }

   if (r1 != null) {
      if (x0 == r1.x && y0 == r1.y) {
	 if ((place & (PLACEMENT_RIGHT|PLACEMENT_LEFT)) != 0) y0 += delta;
	 else if ((place & (PLACEMENT_ABOVE|PLACEMENT_BELOW)) != 0) x0 += delta;
	 else {
	    x0 += delta;
	    y0 += delta;
	  }
       }
    }

   if (rel != null) {
      synchronized (related_bubbles) {
	 List<BudaBubble> ls = related_bubbles.get(rel);
	 if (ls == null) {
	    ls = new ArrayList<BudaBubble>();
	    related_bubbles.put(rel,ls);
	  }
	 ls.add(bbl);
       }
    }

   bubble_area.addBubble(bbl,pos,x0,y0);

   if ((place & PLACEMENT_MOVETO) != 0) {
      bubble_area.scrollBubbleVisible(bbl);
    }
}




/********************************************************************************/
/*										*/
/*	Methods for dealing with groups 					*/
/*										*/
/********************************************************************************/

private static final int	GROUP_SPACE = 100;
private static final int	GROUP_LEEWAY = 40;


private void expandForGroup(Rectangle r,BudaBubble grpb)
{
   if (grpb == null) return;

   Set<BudaBubble> used = new HashSet<BudaBubble>();
   used.add(grpb);

   BudaBubbleGroup grp = grpb.getGroup();
   if (grp == null) return;

   boolean chng = true;
   while (chng) {
      chng = false;
      Rectangle rhor = new Rectangle(r.x - GROUP_SPACE,r.y + GROUP_LEEWAY,
					r.width + 2*GROUP_SPACE,r.height - 2*GROUP_LEEWAY);
      Rectangle rver = new Rectangle(r.x + GROUP_LEEWAY,r.y - GROUP_SPACE,
					r.width - 2*GROUP_LEEWAY,r.height + 2*GROUP_SPACE);

      for (BudaBubble gb : grp.getBubbles()) {
	 if (used.contains(gb)) continue;
	 Rectangle r1 = BudaRoot.findBudaLocation(gb);
	 if (r1.intersects(rhor)) {
	    int lx = Math.min(r.x,r1.x);
	    int rx = Math.max(r.x + r.width, r1.x + r1.width);
	    r.x = lx;
	    r.width = rx-lx;
	    chng = true;
	    used.add(gb);
	    break;
	  }
	 else if (r1.intersects(rver)) {
	    int ty = Math.min(r.y,r1.y);
	    int by = Math.max(r.y + r.height, r1.y + r1.height);
	    r.y = ty;
	    r.height = by-ty;
	    chng = true;
	    used.add(gb);
	    break;
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Bubble area callback methods						*/
/*										*/
/********************************************************************************/

@Override public void focusChanged(BudaBubble bb,boolean set)
{
   if (BudaRoot.findBudaBubbleArea(bb) == bubble_area && set)
      last_focus = bb;
}



@Override public void bubbleAdded(BudaBubble bb)		{ }
@Override public boolean bubbleActionDone(BudaBubble bb)	{ return false; }

@Override public void bubbleRemoved(BudaBubble bb)
{
   if (BudaRoot.findBudaBubbleArea(bb) == bubble_area) {
      if (bb == last_focus) last_focus = null;
      synchronized (related_bubbles) {
	 related_bubbles.remove(bb);
	 for (List<BudaBubble> ls : related_bubbles.values()) {
	    ls.remove(bb);
	  }
       }
    }
}




}	// end of class BudaPlacement




/* end of BudaPlacement.java */
