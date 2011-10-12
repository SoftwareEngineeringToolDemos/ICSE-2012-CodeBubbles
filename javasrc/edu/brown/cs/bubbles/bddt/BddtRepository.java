/********************************************************************************/
/*										*/
/*		BddtRepository.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool explorer repository		*/
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



package edu.brown.cs.bubbles.bddt;


import edu.brown.cs.bubbles.bass.*;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.*;

import java.awt.Point;
import java.awt.event.*;
import java.util.*;


class BddtRepository implements BddtConstants, BumpConstants, BassRepository,
	BassConstants.BassPopupHandler
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BumpClient			 bump_client;
private BumpRunModel			 run_model;
private Map<BumpLaunchConfig,ConfigName> config_map;
private Map<BumpProcess,ProcessName>	 process_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtRepository()
{
   bump_client = BumpClient.getBump();
   run_model = bump_client.getRunModel();

   config_map = new HashMap<BumpLaunchConfig,ConfigName>();
   for (BumpLaunchConfig blc : run_model.getLaunchConfigurations()) {
      if (!blc.isWorkingCopy()) {
	 ConfigName cn = new ConfigName(blc);
	 config_map.put(blc,cn);
       }
    }

   process_map = new HashMap<BumpProcess,ProcessName>();
   for (BumpProcess bp : run_model.getProcesses()) {
      ProcessName pn = new ProcessName(bp);
      process_map.put(bp,pn);
    }

   run_model.addRunEventHandler(new ModelHandler());

   BassFactory.getFactory().addPopupHandler(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   List<BassName> rslt = new ArrayList<BassName>(config_map.values());
   rslt.addAll(process_map.values());

   return rslt;
}


@Override public boolean includesRepository(BassRepository br)
{
   return br == this;
}




/********************************************************************************/
/*										*/
/*	Name representation for launch configureations				*/
/*										*/
/********************************************************************************/

private static class ConfigName extends BassNameBase {

   private BumpLaunchConfig for_config;

   ConfigName(BumpLaunchConfig blc) {
      for_config = blc;
      name_type = BassNameType.LAUNCH_CONFIGURATION;
    }

   @Override public BudaBubble createBubble() {
      return new BddtLaunchBubble(for_config);
    }

   @Override public BudaBubble createPreviewBubble()	{ return null; }

   @Override protected String getKey() {
      return "LAUNCH@" + for_config.getProject() + "@" + for_config.getConfigName();
    }

   @Override public String getProject() 		{ return null; }

   @Override protected String getSymbolName() {
      return BDDT_LAUNCH_CONFIG_PREFIX + for_config.getConfigName().replace(".","_");
    }

   @Override protected String getParameters()		{ return null; }

   BumpLaunchConfig getConfiguration()			{ return for_config; }

}	// end of inner class ConfigName




/********************************************************************************/
/*										*/
/*	Name representation for processes configureations			*/
/*										*/
/********************************************************************************/

private static class ProcessName extends BassNameBase {

   private BumpProcess for_process;

   ProcessName(BumpProcess bp) {
      for_process = bp;
      name_type = BassNameType.DEBUG_PROCESS;
    }

   @Override public BudaBubble createBubble()		{ return null; }

   @Override public BudaBubble createPreviewBubble()	{ return null; }

   @Override public String createPreviewString() {
      return "Process being debugged: " + getSymbolName();
    }

   @Override protected String getKey() {
      return "PROCESS@" + getProcessProject() + "@" + for_process.getId();
    }

   @Override public String getProject() 		{ return null; }

   @Override protected String getSymbolName() {
      if (for_process.getLaunch() == null) return "???";
      else if (for_process.getLaunch().getConfiguration() == null) return "???";

      return BDDT_PROCESS_PREFIX + for_process.getLaunch().getConfiguration().getConfigName() + "." + for_process.getId();
    }

   @Override protected String getParameters()		{ return null; }

   private String getProcessProject() {
      if (for_process.getLaunch() == null) return null;
      else if (for_process.getLaunch().getConfiguration() == null) return null;

      return for_process.getLaunch().getConfiguration().getProject();
    }

}	// end of inner class ProcessName




/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private class ModelHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt) {
      BumpLaunchConfig blc = evt.getLaunchConfiguration();

      switch (evt.getEventType()) {
	 case LAUNCH_ADD :
	    if (!blc.isWorkingCopy()) {
	       ConfigName cn = new ConfigName(blc);
	       config_map.put(blc,cn);
	       BassFactory.reloadRepository(BddtRepository.this);
	     }
	    break;
	 case LAUNCH_REMOVE :
	    if (config_map.remove(blc) != null) {
	       BassFactory.reloadRepository(BddtRepository.this);
	     }
	    break;
	 case LAUNCH_CHANGE :
	    if (!blc.isWorkingCopy() && !config_map.containsKey(blc)) {
	       ConfigName cn = new ConfigName(blc);
	       config_map.put(blc,cn);
	       BassFactory.reloadRepository(BddtRepository.this);
	    }
	    break;
       }
    }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      BumpProcess blp = evt.getProcess();
      switch (evt.getEventType()) {
	 case PROCESS_ADD :
	    ProcessName pn = new ProcessName(blp);
	    process_map.put(blp,pn);
	    BassFactory.reloadRepository(BddtRepository.this);
	    break;
	 case PROCESS_REMOVE :
	    process_map.remove(blp);
	    BassFactory.reloadRepository(BddtRepository.this);
	    break;
	 case PROCESS_PERFORMANCE :
	 case PROCESS_CHANGE :
	    break;
       }
    }

   @Override public void handleThreadEvent(BumpRunEvent evt)	{ }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,String msg)	{ }

}	// end of inner class ModelHandler




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void addButtons(BudaBubble bb,Point where,JPopupMenu m,
				    String fullname,BassName forname)
{
   if (forname instanceof ConfigName) {
      ConfigName cn = (ConfigName) forname;
      BumpLaunchConfig cfg = cn.getConfiguration();
      if (cfg != null) {
	 m.add(new CloneLaunchAction(cfg));
	 m.add(new DebugLaunchAction(cfg));
	 m.add(new RunLaunchAction(cfg));
	 m.add(new DeleteLaunchAction(cfg));
       }
    }
   else if (forname == null && fullname.contains("@Launch Configurations")) {
      BddtFactory.getFactory().addNewConfigurationActions(m);
    }
}



private static class CloneLaunchAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   CloneLaunchAction(BumpLaunchConfig blc) {
      super("Create copy of " + blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpLaunchConfig nblc = for_config.clone(null);
      if (nblc != null) nblc.save();
    }

}	// end of inner class CloneLaunchAction



private static class DeleteLaunchAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   DeleteLaunchAction(BumpLaunchConfig blc) {
      super("Delete " + blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      for_config.delete();
    }

}	// end of inner class DeleteLaunchAction



private class RunLaunchAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   RunLaunchAction(BumpLaunchConfig blc) {
      super("Run " + blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      bump_client.startRun(for_config);
    }

}	// end of inner class RunLaunchAction


private class DebugLaunchAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   DebugLaunchAction(BumpLaunchConfig blc) {
      super("Debug " + blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      bump_client.startDebug(for_config,null);
    }

}	// end of inner class RunLaunchAction





}	// end of class BddtRepository




/* end of BddtRepository.java */
