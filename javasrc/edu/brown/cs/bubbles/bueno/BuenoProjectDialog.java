/********************************************************************************/
/*										*/
/*		BuenoProjectDialog.java 					*/
/*										*/
/*	BUbbles Environment New Objects project path setup dialog		*/
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;

import edu.brown.cs.ivy.swing.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.event.*;
import java.util.List;
import java.io.*;


public class BuenoProjectDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String			project_name;
private List<String>		ref_projects;
private SwingListSet<PathEntry> library_paths;
private Set<PathEntry>		initial_paths;
private Map<String,String>	option_elements;
private Map<String,String>	start_options;
private File			last_directory;
private Map<String,Map<String,String>> option_sets;
private boolean 		optional_error;
private String			current_optionset;
private ProblemPanel		problem_panel;



private static int	dialog_placement = BudaConstants.PLACEMENT_RIGHT |
						BudaConstants.PLACEMENT_LOGICAL|
						BudaConstants.PLACEMENT_GROUPED;

private static String [] compiler_levels = new String[] {
   "1.3", "1.4", "1.5", "1.6", "1.7"
};

private static final String SOURCE_OPTION = "org.eclipse.jdt.core.compiler.source";
private static final String TARGET_OPTION = "org.eclipse.jdt.core.compiler.codegen.targetPlatform";
private static final String COMPLIANCE_OPTION = "org.eclipse.jdt.core.compiler.compliance";
private static final String ERROR_OPTION = "org.eclipse.jdt.core.compiler.problem.fatalOptionalError";




enum PathType {
   NONE,
   SOURCE,
   BINARY,
   LIBRARY
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoProjectDialog(String proj)
{
   project_name = proj;
   ref_projects = new ArrayList<String>();
   library_paths = new SwingListSet<PathEntry>(true);
   option_elements = new HashMap<String,String>();
   last_directory = null;
   option_sets = new HashMap<String,Map<String,String>>();
   initial_paths = new HashSet<PathEntry>();

   BoardProperties bp = BoardProperties.getProperties("Bueno");

   String ld = bp.getProperty("Bueno.library.directory");
   if (ld != null) last_directory = new File(ld);

   BumpClient bc = BumpClient.getBump();
   Element xml = bc.getProjectData(proj);
   if (xml == null) return;

   for (Element e : IvyXml.children(xml,"REFERENCES")) {
      String ref = IvyXml.getText(e);
      ref_projects.add(ref);
    }

   for (Element e : IvyXml.children(xml,"OPTION")) {
      String k = IvyXml.getAttrString(e,"NAME");
      String v = IvyXml.getAttrString(e,"VALUE");
      option_elements.put(k,v);
    }
   start_options = new HashMap<String,String>(option_elements);

   Element cxml = IvyXml.getChild(xml,"RAWPATH");
   for (Element e : IvyXml.children(cxml,"PATH")) {
      PathEntry pe = new PathEntry(e);
      if (!pe.isNested() && pe.getPathType() == PathType.LIBRARY) {
	 library_paths.addElement(pe);
	 initial_paths.add(pe);
       }
    }

   setupProblemOptions();
}




/********************************************************************************/
/*										*/
/*	Methods to create an editor bubble							    */
/*										*/
/********************************************************************************/

public BudaBubble createProjectEditor()
{
   SwingGridPanel pnl = new SwingGridPanel();

   JLabel lbl = new JLabel("Properties for Project " + project_name,JLabel.CENTER);
   pnl.addGBComponent(lbl,0,0,0,1,0,0);

   JTabbedPane tbp = new JTabbedPane(JTabbedPane.TOP);
   tbp.addTab("Libraries",new PathPanel());
   problem_panel = new ProblemPanel();
   tbp.addTab("Compiler",problem_panel);
   pnl.addGBComponent(tbp,0,1,0,1,1,1);

   Box bx = Box.createHorizontalBox();
   bx.add(Box.createHorizontalGlue());
   JButton apply = new JButton("Apply Changes");
   apply.addActionListener(new ProjectEditor());
   bx.add(apply);
   bx.add(Box.createHorizontalGlue());
   pnl.addGBComponent(bx,0,2,0,1,0,0);

   return new ProjectBubble(pnl);
}



private static class ProjectBubble extends BudaBubble {

   ProjectBubble(Component c) {
      setContentPane(c);
    }

}	// end of inner class ProjectBubble



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private void closeWindow(Component c)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
   if (bba != null) bba.removeBubble(bb);
}




/********************************************************************************/
/*										*/
/*	Path panel								*/
/*										*/
/********************************************************************************/

private class PathPanel extends SwingGridPanel implements ActionListener, ListSelectionListener {

   private JButton edit_button;
   private JButton delete_button;
   private JList   path_display;

   PathPanel() {
      int y = 0;
      JButton bn = new JButton("New Jar File");
      bn.addActionListener(this);
      addGBComponent(bn,1,y++,1,1,0,0);
      bn = new JButton("New Directory");
      bn.addActionListener(this);
      addGBComponent(bn,1,y++,1,1,0,0);
      // TODO: add JUNIT button
      edit_button = new JButton("Edit");
      edit_button.addActionListener(this);
      addGBComponent(edit_button,1,y++,1,1,0,0);
      delete_button = new JButton("Delete");
      delete_button.addActionListener(this);
      addGBComponent(delete_button,1,y++,1,1,0,0);
      ++y;

      path_display = new JList(library_paths);
      path_display.setVisibleRowCount(10);
      addGBComponent(new JScrollPane(path_display),0,0,1,y++,1,1);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("New Jar File")) {
	 askForNew(new FileNameExtensionFilter("Jar Files","jar"),JFileChooser.FILES_ONLY);
       }
      else if (cmd.equals("New Directory")) {
	 askForNew(new BinaryFileFilter(),JFileChooser.DIRECTORIES_ONLY);
       }
      else if (cmd.equals("Edit")) {
	 PathEntry pe = (PathEntry) path_display.getSelectedValue();
	 EditPathEntryBubble bb = new EditPathEntryBubble(pe);
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 BudaBubble rbb = BudaRoot.findBudaBubble(this);
	 bba.addBubble(bb,rbb,null,dialog_placement);
       }
      else if (cmd.equals("Delete")) {
	 for (Object o : path_display.getSelectedValues()) {
	    library_paths.removeElement((PathEntry) o);
	  }
       }
      else BoardLog.logE("BUENO","Unknown path panel command " + cmd);
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      updateButtons();
    }

   private void askForNew(FileFilter ff,int mode) {
      NewPathEntryBubble bb = new NewPathEntryBubble(ff,mode);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      BudaBubble rbb = BudaRoot.findBudaBubble(this);
      bba.addBubble(bb,rbb,null,dialog_placement);
    }

   private void updateButtons() {
      Object [] sels = path_display.getSelectedValues();
      boolean edok = false;
      for (Object sel : sels) {
	 PathEntry pe = (PathEntry) sel;
	 if (pe.getPathType() == PathType.LIBRARY) {
	    if (edok) {
	       edok = false;
	       break;
	     }
	    else edok = true;
	  }
       }
      edit_button.setEnabled(edok);
      delete_button.setEnabled(sels.length >= 1);
    }

}	// end of inner class PathPanel



/********************************************************************************/
/*										*/
/*	Methods for adding paths						*/
/*										*/
/********************************************************************************/

private class NewPathEntryBubble extends BudaBubble implements ActionListener {

   private JFileChooser file_chooser;

   NewPathEntryBubble(FileFilter ff,int mode) {
      file_chooser = new JFileChooser(last_directory);
      file_chooser.setMultiSelectionEnabled(true);
      file_chooser.addChoosableFileFilter(ff);
      file_chooser.setFileSelectionMode(mode);
      file_chooser.addActionListener(this);
      setContentPane(file_chooser);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      last_directory = file_chooser.getCurrentDirectory();
      if (cmd.equals(JFileChooser.APPROVE_SELECTION)) {
	 closeWindow(this);
	 for (File f : file_chooser.getSelectedFiles()) {
	    if (f.isDirectory() && !hasClassFiles(f)) continue;
	    PathEntry pe = new PathEntry(f);
	    library_paths.addElement(pe);
	  }
       }
      else if (cmd.equals(JFileChooser.CANCEL_SELECTION)) {
	 closeWindow(this);
       }
    }
}	// end of inner class NewPathEntryBubble



private static class BinaryFileFilter extends FileFilter {

   @Override public String getDescription()	{ return "Java Class File Directory"; }

   @Override public boolean accept(File f) {
      if (!f.isDirectory()) return false;

      return true;
    }



}	// end of inner class BinaryFileFilter


private boolean hasClassFiles(File f)
{
   if (f.isDirectory()) {
	 File [] fls = f.listFiles();
	 if (fls != null) {
	    for (File f1 : fls) {
	       if (hasClassFiles(f1)) return true;
	     }
	  }
    }
   else if (f.getName().endsWith(".class")) return true;

   return false;
 }




/********************************************************************************/
/*										*/
/*	Methods for path element editing					*/
/*										*/
/********************************************************************************/

private static class EditPathEntryBubble extends BudaBubble implements ActionListener {

   private PathEntry	for_path;

   EditPathEntryBubble(PathEntry pp) {
      for_path = pp;
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Edit Project Path Entry");
      switch (for_path.getPathType()) {
	 case LIBRARY :
	    pnl.addFileField("Library",for_path.getBinaryPath(),0,this,null);
	    pnl.addFileField("Source Attachment",for_path.getSourcePath(),0,this,null);
	    pnl.addFileField("Java Doc Attachment",for_path.getJavadocPath(),0,this,null);
	    break;
       }
      pnl.addBoolean("Exported",for_path.isExported(),this);
      pnl.addBoolean("Optional",for_path.isOptional(),this);

      setContentPane(pnl);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Library")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setBinaryPath(tf.getText());
       }
      else if (cmd.equals("Source Attachment")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setSourcePath(tf.getText());
       }
      else if (cmd.equals("Java Doc Attachment")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setJavadocPath(tf.getText());
       }
      else if (cmd.equals("Exported")) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 for_path.setExported(cbx.isSelected());
       }
      else if (cmd.equals("Optional")) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 for_path.setOptional(cbx.isSelected());
       }
    }

}	// end of inner class NewPathEntryBubble



/********************************************************************************/
/*										*/
/*	Path Information							*/
/*										*/
/********************************************************************************/

private static class PathEntry implements Comparable<PathEntry> {

   private PathType path_type;
   private String source_path;
   private String output_path;
   private String binary_path;
   private String javadoc_path;
   private boolean is_exported;
   private boolean is_optional;
   private boolean is_nested;
   private boolean is_new;
   private boolean is_modified;
   private int	   entry_id;

   PathEntry(Element e) {
      path_type = IvyXml.getAttrEnum(e,"TYPE",PathType.NONE);
      source_path = IvyXml.getTextElement(e,"SOURCE");
      output_path = IvyXml.getTextElement(e,"OUTPUT");
      binary_path = IvyXml.getTextElement(e,"BINARY");
      javadoc_path = IvyXml.getTextElement(e,"JAVADOC");
      entry_id = IvyXml.getAttrInt(e,"ID");
      is_exported = IvyXml.getAttrBool(e,"EXPORTED");
      is_nested = IvyXml.getAttrBool(e,"NESTED");
      is_optional = IvyXml.getAttrBool(e,"OPTIONAL");
      is_new = false;
      is_modified = false;
    }

   PathEntry(File f) {
      path_type = PathType.LIBRARY;
      source_path = null;
      output_path = null;
      binary_path = f.getPath();
      is_exported = false;
      is_optional = false;
      is_nested = false;
      is_new = true;
      is_modified = true;
      entry_id = 0;
    }

   boolean isNested()					{ return is_nested; }
   PathType getPathType()				{ return path_type; }
   String getBinaryPath()				{ return binary_path; }
   String getSourcePath()				{ return source_path; }
   String getJavadocPath()				{ return javadoc_path; }
   boolean isExported() 				{ return is_exported; }
   boolean isOptional() 				{ return is_optional; }
   boolean hasChanged() 				{ return is_new || is_modified; }

   void setBinaryPath(String p) {
      if (p == null || p.length() == 0 || p.equals(binary_path)) return;
      binary_path = p;
      is_modified = true;
    }

   void setSourcePath(String p) {
      if (p == null || p.length() == 0 || p.equals(source_path)) return;
      source_path = p;
      is_modified = true;
    }

   void setJavadocPath(String p) {
       if (p == null || p.length() == 0 || p.equals(javadoc_path)) return;
       javadoc_path = p;
       is_modified = true;
     }

   void setExported(boolean fg) {
      if (fg == is_exported) return;
      is_exported = fg;
      is_modified = true;
    }

   void setOptional(boolean fg) {
      if (fg == is_optional) return;
      is_optional = fg;
      is_modified = true;
    }

   void outputXml(IvyXmlWriter xw,boolean del) {
      xw.begin("PATH");
      if (del) xw.field("DELETE",true);
      if (entry_id != 0) xw.field("ID",entry_id);
      xw.field("TYPE",path_type);
      xw.field("NEW",is_new);
      xw.field("MODIFIED",is_modified);
      xw.field("EXPORTED",is_exported);
      xw.field("OPTIONAL",is_optional);
      if (source_path != null) xw.textElement("SOURCE",source_path);
      if (output_path != null) xw.textElement("OUTPUT",output_path);
      if (binary_path != null) xw.textElement("BINARY",binary_path);
      if (javadoc_path != null) xw.textElement("JAVADOC",javadoc_path);
      xw.end("PATH");
    }

   @Override public String toString() {
      switch (path_type) {
	 case LIBRARY :
	 case BINARY :
	    if (binary_path != null) {
	       File f = new File(binary_path);
	       return f.getName();
	     }
	    break;
	 case SOURCE :
	    if (source_path != null) {
	       File f = new File(source_path);
	       return f.getName() + " (SOURCE)";
	     }
	    break;
       }
      return path_type.toString() + " " + source_path + " " + output_path + " " + binary_path;
    }

   @Override public int compareTo(PathEntry pe) {
      return toString().compareTo(pe.toString());
    }

}	// end of inner class PathEntry



/********************************************************************************/
/*										*/
/*	Problem panel								*/
/*										*/
/********************************************************************************/

private void setupProblemOptions()
{
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   Map<Integer,String> opts = new TreeMap<Integer,String>();
   for (String s : bp.stringPropertyNames()) {
      if (s.startsWith("Bueno.problem.set.name.")) {
	 int idx = s.lastIndexOf(".");
	 try {
	    int v = Integer.parseInt(s.substring(idx+1));
	    opts.put(v,s);
	  }
	 catch (NumberFormatException e) { }
       }
    }
   for (String onm : opts.values()) {
      String nm = bp.getProperty(onm);
      String vnm = onm.replaceFirst(".name.",".data.");
      String vls = bp.getProperty(vnm);
      if (nm == null || vls == null) continue;
      Map<String,String> vmap = new HashMap<String,String>();
      for (StringTokenizer tok = new StringTokenizer(vls," \n\t,"); tok.hasMoreTokens(); ) {
	 String kv = tok.nextToken();
	 int idx = kv.indexOf("=");
	 if (idx < 0) continue;
	 String k = kv.substring(0,idx);
	 String v = kv.substring(idx+1);
	 vmap.put(k,v);
       }
      option_sets.put(nm,vmap);
    }

   optional_error = false;
   current_optionset = null;
   Map<String,String> usermap = new HashMap<String,String>();
   for (Map.Entry<String,String> ent : option_elements.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (k.equals(ERROR_OPTION)) {
	 optional_error = v.equals("enabled");
       }
      else if (k.startsWith("org.eclipse.jdt.core.compiler.problem.")) {
	 usermap.put(k,v);
       }
    }
   for (Map.Entry<String,Map<String,String>> ent : option_sets.entrySet()) {
      if (compatibleSets(usermap,ent.getValue())) {
	 if (current_optionset == null) current_optionset = ent.getKey();
       }
    }
   if (current_optionset == null) {
      current_optionset = "Current Settings";
      option_sets.put(current_optionset,usermap);
    }
}




private boolean compatibleSets(Map<String,String> umap,Map<String,String> kmap)
{
   Set<String> xtra = new HashSet<String>();
   boolean compat = true;

   for (Map.Entry<String,String> ent : umap.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (kmap.containsKey(k)) {
	 if (!kmap.get(k).equals(v))
	    compat = false;
       }
      else xtra.add(k);
    }

   for (String s : xtra) {
      kmap.put(s,umap.get(s));
    }

   return compat;
}



private class ProblemPanel extends SwingGridPanel implements ActionListener {

   private boolean needs_update;

   ProblemPanel() {
      needs_update = false;
      beginLayout();
      addBannerLabel("Compiler Problem Settings");
      addChoice("Option Set",option_sets.keySet(),current_optionset,this);
      addBoolean("Warnings as Errors",optional_error,this);
      addChoice("Java Source Version",compiler_levels,
	    option_elements.get(SOURCE_OPTION),this);
      addChoice("Java Target Version",compiler_levels,
	    option_elements.get(TARGET_OPTION),this);
      addChoice("Java Compliance Version",compiler_levels,
	       option_elements.get(COMPLIANCE_OPTION),this);
    }

   boolean needsUpdate()			{ return needs_update; }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Option Set")) {
	 JComboBox cbx = (JComboBox) evt.getSource();
	 String nopt = (String) cbx.getSelectedItem();
	 if (nopt == null || nopt.equals(current_optionset)) return;
	 current_optionset = nopt;
	 Map<String,String> oval = option_sets.get(nopt);
	 for (Map.Entry<String,String> ent : oval.entrySet()) {
	    option_elements.put(ent.getKey(),ent.getValue());
	  }
	 needs_update = true;
       }
      else if (cmd.equals("Warnings as Errors")) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 boolean fg = cbx.isSelected();
	 if (fg == optional_error) return;
	 optional_error = fg;
	 needs_update = true;
       }
      else if (cmd.equals("Java Source Version")) {
	 JComboBox cbx = (JComboBox) evt.getSource();
	 String nval = (String) cbx.getSelectedItem();
	 String oval = option_elements.get(SOURCE_OPTION);
	 if (nval == null || nval.equals(oval)) return;
	 option_elements.put(SOURCE_OPTION,nval);
	 needs_update = true;
       }
      else if (cmd.equals("Java Target Version")) {
	 JComboBox cbx = (JComboBox) evt.getSource();
	 String nval = (String) cbx.getSelectedItem();
	 String oval = option_elements.get(TARGET_OPTION);
	 if (nval == null || nval.equals(oval)) return;
	 option_elements.put(TARGET_OPTION,nval);
	 needs_update = true;
       }
      else if (cmd.equals("Java Compliance Version")) {
	 JComboBox cbx = (JComboBox) evt.getSource();
	 String nval = (String) cbx.getSelectedItem();
	 String oval = option_elements.get(COMPLIANCE_OPTION);
	 if (nval == null || nval.equals(oval)) return;
	 option_elements.put(COMPLIANCE_OPTION,nval);
	 needs_update = true;
       }
      else BoardLog.logE("BUENO","Unknown problem panel command " + cmd);
    }

   void outputXml(IvyXmlWriter xw) {
      for (Map.Entry<String,String> ent : option_elements.entrySet()) {
	 String k = ent.getKey();
	 String v = ent.getValue();
	 String ov = start_options.get(k);
	 if (v.equals(ov)) continue;
	 xw.begin("OPTION");
	 xw.field("NAME",k);
	 xw.field("VALUE",v);
	 xw.end("OPTION");
       }
    }

}	// end of inner class ProblemPanel



/********************************************************************************/
/*										*/
/*	<comment here>								*/
/*										*/
/********************************************************************************/

private boolean anythingChanged()
{
   for (PathEntry pe : library_paths) {
      if (pe.hasChanged()) return true;
    }
   if (problem_panel.needsUpdate()) return true;

   return false;
}



private class ProjectEditor implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      Set<PathEntry> dels = new HashSet<PathEntry>(initial_paths);

      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("PROJECT");
      xw.field("NAME",project_name);
      for (PathEntry pe : library_paths) {
	 pe.outputXml(xw,false);
	 dels.remove(pe);
       }
      for (PathEntry pe : dels) {
	 pe.outputXml(xw,true);
       }
      problem_panel.outputXml(xw);
      xw.end("PROJECT");

      closeWindow(problem_panel);

      BumpClient bc = BumpClient.getBump();

      if (anythingChanged())
	 bc.editProject(project_name,xw.toString());
    }

}	// end of inner class ProjectEditor




}	// end of class BuenoProjectDialog




/* end of BuenoProjectDialog.java */
