/********************************************************************************/
/*										*/
/*		BeamBugReport.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items bug report panel		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Yu Li	      */
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

package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bqst.BqstFactory;
import edu.brown.cs.bubbles.bqst.BqstPanel;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.*;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class BeamBugReport implements BeamConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot		for_root;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/


BeamBugReport(BudaRoot br)
{
   for_root = br;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addPanel()
{
   Icon chevron = BoardImage.getIcon("dropdown_chevron");
   chevron = BoardImage.resizeIcon(((ImageIcon)chevron).getImage(), BUDA_BUTTON_RESIZE_WIDTH, BUDA_BUTTON_RESIZE_HEIGHT);
   // JButton btn = new JButton("<html><center>Report<br>Bugs</center></html>", chevron);
   JButton btn = new JButton("<html><center>Report Bug</center></html>", chevron);
   btn.setHorizontalTextPosition(AbstractButton.LEADING);
   Font ft = btn.getFont();
   ft = ft.deriveFont(10f);
   btn.setBackground(BUDA_BUTTON_PANEL_COLOR);
   btn.setMargin(BUDA_BUTTON_INSETS);
   btn.setFont(ft);
   btn.setOpaque(false);

   btn.addActionListener(new BugReportListener());
   btn.setToolTipText("Report a bubbles bug");
   for_root.addButtonPanelButton(btn);
}



/********************************************************************************/
/*										*/
/*	Callback to gather bug report information				*/
/*										*/
/********************************************************************************/

private class BugReportListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      BqstPanel form = makeReportBugForm();
      form.display();
   }

}	// end of inner class BugReportListener


/**
 * Create a bug report form
 **/
private BqstPanel makeReportBugForm()
{
   BqstPanel form = BqstFactory.createBqstPanel(for_root,"Code Bubbles Beta Bug Report");
   //submit debug.log and bedrock.log at same time
   form.addOtherSubmitFile(BoardLog.getBubblesLogFile());
   form.addOtherSubmitFile(BoardLog.getBedrockLogFile());

   form.addLongText("Short description of the bug",
		    "(e.g. what is the context and the undesirable behavior)",
		    true);
   form.addLongText("How to reproduce?",
		    "(the exact steps needed to reproduce the bug; please mention anything that might be relevant)",
		    true);
   form.addLongText("Additional information",
		    "(Any additional information that might help us understand the context, such as how large the project you are working with is, etc.)",
		    false);
   form.setScreenshotFlag(false);

   form.addHiddenField("Version",BoardSetup.getVersionData());

   return form;
}



}	// end of class BeamBugReport



/* end of BeamBugReport.java */
