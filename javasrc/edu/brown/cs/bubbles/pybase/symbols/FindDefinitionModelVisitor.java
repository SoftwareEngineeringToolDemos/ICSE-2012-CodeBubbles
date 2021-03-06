/********************************************************************************/
/*										*/
/*		FindDefinitionModelVisitor.java 				*/
/*										*/
/*	Python Bubbles Base visitor for finding definitions			*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.*;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.Set;


/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitor extends AbstractVisitor {

/**
 * This is the token to find.
 */
private String		 token_to_find;

/**
 * List of definitions.
 */
public List<Definition> definition_set	    = new ArrayList<Definition>();

/**
 * Stack of classes / methods to get to a definition.
 */
private FastStack<SimpleNode>  defs_stack		= new FastStack<SimpleNode>();

/**
 * This is a stack that will keep the globals for each stack
 */
private FastStack<Set<String>> global_declarations_stack = new FastStack<Set<String>>();

/**
 * This is the module we are visiting: just a weak reference so that we don't create a cycle (let's
 * leave things easy for the garbage collector).
 */
private WeakReference<AbstractModule> for_module;

/**
 * It is only available if the cursor position is upon a NameTok in an import (it represents the complete
 * path for finding the module from the current module -- it can be a regular or relative import).
 */
public String		  module_imported;

private int		    cur_line;

private int		    cur_col;

private boolean 	found_as_definition	  = false;

private Definition	     definition_found;

/**
 * Call is stored for the context for a keyword parameter
 */
private Stack<Call>	    call_stack		= new Stack<Call>();

/**
 * Constructor
 * @param line: starts at 1
 * @param col: starts at 1
 */
public FindDefinitionModelVisitor(String token,int line,int col,AbstractModule module)
{
   token_to_find = token;
   for_module = new WeakReference<AbstractModule>(module);
   cur_line = line;
   cur_col = col;
   module_name = module.getName();
   // we may have a global declared in the global scope
   global_declarations_stack.push(new HashSet<String>());
}

@Override public Object visitImportFrom(ImportFrom node) throws Exception
{
   String modRep = NodeUtils.getRepresentationString(node.module);
   if (NodeUtils.isWithin(cur_line, cur_col, node.module)) {
      // it is a token in the definition of a module
      int startingCol = node.module.beginColumn;
      int endingCol = startingCol;
      while (endingCol < cur_col) {
	 endingCol++;
      }
      int lastChar = endingCol - startingCol;
      module_imported = modRep.substring(0, lastChar);
      int i = lastChar;
      while (i < modRep.length()) {
	 if (Character.isJavaIdentifierPart(modRep.charAt(i))) {
	    i++;
	 }
	 else {
	    break;
	 }
      }
      module_imported += modRep.substring(lastChar, i);
   }
   else {
      // it was not the module, so, we have to check for each name alias imported
      for (aliasType alias : node.names) {
	 // we do not check the 'as' because if it is some 'as', it will be gotten as a
// global in the module
	 if (NodeUtils.isWithin(cur_line, cur_col, alias.name)) {
	    module_imported = modRep + "."
		     + NodeUtils.getRepresentationString(alias.name);
	 }
      }
   }
   return super.visitImportFrom(node);
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
 */
protected Object unhandled_node(SimpleNode node) throws Exception
{
   return null;
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
 */
public void traverse(SimpleNode node) throws Exception
{
   node.traverse(this);
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
 */
public Object visitClassDef(ClassDef node) throws Exception
{
   global_declarations_stack.push(new HashSet<String>());
   defs_stack.push(node);

   node.traverse(this);

   defs_stack.pop();
   global_declarations_stack.pop();

   checkDeclaration(node, (NameTok) node.name);
   return null;
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
 */
public Object visitFunctionDef(FunctionDef node) throws Exception
{
   global_declarations_stack.push(new HashSet<String>());
   defs_stack.push(node);

   if (node.args != null) {
      if (node.args.args != null) {
	 for (exprType arg : node.args.args) {
	    if (arg instanceof Name) {
	       checkParam((Name) arg);
	    }
	 }
      }
      if (node.args.kwonlyargs != null) {
	 for (exprType arg : node.args.kwonlyargs) {
	    if (arg instanceof Name) {
	       checkParam((Name) arg);
	    }
	 }
      }
   }
   node.traverse(this);

   defs_stack.pop();
   global_declarations_stack.pop();

   checkDeclaration(node, (NameTok) node.name);
   return null;
}

/**
 * @param node the declaration node we're interested in (class or function)
 * @param name the token that represents the name of that declaration
 */
private void checkParam(Name name)
{
   String rep = NodeUtils.getRepresentationString(name);
   if (rep.equals(token_to_find) && cur_line == name.beginLine
	    && cur_col >= name.beginColumn && cur_col <= name.beginColumn + rep.length()) {
      found_as_definition = true;
      // if it is found as a definition it is an 'exact' match, so, erase all the others.
      LocalScope scope = new LocalScope(defs_stack);
      for (Iterator<Definition> it = definition_set.iterator(); it.hasNext();) {
	 Definition d = it.next();
	 if (!d.getScope().equals(scope)) {
	    it.remove();
	 }
      }


      definition_found = new Definition(cur_line,name.beginColumn,rep,name,scope,
	       for_module.get());
      definition_set.add(definition_found);
   }
}

@Override public Object visitCall(Call node) throws Exception
{
   call_stack.push(node);
   Object r = super.visitCall(node);
   call_stack.pop();
   return r;
}


@Override public Object visitNameTok(NameTok node) throws Exception
{
   if (node.ctx == NameTok.KeywordName) {
      if (cur_line == node.beginLine) {
	 String rep = NodeUtils.getRepresentationString(node);

	 if (isInside(cur_col, node.beginColumn, rep.length())) {
	    found_as_definition = true;
	    // if it is found as a definition it is an 'exact' match, so, erase all the
// others.
	    LocalScope scope = new LocalScope(defs_stack);
	    for (Iterator<Definition> it = definition_set.iterator(); it.hasNext();) {
	       Definition d = it.next();
	       if (!d.getModule().equals(scope)) {
		  it.remove();
	       }
	    }

	    definition_set.clear();

	    definition_found = new KeywordParameterDefinition(cur_line,node.beginColumn,
		     rep,node,scope,for_module.get(),call_stack.peek());
	    definition_set.add(definition_found);
	    throw new StopVisitingException();
	 }
      }
   }
   return null;
}

/**
 * @param node the declaration node we're interested in (class or function)
 * @param name the token that represents the name of that declaration
 */
private void checkDeclaration(SimpleNode node,NameTok name)
{
   String rep = NodeUtils.getRepresentationString(node);
   if (rep.equals(token_to_find)
	    && ((cur_line == -1 && cur_col == -1) || (cur_line == name.beginLine
		     && cur_col >= name.beginColumn && cur_col <= name.beginColumn
		     + rep.length()))) {
      found_as_definition = true;
      // if it is found as a definition it is an 'exact' match, so, erase all the others.
      LocalScope scope = new LocalScope(defs_stack);
      for (Iterator<Definition> it = definition_set.iterator(); it.hasNext();) {
	 Definition d = it.next();
	 if (!d.getScope().equals(scope)) {
	    it.remove();
	 }
      }


      definition_found = new Definition(name.beginLine,name.beginColumn,rep,node,scope,
	       for_module.get());
      definition_set.add(definition_found);
   }
}

@Override public Object visitGlobal(Global node) throws Exception
{
   for (NameTokType n : node.names) {
      global_declarations_stack.peek().add(NodeUtils.getFullRepresentationString(n));
   }
   return null;
}

@Override public Object visitModule(Module node) throws Exception
{
   defs_stack.push(node);
   return super.visitModule(node);
}


/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
 */
public Object visitAssign(Assign node) throws Exception
{
   LocalScope scope = new LocalScope(defs_stack);
   if (found_as_definition && !scope.equals(definition_found.getScope())) { // if it is
       // found as a definition it is an 'exact' match, so, we do not keep checking it
      return null;
   }

   for (int i = 0; i < node.targets.length; i++) {
      exprType target = node.targets[i];
      if (target instanceof Subscript) {
	 continue; // assigning to an element and not the variable itself. E.g.: mydict[1]
// = 10 (instead of mydict = 10)
      }

      if (target instanceof Tuple) {
	 // if assign is xxx, yyy = 1, 2
	 // let's separate those as different assigns and analyze one by one
	 Tuple targetTuple = (Tuple) target;
	 if (node.value instanceof Tuple) {
	    Tuple valueTuple = (Tuple) node.value;
	    checkTupleAssignTarget(targetTuple, valueTuple.elts);

	 }
	 else if (node.value instanceof org.python.pydev.parser.jython.ast.List) {
	    org.python.pydev.parser.jython.ast.List valueList = (org.python.pydev.parser.jython.ast.List) node.value;
	    checkTupleAssignTarget(targetTuple, valueList.elts);

	 }
	 else {
	    checkTupleAssignTarget(targetTuple, new exprType[] { node.value });
	 }

      }
      else {
	 String rep = NodeUtils.getFullRepresentationString(target);

	 if (token_to_find.equals(rep)) { // note, order of equals is important (because
// one side may be null).
	    exprType nodeValue = node.value;
	    String value = NodeUtils.getFullRepresentationString(nodeValue);
	    if (value == null) {
	       value = "";
	    }

	    // get the line and column correspondent to the target
	    int line = NodeUtils.getLineDefinition(target);
	    int col = NodeUtils.getColDefinition(target);

	    AssignDefinition definition = new AssignDefinition(value,rep,i,node,line,col,
		     scope,for_module.get(),nodeValue);

	    // mark it as global (if it was found as global in some of the previous
// contexts).
	    for (Set<String> globals : global_declarations_stack) {
	       if (globals.contains(rep)) {
		  definition.found_as_global = true;
	       }
	    }

	    definition_set.add(definition);
	 }
      }
   }

   return null;
}


/**
 * Analyze an assign that has the target as a tuple and the multiple elements in the other side.
 *
 * E.g.: www, yyy = 1, 2
 *
 * @param targetTuple the target in the assign
 * @param valueElts the values that are being assigned
 */
private void checkTupleAssignTarget(Tuple targetTuple,exprType[] valueElts)
	 throws Exception
{
   if (valueElts == null || valueElts.length == 0) {
      return; // nothing to do if we don't have any values
   }

   for (int i = 0; i < targetTuple.elts.length; i++) {
      int j = i;
      // that's if the number of values is less than the number of assigns (actually,
// that'd
      // probably be an error, but let's go on gracefully, as the user can be in an
// invalid moment
      // in his code)
      if (j >= valueElts.length) {
	 j = valueElts.length - 1;
      }
      Assign assign = new Assign(new exprType[] { targetTuple.elts[i] },valueElts[j]);
      assign.beginLine = targetTuple.beginLine;
      assign.beginColumn = targetTuple.beginColumn;
      visitAssign(assign);
   }
}




/********************************************************************************/
/*										*/
/*	From PySelection: utility methods					*/
/*										*/
/********************************************************************************/

private static boolean isInside(int col, int initialCol, int len)
{
   if(col >= initialCol && col <= (initialCol + len)){
      return true;
    }
   return false;
}




} // end of class FindDefinitionModelVisitor


/* enf of FindDefinitionModelVisitor.java */
