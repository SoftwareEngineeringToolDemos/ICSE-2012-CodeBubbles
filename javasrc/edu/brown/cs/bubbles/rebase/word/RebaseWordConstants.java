/********************************************************************************/
/*										*/
/*		RebaseWordConstants.java					*/
/*										*/
/*	Constants for word processing for rebase search 			*/
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



package edu.brown.cs.bubbles.rebase.word;



public interface RebaseWordConstants {


/********************************************************************************/
/*										*/
/*	Files									*/
/*										*/
/********************************************************************************/

String WORD_LIST_FILE = "words";



/********************************************************************************/
/*                                                                              */
/*      Word options                                                            */
/*                                                                              */
/********************************************************************************/

enum WordOptions {
   SPLIT_CAMELCASE,             // split camel case words
   SPLIT_UNDERSCORE,            // split words on underscores
   SPLIT_NUMBER,                // split words on numbers
   SPLIT_COMPOUND,              // split compound words
   STEM,                        // do stemming
   VOWELLESS,                   // add abbreviations from dropping vowels
}


}	// end of interface RebaseWordConstants




/* end of RebaseWordConstants.java */
