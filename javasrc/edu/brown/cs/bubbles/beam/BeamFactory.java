/********************************************************************************/
/*										*/
/*		BeamFactory.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items bubble factory		*/
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


/* SVN: $Id$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextListener;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.*;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaPortPosition;
import edu.brown.cs.bubbles.buda.BudaConstants.LinkPort;
import edu.brown.cs.bubbles.bass.BassConstants.BassFlagger;
import edu.brown.cs.bubbles.bass.BassConstants.BassFlag;
import edu.brown.cs.bubbles.bass.*;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblemHandler;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.mint.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;


/**
 *	This class implements the factory for a host of miscellanous bubbles.
 **/

public class BeamFactory implements BeamConstants, BudaConstants.ButtonListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BeamFactory	the_factory = null;
private static BeamHelpPanel	help_panel = null;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Return the singleton instance of the miscellaneous bubble factory.
 **/

public static synchronized BeamFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BeamFactory();
    }
   return the_factory;
}



private BeamFactory()
{
   new SearchProblemFlags();
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Called automatically at startup to initialize the package.
 **/

public static void setup()
{
   BudaRoot.addBubbleConfigurator("BEAM",new BeamConfigurator());
   BudaRoot.registerMenuButton(NOTE_BUTTON,getFactory());
   BudaRoot.registerMenuButton(FLAG_BUTTON,getFactory(), imageToFlagIcon(FLAG_IMAGE));
   BudaRoot.registerMenuButton(FLAG_FIXED_BUTTON,getFactory(), imageToFlagIcon(FLAG_FIXED_IMAGE));
   BudaRoot.registerMenuButton(FLAG_WARNING_BUTTON,getFactory(), imageToFlagIcon(FLAG_WARNING_IMAGE));
   BudaRoot.registerMenuButton(FLAG_ACTION_BUTTON,getFactory(), imageToFlagIcon(FLAG_ACTION_IMAGE));
   BudaRoot.registerMenuButton(FLAG_BOMB_BUTTON,getFactory(), imageToFlagIcon(FLAG_BOMB_IMAGE));
   BudaRoot.registerMenuButton(FLAG_BUG_BUTTON,getFactory(), imageToFlagIcon(FLAG_BUG_IMAGE));
   BudaRoot.registerMenuButton(FLAG_CLOCK_BUTTON,getFactory(), imageToFlagIcon(FLAG_CLOCK_IMAGE));
   BudaRoot.registerMenuButton(FLAG_DATABASE_BUTTON,getFactory(), imageToFlagIcon(FLAG_DATABASE_IMAGE));
   BudaRoot.registerMenuButton(FLAG_FISH_BUTTON,getFactory(), imageToFlagIcon(FLAG_FISH_IMAGE));
   BudaRoot.registerMenuButton(FLAG_IDEA_BUTTON,getFactory(), imageToFlagIcon(FLAG_IDEA_IMAGE));
   BudaRoot.registerMenuButton(FLAG_INVESTIGATE_BUTTON,getFactory(), imageToFlagIcon(FLAG_INVESTIGATE_IMAGE));
   BudaRoot.registerMenuButton(FLAG_LINK_BUTTON,getFactory(), imageToFlagIcon(FLAG_LINK_IMAGE));
   BudaRoot.registerMenuButton(FLAG_STAR_BUTTON,getFactory(), imageToFlagIcon(FLAG_STAR_IMAGE));
   BudaRoot.registerMenuButton(PROBLEM_BUTTON,getFactory());
   BudaRoot.registerMenuButton(TASK_BUTTON,getFactory());
   BudaRoot.registerMenuButton(HELP_HOME_BUTTON,getFactory());
   BudaRoot.registerMenuButton(HELP_VIDEO_BUTTON,getFactory());
   BudaRoot.registerMenuButton(HELP_WIKI_BUTTON,getFactory());
   BudaRoot.registerMenuButton(HELP_TUTORIAL_BUTTON,getFactory());
   BudaRoot.registerMenuButton(HELP_KEY_BUTTON,getFactory());
}



/**
 *	Called automatically to initialize the package when Buda is set up
 **/

public static void initialize(BudaRoot br)
{
   BeamTracBugReport btr = new BeamTracBugReport(br);
   btr.addPanel();

   BeamFeedbackReport bfr = new BeamFeedbackReport(br);
   bfr.addPanel();

   help_panel = new BeamHelpPanel(br);
   help_panel.addPanel();

   BaleFactory.getFactory().addContextListener(new NoteHandler());

   switch (BoardSetup.getSetup().getRunMode()) {
      case SERVER :
	 BoardSetup bs = BoardSetup.getSetup();
	 MintControl mc = bs.getMintControl();
	 mc.register("<BEAM TYPE='NOTE' NAME='_VAR_0'><TEXT>_VAR_1</TEXT></BEAM>",
	       new NoteServer());
	 break;
      case CLIENT :
	 bs = BoardSetup.getSetup();
	 mc = bs.getMintControl();
	 mc.register("<BEAM TYPE='NOTE' NAME='_VAR_0'><TEXT>_VAR_1</TEXT></BEAM>",
	       new NoteClient());
	 new BeamProgressBubble(br);
	 break;
      case NORMAL :
	 new BeamProgressBubble(br);
	 break;
    }
}



private static Icon imageToFlagIcon(String path)
{
   Image img = BoardImage.getImage(path).getScaledInstance(17, 17, java.awt.Image.SCALE_SMOOTH);
   return new ImageIcon(img);
}



/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;

   if (id.equals(NOTE_BUTTON)) {
      bb = new BeamNoteBubble();
    }
   else if (id.equals(WEB_BUTTON)) {
      // bb = new BeamWebBubble();
    }
   else if (id.equals(FLAG_BUTTON)) {
      bb = new BeamFlagBubble("flags/default/Flag.png");
    }
   else if (id.equals(FLAG_FIXED_BUTTON)) {
      bb = new BeamFlagBubble("flags/default/Fixed.png");
    }
   else if (id.equals(FLAG_WARNING_BUTTON)) {
      bb = new BeamFlagBubble("flags/default/Warning.png");
    }
   else if (id.equals(FLAG_ACTION_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Action.png");
    }
   else if (id.equals(FLAG_BOMB_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Bomb.png");
    }
   else if (id.equals(FLAG_BUG_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Bug.png");
    }
   else if (id.equals(FLAG_CLOCK_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Clock.png");
    }
   else if (id.equals(FLAG_DATABASE_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Database.png");
    }
   else if (id.equals(FLAG_FISH_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Fish.png");
    }
   else if (id.equals(FLAG_IDEA_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Idea.png");
    }
   else if (id.equals(FLAG_INVESTIGATE_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Investigate.png");
    }
   else if (id.equals(FLAG_LINK_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Link.png");
    }
   else if (id.equals(FLAG_STAR_BUTTON)) {
      bb = new BeamFlagBubble("flags/additional/Star.png");
    }
   else if (id.equals(PROBLEM_BUTTON)) {
      bb = new BeamProblemBubble(null,false);
    }
   else if (id.equals(TASK_BUTTON)) {
      bb = new BeamProblemBubble(null,true);
    }
   else if (id.equals(HELP_VIDEO_BUTTON)) {
      help_panel.showHelpVideo();
    }
   else if (id.equals(HELP_HOME_BUTTON)) {
      help_panel.showHelpHome();
    }
   else if (id.equals(HELP_WIKI_BUTTON)) {
      help_panel.showHelpWiki();
    }
   else if (id.equals(HELP_TUTORIAL_BUTTON)) {
      help_panel.showHelpTutorial();
    }
   else if (id.equals(HELP_KEY_BUTTON)) {
      bb = new BeamKeyBubble();
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb,bc);
      bb.grabFocus();
    }
}




/********************************************************************************/
/*										*/
/*	<comment here>								*/
/*										*/
/********************************************************************************/

private static class NoteHandler implements BaleContextListener {

   NoteHandler() {
    }

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      if (cfg.getOffset() >= 0) {
	 menu.add(new NoteAction(cfg));
       }
    }

}	// end of inner class NoteHandler




private static class NoteAction extends AbstractAction {

   private BaleContextConfig context_config;

   private static final long serialVersionUID = 1;

   NoteAction(BaleContextConfig cfg) {
      super("Attach New Note");
      context_config = cfg;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      File fil = context_config.getEditor().getContentFile();
      BaleFactory bf = BaleFactory.getFactory();
      BaleFileOverview bfo = bf.getFileOverview(null, fil);
      int off = context_config.getDocumentOffset();
      int lno = context_config.getDocument().findLineNumber(context_config.getOffset());
      BeamNoteAnnotation ann = new BeamNoteAnnotation(fil,bfo,off);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(context_config.getEditor());
      Rectangle r = BudaRoot.findBudaLocation(context_config.getEditor());
      if (r == null || bba == null) return;
      int x = r.x + r.width + 50;
      int y = r.y;
      BeamNoteBubble nb = new BeamNoteBubble(null,null,ann);
      LinkPort p1 = bf.findPortForLine(context_config.getEditor(),lno);
      LinkPort p2 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
      BudaBubbleLink lnk = new BudaBubbleLink(context_config.getEditor(),p1,nb,p2);
      bba.addBubble(nb,x,y);
      bba.addLink(lnk);
    }

}	// end of inner class NoteAction




/********************************************************************************/
/*										*/
/*	Handler for note requests when running in client/server mode		*/
/*										*/
/********************************************************************************/

private static class NoteServer implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String name = args.getArgument(0);
      String cnts = args.getArgument(1);
      File dir = BoardSetup.getBubblesWorkingDirectory();
      File f1 = new File(dir,name);
      try {
	 FileWriter fw = new FileWriter(f1);
	 fw.write(cnts);
	 fw.close();
       }
      catch (IOException e) {
	 BoardLog.logE("BEAM","Problem writing note file",e);
       }
    }

}	// end of inner class NoteServer



private static class NoteClient implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String name = args.getArgument(0);
      String cnts = args.getArgument(1);
      BeamNoteBubble.updateNote(name,cnts);
    }

}	// end of inner class NoteClient



/********************************************************************************/
/*										*/
/*	Error annotations in search box 					*/
/*										*/
/********************************************************************************/

private static ProblemFlag error_flag = new ProblemFlag("error_overlay",20);
private static ProblemFlag error1_flag = new ProblemFlag("error_overlay1",20);
private static ProblemFlag warning_flag = new ProblemFlag("warning_overlay",10);
private static ProblemFlag warning1_flag = new ProblemFlag("warning_overlay1",10);

private static class SearchProblemFlags implements BassFlagger, BumpProblemHandler {

   private Map<String,ProblemFlag> flag_map;

   SearchProblemFlags() {
      flag_map = null;
      BumpClient.getBump().addProblemHandler(null,this);
      BassFactory.getFactory().addFlagChecker(this);
    }

   @Override synchronized  public BassFlag getFlagForName(String nm) {
      if (flag_map == null) computeFlagMap();
      ProblemFlag pf = flag_map.get(nm);

      return pf;
    }

   @Override public synchronized void handleProblemAdded(BumpProblem bp)     { flag_map = null; }
   @Override public synchronized void handleProblemRemoved(BumpProblem bp)   { flag_map = null; }
   @Override public synchronized void handleProblemsDone()		     { }

   private void computeFlagMap() {
      Map<String,ProblemFlag> mpf = new HashMap<String,ProblemFlag>();
      flag_map = mpf;
      for (BumpProblem bp : BumpClient.getBump().getAllProblems()) {
         addFlags(bp,mpf);
       }
    }

   private void addFlags(BumpProblem bp,Map<String,ProblemFlag> mpf) {
      ProblemFlag pf = null;
      ProblemFlag pf1 = null;
      switch (bp.getErrorType()) {
         case ERROR :
         case FATAL :
            pf = error_flag;
            pf1 = error1_flag;
            break;
         case WARNING :
            pf = warning_flag;
            pf1 = warning1_flag;
            break;
         default :
            return;
       }
      BassName bn = BassFactory.getFactory().findBubbleName(bp.getFile(),bp.getStart());
      if (bn == null) return;
      String nm = bn.getFullName();
      addFlag(mpf,nm,pf1);
      String pr = bn.getProject();
      String pnm = bn.getNameHead();
      if (pr == null || pnm == null) return;
      pnm = pr + ":." + pnm;
      while (pnm != null) {
        addFlag(mpf,pnm,pf);
        int idx1 = pnm.lastIndexOf(".");
        if (idx1 < 0) break;
        pnm = pnm.substring(0,idx1);
      }
   }

   private void addFlag(Map<String,ProblemFlag> mpf,String s,ProblemFlag pf) {
      if (s == null || pf == null) return;
      ProblemFlag opf = mpf.get(s);
      if (opf != null && opf.getPriority() >= pf.getPriority()) return;
      mpf.put(s,pf);
   }

}	// end of inner class SearchProblemFlags



private static class ProblemFlag implements BassFlag {

   private Icon overlay_icon;
   private int flag_priority;

   ProblemFlag(String icn,int pri) {
      overlay_icon = BoardImage.getIcon(icn);
      flag_priority = pri;
    }

   @Override public Icon getOverlayIcon()		{ return overlay_icon; }
   @Override public int getPriority()			{ return flag_priority; }

}	// end of inner class ProblemFlag



}	// end of class BeamFactory




/* end of BeamFactory.java */


