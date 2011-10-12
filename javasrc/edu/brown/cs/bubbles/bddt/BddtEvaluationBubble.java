/********************************************************************************/
/*										*/
/*		BddtEvaluationBubble.java					*/
/*										*/
/*	Bubble Environment expression evaluation bubble 			*/
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
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import java.util.List;


class BddtEvaluationBubble extends BudaBubble implements BddtConstants, BudaConstants,
	BumpConstants, BddtConstants.BddtFrameListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	for_control;
private EvaluationTableModel	eval_model;
private EvaluationTable 	eval_table;
private JLabel			frame_label;
private JTextField		input_field;
private BumpStackFrame		active_frame;

enum EvalState {
   NONE,
   CURRENT,
   EXPR_ERROR,
   UPSTACK,
   OUTOFDATE
}



private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtEvaluationBubble(BddtLaunchControl ctrl)
{
   super(null,BudaBorder.RECTANGLE);

   for_control = ctrl;
   active_frame = null;

   eval_model = new EvaluationTableModel();

   eval_table = new EvaluationTable(eval_model);

   JScrollPane scroll = new JScrollPane(eval_table);
   scroll.setCursor(Cursor.getDefaultCursor());

   frame_label = new JLabel();
   input_field = new JTextField();
   input_field.addActionListener(new ExprAdder());

   JPanel mainpanel = new JPanel(new BorderLayout());
   mainpanel.add(frame_label,BorderLayout.NORTH);
   mainpanel.add(scroll,BorderLayout.CENTER);
   mainpanel.add(input_field,BorderLayout.SOUTH);

   setContentPane(mainpanel,input_field);

   mainpanel.addMouseListener(new FocusOnEntry(input_field));

   ctrl.addFrameListener(this);

   setActiveFrame(ctrl.getActiveFrame());
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
   if (frm == null) return;
   if (eval_model == null) return;

   active_frame = frm;
   eval_model.updateForFrame(frm);

   frame_label.setText(frm.getDisplayString());
}



/********************************************************************************/
/*										*/
/*	Menu management 							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   EvalExpr ee = null;
   Point p0 = SwingUtilities.convertPoint(this,e.getPoint(),eval_table);
   int row = eval_table.rowAtPoint(p0);
   if (row >= 0) ee = eval_table.getActualExpr(row);

   if (ee != null) {
      popup.add(new RemoveAction(ee));
      popup.add(new EditAction(ee));
    }

   popup.add(getFloatBubbleAction());

   popup.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Event management							*/
/*										*/
/********************************************************************************/

private class ExprAdder implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JTextField tfld = (JTextField) evt.getSource();
      String expr = tfld.getText();
      if (expr == null || expr.length() == 0) return;

      EvalExpr ee = new EvalExpr(expr);
      eval_model.add(ee);

      BoardMetrics.noteCommand("BDDT","AddExpression");

      tfld.setText("");
    }

}	// end of inner class ExprAdder



private class RemoveAction extends AbstractAction {

   private EvalExpr for_expr;

   RemoveAction(EvalExpr ee) {
      super("Remove Expression");
      for_expr = ee;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","RemoveExpression");
      eval_model.remove(for_expr);
    }

}	// end of inner class RemoveAction




private static class EditAction extends AbstractAction {

   // private EvalExpr for_expr;

   EditAction(EvalExpr ee) {
      super("Edit Expression");
      // for_expr = ee;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","EditExpression");
      // TODO: handle expression and property editing
    }

}	// end of inner class EditAction




/********************************************************************************/
/*										*/
/*	Evaluation table							*/
/*										*/
/********************************************************************************/

private class EvaluationTable extends JTable {

   private static final long serialVersionUID = 1;

   EvaluationTable(EvaluationTableModel mdl) {
      super(mdl);
      setOpaque(false);
      setToolTipText("");
      setAutoCreateRowSorter(true);
      setPreferredScrollableViewportSize(BDDT_EVALUATION_INITIAL_SIZE);
      setFillsViewportHeight(true);
      setDefaultRenderer(String.class,new EvaluationTableRenderer(getDefaultRenderer(String.class)));
    }

   EvalExpr getActualExpr(int row) {
      RowSorter<?> rs = getRowSorter();
      if (rs != null) row = rs.convertRowIndexToModel(row);
      return (EvalExpr) eval_model.getValueAt(row,-1);
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Rectangle r = new Rectangle(0,0,sz.width,sz.height);
      g2.setColor(new Color(0xffff80));
      g2.fill(r);
      super.paintComponent(g);
    }

   @Override public String getToolTipText(MouseEvent e) {
      int r = rowAtPoint(e.getPoint());
      if (r < 0) return null;
      EvalExpr ee = getActualExpr(r);
      if (ee != null) {
	 StringBuffer buf = new StringBuffer();
	 buf.append("<html>");
	 buf.append(ee.getExpr());
	 buf.append(" = ");
	 buf.append(ee.getValue());
	 return buf.toString();
       }
      return null;
    }

}	// end of inner class EvaluationTable




/********************************************************************************/
/*										*/
/*	Cell rendering								*/
/*										*/
/********************************************************************************/

private class EvaluationTableRenderer implements TableCellRenderer {

   private TableCellRenderer default_renderer;

   EvaluationTableRenderer(TableCellRenderer renderer) {
      default_renderer = renderer;
    }

   @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,
							       boolean foc,int r,int c) {
      JComponent cmp = (JComponent) default_renderer.getTableCellRendererComponent(t,v,sel,foc,r,c);
      cmp.setOpaque(false);
      Object vx = eval_model.getValueAt(r,-1);
      if (vx != null) {
	 EvalExpr ee = (EvalExpr) vx;
	 switch (ee.getState()) {
	    // different colors based on states
	 }
      }

      return cmp;
    }

}	// end of inner class EvaluationTableRenderer




/********************************************************************************/
/*										*/
/*	Table model								*/
/*										*/
/********************************************************************************/

private class EvaluationTableModel extends AbstractTableModel {

   private List<EvalExpr>	eval_exprs;

   private String [] column_names = { "Expression", "Value" };

   private static final long serialVersionUID = 1;


   EvaluationTableModel() {
      eval_exprs = new ArrayList<EvalExpr>();
    }

   @Override public int getColumnCount()		{ return column_names.length; }
   @Override public String getColumnName(int i) 	{ return column_names[i]; }
   @Override public int getRowCount()			{ return eval_exprs.size(); }
   @Override public Class<?> getColumnClass(int i)	{ return String.class; }
   @Override public boolean isCellEditable(int r,int c) { return false; }

   @Override public Object getValueAt(int r,int c) {
      EvalExpr ee;
      synchronized (eval_exprs) {
	 if (r < 0 || r >= eval_exprs.size()) return null;
	 ee = eval_exprs.get(r);
       }
      switch (c) {
	 case -1 :
	    return ee;
	 case 0 :
	    return ee.getExpr();
	 case 1 :
	    return ee.getValue();
       }
      return null;
    }

   void add(EvalExpr ee) {
      synchronized (eval_exprs) {
	 int idx = eval_exprs.size();
	 eval_exprs.add(ee);
	 ee.updateForFrame(active_frame);
	 fireTableRowsInserted(idx,idx);
       }
    }

   void remove(EvalExpr ee) {
      synchronized (eval_exprs) {
	 int idx = eval_exprs.indexOf(ee);
	 if (idx < 0) return;
	 eval_exprs.remove(idx);
	 fireTableRowsDeleted(idx,idx);
       }
    }

   void updateForFrame(BumpStackFrame frm) {
      synchronized (eval_exprs) {
	 for (int i = 0; i < eval_exprs.size(); ++i) {
	    EvalExpr ee = eval_exprs.get(i);
	    if (ee.updateForFrame(frm)) {
	       fireTableRowsUpdated(i,i);
	     }
	  }
       }
    }

}	// end of inner class EvaluationTableModel




/********************************************************************************/
/*										*/
/*	EvalExpr -- holder of expression to evaluation				*/
/*										*/
/********************************************************************************/

private class EvalExpr {

   private String expr_string;
   private String current_value;
   private EvalState current_state;
   private BumpThread for_thread;
   private Pattern thread_match;
   private Pattern method_match;

   EvalExpr(String expr) {
      expr_string = expr;
      current_value = null;
      current_state = EvalState.NONE;
      for_thread = null;
      thread_match = null;
      method_match = null;
    }

   String getExpr()				{ return expr_string; }
   String getValue()				{ return current_value; }
   EvalState getState() 			{ return current_state; }

   boolean updateForFrame(BumpStackFrame frm) {
      if (frm == null) return false;
      boolean relevant = true;
      if (for_thread != null && frm.getThread() != for_thread) relevant = false;
      if (relevant && thread_match != null) {
	 if (!thread_match.matcher(frm.getThread().getName()).find()) relevant = false;
       }
      if (relevant && method_match != null) {
	 if (!method_match.matcher(frm.getMethod()).find()) relevant = false;
       }
      if (!relevant) current_state = EvalState.OUTOFDATE;
      else {
	 ExpressionValue ev = for_control.evaluateExpression(frm,expr_string);
	 if (ev != null && ev.isValid()) {
	    current_state = EvalState.CURRENT;
	    current_value = ev.formatResult();
	  }
	 else if (ev != null) {
	    current_state = EvalState.EXPR_ERROR;
	    current_value = ev.getError();
	  }
	 else current_state = EvalState.EXPR_ERROR;
       }

      return false;
    }


}	// end of inner class EvalExpr




}	// end of class BddtEvaluationBubble




/* end of BddtEvaluationBubble.java */
