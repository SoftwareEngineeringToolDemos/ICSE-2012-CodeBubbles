/********************************************************************************/
/*										*/
/*		RebusSearchBubble.java						*/
/*										*/
/*	Bubble to allos search of a repository					*/
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



package edu.brown.cs.bubbles.rebus;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bbook.BbookFactory;
import edu.brown.cs.bubbles.bnote.BnoteConstants.BnoteTask;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.mint.*;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.event.*;
import java.io.StringWriter;
import java.util.List;


class RebusSearchBubble extends BudaBubble implements RebusConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JTextField		key_field;
private JList<SearchEngines>	search_engines;
private JButton 		search_button;
private JComboBox<SearchType>	search_type;

private static final long serialVersionUID = 1;


public enum SearchEngines {
   OHLOH,
   GITHUB,
   GREPCODE
}

public enum SearchType {
   FILE,
   PACKAGE,
   SYSTEM
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebusSearchBubble()
{
   JPanel pnl = setupPanel();

   setContentPane(pnl);
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private JPanel setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();

   SearchListener sl = new SearchListener();

   pnl.addBannerLabel("Repository Search");

   key_field = pnl.addTextField("Keywords",null,sl,sl);

   search_engines = new JList<SearchEngines>(SearchEngines.values());
   search_engines.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
   String engs = BoardProperties.getProperties("Rebus").getProperty("Rebus.search.engines");
   int i = 0;
   for (SearchEngines se : SearchEngines.values()) {
      if (engs == null || engs.contains(se.toString())) {
	 search_engines.addSelectionInterval(i,i);
       }
      ++i;
    }
   pnl.addRawComponent("Repository",search_engines);

   search_type = pnl.addChoice("Search Type",SearchType.FILE,true,sl);

   search_button = pnl.addBottomButton("SEARCH","SEARCH",sl);
   pnl.addBottomButtons();

   checkStatus();

   return pnl;
}



private void checkStatus()
{
   if (search_button == null) return;

   String txt = key_field.getText();
   List<String> keys = IvyExec.tokenize(txt);
   List<SearchEngines> engs = search_engines.getSelectedValuesList();

   if (keys.size() == 0 || engs.size() == 0) {
      search_button.setEnabled(false);
    }
   else {
      search_button.setEnabled(true);
    }
}




/********************************************************************************/
/*										*/
/*	Handle right click requests						*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());
   menu.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Search Methods								*/
/*										*/
/********************************************************************************/

private void doSearch()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();

   List<SearchEngines> engs = search_engines.getSelectedValuesList();
   StringWriter sw = new StringWriter();
   sw.write("TEXT='");
   IvyXml.outputXmlString(key_field.getText(),sw);
   sw.write("' REPO='");
   for (SearchEngines se : engs) {
      sw.write(se.toString());
      sw.write(",");
    }
   sw.write("' TYPE='");
   sw.write(search_type.getSelectedItem().toString());
   sw.write("'");

   BoardProperties bp = BoardProperties.getProperties("Rebus");
   if (bp.getBoolean("Rebus.use.tasks")) {
      BnoteTask task = BbookFactory.getFactory().getCurrentTask(this);
      if (task != null) {
	 sw.write(" TASK='");
	 sw.write(Long.toString(task.getTaskId()));
	 sw.write("'");
      }
    }

   String cmd = "<BUBBLES DO='REBUSSEARCH' ";
   cmd += sw.toString();
   cmd += " LANG='Rebase' />";

   BoardLog.logD("REBUS","Send Command: " + cmd);

   MintDefaultReply mdr = new MintDefaultReply();
   mc.send(cmd,mdr,MintConstants.MINT_MSG_FIRST_NON_NULL);

   String rply = mdr.waitForString();
   System.err.println("REBUS SEARCH REPLY = " + rply);
}




/********************************************************************************/
/*										*/
/*	Listeners								*/
/*										*/
/********************************************************************************/

private class SearchListener implements ActionListener, UndoableEditListener {

   @Override public void actionPerformed(ActionEvent e) {
     if (e.getSource() == search_button) {
	 doSearch();
       }
     else if (e.getSource() == key_field) {
	checkStatus();
	if (search_button != null && search_button.isEnabled() && e.getID() != 0) {
	   doSearch();
	 }
      }
     else {
	checkStatus();
      }
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      checkStatus();
    }

}	// end of inner class SearchListener


}	// end of class RebusSearchBubble




/* end of RebusSearchBubble.java */

