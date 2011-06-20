/********************************************************************************/
/*										*/
/*		BgtaUtil.java							*/
/*										*/
/*	Chat utility functions                                  		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs      		      */
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

package edu.brown.cs.bubbles.bgta;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.packet.Presence;
public class BgtaUtil {

   /**
    * Returns a list of XMPP resources that are logged into the bare JID
    * at the given RosterEntry
    */
   public static List<String> getFullJIDsForRosterEntry(Roster r, String bare_jid)
   {
      ArrayList<String> jids = new ArrayList<String>();
      Iterator<Presence> it = r.getPresences(bare_jid);
      while(it.hasNext()){
         Presence p = it.next();
         jids.add(p.getFrom());
         System.out.println(p.toXML());
      }

      return jids;   
   }
}
