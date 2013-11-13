/********************************************************************************/
/*										*/
/*		BdynTaskWindow.java						*/
/*										*/
/*	Window to hold transaction-task-thread visualization			*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bdyn;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingRangeScrollBar;

import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;


class BdynTaskWindow extends JPanel implements BdynConstants, AdjustmentListener,
	BdynConstants.BdynEventUpdater
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpProcess for_process;
private TaskPanel task_panel;
private SwingRangeScrollBar time_bar;
private long min_time;
private long max_time;
private BdynEventTrace event_trace;
private List<BdynEntryThread> active_threads;
private RunHandler process_handler;
private Map<BdynCallback,Color> callback_set;

private static final int MAX_TIME = 1000;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynTaskWindow()
{
   super(new BorderLayout());
   task_panel = new TaskPanel();
   time_bar = new SwingRangeScrollBar(SwingRangeScrollBar.HORIZONTAL,0,MAX_TIME,0,MAX_TIME);
   time_bar.addAdjustmentListener(this);
   min_time = -1;
   max_time = 0;
   event_trace = null;
   callback_set = null;
   process_handler = new RunHandler();
   active_threads = new ArrayList<BdynEntryThread>();

   BumpClient.getBump().getRunModel().addRunEventHandler(process_handler);

   add(task_panel,BorderLayout.CENTER);
   add(time_bar,BorderLayout.SOUTH);
}



void dispose()
{
   BumpClient.getBump().getRunModel().removeRunEventHandler(process_handler);
   event_trace = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public BudaBubble getBubble()
{
   return new TaskBubble(this);
}



public void setProcess(BumpProcess bp)
{
   BdynFactory bf = BdynFactory.getFactory();
   BdynProcess dp = bf.getBdynProcess(bp);
   BdynEventTrace et = dp.getEventTrace();
   setEventTrace(et);
}



void setEventTrace(BdynEventTrace et)
{
   if (event_trace == et) return;

   if (event_trace != null) {
      event_trace.removeUpdateListener(this);
    }

   event_trace = et;
   if (et != null) {
      callback_set = null;
      active_threads = new ArrayList<BdynEntryThread>();
      for_process = et.getProcess();
      event_trace.addUpdateListener(this);
      min_time = event_trace.getStartTime();
      max_time = event_trace.getEndTime();
      if (max_time == min_time) {
	 min_time = -1;
	 max_time = 0;
       }
      else {
	 time_bar.setValues(0,MAX_TIME,0,MAX_TIME);
       }
      if (et.getActiveThreadCount() != active_threads.size()) {
	 active_threads = et.getActiveThreads();
       }
    }
   else {
      min_time = -1;
      max_time = 0;
    }

   repaint();
}


/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void eventsAdded()
{
   BdynEventTrace evt = event_trace;

   if (evt == null || active_threads == null) return;
   long mxt = evt.getEndTime();
   if (mxt == 0) return;

   if (min_time < 0) {
      min_time = evt.getStartTime();
    }

   if (mxt > max_time) {
      if (evt.getActiveThreadCount() != active_threads.size()) {
	 active_threads = evt.getActiveThreads();
       }
      long omax = max_time;
      max_time = mxt;
      int lv = time_bar.getLeftValue();
      int rv = time_bar.getRightValue();
      double nmxv = (max_time - min_time);
      double t0 = lv * omax / MAX_TIME + min_time;
      double t1 = rv * omax / MAX_TIME + min_time;
      if (rv == MAX_TIME) t1 = max_time;
      int it0 = (int)Math.round((t0 - min_time)/nmxv * MAX_TIME);
      int it1 = (int)Math.round((t1 - min_time)/nmxv * MAX_TIME);
      time_bar.setValues(it0,it1);
      task_panel.setTimes((long) t0,(long) t1);
    }
}




/********************************************************************************/
/*										*/
/*	Methods for handling graphics						*/
/*										*/
/********************************************************************************/

private Color getOutsideColor(Set<BdynEntry> data,long delta)
{
   if (data == null || data.isEmpty()) return null;

   AccumMap accum = new AccumMap();
   for (BdynEntry be : data) {
      BdynCallback cb = be.getEntryTransaction();
      long d0 = be.getEndTime() - be.getStartTime();
      accum.addEntry(cb,d0);
    }

   SortedSet<AccumEntry> rslt = accum.getResult();
   AccumEntry ae = rslt.first();

   return getColor(ae.getCallback());
}


private Map<Color,Double> getInsideColor(Set<BdynEntry> data,long delta)
{
   if (data == null || data.isEmpty()) return null;

   AccumMap accum = new AccumMap();
   for (BdynEntry be : data) {
      BdynCallback cb = be.getEntryTask();
      long d0 = be.getEndTime() - be.getStartTime();
      accum.addEntry(cb,d0);
    }
   long d1 = accum.getTotalTime();
   if (d1 > delta) delta = d1;

   Map<Color,Double> rslt = new LinkedHashMap<Color,Double>();
   for (AccumEntry ae : accum.getResult()) {
      Color c0 = getColor(ae.getCallback());
      double f = ((double) ae.getTime())/((double) delta);
      rslt.put(c0,f);
    }

   return rslt;
}



private Color getColor(BdynCallback cb)
{
   if (callback_set == null) {
      callback_set = new HashMap<BdynCallback,Color>();
    }

   Color c = callback_set.get(cb);
   if (c != null) return c;

   float h = 0;
   int ct = callback_set.size();
   if (ct == 0) h = 0;
   else if (ct == 1) h = 0.5f;
   else {
      int n0 = 2;
      float n = ct - 2;
      float incr = 0.5f;
      for ( ; ; ) {
	 if (n < n0) {
	    h = incr/2.0f + n*incr;
	    break;
	  }
	 n -= n0;
	 n0 *= 2;
	 incr /= 2.0;
       }
    }
   c = new Color(Color.HSBtoRGB(h,1f,1f));
   callback_set.put(cb,c);

   return c;
}



private static class AccumEntry implements Comparable<AccumEntry> {

   private BdynCallback call_back;
   private long total_time;

   AccumEntry(BdynCallback cb,long tot) {
      call_back = cb;
      total_time = tot;
    }

   BdynCallback getCallback()		{ return call_back; }
   long getTime()			{ return total_time; }

   @Override public int compareTo(AccumEntry e) {
      long d = e.total_time - total_time;
      if (d < 0) return -1;
      if (d > 0) return 1;
      return 0;
    }

}	// end of inner class AccumEntry




private static class AccumMap extends HashMap<BdynCallback,long []> {

   private long total_time;

   AccumMap() {
      total_time = 0;
    }

   void addEntry(BdynCallback cb,long delta) {
      long [] v = get(cb);
      if (v == null) {
	 v = new long[1];
	 v[0] = 0;
	 put(cb,v);
       }
      v[0] += delta;
      total_time += delta;
    }

   long getTotalTime()			{ return total_time; }

   SortedSet<AccumEntry> getResult() {
      SortedSet<AccumEntry> rslt = new TreeSet<AccumEntry>();
      for (Map.Entry<BdynCallback,long []> ent : entrySet()) {
	 AccumEntry ae = new AccumEntry(ent.getKey(),ent.getValue()[0]);
	 rslt.add(ae);
       }
      return rslt;
    }

}	// end of inner class AccumMap



/********************************************************************************/
/*										*/
/*	Handle scroll bar							*/
/*										*/
/********************************************************************************/

@Override public void adjustmentValueChanged(AdjustmentEvent ev) {
   long mint = (long)(min_time + time_bar.getLeftValue() * (max_time-min_time)/MAX_TIME + 0.5);
   long maxt = (long)(min_time + time_bar.getRightValue() * (max_time-min_time)/MAX_TIME + 0.5);
   task_panel.setTimes(mint,maxt);
}



/********************************************************************************/
/*										*/
/*	Actual graphics panel							*/
/*										*/
/********************************************************************************/

private class TaskPanel extends JPanel {

   private long task_min_time;
   private long task_max_time;

   TaskPanel() {
      setPreferredSize(new Dimension(400,300));
      task_min_time = 0;
      task_max_time = 0;
      setToolTipText("Task Panel");
    }

   @Override public void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D) g0;
      int rows = active_threads.size();
      Dimension dim = getSize();
      g.setBackground(Color.WHITE);
      g.clearRect(0,0,dim.width,dim.height);
      if (event_trace == null || rows == 0 || task_min_time == task_max_time || task_min_time < 0)
	 return;

      BdynRangeSet rset = null;
      double y0 = 0;
      double yinc = dim.getHeight() / rows;
      double ttot = task_max_time - task_min_time;
      Rectangle2D r2 = new Rectangle2D.Double();
      for (int i = 0; i < dim.width; ++i) {
	 double y1 = y0;
	 long t0 = (long)(task_min_time + (i * ttot/dim.width) + 0.5);
	 long t1 = (long)(task_min_time + ((i+1) * ttot/dim.width) + 0.5);
	 if (i == 0) rset = event_trace.getRange(t0,t1);
	 else rset = event_trace.updateRange(rset,t0,t1);
	 if (rset == null) continue;
	 for (BdynEntryThread td : active_threads) {
	    Set<BdynEntry> out = rset.get(td);
	    Color outcol = getOutsideColor(out,t1-t0);
	    Map<Color,Double> incol = getInsideColor(out,t1-t0);
	    double ya = y1 + yinc * 0.10;
	    double yb = y1 + yinc * 0.25;
	    double yc = y1 + yinc * 0.75;
	    double yd = y1 + yinc * 0.90;
	    if (outcol != null) {
	       g.setColor(outcol);
	       r2.setFrame(i,ya,1,yb-ya);
	       g.fill(r2);
	       r2.setFrame(i,yc,1,yd-yc);
	       g.fill(r2);
	     }
	    if (incol != null) {
	       double ye = yc;
	       for (Map.Entry<Color,Double> ent : incol.entrySet()) {
		  Color c0 = ent.getKey();
		  double v0 = ent.getValue();
		  double yf = ye - (yc-yb)*v0;
		  r2.setFrame(i,yf,1,ye-yf);
		  g.setColor(c0);
		  g.fill(r2);
		  ye = yf;
		}
	     }
	    y1 += yinc;
	  }
       }
      for (int i = 1; i < rows; ++i) {
	 int y1 = (int) (yinc * i + 0.5);
	 g.setColor(Color.BLACK);
	 g.drawLine(0,y1,dim.width,y1);
       }
    }

   void setTimes(long min,long max) {
      if (task_min_time != min || task_max_time != max) {
	 task_min_time = min;
	 task_max_time = max;
	 repaint();
       }
    }

   @Override public String getToolTipText(MouseEvent evt) {
      if (event_trace == null) return "Task Panel";

      Dimension dim = getSize();
      int rows = active_threads.size();
      double yinc = dim.getHeight()/rows;
      int row = (int)(evt.getY()/yinc);
      if (row >= active_threads.size()) return "Task Panel";
      BdynEntryThread thr = active_threads.get(row);
      if (thr == null) return "Task Panel";

      double t0 = task_min_time + (evt.getX() * (task_max_time - task_min_time) / dim.getWidth());
      double t1 = task_min_time + ((evt.getX() + 1) * (task_max_time - task_min_time) / dim.getWidth());
      BdynRangeSet alldata = event_trace.getRange((long) t0, (long) t1);
      Set<BdynEntry> data = null;
      if (alldata != null) data = alldata.get(thr);

      StringBuffer buf = new StringBuffer();
      buf.append("<html>");
      buf.append("<p>Thread: " + thr.getThreadName());

      if (data != null) {
	 for (BdynEntry ent : data) {
	    buf.append("<p>");
	    buf.append(getLabel(ent.getEntryTransaction()));
	    buf.append(" :: ");
	    buf.append(getLabel(ent.getEntryTask()));
	  }
       }

      return buf.toString();
    }

   private String getLabel(BdynCallback cb)
   {
      if (cb == null) return "?";
      return cb.getClassName() + "." + cb.getMethodName();
   }

}	// end of inner class TaskPanel



/********************************************************************************/
/*										*/
/*	Run event handler							*/
/*										*/
/********************************************************************************/

private class RunHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)		{ }
   @Override public void handleThreadEvent(BumpRunEvent evt)		{ }
   @Override public void handleConsoleMessage(BumpProcess bp,boolean e,boolean f,String msg) { }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      BumpProcess blp;
      switch (evt.getEventType()) {
	 case PROCESS_CHANGE :
	 case PROCESS_ADD :
	    blp = evt.getProcess();
	    if (for_process == null) {
	       BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BdynTaskWindow.this);
	       if (bba != null) {
		  Object proc = bba.getProperty("Bddt.process");
		  if (proc != null && proc == blp) {
		     // process for this channel
		     setProcess(blp);
		     break;
		   }
		  if (proc == null && bba.getProperty("Bddt.debug") == null) {
		     // non-debug -- use next process
		     setProcess(blp);
		   }
		}
	     }
	    break;
	 case PROCESS_REMOVE :
	    blp = evt.getProcess();
	    if (for_process == blp) for_process = null;
	    break;
	 default :
	    break;
       }
    }

}	// end of inner class RunHandler




/********************************************************************************/
/*										*/
/*	Bubble for this window							*/
/*										*/
/********************************************************************************/

private static class TaskBubble extends BudaBubble {

   BdynTaskWindow task_window;

   TaskBubble(BdynTaskWindow w) {
      task_window = w;
      setContentPane(w,null);
    }

   @Override protected void localDispose() {
       task_window.dispose();
    }

}	// end of inner class TaskBubble


}	// end of class BdynTaskWindow




/* end of BdynTaskWindow.java */
