/********************************************************************************/
/*										*/
/*		BddtConsoleController.java					*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool console controller		*/
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

import edu.brown.cs.bubbles.board.BoardAttributes;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProcess;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunEvent;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunModel;

import javax.swing.text.*;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;


class BddtConsoleController implements BddtConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BddtLaunchControl,ConsoleDocument> launch_consoles;
private Map<String,ConsoleDocument> process_consoles;
private AttributeSet stdout_attrs;
private AttributeSet stderr_attrs;
private LinkedList<ConsoleMessage> message_queue;




/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

BddtConsoleController()
{
   launch_consoles = new HashMap<BddtLaunchControl,ConsoleDocument>();
   process_consoles = new HashMap<String,ConsoleDocument>();
   message_queue = new LinkedList<ConsoleMessage>();

   ConsoleThread ct = new ConsoleThread();
   ct.start();

   BumpClient bc = BumpClient.getBump();
   BumpRunModel rm = bc.getRunModel();

   rm.addRunEventHandler(new ConsoleHandler());

   BoardAttributes atts = new BoardAttributes("Bddt");
   stdout_attrs = atts.getAttributes("StdOut");
   stderr_attrs = atts.getAttributes("StdErr");
   // stdin_attrs = atts.getAttributes("StdIn");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setupConsole(BddtLaunchControl blc)
{
   getDocument(blc);
}




/********************************************************************************/
/*										*/
/*	Edit methods								*/
/*										*/
/********************************************************************************/

private void queueConsoleMessage(BumpProcess bp,boolean err,boolean eof,String msg)
{
   ConsoleMessage last = null;
   synchronized (message_queue) {
      last = message_queue.peekLast();
      if (last != null && last.getProcess() == bp && last.isStderr() == err &&
	    last.isEof() == eof) {
	 last.merge(msg);
	 return;
       }
      message_queue.add(new ConsoleMessage(bp,msg,err,eof));
      if (last == null) message_queue.notifyAll();
    }
}




private void processConsoleMessage(ConsoleMessage msg)
{
   BumpProcess bp = msg.getProcess();

   if (msg.isEof()) {
      synchronized (launch_consoles) {
	 if (bp != null)  {
	    addText(bp,false,"\n [ Process Terminated ]\n");
	    process_consoles.remove(bp.getId());
	  }
       }
    }
   else {
      addText(bp,msg.isStderr(),msg.getText());
    }
}




private void addText(BumpProcess process,boolean err,String message)
{
   String pid = (process == null ? "*" : process.getId());

   ConsoleDocument doc = getDocument(pid,false);

   if (doc != null && message != null) {
      doc.addText(err,message);
    }
}



void clearConsole(BumpProcess bp)
{
   String pid = (bp == null ? "*" : bp.getId());
   ConsoleDocument doc = getDocument(pid,false);
   if (doc != null) {
      doc.clear();
      // Consider outputing a header line here
    }
   else {
      BoardLog.logD("BDDT","No console found for process " + pid);
   }
}


private ConsoleDocument getDocument(BddtLaunchControl ctrl)
{
   synchronized (launch_consoles) {
      ConsoleDocument doc = launch_consoles.get(ctrl);
      if (doc == null) {
	 doc = new ConsoleDocument();
	 launch_consoles.put(ctrl,doc);
       }
      return doc;
    }
}




private ConsoleDocument getDocument(String pid,boolean force)
{
   synchronized (launch_consoles) {
      ConsoleDocument doc = process_consoles.get(pid);
      if (doc != null) return doc;
      for (Map.Entry<BddtLaunchControl,ConsoleDocument> ent : launch_consoles.entrySet()) {
	 BddtLaunchControl blc = ent.getKey();
	 BumpProcess bp = blc.getProcess();
	 if (bp != null && bp.getId().equals(pid)) {
	    doc = ent.getValue();
	    process_consoles.put(pid,doc);
	    return doc;
	  }
       }
      if (force) {
	 doc = new ConsoleDocument();
	 process_consoles.put(pid,doc);
       }
      return doc;
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BddtConsoleBubble createConsole(BddtLaunchControl blc)
{
   ConsoleDocument doc = getDocument(blc);

   BddtConsoleBubble b = new BddtConsoleBubble(blc,doc);

   return b;
}




BddtConsoleBubble createConsole(BumpProcess bp)
{
   ConsoleDocument doc = getDocument(bp.getId(),true);

   BddtConsoleBubble b = new BddtConsoleBubble(bp,doc);

   return b;
}




/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private class ConsoleHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)	{ }

   @Override public void handleProcessEvent(BumpRunEvent evt) {
//	switch (evt.getEventType()) {
//	 case PROCESS_REMOVE :
//	    BumpProcess bp = evt.getProcess();
//	    synchronized (launch_consoles) {
//	       process_consoles.remove(bp.getId());
//	     }
//	    break;
//	 }
    }

   @Override public void handleThreadEvent(BumpRunEvent evt)	{ }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,boolean eof,String msg) {
      queueConsoleMessage(bp,err,eof,msg);

      // if (eof) {
	 // synchronized (launch_consoles) {
	    // if (bp != null) {
	       // addText(bp,false,"\n [ Process Terminated ]\n");
	       // process_consoles.remove(bp.getId());
	    // }
	 // }
       // }
      // else {
	 // addText(bp,err,msg);
       // }
   }




}	// end of inner class ConsoleHandler




/********************************************************************************/
/*										*/
/*	ConsoleBuffer implementation						*/
/*										*/
/********************************************************************************/

private class ConsoleDocument extends DefaultStyledDocument {

   private int		line_count;
   private int		max_length;
   private int		line_length;

   ConsoleDocument() {
      line_count = 0;
      max_length = 0;
      line_length = 0;
    }

   synchronized void clear() {
      writeLock();
      try {
	 line_count = 0;
	 max_length = 0;
	 line_length = 0;
	 remove(0,getLength());
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BDDT","Problem clearing console",e);
       }
      finally { writeUnlock(); }
    }

   synchronized void addText(boolean err,String txt) {
      int lns = countLines(txt);
      writeLock();
      try {
	 while (line_count+lns >= BDDT_CONSOLE_MAX_LINES) {
	    int lidx = -1;
	    try {
	       Segment s = new Segment();
	       int ln = max_length+2;
	       int dln = getLength();
	       if (ln > dln) ln = dln;
	       getText(0,ln,s);

	       int delct = 0;
	       int idx = -1;
	       for (int i = lidx+1; i < s.length(); ++i) {
		  if (s.charAt(i) == '\n') {
		     idx = i;
		     ++delct;
		     if (line_count + lns - delct < BDDT_CONSOLE_MAX_LINES) break;
		   }
		}
	       if (idx >= 0) {
		  remove(0,idx+1);
		  line_count -= delct;
		}
	       else break;
	     }
	    catch (BadLocationException e) {
	       BoardLog.logE("BDDT","Problem remove line from console",e);
	     }
	  }

	 try {
	    insertString(getLength(),txt,(err ? stderr_attrs : stdout_attrs));
	    line_count += lns;
	    if (txt.length() > max_length) max_length = txt.length();
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BDDT","Problem adding line to console",e);
	  }
       }
      finally { writeUnlock(); }
    }

   private int countLines(String txt) {
      int ct = 0;
      int lidx = 0;
      for (int idx = txt.indexOf("\n"); idx >= 0; idx = txt.indexOf("\n",idx+1)) {
	 line_length += idx-lidx;
	 if (line_length > max_length) max_length = line_length;
	 ++ct;
	 line_length = 0;
	 lidx = idx;
       }
      line_length = txt.length() - lidx;
      if (line_length > max_length) max_length = line_length;

      return ct;
    }


  // TODO: method to accept input and display in a different style

}	// end of inner class ConsoleDocument



private static class ConsoleMessage {

   private BumpProcess for_process;
   private String message_text;
   private boolean is_stderr;
   private boolean is_eof;

   ConsoleMessage(BumpProcess bp,String text,boolean stderr,boolean eof) {
      for_process = bp;
      message_text = text;
      is_stderr = stderr;
      is_eof = eof;
    }

   boolean isStderr()			{ return is_stderr; }
   boolean isEof()			{ return is_eof; }
   String getText()			{ return message_text; }
   void merge(String t) 		{ message_text += t; }
   BumpProcess getProcess()		{ return for_process; }

}	// end of inner class ConsoleMessage



/********************************************************************************/
/*										*/
/*	Thread to handle console updates					*/
/*										*/
/********************************************************************************/

private class ConsoleThread extends Thread {

   ConsoleThread() {
      super("BddtConsoleControllerThread");
    }

   @Override public void run() {
      for ( ; ; ) {
	 ConsoleMessage msg;
	 synchronized (message_queue) {
	    while (message_queue.isEmpty()) {
	       try {
		  message_queue.wait(10000);
		}
	       catch (InterruptedException e) { }
	     }
	    msg = message_queue.removeFirst();
	  }
	 processConsoleMessage(msg);
       }
    }

}	// end of inner class ConsoleThread



}	// end of class BddtConsoleController




/* end of BddtConsoleController.java */
