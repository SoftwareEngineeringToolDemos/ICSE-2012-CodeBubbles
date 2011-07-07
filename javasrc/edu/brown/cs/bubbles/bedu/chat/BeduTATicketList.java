/********************************************************************************/
/*										                                                  */
/*		BeduTATicketList.java			   													  */
/*    Bubbles for Education                                                     */
/*                                                                              */
/*    A TableModel extension of ArrayList for holding StudentTickets            */
/*										     															  */
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs									  */
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

package edu.brown.cs.bubbles.bedu.chat;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

class BeduTATicketList extends ArrayList<BeduStudentTicket> implements
		TableModel {
List<TableModelListener> listeners;



BeduTATicketList() {
	listeners = new ArrayList<TableModelListener>();
}



// TableModel methods
@Override public void addTableModelListener(TableModelListener l) {
	if (!listeners.contains(l))
		listeners.add(l);
}



@Override public Class getColumnClass(int columnIndex) {
	return String.class;
}



@Override public int getColumnCount() {
	return 2;
}



@Override public int getRowCount() {
	return size();
}



@Override public String getColumnName(int idx) throws IndexOutOfBoundsException {
	switch (idx) {
	case 0:
		return "Description";
	case 1:
		return "Time";
	default:
		throw new IndexOutOfBoundsException("No such column " + idx);
	}
}



@Override public String getValueAt(int rowIdx, int colIdx) {
	BeduStudentTicket t = get(rowIdx);
	if (t == null)
		throw new IndexOutOfBoundsException("No such row " + rowIdx);
	switch (colIdx) {
	case 0:
		return t.getText();
	case 1:
		return t.getTimestamp().toString(); // fix this so it'll look better
	default:
		throw new IndexOutOfBoundsException("No such column " + colIdx);
	}
}



@Override public boolean isCellEditable(int rowIdx, int colIdx) {
	// you shouldnt be able to edit the table
	return false;
}



@Override public void removeTableModelListener(TableModelListener l) {
	listeners.remove(l);
}



@Override public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		throws UnsupportedOperationException {
	throw new UnsupportedOperationException("Cannot set values in TicketModel");
}



// ArrayList methods which now fire TableModelEvents
@Override public boolean add(BeduStudentTicket t) {
	boolean ret = super.add(t);
	for (TableModelListener l : listeners) {
		l.tableChanged(new TableModelEvent(this, size() - 1, size() - 1,
				TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
	}

	return ret;
}



@Override public void add(int i, BeduStudentTicket t) {
	super.add(i, t);
	for (TableModelListener l : listeners) {
		l.tableChanged(new TableModelEvent(this, i, size() - 1,
				TableModelEvent.INSERT));
	}
}



@Override public BeduStudentTicket remove(int i) {
	BeduStudentTicket t = super.remove(i);
	for (TableModelListener l : listeners) {
		l.tableChanged(new TableModelEvent(this));
	}

	return t;
}



@Override public boolean remove(Object o) {
	boolean ret = super.remove(o);
	if (ret) {
		for (TableModelListener l : listeners) {
			l.tableChanged(new TableModelEvent(this));
		}
	}
	return ret;
}
}
