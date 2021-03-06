/********************************************************************************/
/*										*/
/*		BedrockJava.java						*/
/*										*/
/*	Handle java-related commands for Bubbles				*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.search.core.text.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class BedrockJava implements BedrockConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockJava(BedrockPlugin bp)
{
   our_plugin = bp;
}




/********************************************************************************/
/*										*/
/*	GET ALL NAMES command							*/
/*										*/
/********************************************************************************/

void getAllNames(String proj,String bid,Set<String> files,String bkg,IvyXmlWriter xw) throws BedrockException
{
   NameThread nt = null;

   if (bkg != null) nt = new NameThread(bid,bkg,files);

   if (proj != null) {
      IJavaProject jp = getJavaProject(proj);
      handleAllNames(jp,files,nt,xw);
    }
   else {
      IJavaElement [] ps = getAllProjects();
      for (int i = 0; i < ps.length; ++i) {
	 handleAllNames(ps[i],files,nt,xw);
       }
    }

   if (nt != null) nt.start();
}



private void handleAllNames(IJavaElement jp,Set<String> files,NameThread nt,IvyXmlWriter xw)
{
   if (jp == null) return;

   if (nt == null) BedrockUtil.outputJavaElement(jp,files,true,xw);
   else nt.addElement(jp);
}



private class NameThread extends Thread {

   private String bump_id;
   private String name_id;
   private Map<IJavaElement,Boolean> separate_elements;
   private Set<String> file_set;

   NameThread(String bid,String nid,Set<String> fset) {
      super("Bedrock_GetNames");
      bump_id = bid;
      name_id = nid;
      separate_elements = new HashMap<IJavaElement,Boolean>();
      file_set = fset;
    }

   void addElement(IJavaElement je) {
      boolean dochld = false;
      boolean doelt = false;

      switch (je.getElementType()) {
	 case IJavaElement.CLASS_FILE :
	    return;
	 case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	    IPackageFragmentRoot ipfr = (IPackageFragmentRoot) je;
	    try {
	       if (!ipfr.isArchive() && !ipfr.isExternal() &&
		      ipfr.getKind() == IPackageFragmentRoot.K_SOURCE)
		  dochld = true;
	     }
	    catch (JavaModelException e) { }
	    break;
	 case IJavaElement.PACKAGE_FRAGMENT :
	 case IJavaElement.JAVA_PROJECT :
	    dochld = true;
	    doelt = true;
	    break;
	 case IJavaElement.JAVA_MODEL :
	 case IJavaElement.IMPORT_CONTAINER :
	 case IJavaElement.IMPORT_DECLARATION :
	 case IJavaElement.TYPE_PARAMETER :
	 case IJavaElement.PACKAGE_DECLARATION :
	 default :
	    dochld = true;
	    break;
	 case IJavaElement.COMPILATION_UNIT :
	    dochld = false;
	    doelt = true;
	    break;
	 case IJavaElement.FIELD :
	 case IJavaElement.METHOD :
	 case IJavaElement.INITIALIZER :
	 case IJavaElement.TYPE :
	 case IJavaElement.LOCAL_VARIABLE :
	    dochld = false;
	    break;
       }

      if (dochld) {
	 if (doelt) separate_elements.put(je,Boolean.FALSE);
	 if (je instanceof IParent) {
	    try {
	       for (IJavaElement c : ((IParent) je).getChildren()) {
		  addElement(c);
		}
	     }
	    catch (JavaModelException e) { }
	    catch (Throwable e) {
	       BedrockPlugin.logE("Problem geting children for all names: " + e);
	     }
	  }
       }
      else if (doelt) {
	 separate_elements.put(je,Boolean.TRUE);
       }
    }

   @Override public void run() {
      BedrockPlugin.logD("START NAMES FOR " + name_id);

      IvyXmlWriter xw = null;

      try {
	 for (Map.Entry<IJavaElement,Boolean> ent : separate_elements.entrySet()) {
	    if (xw == null) {
	       xw = our_plugin.beginMessage("NAMES",bump_id);
	       xw.field("NID",name_id);
	     }
	    boolean cfg = ent.getValue();
	    BedrockUtil.outputJavaElement(ent.getKey(),file_set,cfg,xw);
	    if (xw.getLength() <= 0 || xw.getLength() > 1000000) {
	       our_plugin.finishMessageWait(xw,15000);
	       // BedrockPlugin.logD("OUTPUT NAMES: " + xw.toString());
	       xw = null;
	     }
	  }

	 if (xw != null) {
	    our_plugin.finishMessageWait(xw);
	  }
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem getting names",t);
       }
      finally {
	 BedrockPlugin.logD("FINISH NAMES FOR " + name_id);
	 xw = our_plugin.beginMessage("ENDNAMES",bump_id);
	 xw.field("NID",name_id);
	 our_plugin.finishMessage(xw);
       }
    }

}	// end of inner class NameThread




/********************************************************************************/
/*										*/
/*	FIND ALL command							*/
/*										*/
/********************************************************************************/

void handleFindAll(String proj,String file,int start,int end,boolean defs,boolean refs,
		      boolean impls,boolean equiv,boolean exact,boolean system,boolean typeof,
		      boolean ronly,boolean wonly,
		      IvyXmlWriter xw) throws BedrockException
{
   IJavaProject ijp = getJavaProject(proj);
   IPath fp = new Path(file);

   int limit = 0;
   if (defs && refs) limit = IJavaSearchConstants.ALL_OCCURRENCES;
   else if (defs) limit = IJavaSearchConstants.DECLARATIONS;
   else if (refs) limit = IJavaSearchConstants.REFERENCES;
   int flimit = limit;
   if (ronly) flimit = IJavaSearchConstants.READ_ACCESSES;
   else if (wonly) flimit = IJavaSearchConstants.WRITE_ACCESSES;

   int mrule = -1;
   if (equiv) mrule = SearchPattern.R_EQUIVALENT_MATCH;
   else if (exact) mrule = SearchPattern.R_EXACT_MATCH;

   SearchPattern pat = null;
   IJavaSearchScope scp = null;

   ICompilationUnit icu = our_plugin.getProjectManager().getCompilationUnit(proj,file);
   if (icu == null) throw new BedrockException("Compilation unit not found for " + fp);
   icu = getCompilationElement(icu);

   ICompilationUnit [] working = null;
   FindFilter filter = null;

   IJavaElement cls = null;
   char [] packagename = null;
   char [] typename = null;

   try {
      BedrockPlugin.logD("Getting search scopes");
      IJavaElement [] pelt;
      if (ijp != null) pelt = new IJavaElement[] { ijp };
      else pelt = getAllProjects();
      working = getWorkingElements(pelt);
      int fg = IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS;
      if (system) fg |= IJavaSearchScope.SYSTEM_LIBRARIES | IJavaSearchScope.APPLICATION_LIBRARIES;
      scp = SearchEngine.createJavaSearchScope(pelt,fg);

      BedrockPlugin.logD("Locating item to search for");
      IJavaElement [] elts = icu.codeSelect(start,end-start);

      if (typeof) {
	 Set<IJavaElement> nelt = new LinkedHashSet<IJavaElement>();
	 for (int i = 0; i < elts.length; ++i) {
	    IType typ = null;
	    String tnm = null;
	    switch (elts[i].getElementType()) {
	       case IJavaElement.FIELD :
		  tnm = ((IField) elts[i]).getTypeSignature();
		  break;
	       case IJavaElement.LOCAL_VARIABLE :
		  tnm = ((ILocalVariable) elts[i]).getTypeSignature();
		  break;
	       case IJavaElement.METHOD :
		  typ = ((IMethod) elts[i]).getDeclaringType();
		  break;
	       default :
		  nelt.add(elts[i]);
		  break;
	      }
	    if (typ != null) nelt.add(typ);
	    else if (tnm != null && ijp != null) {
	       IJavaElement elt = ijp.findElement(tnm,null);
	       if (elt != null) {
		  nelt.add(elt);
		  typ = null;
		}
	       else {
		  while (tnm.startsWith("[")) {
		     String xtnm = tnm.substring(1);
		     if (xtnm == null) break;
		     tnm = xtnm;
		   }
		  int ln = tnm.length();
		  String xtnm = tnm;
		  if (tnm.startsWith("L") && tnm.endsWith(";")) {
		     xtnm = tnm.substring(1,ln-1);
		   }
		  else if (tnm.startsWith("Q") && tnm.endsWith(";")) {
		     xtnm = tnm.substring(1,ln-1);
		   }
		  if (xtnm != null) tnm = xtnm;
		  int idx1 = tnm.lastIndexOf(".");
		  if (idx1 > 0) {
		     String pkgnm = tnm.substring(0,idx1);
		     xtnm = tnm.substring(idx1+1);
		     if (xtnm != null) tnm = xtnm;
		     pkgnm = pkgnm.replace('$','.');
		     packagename = pkgnm.toCharArray();
		   }
		  tnm = tnm.replace('$','.');
		  typename = tnm.toCharArray();
		}

	       if (typename != null) {
		  BedrockPlugin.logD("Handling type names");
		  FindTypeHandler fth = new FindTypeHandler(ijp);
		  SearchEngine se = new SearchEngine(working);
		  se.searchAllTypeNames(packagename,SearchPattern.R_EXACT_MATCH,
					   typename,SearchPattern.R_EXACT_MATCH,
					   IJavaSearchConstants.TYPE,
					   scp,fth,
					   IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,null);
		  nelt.addAll(fth.getFoundItems());
		}
	     }
	  }
	 IJavaElement [] nelts = new IJavaElement[nelt.size()];
	 elts = nelt.toArray(nelts);
       }

      if (elts.length == 1 && !typeof) {
	 xw.begin("SEARCHFOR");
	 switch (elts[0].getElementType()) {
	    case IJavaElement.FIELD :
	       xw.field("TYPE","Field");
	       break;
	    case IJavaElement.LOCAL_VARIABLE :
	       xw.field("TYPE","Local");
	       break;
	    case IJavaElement.METHOD :
	       xw.field("TYPE","Function");
	       break;
	    case IJavaElement.TYPE :
	    case IJavaElement.TYPE_PARAMETER :
	       xw.field("TYPE","Class");
	       cls = elts[0];
	       break;
	   }
	 xw.text(elts[0].getElementName());
	 xw.end("SEARCHFOR");
       }
      int etyp = -1;
      for (int i = 0; i < elts.length; ++i) {
	 SearchPattern sp;
	 int xlimit = limit;
	 switch (elts[i].getElementType()) {
	    case IJavaElement.FIELD :
	    case IJavaElement.LOCAL_VARIABLE :
	       xlimit = flimit;
	       break;
	    case IJavaElement.TYPE :
	       if (impls) xlimit = IJavaSearchConstants.IMPLEMENTORS;
	       break;
	    case IJavaElement.METHOD :
	       if (impls) xlimit |= IJavaSearchConstants.IGNORE_DECLARING_TYPE;
	       break;									
	  }
	 if (mrule < 0) sp = SearchPattern.createPattern(elts[i],xlimit);
	 else sp = SearchPattern.createPattern(elts[i],xlimit,mrule);
	 if (pat == null) pat = sp;
	 else pat = SearchPattern.createOrPattern(pat,sp);
	 if (etyp < 0) etyp = elts[i].getElementType();
       }

      if (etyp == IJavaElement.METHOD) {
	 if (impls) {
	    if (defs) filter = new ImplementFilter(elts);
	  }
	 else if (defs && !refs) filter = new ClassFilter(elts);
       }

    }
   catch (JavaModelException e) {
      BedrockPlugin.logE("SEARCH PROBLEM: " + e);
      e.printStackTrace();
      throw new BedrockException("Can't find anything to search for",e);
    }

   if (pat == null) {
      BedrockPlugin.logW("Nothing to search for");
      return;
    }

   BedrockPlugin.logD("Setting up search");
   SearchEngine se = new SearchEngine(working);
   SearchParticipant [] parts = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
   FindHandler fh = new FindHandler(xw,filter,false);

   BedrockPlugin.logD("BEGIN SEARCH " + pat + " " + parts.length + " " + " " + scp + " :: COPIES: " + working.length);

   try {
      se.search(pat,parts,scp,fh,null);
    }
   catch (Throwable e) {
      throw new BedrockException("Problem doing find all search for " + pat + ": " + e,e);
    }

   if (cls != null && defs) {		// need to add the actual class definition
      BedrockUtil.outputJavaElement(cls,false,xw);
    }
}



private static class ImplementFilter implements FindFilter {

   private Set<String> base_types;

   ImplementFilter(IJavaElement [] elts) {
      base_types = new HashSet<String>();
      for (IJavaElement je : elts) {
	 if (je instanceof IMember) {
	    IMember im = (IMember) je;
	    IType ty = im.getDeclaringType();
	    String nm = ty.getFullyQualifiedName().replace('$','.');
	    // BedrockPlugin.logD("ADD FILTER TYPE " + nm);
	    base_types.add(nm);
	  }
       }
    }

   public boolean checkMatch(SearchMatch sm) {
      Object o = sm.getElement();
      if (!(o instanceof IMember)) return false;
      IMember je = (IMember) o;
      if (base_types.size() == 0) return true;
      IType ty = je.getDeclaringType();
      String nm = ty.getFullyQualifiedName().replace('$','.');
      // BedrockPlugin.logD("CHECK FILTER " + ty + " " + nm);
      if (base_types.contains(nm)) return true;
      try {
	 ITypeHierarchy th = ty.newTypeHierarchy(null);
	 for (IType st : th.getAllSupertypes(ty)) {
	    String snm = st.getFullyQualifiedName().replace('$','.');
	    // BedrockPlugin.logD("CHECK SUPERTYPE " + snm);
	    if (base_types.contains(snm)) return true;
	  }
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem building type hierarchy: " + e);
       }
      return false;
    }

}



private static class ClassFilter implements FindFilter {

   private Set<String> base_types;

   ClassFilter(IJavaElement [] elts) {
      base_types = new HashSet<String>();
      for (IJavaElement je : elts) {
	 if (je instanceof IMember) {
	    IMember im = (IMember) je;
	    IType ty = im.getDeclaringType();
	    String nm = ty.getFullyQualifiedName().replace('$','.');
	    // BedrockPlugin.logD("ADD FILTER TYPE " + nm);
	    base_types.add(nm);
	  }
       }
    }

   ClassFilter(String cls) {
      base_types = new HashSet<String>();
      // BedrockPlugin.logD("ADD FILTER TYPE " + cls);
      base_types.add(cls);
    }

   public boolean checkMatch(SearchMatch sm) {
      Object o = sm.getElement();
      if (!(o instanceof IMember)) return false;
      IMember je = (IMember) o;
      if (base_types.size() == 0) return true;
      IType ty = je.getDeclaringType();
      if (ty == null) return true;
      String nm = ty.getFullyQualifiedName().replace('$','.');
      // BedrockPlugin.logD("CHECK FILTER " + nm);
      if (base_types.contains(nm)) return true;
      return false;
    }

}



private static class FindTypeHandler extends TypeNameRequestor {

   private IJavaProject java_project;
   private List<IJavaElement> found_items;

   FindTypeHandler(IJavaProject ijp) {
      java_project = ijp;
      found_items = new ArrayList<IJavaElement>();
    }

   @Override public void acceptType(int mods,char [] pkg,char [] typ,char [][] encs,String path) {
      String snm = "";
      if (pkg != null) snm = new String(pkg) + ".";
      for (int i = 0; i < encs.length; ++i) {
	 String enm = new String(encs[i]);
	 snm += enm + ".";
       }
      snm += new String(typ);
      IJavaElement elt = null;
      try {
	 elt = java_project.findType(snm);
       }
      catch (JavaModelException ex) {
	 BedrockPlugin.logE("Problem looking up type " + snm,ex);
       }
      BedrockPlugin.logD("ACCEPT TYPE " + snm + " " + elt + " " + path);
      if (elt != null) found_items.add(elt);
    }

   List<IJavaElement> getFoundItems()		{ return found_items; }


}	// end of inner class FindTypeHandler


/********************************************************************************/
/*										*/
/*	OPEN ECLIPSE EDITOR command						*/
/*										*/
/********************************************************************************/

void handleOpenEditor(String proj, String filePath, int lineNumber)
{
   File file = new File(filePath);

   if (file.exists() && file.isFile()) {
      try {
	 if (PlatformUI.getWorkbench() == null) return;
       }
      catch (IllegalStateException e) {
	 return;
       }

      IWorkbenchWindow[] wins = PlatformUI.getWorkbench().getWorkbenchWindows();
      int cnt = wins.length;
      IWorkbenchWindow workbenchWindow = (cnt > 0) ? wins[0] : null;

      if (workbenchWindow != null) {
	 IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
	 //final IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());

	 IPath path = new Path(filePath);
	 IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(proj);
	 //TODO: This doesn't work for linked in files

	 String ploc = project.getLocation().toString();
	 int ln = ploc.length() + 1;
	 if (path.toString().startsWith(ploc)) {
	    String relativePath = path.toString().substring(ln);
	    IFile iFile = project.getFile(relativePath);
	    Integer lineNumberInt = Integer.valueOf(lineNumber);

	    if (workbenchPage != null) {
	       workbenchWindow.getShell().getDisplay().syncExec(new OpenEditorRunnable(workbenchWindow, workbenchPage, iFile, lineNumberInt));
	     }
	  }
       }
    }
}



private static class OpenEditorRunnable implements Runnable {

   private IWorkbenchPage workbench_page;
   private IWorkbenchWindow workbench_window;
   private IFile i_file;
   private Integer line_number;

   OpenEditorRunnable(IWorkbenchWindow workbenchWindow, IWorkbenchPage workbenchPage, IFile ifile, Integer lineNumber) {
      workbench_page = workbenchPage;
      workbench_window = workbenchWindow;
      i_file = ifile;
      line_number = lineNumber;
   }

   public void run() {
      try {
	 final IMarker marker = i_file.createMarker(IMarker.TEXT);;

	 HashMap<String, Object> map = new HashMap<String, Object>();
	 map.put(IMarker.LINE_NUMBER, line_number);

	 marker.setAttributes(map);

	 IDE.openEditor(workbench_page, marker).setFocus();
	 marker.delete();

	 workbench_window.getShell().forceActive();
	 workbench_window.getShell().forceFocus();
       }
      catch(PartInitException pie) {
	 BedrockPlugin.logE("PartInitException: " + pie);
       }
      catch(CoreException ce) {
	 BedrockPlugin.logE("CoreException: " + ce);
       }
    }
 }



/********************************************************************************/
/*										*/
/*	Java Search command							*/
/*										*/
/********************************************************************************/

void handleJavaSearch(String proj,String bid,String patstr,String foritems,
			 boolean defs,boolean refs,boolean impls,
			 boolean equiv,boolean exact,boolean system,
			 IvyXmlWriter xw) throws BedrockException
{
   IJavaProject ijp = getJavaProject(proj);

   our_plugin.waitForEdits();

   int forflags = 0;
   if (foritems == null) forflags = IJavaSearchConstants.TYPE;
   else if (foritems.equalsIgnoreCase("CLASS")) forflags = IJavaSearchConstants.CLASS;
   else if (foritems.equalsIgnoreCase("INTERFACE")) forflags = IJavaSearchConstants.INTERFACE;
   else if (foritems.equalsIgnoreCase("ENUM")) forflags = IJavaSearchConstants.ENUM;
   else if (foritems.equalsIgnoreCase("ANNOTATION")) forflags = IJavaSearchConstants.ANNOTATION_TYPE;
   else if (foritems.equalsIgnoreCase("CLASS&ENUM")) forflags = IJavaSearchConstants.CLASS_AND_ENUM;
   else if (foritems.equalsIgnoreCase("CLASS&INTERFACE")) forflags = IJavaSearchConstants.CLASS_AND_INTERFACE;
   else if (foritems.equalsIgnoreCase("TYPE")) forflags = IJavaSearchConstants.TYPE;
   else if (foritems.equalsIgnoreCase("FIELD")) forflags = IJavaSearchConstants.FIELD;
   else if (foritems.equalsIgnoreCase("METHOD")) forflags = IJavaSearchConstants.METHOD;
   else if (foritems.equalsIgnoreCase("CONSTRUCTOR")) forflags = IJavaSearchConstants.CONSTRUCTOR;
   else if (foritems.equalsIgnoreCase("PACKAGE")) forflags = IJavaSearchConstants.PACKAGE;
   else if (foritems.equalsIgnoreCase("FIELDWRITE"))
      forflags = IJavaSearchConstants.FIELD | IJavaSearchConstants.WRITE_ACCESSES;
   else if (foritems.equalsIgnoreCase("FIELDREAD"))
      forflags = IJavaSearchConstants.FIELD | IJavaSearchConstants.READ_ACCESSES;
   else forflags = IJavaSearchConstants.TYPE;

   int limit = 0;
   if (defs && refs) limit = IJavaSearchConstants.ALL_OCCURRENCES;
   else if (defs) limit = IJavaSearchConstants.DECLARATIONS;
   else if (refs) limit = IJavaSearchConstants.REFERENCES;
   else if (impls) limit = IJavaSearchConstants.IMPLEMENTORS;

   int mrule = SearchPattern.R_PATTERN_MATCH;
   if (equiv) mrule = SearchPattern.R_EQUIVALENT_MATCH;
   else if (exact) mrule = SearchPattern.R_EXACT_MATCH;

   SearchPattern pat = SearchPattern.createPattern(patstr,forflags,limit,mrule);
   if (pat == null) {
      throw new BedrockException("Invalid java search pattern `" + patstr + "' " + forflags + " " +
				    limit + " " + mrule);
    }

   FindFilter filter = null;
   if (forflags == IJavaSearchConstants.METHOD) {
      String p = patstr;
      int idx = p.indexOf("(");
      if (idx > 0) p = p.substring(0,idx);
      idx = p.lastIndexOf(".");
      if (idx > 0) {
	 if (defs) filter = new ClassFilter(p.substring(0,idx));
       }
    }

   // TODO: create scope for only user's items
   IJavaElement [] pelt;
   if (ijp != null) pelt = new IJavaElement[] { ijp };
   else pelt = getAllProjects();

   ICompilationUnit [] working = getWorkingElements(pelt);
   for (ICompilationUnit xcu : working) {
      try {
	 BedrockPlugin.logD("WORK WITH " + xcu.isWorkingCopy() + " " + xcu.getPath() +
			       xcu.getSourceRange());
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logD("WORK WITH ERROR: " + e);
       }
    }

   IJavaSearchScope scp = null;
   int fg = IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS;
   if (system) {
      fg |= IJavaSearchScope.SYSTEM_LIBRARIES | IJavaSearchScope.APPLICATION_LIBRARIES;
      scp = SearchEngine.createWorkspaceScope();
    }
   else {
      scp = SearchEngine.createJavaSearchScope(pelt,fg);
    }

   SearchEngine se = new SearchEngine(working);
   SearchParticipant [] parts = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
   FindHandler fh = new FindHandler(xw,filter,system);

   BedrockPlugin.logD("BEGIN SEARCH " + pat);
   BedrockPlugin.logD("SEARCH SCOPE " + system + " " + fg + " " + scp);

   try {
      se.search(pat,parts,scp,fh,null);
    }
   catch (Throwable e) {
      throw new BedrockException("Problem doing Java search: " + e,e);
    }
}



/********************************************************************************/
/*										*/
/*	Callbacks for Java search handling					*/
/*										*/
/********************************************************************************/

private static class FindHandler extends SearchRequestor {

   private boolean allow_system;
   private FindFilter find_filter;
   private IvyXmlWriter xml_writer;

   FindHandler(IvyXmlWriter xw,FindFilter ff,boolean sys) {
      xml_writer = xw;
      find_filter = ff;
      allow_system = sys;
    }

   @Override public void acceptSearchMatch(SearchMatch mat) {
      // BedrockPlugin.logD("FOUND MATCH " + mat);
      if (reportMatch(mat)) {
	 BedrockUtil.outputSearchMatch(mat,xml_writer);
       }
    }

   private boolean reportMatch(SearchMatch mat) {
      IResource irc = mat.getResource();
      if (!allow_system && irc.getType() != IResource.FILE) return false;
      if (find_filter != null) return find_filter.checkMatch(mat);
      return true;
    }

}	// end of subclass FindHandler




private static interface FindFilter {

   boolean checkMatch(SearchMatch mat);

}



/********************************************************************************/
/*										*/
/*	TEXT SEARCH commands							*/
/*										*/
/*	These need to be reimplemented so as not to require the workspace	*/
/*	to be set up within the user interface. 				*/
/*										*/
/********************************************************************************/

void textSearch(String proj,int regexpfgs,String pat,int max,IvyXmlWriter xw)
	throws BedrockException
{
   String fp = ".*\\.java";

   TextSearchEngine se = TextSearchEngine.createDefault();

   TextSearchScope scp = getSearchScope(proj,fp);
   if (scp == null) throw new BedrockException("No search scope given");

   IJavaElement [] pelt;
   if (proj != null) pelt = new IJavaElement[] { getJavaProject(proj) };
   else pelt = getAllProjects();
   pelt = getSearchElements(pelt);
   ICompilationUnit [] units = getCompilationElements(pelt);

   Pattern pp;
   try {
      pp = Pattern.compile(pat,regexpfgs);
    }
   catch (PatternSyntaxException e) {
      pp = Pattern.compile(pat,regexpfgs | Pattern.LITERAL);
    }

   SearchHandler sh = new SearchHandler(xw,units,max);

   se.search(scp,sh,pp,new BedrockProgressMonitor(our_plugin,"Doing text search"));
}



private TextSearchScope getSearchScope(String proj,String filepat)
{
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IResource [] roots = new IResource[1];

   if (proj == null || proj.equals("*")) {
      roots[0] = wr;
    }
   else {
      roots[0] = wr.getProject(proj);
      if (roots[0] == null) return null;
    }

   Pattern jpp = Pattern.compile(filepat);

   TextSearchScope tss = TextSearchScope.newSearchScope(roots,jpp,true);

   return tss;
}




private static class SearchHandler extends TextSearchRequestor {

   private IvyXmlWriter xml_writer;
   private ICompilationUnit [] base_units;
   private int	max_results;
   private int	num_results;

   SearchHandler(IvyXmlWriter xw,ICompilationUnit [] units,int max) {
      xml_writer = xw;
      base_units = units;
      max_results = max;
      num_results = 0;
    }

   @Override public boolean acceptPatternMatch(TextSearchMatchAccess acc) {
      if (++num_results > max_results) return false;
      xml_writer.begin("MATCH");
      xml_writer.field("STARTOFFSET",acc.getMatchOffset());
      xml_writer.field("LENGTH",acc.getMatchLength());
      File f = acc.getFile().getLocation().toFile();
      xml_writer.textElement("FILE",f.toString());
      IJavaElement elt = getElement(acc.getFile(),acc.getMatchOffset(),acc.getMatchLength());
      if (elt != null) BedrockUtil.outputJavaElement(elt,false,xml_writer);
      xml_writer.end("MATCH");
      return true;
    }

   private IJavaElement getElement(IFile r,int off,int len) {
      for (ICompilationUnit icu : base_units) {
	 IResource ir = icu.getResource();
	 if (ir == null) return null;
	 if (ir.equals(r)) {
	    return findInnerElement(icu,off,len);
	  }
       }
      return null;
    }

   private IJavaElement findInnerElement(IJavaElement elt,int off,int len) {
      if (!(elt instanceof ISourceReference)) return null;
      ISourceReference sref = (ISourceReference) elt;
      try {
	 ISourceRange rng = sref.getSourceRange();
	 if (rng.getOffset() > off || rng.getOffset() + rng.getLength() < off + len) return null;
       }
      catch (JavaModelException ex) {
	 BedrockPlugin.logE("Problem getting range: " + ex);
	 return null;
       }

      if (!(elt instanceof IParent)) return elt;

      IParent par = (IParent) elt;
      try {
	 for (IJavaElement je : par.getChildren()) {
	    IJavaElement fe = findInnerElement(je,off,len);
	    if (fe != null) return fe;
	  }
       }
      catch (JavaModelException ex) {
	 BedrockPlugin.logE("Problem getting children: " + ex);
       }

      return elt;
    }

}	// end of subclass SearchHandler




/********************************************************************************/
/*										*/
/*	FULLYQUALIFIEDNAME command						*/
/*										*/
/********************************************************************************/

void getFullyQualifiedName(String proj,String file,int start,int end,IvyXmlWriter xw)
	throws BedrockException
{
   String name = null;
   String key = null;
   String sgn = null;
   String hdl = null;
   ICompilationUnit icu = our_plugin.getProjectManager().getCompilationUnit(proj,file);
   if (icu == null) throw new BedrockException("Compilation unit not found for " + file);
   icu = getCompilationElement(icu);

   try {
      IJavaElement [] elts = icu.codeSelect(start,end-start);
      for (int i = 0; i < elts.length && name == null; ++i) {
	 switch (elts[i].getElementType()) {
	    case IJavaElement.JAVA_PROJECT :
	    case IJavaElement.JAVA_MODEL :
	    case IJavaElement.PACKAGE_FRAGMENT_ROOT :
	    case IJavaElement.CLASS_FILE :
	    case IJavaElement.PACKAGE_FRAGMENT :
	    case IJavaElement.IMPORT_CONTAINER :
	    case IJavaElement.IMPORT_DECLARATION :
	    case IJavaElement.TYPE_PARAMETER :
	    case IJavaElement.COMPILATION_UNIT :
	    default :
	       break;
	    case IJavaElement.TYPE :
	       IType typ = (IType) elts[i];
	       name = typ.getFullyQualifiedName();
	       key = typ.getKey();
	       break;
	    case IJavaElement.FIELD :
	       IField fld = ((IField) elts[i]);
	       name = fld.getDeclaringType().getFullyQualifiedName() + "." + fld.getElementName();
	       key = fld.getKey();
	       sgn = fld.getTypeSignature();
	       break;
	    case IJavaElement.METHOD :
	       IMethod mthd = ((IMethod) elts[i]);
	       name = mthd.getDeclaringType().getFullyQualifiedName() + "." +
		  mthd.getElementName();
	       key = mthd.getKey();
	       sgn = mthd.getSignature();
	       // TODO: might want to add signture here as well
	       break;
	    case IJavaElement.INITIALIZER :
	       IInitializer init = ((IInitializer) elts[i]);
	       name = init.getDeclaringType().getFullyQualifiedName() + ".<clinit>";
	       break;
	    case IJavaElement.PACKAGE_DECLARATION :
	       name = ((IPackageDeclaration) elts[i]).getElementName();
	       break;
	    case IJavaElement.LOCAL_VARIABLE :
	       ILocalVariable lcl = (ILocalVariable) elts[i];
	       name = lcl.getHandleIdentifier();
	       sgn = lcl.getTypeSignature();
	       break;
	  }
	 hdl = elts[i].getHandleIdentifier();
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem getting name",e);
    }

   if (name == null) {
      return;
      // throw new BedrockException("No identifier at location");
    }

   xw.begin("FULLYQUALIFIEDNAME");
   xw.field("NAME",name);
   if (key != null) xw.field("KEY",key);
   if (sgn != null) xw.field("TYPE",sgn);
   if (hdl != null) xw.field("HANDLE",hdl);
   xw.end();
}





/********************************************************************************/
/*										*/
/*	CREATECLASS command							*/
/*										*/
/********************************************************************************/

synchronized void handleNewClass(String proj,String name,boolean frc,
				    String cnts,IvyXmlWriter xw)
	throws BedrockException
{
   if (name == null) return;

   String linesep = System.getProperty("line.separator");
   if (linesep != null && !linesep.equals("\n")) {
      cnts = cnts.replace("\n",linesep);
    }

   String pkg = "";
   String itm = name;
   int idx = name.lastIndexOf(".");
   if (name.endsWith(".java")) idx = name.lastIndexOf(".",idx-1);
   if (idx >= 0) {
      pkg = name.substring(0,idx);
      itm = name.substring(idx+1);
    }

   if (!itm.endsWith(".java")) itm += ".java";

   try {
      IPackageFragment frag = our_plugin.getProjectManager().findPackageFragment(proj,pkg);

      if (frag == null) throw new BedrockException("Package " + pkg + " not found for new class");

      ICompilationUnit icu = frag.createCompilationUnit(itm,cnts,frc,null);
      if (icu == null) throw new BedrockException("Class create failed");
      icu.save(null,true);
      icu.makeConsistent(null);
      BedrockUtil.outputJavaElement(icu,null,false,xw);
    }
   catch (JavaModelException e) {
      System.err.println("Problem with class create: " + e);
      e.printStackTrace();
      throw new BedrockException("Problem with class create",e);
    }
}




/********************************************************************************/
/*										*/
/*	FINDHIERARCHY command							*/
/*										*/
/********************************************************************************/

void handleFindHierarchy(String proj,String pkg,String cls,boolean all,IvyXmlWriter xw)
   throws BedrockException
{
   IJavaProject ijp = getJavaProject(proj);
   IRegion rgn = JavaCore.newRegion();
   IType fortype = null;

   boolean havejp = (ijp != null);

   if (ijp == null && (pkg != null || cls != null)) {
      IJavaElement [] aps = getAllProjects();
      if (aps.length == 0) return;
      if (cls != null) {
	 for (IJavaElement ije : aps) {
	    IJavaProject xjp = ije.getJavaProject();
	    try {
	       if (xjp.findType(cls) != null) {
		  ijp = xjp;
		  break;
		}
	     }
	    catch (JavaModelException e) { }
	  }
       }
      if (ijp == null) ijp = aps[0].getJavaProject();
    }

   int addct = 0;

   if (cls != null && ijp != null) {
      try {
	 IType typ = ijp.findType(cls);
	 fortype = typ;
	 // rgn.add(typ);
	 // ++addct;
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem getting type by name: " + e);
       }
    }

   if (pkg != null && ijp != null) {
      String ppth = "/" + pkg.replace(".","/");
      try {
	 for (IPackageFragmentRoot ipr : ijp.getPackageFragmentRoots()) {
	    IPath rpath = ipr.getPath();
	    Path npath = new Path(rpath.toString() + ppth);
	    IPackageFragment ipf = ijp.findPackageFragment(npath);
	    if (ipf != null) {
	       rgn.add(ipf);
	       ++addct;
	     }
	  }
       }
      catch (Exception e) {
	 BedrockPlugin.logE("Problem getting package fragments for " + ppth + ": " + e);
       }
    }
   else if (havejp && ijp != null) {
      if (all) {
	 rgn.add(ijp);
	 ++addct;
       }
      else {
	 try {
	    for (IPackageFragment ipf : ijp.getPackageFragments()) {
	       for (ICompilationUnit icu : ipf.getCompilationUnits()) {
		  IType ity = ((ITypeRoot) icu).findPrimaryType();
		  if (ity != null) {
		     rgn.add(ity);
		     ++addct;
		   }
		}
	     }
	  }
	 catch (Throwable e) {
	    BedrockPlugin.logE("Problem getting package fragments: " + e);
	  }
       }
    }
   else {
      for (IJavaElement pi : getAllProjects()) {
	 IJavaProject xjp = pi.getJavaProject();
	 if (xjp != null && !rgn.contains(xjp)) {
	    rgn.add(xjp);
	    ++addct;
	  }
	 // String pnm = pi.getJavaProject().getProject().getName();
	 // handleFindHierarchy(pnm,null,null,all,xw);
       }
    }

   if (addct > 0 && ijp != null) {
      try {
	 BedrockPlugin.logD("FIND TYPE HIERARCHY FOR " + fortype + " " + addct + " " + rgn);

	 ITypeHierarchy ith;
	 if (fortype != null) ith = ijp.newTypeHierarchy(fortype,rgn,null);
	 else ith = ijp.newTypeHierarchy(rgn,null);
	 BedrockUtil.outputTypeHierarchy(ith,xw);
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem outputing type hierarchy: " + e);
       }
      catch (NullPointerException e) {
	 // this is a bug in Eclipse that should be fixed
       }
    }
}




/********************************************************************************/
/*										*/
/*	Utility routines							*/
/*										*/
/********************************************************************************/

IJavaProject getJavaProject(String p) throws BedrockException
{
   if (p == null || p.length() == 0) return null;

   IProject ip = our_plugin.getProjectManager().findProject(p);

   return JavaCore.create(ip);
}



static IJavaElement [] getAllProjects()
{
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IProject[] projs = wr.getProjects();

   List<IJavaElement> trslt = new ArrayList<IJavaElement>();

   for (int i = 0; i < projs.length; ++i) {
      if (!BedrockProject.useProject(projs[i].getName())) continue;
      IJavaProject jp = JavaCore.create(projs[i]);
      try {
	 jp.open(null);
       }
      catch (JavaModelException e) {
	 BedrockPlugin.logE("Problem opening java project: " + e);
       }
      trslt.add(jp);
    }

   IJavaElement [] rslt = new IJavaElement[trslt.size()];
   return trslt.toArray(rslt);
}




private ICompilationUnit [] getWorkingElements(IJavaElement [] pelt)
{
   List<ICompilationUnit> active = new ArrayList<ICompilationUnit>();
   for (IJavaElement je : pelt) {
      our_plugin.getWorkingElements(je,active);
    }
   int sz = active.size();
   ICompilationUnit [] rslt = new ICompilationUnit[sz];
   rslt = active.toArray(rslt);

   return rslt;
}



private ICompilationUnit [] getCompilationElements(IJavaElement [] pelt)
{
   List<ICompilationUnit> active = new ArrayList<ICompilationUnit>();
   for (IJavaElement je : pelt) {
      our_plugin.getCompilationElements(je,active);
    }
   int sz = active.size();
   ICompilationUnit [] rslt = new ICompilationUnit[sz];
   rslt = active.toArray(rslt);

   return rslt;
}



private ICompilationUnit getCompilationElement(ICompilationUnit icu)
{
   List<ICompilationUnit> active = new ArrayList<ICompilationUnit>();
   our_plugin.getCompilationElements(icu,active);
   int sz = active.size();
   if (sz == 0) return icu;

   return active.get(0);
}



/********************************************************************************/
/*										*/
/*	Method to get restricted search set					*/
/*										*/
/********************************************************************************/

private IJavaElement [] getSearchElements(IJavaElement [] pelt)
{
   List<IJavaElement> active = new ArrayList<IJavaElement>();
   Set<IJavaProject> done = new HashSet<IJavaProject>();

   for (IJavaElement je : pelt) {
      if (je.getElementType() == IJavaElement.JAVA_PROJECT) {
	 IJavaProject jp = (IJavaProject) je;
	 addProjectElements(jp,done,active);
       }
      else active.add(je);
    }

   int sz = active.size();
   pelt = new IJavaElement[sz];
   pelt = active.toArray(pelt);

   return pelt;
}



private void addProjectElements(IJavaProject jp,Set<IJavaProject> done,List<IJavaElement> rslt)
{
   if (done.contains(jp)) return;
   done.add(jp);

   try {
      for (IPackageFragmentRoot pfr : jp.getPackageFragmentRoots()) {
	 if (!pfr.isArchive() && !pfr.isExternal() &&
		pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
	    rslt.add(pfr);
	  }
       }
      for (String pn : jp.getRequiredProjectNames()) {
	 try {
	    IJavaProject rjp = getJavaProject(pn);
	    if (rjp != null) addProjectElements(rjp,done,rslt);
	  }
	 catch (BedrockException e) { }
       }
    }
   catch (JavaModelException e) { }
}





}	// end of class BedrockJava





/* end of BedrockJava.java */
