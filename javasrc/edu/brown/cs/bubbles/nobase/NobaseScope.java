/********************************************************************************/
/*										*/
/*		NobaseScope.java						*/
/*										*/
/*	Representation of a javascript scope					*/
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


class NobaseScope implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,NobaseSymbol>	defined_names;
private ScopeType			scope_type;
private NobaseScope			parent_scope;
private NobaseValue                     object_value;
private int                             temp_counter;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseScope(ScopeType typ,NobaseScope par)
{
   defined_names = new HashMap<String,NobaseSymbol>();
   scope_type = typ;
   parent_scope = par;
   object_value = null;
   temp_counter = 0;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

ScopeType getScopeType()                { return scope_type; }

NobaseScope getParent()                 { return parent_scope; }

NobaseValue getThisValue()    
{ 
   return object_value;
}

int getNextTemp()                       { return ++temp_counter; }

void setValue(NobaseValue nv)           
{ 
   if (nv == null || object_value == nv) return;
   if (object_value != null) {
      nv.mergeProperties(object_value);
    }
   object_value = nv;
}

NobaseSymbol define(NobaseSymbol sym) 
{
   if (sym == null) return null;
   NobaseSymbol osym = defined_names.get(sym.getName());
   if (osym != null) return osym;
   defined_names.put(sym.getName(),sym);
   return sym;
}


void setProperty(String name,NobaseValue nv) 
{
   if (object_value == null) return;
   object_value.addProperty(name,nv);
}



NobaseSymbol lookup(String name)
{
   NobaseSymbol sym = defined_names.get(name);
   if (sym != null) return sym;
   if (parent_scope == null) return null;
   return parent_scope.lookup(name);
}


NobaseValue lookupValue(String name) 
{
   if (name.equals("this") && object_value != null) return getThisValue();
   
   switch (scope_type) {
      case MEMBER :
      case WITH :
         if (object_value != null) {
            NobaseValue nv = object_value.getProperty(name);
            if (nv != null) return nv;
          }
         break;
      default :
         break;
    }
   
   NobaseSymbol ns = lookup(name);
   if (ns == null) return null;
   return ns.getValue();
}

Collection<NobaseSymbol> getDefinedNames()
{
   return defined_names.values();
}

NobaseScope getDefaultScope() 
{
   if (parent_scope == null) return this;
   switch (scope_type) {
      case BLOCK :
      case FUNCTION :
      case OBJECT :
         return parent_scope.getDefaultScope();
      case FILE :
      case GLOBAL :
         break;
      case MEMBER :
      case WITH :
         break;
    }
   return this;
}

}	// end of class NobaseScope




/* end of NobaseSymbol.java */

