/********************************************************************************/
/*										*/
/*		BuenoProperties.java						*/
/*										*/
/*	BUbbles Environment New Objects creator property handling		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.bump.BumpClient;

import java.util.*;


public class BuenoProperties extends HashMap<BuenoConstants.BuenoKey,Object> implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoProperties()
{
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   bp.put(BuenoKey.KEY_AUTHOR,System.getProperty("user.name"));
}



/********************************************************************************/
/*										*/
/*	Local access methods							*/
/*										*/
/********************************************************************************/

String getStringProperty(BuenoKey k)
{
   Object v = get(k);
   if (v == null) return null;

   return v.toString();
}



boolean getBooleanProperty(BuenoKey k)
{
   Object v = get(k);
   if (v == null) return false;

   if (v instanceof Boolean) return ((Boolean) v);

   return true;
}



int getModifiers()
{
   Object v = get(BuenoKey.KEY_MODIFIERS);
   if (v == null) return 0;
   if (v instanceof Integer) return ((Integer) v);

   // allow string or set here and decode

   return 0;
}



String [] getParameters()		{ return getArrayProperty(BuenoKey.KEY_PARAMETERS); }
String [] getImplements()		{ return getArrayProperty(BuenoKey.KEY_IMPLEMENTS); }
String [] getThrows()			{ return getArrayProperty(BuenoKey.KEY_THROWS); }
String [] getImports()			{ return getArrayProperty(BuenoKey.KEY_IMPORTS); }


String [] getArrayProperty(BuenoKey k)
{
   Object v = get(k);
   if (v == null) return null;

   if (v instanceof String []) return ((String []) v);

   if (v instanceof String) {
      StringTokenizer tok = new StringTokenizer((String) v,",");
      String [] rslt = new String[tok.countTokens()];
      int idx = 0;
      while (tok.hasMoreTokens()) {
	 rslt[idx++] = tok.nextToken();
       }
      return rslt;
    }

   if (v instanceof List<?>) {
      List<?> l = (List<?>) v;
      String [] rslt = new String[l.size()];
      int idx = 0;
      for (Object o : l) {
	 rslt[idx++] = o.toString();
       }
      return rslt;
    }

   return null;
}



public void addToArrayProperty(BuenoKey k,String v)
{
   List<String> rslt = null;
   Object ov = get(k);
   if (ov == null) {
      rslt = new ArrayList<String>();
    }
   else if (ov instanceof String []) {
      rslt = new ArrayList<String>();
      String [] xv = (String []) ov;
      for (String s : xv) rslt.add(s);
    }
   else if (ov instanceof String) {
      rslt = new ArrayList<String>();
      StringTokenizer tok = new StringTokenizer((String) ov,",");
      while (tok.hasMoreTokens()) {
	 rslt.add(tok.nextToken());
       }
    }
   else if (ov instanceof List) {
      @SuppressWarnings("unchecked") List<String> l = (List<String>) ov;
      l.add(v);
    }
   if (rslt != null) {
      rslt.add(v);
      put(k,v);
    }
}




String getIndentString()		{ return getIndentProperty(BuenoKey.KEY_INDENT,-1); }
String getInitialIndentString() 	{ return getIndentProperty(BuenoKey.KEY_INITIAL_INDENT,0); }

String getIndentProperty(BuenoKey k,int dflt)
{
   Object v = get(k);

   if (v != null && v instanceof String) return (String) v;

   int idx = 0;
   if (v instanceof Integer) idx = ((Integer) v);
   else if (dflt >= 0) idx = dflt;
   else {
      idx = BumpClient.getBump().getOptionInt("org.eclipse.jdt.core.formatter.indentation.size");
    }

   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < idx; ++i) buf.append(" ");
   return buf.toString();
}










}	// end of class BuenoProperties




/* end of BuenoProperties.java */
