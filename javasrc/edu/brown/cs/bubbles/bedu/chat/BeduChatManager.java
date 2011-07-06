/********************************************************************************/
/*                         						 								*/
/*    		BeduChatManager.java                 								*/
/*                            													*/
/* 	Bubbles for Education   													*/
/* 	Keeps track of edu chat sessions 	      									*/
/* 				               													*/
/********************************************************************************/
/* 	Copyright 2011 Brown University -- Andrew Kovacs         					*/
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

import java.util.List;

import javax.naming.OperationNotSupportedException;
import org.jivesoftware.smack.XMPPException;
import java.util.ArrayList;

public class BeduChatManager
{
private static List<BeduTAXMPPClient> ta_sessions;
private static BeduCourseRepository course_repository;

static {
	ta_sessions = new ArrayList<BeduTAXMPPClient>();
}


/**
 * Returns the XMPPClient associated with the given course 
 * which the current user is a TA of
 * 
 * Constructs it if it doesn't already exist 
 * @param course
 * @return
 */
public static BeduTAXMPPClient getTAXMPPClientForCourse(BeduCourse.TACourse course)
{
	BeduTAXMPPClient the_client = null;
	for (BeduTAXMPPClient c : ta_sessions) {
		if (c.getCourse().equals(course))
			the_client = c;
	}

	if (the_client == null) {
		the_client = new BeduTAXMPPClient(course);
		ta_sessions.add(the_client);
	}

	return the_client;

}

} // end of class EduchatManager

/* end of EduchatManager.java */
