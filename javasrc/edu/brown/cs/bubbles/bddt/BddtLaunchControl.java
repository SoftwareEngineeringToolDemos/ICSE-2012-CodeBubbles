/********************************************************************************/
/*										*/
/*		BddtLaunchControl.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool process/launch controller	*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



class BddtLaunchControl extends BudaBubble implements BddtConstants, BumpConstants, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private BumpClient		bump_client;
private BumpLaunchConfig	launch_config;
private BumpProcess		cur_process;
private JLabel			state_label;
private LaunchState		launch_state;
private RunEventHandler 	event_handler;
private EditorContextListener	editor_handler;
private Map<BumpThread,ExecutionAnnot> exec_annots;
private BddtBubbleManager	bubble_manager;
private int			freeze_count;

private JPanel			launch_panel;



/********************************************************************************/
/*										*/
/*	Constructors/destructors						*/
/*										*/
/********************************************************************************/

BddtLaunchControl(BumpLaunchConfig blc)
{
   bump_client = BumpClient.getBump();
   launch_config = blc;
   cur_process = null;
   launch_state = LaunchState.READY;
   exec_annots = new ConcurrentHashMap<BumpThread,ExecutionAnnot>();
   freeze_count = 0;

   setupPanel();

   setContentPane(launch_panel);

   event_handler = new RunEventHandler();
   bump_client.getRunModel().addRunEventHandler(event_handler);
   editor_handler = new EditorContextListener();
   BaleFactory.getFactory().addContextListener(editor_handler);
   bubble_manager = new BddtBubbleManager(this);
}



@Override protected void localDispose()
{
   bump_client.getRunModel().removeRunEventHandler(event_handler);
   BaleFactory.getFactory().removeContextListener(editor_handler);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpProcess getProcess()			{ return cur_process; }
String getProject()				{ return launch_config.getProject(); }
BddtBubbleManager getBubbleManager()		{ return bubble_manager; }



/********************************************************************************/
/*										*/
/*	Methods to set up the display panel					*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   int y = 0;

   String nm = launch_config.getProject() + " : " + launch_config.getConfigName();

   JLabel ttl = new JLabel(nm);
   pnl.addGBComponent(ttl,0,y++,0,1,10,0);
   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   BoardProperties bp = BoardProperties.getProperties("Bddt");

   JToolBar btnbar = new JToolBar();
   btnbar.add(new PlayAction());
   if (bp.getBoolean("Bddt.buttons.StepInto",true)) btnbar.add(new StepIntoAction());
   if (bp.getBoolean("Bddt.buttons.StepUser",true)) btnbar.add(new StepUserAction());
   btnbar.add(new StepOverAction());
   btnbar.add(new StepReturnAction());
   btnbar.add(new PauseAction());
   if (bp.getBoolean("Bddt.buttons.DropToFrame",true)) btnbar.add(new DropToFrameAction());
   btnbar.setFloatable(false);
   btnbar.setMargin(new Insets(2,2,2,2));
   pnl.addGBComponent(btnbar,0,y++,0,1,1,0);

   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   state_label = new JLabel(launch_state.toString());
   pnl.addGBComponent(state_label,0,y++,0,1,1,0);

   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   JToolBar bblbar = new JToolBar();
   bblbar.add(new ConsoleAction());
   bblbar.add(new ThreadsAction());
   if (bp.getBoolean("Bddt.bubbons.History",true)) bblbar.add(new HistoryAction());
   if (bp.getBoolean("Bddt.buttons.Performance",true)) bblbar.add(new PerformanceAction());
   bblbar.add(new NewChannelAction());
   bblbar.addSeparator();
   bblbar.add(new StopAction());
   bblbar.setFloatable(false);
   bblbar.setMargin(new Insets(2,2,2,2));
   pnl.addGBComponent(bblbar,0,y++,0,1,1,0);

   launch_panel = pnl;
}




/********************************************************************************/
/*										*/
/*	Keystroke handling							*/
/*										*/
/********************************************************************************/

void setupKeys()
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);

   registerKey(bba,new StepUserAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F5,0));
   registerKey(bba,new StepOverAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
   registerKey(bba,new StepReturnAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F7,0));
   registerKey(bba,new PlayAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F8,0));
   registerKey(bba,new PauseAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F8,InputEvent.SHIFT_DOWN_MASK));
}


private void registerKey(BudaBubbleArea bba,Action act,KeyStroke k)
{
   String cmd = (String) act.getValue(Action.NAME);
   bba.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(k,cmd);
   bba.getActionMap().put(cmd,act);
}



/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*										*/
/*	State maintenance							*/
/*										*/
/********************************************************************************/

void setLaunchState(LaunchState ls)
{
   setLaunchState(ls,0,0);
}




synchronized void setLaunchState(LaunchState ls,int rct,int tct)
{
   launch_state = ls;

   if (state_label != null) {
      String lbl = ls.toString();
      switch (launch_state) {
	 default :
	    break;
	 case STARTING :
	    lbl = "SAVE, COMPILE, & START";
	    break;
	 case PARTIAL_PAUSE :
	    lbl += " (" + Integer.toString(tct-rct) + "/" + Integer.toString(tct) + ")";
	    break;
       }
      state_label.setText(lbl);
    }
}




/********************************************************************************/
/*										*/
/*	Debugging button actions						*/
/*										*/
/********************************************************************************/

private class PlayAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PlayAction() {
      super("Play",BoardImage.getIcon("debug/play"));
      putValue(SHORT_DESCRIPTION,"Start or continue execution");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    BoardMetrics.noteCommand("BDDT","StartDebug");
	    cur_process = null;
	    setLaunchState(LaunchState.STARTING);
	    bubble_manager.restart();
	    BoardThreadPool.start(new StartDebug());
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PAUSED :
	 case PARTIAL_PAUSE :
	    if (cur_process != null) {
	       BoardMetrics.noteCommand("BDDT","ResumeDebug");
	       waitForFreeze();
	       bump_client.resume(cur_process);
	     }
	    break;
       }
    }

}	// end of inner class PlayAction




private class PauseAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PauseAction() {
      super("Pause",BoardImage.getIcon("debug/pause"));
      putValue(SHORT_DESCRIPTION,"Pause execution");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	 case PAUSED :
	    break;
	 case STARTING :
	 case RUNNING :
	 case PARTIAL_PAUSE :
	    if (cur_process != null) {
	       BoardMetrics.noteCommand("BDDT","SuspendDebug");
	       bump_client.suspend(cur_process);
	     }
	    break;
       }
    }

}	// end of inner class PauseAction




private class StopAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StopAction() {
      super("Stop",BoardImage.getIcon("debug/stop"));
      putValue(SHORT_DESCRIPTION,"Terminate execution");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    break;
	 case STARTING :
	 case RUNNING :
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    if (cur_process != null) {
	       BoardMetrics.noteCommand("BDDT","TerminateDebug");
	       waitForFreeze();
	       bump_client.terminate(cur_process);
	     }
	    break;
       }
    }

}	// end of inner class StopAction



private class StepIntoAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepIntoAction() {
      super("Step Into",BoardImage.getIcon("debug/stepin"));
      putValue(SHORT_DESCRIPTION,"Step into");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepIntoDebug");
	       waitForFreeze();
	       bump_client.stepInto(bt);
	     }
	    break;
       }
    }

}	// end of inner class StepIntoAction


private class StepUserAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepUserAction() {
      super("Step User",BoardImage.getIcon("debug/stepuser"));
      putValue(SHORT_DESCRIPTION,"Step into user code");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepUserDebug");
	       waitForFreeze();
	       bump_client.stepUser(bt);
	     }
	    break;
      }
   }

}	// end of inner class StepUserAction



private class StepOverAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepOverAction() {
      super("Step Over",BoardImage.getIcon("debug/stepover"));
      putValue(SHORT_DESCRIPTION,"Step over");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepOverDebug");
	       waitForFreeze();
	       bump_client.stepOver(bt);
	     }
	    break;
       }
    }

}	// end of inner class StepOverAction




private class StepReturnAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepReturnAction() {
      super("Step Return",BoardImage.getIcon("debug/stepreturn"));
      putValue(SHORT_DESCRIPTION,"Step until end of frame and return");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepReturnDebug");
	       waitForFreeze();
	       bump_client.stepReturn(bt);
	     }
	    break;
       }
    }

}	// end of inner class StepReturnAction




private class DropToFrameAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   DropToFrameAction() {
      super("Drop to Frame",BoardImage.getIcon("debug/droptoframe"));
      putValue(SHORT_DESCRIPTION,"Start current frame over");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","DropToFrameDebug");
	       waitForFreeze();
	       bump_client.dropToFrame(bt);
	     }
	    break;
       }
    }

}	// end of inner class DropToFrameAction




private class StartDebug implements Runnable {

   @Override public void run() {
      bump_client.saveAll();
      BudaRoot br = BudaRoot.findBudaRoot(BddtLaunchControl.this);
      br.handleSaveAllRequest();
      String id = "B_" + Integer.toString(((int)(Math.random() * 100000)));
      BumpProcess bp = bump_client.startDebug(launch_config,id);
      if (bp != null) setLaunchState(LaunchState.RUNNING);
      cur_process = bp;
    }

}	// end of inner class StartDebug



/********************************************************************************/
/*										*/
/*	Bubble button actions							*/
/*										*/
/********************************************************************************/

private class ConsoleAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ConsoleAction() {
      super("Console",BoardImage.getIcon("debug/console"));
      putValue(SHORT_DESCRIPTION,"Bring up console bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      // BddtFactory.getFactory().makeConsoleBubble(BddtLaunchControl.this,BddtLaunchControl.this);
      BoardMetrics.noteCommand("BDDT","CreateConsoleBubble");
      bubble_manager.createConsoleBubble();
    }

}	// end of inner class ConsoleAction



private class ThreadsAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ThreadsAction() {
      super("Threads",BoardImage.getIcon("debug/threads"));
      putValue(SHORT_DESCRIPTION,"Bring up threads bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      // BddtFactory.getFactory().makeThreadBubble(BddtLaunchControl.this,BddtLaunchControl.this);
      BoardMetrics.noteCommand("BDDT","CreateThreadBubble");
      bubble_manager.createThreadBubble();
    }

}	// end of inner class ThreadsAction



private class HistoryAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   HistoryAction() {
      super("History",BoardImage.getIcon("debug/history"));
      putValue(SHORT_DESCRIPTION,"Bring up debug history bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateHistoryBubble");
      BddtFactory.getFactory().makeHistoryBubble(BddtLaunchControl.this,BddtLaunchControl.this);
    }

}	// end of inner class HistoryAction



private class PerformanceAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PerformanceAction() {
      super("Performance",BoardImage.getIcon("debug/perf"));
      putValue(SHORT_DESCRIPTION,"Bring up performance bubble");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreatePerformanceBubble");
      BddtFactory.getFactory().makePerformanceBubble(BddtLaunchControl.this,BddtLaunchControl.this);
   }

}	// end of inner class PerformanceAction





private class NewChannelAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NewChannelAction() {
      super("New Channel",BoardImage.getIcon("debug/newchannel"));
      putValue(SHORT_DESCRIPTION,"Start a new debugging channel for this configuration");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","NewDebugChannel");
      BddtFactory bf = BddtFactory.getFactory();
      bf.newDebugger(launch_config);
    }

}	// end of inner class ThreadsAction



/********************************************************************************/
/*										*/
/*	Methods to handle freeze in background					*/
/*										*/
/********************************************************************************/

private synchronized void waitForFreeze()
{
   while (freeze_count > 0) {
      try {
	 wait(10000);
       }
      catch (InterruptedException e) { }
    }
}


synchronized void startFreeze()
{
   ++freeze_count;
}



synchronized void doneFreeze()
{
   --freeze_count;
   if (freeze_count < 0) freeze_count = 0;
   if (freeze_count <= 0) notifyAll();
}



/********************************************************************************/
/*										*/
/*	Event handling								*/
/*										*/
/********************************************************************************/

private class RunEventHandler implements BumpRunEventHandler {

   private Map<BumpThread,BumpThreadState> thread_states;
   private BumpThread		last_stopped;

   RunEventHandler() {
      thread_states = new HashMap<BumpThread,BumpThreadState>();
      last_stopped = null;
    }

   BumpThread getLastStoppedThread()		{ return last_stopped; }

   @Override public void handleLaunchEvent(BumpRunEvent evt) { }

   @Override synchronized public void handleProcessEvent(BumpRunEvent evt) {
      switch (evt.getEventType()) {
	 case PROCESS_ADD :
	    if (cur_process == null && launch_state == LaunchState.STARTING &&
		   evt.getLaunchConfiguration() == launch_config) {
	       cur_process = evt.getProcess();
	       last_stopped = null;
	       BddtFactory.getFactory().getConsoleControl().clearConsole(cur_process);
	       BddtFactory.getFactory().getHistoryControl().clearHistory(cur_process);
	     }
	    break;
	 case PROCESS_REMOVE :
	    if (cur_process == evt.getProcess()) {
	       setLaunchState(LaunchState.TERMINATED);
	       thread_states.clear();
	       cur_process = null;
	       last_stopped = null;
	     }
	    break;
       }
    }

   @Override synchronized public void handleThreadEvent(BumpRunEvent evt) {
      if (evt.getProcess() != cur_process) return;
      BumpThread bt = evt.getThread();
      BumpThreadState ost = thread_states.get(bt);

      switch (evt.getEventType()) {
	 case THREAD_ADD :
	 case THREAD_CHANGE :
	    thread_states.put(bt,bt.getThreadState());
	    if (bt.getThreadState() != ost) {
	       handleThreadStateChange(bt,ost);
	       if (bt.getThreadState().isStopped()) last_stopped = bt;
	       else if (last_stopped == bt) last_stopped = null;
	     }
	    break;
	 case THREAD_REMOVE :
	    removeExecutionAnnot(bt);
	    if (bt == last_stopped) last_stopped = null;
	    thread_states.remove(bt);
	    break;
	 case THREAD_TRACE :
	 case THREAD_HISTORY :
	    return;
       }

      int tct = thread_states.size();
      int rct = 0;
      for (Map.Entry<BumpThread,BumpThreadState> ent : thread_states.entrySet()) {
	 BumpThreadState bts = ent.getValue();
	 if (bts.isStopped() && last_stopped == null) last_stopped = ent.getKey();
	 else if (bts.isRunning()) ++rct;
       }
      if (tct == 0) setLaunchState(LaunchState.TERMINATED);
      else if (rct == 0) setLaunchState(LaunchState.PAUSED);
      else if (rct == tct) setLaunchState(LaunchState.RUNNING);
      else setLaunchState(LaunchState.PARTIAL_PAUSE,rct,tct);
    }

   @Override public void handleConsoleMessage(BumpProcess proc,boolean err,String msg) { }

}	// end of inner class RunEventHandler



/********************************************************************************/
/*										*/
/*	Methods to handle thread state changes					*/
/*										*/
/********************************************************************************/

private void handleThreadStateChange(BumpThread bt,BumpThreadState ost)
{
   BoardLog.logD("BDDT","Thread state change " + bt.getThreadState() + " " + ost);
   if (bt.getThreadState().isStopped() && (ost != null && !ost.isStopped())) {
      addExecutionAnnot(bt);
      BumpThreadStack stk = bt.getStack();
      if (stk != null) {
	 CreateBubble cb = new CreateBubble(bt);
	 SwingUtilities.invokeLater(cb);
       }
    }
   else if (!bt.getThreadState().isStopped()) {
      removeExecutionAnnot(bt);
    }
}



private class CreateBubble implements Runnable {

   private BumpThread for_thread;

   CreateBubble(BumpThread bt) {
      for_thread = bt;
    }

   @Override public void run() {
      bubble_manager.createExecBubble(for_thread);
    }

}	// end of inner class CreateBubble



/********************************************************************************/
/*										*/
/*	Execution annotations							*/
/*										*/
/********************************************************************************/

private void addExecutionAnnot(BumpThread bt)
{
   removeExecutionAnnot(bt);

   BumpThreadStack stk = bt.getStack();
   if (stk != null && stk.getNumFrames() > 0) {
      BumpStackFrame bsf = stk.getFrame(0);
      if (bsf.getFile() != null && bsf.getFile().exists() && bsf.getLineNumber() > 0) {
	 ExecutionAnnot ea = new ExecutionAnnot(bt,bsf);
	 exec_annots.put(bt,ea);
	 BaleFactory.getFactory().addAnnotation(ea);
      }
    }
}


private void removeExecutionAnnot(BumpThread bt)
{
   ExecutionAnnot ea = exec_annots.remove(bt);
   if (ea != null) BaleFactory.getFactory().removeAnnotation(ea);
}



private class ExecutionAnnot implements BaleAnnotation {

   private BumpThread for_thread;
   private BumpStackFrame for_frame;
   private BaleFileOverview for_document;
   private Position execute_pos;
   private Color annot_color;
   private File for_file;

   ExecutionAnnot(BumpThread th,BumpStackFrame frm) {
      for_thread = th;
      for_frame = frm;
      for_file = frm.getFile();
      for_document = BaleFactory.getFactory().getFileOverview(null,for_file);
      int off = for_document.findLineOffset(frm.getLineNumber());
      BoardProperties bp = BoardProperties.getProperties("Bddt");
      annot_color = bp.getColor(BDDT_EXECUTE_ANNOT_COLOR,new Color(0x4000ff00,true));

      execute_pos = null;
      try {
	 execute_pos = for_document.createPosition(off);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BDDT","Bad execution position",e);
       }
    }



   @Override public int getDocumentOffset()	{ return execute_pos.getOffset(); }
   @Override public File getFile()		{ return for_file; }

   @Override public Icon getIcon() {
      return BoardImage.getIcon("exec");
    }

   @Override public String getToolTip() {
      return "Thread " + for_thread.getName() + " stopped at " + for_frame.getLineNumber();
    }

   @Override public Color getLineColor()			{ return annot_color; }

   @Override public boolean getForceVisible(BudaBubble bb) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      return (bba == bba1);
    }

   @Override public int getPriority()				{ return 20; }

   @Override public void addPopupButtons(JPopupMenu m)		{ }

}	// end of inner class ExecutionAnnot



/********************************************************************************/
/*										*/
/*	Handle contextual operations on debugger editors			*/
/*										*/
/********************************************************************************/

private class EditorContextListener implements BaleFactory.BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu m) {
      if (isRelevant(cfg)) m.add(new ValueAction(cfg));
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      if (isRelevant(cfg)) {
	 String id = cfg.getToken();
	 BumpStackFrame frm = bubble_manager.getFrameForBubble(cfg.getEditor());
	 if (frm != null) {
	    BumpRunValue rv = null;
	    switch (cfg.getTokenType()) {
	       case FIELD_ID :
	       case FIELD_DECL_ID :
		  BumpRunValue rv1 = frm.getValue("this");
		  if (rv1 != null) {
		     rv = rv1.getValue("this?" + id);
		   }
		  break;
	     }
	    if (rv == null) rv = frm.getValue(id);

	    if (rv != null) {
	       switch (rv.getKind()) {
		  default :
		  case ARRAY :
		  case CLASS :
		  case OBJECT :
		  case UNKNOWN :
		     // return rv.getDetail();
		     break;
		  case PRIMITIVE :
		  case STRING :
		     return rv.getValue();
		}
	     }
	    String expr = "(" + id + ").toString()";
	    if (rv != null && rv.getKind() == BumpValueKind.ARRAY) {
	       if (rv.getLength() <= 100) {
		  expr = "java.util.Arrays.toString(" + id + ")";
		}
	     }
	    else if (rv != null && rv.getKind() == BumpValueKind.OBJECT && rv.getValue().equals("null")) {
	       return "null";
	     }

	    BumpThreadState ts = frm.getThread().getThreadState();
	    if (!ts.isStopped()) return null;

	    EvaluationListener el = new EvaluationListener();
	    if (frm.evaluateInternal(expr,el)) {
	       //TODO: format the result a bit if it is too long
	       return el.getResult();
	     }
	  }
       }
      return null;
    }

   private boolean isRelevant(BaleContextConfig cfg) {
      BudaBubble bb = cfg.getEditor();
      if (bb == null) return false;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      if (bba == null || bba != bba1) return false;
      if (cur_process == null || !cur_process.isRunning()) return false;
      if (cfg.getToken() == null) return false;
      switch (cfg.getTokenType()) {
	 case FIELD_ID :
	 case LOCAL_ID :
	 case STATIC_FIELD_ID :
	 case LOCAL_DECL_ID :
	 case FIELD_DECL_ID :
	 case CONST_ID :
	    break;
	 default :
	    return false;
       }
      return true;
    }

}	// end of inner class EditorContextListener


private class EvaluationListener implements BumpEvaluationHandler {

   private boolean is_done;
   private String  result_value;

   EvaluationListener() {
      is_done = false;
      result_value = null;
    }

   synchronized String getResult() {
      while (!is_done) {
	 try {
	    wait(1000l);
	  }
	 catch (InterruptedException e) { }
       }
      return result_value;
    }

   @Override public void evaluationResult(String eid,String expr,BumpRunValue v) {
      result_value = v.getValue();
      synchronized (this) {
	 is_done = true;
	 notifyAll();
       }
    }

   @Override public void evaluationError(String eid,String expr,String error) {
      synchronized (this) {
	 is_done = true;
	 notifyAll();
       }
    }

}	// end of class EvaluationListener


private class ValueEvalListener implements BumpEvaluationHandler, Runnable {

   private BaleContextConfig config_context;
   private BumpRunValue run_value;

   ValueEvalListener(BaleContextConfig ctx) {
      config_context = ctx;
   }

   @Override public void evaluationResult(String eid,String ex,BumpRunValue v) {
      run_value = v;
      SwingUtilities.invokeLater(this);
   }

   @Override public void evaluationError(String eid,String ex,String er) { }

   @Override public void run() {
      BddtStackView bsv = new BddtStackView(BddtLaunchControl.this,run_value);
      Rectangle r = BudaRoot.findBudaLocation(config_context.getEditor());
      BudaConstraint bc = new BudaConstraint(r.x,r.y + r.height+20);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      bba.add(bsv,bc);
    }


}	// end of inner class ValueEvalListener



private class ValueAction extends AbstractAction {

   private BaleContextConfig context_config;

   ValueAction(BaleContextConfig cfg) {
      super("Show value of " + cfg.getToken());
      context_config = cfg;
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BumpStackFrame frm = bubble_manager.getFrameForBubble(context_config.getEditor());
      if (frm == null) return;
      BoardMetrics.noteCommand("BDDT","ShowValue");
      ValueEvalListener el = new ValueEvalListener(context_config);
      frm.evaluateInternal(context_config.getToken(),el);
   }

}	// end of inner class ValueAction



}	// end of class BddtLaunchControl




/* end of BddtLaunchControl.java */
