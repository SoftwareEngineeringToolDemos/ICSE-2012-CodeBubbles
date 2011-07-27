/********************************************************************************/
/*                         						 											  */
/*    		BeduCourse.java     	            											  */
/*                            																  */
/* 	Bubbles for Education   																  */
/* 	Represents a school course		 	      											  */
/* 				               																  */
/********************************************************************************/
/* 	Copyright 2011 Brown University -- Andrew Kovacs         					  */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

package edu.brown.cs.bubbles.bedu.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassRepositoryMerge;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.board.BoardProperties;


class BeduCourseRepository implements BassRepository {
private static List<BeduCourse>     courses;
private static BeduCourseRepository instance;
private final static String	 PROP_PREFIX = "Bedu.chat.course.";


static BeduCourseRepository getInstance()
{
   if (instance == null) initialize();
   return instance;
}


/**
 * Initializes the CourseRepository by 
 * having it load courses from the props file 
 * and registering itself for display in the 
 * package explorer 
 */
static void initialize()
{
   instance = new BeduCourseRepository();
   courses = new ArrayList<BeduCourse>();

   // grab courses out of the props file
   BoardProperties bp = BoardProperties.getProperties("Bedu");
   Set<String> courseNames = new HashSet<String>();

   // gather course names
   for (String s : bp.stringPropertyNames()) {
      // Property name should be in the form
      // Educhat.course.CS15.ta_jid
      if (s.startsWith("Bedu.chat.course.")) courseNames.add(s.split("\\.")[3]);
   }

   for (String courseName : courseNames) {
      BeduCourse c = null;
      String strRole = bp.getProperty(PROP_PREFIX + courseName + ".role");

      String ta_jid = bp.getProperty(PROP_PREFIX + courseName + ".ta_jid");

      if (strRole.equals("STUDENT")) {
	 c = new BeduCourse.StudentCourse(courseName,ta_jid);
      }

      if (strRole.equals("TA")) {
	 String xmpp_password = bp.getProperty(PROP_PREFIX + courseName
		  + ".xmpp_password");
	 String server = bp.getProperty(PROP_PREFIX + courseName + ".server");

	 c = new BeduCourse.TACourse(courseName,ta_jid,xmpp_password,server);
      }

      if (c != null) courses.add(c);
   }

   BassRepositoryMerge fullRepo = new BassRepositoryMerge(
	    new BeduManageCoursesRepository(),instance);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER, fullRepo);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_COURSES, instance);
}


@SuppressWarnings(value = "unchecked") @Override public Iterable<BassName> getAllNames()
{
   return new ArrayList<BassName>((List<BassName>) (List<?>) courses);
}


@Override public boolean includesRepository(BassRepository br)
{
   return (br == this);
}

private String coursePrefix(BeduCourse c)
{
   return PROP_PREFIX + c.getCourseName() + ".";
}

void addCourse(BeduCourse c)
{
   BoardProperties bp = BoardProperties.getProperties("Bedu");
   courses.add(c);
   bp.setProperty(coursePrefix(c) + "ta_jid", c.getTAJID());
   if (c instanceof BeduCourse.StudentCourse) {
      bp.setProperty(coursePrefix(c) + "role", "STUDENT");
   }

   if (c instanceof BeduCourse.TACourse) {
      BeduCourse.TACourse tc = (BeduCourse.TACourse) c;
      bp.setProperty(coursePrefix(c) + "role", "TA");
      bp.setProperty(coursePrefix(c) + "xmpp_password", tc.getXMPPPassword());
      bp.setProperty(coursePrefix(c) + "server", tc.getXMPPServer());
   }
   
   BassFactory.reloadRepository(this);
}

void removeCourse(BeduCourse c)
{
   BoardProperties bp = BoardProperties.getProperties("Bedu");
   bp.remove(coursePrefix(c) + "ta_jid");
   bp.remove(coursePrefix(c) + "role");
   courses.remove(c);
   if (c instanceof BeduCourse.TACourse) {
      bp.remove(coursePrefix(c) + "xmpp_password");
      bp.remove(coursePrefix(c) + "server");
   }
   
   BassFactory.reloadRepository(this);
}

void saveConfigFile() throws IOException
{
   BoardProperties.getProperties("Bedu").save();
}
}
