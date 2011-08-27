/********************************************************************************/
/*										*/
/*		BddtConstants.java						*/
/*										*/
/*	Bubbles Environment dynamic debugger tool constants			*/
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunValue;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpStackFrame;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStack;

import javax.swing.tree.TreeNode;

import java.awt.*;
import java.util.EventListener;



public interface BddtConstants {


/********************************************************************************/
/*										*/
/*	Graphic definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Basic color for a debugging channel
 **/

Color	BDDT_CHANNEL_TOP_COLOR = new Color(255,255,182);

Color	BDDT_CHANNEL_BOTTOM_COLOR = Color.WHITE;


/**
 *	Color (top) for the debugging panel.
 **/
Color	BDDT_PANEL_TOP_COLOR = new Color(0xffffffda);

/**
 *	Color (bottom) for the debugging panel.
 **/
Color	BDDT_PANEL_BOTTOM_COLOR = new Color(0xffffffda);



/**
 *	Colors for the configuration panel
 **/

Color BDDT_CONFIG_TOP_COLOR = new Color(182,182,255);
Color BDDT_CONFIG_BOTTOM_COLOR = Color.WHITE;

int	BDDT_CONFIG_WIDTH = 450;
int	BDDT_CONFIG_HEIGHT = 150;


/**
 * Constants for the process panel
 **/

int   BDDT_PROCESS_WIDTH = 350;
int   BDDT_PROCESS_HEIGHT = 100;

Color BDDT_PROCESS_TOP_COLOR = new Color(125,235,250);
Color BDDT_PROCESS_BOTTOM_COLOR = Color.WHITE;


/**
 * Constants for the threads panel
 */

int   BDDT_THREADS_WIDTH = 400;
int   BDDT_THREADS_HEIGHT = 200;

Color BDDT_THREADS_TOP_COLOR = new Color(153,255,133);
Color BDDT_THREADS_BOTTOM_COLOR = Color.WHITE;


/**
 * Constants for the stack/frame/value panesl
 */

int   BDDT_STACK_WIDTH = 400;
int   BDDT_STACK_HEIGHT = 200;
int   BDDT_STACK_VALUE_HEIGHT = 50;

Color BDDT_STACK_TOP_COLOR = new Color(153,255,133);
Color BDDT_STACK_BOTTOM_COLOR = Color.WHITE;

Color BDDT_STACK_FROZEN_TOP_COLOR = new Color(153,255,255);
Color BDDT_STACK_FROZEN_BOTTOM_COLOR = Color.WHITE;

Color BDDT_STACK_EXTINCT_TOP_COLOR = new Color(255,255,133);
Color BDDT_STACK_EXTINCT_BOTTOM_COLOR = Color.WHITE;



/**
 *	Constants for execution (method) bubbles
 **/

Color BDDT_LINK_COLOR = Color.RED;



/**
 *	Constants for history viewer
 **/

int   BDDT_HISTORY_WIDTH = 300;
int   BDDT_HISTORY_HEIGHT = 200;


/**
 *	Constants for stop trace viewer
 **/

int   BDDT_STOP_TRACE_WIDTH = 300;
int   BDDT_STOP_TRACE_HEIGHT = 200;



String BDDT_CONFIG_BUTTON = "Bubble.Debug.Configurations";


String BDDT_PROCESS_BUTTON = "Bubble.Debug.Current Processes";


String BDDT_TOOLBAR_MENU_BUTTON = "DebugMenu";
String BDDT_TOOLBAR_RUN_BUTTON = "DebugRun";




/********************************************************************************/
/*										*/
/*	Constants for console bubbles						*/
/*										*/
/********************************************************************************/

Font BDDT_CONSOLE_FONT = BoardFont.getFont(Font.MONOSPACED, Font.PLAIN, 11);
int BDDT_CONSOLE_WIDTH = 300;
int BDDT_CONSOLE_HEIGHT = 200;

int BDDT_CONSOLE_MAX_LINES = 1000;



/********************************************************************************/
/*										*/
/*	Constants for performance bubbles					*/
/*										*/
/********************************************************************************/

Font BDDT_PERF_FONT = BoardFont.getFont(Font.SERIF, Font.PLAIN, 11);
int BDDT_PERF_WIDTH = 500;
int BDDT_PERF_HEIGHT = 200;

Color BDDT_PERF_TOP_COLOR = new Color(234,135,250);
Color BDDT_PERF_BOTTOM_COLOR = Color.WHITE;




/********************************************************************************/
/*										*/
/*	Breakpoint display constants						*/
/*										*/
/********************************************************************************/
//amc6

/**
 * Initial size of the BbptBubble
 **/
Dimension BDDT_BREAKPOINT_INITIAL_SIZE = new Dimension(320,130);

/**
 * Minimum width of columns
 **/
int[] BDDT_BREAKPOINT_COLUMN_WIDTHS = {40,130,80,45};

/**
 * Color to use in the overview area to display the breakpoint bubble.
 **/
Color BDDT_BREAKPOINT_OVERVIEW_COLOR = new Color(247,80,80);

/**
 * Color at the top of the Breakpoint bubble.
 */
Color BDDT_BREAKPOINT_TOP_COLOR = new Color(255,200,200);

/**
 * Color at the bottom of the Breakpoint bubble.
 */
Color BDDT_BREAKPOINT_BOTTOM_COLOR = new Color(247,140,140);

/**
 * Color displayed when a row is selected in the breakpoint bubble.
 */
Color BDDT_BREAKPOINT_SELECTION_COLOR = new Color(247,80,80);

/**
 * Name of the button on the top-level menu for creating breakpoint bubbles
 */
String BDDT_BREAKPOINT_BUTTON = "Bubble.Debug.Breakpoint List";

/**
 * The margins on buttons in the breakpoints bubble
 */

Insets BDDT_BREAKPOINT_BUTTON_MARGIN = new Insets(0,0,0,0);




/********************************************************************************/
/*										*/
/*	Execution annotation constants						*/
/*										*/
/********************************************************************************/

/**
 *	Color for execution annotations
 **/
String BDDT_EXECUTE_ANNOT_COLOR = "ExecutionAnnotation.color";

Color BDDT_LAUNCH_OVERVIEW_COLOR = new Color(182,182,255);

/**
 *	Color for debug focus annotations
 **/
Color BDDT_DEBUG_FOCUS_ANNOT_COLOR = new Color(0x20ffff00,true);




/********************************************************************************/
/*										*/
/*	Launch control definitions						*/
/*										*/
/********************************************************************************/

enum LaunchState {
   READY,
   STARTING,
   RUNNING,
   PAUSED,
   PARTIAL_PAUSE,
   TERMINATED
}


int BDDT_LAUNCH_CONTROL_X = 16;
int BDDT_LAUNCH_CONTROL_Y = 26;

String BDDT_PROPERTY_FLOAT_LAUNCH_CONTROL = "LaunchControl.float";





String BDDT_PROPERTY_FLOAT_CONSOLE = "Console.float";
String BDDT_PROPERTY_FLOAT_THREADS = "Threads.float";
String BDDT_PROPERTY_FLOAT_HISTORY = "History.float";




/********************************************************************************/
/*										*/
/*	Value definitions							*/
/*										*/
/********************************************************************************/

enum ValueSetType {
   THREAD,			// thread
   STACK,			// stack for a thread
   FRAME,			// single frame
   VALUE,			// variable value
   CATEGORY			// set of values
}



String	BDDT_PROPERTY_FREEZE_LEVELS = "Freeze.levels";



interface ValueTreeNode extends TreeNode {

   String getKey();
   BumpStackFrame getFrame();
   Object getValue();
   BumpRunValue getRunValue();
   boolean showValueArea();

}	// end of inner interface ValueTreeNode




/********************************************************************************/
/*										*/
/*	Repository definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Search box name prefix for launch configurations
 **/

String BDDT_LAUNCH_CONFIG_PREFIX = "zzzzzz#@Launch Configurations.";



/**
 *	Search box name prefix for processes
 **/

String BDDT_PROCESS_PREFIX = "zzzzzz#@Processes.";



/********************************************************************************/
/*										*/
/*	History definitions							*/
/*										*/
/********************************************************************************/

interface BddtHistoryData {

   Iterable<BumpThread> getThreads();
   Iterable<BddtHistoryItem> getItems(BumpThread bt);
   void addHistoryListener(BddtHistoryListener bl);
   void removeHistoryListener(BddtHistoryListener bl);

}


interface BddtHistoryItem {

   BumpThread getThread();
   BumpThreadStack getStack();
   BumpRunValue getThisValue();
   String getClassName();
   long getTime();

   boolean isInside(BddtHistoryItem bhi);

}	// end of inner interface BddtHistoryItem



interface BddtHistoryListener extends EventListener {

   void handleHistoryStarted();
   void handleHistoryUpdated();

}



/********************************************************************************/
/*										*/
/*	History Graph parameters						*/
/*										*/
/********************************************************************************/

int	GRAPH_DEFAULT_WIDTH = 400;
int	GRAPH_DEFAULT_HEIGHT = 300;


double	GRAPH_LEFT_RIGHT_MARGIN = 5;
double	GRAPH_OBJECT_WIDTH = 100;
double	GRAPH_OBJECT_HEIGHT = 24;
double	GRAPH_OBJECT_H_SPACE = 10;
double	GRAPH_OBJECT_V_SPACE = 10;
double	GRAPH_TOP_BOTTOM_MARGIN = 5;
double	GRAPH_ACTIVE_WIDTH = 20;
double	GRAPH_TIME_SPACE = GRAPH_DEFAULT_HEIGHT - 2 * GRAPH_TOP_BOTTOM_MARGIN -
				GRAPH_OBJECT_HEIGHT - GRAPH_OBJECT_V_SPACE;


enum LinkType {
   ENTER,
   RETURN,
   NEXT
}



/********************************************************************************/
/*										*/
/*	Bubble types								*/
/*										*/
/********************************************************************************/

enum BubbleType {
   BDDT,		// launch control, frames, etc
   THREADS,		// thread view
   CONSOLE,		// console
   HISTORY,
   EXEC,		// bubble for current execution point
   FRAME,		// bubble for user selection up the call stack
   VALUES,		// stack frame values bubble
   STOP_TRACE,		// trace of stacks just before debugger stop
   USER 		// bubble created by the user
}



}	// end of interface BddtConstants



/* end of BddtConstants.java */
