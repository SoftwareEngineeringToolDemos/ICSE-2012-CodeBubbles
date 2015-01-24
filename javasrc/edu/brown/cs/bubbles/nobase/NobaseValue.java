/********************************************************************************/
/*										*/
/*		NobaseValue.java						*/
/*										*/
/*	Representation of a javascript value					*/
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


class NobaseValue implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseType	value_type;
protected Object	known_value;


private static Map<Object,NobaseValue>	value_map = new WeakHashMap<Object,NobaseValue>();



private static NobaseValue undef_value = new UndefinedValue();
private static NobaseValue null_value = new NullValue();
private static NobaseValue bool_value = new BooleanValue();
private static NobaseValue true_value = new BooleanValue(true);
private static NobaseValue false_value = new BooleanValue(false);
private static NobaseValue number_value = new NumberValue();
private static NobaseValue string_value = new StringValue();
private static NobaseValue array_value = new ArrayValue();
private static NobaseValue function_value = new FunctionValue(null);

private static NobaseValue any_value = new AnyValue();
private static NobaseValue unknown_value = new UnknownValue();




/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static NobaseValue createUndefined()		{ return undef_value; }

static NobaseValue createNull() 		{ return null_value; }

static NobaseValue createBoolean()		{ return bool_value; }

static NobaseValue createBoolean(boolean fg)
{
   return (fg ? true_value : false_value);
}

static NobaseValue createNumber()		{ return number_value; }

synchronized static NobaseValue createNumber(Number n)
{
   if (n == null) return createNumber();

   NobaseValue nv = value_map.get(n);
   if (nv == null) {
      nv = new NumberValue(n);
      value_map.put(n,nv);
    }
   return nv;
}

static NobaseValue createString()		{ return string_value; }

synchronized static NobaseValue createString(String s)
{
   if (s == null) return createString();

   NobaseValue nv = value_map.get(s);
   if (nv == null) {
      nv = new StringValue(s);
      value_map.put(s,nv);
    }
   return nv;
}


static NobaseValue createObject()		{ return new ObjectValue(); }

static NobaseValue createFunction()	        { return function_value; }

static NobaseValue createFunction(NobaseAst.FunctionConstructor fc) 
{ 
   if (fc == null) return createFunction();
   NobaseValue nv = value_map.get(fc);
   if (nv == null) {
      nv = new FunctionValue(fc);
      value_map.put(fc,nv);
    }
   return nv;
}

static NobaseValue createAnyValue()		{ return any_value; }

static NobaseValue createUnknownValue() 	{ return unknown_value; }

static NobaseValue createArrayValue()		{ return array_value; }

static NobaseValue createArrayValue(List<NobaseValue> v)
{
   return array_value;
}



static NobaseValue mergeValues(NobaseValue t1,NobaseValue t2)
{
   if (t1 == t2) return t1;

   if (t1 == any_value || t2 == any_value) return any_value;
   if (t1 == unknown_value) return t2;
   if (t2 == unknown_value) return t1;
   if (t1 == null) return t2;
   if (t2 == null) return t1;

   // create proper merge

   return createAnyValue();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected NobaseValue(NobaseType typ)
{
   value_type = typ;
   known_value = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/



NobaseType getType()					{ return value_type; }

void setBaseValue(NobaseValue typ)			{ }

boolean addProperty(Object prop,NobaseValue typ)		{ return false; }
void setHasOtherProperties()				{ }
NobaseValue getProperty(Object name)			{ return null; }
void mergeProperties(NobaseValue nv)                    { }

void addDefinition(NobaseAst.FunctionConstructor fc)	{ }

void setConstructor()					{ }


Object getKnownValue()					{ return known_value; }
boolean isKnown()					{ return known_value != null; }
boolean isAnyValue()					{ return false; }
boolean isFunction()                                    { return false; }



/********************************************************************************/
/*										*/
/*	Any Value								*/
/*										*/
/********************************************************************************/

private static class AnyValue extends NobaseValue {

   AnyValue() {
      super(NobaseType.createAnyType());
      known_value = KnownValue.ANY;
    }

   boolean isAnyValue() 				{ return true; }

}	// end of inner class AnyValue





/********************************************************************************/
/*										*/
/*	Unknown Value								*/
/*										*/
/********************************************************************************/

private static class UnknownValue extends NobaseValue {

   UnknownValue() {
      super(NobaseType.createAnyType());
      known_value = KnownValue.UNKNOWN;
    }

}	// end of inner class AnyValue





/********************************************************************************/
/*										*/
/*	Undefined type								*/
/*										*/
/********************************************************************************/

private static class UndefinedValue extends NobaseValue {

   UndefinedValue() {
      super(NobaseType.createUndefined());
      known_value = KnownValue.UNDEFINED;
    }

}	// end of inner class UndefinedValue



/********************************************************************************/
/*										*/
/*	Null type								*/
/*										*/
/********************************************************************************/

private static class NullValue extends NobaseValue {

   NullValue() {
      super(NobaseType.createNull());
      known_value = KnownValue.NULL;
    }

}	// end of inner class NullValue



/********************************************************************************/
/*										*/
/*	Boolean type								*/
/*										*/
/********************************************************************************/

private static class BooleanValue extends NobaseValue {

   BooleanValue() {
      super(NobaseType.createBoolean());
    }

   BooleanValue(boolean fg) {
      this();
      known_value = Boolean.valueOf(fg);
    }

}	// end of inner class BooleanValue



/********************************************************************************/
/*										*/
/*	String Value								*/
/*										*/
/********************************************************************************/

private static class StringValue extends NobaseValue {

   StringValue() {
      super(NobaseType.createString());
    }

   StringValue(String s) {
      this();
      known_value = s;
    }

}	// end of inner class StringValue



/********************************************************************************/
/*										*/
/*	Numeric Values								*/
/*										*/
/********************************************************************************/

private static class NumberValue extends NobaseValue {

   NumberValue() {
      super(NobaseType.createNumber());
    }

   NumberValue(Number n) {
      this();
      known_value = n;
    }

}	// end of inner class NumberValue



/********************************************************************************/
/*										*/
/*	Object Values								 */
/*										*/
/********************************************************************************/

private static class ObjectValue extends NobaseValue {

   private Map<Object,NobaseValue> known_properties;
   private ObjectValue base_value;
   private boolean has_other;

   ObjectValue() {
      super(NobaseType.createObject());
      known_properties = new HashMap<Object,NobaseValue>();
      base_value = null;
      has_other = false;
    }
   
   protected ObjectValue(NobaseType typ) {
      super(typ);
      known_properties = new HashMap<Object,NobaseValue>();
      base_value = null;
      has_other = false;
    }

   @Override void setBaseValue(NobaseValue typ) {
      if (typ instanceof ObjectValue) {
	 base_value = (ObjectValue) typ;
       }
    }

   @Override boolean addProperty(Object name,NobaseValue typ) {
      if (name == null) {
         if (!has_other) {
            setHasOtherProperties();
            return true;
          }
       }   
      else {
         NobaseValue otyp = known_properties.get(name);
         NobaseValue ntyp = mergeValues(typ,otyp);
         known_properties.put(name,ntyp);
         if (ntyp != otyp) return true;
       }
      return false;
    }

   @Override void setHasOtherProperties() {
      has_other = true;
    }

   @Override NobaseValue getProperty(Object name) {
      NobaseValue otyp = null;
      if (name != null) otyp = known_properties.get(name);
      if (otyp != null) return otyp;
      if (has_other) return createAnyValue();
      return null;
    }

   @Override void mergeProperties(NobaseValue nv) {
      if (nv == null || nv == this) return;
      if (nv instanceof ObjectValue) {
         ObjectValue ov = (ObjectValue) nv;
         for (Map.Entry<Object,NobaseValue> ent : ov.known_properties.entrySet()) {
            addProperty(ent.getKey(),ent.getValue());
          }
         if (ov.has_other) setHasOtherProperties();
       }
    }
   
}	// end of inner class ObjectValue




/********************************************************************************/
/*										*/
/*	Array value								*/
/*										*/
/********************************************************************************/

private static class ArrayValue extends NobaseValue {

   ArrayValue() {
      super(NobaseType.createList());
    }

}	// end of inner class ArrayValue




/********************************************************************************/
/*										*/
/*	Function value								*/
/*										*/
/********************************************************************************/

private static class FunctionValue extends ObjectValue {

   private Set<NobaseAst.FunctionConstructor> function_defs;

   FunctionValue(NobaseAst.FunctionConstructor fc) {
      super(NobaseType.createFunction());
      function_defs = new HashSet<NobaseAst.FunctionConstructor>();
      if (fc != null) function_defs.add(fc);
    }

   boolean isFunction()                         { return true; }
   
   @Override void addDefinition(NobaseAst.FunctionConstructor fc) {
      function_defs.add(fc);
    }

}	// end of inner class CompletionValue




}	// end of class NobaseValue




/* end of NobaseType.java */
