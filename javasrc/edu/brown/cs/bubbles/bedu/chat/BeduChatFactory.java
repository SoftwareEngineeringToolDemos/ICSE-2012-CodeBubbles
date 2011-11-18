/********************************************************************************/
/*										*/
/*		BeduChatFactory.java						*/
/*										*/
/*	Bubbles for Education							*/
/*	Keeps track of edu chat sessions					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

package edu.brown.cs.bubbles.bedu.chat;

import java.util.List;

import java.util.ArrayList;

import edu.brown.cs.bubbles.board.*;


public class BeduChatFactory {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

static boolean			 DEBUG = false;
private static List<BeduTAXMPPClient> ta_sessions;

static {
   ta_sessions = new ArrayList<BeduTAXMPPClient>();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   String course = BoardSetup.getSetup().getCourseName();
   if (course == null) return;

   BoardProperties bp = BoardProperties.getProperties("Bedu");
   String pnm = "Bedu.chat." + course;

   if (bp.getProperty(pnm) == null) return;

   if (!bp.getProperty(pnm).equals("false"))
      BeduCourseRepository.initialize();
}




/**
 * Returns the XMPPClient associated with the given course which the current
 * user is a TA of
 *
 * Constructs it if it doesn't already exist
 *
 * @param course
 * @return
 */
static BeduTAXMPPClient getTAXMPPClientForCourse(BeduCourse.TACourse course)
{
   BeduTAXMPPClient client = null;
   for (BeduTAXMPPClient c : ta_sessions) {
      if (c.getCourse().equals(course)) client = c;
   }

   if (client == null) {
      client = new BeduTAXMPPClient(course);
      ta_sessions.add(client);
   }

   return client;

}

} // end of class BeduChatFactgory



/* end of BeduChatFactory.java */
