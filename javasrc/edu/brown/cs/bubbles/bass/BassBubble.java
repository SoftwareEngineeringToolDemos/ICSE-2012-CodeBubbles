/********************************************************************************/
/*										*/
/*		BassBubble.java 						*/
/*										*/
/*	Bubble Augmented Search Strategies bubble				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.buda.BudaBubble;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;


class BassBubble extends BudaBubble implements BassConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;
private BassTreeModel my_tree_model;
private BassSearchBox my_search_box;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassBubble(BassRepository br,String proj,String pfx,boolean trans)
{
   BassSearchBox.setDefault(proj,pfx);

   BassTreeModel tm = BassTreeModelVirtual.create(br,proj,pfx);

   BassSearchBox sb = new BassSearchBox(tm,trans);

   Dimension d = sb.getPreferredSize();
   sb.setSize(d);

   setTransient(trans);
   if (!trans) sb.setStatic(true);

   setContentPane(sb,sb.getEditor());
   my_tree_model = tm;
   my_search_box = sb;
}




/********************************************************************************/
/*										*/
/*     Action methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   // pass e along to search box
   my_search_box.handlePopupMenu(e);
}



void resetTreeModel(BassRepository br)
{
   my_tree_model.rebuild(br);
}



@Override public void paintOverview(Graphics2D g)
{
   //to keep package explorer from appearing in overview
   if (bass_properties.getBoolean(BASS_PACK_IN_OVERVIEW)) super.paintOverview(g);
}




}	// end of class BassBubble





/* end of BassBubble.java */



