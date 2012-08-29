/********************************************************************************/
/*										*/
/*		PybaseDebugger.java						*/
/*										*/
/*	Remote debugger manager for Bubbles from Python 			*/
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

import edu.brown.cs.bubbles.pybase.PybaseMain;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class PybaseDebugger implements PybaseDebugConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<PybaseDebugTarget> debug_targets;
private PybaseLaunchConfig for_config;
private ListenConnector listen_connect;
private String debug_id;

private static IdCounter        debug_counter = new IdCounter();


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseDebugger(PybaseLaunchConfig cfg)
{
   debug_targets = new ArrayList<PybaseDebugTarget>();
   for_config = cfg;
   listen_connect = null;
   debug_id = "DEBUG_" + Integer.toString(debug_counter.nextValue());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addTarget(PybaseDebugTarget t)
{
   debug_targets.add(t);
}


public PybaseLaunchConfig getLaunchConfig()     { return for_config; }

public List<PybaseDebugTarget> getTargets()     { return debug_targets; }

public String getId()                           { return debug_id; }



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void startConnect() throws IOException
{
   listen_connect = new ListenConnector();
   listen_connect.start();
}



Socket waitForConnect(Process p)
{ 
   if (listen_connect == null) return null;
   listen_connect.waitForConnection();
   if (listen_connect.getException() != null) {
      return null;
   }
   return listen_connect.getSocket();
}


public void dispose()
{
   if (listen_connect != null) listen_connect.stopListening();
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("ID",debug_id);
   xw.field("CID",for_config.getId());
   xw.end("LAUNCH");
}



/********************************************************************************/
/*                                                                              */
/*      ListenConnector                                                         */
/*                                                                              */
/********************************************************************************/

private class ListenConnector extends Thread {
   
   private int time_out;
   private ServerSocket server_socket;
   private Socket accept_socket;
   private Exception error_exception;
   
   ListenConnector() throws IOException {
      time_out = 0;
      accept_socket = null;
      error_exception = null;
      server_socket = null;
      try {
         server_socket = new ServerSocket();
       }
      catch (IOException e) {
         PybaseMain.logE("Problem creating server socket",e);
         throw e;
       }
    }
   
   Exception getException()             { return error_exception; }
   Socket getSocket()                   { return accept_socket; }
   
   @Override public void run() {
      try {
         server_socket.setSoTimeout(time_out);
         accept_socket = server_socket.accept();
       }
      catch (IOException e) {
         error_exception = e;
       }
      synchronized (this) {
	 notifyAll();
      }
    }
   
   void waitForConnection() {
      synchronized (this) {
	 while (server_socket != null && accept_socket == null && error_exception == null) {
	    try {
	       wait();
	    }
	    catch (InterruptedException e) { }
	 }
      }
   }
   
   void stopListening() {
      if (server_socket != null) {
         try {
            server_socket.close();
          }
         catch (IOException ex) { }
         server_socket = null;
         synchronized (this) {
            notifyAll();
         }
       }
    }
   
   @Override protected void finalize() throws Throwable {
      stopListening();
    }
   
}       // end of inner class ListenConnector
   
      
      


}	// end of class PybaseDebugger




/* end of PybaseDebugger.java */
