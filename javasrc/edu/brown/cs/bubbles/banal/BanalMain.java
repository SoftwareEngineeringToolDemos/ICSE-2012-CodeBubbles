/********************************************************************************/
/*										*/
/*		BanalMain.java							*/
/*										*/
/*	Bubbles ANALysis package main program for running independently 	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.IOException;



public class BanalMain implements BanalConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BanalMain bm = new BanalMain(args);

   BoardSetup brd = BoardSetup.getSetup();
   brd.setSkipSplash();

   bm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BanalMain(String [] args)
{
   project_name = null;

   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-p") && i+1 < args.length) {            // -project <project>
	    project_name = args[++i];
	  }
	 else badArgs();
       }
      else {
	 badArgs();
       }
    }

   if (project_name == null) badArgs();
}



private void badArgs()
{
   System.err.println("BANALMAIN: banalmain -p <project>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   processPackage();
}


private void processPackage()
{
   BanalProjectManager bpm = new BanalProjectManager();
   BanalPackageGraph pg = new BanalPackageGraph(project_name,"edu.brown.cs.bubbles.bass");
   BanalStaticLoader bsl = new BanalStaticLoader(bpm,pg);
   bsl.process();

   try {
      IvyXmlWriter xw = new IvyXmlWriter("test.out");
      pg.outputXml(xw);
      xw.close();
    }
   catch (IOException e) {
      System.err.println("PROBLEM OUTPUTING XML: " + e);
    }
}




}	// end of class BanalMain



/* end of BanalMain.java */


