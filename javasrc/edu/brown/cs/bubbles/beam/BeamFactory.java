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
import edu.brown.cs.ivy.mint.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URI;


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



private BeamFactory()				{ }




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
}



/**
 *	Called automatically to initialize the package when Buda is set up
 **/

public static void initialize(BudaRoot br)
{
   // BeamBugReport bbr = new BeamBugReport(br);
   // bbr.addPanel();

   BeamTracBugReport btr = new BeamTracBugReport(br);
   btr.addPanel();

   BeamFeedbackReport bfr = new BeamFeedbackReport(br);
   bfr.addPanel();

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
      try {
	 URI u = new URI(HELP_VIDEO_URL);
	 Desktop.getDesktop().browse(u);
       }
      catch (Throwable t) {
	 BoardLog.logE("BEAM","Problem showing help video",t);
       }
    }
   else if (id.equals(HELP_HOME_BUTTON)) {
      try {
	 URI u = new URI(HELP_HOME_URL);
	 Desktop.getDesktop().browse(u);
       }
      catch (Throwable t) {
	 BoardLog.logE("BEAM","Problem showing home page",t);
       }
    }
   else if (id.equals(HELP_WIKI_BUTTON)) {
      try {
	 URI u = new URI(HELP_WIKI_URL);
	 Desktop.getDesktop().browse(u);
       }
      catch (Throwable t) {
	 BoardLog.logE("BEAM","Problem showing wiki page",t);
       }
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
      File dir = BoardSetup.getBubblesPluginDirectory();
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
}	// end of class BeamFactory




/* end of BeamFactory.java */


