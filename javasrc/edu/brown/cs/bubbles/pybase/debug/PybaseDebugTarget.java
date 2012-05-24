/********************************************************************************/
/*										*/
/*		PybaseDebugTarget.java						*/
/*										*/
/*	Handle debugging target for Bubbles from Python 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;

import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class PybaseDebugTarget implements PybaseDebugConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Socket	comm_socket;
private DebugReader debug_reader;
private DebugWriter debug_writer;
private int	sequence_number;

private File	debug_file;
private List<PybaseDebugThread> thread_data;
private boolean is_disconnected;
private PybaseValueModificationChecker modification_checker;
private PybaseDebugger remote_debugger;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseDebugTarget()
{
   comm_socket = null;
   debug_reader = null;
   debug_writer = null;
   sequence_number = -1;

   is_disconnected = false;
   modification_checker = new PybaseValueModificationChecker();
   thread_data = null;
   debug_file = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getNextSequence()
{
   sequence_number += 2;
   return sequence_number;
}



PybaseValueModificationChecker getMofificationChecker() { return modification_checker; }



public boolean canTerminate()					{ return true; }
public boolean isTerminated()					{ return false; }
public boolean isSuspended()					{ return false; }

public boolean canResume()
{
   for (PybaseDebugThread t : thread_data) {
      if (t.canResume()) return true;
    }
   return false;
}


public boolean canSuspend()
{
   for (PybaseDebugThread t : thread_data) {
      if (t.canSuspend()) return true;
    }
   return false;
}

public boolean hasThreads()  			{ return true; }




public PybaseDebugger getDegugger()				{ return remote_debugger; }
public File getFile()                                           { return debug_file; }


public PybaseDebugThread findThreadById(String tid)
{
   for (PybaseDebugThread t : thread_data) {
      if (tid.equals(t.getId())) return t;
    }
   return null;
}


public boolean canDisconnect()			{ return !is_disconnected; }
public boolean isDisconnected() 		{ return is_disconnected; }
public void disconnect() 
{
   terminate();
   modification_checker = null;
}

	


/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

public void addToResponseQueue(PybaseDebugCommand cmd)
{
   if (debug_reader != null) {
      debug_reader.addToResponseQueue(cmd);
    }
}



public void postCommand(PybaseDebugCommand cmd)
{
   if (debug_writer != null) {
      debug_writer.postCommand(cmd);
    }
}



public void startTransmission(Socket s) throws IOException
{
   comm_socket = s;
   debug_reader = new DebugReader(s,this);
   debug_writer = new DebugWriter(s);
   debug_reader.start();
   debug_writer.start();
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

public void initialize() {
   // we post version command just for fun
   // it establishes the connection
   postCommand(new PybaseDebugCommand.Version(this));

   // now, register all the breakpoints in all projects
   // addBreakpointsFor(ResourcesPlugin.getWorkspace().getRoot());

   // Sending python exceptions before sending run command
   onSetConfiguredExceptions();

   // Send the run command, and we are off
   PybaseDebugCommand.Run run = new PybaseDebugCommand.Run(this);
   postCommand(run);
}



public void processCommand(String scode,String seq,String payload)
{
   try {
      int cmdcode = Integer.parseInt(scode);
	
      switch (cmdcode) {
	 case CMD_THREAD_CREATED :
	    processThreadCreated(payload);
	    break;
	 case CMD_THREAD_KILL :
	    processThreadKilled(payload);
	    break;
	 case CMD_THREAD_SUSPEND :
	    processThreadSuspended(payload);
	    break;
	 case CMD_THREAD_RUN :
	    processThreadRun(payload);
	    break;
	 default :
	    PybaseMain.logW("Unexpected debugger command " + scode + " " + seq + " " + payload);
	    break;
       }
    }
   catch (Exception e) {
      PybaseMain.logE("Error processing: " + scode + " payload: "+ payload, e);
    }	
}





public void resume() throws PybaseException
{
   for (PybaseDebugThread t : thread_data) {
      t.resume();
    }
}


public void suspend() throws PybaseException
{
   for (PybaseDebugThread t : thread_data) {
      t.suspend();
    }
}


public List<PybaseDebugThread> getThreads() throws PybaseException
{
   if (remote_debugger == null) return null;

   if (thread_data == null) {
      PybaseDebugCommand.ThreadList cmd = new PybaseDebugCommand.ThreadList(this);
      postCommand(cmd);
      try {
	 cmd.waitUntilDone(1000);
	 thread_data = cmd.getThreads();
       }
      catch (InterruptedException e) {
	 thread_data = new ArrayList<PybaseDebugThread>();
       }
    }
    
   return thread_data;
}



public void terminate() 
{
   if (comm_socket != null) {
      try {
	 comm_socket.shutdownInput(); // trying to make my pydevd notice that the socket is gone
       }
      catch (Exception e) { }
      try {
	 comm_socket.shutdownOutput();
       }
      catch (Exception e) { }
      try {
	 comm_socket.close();
       }
      catch (Exception e) { }
    }
   comm_socket = null;
   is_disconnected = true;

   if (debug_writer != null) {
      debug_writer.done();
      debug_writer = null;
    }
   if (debug_reader != null) {
      debug_reader.done();
      debug_reader = null;
    }

   thread_data = new ArrayList<PybaseDebugThread>();
   // fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
}



/********************************************************************************/
/*										*/
/*	Breakpoint methods							*/
/*										*/
/********************************************************************************/

public void onSetConfiguredExceptions()
{
   // Sending python exceptions to the debugger
   PybaseDebugCommand.SendPyException sendCmd = new PybaseDebugCommand.SendPyException(this);
   postCommand(sendCmd);
}


public void breakpointAdded(PybaseDebugBreakpoint b)
{
   if (b.isEnabled()) {
      String condition = b.getCondition();
      if(condition != null){
	 condition = condition.replaceAll("\n", "@_@NEW_LINE_CHAR@_@");
	 condition = condition.replaceAll("\t", "@_@TAB_CHAR@_@");
       }
      PybaseDebugCommand.SetBreakpoint  cmd = new PybaseDebugCommand.SetBreakpoint(
	 this, b.getFile(), b.getLine(), condition, b.getFunctionName());
      this.postCommand(cmd);
    }
}



public void breakpointRemoved(PybaseDebugBreakpoint b)
{
   PybaseDebugCommand.RemoveBreakpoint cmd = new PybaseDebugCommand.RemoveBreakpoint(this, b.getFile(), b.getLine());
   this.postCommand(cmd);
}


public void breakpointChanged(PybaseDebugBreakpoint breakpoint)
{
   breakpointRemoved(breakpoint);
   breakpointAdded(breakpoint);
}



/********************************************************************************/
/*										*/
/*	Thread methods								*/
/*										*/
/********************************************************************************/

private void processThreadCreated(String payload)
{
   List<PybaseDebugThread> newthreads = PybaseDebugThread.getThreadsFromXml(this, payload);

   for (Iterator<PybaseDebugThread> it = newthreads.iterator(); it.hasNext(); ) {
      PybaseDebugThread t = it.next();
      if (t.isPydevThread()) it.remove();
    }

   // add threads to the thread list, and fire event
   if (thread_data == null) {
      thread_data = newthreads;
    }
   else {
      thread_data.addAll(newthreads);
    }
  
   // Now notify debugger that new threads were added
   for (PybaseDebugThread thrd : newthreads) {
      System.err.println("ADD EVENT " + thrd);
      // fireEvent(new DebugEvent(thrd, DebugEvent.CREATE));
    }
}



private void processThreadKilled(String thread_id)
{
   PybaseDebugThread threadtodelete = findThreadById(thread_id);
   if (threadtodelete != null) {
      thread_data.remove(threadtodelete);
      // fireEvent(new DebugEvent(threadToDelete, DebugEvent.TERMINATE));
    }
}



private void processThreadSuspended(String payload)
{
   Element e = IvyXml.convertStringToXml(payload);
   Element te = IvyXml.getElementByTag(e,"thread");
   String tid = IvyXml.getAttrString(te,"id");
   PybaseDebugThread t = findThreadById(tid);
   if (t == null) {
      PybaseMain.logE("Problem reading thread suspended data: " + payload);
      return;
    }
   int sr = IvyXml.getAttrInt(te,"stop_reason");
   DebugReason reason = DebugReason.UNSPECIFIED;
   switch (sr) {
      case CMD_STEP_OVER :
      case CMD_STEP_INTO :
      case CMD_STEP_RETURN :
      case CMD_RUN_TO_LINE :
      case CMD_SET_NEXT_STATEMENT :
         reason = DebugReason.STEP_END;
         break;
      case CMD_THREAD_SUSPEND :
         reason = DebugReason.CLIENT_REQUEST;
         break;
      case CMD_SET_BREAK :
         reason = DebugReason.BREAKPOINT; 
         break;
      default :
         PybaseMain.logE("Unexpected reason for suspension: " + sr);
         reason = DebugReason.UNSPECIFIED;
         break;
    }

   if (t != null) {
      modification_checker.onlyLeaveThreads(thread_data); 
      List<PybaseDebugStackFrame> frms = new ArrayList<PybaseDebugStackFrame>();
      for (Element fe : IvyXml.children(te,"frame")) {
         String fid = IvyXml.getAttrString(fe,"id");
         String fnm = IvyXml.getAttrString(fe,"name");
         String fil = IvyXml.getAttrString(fe,"file");
         int lno = IvyXml.getAttrInt(fe,"line");
         File file = null;
         if (fil != null) {
            try {
               fil = URLDecoder.decode(fil,"UTF-8");
             }
            catch (UnsupportedEncodingException ex) { }
            file = new File(fil);
            if (file.exists()) file = file.getAbsoluteFile();
          }
         PybaseDebugStackFrame sf = t.findStackFrameByID(fid);
         if (sf == null) {
            sf = new PybaseDebugStackFrame(t,fid,fnm,file,lno,this);
          }
         else {
            sf.setName(fnm);
            sf.setFile(file);
            sf.setLine(lno);
          }
         frms.add(sf);
       }
      t.setSuspended(true,frms);
      System.err.println("SUSPEND EVENT " + reason);
      // fireEvent(new DebugEvent(t, DebugEvent.SUSPEND, reason)); 
    }
}



public static String [] getThreadIdAndReason(String payload) throws PybaseException
{
   String [] split = payload.trim().split("\t");
   if (split.length != 2) {
      String msg = "Unexpected threadRun payload " + payload + "(unable to match)";
      throw new PybaseException(msg);
    }

   return split;
}



private void processThreadRun(String payload)
{
   try {
      String [] threadIdAndReason = getThreadIdAndReason(payload);
      DebugReason resumeReason = DebugReason.UNSPECIFIED;
      try {
	 int raw_reason = Integer.parseInt(threadIdAndReason[1]);
	 switch (raw_reason) {
	    case CMD_STEP_OVER :
	       resumeReason = DebugReason.STEP_OVER;
	       break;
	    case CMD_STEP_RETURN :
	       resumeReason = DebugReason.STEP_RETURN;
	       break;
	    case CMD_STEP_INTO :
	       resumeReason = DebugReason.STEP_INTO;
	       break;
	    case CMD_RUN_TO_LINE :
	       resumeReason = DebugReason.UNSPECIFIED;
	       break;
	    case CMD_SET_NEXT_STATEMENT :
	       resumeReason = DebugReason.UNSPECIFIED;
	       break;
	    case CMD_THREAD_RUN :
	       resumeReason = DebugReason.CLIENT_REQUEST;
	       break;
	    default :
	       PybaseMain.logE("Unexpected resume reason code " + resumeReason);
	       resumeReason = DebugReason.UNSPECIFIED;
	    }
       }
      catch (NumberFormatException e) {
	 // expected, when pydevd reports "None"
	 resumeReason = DebugReason.UNSPECIFIED;
       }
	
      String threadID = threadIdAndReason[0];
      PybaseDebugThread t = findThreadById(threadID);
      if (t != null) {
	 t.setSuspended(false, null);
	 // fireEvent(new DebugEvent(t, DebugEvent.RESUME, resumeReason));
       }
      else {
	 PybaseMain.logE("Unable to find thread " + threadID);
       }
    }
   catch (Exception e1) {
      PybaseMain.logE("Problem processing thread run",e1);
    }
}




/********************************************************************************/
/*										*/
/*	Console methods 							*/
/*										*/
/********************************************************************************/

public void addConsoleInputListener()
{
}




/********************************************************************************/
/*										*/
/*	DebugReader implementation						*/
/*										*/
/********************************************************************************/

private static class DebugReader extends Thread {

    private Socket read_socket;
    private volatile boolean is_done;
    private Map<Integer,PybaseDebugCommand> response_queue;
    private BufferedReader in_reader;
    private PybaseDebugTarget remote_target;

    DebugReader(Socket s,PybaseDebugTarget r) throws IOException {
       super("PybaseDebugReader_" + s.toString());
       remote_target = r;
       read_socket = s;
       is_done = false;
       response_queue = new HashMap<Integer,PybaseDebugCommand>();
       InputStream sin = read_socket.getInputStream();
       in_reader = new BufferedReader(new InputStreamReader(sin));
     }

    void done() 			{ is_done = true; }

    void addToResponseQueue(PybaseDebugCommand cmd) {
       int sequence = cmd.getSequence();
       synchronized (response_queue) {
	  response_queue.put(new Integer(sequence),cmd);
	}
     }

    private void processCommand(String cmdline) {
       try {
          String[] cmdparsed = cmdline.split("\t", 3);
          int cmdcode = Integer.parseInt(cmdparsed[0]);
          int seqcode = Integer.parseInt(cmdparsed[1]);
          String payload = URLDecoder.decode(cmdparsed[2], "UTF-8");
    
          PybaseDebugCommand cmd;
          synchronized (response_queue) {
             cmd = response_queue.remove(new Integer(seqcode));
           }
    
          if (cmd == null) {
             if (remote_target != null) {
        	remote_target.processCommand(cmdparsed[0],cmdparsed[1],payload);
              }
             else {
        	PybaseMain.logE("Debug error: command received no target");
              }
           }
          else {
             cmd.processResponse(cmdcode,payload);
           }
        }
       catch (Exception e) {
          PybaseMain.logE("Error processing debug command",e);
          throw new RuntimeException(e);
        }
     }

    @Override public void run() {
       while (!is_done) {
	  try {
	     String cmdline = in_reader.readLine();
	     if (cmdline == null) {
		is_done = true;
		break;
	      }
	     else if(cmdline.trim().length() > 0) {
		processCommand(cmdline);
	      }
	   }
	  catch (IOException e) {
	     is_done = true;
	   }
	  // there was a 50ms delay here.  why?
	}

       if (is_done || read_socket == null || !read_socket.isConnected() ) {
	  PybaseDebugTarget target = remote_target;
	  if (target != null) {
	     target.terminate();
	   }
	  is_done = true;
	}
     }

}	// end of inner class DebugReader




/********************************************************************************/
/*										*/
/*	DebugWriter implementation						*/
/*										*/
/********************************************************************************/

private static class DebugWriter extends Thread {

   private Socket write_socket;
   private List<PybaseDebugCommand> cmd_queue;
   private OutputStreamWriter out_writer;
   private volatile boolean is_done;

   DebugWriter(Socket s) throws IOException {
      write_socket = s;
      cmd_queue = new ArrayList<PybaseDebugCommand>();
      out_writer = new OutputStreamWriter(s.getOutputStream());
      is_done = false;
    }

   void postCommand(PybaseDebugCommand cmd) {
      synchronized (cmd_queue) {
	 cmd_queue.add(cmd);
	 cmd_queue.notifyAll();
       }
    }

   public void done() {
      synchronized (cmd_queue) {
	 is_done = true;
	 cmd_queue.notifyAll();
       }
    }

   @Override public void run() {
      while (!is_done) {
	 PybaseDebugCommand cmd = null;
	 synchronized (cmd_queue) {
	    while (cmd_queue.size() == 0 && !is_done) {
	       try {
		  cmd_queue.wait();
		}
	       catch (InterruptedException e) { }
	     }
	    cmd = cmd_queue.remove(0);
	  }

	 try {
	    if (cmd != null) {
	       cmd.aboutToSend();
	       out_writer.write(cmd.getOutgoing());
	       out_writer.write("\n");
	       out_writer.flush();
	     }
	  }
	 catch (IOException e1) {
	    is_done = true;
	  }
	 if ((write_socket == null) || !write_socket.isConnected()) {
	    is_done = true;
	  }
       }
    }

}	// end of inner class DebugWriter




}	// end of class PybaseDebugTarget




/* end of PybaseDebugTarget.java */
