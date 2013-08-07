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

import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoType;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoKey;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoBubbleCreator;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bale.*;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;


public class BwizNewWizard  extends SwingGridPanel implements BwizConstants,
		BwizConstants.ISignatureUpdate,
		BwizConstants.IAccessibilityUpdatable
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private CreateType create_type;

private InfoStruct  info_structure;
private JTextField  main_name;	 //Class name, Method Name, Interface Name, or Enum Name
private JTextField  second_name; //Superclass or Return type, in case of Classes or Methods
private BwizAccessibilityPanel accessibility_panel;
private JTextField signature_area;
private static BwizHoverButton create_button;
private BwizListEntryComponent list_panel; //Interfaces, or Parameters
private JComboBox<String> package_dropdown;
private JComboBox<String> project_dropdown;
private BuenoLocation at_location;
private BuenoBubbleCreator bubble_creator;

private static String name_text;
private static String name_hover_text;
private static String second_text;
private static String second_hover_text;
private static String list_text;
private static String list_hover_text;

private static final String DEFAULT_PACKAGE = "<default package>";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizNewWizard(CreateType type)
{
   create_type = type;
   at_location = null;
   bubble_creator = null;
   info_structure = new InfoStruct(create_type);

   setup();
}


// for creating methods
BwizNewWizard(CreateType type, String classname, String projectname, String packagename) {
   create_type = type;
   at_location = null;
   bubble_creator = null;
   //The data structure representing the structure
   info_structure = new InfoStruct(create_type);

   if (type==CreateType.METHOD) {
      info_structure.setClass(classname);
      info_structure.setProject(projectname);
      info_structure.setPackage(packagename);
    }

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

private void setup()
{
   setBackground(Color.WHITE);

   //The data structure representing the structure
   if (info_structure == null) info_structure = new InfoStruct(create_type);

   //The panel that has the name of the class/method/interface/enum
   JPanel namespanel = new JPanel();
   namespanel.setLayout(new BoxLayout(namespanel, BoxLayout.LINE_AXIS));
   namespanel.setOpaque(false);

   Accessibility default_access = Accessibility.DEFAULT;
   if (create_type == CreateType.METHOD) default_access = Accessibility.PRIVATE;

   setupTexts();

   //second panel, used in Classes and Methods for either extends or return, respectively
   JPanel secondpanel = new JPanel();
   if (second_text != null) {
      secondpanel.setLayout(new BoxLayout(secondpanel, BoxLayout.LINE_AXIS));
      secondpanel.setOpaque(false);
      secondpanel.setBorder(BorderFactory.createEmptyBorder(12,0,12,0));
      secondpanel.setBackground(Color.GRAY);
    }

   //check if method or interface/class/enum
   if (create_type==CreateType.METHOD) {
      list_panel = new BwizListEntryComponent(info_structure.getSet(), new ParameterVerifier(), list_text);
    }
   else {
      list_panel = new BwizListEntryComponent(info_structure.getSet(), new InterfaceVerifier(), list_text);
    }

   list_panel.setTitleFont(getRelativeFont(-3));
   list_panel.setTextFont(getRelativeFont(-3));

   //The panel that contains the accessibility radiobuttons and the abstract check box.
   JPanel boxespanel = new JPanel(new GridBagLayout());
   boxespanel.setOpaque(false);
   boxespanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   //A panel that has the radiobuttons and checkboxes. Probably can just remove boxespanel and use this only.
   switch (create_type) {
      case CLASS :
	 accessibility_panel = new BwizAccessibilityPanel(SHOW_ABSTRACT|SHOW_FINAL);
	 break;
      case ENUM :
      case INTERFACE :
	 accessibility_panel = new BwizAccessibilityPanel(0);
	 break;
      case METHOD :
	 accessibility_panel = new BwizAccessibilityPanel(SHOW_PRIVATE|SHOW_PROTECTED|
							     SHOW_ABSTRACT|SHOW_FINAL);
	 accessibility_panel.setSelected(Accessibility.PRIVATE);
	 break;
    }
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

   addRawComponent(name_text,namespanel);

   //Requires list of available projects
   /* sets up the list of projects and packages */
   if (create_type != CreateType.METHOD) {
      setupProject();
      setupPackage();
    }

   //adds components
   if (second_text != null) {
      addRawComponent(second_text,secondpanel);
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
   if (second_text != null) {
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
/*	Setup text strings for different types of dialogs			*/
/*										*/
/********************************************************************************/

private void setupTexts()
{
   switch(create_type) {
      case CLASS:
	 name_text = "Class Name";
	 name_hover_text = "<html>Enter the new class name.  Such names usually start with an uppercase letter";
	 second_text = "Extends";
	 second_hover_text = "This class's parent (super) class";
	 list_text = "Implements: ";
	 list_hover_text = "Any interfaces implemented by this class. Press ENTER after you type the interface name to add an interface.";
	 break;
      case INTERFACE :
	 name_text = "Interface Name";
	 name_hover_text = "<html>Enter the new interface name.  Such names usually start with an uppercase letter";
	 second_text = null;
	 second_hover_text = null;
	 list_text = "Extends";
	 list_hover_text = "Any interfaces extended by this interface. Press ENTER after you type the interface to add an interface.";
	 break;
      case ENUM :
	 name_text = "Enum Name";
	 name_hover_text = "<html> Enter the new enum name.  Such names usually start with an uppercase letter";
	 second_text = null;
	 second_hover_text = null;
	 list_text = "Implements";
	 list_hover_text = "Any interfaces implemented by this enum. Press ENTER after you type the interface to add an interface.";
	 break;
      case METHOD:
	 name_text = "Method Name";
	 second_text = "Return Type";
	 name_hover_text = "Method Name";
	 second_hover_text = "Return Type";
	 list_text = "Parameters";
	 list_hover_text = "Any method parameters. Press enter to add a parameter.";
	 break;
    }
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
   List<String> items = getPackages();
   if (items == null || items.size() == 0) return;

   if (package_dropdown == null) {
      ChoosePackage ca = new ChoosePackage();
      package_dropdown = addChoice("Package",items,0,ca);
      ca.set();
    }
   else {
      package_dropdown.removeAllItems();
      for (String s : items) {
	 package_dropdown.addItem(s);
       }
      if (!items.isEmpty()) package_dropdown.setSelectedIndex(0);
    }
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
   main_name = BwizFocusTextField.getStyledField(name_text, name_hover_text);
   //Sets the font size
   main_name.setFont(getRelativeFont(-2));
   main_name.setAlignmentX(Component.LEFT_ALIGNMENT);
   main_name.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Adds a handler for when the user is typing
   main_name.getDocument().addDocumentListener(new TextListener(info_structure.getMainName()));
   main_name.setMaximumSize(main_name.getPreferredSize());
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
   second_name = BwizFocusTextField.getStyledField("", second_hover_text);
   second_name.setFont(getRelativeFont(-2));
   second_name.setAlignmentX(Component.LEFT_ALIGNMENT);
   second_name.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Adds a handler for when the user is typing
   second_name.getDocument().addDocumentListener(new TextListener(info_structure.getSecondInfo()));
}

private void setupListName(Dimension enMax)
{
   //Style
   list_panel.getInputField().setMaximumSize(enMax);
   list_panel.getInputField().setToolTipText(list_hover_text);
   //Adds handler for when a new item is added to the list
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

   create_button.addActionListener(new Creator());
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

private static class InfoStruct
{
   private StringBuffer m_name;
   private Accessibility m_access;
   private String m_package;
   private String m_project;
   private List<StringBuffer> m_set;
   private StringBuffer m_second_info;
   private boolean is_abstract;
   private boolean is_final;
   private CreateType m_type;
   private boolean is_valid;
   private String m_class;		// what class this is located in

   InfoStruct(CreateType type) {
      m_name = new StringBuffer();
      m_access = Accessibility.PUBLIC;
      m_package = null;
      m_project = null;
      m_set = new ArrayList<StringBuffer>();
      m_second_info = new StringBuffer();
      is_abstract = false;
      is_final = false;
      m_type = type;
      is_valid = false;
      m_class = null;
    }

   List<StringBuffer> getSet()		{ return m_set; }
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
   void setSet(List<StringBuffer> l)	{ m_set = new ArrayList<StringBuffer>(l); }

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
      String temp = "";
      String secondword = "";

      if (m_type == CreateType.CLASS) secondword = " extends";

      if (!acceptableName(m_name.toString())) {
	 updateCreateButton(false);
	 return null;
       }

      //If method, make sure there is a return type specified, else don't continue
      if (m_type == CreateType.METHOD) {
	 if (m_second_info.length()==0) {
	    updateCreateButton(false);
	    return null;
	  }
	 //make sure it's a valid return type
	 String pattern = "[\\w\\s_]+";
	 if (m_second_info.toString().matches(pattern)==false) {
	    updateCreateButton(false);
	    return null;
	  }
       }

      //If Class, make sure superclass has an acceptable name
      if (m_type == CreateType.CLASS) {
	 if (m_second_info.length()!=0 && acceptableName(m_second_info.toString())!=true) {
	    updateCreateButton(false);
	    return null;
	  }
       }

      //Access
      if (m_access.toString().compareTo("default")!=0) {
	 temp = m_access.toString();
       }

      //cannot be both abstract and final
      if (is_abstract && is_final) return null;
      if (is_abstract) temp += " abstract";
      if (is_final) temp += " final";

      //methods do not include "method" in the signature
      //and also have return type before method-name
      if (m_type == CreateType.METHOD) {
	 temp += " " + m_second_info.toString() + " " + m_name.toString();
       }
      else {
	 // other types include typ in signature
	 temp += " " + m_type.toString() + " " + m_name.toString();
	 //extends
	 if (m_second_info.length() != 0) {
	    temp += secondword + " " + m_second_info.toString();
	  }
       }

      int setsize = m_set.size();

      //If Method, Parameters
      if (m_type == CreateType.METHOD) {
	 if (setsize > 0) {
	    temp+="(";
	    for (int count=0; count < setsize-1; count++)
	       temp+=m_set.get(count).toString()+", ";
	    temp += m_set.get(setsize-1).toString()+")";
	  }
	 else temp+="()";
       }
      else {
	 //Otherwise, Interfaces
	 if (setsize > 0) {
	    if (m_type == CreateType.INTERFACE) temp += " extends ";
	    else temp += " implements ";
	    int count = 0;
	    for (StringBuffer sb : m_set) {
	       if (count++ > 0) temp += ", ";
	       temp += sb.toString();
	     }
	  }
       }

      //finalizing the String
      temp = temp.trim();
      if (temp.compareTo("")==0) {
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

   boolean isValid() {
      return is_valid;
    }

   boolean validate() {
      BumpClient bc = BumpClient.getBump();
      String fnm = m_name.toString();
      if (m_package != null) fnm = m_package + "." + fnm;
      List<BumpLocation> locs = bc.findClassDefinition(m_project,fnm);
      if (locs != null && locs.size() > 0) return false;
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
   private boolean acceptableName(String name) {
      int length = name.length();
      if (length==0) return false;
      if (Character.isDigit(name.charAt(0))) return false;

      String pattern = "\\w+";
      if (name.matches(pattern)==false) return false;

      return true;
    }

}	 // end of inner class InfoStruct




/********************************************************************************/
/*										*/
/*	Interface verifier							*/
/*										*/
/********************************************************************************/

private static class InterfaceVerifier implements BwizConstants.IVerifier
{
   //Checks if is a String of valid interface(s) (separated by commas if more than one)
   public boolean verify(String test) {
      boolean to_return = true;
      String[] strs = test.split(",");
      if (strs.length > 1) {
	 for (String s : strs) {
	    to_return &= spaceVerify(s.trim());
	  }
	 return to_return;
       }
      else {
	 return spaceVerify(test);
       }
    }

   //Checks that there are no spaces in the test string
   private boolean spaceVerify(String test) {
      int length = test.length();
      if (length == 0) return false;

      for (int count=0; count < length; count++) {
	 char c = test.charAt(count);
	 if (count == 0 && !Character.isJavaIdentifierStart(c)) return false;
	 if (count > 0 && !Character.isJavaIdentifierPart(c)) return false;
       }
      return true;
    }

}	// end of inner class InterfaceVerifier


/********************************************************************************/
/*										*/
/*	Parameter verifier							*/
/*										*/
/********************************************************************************/

private static class ParameterVerifier implements BwizConstants.IVerifier
{
   //Checks if is a String of valid parameter(s) (separated by commas if more than one)
   @Override public boolean verify(String test) {
      boolean to_return = true;
      String[] strs = test.split(",");
      if (strs.length > 1) {
	 for (String s : strs) {
	    if (to_return == true) to_return = spaceVerify(s);
	  }
	 return to_return;
       }
      else {
	 return spaceVerify(test);
       }
    }

   //Checks that there is exactly one space in the test string between two words
   private boolean spaceVerify(String test) {
      test = test.trim();

      String pattern = "[\\w\\s]+";
      if (test.matches(pattern) == false) return false;

      int spaceCount = 0;
      int length = test.length();
      for (int count = 0; count < length; count++) {
	 if (Character.isSpaceChar(test.charAt(count))) spaceCount++;
       }
      return spaceCount == 1;
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

private class Creator implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      if (!info_structure.isValid()) return;

      BuenoFactory bf = BuenoFactory.getFactory();
      BuenoLocation bl = at_location;
      BuenoProperties bp = new BuenoProperties();
      BudaBubble bbl = BudaRoot.findBudaBubble(BwizNewWizard.this);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
      Rectangle r = BudaRoot.findBudaLocation(bbl);
      Point pt = r.getLocation();
      bba.removeBubble(bbl);
      BudaBubble nbbl = null;

      String pkg = info_structure.getPackageName();
      String proj = info_structure.getProjectName();
      String cls = info_structure.getMainName().toString();
      String fcls = (pkg == null ? cls : pkg + "." + cls);

      if (pkg != null) bp.put(BuenoKey.KEY_PACKAGE, pkg);
      bp.put(BuenoKey.KEY_PROJECT, proj);
      bp.put(BuenoKey.KEY_NAME, cls);
      bp.put(BuenoKey.KEY_MODIFIERS,info_structure.getModifiers());

      switch (create_type) {
	 case CLASS:
	    bp.put(BuenoKey.KEY_EXTENDS,info_structure.getSecondInfo().toString());
	    bp.put(BuenoKey.KEY_IMPLEMENTS,info_structure.getSet());
	    if (bl == null) bl = bf.createLocation(proj,pkg,null,true);
	    bf.createNew(BuenoType.NEW_CLASS,bl,bp);
	    if (bubble_creator == null) nbbl = BaleFactory.getFactory().createFileBubble(proj,null,fcls);
	    else bubble_creator.createBubble(proj,fcls,bba,pt);
	    // BwizNewWizard mwiz = new BwizNewWizard(CreateType.METHOD,class_name,project_name,package_name);
	    // BwizFactory.getFactory().createBubble(mwiz,mwiz.getFocus());
	    break;

	 case METHOD:
	    cls = info_structure.getClassName();
	    String mthd = info_structure.getMainName().toString();
	    fcls = (pkg == null ? cls : pkg + "." + cls);
	    bp.put(BuenoKey.KEY_NAME,mthd);
	    bp.put(BuenoKey.KEY_TYPE,fcls);
	    bp.put(BuenoKey.KEY_RETURNS,info_structure.getSecondInfo().toString());
	    bp.put(BuenoKey.KEY_PARAMETERS,info_structure.getSet());
	    StringBuffer buf = new StringBuffer();
	    buf.append(fcls);
	    buf.append(".");
	    buf.append(mthd);
	    buf.append("(");
	    int i = 0;
	    for (StringBuffer sb : info_structure.getSet()) {
	       if (i++ > 0) buf.append(",");
	       String s = sb.toString();
	       int idx = s.lastIndexOf(" ");
	       String typ = s.substring(0,idx);
	       buf.append(typ);
	     }
	    buf.append(")");
	    String fmthd = buf.toString();
	    if (bl == null) bl = bf.createLocation(proj,pkg,cls,true);
	    bf.createNew(BuenoType.NEW_METHOD,bl,bp);
	    if (bubble_creator == null) nbbl = BaleFactory.getFactory().createMethodBubble(proj,fmthd);
	    else bubble_creator.createBubble(proj,fmthd,bba,pt);
	    break;

	 case INTERFACE:
	    bp.put(BuenoKey.KEY_EXTENDS,info_structure.getSet());
	    if (bl == null) bl = bf.createLocation(proj,pkg,null,true);
	    bf.createNew(BuenoType.NEW_INTERFACE,bl,bp);
	    if (bubble_creator == null) nbbl = BaleFactory.getFactory().createFileBubble(proj,null,fcls);
	    else bubble_creator.createBubble(proj,fcls,bba,pt);
	    break;

	 case ENUM:
	    bp.put(BuenoKey.KEY_IMPLEMENTS,info_structure.getSet());
	    if (bl == null) bl = bf.createLocation(proj,pkg,null,true);
	    bf.createNew(BuenoType.NEW_ENUM,bl,bp);
	    if (bubble_creator == null) nbbl = BaleFactory.getFactory().createFileBubble(proj,null,fcls);
	    else bubble_creator.createBubble(proj,fcls,bba,pt);
	    break;

	 default:
	    //Should not get here
	    BoardLog.logE("BWIZ","Unknown create type " + create_type);
	    break;
       }

      if (nbbl != null) {
	 bba.add(nbbl,new BudaConstraint(pt));
      }
    }

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
