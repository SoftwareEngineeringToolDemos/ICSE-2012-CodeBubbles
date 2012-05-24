/********************************************************************************/
/*										*/
/*		PybaseDebugConstants.java					*/
/*										*/
/*	Constants for pybase debugger implementation				*/
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





public interface PybaseDebugConstants {


/********************************************************************************/
/*										*/
/*	Command ids								*/
/*										*/
/********************************************************************************/

int CMD_RUN = 101;
int CMD_LIST_THREADS = 102;
int CMD_THREAD_CREATED = 103;
int CMD_THREAD_KILL = 104;
int CMD_THREAD_SUSPEND = 105;
int CMD_THREAD_RUN = 106;
int CMD_STEP_INTO = 107;
int CMD_STEP_OVER = 108;
int CMD_STEP_RETURN = 109;
int CMD_GET_VARIABLE = 110;
int CMD_SET_BREAK = 111;
int CMD_REMOVE_BREAK = 112;
int CMD_EVALUATE_EXPRESSION = 113;
int CMD_GET_FRAME = 114;
int CMD_EXEC_EXPRESSION = 115;
int CMD_WRITE_TO_CONSOLE = 116;
int CMD_CHANGE_VARIABLE = 117;
int CMD_RUN_TO_LINE = 118;
int CMD_RELOAD_CODE = 119;
int CMD_GET_COMPLETIONS = 120;
int CMD_SET_NEXT_STATEMENT = 121;
int CMD_SET_PY_EXCEPTION = 122;
int CMD_GET_FILE_CONTENTS = 123;
int CMD_ERROR = 901;
int CMD_VERSION = 501;
int CMD_RETURN = 502;
int CMD_GET_TASKLETS = 503;




/********************************************************************************/
/*                                                                              */
/*      Debugger events                                                         */
/*                                                                              */
/********************************************************************************/

enum DebugReason {
   UNSPECIFIED,
   STEP_END,
   CLIENT_REQUEST,
   BREAKPOINT,
   STEP_OVER,
   STEP_RETURN,
   STEP_INTO,
   
}
   


/********************************************************************************/
/*										*/
/*	Command callback interface						*/
/*										*/
/********************************************************************************/

interface CommandResponseListener {

   void commandComplete(PybaseDebugCommand cmd);

}	// end of inner interface CommandResponseListener


}	// end of interface PybaseDebugConstants




/* end of PybaseDebugConstants.java */


