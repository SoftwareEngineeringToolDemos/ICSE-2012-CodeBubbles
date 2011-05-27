/********************************************************************************/
/*										*/
/*		BudaTask.java							*/
/*										*/
/*	BUblles Display Area task (saved working set)				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.*;
import java.util.*;


class BudaTask implements BudaConstants, BoardConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String task_name;
private Element task_xml;
private String task_text;
// private Image task_image;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaTask(Element xml)
{
   task_name = IvyXml.getAttrString(xml,"NAME");
   task_xml = xml;
   task_text = null;
}



BudaTask(String nm,String txt)
{
   task_name = nm;
   task_xml = null;
   task_text = txt;
   //task_image = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return task_name; }

String getText()
{
   if (task_text == null) task_text = IvyXml.convertXmlToString(task_xml);
   return task_text;
}

Element getXml()
{
   if (task_xml == null) task_xml = IvyXml.convertStringToXml(task_text);
   return task_xml;
}


Date getDate()
{
   Element xml = getXml();
   Element wse = IvyXml.getChild(xml,"WORKINGSET");
   long ctime = IvyXml.getAttrLong(wse,"CREATE",0);

   return new Date(ctime);
}

//Image getImage() { return task_image; }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.writeXml(getXml());
}



/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

void loadTask(BudaBubbleArea bba,int offset)
{
   BudaRoot root = BudaRoot.findBudaRoot(bba);
   Rectangle arearect = bba.getBounds();

   Element te = getXml();
   Element wse = IvyXml.getChild(te,"WORKINGSET");
   long ctime = IvyXml.getAttrLong(wse,"CREATE",0);

   Color c = IvyXml.getAttrColor(wse,"BORDERCOLOR");
   if (c == null) {
      BoardLog.logE("BUDA","Problem reading working set color " + IvyXml.getAttrString(wse,"BORDERCOLOR"));
    }

   Element rgn = IvyXml.getChild(wse,"REGION");
   int x0 = (int) IvyXml.getAttrDouble(rgn,"X",0);
   Rectangle r = new Rectangle(x0,(int) IvyXml.getAttrDouble(rgn,"Y",0),
				  (int) IvyXml.getAttrDouble(rgn,"WIDTH",0),
				  (int) IvyXml.getAttrDouble(rgn,"HEIGHT",0));
   r.x = offset - r.width/2;
   if (r.x < 0) r.x = 0;
   if (r.x + r.width >= arearect.width) r.x = arearect.width - r.width;
   r.height = arearect.height;
   int dx = r.x - x0;

   BudaWorkingSetImpl ws = bba.defineWorkingSet(task_name,r);
   if (ws == null) return;

   ws.setColor(c);
   if (ctime > 0) ws.setCreateTime(ctime);

   Map<String,BudaBubble> bubblemap = new HashMap<String,BudaBubble>();
   Element bbls = IvyXml.getChild(te,"BUBBLES");
   for (Element bbl : IvyXml.children(bbls,"BUBBLE")) {
      BudaBubble bb = root.createBubble(bba,bbl,null,dx);
      if (bb != null) bubblemap.put(IvyXml.getAttrString(bbl,"ID"),bb);
    }

   Element grps = IvyXml.getChild(te,"GROUPS");
   for (Element egrp : IvyXml.children(grps,"GROUP")) {
      BudaBubbleGroup grp = null;
      for (Element ebbl : IvyXml.children(egrp,"BUBBLE")) {
	 String id = IvyXml.getAttrString(ebbl,"ID");
	 BudaBubble bbl = bubblemap.get(id);
	 if (bbl != null) {
	    grp = bbl.getGroup();
	    if (grp != null) break;
	  }
       }
      if (grp != null) {
	 Color lc = IvyXml.getAttrColor(egrp,"LEFTCOLOR");
	 Color rc = IvyXml.getAttrColor(egrp,"RIGHTCOLOR");
	 grp.setColor(lc,rc);
	 String ttl = IvyXml.getTextElement(egrp,"TITLE");
	 grp.setTitle(ttl);
       }
    }

   Element lnks = IvyXml.getChild(te,"LINKS");
   for (Element lnk : IvyXml.children(lnks,"LINK")) {
      boolean rect = IvyXml.getAttrBool(lnk,"RECT");
      Element flnk = IvyXml.getChild(lnk,"FROM");
      BudaBubble fbbl = bubblemap.get(IvyXml.getAttrString(flnk,"ID"));
      LinkPort fprt = root.createPort(fbbl,IvyXml.getChild(flnk,"PORT"));
      Element tlnk = IvyXml.getChild(lnk,"TO");
      BudaBubble tbbl = bubblemap.get(IvyXml.getAttrString(tlnk,"ID"));
      LinkPort tprt = root.createPort(tbbl,IvyXml.getChild(tlnk,"PORT"));
      if (fbbl != null && tbbl != null && fprt != null && tprt != null) {
	 BudaBubbleLink blnk = new BudaBubbleLink(fbbl,fprt,tbbl,tprt,rect);
	 root.addLink(blnk);
       }
    }
}



/********************************************************************************/
/*										*/
/*	String methods for menu display 					*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(task_name);

   return buf.toString();
}




}	// end of class BudaTask




/* end of BudaTask.java */
