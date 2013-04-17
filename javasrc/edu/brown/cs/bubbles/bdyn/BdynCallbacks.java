/********************************************************************************/
/*                                                                              */
/*              BdynCallbacks.java                                              */
/*                                                                              */
/*      Maintain the set of callbacks for Bdyn analysis                         */
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



package edu.brown.cs.bubbles.bdyn;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.banal.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;


class BdynCallbacks implements BdynConstants, BanalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,CallbackMethod> callback_methods;
private File callback_file;

private static AtomicInteger callback_counter = new AtomicInteger();

private static Set<String> primitive_types;


static {
   primitive_types = new HashSet<String>();
   primitive_types.add("int");
   primitive_types.add("short");
   primitive_types.add("byte");  
   primitive_types.add("char");  
   primitive_types.add("long");
   primitive_types.add("float");  
   primitive_types.add("double");  
   primitive_types.add("boolean");  
   primitive_types.add("void");  
}

enum CallbackType {
   UNKNOWN,
   EVENT,			// event handler
   CONSTRUCTOR,			// constructor for event recognition
};



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BdynCallbacks()
{
   File f = BoardSetup.getBubblesWorkingDirectory();
   callback_file = new File(f,BDYN_CALLBACK_FILE);
   
   callback_methods = new HashMap<String,CallbackMethod>();
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

List<BdynCallback> getCallbacks()
{
   return new ArrayList<BdynCallback>(callback_methods.values());
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void setup()
{
   CallbackLoader cl = new CallbackLoader();
   BoardThreadPool.start(cl);
}



private class CallbackLoader implements Runnable {
   
   @Override public void run() {
      loadCallbacks();
      setCallbacks();
      saveCallbacks();
    }
   
}       // end of inner class CallbackLoader



/********************************************************************************/
/*                                                                              */
/*      Callbacks are defined -- now update set of related items                */
/*                                                                              */
/********************************************************************************/

private void setCallbacks()
{
   Set<String> classes = new HashSet<String>();
   
   synchronized (callback_methods) {
      for (Iterator<CallbackMethod> it = callback_methods.values().iterator(); it.hasNext(); ) {
	 CallbackMethod cm = it.next();
	 switch (cm.getCallbackType()) {
	    default :
	    case CONSTRUCTOR :
	    case UNKNOWN :
	       continue;
	    case EVENT :
	       break;
	  }
	       
         String rargs = "";
         int act = 0;
         if (!Modifier.isStatic(cm.getModifiers())) {
            String cnm = cm.getClassName();
            if (isUserClass(cnm)) {
               classes.add(cnm);
               rargs += Integer.toString(act);
             }
            ++act;
          }
         String args = cm.getArgs();
         if (args != null) {
            List<String> prms = BumpLocation.getParameterList(args);
            for (String s : prms) {
               if (isUserClass(s)) {
        	  Element hxml = BumpClient.getBump().getTypeHierarchy(cm.getProject(), null, s, false);
        	  // System.err.println("HIERARCHY: " + IvyXml.convertXmlToString(hxml));
        	  addClasses(cm.getProject(),hxml,s,classes);
                  rargs += Integer.toString(act);
                  if (rargs.length() >= 2) break;
                }
               else if (s.equals("double") || s.equals("long")) ++act;
               ++act;
               if (act > 9) break;
             } 
          }
         // if (rags.length() == 0) remove the callback
         cm.setCallbackArgs(rargs);
       }
   
      for (String s : classes) {
	 List<BumpLocation> cnsts = BumpClient.getBump().findMethods(null,s,false,true,true,false);
	 if (cnsts != null) {
	    for (BumpLocation bl : cnsts) {
	       if (!Modifier.isProtected(bl.getModifiers())) {
		  String nm = bl.getSymbolName();
		  if (!callback_methods.containsKey(nm)) {
		     int idx = nm.lastIndexOf(".");
		     if (idx < 0) continue;
		     String cnm = nm.substring(0,idx);
		     String mnm = nm.substring(idx+1);
		     String key = cnm + "@" + mnm;
		     CallbackMethod cm = new CallbackMethod(cnm,mnm,CallbackType.CONSTRUCTOR,bl);
		     callback_methods.put(key,cm);
		  }
	       }
	    }
	 }
      }
    }

   // save data so it can be used for tracing
}



private void addClasses(String proj,Element hxml,String cls,Set<String> classes)
{
   if (hxml == null) {
      classes.add(cls);
      return;
    }
   
   for (Element cxml : IvyXml.children(hxml,"TYPE")) {
      String nm = IvyXml.getAttrString(cxml,"NAME");
      String pnm = IvyXml.getAttrString(cxml,"PNAME");
      if (nm == null) continue;
      if (pnm == null) pnm = nm;
      if (nm.equals(cls) || pnm.equals(cls)) {
	 if (IvyXml.getAttrString(cxml,"KIND").equals("CLASS")) {
	    if (!classes.contains(nm)) {
	       List<BumpLocation> locs = BumpClient.getBump().findTypes(proj,nm.replace("$","."));
	       if (locs != null && locs.size() > 0) {
		  for (BumpLocation bl : locs) {
		     if (!Modifier.isAbstract(bl.getModifiers())) { 
			classes.add(nm);
		     }
		  }
	       }
	    }
	  }
	 for (Element sxml : IvyXml.children(cxml,"SUBTYPE")) {
	    addClasses(proj,hxml,IvyXml.getAttrString(sxml,"NAME"),classes);
	  }
	 break;
       }
    }	     
}


private boolean isUserClass(String nm)
{
   if (nm == null) return false;
   if (primitive_types.contains(nm)) return false;
   nm = nm.replace('$', '.');
   List<BumpLocation> defs = BumpClient.getBump().findTypes(null,nm);
   if (defs == null || defs.size() == 0) return false;
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Update callbacks based on call trie                                     */
/*                                                                              */
/********************************************************************************/

void updateCallbacks(TrieNode tn)
{
   CallbackUpdater cu = new CallbackUpdater(tn);
   BoardThreadPool.start(cu);
}


private class CallbackUpdater implements Runnable {
   
   private TrieNode root_node;
   private Map<String,BanalHierarchyNode> package_hierarchy;
   
   CallbackUpdater(TrieNode root) {
      root_node = root;
      package_hierarchy = null;
    }
   
   @Override public void run() {
      package_hierarchy = BanalFactory.getFactory().computePackageHierarchy(null);
      addNode(root_node,null);
      setCallbacks();
      saveCallbacks();
    }
   
   private void addNode(TrieNode tn,TrieNode par) {
      boolean cb = false;
      boolean chld = true;
      if (par != null) {
         BanalHierarchyNode ppkg = getPackage(par);
         BanalHierarchyNode npkg = getPackage(tn);
         if (ppkg != null && npkg != null && ppkg != npkg) {
            if (ppkg.getLevel() > npkg.getLevel()) cb = true;
            else if (ppkg.getLevel() == npkg.getLevel() &&
                  ppkg.getCycle() != npkg.getCycle()) cb = true;
          }
       }
      if (cb) {
         CallbackMethod cm = new CallbackMethod(null,tn.getFileName(),tn.getClassName(),
               tn.getMethodName(),null,tn.getLineNumber(),CallbackType.EVENT);
         synchronized (callback_methods) {
            CallbackMethod ocm = callback_methods.get(cm.getFullName());
            if (ocm == null) {
               if (validate(cm)) {
                  callback_methods.put(cm.getFullName(),cm);
                  chld = false;
                }
             }
          }
       }
      else {  
         boolean usecur = false;
         if (par == null) usecur = true;
         else if (tn == null || tn.getMethodName() == null) usecur = false;
         else if (tn.getMethodName().equals("toString")) usecur = false;
         else if (tn.getClassName().startsWith("java.util")) usecur = true;
         else {
            BanalHierarchyNode ppkg = getPackage(par);
            BanalHierarchyNode npkg = getPackage(tn);
            if (ppkg == null) usecur = true;
            else if (npkg != null) {
               if (ppkg.getLevel() >= npkg.getLevel()) usecur = true;
             }
          }
         if (usecur) par = tn;
       }
      
      if (chld) {
	 for (TrieNode cn : tn.getChildren()) {
	    addNode(cn,par);
	  }
       }

    }
   
   private BanalHierarchyNode getPackage(TrieNode tn) {
	
      String cn = tn.getClassName();
      if (cn == null) return null;	// root node
      int idx = cn.lastIndexOf(".");
      if (idx < 0) return null;         // this might be wrong
      return package_hierarchy.get(cn.substring(0,idx));
    }
   
}       // end of inner class CallbackUpdater




/********************************************************************************/
/*                                                                              */
/*      Load and store callback data                                            */
/*                                                                              */
/********************************************************************************/

void saveCallbacks()
{
   synchronized(callback_methods) {
      try {
         IvyXmlWriter xw = new IvyXmlWriter(callback_file);
         xw.begin("CALLBACKS");
         for (CallbackMethod cm : callback_methods.values()) {
            cm.outputXml(xw);
          }
         xw.end("CALLBACKS");
         xw.close();
       }
      catch (IOException e) {
         BoardLog.logE("BDYN","Problem writing callback file",e);
       }
    }
}
  

void loadCallbacks()
{
   Element xml = IvyXml.loadXmlFromFile(callback_file);
   if (xml == null) return;
   
   synchronized (callback_methods) {
      for (Element ce : IvyXml.children(xml,"CALLBACK")) {
         CallbackMethod cm = new CallbackMethod(ce);
         if (validate(cm)) {
            callback_methods.put(cm.getFullName(),cm);
            int id = cm.getId();
            for ( ; ; ) {
               int oct = callback_counter.get();
               if (oct >= id) break;
               if (callback_counter.compareAndSet(oct,id)) break;
             }
          }
       }
    }
}



private boolean validate(CallbackMethod cm)
{
   boolean cnst = false;
   String pat = cm.getClassName().replace("$",".") + "." + cm.getMethodName(); 
   if (cm.getCallbackType() == CallbackType.CONSTRUCTOR) {
      cnst = true; 
      pat = cm.getClassName().replace("$",".");
    }
   // if (cm.getArgs() != null) pat += cm.getArgs();
   
   List<BumpLocation> locs = BumpClient.getBump().findMethods(cm.getProject(),pat,false,true,cnst,false);
   if (locs == null || locs.isEmpty()) return false;
   for (BumpLocation bl : locs) {
      if (bl.getDefinitionOffset() == 0) continue;
      if (cm.getArgs() != null && bl.getParameters() != null && !cm.getArgs().equals(bl.getParameters()));
      int acc = bl.getModifiers();
      if (cnst) {
	 if (!Modifier.isProtected(acc)) {
	    cm.update(bl);
	    return true;
	 }
       }
      else {
	 if (Modifier.isPublic(acc) || Modifier.isProtected(acc)) {
	    cm.update(bl);
	    return true;
	 }
      }
    }
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Callback Method Data                                                    */
/*                                                                              */
/********************************************************************************/

private static class CallbackMethod implements BdynCallback {
   
   private String project_name;
   private String class_name;
   private String method_name;
   private String method_args;
   private int    line_number;
   private int    definition_offset;
   private String file_name;
   private int    callback_id;
   private int    method_mods;
   private String callback_args;
   private CallbackType callback_type;
   
   CallbackMethod(String proj,String file,String cls,String mthd,String args,int line,CallbackType cbt) {
      project_name = proj;
      file_name = file;
      class_name = cls;
      method_name = mthd;
      method_args = args;
      line_number = line;
      callback_id = 0;
      method_mods = 0;
      callback_args = null;
      callback_type = cbt;
    }
   
   CallbackMethod(String cls,String mthd,CallbackType cbt,BumpLocation loc) {
      this(null,null,cls,mthd,null,0,cbt);
      update(loc);
   }
   
   CallbackMethod(Element xml) {
      callback_id = IvyXml.getAttrInt(xml,"ID");
      project_name = IvyXml.getAttrString(xml,"PROJECT");
      class_name = IvyXml.getAttrString(xml,"CLASS");
      method_name = IvyXml.getAttrString(xml,"METHOD");
      method_args = IvyXml.getAttrString(xml,"ARGS");
      file_name = IvyXml.getAttrString(xml,"FILE");
      line_number = IvyXml.getAttrInt(xml,"LINE");
      definition_offset = IvyXml.getAttrInt(xml,"OFFSET");
      method_mods = IvyXml.getAttrInt(xml,"MODS");
      callback_args = IvyXml.getAttrString(xml,"CBARGS");
      callback_type = IvyXml.getAttrEnum(xml,"CBTYPE",CallbackType.UNKNOWN);
    }
   
   void update(BumpLocation loc) {
      if (project_name == null) project_name = loc.getProject();
      if (file_name == null) file_name = loc.getFile().getPath();
      if (method_args == null) method_args = loc.getParameters();
      if (definition_offset <= 0) definition_offset = loc.getDefinitionOffset();
      method_mods = loc.getModifiers();
      line_number = 0;
    }
   
   boolean validate() {
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> locs = bc.findMethod(project_name, class_name + "." + method_name, false);
      if (locs != null && !locs.isEmpty()) return true;
      return false;
   }
   
   String getProject()                          { return project_name; }
   @Override public String getClassName()       { return class_name; }
   @Override public String getMethodName()      { return method_name; }
   @Override public int getId() {
      if (callback_id == 0) callback_id = callback_counter.incrementAndGet();
      return callback_id;
    }
   String getArgs()                     { return method_args; }
   int getModifiers()                   { return method_mods; }
   void setCallbackArgs(String s)       { callback_args = s; }
   CallbackType getCallbackType()	{ return callback_type; }
   
   String getFullName() {
      return class_name + "@" + method_name;
    }
   
   void outputXml(IvyXmlWriter xw) {
      xw.begin("CALLBACK");
      xw.field("ID",getId());
      xw.field("PROJECT",project_name);
      xw.field("FILE",file_name);
      xw.field("CLASS",class_name);
      xw.field("METHOD",method_name);
      xw.field("ARGS",method_args);
      xw.field("LINE",line_number);
      xw.field("OFFSET",definition_offset);
      xw.field("MODS",method_mods);
      xw.field("CBARGS",callback_args);
      xw.field("CBTYPE",callback_type);
      xw.end("CALLBACK");
    }
   
}       // end of inner class CallbackMethod



}       // end of class BdynCallbacks




/* end of BdynCallbacks.java */

