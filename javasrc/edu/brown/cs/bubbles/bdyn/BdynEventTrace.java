/********************************************************************************/
/*										*/
/*		BdynEventTrace.java						*/
/*										*/
/*	Hold event trace for current run					*/
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


import edu.brown.cs.ivy.swing.SwingEventListenerList;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;


class BdynEventTrace implements BdynConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpProcess	for_process;
private ThreadData	current_thread;
private PriorityQueue<TraceEntry> pending_entries;
private Map<Integer,ThreadData> thread_map;
private long		next_time;
private Boolean 	cpu_time;
private int		thread_counter;
private int		task_counter;
private BdynFactory	bdyn_factory;
private Map<Integer,OutputTask> object_tasks;
private OutputTask	dummy_task;
private SortedSet<OutputEntry> output_set;
private long		max_delta;
private SortedSet<ThreadData> active_threads;
private SwingEventListenerList<BdynEventUpdater> update_listeners;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdynEventTrace(BumpProcess bp)
{
   for_process = bp;
   current_thread = null;
   pending_entries = new PriorityQueue<TraceEntry>(100,new EntryComparator());
   thread_map = new HashMap<Integer,ThreadData>();
   next_time = 0;
   cpu_time = null;
   thread_counter = 0;
   task_counter = 0;
   bdyn_factory = BdynFactory.getFactory();
   object_tasks = new HashMap<Integer,OutputTask>();
   dummy_task = new OutputTask(0,null);
   output_set = new ConcurrentSkipListSet<OutputEntry>();
   active_threads = new TreeSet<ThreadData>();
   max_delta = 1;
   update_listeners = new SwingEventListenerList<BdynEventUpdater>(BdynEventUpdater.class);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addUpdateListener(BdynEventUpdater up)
{
   update_listeners.add(up);
}


void removeUpdateListener(BdynEventUpdater up)
{
   update_listeners.remove(up);
}


long getStartTime()
{
   if (output_set.isEmpty()) return 0;
   return output_set.first().getStartTime();
}


long getEndTime()
{
   if (output_set.isEmpty()) return 0;
   return output_set.last().getEndTime();
}

int getActiveThreadCount()
{
   return active_threads.size();
}


List<BdynEntryThread> getActiveThreads()
{
   return new ArrayList<BdynEntryThread>(active_threads);
}


BumpProcess getProcess()		{ return for_process; }


void clear()
{
   pending_entries.clear();
   output_set.clear();
   object_tasks.clear();
   active_threads.clear();
   next_time = 0;
   current_thread = null;
   thread_map.clear();
   cpu_time = null;
   thread_counter = 0;
   task_counter = 0;
   max_delta = 1;
}



/********************************************************************************/
/*										*/
/*	Trace creation methods							*/
/*										*/
/********************************************************************************/

void addEntry(String s)
{
   char s0 = s.charAt(0);
   if (s0 == 'T') {                             // THREAD
      StringTokenizer tok = new StringTokenizer(s);
      tok.nextToken();
      int id = Integer.parseInt(tok.nextToken());
      current_thread = thread_map.get(id);
      if (current_thread == null) {
	 int tid = Integer.parseInt(tok.nextToken());
	 String tnm = tok.nextToken("\n");
	 current_thread = new ThreadData(++thread_counter,tid,tnm);
	 thread_map.put(id,current_thread);
       }
    }
   else if (s0 == 'D') {                        // DONE
      StringTokenizer tok = new StringTokenizer(s);
      tok.nextToken();
      long time = Long.parseLong(tok.nextToken());
      if (next_time != 0) {
	 int ct = 0;
	 while (!pending_entries.isEmpty() && pending_entries.peek().getTime() < next_time) {
	    TraceEntry te = pending_entries.remove();
	    ++ct;
	    outputEntry(te);
	  }
	 if (ct > 0) {
	    for (BdynEventUpdater eu : update_listeners) {
	       eu.eventsAdded();
	     }
	  }
       }
      next_time = time;
    }
   else if (s0 == 'S') {
      if (cpu_time == null) {
	 StringTokenizer tok = new StringTokenizer(s);
	 tok.nextToken();
	 cpu_time = Boolean.parseBoolean(tok.nextToken());
       }
    }
   else if (cpu_time != null) {
      TraceEntry te = new TraceEntry(s,current_thread,cpu_time);
      pending_entries.add(te);
    }
}



private void outputEntry(TraceEntry te)
{
   ThreadData td = te.getThread();
   if (td == null) return;

   System.err.println("TRACE: " + te);

   BdynCallback cb = bdyn_factory.getCallback(te.getEntryLocation());
   if (cb == null) return;
   OutputTask ot = td.getCurrentTask();

   if (cb.getCallbackType() == CallbackType.CONSTRUCTOR) {
      if (ot == null) return;
      int i0 = te.getObject1();
      if (i0 != 0) {
	 // System.err.println("ASSOC TASK " + i0 + " " + te.getObject2() + " " + ot.getTaskId());
	 OutputTask ot1 = object_tasks.get(i0);
	 if (ot1 == null) object_tasks.put(i0,ot);
	 else if (ot1 != dummy_task && ot1 != ot) object_tasks.put(i0,dummy_task);
	 return;
       }
    }
   else if (ot == null) {
      if (te.isExit()) return;
      int i0 = te.getObject1();
      int i1 = te.getObject2();
      if (i0 != 0) {
	 ot = object_tasks.get(i0);
	 if (ot == dummy_task) ot = null;
       }
      if (ot == null) {
	 if (i1!= 0) {
	    ot = object_tasks.get(i1);
	    if (ot == dummy_task) ot = null;
	  }
       }
      if (ot == null) {
	 // System.err.println("CREATE TASK " + (task_counter+1) + " " + i0 + " " + i1);
	 ot = new OutputTask(++task_counter,cb);
       }
      td.beginTask(ot,cb,te.getTime());
    }
   else if (te.isExit() && td.getNestLevel() == 1) {
      long start = td.getStartTime();
      max_delta = Math.max(max_delta,te.getTime() - start);
      // BdynCallback cbs = td.getTaskCallback();
      // System.err.println("TASK " + td.getOutputId() + " " + td.getThreadId() + " " +
			    // td.getThreadName() + " " + start + " " + te.getTime() + " " +
			    // (te.getTime() - start) + " " +
			    // cbs.getId() + " " + cb.getId() + " " +
			    // ot.getTaskId() + " " + ot.getTaskRoot().getId() + " " +
			    // te.getCpuTime() + " " +
			    // cbs.getClassName() + "." + cbs.getMethodName());
      OutputEntry oe = new OutputEntry(start,te.getTime(),td,ot,td.getTaskCallback());
      output_set.add(oe);
      active_threads.add(td);
      td.endTask();
    }
   else td.nestTask(te.isExit());
}



/********************************************************************************/
/*										*/
/*	Methods for accessing output data					*/
/*										*/
/********************************************************************************/

BdynRangeSet getRange(long t0,long t1)
{
   BdynRangeSet rslt = addToRange(t0-max_delta,t0,t1,null);

   return rslt;
}


BdynRangeSet updateRange(BdynRangeSet rslt,long t0,long t1)
{
   rslt = pruneRange(rslt,t0,t1);
   rslt = addToRange(t0,t0,t1,rslt);
   return rslt;
}



private BdynRangeSet pruneRange(BdynRangeSet rslt,long t0,long t1)
{
   if (rslt == null) return null;

   for (Iterator<Set<BdynEntry>> it = rslt.values().iterator(); it.hasNext(); ) {
      Set<BdynEntry> vals = it.next();
      int ct = 0;
      for (Iterator<BdynEntry> it1 = vals.iterator(); it1.hasNext(); ) {
	 BdynEntry oe = it1.next();
	 if (oe.getEndTime() < t0) it1.remove();
	 else ++ct;
       }
      if (ct == 0) it.remove();
    }

   if (rslt.size() == 0) return null;

   return rslt;
}



private BdynRangeSet addToRange(long start,long t0,long t1,BdynRangeSet rslt)
{
   OutputEntry timee = new OutputEntry(start);
   SortedSet<OutputEntry> ss = output_set.tailSet(timee);
   for (OutputEntry e1 : ss) {
      if (e1.getStartTime() > t1) break;
      if (e1.getEndTime() >= t0) {
	 ThreadData td = e1.getThread();
	 if (rslt == null) rslt = new BdynRangeSet();
	 Set<BdynEntry> r1 = rslt.get(td);
	 if (r1 == null) {
	    r1 = new HashSet<BdynEntry>();
	    rslt.put(td,r1);
	 }
	 r1.add(e1);
      }
   }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Thread information							*/
/*										*/
/********************************************************************************/

private static class ThreadData implements Comparable<ThreadData>, BdynEntryThread {

   private int	output_id;
   private String thread_name;
   private OutputTask current_task;
   private BdynCallback task_root;
   private long task_start;
   private int	nest_level;

   ThreadData(int oid,int tid,String tnm) {
      output_id = oid;
      thread_name = tnm;
      current_task = null;
      nest_level = 0;
    }

   @Override public String getThreadName() { return thread_name; }
   int getOutputId()			{ return output_id; }
   OutputTask getCurrentTask()		{ return current_task; }
   int getNestLevel()			{ return nest_level; }
   BdynCallback getTaskCallback()	{ return task_root; }
   long getStartTime()			{ return task_start; }

   void beginTask(OutputTask ot,BdynCallback cb,long when) {
      current_task = ot;
      task_root = cb;
      task_start = when;
      nest_level = 1;
    }

   void endTask() {
      current_task = null;
      task_root = null;
      task_start = 0;
      nest_level = 0;
    }

   void nestTask(boolean exit) {
      if (exit) {
	 if (--nest_level == 0) {
	    endTask();
	  }
       }
      else ++nest_level;
    }

   @Override public int compareTo(ThreadData td) {
      return getThreadName().compareTo(td.getThreadName());
    }

}	// end of inner class ThreadData



/********************************************************************************/
/*										*/
/*	Trace Entry								*/
/*										*/
/********************************************************************************/

private static class TraceEntry {

   private long 	entry_time;
   private ThreadData	entry_thread;
   private int		entry_loc;
   private int		entry_o1;
   private int		entry_o2;
   private boolean	is_exit;

   TraceEntry(String s,ThreadData td,boolean cputime) {
      String [] args = s.split(" ");
      is_exit = false;
      int ct = 0;
      entry_loc = Integer.parseInt(args[ct++]);
      if (entry_loc < 0) {
	 is_exit = true;
	 entry_loc = -entry_loc;
       }
      entry_time = Long.parseLong(args[ct++]);
      if (cputime && ct < args.length) Long.parseLong(args[ct++]);
      entry_thread = td;
      if (ct < args.length) entry_o1 = Integer.parseInt(args[ct++]);
      else entry_o1 = 0;
      if (ct < args.length) entry_o2 = Integer.parseInt(args[ct++]);
      else entry_o2 = 0;
    }

   long getTime()			{ return entry_time; }
   ThreadData getThread()		{ return entry_thread; }
   int getEntryLocation()		{ return entry_loc; }
   int getObject1()			{ return entry_o1; }
   int getObject2()			{ return entry_o2; }
   boolean isExit()			{ return is_exit; }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append(entry_loc);
      if (is_exit) buf.append("^");
      buf.append(" ");
      buf.append(entry_time);
      buf.append(" ");
      buf.append(entry_thread.getOutputId());
      buf.append(" ");
      buf.append(entry_o1);
      buf.append(" ");
      buf.append(entry_o2);
      return buf.toString();
   }

}	// end of inner class TraceEntry


private static class EntryComparator implements Comparator<TraceEntry>
{

   @Override public int compare(TraceEntry e1,TraceEntry e2) {
      long d0 = e1.getTime() - e2.getTime();
      if (d0 < 0) return -1;
      else if (d0 > 0) return 1;
      return 0;
    }

}	// end of inner class EntryComparator


/********************************************************************************/
/*										*/
/*	OutputEntry -- entry to actually visualize				*/
/*										*/
/********************************************************************************/

private static class OutputEntry implements Comparable<OutputEntry>, BdynEntry {

   private long start_time;
   private long finish_time;
   private ThreadData entry_thread;
   private OutputTask entry_transaction;
   private BdynCallback entry_task;

   OutputEntry(long startt,long endt,ThreadData td,OutputTask ot,BdynCallback tt) {
      start_time = startt;
      finish_time = endt;
      entry_thread = td;
      entry_transaction = ot;
      entry_task = tt;
    }

   OutputEntry(long time) {
      start_time = time;
      finish_time = time;
      entry_thread = null;
      entry_transaction = null;
      entry_task = null;
    }

   @Override public long getStartTime() 	{ return start_time; }
   @Override public long getEndTime()		{ return finish_time; }
   ThreadData getThread()			{ return entry_thread; }
   @Override public BdynEntryThread getEntryThread()	{ return entry_thread; }
   @Override public BdynCallback getEntryTask() { return entry_task; }
   @Override public BdynCallback getEntryTransaction()	{ return entry_transaction.getTaskRoot(); }

   @Override public int compareTo(OutputEntry e) {
      long dl = start_time - e.start_time;
      if (dl < 0) return -1;
      if (dl > 0) return 1;
      dl = finish_time = e.finish_time;
      if (dl < 0) return -1;
      if (dl > 0) return 1;
      int idl = entry_thread.getOutputId() - e.entry_thread.getOutputId();
      if (idl < 0) return -1;
      if (idl > 0) return 1;
      return 0;
    }

}	// end of inner class OutputEntry



private static class OutputTask implements BdynEntryTask {

   private BdynCallback task_root;

   OutputTask(int id,BdynCallback root) {
      task_root = root;
    }

   @Override public BdynCallback getTaskRoot()	{ return task_root; }

}



}	// end of class BdynEventTrace




/* end of BdynEventTrace.java */

