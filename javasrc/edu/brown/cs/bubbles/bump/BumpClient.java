/********************************************************************************/
/*										*/
/*		BumpClient.java 						*/
/*										*/
/*	BUblles Mint Partnership main class					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko	*/
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



package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.xml.*;
import edu.brown.cs.ivy.swing.SwingEventListenerList;

import org.w3c.dom.Element;

import javax.swing.JOptionPane;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.net.InetAddress;


/**
 *	This class provides an interface between Code Bubbles and the back-end
 *	IDE.  This particular implementation works for ECLIPSE.
 *
 *	At some point, this should be converted into an interface that can then
 *	be implemented by an appropriate class for each back end IDE.
 *
 **/

public class BumpClient implements BumpConstants, BoardConstants, MintConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private MintControl	mint_control;
private String		mint_name;
private String		source_id;
private BumpProblemSet	problem_set;
private BumpBreakSet	break_set;
private BumpRunManager	run_manager;
private Map<BumpFileHandler,File> file_handlers;
private Map<String,String> option_map;
private Map<String,NameCollector> name_collects;
private Map<String,EvalData> eval_handlers;
private int		collect_id;
private Map<BumpChangeHandler,Boolean> change_handlers;
private SwingEventListenerList<BumpOpenEditorBubbleHandler> open_editor_bubble_handlers;
private SwingEventListenerList<BumpProgressHandler> progress_handlers;

private boolean 	eclipse_active;
private boolean 	eclipse_starting;
private boolean 	doing_exit;
private boolean 	same_host;

private static BoardProperties board_properties = null;
private static BumpClient default_client = null;

private static final int MAX_DELAY = 30000;





/********************************************************************************/
/*										*/
/*	Static constructors							*/
/*										*/
/********************************************************************************/

/**
 *	Get the singular instance of the IDE interface.
 **/

public synchronized static BumpClient getBump()
{
   if (default_client == null) {
      loadProperties();
      default_client = new BumpClient();
    }

   return default_client;
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BumpClient()
{
   problem_set = new BumpProblemSet();
   break_set = new BumpBreakSet(this);
   run_manager = new BumpRunManager();
   eclipse_active = false;
   eclipse_starting = false;
   doing_exit = false;
   same_host = true;

   mint_control = BoardSetup.getSetup().getMintControl();
   mint_name = BoardSetup.getSetup().getMintName();

   file_handlers = new ConcurrentHashMap<BumpFileHandler,File>();
   option_map = new HashMap<String,String>();
   name_collects = new HashMap<String,NameCollector>();
   eval_handlers = new HashMap<String,EvalData>();
   change_handlers = new ConcurrentHashMap<BumpChangeHandler,Boolean>();
   open_editor_bubble_handlers = new SwingEventListenerList<BumpOpenEditorBubbleHandler>(
      BumpOpenEditorBubbleHandler.class);
   progress_handlers = new SwingEventListenerList<BumpProgressHandler>(BumpProgressHandler.class);

   collect_id = (int)(Math.random() * 10000);

   source_id = "BUBBLES_" + IvyExecQuery.getHostName() + "_" + IvyExecQuery.getProcessId();

   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());

   switch (BoardSetup.getSetup().getRunMode()) {
      case SERVER :
	 mint_control.register("<BUMP TYPE='FILEGET'/>",new FileGetServerHandler());
	 break;
    }
}




/********************************************************************************/
/*										*/
/*	Web interaction methods 						*/
/*										*/
/********************************************************************************/

public void useWebMint(String key,String url)
{
   if (key == null || url == null) return;

   String msg = "<MINT><WEB URL='" + url + "' KEY='" + key + "' /></MINT>";

   mint_control.send(msg);

   BoardLog.logD("BUMP","Web connectivity setup up");
}



/********************************************************************************/
/*										*/
/*	Eclipse interaction methods						*/
/*										*/
/********************************************************************************/

/**
 *	Return the name of the back end.
 **/

public String getName() 			{ return "Eclipse"; }




/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

public void startIDE()
{
   synchronized (this) {
      if (eclipse_starting) return;
      eclipse_starting = true;
    }

   ensureRunning();

   sendMessage("ENTER");

   Runtime.getRuntime().addShutdownHook(new CloseBedrock());

   sendMessage("LOGLEVEL",null,"LEVEL='" + BoardLog.getLogLevel().toString() +"'",null);

   BoardProperties mprops = BoardProperties.getProperties("Metrics");
   String monon = mprops.getProperty(BOARD_METRIC_PROP_MONITOR,"true");
   sendMessage("MONITOR",null,"ON='" + monon + "'", null);

   same_host = isIdeOnSameHost();

   grabPreferences();

   Element pxml = getXmlReply("PROJECTS",null,null,null,0);
   if (!IvyXml.isElement(pxml,"RESULT")) {
      JOptionPane.showMessageDialog(null,
				       "<html><p>Eclipse was not started correctly.<p>Perhaps it hadn't terminated yet." +
				       "<p>Please try starting again.",
				       "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

   if (IvyXml.getChild(pxml,"PROJECT") == null) {
      sendMessage("CREATEPROJECT",null,null,null);
    }

   buildAllProjects(false,false,false);

   loadBreakpoints();

   run_manager.setup();

   synchronized (this) {
      eclipse_active = true;
      notifyAll();
    }
}


private void ensureRunning()
{
   if (tryPing()) return;

   String eclipsedir = board_properties.getProperty(BOARD_PROP_ECLIPSE_DIR);
   String ws = board_properties.getProperty(BOARD_PROP_ECLIPSE_WS);

   File ef = new File(eclipsedir);
   File ef1 = null;
   for (String s : BOARD_ECLIPSE_START) {
      ef1 = new File(ef,s);
      if (ef1.exists() && ef1.canExecute()) break;
    }
   if (!ef1.exists() || !ef1.canExecute()) ef1 = new File(ef,"eclipse");
   String efp = ef1.getPath();
   if (efp.endsWith(".app") || efp.endsWith(".exe")) efp = efp.substring(0,efp.length()-4);

   String cmd = "'" + efp + "'";
   if (!board_properties.getBoolean(BOARD_PROP_ECLIPSE_FOREGROUND,false)) {
      cmd += " -application edu.brown.cs.bubbles.bedrock.application -nosplash";
    }
   else {
      cmd += " -nosplash";
    }
   if (ws != null) cmd += " -data '" + ws + "'";

   String eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_OPTIONS);
   if (eopt != null) cmd += " " + eopt;
   if (board_properties.getBoolean(BOARD_PROP_ECLIPSE_CLEAN)) {
      if (!cmd.contains("-clean")) cmd += " -clean";
      board_properties.remove(BOARD_PROP_ECLIPSE_CLEAN);
      try {
	 board_properties.save();
       }
      catch (IOException e) { }
    }

   cmd += " -vmargs '-Dedu.brown.cs.bubbles.MINT=" + mint_name + "'";
   eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
   if (eopt != null) cmd += " " + eopt;

   try {
      IvyExec ex = new IvyExec(cmd);
      boolean eok = false;
      for (int i = 0; i < 200; ++i) {
	 synchronized (this) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (tryPing()) {
	    BoardLog.logI("BUMP","Eclipse started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting eclipse");
	    JOptionPane.showMessageDialog(null,
					     "Eclipse could not be started. Check the eclipse log",
					     "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Eclipse doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running eclipse: " + e);
      System.exit(1);
    }
}



/**
 *	Shut down the back end IDE if we started it.  This should have no effect if the user is running
 *	the back end with a full user interface.
 **/

public void stopIDE()
{
   synchronized (this) {
      eclipse_active = false;
      doing_exit = true;
    }

   sendMessage("EXIT");
}



/**
 *	Start the back end if necessary and wait for it to be ready.
 **/

public void waitForIDE()
{
   synchronized (this) {
      if (eclipse_active) return;
    }

   startIDE();

   synchronized (this) {
      while (!eclipse_active) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
    }
}



/********************************************************************************/
/*										*/
/*	File editing methods							*/
/*										*/
/********************************************************************************/

/**
 *	Open a file.  This causes the IDE to track changes to the given file.  If
 *	getcnts is specified or if the file is open in the IDE and is different
 *	tha it is on disk, the the contents of the file is returned.  Otherwise
 *	null is returned.
 *	@param pname The project name
 *	@param file The file to open
 *	@param getcnts A flag that forces the IDE to return the file contents
 *	@param id The current edit-id to indicate which edit this represents
 **/

public Element startFile(String pname,File file,boolean getcnts,int id) throws BumpException
{
   waitForIDE();

   if (file == null) return null;

   String flds = "FILE='" + file.getPath() + "'";
   if (id >= 0) flds += " ID='" + id + "'";
   if (getcnts || !same_host) flds += " CONTENTS='T'";

   Element x = getXmlReply("STARTFILE",pname,flds,null,0);

   if (x == null) throw new BumpException("Couldn't start file " + file);
   if (IvyXml.isElement(x,"ERROR")) {
      String txt = IvyXml.getText(x);
      if (txt == null) txt = IvyXml.getTextElement(x,"MESSAGE");
      throw new BumpException("Problem starting file: " + txt);
    }
   if (!IvyXml.isElement(x,"RESULT")) {
      throw new BumpException("Unexpected return on start file: " + IvyXml.convertXmlToString(x));
    }

   return x;
}



/**
 *	Note an edit to the specified file.  The region from start to end is first
 *	deleted and then the given string is inserted in its place.  If start == end,
 *	then no deletion occurs.  If txt is null, then no insertion occurs.
 *
 *	This routine causes the IDE and any other bubbles instances to update their
 *	version of the file in sync with this update.
 *
 *	The side effect of an edit is that a new elision structure as well as the
 *	set of problems associated with the file will be sent back to BUMP
 *	asynchronously and then reported to the user.
 *
 *	The id field indicates the edit number.  These are checked when reporting the
 *	asynchronous results.  If the last edit number for the file is different than
 *	the edit number associated with problems or the AST, then no asynchronous
 *	notifications are given.  This lets multiple edits be quickly done without
 *	having to report all the intermediate results.	Note that because of timing
 *	issues, the user might also want to compare the edit id associated with the
 *	asynchronouos results against what it perceives as the current edit id.
 **/

public void editFile(String pname,File file,int id,int start,int end,String txt)
{
   waitForIDE();

   String flds = "FILE='" + file.getPath() + "'";
   if (id >= 0) flds += " ID='" + id + "'";
   flds += " NEWLINE='true'";
   String edit = "<EDIT START='" + start + "' END='" + end + "'>";
   if (txt != null) {
      // TODO: if txt contains ]]> we have to split it
      edit += "<![CDATA[" + txt + "]]>";
    }
   edit += "</EDIT>";

   sendMessage("EDITFILE",pname,flds,edit);
}



/**
 *	This call sets up AST feedback from the back end for a file.  Since files can be
 *	large and we generally only want a small part of them, this routine lets the user
 *	set up a list of file regions that should be reported.	 The rgns parameter is a
 *	set of XML descriptions of the form <REGION START='offset' END='endoffset' />.
 *
 *	The compute parameter will force an immediate computation of the elision structure
 *	which will be returned in XML.	If the parameter is false, no tree is computed
 *	at this point.	In either case, the region settings are used the next time the file
 *	is edited.
 **/

public Element setupElision(String pname,File file,String rgns,boolean compute)
{
   waitForIDE();

   String flds = "FILE='" + file.getPath() + "' COMPUTE='" + Boolean.toString(compute) + "'";

   return getXmlReply("ELIDESET",pname,flds,rgns,0);
}



/**
 *	This lets code bubbles control how the IDE interface works.  The properties that
 *	can be set here include:
 *
 *	AUTOELIDE	TRUE or FALSE		If true, then return elision after each edit
 *
 *	ELIDEDELAY	integer 		Delay in milliseconds before sending AST data
 *
 *	LINESEP 	string			The line separator to be used in the file.
 *
 **/

public boolean setEditParameter(String key,String value)
{
   waitForIDE();

   String flds = "NAME='" + key + "' VALUE='" + value + "'";

   return getStatusReply("EDITPARAM",null,flds,null,0);
}



/**
 *	Reverts the given file to the last saved version.
 **/

public Element revertFile(String pname,File file)
{
   waitForIDE();

   String flds = "REFRESH='T'";
   String fils = "<FILE NAME='" + file.getPath() + "' />";

   return getXmlReply("COMMIT",pname,flds,fils,0);
}



/**
 *	Save the given file.
 **/

public Element saveFile(String pname,File file)
{
   waitForIDE();

   String flds = "SAVE='T'";
   String fils = "<FILE NAME='" + file.getPath() + "' />";

   return getXmlReply("COMMIT",pname,flds,fils,0);
}



/**
 *	Save all open files
 **/

public Element saveAll()
{
   waitForIDE();

   String flds = "SAVE='T'";

   Element rslt = getXmlReply("COMMIT",null,flds,null,0);

   compile(false,false,false);

   return rslt;
}



/**
 *	Revert all open files
 **/

public Element revertAll()
{
   waitForIDE();

   String flds = "REFRESH='T'";

   return getXmlReply("COMMIT",null,flds,null,0);
}



/********************************************************************************/
/*										*/
/*	Remote file methods							*/
/*										*/
/********************************************************************************/

public File getRemoteFile(File lcl,String kind,File rem)
{
   switch (BoardSetup.getSetup().getRunMode()) {
      case NORMAL :
      case SERVER :
	 if (rem == null) return lcl;
	 return rem;
    }

   String id = source_id + "_" + (++collect_id);
   String msg = "<BUMP TYPE='FILEGET' ID='" + id + "'";
   if (kind != null) msg += " KIND='" + kind + "'";
   if (rem != null) msg += " FILE='" + rem.getPath() + "'";
   FileGetClientHandler ch = new FileGetClientHandler(lcl);
   mint_control.register("<BUMPFILE ID='" + id + "' />",ch);
   MintDefaultReply mr = new MintDefaultReply();
   mint_control.send(msg,mr,MINT_MSG_FIRST_NON_NULL);
   String sts = mr.waitForString();
   if (!ch.isOkay()) sts = "FAIL";
   mint_control.unregister(ch);
   if (sts == null || !sts.equals("OK")) {
      lcl.delete();
      return null;
    }
   return lcl;

}




private class FileGetServerHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      Element xml = msg.getXml();
      String kind = IvyXml.getAttrString(xml,"KIND");
      String filenm = IvyXml.getAttrString(xml,"FILE");
      String id = IvyXml.getAttrString(xml,"ID");
      File f = null;
      if (kind != null && kind.equals("BDOC")) f = BoardSetup.getDocumentationFile();
      else if (kind != null && kind.equals("NOTE")) {
	 File f1 = BoardSetup.getBubblesPluginDirectory();
	 File f2 = new File(filenm);
	 f = new File(f1,f2.getName());
       }
      else f = new File(filenm);

      try {
	 FileInputStream fr = new FileInputStream(f);
	 long len = f.length();
	 int pos = 0;
	 byte [] buf = new byte[40960];
	 while (pos < len) {
	    int ct = fr.read(buf,0,buf.length);
	    MintDefaultReply mr = new MintDefaultReply();
	    IvyXmlWriter xw = new IvyXmlWriter();
	    xw.begin("BUMPFILE");
	    xw.field("ID",id);
	    xw.field("POS",pos);
	    xw.field("LEN",len);
	    xw.field("CT",ct);
	    xw.bytesElement("CNTS",buf,0,ct);
	    xw.end("BUMPFILE");
	    mint_control.send(xw.toString(),mr,MintConstants.MINT_MSG_FIRST_NON_NULL);
	    mr.waitFor();
	    pos += ct;
	  }
	 fr.close();
	 msg.replyTo("OK");
       }
      catch (IOException e) {
	 msg.replyTo("FAIL");
       }

    }

}	// end of inner class FileGetServerHandler




private class FileGetClientHandler implements MintHandler {

   private FileOutputStream file_writer;
   private boolean is_okay;

   FileGetClientHandler(File f) {
      file_writer = null;
      is_okay = false;
      try {
	 file_writer = new FileOutputStream(f);
       }
      catch (IOException e) { }
    }

   boolean isOkay()				{ return is_okay; }

   @Override public void receive(MintMessage msg,MintArguments args) {
      Element xml = msg.getXml();
      byte [] buf = IvyXml.getBytesElement(xml,"CNTS");
      int ct = IvyXml.getAttrInt(xml,"CT");
      int pos = IvyXml.getAttrInt(xml,"POS");
      int len = IvyXml.getAttrInt(xml,"LEN");
      try {
	 if (file_writer != null) {
	    file_writer.write(buf,0,ct);
	    if (ct + pos >= len) file_writer.close();
	    is_okay = true;
	  }
       }
      catch (IOException e) {
	 file_writer = null;
	 is_okay = false;
       }
      msg.replyTo("OK");
    }

}	// end of inner class FileGetClientHandler




/********************************************************************************/
/*										*/
/*	Project methods 							*/
/*										*/
/********************************************************************************/

/**
 *	Compile all active projects.  The parameters provide for different types
 *	of builds.
 *	@param clean If set, then a clean will be done before the build
 *	@param full If set, then everything will be recompiled, even files that have
 *	not been changed.
 *	@param refresh If set, then eclipse will refresh all projects from the current
 *	contents of the disk.
 **/

public void compile(boolean clean,boolean full,boolean refresh)
{
   waitForIDE();

   buildAllProjects(clean,full,refresh);
}



/**
 *	Open a project (if not already open) and return project information
 **/

public Element openProject(String name)
{
   waitForIDE();

   String q = "CLASSES='true'";

   Element xml = getXmlReply("OPENPROJECT",name,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Element proj = IvyXml.getChild(xml,"PROJECT");

   return proj;
}



/**
 *	Return the list of all projects
 **/

public Element getAllProjects()
{
   waitForIDE();

   Element e = getXmlReply("PROJECTS",null,null,null,0);

   if (!IvyXml.isElement(e,"RESULT")) return null;

   return e;
}



public void editProject(String name)
{
   waitForIDE();

   sendMessage("EDITPROJECT",name,null,null);
}



public void createProject()
{
   waitForIDE();

   sendMessage("CREATEPROJECT",null,null,null);
}




/********************************************************************************/
/*										*/
/*	Java Search queries							*/
/*										*/
/********************************************************************************/

/**
 *	Find the loction of a method given the project and name.  If the name is
 *	ambiguous, mutliple locations may be returned.	The name can contain a
 *	parenthesized argument list, i.e. package.method(int,java.lang.String).
 **/

public List<BumpLocation> findMethod(String proj,String name,boolean system)
{
   boolean cnstr = false;
   name = name.replace('$','.');

   String nm0 = name;
   int idx2 = name.indexOf("(");
   String args = "";
   if (idx2 >= 0) {
      nm0 = name.substring(0,idx2);
      args = name.substring(idx2);
      if (args.contains("<")) {
	 StringBuffer buf = new StringBuffer();
	 int lvl = 0;
	 for (int i = 0; i < args.length(); ++i) {
	    char c = args.charAt(i);
	    if (c == '<') ++lvl;
	    else if (c == '>') --lvl;
	    else if (lvl == 0) buf.append(c);
	  }
	 args = buf.toString();
	 name = nm0 + args;
       }
    }

   int idx0 = nm0.lastIndexOf(".");
   if (idx0 >= 0) {
      String mthd = nm0.substring(idx0+1);
      int idx1 = nm0.lastIndexOf(".",idx0-1);
      String clsn = nm0.substring(idx1+1,idx0);
      if (mthd.equals(clsn) || mthd.equals("<init>")) {
	 cnstr = true;
	 nm0 = nm0.substring(0,idx0);
	 if (idx2 > 0) nm0 += args;
	 name = nm0;
       }
    }

   if (name == null) {
      BoardLog.logI("BUMP","Empty name provided to java search");
      return null;
    }

   List<BumpLocation> locs = findMethods(proj,name,false,true,cnstr,system);

   if (locs == null || locs.isEmpty()) {
      int x1 = name.indexOf('(');
      if (cnstr && x1 >= 0) {				// check for nested constructor with extra argument
	 String mthd = name.substring(0,x1);
	 int x2 = mthd.lastIndexOf('.');
	 if (x2 >= 0) {
	    String pfx = name.substring(0,x2);
	    if (args.startsWith("(" + pfx + ",") || args.startsWith("(" + pfx + ")")) {
	       int ln = pfx.length();
	       args = "(" + args.substring(ln+2);
	       name = mthd + args;
	       locs = findMethods(proj,name,false,true,cnstr,system);
	    }
	 }
      }
   }

   return locs;
}




/**
 *	Return matches for methods, either references or definitions.  The cnstr parameter
 *	distinguishes normal methods from constructors.
 **/

public List<BumpLocation> findMethods(String proj,String pat,boolean refs,boolean defs,
					 boolean cnstr,boolean system)
{
   pat = pat.replace('$','.');

   if (pat == null) {
      BoardLog.logI("BUMP","Empty name provided to java search");
      return null;
    }

   waitForIDE();

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(pat,sw);
   sw.write("' DEFS='" + defs +"' REFS='" + refs + "'");
   if (cnstr) sw.write(" FOR='CONSTRUCTOR'");
   else sw.write(" FOR='METHOD'");
   if (system) sw.write(" SYSTEM='T'");

   Element xml = getXmlReply("JAVASEARCH",proj,sw.toString(),null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of BumpLocations containing the definitions of all the fields
 *	of the given class.
 **/

public List<BumpLocation> findFields(String proj,String clsn)
{
   waitForIDE();

   return findFields(proj,clsn,false,true);
}



/**
 *	Return a list of BumpLocations containing the definitions of all the fields
 *	of the given class.
 **/

public List<BumpLocation> findFields(String proj,String pat,boolean refs,boolean defs)
{
   waitForIDE();

   if (pat == null) return null;

   pat = pat.replace('$','.');

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(pat,sw);
   sw.write(".*");
   sw.write("' DEFS='" + defs + "' REFS='" + refs + "'");
   sw.write(" FOR='FIELD'");

   Element xml = getXmlReply("JAVASEARCH",proj,sw.toString(),null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of BumpLocations containing the class prefix.  This includes any
 *	initial comments, the package and import definitions, the class declaration line
 *	with extends and implements and the end of class brace and any subsequent comments.
 **/

public List<BumpLocation> findClassHeader(String proj,String clsn)
{
   waitForIDE();

   clsn = clsn.replace('$','.');
   clsn = IvyXml.xmlSanitize(clsn);

   String flds = "CLASS='" + clsn + "' PREFIX='T'";
   Element xml = getXmlReply("FINDREGIONS",proj,flds,null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of the compilation units that contain the given class.
 **/

public List<BumpLocation> findCompilationUnit(String proj,String clsn)
{
   waitForIDE();

   clsn = clsn.replace('$','.');
   clsn = IvyXml.xmlSanitize(clsn);

   String flds = "CLASS='" + clsn + "' COMPUNIT='T'";
   Element xml = getXmlReply("FINDREGIONS",proj,flds,null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of BumpLocations containing all the static initializers of
 *	the given class.  This does not include field definitions that have assignments.
 **/

public List<BumpLocation> findClassInitializers(String proj,String clsn)
{
   waitForIDE();

   clsn = clsn.replace('$','.');
   clsn = IvyXml.xmlSanitize(clsn);

   String flds = "CLASS='" + clsn + "' STATICS='T'";
   Element xml = getXmlReply("FINDREGIONS",proj,flds,null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return information about headers in a file
**/

public List<BumpLocation> findClassHeader(String proj,String clsn,boolean pkg,boolean imp)
{
   waitForIDE();

   clsn = clsn.replace('$','.');
   clsn = IvyXml.xmlSanitize(clsn);

   String flds = "CLASS='" + clsn + "'";
   if (imp) flds += " IMPORTS='T'";
   if (pkg) flds += " PACKAGE='T'";
   if (!imp && !pkg) flds += " TOPDECLS='T'";

   Element xml = getXmlReply("FINDREGIONS",proj,flds,null,0);

   return getSearchResults(proj,xml,false);
}





/**
 *	Return a list of BumpLocations (generally only a singleton) containing
 *	the complete class definition for the given class.
 **/

public List<BumpLocation> findClassDefinition(String proj,String clsn)
{
   waitForIDE();

   clsn = clsn.replace('$','.');

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(clsn,sw);
   sw.write("' DEFS='true' REFS='false'");
   sw.write(" FOR='TYPE'");

   Element xml = getXmlReply("JAVASEARCH",proj,sw.toString(),null,0);

   return getSearchResults(proj,xml,false);
}




/**
 *	Return a list of BumpLocations containing the definitions of all packages
 *	matching the given pattern
 **/

public List<BumpLocation> findPackages(String proj,String nm)
{
   waitForIDE();

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(nm,sw);
   sw.write("' DEFS='true' REFS='true'");
   sw.write(" FOR='PACKAGE'");

   Element xml = getXmlReply("JAVASEARCH",proj,sw.toString(),null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of BumpLocations containing the definitions of all packages
 *	matching the given pattern
 **/

public List<BumpLocation> findPackage(String proj,String nm)
{
   waitForIDE();

   if (nm == null) return null;
   if (nm.indexOf("<") >= 0) return null;

   String q = "NAME='" + IvyXml.xmlSanitize(nm) + "'";

   Element xml = getXmlReply("FINDPACKAGE",proj,q,null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of BumpLocations containing the definitions of all types
 *	matching the given pattern
 **/

public List<BumpLocation> findTypes(String proj,String nm)
{
   waitForIDE();

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(nm,sw);
   sw.write("' DEFS='true' REFS='false'");
   sw.write(" FOR='TYPE'");

   Element xml = getXmlReply("JAVASEARCH",proj,sw.toString(),null,0);

   return getSearchResults(proj,xml,false);
}



public List<BumpLocation> findAllClasses(String nm)
{
   waitForIDE();

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(nm,sw);
   sw.write("' DEFS='true' REFS='false' SYSTEM='true'");
   sw.write(" FOR='CLASS'");

   Element xml = getXmlReply("JAVASEARCH",null,sw.toString(),null,0);

   return getSearchResults(null,xml,false);
}



/**
 *	Return a list of BumpLocations containing the definitions of all annotations
 *	matching the given pattern.
 *	@param proj the project to search in, null implies all projects
 *	@param nm the search pattern
 *	@param def if true, include definitions in the output set
 *	@param ref if true, include references in the output set
 **/

public List<BumpLocation> findAnnotations(String proj,String nm,boolean def,boolean ref)
{
   waitForIDE();

   StringWriter sw = new StringWriter();
   sw.write("PATTERN='");
   IvyXml.outputXmlString(nm,sw);
   sw.write("' DEFS='" + Boolean.toString(def) + "' REFS='" + Boolean.toString(ref) + "'");
   sw.write(" FOR='ANNOTATION'");

   Element xml = getXmlReply("JAVASEARCH",proj,sw.toString(),null,0);

   return getSearchResults(proj,xml,false);
}



/**
 *	Return a list of all declarations of the given class.  This includes
 *	fields, methods, static initializers, and nested classes and interfaces.
 **/

public List<BumpLocation> findAllDeclarations(String proj,String clsn)
{
   waitForIDE();

   clsn = clsn.replace('$','.');
   clsn = IvyXml.xmlSanitize(clsn);

   String flds = "CLASS='" + clsn + "' ALL='T'";
   Element xml = getXmlReply("FINDREGIONS",proj,flds,null,0);

   return getSearchResults(proj,xml,false);
}



private static List<BumpLocation> getSearchResults(String proj,Element xml,boolean mtch)
{
   if (!IvyXml.isElement(xml,"RESULT")) return null;

   List<BumpLocation> rslt = new ArrayList<BumpLocation>();

   String srctyp = null;
   Element sfor = IvyXml.getChild(xml,"SEARCHFOR");
   if (sfor != null) srctyp = IvyXml.getAttrString(sfor,"TYPE");

   if (mtch) {
      for (Element me : IvyXml.children(xml,"MATCH")) {
	 String fnm = IvyXml.getAttrString(me,"FILE");
	 int offset = IvyXml.getAttrInt(me,"STARTOFFSET");
	 int length = IvyXml.getAttrInt(me,"LENGTH");
	 Element mi = IvyXml.getChild(me,"ITEM");
	 String pnm = IvyXml.getAttrString(me,"PROJECT");
	 if (pnm == null) pnm = IvyXml.getAttrString(mi,"PROJECT");
	 if (pnm == null) pnm = proj;
	 int acc = IvyXml.getAttrInt(me,"ACCURACY");
	 if (acc > 0) continue;
	 BumpLocation bl = new BumpLocation(pnm,fnm,offset,length,srctyp,mi);
	 rslt.add(bl);
       }
    }
   else {
      for (Element m : IvyXml.children(xml,"MATCH")) {
	 Element mi = IvyXml.getChild(m,"ITEM");
	 if (mi != null) {
	    String fnm = IvyXml.getAttrString(mi,"PATH");
	    int offset = IvyXml.getAttrInt(mi,"STARTOFFSET");
	    int length = IvyXml.getAttrInt(mi,"LENGTH");
	    String pnm = IvyXml.getAttrString(mi,"PROJECT");
	    if (pnm == null) pnm = proj;
	    BumpLocation bl = new BumpLocation(pnm,fnm,offset,length,mi);
	    rslt.add(bl);
	  }
       }
    }

   for (Element r : IvyXml.children(xml,"RANGE")) {
      String fnm = IvyXml.getAttrString(r,"PATH");
      int offset = IvyXml.getAttrInt(r,"START");
      int eoffset = IvyXml.getAttrInt(r,"END");
      if (eoffset < 0) continue;
      BumpLocation bl = new BumpLocation(proj,fnm,offset,eoffset-offset,null);
      rslt.add(bl);
    }

   for (Element mi : IvyXml.children(xml,"ITEM")) {
      String fnm = IvyXml.getAttrString(mi,"PATH");
      int offset = IvyXml.getAttrInt(mi,"STARTOFFSET");
      int length = IvyXml.getAttrInt(mi,"LENGTH");
      String pnm = IvyXml.getAttrString(mi,"PROJECT");
      if (pnm == null) pnm = proj;
      BumpLocation bl = new BumpLocation(pnm,fnm,offset,length,mi);
      rslt.add(bl);
    }

   for (Element pi : IvyXml.children(xml,"PACKAGE")) {
      String fnm = IvyXml.getAttrString(pi,"PATH");
      Element pe = IvyXml.getChild(pi,"ITEM");
      BumpLocation bl = new BumpLocation(proj,fnm,0,0,pe);
      rslt.add(bl);
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Text search queries							*/
/*										*/
/********************************************************************************/

/**
 *	Return a list of BumpLocations containing all text fragments in the
 *	given project that match the given text string.  Each location contains
 *	the match string and includes as its definition, the containing entity,
 *	i.e. method or field definition.  The flags control the type of search.
 *
 *	@param proj The project to search; use null for all open projects
 *	@param text The pattern to search for
 *	@param literal If true, then the pattern is taken as a literal string; otherwise
 *	it is assumed to be a regular expression using Java regular expression syntax.
 *	@param nocase If true, ignore case during matching (literal or regular expression)
 *	@param multiline Allow multiple line patterns if true
 **/


public Collection<BumpLocation> textSearch(String proj,String text,boolean literal,
					      boolean nocase,boolean multiline)
{
   waitForIDE();

   int fgs = 0;
   if (literal) fgs |= Pattern.LITERAL;
   if (nocase) fgs |= Pattern.CASE_INSENSITIVE;
   if (multiline) fgs |= Pattern.MULTILINE;

   text = IvyXml.xmlSanitize(text);

   String q = "PATTERN='" + text + "' FLAGS='" + fgs + "' MAX='256'";
   Element xml = getXmlReply("SEARCH",proj,q,null,0);
   BoardLog.logD("BUMP","Text search result = " + IvyXml.convertXmlToString(xml));

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   List<BumpLocation> rslt = getSearchResults(proj,xml,true);

   return rslt;
}


/********************************************************************************/
/*										*/
/*	Definition and reference queries					*/
/*										*/
/********************************************************************************/

/**
 *	Return a list of BumpLocations contain all references to the identifier at
 *	the given source positions.  If spos == epos, then the identifier is whatever
 *	is at that position.  Otherwise, spos and epos should bound the identifier. The
 *	match value of the returned locations indicates the use/definition of the identifier
 *	while the containing entity is returned as the source in the location.
 **/

public List<BumpLocation> findReferences(String proj,File file,int spos,int epos)
{
   waitForIDE();

   String q = "FILE='" + file.getPath() + "' START='" + spos + "' END='" + epos + "'";
   q += " EXACT='true' EQUIV='true'";

   Element xml = getXmlReply("FINDREFERENCES",proj,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   return getSearchResults(proj,xml,true);
}

/**
 *	Open an editor in eclipse with specified file and lineNumber
 **/

public void openEclipseEditor(String proj, File file, int lineNumber)
{
   waitForIDE();

   String q = "FILE='" + file.getPath() + "' LINENUMBER='" + lineNumber + "'";

   getXmlReply("OPENEDITOR", proj , q, null, 0);
}

/**
 *	This routine is similar to findReferences except that for fields and variables
 *	it restricts the search to either read references (write == false), or write
 *	references (write == true).  Note that for non-variables and fields, it returns
 *	exactly the same list as findReferences.
 **/

public List<BumpLocation> findRWReferences(String proj,File file,int spos,int epos,
						  boolean write)
{
   waitForIDE();

   String q = "FILE='" + file.getPath() + "' START='" + spos + "' END='" + epos + "'";
   if (write) q += " WONLY='T'";
   else q+= " RONLY='T'";
   q += " EXACT='true' EQUIV='true'";

   Element xml = getXmlReply("FINDREFERENCES",proj,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   return getSearchResults(proj,xml,true);
}



/**
 *	This routine returns a list of the definitions of the identifier denoted by the
 *	given positions.  If spos == epos then the position can be anywhere in an identiier.
 *	Otherwise the positions should bound the identifier.
 **/

public List<BumpLocation> findDefinition(String proj,File file,int spos,int epos)
{
   waitForIDE();

   String q = "FILE='" + file.getPath() + "' START='" + spos + "' END='" + epos + "'";

   Element xml = getXmlReply("FINDDEFINITIONS",proj,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   return getSearchResults(proj,xml,true);
}



/**
 *	This routine finds all implementations of a method.  It is similar to findDefinitions
 *	except that for a method, it will find not only the declared definition, but any other
 *	definitions that implement or override that definition.
 **/

public List<BumpLocation> findImplementations(String proj,File file,int spos,int epos)
{
   waitForIDE();

   String q = "FILE='" + file.getPath() + "' START='" + spos + "' END='" + epos + "'";
   q += " IMPLS='T'";

   Element xml = getXmlReply("FINDDEFINITIONS",proj,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   return getSearchResults(proj,xml,true);
}



/**
 *	This routine returns a list of the definitions of the identifier denoted by the
 *	given positions.  If spos == epos then the position can be anywhere in an identiier.
 *	Otherwise the positions should bound the identifier.
 **/

public List<BumpLocation> findTypeDefinition(String proj,File file,int spos,int epos)
{
   waitForIDE();

   String q = "FILE='" + file.getPath() + "' START='" + spos + "' END='" + epos + "'";
   q += " TYPE='true'";

   Element xml = getXmlReply("FINDDEFINITIONS",proj,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   return getSearchResults(proj,xml,true);
}



/**
 *	This routine will find all the names in the given project.  For small and moderate sized
 *	projects this is practical.  For very large projects this routine should probably be
 *	rewritten so that it only finds top level names (e.g. packages) and then another routine
 *	is made available to get the names internal to those.
 **/

public Collection<BumpLocation> findAllNames(String proj)
{
   return findAllNames(proj,null,true);
}



public Collection<BumpLocation> findAllNames(String proj,List<String> files,boolean bkg)
{
   waitForIDE();

   String nid = null;
   if (bkg) nid = "NAME_" + (++collect_id);
   NameCollector nc = null;

   String q = null;
   if (nid != null) {
      nc = new NameCollector();
      name_collects.put(nid,nc);
      q = "BACKGROUND='" + nid + "'";
    }

   String fls = null;
   if (files != null) {
      fls = "";
      for (String s : files) {
	 fls += "<FILE>" + s + "</FILE>\n";
       }
    }

   Element xml = getXmlReply("GETALLNAMES",proj,q,fls,0);
   if (!IvyXml.isElement(xml,"RESULT")) return null;

   if (nid == null) {
      Collection<BumpLocation> rslt = new ArrayList<BumpLocation>();
      for (Element fe : IvyXml.children(xml,"FILE")) {
	 String path = IvyXml.getTextElement(fe,"PATH");
	 for (Element itm : IvyXml.children(fe,"ITEM")) {
	    int offset = IvyXml.getAttrInt(itm,"STARTOFFSET");
	    int length = IvyXml.getAttrInt(itm,"LENGTH");
	    String pnm = IvyXml.getAttrString(itm,"PROJECT");
	    BumpLocation bl = new BumpLocation(pnm,path,offset,length,itm);
	    rslt.add(bl);
	  }
       }
      return rslt;
    }

   return nc.getNames();
}



/**
 *	This routine will find the fully qualified name of the identifier denoted by the
 *	given position.  For classes and interfaces it returns the full class name; for
 *	fields it returns the fully qualified field name; for methods it returns the fully
 *	qualified method name followed by a parenthesized list of the argument types.
 **/

public String getFullyQualifiedName(String proj,File file,int start,int end)
{
   waitForIDE();

   String flds = "FILE='" + file.getPath() + "' START='" + start + "' END='" + end + "'";
   Element xml = getXmlReply("GETFULLYQUALIFIEDNAME",proj,flds,null,0);

   Element cnt = IvyXml.getElementByTag(xml,"FULLYQUALIFIEDNAME");
   if (cnt == null) return null;
   String nm = IvyXml.getTextElement(cnt,"NAME");
   String sgn = IvyXml.getTextElement(cnt,"TYPE");
   if (sgn != null) {
      int idx0 = sgn.indexOf('(');
      if (idx0 >= 0) {
	 int idx1 = sgn.lastIndexOf(')');
	 String ps = sgn.substring(idx0,idx1+1);
	 String p = IvyFormat.formatTypeName(ps);
	 nm += p;
      }
    }

   return nm;
}




/********************************************************************************/
/*										*/
/*	Auto completion methods 						*/
/*										*/
/********************************************************************************/

/**
 *	This returns the list of possible completions for the given file at the
 *	given offset.  Since this routine can take a bit of time to evaluate, the
 *	passed in id is the current edit id and can be checked as part of the
 *	result to see if the user has made further changes since the request was
 *	made.
 **/

public Collection<BumpCompletion> getCompletions(String proj,File file,int id,int off)
{
   waitForIDE();

   String rq = "FILE='" + file.getPath() +"' OFFSET='" + off + "'";

   Element xml = getXmlReply("GETCOMPLETIONS",proj,rq,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Collection<BumpCompletion> rslt = new ArrayList<BumpCompletion>();

   Element root = IvyXml.getChild(xml,"COMPLETIONS");
   for (Element c : IvyXml.children(root,"COMPLETION")) {
      BumpCompletion bc = new BumpCompletionImpl(c);
      rslt.add(bc);
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Renaming methods							*/
/*										*/
/********************************************************************************/

public Element rename(String proj,File file,int spos,int epos,String newname)
{
   waitForIDE();

   saveAll();

   String rq = "FILE='" + file.getPath() + "' START='" + spos + "' END='" + epos + "'";
   rq += " NEWNAME='" + newname + "'";

   Element xml = getXmlReply("RENAME",proj,rq,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Element edits = IvyXml.getChild(xml,"EDITS");
   BoardLog.logD("BALE","RENAME EDITS: " + IvyXml.convertXmlToString(edits));

   return edits;
}



/********************************************************************************/
/*										*/
/*	Formatting methods							*/
/*										*/
/********************************************************************************/

public Element format(String proj,File file,int spos,int epos)
{
   waitForIDE();

   String rq = "FILE='" + file.getPath() + "'";
   String rgns = "<REGION START='" + spos + "' END='" + epos + "' />";

   Element xml = getXmlReply("FORMATCODE",proj,rq,rgns,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   BoardLog.logD("BALE","FORMAT EDITS: " + IvyXml.convertXmlToString(xml));

   return xml;
}




/********************************************************************************/
/*										*/
/*	Type hierarchy methods							*/
/*										*/
/********************************************************************************/

/**
 *	Return a structure representing type hierarchy information for the given
 *	package or class
 **/

public Element getTypeHierarchy(String proj,String pkg,String cls,boolean all)
{
   waitForIDE();

   String q = null;
   pkg = IvyXml.xmlSanitize(pkg);
   cls = IvyXml.xmlSanitize(cls);

   if (pkg != null) {
      q = "PACKAGE='" + pkg + "'";
    }
   else if (cls != null) {
      q = "CLASS='" + cls + "'";
    }
   if (all && q != null) q += " ALL='true'";
   else if (all) q = "ALL='true'";

   Element xml = getXmlReply("FINDHIERARCHY",proj,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Element he = IvyXml.getChild(xml,"HIERARCHY");

   return he;
}



/********************************************************************************/
/*										*/
/*	Call path methods							*/
/*										*/
/********************************************************************************/

/**
 *	This routine computes the set of calls between the from routine and the
 *	to routine.  Both are given as fully qualified method names (i.e. should
 *	include parameter types).  What is returned is a XML tree denoting the
 *	various calls.	The tree is truncated at either 4 levels deep or when
 *	a path is found.  The returned tree includes the locations of the various
 *	calls.
 *
 *	TODO: document the return XML
 **/

public List<BumpLocation> getCallPath(String proj,String from,String to)
{
   String rq = "FROM='" + from + "' TO='" + to + "' SHORTEST='T' LEVELS='4'";

   Element xml = getXmlReply("CALLPATH",proj,rq,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Element root = IvyXml.getChild(xml,"PATH");
   LinkedList<Element> workqueue = new LinkedList<Element>();
   workqueue.add(root);

   Element rslt = null;
   while (!workqueue.isEmpty()) {
      Element next = workqueue.poll();
      for (Element c : IvyXml.children(next,"CALL")) {
	 if (IvyXml.getAttrBool(c,"TARGET")) {
	    rslt = c;
	    break;
	  }
	 else workqueue.add(c);
       }
    }
   if (rslt == null) return null;

   workqueue.clear();
   for (Element c = rslt; c != null && c != root; c = (Element) c.getParentNode()) {
      workqueue.addFirst(c);
    }

   List<BumpLocation> locs = new ArrayList<BumpLocation>();
   for (Element c : workqueue) {
      Element mtch = IvyXml.getChild(c,"MATCH");
      String file = IvyXml.getAttrString(mtch,"FILE");
      int offset = IvyXml.getAttrInt(mtch,"STARTOFFSET",0);
      int length = IvyXml.getAttrInt(mtch,"LENGTH",0);
      Element itm = IvyXml.getChild(mtch,"ITEM");
      locs.add(new BumpLocation(proj,file,offset,length,itm));
    }

   Element ritm = IvyXml.getChild(rslt,"ITEM");
   locs.add(new BumpLocation(proj,null,0,0,ritm));

   return locs;
}




/********************************************************************************/
/*										*/
/*	Breakpoint commands							*/
/*										*/
/********************************************************************************/

/**
 *	Insert a breakpoint at the given line of the given file.  The class parameter
 *	is optional.  The breakpoint will cause the current thread to suspend unless
 *	suspendvm is true in which case all threads will suspend.
 **/

boolean addLineBreakpoint(String proj,File file,String cls,int line,
				 boolean suspendvm,boolean istrace)
{
   waitForIDE();

   cls = IvyXml.xmlSanitize(cls);

   String q = "LINE='" + line + "'";
   if (file != null) q += " FILE='" + file.getPath() + "'";
   if (cls != null) q += " CLASS='" + cls + "'";
   if (suspendvm) q += " SUSPENDVM='T'";
   if (istrace) q += " TRACE='T'";

   return getStatusReply("ADDLINEBREAKPOINT",proj,q,null,0);
}



/**
 *	Insert a breakpoint for when an exception is thrown.  This can be specialized
 *	by whether the exception is caught or uncaught or both.
 *	@param cls The class name of the exception (e.g. java.lang.NullPointerException)
 **/

boolean addExceptionBreakpoint(String proj,String cls,boolean caught,
					 boolean uncaught,boolean suspendvm)
{
   waitForIDE();

   cls = IvyXml.xmlSanitize(cls);

   String q = "CLASS='" + cls + "'";
   q += " CAUGHT='" + Boolean.toString(caught) + "'";
   q += " UNCAUGHT='" + Boolean.toString(uncaught) + "'";
   if (suspendvm) q += " SUSPENDVM='T'";

   return getStatusReply("ADDEXCEPTIONBREAKPOINT",proj,q,null,0);
}




/**
 *	Remove any breakpoints at the given line of the given file.
 **/

boolean clearLineBreakpoint(String proj,File file,String cls,int line)
{
   waitForIDE();

   cls = IvyXml.xmlSanitize(cls);

   String q = "LINE='" + line + "'";
   if (file != null) q += " FILE='" + file.getPath() + "'";
   if (cls != null) q += " CLASS='" + cls + "'";

   return getStatusReply("CLEARLINEBREAKPOINT",proj,q,null,0);
}



/**
 *	Edit breakpoint properties
 **/

public boolean editBreakpoint(String id,String prop,String ... args)
{
   String q = "ID='" + id + "' PROP='" + prop + "'";
   if (args.length > 0) {
      q += " VALUE='" + args[0] + "'";
    }
   for (int i = 1; i < args.length; i += 2) {
      int j = i/2 + 1;
      q += " PROP" + j + "='" + args[i] + "'";
      if (i+1 < args.length) {
	 q += " VALUE" + j + "='" + args[i+1] + "'";
       }
    }

   return getStatusReply("EDITBREAKPOINT",null,q,null,0);
}



/********************************************************************************/
/*										*/
/*	Callback management							*/
/*										*/
/********************************************************************************/

/**
 *	Add a callback for file events for the given file.
 **/

public void addFileHandler(File file,BumpFileHandler hdlr)
{
   file_handlers.put(hdlr,file);
}



/**
 *	Remove a file callback previously added with addFileHandler.
 **/

public void removeFileHandler(BumpFileHandler hdlr)
{
   file_handlers.remove(hdlr);
}


/**
 *	Add a callback for handling additions/deletions and changes to the set
 *	of projects associated with a file.  If the file argument is null, the
 *	callback is invoked for all problems.
 **/

public void addProblemHandler(File file,BumpProblemHandler hdlr)
{
   problem_set.addProblemHandler(file,hdlr);
}


/**
 *	Remove a previously registered problem handler.
 **/

public void removeProblemHandler(BumpProblemHandler hdlr)
{
   problem_set.removeProblemHandler(hdlr);
}



/**
 *	Add a callback for handling additions/deletions and changes to the set
 *	of breakpoints associated with a file.	If the file argument isn't null, the
 *	callback is invoked for all breakpoints.
 **/

public void addBreakpointHandler(File file,BumpBreakpointHandler hdlr)
{
   break_set.addBreakpointHandler(file,hdlr);
}


/**
 *	Remove a previously registered breakpoint callback.
 **/

public void removeBreakpointHandler(BumpBreakpointHandler hdlr)
{
   break_set.removeBreakpointHandler(hdlr);
}



/**
 *	Add a callback for handling new files, deleted files and changed files
 **/

public void addChangeHandler(BumpChangeHandler hdlr)
{
   change_handlers.put(hdlr,Boolean.TRUE);
}


/**
 *	Remove a previously registered breakpoint callback.
 **/

public void removeChangeHandler(BumpChangeHandler hdlr)
{
   change_handlers.remove(hdlr);
}

/**
 *	Add a callback for handling open editor bubble from eclipse
 **/

public void addOpenEditorBubbleHandler(BumpOpenEditorBubbleHandler hdlr)
{
   open_editor_bubble_handlers.add(hdlr);
}


/**
 *	Remove a previously registered callback for handling open editor bubble from eclipse.
 **/

public void removeOpenEditorBubbleHandler(BumpOpenEditorBubbleHandler hdlr)
{
   open_editor_bubble_handlers.remove(hdlr);
}



/**
 *	Add a progress handler
 **/
public void addProgressHandler(BumpProgressHandler hdlr)
{
   progress_handlers.add(hdlr);
}


/**
 *	Remove previously registered progress handler
 **/
public void removeProgressHandler(BumpProgressHandler hdlr)
{
   progress_handlers.remove(hdlr);
}



/********************************************************************************/
/*										*/
/*	Error repository access 						*/
/*										*/
/********************************************************************************/

/**
 *	Return the set of all active problems (errors/warnings/notices).
 **/

public List<BumpProblem> getAllProblems()
{
   return problem_set.getProblems(null);
}


/**
 *	Return the set of all active problems (errors/warnings/notices) for the given file.
 **/

public List<BumpProblem> getProblems(File f)
{
   return problem_set.getProblems(f);
}



/********************************************************************************/
/*										*/
/*	Breakpoint repository access						*/
/*										*/
/********************************************************************************/

private void loadBreakpoints()
{
   Element xml = getXmlReply("GETALLBREAKPOINTS",null,null,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return;

   Element be = IvyXml.getChild(xml,"BREAKPOINTS");
   break_set.handleUpdate(be);
}



/**
 *	Return the set of all active breakpoints
 **/

public List<BumpBreakpoint> getAllBreakpoints()
{
   return break_set.getBreakpoints(null);
}


/**
 *	Return the set of all active breakpoints for the given file
 **/

public List<BumpBreakpoint> getBreakpoints(File f)
{
   return break_set.getBreakpoints(f);
}



/********************************************************************************/
/*										*/
/*	Run/debug methods							*/
/*										*/
/********************************************************************************/

public BumpBreakModel getBreakModel()
{
   return break_set;
}



public BumpRunModel getRunModel()
{
   return run_manager;
}



Element getRunConfigurations()
{
   Element e = getXmlReply("GETRUNCONFIG",null, null, null, 0);
   return e;
}


Element getNewRunConfiguration(String name,String clone,BumpLaunchConfigType typ)
{
   String q = "";
   if (name != null) q = "NAME='" + name + "'";
   if (typ != null) q += " TYPE='" + typ.getEclipseName() + "'";
   if (clone != null) { 	// must have a new name
      q += " CLONE='" + clone + "'";
    }

   Element e = getXmlReply("NEWRUNCONFIG",null,q,null,0);
   return e;
}

Element editRunConfiguration(String id,String prop,String val)
{
   String q = "LAUNCH='" + id + "' PROP='" + prop + "' VALUE='" + val + "'";
   Element e = getXmlReply("EDITRUNCONFIG",null,q,null,0);
   return e;
}


Element saveRunConfiguration(String id)
{
   String q = "LAUNCH='" + id + "'";
   Element e = getXmlReply("SAVERUNCONFIG",null,q,null,0);
   return e;
}


Element deleteRunConfiguration(String id)
{
   String q = "LAUNCH='" + id + "'";
   Element e = getXmlReply("DELETERUNCONFIG",null,q,null,0);
   return e;
}


Element getThreadStack(BumpThread bt)
{
   if (bt == null) return null;
   if (bt.getLaunch() == null) return null;

   String q = "LAUNCH='" + bt.getLaunch().getId() + "' THREAD='" + bt.getId() + "'";

   return getXmlReply("GETSTACKFRAMES",null,q,null,0);
}



public BumpLaunch startRun(BumpLaunchConfig cfg)
{
   if (cfg == null) return null;

   String q = "NAME='" + cfg.getId() +"' MODE='run'";

   Element xml = getXmlReply("START",null,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Element lnch = IvyXml.getChild(xml,"LAUNCH");

   return run_manager.findLaunch(lnch);
}



public BumpProcess startDebug(BumpLaunchConfig cfg,String id)
{
   if (cfg == null) return null;

   String q = "NAME='" + cfg.getId() +"' MODE='debug'";

   String xtr = run_manager.startDebugArgs(id);
   if (xtr != null) q += " VMARG='" + xtr + "'";

   Element xml = getXmlReply("START",null,q,null,0);

   if (!IvyXml.isElement(xml,"RESULT")) return null;

   Element lnch = IvyXml.getChild(xml,"LAUNCH");

   BumpProcess bp = run_manager.findProcess(lnch);

   if (id != null) run_manager.setProcessName(bp,id);

   return bp;
}



public boolean terminate(BumpLaunch bl)
{
   return debugAction(bl,null,null,null,"TERMINATE");
}


public boolean terminate(BumpProcess bp)
{
   return debugAction(null,bp,null,null,"TERMINATE");
}


public boolean suspend(BumpLaunch bl)
{
   return debugAction(bl,null,null,null,"SUSPEND");
}


public boolean suspend(BumpProcess bp)
{
   return debugAction(null,bp,null,null,"SUSPEND");
}


public boolean suspend(BumpThread bt)
{
   return debugAction(null,null,bt,null,"SUSPEND");
}


public boolean resume(BumpLaunch bl)
{
   return debugAction(bl,null,null,null,"RESUME");
}


public boolean resume(BumpProcess bp)
{
   return debugAction(null,bp,null,null,"RESUME");
}


public boolean resume(BumpThread bt)
{
   return debugAction(null,null,bt,null,"RESUME");
}



public boolean stepInto(BumpThread bt)
{
   return debugAction(null,null,bt,null,"STEP_INTO");
}





/**
 * Handle step into user code command
 **/

public void stepUser(BumpThread bt)
{
   run_manager.stepUser(bt);
}
public boolean stepOver(BumpThread bt)
{
   return debugAction(null,null,bt,null,"STEP_OVER");
}



public boolean stepReturn(BumpThread bt)
{
   return debugAction(null,null,bt,null,"STEP_RETURN");
}



public boolean dropToFrame(BumpThread bt)
{
   return debugAction(null,null,bt,null,"DROP_TO_FRAME");
}



public boolean dropToFrame(BumpStackFrame frm)
{
   return debugAction(null,null,frm.getThread(),frm,"DROP_TO_FRAME");
}



private boolean debugAction(BumpLaunch bl,BumpProcess bp,BumpThread bt,BumpStackFrame frm,String act)
{
   if (bt != null && bp == null) bp = bt.getProcess();
   if (bp != null && bl == null) bl = bp.getLaunch();
   if (bl == null) return false;

   String q = "LAUNCH='" + bl.getId() + "'";
   if (bp != null) q += " PROCESS='" + bp.getId() + "'";
   if (bt != null) q += " THREAD='" + bt.getId() + "'";
   if (frm != null) q += " FRAME='" + frm.getId() + "'";
   q += " ACTION='" + act + "'";

   return getStatusReply("DEBUGACTION",null,q,null,0);
}



/********************************************************************************/
/*										*/
/*	Value access methods							*/
/*										*/
/********************************************************************************/

Element getVariableValue(BumpStackFrame frm,String var,int lvls)
{
   String q = "FRAME='" + frm.getId() + "'";
   if (frm.getThread() != null) q += " THREAD='" + frm.getThread().getId() + "'";
   if (lvls > 0) q += " DEPTH='" + lvls + "'";
   String data = "<VAR>" + IvyXml.xmlSanitize(var) + "</VAR>";

   return getXmlReply("VARVAL",null,q,data,0);
}



public String getVariableDetail(BumpStackFrame frm,String var)
{
   String q = "FRAME='" + frm.getId() + "'";
   if (frm.getThread() != null) q += " THREAD='" + frm.getThread().getId() + "'";
   String data = "<VAR>" + IvyXml.xmlSanitize(var) + "</VAR>";

   Element xml = getXmlReply("VARDETAIL",null,q,data,0);
   if (!IvyXml.isElement(xml,"RESULT")) return null;

   // String rslt = IvyXml.getTextElement(xml,"DETAIL");

   Element val = IvyXml.getChild(xml,"VALUE");
   if (val == null) return null;
   String rslt = IvyXml.getTextElement(val,"DESCRIPTION");

   return rslt;
}



boolean evaluateExpression(BumpStackFrame frm,String expr,boolean impl,boolean brk,
				     BumpEvaluationHandler hdlr)
{
   String proj = frm.getThread().getLaunch().getConfiguration().getProject();

   String rid = "EX" + (collect_id++);
   String q = "THREAD='" + frm.getThread().getId() + "' FRAME='" + frm.getId() + "'";
   if (impl) q += " IMPLICIT='true'";
   q += " BREAK='" + Boolean.toString(brk) + "'";
   q += " REPLYID='" + rid + "'";

   String data = "<EXPR>" + IvyXml.xmlSanitize(expr) + "</EXPR>";

   eval_handlers.put(rid,new EvalData(frm,hdlr));
   boolean sts = getStatusReply("EVALUATE",proj,q,data,0);

   if (!sts) eval_handlers.remove(rid);

   return sts;
}



private class EvalData {

   private BumpStackFrame for_frame;
   private BumpEvaluationHandler for_handler;

   EvalData(BumpStackFrame frm,BumpEvaluationHandler hdlr) {
      for_frame = frm;
      for_handler = hdlr;
    }

   void handleResult(Element xml) {
      run_manager.handleEvaluationResult(for_frame,xml,for_handler);
    }

}	// end of class EvalData




/********************************************************************************/
/*										*/
/*	New methods								*/
/*										*/
/********************************************************************************/

/**
 *	Create a new class with the given contents in the given project.
 **/

public File createNewClass(String proj,String name,boolean force,String cnts)
{
   waitForIDE();

   String flds = "NAME='" + name + "' FORCE='" + force + "'";
   String data = "<CONTENTS>";
   if (cnts != null) {
      // TODO: if cnts contains ]]> we have to split it
      data += "<![CDATA[" + cnts + "]]>";
    }
   data += "</CONTENTS>";

   Element xml = getXmlReply("CREATECLASS",proj,flds,data,0);
   if (!IvyXml.isElement(xml,"RESULT")) return null;
   Element felt = IvyXml.getChild(xml,"FILE");
   if (felt == null) return null;
   String path = IvyXml.getTextElement(felt,"PATH");
   if (path == null) return null;

   return new File(path);
}




/**
 *	Create a new package with the given name
 **/

public File createNewPackage(String proj,String pkg,boolean force)
{
   waitForIDE();

   String flds = "NAME='" + pkg + "' FORCE='" + force + "'";
   Element xml = getXmlReply("CREATEPACKAGE",proj,flds,null,0);
   if (!IvyXml.isElement(xml,"RESULT")) return null;
   Element pe = IvyXml.getChild(xml,"PACKAGE");
   if (pe == null) return null;

   String fnm = IvyXml.getTextElement(pe,"PATH");
   if (fnm == null) return null;

   return new File(fnm);
}




/********************************************************************************/
/*										*/
/*	Host checking								*/
/*										*/
/********************************************************************************/

private boolean isIdeOnSameHost()
{
   Element xml = getXmlReply("GETHOST",null,null,null,5000);
   if (xml == null) return true;

   String h1 = null;
   String h3 = null;
   try {
      InetAddress lh = InetAddress.getLocalHost();
      h1 = lh.getHostAddress();
      h3 = lh.getCanonicalHostName();
    }
   catch (IOException e) { }

   if (h1 != null && h1.equals(IvyXml.getAttrString(xml,"ADDR"))) return true;
   if (h3 != null && h3.equals(IvyXml.getAttrString(xml,"CNAME"))) return true;

   return false;
}




/********************************************************************************/
/*										*/
/*	Higher level messages							*/
/*										*/
/********************************************************************************/

private boolean tryPing()
{
   String r = getStringReply("PING",null,null,null,5000);

   return r != null;
}




/********************************************************************************/
/*										*/
/*	Send message to eclipse 						*/
/*										*/
/********************************************************************************/

private void sendMessage(String cmd)
{
   sendMessage(cmd,null,null,null,null,MINT_MSG_NO_REPLY);
}


private void sendMessage(String cmd,String proj,String flds,String cnts)
{
   sendMessage(cmd,proj,flds,cnts,null,MINT_MSG_NO_REPLY);
}



private String getStringReply(String cmd,String proj,String flds,String cnts,long delay)
{
   MintDefaultReply mdr = new ReplyHandler();

   sendMessage(cmd,proj,flds,cnts,mdr,MINT_MSG_FIRST_NON_NULL);

   if (delay <= 0) delay = MAX_DELAY;

   return mdr.waitForString(delay);
}



private Element getXmlReply(String cmd,String proj,String flds,String cnts,long delay)
{
   MintDefaultReply mdr = new ReplyHandler();
   sendMessage(cmd,proj,flds,cnts,mdr,MINT_MSG_FIRST_NON_NULL);

   if (delay <= 0) delay = MAX_DELAY;

   Element r = mdr.waitForXml(delay);

   return r;
}



private boolean getStatusReply(String cmd,String proj,String flds,String cnts,long delay)
{
   Element e = getXmlReply(cmd,proj,flds,cnts,delay);
   if (e == null) return false;
   if (IvyXml.isElement(e,"ERROR")) {
      String txt = IvyXml.getText(e);
      if (txt == null) txt = IvyXml.getTextElement(e,"MESSAGE");
      BoardLog.logE("BUMP","Problem with command: " + txt);
      return false;
    }
   else if (IvyXml.isElement(e,"RESULT")) {
      return true;
    }

   return false;
}



private void sendMessage(String cmd,String proj,String flds,String cnts,MintReply rply,int flags)
{
   String xml = "<BUBBLES DO='" + cmd + "'";
   xml += " BID='" + source_id + "'";
   if (proj != null && proj.length() > 0) xml += " PROJECT='" + proj + "'";
   if (flds != null) xml += " " + flds;
   xml += ">";
   if (cnts != null) xml += cnts;
   xml += "</BUBBLES>";

   BoardLog.logD("BUMP","SEND: " + xml);

   mint_control.send(xml,rply,flags);
}




/********************************************************************************/
/*										*/
/*	Handle reply error checking						*/
/*										*/
/********************************************************************************/

private static class ReplyHandler extends MintDefaultReply {

   @Override public void handleReply(MintMessage msg,MintMessage rply) {
      super.handleReply(msg,rply);

      if (rply != null) {
	 Element xml = rply.getXml();
	 if (IvyXml.isElement(xml,"ERROR")) {
	    String stk = IvyXml.getTextElement(xml,"STACK");
	    if (stk != null) {
	       String txt = IvyXml.getTextElement(xml,"MESSAGE");
	       String id = IvyXml.getTextElement(xml,"EXCEPTION");
	       BoardLog.logE("BEDROCK",txt,id,stk);
	     }
	  }
       }
    }

}	// end of inner class ReplyHandler




/********************************************************************************/
/*										*/
/*	Property methods							*/
/*										*/
/********************************************************************************/

private static void loadProperties()
{
   BoardSetup bs = BoardSetup.getSetup();
   bs.doSetup();

   board_properties = BoardProperties.getProperties("System");
}




/********************************************************************************/
/*										*/
/*	Methods to send elision data to editors via callbacks			*/
/*										*/
/********************************************************************************/

private void handleElision(String bid,String fnm,int id,Element data)
{
   File f = new File(fnm);
   for (Map.Entry<BumpFileHandler,File> ent : file_handlers.entrySet()) {
      BumpFileHandler hdlr = ent.getKey();
      File hf = ent.getValue();
      if (hf == null || f.equals(hf)) {
	 hdlr.handleElisionData(f,id,data);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Handle remote edits							*/
/*										*/
/********************************************************************************/

private void handleEdit(String bid,String fnm,int len,int off,boolean complete,String txt)
{
   if (bid != null && !bid.equals(source_id)) return;
   if (fnm == null) return;

   int xlen = len;
   if (complete) xlen = -len;

   File f = new File(fnm);
   for (Map.Entry<BumpFileHandler,File> ent : file_handlers.entrySet()) {
      BumpFileHandler hdlr = ent.getKey();
      File hf = ent.getValue();
      if (hf == null || f.equals(hf)) {
	 hdlr.handleRemoteEdit(f,xlen,off,txt);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle resource changes					*/
/*										*/
/********************************************************************************/

private void handleResourceChange(Element de)
{
   String k = IvyXml.getAttrString(de,"KIND");
   Element re = IvyXml.getChild(de,"RESOURCE");
   String rtyp = IvyXml.getAttrString(re,"TYPE");
   if (rtyp != null && rtyp.equals("FILE")) {
      String fp = IvyXml.getAttrString(re,"LOCATION");
      String proj = IvyXml.getAttrString(re,"PROJECT");
      if (k.equals("ADDED") || k.equals("ADDED_PHANTOM")) {
	 for (BumpChangeHandler bch : change_handlers.keySet()) {
	    bch.handleFileAdded(proj,fp);
	  }
       }
      else if (k.equals("REMOVED") || k.equals("REMOVED_PHANTOM")) {
	 for (BumpChangeHandler bch : change_handlers.keySet()) {
	    bch.handleFileRemoved(proj,fp);
	  }
       }
      else {
	 for (BumpChangeHandler bch : change_handlers.keySet()) {
	    bch.handleFileChanged(proj,fp);
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to handle preferences						*/
/*										*/
/********************************************************************************/

private void grabPreferences()
{
   if (!option_map.isEmpty()) return;

   Element pe = getXmlReply("PREFERENCES",null,null,null,0);
   Element spe = IvyXml.getChild(pe,"PREFERENCES");
   if (spe != null) pe = spe;
   for (Element pr : IvyXml.children(pe,"PREF")) {
      String nm = IvyXml.getAttrString(pr,"NAME");
      String vl = IvyXml.getAttrString(pr,"VALUE");
      if (IvyXml.getAttrBool(pr,"OPTS")) {
	 option_map.put(nm,vl);
       }
    }

   // pe = getXmlReply("GETPROXY",null,"HOST='http://conifer.cs.brown.edu'",null,0);
   // pe = getXmlReply("GETPROXY",null,"HOST='http://www.cs.brown.edu/people/spr/bubbles'",null,0);
}



/**
 *	Return the string value of the IDE option with the given name.
 **/

public String getOption(String nm)		{ return option_map.get(nm); }


/**
 *	Return the boolean value of the IDE option with the given name
 **/

public boolean getOptionBool(String nm)
{
   String v = option_map.get(nm);
   if (v == null || v.length() == 0) return false;
   if ("yYtT1".indexOf(v.charAt(0)) >= 0) return true;
   return false;
}


/**
 *	Return the integer value of the IDE option with the given name.  If the option
 *	is not defined, return 0.
 **/

public int getOptionInt(String nm)		{ return getOptionInt(nm,0); }


/**
 *	Return the integer value of the IDE option with the given name.  If the option
 *	is not defined, return the given default value.
 **/

public int getOptionInt(String nm,int dflt)
{
   String v = option_map.get(nm);
   if (v == null || v.length() == 0) return dflt;
   try {
      return Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }
   return dflt;
}




/********************************************************************************/
/*										*/
/*	Methods to handle initial build 					*/
/*										*/
/********************************************************************************/

private void buildAllProjects(boolean clean,boolean full,boolean refresh)
{
   Element e = getXmlReply("PROJECTS",null,null,null,0);
   String q = "REFRESH='" + Boolean.toString(refresh) + "'";
   q += " CLEAN='" + Boolean.toString(clean) + "'";
   q += " FULL='" + Boolean.toString(full) + "'";

   problem_set.clearProblems();

   for (Element p : IvyXml.children(e,"PROJECT")) {
      String pnm = IvyXml.getAttrString(p,"NAME");
      if (!IvyXml.getAttrBool(p,"OPEN")) {
	 getStringReply("OPENPROJECT",pnm,null,null,0);
       }
      Element probs = getXmlReply("BUILDPROJECT",pnm,q,null,0);
      problem_set.handleErrors(pnm,null,0,probs);
    }
}




/********************************************************************************/
/*										*/
/*	Eclipse message handling						*/
/*										*/
/********************************************************************************/

private class EclipseHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element e = msg.getXml();

      BoardLog.logD("BUMP","Eclipse command " + cmd);

      try {
	 if (cmd == null) {
	    BoardLog.logE("BUMP","Bad eclipse message:" + msg.getText());
	  }
	 else if (doing_exit) {
	    msg.replyTo();
	  }
	 else if (cmd.equals("ELISION")) {
	    handleElision(IvyXml.getAttrString(e,"BID"),
			     IvyXml.getAttrString(e,"FILE"),
			     IvyXml.getAttrInt(e,"ID"),
			     IvyXml.getChild(e,"ELISION"));
	  }
	 else if (cmd.equals("EDITERROR")) {
	    problem_set.handleErrors(IvyXml.getAttrString(e,"PROJECT"),
					new File(IvyXml.getAttrString(e,"FILE")),
					IvyXml.getAttrInt(e,"ID"),
					IvyXml.getChild(e,"MESSAGES"));
	  }
	 else if (cmd.equals("FILEERROR")) {
	    problem_set.handleErrors(IvyXml.getAttrString(e,"PROJECT"),
					new File(IvyXml.getAttrString(e,"FILE")),
					-1,
					IvyXml.getChild(e,"MESSAGES"));
	  }
	 else if (cmd.equals("EDIT")) {
	    BoardLog.logD("BUMP","REMOTE EDIT: " + IvyXml.convertXmlToString(e));
	    String txt = IvyXml.getText(e);
	    boolean complete = IvyXml.getAttrBool(e,"COMPLETE");
	    if (complete) {
	       byte [] data = IvyXml.getBytesElement(e,"CONTENTS");
	       if (data != null) txt = new String(data);
	     }
	    handleEdit(IvyXml.getAttrString(e,"BID"),
			  IvyXml.getAttrString(e,"FILE"),
			  IvyXml.getAttrInt(e,"LENGTH"),
			  IvyXml.getAttrInt(e,"OFFSET"),
			  complete,
			  txt);
	  }
	 else if (cmd.equals("BREAKEVENT")) {
	    BoardLog.logD("BUMP","BREAK EVENT: " + IvyXml.convertXmlToString(e));
	    break_set.handleUpdate(IvyXml.getChild(e,"BREAKPOINTS"));
	  }
	 else if (cmd.equals("LAUNCHCONFIGEVENT")) {
	    BoardLog.logD("BUMP","LAUNCH EVENT: " + IvyXml.convertXmlToString(e));
	    run_manager.handleLaunchEvent(IvyXml.getChild(e,"LAUNCH"));
	  }
	 else if (cmd.equals("RUNEVENT")) {
	    long when = IvyXml.getAttrLong(e,"TIME");
	    for (Element re : IvyXml.children(e,"RUNEVENT")) {
	       run_manager.handleRunEvent(re,when);
	     }
	    msg.replyTo();
	  }
	 else if (cmd.equals("NAMES") && name_collects != null) {
	    // BoardLog.logD("BUMP","NAMES: " + IvyXml.convertXmlToString(e));
	    String nid = IvyXml.getAttrString(e,"NID");
	    NameCollector nc = name_collects.get(nid);
	    if (nc != null) {
	       nc.addNames(e);
	       BoardLog.logD("BUMP","NAMES: " + nc.getSize());
	    }
	    msg.replyTo();	// wait until add to ensure end doesn't come before we are all processed
	  }
	 else if (cmd.equals("ENDNAMES")) {
	    msg.replyTo();
	    // BoardLog.logD("BUMP","ENDNAMES: " + IvyXml.convertXmlToString(e));
	    String nid = IvyXml.getAttrString(e,"NID");
	    NameCollector nc = name_collects.remove(nid);
	    if (nc != null) nc.noteDone();
	  }
	 else if (cmd.equals("PING")) {
	    msg.replyTo("<PONG/>");
	  }
	 else if (cmd.equals("PROGRESS")) {
	    long sid = IvyXml.getAttrLong(e,"S");
	    String id = IvyXml.getAttrString(e,"ID");
	    String kind = IvyXml.getAttrString(e,"KIND");
	    String task = IvyXml.getAttrString(e,"TASK");
	    String subtask = IvyXml.getAttrString(e,"SUBTASK","");
	    double work = IvyXml.getAttrDouble(e,"WORK",0);
	    BoardLog.logD("BUMP","Progress " + sid + " " + id + " " + kind + " " + task + " " + subtask + " " + work);
	    for (BumpProgressHandler hdlr : progress_handlers) {
	       hdlr.handleProgress(sid,id,kind,task,subtask,work);
	     }
	    msg.replyTo();
	  }
	 else if (cmd.equals("RESOURCE")) {
	    BoardLog.logD("BUMP","RESOURCE: " + IvyXml.convertXmlToString(e));
	    for (Element re : IvyXml.children(e,"DELTA")) {
	       handleResourceChange(re);
	     }
	  }
	 else if (cmd.equals("CONSOLE"))   {
	    // BoardLog.logD("BUMP","CONSOLE: " + IvyXml.convertXmlToString(e));
	    run_manager.handleConsoleEvent(e);
	    msg.replyTo();
	  }
	 else if (cmd.equals("OPENEDITOR")) {
	    String projname = IvyXml.getAttrString(e,"PROJECT");
	    String filepath = IvyXml.getAttrString(e,"RESOURCEPATH");
	    String type = IvyXml.getAttrString(e,"RESOURCETYPE");

	    for(BumpOpenEditorBubbleHandler handler : open_editor_bubble_handlers)
	       handler.handleOpenEditorBubble(projname, filepath, type);
	  }
	 else if (cmd.equals("EVALUATION")) {
	    BoardLog.logD("BUMP","EVALUATION RESULT: " + IvyXml.convertXmlToString(e));
	    String bid = IvyXml.getAttrString(e,"BID");
	    String id = IvyXml.getAttrString(e,"ID");
	    if ((bid == null || bid.equals(source_id)) && id != null) {
	       EvalData ed = eval_handlers.remove(id);
	       if (ed != null) {
		  ed.handleResult(e);
		}
	     }
	    msg.replyTo();
	  }
	 else {
	    BoardLog.logD("BUMP","Received " + cmd + " FROM ECLIPSE");
	    BoardLog.logD("BUMP","Message: " + IvyXml.convertXmlToString(e));
	  }
       }
      catch (Throwable t) {
	 BoardLog.logE("BUMP","Problem processing eclipse message " + cmd,t);
       }
    }

}	// end of inner class EclipseHandler



/********************************************************************************/
/*										*/
/*	Name collection methods 						*/
/*										*/
/********************************************************************************/

private static class NameCollector {

   private Collection<BumpLocation> result_names;
   private boolean is_done;

   NameCollector() {
      result_names = new ArrayList<BumpLocation>();
      is_done = false;
    }

   synchronized void addNames(Element xml) {
      int ctr = 0;
      for (Element fe : IvyXml.children(xml,"FILE")) {
	 String path = IvyXml.getTextElement(fe,"PATH");
	 for (Element itm : IvyXml.children(fe,"ITEM")) {
	    int offset = IvyXml.getAttrInt(itm,"STARTOFFSET");
	    int length = IvyXml.getAttrInt(itm,"LENGTH");
	    String pnm = IvyXml.getAttrString(itm,"PROJECT");
	    BumpLocation bl = new BumpLocation(pnm,path,offset,length,itm);
	    result_names.add(bl);
	    ++ctr;
	  }
       }
      if (ctr == 0) {
	 for (Element itm : IvyXml.children(xml,"ITEM")) {
	    String pnm = IvyXml.getAttrString(itm,"PROJECT");
	    String pth = IvyXml.getAttrString(itm,"PATH");
	    BumpLocation bl = new BumpLocation(pnm,pth,0,0,itm);
	    result_names.add(bl);
	  }
       }
    }

   int getSize()				{ return result_names.size(); }

   synchronized void noteDone() {
      is_done = true;
      notifyAll();
    }

   synchronized Collection<BumpLocation> getNames() {
      while (!is_done) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }

      return result_names;
    }

}	// end of inner class NameCollector




/********************************************************************************/
/*										*/
/*	Exit handling								*/
/*										*/
/********************************************************************************/

private class CloseBedrock extends Thread {

   @Override public void run() {
      stopIDE();
    }

}	// end of inner class CloseBedrock



}	// end of class BumpClient




/* end of BumpClient.java */
