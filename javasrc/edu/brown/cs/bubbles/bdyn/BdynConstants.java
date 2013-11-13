/********************************************************************************/
/*										*/
/*		BdynConstants.java						*/
/*										*/
/*	Constants for dynamic visualizations in Bubbles 			*/
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

import edu.brown.cs.bubbles.bump.BumpConstants;

import java.util.*;


public interface BdynConstants extends BumpConstants
{


/********************************************************************************/
/*										*/
/*	Trie Node access							*/
/*										*/
/********************************************************************************/

interface TrieNode {
   TrieNode getParent();
   Collection<TrieNode> getChildren();
   int [] getCounts();
   Collection<BumpThread> getThreads();
   int [] getThreadCounts(BumpThread th);

   String getClassName();
   String getMethodName();
   int getLineNumber();
   String getFileName();
}



/********************************************************************************/
/*										*/
/*     Array elements for counts						*/
/*										*/
/********************************************************************************/

int OP_RUN = 0;
int OP_IO = 1;
int OP_WAIT = 2;
int OP_COUNT = 3;



/********************************************************************************/
/*										*/
/*	Callback data								*/
/*										*/
/********************************************************************************/


enum CallbackType {
   UNKNOWN,
   EVENT,			// event handler
   CONSTRUCTOR, 	// constructor for event recognition
};



interface BdynCallback {
   String getClassName();
   String getMethodName();
   int getId();
   CallbackType getCallbackType();
}



/********************************************************************************/
/*										*/
/*	Graph data								*/
/*										*/
/********************************************************************************/

interface BdynEntry {
   long getStartTime();
   long getEndTime();
   BdynEntryThread getEntryThread();
   BdynCallback getEntryTask();
   BdynCallback getEntryTransaction();
   
}

interface BdynEntryThread {
   String getThreadName();
}

interface BdynEntryTask {
   BdynCallback getTaskRoot();
}


class BdynRangeSet extends HashMap<BdynEntryThread,Set<BdynEntry>> { }


interface BdynEventUpdater extends EventListener {
   
   void eventsAdded();
   
}


/********************************************************************************/
/*										*/
/*	Files									*/
/*										*/
/********************************************************************************/

String BDYN_CALLBACK_FILE = "callbacks.xml";
String BDYN_BANDAID_FILE = "tracedata.bandaid";


}	// end of interface BdynConstants




/* end of BdynConstants.java */

