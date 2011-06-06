/********************************************************************************/
/*										*/
/*		BattConstants.java						*/
/*										*/
/*	Bubble Automated Testing Tool constant definitions			*/
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


package edu.brown.cs.bubbles.batt;


import java.util.EventListener;



public interface BattConstants {


/********************************************************************************/
/*										*/
/*	File States								*/
/*										*/
/********************************************************************************/

enum FileState {
   STABLE,
   EDITED,
   CHANGED,
   ERRORS;

   public FileState merge(FileState fs) {
      if (fs == null) return this;
      if (fs.ordinal() > ordinal()) return fs;
      return this;
    }

}




/********************************************************************************/
/*										*/
/*	Testing mode								*/
/*										*/
/********************************************************************************/

enum TestMode {
   ON_DEMAND,
   CONTINUOUS
}



/********************************************************************************/
/*										*/
/*	Test Status								*/
/*										*/
/********************************************************************************/

enum TestStatus {
   UNKNOWN,
   SUCCESS,
   FAILURE
}


enum TestState {
   UNKNOWN,
   PENDING,
   RUNNING,
   EDITED,
   NEEDS_CHECK,
   CANT_RUN,
   UP_TO_DATE
}



enum RunType {
   ALL,
   FAIL,
   PENDING
}



/********************************************************************************/
/*										*/
/*	Menu and button definitions						*/
/*										*/
/********************************************************************************/

String TEST_BUTTON = "Test Management";



enum DisplayMode {
   FAIL,
   NEEDED,
   PENDING,
   SUCCESS,
   ALL
}


interface BattModelListener extends EventListener {

   void battModelUpdated(BattModeler bm);

}



}	// end of interface BattConstants




/* end of BattConstants.java */
