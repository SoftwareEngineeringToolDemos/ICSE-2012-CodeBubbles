/********************************************************************************/
/*                                                                              */
/*              BuenoProjectMakerNew.java                                       */
/*                                                                              */
/*      Create a new empty project                                              */
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

import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;



class BuenoProjectMakerNew implements BuenoConstants, BuenoConstants.BuenoProjectMaker
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String PKG_NAME = "PackageName";
private static final String PKG_FIELD = "PackageField";


private static final Pattern pkg_pat = Pattern.compile("(\\p{Alpha}\\w*\\.)*\\p{Alpha}\\w*");
      


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoProjectMakerNew()
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
   return "Completely New Project";
}


@Override public boolean checkStatus(BuenoProjectProps props)
{
   String pnm = props.getString(PKG_NAME);
   if (pnm == null || pnm.length() == 0) return true;
   if (!pkg_pat.matcher(pnm).matches()) return false;
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
   JTextField pkgfld = pnl.addTextField("Package Name",props.getString(PKG_NAME),32,cact,cact);
   props.put(PKG_FIELD,pkgfld);
   pnl.addSeparator();

   return pnl;
}   


@Override public void resetPanel(BuenoProjectProps props)
{
   JTextField pkgfld = (JTextField) props.get(PKG_FIELD);
   if (pkgfld != null) {
      pkgfld.setText(props.getString(PKG_NAME));
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
      if (cmd.equals("Package Name")) {
         JTextField tfld = (JTextField) evt.getSource();
         project_props.put(PKG_NAME,tfld.getText());
       }
      project_control.checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      JTextField tfld = (JTextField) project_props.get(PKG_FIELD);
      if (tfld != null && tfld.getDocument() == evt.getSource()) {
	 project_props.put(PKG_NAME,tfld.getText());
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
   File pdir = props.getFile(PROJ_PROP_DIRECTORY);
   File sdir = new File(pdir,"src");
   props.getSources().clear();
   props.getSources().add(sdir);
   
   if (!sdir.mkdir()) return false;
   
   BuenoProperties bp = new BuenoProperties();
   String projnm = props.getString(PROJ_PROP_NAME);
   bp.put(BuenoKey.KEY_PROJECT,projnm);
   
   String pnm = props.getString(PKG_NAME);
   if (pnm != null && pnm.length() > 0) {
      bp.put(BuenoKey.KEY_PACKAGE,pnm);
      StringTokenizer tok = new StringTokenizer(pnm,".");
      while (tok.hasMoreTokens()) {
	 sdir = new File(sdir,tok.nextToken());
       }
      if (!sdir.mkdirs()) return false;
    }
   
   Reader rd = BuenoCreator.findTemplate("scratch",bp);
   if (rd != null) {
      StringBuffer buf = new StringBuffer();
      try {
	 BuenoCreator.expand(rd,bp,null,buf);
	 File f1 = new File(sdir,"Main.java");
	 FileWriter fw = new FileWriter(f1);
	 fw.write(buf.toString());
	 fw.close();
       }
      catch (IOException e) { }
    }
   
   return true;
}



}       // end of class BuenoProjectMakerNew




/* end of BuenoProjectMakerNew.java */

