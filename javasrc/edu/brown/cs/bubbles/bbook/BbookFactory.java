/********************************************************************************/
/*										*/
/*		BbookFactory.java						*/
/*										*/
/*	Factory for setting up programmers notebook display			*/
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

import java.awt.*;


public class BbookFactory implements BbookConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BbookTasker		task_manager;
private BubbleViewer		view_checker;

private static BbookFactory	the_factory = new BbookFactory();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BbookFactory()
{
   task_manager = new BbookTasker();
   view_checker = new BubbleViewer();
}



public static BbookFactory getFactory() 	{ return the_factory; }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
}


public static void initialize(BudaRoot br)
{
   // should wait until configuration is set up
   BudaRoot.addBubbleViewCallback(getFactory().view_checker);
}


/********************************************************************************/
/*										*/
/*	Bubble view callback handler						*/
/*										*/
/********************************************************************************/

private class BubbleViewer implements BudaConstants.BubbleViewCallback
{
   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }

   @Override public void bubbleRemoved(BudaBubble bb)			{ }

   @Override public boolean bubbleActionDone(BudaBubble bb)		{ return false; }

   @Override public void bubbleAdded(BudaBubble bb) {
      if (bb.isTransient() || bb.isFloating()) return;
      if (bb.getContentProject() == null) return;
      if (bb.getContentName() == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      if (!bba.isPrimaryArea()) return;
      Rectangle rgn = bba.computeRegion(bb);
      int ctr = 0;
      for (BudaBubble xbb : bba.getBubblesInRegion(rgn)) {
	 if (xbb.isFloating() || xbb.isTransient()) continue;
	 if (xbb != bb) ++ctr;
       }
      if (ctr == 0) {
	 Rectangle r = BudaRoot.findBudaLocation(bb);
	 Point pt = new Point(r.x + 20,r.y + 20);
	 task_manager.createTaskSelector(bba,pt,bb.getContentProject());
	 // pop up initial query
       }
    }

}	// end of inner class BubbleViewer



}	// end of class BbookFactory




/* end of BbookFactory.java */

