/********************************************************************************/
/*										*/
/*		BaleCorrector.java						*/
/*										*/
/*	Class to handle spelling corrections					*/
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



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;



class BaleCorrector implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleFragmentEditor	for_editor;
private BaleDocument		for_document;
private DocHandler		event_handler;
private ProblemHandler		problem_handler;

private int			start_offset;
private int			end_offset;
private long			start_time;
private int			caret_position;
private Set<BumpProblem>	active_problems;

private static Contexter	context_handler = null;
private static Map<BaleCorrector,Boolean> all_correctors;

private static int	MAX_REGION_SIZE = 50;

static {
   all_correctors = new WeakHashMap<BaleCorrector,Boolean>();
   context_handler = new Contexter();
   BaleFactory.getFactory().addContextListener(context_handler);
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCorrector(BaleFragmentEditor ed,boolean auto)
{
   for_editor = ed;
   for_document = ed.getDocument();
   event_handler = null;
   problem_handler = null;

   start_offset = -1;
   end_offset = -1;
   start_time = 0;
   caret_position = -1;
   active_problems = new ConcurrentSkipListSet<BumpProblem>(new ProblemComparator());

   if (auto) {
      event_handler = new DocHandler();
      problem_handler = new ProblemHandler();
      for_document.addDocumentListener(event_handler);
      for_editor.getEditor().addCaretListener(event_handler);
      BumpClient.getBump().addProblemHandler(for_document.getFile(),problem_handler);
    }

   all_correctors.put(this,Boolean.TRUE);
}




/********************************************************************************/
/*										*/
/*	Region management							*/
/*										*/
/********************************************************************************/

private void clearRegion()
{
   start_offset = -1;
   end_offset = -1;
   start_time = 0;
   caret_position = -1;
   active_problems.clear();
}


private void handleTyped(int off,int len)
{
   if (!checkFocus()) return;

   if (start_offset < 0) {
      int lno = for_document.findLineNumber(off);
      start_offset = for_document.findLineOffset(lno);
      start_time = System.currentTimeMillis();
    }
   caret_position = off+len;
   end_offset = Math.max(end_offset,caret_position);

   while (end_offset - start_offset > MAX_REGION_SIZE) {
      int lno = for_document.findLineNumber(start_offset);
      int npos = for_document.findLineOffset(lno+1);
      if (npos < end_offset) {
	 start_offset = npos;
       }
      else break;
    }
}

private void handleBackspace(int off)
{
   if (start_offset < 0) return;
   if (!checkFocus()) return;

   caret_position = off;
   start_offset = Math.min(start_offset,caret_position);
}



private void addProblem(BumpProblem bp)
{
   // must be an error, not a warning
   if (bp.getErrorType() != BumpErrorType.ERROR) return;

   int soff = for_document.mapOffsetToJava(bp.getStart());
   if (start_offset >= 0 && soff >= start_offset && soff < end_offset) {
      BoardLog.logD("BALE","SPELL: consider problem " + bp.getMessage());
      active_problems.add(bp);
    }
}


private void removeProblem(BumpProblem bp)
{
   active_problems.remove(bp);
}


private boolean checkFocus()
{
   BudaBubble bbl = BudaRoot.findBudaBubble(for_editor);
   if (bbl == null) {
      clearRegion();
      return false;
    }
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
   if (bba.getFocusBubble() == bbl) return true;
   clearRegion();
   return false;
}


/********************************************************************************/
/*										*/
/*	Find something to fix							*/
/*										*/
/********************************************************************************/

private void checkForElementToFix()
{
   List<BumpProblem> totry = new ArrayList<BumpProblem>();

   if (start_offset < 0) return;
   if (active_problems.isEmpty()) return;
   for (Iterator<BumpProblem> it = active_problems.iterator(); it.hasNext(); ) {
      BumpProblem bp = it.next();
      int soff = for_document.mapOffsetToJava(bp.getStart());
      if (soff < start_offset) {
	 it.remove();
	 continue;
       }
      totry.add(bp);
    }

   if (totry.isEmpty()) return;

   for (BumpProblem bp : totry) {
      String txt = tryProblem(bp);
      if (txt != null) {
	 BoardLog.logD("BALE","SPELL: HANDLE PROBLEM " + bp.getMessage());
	 int minsize = BALE_PROPERTIES.getInt("Bale.correct.spelling.edits",2);
	 SpellFixer sf = new SpellFixer(bp,txt,minsize);
	 BoardThreadPool.start(sf);
	 break;
       }
    }
}



private String tryProblem(BumpProblem bp)
{
   BoardLog.logD("BALE","SPELL: try problem " + bp.getMessage());
   int soff = for_document.mapOffsetToJava(bp.getStart());
   int eoff = for_document.mapOffsetToJava(bp.getEnd());
   BaleElement elt = for_document.getCharacterElement(soff);
   // need to have an identifier to correct
   if (!elt.isIdentifier()) return null;
   // can't be working on the identifier at this point
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff) return null;
   if (end_offset > 0 && eloff + 1 >= end_offset) return null;
   //  if (!elt.isUndefined()) continue;

   String txt = null;
   try {
      txt = for_document.getText(soff,eoff-soff+1);
    }
   catch (BadLocationException e) { }
   if (txt == null) return null;

   return txt;
}



/********************************************************************************/
/*										*/
/*	String methods								*/
/*										*/
/********************************************************************************/

private static int stringDiff(CharSequence s,CharSequence t)
{
   int n = s.length();
   int m = t.length();
   if (n == 0) return m;
   if (m == 0) return n;

   int [][] d = new int[n+1][m+1];
   for (int i = 0; i <= n; i++) d[i][0] = i;
   for (int j = 0; j <= m; j++) d[0][j] = j;

   for (int i = 1; i <= n; ++i) {
      char s_i = s.charAt(i-1);
      for (int j = 1; j <= m; ++j) {
	 char t_j = t.charAt (j - 1);
	 int cost = (s_i == t_j ? 0 : 1);
	 d[i][j] = min3(d[i-1][j]+1,d[i][j-1]+1,d[i-1][j-1]+cost);
       }
    }

   return d[n][m];
}



private static int min3(int a, int b, int c)
{
   if (b < a) a = b;
   if (c < a) a = c;
   return a;
}



/********************************************************************************/
/*										*/
/*	Error methods								*/
/*										*/
/********************************************************************************/

private static boolean checkProblemPresent(BumpProblem prob,Collection<BumpProblem> bpl)
{
   for (BumpProblem bp : bpl) {
      if (!bp.getProblemId().equals(prob.getProblemId())) continue;
      if (bp.getStart() != prob.getStart()) continue;
      if (bp.getEnd() != prob.getEnd()) continue;
      if (!bp.getFile().equals(prob.getFile())) continue;
      return true;
    }

   return false;
}



private static boolean checkAnyProblemPresent(BumpProblem prob,Collection<BumpProblem> bpl)
{
   for (BumpProblem bp : bpl) {
      if (!bp.getFile().equals(prob.getFile())) continue;
      if (bp.getErrorType() != BumpErrorType.ERROR) continue;
      if (bp.getStart() < prob.getEnd() && bp.getEnd() > prob.getStart()) return true;
    }

   return false;
}




private static int getErrorCount(Collection<BumpProblem> bpl)
{
   int ct = 0;

   for (BumpProblem bp : bpl) {
      if (bp.getErrorType() == BumpErrorType.ERROR) ++ct;
    }

   return ct;
}



/********************************************************************************/
/*										*/
/*	Class to find a good fix						*/
/*										*/
/********************************************************************************/

private class SpellFixer implements Runnable {

   private BumpProblem for_problem;
   private String for_identifier;
   private long initial_time;
   private int min_size;

   SpellFixer(BumpProblem bp,String txt,int min) {
      for_problem = bp;
      for_identifier = txt;
      initial_time = start_time;
      min_size = min;
      if (min <= 0) min_size = 2;
    }

   @Override public void run() {
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      Set<SpellFix> totry = new TreeSet<SpellFix>();
      int minsize = Math.min(min_size, for_identifier.length()-1);
      minsize = Math.min(minsize,(for_identifier.length()+2)/3);

      BumpClient bc = BumpClient.getBump();
      Collection<BumpCompletion> cmps = bc.getCompletions(proj,file,-1,for_problem.getStart());
      int ct = 0;
      for (BumpCompletion bcm : cmps) {
	 String txt = bcm.getCompletion();
	 if (txt == null || txt.length() == 0) continue;
	 int d = stringDiff(for_identifier,txt);
	 ++ct;
	 if (d <= minsize && d > 0) {
	    BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + txt);
	    SpellFix sf = new SpellFix(txt,d);
	    totry.add(sf);
	  }
       }
      if (ct == 0) {
	 cmps = bc.getCompletions(proj,file,-1,for_problem.getStart()+1);
	 for (BumpCompletion bcm : cmps) {
	    String txt = bcm.getCompletion();
	    if (txt == null || txt.length() == 0) continue;
	    int d = stringDiff(for_identifier,txt);
	    ++ct;
	    if (d <= minsize && d > 0) {
	       BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + txt);
	       SpellFix sf = new SpellFix(txt,d);
	       totry.add(sf);
	     }
	  }
       }

      String key = for_identifier;
      if (key.length() > 3) {
	 key = key.substring(0,3) + "*";
	 for (BumpLocation bl : bc.findTypes(proj,key)) {
	    String nm = bl.getSymbolName();
	    int idx = nm.lastIndexOf(".");
	    if (idx >= 0) nm = nm.substring(idx+1);
	    int d = stringDiff(for_identifier,nm);
	    if (d <= minsize && d > 0) {
	       BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + nm);
	       SpellFix sf = new SpellFix(nm,d);
	       totry.add(sf);
	     }
	   }
       }
      Collection<String> keys = BaleTokenizer.getKeywords(for_document.getLanguage());
      for (String s : keys) {
	 int d = stringDiff(for_identifier,s);
	 if (d <= minsize && d > 0) {
	    BoardLog.logD("BALE","SPELL: Consider replacing " + for_identifier + " WITH " + s);
	    SpellFix sf = new SpellFix(s,d);
	    totry.add(sf);
	  }
       }

      // remove problematic cases
      for (Iterator<SpellFix> it = totry.iterator(); it.hasNext(); ) {
	 SpellFix sf = it.next();
	 if (for_identifier.equals("put") && sf.getText().equals("get")) it.remove();
	 if (for_identifier.startsWith("set") && sf.getText().startsWith("get")) it.remove();
	 if (for_identifier.equals("List") && sf.getText().equals("int")) it.remove();
	 if (for_identifier.equals("is") && sf.getText().equals("if")) it.remove();
	 if (for_identifier.equals("add") && sf.getText().equals("do")) it.remove();
       }

      if (totry.size() == 0) {
	 BoardLog.logD("BALE", "SPELL: No spelling correction found");
	 return;
       }

      String pid = bc.createPrivateBuffer(proj,filename,null);
      if (pid == null) return;
      BoardLog.logD("BALE","SPELL: using private buffer " + pid);
      SpellFix usefix = null;

      try {
	 Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
	 if (probs == null) {
	    BoardLog.logE("BALE","SPELL: Problem getting errors for " + pid);
	    return;
	  }
	 int probct = getErrorCount(probs);
	 if (!checkProblemPresent(for_problem,probs)) {
	    BoardLog.logD("BALE","SPELL: Problem went away");
	    return;
	  }
	 int soff = for_problem.getStart();
	 int eoff = for_problem.getEnd()+1;

	 for (SpellFix sf : totry) {
	    bc.beginPrivateEdit(filename,pid);
	    BoardLog.logD("BALE","SPELL: Try replacing " + for_identifier + " WITH " + sf.getText());
	    bc.editPrivateFile(proj,file,pid,soff,eoff,sf.getText());
	    probs = bc.getPrivateProblems(filename,pid);
	    bc.beginPrivateEdit(filename,pid);		// undo and wait
	    bc.editPrivateFile(proj,file,pid,soff,soff+sf.getText().length(),for_identifier);
	    bc.getPrivateProblems(filename,pid);

	    if (probs == null || getErrorCount(probs) >= probct) continue;
	    if (checkAnyProblemPresent(for_problem,probs)) continue;
	    if (usefix != null) {
	       if (sf.getEditCount() > usefix.getEditCount()) break;
	       // multiple edits of same length seem to work out -- ignore.
	       return;
	     }
	    else usefix = sf;
	  }
       }
      finally {
	 bc.removePrivateBuffer(proj,filename,pid);
       }

      if (usefix == null) return;
      if (start_time != initial_time) return;
      BoardLog.logD("BALE","SPELL: DO replace " + for_identifier + " WITH " + usefix.getText());
      BoardMetrics.noteCommand("BALE","SPELLFIX");
      SpellDoer sd = new SpellDoer(for_problem,usefix,initial_time);
      SwingUtilities.invokeLater(sd);
    }

}	// end of inner class SpellFixer



/********************************************************************************/
/*										*/
/*	Class to do a fix							*/
/*										*/
/********************************************************************************/

private class SpellDoer implements Runnable {

   private BumpProblem for_problem;
   private SpellFix for_fix;
   private long initial_time;

   SpellDoer(BumpProblem bp,SpellFix fix,long time0) {
      for_problem = bp;
      for_fix = fix;
      initial_time = time0;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (start_time != initial_time) return;

      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      int eoff = for_document.mapOffsetToJava(for_problem.getEnd());
      String txt = for_fix.getText();
      try {
	 for_document.replace(soff,eoff-soff+1,txt,null);
       }
      catch (BadLocationException e) { }
    }

}	// end of inner class SpellDoer




/********************************************************************************/
/*										*/
/*	Hold a potential quick fix						*/
/*										*/
/********************************************************************************/

private static class SpellFix implements Comparable<SpellFix> {

   private String new_text;
   private int text_delta;

   SpellFix(String txt,int d) {
      new_text = txt;
      text_delta = d;
    }

   String getText()			{ return new_text; }
   int getEditCount()			{ return text_delta; }

   @Override public int compareTo(SpellFix sf) {
      int d = getEditCount() - sf.getEditCount();
      if (d < 0) return -1;
      if (d > 0) return 1;
      return new_text.compareTo(sf.new_text);
    }
}




/********************************************************************************/
/*										*/
/*	Handle editor events							*/
/*										*/
/********************************************************************************/

private class DocHandler implements DocumentListener, CaretListener {

   @Override public void changedUpdate(DocumentEvent e) {
       int len = e.getLength();
       int dlen = e.getDocument().getLength();
       if (len != dlen) {
	  BoardLog.logD("BALE","SPELL: Clear for changed update");
	  clearRegion();
	}
    }

   @Override public void insertUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      if (len == 0) return;
      else if (len == 1) {
	 handleTyped(off,1);
       }
      else {
	 try {
	    String s = for_document.getText(off,len).trim();
	    if (s.equals("") || s.equals("}")) {
	       handleTyped(off,len);
	       return;
	     }
	  }
	 catch (BadLocationException ex) { }
	 BoardLog.logD("BALE","SPELL: Clear for insert update");
	 clearRegion();
       }
    }

   @Override public void removeUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      if (len == 1) {
	 handleBackspace(off);
       }
      else {
	 BoardLog.logD("BALE","SPELL: Clear for remove update");
	 clearRegion();
       }
    }

   @Override public void caretUpdate(CaretEvent e) {
      int off = e.getDot();
      if (off == caret_position) return;
      if (off >= start_offset && off <= end_offset) {
	 caret_position = off;
	 return;
       }
      BoardLog.logD("BALE","SPELL: Clear for caret update");
      clearRegion();
    }

}	// end of inner class DocHandler




/********************************************************************************/
/*										*/
/*	Handle Compilation Events						*/
/*										*/
/********************************************************************************/

private class ProblemHandler implements BumpConstants.BumpProblemHandler {

   @Override public void handleProblemAdded(BumpProblem bp) {
      addProblem(bp);
    }

   @Override public void handleProblemRemoved(BumpProblem bp) {
      removeProblem(bp);
    }

   @Override public void handleClearProblems() {
      active_problems.clear();
    }

   @Override public void handleProblemsDone() {
      SwingUtilities.invokeLater(new Checker());
    }

}	// end of inner class ProblemHandler



private class ProblemComparator implements Comparator<BumpProblem> {

   @Override public int compare(BumpProblem p1,BumpProblem p2) {
      int d = p1.getStart() - p2.getStart();
      if (d < 0) return -1;
      if (d > 0) return 1;
      return p1.getProblemId().compareTo(p2.getProblemId());
    }
}


private class Checker implements Runnable {

   @Override public void run() {
      checkForElementToFix();
   }

}	// end of inner class Checker




/********************************************************************************/
/*										*/
/*	Handle popup menu for spelling correction				*/
/*										*/
/********************************************************************************/

private void addPopupMenuItems(BaleContextConfig ctx,JPopupMenu menu)
{
   clearRegion();

   BaleDocument bd = (BaleDocument) ctx.getDocument();
   List<BumpProblem> probs = bd.getProblemsAtLocation(ctx.getOffset());
   if (probs == null) return;
   for (BumpProblem bp : probs) {
      String txt = tryProblem(bp);
      if (txt != null) {
	 menu.add(new SpellTryer(bp,txt));
	 break;
       }
    }
}


private class SpellTryer extends AbstractAction {

   private BumpProblem for_problem;
   private String for_text;

   private static final long serialVersionUID = 1;

   SpellTryer(BumpProblem bp,String txt) {
      super("Check Spelling of '" + txt + "'");
      for_problem = bp;
      for_text = txt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      int minsize = BALE_PROPERTIES.getInt("Bale.correct.spelling.user",5);
      SpellFixer sf = new SpellFixer(for_problem,for_text,minsize);
      sf.run();
    }

}	// end of inner class SpellTryer




private static class Contexter implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }


   @Override public void addPopupMenuItems(BaleContextConfig ctx,JPopupMenu menu) {
      for (BaleCorrector bc : all_correctors.keySet()) {
	 BudaBubble bbl = BudaRoot.findBudaBubble(bc.for_editor);
	 if (bbl == ctx.getEditor()) {
	    bc.addPopupMenuItems(ctx,menu);
	    break;
	  }
       }
    }

}



}	// end of class BaleCorrector




/* end of BaleCorrector.java */
