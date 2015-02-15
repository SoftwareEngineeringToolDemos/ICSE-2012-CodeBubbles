/********************************************************************************/
/*										*/
/*		NobaseResolver.java						*/
/*										*/
/*	Handle symbol and type resolution for JavaScript			*/
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



package edu.brown.cs.bubbles.nobase;


import java.util.*;


class NobaseResolver implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseScope		global_scope;
private NobaseProject		for_project;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseResolver(NobaseProject proj,NobaseScope glbl)
{
   global_scope = glbl;
   for_project = proj;
}




/********************************************************************************/
/*										*/
/*	Remove Resolution methods						*/
/*										*/
/********************************************************************************/

void unresolve(NobaseAstNode node)
{
   UnresolveVisitor uv = new UnresolveVisitor();
   node.accept(uv);
}


private static class UnresolveVisitor extends NobaseAstVisitor {

   @Override public void postVisit(NobaseAstNode n) {
      n.clearResolve();
    }

}	// end of inner class UnresolveVisitor



/********************************************************************************/
/*										*/
/*	Resolution methods							*/
/*										*/
/********************************************************************************/

void resolveSymbols(ISemanticData isd)
{
   NobaseAstNode node = isd.getRootNode();
   List<NobaseMessage> errors = new ArrayList<NobaseMessage>();

   ValuePass vp = new ValuePass(isd.getFileData(),errors);
   for (int i = 0; i < 5; ++i) {
      node.accept(vp);
      if (!vp.checkChanged()) break;
      vp.forceDefine();
    }

   // DefPass dp = new DefPass(errors);
   // node.accept(dp);
//
   // RefPass rp = new RefPass(errors);
   // node.accept(rp);

   // NamePass np = new NamePass(isd.getFileData());
   // node.accept(np);

   isd.addMessages(errors);
}





/********************************************************************************/
/*										*/
/*	Value pass -- assign or update values					*/
/*										*/
/********************************************************************************/

private class ValuePass extends NobaseAstVisitor {

   private NobaseScope cur_scope;
   private List<NobaseMessage> error_list;
   private boolean change_flag;
   private NobaseValue set_lvalue;
   private String enclosing_function;
   private NobaseFile enclosing_file;
   private Stack<String> name_stack;
   private boolean force_define;

   ValuePass(NobaseFile module,List<NobaseMessage> errors) {
      cur_scope = global_scope;
      error_list = errors;
      change_flag = false;
      set_lvalue = null;
      force_define = false;
      enclosing_file = module;
      enclosing_function = null;
      name_stack = new Stack<String>();
    }

   boolean checkChanged() {
      boolean rslt = change_flag;
      change_flag = false;
      return rslt;
    }

   void forceDefine()				{ force_define = true; }

   @Override public boolean visit(FileModule b) {
      cur_scope = b.getScope();
      if (cur_scope == null) {
         cur_scope = new NobaseScope(ScopeType.FILE,global_scope);
         for_project.setupModule(enclosing_file,cur_scope);
         b.setScope(cur_scope);
       }
      NobaseSymbol osym = b.getDefinition();
      if (osym == null) {
         NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,b,
        					 enclosing_file.getModuleName(),false);
         nsym.setBubblesName(enclosing_file.getModuleName());
         b.setDefinition(nsym);
       }
   
      return true;
    }

   @Override public void endVisit(FileModule b) {
      for_project.finishModule(enclosing_file);
      cur_scope = cur_scope.getParent();
    }

   @Override public void endVisit(ArrayConstructor n) {
      List<NobaseValue> vals = new ArrayList<NobaseValue>();
      for (int i = 0; i < n.getNumElements(); ++i) {
	 NobaseValue nv = n.getElement(i).getNobaseValue();
	 vals.add(nv);
       }
      setValue(n,NobaseValue.createArrayValue(vals));
    }

   @Override public boolean visit(ArrayIndex n) {
      NobaseValue lvl = set_lvalue;
      set_lvalue = null;
      for (int i = 0; i < n.getNumOperands(); ++i) {
	 n.getOperand(i).accept(this);
       }
      NobaseValue nvl = n.getOperand(0).getNobaseValue();
      NobaseValue ivl = n.getOperand(1).getNobaseValue();      if (lvl == null) {
	 Object idxv = (ivl == null ? null : ivl.getKnownValue());
	 NobaseValue rvl = null;
	 if (nvl != null) rvl = nvl.getProperty(idxv);
	 if (rvl == null) rvl = NobaseValue.createUnknownValue();
	 setValue(n,rvl);
       }
      else {
	 Object idxv = (ivl == null ? null : ivl.getKnownValue());
	 if (idxv != null && idxv instanceof String && nvl != null) {
	    if (nvl.addProperty(idxv,lvl)) change_flag = true;
	  }
	 setValue(n,lvl);
       }
      return false;
    }

   @Override public boolean visit(AssignOperation n) {
      NobaseValue ovl = set_lvalue;
      set_lvalue = null;
      if (n.getNumOperands() > 1) {
	 n.getOperand(1).accept(this);
	 if (n.getOperator().equals("=")) {
	    set_lvalue = n.getOperand(1).getNobaseValue();
	  }
       }
      n.getOperand(0).accept(this);
      set_lvalue = ovl;
      return false;
    }

   @Override public void endVisit(BooleanLiteral n) {
      setValue(n,NobaseValue.createBoolean(n.getValue()));
    }

   @Override public boolean visit(CatchStatement cstmt) {
      NobaseScope nscp = cstmt.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.BLOCK,cur_scope);
	 cstmt.setScope(nscp);
       }
      cur_scope = nscp;
      return true;
    }

   @Override public void endVisit(CatchStatement cstmt) {
      cur_scope = cur_scope.getParent();
    }

   @Override public void endVisit(CommaOperation n) {
      NobaseValue nv = n.getOperand(n.getNumOperands()-1).getNobaseValue();
      setValue(n,nv);
    }

   @Override public void endVisit(ConstructorCall n) {
      NobaseValue nv = NobaseValue.createObject();
      NobaseValue fv = n.getOperand(0).getNobaseValue();
      nv.setBaseValue(fv);
      setValue(n,nv);
      // handle new X
    }

   @Override public void endVisit(ControlOperation n) {
      setValue(n,NobaseValue.createBoolean());
    }

   @Override public void endVisit(Declaration n) {
      Identifier vident = n.getIdentifier();
      if (vident != null) {
         NobaseSymbol sym = n.getDefinition();
         if (sym == null) {
            NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,n,vident.getName(),true);
            sym = cur_scope.define(nsym);
            if (nsym != sym) duplicateDef(vident.getName(),n);
            n.setDefinition(sym);
            vident.setDefinition(sym);
            if (enclosing_function != null) {
               setName(sym,enclosing_function + "." + vident.getName());
             }
            else {
               setName(sym,vident.getName());
             }
          }
         if (n.getInitializer() != null) {
            NobaseValue nv = n.getInitializer().getNobaseValue();
            sym.setValue(nv);
          }
       }
    }

   @Override public void endVisit(DeleteOperation n) {
      setValue(n,NobaseValue.createAnyValue());
    }

   @Override public void endVisit(Elision n) {
      setValue(n,NobaseValue.createAnyValue());
    }

   @Override public boolean visitExpression(Expression e) {
      set_lvalue = null;
      return true;
    }

   @Override public void endVisit(FormalParameter fp) {
      Identifier fident = fp.getIdentifier();
      if (fident != null) {
         NobaseSymbol osym = fp.getDefinition();
         if (osym == null) {
            String newname = fident.getName();
            NobaseSymbol sym = new NobaseSymbol(for_project,enclosing_file,fp,newname,true);
            osym = cur_scope.define(sym);
            if (osym != sym) duplicateDef(fident.getName(),fp);
            if (enclosing_function != null) {
               newname = enclosing_function + "." + newname;
             }
            setName(sym,newname);
            fp.setDefinition(osym);
            fp.getIdentifier().setDefinition(osym);
          }
       }
    }

   @Override public void endVisit(FunctionCall n) {
      NobaseValue nv = NobaseValue.createUnknownValue();
      NobaseValue fv = n.getOperand(0).getNobaseValue();
      if (fv != null) {
         List<NobaseValue> args = new ArrayList<NobaseValue>();
         for (int i = 1; i < n.getNumOperands(); ++i) {
            args.add(n.getOperand(i).getNobaseValue());
          }
         nv = fv.evaluate(enclosing_file,args);
       }
      setValue(n,nv);
    }

   @Override public boolean visit(FunctionConstructor n) {
      Identifier ident = n.getIdentifier();
      NobaseValue nv = NobaseValue.createFunction(n);
      setValue(n,nv);
      if (ident != null && ident.getName() != null) {
	 NobaseSymbol osym = n.getDefinition();
	 if (osym == null) {
	    NobaseSymbol nsym = new NobaseSymbol(for_project,enclosing_file,n,ident.getName(),true);
	    osym = cur_scope.define(nsym);
	    n.setDefinition(osym);
	    if (osym != nsym) {
	       duplicateDef(ident.getName(),n);
	       nsym = osym;
	     }
	    nsym.setValue(nv);
	  }
       }

      NobaseScope nscp = n.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.FUNCTION,cur_scope);
	 n.setScope(nscp);
       }
      cur_scope = nscp;
      cur_scope.setValue(nv);

      name_stack.push(enclosing_function);
      NobaseAstNode defnd = getFunctionNode(n);
      String defnm = getFunctionName(n);
      if (defnm != null) {
	 NobaseSymbol nsym = defnd.getDefinition();
	 if (nsym == null) nsym = n.getDefinition();
	 if (nsym == null) {
	    String fnm = null;
	    if (ident != null && ident.getName() != null) fnm = ident.getName();
	    else fnm = "$" + cur_scope.getNextTemp();
	    nsym = new NobaseSymbol(for_project,enclosing_file,defnd,fnm,true);
	  }
	 defnd.setDefinition(nsym);
	 setName(nsym,defnm);
	 if (enclosing_function == null) enclosing_function = defnm;
	 else if (defnm.contains(".")) enclosing_function = defnm;
	 else enclosing_function += "." + defnm;
       }

      return true;
    }


   @Override public void endVisit(FunctionConstructor n) {
      cur_scope = cur_scope.getParent();
      enclosing_function = name_stack.pop();
    }

   @Override public void endVisit(InOperation n) {
      setValue(n,NobaseValue.createBoolean());
    }

   @Override public void endVisit(IntegerLiteral n) {
      setValue(n,NobaseValue.createNumber(n.getValue()));
    }

   @Override public boolean visit(MemberAccess n) {
      n.getOperand(0).accept(this);
      NobaseScope scp = n.getScope();
      if (scp == null) {
         scp = new NobaseScope(ScopeType.MEMBER,cur_scope);
         n.setScope(scp);
       }
      scp.setValue(n.getOperand(0).getNobaseValue());
      cur_scope = scp;
      if (set_lvalue != null) {
         String nm = n.getMemberName();
         cur_scope.setProperty(nm,set_lvalue);
         setValue(n,set_lvalue);
       }
      else {
         n.getOperand(1).accept(this);
         setValue(n,n.getOperand(1).getNobaseValue());
       }
      cur_scope = cur_scope.getParent();
      return false;
    }


   @Override public void endVisit(NullLiteral n) {
      setValue(n,NobaseValue.createNull());
    }

   @Override public boolean visit(ObjectConstructor n) {
      NobaseScope scp = n.getScope();
      if (scp == null) {
	 scp = new NobaseScope(ScopeType.OBJECT,cur_scope);
	 scp.setValue(NobaseValue.createObject());
	 n.setScope(scp);
       }
      cur_scope = scp;
      for (int i = 0; i < n.getNumElements(); ++i) {
	 NobaseAstNode nn = n.getElement(i);
	 nn.accept(this);
       }
      cur_scope = scp.getParent();
      return false;
    }

   @Override public void endVisit(RealLiteral n) {
      setValue(n,NobaseValue.createNumber(n.getValue()));
    }

   @Override public boolean visit(Reference n) {
      Identifier id = n.getIdentifier();
      String name = id.getName();
      NobaseSymbol ref = id.getDefinition();
      if (ref == null) ref = id.getReference();
      if (ref == null) {
         ref = cur_scope.lookup(name);
         if (ref == null && force_define) {
            // see if we should create implicit definition
            NobaseScope dscope = cur_scope.getDefaultScope();
            if (dscope.getScopeType() == ScopeType.FILE ||
        	  dscope.getScopeType() == ScopeType.GLOBAL) {
               NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
        	     "Implicit declaration of " + name,
        	     n.getStartLine(),n.getStartChar(),
        	     n.getEndLine(),n.getEndChar());
               error_list.add(msg);
               ref = new NobaseSymbol(for_project,enclosing_file,n,name,false);
               dscope.define(ref);
               setName(ref,name);
             }
            else {
               dscope.setProperty(name,NobaseValue.createAnyValue());
             }
          }
         id.setReference(ref);
       }
     if (ref != null) {
        if (set_lvalue != null) {
           NobaseValue nv = NobaseValue.mergeValues(set_lvalue,ref.getValue());
           setValue(n,nv);
         }
        else {
           n.setNobaseValue(ref.getValue());
         }
      }
     else {
        NobaseValue nv = cur_scope.lookupValue(name);
        if (nv == null) {
           System.err.println("NOBASE: no value found for " + name + " at " +
              n.getStartLine());
           nv = NobaseValue.createUnknownValue();
         }
        n.setNobaseValue(nv);
      }
   
     return false;
    }

   @Override public void endVisit(RegexpLiteral n) {
      NobaseSymbol regex = global_scope.lookup("RegExp");
      NobaseValue nv = NobaseValue.createObject();
      if (regex != null) nv = regex.getValue();
      setValue(n,nv);
    }

   @Override public void endVisit(SimpleOperation n) {
      NobaseValue nv = NobaseValue.createAnyValue();
      setValue(n,nv);
    }

   @Override public void endVisit(StringLiteral n) {
      setValue(n,NobaseValue.createString(n.getValue()));
    }

   @Override public void endVisit(TypeofOperation n) {
      setValue(n,NobaseValue.createString());
    }

   @Override public void endVisit(ValueProperty n) {
      // handle GETTER and SETTER similarly
      NobaseValue nv = n.getValueExpression().getNobaseValue();
      cur_scope.setProperty(n.getPropertyName(),nv);
    }

   @Override public void endVisit(VoidOperation n) {
      setValue(n,NobaseValue.createAnyValue());
    }

   @Override public boolean visit(WithStatement wstmt) {
      wstmt.getScopeObject().accept(this);
      NobaseValue nv = wstmt.getNobaseValue();
      NobaseScope nscp = wstmt.getScope();
      if (nscp == null) {
	 nscp = new NobaseScope(ScopeType.WITH,cur_scope);
	 wstmt.setScope(nscp);
       }
      cur_scope = nscp;
      cur_scope.setValue(nv);
      wstmt.getBody().accept(this);
      cur_scope = cur_scope.getParent();
      return false;
    }


   private void setValue(NobaseAstNode n,NobaseValue v) {
      if (v == null) v = NobaseValue.createUnknownValue();
      change_flag |= n.setNobaseValue(v);
    }

   private void duplicateDef(String nm,NobaseAstNode n) {
      NobaseMessage msg = new NobaseMessage(ErrorSeverity.WARNING,
	    "Duplicate defintion of " + nm,
	    n.getStartLine(),n.getStartChar(),n.getEndLine(),n.getEndChar());
      error_list.add(msg);
    }

   private void setName(NobaseSymbol sym,String name) {
      String tnm = enclosing_file.getModuleName();
      String qnm = tnm + "." + name;
      if (sym != null) sym.setBubblesName(qnm);
    }

   private String getFunctionName(FunctionConstructor fc) {
      if (fc.getIdentifier() != null && fc.getIdentifier().getName() != null)
	 return fc.getIdentifier().getName();
      if (fc.getParent() instanceof Declaration) {
	 Declaration d = (Declaration) fc.getParent();
	 return d.getIdentifier().getName();
       }
      if (fc.getParent() instanceof AssignOperation) {
	 AssignOperation ao = (AssignOperation) fc.getParent();
	 if (ao.getOperand(1) != fc) return null;
	 if (ao.getOperand(0) instanceof MemberAccess) {
	    MemberAccess ma = (MemberAccess) ao.getOperand(0);
	    String m1 = getIdentName(ma.getOperand(1));
	    if (m1 == null) return null;
	    String m0 = getIdentName(ma.getOperand(0));
	    if (m0 != null && m0.equals("this") && enclosing_function != null) {
	       return m1;
	     }
	    else if (m0 == null && ma.getOperand(0) instanceof MemberAccess) {
	       MemberAccess ma1 = (MemberAccess) ma.getOperand(0);
	       String k1 = getIdentName(ma1.getOperand(0));
	       String k2 = getIdentName(ma1.getOperand(1));
	       if (k2 != null && k2.equals("prototype") && k1 != null) {
		  return k1 + "." + m1;
		}
	     }
	  }
       }
      return null;
    }

   private NobaseAstNode getFunctionNode(FunctionConstructor fc) {
      if (fc.getParent() instanceof FunctionDeclaration) return fc.getParent();
      if (fc.getIdentifier() != null && fc.getIdentifier().getName() != null) return fc;
      if (fc.getParent() instanceof Declaration) return fc.getParent();
      if (fc.getParent() instanceof AssignOperation) return fc.getParent();
      return null;
    }

   private String getIdentName(Expression e) {
      if (e instanceof Identifier) {
	 return ((Identifier) e).getName();
       }
      else if (e instanceof Reference) {
	 return ((Reference) e).getIdentifier().getName();
       }
      return null;
    }

}	// end of inner class ValuePass






}	// end of class NobaseResolver




/* end of NobaseResolver.java */

