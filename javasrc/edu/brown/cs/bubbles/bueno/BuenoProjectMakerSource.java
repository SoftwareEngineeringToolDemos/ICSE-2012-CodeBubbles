/********************************************************************************/
/*                                                                              */
/*              BuenoProjectMakerSource.java                                    */
/*                                                                              */
/*      Create a new project from existing source                               */
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.ivy.swing.*;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;



class BuenoProjectMakerSource implements BuenoConstants, BuenoConstants.BuenoProjectMaker
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String SRC_DIR = PROJ_PROP_BASE;
private static final String SRC_FIELD = "SourceField";

private static final String SRC_NAME = "src_";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoProjectMakerSource()
{
   BuenoProjectCreator.addProjectMaker(this);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getLabel()
{
   return "Create Project Using Existing Source";
}


@Override public boolean checkStatus(BuenoProjectProps props)
{
   File sdir = props.getFile(SRC_DIR);
   if (sdir == null) return false;
   if (!sdir.exists()) return false;
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Interaction methods                                                     */
/*                                                                              */
/********************************************************************************/

@Override public JPanel createPanel(BuenoProjectCreationControl ctrl,BuenoProjectProps props)
{
   NewActions cact = new NewActions(ctrl,props);
   
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   JTextField srcfld = pnl.addFileField("Source Directory",props.getFile(SRC_DIR),
	 JFileChooser.DIRECTORIES_ONLY,cact,cact);   
   props.put(SRC_FIELD,srcfld);
   pnl.addSeparator();
   
   return pnl;
}   


@Override public void resetPanel(BuenoProjectProps props)
{
   JTextField srcfld = (JTextField) props.get(SRC_FIELD);
   File sdir = props.getFile(SRC_DIR);
   if (srcfld != null && sdir != null) {
      srcfld.setText(sdir.getPath());
    }
}



private class NewActions implements ActionListener, UndoableEditListener {
   
   private BuenoProjectCreationControl project_control;
   private BuenoProjectProps project_props;
   
   NewActions(BuenoProjectCreationControl ctrl,BuenoProjectProps props) {
      project_control = ctrl;
      project_props = props;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Source Directory")) {
         JTextField tfld = (JTextField) evt.getSource();
         project_props.put(SRC_DIR,new File(tfld.getText()));
       }
      project_control.checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      JTextField tfld = (JTextField) project_props.get(SRC_FIELD);
      if (tfld != null && tfld.getDocument() == evt.getSource()) {
	 project_props.put(SRC_DIR,new File(tfld.getText()));
	 project_control.checkStatus();
       }
    }
   
}       // end of inner class NewActions


/********************************************************************************/
/*                                                                              */
/*      Project Creation methods                                                */
/*                                                                              */
/********************************************************************************/

@Override public boolean setupProject(BuenoProjectCreationControl ctrl,BuenoProjectProps props)
{
   File dir = props.getFile(SRC_DIR);
   List<File> srcs = new ArrayList<File>();
   List<File> libs = new ArrayList<File>();
   
   findFiles(dir,srcs,libs);
   Map<File,List<File>> roots = new HashMap<File,List<File>>();
   for (File sf : srcs) {
      String pkg = ctrl.getPackageName(sf);
      File par = sf.getParentFile();
      if (pkg != null) {
         String [] ps = pkg.split("\\.");
         for (int i = ps.length-1; par != null && i >= 0; --i) {
            if (!par.getName().equals(ps[i])) par = null;
            else par = par.getParentFile();
          }
       }
      if (par != null) {
         List<File> lf = roots.get(par);
         if (lf == null) {
            lf = new ArrayList<File>();
            roots.put(par,lf);
          }
         lf.add(sf);
       }
    }
   props.getLinks().clear();
   props.getSources().clear();
   props.getLibraries().clear();
   
   int ctr = 1;
   for (File f : roots.keySet()) {
      String nm = SRC_NAME + Integer.toString(ctr++);
      props.getLinks().put(nm,f);
      props.getSources().add(f);
    }
   
   for (File lf : libs) {
      props.getLibraries().add(lf);
    }
   
   return true;
}




private void findFiles(File dir,List<File> srcs,List<File> libs)
{
   if (dir.isDirectory()) {
      if (dir.listFiles() != null) {
	 for (File sf : dir.listFiles()) {
	    findFiles(sf,srcs,libs);
	  }
       }
      return;
    }
   
   String pnm = dir.getPath();
   if (pnm.endsWith(".java")) srcs.add(dir.getAbsoluteFile());
   else if (pnm.endsWith(".jar")) srcs.add(dir.getAbsoluteFile());
}
  

}       // end of class BuenoProjectMakerSource




/* end of BuenoProjectMakerSource.java */

