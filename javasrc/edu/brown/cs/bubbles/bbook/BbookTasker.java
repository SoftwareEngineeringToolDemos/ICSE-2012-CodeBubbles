/********************************************************************************/
/*										*/
/*		BbookTasker.java						*/
/*										*/
/*	Create bubbles for task-related data entry				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bbook;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.bnote.*;

import edu.brown.cs.ivy.swing.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


class BbookTasker implements BbookConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<String>	all_projects;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookTasker()
{
   all_projects = null;
}



/********************************************************************************/
/*										*/
/*	Create task selection bubble						*/
/*										*/
/********************************************************************************/

void createTaskSelector(BudaBubbleArea bba,Point pt,String proj)
{
   TaskSelector tsc = new TaskSelector(proj);

   BudaConstraint bc = new BudaConstraint(BudaConstants.BudaBubblePosition.DIALOG,pt);
   bba.add(tsc,bc);
}



private class TaskSelector extends SwingGridPanel implements ActionListener {

   private SwingComboBox task_box;
   private String current_project;
   private int result_status;

   TaskSelector(String proj) {
      task_box = null;
      current_project = proj;
      result_status = 0;

      setInsets(4);
      setBackground(Color.CYAN);

      beginLayout();
      addBannerLabel("Programmer's Log Book");
      addSeparator();

      List<String> plist = getProjects();
      addChoice("Project",plist,proj,this);

      task_box = addChoice("Task",(Collection<String>) null,0,this);
      setupTasks();

      addBottomButton("Cancel","Cancel",this);
      addBottomButton("Add Note","Add Note",this);
      addBottomButton("Done","Done",this);
      addBottomButtons();
   }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Project")) {
	 JComboBox cbx = (JComboBox) evt.getSource();
	 String p = (String) cbx.getSelectedItem();
	 if (p != null && !p.equals(current_project)) setupTasks();
       }
      else if (cmd.equals("Task")) { }
      else if (cmd.equals("Cancel")) {
	 result_status = -1;
	 closeDialog(evt);
       }
      else if (cmd.equals("Done")) {
	 closeDialog(evt);
	 if (task_box != null && current_project != null) {
	    String task = (String) task_box.getSelectedItem();
	    if (task.equals("< New Task >")) {
	       result_status = 1;
	     }
	    else result_status = -1;
	  }
       }
      else if (cmd.equals("Add Note")) {
	 closeDialog(evt);
	 if (task_box != null && current_project != null) {
	    result_status = 2;
	  }
       }

      if (result_status == 0) updateButtons();
    }

   private void setupTasks() {
      if (task_box == null || current_project == null) return;
      task_box.removeAllItems();
      task_box.addItem("< New Task >");
      List<String> tasks = BnoteFactory.getFactory().getTasksForProject(current_project);
      if (tasks != null && tasks.size() > 0) {
	 for (String t : tasks) {
	    task_box.addItem(t);
	 }
	 task_box.setSelectedIndex(1);
      }
      else task_box.setSelectedIndex(0);
      updateButtons();
    }

   private void updateButtons() {
      JButton done = (JButton) getComponentForLabel("Done");
      JButton note = (JButton) getComponentForLabel("Add Note");
      if (current_project != null && task_box != null && task_box.getSelectedItem() != null) {
	 if (done != null) done.setEnabled(true);
	 if (note != null) note.setEnabled(true);
       }
      else {
	 if (done != null) done.setEnabled(false);
	 if (note != null) note.setEnabled(false);
       }
    }

   private void closeDialog(ActionEvent evt) {
      setVisible(false);
      Component c = (Component) evt.getSource();
      BudaBubble bb = BudaRoot.findBudaBubble(c);
      if (bb != null) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
	 if (bba != null) {
	    bba.remove(bb);
	    return;
	  }
       }

      while (c != null) {
	 if (c instanceof Dialog) {
	    c.setVisible(false);
	    break;
	  }
	 c = c.getParent();
       }
   }


}	// end of inner class TaskSelector



/********************************************************************************/
/*										*/
/*	Project management							*/
/*										*/
/********************************************************************************/

private List<String> getProjects()
{
   if (all_projects != null) return all_projects;

   all_projects = new ArrayList<String>();

   Element pxml = BumpClient.getBump().getAllProjects();
   if (pxml != null) {
      Set<String> pset = new TreeSet<String>();
      for (Element pe : IvyXml.children(pxml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 pset.add(pnm);
       }
      all_projects.addAll(pset);
    }

   return all_projects;
}



}	// end of class BbookTasker




/* end of BbookTasker.java */

