/********************************************************************************/
/*										*/
/*		PybaseLaunchConfig.java 					*/
/*										*/
/*	Launch configuration representation for Python Bubbles			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;

import java.io.File;




public class PybaseLaunchConfig implements PybaseDebugConstants {
   
   
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String argument_set;
private File base_file;
   


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public PybaseLaunchConfig()
{ 
   argument_set = null;
   base_file = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public File getFileToRun()                      { return base_file; }
public void setFileToRun(File f)                { base_file = f; }

public String getArguments()                    { return argument_set; }
public void setArguments(String a)              { argument_set = a; }

public String [] getEnvironment()               { return null; }
public File getWorkingDirectory()               { return null; }
public String getEncoding()                     { return null; }
public String getPySrcPath()                    { return null; }

public String [] getCommandLine()
{
   return null;
}

}	// end of class PybaseLaunchConfig




/* end of PybaseLaunchConfig.java */
