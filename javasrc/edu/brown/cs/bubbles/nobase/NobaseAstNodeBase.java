/********************************************************************************/
/*										*/
/*		NobaseAstNodeBase.java						*/
/*										*/
/*	Common code for an AST node						*/
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


abstract class NobaseAstNodeBase implements NobaseConstants, NobaseAst.NobaseAstNode
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Object		prop_a;
private Object		prop_b;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected NobaseAstNodeBase()
{
   prop_a = null;
   prop_b = null;
}


/********************************************************************************/
/*										*/
/*	Property setting methods						*/
/*										*/
/********************************************************************************/

protected void setProperty(AstProperty p,Object v)
{
   if (prop_a == null) {
      prop_a = p;
      prop_b = v;
    }
   else if (prop_a instanceof AstProperty) {
      Map<AstProperty,Object> val = new HashMap<AstProperty,Object>();
      val.put((AstProperty) prop_a,prop_b);
      prop_a = val;
      prop_b = null;
      val.put(p,v);
    }
   else {
      @SuppressWarnings("unchecked")
      Map<AstProperty,Object> val = (Map<AstProperty,Object>) prop_a;
      val.put(p,v);
    }
}


protected Object getProperty(AstProperty p)
{
   if (prop_a == null) return null;
   if (prop_a == p) return prop_b;
   if (prop_a instanceof AstProperty) return null;
   @SuppressWarnings("unchecked")
   Map<AstProperty,Object> val = (Map<AstProperty,Object>) prop_a;
   return val.get(p);
}

protected void removeProperty(AstProperty p)
{
   if (prop_a == null) ;
   else if (prop_a == p) removeAllProperties();
   else if (prop_a instanceof AstProperty) ;
   else {
      @SuppressWarnings("unchecked")
      Map<AstProperty,Object> val = (Map<AstProperty,Object>) prop_a; 
      if (val.containsKey(p)) {
         val.remove(p);
         if (val.size() == 0) removeAllProperties();
         else if (val.size() == 1) {
            for (Map.Entry<AstProperty,Object> ent : val.entrySet()) {
               prop_a = ent.getKey();
               prop_b = ent.getValue();
             }
          }
       }
    }
}


protected void removeAllProperties()
{
   prop_a = null;
   prop_b = null;
}



/********************************************************************************/
/*										*/
/*	Specific property methods						*/
/*										*/
/********************************************************************************/

@Override public void setReference(NobaseSymbol s)
{
   setProperty(AstProperty.REF,s);
}

@Override public NobaseSymbol getReference()
{
   return (NobaseSymbol) getProperty(AstProperty.REF);
}

@Override public void setDefinition(NobaseSymbol s)
{
   setProperty(AstProperty.DEF,s);
}

@Override public NobaseSymbol getDefinition()
{
   return (NobaseSymbol) getProperty(AstProperty.DEF);
}

@Override public void setNobaseName(NobaseName s)
{
   setProperty(AstProperty.NAME,s);
}

@Override public NobaseName getNobaseName()
{
   return (NobaseName) getProperty(AstProperty.NAME);
}

@Override public void setScope(NobaseScope s)
{
   setProperty(AstProperty.SCOPE,s);
}

@Override public NobaseScope getScope()
{
   return (NobaseScope) getProperty(AstProperty.SCOPE);
}


@Override public boolean setNobaseValue(NobaseValue t)
{
   NobaseValue nv = getNobaseValue();
   setProperty(AstProperty.TYPE,t);
   return (t != nv);
}

@Override public NobaseValue getNobaseValue()
{
   return (NobaseValue) getProperty(AstProperty.TYPE);
}


@Override public void clearResolve()
{
   removeAllProperties();
}



}	// end of class NobaseAstNodeBase




/* end of NobaseAstNodeBase.java */
