/********************************************************************************/
/*										*/
/*		BddtBubbleManager.java						*/
/*										*/
/*	Bubbles debugger bubble management routines				*/
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

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.awt.Rectangle;
import java.util.*;



class BddtBubbleManager implements BddtConstants, BudaConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	launch_control;
private BudaBubbleArea		bubble_area;
private Map<BudaBubble,BubbleData> bubble_map;

private static BoardProperties bddt_properties = BoardProperties.getProperties("Bddt");

private static boolean	delete_old = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtBubbleManager(BddtLaunchControl blc)
{
   launch_control = blc;
   bubble_area = BudaRoot.findBudaBubbleArea(blc);
   bubble_map = new HashMap<BudaBubble,BubbleData>();
}



/********************************************************************************/
/*										*/
/*	Bubble creation entries 						*/
/*										*/
/********************************************************************************/

void createExecBubble(BumpThread bt)
{
   BumpThreadStack stk = bt.getStack();
   BudaBubble bb = createSourceBubble(stk,0,BubbleType.EXEC);
   if (bb != null) {
      BubbleData bd = bubble_map.get(bb);
      if (bd == null || bd.getBubbleType() != BubbleType.EXEC) return;
      if (bddt_properties.getBoolean("Bddt.show.values")) {
	 BddtStackView sv = new BddtStackView(launch_control,bt);
	 Rectangle r = BudaRoot.findBudaLocation(bb);
	 BudaRoot br = BudaRoot.findBudaRoot(bb);
	 BubbleData nbd = new BubbleData(sv,bt,stk,stk.getFrame(0),BubbleType.FRAME);
	 bubble_map.put(sv,nbd);
	 br.add(sv,new BudaConstraint(r.x,r.y+r.height+20));
	 bd.setAssocBubble(sv);
       }
    }
}


private BudaBubble createSourceBubble(BumpThreadStack stk,int frm,BubbleType typ)
{
   // TODO: handle system and class files here
   setupBubbleArea();

   if (stk == null) return null;

   BumpThread bt = stk.getThread();

   // find user stack frame for the stack
   if (stk.getNumFrames() <= frm) return null;
   BumpStackFrame frame = stk.getFrame(frm);

   int xpos = -1;
   int ypos = -1;
   BudaBubble link = null;
   int linkline = -1;

   BubbleData bd = findClosestBubble(bt,stk,frame);
   if (bd != null && bd.match(bt,stk,frame)) {
      BoardLog.logD("BDDT","Existing bubbles found for " + bd.getBubble().getContentName());
      bd.update(stk,frame);
      showBubble(bd.getBubble());
      return null;
    }

   if (bd != null) {
      Rectangle r = BudaRoot.findBudaLocation(bd.getBubble());
      if (r != null && bd.aboveLevel(bt,stk,frame) >= 0) {
	 xpos = r.x + r.width + 40;
	 ypos = r.y;
	 link = bd.getBubble();
	 linkline = bd.getLineNumber();
       }
      else if (r != null && bd.getBubbleType() == BubbleType.EXEC && bd.getThread() == bt) {
	 xpos = r.x + r.width + 40;
	 xpos = r.x - 300;
	 if (xpos < 0) xpos = 0;
	 ypos = r.y;
	 link = bd.getBubble();
       }
      else if (r != null) {
	 xpos = r.x + r.width + 40;
	 ypos = r.y;
       }
      else {
	 xpos = 100;
	 ypos = 100;
       }
    }
   else {
      xpos = 100;
      ypos = 100;
    }

   if (ypos < 0) ypos = 0;
   if (xpos < 0) xpos = 0;

   String proj = launch_control.getProject();
   // TODO: The project might be wrong: the file might be fron a sub project
   BudaBubble bb = null;
   if (frame.getFile() != null && frame.getFile().exists()) {
      String mid = frame.getMethod() + frame.getSignature();
      if (frame.isSystem()) {
	 bb = BaleFactory.getFactory().createSystemMethodBubble(proj,mid,frame.getFile());
	 if (bb == null) {
	    bb = new BddtLibraryBubble(frame);
	  }
       }
      else {
	 bb = BaleFactory.getFactory().createMethodBubble(proj,mid);
	 if (bb == null) {
	    bb = BaleFactory.getFactory().createMethodBubble(null,mid);
	  }
       }
    }
   else {
      bb = new BddtLibraryBubble(frame);
    }

   if (bb == null) {
      BoardLog.logD("BDDT","No bubble created for " + frame.getMethod() + frame.getSignature());
    }

   if (bb != null) {
      BubbleData nbd = new BubbleData(bb,bt,stk,frame,typ);
      bubble_map.put(bb,nbd);
      BudaRoot root = BudaRoot.findBudaRoot(bubble_area);
      BudaConstraint bc = new BudaConstraint(xpos,ypos);
      bubble_area.add(bb,bc);
      if (link != null && link.isShowing()) {
	 LinkPort port0;
	 if (link instanceof BddtLibraryBubble || linkline <= 0) {
	    port0 = new BudaDefaultPort(BudaPortPosition.BORDER_E,true);
	 }
	 else {
	    port0 = BaleFactory.getFactory().findPortForLine(link,linkline);
	 }
	 if (port0 != null) {
	    LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	    BudaBubbleLink lnk = new BudaBubbleLink(link,port0,bb,port1);
	    lnk.setColor(BDDT_LINK_COLOR);
	    root.addLink(lnk);
	 }
      }
      bb.markBubbleAsNew();
      showBubble(bb);
    }

   return bb;
}





void restart()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case BDDT :
	 case THREADS :
	 case CONSOLE :
	 case HISTORY :
	    break;
	 case EXEC :
	 case FRAME :
	 case STOP_TRACE :
	    if (bd.canRemove()) bubble_area.userRemoveBubble(bd.getBubble());
	    break;
	 case VALUES :
	    bubble_area.userRemoveBubble(bd.getBubble());
	    break;
	 case USER :
	    break;
       }
    }
}



BudaBubble createThreadBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case THREADS :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   BudaRoot br = BudaRoot.findBudaRoot(launch_control);
   BudaBubble bb = new BddtThreadView(launch_control);
   Rectangle r = launch_control.getBounds();
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

   return bb;
}



BudaBubble createConsoleBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case CONSOLE :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   BudaRoot br = BudaRoot.findBudaRoot(launch_control);
   BudaBubble bb = BddtFactory.getFactory().getConsoleControl().createConsole(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_CONSOLE)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   br.add(bb,bc);

   return bb;
}




BudaBubble createHistoryBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case HISTORY :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   BudaRoot br = BudaRoot.findBudaRoot(launch_control);
   BudaBubble bb = BddtFactory.getFactory().getHistoryControl().createHistory(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x;
   int y = r.y + r.height + BDDT_CONSOLE_HEIGHT + 20 + BDDT_STACK_HEIGHT + 20 + 20;

   BudaConstraint bc;
   if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_HISTORY)) {
      bc = new BudaConstraint(BudaBubblePosition.FLOAT,x,y);
    }
   else {
      bc = new BudaConstraint(BudaBubblePosition.MOVABLE,x,y);
    }

   br.add(bb,bc);

   return bb;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpStackFrame getFrameForBubble(BudaBubble bb)
{
   BubbleData bd = bubble_map.get(bb);
   if (bd == null) return null;

   return bd.getFrame();
}




/********************************************************************************/
/*										*/
/*	Bubble support routines 						*/
/*										*/
/********************************************************************************/

private void setupBubbleArea()
{
   if (bubble_area != null) return;

   bubble_area = BudaRoot.findBudaBubbleArea(launch_control);
   if (bubble_area == null) return;

   for (BudaBubble bb : bubble_area.getBubbles()) {
      if (bubble_map.get(bb) == null) {
	 bubble_map.put(bb,new BubbleData(bb));
       }
    }

   BudaRoot.addBubbleViewCallback(new BubbleUpdater());
}




private BubbleData findClosestBubble(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm)
{
   BubbleData best = null;

   // first try to find an exact match
   for (BubbleData bd : bubble_map.values()) {
      if (bd.match(bt,stk,frm)) {
	 best = bd;
	 break;
       }
    }

   // next try to find a user bubble that matches
   if (best == null) {
      for (BubbleData bd : bubble_map.values()) {
	 if (bd.matchUser(bt,stk,frm)) {
	    best = bd;
	    break;
	 }
      }
   }

   if (best == null) {
      // alternatively, try to find a bubble we are below
      int blvl = -1;
      for (BubbleData bd : bubble_map.values()) {
	 if (bd.getBubbleType() != BubbleType.EXEC) continue;
	 int lvl = bd.aboveLevel(bt,stk,frm);
	 if (lvl > 0 && blvl < lvl) {
	    best = bd;
	    blvl = lvl;
	  }
       }
    }

   if (best != null) {
      // we have found an entry relevant to the stack
      if (delete_old) {
	 Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
	 for (BubbleData bd : bbls) {
	    if (bd != best && bd.isBelow(best) && bd.canRemove() && bd.getBubbleType() == BubbleType.EXEC) {
	       bubble_area.userRemoveBubble(bd.getBubble());
	       BudaBubble sb = bd.getAssocBubble();
	       if (sb != null) {
		  bubble_area.userRemoveBubble(sb);
		  bd.setAssocBubble(null);
		}
	     }
	  }
       }
      return best;
    }

   // find rightmost bubble for the current thread
   int rmost = -1;
   for (BubbleData bd : bubble_map.values()) {
      if (bd.getThread() == bt && bd.getBubbleType() == BubbleType.EXEC) {
	 BudaBubble bb = bd.getBubble();
	 Rectangle r = BudaRoot.findBudaLocation(bb);
	 if (r.x + r.width > rmost) {
	    rmost = r.x + r.width;
	    best = bd;
	  }
       }
    }

   if (best == null) {
      // if first bubble for this thread, use most recent bubble
      long tmost = -1;
      for (BubbleData bd : bubble_map.values()) {
	 long btim = bd.getLastTime();
	 if (btim < 0) continue;
	 switch (bd.getBubbleType()) {
	    case CONSOLE :
	    case HISTORY :
	    case STOP_TRACE :
	    case THREADS :
	       continue;
	  }
	 if (best == null) {
	    tmost = btim;
	    best = bd;
	  }
	 else if (best.getBubbleType() != BubbleType.EXEC) {
	    if (bd.getBubbleType() == BubbleType.EXEC || btim > tmost) {
	       tmost = btim;
	       best = bd;
	     }
	  }
	 else if (bd.getBubbleType() == BubbleType.EXEC && btim > tmost) {
	    tmost = btim;
	    best = bd;
	  }
       }
    }

   return best;
}



private void showBubble(BudaBubble bb)
{
   bubble_area.scrollBubbleVisible(bb);
}



/********************************************************************************/
/*										*/
/*	Utility functions							*/
/*										*/
/********************************************************************************/

private static boolean matchFrameMethod(BumpStackFrame sf1,BumpStackFrame sf2)
{
   if (sf1.getMethod() != null) {
      if (!sf1.getMethod().equals(sf2.getMethod())) return false;
    }
   else if (sf2.getMethod() != null) return false;

   if (sf1.getSignature() != null) {
      if (!sf1.getSignature().equals(sf2.getSignature())) return false;
    }
   else if (sf2.getSignature() != null) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	Handle user changes to the bubble area					*/
/*										*/
/********************************************************************************/

private class BubbleUpdater implements BubbleViewCallback {

   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }

   @Override public void bubbleAdded(BudaBubble bb) {
      if (BudaRoot.findBudaBubbleArea(bb) != bubble_area) return;
      if (bubble_map.get(bb) == null) {
	 bubble_map.put(bb,new BubbleData(bb));
       }
    }

   @Override public void bubbleRemoved(BudaBubble bb) {
      bubble_map.remove(bb);
    }

   @Override public boolean bubbleActionDone(BudaBubble bb)		{ return false; }

}



/********************************************************************************/
/*										*/
/*	Information associated with a bubble					*/
/*										*/
/********************************************************************************/

private static class BubbleData {

   private BumpThread	base_thread;
   private BumpThreadStack for_stack;
   private BumpStackFrame for_frame;
   private int		frame_level;
   private BudaBubble	for_bubble;
   private BubbleType	bubble_type;
   private long 	last_used;
   private boolean	can_remove;
   private BudaBubble	assoc_bubble;

   BubbleData(BudaBubble bb) {
      for_bubble = bb;
      base_thread = null;
      for_stack = null;
      for_frame = null;
      frame_level = -1;
      if (bb instanceof BddtConsoleBubble) bubble_type = BubbleType.CONSOLE;
      else if (bb instanceof BddtLaunchControl) bubble_type = BubbleType.BDDT;
      else if (bb instanceof BddtThreadView) bubble_type = BubbleType.THREADS;
      else if (bb instanceof BddtHistoryBubble) bubble_type = BubbleType.HISTORY;
      else if (bb instanceof BddtStopTraceBubble) bubble_type = BubbleType.STOP_TRACE;
      else bubble_type = BubbleType.USER;
      last_used = System.currentTimeMillis();
      can_remove = false;
      assoc_bubble = null;
    }

   BubbleData(BudaBubble bb,BumpThread bt,BumpThreadStack stk,BumpStackFrame sf,BubbleType typ) {
      for_bubble = bb;
      base_thread = bt;
      for_stack = stk;
      for_frame = sf;
      frame_level = -1;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 if (stk.getFrame(i) == for_frame) {
	    frame_level = i;
	    break;
	  }
       }
      bubble_type = typ;
      last_used = System.currentTimeMillis();
      can_remove = true;
      assoc_bubble = null;
    }

   long getLastTime()					{ return last_used; }
   BumpThread getThread()				{ return base_thread; }
   boolean canRemove()					{ return can_remove; }
   BudaBubble getBubble()				{ return for_bubble; }
   int getLineNumber()					{ return for_frame.getLineNumber(); }
   BumpStackFrame getFrame()				{ return for_frame; }
   BubbleType getBubbleType()				{ return bubble_type; }

   void setAssocBubble(BudaBubble bb)			{ assoc_bubble = bb; }
   BudaBubble getAssocBubble()				{ return assoc_bubble; }

   void update(BumpThreadStack stk,BumpStackFrame sf) {
      last_used = System.currentTimeMillis();
      for_stack = stk;
      for_frame = sf;
      if (for_bubble instanceof BddtLibraryBubble) {
	 BddtLibraryBubble lbb = (BddtLibraryBubble) for_bubble;
	 lbb.resetFrame(sf);
       }
    }

   boolean match(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm) {
      if (bt != base_thread) return false;
      if (bubble_type != BubbleType.EXEC) return false;
      int lvl = -1;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 if (stk.getFrame(i) == frm) {
	    lvl = i;
	    break;
	  }
       }
      if (lvl != frame_level) return false;
      return matchFrameMethod(frm,for_frame);
    }

   boolean matchUser(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm) {
      if (base_thread != null) return false;
      if (bubble_type != BubbleType.USER) return false;
      if (for_bubble.getContentType() != BudaContentNameType.METHOD) return false;
      String s1 = for_bubble.getContentName();
      if (s1 == null) return false;
      int idx1 = s1.indexOf("(");
      String s1a = s1.substring(0,idx1);
      String s1b = s1.substring(idx1);
      if (!s1a.equals(frm.getMethod())) return false;
      if (!BumpLocation.compareParameters(s1b,frm.getSignature())) return false;

      int lvl = -1;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 if (stk.getFrame(i) == frm) {
	    lvl = i;
	    break;
	  }
       }
      // take over user bubble here
      for_stack = stk;
      for_frame = frm;
      base_thread = stk.getThread();
      frame_level = lvl;
      return true;
    }

   boolean isBelow(BubbleData bd) {
      if (base_thread != bd.base_thread) return false;
      if (for_stack.getNumFrames() < bd.for_stack.getNumFrames()) return false;
      return true;
    }

   int aboveLevel(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm) {
      if (base_thread != bt || frame_level < 0) return -1;

      int ct0 = for_stack.getNumFrames();
      int ct1 = stk.getNumFrames();
      for (int i = ct0-1; i >= frame_level; --i) {
	 int j = ct1 - (ct0 - i);
	 BumpStackFrame bsf0 = for_stack.getFrame(i);
	 BumpStackFrame bsf1 = stk.getFrame(j);
	 if (bsf1 == frm) return -1;
	 if (!matchFrameMethod(bsf0,bsf1)) return -1;
       }
      return ct0 - frame_level;
    }

}	// end of inner class BubbleData



}	// end of class BddtBubbleManager




/* end of BddtBreakpointBubble.java */

