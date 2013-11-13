/********************************************************************************/
/*										*/
/*		BvcrGenerateUid.java						*/
/*										*/
/*	Bubble Version Collaboration Repository uid generator			*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.bvcr;


import java.io.*;
import java.util.UUID;


public class BvcrGenerateUid implements BvcrConstants
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   UUID u = UUID.randomUUID();

   File f = new File(".uid");
   if (f.exists()) {
      System.err.println("BVCR: .uid file already exists in this directory");
    }
   else {
      try {
	 FileWriter fw = new FileWriter(f);
	 fw.write(u.toString() + "\n");
	 fw.close();
       }
      catch (IOException e) {
	 System.err.println("BVCR: Can't create .uid file in this directory");
       }
    }
}




}	// end of class BvcrGenerateUid



/* end of BvcrGenerateUid.java */
