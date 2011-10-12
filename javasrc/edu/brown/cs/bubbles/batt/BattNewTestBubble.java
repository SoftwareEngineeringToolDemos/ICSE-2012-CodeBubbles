/********************************************************************************/
/*										*/
/*		BattNewTestBubble.java						*/
/*										*/
/*	Bubble Automated Testing Tool class for defining new tests		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bale.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.bueno.*;

import edu.brown.cs.ivy.swing.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.lang.reflect.*;
import java.util.List;


class BattNewTestBubble implements BattConstants, BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private NewTestMode		test_mode;
private String			method_name;
private BumpLocation		method_data;


private interface StatusUpdate {

   void itemUpdated();

}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattNewTestBubble(String mthd,BumpLocation loc,NewTestMode md)
{
   test_mode = md;
   method_name = mthd;
   method_data = loc;
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

BudaBubble createNewTestBubble()
{
   if (method_data == null) return null;

   String fnm = method_name;
   int idx1 = fnm.indexOf("(");
   if (idx1 >= 0) fnm = fnm.substring(0,idx1);
   idx1 = fnm.lastIndexOf(".");
   if (idx1 >= 0) fnm = fnm.substring(idx1+1);

   BudaBubble bb = null;

   switch (test_mode) {
      default :
      case USER_CODE :
	 String nm = getTestMethodName();
	 createNewTestMethod(fnm,nm,null);
	 bb = BaleFactory.getFactory().createMethodBubble(null,nm);
	 break;
      case INPUT_OUTPUT :
	 bb = new CallMethodBubble(fnm);
	 break;
    }


   return bb;
}



private String getTestMethodName()
{
   String cnm = null;
   String mnm = method_name;
   int idx = mnm.indexOf("(");
   if (idx >= 0) mnm = mnm.substring(0,idx);
   idx = mnm.lastIndexOf(".");
   if (idx >= 0) {
      cnm = mnm.substring(0,idx);
      mnm = mnm.substring(idx+1);
    }
   if (mnm.length() == 0) mnm = "something";

   BumpClient bc = BumpClient.getBump();

   for (int i = 1; i < 100; ++i) {
      String tnm = "test_" + mnm + "_" + i;
      String fnm = cnm + "." + tnm;
      List<BumpLocation> locs = bc.findMethod(null,fnm,false);
      if (locs == null || locs.size() == 0) return fnm;
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	User method handling							*/
/*										*/
/********************************************************************************/

private void createNewTestMethod(String fnm,String nm,String cnts)
{
   int idx = nm.lastIndexOf(".");
   String cnm = nm.substring(0,idx);
   String mnm = nm.substring(idx+1);
   if (cnts == null) cnts = "// insert test code here";

   String anm = null;
   // anm should be last test in class, or null if there are none
   BuenoLocation loc = BuenoFactory.getFactory().createLocation(null,cnm,anm,true);

   BuenoProperties props = new BuenoProperties();
   props.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
   props.put(BuenoKey.KEY_COMMENT,"Test case for " + fnm);
   props.put(BuenoKey.KEY_NAME,mnm);
   props.put(BuenoKey.KEY_RETURNS,"void");
   props.put(BuenoKey.KEY_MODIFIERS,Modifier.PUBLIC);
   props.put(BuenoKey.KEY_CONTENTS,cnts);
   props.put(BuenoKey.KEY_ATTRIBUTES,"@Test");

   BuenoFactory.getFactory().createNew(BuenoType.NEW_METHOD,loc,props);
}



/********************************************************************************/
/*										*/
/*	Input Output method handling						*/
/*										*/
/********************************************************************************/

private class CallMethodBubble extends BudaBubble implements ActionListener, StatusUpdate {

   private NewTestArea test_area;
   private JButton generate_button;

   CallMethodBubble(String fnm) {
      test_area = new NewTestArea(this);
      for (int i = 0; i < 3; ++i) {		   // initial test cases
	 test_area.addTestCase();
       }

      JPanel pnl = new JPanel(new BorderLayout());
      pnl.setOpaque(false);
      pnl.add(test_area.getPanel(),BorderLayout.CENTER);

      JLabel top = new JLabel("Test Cases for " + fnm);
      top.setOpaque(false);

      top.setHorizontalAlignment(JLabel.CENTER);
      pnl.add(top,BorderLayout.NORTH);

      generate_button = new JButton("Generate");
      generate_button.addActionListener(this);
      generate_button.setEnabled(false);
      Box bx = Box.createHorizontalBox();
      bx.add(Box.createHorizontalGlue());
      bx.add(generate_button);
      bx.add(Box.createHorizontalGlue());
      pnl.add(bx,BorderLayout.SOUTH);

      setInteriorColor(new Color(0xf0d0a0));

      setContentPane(pnl);
    }

   @Override protected void localDispose() {
      test_area = null;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String mnm = getTestMethodName();
      setVisible(false);
      if (mnm == null) return;
      BattNewTestChecker ckr = new BattNewTestChecker();
      List<NewTestCase> cases = test_area.getActiveTests();
      int sz = cases.size();
      if (sz == 0) return;
      String [][] tests = new String[sz][2];
      for (int i = 0; i < cases.size(); ++i) {
         NewTestCase ntc = cases.get(i);
         tests[i][0] = ntc.getTestInput();
         tests[i][1] = ntc.getTestOutput();
       }
      String rslt = ckr.generateCallTestCode(tests);
      if (rslt == null) return;
      // create new method
      // bring up bubble on that method
    }

   @Override public void itemUpdated() {
      generate_button.setEnabled(test_area.validate());
    }

}	// end of inner class CallMethodBubble



/********************************************************************************/
/*										*/
/*	NewTestArea -- holder of a set of test cases				*/
/*										*/
/********************************************************************************/

private class NewTestArea {

   private SwingGridPanel test_panel;
   private List<NewTestCase> test_cases;
   private StatusUpdate status_update;

   NewTestArea(StatusUpdate upd) {
      status_update = upd;
      test_cases = new ArrayList<NewTestCase>();
      test_panel = new SwingGridPanel();
      test_panel.setOpaque(false);
      test_panel.setInsets(2);
    }

   JPanel getPanel()				{ return test_panel; }

   int getTestRow(NewTestCase tc)		{ return test_cases.indexOf(tc); }

   void addTestCell(NewTestCase tc,JComponent c,int pos,int span) {
      int r = getTestRow(tc);
      test_panel.addGBComponent(c,pos,r,span,1,1,0);
    }

   void handleUpdate(NewTestCase tc) {
      ensureEmptyTest();
      resetSubsequentTests(tc);
      status_update.itemUpdated();
    }

   void resetSubsequentTests(NewTestCase tc)			{ }

   void ensureEmptyTest() {
      boolean needtest = false;
      int idx = test_cases.size()-1;
      if (idx < 0) needtest = true;
      else if (!test_cases.get(idx).isEmpty()) needtest = true;

      if (needtest) addTestCase();
    }

   void addTestCase() {
      NewTestCase ntc = null;
      switch (test_mode) {
	 case INPUT_OUTPUT :
	    ntc = new CallTestCase(this);
	    break;
	 case CALL_SEQUENCE :
	    ntc = null;
	    break;
       }
      test_cases.add(ntc);
      ntc.setup();
    }

   boolean validate() {
      BattNewTestChecker bc = new BattNewTestChecker();
      int ntest = 0;
      boolean valid = true;
      for (NewTestCase tc : test_cases) {
	 if (tc.isEmpty()) continue;
	 if (!tc.validate(bc)) valid = false;
	 else ++ntest;
       }
      if (ntest == 0) valid = false;
      return valid;
    }
   
   List<NewTestCase> getActiveTests() {
      List<NewTestCase> ltc = new ArrayList<NewTestCase>();
      BattNewTestChecker bc = new BattNewTestChecker();
      for (NewTestCase tc : test_cases) {
         if (!tc.isEmpty() && tc.validate(bc)) ltc.add(tc);
       }
      return ltc;
    }

}	// end of inner class NewTestArea



/********************************************************************************/
/*										*/
/*	NewTestCase -- implementation of a test case				*/
/*										*/
/********************************************************************************/

private abstract class NewTestCase implements CaretListener, ActionListener, FocusListener {

   protected NewTestArea test_area;
   private String last_error;
   private boolean is_checked;
   protected JTextField test_args;
   protected JTextField test_result;
   protected JComboBox test_op;

   NewTestCase(NewTestArea ta) {
      test_area = ta;
      last_error = null;
      is_checked = false;
    }

   String getTestOutput() {
      if (test_result == null) return null;
      return test_result.getText().trim();
    }

   String getTestInput() {
      if (test_args == null) return null;
      return test_args.getText().trim();
    }

   boolean isEmpty() {
      String ta = getTestInput();
      String tb = getTestOutput();
      if (ta != null && !ta.equals("")) return false;
      if (tb != null && !tb.equals("")) return false;
      return true;
    }

   abstract void setup();

   void invalidate()				{ is_checked = false; }
   boolean validate(BattNewTestChecker tc) {
      if (!is_checked) {
	 last_error = check(tc);
	 is_checked = true;
       }
      return last_error == null;
    }
   abstract String check(BattNewTestChecker btc);

   protected JTextField createTextField(int len) {
      JTextField tf = new JTextField(len);
      tf.addCaretListener(this);
      tf.addFocusListener(this);
      return tf;
    }

   protected JComboBox createSelection(Enum<?> dflt,Enum<?> [] opts) {
      JComboBox cbx = new JComboBox(opts);
      cbx.setSelectedItem(dflt);
      cbx.addActionListener(this);
      return cbx;
    }

   @Override public void caretUpdate(CaretEvent e) {
      invalidate();
      test_area.handleUpdate(this);
    }

   @Override public void actionPerformed(ActionEvent e) {
      invalidate();
      test_area.handleUpdate(this);
    }

   @Override public void focusGained(FocusEvent e)		{ }
   @Override public void focusLost(FocusEvent e) {
      // validate the test case here
    }

}	// end of inner class NewTestCase



/********************************************************************************/
/*										*/
/*	Call Test case								*/
/*										*/
/********************************************************************************/

private static NewTestOp [] CALL_OPS = { NewTestOp.EQL, NewTestOp.NEQ,
					    NewTestOp.THROW, NewTestOp.SAME,
					    NewTestOp.DIFF, NewTestOp.SHOW };
private static NewTestOp [] VOID_CALL_OPS = { NewTestOp.THROW, NewTestOp.IGNORE };


private class CallTestCase extends NewTestCase {

   private boolean no_return;
   private boolean no_args;

   CallTestCase(NewTestArea ta) {
      super(ta);
      String pls = method_data.getParameters();
      no_args = (pls == null || pls.equals("()"));
      pls = method_data.getReturnType();
      no_return = (pls == null || pls.equals("void"));
    }

   void setup() {
      Box bx = Box.createHorizontalBox();
      bx.setOpaque(false);
      JLabel l1 = new JLabel("(");
      l1.setOpaque(false);
      bx.add(l1);
      test_args = createTextField(20);
      if (no_args) {
	 test_args.setEditable(false);
	 test_args.setText("void");
       }
      bx.add(test_args);
      l1 = new JLabel(")");
      l1.setOpaque(false);
      bx.add(l1);
      test_area.addTestCell(this,bx,0,1);

      test_result = createTextField(15);
      test_area.addTestCell(this,test_result,2,1);

      if (no_return) {
	 test_op = createSelection(NewTestOp.IGNORE,VOID_CALL_OPS);
	 test_result.setEditable(false);
	 test_result.setText("void");
       }
      else {
	 test_op = createSelection(NewTestOp.EQL,CALL_OPS);
       }
      test_area.addTestCell(this,test_op,1,1);
    }

   String check(BattNewTestChecker btc) {
      return btc.checkCallTest(method_data,getTestInput(),getTestOutput());
    }

}


}	// end of class BattNewTestBubble




/* end of BattNewTestBubble.java */