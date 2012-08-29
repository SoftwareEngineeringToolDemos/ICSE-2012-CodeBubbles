/********************************************************************************/
/*										*/
/*		BuenoPythonProject.java 					*/
/*										*/
/*	Python project creation/editing dialogs and actions			*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.swing.*;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.*;

import java.io.File;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;


public class BuenoPythonProject implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private File		project_dir;
private Vector<PathData>   project_paths;
private Map<String,OptionData> project_options;



/********************************************************************************/
/*										*/
/*	Static entries								*/
/*										*/
/********************************************************************************/

public static BudaBubble createNewPythonProjectBubble()
{
   BuenoPythonProject pp = new BuenoPythonProject();
   JPanel pc = pp.getProjectCreator();
   if (pc == null) return null;

   return new BuenoPythonBubble(pc);
}



public static BudaBubble createEditPythonProjectBubble(String proj)
{
   BuenoPythonProject pp = null;
   try {
      pp = new BuenoPythonProject(proj);
    }
   catch (BuenoException e) {
      return null;
    }
   JComponent pc = pp.getProjectEditor();
   if (pc == null) return null;

   return new BuenoPythonBubble(pc);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BuenoPythonProject()
{
   project_name = null;
   project_dir = null;
   project_paths = new Vector<PathData>();
   project_options = new HashMap<String,OptionData>();
}


private BuenoPythonProject(String nm) throws BuenoException
{
   this();
   project_name = nm;
   Element xml = BumpClient.getBump().getProjectData(nm);
   if (xml == null) throw new BuenoException("Project " + nm + " not defined");
   project_dir = new File(IvyXml.getAttrString(xml,"PATH"));
   for (Element pelt : IvyXml.children(xml,"PATH")) {
      PathData pd = new PathData(pelt);
      project_paths.add(pd);
    }
   Element prefs = IvyXml.getChild(xml,"PREFERENCES");
   for (Element pref : IvyXml.children(prefs)) {
      OptionData od = new OptionData(pref);
      project_options.put(od.getKey(),od);
    }
}



/********************************************************************************/
/*										*/
/*	Initial project creation dialog 					*/
/*										*/
/********************************************************************************/

JPanel getProjectCreator()
{
   return new ProjectCreator();
}


private void createProject()
{
   BumpClient bc = BumpClient.getBump();
   if (project_dir == null) {
      File f1 = new File(BoardSetup.getSetup().getDefaultWorkspace());
      project_dir = new File(f1,project_name);
    }

   bc.createProject(project_name,project_dir);
}


private void updateProject()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("PATH",project_dir.getPath());
   for (OptionData od : project_options.values()) {
      xw.begin("OPTION");
      xw.field("KEY",od.getKey());
      xw.field("VALUE",od.getValue());
      xw.end("OPTION");
    }
   for (PathData pd : project_paths) {
      xw.begin("PATH");
      if (!pd.isLibrary()) {
         xw.field("USER",true);
       }
      xw.field("DIRECTORY",pd.getDirectory().getPath());
      xw.end("PATH");
    }
   xw.end("PROJECT");
   
   BumpClient bc = BumpClient.getBump();
   bc.editProject(project_name,xw.toString());
   xw.close();
}



private class ProjectCreator extends SwingGridPanel implements ActionListener, UndoableEditListener {

   private JTextField name_field;
   private JTextField file_field;
   private JTextField source_field;

   ProjectCreator() {
      beginLayout();
      addBannerLabel("Create PYBLES Python Project");
      name_field = addTextField("Name",null,this,this);
      file_field = addFileField("Project Directory",((File) null),JFileChooser.DIRECTORIES_ONLY,this,null);
      source_field = addFileField("External Source",((File) null),JFileChooser.DIRECTORIES_ONLY,this,null);
      addSeparator();
      addBottomButton("CANCEL","CANCEL",this);
      addBottomButton("Create","CREATE",this);
      addBottomButtons();
    }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("CANCEL")) {
         removeBubble();
       }
      else if (cmd.equals("CREATE")) {
         project_name = name_field.getText();
         String dnm = file_field.getText();
         if (dnm != null) project_dir = new File(dnm);
         removeBubble();
         
         createProject();
        
         String sdir = source_field.getText();
         File sfil = null;
         if (sdir == null) {
            sfil = new File(project_dir,"src");
          }
         else {
            sfil = new File(sdir);
          }
         if (sfil != null) {
            PathData pd = new PathData(sfil,false);
            project_paths.add(pd);
          }
         
         updateProject();
       }
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      if (e.getSource() == name_field) {
         String txt = name_field.getText();
         file_field.setText(txt);
       }
    }

   private void removeBubble() {
      BudaBubble bb = BudaRoot.findBudaBubble(this);
      if (bb != null) bb.setVisible(false);
    }

}	// end of inner class ProjectCreator




/********************************************************************************/
/*										*/
/*	Project editing methods 						*/
/*										*/
/********************************************************************************/

JComponent getProjectEditor()
{
   return new ProjectEditor();
}



private class ProjectEditor extends SwingGridPanel implements ActionListener {

   ProjectEditor() {
      addBannerLabel("Edit PYBLES Python Project " + project_name);
      JTabbedPane tabs = new JTabbedPane();
      addLabellessRawComponent("TABS",tabs);
      PackagePanel ppnl = new PackagePanel();
      tabs.addTab("Packages",ppnl);
      OptionPanel opnl = new OptionPanel();
      tabs.addTab("Options",opnl);
    }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("CANCEL")) {
	 removeBubble();
       }
      else if (cmd.equals("ACCEPT")) {
         updateProject();
       }
    }

   private void removeBubble() {
      BudaBubble bb = BudaRoot.findBudaBubble(this);
      if (bb != null) bb.setVisible(false);
    }

}	// end of inner class ProjectEditor





/********************************************************************************/
/*										*/
/*	Path/package editing display						*/
/*										*/
/********************************************************************************/

private class PackagePanel extends SwingGridPanel implements ActionListener, ListSelectionListener {

   private JButton edit_button;
   private JButton delete_button;
   private JList   path_display;

   PackagePanel() {
      int y = 0;
      JButton bn = new JButton("New Package Directory");
      addGBComponent(bn,1,y++,1,1,0,0);
      edit_button = new JButton("Edit");
      edit_button.addActionListener(this);
      addGBComponent(edit_button,1,y++,1,1,0,0);
      delete_button = new JButton("Delete");
      delete_button.addActionListener(this);
      addGBComponent(delete_button,1,y++,1,1,0,0);
      ++y;

      path_display = new JList(project_paths);
      path_display.setVisibleRowCount(5);
      addGBComponent(new JScrollPane(path_display),0,0,1,y++,1,1);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("New Package Directory")) {
	 // handle new directory
       }
      else if (cmd.equals("Edit")) {
	 // handle editing path
       }
      else if (cmd.equals("Delete")) {
	 // handle deleting paths
       }
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      updateButtons();
    }

   private void updateButtons() {
      Object [] sels = path_display.getSelectedValues();
      boolean edok = false;
      for (Object sel : sels) {
	 PathData pe = (PathData) sel;
	 if (pe.isLibrary()) {
	    if (edok) {
	       edok = false;
	       break;
	     }
	    edok = true;
	  }
       }
      edit_button.setEnabled(edok);
      delete_button.setEnabled(sels.length >= 1);
    }

}	// end of inner class PackagePanel




/********************************************************************************/
/*										*/
/*	Option editing display							*/
/*										*/
/********************************************************************************/

private static Map<String,String> error_descriptions;
private static String [] severity_set = new String [] { "IGNORE", "INFO", "WARNING", "ERROR" };

static {
   error_descriptions = new LinkedHashMap<String,String>();
   error_descriptions.put("ASSSIGNMENT_TO_BUILT_IN_SYMBOL","Assignment to a built in name");
   error_descriptions.put("DUPLICATED_SIGNATURE","Duplicate function/method signature");
   error_descriptions.put("INDENTATION_PROBLEM","Problem with indentation");
   error_descriptions.put("NO_EFFECT_STATEMENT","Statement with no effect");
   error_descriptions.put("NO_SELF","Self should be the first parameter");
   error_descriptions.put("REIMPORT","Import redefinition");
   error_descriptions.put("UNDEFINED_IMPORT_VARIABLE","Undefined variable from import");
   error_descriptions.put("UNDEFINED_VARIABLE","Undefined variable");
   error_descriptions.put("UNUSED_IMPORT","Unused import");
   error_descriptions.put("UNUSED_PARAMETER","Unused parameter");
   error_descriptions.put("UNUSED_VARIABLE","Unused variable");
   error_descriptions.put("UNUSED_WILD_IMPORT","Unused in wild import");
   error_descriptions.put("SYNTAX_ERROR","Syntax error");
}


private class OptionPanel extends SwingGridPanel implements ActionListener {

   OptionPanel() {
      addSectionLabel("Error Settings");
      for (Map.Entry<String,String> ent : error_descriptions.entrySet()) {
	 OptionData od = project_options.get(ent.getKey());
	 if (od != null) {
	    addChoice(ent.getValue(),severity_set,od.getValue(),this);
	  }
       }
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String what = evt.getActionCommand();
      for (Map.Entry<String,String> ent : error_descriptions.entrySet()) {
	 if (ent.getValue().equals(what)) {
	    // process option
	  }
       }
    }

}	// end of inner class OptionPanel




/********************************************************************************/
/*										*/
/*	Bubble container							*/
/*										*/
/********************************************************************************/

private static class BuenoPythonBubble extends BudaBubble {

   BuenoPythonBubble(JComponent pnl) {
      setContentPane(pnl,null);
    }

}	// end of inner class BuenoPythonBubble


/********************************************************************************/
/*										*/
/*	Path Data								*/
/*										*/
/********************************************************************************/

private static class PathData {

   private File path_directory;
   private boolean is_library;

   PathData(Element xml) {
      path_directory = new File(IvyXml.getAttrString(xml,"DIR"));
      is_library = false;
    }
   
   PathData(File dir,boolean lib) {
      path_directory = dir;
      is_library = false;
    }
    
    

   boolean isLibrary()			{ return is_library; }
   File getDirectory()                  { return path_directory; }
   
   @Override public String toString() {
      return path_directory.toString();
    }

}	// end of inner class PathData




/********************************************************************************/
/*										*/
/*	Option Data								*/
/*										*/
/********************************************************************************/

private static class OptionData {

   private String option_name;
   private String option_value;

   OptionData(Element xml) {
      if (IvyXml.isElement(xml,"SEVERITY")) {
	 option_name = IvyXml.getAttrString(xml,"TYPE");
	 option_value = IvyXml.getAttrString(xml,"VALUE");
       }
    }

   String getKey()				{ return option_name; }
   String getValue()				{ return option_value; }

}	// end of inner class OptionData;


}	// end of class BuenoPythonProject




/* end of BuenoPythonProject.java */

