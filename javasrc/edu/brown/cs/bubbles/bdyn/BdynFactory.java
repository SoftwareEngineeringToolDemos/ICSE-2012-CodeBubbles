/********************************************************************************/
/*										*/
/*		BdynFactory.java						*/
/*										*/
/*	Factory for Bubbles DYNamic views					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bdyn;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.banal.*;

import org.w3c.dom.*;
import java.util.*;
import java.awt.Point;


public class BdynFactory implements BdynConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BumpProcess,BdynProcess>    process_map;
private BdynCallbacks                   callback_set;

private static BdynFactory      the_factory = new BdynFactory();




/********************************************************************************/
/*                                                                              */
/*      Setup Methods                                                           */
/*                                                                              */
/********************************************************************************/

public static void setup()
{
}



public static void initialize(BudaRoot br)
{
   BudaRoot.registerMenuButton("Bubble.Compute Package Hierarchy",
         new HierarchyAction());
   getFactory().callback_set.setup();
}

/**
 *      Return the singleton instance of the factory
 **/

public static BdynFactory getFactory()
{
   return the_factory;
}
   


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BdynFactory()
{
   process_map = new HashMap<BumpProcess,BdynProcess>();
   callback_set = new BdynCallbacks();
   ProcessHandler ph = new ProcessHandler();
   BumpClient.getBump().getRunModel().addRunEventHandler(ph);   
}



/********************************************************************************/
/*                                                                              */
/*      Handle new processes                                                    */
/*                                                                              */
/********************************************************************************/

private void setupProcess(BumpRunEvent evt)
{
   BdynProcess bp = new BdynProcess(evt.getProcess());
   process_map.put(evt.getProcess(),bp);
}



private class ProcessHandler implements BumpRunEventHandler {
   
   @Override public void handleLaunchEvent(BumpRunEvent evt)            { }
   
   @Override public void handleThreadEvent(BumpRunEvent evt)            { }
   
   @Override public void handleConsoleMessage(BumpProcess p,boolean err,boolean eof,String msg)
   { }
   
   @Override public synchronized void handleProcessEvent(BumpRunEvent evt) {
      BumpProcess proc = evt.getProcess();
      if (proc == null) return;
      BdynProcess bp = process_map.get(proc);
      
      switch (evt.getEventType()) {
         case PROCESS_ADD :
            if (bp == null) setupProcess(evt);
            break;
         case PROCESS_REMOVE :
            if (bp != null) {
               process_map.remove(proc);
               TrieNode tn = bp.getTrieRoot();
               if (tn != null) callback_set.updateCallbacks(tn);
             }
            break;
         case PROCESS_PERFORMANCE :
            break;
         case PROCESS_SWING :
            break;
         case PROCESS_TRIE :
            if (bp != null) { 
               Element xml = (Element) evt.getEventData();
               bp.handleTrieEvent(xml);
             }
            break;
         default :
            break;
       }
    }

}       // end of inner class ProcessHandler




/********************************************************************************/
/*                                                                              */
/*      Button actions                                                          */
/*                                                                              */
/********************************************************************************/

private static class HierarchyAction implements BudaConstants.ButtonListener
{
   
   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BanalFactory bf = BanalFactory.getFactory();
      bf.computePackageHierarchy(null);
    }
}



}	// end of class BdynFactory




/* end of BdynFactory.java */

