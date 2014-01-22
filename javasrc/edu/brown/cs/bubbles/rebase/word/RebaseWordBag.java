/********************************************************************************/
/*										*/
/*		RebaseWordBag.java						*/
/*										*/
/*	Class to hold a bag of words and counts 				*/
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



package edu.brown.cs.bubbles.rebase.word;

import java.util.*;
import java.util.zip.*;
import java.io.*;


public class RebaseWordBag implements RebaseWordConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,Count>		word_table;
private double				total_squared;
private int                             total_documents;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public RebaseWordBag()
{
   word_table = new HashMap<String,Count>();
   total_squared = 0;
   total_documents = 0;
}


public RebaseWordBag(String text)
{
   this();

   addWords(text);
   total_documents = 1;
}


public RebaseWordBag(RebaseWordBag bag)
{
   this();
   
   addWords(bag);
   total_documents = bag.total_documents;
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public void addWords(String text)
{
   if (text == null) return;

   RebaseWordStemmer stm = new RebaseWordStemmer();
   int ln = text.length();
   for (int i = 0; i < ln; ++i) {
      char ch = text.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
         boolean havealpha = Character.isAlphabetic(ch);
	 int start = i;
	 while (Character.isJavaIdentifierPart(ch)) {
            havealpha |= Character.isAlphabetic(ch);
            if (++i >= ln) break;
            ch = text.charAt(i);
	  }
	 if (havealpha) {
            Collection<String> wds =  RebaseWordFactory.getCandidateWords(stm,text,start,i-start);
            if (wds != null) {
               for (String wd : wds) { 
                  addWord(wd);
                }
             }
            // addCandidates(stm,text,start,i-start);
          }
       }
    }
   
   ++total_documents;
}



/********************************************************************************/
/*										*/
/*	Augment the word bag							*/
/*										*/
/********************************************************************************/



public void addWords(RebaseWordBag bag)
{
   if (bag == null) return;
   for (Map.Entry<String,Count> ent : bag.word_table.entrySet()) {
      String s = ent.getKey();
      addWord(s,ent.getValue());
    }
   total_documents += bag.total_documents;
}


private void addWord(String s)
{
   Count c = word_table.get(s);
   if (c == null) {
      c = new Count();
      word_table.put(s,c);
    }
   int ct = c.incr();
   total_squared += ct*ct - (ct-1)*(ct-1);
}


private void addWord(String s,Count ct)
{
   Count c = word_table.get(s);
   if (c == null) {
      c = new Count();
      word_table.put(s,c);
    }
   int oct = c.getCount();
   int nct = c.add(ct);
   total_squared += nct*nct - oct*oct;
}



/********************************************************************************/
/*										*/
/*	Other word bag operations						*/
/*										*/
/********************************************************************************/

public void removeWords(RebaseWordBag bag)
{
   for (Map.Entry<String,Count> ent : bag.word_table.entrySet()) {
      String wd = ent.getKey();
      Count ct = word_table.get(wd);
      if (ct == null) continue;
      int oct = ent.getValue().getCount();
      int odct = ent.getValue().getDocCount();
      
      int nct = ct.getCount();
      total_squared -= nct*nct;
      
      int xct = ct.decr(oct,odct);
      if (xct == 0) word_table.remove(wd);
      else {
	 total_squared += xct*xct;
       }
    }
   total_documents -= bag.total_documents;
}



public double cosine(RebaseWordBag b2)
{
   RebaseWordBag b1 = this;
   // ensure b2 is the smaller set
   if (b1.word_table.size() < b2.word_table.size()) {
      RebaseWordBag bt = b1;
      b1 = b2;
      b2 = bt;
    }

   double norm1 = Math.sqrt(b1.total_squared);
   double norm2 = Math.sqrt(b2.total_squared);

   double cos = 0;
   for (Map.Entry<String,Count> ent : b2.word_table.entrySet()) {
      String wd = ent.getKey();
      Count c2 = ent.getValue();
      Count c1 = b1.word_table.get(wd);
      if (c1 == null || c2 == null) continue;
      double v0 = c2.getCount() / norm2;
      double v1 = c1.getCount() / norm1;
      cos += v0*v1;
    }

   return cos;
}




/********************************************************************************/
/*										*/
/*	Bag I/O methods 							*/
/*										*/
/********************************************************************************/

public void outputBag(File f) throws IOException
{
   PrintStream pw = null;
   ZipOutputStream zip = null;

   if (f.getPath().endsWith(".zip")) {
      zip = new ZipOutputStream(new FileOutputStream(f));
      zip.putNextEntry(new ZipEntry("wordbag"));
      pw = new PrintStream(zip);
    }
   else {
      pw = new PrintStream(new FileOutputStream(f));
    }
   pw.println(total_documents);
   for (Map.Entry<String,Count> ent : word_table.entrySet()) {
      Count c = ent.getValue();
      pw.println(ent.getKey() + "," + c.getCount() + "," + c.getDocCount());
    }

   pw.close();
}



public void inputBag(File f) throws IOException
{
   BufferedReader br = null;
   ZipInputStream zin = null;
   
   if (f.getPath().endsWith(".zip")) {
      zin = new ZipInputStream(new FileInputStream(f));
      zin.getNextEntry();
      br = new BufferedReader(new InputStreamReader(zin));
    }
   else {
      br = new BufferedReader(new FileReader(f));
    }

   Scanner scn = new Scanner(br);
   scn.useDelimiter("\\n|,");
   if (scn.hasNextInt()) total_documents = scn.nextInt();
   while (scn.hasNext()) {
      String wd = scn.next();
      int wct = 0;
      int dct = 0;
      if (scn.hasNextInt()) wct = scn.nextInt();
      if (scn.hasNextInt()) {
         int oct = 0;
         dct = scn.nextInt();
         Count c = word_table.get(wd);
         if (c == null) {
            word_table.put(wd,new Count(wct,dct));
          }
         else {
            oct = c.getCount();
            wct = c.add(wct,dct);
          }
         total_squared += wct*wct - oct*oct;
       }
    }

   scn.close();
   
}



/********************************************************************************/
/*										*/
/*	Class to hold changeable counts 					*/
/*										*/
/********************************************************************************/

private static class Count {

   private int count_value;
   private int num_document;

   Count()				{ count_value = num_document = 0; }
   Count(int ct,int dct)		{ count_value = ct; num_document = dct; }

   int incr() {
      if (num_document == 0) num_document = 1;
      return ++count_value;
    }
   int add(int ct,int dct) {
      count_value += ct;
      num_document += dct;
      return count_value;
    }
   int getCount()			{ return count_value; }
   int getDocCount()                    { return num_document; }
   int decr(int ct,int dct) { 
      count_value -= ct;
      num_document -= dct;
      if (count_value < 0) count_value = 0;
      if (num_document < 0) num_document = 0;
      return count_value;
    }

   int add(Count c) {
      if (c != null) {
         count_value += c.count_value;
         num_document += c.num_document;
       }  
      return count_value;
    }

}	// end of inner class Count



}	// end of class RebaseWordBag




/* end of RebaseWordBag.java */

