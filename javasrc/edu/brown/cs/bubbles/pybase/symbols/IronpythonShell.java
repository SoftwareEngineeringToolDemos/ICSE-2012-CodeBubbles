/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.*;

import java.io.File;
import java.io.IOException;


/**
 * @author Fabio Zadrozny
 */
public class IronpythonShell extends AbstractShell implements PybaseConstants {




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public IronpythonShell() throws IOException, PybaseException
{
   super(PybaseNature.getScriptWithinPySrc("pycompletionserver.py"));
}


/********************************************************************************/
/*										*/
/*	Shell creation methods							*/
/*										*/
/********************************************************************************/

@Override protected synchronized String createServerProcess(PybaseInterpreter interpreter,
							       int pWrite,int pRead)
	throws IOException
{
   File file = new File(interpreter.getExecutableOrJar());
   if (file.exists() == false) {
      throw new RuntimeException("The interpreter location found does not exist. "
				    + interpreter);
    }
   if (file.isDirectory() == true) {
      throw new RuntimeException("The interpreter location found is a directory. "
				    + interpreter);
    }


   String execMsg;
   if (PybaseNature.isWindowsPlatform()) { // in windows, we have to put python "path_to_file.py"
      execMsg = interpreter + " \"" + PybaseFileSystem.getFileAbsolutePath(serverFile)
	 + "\" " + pWrite + " " + pRead;
    }
   else { // however in mac, or linux, this gives an error...
      execMsg = interpreter + " " + PybaseFileSystem.getFileAbsolutePath(serverFile)
	 + " " + pWrite + " " + pRead;
    }
   String[] parameters = SimpleIronpythonRunner.preparePythonCallParameters(
      interpreter.getExecutableOrJar(),
	 PybaseFileSystem.getFileAbsolutePath(serverFile), new String[] { "" + pWrite,
									     "" + pRead }, true);

   AbstractInterpreterManager manager = PybaseNature.getInterpreterManager(PybaseInterpreterType.IRONPYTHON);

   String[] envp = null;
   try {
      envp = SimpleRunner.getEnvironment(null, interpreter, manager, true);
    }
   catch (PybaseException e) {
      PybaseMain.logE("Problem createing ironpython server", e);
    }

   process = SimpleIronpythonRunner.createProcess(parameters, envp,
						     serverFile.getParentFile());

   return execMsg;
}



}	// end of class IronpythonShell




/* end of IronpythonShell.java */
