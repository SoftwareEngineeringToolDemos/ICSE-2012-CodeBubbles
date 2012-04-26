/********************************************************************************/ /*										   */
/*		BedrockEditor.java						*/
/*										*/
/*	Handle editor-related commands for Bubbles				*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.text.edits.*;
import org.w3c.dom.Element;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;


class BedrockEditor implements BedrockConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;

private Map<String,FileData> file_map;
private ThreadPoolExecutor thread_pool;
private BlockingQueue<Runnable> edit_queue;
private Map<String,ParamSettings> param_map;
private CodeFormatter code_formatter;
private int active_edits;

private static final int	QUEUE_SIZE = 1000;
private static final int	CORE_SIZE = 1;
private static final int	MAX_SIZE = 8;
private static final long	KEEP_ALIVE = 1000l;
private static final TimeUnit	KEEP_ALIVE_UNIT = TimeUnit.SECONDS;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockEditor(BedrockPlugin bp)
{
   our_plugin = bp;

   file_map = new HashMap<String,FileData>();
   edit_queue = new ArrayBlockingQueue<Runnable>(QUEUE_SIZE,true);
   param_map = new HashMap<String,ParamSettings>();
   code_formatter = null;
   thread_pool = null;
   active_edits = 0;
}





/********************************************************************************/
/*										*/
/*	Worker thread pool setup						*/
/*										*/
/********************************************************************************/

void start()
{
   thread_pool = new ThreadPoolExecutor(CORE_SIZE,MAX_SIZE,KEEP_ALIVE,KEEP_ALIVE_UNIT,
					   edit_queue);
}



/********************************************************************************/
/*										*/
/*	Parameter commands							*/
/*										*/
/********************************************************************************/

void handleParameter(String bid,String name,String value) throws BedrockException
{
   if (name == null) return;
   else if (name.equals("AUTOELIDE")) {
      setAutoElide(bid,(value != null && value.length() > 0 && "tTyY1".indexOf(value.charAt(0)) >= 0));
    }
   else if (name.equals("ELIDEDELAY")) {
      try {
	 setElideDelay(bid,Long.parseLong(value));
       }
      catch (NumberFormatException e) {
	 throw new BedrockException("Bad elide delay value: " + value);
       }
    }
   else {
      throw new BedrockException("Unknown editor parameter " + name);
    }
}



/********************************************************************************/
/*										*/
/*	Basic editing commands							*/
/*										*/
/********************************************************************************/

void handleStartFile(String proj,String bid,String file,String id,boolean cnts,IvyXmlWriter xw)
		throws BedrockException
{
   FileData fd = findFile(proj,file,bid,id);

   if (fd == null) {
      throw new BedrockException("Compilation unit for file " + file + " not available in " + proj);
    }

   fd.getEditableUnit(bid);

   BedrockPlugin.logD("OPEN file " + file + " " + fd.hasChanged());

   String lsep = fd.getLineSeparator();
   if (lsep.equals("\n")) xw.field("LINESEP","LF");
   else if (lsep.equals("\r\n")) xw.field("LINESEP","CRLF");
   else if (lsep.equals("\r")) xw.field("LINESEP","CR");

   if (cnts || fd.hasChanged()) {
      String s = fd.getCurrentContents();
      if (s == null) xw.emptyElement("EMPTY");
      else {
	 byte [] data = s.getBytes();
	 xw.bytesElement("CONTENTS",data);
       }
    }
   else xw.emptyElement("SUCCESS");
}



void handleEdit(String proj,String sid,String file,String id,List<EditData> edits,
		   IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,sid,id);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file + " not available");

   TextEdit xe = null;
   boolean havemult = false;
   for (EditData eds : edits) {
      TextEdit te = null;

      if (eds.getText() != null) {
	 // TODO: this only works for insertions, not for replace
	 String s = eds.getText();
	 if (!fd.getLineSeparator().equals("\n")) s = s.replace("\n",fd.getLineSeparator());
	 BedrockPlugin.logD("EDIT REPLACE " + eds.getOffset() + " " + eds.getLength() + " " +
			       s.length() + " " + fd.getLineSeparator().length());
	 te = new ReplaceEdit(eds.getOffset(),eds.getLength(),s);
	 fd.noteEdit(sid,eds.getOffset(),eds.getLength(),s.length());
       }
      else {
	 int delta = 0;
	 delta = fd.getLineSeparator().length() - 1;
	 int off = eds.getOffset();
	 int len = eds.getLength();

	 if (delta > 0) {			// handle crlf pairs being deleted
	    String s = fd.getContents(sid);
	    for (int i = 0; i < len; ++i) {
	       char c = s.charAt(i+off);
	       if (c == '\r') ++len;
	     }
	  }

	 BedrockPlugin.logD("EDIT DELETE " + eds.getOffset() + " " + eds.getLength() + " " +
			       len + " " + fd.getLineSeparator().length());
	 te = new DeleteEdit(off,len);
	 fd.noteEdit(sid,off,len,0);
       }
      if (xe == null) xe = te;
      else if (!havemult) {
	 MultiTextEdit mte = new MultiTextEdit();
	 mte.addChild(xe);
	 mte.addChild(te);
	 xe = mte;
	 havemult = true;
       }
    }

   fd.applyEdit(sid,xe);

   EditTask et = new EditTask(fd,sid,id);
   try {
      synchronized (thread_pool) {
	 thread_pool.execute(et);
	 ++active_edits;
       }
    }
   catch (RejectedExecutionException ex) {
      BedrockPlugin.logE("Edit task rejected " + ex + " " +
			    edit_queue.size() + " " + thread_pool.getActiveCount());
    }

   xw.emptyElement("SUCCESS");
}



private void doneEdit()
{
   synchronized (thread_pool) {
      --active_edits;
      if (active_edits == 0) thread_pool.notifyAll();
    }
}


void waitForEdits()
{
   synchronized (thread_pool) {
      while (active_edits > 0) {
	 try {
	    thread_pool.wait();
	  }
	 catch (InterruptedException e) { }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle code completion					*/
/*										*/
/********************************************************************************/

void getCompletions(String proj,String bid,String file,int offset,IvyXmlWriter xw)
		throws BedrockException
{
   FileData fd = findFile(proj,file,bid,null);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file + " not available");

   ICompilationUnit icu = fd.getEditableUnit(bid);

   CompletionHandler ch = new CompletionHandler(xw,bid);

   try {
      icu.codeComplete(offset,ch);
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem getting completions: ",e);
    }
}




/********************************************************************************/
/*										*/
/*	Elision commands							*/
/*										*/
/********************************************************************************/

void elisionSetup(String proj,String bid,String file,boolean compute,
		     Collection<Element> rgns,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,bid,null);

   if (fd == null) {
      throw new BedrockException("Compilation unit for file " + file +
				    " not available for elision");
    }

   CompilationUnit cu = fd.getAstRoot(bid,null);

   if (cu == null) throw new BedrockException("Unable to get AST for file " + file);

   BedrockElider be = null;

   if (rgns != null) {
      be = fd.getElider(bid);
      be.clearElideData();
      for (Element r : rgns) {
	 double p = IvyXml.getAttrDouble(r,"PRIORITY",-1);
	 int soff = IvyXml.getAttrInt(r,"START");
	 int eoff = IvyXml.getAttrInt(r,"END");
	 if (soff < 0 || eoff < 0) throw new BedrockException("Missing start or end offset for elision region");
	 if (p >= 0) be.addElidePriority(soff,eoff,p);
	 else be.addElideRegion(soff,eoff);
       }
    }
   else if (compute) {
      be = fd.checkElider(bid);
    }
   else fd.clearElider(bid);		// no regions and no compute

   if (compute) {
      xw.begin("ELISION");
      if (be != null) be.computeElision(cu,xw);
      xw.end("ELISION");
    }
   else xw.emptyElement("SUCCESS");
}




/********************************************************************************/
/*										*/
/*	Remote file editing commands						*/
/*										*/
/********************************************************************************/

void fileElide(byte [] bytes,IvyXmlWriter xw)
{
   BedrockElider be = new BedrockElider();

   // would really like to resolvie bindings here

   ASTParser ap = ASTParser.newParser(AST.JLS3);
   String s1 = new String(bytes);
   char [] cdata = s1.toCharArray();
   be.addElideRegion(0,cdata.length);
   ap.setSource(cdata);
   CompilationUnit cu = (CompilationUnit) ap.createAST(null);

   be.computeElision(cu,xw);
}



/********************************************************************************/
/*										*/
/*	Commitment commands							*/
/*										*/
/********************************************************************************/

synchronized void handleCommit(String proj,String bid,boolean refresh,boolean save,
				  Collection<Element> files,IvyXmlWriter xw)
{
   xw.begin("COMMIT");

   if (files == null || files.size() == 0) {
      for (FileData fd : file_map.values()) {
	 if (refresh || !save || fd.hasChanged())
	    commitFile(fd,refresh,save,xw);
       }
    }
   else {
      for (Element e : files) {
	 String fnm = IvyXml.getAttrString(e,"NAME");
	 if (fnm == null) fnm = IvyXml.getText(e);
	 FileData fd = file_map.get(fnm);
	 if (fd != null) {
	    boolean r = IvyXml.getAttrBool(e,"REFRESH",refresh);
	    boolean s = IvyXml.getAttrBool(e,"SAVE",save);
	    commitFile(fd,r,s,xw);
	  }
       }
    }

   xw.end("COMMIT");
}



private void commitFile(FileData fd,boolean refresh,boolean save,IvyXmlWriter xw)
{
   xw.begin("FILE");
   xw.field("NAME",fd.getFileName());
   try {
      fd.commit(refresh,save);
    }
   catch (JavaModelException e) {
      xw.field("ERROR",e.toString());
    }
   xw.end("FILE");
}




/********************************************************************************/
/*										*/
/*	Renaming commands							*/
/*										*/
/********************************************************************************/

void rename(String proj,String bid,String file,int start,int end,String name,String handle,
	       String newname,
	       boolean keeporig, boolean getters,boolean setters, boolean dohier,
	       boolean qual,boolean refs,boolean dosimilar,boolean textocc,
	       boolean doedit,
	       String filespat,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = file_map.get(file);
   ICompilationUnit icu;

   if (doedit) {
      // icu = fd.getDefaultUnit();
      icu = fd.getEditableUnit(bid);
    }
   else icu = fd.getEditableUnit(bid);

   IJavaElement [] elts;
   try {
      elts = icu.codeSelect(start,end-start);
    }
   catch (JavaModelException e) {
      throw new BedrockException("Bad location: " + e,e);
    }

   IJavaElement relt = null;
   for (IJavaElement ije : elts) {
      if (handle != null && !handle.equals(ije.getHandleIdentifier())) continue;
      if (name != null && !name.equals(ije.getElementName())) continue;
      relt = ije;
      break;
    }
   if (relt == null) throw new BedrockException("Item to rename not found");

   String id = null;
   switch (relt.getElementType()) {
      case IJavaElement.COMPILATION_UNIT :
	 id = IJavaRefactorings.RENAME_COMPILATION_UNIT;
	 break;
      case IJavaElement.FIELD :
	 IField ifld = (IField) relt;
	 try {
	    if (ifld.isEnumConstant()) id = IJavaRefactorings.RENAME_ENUM_CONSTANT;
	    else id = IJavaRefactorings.RENAME_FIELD;
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 id = IJavaRefactorings.RENAME_PACKAGE;
	 break;
      case IJavaElement.LOCAL_VARIABLE :
	 id = IJavaRefactorings.RENAME_LOCAL_VARIABLE;
	 break;
      case IJavaElement.TYPE :
	 id = IJavaRefactorings.RENAME_TYPE;
	 break;
      case IJavaElement.TYPE_PARAMETER :
	 id = IJavaRefactorings.RENAME_TYPE_PARAMETER;
	 break;
      case IJavaElement.METHOD :
	 id = IJavaRefactorings.RENAME_METHOD;
	 break;
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_DECLARATION :
	 break;
    }
   if (id == null) throw new BedrockException("Invalid element type to rename");

   RenameJavaElementDescriptor renamer = new RenameJavaElementDescriptor(id);
   renamer.setJavaElement(relt);
   renamer.setKeepOriginal(keeporig);
   renamer.setNewName(newname);
   if (proj != null) renamer.setProject(proj);
   renamer.setRenameGetters(getters);
   renamer.setRenameSetters(setters);
   renamer.setUpdateHierarchy(dohier);
   renamer.setUpdateQualifiedNames(qual);
   renamer.setUpdateReferences(refs);
   renamer.setUpdateSimilarDeclarations(dosimilar);
   renamer.setUpdateTextualOccurrences(textocc);
   if (filespat != null) renamer.setFileNamePatterns(filespat);

   RefactoringStatus sts = renamer.validateDescriptor();
   if (!sts.isOK()) {
      xw.begin("FAILURE");
      xw.field("TYPE","VALIDATE");
      BedrockUtil.outputStatus(sts,xw);
      xw.end("FAILURE");
      return;
    }

   try {
      Refactoring refactor = renamer.createRefactoring(sts);
      if (refactor == null) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CREATE");
	 xw.textElement("REFACTOR",renamer.toString());
	 xw.end("FAILURE");
	 return;
       }

      refactor.setValidationContext(null);

      // this seems to reset files from disk (mutliple times)
      sts = refactor.checkAllConditions(new NullProgressMonitor());
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CHECK");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 if (sts.hasFatalError()) return;
       }
      BedrockPlugin.logD("RENAME: Refactoring checked");

      Change chng = refactor.createChange(new NullProgressMonitor());
      BedrockPlugin.logD("RENAME: Refactoring change created");

      if (doedit && chng != null) {
	 chng.perform(new NullProgressMonitor());
       }
      else if (chng != null) {
	 xw.begin("EDITS");
	 BedrockUtil.outputChange(chng,xw);
	 xw.end("EDITS");
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating refactoring: " + e,e);
    }

   BedrockPlugin.logD("RENAME RESULT = " + xw.toString());
}



/********************************************************************************/
/*										*/
/*	Method extraction commands						*/
/*										*/
/********************************************************************************/

void extractMethod(String proj,String bid,String file,int start,int end,String newname,
		      boolean replacedups,boolean cmmts,boolean exceptions,
		      IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = file_map.get(file);
   ICompilationUnit icu = fd.getEditableUnit(bid);

   String id = IJavaRefactorings.EXTRACT_METHOD;
   Map<String,String> rargs = new HashMap<String,String>();
   String sel = Integer.toString(start) + " " + Integer.toString(end-start+1);
   String hdl = icu.getHandleIdentifier();
   rargs.put("selection",sel);
   rargs.put("input",hdl);
   rargs.put("name",newname);
   rargs.put("replace",Boolean.toString(replacedups));
   rargs.put("comments",Boolean.toString(cmmts));
   rargs.put("exceptions",Boolean.toString(exceptions));

   RefactoringContribution rc1 = RefactoringCore.getRefactoringContribution(id);
   RefactoringDescriptor rd1 = rc1.createDescriptor(id,proj,"Bedrock extract method",
						       null,rargs,RefactoringDescriptor.NONE);
   ExtractMethodDescriptor emd = (ExtractMethodDescriptor) rd1;

   RefactoringStatus sts = emd.validateDescriptor();
   if (!sts.isOK()) {
      xw.begin("FAILURE");
      xw.field("TYPE","VALIDATE");
      BedrockUtil.outputStatus(sts,xw);
      xw.end("FAILURE");
      return;
    }
   try {
      Refactoring refactor = emd.createRefactoring(sts);

      sts = refactor.checkAllConditions(new NullProgressMonitor());
      if (!sts.isOK()) {
	 xw.begin("FAILURE");
	 xw.field("TYPE","CHECK");
	 BedrockUtil.outputStatus(sts,xw);
	 xw.end("FAILURE");
	 if (sts.hasFatalError()) return;
       }

      Change chng = refactor.createChange(new NullProgressMonitor());

      xw.begin("EDITS");
      BedrockUtil.outputChange(chng,xw);
      xw.end("EDITS");
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating refactoring: " + e,e);
    }
}



/********************************************************************************/
/*										*/
/*	Formatting commands							*/
/*										*/
/********************************************************************************/

void formatCode(String proj,String bid,String file,Collection<Element> rgns,IvyXmlWriter xw)
	throws BedrockException
{
   FileData fd = findFile(proj,file,bid,null);

   if (fd == null) throw new BedrockException("Compilation unit for file " + file +
						 " not available for formatting");

   ICompilationUnit icu = fd.getEditableUnit(bid);
   String cnts = null;
   try {
      cnts = icu.getBuffer().getContents();
    }
   catch (JavaModelException e) {
      throw new BedrockException("Unable to get compilation unit contents: " + e,e);
    }

   IRegion [] irgns = new IRegion[rgns.size()];
   int i = 0;
   for (Element r : rgns) {
      int soff = IvyXml.getAttrInt(r,"START");
      int eoff = IvyXml.getAttrInt(r,"END");
      if (soff < 0 || eoff < 0) throw new BedrockException("Missing start or end offset for formatting region");
      irgns[i++] = new Region(soff,eoff-soff);
    }

   if (code_formatter == null) {
      code_formatter = ToolFactory.createCodeFormatter(null);
    }

   // TODO: why doesn't K_CLASS_BODY_DECLARATIONS work here?
   // TextEdit te = code_formatter.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS,cnts,irgns,0,null);
   TextEdit te = code_formatter.format(CodeFormatter.K_UNKNOWN,cnts,irgns,0,null);

   if (te == null) throw new BedrockException("Unable to format method");

   BedrockUtil.outputTextEdit(te,xw);
}




/********************************************************************************/
/*										*/
/*	Text Region extraction commands 					*/
/*										*/
/********************************************************************************/

void getTextRegions(String proj,String bid,String file,String cls,boolean pfx,
		       boolean statics,
		       boolean compunit,
		       boolean imports,
		       boolean pkgfg,
		       boolean topdecls,
		       boolean all,
		       IvyXmlWriter xw)
	throws BedrockException
{
   if (file == null) {
      IProject ip = our_plugin.getProjectManager().findProjectForFile(proj,null);
      IType ityp = null;
      IJavaProject ijp = JavaCore.create(ip);
      try {
	 if (ijp != null) ityp = ijp.findType(cls);
       }
      catch (JavaModelException ex) { }
      if (ityp == null) throw new BedrockException("Class " + cls + " not defined in project " + proj);

      ICompilationUnit icu = ityp.getCompilationUnit();
      File f = BedrockUtil.getFileForPath(icu.getPath(),ip);
      file = f.getPath();
    }

   FileData fd = findFile(proj,file,bid,null);
   if (fd == null) throw new BedrockException("Can't find file " + file + " in " + proj);


   CompilationUnit cu = fd.getDefaultRoot(bid);
   if (cu == null) throw new BedrockException("Can't get compilation unit for " + file);

   List<?> typs = cu.types();
   AbstractTypeDeclaration atd = findTypeDecl(cls,typs);
   int start = 0;
   if (atd != null && atd != typs.get(0)) start = cu.getExtendedStartPosition(atd);

   if (compunit) {
      xw.begin("RANGE");
      xw.field("PATH",file);
      xw.field("START",0);
      int ln = fd.getLength();
      if (ln < 0) {
	 File f = new File(file);
	 ln = (int) f.length();
       }
      xw.field("END",ln);
      xw.end("RANGE");
    }

   if (pfx && atd != null) {
      int xpos = cu.getExtendedStartPosition(atd);
      int xlen = cu.getExtendedLength(atd);
      int spos = atd.getStartPosition();
      int len = atd.getLength();
      int epos = -1;
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 int apos = cu.getExtendedStartPosition(an);
	 if (epos < 0 || epos >= apos) epos = apos-1;
       }
      if (epos < 0) {		     // no body declarations
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",start);
	 xw.field("END",xpos+xlen);
	 xw.end("RANGE");
       }
      else {
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",start);
	 xw.field("END",epos);
	 xw.end("RANGE");
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",spos+len-1);
	 xw.field("END",xpos+xlen);
	 xw.end("RANGE");
       }
    }

   if (pkgfg) {
      PackageDeclaration pkg = cu.getPackage();
      if (pkg != null) {
	 outputRange(cu,pkg,file,xw);
       }
    }

   if (imports) {
      for (Iterator<?> it = cu.imports().iterator(); it.hasNext(); ) {
	 ImportDeclaration id = (ImportDeclaration) it.next();
	 outputRange(cu,id,file,xw);
       }
    }

   if (topdecls && atd != null) {
      int spos = atd.getStartPosition();
      int len = atd.getLength();
      int epos = -1;
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 int apos = cu.getExtendedStartPosition(an);
	 if (epos < 0 || epos >= apos) epos = apos-1;
       }
      if (epos < 0) {		     // no body declarations
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",spos);
	 xw.field("END",spos+len);
	 xw.end("RANGE");
       }
      else {
	 xw.begin("RANGE");
	 xw.field("PATH",file);
	 xw.field("START",spos);
	 xw.field("END",epos);
	 xw.end("RANGE");
       }
    }

   if ((statics || all) && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 if (an.getNodeType() == ASTNode.INITIALIZER) {
	    outputRange(cu,an,file,xw);
	  }
       }
    }

   if (all && atd != null) {
      for (Object o : atd.bodyDeclarations()) {
	 ASTNode an = (ASTNode) o;
	 IJavaElement elt = null;
	 switch (an.getNodeType()) {
	    case ASTNode.ANNOTATION_TYPE_DECLARATION :
	    case ASTNode.ENUM_DECLARATION :
	    case ASTNode.TYPE_DECLARATION :
	       AbstractTypeDeclaration atdecl = (AbstractTypeDeclaration) an;
	       ITypeBinding atbnd = atdecl.resolveBinding();
	       if (atbnd != null) elt = atbnd.getJavaElement();
	       break;
	    case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
	       break;
	    case ASTNode.ENUM_CONSTANT_DECLARATION :
	       EnumConstantDeclaration ecdecl = (EnumConstantDeclaration) an;
	       IVariableBinding ecbnd = ecdecl.resolveVariable();
	       if (ecbnd != null) elt = ecbnd.getJavaElement();
	       break;
	    case ASTNode.FIELD_DECLARATION :
	       FieldDeclaration fdecl = (FieldDeclaration) an;
	       for (Iterator<?> it = fdecl.fragments().iterator(); it.hasNext(); ) {
		  VariableDeclarationFragment vdf = (VariableDeclarationFragment) it.next();
		  IVariableBinding vbnd = vdf.resolveBinding();
		  if (vbnd != null) {
		     IJavaElement velt = vbnd.getJavaElement();
		     if (velt != null) BedrockUtil.outputJavaElement(velt,xw);
		   }
		}
	       break;
	    case ASTNode.INITIALIZER :
	       break;
	    case ASTNode.METHOD_DECLARATION :
	       MethodDeclaration mdecl = (MethodDeclaration) an;
	       IMethodBinding mbnd = mdecl.resolveBinding();
	       if (mbnd != null) elt = mbnd.getJavaElement();
	       break;
	    default :
	       break;
	 }
	 if (elt != null) BedrockUtil.outputJavaElement(elt,false,xw);
      }
    }
}



private void outputRange(CompilationUnit cu,ASTNode an,String file,IvyXmlWriter xw)
{
   int xpos = cu.getExtendedStartPosition(an);
   int xlen = cu.getExtendedLength(an);
   xw.begin("RANGE");
   xw.field("PATH",file);
   xw.field("START",xpos);
   xw.field("END",xpos+xlen);
   xw.end("RANGE");
}




private AbstractTypeDeclaration findTypeDecl(String cls,List<?> typs)
{
   AbstractTypeDeclaration atd = null;
   for (int i = 0; atd == null && i < typs.size(); ++i) {
      if (!(typs.get(i) instanceof AbstractTypeDeclaration)) continue;
      AbstractTypeDeclaration d = (AbstractTypeDeclaration) typs.get(i);
      if (cls != null) {
	 ITypeBinding tb = d.resolveBinding();
	 if (!tb.getQualifiedName().equals(cls)) {
	    if (cls.startsWith(tb.getQualifiedName() + ".")) {
	       atd = findTypeDecl(cls,d.bodyDeclarations());
	     }
	    continue;
	  }
       }
      atd = d;
    }

   return atd;
}



/********************************************************************************/
/*										*/
/*	Methods to update a key-based item					*/
/*										*/
/********************************************************************************/

void findByKey(String proj,String bid,String key,String file,IvyXmlWriter xw)
		throws BedrockException
{
   FileData fd = findFile(proj,file,bid,null);
   if (fd == null) return;

   ICompilationUnit icu = fd.getEditableUnit(bid);

   IJavaElement elt1 = null;

   // elt1 = JavaCore.create(key,icu.getOwner());

   elt1 = findElementForKey(icu,key);

   if (elt1 != null) BedrockUtil.outputJavaElement(elt1,false,xw);
}


private IJavaElement findElementForKey(IJavaElement elt,String key)
{
   if (key.equals(elt.getHandleIdentifier())) return elt;

   if (elt instanceof IParent) {
      IParent ip = (IParent) elt;
      try {
	 if (ip.hasChildren()) {
	    for (IJavaElement je : ip.getChildren()) {
	       IJavaElement re = findElementForKey(je,key);
	       if (re != null) return re;
	     }
	  }
       }
      catch (JavaModelException e) { }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Methods to get a list of active java elements				*/
/*										*/
/********************************************************************************/

// This shouldn't be needed since edits in a window should also be made in the default
// buffer and hence in the actual compilation unit that would be reported

void getActiveElements(IJavaElement root,List<IJavaElement> rslt)
{
   switch (root.getElementType()) {
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.FIELD :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.METHOD :
      case IJavaElement.PACKAGE_DECLARATION :
      case IJavaElement.TYPE :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) root;
	 try {
	    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE && pfr.hasChildren()) {
	       IJavaElement [] chld = pfr.getChildren();
	       for (IJavaElement c : chld) getActiveElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 IParent par = (IParent) root;
	 try {
	    if (par.hasChildren()) {
	       IJavaElement [] chld = par.getChildren();
	       for (IJavaElement c : chld) getActiveElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 ICompilationUnit cu = (ICompilationUnit) root;
	 IProject ip = cu.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(cu.getPath(),ip);
	 String fnm = f.getPath();
	 FileData fd = file_map.get(fnm);
	 if (fd == null) rslt.add(cu);
	 else {
	    rslt.add(fd.getSearchUnit());
	  }
	 break;
    }
}




void getWorkingElements(IJavaElement root,List<ICompilationUnit> rslt)
{
   switch (root.getElementType()) {
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.FIELD :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.METHOD :
      case IJavaElement.PACKAGE_DECLARATION :
      case IJavaElement.TYPE :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) root;
	 try {
	    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE && pfr.hasChildren()) {
	       IJavaElement [] chld = pfr.getChildren();
	       for (IJavaElement c : chld) getWorkingElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 IParent par = (IParent) root;
	 try {
	    if (par.hasChildren()) {
	       IJavaElement [] chld = par.getChildren();
	       for (IJavaElement c : chld) getWorkingElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 ICompilationUnit cu = (ICompilationUnit) root;
	 IProject ip = cu.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(cu.getPath(),ip);
	 String fnm = f.getPath();
	 FileData fd = file_map.get(fnm);
	 if (fd != null) {
	    rslt.add(fd.getSearchUnit());
	  }
	 break;
    }
}



void getCompilationElements(IJavaElement root,List<ICompilationUnit> rslt)
{
   switch (root.getElementType()) {
      case IJavaElement.ANNOTATION :
      case IJavaElement.CLASS_FILE :
      case IJavaElement.FIELD :
      case IJavaElement.IMPORT_CONTAINER :
      case IJavaElement.IMPORT_DECLARATION :
      case IJavaElement.INITIALIZER :
      case IJavaElement.JAVA_MODEL :
      case IJavaElement.LOCAL_VARIABLE :
      case IJavaElement.METHOD :
      case IJavaElement.PACKAGE_DECLARATION :
      case IJavaElement.TYPE :
      case IJavaElement.TYPE_PARAMETER :
      default :
	 break;
      case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	 IPackageFragmentRoot pfr = (IPackageFragmentRoot) root;
	 try {
	    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE && pfr.hasChildren()) {
	       IJavaElement [] chld = pfr.getChildren();
	       for (IJavaElement c : chld) getCompilationElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.JAVA_PROJECT :
      case IJavaElement.PACKAGE_FRAGMENT :
	 IParent par = (IParent) root;
	 try {
	    if (par.hasChildren()) {
	       IJavaElement [] chld = par.getChildren();
	       for (IJavaElement c : chld) getCompilationElements(c,rslt);
	     }
	  }
	 catch (JavaModelException e) { }
	 break;
      case IJavaElement.COMPILATION_UNIT :
	 ICompilationUnit cu = (ICompilationUnit) root;
	 IProject ip = cu.getJavaProject().getProject();
	 File f = BedrockUtil.getFileForPath(cu.getPath(),ip);
	 String fnm = f.getPath();
	 FileData fd = file_map.get(fnm);
	 if (fd != null) {
	    rslt.add(fd.getSearchUnit());
	  }
	 else rslt.add(cu);
	 break;
    }
}



/********************************************************************************/
/*										*/
/*	Methods to get the compilation unit					*/
/*										*/
/********************************************************************************/

ICompilationUnit getCompilationUnit(String proj,String file) throws BedrockException
{
   FileData fd = findFile(proj,file,null,null);

   return fd.getSearchUnit();
}



CompilationUnit getAST(String bid,String proj,String file) throws BedrockException
{
   FileData fd = findFile(proj,file,null,null);

// return fd.getDefaultRoot(bid);
   return fd.getAstRoot(bid,null);
}



/********************************************************************************/
/*										*/
/*	Methods for managing file data						*/
/*										*/
/********************************************************************************/

private synchronized FileData findFile(String proj,String file,String bid,String id)
		throws BedrockException
{
   FileData fd = file_map.get(file);

   if (fd == null) {
      ICompilationUnit icu = null;
      icu = our_plugin.getProjectManager().getCompilationUnit(proj,file);
      if (icu == null && proj != null) {
	 icu = our_plugin.getProjectManager().getCompilationUnit(null,file);
	 if (icu != null) proj = null;
       }

      if (icu == null) return null;
      icu = icu.getPrimary();

      fd = new FileData(proj,file,icu);
      file_map.put(file,fd);
    }

   if (id != null) fd.setCurrentId(bid,id);

   return fd;
}




/********************************************************************************/
/*										*/
/*	Methods to handle end of line delimiters				*/
/*										*/
/********************************************************************************/

private void checkForOpenEditor(IBuffer buf,ICompilationUnit icu)
{
   // see if we can find an open editor for this icu and use its contents if so
}



/********************************************************************************/
/*										*/
/*	Background compilation and analysis task				*/
/*										*/
/********************************************************************************/

private class EditTask implements Runnable {

   private FileData file_data;
   private String bedrock_id;
   private String for_id;

   EditTask(FileData fd,String bid,String id) {
      file_data = fd;
      bedrock_id = bid;
      for_id = id;
    }

   public void run() {
      try {
	 performEdit();
       }
      finally {
	 doneEdit();
       }
    }

   private void performEdit() {
      if (file_data.getCurrentId(bedrock_id) != null &&
	     !file_data.getCurrentId(bedrock_id).equals(for_id))
	 return;

      long delay = getElideDelay(bedrock_id);

      if (delay > 0) {
	 synchronized (this) {
	    try { wait(delay); }
	    catch (InterruptedException e) { }
	  }
       }

      file_data.getEditableUnit(bedrock_id);
      if (file_data.getCurrentId(bedrock_id) != null &&
	     !file_data.getCurrentId(bedrock_id).equals(for_id))
	 return;

      // System.err.println("BEDROCK: BUILD AST " + for_id);
      CompilationUnit cu = file_data.getAstRoot(bedrock_id,for_id);

      if (file_data.getCurrentId(bedrock_id) != null &&
	     !file_data.getCurrentId(bedrock_id).equals(for_id))
	 return;

      if (getAutoElide(bedrock_id) && cu != null) {
	 // System.err.println("BEDROCK: ELIDE " + for_id);
	 BedrockElider be = file_data.checkElider(bedrock_id);
	 if (be != null) {
	    IvyXmlWriter xw = our_plugin.beginMessage("ELISION",bedrock_id);
	    xw.field("FILE",file_data.getFileName());
	    xw.field("ID",for_id);
	    xw.begin("ELISION");
	    if (be.computeElision(cu,xw)) {
	       if (file_data.getCurrentId(bedrock_id) == null ||
		      file_data.getCurrentId(bedrock_id).equals(for_id)) {
		  xw.end("ELISION");
		  our_plugin.finishMessage(xw);
		}
	     }
	  }
       }
    }

}	// end of innerclass EditTask




/********************************************************************************/
/*										*/
/*	Class for holding data about a file					*/
/*										*/
/********************************************************************************/

private class FileData implements IBufferChangedListener {

   private IProject for_project;
   private String file_name;
   private ICompilationUnit comp_unit;
   private IBuffer default_buffer;
   private Map<String,BufferData> buffer_map;
   private boolean doing_change;
   private String last_ast;
   private String line_separator;

   FileData(String proj,String nm,ICompilationUnit cu) {
      try {
	 for_project = our_plugin.getProjectManager().findProjectForFile(proj,nm);
       }
      catch (BedrockException e) { }
      if (for_project == null) BedrockPlugin.logE("File " + nm + " has no associated project");
      file_name = nm;
      comp_unit = cu;
      doing_change = false;
      last_ast = null;
      buffer_map = new HashMap<String,BufferData>();
      line_separator = System.getProperty("line.separator");
      try {
	 // cu.becomeWorkingCopy(null); 		??? will this work?
	 default_buffer = cu.getBuffer();
	 default_buffer.addBufferChangedListener(this);
	 BedrockPlugin.logD("CREATE FILE " + nm + " " +
			       cu.isWorkingCopy() + " " + cu.hasResourceChanged() + " " +
			       cu.isConsistent() + " " + cu.isOpen() + " " +
			       cu.hasUnsavedChanges() + " " +
			       cu.getBuffer().hasUnsavedChanges() + " " +
			       cu.getBuffer().getLength());
	 checkForOpenEditor(default_buffer,comp_unit);
	 checkLineSeparator();
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Couldn't get default buffer: " + e);
       }
    }

   ICompilationUnit getSearchUnit() {
      if (last_ast != null) {
	 ICompilationUnit icu = getEditableUnit(last_ast);
	 if (icu != null) return icu;
       }
      return comp_unit;
    }

   ICompilationUnit getEditableUnit(String sid) {
      return getBuffer(sid).getEditableUnit();
    }

   CompilationUnit getAstRoot(String sid,String id) {
      last_ast = sid;
      return getBuffer(sid).getAstRoot(id);
    }

   CompilationUnit getDefaultRoot(String sid) {
      return getBuffer(sid).getDefaultRoot();
    }

   void applyEdit(String sid,TextEdit xe) throws BedrockException {
      if (sid == null) {
	 try {
	    comp_unit.applyTextEdit(xe,null);
	  }
	 catch (JavaModelException e) {
	    throw new BedrockException("Problem applying text edit",e);
	  }
       }
      else {
	 getBuffer(sid).applyEdit(xe);
       }
    }

   String getFileName() 			{ return file_name; }
   IProject getProject()			{ return for_project; }
   String getLineSeparator()			{ return line_separator; }

   String getCurrentId(String bid)		{ return getBuffer(bid).getCurrentId(); }
   void setCurrentId(String bid,String id)	{ getBuffer(bid).setCurrentId(id); }

   BedrockElider checkElider(String bid)	{ return getBuffer(bid).checkElider(); }
   void clearElider(String bid) 		{ getBuffer(bid).clearElider(); }
   BedrockElider getElider(String bid)		{ return getBuffer(bid).getElider(); }

   boolean hasChanged() 			{ return default_buffer.hasUnsavedChanges(); }
   String getCurrentContents()			{ return default_buffer.getContents(); }
   int getLength()				{ return default_buffer.getLength(); }

   String getContents(String bid) {
      if (bid == null) {
	 return default_buffer.getContents();
       }

      return getBuffer(bid).getBuffer().getContents();
    }

   void noteEdit(String bid,int soff,int len,int rlen) {
      getBuffer(bid).noteEdit(soff,len,rlen);
    }

   void commit(boolean refresh,boolean save) throws JavaModelException {
      // TODO: This needs to be synchronized correctly among buffers
      for (BufferData bd : buffer_map.values()) {
	 bd.commit(refresh,save);
       }
      default_buffer = comp_unit.getBuffer();
      last_ast = null;
    }

   private BufferData getBuffer(String sid) {
      synchronized (buffer_map) {
	 BufferData bd = buffer_map.get(sid);
	 if (bd == null) {
	    bd = new BufferData(this,sid,comp_unit);
	    buffer_map.put(sid,bd);
	  }
	 return bd;
       }
    }

   @Override public void bufferChanged(BufferChangedEvent evt) {
      if (doing_change) return;
      doing_change = true;
      IBuffer buf = evt.getBuffer();
      IBuffer buf0 = buf;
      int len = evt.getLength();
      int off = evt.getOffset();
      String txt = evt.getText();
      BedrockPlugin.logD("Buffer change " + len + " " + off + " " + (txt == null) + " " +
			    (buf == default_buffer));
      if (len == 0 && off == 0 && txt == null && buf == default_buffer) {
	 BedrockPlugin.logD("Buffer switch occurred for " + file_name);
	 try {
	    default_buffer = comp_unit.getBuffer();
	    buf0 = default_buffer;
	    len = default_buffer.getLength();
	    txt = default_buffer.getContents();
	    buf0.removeBufferChangedListener(this);
	    default_buffer.addBufferChangedListener(this);
	    buf = default_buffer;
	    last_ast = null;
	    if (buf.getContents().equals(txt)) {
	       BedrockPlugin.logD("Buffer contents not changed");
	       return;
	     }
	    // BedrockPlugin.logD("New buffer contents:\n" + buf.getContents());
	    // BedrockPlugin.logD("End of contents");
	  }
	 catch (JavaModelException e) {
	    BedrockPlugin.logD("BUFFER MODEL EXCEPTION " + e);
	  }
       }

      int ctr = 0;
      for (Map.Entry<String,BufferData> ent : buffer_map.entrySet()) {
	 BufferData bd = ent.getValue();
	 IBuffer bdb = bd.getBuffer();
	 if (bdb == null || bdb == buf) continue;
	 IvyXmlWriter xw = our_plugin.beginMessage("EDIT",ent.getKey());
	 BedrockPlugin.logD("START EDIT " + len + " " + off + " " + (ctr++));
	 xw.field("FILE",file_name);
	 xw.field("LENGTH",len);
	 xw.field("OFFSET",off);
	 int xlen = len;
	 if (len == buf.getLength() && off == 0 && txt != null) {
	    xw.field("COMPLETE",true);
	    byte [] data = txt.getBytes();
	    xw.bytesElement("CONTENTS",data);
	    xlen = bdb.getLength();
	    // BedrockPlugin.logD("TEXT = " + txt);
	  }
	 else {
	    xw.cdata(txt);
	  }
	 our_plugin.finishMessage(xw);
	 BedrockPlugin.logD("SENDING EDIT " + xw.toString());
	 bdb.replace(off,xlen,txt);
       }
      if (buf != default_buffer && default_buffer != null) default_buffer.replace(off,len,txt);

      doing_change = false;
    }

   private void checkLineSeparator() {
      line_separator = null;
      int ln = default_buffer.getLength();
      boolean havecr = false;
      for (int i = 0; i < ln; ++i) {
	 char c = default_buffer.getChar(i);
	 if (c == '\r') havecr = true;
	 else if (c == '\n') {
	    if (havecr) line_separator = "\r\n";
	    else line_separator = "\n";
	    break;
	  }
	 else {
	    if (havecr) {
	       line_separator = "\r";
	       break;
	     }
	  }
       }
      if (line_separator == null) {
	 QualifiedName qn0 = new QualifiedName("line","separator");
	 QualifiedName qn1 = new QualifiedName(null,"line.separator");
	 for (IResource ir = default_buffer.getUnderlyingResource(); ir != null; ir = ir.getParent()) {
	    String ls = null;
	    try {
	       ls = ir.getPersistentProperty(qn0);
	       if (ls == null) {
		  ls = ir.getPersistentProperty(qn1);
		  if (ls != null) System.err.println("LINE SEPARATOR OPTION 1");
		}
	     }
	    catch (CoreException e) {
	       System.err.println("EXCEPTION ON LINE SEPARATOR: " + e);
	     }
	    if (ls != null) {
	       System.err.println("LINE SEPARATOR STRING = '" + ls + "'");
	       if (ls.equals("\\n")) line_separator = "\n";
	       else if (ls.equals("\\r\\n")) line_separator = "\r\n";
	       else if (ls.equals("\\r")) line_separator = "\r";
	       else line_separator = ls;
	       break;
	     }
	  }
       }
      if (line_separator == null) line_separator = System.getProperty("line.separator");
    }

}	// end of innerclass FileData





private class BufferData {

   private FileData file_data;
   private String bedrock_id;
   private ICompilationUnit comp_unit;
   private boolean is_changed;
   private String current_id;
   private BedrockElider elision_data;
   private CopyOwner copy_owner;
   private CompilationUnit last_ast;

   BufferData(FileData fd,String bid,ICompilationUnit base) {
      file_data = fd;
      bedrock_id = bid;
      comp_unit = base;
      is_changed = false;
      current_id = null;
      elision_data = null;
      last_ast = null;
      copy_owner = new CopyOwner(file_data,bedrock_id);
    }

   synchronized ICompilationUnit getEditableUnit() {
      if (is_changed) return comp_unit;
      try {
	 // comp_unit.becomeWorkingCopy(null);
	 // comp_unit.getSource() being null causes NullPointerException
	 if (comp_unit.getSource() != null)
	    comp_unit = comp_unit.getWorkingCopy(copy_owner,null);
	 comp_unit.getBuffer().addBufferChangedListener(file_data);
	 is_changed = true;
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem creating working copy: " + e);
       }
      catch (Throwable t) {
	 throw new Error("Problem getting editable unit: " + t,t);
       }

      return comp_unit;
    }

   synchronized CompilationUnit getDefaultRoot() {
      if (last_ast != null) return last_ast;
      ASTParser p = ASTParser.newParser(AST.JLS3);
      p.setKind(ASTParser.K_COMPILATION_UNIT);
      p.setResolveBindings(true);
      p.setSource(comp_unit);
      return (CompilationUnit) p.createAST(null);
    }

   synchronized CompilationUnit getAstRoot(String id) {
      ICompilationUnit icu = getEditableUnit();
      CompilationUnit cu = null;
      try {
	 copy_owner.setId(id);
	 cu = icu.reconcile(AST.JLS3,true,true,null,null);
	 if (cu == null) cu = last_ast;
	 else last_ast = cu;
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem getting AST for file " +
			       file_data.getFileName() + ": " + e);
       }

      return last_ast;
    }

   synchronized void applyEdit(TextEdit xe) throws BedrockException {
      ICompilationUnit icu = getEditableUnit();
      ICompilationUnit bcu = icu.getPrimary();
      try {
	 if (bcu != icu) {
	    // bcu.applyTextEdit(xe,null);
	    icu.applyTextEdit(xe,null);
	  }
	 else {
	    // bcu.applyTextEdit(xe,null);
	    icu.applyTextEdit(xe,null);
	  }
       }
      catch (JavaModelException e) {
	 throw new BedrockException("Problem editing source file " + file_data.getFileName(),e);
       }
      last_ast = null;
    }

   IBuffer getBuffer() {
      if (!is_changed) return null;
      try {
	 return comp_unit.getBuffer();
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem getting compilation unit buffer: " + e);
       }

      return null;
    }

   String getCurrentId()			{ return current_id; }
   void setCurrentId(String id) 		{ current_id = id; }

   BedrockElider checkElider()			{ return elision_data; }
   void clearElider()				{ elision_data = null; }
   synchronized BedrockElider getElider() {
      if (elision_data == null) {
	 elision_data = new BedrockElider();
       }
      return elision_data;
    }

   void noteEdit(int soff,int len,int rlen) {
      if (elision_data != null) elision_data.noteEdit(soff,len,rlen);
    }

   synchronized void commit(boolean refresh,boolean save) throws JavaModelException {
      if (comp_unit != null) {
	 if (save) {
	    BedrockPlugin.log("COMMITING " + file_data.getFileName() + " " +
				 comp_unit.isWorkingCopy() + " " +
				 comp_unit.hasResourceChanged() + " " +
				 comp_unit.getElementName() + " " +
				 comp_unit.hasUnsavedChanges() + " " +
				 comp_unit.isConsistent());
	    if (comp_unit.isWorkingCopy()) {
	       comp_unit.commitWorkingCopy(true,new BedrockProgressMonitor(our_plugin,"Committing"));
	     }
	    comp_unit.save(new BedrockProgressMonitor(our_plugin,"Saving"),false);
	    // try this for now
	    try {
	       comp_unit = comp_unit.getWorkingCopy(
		  copy_owner,
		  new BedrockProgressMonitor(our_plugin,"Updating"));
	     }
	    catch (Throwable t) {
	       BedrockPlugin.logE("Problem get working copy after a save",t);
	     }
	  }
	 else if (refresh) {
	    BedrockProgressMonitor bpm = new BedrockProgressMonitor(our_plugin,"Refreshing");
	    comp_unit.restore();
	    comp_unit = comp_unit.getPrimary();
	    comp_unit.restore();
	    comp_unit.makeConsistent(bpm);
	    comp_unit = comp_unit.getWorkingCopy(copy_owner,null);
	  }
       }
    }

}	// end of innerclass BufferData





/********************************************************************************/
/*										*/
/*	Working copy owner							*/
/*										*/
/********************************************************************************/

private class CopyOwner extends WorkingCopyOwner {

   private FileData for_file;
   private String bedrock_id;
   private ProblemHandler problem_handler;

   CopyOwner(FileData fd,String bid) {
      for_file = fd;
      bedrock_id = bid;
      problem_handler = null;
    }

   @Override public IProblemRequestor getProblemRequestor(ICompilationUnit ic) {
      if (problem_handler == null) problem_handler = new ProblemHandler(for_file,bedrock_id,ic);
      return problem_handler;
    }

   void setId(String id) {
      if (problem_handler != null) problem_handler.setId(id);
    }

}	// end of innerclass CopyOwner




/********************************************************************************/
/*										*/
/*	Class to handle compilation problems					*/
/*										*/
/********************************************************************************/

private class ProblemHandler implements IProblemRequestor {

   private FileData file_data;
   private String bedrock_id;
   private List<IProblem> problem_set;
   private String for_id;

   ProblemHandler(FileData fd,String bid,ICompilationUnit cu) {
      file_data = fd;
      bedrock_id = bid;
      for_id = null;
      problem_set = null;
    }

   void setId(String id)			{ for_id = id; }

   public void acceptProblem(IProblem ip) {
      if (problem_set == null) problem_set = new ArrayList<IProblem>();
      problem_set.add(ip);
    }

   public void beginReporting() {
      problem_set = null;
    }

   public void endReporting() {
      if (for_id != null && !for_id.equals(file_data.getCurrentId(bedrock_id))) return;

      IvyXmlWriter xw;
      if (for_id != null) {
	 xw = our_plugin.beginMessage("EDITERROR",bedrock_id);
	 xw.field("FILE",file_data.getFileName());
	 xw.field("ID",for_id);
       }
      else if (file_data != null) {
	 xw = our_plugin.beginMessage("FILEERROR",bedrock_id);
	 xw.field("FILE",file_data.getFileName());
       }
      else return;

      if (file_data != null && file_data.getProject() != null) {
	 xw.field("PROJECT",file_data.getProject().getName());
       }
      xw.begin("MESSAGES");
      if (problem_set != null) {
	 for (IProblem ip : problem_set) {
	    BedrockUtil.outputProblem(file_data.getProject(),ip,xw);
	  }
       }
      xw.end("MESSAGES");

      if (for_id != null && !for_id.equals(file_data.getCurrentId(bedrock_id))) return;
      our_plugin.finishMessage(xw);

      BedrockPlugin.logD("ERROR REPORT: " + xw.toString());
    }

   public boolean isActive()			{ return true; }

}	// end of innerclass ProblemHandler



/********************************************************************************/
/*										*/
/*	Class to handle completion information					*/
/*										*/
/********************************************************************************/

private class CompletionHandler extends CompletionRequestor {

   private IvyXmlWriter xml_writer;
   private String bedrock_id;
   private boolean generate_message;

   CompletionHandler(IvyXmlWriter xw,String bid) {
      setRequireExtendedContext(true);
      setAllowsRequiredProposals(CompletionProposal.FIELD_REF,CompletionProposal.TYPE_REF,true);
      setAllowsRequiredProposals(CompletionProposal.FIELD_REF,CompletionProposal.TYPE_IMPORT,true);
      setAllowsRequiredProposals(CompletionProposal.FIELD_REF,CompletionProposal.FIELD_IMPORT,true);
      for (int i = 1; i <= 25; ++i) {
	 setIgnored(i,false);
       }

      bedrock_id = bid;
      xml_writer = xw;
      if (xw != null) {
	 generate_message = false;
       }
      else {
	 generate_message = true;
	 xml_writer = our_plugin.beginMessage("COMPLETIONS",bedrock_id);
       }
    }

   @Override public void beginReporting() {
      xml_writer.begin("COMPLETIONS");
    }

   @Override public void accept(CompletionProposal cp) {
      BedrockUtil.outputCompletion(cp,xml_writer);
    }

   @Override public void endReporting() {
      xml_writer.end("COMPLETIONS");
      if (generate_message) our_plugin.finishMessage(xml_writer);
    }

   @Override public void completionFailure(IProblem ip) {
      BedrockUtil.outputProblem(null,ip,xml_writer);
    }

}	// end of innerclass CompletionHandler



/********************************************************************************/
/*										*/
/*	Class to hold parameter settings for bedrock client			*/
/*										*/
/********************************************************************************/

private boolean getAutoElide(String id) { return getParameters(id).getAutoElide(); }
private long getElideDelay(String id)	{ return getParameters(id).getElideDelay(); }

private void setAutoElide(String id,boolean v)	{ getParameters(id).setAutoElide(v); }
private void setElideDelay(String id,long v)	{ getParameters(id).setElideDelay(v); }


private ParamSettings getParameters(String id)
{
   ParamSettings ps = param_map.get(id);
   if (ps == null) {
      ps = new ParamSettings();
      param_map.put(id,ps);
    }
   return ps;
}



private static class ParamSettings {

   private boolean auto_elide;
   private long    elide_delay;

   ParamSettings() {
      auto_elide = false;
      elide_delay = 0;
    }

   boolean getAutoElide()		{ return auto_elide; }
   long getElideDelay() 		{ return elide_delay; }

   void setAutoElide(boolean fg)	{ auto_elide = fg; }
   void setElideDelay(long v)		{ elide_delay = v; }

}	// end of inner class ParamSettings





}	// end of class BedrockEditor



/* end of BedrockEditor.java */

