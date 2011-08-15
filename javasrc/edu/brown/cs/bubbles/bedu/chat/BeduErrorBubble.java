/********************************************************************************/
/*                         							*/
/*    		BeduErrorBubble.java  	            				*/
/*                            							*/
/* 	Bubbles for Education   						*/
/* 	Bubble for displaying error messages in Bedu package  			*/
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

package edu.brown.cs.bubbles.bedu.chat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.brown.cs.bubbles.buda.BudaBubble;


class BeduErrorBubble extends BudaBubble {
private static final long serialVersionUID = 1L;

BeduErrorBubble(final String err_msg)
{
   setContentPane(new JPanel() {
      private static final long serialVersionUID = 1L;
      {
	 add(new JLabel(err_msg));
      }
   });
}
}
