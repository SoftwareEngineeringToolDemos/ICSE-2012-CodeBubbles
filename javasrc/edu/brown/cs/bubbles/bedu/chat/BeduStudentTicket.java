/********************************************************************************/
/*										                                                  */
/*		StudentTicket.java						                                      */
/*										     															  */
/*		This class represents a request for help from a student						  */
 /*		to a TA		 																			  */
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs									  */
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

import java.util.Date;

class BeduStudentTicket {
private String text;
private Date   timestamp;
private String studentJID;



BeduStudentTicket(String txt, Date time, String jid) {
   text = txt;
   timestamp = time;
   studentJID = jid;
}


/**
 * Hashcode of the text contained in the ticket
 * @return
 */
int textHash() {
   return text.hashCode();
}


String getText() {
   return text;
}


/**
 * The time the ticket was received 
 * @return
 */
Date getTimestamp() {
   return timestamp;
}


/**
 * The XMPP JID of the student
 * who submitted the ticket
 * @return
 */
String getStudentJID() {
   return studentJID;
}



@Override
public int hashCode() {
   final int prime = 31;
   int result = 1;
   result = prime * result + ((text == null) ? 0 : text.hashCode());
   return result;
}



@Override
public boolean equals(Object obj) {
   if (this == obj)
      return true;
   if (obj == null)
      return false;
   if (getClass() != obj.getClass())
      return false;
   BeduStudentTicket other = (BeduStudentTicket) obj;
   if (text == null) {
      if (other.text != null)
         return false;
   } else if (!text.equals(other.text))
      return false;
   return true;
}

}
