/********************************************************************************/
/*										*/
/*		BattJUnit.java							*/
/*										*/
/*	Bubble Automated Testing Tool junit main program			*/
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


/* SVN: $Id$ */


package edu.brown.cs.bubbles.batt;


import org.junit.runner.*;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestClass;

import javax.xml.stream.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.security.Permission;
import java.util.*;


public class BattJUnit implements BattConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BattJUnit bj = new BattJUnit(args);

   bj.process();
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	list_only;
private String		result_file;
private OutputStream	result_stream;
private Class<?> []	class_set;
private Map<Description,JunitTest> test_cases;

private static final JunitTestStatus STATUS_RUNNING;
private static final JunitTestStatus STATUS_UNKNOWN;
private static final JunitTestStatus STATUS_SUCCESS;
private static final JunitTestStatus STATUS_IGNORED;
private static final JunitTestStatus STATUS_LISTING;


enum StatusType {
   UNKNOWN,
   LISTING,
   IGNORED,
   RUNNING,
   FAILURE,
   SUCCESS
}

static {
   STATUS_RUNNING = new JunitTestStatus(StatusType.RUNNING);
   STATUS_UNKNOWN = new JunitTestStatus(StatusType.UNKNOWN);
   STATUS_LISTING = new JunitTestStatus(StatusType.LISTING);
   STATUS_SUCCESS = new JunitTestStatus(StatusType.SUCCESS);
   STATUS_IGNORED = new JunitTestStatus(StatusType.IGNORED);
}


private static Set<String> bad_messages;

static {
   bad_messages = new HashSet<String>();
   bad_messages.add("No runnable methods");
   bad_messages.add("Test class should have exactly one public constructor");
   bad_messages.add("Test class can only have one constructor");
   bad_messages.add("Test class should have exactly one public zero-argument constructor");
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BattJUnit(String [] args)
{
   list_only = false;
   class_set = null;
   result_file = "batt.out";
   result_stream = null;
   test_cases = new HashMap<Description,JunitTest>();

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   List<String> clsstr = new ArrayList<String>();
   List<String> tststr = new ArrayList<String>();
   List<Class<?>> clss = new ArrayList<Class<?>>();

   boolean havecls = false;
   boolean useall = false;

   for (int i = 0; i < args.length; ++i) {
      if (!havecls && args[i].startsWith("-")) {
	 if (args[i].startsWith("-l")) {                                // -list
	    list_only = true;
	  }
	 else if (args[i].startsWith("-o") && i+1 < args.length) {      // -o <output>
	    result_file = args[++i];
	  }
	 else if (args[i].startsWith("-s") && i+1 < args.length) {      // -s port@host
	    result_file = null;
	    setupSocket(args[++i]);
	  }
	 else if (args[i].startsWith("-a")) {                           // -all
	    useall = true;
	  }
	 else badArgs();
       }
      else {
	 havecls = true;
	 String clsnm = args[i];
	 if (clsnm.startsWith("@")) {
	    clsnm = clsnm.substring(1);
	    tststr.add(clsnm);
	  }
	 else if (useall) tststr.add(clsnm);

	 clsstr.add(clsnm);
       }

    }

   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleUserClasses",String [].class);
      String [] strarr = new String[clsstr.size()];
      strarr = clsstr.toArray(strarr);
      mac.invoke(null,(Object) strarr);
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATT: Problem with agent: " + t);
      t.printStackTrace();
    }

   for (String cnm : tststr) {
      System.err.println("BATT: Work on " + cnm);
      System.err.flush();
      try {
	 // TODO: if this can be done without actually calling the static initializer
	 // it would be better
	 Class<?> c = Class.forName(cnm);
	 new TestClass(c);
	 c.getConstructor();
	 clss.add(c);
       }
      catch (AssertionError e) {
	 // System.err.println("Assertion error: " + e);
       }
      catch (IllegalArgumentException e) {
	 // System.err.println("Arg exception: " + e);
	 // e.printStackTrace();
       }
      catch (ExceptionInInitializerError e) {
	 // System.err.println("Init exception: " + e);
       }
      catch (NoSuchMethodException e) {
	 // System.err.println("No construtor: " + e);
       }
      catch (NoClassDefFoundError e) {
	 System.err.println("BATT: Class " + cnm + " not found");
       }
      catch (ClassNotFoundException e) {
	 System.err.println("BATT: Class " + cnm + " not found");
       }
      catch (Throwable t) {
	 System.err.println("BATT: Class " + cnm + " can't be loaded: " + t);
       }

      System.err.println("DONE: " + cnm);
    }

   class_set = new Class<?>[clss.size()];
   class_set = clss.toArray(class_set);

   if (result_stream == null && result_file != null) {
      try {
	 result_stream = new FileOutputStream(result_file);
       }
      catch (IOException e) {
	 System.err.println("BATT: Couldn't open output file: " + e);
	 System.exit(1);
       }
    }

}



private void setupSocket(String ph)
{
   int idx = ph.indexOf('@');
   String host = "127.0.0.1";
   int port = -1;
   if (idx > 0) {
      host = ph.substring(idx+1);
      host = fixHost(host);
      ph = ph.substring(0,idx);
    }
   try {
      port = Integer.parseInt(ph);
    }
   catch (NumberFormatException e) { }

   try {
      @SuppressWarnings("resource")
      Socket s = new Socket(host,port);
      result_stream = s.getOutputStream();
    }
   catch (IOException e) {
      System.err.println("BATT: Problem connecting to socket " + port + " @" + host + ": " + e);
      System.exit(1);
    }
}



private static String fixHost(String h)
{
   if (h == null) return null;

   try {
      String h1 = InetAddress.getLocalHost().getHostName();
      String h2 = InetAddress.getLocalHost().getHostAddress();
      String h3 = InetAddress.getLocalHost().getCanonicalHostName();

      if (h.equals(h1) || h.equals(h2) || h.equals(h3)) {
	 return "127.0.0.1";
       }
   }
   catch (UnknownHostException e) { }

   return h;
}




private void badArgs()
{
   System.err.println("BATT: battjunit [-list] [-o output] class...");
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   Request rq = null;
   rq = Request.classes(class_set);

   JUnitCore juc = new JUnitCore();

   if (list_only) {
      rq = rq.filterWith(new ListFilter());
    }

   TestListener ll = new TestListener();
   juc.addListener(ll);

   System.err.println("BATTJ: START RUN: " + list_only);

   juc.run(rq);

   System.err.println("BATTJ: FINISH RUN: " + list_only);

   if (result_stream != null) {
      try {
	 result_stream.close();
       }
      catch (IOException e) { }
    }

// System.exit(0);
   Runtime.getRuntime().halt(0);
}



/********************************************************************************/
/*										*/
/*	Test case maintenance methods						*/
/*										*/
/********************************************************************************/

synchronized JunitTest addTestCase(Description d,JunitTestStatus sts)
{
   JunitTest btc = test_cases.get(d);
   if (btc == null) {
      btc = new JunitTest(d);
      System.err.println("BATT: Create new test case for " + d + " " + test_cases.size());
      test_cases.put(d,btc);
    }
   btc.setStatus(sts);

   for (Description d1 : d.getChildren()) {
      addTestCase(d1,sts);
    }

   return btc;
}



synchronized void removeTestCase(Description d)
{
   System.err.println("BATT: Remove test case " + d);

   test_cases.remove(d);
}


void setTestStatus(Description d,JunitTestStatus sts)
{
   JunitTest btc = test_cases.get(d);
   if (btc != null) btc.setStatus(sts);
}



JunitTestStatus getTestStatus(Description d)
{
   JunitTest btc = test_cases.get(d);
   if (btc == null) return STATUS_UNKNOWN;
   return btc.getStatus();
}



void noteStart(Description d)
{
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleStartTest",String.class);
      mac.invoke(null,d.toString());
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATT: Problem with agent: " + t);
      t.printStackTrace();
    }
}



void noteFinish(Description d)
{
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleFinishTest",String.class);
      mac.invoke(null,d.toString());
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATT: Problem with agent: " + t);
      t.printStackTrace();
    }
}



void noteDone()
{
   try {
      Class<?> ac = Class.forName("edu.brown.cs.bubbles.batt.BattAgent");
      Method mac = ac.getMethod("handleFinishRun");
      mac.invoke(null);
    }
   catch (ClassNotFoundException e) { }
   catch (Throwable t) {
      System.err.println("BATT: Problem with agent: " + t);
      t.printStackTrace();
    }
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

private void outputSingleTest(JunitTest jt)
{
   XMLStreamWriter xw = null;
   try {
      XMLOutputFactory xof = XMLOutputFactory.newInstance();
      xw = xof.createXMLStreamWriter(result_stream);
      outputTestCase(jt,xw);
      xw.flush();
    }
   catch (XMLStreamException e) {
      System.err.println("BATT: Problem writing output file " + result_file + ": " + e);
      System.exit(1);
    }
}



void outputTestCase(JunitTest btc,XMLStreamWriter xw) throws XMLStreamException
{
   Description d = btc.getDescription();

   xw.writeStartElement("TESTCASE");
   if (d.getClassName() != null) xw.writeAttribute("CLASS",d.getClassName());
   if (d.getMethodName() != null) xw.writeAttribute("METHOD",d.getMethodName());
   if (d.getTestClass() != null) {
      Class<?> tcls = d.getTestClass();
      if (Modifier.isAbstract(tcls.getModifiers())) xw.writeAttribute("ABSTRACT","true");
      String tc = d.getTestClass().getName();
      if (!tc.equals(d.getClassName())) {
	 xw.writeAttribute("TCLASS",tc);
       }
    }
   if (d.isEmpty()) xw.writeAttribute("EMPTY","TRUE");
   if (d.isSuite()) xw.writeAttribute("SUITE","TRUE");
   if (d.isTest()) xw.writeAttribute("TEST","TRUE");
   if (d.testCount() > 1) xw.writeAttribute("COUNT",Integer.toString(d.testCount()));

   xw.writeAttribute("STATUS",btc.getStatus().getType().toString());
   xw.writeAttribute("NAME",d.getDisplayName());
   xw.writeAttribute("HASH",Integer.toString(d.hashCode()));

   Failure f = btc.getStatus().getFailure();
   if (f != null) {
      xw.writeStartElement("EXCEPTION");
      if (f.getMessage() != null) xw.writeCData(f.getMessage());
      xw.writeEndElement();
      xw.writeStartElement("TRACE");
      xw.writeCData(shortenTrace(f.getTrace()));
      xw.writeEndElement();
    }

   for (Annotation an : d.getAnnotations()) {
      xw.writeStartElement("ANNOT");
      xw.writeCData(an.toString());
      xw.writeEndElement();
    }

   xw.writeEndElement();
}



private String shortenTrace(String t)
{
   if (t.length() > 10240) {
      StringBuilder buf = new StringBuilder();
      StringTokenizer tok = new StringTokenizer(t,"\n");
      for (int i = 0; i < 40 && tok.hasMoreTokens(); ++i) {
	 String ln = tok.nextToken();
	 buf.append(ln);
	 buf.append("\n");
       }
      if (tok.hasMoreTokens()) buf.append("...\n");
      t = buf.toString();
    }

   return t;
}




/********************************************************************************/
/*										*/
/*	FIlter for handing listing test classes 				*/
/*										*/
/********************************************************************************/

private class ListFilter extends Filter {

   @Override public String describe()			{ return "List test cases"; }

   @Override public boolean shouldRun(Description d) {
      System.err.println("BATT: Consider test: " + d.isTest() + " " + d.isEmpty() + " " + d.isSuite() + " " +
			    d.getClassName() + " " + d.getMethodName() + " " +
			    d.getChildren().size() + " " + d.getDisplayName() + " " + d);
      if (d.isSuite()) return true;
      if (!d.isTest()) return false;
      if (d.getMethodName() != null) {
	 if (d.getClassName().startsWith("junit.") || d.getClassName().startsWith("org.junit."))
	    return false;
	 if (Modifier.isAbstract(d.getTestClass().getModifiers())) return false;
	 JunitTest jt = addTestCase(d,STATUS_LISTING);
	 outputSingleTest(jt);
	 return false;
       }

      System.err.println("BATT: Unknown test: " + d.isTest() + " " + d.isEmpty() + " " +
			    d.getClassName() + " " + d.isSuite() + " " + d.getChildren().size() + " " + d);
      setTestStatus(d,STATUS_UNKNOWN);
      // might want to check classes to see if they are relevant here as well
      return false;
    }

}	// end of inner class ListFilter




/********************************************************************************/
/*										*/
/*	Listener for handling testing						*/
/*										*/
/********************************************************************************/

private class TestListener extends RunListener {

   TestListener() { }

   @Override public void testStarted(Description d) {
      System.err.println("BATT: START " + d);
      JunitTestStatus bts = getTestStatus(d);
      JunitTest jt = addTestCase(d,STATUS_RUNNING);
      noteStart(d);
      switch (bts.getType()) {
	 case FAILURE :
	 case SUCCESS :
	 case LISTING :
	    if (d.isTest()) outputSingleTest(jt);
	    break;
	 default:
	    break;
       }
    }

   @Override public void testIgnored(Description d) {
      addTestCase(d,STATUS_IGNORED);
    }

   @Override public void testFinished(Description d) {
      System.err.println("BATT: FINISH " + d + " " + test_cases.containsKey(d));

      JunitTest jt = test_cases.get(d);
      if (jt == null) return;

      noteFinish(d);

      JunitTestStatus bts = getTestStatus(d);
      switch (bts.getType()) {
	 case FAILURE :
	 case IGNORED :
	 case LISTING :
	    break;
	 default :
	    setTestStatus(d,STATUS_SUCCESS);
	    break;
       }

      outputSingleTest(jt);
    }

   @Override public void testRunStarted(Description d) {
      System.setSecurityManager(new NoExitManager());
    }

   @Override public void testRunFinished(Result r) {
      System.setSecurityManager(null);
      noteDone();
    }

   @Override public void testFailure(Failure f) {
      if (f.getMessage() != null && bad_messages.contains(f.getMessage())) {
	 removeTestCase(f.getDescription());
       }
      else if (f.getMessage() != null &&
		  (f.getMessage().startsWith("No tests found matching List test cases from org.junit.runner.Request") ||
		      f.getMessage().startsWith("No runnable methods") ||
		      f.getMessage().startsWith("No tests found in "))) {
	 removeTestCase(f.getDescription());
       }
      else {
	 System.err.println("BATT: FAIL " + f.getTestHeader() + " " + f.getDescription() + " " + f.getException() + " " + f.getMessage() + "\nTRACE: " + f.getTrace());
	 addTestCase(f.getDescription(),new JunitTestStatus(f));
       }

      JunitTest jt = test_cases.get(f.getDescription());
      if (jt != null) outputSingleTest(jt);
    }

}	// end of inner class TestListener



/********************************************************************************/
/*										*/
/*	Handle System.exit() calls in tests					*/
/*										*/
/********************************************************************************/

private static class ExitException extends SecurityException {

   ExitException(int sts) {
      super("Attempt to call System.exit");
    }

}	// end of inner class ExitException



private static class NoExitManager extends SecurityManager {

   @Override public void checkPermission(Permission p)			{ }
   @Override public void checkPermission(Permission p,Object ctx)	{ }

   @Override public void checkExit(int sts) {
      super.checkExit(sts);
      throw new ExitException(sts);
    }

}	// end of inner class NoExitManager




/********************************************************************************/
/*										*/
/*	TestCase information							*/
/*										*/
/********************************************************************************/

private static class JunitTest {

   private Description test_info;
   private JunitTestStatus test_status;

   JunitTest(Description d) {
      test_info = d;
      test_status = STATUS_UNKNOWN;
    }

   Description getDescription() 		{ return test_info; }
   JunitTestStatus getStatus()			{ return test_status; }
   void setStatus(JunitTestStatus sts)		{ test_status = sts; }

}	// end of inner class JunitTest



private static class JunitTestStatus {

   private StatusType status_type;
   private Failure fail_data;

   JunitTestStatus(StatusType st) {
      status_type = st;
      fail_data = null;
    }

   JunitTestStatus(Failure f) {
      status_type = StatusType.FAILURE;
      fail_data = f;
    }

   StatusType getType() 			{ return status_type; }
   Failure getFailure() 			{ return fail_data; }

}	// end of inner class JunitTestStatus




}	// end of class BattJUnit




/* end of BattJUnit.java */
