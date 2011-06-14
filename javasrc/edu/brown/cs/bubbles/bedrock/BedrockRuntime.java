/********************************************************************************/
/*										*/
/*		BedrockRuntime.java						*/
/*										*/
/*	Runtime manager for Bubbles - Eclipse interface 			*/
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


/* SVN: $Id$ */




package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.*;
import org.eclipse.jdt.debug.eval.*;

import java.io.IOException;
import java.util.*;



class BedrockRuntime implements BedrockConstants, IDebugEventSetListener,
	ILaunchConfigurationListener, IJavaHotCodeReplaceListener
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin our_plugin;
private DebugPlugin debug_plugin;

private ConsoleThread		console_thread;
private Map<Integer,ConsoleData> console_map;
private Set<ILaunchConfiguration> working_configs;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockRuntime(BedrockPlugin bp)
{
   our_plugin = bp;
   debug_plugin = DebugPlugin.getDefault();
   console_thread = null;
   console_map = new LinkedHashMap<Integer,ConsoleData>();
   working_configs = new HashSet<ILaunchConfiguration>();
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

void start()
{
   debug_plugin.addDebugEventListener(this);
   JDIDebugModel.addHotCodeReplaceListener(this);

   ILaunchManager lm = debug_plugin.getLaunchManager();
   lm.addLaunchConfigurationListener(this);
}




/********************************************************************************/
/*										*/
/*	Get the run configurations for a project				*/
/*										*/
/********************************************************************************/

void getRunConfigurations(IvyXmlWriter xw) throws BedrockException
{
   ILaunchManager lm = debug_plugin.getLaunchManager();

   try {
      ILaunchConfiguration [] cnfg = lm.getLaunchConfigurations();

      for (int i = 0; i < cnfg.length; ++i) {
	 BedrockUtil.outputLaunch(cnfg[i],xw);
       }

      IProcess [] prcs = lm.getProcesses();
      for (int i = 0; i < prcs.length; ++i) {
	 BedrockUtil.outputProcess(prcs[i],xw,true);
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem getting configurations",e);
    }
   catch (Throwable t) {
      // eclispe sometimes fails for no reason here
    }
}




void getNewRunConfiguration(String proj,String name,String clone,String typ,IvyXmlWriter xw)
	throws BedrockException
{
   ILaunchConfiguration cln = findLaunchConfig(clone);
   ILaunchConfigurationWorkingCopy config = null;

   if (typ == null) typ = "Java Application";

   try {
      if (cln != null) {
	 if (name == null) {
	    if (cln.isWorkingCopy()) config = (ILaunchConfigurationWorkingCopy) cln;
	    else config = cln.getWorkingCopy();
	  }
	 else config = cln.copy(name);
       }
      else {
	 if (name == null) {
	    Random r = new Random();
	    name = "NewLaunch_" + r.nextInt(1024);
	  }
	 String ltid = null;
	 ILaunchManager lm = debug_plugin.getLaunchManager();
	 ILaunchConfigurationType [] typs = lm.getLaunchConfigurationTypes();
	 for (ILaunchConfigurationType lct : typs) {
	    if (lct.getName().equals(typ)) {
	       ltid = lct.getIdentifier();
	       break;
	     }
	  }
	 ILaunchConfigurationType lct = lm.getLaunchConfigurationType(ltid);
	 IProject ip = our_plugin.getProjectManager().findProject(proj);
	 config = lct.newInstance(ip,name);
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem creating launch config working copy",e);
    }

   if (config != null) {
      config.setAttribute(BEDROCK_LAUNCH_ID_PROP,"L_" + System.identityHashCode(config));
      config.removeAttribute(BEDROCK_LAUNCH_ORIGID_PROP);
      working_configs.add(config);
      BedrockUtil.outputLaunch(config,xw);
    }
}




void editRunConfiguration(String lnch,String prop,String val,IvyXmlWriter xw)
	throws BedrockException
{
   if (lnch == null) return;
   ILaunchConfigurationWorkingCopy wc = findWorkingLaunchConfig(lnch);

   if (prop.endsWith("_MAP") && val != null) {
      StringTokenizer tok = new StringTokenizer(val," {},");
      HashMap<String,String> map = new HashMap<String,String>();
      while (tok.hasMoreTokens()) {
	 String s = tok.nextToken();
	 int idx = s.indexOf("=");
	 if (idx < 0) continue;
	 String nm = s.substring(0,idx);
	 String vl = s.substring(idx+1);
	 map.put(nm,vl);
       }
      wc.setAttribute(prop,map);
    }
   else if (prop.equals("NAME")) {
      wc.rename(val);
    }
   else {
      wc.setAttribute(prop,val);
    }

   BedrockUtil.outputLaunch(wc,xw);
}





void saveRunConfiguration(String lnch,IvyXmlWriter xw) throws BedrockException
{
   if (lnch == null) return;
   ILaunchConfigurationWorkingCopy wc = findWorkingLaunchConfig(lnch);
   if (wc == null) return;

   ILaunchConfiguration cln = null;
   try {
      String xid = BedrockUtil.getId(wc) + "X";
      xid = wc.getAttribute(BEDROCK_LAUNCH_ORIGID_PROP,xid);
      wc.setAttribute(BEDROCK_LAUNCH_ID_PROP,xid);
      cln = wc.doSave();
      working_configs.remove(wc);
    }
   catch (CoreException e) {
      throw new BedrockException("Problem saving launch configuration",e);
    }

   BedrockUtil.outputLaunch(cln,xw);
}



void deleteRunConfiguration(String lnch,IvyXmlWriter xw) throws BedrockException
{
   if (lnch == null) return;
   ILaunchConfiguration lc = findLaunchConfig(lnch);
   if (lc == null) return;
   try {
      lc.delete();
    }
   catch (CoreException e) {
      throw new BedrockException("Problem deleting launch configuration",e);
    }
}




private ILaunchConfigurationWorkingCopy findWorkingLaunchConfig(String id) throws BedrockException
{
   ILaunchConfiguration cln = findLaunchConfig(id);
   if (cln == null) return null;

   ILaunchConfigurationWorkingCopy wc = null;
   if (cln.isWorkingCopy()) wc = (ILaunchConfigurationWorkingCopy) cln;
   else {
      try {
	 wc = cln.getWorkingCopy();
	 if (!wc.hasAttribute(BEDROCK_LAUNCH_ORIGID_PROP)) {
	    String xid = BedrockUtil.getId(cln);
	    wc.setAttribute(BEDROCK_LAUNCH_ORIGID_PROP,xid);
	  }
	 String nid = "L_" + System.identityHashCode(wc);
	 wc.setAttribute(BEDROCK_LAUNCH_ID_PROP,nid);
       }
      catch (CoreException e) {
	 throw new BedrockException("Problem creating working copy",e);
       }
      working_configs.add(wc);
    }

   return wc;
}



private ILaunchConfiguration findLaunchConfig(String id) throws BedrockException
{
   if (id == null) return null;

   try {
      ILaunchManager lm = debug_plugin.getLaunchManager();
      for (ILaunchConfiguration cfg : lm.getLaunchConfigurations()) {
	 if (matchLaunchConfiguration(id,cfg)) return cfg;
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Problem looking up launch configuration " + id,e);
    }

   for (ILaunchConfiguration cfg : working_configs) {
      if (matchLaunchConfiguration(id,cfg)) return cfg;
    }

   throw new BedrockException("Unknown launch configuration " + id);
}




/********************************************************************************/
/*										*/
/*	Handle running a launch configuration					*/
/*										*/
/********************************************************************************/

void runProject(String cfg,String mode,boolean build,boolean reg,String vmarg,String id,IvyXmlWriter xw)
	throws BedrockException
{
   try {
      ILaunchManager lm = debug_plugin.getLaunchManager();
      ILaunchConfiguration [] cnfg = lm.getLaunchConfigurations();

      for (int i = 0; i < cnfg.length; ++i) {
	 if (matchLaunchConfiguration(cfg,cnfg[i])) {
	    ILaunchConfiguration cnf = cnfg[i];
	    if (vmarg != null) {
	       ILaunchConfigurationWorkingCopy ccnf = cnf.getWorkingCopy();
	       String vmatt = "org.eclipse.jdt.launching.VM_ARGUMENTS";
	       String ja = ccnf.getAttribute(vmatt,(String) null);
	       if (ja == null || ja.length() == 0) ja = vmarg;
	       else ja = ja + " " + vmarg;
	       ccnf.setAttribute(vmatt,ja);
	       cnf = ccnf;
	     }
	    ILaunch lnch = cnf.launch(mode,null,build,reg);
	    if (lnch == null) return;
	    xw.begin("LAUNCH");
	    xw.field("MODE",lnch.getLaunchMode());
	    xw.field("TAG",lnch.toString());
	    xw.field("ID",lnch.hashCode());
	    IDebugTarget tgt = lnch.getDebugTarget();
	    if (tgt != null) {				// will be null if doing a run rather than debug
	       xw.field("TARGET",tgt.hashCode());
	       xw.field("TARGETTAG",tgt.toString());
	       xw.field("NAME",tgt.getName());
	       IProcess ip = tgt.getProcess();
	       if (ip != null) {
		  xw.field("PROCESSTAG",ip.getLabel());
		  xw.field("PROCESS",ip.hashCode());
		  setupConsole(ip);
		}
	     }
	    xw.end("LAUNCH");
	    return;
	  }
       }
    }
   catch (CoreException e) {
      throw new BedrockException("Launch failed: " + e);
    }

   throw new BedrockException("Launch configuration not found");
}



/********************************************************************************/
/*										*/
/*	Handle debug actions							*/
/*										*/
/********************************************************************************/

void debugAction(String lname,String gname,String pname,String tname,String fname,
		    BedrockDebugAction act,
		    IvyXmlWriter xw) throws BedrockException
{
   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      if (!matchLaunch(lname,launch)) continue;
      if (gname == null && pname == null && tname == null) {
	 if (doAction(launch,act)) {
	    xw.textElement("LAUNCH",act.toString());
	    continue;
	  }
       }
      try {
	 for (IDebugTarget dt : launch.getDebugTargets()) {
	    if (!matchDebugTarget(gname,dt)) continue;
	    if (pname != null && !matchProcess(pname,dt.getProcess())) continue;
	    if (tname == null) {
	       if (doAction(dt,act)) {
		  xw.textElement("TARGET",act.toString());
		  continue;
		}
	     }
	    for (IThread th : dt.getThreads()) {
	       if (!matchThread(tname,th)) continue;
	       if (!th.isSuspended() && act != BedrockDebugAction.SUSPEND) continue;
	       doAction(th,fname,act);
	       xw.textElement("THREAD", act.toString());
	     }
	  }
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting launch information: " + e);
       }
    }
}



private boolean doAction(ILaunch il,BedrockDebugAction act) throws BedrockException
{
   try {
      switch (act) {
	 case NONE :
	    break;
	 case TERMINATE :
	    if (il.canTerminate()) il.terminate();
	    else return false;
	    break;
	 default :
	    return false;
       }
    }
   catch (DebugException e) {
      throw new BedrockException("Problem setting launch status",e);
    }

   return true;
}



private boolean doAction(IDebugTarget dt,BedrockDebugAction act) throws BedrockException
{
   try {
      switch (act) {
	 case NONE :
	    break;
	 case TERMINATE :
	    if (dt.canTerminate()) dt.terminate();
	    else return false;
	    break;
	 case SUSPEND :
	    if (dt.canSuspend()) dt.suspend();
	    else return false;
	    break;
	 case RESUME :
	    if (dt.canResume()) dt.resume();
	    else return false;
	    break;
	 default :
	    return false;
       }
    }
   catch (DebugException e) {
      throw new BedrockException("Problem setting debug target status",e);
    }

   return true;
}


private boolean doAction(IThread thrd,String fname,BedrockDebugAction act) throws BedrockException
{
   try {
      switch (act) {
	 case NONE :
	    return false;
	 case TERMINATE :
	    if (thrd.canTerminate()) thrd.terminate();
	    else return false;
	    break;
	 case RESUME :
	    if (thrd.canResume()) thrd.resume();
	    else return false;
	    break;
	 case SUSPEND :
	    if (thrd.canSuspend()) thrd.suspend();
	    else return false;
	    break;
	 case STEP_INTO :
	    if (thrd.canStepInto()) thrd.stepInto();
	    else return false;
	    break;
	 case STEP_OVER :
	    if (thrd.canStepOver()) thrd.stepOver();
	    else return false;
	    break;
	 case DROP_TO_FRAME :
	    IStackFrame topfrm = thrd.getTopStackFrame();
	    if (fname != null) {
	       for (IStackFrame frame: thrd.getStackFrames()) {
		  if (matchFrame(fname,frame)) topfrm = frame;
		}
	     }
	    if (topfrm instanceof IDropToFrame) {
	       IDropToFrame idtf = (IDropToFrame) topfrm;
	       if (idtf.canDropToFrame()) idtf.dropToFrame();
	       else return false;
	     }
	    else {
	       BedrockPlugin.log("No support for drop to frame " + thrd);
	       return false;
	     }
	    break;
	 case STEP_RETURN :
	    if (thrd.canStepReturn()) thrd.stepReturn();
	    else return false;
	    break;
       }
    }
   catch (DebugException e) {
      BedrockPlugin.log(BedrockLogLevel.INFO,"Problem with debug action",e);
      throw new BedrockException("Problem setting thread status: + e",e);
    }

   return true;
}




/********************************************************************************/
/*										*/
/*	Stack access methods							*/
/*										*/
/********************************************************************************/

void getStackFrames(String lname,String tname,int count,int vdepth,IvyXmlWriter xw) throws BedrockException
{
   ILaunch[] launches = debug_plugin.getLaunchManager().getLaunches();
   xw.begin("STACKFRAMES");

   try {
      for (ILaunch launch: launches) {
	 if (!matchLaunch(lname,launch)) continue;
	 IDebugTarget dt = launch.getDebugTarget();
	 if (tname != null) {
	    boolean fnd = false;
	    for (IThread th : dt.getThreads()) {
	       if (matchThread(tname,th)) {
		  fnd = true;
		  break;
		}
	     }
	    if (!fnd) continue;
	  }
	 for (IThread th : dt.getThreads()) {
	    if (matchThread(tname,th)) {
	       dumpFrames(th,count,vdepth,xw);
	     }
	  }
       }
    }
   catch (DebugException e) {
      throw new BedrockException("Problem getting stack frames: " + e,e);
    }

   xw.end("STACKFRAMES");
}




private void dumpFrames(IThread thread,int count,int vdepth,IvyXmlWriter xw) throws DebugException
{
   xw.begin("THREAD");
   xw.field("NAME",thread.getName());
   xw.field("ID",thread.hashCode());
   xw.field("TAG",thread.toString());

   int ctr = 0;
   for (IStackFrame frame: thread.getStackFrames()) {
      if (frame == null) continue;
      IJavaStackFrame jsf = (IJavaStackFrame) frame;

      BedrockUtil.outputStackFrame(jsf,ctr,vdepth,xw);
      if (count > 0 && ctr > count) break;
      ++ctr;
    }

   xw.end("THREAD");
}




/********************************************************************************/
/*										*/
/*	Methods to access variables						*/
/*										*/
/********************************************************************************/

void getVariableValue(String tname,String frid,String vname,int lvls,IvyXmlWriter xw)
		throws BedrockException
{
   IThread thrd = null;
   IStackFrame sfrm = null;
   IVariable var = null;

   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 for (IThread thread : launch.getDebugTarget().getThreads()) {
	    if (matchThread(tname,thread)) {
	       if (thread.isSuspended()) {
		  thrd = thread;
		  break;
		}
	       else if (tname != null) {
		  throw new BedrockException("Thread " + tname + " not suspended");
		}
	     }
	  }

	 if (thrd == null) continue;		// not in this launch

	 for (IStackFrame frame: thrd.getStackFrames()) {
	    if (matchFrame(frid,frame)) sfrm = frame;
	  }

	 if (sfrm == null) throw new BedrockException("Stack frame " + frid + " doesn't exist");

	 StringTokenizer tok = new StringTokenizer(vname,"?");
	 if (!tok.hasMoreTokens()) throw new BedrockException("No variable specified");

	 String vhead = tok.nextToken();

	 for (IVariable variable : sfrm.getVariables()) {
	    if (variable.getName().equals(vhead)) var = variable;
	  }

	 if (var == null) throw new BedrockException("Variable " + vhead + " not found");
	 IValue val = var.getValue();
	 BedrockPlugin.logD("VAR START " + vhead + " " + var + " " + val + " " + val.hasVariables());

	 while (tok.hasMoreTokens()) {
	    boolean found = false;
	    var = null;
	    String next = tok.nextToken();
	    if (val.hasVariables()) {
	       for (IVariable t: val.getVariables()) {
		  BedrockPlugin.logD("VAR LOOKUP " + t + " " + next);
		  if (matchVariable(next,t)) {
		     found = true;
		     val = t.getValue();
		     BedrockPlugin.logD("VAR FOUND " + val + " " + val.hasVariables());
		     break;
		   }
		}
	     }
	    else if (val instanceof IJavaArray) {
	       IJavaArray arr = (IJavaArray) val;
	       int idx0 = next.indexOf("[");
	       if (idx0 >= 0) next = next.substring(idx0+1);
	       idx0 = next.indexOf("]");
	       if (idx0 >= 0) next = next.substring(0,idx0);
	       try {
		  int sub = Integer.parseInt(next);
		  val = arr.getValue(sub);
		  found = true;
		}
	       catch (NumberFormatException e) {
		  throw new BedrockException("Index expected");
		}
	     }
	    if (!found) {
	       val = null;
	       break;
	     }
	  }

	 if (val == null || tok.hasMoreTokens()) throw new BedrockException("Variable doesn't exists");

	 if (lvls < 0 && thrd instanceof IJavaThread) {
	    IJavaThread jthrd = (IJavaThread) thrd;
	    if (val instanceof IJavaArray) {
	       IJavaArray avl = (IJavaArray) val;
	       IJavaType typ = avl.getJavaType();
	       String tsg = typ.getSignature();
	       if (tsg.startsWith("[[") || tsg.contains(";")) tsg = "[Ljava/lang/Object;";
	       tsg = "(" + tsg + ")Ljava/lang/String;";
	       IJavaValue [] args = new IJavaValue[1];
	       args[0] = avl;
	       try {
		  val = avl.sendMessage("toString",tsg,args,jthrd,"Ljava/util/Arrays;");
		}
	       catch (Throwable t) {
		  BedrockPlugin.logE("Problem getting array value: " + tsg,t);
		  val = avl.sendMessage("toString","()Ljava/lang/String;",null,jthrd,false);
		}
	     }
	    else if (val instanceof IJavaObject) {
	       IJavaObject ovl = (IJavaObject) val;
	       val = ovl.sendMessage("toString","()Ljava/lang/String;",null,jthrd,false);
	     }
	  }

	 BedrockUtil.outputValue(val,(IJavaVariable) var,vname,lvls,xw);
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting variable: " + e,e);
	 throw new BedrockException("Problem accessing variable: " + e,e);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Evaluate an expression in the context of the given thread		*/
/*										*/
/********************************************************************************/

void evaluateExpression(String proj,String bid,String expr,String tname,String frid,boolean impl,
			   boolean bkpt,String eid,IvyXmlWriter xw) throws BedrockException
{
   IProject ip = our_plugin.getProjectManager().findProject(proj);
   IJavaProject jproj = JavaCore.create(ip);

   IThread thrd = null;
   IStackFrame sfrm = null;
   boolean evaldone = false;

   int detail = (impl ? DebugEvent.EVALUATION_IMPLICIT : DebugEvent.EVALUATION);

   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 IJavaDebugTarget tgt = (IJavaDebugTarget) launch.getDebugTarget();
	 for (IThread thread : tgt.getThreads()) {
	    if (matchThread(tname,thread)) {
	       if (thread.isSuspended()) {
		  thrd = thread;
		  break;
		}
	       else if (tname != null) {
		  throw new BedrockException("Thread " + tname + " not suspended");
		}
	     }
	  }
	 if (thrd == null) continue;		// not in this launch
	 for (IStackFrame frame: thrd.getStackFrames()) {
	    if (matchFrame(frid,frame)) {
	       sfrm = frame;
	       break;
	     }
	  }
	 if (sfrm == null) throw new BedrockException("Stack frame " + frid + " doesn't exist");
	 if (!(sfrm instanceof IJavaStackFrame))
	    throw new BedrockException("Stack frame " + frid + " not java frame");
	 IJavaStackFrame jsf = (IJavaStackFrame) sfrm;

	 IAstEvaluationEngine eeng = EvaluationManager.newAstEvaluationEngine(jproj,tgt);
	 ICompiledExpression eexp = eeng.getCompiledExpression(expr,jsf);
	 eeng.evaluateExpression(eexp,jsf,new EvalListener(bid,eid),detail,bkpt);
	 evaldone = true;
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting variable: " + e,e);
	 throw new BedrockException("Problem accessing variable: " + e,e);
       }
    }

   if (!evaldone) throw new BedrockException("No evaluation to do");
}



private class EvalListener implements IEvaluationListener {

   String for_id;
   String reply_id;

   EvalListener(String bid,String eid) {
      for_id = bid;
      reply_id = eid;
    }

   @Override public void evaluationComplete(IEvaluationResult rslt) {
      IvyXmlWriter xw = our_plugin.beginMessage("EVALUATION",for_id);
      xw.field("ID",reply_id);
      BedrockUtil.outputValue(rslt,xw);
      our_plugin.finishMessage(xw);
    }

}	// end of inner class EvalListener




/********************************************************************************/
/*										*/
/*	Get the details of a value						*/
/*										*/
/********************************************************************************/

void getVariableDetails(String tname,String frid,String vname,IvyXmlWriter xw)
		throws BedrockException
{
   throw new BedrockException("Not implemented yet");
}

/****************
	// IValueDetailListener requires org.eclipse.debug.ui
	//    which in turn requires that the workbench be
	//    running before this is initialized

void newGetVaiableDetails(String tname,String frid,String vname,IvyXmlWriter xw)
		throws BedrockException
{
   IThread thrd = null;
   IStackFrame sfrm = null;
   IVariable var = null;

   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 for (IThread thread : launch.getDebugTarget().getThreads()) {
	    if (matchThread(tname,thread)) {
	       if (thread.isSuspended()) {
		  thrd = thread;
		  break;
		}
	       else if (tname != null) {
		  throw new BedrockException("Thread " + tname + " not suspended");
		}
	     }
	  }

	 if (thrd == null) continue;		// not in this launch

	 for (IStackFrame frame: thrd.getStackFrames()) {
	    if (matchFrame(frid,frame)) sfrm = frame;
	  }

	 if (sfrm == null) throw new BedrockException("Stack frame " + frid + " doesn't exist");

	 StringTokenizer tok = new StringTokenizer(vname,"?");
	 if (!tok.hasMoreTokens()) throw new BedrockException("No variable specified");

	 String vhead = tok.nextToken();

	 for (IVariable variable : sfrm.getVariables()) {
	    if (variable.getName().equals(vhead)) var = variable;
	  }

	 if (var == null) throw new BedrockException("Variable " + vhead + " not found");
	 IValue val = var.getValue();
	 BedrockPlugin.logD("VARD START " + vhead + " " + var + " " + val + " " + val.hasVariables());

	 while (tok.hasMoreTokens()) {
	    boolean found = false;
	    var = null;
	    String next = tok.nextToken();
	    if (val.hasVariables()) {
	       for (IVariable t: val.getVariables()) {
		  BedrockPlugin.logD("VARD LOOKUP " + t + " " + next);
		  if (matchVariable(next,t)) {
		     found = true;
		     val = t.getValue();
		     BedrockPlugin.logD("VARD FOUND " + val + " " + val.hasVariables());
		     break;
		   }
		}
	     }
	    else if (val instanceof IJavaArray) {
	       IJavaArray arr = (IJavaArray) val;
	       int idx0 = next.indexOf("[");
	       if (idx0 >= 0) next = next.substring(idx0+1);
	       idx0 = next.indexOf("]");
	       if (idx0 >= 0) next = next.substring(0,idx0);
	       try {
		  int sub = Integer.parseInt(next);
		  val = arr.getValue(sub);
		  found = true;
		}
	       catch (NumberFormatException e) {
		  throw new BedrockException("Index expected");
		}
	     }
	    if (!found) {
	       val = null;
	       break;
	     }
	  }

	 IDebugTarget tgt = launch.getDebugTarget();
	 String id = tgt.getModelIdentifier();
	 try {
	    IWorkbench wb = PlatformUI.getWorkbench();
	    IWorkbenchPage page = wb.getActiveWorkbenchWindow().getActivePage();
	    IDebugView idv = (IDebugView) page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
	    IDebugModelPresentation dmp = idv.getPresentation(id);
	    DetailListener dl = new DetailListener();
	    dmp.computeDetail(val,dl);
	    String rslt = dl.waitFor();
	    xw.textElement("DETAIL",rslt);
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logE("Problem getting detail data: " + t,t);
	  }
       }
      catch (DebugException e) {
	 BedrockPlugin.logE("Problem getting variable: " + e,e);
	 throw new BedrockException("Problem accessing variable: " + e,e);
       }
    }
}




private class DetailListener implements IValueDetailListener {

   private boolean is_done;
   private String detail_result;

   DetailListener() {
      is_done = false;
      detail_result = null;
    }

   String waitFor() {
      synchronized (this) {
	 while (!is_done) {
	    try {
	       wait();
	     }
	    catch (InterruptedException e) { }
	  }
       }
      return detail_result;
    }

   @Override public void detailComputed(IValue val,String rslt) {
      BedrockPlugin.logD("Detail computed: " + rslt);
      synchronized (this) {
	 detail_result = rslt;
	 is_done = true;
	 notifyAll();
       }
    }

}	// end of inner class DetailListener

******************/





/********************************************************************************/
/*										*/
/*	Event handler for debug events						*/
/*										*/
/********************************************************************************/

static Map<String,Integer> kind_values;
static Map<String,Integer> detail_values;

static {
   kind_values = new HashMap<String,Integer>();
   kind_values.put("RESUME",1);
   kind_values.put("SUSPEND",2);
   kind_values.put("CREATE",4);
   kind_values.put("TERMINATE",8);
   kind_values.put("CHANGE",16);
   kind_values.put("MODEL_SPECIFIC",32);

   detail_values = new HashMap<String,Integer>();
   detail_values.put("STEP_INTO",1);
   detail_values.put("STEP_OVER",2);
   detail_values.put("STEP_RETURN",4);
   detail_values.put("STEP_END",8);
   detail_values.put("BREAKPOINT",16);
   detail_values.put("CLIENT_REQUEST",32);
   detail_values.put("EVALUATION",64);
   detail_values.put("EVALUATION_IMPLICIT",128);
   detail_values.put("STATE",256);
   detail_values.put("CONTENT",512);
}



public void handleDebugEvents(DebugEvent[] events)
{
   if (events.length <= 0) return;

   IvyXmlWriter xw = our_plugin.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());

   for (DebugEvent event: events) {
      xw.begin("RUNEVENT");
      if (event.getData() != null) xw.field("DATA",event.getData());
      BedrockUtil.fieldValue(xw,"KIND",event.getKind(),kind_values);
      BedrockUtil.fieldValue(xw,"DETAIL",event.getDetail(),detail_values);
      xw.field("EVAL",event.isEvaluation());
      xw.field("STEPSTART",event.isStepStart());
      if (event.getSource() instanceof IProcess) {
	 xw.field("TYPE","PROCESS");
	 BedrockUtil.outputProcess((IProcess) event.getSource(),xw,false);
       }
      else if (event.getSource() instanceof IJavaThread) {
	 xw.field("TYPE","THREAD");
	 BedrockUtil.outputThread((IJavaThread) event.getSource(),xw);
       }
      else if (event.getSource() instanceof IJavaDebugTarget) {
	 xw.field("TYPE","TARGET");
	 BedrockUtil.outputDebugTarget((IJavaDebugTarget) event.getSource(),xw);
       }
      else xw.field("SOURCE",event.getSource());
      xw.end("RUNEVENT");
    }

   BedrockPlugin.logD("RUNEVENT: " + xw.toString());

   our_plugin.finishMessageWait(xw);
}



/********************************************************************************/
/*										*/
/*	Event handler for launch configuration events				*/
/*										*/
/********************************************************************************/

public void launchConfigurationAdded(ILaunchConfiguration cfg)
{
   IvyXmlWriter xw = our_plugin.beginMessage("LAUNCHCONFIGEVENT");

   xw.begin("LAUNCH");
   xw.field("REASON","ADD");
   BedrockUtil.outputLaunch(cfg,xw);
   xw.end();

   our_plugin.finishMessage(xw);
}



public void launchConfigurationChanged(ILaunchConfiguration cfg)
{
   IvyXmlWriter xw = our_plugin.beginMessage("LAUNCHCONFIGEVENT");

   xw.begin("LAUNCH");
   xw.field("REASON","CHANGE");
   BedrockUtil.outputLaunch(cfg,xw);
   xw.end();

   our_plugin.finishMessage(xw);
}



public void launchConfigurationRemoved(ILaunchConfiguration cfg)
{
   IvyXmlWriter xw = our_plugin.beginMessage("LAUNCHCONFIGEVENT");

   xw.begin("LAUNCH");
   xw.field("REASON","REMOVE");
   BedrockUtil.outputLaunch(cfg,xw);
   xw.end();

   our_plugin.finishMessage(xw);
}




/********************************************************************************/
/*										*/
/*	Event handler for hot code replace					*/
/*										*/
/********************************************************************************/

@Override public void hotCodeReplaceFailed(IJavaDebugTarget tgt,DebugException e)
{ }


@Override public void hotCodeReplaceSucceeded(IJavaDebugTarget tgt)
{ }


@Override public void obsoleteMethods(IJavaDebugTarget tgt)
{ }




/********************************************************************************/
/*										*/
/*	Console management							*/
/*										*/
/********************************************************************************/

private void setupConsole(IProcess ip)
{
   IStreamsProxy isp = ip.getStreamsProxy();
   if (isp == null) {
      BedrockPlugin.logD("CONSOLE Streams proxy not supported");
      return;
    }

   IStreamMonitor ism = isp.getOutputStreamMonitor();
   ism.addListener(new ConsoleListener(ip,false));
   ism = isp.getErrorStreamMonitor();
   ism.addListener(new ConsoleListener(ip,true));
}



void writeToProcess(String pid,String text)
{
   for (ILaunch launch : debug_plugin.getLaunchManager().getLaunches()) {
      try {
	 for (IDebugTarget dt : launch.getDebugTargets()) {
	    IProcess ip = dt.getProcess();
	    if (ip == null) continue;
	    if (pid != null && !matchProcess(pid,ip)) continue;
	    IStreamsProxy isp = ip.getStreamsProxy();
	    isp.write(text);
	  }
       }
      catch (IOException e) { }
    }
}



private class ConsoleListener implements IStreamListener {

   private int process_id;
   private boolean is_stderr;

   ConsoleListener(IProcess ip,boolean err) {
      process_id = ip.hashCode();
      is_stderr = err;
    }

   @Override public void streamAppended(String txt,IStreamMonitor mon) {
      BedrockPlugin.logD("Console output: " + process_id + " " + txt);
      queueConsole(process_id,txt,is_stderr);
    }

}	// end of inner class ConsoleListener




private void queueConsole(int pid,String txt,boolean err)
{
   synchronized (console_map) {
      if (console_thread == null) {
	 console_thread = new ConsoleThread();
	 console_thread.start();
       }

      ConsoleData cd = console_map.get(pid);
      if (cd != null) {
	 cd.addWrite(txt,err);
       }
      else {
	 cd = new ConsoleData();
	 cd.addWrite(txt,err);
	 console_map.put(pid,cd);
	 console_map.notifyAll();
       }
    }
}



private class ConsoleWrite {

   private String write_text;
   private boolean is_stderr;

   ConsoleWrite(String txt,boolean err) {
      write_text = txt;
      is_stderr = err;
    }

   String getText()			{ return write_text; }
   boolean isStdErr()			{ return is_stderr; }

}	// end of inner class ConsoleWrite



private class ConsoleData {

   private List<ConsoleWrite> pending_writes;

   ConsoleData() {
      pending_writes = new ArrayList<ConsoleWrite>();
    }

   synchronized void addWrite(String txt,boolean err) {
      pending_writes.add(new ConsoleWrite(txt,err));
    }

   List<ConsoleWrite> getWrites()		{ return pending_writes; }

}	// end of inner class ConsoleData



private class ConsoleThread extends Thread {

   ConsoleThread() {
      super("BedrockConsoleMonitor");
    }

   public void run() {
      for ( ; ; ) {
	 try {
	    ConsoleData cd = null;
	    int pid = 0;
	    synchronized (console_map) {
	       while (console_map.isEmpty()) {
		  try {
		     console_map.wait();
		   }
		  catch (InterruptedException e) { }
		}
	       for (Iterator<Map.Entry<Integer,ConsoleData>> it = console_map.entrySet().iterator(); it.hasNext(); ) {
		  Map.Entry<Integer,ConsoleData> ent = it.next();
		  pid = ent.getKey();
		  cd = ent.getValue();
		  BedrockPlugin.logD("Console thread data " + pid + " " + cd);
		  it.remove();
		  if (cd != null) break;
		}
	     }
	    if (cd != null) processConsoleData(pid,cd);
	  }
	 catch (Throwable t) {
	    BedrockPlugin.logE("Problem with console thread: " + t,t);
	  }
       }
    }

   private void processConsoleData(int pid,ConsoleData cd) {
      StringBuffer buf = null;
      boolean iserr = false;
      for (ConsoleWrite cw : cd.getWrites()) {
	 if (buf == null) {
	    buf = new StringBuffer();
	    iserr = cw.isStdErr();
	    buf.append(cw.getText());
	  }
	 else if (iserr == cw.isStdErr()) {
	    buf.append(cw.getText());
	  }
	 else {
	    flushConsole(pid,buf,iserr);
	    buf = null;
	  }
	 if (buf != null && buf.length() > 16384) {
	    flushConsole(pid,buf,iserr);
	    buf = null;
	  }
       }
      if (buf != null) flushConsole(pid,buf,iserr);
    }

   private void flushConsole(int pid,StringBuffer buf,boolean iserr) {
      IvyXmlWriter xw = our_plugin.beginMessage("CONSOLE");
      xw.field("PID",pid);
      xw.field("STDERR",iserr);
      //TODO: fix this correctly
      String txt = buf.toString();
      txt = txt.replace("]]>","] ]>");
      xw.cdataElement("TEXT",txt);
      our_plugin.finishMessageWait(xw);
    }

}	// end of innerclass ConsoleThread



/********************************************************************************/
/*										*/
/*	Matching utilities							*/
/*										*/
/********************************************************************************/

private boolean matchThread(String id,IThread ith)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(ith.toString())) return true;
   if (id.equals(Integer.toString(ith.hashCode()))) return true;

   return false;
}


private boolean matchLaunch(String id,ILaunch iln)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(iln.toString())) return true;
   if (id.equals(Integer.toString(iln.hashCode()))) return true;

   return false;
}



private boolean matchDebugTarget(String id,IDebugTarget iln)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(iln.toString())) return true;
   if (id.equals(Integer.toString(iln.hashCode()))) return true;

   return false;
}



private boolean matchProcess(String id,IProcess ipr)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(ipr.toString())) return true;
   if (id.equals(Integer.toString(ipr.hashCode()))) return true;

   return false;
}



private boolean matchFrame(String id,IStackFrame ifr)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(ifr.toString())) return true;
   if (id.equals(Integer.toString(ifr.hashCode()))) return true;

   return false;
}



private boolean matchLaunchConfiguration(String id,ILaunchConfiguration il)
{
   if (id == null) return true;
   if (id.equals("*")) return true;
   if (id.equals(il.toString())) return true;
   if (id.equals(Integer.toString(System.identityHashCode(il)))) return true;
   try {
      String atr = il.getAttribute(BEDROCK_LAUNCH_ID_PROP,(String) null);
      if (atr != null && id.equals(atr)) return true;
      if (id.equals(il.getMemento())) return true;
    }
   catch (CoreException e) { }
   if (id.equals(il.getName())) return true;

   return false;
}



private boolean matchVariable(String id,IVariable v)
{
   int idx = id.lastIndexOf(".");
   if (idx >= 0) {
      if (v instanceof IJavaFieldVariable) {
	 IJavaFieldVariable jfv = (IJavaFieldVariable) v;
	 try {
	    if (id.substring(0,idx).equals(jfv.getDeclaringType().getName()) ||
		   id.substring(0,idx).equals(jfv.getReceivingType().getName()))
	       return true;
	  }
	 catch (DebugException e) { }
       }
      return false;
    }

   try {
      if (id.equals(v.getName())) {
	 if (v instanceof IJavaVariable) {
	    IJavaVariable ijv = (IJavaVariable) v;
	    try {
	       if (ijv.isLocal()) return true;
	     }
	    catch (DebugException e) { return false; }
	  }
	 return true;
       }
    }
   catch (DebugException e) { }

   return false;
}




}	// end of class BedrockRuntime




/* end of BedrockRuntime.java */
