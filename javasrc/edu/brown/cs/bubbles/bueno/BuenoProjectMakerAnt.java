/********************************************************************************/
/*                                                                              */
/*              BuenoProjectMakerAnt.java                                       */
/*                                                                              */
/*      Create a new project from an ant file                                   */
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

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


class BuenoProjectMakerAnt implements BuenoConstants, BuenoConstants.BuenoProjectMaker
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String SRC_DIR = PROJ_PROP_BASE;
private static final String SRC_FIELD = "AntField";
   



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoProjectMakerAnt()
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
   return "Create Project Using Ant Build";
}


@Override public boolean checkStatus(BuenoProjectProps props)
{
   File sdir = props.getFile(SRC_DIR);
   if (sdir == null) return false;
   if (!sdir.exists()) return false;
   File f1 = new File(sdir,"build.xml");
   if (!f1.exists()) return false;
   
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
   JTextField srcfld = pnl.addFileField("Ant Build Directory",props.getFile(SRC_DIR),
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
      if (cmd.equals("Ant Build Directory")) {
         JTextField tfld = (JTextField) evt.getSource();
         project_props.put(SRC_DIR,new File(tfld.getText()));
       }
      project_control.checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      JTextField tfld = (JTextField) project_props.get(SRC_FIELD);
      if (tfld.getDocument() == evt.getSource()) {
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
   return false;
}



}       // end of class BuenoProjectMakerAnt




/* end of BuenoProjectMakerAnt.java */

