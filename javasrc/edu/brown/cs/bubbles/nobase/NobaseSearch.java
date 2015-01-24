/********************************************************************************/
/*                                                                              */
/*              NobaseSearch.java                                               */
/*                                                                              */
/*      Search management for nobase                                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.*;
import java.util.*;
import java.util.regex.*;


class NobaseSearch implements NobaseConstants, NobaseAst
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseSearch(NobaseMain nm)
{
}













/********************************************************************************/
/*										*/
/*	Text Search commands							*/
/*										*/
/********************************************************************************/

void handleTextSearch(String proj,int fgs,String pat,int maxresult,IvyXmlWriter xw)
throws NobaseException
{
   Pattern pp = null;
   try {
      pp = Pattern.compile(pat,fgs);
    }
   catch (PatternSyntaxException e) {
      pp = Pattern.compile(pat,fgs|Pattern.LITERAL);
    }
   
   Pattern filepat = null;
   
}









}       // end of class NobaseSearch




/* end of NobaseSearch.java */

