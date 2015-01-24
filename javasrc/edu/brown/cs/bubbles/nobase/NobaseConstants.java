/********************************************************************************/
/*										*/
/*		NobaseConstants.java						*/
/*										*/
/*	Constants for Node Bubbles base interface						    */
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.IDocument;

import java.io.File;
import java.util.*;

public interface NobaseConstants
{




/********************************************************************************/
/*										*/
/*	Messaging definitions							*/
/*										*/
/********************************************************************************/

// Must Match BumpConstants.BUMP_MINT_NAME
String	NOBASE_MINT_NAME = "BUBBLES_" + System.getProperty("user.name").replace(" ","_");



/********************************************************************************/
/*										*/
/*	Logging constants							*/
/*										*/
/********************************************************************************/

enum NobaseLogLevel {
   NONE,
   ERROR,
   WARNING,
   INFO,
   DEBUG
}




/********************************************************************************/
/*										*/
/*	Search Information							*/
/*										*/
/********************************************************************************/

int MAX_TEXT_SEARCH_RESULTS = 128;


enum SearchFor {
   NONE,
   ANNOTATION,
   CONSTRUCTOR,
   METHOD,
   FIELD,
   TYPE,
   PACKAGE,
   CLASS,
}



interface SearchResult {
   
   int getOffset();
   int getLength();
   NobaseFile getFile();
   NobaseSymbol getSymbol();
   NobaseSymbol getContainer();
   
}




/********************************************************************************/
/*										*/
/*	Errro Messages								*/
/*										*/
/********************************************************************************/

enum ErrorSeverity {
   IGNORE,
   INFO,
   WARNING,
   ERROR
}



/********************************************************************************/
/*										*/
/*	Thread pool information 						*/
/*										*/
/********************************************************************************/

int NOBASE_CORE_POOL_SIZE = 2;
int NOBASE_MAX_POOL_SIZE = 8;
long NOBASE_POOL_KEEP_ALIVE_TIME = 2*60*1000;




/********************************************************************************/
/*										*/
/*	Edit command information						*/
/*										*/
/********************************************************************************/

interface IEditData {

   public int getOffset();
   public int getLength();
   public String getText();

}	// end of subinterface EditData



/********************************************************************************/
/*										*/
/*	File information							*/
/*										*/
/********************************************************************************/

interface XXXIFileData {

   File getFile();
   IDocument getDocument();
   String getModuleName();
   void reload();
   long getLastDateLastModified();

   boolean hasChanged();
   void markChanged();
   boolean commit(boolean refresh,boolean save);

   void clearPositions();
   void setStart(Object o,int line,int col);
   void setEnd(Object o,int line,int col);
   void setEndFromStart(Object o,int line,int col);
   void setEnd(Object o,int off);
   int getStartOffset(Object o);
   int getEndOffset(Object o);
   int getLength(Object o);

}	// end of inner interface IFileData


enum FileType {
   UNKNOWN
}











/********************************************************************************/
/*										*/
/*	Compiler information							*/
/*										*/
/********************************************************************************/

interface IParser {
   ISemanticData parse(NobaseProject proj,NobaseFile fd);
}



interface ISemanticData {

   NobaseFile getFileData();
   NobaseProject getProject();
   List<NobaseMessage> getMessages();
   void addMessages(List<NobaseMessage> msgs);

   NobaseAst.NobaseAstNode getRootNode();

}	// end of inner interface ISemanticData





interface IAstScope {

}


enum ScopeType {
   GLOBAL,
   FILE,
   FUNCTION,
   BLOCK,
   MEMBER,
   OBJECT,
   WITH
};


enum AstProperty {
   SCOPE,
   REF,
   DEF,
   TYPE,
   EXPR,
   NAME
}



enum NameType {
   FUNCTION,
   VARIABLE,
   LOCAL,
   MODULE,
}


enum KnownValue {
   UNDEFINED,
   NULL,
   ANY,
   UNKNOWN
}



/********************************************************************************/
/*										*/
/*	Debugging constants							*/
/*										*/
/********************************************************************************/

enum NobaseDebugAction {
   NONE,
   TERMINATE,
   RESUME,
   STEP_INTO,
   STEP_OVER,
   STEP_RETURN,
   SUSPEND,
   DROP_TO_FRAME
}



}	// end of interface NobaseConstants




/* end of NobaseConstants.java */
