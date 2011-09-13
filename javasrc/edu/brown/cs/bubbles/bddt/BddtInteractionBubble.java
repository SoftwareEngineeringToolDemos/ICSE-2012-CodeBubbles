   /********************************************************************************/
/*										*/
/*		BddtInteractionBubble.java					*/
/*										*/
/*	Bubble Environment interactive expression bubble			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook, Steven P. Reiss	*/
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




package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;


class BddtInteractionBubble extends BudaBubble implements BddtConstants, BudaConstants,
	BumpConstants, BddtConstants.BddtFrameListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	for_control;
private JEditorPane		display_area;
private JTextField		input_field;
private BumpStackFrame		active_frame;
private BumpStackFrame		last_frame;

private static SimpleAttributeSet	frame_attrs;
private static SimpleAttributeSet	input_attrs;
private static SimpleAttributeSet	output_attrs;
private static SimpleAttributeSet	error_attrs;


private static final long serialVersionUID = 1;


static {
   frame_attrs = new SimpleAttributeSet();
   frame_attrs.addAttribute(StyleConstants.Bold,Boolean.TRUE);
   input_attrs = new SimpleAttributeSet();
   output_attrs = new SimpleAttributeSet();
   output_attrs.addAttribute(StyleConstants.Foreground,Color.GREEN);
   error_attrs = new SimpleAttributeSet();
   error_attrs.addAttribute(StyleConstants.Foreground,Color.RED);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtInteractionBubble(BddtLaunchControl ctrl)
{
   super(null,BudaBorder.RECTANGLE);

   for_control = ctrl;
   active_frame = null;
   last_frame = null;

   display_area = new JEditorPane("text/html",null);
   display_area.setEditable(false);

   input_field = new JTextField();
   input_field.addActionListener(new ExprTypein());

   JScrollPane scrl = new JScrollPane(display_area);
   scrl.setPreferredSize(BDDT_INTERACTION_INITIAL_SIZE);

   JPanel pnl = new JPanel(new BorderLayout());

   pnl.add(scrl,BorderLayout.CENTER);
   pnl.add(input_field,BorderLayout.SOUTH);

   setContentPane(pnl,input_field);
   pnl.addMouseListener(new FocusOnEntry(input_field));
   for_control.addFrameListener(this);
   setActiveFrame(for_control.getActiveFrame());
}




@Override protected void localDispose()
{
   for_control.removeFrameListener(this);
}



/********************************************************************************/
/*										*/
/*	Activation routines							*/
/*										*/
/********************************************************************************/

@Override public void setActiveFrame(BumpStackFrame frm)
{
   active_frame = frm;
}




/********************************************************************************/
/*										*/
/*	Menu management 							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   popup.add(getFloatBubbleAction());

   popup.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private void evaluate(String expr)
{
   if (active_frame == null) return;
   if (!active_frame.match(last_frame)) {
      addText("\nFRAME: " + active_frame.getDisplayString() + "\n",frame_attrs);
      last_frame = active_frame;
    }

   addText(expr,input_attrs);
   addText(" = ",input_attrs);

   ExpressionValue ev = for_control.evaluateExpression(active_frame,expr);
   if (ev != null) {
      if (ev.isValid()) addText(ev.formatResult(),output_attrs);
      else addText(ev.getError(),error_attrs);
    }
   addText("\n",null);
}



private synchronized void addText(String t,AttributeSet attr)
{
   if (t == null || t.length() == 0) return;

   Document d = display_area.getDocument();
   int ln = d.getLength();

   try {
      d.insertString(ln,t,attr);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BDDT","Problem inserting evaluation output",e);
    }
}




private class ExprTypein implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JTextField tfld = (JTextField) evt.getSource();
      String expr = tfld.getText();
      if (expr == null || expr.length() == 0) return;

      evaluate(expr);

      tfld.setText("");
    }

}	// end of inner class ExprTypein





}	// end of class BddtInteractionBubble




/* end of BddtInteractionBubble.java */
