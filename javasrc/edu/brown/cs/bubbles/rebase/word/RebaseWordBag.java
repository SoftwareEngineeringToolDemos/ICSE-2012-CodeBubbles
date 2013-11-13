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

import edu.brown.cs.bubbles.rebase.*;

import java.util.*;
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



private static Set<String>		stop_words;
private static Map<String,String>	short_words;
private static Set<String>		dictionary_words;

static {
   stop_words = new HashSet<String>();
   String wds = "a,able,about,across,after,all,almost,also,am,among,an,and,any,are,as,at," +
      "be,because,been,but,by,can,cannot,could,dear,did,do,does,either,else,ever,every," +
      "for,from,get,got,had,has,have,he,her,hers,him,his,how,however,i,if,in,into,is,it,its," +
      "just,least,let,like,likely,may,me,might,most,must,my,neither,no,nor,not," +
      "of,off,often,on,only,or,other,our,own,rather,said,say,says,she,should,since,so,some," +
      "than,that,the,their,them,then,there,these,they,this,tis,to,too,twas,us," +
      "wants,was,we,were,what,when,where,which,while,who,whom,why,will,with,would," +
      "yet,you,your";
   String keys = "abstract,break,boolean,byte,case,catch,char,class,const,continue," +
      "default,do,double,else,enum,extends,false,final,finally,float,for,goto,if," +
      "implements,import,instanceof,int,interface,long,native,new,null,package,private," +
      "protected,public,return,short,static,super,switch,synchronized,this,throw,throws," +
      "true,try,void,while,java,com,org,javax";

   for (StringTokenizer tok = new StringTokenizer(wds," ,"); tok.hasMoreTokens(); ) {
      stop_words.add(tok.nextToken());
    }
   for (StringTokenizer tok = new StringTokenizer(keys," ,"); tok.hasMoreTokens(); ) {
      stop_words.add(tok.nextToken());
    }

   createShortWordSet();
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public RebaseWordBag()
{
   word_table = new HashMap<String,Count>();
   total_squared = 0;
}


public RebaseWordBag(String text)
{
   this();

   addWords(text);
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
      if (Character.isAlphabetic(ch)) {
	 int start = i;
	 while (Character.isAlphabetic(ch)) {
	    if (++i >= ln) break;
	    ch = text.charAt(i);
	  }
	 addCandidates(stm,text,start,i-start);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Process potential words 						*/
/*										*/
/********************************************************************************/

private void addCandidates(RebaseWordStemmer stm,String text,int off,int len)
{
   if (len < 3 || len > 32) return;
   int [] breaks = new int[32];
   int breakct = 0;

   // try to break the word up using camelCase
   boolean islower = false;
   for (int i = 0; i < len; ++i) {
      char ch = text.charAt(off+i);
      if (Character.isUpperCase(ch)) {
	 if (islower) breaks[breakct++] = i;
	 islower = false;
       }
      else islower = true;
    }

   // first use whole word
   addCandidate(stm,text,off,len);
   if (breakct > 0) {
      int lbrk = 0;
      for (int i = 0; i < breakct; ++i) {
	 addCandidate(stm,text,off+lbrk,breaks[i]-lbrk);
	 lbrk = breaks[i];
       }
      addCandidates(stm,text,off+lbrk,len-lbrk);
    }
}


private void addCandidate(RebaseWordStemmer stm,String text,int off,int len)
{
   String wd0 = text.substring(off,off+len).toLowerCase();
   addCandidateWord(wd0);

   for (int i = 0; i < len; ++i) {
      stm.add(text.charAt(off+i));
    }
   String wd = stm.stem();    // stem and convert to lower case

   if (dictionary_words.contains(wd) && !wd0.equals(wd)) {
      System.err.println("STEM " + wd0 + " => " + wd);
      addCandidateWord(wd);
    }
}



private void addCandidateWord(String wd)
{
   if (stop_words.contains(wd)) return;

   addWord(wd);

   String nwd = short_words.get(wd);
   if (nwd != null) addWord(nwd);
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
      int nct = ct.getCount();
      if (oct > nct) oct = nct;
      total_squared -= nct*nct;
      if (oct == nct) word_table.remove(wd);
      else {
	 int xct = ct.decr(oct);
	 total_squared += xct*xct;
       }
    }
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
   PrintWriter pw = new PrintWriter(new FileWriter(f));
   for (Map.Entry<String,Count> ent : word_table.entrySet()) {
      pw.println(ent.getKey() + "," + ent.getValue().getCount());
    }
   pw.close();
}



public void inputBag(File f) throws IOException
{
   BufferedReader br = new BufferedReader(new FileReader(f));
   for ( ; ; ) {
      String ln = br.readLine();
      if (ln == null) break;
      int idx = ln.indexOf(",");
      String wd = ln.substring(0,idx);
      int ct = Integer.parseInt(ln.substring(idx+1));
      Count c = word_table.get(wd);
      int oct = 0;
      if (c == null) word_table.put(wd,new Count(ct));
      else {
	 oct = c.getCount();
	 ct = c.add(ct);
       }
      total_squared += ct*ct - oct*oct;
    }
   br.close();
}



/********************************************************************************/
/*										*/
/*	Class to hold changeable counts 					*/
/*										*/
/********************************************************************************/

private static class Count {

   private int count_value;

   Count()				{ count_value = 0; }
   Count(int ct)			{ count_value = ct; }

   int incr()				{ return ++count_value; }
   int add(int ct)			{ count_value += ct; return count_value; }
   int getCount()			{ return count_value; }
   int decr(int ct)			{ count_value -= ct; return count_value; }

   int add(Count c) {
      if (c != null) count_value += c.count_value;
      return count_value;
    }

}	// end of inner class Count



/********************************************************************************/
/*										*/
/*	Create programmer abbreviations of common words 			*/
/*										*/
/********************************************************************************/

private static void createShortWordSet()
{
   dictionary_words = new HashSet<String>();
   short_words = new HashMap<String,String>();

   String root = System.getProperty("edu.brown.cs.bubbles.rebase.ROOT");
   File f1 = new File(root);
   File f2 = new File(f1,"lib");
   File f = new File(f2,WORD_LIST_FILE);

   try {
      BufferedReader br = new BufferedReader(new FileReader(f));
      for ( ; ; ) {
	 String wd = br.readLine();
	 if (wd == null) break;
	 if (wd.contains("'") || wd.contains("-")) continue;
	 if (wd.length() < 3 || wd.length() > 24) continue;
	 wd = wd.toLowerCase();
	 dictionary_words.add(wd);
	 String nwd = wd.replaceAll("[aeiou]","");
	 if (!nwd.equals(wd)) short_words.put(nwd,wd);
       }
      br.close();
    }
   catch (IOException e) {
      RebaseMain.logE("Problem reading word file",e);
    }
}



}	// end of class RebaseWordBag




/* end of RebaseWordBag.java */

