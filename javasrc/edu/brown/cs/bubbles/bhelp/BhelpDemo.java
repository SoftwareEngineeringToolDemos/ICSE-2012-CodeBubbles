/********************************************************************************/
/*										*/
/*		BhelpDemo.java							*/
/*										*/
/*	Demonstration root class for bubbles help demonstrations		*/
/*										*/
/********************************************************************************/
/*	Copyright 2012 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2012, Brown University, Providence, RI.				 *
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



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import java.util.*;



class BhelpDemo implements BhelpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		demo_name;
private List<BhelpAction> help_actions;

private static Boolean	doing_demo = Boolean.FALSE;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BhelpDemo(Element xml)
{
   demo_name = IvyXml.getAttrString(xml,"NAME");
   help_actions = new ArrayList<BhelpAction>();
   for (Element ea : IvyXml.children(xml,"ACTION")) {
      BhelpAction act = BhelpAction.createAction(ea);
      if (act != null) help_actions.add(act);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return demo_name; }


/********************************************************************************/
/*										*/
/*	Execution methods							*/
/*										*/
/********************************************************************************/

void executeDemo(BudaBubbleArea bba)
{
    BhelpContext ctx = new BhelpContext(bba);

    synchronized (doing_demo) {
       if (doing_demo) return;		// can't do more than one
       doing_demo = true;
     }

    DemoRun dr = new DemoRun(ctx);
    BoardThreadPool.start(dr);
}



private class DemoRun implements Runnable {

   private BhelpContext using_context;

   DemoRun(BhelpContext ctx) {
      using_context = ctx;
   }

   @Override public void run() {
      try {
	 for (BhelpAction ba : help_actions) {
	    ba.executeAction(using_context);
	 }
      }
      catch (BhelpException e) {
	 BoardLog.logE("BHELP","Help execution aborted: " + e);
      } 
      finally {
	 synchronized (doing_demo) {
	    doing_demo = false;
	 }
      }
   }

}	// end of inner class DemoRun



}	// end of class BhelpDemo




/* end of BhelpDemo.java */
