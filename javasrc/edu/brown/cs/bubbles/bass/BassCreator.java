/********************************************************************************/
/*										*/
/*		BassCreator.java						*/
/*										*/
/*	Bubble Augmented Search Strategies new object buttons and actions	*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.board.*;

import javax.swing.*;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;


class BassCreator implements BassConstants, BuenoConstants, BassConstants.BassPopupHandler {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubble	search_bubble;
private Point		access_point;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassCreator()
{
   search_bubble = null;
   access_point = null;
}




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,String fullname,BassName forname)
{
   search_bubble = bb;
   access_point = where;

   if (fullname.startsWith("@")) return;
   if (forname != null && !(forname instanceof BassNameLocation)) return;

   List<BuenoLocation> memblocs = new ArrayList<BuenoLocation>();
   BuenoLocation clsloc = null;

   // TODO: if forname == null and it represents an inner class, create alternatives for before/after the inner class

   if (forname == null) {
      String proj = null;
      int idx = fullname.indexOf(":");
      if (idx > 0) {
	 proj = fullname.substring(0,idx);
	 fullname = fullname.substring(idx+1);
       }
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(proj,fullname,null,true);
      if (loc.getClassName() != null) memblocs.add(loc);
      if (loc.getPackage() != null) clsloc = loc;
    }
   else {
      BuenoLocation loc = new BassNewLocation(forname,false,false);
      memblocs.add(new BassNewLocation(forname,false,true));
      memblocs.add(new BassNewLocation(forname,true,false));
      memblocs.add(loc);
      if (loc.getPackage() != null) clsloc = loc;
    }

   if (memblocs.size() > 0) {
      JMenu m1 = (JMenu) menu.add(new JMenu("New Method ..."));
      for (BuenoLocation bl : memblocs) {
	 m1.add(new NewMethodAction(bl));
       }
      m1 = (JMenu) menu.add(new JMenu("New Field ..."));
      for (BuenoLocation bl : memblocs) {
	 m1.add(new NewFieldAction(bl));
       }
      m1 = (JMenu) menu.add(new JMenu("New Inner Class/Interface/Enum ..."));
      for (BuenoLocation bl : memblocs) {
	 m1.add(new NewInnerTypeAction(bl));
       }
    }
   if (clsloc != null) {
      menu.add(new NewTypeAction(clsloc));
      if (clsloc.getPackage() != null) {
	 menu.add(new NewPackageAction(clsloc));
       }
    }
}




/********************************************************************************/
/*										*/
/*	Actions for creating a new method					*/
/*										*/
/********************************************************************************/

private abstract class NewAction extends AbstractAction {

   protected BuenoLocation for_location;
   protected BuenoProperties property_set;
   protected BuenoType create_type;

   NewAction(BuenoType typ,BuenoLocation loc) {
      super(loc.getTitle(typ));
      create_type = typ;
      for_location = loc;
      property_set = new BuenoProperties();
    }

}	// end of inner class NewAction



private class NewMethodAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewMethodAction(BuenoLocation loc) {
      super(BuenoType.NEW_METHOD,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewMethod");
      BuenoMethodDialog bmd = new BuenoMethodDialog(search_bubble,access_point,
						       property_set,for_location,this);
      bmd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewMethodAction




private class NewFieldAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewFieldAction(BuenoLocation loc) {
      super(BuenoType.NEW_FIELD,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewField");
      BuenoFieldDialog bfd = new BuenoFieldDialog(search_bubble,access_point,
						       property_set,for_location,this);
      bfd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      int idx = name.lastIndexOf(".");
      String cnm = name.substring(0,idx);
      BudaBubble bb = BaleFactory.getFactory().createFieldsBubble(proj,cnm);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewFieldAction




private class NewTypeAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewTypeAction(BuenoLocation loc) {
      super(BuenoType.NEW_TYPE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewType");
      BuenoClassDialog bcd = new BuenoClassDialog(search_bubble,access_point,create_type,
						     property_set,for_location,this);
      bcd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewTypeAction




private class NewInnerTypeAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewInnerTypeAction(BuenoLocation loc) {
      super(BuenoType.NEW_INNER_TYPE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewInnerType");
      BuenoInnerClassDialog bcd = new BuenoInnerClassDialog(search_bubble,access_point,create_type,
						     property_set,for_location,this);
      bcd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createClassBubble(proj,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewInnerTypeAction




private class NewPackageAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewPackageAction(BuenoLocation loc) {
      super(BuenoType.NEW_PACKAGE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewPackage");
      BuenoPackageDialog bpd = new BuenoPackageDialog(search_bubble,access_point,
							 property_set,for_location,this);
      bpd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewInnerTypeAction




/********************************************************************************/
/*										*/
/*	BuenoLocation based on a bass name					*/
/*										*/
/********************************************************************************/

private class BassNewLocation extends BuenoLocation {

   private BassName for_name;
   private boolean is_after;
   private boolean is_before;

   BassNewLocation(BassName nm,boolean after,boolean before) {
      for_name = nm;
      is_after = after;
      is_before = before;
    }

   @Override public String getProject() 		{ return for_name.getProject(); }
   @Override public String getPackage() 		{ return for_name.getPackageName(); }
   @Override public String getClassName() {
      String pkg = for_name.getPackageName();
      String cls = for_name.getClassName();
      if (cls == null) return null;
      if (pkg != null) cls = pkg + "." + cls;
      return cls;
    }

   @Override public String getInsertAfter() {
      if (is_after) return for_name.getFullName();
      return null;
    }

   @Override public String getInsertBefore() {
      if (is_before) return for_name.getFullName();
      return null;
    }

   @Override public String getInsertAtEnd()		{ return for_name.getClassName(); }

}	// end of inner class BassNewLocation




}	// end of class BassCreator




/* end of BassCreator.java */
