/********************************************************************************/
/*										*/
/*		BedrockQuickFix.java						*/
/*										*/
/*	Tie into Eclipse's Quick Fix mechanism from bubbles                     */
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


package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.xml.*;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.ui.text.java.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.compiler.*;

import org.w3c.dom.*;


class BedrockQuickFix implements BedrockConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin		our_plugin;

private List<IQuickFixProcessor> quick_fixes;
private List<IQuickAssistProcessor> quick_assists;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockQuickFix(BedrockPlugin bp)
{
   our_plugin = bp;
   quick_fixes = new ArrayList<IQuickFixProcessor>();
   quick_fixes.add(new org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor());
   quick_fixes.add(new org.eclipse.jdt.internal.ui.text.spelling.WordQuickFixProcessor());
   quick_assists = new ArrayList<IQuickAssistProcessor>();
   quick_assists.add(new org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor());
   quick_assists.add(new org.eclipse.jdt.internal.ui.text.correction.QuickTemplateProcessor());
}



/********************************************************************************/
/*										*/
/*	Action routines 							*/
/*										*/
/********************************************************************************/

void handleQuickFix(String bid,String proj,String file,int off,int len,List<Element> problems,
		       IvyXmlWriter xw)
	throws BedrockException
{
   CompilationUnit cu = our_plugin.getAST(bid,proj,file);
   ICompilationUnit icu = our_plugin.getCompilationUnit(proj,file);

   List<CategorizedProblem> probs = getProblems(cu,problems);
   if (probs == null || probs.size() == 0) return;

   FixContext fc = new FixContext(cu,icu,off,len);

   ProblemContext [] pcs = new ProblemContext[probs.size()];
   for (int i = 0; i < probs.size(); ++i) {
      pcs[i] = new ProblemContext(probs.get(i));
    }

   for (IQuickFixProcessor qp : quick_fixes) {
      try {
	 IJavaCompletionProposal [] props = qp.getCorrections(fc,pcs);
	 outputProposals(props,xw);
       }
      catch (CoreException e) { }
    }
   for (IQuickAssistProcessor qp : quick_assists) {
      try {
	 IJavaCompletionProposal [] props = qp.getAssists(fc,pcs);
	 outputProposals(props,xw);
       }
      catch (CoreException e) { }
    }
}



/********************************************************************************/
/*										*/
/*	Relevant problems							*/
/*										*/
/********************************************************************************/

private List<CategorizedProblem> getProblems(CompilationUnit cu,List<Element> xmls)
{
   IProblem [] probs = cu.getProblems();
   List<CategorizedProblem> pbs = new ArrayList<CategorizedProblem>();

   for (Element e : xmls) {
      int mid = IvyXml.getAttrInt(e,"MSGID",0);
      if (mid == 0) continue;
      int sln = IvyXml.getAttrInt(e,"START");
      if (sln < 0) continue;
      for (IProblem ip : probs) {
	 if (!(ip instanceof CategorizedProblem)) continue;
	 if (ip.getID() != mid) continue;
	 if (Math.abs(ip.getSourceStart() - sln) > 2) continue;
	 pbs.add((CategorizedProblem) ip);
       }
    }

   return pbs;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputProposals(IJavaCompletionProposal [] props,IvyXmlWriter xw)
{
   for (IJavaCompletionProposal p : props)
      outputProposal(p,xw);
}



private void outputProposal(IJavaCompletionProposal p,IvyXmlWriter xw)
{
   xw.begin("FIX");
   xw.field("TYPE",p.getClass().getName());
   xw.field("RELEVANCE",p.getRelevance());
   xw.field("DISPLAY",p.getDisplayString());
   xw.field("INFO",p.getAdditionalProposalInfo());
   xw.field("ID",System.identityHashCode(p));
   xw.end("FIX");
}			



/********************************************************************************/
/*										*/
/*	Context for doing the quick fix 					*/
/*										*/
/********************************************************************************/

private static class FixContext implements IInvocationContext {

   private CompilationUnit ast_root;
   private ICompilationUnit comp_unit;
   private int source_offset;
   private int source_length;
   private NodeFinder node_finder;

   FixContext(CompilationUnit cu,ICompilationUnit icu,int off,int len) {
      ast_root = cu;
      comp_unit = icu;
      source_offset = off;
      source_length = len;
      node_finder = new NodeFinder(ast_root,off,len);
    }

   @Override public CompilationUnit getASTRoot()		{ return ast_root; }

   @Override public ICompilationUnit getCompilationUnit()	{ return comp_unit; }

   @Override public int getSelectionLength()		{ return source_length; }
   @Override public int getSelectionOffset()		{ return source_offset; }

   @Override public ASTNode getCoveredNode()		{ return node_finder.getCoveredNode(); }
   @Override public ASTNode getCoveringNode()		{ return node_finder.getCoveringNode(); }

}	// end of inner class FixContext



/********************************************************************************/
/*										*/
/*	Problem context for quick fix						*/
/*										*/
/********************************************************************************/

private static class ProblemContext implements IProblemLocation {

   private CategorizedProblem for_problem;
   private Map<CompilationUnit,NodeFinder> finder_map;

   ProblemContext(CategorizedProblem ip) {
      for_problem = ip;
      finder_map = new HashMap<CompilationUnit,NodeFinder>();
    }

   @Override public ASTNode getCoveredNode(CompilationUnit cu) {
      NodeFinder nf = null;
      synchronized (finder_map) {
	 nf = finder_map.get(cu);
	 if (nf == null) {
	    nf = new NodeFinder(cu,for_problem.getSourceStart(),getLength());
	    finder_map.put(cu,nf);
	  }
       }
      return nf.getCoveredNode();
    }

   @Override public ASTNode getCoveringNode(CompilationUnit cu) {
      NodeFinder nf = null;
      synchronized (finder_map) {
	 nf = finder_map.get(cu);
	 if (nf == null) {
	    nf = new NodeFinder(cu,for_problem.getSourceStart(),getLength());
	    finder_map.put(cu,nf);
	  }
       }
      return nf.getCoveringNode();
    }

   @Override public int getLength() {
      return for_problem.getSourceEnd() - for_problem.getSourceStart();
    }

   @Override public String getMarkerType() {
      return for_problem.getMarkerType();
    }

   @Override public int getOffset() {
      return for_problem.getSourceStart();
    }

   @Override public String [] getProblemArguments() {
      return for_problem.getArguments();
    }

   @Override public int getProblemId() {
      return for_problem.getID();
    }

   @Override public boolean isError() {
      return for_problem.isError();
    }

}	// end of inner class ProblemContext



}	// end of class BedrockQuickFix




/* end of BedrockQuicFix.java */
