/********************************************************************************/
/*										*/
/*		BumpProblemSet.java						*/
/*										*/
/*	BUblles Mint Partnership problem set maintainer 			*/
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


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.*;


class BumpProblemSet implements BumpConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BumpProblemImpl>	current_problems;
private SwingEventListenerList<BumpProblemHandler> handler_set;
private Map<BumpProblemHandler,File>	problem_handlers;
private Map<String,Set<BumpProblemImpl>> private_problems;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpProblemSet()
{
   current_problems = new HashMap<String,BumpProblemImpl>();
   problem_handlers = new HashMap<BumpProblemHandler,File>();
   handler_set = new SwingEventListenerList<BumpProblemHandler>(BumpProblemHandler.class);
   private_problems = new HashMap<String,Set<BumpProblemImpl>>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addProblemHandler(File f,BumpProblemHandler ph)
{
   handler_set.add(ph);
   synchronized (problem_handlers) {
      problem_handlers.put(ph,f);
    }
}



synchronized void removeProblemHandler(BumpProblemHandler ph)
{
   handler_set.remove(ph);
   synchronized (problem_handlers) {
      problem_handlers.remove(ph);
    }
}



/********************************************************************************/
/*										*/
/*	Set access methods							*/
/*										*/
/********************************************************************************/

List<BumpProblem> getProblems(File f)
{
   List<BumpProblem> rslt = new ArrayList<BumpProblem>();

   synchronized (current_problems) {
      for (BumpProblemImpl bp : current_problems.values()) {
	 if (fileMatch(f,bp)) {
	    rslt.add(bp);
	  }
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Maintenance methods							*/
/*										*/
/********************************************************************************/

void handleErrors(String proj,File forfile,int eid,Element ep)
{
   Set<BumpProblemImpl> found = new HashSet<BumpProblemImpl>();
   List<BumpProblemImpl> added = null;
   List<BumpProblemImpl> deled = null;

   // first add new problems and build set of all problems provided

   synchronized (current_problems) {
      for (Element e : IvyXml.children(ep,"PROBLEM")) {
	 String pid = getProblemId(e);
	 BumpProblemImpl bp = current_problems.get(pid);
	 if (bp == null) {
	    bp = new BumpProblemImpl(e,pid,eid,proj);
	    current_problems.put(pid,bp);
	    if (added == null) added = new ArrayList<BumpProblemImpl>();
	    added.add(bp);
	  }
	 else {
	    bp.setEditId(eid);
	    bp.update(e);
	  }

	 found.add(bp);
       }

      // next remove any problems that seem to have disappeared
      if (forfile != null) {
	 for (Iterator<BumpProblemImpl> it = current_problems.values().iterator(); it.hasNext(); ) {
	    BumpProblemImpl bp = it.next();
	    if (found.contains(bp)) continue;
	    if (!fileMatch(forfile,bp)) continue;
	    // if (bp.getErrorType() == BumpErrorType.NOTICE) continue; // notes not returned on recompile -- seems fixed
	    if (deled == null) deled = new ArrayList<BumpProblemImpl>();
	    deled.add(bp);
	    it.remove();
	 }
      }
    }

   if (added == null && deled == null) return;

   for (BumpProblemHandler bph : handler_set) {
      File f;
      synchronized (problem_handlers) {
	 f = problem_handlers.get(bph);
	 if (f == null && !problem_handlers.containsKey(bph)) continue;
       }
      int ct = 0;
      if (deled != null) {
	 for (BumpProblemImpl bp : deled) {
	    if (fileMatch(f,bp)) {
	       bph.handleProblemRemoved(bp);
	       ++ct;
	     }
	  }
       }
      if (added != null) {
	 for (BumpProblemImpl bp : added) {
	    if (fileMatch(f,bp)) {
	       bph.handleProblemAdded(bp);
	       ++ct;
	     }
	  }
       }
      if (ct > 0) bph.handleProblemsDone();
    }
}




void clearProblems()
{
   List<BumpProblemImpl> clear;
   synchronized (current_problems) {
      clear = new ArrayList<BumpProblemImpl>(current_problems.values());
      current_problems.clear();
    }

   if (clear.size() > 0) {
      for (BumpProblemHandler bph : handler_set) {
	 for (BumpProblemImpl bp : clear) {
	    bph.handleProblemRemoved(bp);
	  }
	 bph.handleClearProblems();
	 bph.handleProblemsDone();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Private buffer problem management					*/
/*										*/
/********************************************************************************/

void clearPrivateProblems(String pid)
{
   synchronized (private_problems) {
      private_problems.remove(pid);
    }
}


void handlePrivateErrors(String proj,File forfile,String privid,Element ep)
{
   Set<BumpProblemImpl> probs = new HashSet<BumpProblemImpl>();
   for (Element e : IvyXml.children(ep,"PROBLEM")) {
      String pid = getProblemId(e);
      BumpProblemImpl bp = new BumpProblemImpl(e,pid,-1,proj);
      probs.add(bp);
    }

   synchronized (private_problems) {
      private_problems.put(privid,probs);
      private_problems.notifyAll();
    }
}


Collection<BumpProblem> getPrivateErrors(String privid)
{
   synchronized (private_problems) {
      if (private_problems.get(privid) == null) {
	 try {
	    private_problems.wait(60000);
	  }
	 catch (InterruptedException e) { }
       }
      if (private_problems.get(privid) == null) return null;
      return new ArrayList<BumpProblem>(private_problems.get(privid));
    }
}

/********************************************************************************/
/*										*/
/*	Problem handling methods						*/
/*										*/
/********************************************************************************/

private boolean fileMatch(File forfile,BumpProblemImpl bp)
{
   if (forfile == null) return true;
   return forfile.equals(bp.getFile());
}



private String getProblemId(Element e)
{
   int lno = IvyXml.getAttrInt(e,"LINE",0);
   int sloc = IvyXml.getAttrInt(e,"START",0);
   int mid = IvyXml.getAttrInt(e,"MSGID",0);
   String fnm = IvyXml.getTextElement(e,"FILE");

   int id = mid + lno*2 + sloc*3;
   if (fnm != null) id ^= fnm.hashCode();

   return Integer.toString(id);
}



}	// end of class BumpProblemSet




/* end of BumpProblemSet.java */

