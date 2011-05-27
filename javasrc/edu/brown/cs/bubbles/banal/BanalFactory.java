/********************************************************************************/
/*										*/
/*		BanalFactory.java						*/
/*										*/
/*	Bubbles ANALysis package factory for use within bubbles 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.bubbles.buda.BudaRoot;

import java.util.Collection;


public class BanalFactory implements BanalConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BanalProjectManager	project_manager;

private static BanalFactory	the_factory;


static {
   the_factory = new BanalFactory();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BanalFactory()
{ }


public static BanalFactory getFactory() 	{ return the_factory; }




/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

public static void setup()
{ }



public static void initialize(BudaRoot br)
{
   the_factory.setupProjectManager();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Collection<BanalPackageClass> computePackageGraph(String proj,String pkg)
{
   setupProjectManager();

   BanalPackageGraph pg = new BanalPackageGraph(proj,pkg);
   BanalStaticLoader bsl = new BanalStaticLoader(project_manager,pg);
   bsl.process();
   return pg.getClassNodes();
}



private synchronized void setupProjectManager()
{
   if (project_manager == null) {
      project_manager = new BanalProjectManager();
    }
}





}	// end of class BanalFactory




/* end of BanalFactory.java */
