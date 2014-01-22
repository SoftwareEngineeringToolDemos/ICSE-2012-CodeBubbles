/********************************************************************************/
/*										*/
/*		BassCreator.java						*/
/*										*/
/*	Bubble Augmented Search Strategies new object buttons and actions	*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.*;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
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
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case REBUS :
	 addJavaButtons(bb,where,menu,fullname,forname);
	 break;
      case PYTHON :
	 addPythonButtons(bb,where,menu,fullname,forname);
	 break;
      default :
	 break;
    }
}



private void addJavaButtons(BudaBubble bb,Point where,JPopupMenu menu,String fullname,BassName forname)
{
   search_bubble = bb;
   access_point = where;

   if (fullname.startsWith("@")) return;
   if (forname != null && !(forname instanceof BassNameLocation)) return;

   List<BuenoLocation> memblocs = new ArrayList<BuenoLocation>();
   BuenoLocation clsloc = null;
   Action delact = null;

   // TODO: if forname == null and it represents an inner class, create alternatives for before/after the inner class

   if (forname != null && forname.getNameType() == BassNameType.PROJECT) {
      forname = null;
      BuenoLocation dfltloc = BuenoFactory.getFactory().createLocation(fullname,null,null,true);
      menu.add(new NewPackageAction(dfltloc));
      delact = new DeleteProjectAction(fullname,bb);
    }
   else if (forname == null) {
      String proj = null;
      int idx = fullname.indexOf(":");
      if (idx > 0) {
	 proj = fullname.substring(0,idx);
	 fullname = fullname.substring(idx+1);
       }
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(proj,fullname,null,true);
      if (loc.getClassName() != null) {
	 memblocs.add(loc);
	 String cnm = loc.getClassName();
	 String pnm = loc.getPackage();
	 String outer;
	 if (pnm == null) outer = "";
	 else outer = cnm.substring(pnm.length() + 1);
	 if (outer.contains(".") || outer.contains("$")) {
	    int xidx = outer.indexOf(".");
	    String inner = outer.replace(".", "$");
	    outer = pnm + "." + outer.substring(0,xidx);
	    memblocs.add(BuenoFactory.getFactory().createLocation(proj,outer,inner,false));
	    memblocs.add(BuenoFactory.getFactory().createLocation(proj,outer,inner,true));
	  }
	 if (bass_properties.getBoolean("Bass.delete.class")) {
	    if (pnm != null) {
	       cnm = cnm.substring(pnm.length()+1);
	       if (!cnm.contains(".")) {
		  delact = new DeleteClassAction(proj,loc.getClassName(),bb);
		}
	     }
	    else {
	       if (cnm != null) {
		  delact = new DeleteClassAction(proj,cnm,bb);
		}
	     }
	  }
	 if (delact == null && bass_properties.getBoolean("Bass.delete.file")) {
	    File f = loc.getFile();
	    if (f != null) {
	       delact = new DeleteFileAction(proj,f,bb);
	     }
	  }
       }
      else if (!fullname.contains("@")) {
	 if (bass_properties.getBoolean("Bass.delete.package"))
	    delact = new DeletePackageAction(proj,fullname,bb);
       }
      if (loc.getPackage() != null || loc.getClassName() != null) clsloc = loc;
    }
   else if (forname.getNameType() != BassNameType.PROJECT) {
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
   if (delact != null) {
      menu.add(delact);
    }
}




private void addPythonButtons(BudaBubble bb,Point where,JPopupMenu menu,String fullname,BassName forname)
{
   search_bubble = bb;
   access_point = where;

   if (fullname.startsWith("@")) return;
   if (forname != null && !(forname instanceof BassNameLocation)) return;

   List<BuenoLocation> memblocs = new ArrayList<BuenoLocation>();
   BuenoLocation clsloc = null;

   if (forname != null && forname.getNameType() == BassNameType.PROJECT) {
      forname = null;
      BuenoLocation dfltloc = BuenoFactory.getFactory().createLocation(fullname,null,null,true);
      menu.add(new NewPackageAction(dfltloc));
    }
   else if (forname == null) {
      String proj = null;
      int idx = fullname.indexOf(":");
      if (idx > 0) {
	 proj = fullname.substring(0,idx);
	 fullname = fullname.substring(idx+1);
       }
      BuenoLocation loc = BuenoFactory.getFactory().createLocation(proj,fullname,null,true);	// this needs to be python-specialized
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
      JMenu m1 = (JMenu) menu.add(new JMenu("New Function ..."));
      for (BuenoLocation bl : memblocs) {
	 m1.add(new NewMethodAction(bl));
       }
      m1 = (JMenu) menu.add(new JMenu("New Attribute ..."));
      for (BuenoLocation bl : memblocs) {
	 m1.add(new NewFieldAction(bl));
       }
      m1 = (JMenu) menu.add(new JMenu("New Class ..."));
      for (BuenoLocation bl : memblocs) {
	 m1.add(new NewInnerTypeAction(bl));
       }
    }
   if (clsloc != null) {
      menu.add(new NewModuleAction(clsloc));
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
      BudaRoot.hideSearchBubble(e);
      BuenoFactory.getFactory().createMethodDialog(search_bubble,access_point,property_set,
						      for_location,null,this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb != null) {
	 bba.add(bb,new BudaConstraint(p));
	 File f1 = bb.getContentFile();
	 if (f1 != null) BumpClient.getBump().saveFile(proj,f1);
	 BudaRoot br = BudaRoot.findBudaRoot(bb);
	 if (br != null) br.handleSaveAllRequest();
       }
   }

}	// end of inner class NewMethodAction




private class NewFieldAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewFieldAction(BuenoLocation loc) {
      super(BuenoType.NEW_FIELD,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewField");
      BudaRoot.hideSearchBubble(e);
      BuenoFieldDialog bfd = new BuenoFieldDialog(search_bubble,access_point,
						       property_set,for_location,this);
      bfd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      int idx = name.lastIndexOf(".");
      String cnm = name.substring(0,idx);
      BudaBubble bb = BaleFactory.getFactory().createFieldsBubble(proj,for_location.getFile(),cnm);
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
      BudaRoot.hideSearchBubble(e);
      BuenoClassDialog bcd = new BuenoClassDialog(search_bubble,access_point,create_type,
						     property_set,for_location,this);
      bcd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,null,name);
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
      BudaRoot.hideSearchBubble(e);
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
      BudaRoot.hideSearchBubble(e);
      BuenoPackageDialog bpd = new BuenoPackageDialog(search_bubble,access_point,
							 property_set,for_location,this);
      bpd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,null,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewPackageAction




private class NewModuleAction extends NewAction implements BuenoConstants.BuenoBubbleCreator {

   private final static long serialVersionUID = 1;

   NewModuleAction(BuenoLocation loc) {
      super(BuenoType.NEW_MODULE,loc);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","NewModule");
      BudaRoot.hideSearchBubble(e);
      BuenoPythonModuleDialog bpd = new BuenoPythonModuleDialog(search_bubble,access_point,
						 property_set,for_location,this);
      bpd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createFileBubble(proj,null,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
   }

}	// end of inner class NewModuleAction






/********************************************************************************/
/*										*/
/*	BuenoLocation based on a bass name					*/
/*										*/
/********************************************************************************/

private static class BassNewLocation extends BuenoLocation {

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



/********************************************************************************/
/*										*/
/*	Delete actions								*/
/*										*/
/********************************************************************************/

private static class DeleteProjectAction extends AbstractAction implements Runnable {

   private String project_name;
   private BudaBubble rel_bubble;

   private static final long serialVersionUID = 1;

   DeleteProjectAction(String proj,BudaBubble bb) {
      super("Delete Project " + proj);
      project_name = proj;
      rel_bubble = bb;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (bass_properties.getBoolean("Bass.delete.confirm",true)) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(rel_bubble);
	 int sts = JOptionPane.showConfirmDialog(bba,"Do you really want to delete project " + project_name,
						    "Confirm Delete Project",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }
      BoardThreadPool.start(this);
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"PROJECT",project_name,bass_properties.getBoolean("Bass.delete.rebuild",true));
    }
}	// end of inner class DeleteProjectAction





private static class DeletePackageAction extends AbstractAction implements Runnable {

   private String project_name;
   private String package_name;
   private BudaBubble rel_bubble;

   private static final long serialVersionUID = 1;

   DeletePackageAction(String proj,String pkg,BudaBubble bb) {
      super("Delete Package " + pkg);
      project_name = proj;
      package_name = pkg;
      rel_bubble = bb;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (bass_properties.getBoolean("Bass.delete.confirm",true)) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(rel_bubble);
	 int sts = JOptionPane.showConfirmDialog(bba,"Do you really want to delete all of package " + package_name,
						    "Confirm Delete Package",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"PACKAGE",package_name,bass_properties.getBoolean("Bass.delete.rebuild",true));
    }
   
}	// end of inner class DeletePackageAction




private static class DeleteFileAction extends AbstractAction implements Runnable {

   private String project_name;
   private File file_name;
   private BudaBubble rel_bubble;

   private static final long serialVersionUID = 1;

   DeleteFileAction(String proj,File fil,BudaBubble bb) {
      super("Delete File " + fil.getName());
      project_name = proj;
      file_name = fil;
      rel_bubble = bb;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (bass_properties.getBoolean("Bass.delete.confirm",true)) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(rel_bubble);
	 int sts = JOptionPane.showConfirmDialog(bba,"Do you really want to delete file " + file_name.getPath(),
						    "Confirm Delete File",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
       }
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"FILE",file_name.getAbsolutePath(),bass_properties.getBoolean("Bass.delete.rebuild",true));
    }
}	// end of inner class DeletePackageAction




private static class DeleteClassAction extends AbstractAction implements Runnable {

   private String project_name;
   private String class_name;
   private BudaBubble rel_bubble;

   private static final long serialVersionUID = 1;

   DeleteClassAction(String proj,String cls,BudaBubble bb) {
      super("Delete Class " + cls);
      project_name = proj;
      class_name = cls;
      rel_bubble = bb;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (bass_properties.getBoolean("Bass.delete.confirm",true)) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(rel_bubble);
	 int sts = JOptionPane.showConfirmDialog(bba,"Do you really want to delete the class " + class_name,
						    "Confirm Delete Class",
						    JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
	 if (sts != JOptionPane.YES_OPTION) return;
      }
      BoardThreadPool.start(this);
   }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(project_name,"CLASS",class_name,bass_properties.getBoolean("Bass.delete.rebuild",true));
    }

}	// end of inner class DeleteClassAction




}	// end of class BassCreator




/* end of BassCreator.java */
