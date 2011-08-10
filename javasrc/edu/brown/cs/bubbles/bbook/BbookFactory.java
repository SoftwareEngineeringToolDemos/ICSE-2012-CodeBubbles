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

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;



public class BbookFactory implements BbookConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BbookTasker		task_manager;
private BbookRegionManager	region_manager;

private static BbookFactory	the_factory = new BbookFactory();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BbookFactory()
{
   task_manager = new BbookTasker();
   region_manager = new BbookRegionManager();
}



public static BbookFactory getFactory() 	{ return the_factory; }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   BudaRoot.addBubbleConfigurator("BBOOK",new TaskConfig());
}


public static void initialize(BudaRoot br)
{
   br.registerKeyAction(new BbookAction(br),"Programmer's Log",
			   KeyStroke.getKeyStroke(KeyEvent.VK_F2,0));
}


/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

void createTaskSelector(BudaBubbleArea bba,BudaBubble bb,Point pt,String proj)
{
   task_manager.createTaskSelector(bba,bb,pt,proj);
}


BbookRegion findTaskRegion(BudaBubbleArea bba,Rectangle r0)
{
   return region_manager.findTaskRegion(bba,r0);
}



/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

void handleSetTask(BnoteTask task,BudaBubbleArea bba,Rectangle loc)
{
   region_manager.handleSetTask(task,bba,loc);
}




/********************************************************************************/
/*										*/
/*	Task request action							*/
/*										*/
/********************************************************************************/

private static class BbookAction extends AbstractAction {

   private BudaRoot buda_root;

   BbookAction(BudaRoot br) {
      super("ProgrammersLogAction");
      buda_root = br;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
      if (!bba.isPrimaryArea()) return;
      Point pt = bba.getCurrentMouse();
      Rectangle r0 = new Rectangle(pt);
      BbookRegion tr = getFactory().findTaskRegion(bba,r0);
      if (tr == null) return;
      getFactory().task_manager.createTaskSelector(bba,null,pt,null);
    }

}	// end of inner class BbookAction




/********************************************************************************/
/*										*/
/*	Configurator for saving task set					*/
/*										*/
/********************************************************************************/

private static class TaskConfig implements BubbleConfigurator {

   @Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml) {
      return null;
    }

   @Override public void outputXml(BudaXmlWriter xw,boolean history) {
      if (history) return;
      xw.begin("BBOOK");
      getFactory().region_manager.outputTasks(xw);
      xw.end("BBOOK");
    }

   @Override public void loadXml(BudaBubbleArea bba,Element root) {
      if (bba == null) return;		// history loading
      Element e = IvyXml.getChild(root,"BBOOK");
      if (e == null) return;
      getFactory().region_manager.loadTasks(e,bba);
    }

}	// end of inner class TaskConfig



}	// end of class BbookFactory




/* end of BbookFactory.java */

