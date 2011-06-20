/********************************************************************************/
/*										*/
/*		StudentTicket.java						*/
/*										*/
/*		This class represents a request for help from a student
/*		to a TA		 						*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

package edu.brown.cs.bubbles.bgta.educhat;

import java.util.Date;

public class StudentTicket {
   private String text;
   private Date timestamp;
   private String studentJID;
   
   public StudentTicket(String txt, Date time, String jid)
   {
      text = txt;
      timestamp = time;
      studentJID = jid;
   }
   
   int textHash()
   {
      return text.hashCode();
   }
   
   public String getText()
   {
      return text;
   }
   
   public Date getTimestamp()
   {
      return timestamp;
   }
   
   String getStudentJID()
   {
      return studentJID;
   }
}
