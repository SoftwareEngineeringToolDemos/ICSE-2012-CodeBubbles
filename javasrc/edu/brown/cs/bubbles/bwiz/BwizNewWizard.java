/********************************************************************************/
/*										*/
/*		BwizNewWizard.java						*/
/*										*/
/*	New class,enum,interface,method wizard implementation			*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 UCF -- Jared Bott				      */
/*	Copyright 2013 Brown University -- Annalia Sunderland		      */
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bwiz;

import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoBubbleCreator;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoKey;
import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;


abstract class BwizNewWizard  extends SwingGridPanel implements BwizConstants,
		BwizConstants.ISignatureUpdate,
		BwizConstants.IAccessibilityUpdatable
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected InfoStruct  info_structure;
private JTextField  main_name;	 //Class name, Method Name, Interface Name, or Enum Name
private JTextField  second_name; //Superclass or Return type, in case of Classes or Methods
private BwizAccessibilityPanel accessibility_panel;
private JTextField signature_area;
private BwizHoverButton create_button;
private BwizListEntryComponent list_panel; //Interfaces, or Parameters
private SwingComboBox<String> package_dropdown;
private JComboBox<String> project_dropdown;
protected BuenoLocation at_location;
protected BuenoBubbleCreator bubble_creator;

private static final String DEFAULT_PACKAGE = "<default package>";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizNewWizard()
{
   this(null,null,null);
}


// for creating methods
BwizNewWizard(String projectname, String packagename,String classname) {
   at_location = null;
   bubble_creator = null;
   //The data structure representing the structure
   info_structure = getInfoStruct();

   if (classname != null) info_structure.setClass(classname);
   if (projectname != null) info_structure.setProject(projectname);
   if (packagename != null) info_structure.setPackage(packagename);

   setup();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Component getFocus()				{ return main_name; }


void setInsertLocation(BuenoLocation ins)	{ at_location = ins; }

void setBubbleCreator(BuenoBubbleCreator bbc)	{ bubble_creator = bbc; }




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

protected abstract InfoStruct getInfoStruct();

protected int getAccessibilityInfo()      { return 0; }

protected IVerifier getVerifier()       { return new InterfaceVerifier(); }

protected abstract Creator getCreator();

protected abstract String getNameText();
protected abstract String getNameHoverText();
protected abstract String getSecondText();
protected abstract String getSecondHoverText();
protected abstract String getListText();
protected abstract String getListHoverText();

private void setup()
{
   setBackground(Color.WHITE);

   //The data structure representing the structure
   if (info_structure == null) info_structure = getInfoStruct();

   //The panel that has the name of the class/method/interface/enum
   JPanel namespanel = new JPanel();
   namespanel.setLayout(new BoxLayout(namespanel, BoxLayout.LINE_AXIS));
   namespanel.setOpaque(false);

   Accessibility default_access = Accessibility.DEFAULT;
   if ((getAccessibilityInfo() & SHOW_PRIVATE) != 0) default_access = Accessibility.PRIVATE;

   //second panel, used in Classes and Methods for either extends or return, respectively
   JPanel secondpanel = new JPanel(); 
   if (getSecondText() != null) {
      secondpanel.setLayout(new BoxLayout(secondpanel, BoxLayout.LINE_AXIS));
      secondpanel.setOpaque(false);
      secondpanel.setBorder(BorderFactory.createEmptyBorder(12,0,12,0));
      secondpanel.setBackground(Color.GRAY);
    }

   //check if method or interface/class/enum
   list_panel = new BwizListEntryComponent(getVerifier(),getListText());

   list_panel.setTitleFont(getRelativeFont(-3));
   list_panel.setTextFont(getRelativeFont(-3));

   //The panel that contains the accessibility radiobuttons and the abstract check box.
   JPanel boxespanel = new JPanel(new GridBagLayout());
   boxespanel.setOpaque(false);
   boxespanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   accessibility_panel = new BwizAccessibilityPanel(getAccessibilityInfo());
   accessibility_panel.addAccessibilityActionListener(new AccessibilityChange());
   accessibility_panel.addModifierListener(new ModifierChange());

   //A panel for the create button and class signature
   JPanel buttonpanel=new JPanel();
   buttonpanel.setLayout(new BoxLayout(buttonpanel, BoxLayout.LINE_AXIS));
   buttonpanel.setOpaque(false);
   buttonpanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   JLabel nameslabel = new JLabel("");
   nameslabel.setFont(BWIZ_FONT_SIZE_MAIN);
   nameslabel.setAlignmentX(Component.LEFT_ALIGNMENT);
   nameslabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);

   namespanel.add(nameslabel);
   namespanel.add(Box.createRigidArea(new Dimension(7, 0)));

   addRawComponent(getNameText(),namespanel);

   //Requires list of available projects
   /* sets up the list of projects and packages */
   if (info_structure.getProjectName() == null) {
      setupProject();
    }
   if (info_structure.getPackageName() == null) {
      setupPackage();
    }

   //adds components
   if (getSecondText() != null) {
      addRawComponent(getSecondText(),secondpanel);
    }

   addLabellessRawComponent("",list_panel);
   addLabellessRawComponent("",boxespanel);
   addLabellessRawComponent("",accessibility_panel);
   addLabellessRawComponent("",buttonpanel);

   //Construct the main_name textfield
   setupMainName();
   namespanel.add(main_name);

   //This is used to make certain UI elements the same height and to make other elements not change height
   Dimension enMax = new Dimension(Integer.MAX_VALUE,main_name.getPreferredSize().height);

   //check if class or method, superclass or return, respectively; sets up second panel label
   if (getSecondText() != null) {
      JLabel secondLabel = new JLabel();
      secondLabel.setFont(BWIZ_FONT_SIZE_MAIN);
      secondLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      secondLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
      secondpanel.add(secondLabel);
      secondpanel.add(Box.createRigidArea(new Dimension(7, 0)));
      //Construct second textfield
      setupSecondName();
      second_name.setMaximumSize(enMax);
      secondpanel.add(second_name);
    }

   //Adds handlers and sets heights
   setupListName(enMax);

   //Adds a handler for when a different radio button is selected
   setupAccessibilityButtons();

   //Creates the class signature area
   setupSignatureArea();

   //Creates a button for creating the class. Needs an action hooked up to the button.
   setupCreateButton();

   buttonpanel.add(signature_area);
   buttonpanel.add(Box.createRigidArea(new Dimension(5, 0)));
   buttonpanel.add(create_button);

   info_structure.setAccess(default_access);
}








/********************************************************************************/
/*										*/
/*	Handle Projects 							*/
/*										*/
/********************************************************************************/

private void setupProject() {
   List<String> items = getProjects();
   ChooseProject ca = new ChooseProject();
   project_dropdown = addChoice("Project",items,0,ca);
   ca.set();
}


private List<String> getProjects()
{
   List<String> rslt = new ArrayList<String>();
   BumpClient bc = BumpClient.getBump();
   Element e = bc.getAllProjects();
   for (Element pe : IvyXml.children(e,"PROJECT")) {
      String nm = IvyXml.getAttrString(pe,"NAME");
      rslt.add(nm);
    }
   return rslt;
}



private class ChooseProject implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd==null);
      else set();
      if (package_dropdown != null) setupPackage();
    }

   void set() {
      if (project_dropdown != null) {
         String proj = project_dropdown.getSelectedItem().toString();
         info_structure.setProject(proj);
      }
    }

}	// end of inner class ChooseProject




/********************************************************************************/
/*										*/
/*	Handle Packages 							*/
/*										*/
/********************************************************************************/

private void setupPackage() {
   if (package_dropdown == null) {
      ChoosePackage ca = new ChoosePackage();
      package_dropdown = addChoice("Package",new String [] { },0,true,ca);
      ca.set();
    }
   PackageFinder pf = new PackageFinder();
   BoardThreadPool.start(pf);
}





private List<String> getPackages()
{
   Set<String> rslt = new TreeSet<String>();
   BumpClient bc = BumpClient.getBump();
   String proj = info_structure.getProjectName();
   List<BumpLocation> locs = bc.findTypes(proj,"*");

   if (locs != null) {
      Set<String> cls = new HashSet<String>();
      for (BumpLocation bl : locs) {
	 cls.add(bl.getSymbolName());
       }
      for (BumpLocation bl : locs) {
	 String nm = bl.getSymbolName();
	 int idx = nm.lastIndexOf(".");
	 String cnm = null;
	 if (idx > 0) {
	    cnm = nm.substring(0,idx);
	  }
	 else cnm = DEFAULT_PACKAGE;
	 if (cls.contains(cnm)) continue;
	 rslt.add(cnm);
       }
    }
   return new ArrayList<String>(rslt);
}



private class PackageFinder implements Runnable {
   
   private List<String> all_packages;
   PackageFinder() {
      all_packages = null;
    }
   
   @Override public void run() {
      if (all_packages == null) {
         all_packages = getPackages();
         SwingUtilities.invokeLater(this);
       }
      else if (package_dropdown != null) {
         package_dropdown.setContents(all_packages);
         if (all_packages.size() > 0) {
            package_dropdown.setSelectedIndex(0);
          }
       }
    }
   
}       // end of inner class PackageFinder



private class ChoosePackage implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd==null);
      else set();
    }

   void set() {
      if (package_dropdown != null && package_dropdown.getSelectedItem() != null) {
	 String pkg = package_dropdown.getSelectedItem().toString();
	 info_structure.setPackage(pkg);
      }
   }

}	// end of inner class ChoosePackage



/********************************************************************************/
/*										*/
/*	Handle name areas							*/
/*										*/
/********************************************************************************/

private void setupMainName()
{
   //Creates a textfield with the default styling
   main_name = BwizFocusTextField.getStyledField("Enter " + getNameText(), getNameHoverText());
   //Sets the font size
   main_name.setFont(getRelativeFont(-2));
   main_name.setAlignmentX(Component.LEFT_ALIGNMENT);
   main_name.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Adds a handler for when the user is typing
   main_name.getDocument().addDocumentListener(new TextListener(info_structure.getMainName()));
   // main_name.setMaximumSize(main_name.getPreferredSize());
}




private void setupSignatureArea()
{
   //Creates a textfield that selects all text when it gets focus
   signature_area = new BwizFocusTextField("signature");

   //Styling
   signature_area.setEditable(false);
   signature_area.setFont(getRelativeFont(-6));
   signature_area.setForeground(Color.GRAY);
   signature_area.setOpaque(false);
   signature_area.setBorder(BorderFactory.createEmptyBorder());
   signature_area.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   Dimension d=new Dimension(Integer.MAX_VALUE, signature_area.getPreferredSize().height);
   signature_area.setMaximumSize(d);

}



private void setupSecondName()
{
   //Creates a textfield with the default styling
   second_name = BwizFocusTextField.getStyledField("", getSecondHoverText());
   second_name.setFont(getRelativeFont(-2));
   second_name.setAlignmentX(Component.LEFT_ALIGNMENT);
   second_name.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Adds a handler for when the user is typing
   second_name.getDocument().addDocumentListener(new TextListener(info_structure.getSecondInfo()));
}



private void setupListName(Dimension enMax)
{
   list_panel.setHeight(main_name.getPreferredSize().height);
   list_panel.setHoverText(getListHoverText());
   list_panel.addItemChangeEventListener(new ClassItemListener());
}



private void setupAccessibilityButtons()
{
   //Adds a handler to when the radiobutton selection changes
   accessibility_panel.addAccessibilityActionListener(new AccessibilityChange());
}



private void setupCreateButton()
{
   //Creates a button that changes cover when the mouse is over it
   create_button = new BwizHoverButton("Create", Color.BLACK, Color.RED);
   //Styling
   create_button.setFont(getRelativeFont(4));
   create_button.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   create_button.setMaximumSize(create_button.getPreferredSize());
   create_button.setEnabled(false);

   create_button.addActionListener(getCreator());
}



public void updateSignature()
{
   //Changes the class signature
   signature_area.setText(info_structure.getSignature());
   signature_area.moveCaretPosition(0);
}



public void updateAccessibility(ActionEvent e)
{
   //Sets what radiobutton is selected in the backing data structure
   String command = e.getActionCommand();
   Accessibility a = Accessibility.fromString(command);

   info_structure.setAccess(a);
}



private Font getRelativeFont(int x)
{
   //Uses deriveFont to derive a new font based on MAIN font given an int deviation
   int size = BWIZ_FONT_SIZE_MAIN.getSize();
   int newsize = size + x;
   float f = newsize;
   if (newsize<18) {
      return BWIZ_FONT_SIZE_MAIN.deriveFont(1,f);
    }
   return BWIZ_FONT_SIZE_MAIN.deriveFont(0,f);
}



/********************************************************************************/
/*										*/
/*	Structure information holder						*/
/*										*/
/********************************************************************************/

protected abstract class InfoStruct
{
   private StringBuffer m_name;
   private Accessibility m_access;
   private String m_package;
   private String m_project;
   private List<String> m_set;
   private StringBuffer m_second_info;
   private boolean is_abstract;
   private boolean is_final;
   private boolean is_valid;
   private String m_class;		// what class this is located in
 
   InfoStruct() {
      m_name = new StringBuffer();
      m_access = Accessibility.PUBLIC;
      m_package = null;
      m_project = null;
      m_set = new ArrayList<String>();
      m_second_info = new StringBuffer();
      is_abstract = false;
      is_final = false;
      is_valid = false;
      m_class = null;
    }

   List<String> getSet()		{ return m_set; }
   String getPackageName()		{ return m_package; }
   String getProjectName()		{ return m_project; }
   String getClassName()		{ return m_class; }

   StringBuffer getMainName()		{ return m_name; }
   StringBuffer getSecondInfo() 	{ return m_second_info; }

   void setPackage(String p) {
      if (p != null && p.equals(DEFAULT_PACKAGE)) p = null;
      m_package = p;
    }

   void setProject(String p)		{ m_project = p; }
   void setClass(String c)		{ m_class = c; }
   void setAbstract(boolean fg) 	{ is_abstract = fg; }
   void setFinal(boolean fg)		{ is_final = fg; }
   void setAccess(Accessibility a)	{ m_access = a; }
   void setSet(Collection<String> vals) { m_set = new ArrayList<String>(vals); }

   int getModifiers() {
      int rslt = 0;
      switch (m_access) {
	 case DEFAULT :
	    break;
	 case PUBLIC :
	    rslt |= Modifier.PUBLIC;
	    break;
	 case PRIVATE :
	    rslt |= Modifier.PRIVATE;
	    break;
	 case PROTECTED :
	    rslt |= Modifier.PROTECTED;
       }
      if (is_abstract) rslt |= Modifier.ABSTRACT;
      if (is_final) rslt |= Modifier.FINAL;
      return rslt;
   }

   String getSignature() {
      if (!acceptableName(m_name.toString())) {
         updateCreateButton(false);
         return null;
       }
      if (!initialCheck()) {
         updateCreateButton(false);
         return null;
       }
   
      String prefix = "";
      //Access
      if (m_access.toString().compareTo("default")!=0) {
         prefix = m_access.toString();
       }
   
      //cannot be both abstract and final
      if (is_abstract && is_final) return null;
      if (is_abstract) prefix += " abstract";
      if (is_final) prefix += " final";
   
      String items = null;
      int setsize = m_set.size();
      if (setsize > 0) {
         for (int count=0; count < setsize; ++count) {
            String itm = m_set.get(count).toString().trim();
            if (count == 0) items = itm;
            else items += getSetSeparator() + itm;
          }
       }
      
      String temp = getSignature(prefix,items);
      temp = temp.trim();
      
      if (temp.equals("")) {
         temp = null;
         updateCreateButton(false);
       }
      else if (!validate()) {
         updateCreateButton(false);
       }
      else {
         updateCreateButton(true);
       }
   
      return temp;
    }
   
   protected boolean initialCheck() {
      if (getSecondText() != null) {
         String snm = m_second_info.toString();
         if (!acceptableName(snm)) return false;
       }
      return true;
    }
   
   protected String getSetSeparator()           { return ", "; }
   
   protected abstract String getSignature(String pfx,String itms);

   boolean isValid() {
      return is_valid;
    }

   boolean validate() {
      BumpClient bc = BumpClient.getBump();
      String fnm = m_name.toString();
      if (m_package != null) fnm = m_package + "." + fnm;
      List<BumpLocation> locs = bc.findClassDefinition(m_project,fnm);
      if (locs != null && locs.size() > 0) return false;
      
      if (list_panel != null) {
         if (list_panel.isActive()) return false;
       }
      
      return true;
    }

   //in which the create button is set to be clickable or not
   private void updateCreateButton(boolean valid) {
      is_valid = valid;
      if (valid) {
         create_button.setEnabled(true);
       }
      else {
         create_button.setEnabled(false);
       }
    }

   //checks whether the String is an acceptable class,enum,or interface name
   protected boolean acceptableName(String name) {
      if (name.length() == 0) return false;
      BwizParser parser = new BwizParser();
      return parser.acceptableName(name);
    }

}	 // end of inner class InfoStruct




/********************************************************************************/
/*										*/
/*	String Parser for Bwiz							*/
/*										*/
/********************************************************************************/

private static class BwizParser {

   protected int nextToken(StreamTokenizer stok) {
      try {
	 return stok.nextToken();
       }
      catch (IOException e) {
	 return StreamTokenizer.TT_EOF;
       }
    }

   protected boolean checkNextToken(StreamTokenizer stok,String tok) {
      if (nextToken(stok) == StreamTokenizer.TT_WORD && stok.sval.equals(tok)) {
	 return true;
       }
      stok.pushBack();
      return false;
    }

   protected boolean checkNextToken(StreamTokenizer stok,char tok) {
      if (nextToken(stok) == tok) return true;
      stok.pushBack();
      return false;
    }

   protected boolean acceptableName(String input) {
      StreamTokenizer tok = new StreamTokenizer(new StringReader(input));

      //check if a name even exists
      if (nextToken(tok) != StreamTokenizer.TT_WORD)
	 return false;

      //check if generic type follows
      try {
	 parseGenerics(tok);
	 parseEnd(tok);
       }
      catch (BwizException e) {
	 // System.out.println(e);
	 return false;
       }

      return true;
    }

   //generic type parsing
   protected void parseGenerics(StreamTokenizer tok) throws BwizException {
      if (!checkNextToken(tok,'<')) return;
      parseType(tok);
      while (true) {
	 if (checkNextToken(tok,',')) {
	    parseType(tok);
	  }
	 else if (checkNextToken(tok,'>')) {
	    break;
	  }
	 else {
	    throw new BwizException("Unclosed generic specification");
	  }
       }
    }


   protected String parseType(StreamTokenizer stok) throws BwizException {
      String rslt = null;
      if (checkNextToken(stok,"byte") || checkNextToken(stok,"short") ||
	     checkNextToken(stok,"int") || checkNextToken(stok,"long") ||
	     checkNextToken(stok,"char") || checkNextToken(stok,"float") ||
	     checkNextToken(stok,"double") || checkNextToken(stok,"boolean") ||
	     checkNextToken(stok,"void")) {
	 rslt = stok.sval;
       }
      else if (checkNextToken(stok,'?')) {
	 rslt = "?";
	 if (nextToken(stok) != StreamTokenizer.TT_WORD) {
	    stok.pushBack();
	  }
	 else if (checkNextToken(stok,"extends") || checkNextToken(stok,"super")) {
	    String ext = stok.sval;
	    String ntyp = null;
	    try {
	       ntyp = parseType(stok);
	     }
	    catch (BwizException e) {
	       // System.out.println(e);
	     }
	    rslt = rslt + " " + ext + " " + ntyp;
	  }
	 else {
	    stok.pushBack();
	  }
       }
      else if (nextToken(stok) == StreamTokenizer.TT_WORD) {
	 String tnam = stok.sval;
	 for ( ; ; ) {
	    if (!checkNextToken(stok,'.')) break;
	    if (nextToken(stok) != StreamTokenizer.TT_WORD)
	       throw new BwizException("Illegal qualified name");
	    tnam += "." + stok.sval;
	  }
	 rslt = tnam;
       }
      else throw new BwizException("Type expected");

      if (checkNextToken(stok,'<')) {
	 String ptyp = null;
	 for ( ; ; ) {
	    String atyp = parseType(stok);
	    if (ptyp == null) ptyp = atyp;
	    else ptyp += "," + atyp;
	    if (checkNextToken(stok,'>')) break;
	    else if (!checkNextToken(stok,',')) throw new BwizException("Bad parameterized argument");
	  }
	 if (ptyp == null) throw new BwizException("Parameterized type list missing");
	 rslt += "<" + ptyp + ">";
       }

      while (checkNextToken(stok,'[')) {
	 if (!checkNextToken(stok,']')) throw new BwizException("Missing right bracket");
	 rslt += "[]";
       }

      return rslt;
    }

   protected void parseEnd(StreamTokenizer stok) throws BwizException {
      if (nextToken(stok) != StreamTokenizer.TT_EOF) throw new BwizException("Excess at end");
    }

}	// end of inner class BwizParser



/********************************************************************************/
/*										*/
/*	Interface verifier							*/
/*										*/
/********************************************************************************/

private static class InterfaceVerifier implements BwizConstants.IVerifier
{
   //Checks if is a String of valid interface(s) (separated by commas if more than one)
   @Override public boolean verify(String test) {
      List<String> rslt = results(test);
      return rslt.size() > 0;
    }

    @Override public List<String> results(String test) {
      List<String> rslt = new ArrayList<String>();
      try {
	 StreamTokenizer tok = new StreamTokenizer(new StringReader(test));
	 BwizParser parser = new BwizParser();
	 for ( ; ; ) {
	    String typ = parser.parseType(tok);
	    rslt.add(typ);
	    if (!parser.checkNextToken(tok,',')) break;
	  }
       }
      catch (BwizException e) {
	 System.out.println(e);
       }
      return rslt;
     }

}	// end of inner class InterfaceVerifier




/********************************************************************************/
/*										*/
/*	Parameter verifier							*/
/*										*/
/********************************************************************************/

protected static class ParameterVerifier implements BwizConstants.IVerifier
{
   //Checks if is a String of valid parameter(s) (separated by commas if more than one)
   @Override public boolean verify(String test) {
      List<String> rslts = results(test);
      return rslts.size() > 0;
    }

   @Override public List<String> results(String test) {
      List<String> rslt = new ArrayList<String>();
      try {
	 StreamTokenizer tok = new StreamTokenizer(new StringReader(test));
	 BwizParser parser = new BwizParser();
	 for ( ; ; ) {
	    String typ = parser.parseType(tok);
	    String typ_two = parser.parseType(tok);
	    rslt.add(typ + " " + typ_two);
	    if (!parser.checkNextToken(tok,',')) break;
	 }
      }
      catch (BwizException e) {
	 // System.out.println(e);
       }
      return rslt;
    }

}	// end of inner class ParameterVerifier




/********************************************************************************/
/*										*/
/*	Item change listener							*/
/*										*/
/********************************************************************************/

private class ClassItemListener implements ItemChangeListener {

   @Override public void itemAdded(String item) {
      info_structure.setSet(list_panel.getListElements());
      updateSignature();
    }

   @Override public void itemRemoved(String item) {
      info_structure.setSet(list_panel.getListElements());
      updateSignature();
    }

}	// end of inner class ClassItemListener



/********************************************************************************/
/*										*/
/*	Accessility selection listener						*/
/*										*/
/********************************************************************************/

private class AccessibilityChange implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      updateAccessibility(e);
      updateSignature();
    }

}	// end of inner class Accessibility Change



/********************************************************************************/
/*										*/
/*	Abstract selection listener						*/
/*										*/
/********************************************************************************/

private class ModifierChange implements ItemListener {

   @Override public void itemStateChanged(ItemEvent e) {
      JCheckBox cbx = (JCheckBox) e.getItem();
      String cmd = cbx.getActionCommand();
      if (cmd.equals("abstract")) {
	 info_structure.setAbstract(e.getStateChange() == ItemEvent.SELECTED);
       }
      else if (cmd.equals("final")) {
	 info_structure.setFinal(e.getStateChange() == ItemEvent.SELECTED);
       }

      updateSignature();
    }

}	// end of inner class ModifierChange




/********************************************************************************/
/*										*/
/*	Action handler for doing the creation					*/
/*										*/
/********************************************************************************/

protected abstract class Creator implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      if (!info_structure.isValid()) return;
   
      BuenoProperties bp = new BuenoProperties();
      BudaBubble bbl = BudaRoot.findBudaBubble(BwizNewWizard.this);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
      Rectangle r = BudaRoot.findBudaLocation(bbl);
      Point pt = r.getLocation();
      bba.removeBubble(bbl);
   
      String pkg = info_structure.getPackageName();
      String proj = info_structure.getProjectName();
      String cls = info_structure.getMainName().toString();
      String fcls = (pkg == null ? cls : pkg + "." + cls);
   
      if (pkg != null) bp.put(BuenoKey.KEY_PACKAGE, pkg);
      bp.put(BuenoKey.KEY_PROJECT, proj);
      bp.put(BuenoKey.KEY_NAME, cls);
      bp.put(BuenoKey.KEY_MODIFIERS,info_structure.getModifiers());
   
      BudaBubble nbbl = doCreate(bba,pt,fcls,bp);
   
      if (nbbl != null) {
         bba.add(nbbl,new BudaConstraint(pt));
      }
   }
   
   abstract protected BudaBubble doCreate(BudaBubbleArea bba,Point pt,String nm,BuenoProperties bp);

}	// end of inner class Creator



/********************************************************************************/
/*										*/
/*	Handle text changes							*/
/*										*/
/********************************************************************************/

private class TextListener implements DocumentListener {

   private StringBuffer text_to_update;

   TextListener(StringBuffer buf) {
      text_to_update = buf;
    }

   @Override public void insertUpdate(DocumentEvent e) {
      updateAll(e);
    }


   @Override public void removeUpdate(DocumentEvent e) {
      updateAll(e);
    }

   @Override public void changedUpdate(DocumentEvent e) { }

   private void updateAll(DocumentEvent e) {
      try {
	 Document doc = e.getDocument();
	 String data = doc.getText(0, doc.getLength());

	 if (data != "") {
	    text_to_update.delete(0, text_to_update.length());
	    text_to_update.append(data);
	  }

	 updateSignature();
       }
      catch (BadLocationException ex) { }
    }

}	// end of inner class TextListener



}	// end of class BwizClassWizard




/* end of BwizClassWizard.java */
