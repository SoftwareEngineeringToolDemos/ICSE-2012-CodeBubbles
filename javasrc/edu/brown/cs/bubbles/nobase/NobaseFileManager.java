/********************************************************************************/
/*                                                                              */
/*              NobaseFileManager.java                                          */
/*                                                                              */
/*      description of class                                                    */
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

import org.eclipse.jface.text.*;

import java.util.*;
import java.io.*;


class NobaseFileManager implements NobaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<File,NobaseFile> file_data;
   
   
   

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

NobaseFileManager(NobaseMain nm)
{
   file_data = new HashMap<File,NobaseFile>();
}


/********************************************************************************/
/*										*/
/*	Get file buffer for a file						*/
/*										*/
/********************************************************************************/

NobaseFile getFileData(File f)
{
   return file_data.get(f);
}


NobaseFile getFileData(String fnm)
{
   return file_data.get(new File(fnm));
}



NobaseFile getNewFileData(File f,String nm,NobaseProject pp)
{
   NobaseFile fd = file_data.get(f);
   
   if (fd == null) {
      fd = new NobaseFile(f,nm,pp);
      file_data.put(f,fd);
    }
   
   return fd;
}


NobaseFile getFileData(IDocument d,File f)
{
   return new NobaseFile(d,f);
}



int getFileOffset(File f,int line,int col)
{
   NobaseFile ifd = getFileData(f);
   if (ifd == null) return 0;
   
   IDocument doc = ifd.getDocument();
   try {
      int off = doc.getLineOffset(line-1);
      return off + col - 1;
    }
   catch (BadLocationException e) { }
   
   return 0;
}






}       // end of class NobaseFileManager




/* end of NobaseFileManager.java */

