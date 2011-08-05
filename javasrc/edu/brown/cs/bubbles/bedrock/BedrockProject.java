/********************************************************************************/
/*										*/
/*		BedrockProject.java						*/
/*										*/
/*	Project manager for Bubbles - Eclipse interface 			*/
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
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.actions.NewProjectAction;

import org.w3c.dom.*;

import java.io.*;
import java.util.*;




class BedrockProject implements BedrockConstants, IResourceChangeListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;
private boolean projects_inited;
private boolean projects_registered;
private Set<IProject> open_projects;

private static boolean		initial_build = false;
private static boolean		initial_refresh = false;

private static Set<String> ignore_projects;

static {
   ignore_projects = new HashSet<String>();
   ignore_projects.add("RemoteSystemsTempFiles");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockProject(BedrockPlugin bp)
{
   our_plugin = bp;
   projects_inited = false;
   projects_registered = false;
   open_projects = new HashSet<IProject>();
}



/********************************************************************************/
/*										*/
/*	Starutp methods 							*/
/*										*/
/********************************************************************************/

void initialize()
{
   if (projects_inited) return;

   new IvyXmlWriter();			// force loading

   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();

   BedrockPlugin.logI("WORKSPACE = " + wr.getName() + " " + wr.getFullPath());

   IProject[] projs = wr.getProjects();
   for (int i = 0; i < projs.length; ++i) {
      if (ignore_projects.contains(projs[i].getName())) continue;
      BedrockPlugin.logI("    PROJECT = " + projs[i].getName());
      String desc = "Project Setup for " + projs[i].getName();
      if (projs[i].isOpen()) {
	 BedrockPlugin.logI("PROJECT OPEN");
	 if (initial_build) {
	    try {
	       BedrockPlugin.logD("BUILD");
	       projs[i].build(IncrementalProjectBuilder.INCREMENTAL_BUILD,
				 new BedrockProgressMonitor(our_plugin,desc));
	     }
	    catch (CoreException e) {
	       BedrockPlugin.logE("Problem doing initial build: " + e);
	     }
	  }
	 attachProject(projs[i]);
       }
      else if (!PlatformUI.isWorkbenchRunning()) {
	 try {
	    if (initial_refresh) {
	       BedrockPlugin.logI("REFRESH");
	       projs[i].refreshLocal(IResource.DEPTH_INFINITE,null);
	     }
	    if (initial_build) {
	       BedrockPlugin.logI("BUILD");
	       projs[i].build(IncrementalProjectBuilder.INCREMENTAL_BUILD,
				 new BedrockProgressMonitor(our_plugin,desc));
	     }
	  }
	 catch (CoreException e) {
	    BedrockPlugin.logE("Problem doing initial build: " + e);
	  }
       }
    }

   projects_inited = true;
}



void register()
{
   if (projects_registered) return;
   projects_registered = true;

   JavaCore.addPreProcessingResourceChangedListener(this,
				     IResourceChangeEvent.POST_CHANGE|IResourceChangeEvent.POST_BUILD);
}


void terminate()
{
   for (IProject p : new ArrayList<IProject>(open_projects)) {
      detachProject(p);
    }
}



/********************************************************************************/
/*										*/
/*	Command processing							*/
/*										*/
/********************************************************************************/

void listProjects(IvyXmlWriter xw)
{
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IProject[] projs = wr.getProjects();

   for (int i = 0; i < projs.length; ++i) {
      if (ignore_projects.contains(projs[i].getName())) continue;
      xw.begin("PROJECT");
      xw.field("NAME",projs[i].getName());
      xw.field("OPEN",projs[i].isOpen());
      xw.field("WORKSPACE",wr.getLocation().toOSString());
      boolean isjava = false;
      try {
	 isjava = projs[i].hasNature(JavaCore.NATURE_ID);
       }
      catch (CoreException e) { }
      xw.field("ISJAVA",isjava);
      try {
	 xw.textElement("DESCRIPTION",projs[i].getDescription().getComment());
       }
      catch (CoreException e) { }
      xw.textElement("PATH",projs[i].getFullPath().toOSString());
      try {
	 IProject[] rp = projs[i].getReferencedProjects();
	 for (int j = 0; j < rp.length; ++j) {
	    xw.textElement("REFERENCES",rp[j].getName());
	  }
       }
      catch (Exception e) { }
      IProject[] up = projs[i].getReferencingProjects();
      for (int j = 0; j < up.length; ++j) {
	 xw.textElement("USEDBY",up[j].getName());
       }
      xw.end("PROJECT");
    }
}



void openProject(String name,boolean fil,boolean pat,boolean cls,boolean opt,IvyXmlWriter xw)
	throws BedrockException
{
   IProject p = findProject(name);

   attachProject(p);

   if (xw != null) outputProject(p,fil,pat,cls,opt,xw);
}



void closeProject(String name,IvyXmlWriter _xw) throws BedrockException
{
   detachProject(findProject(name));
}



void listSourceFiles(String name,IvyXmlWriter xw) throws BedrockException
{
   IProject ip = findProject(name);

   addSourceFiles(ip,xw,new JavaSourceFilter());
}



void buildProject(String proj,boolean clean,boolean full,boolean refresh,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = findProject(proj);

   handleBuild(ip,clean,full,refresh);

   IMarker [] mrks;
   try {
      mrks = ip.findMarkers(null,true,IResource.DEPTH_INFINITE);
      BedrockUtil.outputMarkers(ip,mrks,xw);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem finding errors",e);
    }
}



/********************************************************************************/
/*										*/
/*	Project property editing						*/
/*										*/
/********************************************************************************/

void editProject(String proj)
{
   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IProject ip = wr.getProject(proj);
   IWorkbenchWindow ww = null;

   try {
      IWorkbench wb = PlatformUI.getWorkbench();
      ww = wb.getActiveWorkbenchWindow();
      if (ww == null) {
	 IWorkbenchWindow [] wins = wb.getWorkbenchWindows();
	 if (wins != null && wins.length > 0) ww = wins[0];
       }
      if (ww == null) ww = wb.openWorkbenchWindow(ip);
    }
   catch (Throwable t) {
      BedrockPlugin.logE("BEDROCK: problem finding window: " + t,t);
    }

   SelProvider sp = new SelProvider(ip,ww.getShell());

   Display.getDefault().asyncExec(new RunPropDialog(sp));
}


private static final class SelProvider implements ISelectionProvider, IShellProvider {

   private IStructuredSelection project_selection;
   private Shell use_shell;

   SelProvider(IProject ip,Shell sh) {
      project_selection = new StructuredSelection(ip);
      use_shell = sh;
    }

   public void addSelectionChangedListener(ISelectionChangedListener listener) { }
   public void removeSelectionChangedListener(ISelectionChangedListener listener) { }
   public void setSelection(ISelection selection) { }

   public ISelection getSelection()		{ return project_selection; }
   public Shell getShell()			{ return use_shell; }


}	// end of inner class SelProvider



private static final class RunPropDialog implements Runnable {

   private SelProvider use_provider;

   RunPropDialog(SelProvider sp) {
      use_provider = sp;
    }

   @Override public void run() {
      PropertyDialogAction act = new PropertyDialogAction(use_provider,use_provider);
      try {
	 act.run();
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("BEDROCK: Problem with project property: " + t,t);
       }
    }

}	// end of inner class RunPropDialog




/********************************************************************************/
/*										*/
/*	New project setup							*/
/*										*/
/********************************************************************************/

void createProject()
{
   IWorkbenchWindow ww = null;

   try {
      IWorkbench wb = PlatformUI.getWorkbench();
      ww = wb.getActiveWorkbenchWindow();
      if (ww == null) {
	 IWorkbenchWindow [] wins = wb.getWorkbenchWindows();
	 if (wins != null && wins.length > 0) ww = wins[0];
       }
      if (ww == null) ww = wb.openWorkbenchWindow(wb);
    }
   catch (Throwable t) {
      BedrockPlugin.logE("BEDROCK: problem finding window: " + t,t);
    }

   Display.getDefault().asyncExec(new RunNewDialog(ww));
}

/**
 * Adds a project that is already in the workspace folder but is not 
 * a member of the workspace yet 
 */
void importExistingProject(String name) throws Exception
{
   IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
   
   //if it already exists in the workspace don't do anything
   if(p.exists())
   {
      System.out.println("Project already exists");
      return;
   }
   p.create(null);
   p.open(null);
   JavaCore.create(p);
   p.refreshLocal(IResource.DEPTH_INFINITE, null);
}


private static final class RunNewDialog implements Runnable {

   private IWorkbenchWindow work_window;

   RunNewDialog(IWorkbenchWindow ww) {
      work_window = ww;
    }

   @Override public void run() {
      NewProjectAction act = new NewProjectAction(work_window);
      try {
	 act.run();
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("BEDROCK: Problem with new project: " + t,t);
       }
    }

}	// end of inner class RunNewDialog




/********************************************************************************/
/*										*/
/*	Command utility routines						*/
/*										*/
/********************************************************************************/

IProject findProject(String name) throws BedrockException
{
   if (name == null) return null;
   if (ignore_projects.contains(name)) return null;

   IWorkspace ws = ResourcesPlugin.getWorkspace();
   IWorkspaceRoot wr = ws.getRoot();
   IProject ip = wr.getProject(name);

   if (ip == null) throw new BedrockException("Project " + name + " not in workspace");

   return ip;
}



private static class JavaSourceFilter implements FileFilter {

   public boolean accept(File f) {
      return f.getPath().endsWith(".java");
    }

}	// end of subclass JavaSourceFilter




static boolean useProject(String name)
{
   if (ignore_projects.contains(name)) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	Attach and detach methods						*/
/*										*/
/********************************************************************************/

private void attachProject(IProject p)
{
   if (!open_projects.contains(p)) {
      open_projects.add(p);
    }

   try {
      p.open(null);
      IJavaProject ijp = JavaCore.create(p);
      ijp.open(null);
      // resolveAll(ijp);
    }
   catch (JavaModelException e) {
      BedrockPlugin.logE("Error resolving project: " + e);
      return;
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Error opening project: " + e);
      return;
    }
   catch (Throwable e) {
      BedrockPlugin.logE("Error with project attach: " + e);
      return;
    }
}




private void detachProject(IProject p)
{
   if (!open_projects.contains(p)) return;

   open_projects.remove(p);
}



/********************************************************************************/
/*										*/
/*	Project Access methods							*/
/*										*/
/********************************************************************************/

Collection<IFile> getAllSourceFiles(String p) throws BedrockException
{
   Collection<IFile> rslt = findSourceFiles(findProject(p),null);

   return rslt;
}



Collection<IProject> getOpenProjects()
{
   Collection<IProject> rslt = new ArrayList<IProject>(open_projects);

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Package management methods						*/
/*										*/
/********************************************************************************/

void createPackage(String proj,String pkg,boolean force,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = findProject(proj);
   IJavaProject ijp = JavaCore.create(ip);

   IPackageFragmentRoot ipfr = null;
   try {
      for (IPackageFragmentRoot pfr : ijp.getAllPackageFragmentRoots()) {
	 try {
	    if (!pfr.isExternal() && !pfr.isArchive() &&
		   pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
	       ipfr = pfr;
	       break;
	     }
	  }
	 catch (JavaModelException e) { }
       }
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem finding package roots: " + e,e);
    }

   if (ipfr == null) throw new BedrockException("Can't find source fragment root");

   IPackageFragment ifr = null;
   try {
      ifr = ipfr.createPackageFragment(pkg,force,null);
      ifr.save(null,force);
      ifr.open(null);
    }
   catch (JavaModelException e) {
      throw new BedrockException("Problem creating package: " + e,e);
    }

   xw.begin("PACKAGE");
   xw.field("NAME",ifr.getElementName());
   File f = BedrockUtil.getFileForPath(ifr.getPath(),ip);
   xw.field("PATH",f.getAbsolutePath());
   xw.end("PACKAGE");
}




void findPackage(String proj,String pkg,IvyXmlWriter xw)
	throws BedrockException
{
   IProject ip = findProject(proj);
   IPackageFragment ipf = findPackageFragment(proj,pkg);

   if (ipf == null) return;

   File f = BedrockUtil.getFileForPath(ipf.getPath(),ip);

   xw.begin("PACKAGE");
   xw.field("NAME",ipf.getElementName());
   xw.field("PATH",f.getAbsolutePath());
   BedrockUtil.outputJavaElement(ipf,xw);
   xw.end("PACKAGE");
}




IPackageFragment findPackageFragment(String proj,String pkg)
    throws BedrockException
{
   IProject ip = findProject(proj);
   IJavaProject ijp = JavaCore.create(ip);
   if (ijp == null) return null;

   try {
      for (IPackageFragmentRoot pfr : ijp.getAllPackageFragmentRoots()) {
	 try {
	    if (!pfr.isExternal() && !pfr.isArchive() &&
		   pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
	       IPackageFragment ipf = pfr.getPackageFragment(pkg);
	       if (ipf != null && ipf.isOpen()) {
		  File f = BedrockUtil.getFileForPath(ipf.getPath(),ip);
		  if (f.exists()) return ipf;
		  BedrockPlugin.logE("Fragment path doesn't exist: " + f);
		}
	     }
	  }
	 catch (JavaModelException e) { }
       }
    }
   catch (JavaModelException e) {
      e.printStackTrace();
      throw new BedrockException("Problem finding package roots: " + e,e);
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Preference methods							*/
/*										*/
/********************************************************************************/

private static final Map<String,String> pref_types;

static {
   pref_types = new HashMap<String,String>();
   pref_types.put("currentLine","B");
   pref_types.put("currentLineColor","S");
   pref_types.put("disable_overwrite_mode","B");
   pref_types.put("spacesForTabs","B");
   pref_types.put("tabWidth","I");
   pref_types.put("textDragAndDropEnabled","B");
   pref_types.put("org.eclipse.debug.ui.build_before_launch","B");
   pref_types.put("line.separator","S");
   pref_types.put("org.eclipse.jface.textfont","S");
}



void handlePreferences(String proj,IvyXmlWriter xw)
{
   xw.begin("PREFERENCES");

   Map<?,?> opts;
   if (proj == null) opts = JavaCore.getOptions();
   else {
      try {
	 IProject ip = findProject(proj);
	 IJavaProject ijp = JavaCore.create(ip);
	 opts = ijp.getOptions(true);
       }
      catch (BedrockException e) {
	 opts = JavaCore.getOptions();
       }
    }

   for (Map.Entry<?,?> ent : opts.entrySet()) {
      String key = (String) ent.getKey();
      String val = (String) ent.getValue();
      xw.begin("PREF");
      xw.field("NAME",key);
      xw.field("VALUE",val);
      xw.field("OPTS",true);
      xw.end("PREF");
    }

   xw.end("PREFERENCES");
}




/********************************************************************************/
/*										*/
/*	Project search methods							*/
/*										*/
/********************************************************************************/

private Collection<IFile> findSourceFiles(IResource ir,Collection<IFile> rslt)
{
   if (rslt == null) rslt = new ArrayList<IFile>();

   try {
      if (ir instanceof IFile) {
	 IFile ifl = (IFile) ir;
	 rslt.add(ifl);
       }
      else if (ir instanceof IContainer) {
	 IContainer ic = (IContainer) ir;
	 IResource[] mems = ic.members();
	 for (int i = 0; i < mems.length; ++i) {
	    findSourceFiles(mems[i],rslt);
	  }
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem getting source files: " + e);
    }

   return rslt;
}



ICompilationUnit getCompilationUnit(String proj,String file) throws BedrockException
{
   //TODO: This should find the current working copy, not the underlying unit

   IProject ip = findProjectForFile(proj,file);
   if (ip == null) return null;

   IJavaProject ijp = JavaCore.create(ip);
   ICompilationUnit icu = checkFilePrefix(ijp,null,file);
   if (icu != null) return icu;
   if (ijp == null) return null;

   try {
      IClasspathEntry[] ents = ijp.getResolvedClasspath(true);
      for (int i = 0; i < ents.length; ++i) {
	 if (ents[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
	    IPath p = ents[i].getPath();
	    File f = BedrockUtil.getFileForPath(p,ip);
	    if (!f.exists()) continue;
	    icu = checkFilePrefix(ijp,f.getAbsolutePath(),file);
	    if (icu != null) return icu;
	  }
       }
    }
   catch (JavaModelException e) {
      BedrockPlugin.logE("Problem getting compilation unit: " + e);
    }

   return null;
}



private ICompilationUnit checkFilePrefix(IJavaProject ijp,String pfx,String file)
{
   if (ijp == null) return null;

   if (pfx != null) {
      if (!file.startsWith(pfx)) return null;
      int ln = pfx.length();
      if (file.charAt(ln) == File.separatorChar || file.charAt(ln) == '/') ++ln;
      file = file.substring(ln);
    }

   try {
      IPath fp = new Path(file);
      IJavaElement je = ijp.findElement(fp);
      if (je != null && je instanceof ICompilationUnit) {
	 return (ICompilationUnit) je;
       }
    }
   catch (JavaModelException e) { }

   file = file.replace('\\','/');

   try {
      IPath fp = new Path(file);
      IJavaElement je = ijp.findElement(fp);
      if (je != null && je instanceof ICompilationUnit) {
	 return (ICompilationUnit) je;
       }
    }
   catch (JavaModelException e) { }

   return null;
}




IFile getProjectFile(String proj,String file) throws BedrockException
{
   if (proj == null) {
      for (IProject ip : open_projects) {
	 IFile ifl = findProjectFile(ip,file);
	 if (ifl != null) return ifl;
       }
      return null;
    }

   IProject ip = findProject(proj);

   return findProjectFile(ip,file);
}



IProject findProjectForFile(String proj,String file) throws BedrockException
{
   if (proj == null && file != null) {
      for (IProject ip : open_projects) {
	 IFile ifl = findProjectFile(ip,file);
	 if (ifl != null) return ip;
       }
      BedrockPlugin.logE("No project found for file " + file);
      return null;
    }

   return findProject(proj);
}



private IFile findProjectFile(IResource ir,String name)
{
   try {
      if (ir instanceof IFile) {
	 IFile ifl = (IFile) ir;
	 File f = ifl.getLocation().toFile();
	 if (f.getAbsolutePath().equals(name) || f.getPath().equals(name)) return ifl;
       }
      else if (ir instanceof IContainer) {
	 IContainer ic = (IContainer) ir;
	 IResource[] mems = ic.members();
	 for (int i = 0; i < mems.length; ++i) {
	    IFile ifl = findProjectFile(mems[i],name);
	    if (ifl != null) return ifl;
	  }
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem getting source files: " + e);
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Project dumping methods 						*/
/*										*/
/********************************************************************************/

private void outputProject(IProject p,boolean fil,boolean pat,boolean cls,boolean opt,
			      IvyXmlWriter xw)
{
   xw.begin("PROJECT");
   xw.field("NAME",p.getName());
   xw.field("PATH",p.getLocation().toOSString());
   xw.field("WORKSPACE",p.getWorkspace().getRoot().getLocation().toOSString());
   xw.field("BEDROCKDIR",p.getWorkingLocation(BEDROCK_PLUGIN).toOSString());

   IJavaProject jp = JavaCore.create(p);
   if (jp != null && pat) {
      xw.begin("CLASSPATH");
      addClassPaths(jp,xw,null,false);
      xw.end("CLASSPATH");
    }

   if (fil) {
      xw.begin("FILES");
      addSourceFiles(p,xw,null);
      xw.end("FILES");
    }

   if (jp != null && cls) {
      xw.begin("CLASSES");
      addClasses(jp,xw);
      xw.end("CLASSES");
    }

   try {
      IProject[] rp = p.getReferencedProjects();
      IProject[] up = p.getReferencingProjects();
      for (int j = 0; j < rp.length; ++j) {
	 xw.textElement("REFERENCES",rp[j].getName());
       }
      for (int j = 0; j < up.length; ++j) {
	 xw.textElement("USEDBY",up[j].getName());
       }
    }
   catch (Exception e) { }

   if (opt) {
      Map<?,?> opts = jp.getOptions(false);
      for (Map.Entry<?,?> ent : opts.entrySet()) {
	 xw.begin("OPTION");
	 xw.field("NAME",ent.getKey().toString());
	 xw.field("VALUE",ent.getValue().toString());
	 xw.end("OPTION");
       }
    }

   xw.end("PROJECT");
}





private void addClassPaths(IJavaProject jp,IvyXmlWriter xw,Set<IProject> done,boolean nest)
{
   if (done == null) done = new HashSet<IProject>();
   done.add(jp.getProject());
   BedrockPlugin.logD("Getting class path for " + jp.getProject().getName());

   try {
      IClasspathEntry[] ents = jp.getResolvedClasspath(true);
      for (IClasspathEntry ent : ents) {
	 addPath(xw,jp,ent,nest);
       }
      IPath op = jp.getOutputLocation();
      if (op != null) {
	 xw.begin("PATH");
	 xw.field("TYPE","BINARY");
	 File f = BedrockUtil.getFileForPath(op,jp.getProject());
	 if (f.exists()) xw.textElement("BINARY",f.getAbsolutePath());
	 xw.end("PATH");
       }
      for (IProject rp : jp.getProject().getReferencedProjects()) {
	 if (done.contains(rp)) continue;
	 IJavaProject jrp = JavaCore.create(rp);
	 addClassPaths(jrp,xw,done,true);
       }
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem resolving classpath: " + e);
    }
}




private void addPath(IvyXmlWriter xw,IJavaProject jp,IClasspathEntry ent,boolean nest)
{
   IPath p = ent.getPath();
   IPath op = ent.getOutputLocation();
   IPath sp = ent.getSourceAttachmentPath();
   IProject ip = jp.getProject();

   if (p == null && op == null) return;
   File f1 = null;
   if (p != null) {
      f1 = BedrockUtil.getFileForPath(p,ip);
      if (!f1.exists()) {
	 BedrockPlugin.logD("Path file " + p + " not found as " + f1);
	 f1 = null;
       }
    }
   File f2 = null;
   if (op != null) {
      f2 = BedrockUtil.getFileForPath(op,ip);
      if (!f2.exists()) {
	 BedrockPlugin.logD("Path file " + op + " not found");
	 f2 = null;
       }
    }
   File f3 = null;
   if (sp != null) {
      f3 = BedrockUtil.getFileForPath(sp,ip);
      if (!f3.exists()) {
	 BedrockPlugin.logD("Path file " + sp + " not found");
	 f3 = null;
       }
    }
   if (f1 == null && f2 == null) return;

   // references to nested projects are handled in addClassPaths
   if (ent.getEntryKind() == IClasspathEntry.CPE_PROJECT) return;

   xw.begin("PATH");
   if (nest) xw.field("NESTED","TRUE");

   switch (ent.getEntryKind()) {
      case IClasspathEntry.CPE_SOURCE :
	 xw.field("TYPE","SOURCE");
	 f3 = f1;
	 f1 = null;
	 break;
      case IClasspathEntry.CPE_PROJECT :
	 xw.field("TYPE","BINARY");
	 break;
      case IClasspathEntry.CPE_LIBRARY :
	 xw.field("TYPE","LIBRARY");
	 break;
    }
   if (ent.isExported()) xw.field("EXPORTED",true);

   if (f1 != null) xw.textElement("BINARY",f1.getAbsolutePath());
   if (f2 != null) xw.textElement("OUTPUT",f2.getAbsolutePath());
   if (f3 != null) xw.textElement("SOURCE",f3.getAbsolutePath());

   xw.end("PATH");
}



private void addSourceFiles(IResource ir,IvyXmlWriter xw,FileFilter ff)
{
   Collection<IFile> fls = findSourceFiles(ir,null);

   for (IFile ifl : fls) {
      IContentDescription cd = null;
      try {
	 cd = ifl.getContentDescription();
       }
      catch (CoreException e) { }
      if (cd == null) continue;

      IContentType ct = cd.getContentType();
      IPath ip = ifl.getLocation();
      IPath ipr = ifl.getProjectRelativePath();
      File f = ip.toFile();
      if (ff != null && !ff.accept(f)) continue;
      if (f.exists()) {
	 try {
	    f = f.getCanonicalFile();
	  }
	 catch (IOException _e) { }
	 xw.begin("FILE");
	 if (ct.getId().endsWith("javaSource")) xw.field("SOURCE",true);
	 else if (ct.getId().endsWith("javaClass")) xw.field("BINARY",true);
	 else xw.field("TYPENAME",ct.getId());
	 xw.field("NAME",ifl.getName());
	 if (ifl.isReadOnly()) xw.field("READONLY",true);
	 if (ifl.isLinked()) xw.field("LINKED",true);
	 if (!ifl.isSynchronized(IResource.DEPTH_ONE)) xw.field("SYNC",false);
	 xw.field("PROJPATH",ipr.toOSString());
	 xw.field("PATH",ip.toOSString());
	 xw.text(f.getPath());
	 xw.end("FILE");
       }
    }
}




private void addClasses(IJavaProject jp,IvyXmlWriter xw)
{
   try {
      IClasspathEntry[] ents = jp.getResolvedClasspath(true);
      for (int k = 0; k < ents.length; ++k) {
	 if (ents[k].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
	    IPackageFragmentRoot[] rts = jp.findPackageFragmentRoots(ents[k]);
	    for (int l = 0; l < rts.length; ++l) {
	       IJavaElement[] elts = rts[l].getChildren();
	       for (int m = 0; m < elts.length; ++m) {
		  if (elts[m] instanceof IPackageFragment) {
		     IPackageFragment frag = (IPackageFragment) elts[m];
		     xw.textElement("PACKAGE",frag.getElementName());
		     for (ICompilationUnit icu : frag.getCompilationUnits()) {
			for (IType typ : icu.getTypes()) {
			   outputType(typ,jp,xw);
			 }
		      }
		   }
		}
	     }
	  }
       }
    }
   catch (JavaModelException e) {
      BedrockPlugin.logE("Problem getting class list: " + e);
    }
}



private void outputType(IType typ,IJavaProject jp,IvyXmlWriter xw) throws JavaModelException
{
   xw.begin("TYPE");
   xw.field("NAME",typ.getFullyQualifiedName());
   IPath ip = typ.getPath();
   File f = BedrockUtil.getFileForPath(ip,jp.getProject());
   if (f.exists()) xw.field("SOURCE",f.getAbsolutePath());
   File bf = findBinaryFor(jp,typ);
   if (bf != null) xw.field("BINARY",bf.getAbsolutePath());
   xw.end("TYPE");

   for (IType ntyp : typ.getTypes()) {
      outputType(ntyp,jp,xw);
    }
}




private File findBinaryFor(IJavaProject jp,IType typ) throws JavaModelException
{
   IPath op = jp.readOutputLocation();
   // op might not be correct if there is an output directory associated with
   // the source directory

   String tnm = typ.getFullyQualifiedName();
   tnm = tnm.replace('.',File.separatorChar);
   IPath cp = op.append(tnm + ".class");
   File f= BedrockUtil.getFileForPath(cp,jp.getProject());
   return f;
}



/********************************************************************************/
/*										*/
/*	Build methods								*/
/*										*/
/********************************************************************************/

private void handleBuild(IProject p,boolean clean,boolean full,boolean refresh) throws BedrockException
{
   try {
      if (refresh) {
	 String desc = "Refreshing project " + p.getName();
	 BedrockProgressMonitor wait = new BedrockProgressMonitor(our_plugin,desc);
	 p.refreshLocal(IResource.DEPTH_INFINITE,wait);
	 wait.waitFor();
       }
      int kind = IncrementalProjectBuilder.INCREMENTAL_BUILD;
      String knm = "";
      if (clean) {
	 kind = IncrementalProjectBuilder.CLEAN_BUILD;
	 knm = " (clean)";
       }
      else if (full) {
	 kind = IncrementalProjectBuilder.FULL_BUILD;
	 knm = " (full)";
       }
      String desc = "Building " + knm + " project " + p.getName();
      p.build(kind,new BedrockProgressMonitor(our_plugin,desc));
    }
   catch (Throwable t) {
      throw new BedrockException("Build error: " + t);
    }
}




/********************************************************************************/
/*										*/
/*	Event handler for workspace resources					*/
/*										*/
/********************************************************************************/

@Override public void resourceChanged(IResourceChangeEvent evt)
{
   /*********************
   BedrockPlugin.logD("Resource Change: " + evt.getBuildKind() + " " + evt.getType());
   IvyXmlWriter dxw = new IvyXmlWriter();
   dxw.begin("RESOURCECHANGE");
   IResourceDelta drd = evt.getDelta();
   dumpDelta(0,drd);
   BedrockUtil.outputResource(drd,dxw);
   dxw.end();
   BedrockPlugin.logD("Resource: " + dxw.toString());
   *************************/

   if (evt.getType() == IResourceChangeEvent.POST_CHANGE) {
      try {
	 IvyXmlWriter xw = our_plugin.beginMessage("RESOURCE");
	 IResourceDelta rd = evt.getDelta();
	 int ctr = BedrockUtil.outputResource(rd,xw);
	 if (ctr == 0) return;				   // nothing output
	 our_plugin.finishMessage(xw);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem with resource: " + t);
	 t.printStackTrace();
       }
    }
   else if (evt.getType() == IResourceChangeEvent.POST_BUILD) {
      try {
	 IvyXmlWriter xw = our_plugin.beginMessage("BUILDDONE");
	 IResourceDelta rd = evt.getDelta();
	 int ctr = BedrockUtil.outputResource(rd,xw);
	 if (ctr == 0) return;				   // nothing output
	 our_plugin.finishMessage(xw);
       }
      catch (Throwable t) {
	 BedrockPlugin.logE("Problem with resource: " + t);
	 t.printStackTrace();
       }
    }
}


/***********************
private void dumpDelta(int lvl,IResourceDelta drd)
{
   if (drd == null) return;
   BedrockPlugin.logD("Resource " + lvl + " Delta: " + drd + " " + drd.getFullPath() + " " + drd.getFlags());
   for (IResourceDelta xrd : drd.getAffectedChildren()) {
      dumpDelta(lvl+1,xrd);
    }
}
********************/



/********************************************************************************/
/*										*/
/*	Project editing methods 						*/
/*										*/
/********************************************************************************/

void setProjectClassPath(String proj,Element desc)
{
   // List<IClasspathEntry> class_paths = new ArrayList<IClasspathEntry>();
}


void setProjectOutputPath(String proj,String path)
{
}



void setProjectOption(String proj,String name,String value)
{
}


void setProjectDescription(String proj,Element desc)
{
}


void createProject(String proj,Element desc)
{
}



}	// end of class BedrockProject




/* end of BedrockProject.java */
