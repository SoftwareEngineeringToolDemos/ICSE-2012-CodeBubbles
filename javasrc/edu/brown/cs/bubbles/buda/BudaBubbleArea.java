/********************************************************************************/
/*										*/
/*		BudaBubbleArea.java						*/
/*										*/
/*	BUblles Display Area bubble panel					*/
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;



/**
 *	This class represents the bubble area, the portion of the overall display
 *	in which bubbles are presented.  The bubble area is typically a very large
 *	canvas that is scrolled over using the overview bar or panning commands
 *	inside the canvas.  This is created automatically.  Most clients do not
 *	need to access this class directly, since most of the actions on it such
 *	as adding bubbles can be done in terms of the root, BudaBubbleRoot.
 *
 **/

public class BudaBubbleArea extends JLayeredPane implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot	  for_root;
private BudaLayoutManager layout_manager;
private BubbleManager	  bubble_manager;
private BudaMoveAnimator  move_animator;
private BudaPlacement	  bubble_placer;
private SwingEventListenerList<BubbleAreaCallback> area_callbacks;
private MouseRegion	  last_mouse;
private MouseContext	  mouse_context;
private AutoScroller	  auto_scroller;
private double		  scale_factor;
private Color		  top_color;
private Color		  bottom_color;
private Color		  middle_color;
private boolean 	  routes_valid;
private BudaBubble	  focus_bubble;
private BudaChannelSet	  channel_set;
private Cursor		  palm_cursor;
private boolean 	  first_time;

private Map<BudaBubble,Point> floating_bubbles;
private Map<BudaBubble,BudaBubbleDock[]> docked_bubbles;
private Rectangle	  cur_viewport;

private Collection<BudaBubbleGroup> bubble_groups;
private Collection<BudaBubble> active_bubbles;
private Collection<BudaBubbleLink> bubble_links;
private Collection<BudaWorkingSetImpl> working_sets;
private Collection<BudaBubble> moving_bubbles;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaBubbleArea(BudaRoot br,Element cfg,BudaChannelSet cs)
{
   for_root = br;
   layout_manager = new BudaLayoutManager();
   setLayout(layout_manager);
   setOpaque(true);
   first_time = true;

   active_bubbles = new HashSet<BudaBubble>();
   bubble_groups = new HashSet<BudaBubbleGroup>();
   bubble_links = new ArrayList<BudaBubbleLink>();
   working_sets = new ArrayList<BudaWorkingSetImpl>();
   moving_bubbles = new HashSet<BudaBubble>();
   channel_set = cs;

   floating_bubbles = new HashMap<BudaBubble,Point>();
   docked_bubbles = new HashMap<BudaBubble,BudaBubbleDock[]>();
   cur_viewport = null;
   routes_valid = false;
   focus_bubble = null;

   bubble_manager = new BubbleManager();
   addContainerListener(bubble_manager);

   move_animator = new BudaMoveAnimator();
   bubble_placer = new BudaPlacement(this);

   area_callbacks = new SwingEventListenerList<BubbleAreaCallback>(BubbleAreaCallback.class);

   mouse_context = null;
   Mouser mm = new Mouser();
   addMouseListener(mm);
   addMouseMotionListener(mm);
   addMouseWheelListener(mm);
   last_mouse = null;
   auto_scroller = new AutoScroller();
   top_color = DISPLAY_TOP_COLOR;
   bottom_color = DISPLAY_BOTTOM_COLOR;
   middle_color = new Color((top_color.getRed() + bottom_color.getRed())/2,
			       (top_color.getGreen() + bottom_color.getGreen())/2,
			       (top_color.getBlue() + bottom_color.getBlue())/2);

   Element shape = IvyXml.getChild(cfg,"SHAPE");
   int w = (int) IvyXml.getAttrDouble(shape,"WIDTH",BUBBLE_DISPLAY_WIDTH);
   int h = (int) IvyXml.getAttrDouble(shape,"HEIGHT",BUBBLE_DISPLAY_HEIGHT);
   int w0 = IvyXml.getAttrInt(cfg,"MAXX") + 100;
   int h0 = IvyXml.getAttrInt(cfg,"MAXY") + 100;
   if (w > BUBBLE_DISPLAY_WIDTH && w > w0) w = Math.max(w0,BUBBLE_DISPLAY_WIDTH);
   if (h > BUBBLE_DISPLAY_HEIGHT && h > h0) h = Math.max(h0,BUBBLE_DISPLAY_HEIGHT);

   setSize(w,h);

   setTransferHandler(new BubbleDropper());

   scale_factor = 1.0;

   Toolkit t = getToolkit();
   Dimension d = t.getBestCursorSize(32,32);
   if(d.width == 32 && d.height == 32) palm_cursor= t.createCustomCursor(BoardImage.getImage("palm3"), new Point(16,16), "PALM_CURSOR");
   else palm_cursor = new Cursor(Cursor.MOVE_CURSOR);
}



/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

/**
 *	Convenience method for adding a bubble to the bubble area.  This would
 *	normally be done by add(Component,BubdaConstraint) on either the root
 *	(assuming Component is a BudaBubble) or on the bubble area.  In the latter
 *	case, if the component is not a bubble, a default bubble enclosing it is
 *	created.
 **/

public void addBubble(BudaBubble bb,int x,int y)
{
   add(bb,new BudaConstraint(x,y));
}


public void addBubble(BudaBubble bb,BudaBubblePosition pos,int x,int y)
{
   add(bb,new BudaConstraint(pos,x,y));
}


public void addBubble(BudaBubble bb,Component rel,Point relpt,int placement)
{
   bubble_placer.placeBubble(bb,rel,relpt,placement,BudaBubblePosition.MOVABLE);
}



public void addBubble(BudaBubble bb,Component rel,Point relpt,int placement,BudaBubblePosition pos)
{
   bubble_placer.placeBubble(bb,rel,relpt,placement,pos);
}



/**
 *	Convenience method for removing a bubble.  It is sufficient to either set
 *	the bubble as invisible or reset its parent.
 **/


public void removeBubble(BudaBubble bb)
{
   bb.setVisible(false);
   remove(bb);
}



/**
 *	Return the set of active bubbles
 **/

public Iterable<BudaBubble> getBubbles()
{
   synchronized (active_bubbles) {
      return new ArrayList<BudaBubble>(active_bubbles);
    }
}

public Iterable<BudaBubbleGroup> getBubbleGroups()
{
   synchronized (bubble_groups) {
      return new ArrayList<BudaBubbleGroup>(bubble_groups);
    }
}

/**
 *	Return all bubbles in a specified region
 **/

public Collection<BudaBubble> getBubblesInRegion(Rectangle r)
{
   Collection<BudaBubble> rslt = new ArrayList<BudaBubble>();

   synchronized (active_bubbles) {
      for (BudaBubble bb : active_bubbles) {
	 if (bb.getBounds().intersects(r)) rslt.add(bb);
       }
    }

   return rslt;
}




void removeCurrentBubble(MouseEvent e)
{
   MouseRegion mr = last_mouse;

   if (e != null) mr = new MouseRegion(e);

   if (mr == null) return;

   BudaBubble bb = mr.getBubble();
   BudaBubbleGroup grp = mr.getGroup();

   if (bb == null && focus_bubble != null) bb = focus_bubble;

   if (bb != null) userRemoveBubble(bb);
   else if (grp != null) userRemoveGroup(grp);
}



/********************************************************************************/
/*										*/
/*	Methods for handling add and remove of bubbles				*/
/*										*/
/********************************************************************************/

@Override protected void addImpl(Component c,Object cnst,int idx)
{
   boolean fixed = false;
   BudaBubble bb = null;

   if (c instanceof BudaBubble) {
      bb = (BudaBubble) c;
    }
   else if (!(c instanceof NoBubble)) {
      c = BudaBubble.createInternalBubble(c);
      fixed = true;
      bb = (BudaBubble) c;
    }
   else {
      super.addImpl(c,cnst,idx);
      return;
    }

   if (cnst != null && !(cnst instanceof BudaConstraint)) {
      throw new IllegalArgumentException("BudaConstraint expected");
    }

   BudaConstraint bc = (BudaConstraint) cnst;
   Point loc = null;

   if (bc == null || bc.getPositionType() == BudaBubblePosition.NONE) {
      if (last_mouse != null) loc = last_mouse.getLocation();
      else if (cur_viewport != null) {
	 loc = new Point(cur_viewport.x + cur_viewport.width/2 - c.getWidth()/2,
			    cur_viewport.y + cur_viewport.height/2 - c.getHeight()/2);
       }
      else loc = new Point(0,0);
    }
   else {
      loc = bc.getLocation();
      Point floc = new Point(loc);
      if (cur_viewport != null) {
	 floc.x -= cur_viewport.x;
	 floc.y -= cur_viewport.y;
       }
      if (bc.getPositionType() == BudaBubblePosition.FIXED) {
	 fixed = true;
       }
      else if (bc.getPositionType() == BudaBubblePosition.HOVER) {
	 fixed = true;
	 setLayer(bb,MODAL_LAYER+3);
       }
      else if (bc.getPositionType() == BudaBubblePosition.FLOAT) {
	 fixed = false;
	 floating_bubbles.put(bb,floc);
	 loc = setFloatingLocation(bb);
	 bb.setFloating(true);
	 setLayer(bb,MODAL_LAYER);
       }
      else if (bc.getPositionType() == BudaBubblePosition.USERPOS) {
	 fixed = true;
	 setLayer(bb,MODAL_LAYER+4);
       }
      else if (bc.getPositionType() == BudaBubblePosition.DIALOG) {
	 fixed = false;
	 floating_bubbles.put(bb,floc);
	 loc = setFloatingLocation(bb);
	 bb.setFloating(true);
	 setLayer(bb,MODAL_LAYER+2);
       }
      else if (bc.getPositionType() == BudaBubblePosition.STATIC) {
	 floating_bubbles.put(bb,floc);
	 loc = setFloatingLocation(bb);
	 bb.setFloating(true);
	 setLayer(bb,MODAL_LAYER);
       }
      else if (bc.getPositionType() == BudaBubblePosition.DOCKED) {
	 floating_bubbles.put(bb, floc);
	 loc = setFloatingLocation(bb);
	 bb.setFloating(true);
	 BudaBubbleDock[] bbda= {BudaBubbleDock.NORTH, BudaBubbleDock.SOUTH, BudaBubbleDock.EAST};
	 docked_bubbles.put(bb, bbda);//getProperDock(bb));
	 bb.setDocked(true);
	 setLayer(bb,MODAL_LAYER);
      }
    }

   c.setLocation(loc);
   if (bb != null && fixed) bb.setFixed(fixed);

   boolean added = false;
   if (idx < 0) {
      // This should always work if idx < 0; if it fails, retry a few time
      for (int i = 0; i < 3; ++i) {
	 try {
	    super.addImpl(bb,null,idx);
	    added = true;
	    break;
	  }
	 catch (IllegalArgumentException e) { }
       }
    }

   try {
      if (!added) super.addImpl(bb,null,idx);
    }
   catch (IllegalArgumentException e) {
      BoardLog.logE("BUDA","Problem with window creation " + idx + " " + loc + " " + bb.isFixed() + " " +
		       bc.getPositionType(),e);
    }

   BoardMetrics.noteCommand("BUDA","addBubble_" + bb.getClass().getName() + "_" + bb.getHashId());

   bb.addComponentListener(bubble_manager);
}




private void localAddBubble(BudaBubble bb,boolean spacer)
{
   synchronized (active_bubbles) {
      active_bubbles.add(bb);
    }

   // routes_valid = false;		// if we take bubbles into account when routing

   if (spacer) fixupBubble(bb);

   fixupGroups(bb);

   for_root.noteBubbleAdded(bb);
}




private void localRemoveBubble(BudaBubble bb)
{
   BudaBubbleGroup grp = bb.getGroup();

   bb.setGroup(null);
   bb.removeComponentListener(bubble_manager);
   synchronized (active_bubbles) {
      active_bubbles.remove(bb);
    }

   synchronized (bubble_links) {
      for (Iterator<BudaBubbleLink> it = bubble_links.iterator(); it.hasNext(); ) {
	 BudaBubbleLink bl = it.next();
	 if (bl.usesBubble(bb)) {
	    it.remove();
	    bl.noteRemoved();
	  }
       }
    }

   // routes_valid = false;	// if we take bubbles into account when routing

   if (grp != null) checkGroup(grp);

   repaint();

   for_root.noteBubbleRemoved(bb);
}



/********************************************************************************/
/*										*/
/*	Link methods								*/
/*										*/
/********************************************************************************/

/**
 *	Force rerouting
 **/

public void forceReroute()
{
   routes_valid = false;
   repaint();
}



/**
 *	Force rerouting is this bubble has links
 **/

public void forceReroute(BudaBubble bb)
{
   boolean fnd = false;
   synchronized (bubble_links) {
      for (BudaBubbleLink bl : bubble_links) {
	 if (bb == bl.getTarget() || bb  == bl.getSource()) {
	    fnd = true;
	    break;
	  }
       }
    }

   if (fnd) forceReroute();
}



/**
 *	Add a link between bubbles
 **/

public void addLink(BudaBubbleLink lnk)
{
   if (lnk == null) return;
   BudaBubble src = lnk.getSource();
   if (src.isFloating() || lnk.getTarget().isFloating()) return;

   synchronized (bubble_links) {
      bubble_links.add(lnk);
    }

   routes_valid = false;

   repaint();
}



void removeLink(BudaBubbleLink lnk)
{
   synchronized (bubble_links) {
      bubble_links.remove(lnk);
    }

   lnk.noteRemoved();

   repaint();
}

/**
 * A method to make sure that all bubble links are recalculated during the next repaint
 */
public void invalidateLinks() { routes_valid = false; }


List<BudaBubbleLink> getAllLinks()
{
   synchronized (bubble_links) {
      return new ArrayList<BudaBubbleLink>(bubble_links);
    }
}




Collection<BudaBubbleLink> getLinks(Set<BudaBubble> bbls)
{
   Collection<BudaBubbleLink> rslt = new ArrayList<BudaBubbleLink>();

   synchronized (bubble_links) {
      for (BudaBubbleLink bl : bubble_links) {
	 if (bbls.contains(bl.getTarget()) && bbls.contains(bl.getSource()))
	    rslt.add(bl);
       }
    }

   return rslt;
}




void setFocusBubble(BudaBubble bb,boolean fg)
{
   BudaBubble obb = focus_bubble;
   String okey = (focus_bubble == null ? null : focus_bubble.getContentKey());
   String nkey = (bb == null ? null : bb.getContentKey());

   if (fg) focus_bubble = bb;
   else focus_bubble = null;

   for (BudaBubble bbl : active_bubbles) {
      String key = bbl.getContentKey();
      if (bbl == obb || bbl == bb ||
	     (key != null && (key.equals(okey) || key.equals(nkey)))) {
	 bbl.repaint();
       }
    }

   focusLinks(bb,fg);

   for_root.noteFocusChanged(bb,fg);
}


BudaBubble getFocusBubble()
{
   return focus_bubble;
}



private void focusLinks(BudaBubble bb,boolean fg)
{
   synchronized (bubble_links) {
      for (BudaBubbleLink bl : bubble_links) {
	 if (bl.getTarget() == bb) {
	    if (bl.getHasFocus() != fg) {
	       bl.setHasFocus(fg);
	       repaint();
	       focusLinks(bl.getSource(),fg);
	     }
	  }
       }
    }
}



String getFocusKey()
{
   if (focus_bubble == null) return null;
   return focus_bubble.getContentKey();
}



/********************************************************************************/
/*										*/
/*	Working set methods							*/
/*										*/
/********************************************************************************/

BudaWorkingSetImpl defineWorkingSet(String lbl,Rectangle rgn)
{
   // Ensure this working set is unique
   for (BudaWorkingSetImpl ows : working_sets) {
      Rectangle or = ows.getRegion();
      if (or.intersects(rgn)) return null;
    }

   // ensure name is unique
   if (lbl != null) {
      String olbl = lbl;
      boolean chng = true;
      int copy = 2;
      while (chng) {
	 chng = false;
	 for (BudaWorkingSetImpl ows : working_sets) {
	    if (lbl.equals(ows.getLabel())) {
	       lbl = olbl + " (" + copy + ")";
	       ++copy;
	       chng = true;
	       break;
	     }
	  }
       }
    }

   int y = rgn.y;
   rgn.y = 0;
   rgn.height = getHeight();

   BudaWorkingSetImpl ws = new BudaWorkingSetImpl(this,lbl,rgn,y);
   working_sets.add(ws);
   repaint();

   for_root.noteWorkingSetAdded(ws);

   return ws;
}



void removeWorkingSet(BudaWorkingSetImpl ws)
{
   if (working_sets.remove(ws)) {
      for_root.noteWorkingSetRemoved(ws);
      repaint();
    }
}



public BudaWorkingSet findWorkingSetForBubble(BudaBubble bb)
{
   Rectangle r1 = BudaRoot.findBudaLocation(bb);

   for (BudaWorkingSetImpl ws : getWorkingSets()) {
      Rectangle r2 = ws.getRegion();
      if (r1.intersects(r2)) return ws;
    }

   return null;
}

/**
 * Add the working set with the given xml with the given offset to the area
 * @param xml
 * @param offset
 */
public void addWorkingSet(Element xml, int offset)
{
   BudaTask bt = new BudaTask(xml);
   bt.loadTask(this, offset);
}

/**
 * Add the working set represented by the given xml to the bubble area
 * @param xml
 */
public void addWorkingSet(Element xml)
{
   addWorkingSet(xml, 0);
}

Collection<BudaWorkingSetImpl> getWorkingSets()
{
   return new ArrayList<BudaWorkingSetImpl>(working_sets);
}



/********************************************************************************/
/*										*/
/*	Bubble movement handling						*/
/*										*/
/********************************************************************************/

void moveBubble(BudaBubble bb,Point loc)
{
   move_animator.moveBubble(bb,loc,true);
}



void fixupBubble(BudaBubble bb)
{
   if (bb.isFixed() || bb.isFloating() || bb.isUserPos()) return;

   BudaSpacer bs = new BudaSpacer(this,bb);

   bs.makeRoomFor(bb);
}

private BudaBubbleGroup getBubbleGroup(BudaBubble bb)
{
   if (bb == null) return null;

   synchronized (bubble_groups) {
      for (BudaBubbleGroup bg : bubble_groups) {
	 if (bg.getBubbles().contains(bb)) return bg;
       }
    }

   return null;
}


void fixupBubbleGroup(BudaBubble bb)
{
   BudaBubbleGroup bg = getBubbleGroup(bb);

   if (bg == null) return;

   BudaGroupSpacer gs = new BudaGroupSpacer(this, bg);

   gs.makeRoomFor(bg);
}




void checkAreaDimensions()
{
   if (moving_bubbles.size() > 0) return;
   if (move_animator.isActive()) return;

   Component [] cnts = getComponents();
   Dimension osz = getSize();
   Rectangle r = null;
   int minx = 0;
   int maxx = 0;
   int miny = 0;
   int maxy = 0;

   for (Component c : cnts) {
      r = c.getBounds(r);
      if (r.x < minx) minx = r.x;
      if (r.x + r.width > maxx) maxx = r.x + r.width;
      if (r.y < miny) miny = r.y;
      if (r.y + r.height > maxy) maxy = r.y + r.height;
   }

   int deltax = 0;
   int deltay = 0;
   if (minx < 0) {
      deltax = -minx;
      maxx += deltax;
   }
   if (miny < 0) {
      deltay = -miny;
      maxy += deltay;
   }
   if (deltax > 0 || deltay > 0) {
      for (Component c : cnts) {
	 if (c instanceof BudaBubble) {
	    BudaBubble bb = (BudaBubble) c;
	    if (bb.isFloating()) continue;
	 }
	 r = c.getBounds(r);
	 r.x += deltax;
	 r.y += deltay;
	 c.setBounds(r);
      }
      if (!auto_scroller.isRunning())
	 for_root.moveCurrentViewport(deltax,deltay);
   }
   if (maxx > osz.width || maxy > osz.height) {
      osz.width = Math.max(osz.width,maxx);
      osz.height = Math.max(osz.height,maxy);
      setSize(osz);
   }
}





/********************************************************************************/
/*										*/
/*	Scaling methods 							*/
/*										*/
/********************************************************************************/

double getScaleFactor() 			{ return scale_factor; }

void setScaleFactor(double sf)
{
   scale_factor = sf;
   repaint();
}




/********************************************************************************/
/*										*/
/*	Color methods								*/
/*										*/
/********************************************************************************/

void setColors(Color top,Color bottom)
{
   top_color = top;
   bottom_color = bottom;
   middle_color = new Color((top_color.getRed() + bottom_color.getRed())/2,
			       (top_color.getGreen() + bottom_color.getGreen())/2,
			       (top_color.getBlue() + bottom_color.getBlue())/2);
}




/********************************************************************************/
/*										*/
/*	Channel set methods							*/
/*										*/
/********************************************************************************/

public boolean isPrimaryArea()			{ return channel_set == null; }


BudaChannelSet getChannelSet()			{ return channel_set; }




/********************************************************************************/
/*										*/
/*	Region methods								*/
/*										*/
/********************************************************************************/

/**
 *	Compute screen region including given window (or what is displayed)
 **/

public Rectangle computeRegion(Component base)
{
   Rectangle rloc = null;
   if (base != null) {
      rloc = BudaRoot.findBudaLocation(base);
      if (rloc == null) return null;
    }
   else rloc = new Rectangle(for_root.getViewport());

   return computeRegion(rloc);
}



public Rectangle computeRegion(Rectangle rloc)
{
   int space = getRegionSpace();

   if (isPrimaryArea()) {
      // check if we are inside a working set and use it if so
      for (BudaConstants.BudaWorkingSet ws : for_root.getWorkingSets()) {
	 Rectangle r = ws.getRegion();
	 if (r != null && r.intersects(rloc)) {
	    return new Rectangle(r);
	  }
       }
    }

   int left = Math.max(0,rloc.x - space);
   int right = rloc.x + rloc.width + space;
   boolean chng = true;
   while (chng) {
      chng = false;
      for (BudaBubble bb : getBubbles()) {
	 Rectangle bloc = BudaRoot.findBudaLocation(bb);
	 if (bloc != null && bloc.x <= right && bloc.x + bloc.width >= left) {
	    int l0 = Math.max(0,bloc.x - space);
	    int r0 = bloc.x + bloc.width + space;
	    if (l0 < left) {
	       left = l0;
	       chng = true;
	    }
	    if (r0 > right) {
	       right = r0;
	       chng = true;
	    }
	 }
      }
   }

   Rectangle r0 = new Rectangle();
   r0.x = left;
   r0.width = right-left+1;
   r0.y = 0;
   r0.height = getHeight();

   return r0;
}


public int getRegionSpace()
{
   Rectangle rview = new Rectangle(for_root.getViewport());
   return Math.max(rview.width/2,768);
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   validate();

   if (first_time) {
      first_time = false;
      synchronized (active_bubbles) {
	 for (Component c : getComponents()) {
	    if (c instanceof BudaBubble) {
	       active_bubbles.add(((BudaBubble) c));
	     }
	  }
       }
    }

   if (scale_factor != 1.0 && g instanceof Graphics2D) {
      Graphics2D g1 = (Graphics2D) g.create();
      g1.scale(scale_factor,scale_factor);
      super.paint(g1);
    }
   else {
      super.paint(g);
    }
}




@Override protected void paintComponent(Graphics g0)
{
   Graphics2D g2 = (Graphics2D) g0.create();

   Dimension sz = getSize();

   Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
   // Paint p = new GradientPaint(0f,0f,top_color,0f,sz.height,bottom_color);
   // g2.setPaint(p);
   g2.setColor(middle_color);
   g2.fill(r);

   for (BudaWorkingSetImpl ws : working_sets) {
      Rectangle wr = ws.getRegion();
      Paint ps = new GradientPaint(wr.x,wr.y,ws.getTopColor(),
				      wr.x+wr.width,wr.y+wr.height,ws.getBottomColor());
      g2.setPaint(ps);
      g2.fill(wr);
    }

   synchronized (bubble_groups) {
      for (BudaBubbleGroup bg : bubble_groups) {
	 g2 = (Graphics2D) g0.create();
	 bg.drawGroup(g2,false);
       }
    }

   if (!routes_valid) {
      routes_valid = true;
      BudaRouter br = new BudaRouter(this);
      br.computeRoutes();
    }

   synchronized (bubble_links) {
      for (BudaBubbleLink bl : bubble_links) {
	 if (!bl.getHasFocus()) {
	    g2 = (Graphics2D) g0.create();
	    bl.drawLink(g2,false);
	  }
       }
      for (BudaBubbleLink bl : bubble_links) {
	 if (bl.getHasFocus()) {
	    g2 = (Graphics2D) g0.create();
	    bl.drawLink(g2,false);
	  }
       }
    }
}



void paintOverview(Graphics2D g)
{
   for (BudaWorkingSetImpl ws : working_sets) {
      Rectangle wr = ws.getRegion();
      Paint ps = new GradientPaint(wr.x,wr.y,ws.getTopColor(),
				      wr.x+wr.width,wr.y+wr.height,ws.getBottomColor());
      g.setPaint(ps);
      g.fill(wr);
      if (ws.isBeingChanged()) {
	 g.setColor(ws.getBorderColor());
	 g.draw(wr);
       }
    }

   synchronized (bubble_groups) {
      for (BudaBubbleGroup bg : bubble_groups) {
	 bg.drawGroup(g,true);
       }
    }

   synchronized (bubble_links) {
      for (BudaBubbleLink bl : bubble_links) {
	 if (!bl.getHasFocus()) bl.drawLink(g,true);
       }
      for (BudaBubbleLink bl : bubble_links) {
	 if (bl.getHasFocus()) bl.drawLink(g,true);
       }
    }

   synchronized (active_bubbles) {
      for (BudaBubble bb : active_bubbles) {
	 Graphics2D g1 = (Graphics2D) g.create();
	 Point loc = bb.getLocation();
	 g1.translate(loc.x,loc.y);
	 bb.paintOverview(g1);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Action requests 							*/
/*										*/
/********************************************************************************/

boolean handleQuitRequest()
{
   for (BudaBubble bb : getBubbles()) {
      if (!bb.handleQuitRequest()) return false;
    }

   return true;
}



void handleSaveAllRequest()
{
   for (BudaBubble bb : getBubbles()) {
      bb.handleSaveRequest();
    }
}



void handleCheckpointRequest()
{
   for (BudaBubble bb : getBubbles()) {
      bb.handleCheckpointRequest();
    }
}



public Point getCurrentMouse()
{
   if (last_mouse == null) return null;

   return last_mouse.getLocation();
}



/********************************************************************************/
/*										*/
/*	Callback registration methods						*/
/*										*/
/********************************************************************************/

public void addBubbleAreaCallback(BubbleAreaCallback cb)
{
   area_callbacks.add(cb);
}



public void removeBubbleAreaCallback(BubbleAreaCallback cb)
{
   area_callbacks.remove(cb);
}



/********************************************************************************/
/*										*/
/*	Configuration methods							*/
/*										*/
/********************************************************************************/

void configure(Element xml,Rectangle delta)
{
   Map<String,BudaBubble> bubblemap = new HashMap<String,BudaBubble>();

   Element bbls = IvyXml.getChild(xml,"BUBBLES");
   for (Element bbl : IvyXml.children(bbls,"BUBBLE")) {
      BudaBubble bb = for_root.createBubble(this,bbl,delta);
      if (bb != null) bubblemap.put(IvyXml.getAttrString(bbl,"ID"),bb);
    }

   Element grps = IvyXml.getChild(xml,"GROUPS");
   for (Element egrp : IvyXml.children(grps,"GROUP")) {
      // The groups should be automatically configured by distance at this point
      BudaBubbleGroup grp = null;
      for (Element ebbl : IvyXml.children(egrp,"BUBBLE")) {
	 String id = IvyXml.getAttrString(ebbl,"ID");
	 BudaBubble bbl = bubblemap.get(id);
	 if (bbl != null) {
	    grp = bbl.getGroup();
	    if (grp != null) break;
	  }
       }
      if (grp != null) {
	 Color lc = IvyXml.getAttrColor(egrp,"LEFTCOLOR");
	 Color rc = IvyXml.getAttrColor(egrp,"RIGHTCOLOR");
	 grp.setColor(lc,rc);
	 String ttl = IvyXml.getTextElement(egrp,"TITLE");
	 grp.setTitle(ttl);
       }
    }

   Element lnks = IvyXml.getChild(xml,"LINKS");
   for (Element lnk : IvyXml.children(lnks,"LINK")) {
      boolean rect = IvyXml.getAttrBool(lnk,"RECT");
      Element flnk = IvyXml.getChild(lnk,"FROM");
      Element tlnk = IvyXml.getChild(lnk,"TO");
      BudaLinkStyle sty = IvyXml.getAttrEnum(lnk,"STYLE",BudaLinkStyle.STYLE_SOLID);
      BudaBubble fbbl = bubblemap.get(IvyXml.getAttrString(flnk,"ID"));
      BudaBubble tbbl = bubblemap.get(IvyXml.getAttrString(tlnk,"ID"));
      if (fbbl != null && tbbl != null) {
	 LinkPort fprt = for_root.createPort(fbbl,IvyXml.getChild(flnk,"PORT"));
	 LinkPort tprt = for_root.createPort(tbbl,IvyXml.getChild(tlnk,"PORT"));
	 if (fprt != null && tprt != null) {
	    BudaBubbleLink blnk = new BudaBubbleLink(fbbl,fprt,tbbl,tprt,rect,sty);
	    for_root.addLink(blnk);
	  }
	 else {
	    if (fprt != null) fprt.noteRemoved();
	    if (tprt != null) tprt.noteRemoved();
	  }
       }
    }

   Element wsets = IvyXml.getChild(xml,"WORKINGSETS");
   for (Element wset : IvyXml.children(wsets,"WORKINGSET")) {
      String lbl = IvyXml.getTextElement(wset,"NAME");
      long ctime = IvyXml.getAttrLong(wset,"CREATE",0);
      Color c = IvyXml.getAttrColor(wset,"BORDERCOLOR");
      Element rgn = IvyXml.getChild(wset,"REGION");
      Rectangle r = new Rectangle((int) IvyXml.getAttrDouble(rgn,"X",0),
				     (int) IvyXml.getAttrDouble(rgn,"Y",0),
				     (int) IvyXml.getAttrDouble(rgn,"WIDTH",0),
				     (int) IvyXml.getAttrDouble(rgn,"HEIGHT",0));
      BudaWorkingSetImpl ws = defineWorkingSet(lbl,r);
      if (ws != null) {
	 ws.setColor(c);
	 if (ctime > 0) ws.setCreateTime(ctime);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   int maxx = 0;
   int maxy = 0;
   for (BudaBubble bb : getBubbles()) {
      if (!bb.isTransient()) {
	 Rectangle r = BudaRoot.findBudaLocation(bb);
	 if (r != null) {
	    if (r.x + r.width > maxx) maxx = r.x + r.width;
	    if (r.y + r.height > maxy) maxy = r.y + r.height;
	  }
       }
    }

   xw.begin("BUBBLEAREA");

   xw.field("MAXX",maxx);
   xw.field("MAXY",maxy);

   xw.element("SHAPE",getBounds());

   xw.begin("BUBBLES");
   for (BudaBubble bb : getBubbles()) {
      if (!bb.isTransient()) {
	 bb.outputBubbleXml(xw);
       }
    }
   xw.end("BUBBLES");

   xw.begin("GROUPS");
   synchronized (bubble_groups) {
      for (BudaBubbleGroup bg : bubble_groups) {
	 bg.outputXml(xw);
       }
    }
   xw.end("GROUPS");

   xw.begin("LINKS");
   synchronized (bubble_links) {
      for (BudaBubbleLink bl : bubble_links) {
	 bl.outputXml(xw);
       }
    }
   xw.end("LINKS");

   xw.begin("WORKINGSETS");
   for (BudaWorkingSetImpl ws : working_sets) {
      ws.outputXml(xw);
    }
   xw.end("WORKINGSETS");

   xw.end("BUBBLEAREA");
}




/********************************************************************************/
/*										*/
/*	Viewport handling							*/
/*										*/
/********************************************************************************/

void setViewPosition(Rectangle r)
{
   cur_viewport = new Rectangle(r);
   for (BudaBubble bb : floating_bubbles.keySet()) {
      setFloatingLocation(bb);
    }
   for (BudaBubble bb : docked_bubbles.keySet()) {
      setDockedLocation(bb);
   }
}

/**
 * get the position of upper left corner of the current viewport
 **/

public Point getViewPosition()
{
   Point p = cur_viewport.getLocation();

   return p;
}



/**
 * get the current viewport
 **/

public Rectangle getViewport()
{
   return new Rectangle(cur_viewport);
}



/**
 *	Ensure the given bubble is visible by scrolling appropriately
 **/

public void scrollBubbleVisible(BudaBubble bb)
{
   Rectangle rbubble = bb.getBounds();
   Rectangle rview = cur_viewport.getBounds();

   // adjust visible region to account for docked bubbles
   for (BudaBubble dbb : docked_bubbles.keySet()) {
      if (dbb.isFloating() && dbb.isShowing()) {
	 Rectangle dr = dbb.getBounds();
	 if (Math.abs(dr.y - rview.y) < 10 &&
		Math.abs(dr.height - rview.height) < 10) {
	    if (Math.abs(dr.x - rview.x) < 10) {
	       rview.x += dr.width;
	       rview.width -= dr.width;
	     }
	    else if (Math.abs(dr.x + dr.width - rview.x - rview.width) < 10) {
	       rview.width -= dr.width;
	     }
	  }
       }
    }

   // adjust bubble size so that it is reasonable wrt the viewport
   if (rbubble.height * 2 > rview.height) rbubble.height = rview.height/2;
   if (rbubble.width * 2 > rview.width) rbubble.width = rview.width/2;

   if (rview.contains(rbubble)) return;

   int tx = cur_viewport.x;
   int ty = cur_viewport.y;

   if (rbubble.x > rview.x && rbubble.x + rbubble.width > rview.x + rview.width) {
      int rpos = rbubble.x + rbubble.width + 5;
      tx = rpos - rview.width;
    }
   else if (rbubble.x < rview.x) {
      tx = rbubble.x - 5 + rview.x - cur_viewport.x;
    }

   if (rbubble.y > rview.y && rbubble.y + rbubble.height > rview.y + rview.height) {
      int bpos = rbubble.y + rbubble.height + 5;
      ty = bpos - rview.height;
    }
   else if (rbubble.y < rview.y) {
      ty = rbubble.y - 5 + rview.y - cur_viewport.y;
    }

   ScrollAnimator sa = new ScrollAnimator(tx,ty);
   sa.start();
}





private Point setFloatingLocation(BudaBubble bb)
{
   Point p = floating_bubbles.get(bb);
   if (p == null) return null;

   if (cur_viewport == null) {
      return p;
    }

   int x = p.x;
   int y = p.y;

   x += cur_viewport.x;
   y += cur_viewport.y;

   if (x < 0) x = 0;
   if (y < 0) y = 0;

   Point p0 = new Point(x,y);

   bb.setLocation(p0);

   return p0;
}




private class ScrollAnimator extends javax.swing.Timer implements ActionListener
{
   private int target_x;
   private int target_y;
   private int start_x;
   private int start_y;
   private long start_time;

   private static final long serialVersionUID = 1;

   ScrollAnimator(int tx,int ty) {
      super(SCROLL_ANIM_DELAY,null);

      target_x = tx;
      target_y = ty;
      start_time = System.currentTimeMillis();
      start_x = cur_viewport.x;
      start_y = cur_viewport.y;

      addActionListener(this);
      setActionCommand("AUTOSCROLL");
      setRepeats(true);

      setInitialDelay(0);
    }

   public void actionPerformed(ActionEvent e) {
      long now = System.currentTimeMillis();
      double t0 = (now - start_time);
      t0 /= SCROLL_ANIM_TOTAL;
      int dx = 0;
      int dy = 0;
      if (t0 > 1) {
	 dx = target_x - cur_viewport.x;
	 dy = target_y - cur_viewport.y;
	 stop();
       }
      else {
	 double t1 = -t0*t0 + 2 * t0;
	 int x0 = (int)((target_x - start_x)*t1) + start_x;
	 int y0 = (int)((target_y - start_y)*t1) + start_y;
	 dx = x0 - cur_viewport.x;
	 dy = y0 - cur_viewport.y;
       }
      if (Math.abs(dx) < SCROLL_ANIM_DELTA && Math.abs(dy) < SCROLL_ANIM_DELTA) {
	 dx = target_x - cur_viewport.x;
	 dy = target_y - cur_viewport.y;
       }
      if (dx == 0 && dy == 0) {
	 stop();
       }
      else {
	 for (BubbleAreaCallback cb : area_callbacks) {
	    cb.moveDelta(dx,dy);
	  }
       }
    }

}	// end of inner class ScrollAnimator







/********************************************************************************/
/*										*/
/*	Dock Handling methods  (written by Ian Strickman)			*/
/*										*/
/********************************************************************************/

private Point setDockedLocation(BudaBubble bb)
{
   BudaBubbleDock[] bbd = docked_bubbles.get(bb);
   if (bbd == null) return null;
   boolean havehadhoriz = false;
   boolean havehadvert = false;
   Point p = bb.getLocation();
   if (p == null) return null;
   for (int i = 0; i < bbd.length; i++) {
      BudaBubbleDock bbdi = bbd[i];
      if (bbdi == BudaBubbleDock.EAST) {
	 if (havehadhoriz) {
	    bb.setSize(cur_viewport.width, bb.getHeight());
	    bb.setLocation(new Point(cur_viewport.x,p.y));
	  }
	 else bb.setLocation(new Point(cur_viewport.x + cur_viewport.width
					  - bb.getWidth(),p.y));
	 havehadhoriz = true;
       }
      else if (bbdi == BudaBubbleDock.WEST) {
	 if (havehadhoriz) bb.setSize(cur_viewport.width, bb.getHeight());
	 bb.setLocation(new Point(cur_viewport.x,p.y));
	 havehadhoriz = true;
       }
      else if (bbdi == BudaBubbleDock.NORTH) {
	 if (havehadvert) bb.setSize(bb.getWidth(), cur_viewport.height);
	 bb.setLocation(new Point(p.x,cur_viewport.y));
	 havehadvert = true;
       }
      else if (bbdi == BudaBubbleDock.SOUTH) {
	 if (havehadvert) {
	    bb.setSize(bb.getWidth(), cur_viewport.height);
	    bb.setLocation(new Point(p.x,cur_viewport.y));
	  }
	 else bb.setLocation(new Point(p.x,cur_viewport.y + cur_viewport.height
					  - bb.getHeight()));
	 havehadvert = true;
       }
      p = bb.getLocation();
    }
   p.x -= cur_viewport.x;
   p.y -= cur_viewport.y;
   floating_bubbles.put(bb, p);
   return bb.getLocation();
}




/********************************************************************************/
/*										*/
/*	Floating bubble management						*/
/*										*/
/********************************************************************************/

// written by Ian Strickman
/**
 *	Make the given bubble float or not.  This should be used instead of
 *	BudaBubble.setFloating()
 **/

public boolean setBubbleFloating(BudaBubble bb, boolean fg)
{
   synchronized(active_bubbles) {
      if(!active_bubbles.contains(bb)) return false;
    }

   if (bb.isFloating() == fg && !bb.isUserPos()) return false;

   if (!fg) {
      bb.setGroup(bb.getNewGroup());
      bb.getGroup().addBubble(bb);
      addBubbleGroup(bb.getGroup());
      floating_bubbles.remove(bb);
      bb.setFloating(false);
      bb.setUserPos(false);
      bb.setFixed(false);
      setLayer(bb,DEFAULT_LAYER);
      checkGroup(bb.getGroup());
      fixupBubble(bb);
      fixupGroups(bb);
    }
   else {
      BudaBubbleGroup bbg = bb.getGroup();
      Point loc = bb.getLocation();
      loc.x -= cur_viewport.x;
      loc.y -= cur_viewport.y;
      floating_bubbles.put(bb, loc);
      bb.setFloating(true);
      setLayer(bb,MODAL_LAYER);
      bbg.removeBubble(bb);
      checkGroup(bbg);
      synchronized (bubble_links) {
	 for (Iterator<BudaBubbleLink> it = bubble_links.iterator(); it.hasNext(); ) {
	    BudaBubbleLink bl = it.next();
	    if (bl.usesBubble(bb)) {
	       it.remove();
	       bl.noteRemoved();
	     }
	  }
       }
    }

   repaint();

   return true;
}




/********************************************************************************/
/*										*/
/*	Group management methods						*/
/*										*/
/********************************************************************************/

private void addBubbleGroup(BudaBubbleGroup bg)
{
   synchronized (bubble_groups) {
      bubble_groups.add(bg);
      JComponent c = bg.getTitleComponent();
      if (c != null) {
	 if (c.getParent() == this) c.setVisible(true);
	 else add(c);
       }
    }
}




private void removeBubbleGroup(BudaBubbleGroup bg)
{
   synchronized (bubble_groups) {
      JComponent c = bg.getTitleComponent();
      if (c != null) {
	 c.setVisible(false);
	 JComponent dmy = new JPanel();
	 dmy.add(c);
	 // c.setParent(null);
       }

      bubble_groups.remove(bg);
    }
}


void removeMovingBubbles(Collection<BudaBubble> bubbles)
{
   synchronized (moving_bubbles) {
      moving_bubbles.removeAll(bubbles);
    }
   checkAreaDimensions();
}



void removeMovingBubble(BudaBubble bb)
{
   synchronized (moving_bubbles) {
      moving_bubbles.remove(bb);
    }
   checkAreaDimensions();
}



void addMovingBubble(BudaBubble bubble)
{
   synchronized (moving_bubbles) {
      moving_bubbles.add(bubble);
    }
}


boolean isMoving(BudaBubble bb)
{
   synchronized (moving_bubbles) {
      return moving_bubbles.contains(bb);
    }
}



/********************************************************************************/
/*										*/
/*	Fix up the group after given bubble has moved.				*/
/*										*/
/*	This method handles splitting the group if needed, adding the bubble	*/
/*	to a new group, and merging groups					*/
/*										*/
/********************************************************************************/

private void fixupGroups(BudaBubble bb)
{
   if (bb == null || bb.isFixed() || bb.isFloating()) return;

   synchronized (bubble_groups) {
      boolean changed = true;
      while (changed) {
	 changed = false;
	 BudaBubbleGroup bg = bb.getGroup();
	 // First check for splitting the current group
	 if (bg != null && bg.isSplit()) {
	    checkGroup(bg);
	    bg = bb.getGroup();
	  }

	 // Next create a new group for the bubble if needed
	 if (bg == null) {
	    bg = new BudaBubbleGroup();
	    bb.setGroup(bg);
	    addBubbleGroup(bg);
	  }

	 // Finally see if we need to merge this group with any other
	 for (BudaBubbleGroup nbg : bubble_groups) {
	    if (nbg.shouldMerge(bg)) {
	       mergeGroups(nbg,bg);
	       changed = true;
	       break;
	     }
	  }
       }
    }

   repaint();
}



private void mergeGroups(BudaBubbleGroup master,BudaBubbleGroup toadd)
{
   if (master.compareTo(toadd) < 0) {
      BudaBubbleGroup g = master;
      master = toadd;
      toadd = g;
    }

   for (BudaBubble bb : toadd.getBubbles()) {
      bb.saveGroup();
      bb.setGroup(master);
    }

   removeBubbleGroup(toadd);

   checkGroup(master);
}



private boolean checkGroup(BudaBubbleGroup g)
{
   if (g != null && g.isEmpty()) {
      removeBubbleGroup(g);
      return true;
    }

   if (g == null || !g.isSplit()) return false;

   // handle splitting a group into subgroups

   ArrayList<BudaBubble> bbls = new ArrayList<BudaBubble>(g.getBubbles());

   // first clear the group of all component bubbles
   for (BudaBubble bb : bbls) bb.setGroup(null);

   // next add each bubble in turn to a group
   // For each unused bubble, create a new group
   // Then expand that group by adding all other bubbles that fit in it.  This is
   //	done over and over as things are added

   SortedSet<BudaBubbleGroup> grps = new TreeSet<BudaBubbleGroup>();
   while (!bbls.isEmpty()) {
      BudaBubble bb = bbls.remove(0);
      BudaBubbleGroup g0 = bb.getNewGroup();
      if (g0 == g) g0 = new BudaBubbleGroup();
      bb.setGroup(g0);
      boolean chng = true;
      while (chng) {
	 chng = false;
	 for (BudaBubble nbb : bbls) {
	    if (g0.shouldAdd(nbb)) {
	       nbb.setGroup(g0);
	       bbls.remove(nbb);
	       chng = true;
	       break;
	     }
	  }
       }
      grps.add(g0);
    }

   // Now make the biggest group that resulted be the original group
   BudaBubbleGroup lst = grps.last();
   grps.remove(lst);
   for (BudaBubble bb : lst.getBubbles()) bb.setGroup(g);

   // Finally, add the new groups
   for (BudaBubbleGroup g2 : grps) addBubbleGroup(g2);

   return grps.size() > 0;
}




/********************************************************************************/
/*										*/
/*	Mouse handling entry points						*/
/*										*/
/********************************************************************************/

/**
 *	These are public to enable the mouse event queue handler to send events
 *	from the middle/right buttons to this area rather than to components
 **/

@Override public void processMouseMotionEvent(MouseEvent e)	{ super.processMouseMotionEvent(e); }
@Override public void processMouseEvent(MouseEvent e)		{ super.processMouseEvent(e); }




/********************************************************************************/
/*										*/
/*	Mouse handling methods to handle to level events			*/
/*										*/
/********************************************************************************/

private void handleMouseEvent(MouseEvent e)
{
   MouseRegion mr = new MouseRegion(e);
   last_mouse = mr;

   for_root.setCurrentChannel(this);

   if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getButton() == MouseEvent.BUTTON2) {
      if (mr.getBubble() != null) userRemoveBubble(mr.getBubble());
      else if (mr.getGroup() != null) {
	 if (e.getClickCount() == 1) {
	    //TODO: put up help message that telling user to double click to remove group
	  }
	 else if (e.getClickCount() == 2) {
	    userRemoveGroup(mr.getGroup());
	  }
       }
      else if (mr.getLink() != null) {
	 // TODO: should this be undoable?
	 removeLink(mr.getLink());
       }
    }
   else if(e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1) {
      if (mr.getBubble() == null) {
	 for_root.hideSearchBubble();
       }
      if (mr.getBubble() != null && mr.getRegion().isBorder() &&
		  mr.getBubble().isResizable()) {
	 mouse_context = new BubbleResizeContext(mr.getBubble(),mr.getRegion(),e);
       }
    }
   else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON3 &&
	       (e.getModifiers() & (MouseEvent.SHIFT_MASK | MouseEvent.CTRL_MASK)) != 0) {
      if (mr.getBubble() != null) mouse_context = new BubbleConnectContext(mr.getBubble(),e);
    }
   else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON3) {
      if (mr.getBubble() != null){
	 BudaBubble bubble = mr.getBubble();
	 bubble = bubble.getActualBubble(e.getX(),e.getY(), true);
	 if (!bubble.isFixed() || bubble.isUserPos()) mouse_context = new BubbleMoveContext(bubble,e);
      }
      else if (mr.getGroup() != null) mouse_context = new GroupMoveContext(mr.getGroup(),e);
      else mouse_context = new AreaMoveContext(e);
    }
   else if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getButton() == MouseEvent.BUTTON3) {
      if (mr.getBubble() != null) {
	 Point point = new Point(e.getX(), e.getY());
	 Point pt = SwingUtilities.convertPoint(this,e.getPoint(),mr.getBubble());
	 e.translatePoint(pt.x-e.getX(),pt.y-e.getY());
	 mr.getBubble().getActualBubble(point.x, point.y, false).handlePopupMenu(e);
       }
      else if (mr.getGroup() != null) {
	 mr.getGroup().handlePopupMenu(e);
       }
      else {
	 int mods = e.getModifiersEx();
	 boolean mergeforreg = BUDA_PROPERTIES.getBoolean(SEARCH_MERGED_ON_REGULAR);
	 if ((mods & MouseEvent.CTRL_DOWN_MASK) != 0)
	    for_root.createDocSearchBubble(e.getPoint(),null,null);
	 else if ((mods & MouseEvent.SHIFT_DOWN_MASK) != 0) {
	    if (!mergeforreg) for_root.createMergedSearchBubble(e.getPoint(),null,null);
	    else for_root.createSearchBubble(e.getPoint(), null, null);
	 }
	 else {
	    if (!mergeforreg) for_root.createSearchBubble(e.getPoint(),null,null);
	    else for_root.createMergedSearchBubble(e.getPoint(),null,null);
	 }
       }
    }
}



private void handleScrollEvent(MouseWheelEvent e)
{
   int dx = 0;
   int dy = 0;
   if ((e.getModifiers() & (MouseEvent.SHIFT_MASK | MouseEvent.CTRL_MASK)) != 0) {
      dx = e.getWheelRotation() * 20;
    }
   else {
      dy = e.getWheelRotation() * 20;
    }

   for_root.moveCurrentViewport(dx,dy);
}




private void handleMouseMoved(MouseEvent e)
{
   MouseRegion mr = new MouseRegion(e);

   if (focus_bubble != mr.in_bubble && mr.in_bubble != null) {
      if (focus_bubble != null) setFocusBubble(focus_bubble,false);
      setFocusBubble(mr.in_bubble,true);
    }

   last_mouse = mr;

   switch (mr.getRegion()) {
      default :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	 break;
      case BORDER_N :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
	 break;
      case BORDER_NE :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
	 break;
      case BORDER_E :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
	 break;
      case BORDER_SE :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
	 break;
      case BORDER_S :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
	 break;
      case BORDER_SW :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
	 break;
      case BORDER_W :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
	 break;
      case BORDER_NW :
	 BudaCursorManager.setTemporaryCursor(this, Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
	 break;
    }
}



private void handleMouseSet(MouseEvent e)
{
   last_mouse = new MouseRegion(e);
}



private class Mouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      handleMouseEvent(e);
    }

   @Override public void mousePressed(MouseEvent e) {
      if (mouse_context != null) {
	 mouse_context.finish();
	 mouse_context = null;
       }
      handleMouseEvent(e);
    }

   @Override public void mouseReleased(MouseEvent e) {
      if (mouse_context != null) {
	 mouse_context.finish();
	 mouse_context = null;
       }
      handleMouseSet(e);
    }

   @Override public void mouseDragged(MouseEvent e) {
      if (mouse_context != null) mouse_context.next(e);
      else handleMouseSet(e);
    }

   @Override public void mouseMoved(MouseEvent e) {
      handleMouseMoved(e);
    }

   @Override public void mouseExited(MouseEvent e) {
    }

   @Override public void mouseWheelMoved(MouseWheelEvent e) {
      handleScrollEvent(e);
    }


}	// end of inner class Mouser




/********************************************************************************/
/*										*/
/*	Class to contain information about the current mouse position		*/
/*										*/
/********************************************************************************/

private class MouseRegion {

   private BudaRegion region_type;
   private BudaBubble in_bubble;
   private BudaBubbleGroup in_group;
   private BudaBubbleLink in_link;
   private Point mouse_loc;

   MouseRegion(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();

      if (scale_factor != 1.0) {
	 Point p0 = SwingUtilities.convertPoint((Component) e.getSource(),e.getPoint(),
							BudaBubbleArea.this);
	 x = (int)(p0.x / scale_factor);
	 y = (int)(p0.y / scale_factor);
       }

      if (scale_factor != 1.0) {
	 x = (int)(x / scale_factor);
	 y = (int)(y / scale_factor);
       }

      region_type = BudaRegion.NONE;
      in_bubble = null;
      in_group = null;
      in_link = null;
      mouse_loc = new Point(x,y);

      int maxlayer = Integer.MIN_VALUE;
      // First see if the mouse is inside a bubble

      synchronized (active_bubbles) {
	 for (BudaBubble bb : active_bubbles) {
	    BudaRegion br = bb.correlate(x,y);
	    if (br != BudaRegion.NONE && JLayeredPane.getLayer(bb) > maxlayer) {
	       maxlayer = JLayeredPane.getLayer(bb);
	       region_type = br;
	       in_bubble = bb;
	     }
	  }
       }

      // Next see if the mouse is on a link
      if (region_type == BudaRegion.NONE) {
	 synchronized (bubble_links) {
	    for (BudaBubbleLink bl : bubble_links) {
	       BudaRegion br = bl.correlate(x,y);
	       if (br != BudaRegion.NONE) {
		  region_type = br;
		  in_link = bl;
		  break;
		}
	     }
	  }
       }

      // If not, check if it is inside a bubble group
      if (region_type == BudaRegion.NONE) {
	 synchronized (bubble_groups) {
	    for (BudaBubbleGroup bg : bubble_groups) {
	       BudaRegion br = bg.correlate(x,y);
	       if (br != BudaRegion.NONE) {
		  region_type = br;
		  in_group = bg;
		  break;
		}
	     }
	  }
       }
    }

   BudaRegion getRegion()		{ return region_type; }
   BudaBubble getBubble()		{ return in_bubble; }
   BudaBubbleGroup getGroup()		{ return in_group; }
   BudaBubbleLink getLink()		{ return in_link; }
   Point getLocation()			{ return mouse_loc; }

}	// end of inner class MouseRegion





/********************************************************************************/
/*										*/
/*	Methods to handle move and resize of bubbles				*/
/*										*/
/*	Resize and move events are handled by placing a subclass of		*/
/*	MouseContext into the field mouse_context.  Then drag events and	*/
/*	the release events are passed to the context using the next()		*/
/*	method.  finish() is called after release				*/
/*										*/
/********************************************************************************/

private abstract class MouseContext {

   protected Point initial_mouse;

   MouseContext(MouseEvent e) {
      initial_mouse = new Point((int)(e.getPoint().x / scale_factor), (int)(e.getPoint().y / scale_factor));
      auto_scroller.setDelta(0,0);
    }

   void finish() {
      auto_scroller.setDelta(0,0);
      //setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

   abstract void next(MouseEvent e);

}	// end of inner class MouseContext




private class BubbleMoveContext extends MouseContext {

   private BudaBubble for_bubble;
   private Point initial_location;
   private Dimension bubble_size;
   private Dimension area_size;
   private int start_layer;
   private int move_count;

   BubbleMoveContext(BudaBubble bb,MouseEvent e) {
      super(e);
      for_bubble = bb;
      initial_location = bb.getLocation();
      bubble_size = bb.getSize();
      area_size = getSize();
      start_layer = getLayer(bb);
      addMovingBubble(bb);
      move_count = 0;
      //BudaCursorManager.setGlobalCursorForComponent(bb, palm_cursor);
    }

   void next(MouseEvent e) {
      if (for_bubble == null) return;

      for_bubble.forceFreeze();
      if (docked_bubbles.containsKey(for_bubble)) {
	 docked_bubbles.remove(for_bubble);
	 for_bubble.setDocked(false);
       }

      BudaCursorManager.setGlobalCursorForComponent(for_bubble, palm_cursor);

      ++move_count;
      setLayer(for_bubble,DRAG_LAYER,0);
      Point p0 = e.getPoint();
      int x0 = initial_location.x + (int)(p0.x / scale_factor) - initial_mouse.x;
      int y0 = initial_location.y + (int)(p0.y / scale_factor) - initial_mouse.y;

      if (scale_factor != 1.0) {
	 p0 = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(),
					     BudaBubbleArea.this);
	 p0 = new Point((int)(p0.x / scale_factor), (int)(p0.y / scale_factor));
       }

      checkMoveViewport(p0);

      if (x0 + bubble_size.width < MIN_SHOW_SIZE) {
	 x0 = MIN_SHOW_SIZE - bubble_size.width;
       }
      else if (x0 > area_size.width - MIN_SHOW_SIZE) {
	 x0 = area_size.width - MIN_SHOW_SIZE;
       }
      if (y0 + bubble_size.height < MIN_SHOW_SIZE) {
	 y0 = MIN_SHOW_SIZE - bubble_size.height;
       }
      else if (y0 > area_size.height - MIN_SHOW_SIZE) {
	 y0 = area_size.height - MIN_SHOW_SIZE;
       }

      for_bubble.setLocation(x0,y0);
      // TODO: ensure that this isn't done twice
      fixupGroups(for_bubble);
      if (for_bubble.isUserPos()) repaint();
    }

   void finish() {
      super.finish();
      if (for_bubble == null) return;

      for_bubble.unfreeze();
      for_bubble.grabFocus();
      setLayer(for_bubble,start_layer,0);

      if (for_root.noteBubbleActionDone(for_bubble)) return;

      fixupBubble(for_bubble);
      fixupGroups(for_bubble);
      if (for_bubble.isUserPos()) repaint();

      BudaCursorManager.resetDefaults(for_bubble);
      //for_bubble.setCursor(for_bubble.getBubbleCursor());

      if (move_count > 0) BoardMetrics.noteCommand("BUDA","bubbleMoved");
      removeMovingBubble(for_bubble);
    }


}	// end of BubbleMoveContext





private class BubbleResizeContext extends MouseContext {

   private BudaBubble for_bubble;
   private BudaRegion border_region;
   private Rectangle initial_bounds;
   private Rectangle new_bounds;
   private int min_width;
   private int min_height;
   private int resize_count;

   BubbleResizeContext(BudaBubble bb,BudaRegion br,MouseEvent e) {
      super(e);
      for_bubble = bb;
      border_region = br;
      initial_bounds = bb.getBounds();
      new_bounds = new Rectangle(initial_bounds);
      min_width = bb.getMinimumResizeWidth();
      min_height = bb.getMinimumResizeHeight();
      resize_count = 0;
    }

   void next(MouseEvent e) {
      if (for_bubble == null) return;

      for_bubble.unfreeze();
      ++resize_count;
      Point p0 = e.getPoint();
      int dx = p0.x - initial_mouse.x;
      int dy = p0.y - initial_mouse.y;
      switch (border_region) {
	 case BORDER_S :
	 case BORDER_SE :
	 case BORDER_SW :
	    new_bounds.height = initial_bounds.height + dy;
	    if (new_bounds.height < min_height) new_bounds.height = min_height;
	    break;
	 case BORDER_N :
	 case BORDER_NE :
	 case BORDER_NW :
	    new_bounds.height = initial_bounds.height - dy;
	    if (new_bounds.height < min_height) {
	       new_bounds.height = min_height;
	       dy = initial_bounds.height - min_height;
	     }
	    new_bounds.y = initial_bounds.y + dy;
	    break;
       }
      switch (border_region) {
	 case BORDER_W :
	 case BORDER_SW :
	 case BORDER_NW :
	    new_bounds.width = initial_bounds.width - dx;
	    if (new_bounds.width < min_width) {
	       new_bounds.width = min_width;
	       dx = initial_bounds.width - min_width;
	     }
	    new_bounds.x = initial_bounds.x + dx;
	    break;
	 case BORDER_E :
	 case BORDER_SE :
	 case BORDER_NE :
	    new_bounds.width = initial_bounds.width + dx;
	    if (new_bounds.width < min_width) new_bounds.width = min_width;
	    break;
       }
      for_bubble.setBounds(new_bounds);
    }

   void finish() {
      super.finish();
      fixupBubble(for_bubble);
      fixupGroups(for_bubble);
      if (resize_count > 0) for_bubble.noteResize(initial_bounds.width,initial_bounds.height);
    }

}	// end of BubbleResizeContext




private class GroupMoveContext extends MouseContext {

   private List<BudaBubble> move_bubbles;
   private Point mouse_point;
   private int move_count;

   GroupMoveContext(BudaBubbleGroup bg,MouseEvent e) {
      super(e);

      removeMovingBubbles(bg.getBubbles());

      mouse_point = e.getPoint();
      move_bubbles = new ArrayList<BudaBubble>(bg.getBubbles());
      move_count = 0;
    }

   void next(MouseEvent e) {
      for (BudaBubble b : move_bubbles) b.forceFreeze();

      ++move_count;
      BudaCursorManager.setGlobalCursorForComponent(BudaBubbleArea.this, Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      Point p1 = e.getPoint();
      Point p0 = e.getPoint();
      int dx = (int)((p1.x - mouse_point.x) / scale_factor);
      int dy = (int)((p1.y - mouse_point.y)/ scale_factor);

      if (dx == 0 && dy == 0) return;

      if (scale_factor != 1.0) {
	  p0 = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(),
					BudaBubbleArea.this);
	  p0 = new Point((int)(p0.x / scale_factor), (int)(p0.y / scale_factor));
       }

      checkMoveViewport(p0);
      //TODO: if p1 is on the overview bar and e is a release, move to that position
      //TODO: prevent the group from going off the screen completely

      mouse_point = p1;
      for (BudaBubble bb : move_bubbles) {
	 Point p = bb.getLocation();
	 p.x += dx;
	 p.y += dy;
	 bb.setLocation(p);
       }
    }


   void finish() {
      super.finish();
      for (BudaBubble b : move_bubbles) {
	 b.unfreeze();
       }
      fixupBubbleGroup(move_bubbles.get(0));

      BudaCursorManager.resetDefaults(BudaBubbleArea.this);

      if (move_count > 0) BoardMetrics.noteCommand("BUDA","bubbleGroupMoved");

      removeMovingBubbles(move_bubbles);
    }

}	// end of GroupMoveContext




private class AreaMoveContext extends MouseContext {

   private Point mouse_point;
   private int move_count;

   AreaMoveContext(MouseEvent e) {
      super(e);
      mouse_point = e.getLocationOnScreen();
      move_count = 0;
    }

   void next(MouseEvent e) {
      if (focus_bubble!=null) focus_bubble.forceFreeze();

      ++move_count;
      Point p1 = e.getLocationOnScreen();
      int dx = p1.x - mouse_point.x;
      int dy = p1.y - mouse_point.y;

      if (dx == 0 && dy == 0) return;
      mouse_point = p1;
      for (BubbleAreaCallback cb : area_callbacks) {
	 cb.moveDelta(-dx,-dy);
       }
    }

   void finish() {
      super.finish();
      if (focus_bubble != null) focus_bubble.unfreeze();
      if (move_count > 0) BoardMetrics.noteCommand("BUDA","areaMoved");
    }

}	// end of AreaMoveContext



/********************************************************************************/
/*										*/
/*	Context for making/breaking connections 				*/
/*										*/
/********************************************************************************/

private class BubbleConnectContext extends MouseContext {

   private BudaBubble start_bubble;
   private DrawPanel draw_panel;
   private MouseEvent start_event;
   private MouseEvent last_event;

   BubbleConnectContext(BudaBubble bb,MouseEvent e) {
      super(e);
      start_bubble = bb;
      draw_panel = null;
      start_event = e;
      last_event = null;
    }

   void next(MouseEvent e) {
      if (draw_panel == null) {
	 BudaCursorManager.setGlobalCursorForComponent(BudaBubbleArea.this, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	 draw_panel = new DrawPanel();
	 draw_panel.setOpaque(false);
	 Dimension d = getSize();
	 draw_panel.setSize(d);
	 draw_panel.addPoint(start_event.getX(),start_event.getY());
	 add(draw_panel,JLayeredPane.DRAG_LAYER);
       }
      draw_panel.addPoint(e.getX(),e.getY());
      last_event = e;
    }

   void finish() {
      super.finish();
      if (draw_panel == null) return;
      draw_panel.setVisible(false);
      remove(draw_panel);
      MouseRegion mr = new MouseRegion(last_event);
      if (mr.getBubble() == null || mr.getBubble() == start_bubble) return;
      if (!start_bubble.connectTo(mr.getBubble(),start_event)) {
	 BudaBubbleLink rembbl = null;
	 synchronized (bubble_links) {
	    for (BudaBubbleLink bbl : bubble_links) {
	       if ((bbl.getSource() == start_bubble && bbl.getTarget() == mr.getBubble()) ||
		      (bbl.getSource() == mr.getBubble() && bbl.getTarget() == start_bubble)) {
		  rembbl = bbl;
		  break;
		}
	     }
	  }
	 if (rembbl != null) {
	    BoardMetrics.noteCommand("BUDA","userRemoveLink");
	    for_root.removeLink(rembbl);
	  }
	 else {
	    BoardMetrics.noteCommand("BUDA","userAddLink");
	    BudaBubbleLink bbl = new BudaBubbleLink(
	       start_bubble,
	       new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true),
	       mr.getBubble(),
	       new BudaDefaultPort(BudaPortPosition.BORDER_ANY,true),
	       true,BudaLinkStyle.STYLE_DASHED);
	    for_root.addLink(bbl);
	  }
       }
      BudaCursorManager.resetDefaults(BudaBubbleArea.this);
      for_root.repaint();
    }

}	// end of BubbleMoveContext




private static class DrawPanel extends JPanel implements NoBubble {

   private Path2D	line_points;
   private Stroke	line_stroke;
   private int		num_points;

   private static final long serialVersionUID = 1;

   DrawPanel() {
      line_points = new Path2D.Double();
      line_stroke = new BasicStroke(3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
      num_points = 0;
    }

   void addPoint(double x,double y) {
      if (num_points++ == 0) line_points.moveTo(x,y);
      else {
	 line_points.lineTo(x,y);
	 repaint();
       }
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(line_stroke);
      g2.setColor(LINK_DRAW_COLOR);
      g2.draw(line_points);
    }

}	// end of inner class DrawPanel




/********************************************************************************/
/*										*/
/*	Methods to remove and restore bubbles					*/
/*										*/
/********************************************************************************/

/**
 *	Remove the specified bubble but let the user unremove it if desired.
 **/

public void userRemoveBubble(BudaBubble bb)
{
   if (bb == null) return;

   UndoRemove ur = new UndoRemove(bb);

   Rectangle r0 = bb.getBounds();
   removeBubble(bb);
   ToolTipManager.sharedInstance().setEnabled(false);
   ToolTipManager.sharedInstance().setEnabled(true);
   if (bb.isTransient() || (bb instanceof BudaHintBubble)) {
      disposeBubble(bb);
      return;
    }

   BudaHintBubble undo = new BudaHintBubble("Undo Delete Bubble",ur,30000);

   Dimension usz = undo.getSize();
   add(undo,new BudaConstraint(BudaBubblePosition.FIXED,
				  r0.x + r0.width/2 - usz.width/2,
				  r0.y + r0.height/2 - usz.height/2));
   setPosition(undo,-1);
   setLayer(undo,DEFAULT_LAYER);

   BoardMetrics.noteCommand("BUDA","removeBubble_" + bb.getHashId());
}



private void userRemoveGroup(BudaBubbleGroup bg)
{
   BudaHintBubble undo = new BudaHintBubble("Undo Delete Group",
					       new UndoRemove(bg.getBubbles()),30000);

   Rectangle r0 = null;
   for (BudaBubble bb : bg.getBubbles()) {
      if (r0 == null) r0 = bb.getBounds();
      else r0.add(bb.getBounds());
      removeBubble(bb);
    }
   if (r0 == null) return;

   Dimension usz = undo.getSize();
   add(undo,new BudaConstraint(BudaBubblePosition.FIXED,
				  r0.x + r0.width/2 - usz.width/2,
				  r0.y + r0.height/2 - usz.height/2));
   setPosition(undo,-1);
   setLayer(undo,DEFAULT_LAYER);

   BoardMetrics.noteCommand("BUDA","removeBubbleGroup");
}



private void disposeBubble(BudaBubble bb)
{
   removeMovingBubble(bb);
   bb.disposeBubble();
}



private class UndoRemove implements BudaHintActions {

   private Set<BudaBubble> bubble_set;
   private List<BudaBubbleLink> link_set;

   UndoRemove(BudaBubble bb) {
      bubble_set = new HashSet<BudaBubble>();
      bubble_set.add(bb);
      setupLinks();
    }

   UndoRemove(Collection<BudaBubble> bbl) {
      bubble_set = new HashSet<BudaBubble>(bbl);
      setupLinks();
    }

   private void setupLinks() {
      link_set = new ArrayList<BudaBubbleLink>();
      synchronized (bubble_links) {
	 for (BudaBubbleLink bl : bubble_links) {
	    if (bubble_set.contains(bl.getSource()) || bubble_set.contains(bl.getTarget()))
	       link_set.add(bl);
	  }
       }
    }

   @Override public void clickAction() {
      if (bubble_set == null) return;
      BoardMetrics.noteCommand("BUDA","undoRemove");
      for (BudaBubble bb : bubble_set) {
	  bb.setVisible(true);
	  BudaBubblePosition pos = BudaBubblePosition.MOVABLE;
	  if (bb.isFixed()) pos = BudaBubblePosition.FIXED;
	  else if (bb.isFloating()) pos = BudaBubblePosition.FLOAT;
	  BudaConstraint bc = new BudaConstraint(pos,bb.getX(),bb.getY());
	  add(bb,bc);
       }
      for (BudaBubbleLink bl : link_set) {
	 if (bl.getSource().isVisible() && bl.getTarget().isVisible()) {
	    addLink(bl);
	  }
       }
      bubble_set = null;
      link_set = null;
    }

   @Override public void finalAction() {
      if (bubble_set != null) {
	 for (BudaBubble bb : bubble_set) {
	    disposeBubble(bb);
	  }
       }
      bubble_set = null;
      link_set = null;
    }

}	// end of inner class UndoRemove




/********************************************************************************/
/*										*/
/*	Methods for automatic scrolling the background				*/
/*										*/
/********************************************************************************/

private void checkMoveViewport(Point p)
{
   // BoardLog.logD("Buda","POINT " + p + " " + cur_viewport);

   int ax = 0, ay = 0;
   if (p.x < cur_viewport.x) ax = -1;
   else if (p.x > cur_viewport.x + cur_viewport.width) ax = 1;
   if (p.y < cur_viewport.y) ay = -1;
   else if (p.y > cur_viewport.y + cur_viewport.height) ay = 1;
   auto_scroller.setDelta(ax,ay);
}



private class AutoScroller extends javax.swing.Timer implements ActionListener
{
   private int delta_x;
   private int delta_y;

   private static final long serialVersionUID = 1;

   AutoScroller() {
      super(AUTOSCROLL_DELAY,null);

      delta_x = 0;
      delta_y = 0;

      addActionListener(this);
      setActionCommand("AUTOSCROLL");
      setRepeats(true);

      setInitialDelay(AUTOSCROLL_INITIAL_DELAY);
    }

   void setDelta(int dx,int dy) {
      delta_x = dx;
      delta_y = dy;
      if (dx == 0 && dy == 0) {
	 if (isRunning()) {
	    BoardMetrics.noteCommand("BUDA","autoScroll");
	    stop();
	  }
       }
      else if (!isRunning()) start();
    }

   public void actionPerformed(ActionEvent e) {
      for (BubbleAreaCallback cb : area_callbacks) {
	 cb.moveDelta(delta_x*AUTOSCROLL_SPEED,delta_y*AUTOSCROLL_SPEED);
       }
    }

}	// end of inner class AutoScroller




/********************************************************************************/
/*										*/
/*	Methods for handling component actions					*/
/*										*/
/********************************************************************************/

private class BubbleManager implements ComponentListener, ContainerListener {

   BubbleManager()		{ }

   public void componentHidden(ComponentEvent e) {
      BudaBubble bb = (BudaBubble) e.getSource();
      localRemoveBubble(bb);
      updateOverview();
    }

   public void componentMoved(ComponentEvent e) {
      if (cur_viewport == null) return;
      if (e.getSource() instanceof BudaBubble) {
	 BudaBubble bbl = (BudaBubble) e.getSource();
	 if (bbl.isFloating()) {
	    Point floc = new Point(bbl.getLocation());
	    floc.x -= cur_viewport.x;
	    floc.y -= cur_viewport.y;
	    floating_bubbles.put(bbl,floc);
	  }
	 else {
	    if (moving_bubbles.contains(bbl)) fixupGroups(bbl);
	    else repaint();
	    routes_valid = false;
	  }
	 updateOverview();
      }
   }

   public void componentResized(ComponentEvent e) {
      if (e.getSource() instanceof BudaBubble) {
	 BudaBubble bb = (BudaBubble) e.getSource();
	 fixupBubble(bb);
	 fixupGroups(bb);
	 routes_valid = false;
	 updateOverview();
       }
    }

   public void componentShown(ComponentEvent e) {
      BudaBubble bb = (BudaBubble) e.getSource();
      localAddBubble(bb,true);
      updateOverview();
    }

   public void componentAdded(ContainerEvent e) {
      if (e.getChild() instanceof BudaBubble) {
	 BudaBubble bb = (BudaBubble) e.getChild();
	 if (bb.isShowing()) {
	    localAddBubble(bb,true);
	    updateOverview();
	  }
       }
    }

   public void componentRemoved(ContainerEvent e) {
      if (e.getChild() instanceof BudaBubble) {
	 BudaBubble bb = (BudaBubble) e.getChild();
	 localRemoveBubble(bb);
	 updateOverview();
       }
    }

   private void updateOverview() {
      checkAreaDimensions();
      for (BubbleAreaCallback cb : area_callbacks) {
	 cb.updateOverview();
       }
    }

}	// end of inner class BubbleManager



/********************************************************************************/
/*										*/
/*	Dummy layout manager for bubble area					*/
/*										*/
/********************************************************************************/

private static class BudaLayoutManager implements LayoutManager {

   public void addLayoutComponent(String name,Component c)		{ }

   public void removeLayoutComponent(Component c)			{ }

   public void layoutContainer(Container par)				{ }

   public Dimension minimumLayoutSize(Container par) {
      return par.getSize();
    }

   public Dimension preferredLayoutSize(Container par) {
      return par.getSize();
    }

}	// end of BudaLayoutManager



/********************************************************************************/
/*										*/
/*	Bubble drag and drop support						*/
/*										*/
/********************************************************************************/

private class BubbleDropper extends TransferHandler {

   private static final long serialVersionUID = 1;

   BubbleDropper() {
    }

   public Transferable createTransferable(JComponent c) {
      return null;
    }

   @Override public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }

   @Override public boolean canImport(JComponent c,DataFlavor [] flavors) {
      DataFlavor df = BudaRoot.getBubbleTransferFlavor();
      for (int i = 0; i < flavors.length; ++i) {
	 if (flavors[i].equals(df)) return true;
       }
      return false;
    }

   @Override public boolean canImport(TransferSupport sup) {
      DataFlavor df = BudaRoot.getBubbleTransferFlavor();
      if (sup.isDataFlavorSupported(df)) return true;
      return false;
    }

   @Override public boolean importData(TransferHandler.TransferSupport sup) {
      Transferable t = sup.getTransferable();
      DataFlavor df = BudaRoot.getBubbleTransferFlavor();
      // TODO: need to handle text transfers
      try {
	 BudaDragBubble bdb = (BudaDragBubble) t.getTransferData(df);
	 TransferHandler.DropLocation loc = sup.getDropLocation();
	 Point pt = loc.getDropPoint();
	 BudaBubble [] bba = bdb.createBubbles();
	 int y = pt.y;
	 for (BudaBubble bb : bba) {
	    if (bb != null) {
	       addBubble(bb,pt.x,y);
	       bb.markBubbleAsNew();
	       y += 10;
	     }
	  }
	 sup.setDropAction(COPY);
	 BoardMetrics.noteCommand("BUDA","bubbleDrop");
	 return true;
       }
      catch (Exception e) { }
      return false;
   }

}	// end of inner class BubbleDropper



}	// end of class BudaBubbleArea




/* end of BudaBubbleArea.java */
