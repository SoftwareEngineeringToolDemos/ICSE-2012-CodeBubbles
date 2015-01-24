/********************************************************************************/
/*                                                                              */
/*              NobaseElider.java                                               */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.*;
import java.util.List;

class NobaseElider implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<ElidePriority> elide_pdata;
private List<ElideRegion> elide_rdata;

private static final double	UP_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_COUNT = 0.95;
private static final double	DOWN_DEFAULT_ITEM  = 0.99;


   
   
   
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseElider()
{
   elide_pdata = new ArrayList<ElidePriority>();
   elide_rdata = new ArrayList<ElideRegion>();
}



/********************************************************************************/
/*										*/
/*	Methods for maintaining elision information				*/
/*										*/
/********************************************************************************/


void clearElideData()
{
   elide_pdata.clear();
   elide_rdata.clear();
}



void addElidePriority(int soff,int eoff,double pri)
{
   ElidePriority ed = new ElidePriority(soff,eoff,pri);
   elide_pdata.add(ed);
}


void addElideRegion(int soff,int eoff)
{
   ElideRegion er = new ElideRegion(soff,eoff);
   elide_rdata.add(er);
}



void noteEdit(int soff,int len,int rlen)
{
   for (Iterator<ElidePriority> it = elide_pdata.iterator(); it.hasNext(); ) {
      ElidePriority ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }
   
   for (Iterator<ElideRegion> it = elide_rdata.iterator(); it.hasNext(); ) {
      ElideRegion ed = it.next();
      if (!ed.noteEdit(soff,len,rlen)) it.remove();
    }
}



/********************************************************************************/
/*										*/
/*	Elision computaton methods						*/
/*										*/
/********************************************************************************/

boolean computeElision(ISemanticData isd,IvyXmlWriter xw)
{
   return false;
}




/********************************************************************************/
/*										*/
/*	Access methods for elision information					*/
/*										*/
/********************************************************************************/

private boolean isActiveRegion(int soff,int len)
{
   for (ElideRegion er : elide_rdata) {
      if (er.overlaps(soff,len)) return true;
    }
   
   return false;
}



private boolean isRootRegion(int soff,int len)
{
   for (ElideRegion er : elide_rdata) {
      if (er.contains(soff,len)) return true;
    }
   
   return false;
}





/********************************************************************************/
/*										*/
/*	Main priority function							*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Formatting type function						*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Tree walk for setting initial priorities				*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Classes for elision region and priorities				*/
/*										*/
/********************************************************************************/

private abstract class ElideData {
   
   private int start_offset;
   private int end_offset;
   
   ElideData(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
    }
   
   boolean contains(int soff,int len) {
      return (start_offset <= soff && end_offset >= soff+len-1);
    }
   
   // boolean useForPriority(IFileData ifd,SimpleNode n) {
      // int sp = ifd.getStartOffset(n);
      // int ln = ifd.getLength(n);
      // if (sp == 0) return false;
      // if (start_offset != end_offset) return contains(sp,ln);
      // if (!overlaps(sp,ln)) return false;
      // 
      // 
      // return true;
    // }
   
   boolean overlaps(int soff,int len) {
      if (start_offset >= soff+len-1) return false;
      if (end_offset <= soff) return false;
      return true;
    }
   
   boolean noteEdit(int soff,int len,int rlen) {
      if (end_offset <= soff) ; 			// before the change
      else if (start_offset > soff + len - 1) { 	// after the change
	 start_offset += rlen - len;
	 end_offset += rlen - len;
       }
      else if (start_offset <= soff && end_offset >= soff+len-1) {	// containing the change
	 end_offset += rlen -len;
       }
      else return false;				     // in the edit -- remove it
      return true;
    }
   
}	// end of inner abstract class ElideData




private class ElideRegion extends ElideData {
   
   ElideRegion(int soff,int eoff) {
      super(soff,eoff);
    }
   
}	// end of innerclass ElideData




private class ElidePriority extends ElideData {
   
   private double elide_priority;
   
   ElidePriority(int soff,int eoff,double pri) {
      super(soff,eoff);
      elide_priority = pri;
    }
   
   double getPriority() 			{ return elide_priority; }
   
}	// end of innerclass ElideData




}       // end of class NobaseElider




/* end of NobaseElider.java */

