/********************************************************************************/
/*										*/
/*		BedrockProgressMonitor.java					*/
/*										*/
/*	Progress monitor for Bubbles - Eclipse interface			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */


/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.runtime.NullProgressMonitor;




class BedrockProgressMonitor extends NullProgressMonitor {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin	for_plugin;
private String		task_name;
private double		total_work;
private boolean 	is_done;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockProgressMonitor(BedrockPlugin bp,String nm)
{
   for_plugin = bp;
   task_name = nm;
   total_work = UNKNOWN;
   is_done = false;
}



/********************************************************************************/
/*										*/
/*	Monitor methods 							*/
/*										*/
/********************************************************************************/

@Override public void beginTask(String name,int total)
{
   super.beginTask(name,total);

   if (name != null && name.length() > 0) task_name = name;
   total_work = total;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","BEGIN");
   xw.field("TASK",task_name);
   for_plugin.finishMessage(xw);
}



@Override public void done()
{
   super.done();

   if (task_name == null) return;

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","DONE");
   xw.field("TASK",task_name);
   for_plugin.finishMessage(xw);

   task_name = null;

   synchronized (this) {
      is_done = true;
      notifyAll();
    }
}



@Override public void setCanceled(boolean v)
{
   super.setCanceled(v);

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   if (v) xw.field("KIND","CANCEL");
   else xw.field("KIND","UNCANCEL");
   xw.field("TASK",task_name);
   for_plugin.finishMessage(xw);
}




@Override public void setTaskName(String name)
{
   super.setTaskName(name);

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","ENDSUBTASK");
   xw.field("TASK",task_name);
   for_plugin.finishMessage(xw);
}




@Override public void subTask(String name)
{
   super.subTask(name);

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","SUBTASK");
   xw.field("TASK",task_name);
   xw.field("SUBTASK",name);
   for_plugin.finishMessage(xw);
}




@Override public void worked(int w)
{
   super.worked(w);

   if (total_work == UNKNOWN) return;

   double v = w / total_work;
   double v0 = (v*100);
   if (v0 < 0) v0 = 0;
   if (v0 > 100) v0 = 100;
   if (w != 0 && v0 == 0) {
      BedrockPlugin.log("Inconsistent progress numbers: " + w + " " + total_work + " " + v0);
    }

   IvyXmlWriter xw = for_plugin.beginMessage("PROGRESS");
   xw.field("KIND","WORKED");
   xw.field("TASK",task_name);
   xw.field("WORK",v0);
   for_plugin.finishMessage(xw);
}



/********************************************************************************/
/*										*/
/*	Waiting methods 							*/
/*										*/
/********************************************************************************/

synchronized void waitFor()
{
   while (!is_done) {
      try {
	 wait();
       }
      catch (InterruptedException e) { }
    }
}



}	// end of class BedrockProgressMonitor




/* end of BedrockProgressMonitor.java */
