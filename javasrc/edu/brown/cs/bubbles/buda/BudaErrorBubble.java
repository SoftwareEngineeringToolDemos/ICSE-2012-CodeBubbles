/********************************************************************************/
/*                         							*/
/*    		BudaErrorBubble.java  	            				*/
/*                            							*/
/* 	Bubble for displaying error messages 					*/
/* 				               					*/
/********************************************************************************/
/* 	Copyright 2011 Brown University -- Andrew Kovacs         		*/
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

package edu.brown.cs.bubbles.buda;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;



public class BudaErrorBubble extends BudaBubble {
private static final long serialVersionUID = 1L;

public BudaErrorBubble(final String errmsg)
{
   setContentPane(new JPanel() {
      private static final long serialVersionUID = 1L;
      {
	 JLabel errlabel = new JLabel(errmsg);
	 errlabel.setForeground(Color.RED);
	 add(new JLabel(errmsg));
      }
   });
}
}
