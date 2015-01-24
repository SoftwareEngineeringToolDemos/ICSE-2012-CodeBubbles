/********************************************************************************/
/*										*/
/*		NobaseProject.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.ivy.exec.*;
import edu.brown.cs.ivy.file.*;

import org.w3c.dom.Element;

import java.io.*;
import java.util.*;

class NobaseProject implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseMain	nobase_main;
private NobaseProjectManager project_manager;
private String project_name;
private File base_directory;
private List<NobasePathSpec> project_paths;
private List<File> project_files;
private boolean is_open;
private NobasePreferences nobase_prefs;
private File		js_runner;
private NobaseScope	global_scope;
private NobaseResolver	project_resolver;

private Map<NobaseFile,ISemanticData> parse_data;
private Set<NobaseFile> all_files;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseProject(NobaseMain pm,String name,File base)
{
   nobase_main = pm;
   project_manager = pm.getProjectManager();
   base_directory = base.getAbsoluteFile();
   try {
      base_directory = base_directory.getCanonicalFile();
    }
   catch (IOException e) { }
   if (name == null) name = base.getName();
   project_name = name;
   project_paths = new ArrayList<NobasePathSpec>();
   project_files = new ArrayList<File>();
   nobase_prefs = new NobasePreferences(pm.getSystemPreferences());
   all_files = new HashSet<NobaseFile>();
   parse_data = new HashMap<NobaseFile,ISemanticData>();
   global_scope = new NobaseScope(ScopeType.GLOBAL,null);

   File f = new File(base_directory,".nobase");
   if (!f.exists()) f.mkdir();

   File f1 = new File(base_directory,".jsproject");
   Element xml = IvyXml.loadXmlFromFile(f1);
   if (xml == null) {
      setupDefaults();
    }
   else {
      String bfile = IvyXml.getTextElement(xml,"EXE");
      js_runner = findInterpreter(bfile);
      if (js_runner == null) js_runner = findInterpreter(null);
      for (Element pe : IvyXml.children(xml,"PATH")) {
	 NobasePathSpec ps = project_manager.createPathSpec(pe);
	 project_paths.add(ps);
       }
      for (Element fe : IvyXml.children(xml,"FILE")) {
         String nm = IvyXml.getTextElement(fe,"NAME");
         File fs = new File(nm);
	 project_files.add(fs);
       }
      nobase_prefs.loadXml(xml);
    }
   is_open = false;
   saveProject();

   project_resolver = new NobaseResolver(this,global_scope);
}




void setupDefaults()
{
   File src = new File(base_directory,"src");
   if (src.isDirectory() || src.mkdir()) {
      File rsrc = new File("/src");
      NobasePathSpec ps = project_manager.createPathSpec(rsrc,true,false);
      project_paths.add(ps);
      File f1 = new File("*/node_modules");
      NobasePathSpec ps1 = project_manager.createPathSpec(f1,false,false);
      project_paths.add(ps1);
      File f2 = new File("*/web");
      NobasePathSpec ps2 = project_manager.createPathSpec(f2,false,true);
      project_paths.add(ps2);
      File f3 = new File("*/html");
      NobasePathSpec ps3 = project_manager.createPathSpec(f3,false,true);
      project_paths.add(ps3);            
    }
   js_runner = findInterpreter(null);
}



private File findInterpreter(String name)
{
   if (name == null) {
      File f = findInterpreter("node");
      if (f != null) return f;
      f = findInterpreter("js");
      if (f != null) return f;
      f = findInterpreter("rhino");
      if (f != null) return f;
      return null;
    }

   File f1 = nobase_main.getRootDirectory();
   File f1a = new File(f1,"lib");
   File f2 = new File(f1a,"JSFiles");
   File f3 = new File(f2,"checkinterpreter.js");

   List<String> args = new ArrayList<String>();
   args.add(name);
   // args.add(f3.getPath());
   try {
      String checkfile = IvyFile.loadFile(f3);
      IvyExec ex = new IvyExec(args,null,null,IvyExec.READ_OUTPUT|IvyExec.PROVIDE_INPUT);
      OutputStream ost = ex.getOutputStream();
      ost.write(checkfile.getBytes());
      ost.close();
      InputStream ins = ex.getInputStream();
      InputStreamReader isr = new InputStreamReader(ins);
      BufferedReader br = new BufferedReader(isr);
      List<String> lines = new ArrayList<String>();
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 lines.add(ln);
       }
      for (String ln : lines) {
	 StringTokenizer tok = new StringTokenizer(ln);
	 String nam = tok.nextToken();
	 String typ = tok.nextToken();
	 if (nam.contains(".")) continue;
	 if (typ.equalsIgnoreCase("function")) {
	    NobaseSymbol sym = new NobaseSymbol(null,null,null,nam,true);
	    sym.setValue(NobaseValue.createFunction());
	    global_scope.define(sym);
	  }
	 else if (typ.equalsIgnoreCase("object")) {
	    NobaseSymbol sym = new NobaseSymbol(null,null,null,nam,true);
	    sym.setValue(NobaseValue.createObject());
	    global_scope.define(sym);
	  }
	 else if (typ.equalsIgnoreCase("string")) {
	    NobaseSymbol sym = new NobaseSymbol(null,null,null,nam,true);
	    sym.setValue(NobaseValue.createString());
	    global_scope.define(sym);
	  }
       }
      // check versions, etc. here
      ex.waitFor();
    }
   catch (IOException e) {
      return null;
    }

   // might want to get full executable name here
   return new File(name);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getName() 			{ return project_name; }
File getBasePath()				{ return base_directory; }
NobasePreferences getPreferences()		{ return nobase_prefs; }
public boolean isOpen() 			{ return is_open; }
public boolean exists() 			{ return base_directory.exists(); }
public NobaseProject [] getReferencedProjects() { return new NobaseProject[0]; }
public NobaseProject [] getReferencingProjects() { return new NobaseProject[0]; }

Collection<NobaseFile> getAllFiles()
{
   return new ArrayList<NobaseFile>(all_files);
}

NobaseFile findFile(String path)
{
   if (path == null) return null;
   if (File.separatorChar != '/') path = path.replace(File.separatorChar,'/');

   for (NobaseFile ifd : all_files) {
      if (ifd.getFile().getPath().equals(path)) return ifd;
    }

   return null;
}



ISemanticData getSemanticData(String file)
{
   for (NobaseFile ifd : all_files) {
      if (ifd.getFile().getPath().equals(file)) return getParseData(ifd);
    }

   return null;
}


String getProjectSourcePath()
{
   String rslt = "";
   for (NobasePathSpec ps : project_paths) {
      if (rslt.length() > 0) rslt += "|";
      rslt += ps.getFile().getPath();
    }
   return rslt;
}


ISemanticData reparseFile(NobaseFile fd)
{
   if (fd == null) return null;
   parseFile(fd);
   return parse_data.get(fd);
}



ISemanticData getParseData(NobaseFile fd)
{
   if (fd == null) return null;
   if (parse_data.get(fd) == null) {
      parseFile(fd);
    }

   return parse_data.get(fd);
}



/********************************************************************************/
/*										*/
/*	Editing methods 							 */
/*										*/
/********************************************************************************/

void editProject(Element pxml)
{
   for (Element oelt : IvyXml.children(pxml,"OPTION")) {
      String k = IvyXml.getAttrString(oelt,"KEY");
      String v = IvyXml.getAttrString(oelt,"VALUE");
      nobase_prefs.setProperty(k,v);
    }

   Set<NobasePathSpec> done = new HashSet<NobasePathSpec>();
   boolean havepath = false;
   for (Element pelt : IvyXml.children(pxml,"PATH")) {
      havepath = true;
      File f1 = new File(IvyXml.getAttrString(pelt,"DIRECTORY"));
      try {
	 f1 = f1.getCanonicalFile();
       }
      catch (IOException e) {
	 f1 = f1.getAbsoluteFile();
       }
      boolean fnd = false;
      for (NobasePathSpec ps : project_paths) {
	 if (done.contains(ps)) continue;
	 File p1 = ps.getFile();
	 if (f1.equals(p1)) {
	    done.add(ps);
	    fnd = true;
	    // handle changes to ps at this point
	    break;
	  }
       }
      if (!fnd) {
	 boolean usr = IvyXml.getAttrBool(pelt,"USER");
         boolean exc = IvyXml.getAttrBool(pelt,"EXCLUDE");
	 NobasePathSpec ps = project_manager.createPathSpec(f1,usr,exc);
	 done.add(ps);
	 project_paths.add(ps);
       }
    }

   if (havepath) {
      for (Iterator<NobasePathSpec> it = project_paths.iterator(); it.hasNext(); ) {
	 NobasePathSpec ps = it.next();
	 if (!done.contains(ps)) it.remove();
       }
    }

   // project_nature.rebuildPath();

   saveProject();
}




/********************************************************************************/
/*										*/
/*	Create new package							*/
/*										*/
/********************************************************************************/

void createPackage(String name,boolean force,IvyXmlWriter xw)
{
   File dir = null;
   String [] comps = name.split("\\.");
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getFile();
      if (!f.exists()) continue;
      for (int i = 0; i < comps.length-1; ++i) {
	 f = new File(f,comps[i]);
       }
      if (f.exists()) {
	 dir = new File(f,comps[comps.length-1]);
	 break;
       }
    }

   if (dir != null && xw != null) {
      xw.begin("PACKAGE");
      xw.field("NAME",name);
      xw.field("PATH",dir.getAbsolutePath());
      xw.end("PACKAGE");
    }
}


/********************************************************************************/
/*										*/
/*	Create new package							*/
/*										*/
/********************************************************************************/

void findPackage(String name,IvyXmlWriter xw)
{
   File dir = null;
   String [] comps = name.split("\\.");
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getFile();
      if (!f.exists()) continue;
      for (int i = 0; i < comps.length; ++i) {
	 f = new File(f,comps[i]);
       }
      if (f.exists()) {
	 dir = new File(f,comps[comps.length-1]);
	 break;
       }
    }

   if (dir != null && xw != null) {
      xw.begin("PACKAGE");
      xw.field("NAME",name);
      xw.field("PATH",dir.getAbsolutePath());
      xw.end("PACKAGE");
    }
}



/********************************************************************************/
/*										*/
/*	Create new module							*/
/*										*/
/********************************************************************************/

void createModule(String name,String cnts,IvyXmlWriter xw) throws NobaseException
{
   File fil = findModuleFile(name);

   try {
      FileWriter fw = new FileWriter(fil);
      fw.write(cnts);
      fw.close();
    }
   catch (IOException e) {
      throw new NobaseException("Problem writing new module code",e);
    }

   if (xw != null) {
      xw.begin("FILE");
      xw.field("PATH",fil.getAbsolutePath());
      xw.end();
    }

   build(true,false);
}




public File findModuleFile(String name) throws NobaseException
{
   File dir = null;
   String [] comps = name.split("\\.");
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser()) continue;
      File f = ps.getFile();
      if (!f.exists()) continue;
      for (int i = 0; i < comps.length-1; ++i) {
	 f = new File(f,comps[i]);
       }
      if (f.exists()) {
	 dir = f;
	 break;
       }
    }

   if (dir == null)
      throw new NobaseException("Can't find package for new module " + name);

   File fil = new File(dir,comps[comps.length-1] + ".js");

   return fil;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void saveProject()
{
   try {
      File f1 = new File(base_directory,".jsproject");
      IvyXmlWriter xw = new IvyXmlWriter(f1);
      outputXml(xw);
      xw.close();
    }
   catch (IOException e) {
      NobaseMain.logE("Problem writing project file",e);
    }
}



void outputXml(IvyXmlWriter xw)
{
   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("BASE",base_directory.getPath());
   for (NobasePathSpec ps : project_paths) {
      ps.outputXml(xw);
    }
   for (File fs : project_files) {
      outputFile(fs,xw);
    }
   nobase_prefs.outputXml(xw,false);
   xw.end("PROJECT");
}




void outputProject(boolean files,boolean paths,boolean clss,boolean opts,IvyXmlWriter xw)
{
   if (xw == null) return;

   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("PATH",base_directory.getPath());
   xw.field("WORKSPACE",project_manager.getWorkSpaceDirectory().getPath());

   if (paths) {
      for (NobasePathSpec ps : project_paths) {
	 ps.outputXml(xw);
       }
    }
   if (files) {
      for (File fs : project_files) {
	 outputFile(fs,xw);
       }
    }

   if (opts) nobase_prefs.outputXml(xw,true);

   xw.end("PROJECT");
}



private void outputFile(File fs,IvyXmlWriter xw) 
{
   xw.begin("FILE");
   xw.field("NAME",fs.getPath());
   xw.end("FILE");
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void open()
{
   if (is_open) return;
   for (NobasePathSpec ps : project_paths) {
      File dir = ps.getFile();
      loadFiles(null,dir,false);
    }
   is_open = true;
   saveProject();
}


private void findFiles(boolean reload)
{
   for (NobasePathSpec ps : project_paths) {
      if (ps.isUser()) {
         File dir = ps.getFile();
         findFiles(dir,null,reload);
       }
    } 
}



private void findFiles(File f,String pfx,boolean reload)
{
   for (NobasePathSpec ps : project_paths) {
      if (!ps.isUser() || ps.isExclude()) {
         if (ps.match(f)) return;
       }
    }
   
   if (f.isDirectory()) {
      File [] fls = f.listFiles(new SourceFilter());
      for (File f1 : fls) {
         String npfx = f1.getName();
         if (pfx != null) npfx = pfx + "." + npfx;
         findFiles(f1,npfx,reload);
       }
      return;
    }
   
   String mnm = f.getName();
   int idx = mnm.lastIndexOf(".");
   if (idx >= 0) mnm = mnm.substring(0,idx);
   if (pfx != null) mnm = pfx + "." + mnm;
   NobaseFile fd = nobase_main.getFileManager().getNewFileData(f,mnm,this);
   ISemanticData isd = parse_data.get(fd);
   if (reload) {
      fd.reload();
      isd = null;
    }
   all_files.add(fd);
   if (isd == null) {
      ISemanticData sd = parseFile(fd);
      if (sd != null) {
         IvyXmlWriter xw = NobaseMain.getNobaseMain().beginMessage("FILEERROR");
         xw.field("PROJECT",sd.getProject().getName());
         xw.field("FILE",fd.getFile().getPath());
         xw.begin("MESSAGES");
         for (NobaseMessage m : sd.getMessages()) {
            try {
               System.err.println("NOBASE: PARSE ERROR: " + m);
               NobaseUtil.outputProblem(m,sd,xw);
             }
            catch (Throwable t) {
               NobaseMain.logE("Nobase error message: ",t);
             }
          }
         xw.end("MESSAGES");
         NobaseMain.getNobaseMain().finishMessage(xw);
       }
    }
}



void build(boolean refresh,boolean reload)
{
   if (!is_open) {
      open();
      return;
    }

   Set<NobaseFile> oldfiles = null;

   if (refresh) {
      oldfiles = new HashSet<NobaseFile>(all_files);
      if (reload) {
	 all_files.clear();
	 parse_data.clear();
       }
    }

   for (NobasePathSpec ps : project_paths) {
      File dir = ps.getFile();
      loadFiles(null,dir,reload);
    }

   if (oldfiles != null) {
      handleRefresh(oldfiles);
    }
}



private void handleRefresh(Set<NobaseFile> oldfiles)
{
   IvyXmlWriter xw = nobase_main.beginMessage("RESOURCE");
   int ctr = 0;
   for (NobaseFile fd : all_files) {
      NobaseFile old = null;
      for (NobaseFile ofd : oldfiles) {
	 if (ofd.getFile().equals(fd.getFile())) {
	    old = ofd;
	    break;
	  }
       }
      if (old == null) {
	 outputDelta(xw,"ADDED",fd);
	 ++ctr;
       }
      else if (old.getLastDateLastModified() != fd.getLastDateLastModified()) {
	 oldfiles.remove(old);
	 outputDelta(xw,"CHANGED",fd);
	 ++ctr;
       }
      else {
	 oldfiles.remove(old);
       }
    }
   for (NobaseFile fd : oldfiles) {
      outputDelta(xw,"REMOVED",fd);
      ++ctr;
    }
   if (ctr > 0) {
      nobase_main.finishMessage(xw);
    }
}



private void outputDelta(IvyXmlWriter xw,String act,NobaseFile ifd)
{
   xw.begin("DELTA");
   xw.field("KIND",act);
   xw.begin("RESOURCE");
   xw.field("TYPE","FILE");
   xw.field("PROJECT",project_name);
   xw.field("LOCATION",ifd.getFile().getAbsolutePath());
   xw.end("RESOURCE");
   xw.end("DELTA");
}




/********************************************************************************/
/*										*/
/*	Search commands 							*/
/*										*/
/********************************************************************************/

synchronized void patternSearch(String pat,String typ,boolean defs,boolean refs,boolean sys,IvyXmlWriter xw)
{
   NobaseSearchInstance search = new NobaseSearchInstance(this);
   NobaseAstVisitor nv = search.getFindSymbolsVisitor(pat,typ);
   for (NobaseFile ifd : all_files) {
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      isd.getRootNode().accept(nv);
    }

   NobaseAstVisitor av = search.getLocationsVisitor(defs,refs,false,false,false);
   for (NobaseFile ifd : all_files) {
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      isd.getRootNode().accept(av);
    }

   List<SearchResult> rslt = search.getMatches();
   if (rslt == null) return;
   for (SearchResult mtch : rslt) {
      xw.begin("MATCH");
      xw.field("OFFSET",mtch.getOffset());
      xw.field("LENGTH",mtch.getLength());
      xw.field("STARTOFFSET",mtch.getOffset());
      xw.field("ENDOFFSET",mtch.getOffset() + mtch.getLength());
      xw.field("FILE",mtch.getFile().getFile().getPath());
      NobaseSymbol sym = mtch.getContainer();
      if (sym != null) {
	 sym.outputNameData(mtch.getFile(),xw);
       }
      xw.end("MATCH");
    }
}


void findAll(String file,int soff,int eoff,boolean defs,boolean refs,
      boolean imps,boolean typ,boolean ronly,boolean wonly,IvyXmlWriter xw)
{
   NobaseSearchInstance search = new NobaseSearchInstance(this);
   NobaseAstVisitor av = search.getFindLocationVisitor(soff,eoff);
   for (NobaseFile ifd : all_files) {
      if (!ifd.getFile().getPath().equals(file)) continue;
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      isd.getRootNode().accept(av);
    }

   search.outputSearchFor(xw);

   NobaseAstVisitor av1 = search.getLocationsVisitor(defs,refs,imps,ronly,wonly);
   for (NobaseFile ifd : all_files) {
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      isd.getRootNode().accept(av1);
    }

   List<SearchResult> rslt = search.getMatches();
   if (rslt == null) return;
   for (SearchResult mtch : rslt) {
      xw.begin("MATCH");
      xw.field("OFFSET",mtch.getOffset());
      xw.field("LENGTH",mtch.getLength());
      xw.field("STARTOFFSET",mtch.getOffset());
      xw.field("ENDOFFSET",mtch.getOffset() + mtch.getLength());
      xw.field("FILE",mtch.getFile().getFile().getPath());
      NobaseSymbol sym = mtch.getContainer();
      if (sym != null) {
	 sym.outputNameData(mtch.getFile(),xw);
       }
      xw.end("MATCH");
    }
}




void getFullyQualifiedName(String file,int spos,int epos,IvyXmlWriter xw)
{
   NobaseSearchInstance search = new NobaseSearchInstance(this);
   NobaseAstVisitor av = search.getFindLocationVisitor(spos,epos);
   for (NobaseFile ifd : all_files) {
      if (!ifd.getFile().getPath().equals(file)) continue;
      search.setFile(ifd);
      ISemanticData isd = getParseData(ifd);
      isd.getRootNode().accept(av);
    }

   Set<NobaseSymbol> syms = search.getSymbols();
   if (syms == null) return;
   for (NobaseSymbol sym : syms) {
      sym.outputFullName(xw);
    }
}



synchronized void getTextRegions(String bid,String file,String cls,boolean pfx,boolean statics,
      boolean compunit,boolean imports,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw)
	throws NobaseException
{
   if (file == null) {
      String mnm = cls;
      while (mnm != null) {
	 try {
	    File f1 = findModuleFile(mnm);
	    if (f1 != null) {
	       file = f1.getAbsolutePath();
	       break;
	     }
	  }
	 catch (Throwable t) { }
	 int idx = mnm.lastIndexOf(".");
	 if (idx < 0) break;
	 mnm = mnm.substring(0,idx);
       }
      if (file == null) throw new NobaseException("File must be given");
    }
   try {
      File f1 = new File(file);
      file = f1.getCanonicalPath();
    }
   catch (IOException e) { }

   NobaseFile rf = findFile(file);

   ISemanticData isd = getSemanticData(file);
   if (isd == null) throw new NobaseException("Can't find file data for " + file);

   NobaseSearchInstance search = new NobaseSearchInstance(this);
   search.setFile(rf);
   search.findTextRegions(isd,pfx,statics,compunit,imports,pkg,topdecls,fields,all,xw);
}





/********************************************************************************/
/*										*/
/*	Methods to load files							*/
/*										*/
/********************************************************************************/

private void loadFiles(String pfx,File dir,boolean reload)
{
   File [] fls = dir.listFiles(new SourceFilter());

   if (fls != null) {
      for (File f : fls) {
	 if (f.isDirectory()) {
	    String nm = f.getName();
	    if (!nm.equals("node_modules")) {
	       String opfx = pfx;
	       if (pfx == null) pfx = nm;
	       else pfx += "." + nm;
	       loadFiles(pfx,f,reload);
	       pfx = opfx;
	     }
	  }
	 else {
	    String mnm = f.getName();
	    int idx = mnm.lastIndexOf(".");
	    if (idx >= 0) mnm = mnm.substring(0,idx);
	    if (pfx != null) mnm = pfx + "." + mnm;
	    NobaseFile fd = nobase_main.getFileManager().getNewFileData(f,mnm,this);
	    ISemanticData isd = parse_data.get(fd);
	    if (reload) {
	       fd.reload();
	       isd = null;
	     }
	    all_files.add(fd);
	    if (isd == null) {
	       ISemanticData sd = parseFile(fd);
	       if (sd != null) {
		  IvyXmlWriter xw = NobaseMain.getNobaseMain().beginMessage("FILEERROR");
		  xw.field("PROJECT",sd.getProject().getName());
		  xw.field("FILE",fd.getFile().getPath());
		  xw.begin("MESSAGES");
		  for (NobaseMessage m : sd.getMessages()) {
		     try {
			System.err.println("NOBASE: PARSE ERROR: " + m);
			NobaseUtil.outputProblem(m,sd,xw);
		      }
		     catch (Throwable t) {
			NobaseMain.logE("Nobase error message: ",t);
		      }
		   }
		  xw.end("MESSAGES");
		  NobaseMain.getNobaseMain().finishMessage(xw);
		}
	     }
	  }
       }
    }
}



private ISemanticData parseFile(NobaseFile fd)
{
   IParser pp = nobase_main.getParser();
   ISemanticData sd = pp.parse(this,fd);
   if (sd != null) {
      parse_data.put(fd,sd);
      project_resolver.resolveSymbols(sd);
    }
   else parse_data.remove(fd);

   System.err.println("NOBASE: PARSE " + fd.getFile());
   return sd;
}




private static class SourceFilter implements FileFilter {

   @Override public boolean accept(File path) {
      if (path.isDirectory()) return true;
      String name = path.getName();
      if (name.endsWith(".js") || name.endsWith(".JS")) return true;
      return false;
    }

}	// end of inner class SourceFilter


}	// end of class NobaseProject




/* end of NobaseProject.java */

