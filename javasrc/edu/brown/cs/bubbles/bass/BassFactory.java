/********************************************************************************/
/*										*/
/*		BassFactory.java						*/
/*										*/
/*	Bubble Augmented Search Strategies factory for search boxes		*/
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.bowi.BowiConstants.BowiTaskType;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bueno.*;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;



/**
 *	This class provides the factory methods for creating search bubbles
 *	and their associated repositories.
 **/

public class BassFactory implements BudaRoot.SearchBoxCreator, BassConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<BassPopupHandler> popup_handlers;


private static BassRepositoryLocation	bass_repository;
private static BassFactory		the_factory;
private static Map<BudaBubbleArea,BassBubble> package_explorers;
private static Map<SearchType,Set<BassRepository>> use_repositories;
private static Map<BassRepository,BassTreeModelBase> repository_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Return the singleton instance of the Bass search bubble factory.
 **/

public static BassFactory getFactory()
{
   return the_factory;
}



private BassFactory()
{
   popup_handlers = new SwingEventListenerList<BassPopupHandler>(BassPopupHandler.class);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Called by initialization to ensure that the search package is set up correctly.
 **/

public static void setup()
{
   if (use_repositories != null) return;

   use_repositories = new HashMap<SearchType,Set<BassRepository>>();
   use_repositories.put(SearchType.SEARCH_ALL,new HashSet<BassRepository>());

   the_factory = new BassFactory();
   BudaRoot.registerSearcher(the_factory);
   BudaRoot.addBubbleConfigurator("BASS",new BassConfigurator());
   package_explorers = new HashMap<BudaBubbleArea,BassBubble>();

   BudaRoot.registerMenuButton("Package Explorer",new PackageExplorerButton());
   BudaRoot.registerMenuButton("Text Search",new TextSearchButton());

   repository_map = new HashMap<BassRepository,BassTreeModelBase>();

   the_factory.addPopupHandler(new BassCreator());
   the_factory.addPopupHandler(new ProjectProps());

   bass_repository = new BassRepositoryLocation();
   registerRepository(SearchType.SEARCH_CODE,bass_repository);
   registerRepository(SearchType.SEARCH_EXPLORER,bass_repository);
}



/**
 *	Called to initialize once BudaRoot is setup
 **/


public static void initialize(BudaRoot br)
{
   BoardLog.logD("BASS","Initialize");

   if (bass_properties.getBoolean(BASS_PACK_ON_START_NAME) &&
	  !BoardSetup.getConfigurationFile().exists()) {
      BudaBubbleArea bba = br.getCurrentBubbleArea();
      BassBubble peb = getFactory().createPackageExplorer(bba);
      package_explorers.put(bba,peb);
      Rectangle r = br.getViewport();
      Dimension d = peb.getPreferredSize();
      d.height = r.height;
      peb.setSize(d);
      BudaConstraint bc = new BudaConstraint(BudaBubblePosition.DOCKED,
						r.x + r.width - d.width,
						r.y);
      bba.add(peb,bc);
    }
}



/**
 *	Register a BassRepository for a particular type of search.
 **/

public static void registerRepository(SearchType st,BassRepository br)
{
   Set<BassRepository> sbr = use_repositories.get(st);
   if (sbr == null) {
      sbr = new HashSet<BassRepository>();
      use_repositories.put(st,sbr);
    }
   sbr.add(br);
   use_repositories.get(SearchType.SEARCH_ALL).add(br);
}





/**
 *	Wait for names to be loaded
 **/

public static void waitForNames()
{


   bass_repository.waitForNames();
}




/********************************************************************************/
/*										*/
/*	Popup handling setup							*/
/*										*/
/********************************************************************************/

/**
 *	Add a new handler for popup menu options in the search box
 **/

public void addPopupHandler(BassPopupHandler ph)
{
   popup_handlers.add(ph);
}


/**
 *	Remove a handler for popup menu options in the search box
 **/

public void removePopupHandler(BassPopupHandler ph)
{
   popup_handlers.remove(ph);
}



void addButtons(Component c,Point where,JPopupMenu m,String fnm,BassName bn)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);

   for (BassPopupHandler ph : popup_handlers) {
      ph.addButtons(bb,where,m,fnm,bn);
    }
}




/**
 *	Add a bubble relative to the search box of the given window
 **/

public void addNewBubble(BudaBubble searchbox,Point loc,BudaBubble bbl)
{
   if (bbl == null || searchbox == null) return;

   Component c = searchbox.getContentPane();
   if (!(c instanceof BassSearchBox)) return;
   BassSearchBox sbox = (BassSearchBox) c;

   int ypos = 0;
   if (loc != null) ypos = loc.y;
   else {
      Rectangle r = BudaRoot.findBudaLocation(searchbox);
      ypos = r.y;
    }

   sbox.addAndLocateBubble(bbl,ypos);
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

/**
 *	Create as search bubble of a given type for the given project with the
 *	given initial search string
 **/

public BassBubble createSearch(SearchType type,String proj,String pfx)
{
   BassRepository br = getRepository(type);

   if (br == null) return null;

   return new BassBubble(br,proj,pfx,true);
}

/**
 *	returns the package explorer if it exists, othewise null
 */

public BudaBubble getPackageExplorer(BudaBubbleArea bba)
{
   return package_explorers.get(bba);
}


/**
 *	Create a buttle that can be used as the package explorer.
 **/

public BassBubble createPackageExplorer(BudaBubbleArea bba)
{
   BassRepository brm = getRepository(SearchType.SEARCH_EXPLORER);
   BassBubble peb = new BassBubble(brm,null,null,false);
   package_explorers.put(bba,peb);
   return peb;
}



/**
 *	Create a text search bubble.
 **/

public BassTextBubble createTextSearch()
{
   return new BassTextBubble();
}



public static BassRepository getRepository(BudaConstants.SearchType typ)
{
   Set<BassRepository> sbr = use_repositories.get(typ);

   if (sbr == null) return null;

   BassRepository rslt = null;
   for (BassRepository br : sbr) {
      if (rslt == null) rslt = br;
      else rslt = new BassRepositoryMerge(br,rslt);
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Methods to handle query 						*/
/*										*/
/********************************************************************************/

/**
 *	This returns the name of the bubble associated with the given file at the
 *	given position.  Note that this position is given in terms of the IDE, not
 *	in terms of how Java sees it.  This is used, for example, by error handling
 *	to create a bubble for the code corresponding to an error message.  When there
 *	are multiple symbols spanning the given location (e.g. a class and a method),
 *	this will return the innermost one.
 **/

public BassName findBubbleName(File f,int eclipsepos)
{
   return bass_repository.findBubbleName(f,eclipsepos);
}



/********************************************************************************/
/*										*/
/*	Methods to handle package explorer creation				*/
/*										*/
/********************************************************************************/

private static class PackageExplorerButton implements BudaConstants.ButtonListener
{

   PackageExplorerButton()			{ }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BassBubble peb = package_explorers.get(bba);
      if (peb != null && peb.isVisible()) {		// isShowing() ?
	 peb.setVisible(false);
	 return;
       }

      peb = the_factory.createPackageExplorer(bba);
      package_explorers.put(bba,peb);

      BudaRoot br = BudaRoot.findBudaRoot(bba);
      Rectangle r = br.getViewport();
      Dimension d = peb.getPreferredSize();
      d.height = r.height;
      peb.setSize(d);

      BudaConstraint bc = new BudaConstraint(BudaBubblePosition.DOCKED,
						r.x + r.width - d.width,
						r.y);
      bba.add(peb,bc);
    }

}	// end of inner class PackageExplorerButton




/********************************************************************************/
/*										*/
/*	Methods to handle text search request					*/
/*										*/
/********************************************************************************/

private static class TextSearchButton implements BudaConstants.ButtonListener
{

   TextSearchButton()			{ }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BowiFactory.startTask(BowiTaskType.TEXT_SEARCH);
      BudaRoot br = BudaRoot.findBudaRoot(bba);
      BudaBubble bb = the_factory.createTextSearch();
      BudaConstraint bc = new BudaConstraint(BudaBubblePosition.STATIC,pt);
      br.add(bb,bc);
      bb.grabFocus();
      BowiFactory.stopTask(BowiTaskType.TEXT_SEARCH);
    }

}	// end of inner class TextSearchButton



/********************************************************************************/
/*										*/
/*	Methods to handle repository mapping					*/
/*										*/
/********************************************************************************/

public static void reloadRepository(BassRepository br)
{
   synchronized (repository_map) {
      for (BassRepository abr : repository_map.keySet()) {
	 if (abr.includesRepository(br)) {
	    BassTreeModelBase tmb = getModelBase(abr);
	    tmb.requestRebuild();
	    // tmb.rebuild();
	  }
       }
    }
}




static BassTreeModelBase getModelBase(BassRepository br)
{
   synchronized (repository_map) {
      BassTreeModelBase tmb = repository_map.get(br);
      if (tmb == null) {
	 tmb = new BassTreeModelBase(br);
	 repository_map.put(br,tmb);
       }
      return tmb;
    }
}



/********************************************************************************/
/*										*/
/*	Class to handle project property dialog 				*/
/*										*/
/********************************************************************************/

private static class ProjectProps implements BassPopupHandler {

   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,
        			       String fullname,BassName bn) {
      if (bn != null) return;
      if (fullname.startsWith("@")) return;
   
      int idx = fullname.indexOf(":");
      if (idx <= 0) return;
      String proj = fullname.substring(0,idx);
      fullname = fullname.substring(idx+1);
   
      menu.add(new EclipseProjectAction(proj));
      if (System.getProperty("user.name").equals("spr"))
         menu.add(new ProjectAction(proj,bb,where));
      menu.add(new NewProjectAction());
      menu.add(new BassImportProjectAction());
    }

}	// end of inner class ProjectProps



private static class EclipseProjectAction extends AbstractAction {

   private String for_project;

   private static final long serialVersionUID = 1;

   EclipseProjectAction(String proj) {
      super("Eclipse Project Properties for " + proj);
      for_project = proj;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient bc = BumpClient.getBump();
      bc.saveAll();
      bc.editProject(for_project);
    }

}	// end of inner class EclipseProjectAction



private static class ProjectAction extends AbstractAction {

   private String for_project;
   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   ProjectAction(String proj,BudaBubble rel,Point pt) {
      super("TESTING PROPERTIES OF " + proj);
      // super("Edit Properties of Project " + proj);
      for_project = proj;
      rel_bubble = rel;
      rel_point = pt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BuenoProjectDialog dlg = new BuenoProjectDialog(for_project);
      BudaBubble bb = dlg.createProjectEditor();
      if (bb == null) return;
      BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bb);
    }

}	// end of inner class ProjectAction



private static class NewProjectAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NewProjectAction() {
      super("Create New Project");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient bc = BumpClient.getBump();
      bc.createProject();
    }

}	// end of inner class ProjectAction

}	// end of class BassFactory




/* end of BassFactory.java */
