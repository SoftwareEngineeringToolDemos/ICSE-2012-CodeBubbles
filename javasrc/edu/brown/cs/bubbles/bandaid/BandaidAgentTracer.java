/********************************************************************************/
/*                                                                              */
/*              BandaidAgentTracer.java                                         */
/*                                                                              */
/*      Agent to capture event trace for visualization                          */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bandaid;

import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.*;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.commons.CodeSizeEvaluator;
import edu.brown.cs.bubbles.bandaid.org.objectweb.asm.util.TraceMethodVisitor;

import java.util.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;

import java.io.*;
import java.util.*;


class BandaidAgentTracer extends BandaidAgent implements BandaidConstants,
        ClassFileTransformer
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,Map<String,TraceData>> trace_map;
private boolean         trace_enabled;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BandaidAgentTracer(BandaidController bc)
{
   super(bc,"Tracer");
   
   trace_enabled = true;
   
   trace_map = null;
   String sf = bc.getBaseDirectory();
   if (sf != null) {
      File f1 = new File(sf);
      f1 = new File(f1,TRACE_DATA_FILE);
      loadTraceData(f1);
    }
}



/********************************************************************************/
/*                                                                              */
/*     Agent interface                                                          */
/*                                                                              */
/********************************************************************************/

@Override void enableMonitoring(boolean fg,long now)
{
   trace_enabled = fg;
   super.enableMonitoring(fg,now);
}


@Override void generateReport(BandaidXmlWriter xw,long now) 
{
   // output trace data
}




/********************************************************************************/
/*                                                                              */
/*      Patching interface                                                      */
/*                                                                              */
/********************************************************************************/

@Override ClassFileTransformer getTransformer()
{
   return this;
}


@Override public byte [] transform(ClassLoader ldr,String nm,Class<?> redef,ProtectionDomain dom,
      byte [] buf)
{
   Map<String,TraceData> tdm = trace_map.get(nm);
   
   if (tdm == null) return patchClass(buf,tdm);
   
   return null;
}



private byte [] patchClass(byte [] buf,Map<String,TraceData> mthds)
{
   byte [] rsltcode = null;
   
   try {
      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
      ClassReader reader = new ClassReader(buf);
      ClassVisitor ins = new ClassTransformer(writer,mthds);
      reader.accept(ins,ClassReader.SKIP_FRAMES);
      rsltcode = writer.toByteArray();
    }
   catch (Throwable t) {
      System.err.println("BANDAID: Problem instrumenting class: " + t);
      t.printStackTrace();
    }
   
   return rsltcode;
}



private class ClassTransformer extends ClassAdapter {
   
   private Map<String,TraceData> method_data;
   
   ClassTransformer(ClassVisitor v,Map<String,TraceData> mthds) {
      super(v);
      method_data = mthds;
    }
   
}


/********************************************************************************/
/*                                                                              */
/*      Load information on what to trace                                       */
/*                                                                              */
/********************************************************************************/

private void loadTraceData(File f)
{
   try {
      BufferedReader fr = new BufferedReader(new FileReader(f));
      for ( ; ; ) {
         String ln = fr.readLine();
         if (ln == null) break;
         ln = ln.trim();
         if (ln.length() == 0) continue;
         if (ln.startsWith("#")) continue;
         StringTokenizer tok = new StringTokenizer(ln);
         int id = 0;
         int fgs = 0;
         int cargs = 0;
         String cls = null;
         String mthd = null;
         String args = null;
         try {
            if (tok.hasMoreTokens()) {
               id = Integer.parseInt(tok.nextToken());
             }
            if (tok.hasMoreTokens()) {
               fgs = Integer.parseInt(tok.nextToken());
             }
            if (tok.hasMoreTokens()) {
               cargs = Integer.parseInt(tok.nextToken());
             }
            if (tok.hasMoreTokens()) {
               cls = tok.nextToken();
             }
            if (tok.hasMoreTokens()) {
               mthd = tok.nextToken();
             }
            if (tok.hasMoreTokens()) {
               args = tok.nextToken();
             }
            if (mthd != null && cls != null && id > 0 && fgs != 0) {
               String mkey = mthd;
               if (args != null) mkey += args;
               TraceData td = new TraceData(id,fgs,cargs);
               Map<String,TraceData> tm = trace_map.get(cls);
               if (tm == null) {
                  tm = new HashMap<String,TraceData>();
                  trace_map.put(cls,tm);
                }
               tm.put(mkey,td);
             }
          }
         catch (NumberFormatException e) { }
       }
      fr.close();
    }
   catch (IOException e) {
      trace_map = null;
    }
}




/********************************************************************************/
/*                                                                              */
/*      TraceData -- information for instumentation/tracing                     */
/*                                                                              */
/********************************************************************************/

private static class TraceData {
   
   private int trace_id;
   private int trace_flags;
   private int trace_args;
   
   TraceData(int id,int fg,int arg) {
      trace_id = id;
      trace_flags = fg;
      trace_args = arg;
    }
  
}       // end of inner class TraceData



}       // end of class BandaidAgentTracer




/* end of BandaidAgentTracer.java */

