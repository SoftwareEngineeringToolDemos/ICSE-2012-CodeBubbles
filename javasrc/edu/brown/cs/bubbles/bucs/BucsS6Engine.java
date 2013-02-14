/********************************************************************************/
/*										*/
/*		BucsS6Engine.java						*/
/*										*/
/*	Interface to S6 for Bubbles Code Search 				*/
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



package edu.brown.cs.bubbles.bucs;


import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.batt.BattConstants.BattCallTest;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.lang.reflect.Modifier;
import java.net.*;


class BucsS6Engine implements BucsConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpLocation	bump_location;
private String		context_file;
private List<String>	search_keys;
private List<BattCallTest> test_cases;
private List<BattTest>	user_tests;
private List<BucsUserFile> data_files;
private String		test_code;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BucsS6Engine(BumpLocation loc)
{
   bump_location = loc;
   context_file = null;
   search_keys = null;
   test_cases = null;
   user_tests = null;
   test_code = null;
   data_files = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setKeywords(List<String> k)		{ search_keys = k; }

void setTestCases(List<BattCallTest> t) 	{ test_cases = t; }

void setTestCode(String cd)			{ test_code = cd; }

void setUserTest(List<BattTest> t)		{ user_tests = t; }

void setDataFiles(Collection<BucsUserFile> fl)		{ data_files = new ArrayList<BucsUserFile>(fl); }




/********************************************************************************/
/*										*/
/*	Context methods 							*/
/*										*/
/********************************************************************************/

void createSearchContext()
{
   Element e = BumpClient.getBump().getProjectData(bump_location.getProject(),false,true,false,false);
   if (e == null) return;

   List<File> classpaths = new ArrayList<File>();
   Element cpth = IvyXml.getChild(e,"CLASSPATH");

   for (Element pe : IvyXml.children(cpth,"PATH")) {
      String typ = IvyXml.getAttrString(pe,"TYPE");
      if (typ.equals("SOURCE")) continue;
      String onm = IvyXml.getTextElement(pe,"BINARY");
      if (onm == null) onm = IvyXml.getTextElement(pe,"OUTPUT");
      if (onm == null) continue;

      // skip standard java libraries
      if (onm.contains("/jdk") || onm.contains("\\jdk") || onm.contains("/jre") || onm.contains("\\jre")) continue;
      Element acc = IvyXml.getChild(pe,"ACCESS");
      if (acc != null) continue;

      File f = new File(onm);
      if (f.exists()) classpaths.add(f);
    }

   Manifest manifest = null;
   for (File f : classpaths) manifest = handleManifest(f,manifest);

   try {
      File tnm = File.createTempFile("bucscontext","jar");
      OutputStream ost = new BufferedOutputStream(new FileOutputStream(tnm));
      JarOutputStream jst = null;
      if (manifest == null) jst = new JarOutputStream(ost);
      else jst = new JarOutputStream(ost,manifest);

      for (File f : classpaths) addToClassContext(f,jst);

      if (data_files != null) {
	 for (BucsUserFile uf : data_files) addUserFile(uf,jst);
       }

      addSourceFile(jst);

      addContextFile(jst);

      jst.close();
      // send file to server and get remote name

      StringWriter sw = new StringWriter();
      sw.write("<FILE EMBED='FALSE' XML='TRUE'>\n");
      sw.write("<CONTENTS><![CDATA[");

      byte [] buf = new byte[8192];
      FileInputStream fis = new FileInputStream(tnm);
      for ( ; ; ) {
	 int rln = fis.read(buf);
	 if (rln <= 0) break;
	 for (int i = 0; i < rln; ++i) {
	    int v = buf[i] & 0xff;
	    String s1 = Integer.toHexString(v);
	    if (s1.length() == 1) sw.write("0");
	    sw.write(s1);
	  }
       }
      fis.close();
      sw.write("]]></CONTENTS>\n</FILE>\n");

      Element xml = sendMessageToS6(sw.toString());
      sw.close();

      if (xml != null) context_file = IvyXml.getText(xml);
    }
   catch (IOException ex) {
      BoardLog.logE("BUCS","Problem creating output file",ex);
      return;
    }
}



/********************************************************************************/
/*										*/
/*	Basic Search methods							*/
/*										*/
/********************************************************************************/

void startSearch(BucsSearchRequest sr)
{
   SearchRunner searcher = new SearchRunner(sr,createSearchRequest());

   BoardThreadPool.start(searcher);
}


private String createSearchRequest()
{
   Element sgn = checkSignature();
   if (sgn == null) return null;
   Element msgn = IvyXml.getChild(sgn,"METHOD");
   String methodname = IvyXml.getTextElement(msgn,"NAME");

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("SEARCH");
   xw.field("FORMAT","NONE");
   xw.field("LOCAL",false);
   xw.field("REMOTE",true);
   xw.field("OHLOH",true);
   xw.field("WHAT","METHOD");
   xw.writeXml(sgn);

   xw.begin("TESTS");
   int ctr = 0;
   if (test_cases != null) {
      for (BattCallTest bct : test_cases) {
	 xw.begin("TESTCASE");
	 xw.field("NAME","TEST_" + (++ctr));
	 xw.field("TYPE","CALLS");
	 xw.begin("CALL");
	 xw.field("METHOD",methodname);
	 xw.field("OP",bct.getTestOp());
	 xw.begin("INPUT");
	 xw.cdataElement("VALUE",bct.getTestInput());
	 xw.end("INPUT");
	 xw.begin("OUTPUT");
	 xw.cdataElement("VALUE",bct.getTestOutput());
	 xw.end("OUTPUT");
	 xw.end("CALL");
	 xw.end("TESTCASE");
      }
    }
   if (test_code != null) {
      xw.begin("TESTCASE");
      xw.field("NAME", "TEST_" + (++ctr));
      xw.field("TYPE","USERCODE");
      xw.cdataElement("CODE",test_code);
      xw.end("TESTCASE");
    }
   if (user_tests != null) {
      for (BattTest bt : user_tests) {
	 xw.begin("TESTCASE");
	 xw.field("NAME",bt.getName());
	 xw.field("TYPE","JUNIT");
	 xw.field("CLASS",bt.getClassName());
	 xw.field("METHOD",bt.getMethodName());
	 xw.field("TESTNAME",bt.getMethodName() + "(" + bt.getClassName() + ")");
	 xw.end("TESTCASE");
       }
    }
   xw.end("TESTS");

   // output context here

   xw.begin("KEYWORDS");
   for (String k : search_keys) {
      xw.cdataElement("KEYWORD",k);
    }
   xw.end("KEYWORDS");

   if (context_file != null) {
      xw.begin("CONTEXT");
      xw.field("FILE",context_file);
      xw.end("CONTEXT");
    }

   xw.end("SEARCH");

   String rslt = xw.toString();
   xw.close();

   return rslt;
}




private class SearchRunner implements Runnable {

   private String search_request;
   private BucsSearchRequest search_callback;

   SearchRunner(BucsSearchRequest sr,String rq) {
      search_callback = sr;
      search_request = rq;
    }

   @Override public void run() {
      if (search_request == null) {
	 search_callback.handleSearchFailed();
	 return;
       }
      Element rslt = sendMessageToS6(search_request);

      List<BucsSearchResult> rslts = new ArrayList<BucsSearchResult>();
      Element sols = IvyXml.getChild(rslt,"SOLUTIONS");
      for (Element sol : IvyXml.children(sols,"SOLUTION")) {
	 S6SearchResult sr = new S6SearchResult(sol);
	 rslts.add(sr);
       }

      if (rslts.size() == 0) search_callback.handleSearchFailed();
      else search_callback.handleSearchSucceeded(rslts);
    }

}	// end of inner class SearchRunner






private Element checkSignature()
{
   String snm = bump_location.getSymbolName();
   String pnm = bump_location.getParameters();
   String pfx = "";
   if (Modifier.isStatic(bump_location.getModifiers())) pfx = "static ";
   int idx = snm.lastIndexOf(".");
   if (idx > 0) snm = snm.substring(idx+1);
   String ret = bump_location.getReturnType();

   String sgn = pfx + ret + " " + snm + pnm;

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("CHECK");
   xw.field("WHAT","METHOD");
   xw.cdataElement("METHOD",sgn);
   // add context here
   xw.end("CHECK");

   Element rslt = sendMessageToS6(xw.toString());
   xw.close();

   if (rslt == null) return rslt;

   return IvyXml.getChild(rslt,"SIGNATURE");
}




private Element sendMessageToS6(String cnts)
{
   byte [] cntb = cnts.getBytes();

   Element rslt = null;

   try {
      URL u = new URL("http://conifer.cs.brown.edu/s6web/dosearch.php");
      HttpURLConnection huc = (HttpURLConnection) u.openConnection();
      huc.setDoInput(true);
      huc.setDoOutput(true);
      huc.setUseCaches(false);
      huc.setRequestMethod("POST");
      huc.setRequestProperty("Accept","application/xml");
      huc.setRequestProperty("Content-Length",Integer.toString(cntb.length));
      huc.setRequestProperty("Content-Type","text/xml");
      huc.connect();
      OutputStream ots = huc.getOutputStream();
      ots.write(cntb);
      ots.close();
      InputStream ins = huc.getInputStream();
      rslt = IvyXml.loadXmlFromStream(ins);
      ins.close();
    }
   catch (IOException e) {
      return null;
    }

   if (!IvyXml.isElement(rslt,"RESULT")) return null;

   return rslt;
}



/********************************************************************************/
/*										*/
/*	License methods 							*/
/*										*/
/********************************************************************************/

String getLicense(String uid)
{
   if (uid == null) return null;

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("CHECK");
   xw.field("WHAT","LICENSE");
   xw.field("UID",uid);
   xw.end("CHECK");
   String rqst = xw.toString();
   xw.close();

   Element e = sendMessageToS6(rqst);
   if (e == null) return null;
   Element ue = IvyXml.getChild(e,"LICENSE");

   return IvyXml.getTextElement(ue,"TEXT");
}




/********************************************************************************/
/*										*/
/*	Context Creation Helper methods 					*/
/*										*/
/********************************************************************************/

private void addContextFile(JarOutputStream jst) throws IOException
{
   ZipEntry ze = new ZipEntry("S6.CONTEXT");
   jst.putNextEntry(ze);


   IvyXmlWriter xw = new IvyXmlWriter(jst);
   xw.begin("CONTEXT");
   xw.field("LANGUAGE","JAVA");
   xw.field("USEPATH",true);
   xw.field("SEPARATOR",File.separator);

   String mnm = bump_location.getSymbolName();
   int idx = mnm.lastIndexOf(".");
   String cnm = null;
   String pnm = null;
   if (idx > 0) {
      cnm = mnm.substring(0,idx);
      mnm = mnm.substring(idx+1);
      idx = cnm.lastIndexOf(".");
      if (idx > 0) {
	 pnm = cnm.substring(0,idx);
	 cnm = cnm.substring(idx+1);
       }
    }
   if (pnm != null) xw.field("PACKAGE",pnm);
   if (cnm != null) xw.field("CLASS",cnm);

   // output imports ?

   if (data_files != null) {
      for (BucsUserFile uf : data_files) uf.addEntry(xw);
    }

   xw.end("CONTEXT");
   xw.flush();
   jst.closeEntry();
}




private Manifest handleManifest(File f,Manifest m)
{
   if (!f.exists() || !f.canRead()) return m;
   if (f.isDirectory()) return m;

   try {
      JarFile jf = new JarFile(f);
      Manifest m1 = jf.getManifest();
      if (m1 != null) m = mergeManifest(m1,m);
      jf.close();
    }
   catch (IOException e) {
      BoardLog.logE("BUCS","Java file must be a directory or a jar file");
    }

   return m;
}



private Manifest mergeManifest(Manifest m0,Manifest m1)
{
   if (m0 == null) return m1;
   if (m1 == null) m1 = new Manifest();

   Attributes na = m0.getMainAttributes();
   Attributes a = m1.getMainAttributes();
   a.putAll(na);
   Map<String,Attributes> nm = m0.getEntries();
   Map<String,Attributes> mm = m1.getEntries();
   for (Map.Entry<String,Attributes> ent : nm.entrySet()) {
      Attributes ma = mm.get(ent.getKey());
      if (ma == null) mm.put(ent.getKey(),ent.getValue());
      else ma.putAll(ent.getValue());
    }

   return m1;
}



private void addToClassContext(File f,JarOutputStream jst) throws IOException
{
   if (!f.exists() || !f.canRead()) return;
   if (f.isDirectory()) {
      addDirectoryClassFiles(f,f.getPath(),jst);
    }
   else {
      JarFile jf = new JarFile(f);
      addJarFile(jf,jst);
    }
}



private void addDirectoryClassFiles(File dir,String pfx,JarOutputStream jst) throws IOException
{
   if (dir.isDirectory()) {
      File [] dirf = dir.listFiles();
      if (dirf != null) {
	 for (File f : dirf) addDirectoryClassFiles(f,pfx,jst);
       }
    }
   else if (dir.getPath().endsWith(".class")) addSimpleFile(dir,pfx,jst);
}



private void addSimpleFile(File f,String pfx,JarOutputStream jst) throws IOException
{
   String x = f.getPath();
   if (pfx != null && x.startsWith(pfx)) {
      int i = pfx.length();
      x = x.substring(i);
      if (x.startsWith(File.separator)) x = x.substring(1);
    }

   addToJarFile(f,x,jst);
}



private void addJarFile(JarFile jf,JarOutputStream jst) throws IOException
{
   for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
      ZipEntry je = e.nextElement();
      if (je.getName().equals("META-INF/MANIFEST.MF")) continue;

      BufferedInputStream ins = new BufferedInputStream(jf.getInputStream(je));
      addToJarFile(ins,je.getName(),jst);
    }
}


private void addSourceFile(JarOutputStream jst) throws IOException
{
   File f = bump_location.getFile();
   if (f != null && f.exists()) addToJarFile(f,"S6.SOURCE",jst);
}



private void addToJarFile(File f,String jnm,JarOutputStream jst) throws IOException
{
   if (!f.exists() || !f.canRead()) return;
   BufferedInputStream ins = new BufferedInputStream(new FileInputStream(f));

   addToJarFile(ins,jnm,jst);
}



private void addToJarFile(InputStream ins,String jnm,JarOutputStream jst) throws IOException
{
   byte [] buf = new byte[16384];

   ZipEntry ze = new ZipEntry(jnm);
   try {
      jst.putNextEntry(ze);
    }
   catch (ZipException ex) {
      ins.close();
      return;
    }

   for ( ; ; ) {
      int ln = ins.read(buf);
      if (ln <= 0) break;
      jst.write(buf,0,ln);
    }
   ins.close();
   jst.flush();
   jst.closeEntry();
}



/********************************************************************************/
/*										*/
/*	User Data files 							*/
/*										*/
/********************************************************************************/

private void addUserFile(BucsUserFile uf,JarOutputStream jst) throws IOException
{
   addToJarFile(uf.getFile(),uf.getJarName(),jst);
}



/********************************************************************************/
/*										*/
/*	Search result holder							*/
/*										*/
/********************************************************************************/

private static class S6SearchResult implements BucsSearchResult {

   private String result_name;
   private String result_source;
   private String result_code;
   private int	  result_lines;
   private int	  result_size;
   private String result_license;

   S6SearchResult(Element xml) {
      result_name = IvyXml.getTextElement(xml,"NAME");
      result_source = IvyXml.getTextElement(xml,"SOLSRC");
      result_code = IvyXml.getTextElement(xml,"CODE");
      result_license = IvyXml.getTextElement(xml,"LICENSE");
      Element comp = IvyXml.getChild(xml,"COMPLEXITY");
      result_lines = IvyXml.getAttrInt(comp,"LINES");
      result_size = IvyXml.getAttrInt(comp,"CODE");
      // result_time = IvyXml.getAttrDouble(comp,"TESTTIME");
    }

   @Override public String getResultName()	{ return result_name; }
   @Override public String getCode()		{ return result_code; }
   @Override public String getSource()		{ return result_source; }
   @Override public int getNumLines()		{ return result_lines; }
   @Override public int getCodeSize()		{ return result_size; }
   @Override public String getLicenseUid()	{ return result_license; }

}	// end of inner class SearchResult

}	// end of class BucsS6Engine




/* end of BucsS6Engine.java */
