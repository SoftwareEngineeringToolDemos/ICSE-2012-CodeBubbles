package edu.brown.cs.bubbles.bass;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaErrorBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

public class BassImportProjectAction extends AbstractAction {

BassImportProjectAction() {
   super("Import Project");
 }

@Override public void actionPerformed(ActionEvent e) {
   JFileChooser chooser = new JFileChooser();
  
   chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

   int returnVal = chooser.showOpenDialog(null);
   if(returnVal == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();

      File dest = new File(BoardSetup.getSetup().getDefaultWorkspace(), f.getName());

      try
      {
	 if(!Arrays.asList(f.list()).contains(".project"))
	    throw new Exception("Directory is not an eclipse project");
	 copyR(f, dest);
      }catch(Throwable t)
      {
   Component c = (Component)e.getSource();
	BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
	BudaErrorBubble bub = new BudaErrorBubble("Error importing project: " + t.getMessage());
	
	//TODO:placement could be better but this will work for now
	bba.addBubble(bub, c, null, BudaRoot.PLACEMENT_LOGICAL);
	return;
      }
      BumpClient bc = BumpClient.getBump();
      
      bc.importProject(f.getName());
   } 
 }

/**
 * Copies the given file or directory recursively including its contents 
 * @param src
 * @param dest
 * @throws IOException
 */
void copyR(File src, File dest) throws IOException
{
   if(src.isDirectory())
   {
      if(!dest.exists())
	 dest.mkdir();
      for(String file : src.list())
      {
	 System.out.println(file);
	 copyR(new File(src,file), new File(dest,file));
      }
   }
   else
   {
      InputStream in = new FileInputStream(src);
      OutputStream out = new FileOutputStream(dest);
      
      byte[] buf = new byte[1024];
      int len;
      while((len = in.read(buf)) > 0)
	 out.write(buf, 0, len);
      
      in.close();
      out.close();
   }
   
}
}
