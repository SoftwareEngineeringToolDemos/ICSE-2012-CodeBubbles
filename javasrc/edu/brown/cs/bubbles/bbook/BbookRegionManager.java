/********************************************************************************/
/*										*/
/*		BbookRegionManager.java 					*/
/*										*/
/*	Manager of task regions for programmers notebook display		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bbook;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bnote.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.io.File;


public class BbookRegionManager implements BbookConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<TaskRegion>	task_regions;
private boolean 		config_done;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookRegionManager()
{
   task_regions = new ArrayList<TaskRegion>();
   config_done = false;

   BudaRoot.addBubbleViewCallback(new BubbleViewer());
}



/********************************************************************************/
/*										*/
/*	Bubble management helper methods					*/
/*										*/
/********************************************************************************/

private static boolean isBubbleRelevant(BudaBubble bb)
{
   if (bb.isTransient() || bb.isFloating() || (bb.isFixed() && !bb.isUserPos())) return false;
   if (bb.getContentProject() == null) return false;
   if (bb.getContentName() == null) return false;
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   if (bba == null || !bba.isPrimaryArea()) return false;
   return true;
}




/********************************************************************************/
/*										*/
/*	Bubble view callback handler						*/
/*										*/
/********************************************************************************/

private class BubbleViewer implements BudaConstants.BubbleViewCallback
{
   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }

   @Override public boolean bubbleActionDone(BudaBubble bb)		{ return false; }

   @Override public void bubbleAdded(BudaBubble bb) {
      if (isBubbleRelevant(bb)) handleBubbleAdded(bb);
    }

   @Override public void bubbleRemoved(BudaBubble bb) {
      handleBubbleRemoved(bb);
    }

   @Override public void workingSetAdded(BudaWorkingSet ws) {
      if (isRelevant(ws)) handleWorkingSetAdded(ws);
    }

   @Override public void workingSetRemoved(BudaWorkingSet ws) {
      if (isRelevant(ws)) handleWorkingSetRemoved(ws);
    }

   @Override public void doneConfiguration() {
      config_done = true;
    }

   private boolean isRelevant(BudaWorkingSet ws) {
      BudaBubbleArea bba = ws.getBubbleArea();
      if (!bba.isPrimaryArea()) return false;
      return true;
    }

}	// end of inner class BubbleViewer




/********************************************************************************/
/*										*/
/*	Locate task regions							*/
/*										*/
/********************************************************************************/

TaskRegion findTaskRegion(BudaBubble bb)
{
   if (bb == null) return null;

   for (TaskRegion tr : task_regions) {
      if (tr.contains(bb)) return tr;
    }

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   if (bba == null) return null;

   Rectangle r1 = BudaRoot.findBudaLocation(bb);
   if (r1 == null) return null;

   return findTaskRegion(bba,r1);
}


TaskRegion findTaskRegion(BudaBubbleArea bba,Rectangle r1)
{
   if (r1 == null) return null;

   for (TaskRegion tr : task_regions) {
      if (tr.contains(bba,r1)) return tr;
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Task region updating							*/
/*										*/
/********************************************************************************/

void handleBubbleAdded(BudaBubble bb)
{
   TaskRegion tr = findTaskRegion(bb);
   if (tr != null) {
      tr.addBubble(bb);
      BnoteTask task = tr.getTask();
      if (task != null) {
	 String b1 = bb.getContentProject();
	 String b2 = bb.getContentName();
	 File f3 = bb.getContentFile();
	 if (b1 != null && b2 != null && f3 != null) {
	    BnoteStore.log(task.getProject(),task,BnoteEntryType.OPEN,"INPROJECT",b1,"NAME",b2,"File",f3.getPath());
	  }
       }
      return;
    }

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   tr = new TaskRegion(bb);
   task_regions.add(tr);

   if (config_done) {
      BbookFactory.getFactory().createTaskSelector(bba,bb,null,bb.getContentProject());
    }
}



void handleBubbleRemoved(BudaBubble bb)
{
   TaskRegion tr = findTaskRegion(bb);
   if (tr == null) return;

   BnoteTask task = tr.getTask();
   if (task != null) {
      String b1 = bb.getContentProject();
      String b2 = bb.getContentName();
      File f3 = bb.getContentFile();
      if (b1 != null && b2 != null && f3 != null) {
	 BnoteStore.log(task.getProject(),task,BnoteEntryType.CLOSE,"INPROJECT",b1,"NAME",b2,"File",f3.getPath());
      }
    }
   

   if (tr.removeBubble(bb)) task_regions.remove(tr);
}


void handleWorkingSetAdded(BudaWorkingSet ws)
{
   for (TaskRegion tr : task_regions) {
      if (tr.contains(ws)) {
	 tr.setWorkingSet(ws);
       }
    }
}


void handleWorkingSetRemoved(BudaWorkingSet ws)
{
   for (TaskRegion tr : task_regions) {
      if (tr.getWorkingSet() == ws) {
	 tr.setWorkingSet(null);
       }
    }
}



void handleSetTask(BnoteTask task,BudaBubbleArea bba,Rectangle r)
{
   TaskRegion tr = findTaskRegion(bba,r);
   if (tr == null) {
      tr = new TaskRegion(bba,r);
      if (tr.size() == 0) return;
      task_regions.add(tr);
    }
   tr.setTask(task);
}



/********************************************************************************/
/*										*/
/*	Task output and setup							*/
/*										*/
/********************************************************************************/

void outputTasks(BudaXmlWriter xw)
{
   for (TaskRegion tr : task_regions) {
      tr.outputXml(xw);
    }
}



void loadTasks(Element xml,BudaBubbleArea bba)
{

   for (Element e : IvyXml.children(xml,"REGION")) {
      int tid = IvyXml.getAttrInt(e,"TASK");
      BnoteTask task = BnoteFactory.getFactory().findTaskById(tid);
      if (task != null) {
	 Element ar = IvyXml.getChild(e,"AREA");
	 Rectangle r = new Rectangle(IvyXml.getAttrInt(ar,"X"),IvyXml.getAttrInt(ar,"Y"),
					IvyXml.getAttrInt(ar,"WIDTH"),IvyXml.getAttrInt(ar,"HEIGHT"));
	  TaskRegion tr = findTaskRegion(bba,r);
	   if (tr == null) {
	      tr = new TaskRegion(bba,r);
	      task_regions.add(tr);
	    }
	   tr.setTask(task);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Task region information 						*/
/*										*/
/********************************************************************************/

private class TaskRegion implements BbookRegion
{
   private BudaBubbleArea bubble_area;
   private BnoteTask region_task;
   private Rectangle region_area;
   private Set<BudaBubble> active_bubbles;
   private BudaWorkingSet working_set;

   TaskRegion(BudaBubble bb) {
      bubble_area = BudaRoot.findBudaBubbleArea(bb);
      region_task = null;
      region_area = bubble_area.computeRegion(bb);
      initializeBubbles(bb);
    }

   TaskRegion(BudaBubbleArea bba,Rectangle r) {
      bubble_area = bba;
      region_task = null;
      region_area = bubble_area.computeRegion(r);
      initializeBubbles(null);
    }

   private void initializeBubbles(BudaBubble b0) {
      active_bubbles = new HashSet<BudaBubble>();
      for (BudaBubble bb : bubble_area.getBubblesInRegion(region_area)) {
	 if (isBubbleRelevant(bb)) {
	    if (b0 == null) b0 = bb;
	    active_bubbles.add(bb);
	  }
       }
      working_set = null;
      if (b0 != null) {
	 working_set = bubble_area.findWorkingSetForBubble(b0);
       }
    }

   int size()				{ return active_bubbles.size(); }
   BnoteTask getTask()			{ return region_task; }

   boolean contains(BudaBubble bb) {
      return active_bubbles.contains(bb);
    }

   boolean contains(BudaBubbleArea bba,Rectangle r1) {
      if (bba != bubble_area) return false;
      int space = bubble_area.getRegionSpace();
      if (r1.x + r1.width >= region_area.x - space &&
	     r1.x < region_area.x + region_area.width + space)
	 return true;
      return false;
    }

   boolean contains(BudaWorkingSet ws) {
      if (ws == working_set) return true;
      if (ws.getBubbleArea() != bubble_area) return false;
      Rectangle r1 = ws.getRegion();
      if (r1.x <= region_area.x + region_area.width && r1.x + r1.width >= region_area.x)
	 return true;
      return false;
    }

   void addBubble(BudaBubble bb) {
      region_area = bubble_area.computeRegion(bb);
      active_bubbles.add(bb);
    }

   boolean removeBubble(BudaBubble nbb) {
      if (!active_bubbles.remove(nbb)) return false;
      Rectangle r0 = null;
      for (BudaBubble bb : active_bubbles) {
	 Rectangle r1 = BudaRoot.findBudaLocation(bb);
	 if (r1 == null) continue;
	 if (r0 == null) r0 = new Rectangle(r1);
	 else r0 = r0.union(r1);
       }
      if (r0 == null) return true;
      region_area = bubble_area.computeRegion(r0);
      return false;
    }

   void setWorkingSet(BudaWorkingSet ws)		{ working_set = ws; }
   BudaWorkingSet getWorkingSet()			{ return working_set; }

   void setTask(BnoteTask task) 			{ region_task = task; }

   void outputXml(BudaXmlWriter xw) {
      if (region_task != null) {
	 xw.begin("REGION");
	 xw.field("TASK",region_task.getTaskId());
	 xw.element("AREA",region_area);
	 xw.end("REGION");
       }
    }

}	// end of inner class TaskRegion



}	// end of class BbookRegionManager




/* end of BbookRegionManager.java */

