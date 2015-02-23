/********************************************************************************/
/*                                                                              */
/*              NobaseNashorn.java                                              */
/*                                                                              */
/*      AST and parsing using Nashorn parser                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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



package edu.brown.cs.bubbles.nobase;

import jdk.nashorn.internal.ir.*;
import jdk.nashorn.internal.ir.visitor.*;
import jdk.nashorn.internal.runtime.*;
import jdk.nashorn.internal.runtime.options.*;
import jdk.nashorn.internal.parser.*;
import jdk.nashorn.internal.ir.debug.*;
    
    
import java.util.*;
import java.io.*;


class NobaseNashorn implements NobaseConstants, NobaseConstants.IParser
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Map<Node,NashornAstNode> node_map;

private static boolean do_debug = true;


static {
   node_map = new WeakHashMap<Node,NashornAstNode>();
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseNashorn() 
{ }



/********************************************************************************/
/*                                                                              */
/*      Parsing methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public ISemanticData parse(NobaseProject proj,NobaseFile fd,boolean lib)
{
   try {
      Source src = new Source(fd.getFileName(),fd.getFile());
      Options opts = new Options("nashorn");
      PrintWriter pw = new PrintWriter(new StringWriter());
      ScriptEnvironment env = new ScriptEnvironment(opts,pw,pw);
      ErrorManager em = new ErrorManager();
      Parser parser = new Parser(env,src,em);
      FunctionNode fn = parser.parse();
      if (do_debug) {
         ASTWriter pv = new ASTWriter(fn);
         System.err.println("PARSE: " + pv.toString());
       }
      ParseData rslt = new ParseData(proj,fd,em,fn.getBody(),lib);
      return rslt;
    }
   catch (IOException e) {
      return null;
    }
}



/********************************************************************************/
/*                                                                              */
/*       Result Data                                                            */
/*                                                                              */
/********************************************************************************/

private static class ParseData implements ISemanticData {
   
   private NobaseProject for_project;
   private NobaseFile for_file;
   private List<NobaseMessage> message_list;
   private NashornAstNode root_node;
   private boolean is_library;
   
   ParseData(NobaseProject proj,NobaseFile file,ErrorManager em,Block b,boolean lib) {
      for_project = proj;
      for_file = file;
      is_library = lib;
      message_list = new ArrayList<NobaseMessage>();
      // copy errors from error manager
      for_project = proj;
      for_file = file;
      is_library = lib;
      root_node = new NashornAstFileModule(b,null);
    }
   
   @Override public NobaseFile getFileData()		{ return for_file; }
   @Override public NobaseProject getProject()		{ return for_project; }
   @Override public List<NobaseMessage> getMessages()	{ return message_list; }
   @Override public NobaseAst.NobaseAstNode getRootNode() {
      return root_node; 
    }
   
   @Override public void addMessages(List<NobaseMessage> msgs) {
      if (msgs == null || is_library) return;
      message_list.addAll(msgs);
    }
   
}       // end of inner class ParseData



/********************************************************************************/
/*                                                                              */
/*      Generic AST Node                                                        */
/*                                                                              */
/********************************************************************************/

private static NashornAstNode createNashornAstNode(Node pn,NashornAstNode par)
{
   if (pn == null) return null;
   NashornAstNode rslt = node_map.get(pn);
   if (rslt != null) return rslt;
   if (pn instanceof LiteralNode.ArrayLiteralNode) rslt = new NashornAstArrayConstructor(pn,par);
   else if (pn instanceof BinaryNode) {
      BinaryNode bin = (BinaryNode) pn;
      switch (bin.tokenType()) {
         case COMMALEFT :
         case COMMARIGHT :
            rslt = new NashornAstCommaOperation(pn,par);
            break;
         case AND :
         case OR :
            rslt = new NashornAstControlOperation(pn,par);
            break;
         case DELETE :
            rslt = new NashornAstDeleteOperation(pn,par);
            break;
         case IN :
            rslt = new NashornAstInOperation(pn,par);
            break;
         default :
            break;
       }
      if (rslt == null && bin.isAssignment()) {
         rslt = new NashornAstAssignOperation(pn,par);
       }
      else if (rslt == null) {
         rslt = new NashornAstSimpleOperation(pn,par);
       }
    }
   else if (pn instanceof UnaryNode) {
      UnaryNode uny = (UnaryNode) pn;
      switch (uny.tokenType()) {
         case TYPEOF :
            rslt = new NashornAstTypeofOperation(pn,par);
            break;
         default :
            break;
       }
      if (rslt == null) {
         rslt = new NashornAstSimpleOperation(pn,par);
       }
    }
   else if (pn instanceof TernaryNode) {
      rslt = new NashornAstSimpleOperation(pn,par);
    }
   else if (pn instanceof AccessNode) {
      return new NashornAstMemberAccess(pn,par);
    }
   else if (pn instanceof Block) {
      if (par instanceof NashornAstSwitchStatement) {
         NashornAstNode npar = new NashornAstFinallyStatement(pn,par);
         rslt = new NashornAstBlock(pn,npar);
       }
      else rslt = new NashornAstBlock(pn,par);
    }
   else if (pn instanceof BreakNode) {
      rslt = new NashornAstBreakStatement(pn,par);
    }
   else if (pn instanceof CallNode) {
      CallNode cn = (CallNode) pn;
      if (cn.isNew()) rslt = new NashornAstConstructorCall(pn,par);
      else rslt = new NashornAstFunctionCall(pn,par);
    }
   else if (pn instanceof CaseNode) {
      CaseNode cn = (CaseNode) pn;
      if (cn.getTest() == null) rslt = new NashornAstDefaultCaseStatement(pn,par);
      else rslt = new NashornAstCaseStatement(pn,par);
    }
   else if (pn instanceof CatchNode) {
      rslt = new NashornAstCatchStatement(pn,par);
    }
   else if (pn instanceof ContinueNode) {
      rslt = new NashornAstContinueStatement(pn,par);
    }
   else if (pn instanceof EmptyNode) {
      rslt = new NashornAstNoopStatement(pn,par);
    }
   else if (pn instanceof ExpressionStatement) {
      rslt = new NashornAstExpressionStatement(pn,par);
    }
   else if (pn instanceof ForNode) {
      ForNode fn = (ForNode) pn;
      if (fn.isForEach() || fn.isForIn()) {
         rslt = new NashornAstForEachLoop(pn,par);
       }
      else rslt = new NashornAstForLoop(pn,par);
    }
   else if (pn instanceof FunctionNode) {
      return new NashornAstFunctionConstructor(pn,par);
    }
   else if (pn instanceof IdentNode) {
      IdentNode in = (IdentNode) pn;
      if (par instanceof NobaseAst.FunctionConstructor) {
         FunctionNode fn = (FunctionNode) par.getNashornNode();
         if (fn.getIdent() == in) {
            rslt = new NashornAstIdentifier(pn,par);
          }
         else {
            rslt = new NashornAstFormalParameter(pn,par);
            new NashornAstIdentifier(pn,rslt);
          }
       }
      else  {
         rslt = new NashornAstIdentifier(pn,par);
       }
    }
   else if (pn instanceof IndexNode) {
      rslt = new NashornAstArrayIndex(pn,par);
    }
   else if (pn instanceof IfNode) {
      rslt = new NashornAstIfStatement(pn,par);
    }
   else if (pn instanceof LabelNode) {
      rslt = new NashornAstLabeledStatement(pn,par);
    }
   else if (pn instanceof LiteralNode) {
      LiteralNode<?> ln = (LiteralNode<?>) pn;
      if (ln.isNull()) {
         rslt = new NashornAstNullLiteral(pn,par);
       }
      else if (ln.isString()) {
         rslt = new NashornAstStringLiteral(pn,par);
       }
      else if (ln.getValue() instanceof Boolean) {
         rslt = new NashornAstBooleanLiteral(pn,par);
       }
      else if (ln.getValue() instanceof Long || ln.getValue() instanceof Integer) {
         rslt = new NashornAstIntegerLiteral(pn,par);
       }
      else if (ln.isNumeric()) {
         rslt = new NashornAstRealLiteral(pn,par);
       }
      // handle regex
    }
   else if (pn instanceof ObjectNode) {
      rslt = new NashornAstObjectConstructor(pn,par);
    }
   else if (pn instanceof PropertyNode) {
      PropertyNode prop = (PropertyNode) pn;
      // getter/etter might not be correct
      if (prop.getGetter() != null) {
         rslt = new NashornAstGetterProperty(pn,par);
       }
      else if (prop.getSetter() != null) {
         rslt = new NashornAstSetterProperty(pn,par);
       }
      else rslt = new NashornAstValueProperty(pn,par);
    }
   else if (pn instanceof ReturnNode) {
      rslt = new NashornAstReturnStatement(pn,par);
    }
   else if (pn instanceof SwitchNode) {
      rslt = new NashornAstSwitchStatement(pn,par);
    }
   else if (pn instanceof ThrowNode) {
      rslt = new NashornAstThrowStatement(pn,par);
    }
   else if (pn instanceof TryNode) {
      rslt = new NashornAstTryStatement(pn,par);
    }
   else if (pn instanceof VarNode) {
      rslt = new NashornAstDeclaration(pn,par);
    }
   else if (pn instanceof WhileNode) {
      WhileNode wn = (WhileNode) pn;
      if (wn.isDoWhile()) rslt = new NashornAstDoWhileLoop(pn,par);
      else rslt = new NashornAstWhileLoop(pn,par);
    }
   else if (pn instanceof WithNode) {
      rslt = new NashornAstWithStatement(pn,par);
    }
   
   
   return rslt;
}



private abstract static class NashornAstNode extends NobaseAstNodeBase {
   
   private Node nashorn_node;
   private NashornAstNode parent_node;
   private List<NashornAstNode> child_nodes;
   
   NashornAstNode(Node ptn,NashornAstNode par) {
      nashorn_node = ptn;
      parent_node = par;
      child_nodes = null;
      if (ptn != null) {
         node_map.put(ptn,this);
         ChildFinder cf = new ChildFinder();
         ptn.accept(cf);
       }
    }
   
   protected Node getNashornNode()		{ return nashorn_node; }
   
   void addChild(NashornAstNode node) {
      if (child_nodes == null) child_nodes = new ArrayList<NashornAstNode>();
      child_nodes.add(node);
    }
   
   public void accept(NobaseAstVisitor v) {
      if (v == null) return;
      if (v.preVisit2(this)) {
         if (accept0(v)) {
            if (child_nodes != null) {
               for (NashornAstNode cn : child_nodes) {
                  cn.accept(v);
                }
             }
          }
         accept1(v);
       }
      v.postVisit(this);
    }
   
   protected abstract boolean accept0(NobaseAstVisitor v);
   protected abstract void accept1(NobaseAstVisitor v);
   
   @Override public NobaseAst.NobaseAstNode getParent() { return parent_node; }
   @Override public int getNumChildren() {
      if (child_nodes == null) return 0;
      return child_nodes.size();
    }
   @Override public NobaseAst.NobaseAstNode getChild(int i) {
      if (i < 0) return null;
      if (child_nodes == null) return null;
      if (i >= child_nodes.size()) return null;
      return child_nodes.get(i);
    }
   
   @Override public int getStartLine() {
      return 0;
    }
   @Override public int getStartChar() {
      return 0;
    }
   @Override public int getStartPosition() {
      return nashorn_node.getStart();
    }
   @Override public int getEndLine() {
      return 0;
    }
   @Override public int getEndChar() {
      return 0;
    }
   @Override public int getEndPosition() {
      return nashorn_node.getFinish();
    }
   
   @Override public int getExtendedStartPosition() {
     return getStartPosition();
    }
   
   @Override public int getExtendedEndPosition() {
      int epos = getEndPosition();
      return epos;
    }
   
   @Override public String toString() {
      if (nashorn_node != null) {
         PrintVisitor pv = new PrintVisitor(nashorn_node,false);
         return pv.toString();
       }
      else return super.toString();
    }
   
}	// end of inner class NashornAst



/********************************************************************************/
/*                                                                              */
/*      Visitor to get child nodes                                              */
/*                                                                              */
/********************************************************************************/

private static class ChildFinder extends NodeVisitor<LexicalContext> {
   
   private Stack<NashornAstNode> parent_stack;
   
   ChildFinder() {
      super(new LexicalContext());
      parent_stack = new Stack<NashornAstNode>();
    }
   
   @Override protected boolean enterDefault(Node n) {
      NashornAstNode par = null;
      if (!parent_stack.isEmpty()) par = parent_stack.peek();
      NashornAstNode node = createNashornAstNode(n,par);
      if (par != null) par.addChild(node);
      parent_stack.push(node);
      return true;
    }
   
  @Override protected Node leaveDefault(Node n) {
     parent_stack.pop();
     return n;
   }
  
}       // end of inner class ChildFinder


/********************************************************************************/
/*                                                                              */
/*      FileModule node                                                         */
/*                                                                              */
/********************************************************************************/

private static class NashornAstFileModule extends NashornAstNode implements NobaseAst.FileModule {
   
   NashornAstBlock  block_node;
   
   NashornAstFileModule(Block b,NashornAstNode par) {
      super(null,par);
      block_node = (NashornAstBlock) createNashornAstNode(b,this);
    }
   
   protected Node getNashornNode()		{ return block_node.getNashornNode(); }
   
   @Override public NobaseAst.Block getBlock() {
      return block_node;
    }
   @Override public int getNumChildren()                 { return 1; }
   @Override public NobaseAst.NobaseAstNode getChild(int i) {
      if (i == 0) return block_node;
      return null;
    }
   
   public void accept(NobaseAstVisitor v) {
      if (v == null) return;
      if (v.preVisit2(this)) {
         if (accept0(v)) {
            block_node.accept(v);
          }
         accept1(v);
       }
      v.postVisit(this);
    } 
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}       // end of inner class NashornAstFileModule



/********************************************************************************/
/*                                                                              */
/*      Generic node types                                                      */
/*                                                                              */
/********************************************************************************/

private abstract static class NashornAstExpression extends NashornAstNode implements NobaseAst.Expression {
   
   protected NashornAstExpression(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private Expression getExprNode()             { return (Expression) getNashornNode(); }
   
   @Override public boolean isLeftHandSide() {
      // go up parents
      return false;
    }
}




private abstract static class NashornAstOperation extends NashornAstExpression implements NobaseAst.Operation {
   
   protected NashornAstOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public int getNumOperands() {
       return getNumChildren();
    }
   
   @Override public String getOperator() {
      return getNashornNode().tokenType().getName();
    }
   
   @Override public NobaseAst.Expression getOperand(int i) {
      return (NobaseAst.Expression) getChild(i);
    }
   
}       // end of inner calss NashornAstOperation



/********************************************************************************/
/*                                                                              */
/*      AST nodes for Nashorn AST                                               */
/*                                                                              */
/********************************************************************************/


private static class NashornAstArrayConstructor extends NashornAstExpression implements NobaseAst.ArrayConstructor {
   
   NashornAstArrayConstructor(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public int getNumElements() {
      return getNumChildren();
    }
   
   @Override public NobaseAst.Expression getElement(int i) {
      return (NobaseAst.Expression) getChild(i);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstArrayConstructor



private static class NashornAstArrayIndex extends NashornAstOperation implements NobaseAst.ArrayIndex {
   
   NashornAstArrayIndex(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}       // end of inner class NashornAstArrayIndex	



private static class NashornAstAssignOperation extends NashornAstOperation implements NobaseAst.AssignOperation {
   
   NashornAstAssignOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstAssignOperation




private static class NashornAstBlock extends NashornAstNode implements NobaseAst.Block {
   
   NashornAstBlock(Node n,NashornAstNode par) {
      super(n,par);
    }
   
   @Override protected boolean accept0(NobaseAstVisitor v)      { return v.visit(this); }
   @Override protected void accept1(NobaseAstVisitor v)         { v.endVisit(this); }
   
}       // end of inner class NashornAstBlock


private static class NashornAstBooleanLiteral extends NashornAstExpression implements NobaseAst.BooleanLiteral {
   
   NashornAstBooleanLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public Boolean getValue() {
      return ((LiteralNode<?>) getNashornNode()).getBoolean();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstBooleanLiteral




private static class NashornAstBreakStatement extends NashornAstNode implements NobaseAst.BreakStatement {
   
   NashornAstBreakStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstBreakStatement




private static class NashornAstCaseStatement extends NashornAstNode implements NobaseAst.CaseStatement {
   
   NashornAstCaseStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstCaseStatement




private static class NashornAstCatchStatement extends NashornAstNode implements NobaseAst.CatchStatement {
   
   NashornAstCatchStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstCatchStatement



private static class NashornAstCommaOperation extends NashornAstOperation implements NobaseAst.CommaOperation {
   
   NashornAstCommaOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstCommaOperation



private static class NashornAstConstructorCall extends NashornAstOperation implements NobaseAst.ConstructorCall {
   
   NashornAstConstructorCall(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstConstructorCall


private static class NashornAstContinueStatement extends NashornAstNode implements NobaseAst.ContinueStatement {
   
   NashornAstContinueStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstContinueStatement




private static class NashornAstControlOperation extends NashornAstOperation implements NobaseAst.ControlOperation {
   
   NashornAstControlOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstControlOperation




private static class NashornAstDefaultCaseStatement extends NashornAstNode implements NobaseAst.DefaultCaseStatement {
   
   NashornAstDefaultCaseStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstDefaultCaseStatement



private static class NashornAstDeleteOperation extends NashornAstOperation implements NobaseAst.DeleteOperation {
   
   NashornAstDeleteOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstDeleteOperation




private static class NashornAstDeclaration extends NashornAstNode implements NobaseAst.Declaration {
   
   NashornAstDeclaration(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private VarNode getNode() { 
      return (VarNode) getNashornNode();
    }
   
   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) createNashornAstNode(getNode().getAssignmentDest(),this);
    }
   
   @Override public NobaseAst.Expression getInitializer() {
      return (NobaseAst.Expression) createNashornAstNode(getNode().getInit(),this);
    }
   
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstDeclaration





private static class NashornAstDoWhileLoop extends NashornAstNode implements NobaseAst.DoWhileLoop {
   
   NashornAstDoWhileLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstDoWhileLoop





private static class NashornAstExpressionStatement extends NashornAstNode implements NobaseAst.ExpressionStatement {
   
   NashornAstExpressionStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private ExpressionStatement getNode()        { return (ExpressionStatement) getNashornNode(); }
   
   @Override public NobaseAst.Expression getExpression() {
      return (NobaseAst.Expression) createNashornAstNode(getNode().getExpression(),this);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstExpressionStatement




private static class NashornAstFinallyStatement extends NashornAstNode implements NobaseAst.FinallyStatement {
   
   NashornAstFinallyStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstFinallyStatement




private static class NashornAstForEachLoop extends NashornAstNode implements NobaseAst.ForEachLoop {
   
   NashornAstForEachLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstForEachLoop




private static class NashornAstForLoop extends NashornAstNode implements NobaseAst.ForLoop {
   
   NashornAstForLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstForLoop




private static class NashornAstFormalParameter extends NashornAstNode implements NobaseAst.FormalParameter {
   
   NashornAstFormalParameter(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public NobaseAst.Identifier getIdentifier() {
     return (NobaseAst.Identifier)  getChild(0);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstFormalParameter



private static class NashornAstFunctionCall extends NashornAstOperation implements NobaseAst.FunctionCall {
   
   NashornAstFunctionCall(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstFunctionCall



private static class NashornAstFunctionConstructor extends NashornAstExpression implements NobaseAst.FunctionConstructor {
   
   NashornAstFunctionConstructor(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private FunctionNode getNode()        { return (FunctionNode) getNashornNode(); }
   
   @Override public NobaseAst.Block getBody() {
      return (NobaseAst.Block) createNashornAstNode(getNode().getBody(),this);
    }
   
   @Override public NobaseAst.Identifier getIdentifier() {
      return (NobaseAst.Identifier) createNashornAstNode(getNode().getIdent(),this);
    }
   
   @Override public List<NobaseAst.FormalParameter> getParameters() {
      List<NobaseAst.FormalParameter> rslt = new ArrayList<NobaseAst.FormalParameter>();
      // this might not work
      for (IdentNode fp : getNode().getParameters()) {
         rslt.add((NobaseAst.FormalParameter) createNashornAstNode(fp,this));
       }
      return rslt;
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstFunctionConstructor




private static class NashornAstFunctionDeclaration extends NashornAstNode implements NobaseAst.FunctionDeclaration {
   
   NashornAstFunctionDeclaration(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstFunctionDeclaration




private static class NashornAstGetterProperty extends NashornAstNode implements NobaseAst.GetterProperty {
   
   NashornAstGetterProperty(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstGetterProperty




private static class NashornAstIdentifier extends NashornAstNode implements NobaseAst.Identifier {
   
   NashornAstIdentifier(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private IdentNode getNode()         { return (IdentNode) getNashornNode(); }
   
   @Override public String getName() {
      return getNode().getName();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstIdentifier




private static class NashornAstIfStatement extends NashornAstNode implements NobaseAst.IfStatement {
   
   NashornAstIfStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstIfStatement



private static class NashornAstInOperation extends NashornAstOperation implements NobaseAst.InOperation {
   
   NashornAstInOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstInOperation



private static class NashornAstIntegerLiteral extends NashornAstExpression implements NobaseAst.IntegerLiteral {
   
   NashornAstIntegerLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public Number getValue() {
      return (Number) ((LiteralNode<?>) getNashornNode()).getValue();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstIntegerLiteral




private static class NashornAstLabeledStatement extends NashornAstNode implements NobaseAst.LabeledStatement {
   
   NashornAstLabeledStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstLabeledStatement




private static class NashornAstMemberAccess extends NashornAstOperation implements NobaseAst.MemberAccess {
   
   NashornAstMemberAccess(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public String getMemberName() {
      AccessNode an = (AccessNode) getNashornNode();
      return an.getProperty().getName();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstMemberAccess




private static class NashornAstMultiDeclaration extends NashornAstNode implements NobaseAst.MultiDeclaration {
   
   NashornAstMultiDeclaration(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstMultiDeclaration




private static class NashornAstNoopStatement extends NashornAstNode implements NobaseAst.NoopStatement {
   
   NashornAstNoopStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstNoopStatement




private static class NashornAstNullLiteral extends NashornAstExpression implements NobaseAst.NullLiteral {
   
   NashornAstNullLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstNullLiteral




private static class NashornAstObjectConstructor extends NashornAstExpression implements NobaseAst.ObjectConstructor {
   
   NashornAstObjectConstructor(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public int getNumElements()        { return getNumChildren(); }
   @Override public NobaseAst.NobaseAstNode getElement(int i) {
      return getChild(i);
    }
   
   
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstObjectConstructor




private static class NashornAstRealLiteral extends NashornAstExpression implements NobaseAst.RealLiteral {
   
   NashornAstRealLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public Number getValue() {
      return ((LiteralNode<?>) getNashornNode()).getNumber();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstRealLiteral





private static class NashornAstRegexpLiteral extends NashornAstExpression implements NobaseAst.RegexpLiteral {
   
   NashornAstRegexpLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstRegexpLiteral




private static class NashornAstReturnStatement extends NashornAstNode implements NobaseAst.ReturnStatement {
   
   NashornAstReturnStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstReturnStatement




private static class NashornAstSetterProperty extends NashornAstNode implements NobaseAst.SetterProperty {
   
   NashornAstSetterProperty(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstSetterProperty




private static class NashornAstSimpleOperation extends NashornAstOperation implements NobaseAst.SimpleOperation {
   
   NashornAstSimpleOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstSimpleOperation




private static class NashornAstStringLiteral extends NashornAstExpression implements NobaseAst.StringLiteral {
   
   NashornAstStringLiteral(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   @Override public String getValue() {
      return ((LiteralNode<?>) getNashornNode()).getString();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstStringLiteral




private static class NashornAstSwitchStatement extends NashornAstNode implements NobaseAst.SwitchStatement {
   
   NashornAstSwitchStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstSwitchStatement




private static class NashornAstThrowStatement extends NashornAstNode implements NobaseAst.ThrowStatement {
   
   NashornAstThrowStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstThrowStatement




private static class NashornAstTryStatement extends NashornAstNode implements NobaseAst.TryStatement {
   
   NashornAstTryStatement(Node pn,NashornAstNode par) {
      super(pn,par); }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstTryStatement



private static class NashornAstTypeofOperation extends NashornAstOperation implements NobaseAst.TypeofOperation {
   
   NashornAstTypeofOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstTypeofOperation



private static class NashornAstValueProperty extends NashornAstNode implements NobaseAst.ValueProperty {
   
   NashornAstValueProperty(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private PropertyNode getNode()      { return (PropertyNode) getNashornNode(); }
   
   @Override public NobaseAst.Expression getValueExpression() {
      return (NobaseAst.Expression) createNashornAstNode(getNode().getValue(),this);
    }
   
   @Override public String getPropertyName() {
      return getNode().getKeyName();
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstValueProperty




private static class NashornAstWhileLoop extends NashornAstNode implements NobaseAst.WhileLoop {
   
   NashornAstWhileLoop(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstWhileLoop




private static class NashornAstWithStatement extends NashornAstNode implements NobaseAst.WithStatement {
   
   NashornAstWithStatement(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   private WithNode getNode()              { return (WithNode) getNashornNode(); }
   
   @Override public NobaseAst.Statement getBody() {
      return (NobaseAst.Statement) createNashornAstNode(getNode().getBody(),this);
    }
   
   @Override public NobaseAst.Expression getScopeObject() {
      return (NobaseAst.Expression) createNashornAstNode(getNode().getExpression(),this);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstWithStatement


private static class NashornAstVoidOperation extends NashornAstOperation implements NobaseAst.VoidOperation {
   
   NashornAstVoidOperation(Node pn,NashornAstNode par) {
      super(pn,par);
    }
   
   protected boolean accept0(NobaseAstVisitor visitor)	{ return visitor.visit(this); }
   protected void accept1(NobaseAstVisitor visitor)	{ visitor.endVisit(this); }
   
}	// end of inner class NashornAstVoidOperation

}       // end of class NobaseNashorn




/* end of NobaseNashorn.java */

