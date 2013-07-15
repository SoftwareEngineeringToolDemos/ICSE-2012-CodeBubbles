/********************************************************************************/
/*										*/
/*		BwizListEntryComponent.java					*/
/*										*/
/*	Wizard component holding a list of items				*/
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

import edu.brown.cs.ivy.swing.*;

import javax.swing.*;

import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Font;
import java.util.*;



class BwizListEntryComponent extends JPanel implements BwizConstants,
	BwizConstants.VerificationListener, ListSelectionListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String ui_title;
private JList<StringBuffer> ui_list;
private JTextField input_field;
private JLabel title_label;
private Vector<StringBuffer> list_data;
private HashSet<String> set_data;
private IVerifier the_verifier;
private JButton add_button;
private JButton remove_button;
private SwingEventListenerList<ItemChangeListener> item_listeners;

private static Border cell_padding = BorderFactory.createEmptyBorder(4,4,4,4);



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizListEntryComponent(List<StringBuffer> data, IVerifier verifier)
{
   this(data, verifier, "");
}



BwizListEntryComponent(Collection<StringBuffer> data, IVerifier verifier, String title)
{
   the_verifier = null;
   ui_title=title;
   list_data= new Vector<StringBuffer>();
   set_data = new HashSet<String>();
   for (StringBuffer bf : data) {
      list_data.add(bf);
      set_data.add(bf.toString());
    }

   the_verifier=verifier;
   item_listeners = new SwingEventListenerList<ItemChangeListener>(ItemChangeListener.class);

   setup();
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JTextField getInputField()		{ return input_field; }

List<StringBuffer> getListElements()	{ return new ArrayList<StringBuffer>(list_data); }

HashSet<String> getSetElements() { return set_data; }


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setup()
{
   setLayout(new BoxLayout(this,
			      BoxLayout.PAGE_AXIS));
   setOpaque(false);
   setAlignmentX(Component.LEFT_ALIGNMENT);

   //A panel for the title and entry area
   JPanel subpanel = new JPanel();
   subpanel.setLayout(new BoxLayout(subpanel,BoxLayout.LINE_AXIS));
   subpanel.setOpaque(false);
   subpanel.setAlignmentX(Component.LEFT_ALIGNMENT);

   add(subpanel);

   //Creates the title area
   title_label=new JLabel(ui_title);
   title_label.setFont(BWIZ_FONT_SIZE_MAIN.deriveFont((float)24));
   title_label.setAlignmentY(Component.BOTTOM_ALIGNMENT);

   subpanel.add(title_label);
   subpanel.add(Box.createRigidArea(new Dimension(7, 0)));

   //Creates a textfield with default styling for the input area
   input_field=BwizVerifiedTextField.getStyledField("", "", the_verifier);
   input_field.setFont(BWIZ_FONT_SIZE_MAIN.deriveFont((float)24));
   input_field.setColumns(8);
   input_field.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   //Add a handler for when the user hits enter
   input_field.addActionListener(new InputAction());

   subpanel.add(input_field);

   subpanel.add(Box.createRigidArea(new Dimension(7,0)));

   //A button for adding values to the list
   add_button=new BwizHoverButton();
   add_button.setText("+");
   add_button.setFont(BWIZ_FONT_SIZE_MAIN.deriveFont((float)14));
   add_button.addActionListener(new InputAction());
   add_button.setAlignmentY(Component.BOTTOM_ALIGNMENT);

   if (the_verifier!=null)
       add_button.setEnabled(false);

   //A button for removing values from the list
   remove_button=new BwizHoverButton();
   remove_button.setText("-");
   remove_button.setFont(BWIZ_FONT_SIZE_MAIN.deriveFont((float)14));
   remove_button.addActionListener(new RemoveAction());
   remove_button.setAlignmentY(Component.BOTTOM_ALIGNMENT);
   remove_button.setEnabled(false);

   subpanel.add(add_button);
   subpanel.add(remove_button);

   //Creates a UI list area
   ui_list = new JList<StringBuffer>(list_data);
   ui_list.setFont(BWIZ_FONT_SIZE_MAIN.deriveFont((float)14));
   ui_list.setVisibleRowCount(0);
   ui_list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
   ui_list.setAlignmentX(Component.RIGHT_ALIGNMENT);
   ui_list.setCellRenderer(new PaddedListCellRenderer());
   //Adds a handler to allow to removing items from the list by hitting the delete key.
   ui_list.addKeyListener(new KeyAction());
   ui_list.addListSelectionListener(this);

   //UI to allow the list to scroll if needed
   JScrollPane implementsPane = new JScrollPane(ui_list);
   //Rough calculations of how large to be to display a couple rows
   implementsPane.setPreferredSize(new Dimension(100, 3*ui_list.getFont().getSize()+16));
   implementsPane.setAlignmentX(Component.LEFT_ALIGNMENT);
   implementsPane.setBorder(BorderFactory.createEmptyBorder());

   add(Box.createRigidArea(new Dimension(0, 10)));
   add(implementsPane);
}




private void addItem(String text)
{
   boolean add = true;
   //Add text to list
   if (the_verifier !=null)
      {
      //Use the verifier to check the text
      if (the_verifier.verify(text)) {
	 String[] strs = text.split(",");
	 if (strs.length > 1) {
	    for (String s : strs) addItem(s.trim());
	  }
	 else {
	    //Make sure no duplicated in Parameters case
	    text = text.trim();
	    strs = text.split(" ");
	    if (strs.length > 1) {
	       if (!set_data.contains(strs[1])) {
		  set_data.add(strs[1]);
		}
	       else {
		  add = false;
		}
	     }
	    //Make sure no duplicates
	    if (add==true && !set_data.contains(text)) {
	       set_data.add(text);
	       list_data.add(new StringBuffer(text));
	       ui_list.setListData(list_data);
	       input_field.setText("");
	       //Throw an event that an item was added
	       fireItemAdded(text);
	       disableAddition();
	       disableRemoval();
	     }
	  }
       }
      else {
	 //TODO: Possibly show some kind of error notification)
       }
    }
   else {
      //No verification of entry
      //Make sure no duplicates
      if (!set_data.contains(text)) {
	 set_data.add(text);
	 list_data.add(new StringBuffer(text));
	 ui_list.setListData(list_data);
	 input_field.setText("");
	 //Throw an event that an item was added
	 fireItemAdded(text);
	 disableAddition();
	 disableRemoval();
       }
    }
}




private void remove(List<StringBuffer> objects)
{
   //Find the object in the list to remove
   for (StringBuffer obj : objects) {
      String[] strs = obj.toString().trim().split(" ");
      if (strs.length > 1) {
	 set_data.remove(strs[1]);
       }
      list_data.remove(obj);
      set_data.remove(obj.toString());

      //I haven't tested that this will be the correct string
      //Throw an event that an item was removed
      fireItemRemoved(obj.toString());
    }

   //Update the UI
   ui_list.setListData(list_data);
}



private void removeSelected()
{
    List<StringBuffer> indices = ui_list.getSelectedValuesList();
    if (indices != null && indices.size() > 0)
    {
	remove(indices);
	disableRemoval();
    }
}

void addItemChangeEventListener(ItemChangeListener listener)
{
   item_listeners.add(listener);
}


@Override public void verificationEvent(Object sender, boolean success)
{
    if (success) {
       //Enable + button
       enableAddition();
     }
}



@Override public void valueChanged(ListSelectionEvent e)
{
    if (!e.getValueIsAdjusting()) {
       if (ui_list.getSelectedIndices().length>0) {
	  enableRemoval();
	}
     }
}



private void disableAddition()
{
    add_button.setEnabled(false);
}

private void enableAddition()
{
    add_button.setEnabled(true);
}

private void disableRemoval()
{
    remove_button.setEnabled(false);
}

private void enableRemoval()
{
    remove_button.setEnabled(true);
}


private void fireItemAdded(String item)
{
   for (ItemChangeListener icl : item_listeners) {
      icl.itemAdded(item);
    }
}


private void fireItemRemoved(String item)
{
   for (ItemChangeListener icl : item_listeners) {
      icl.itemRemoved(item);
    }
}



protected void setTitleFont(Font f)
{
   title_label.setFont(f);
}



protected void setTextFont(Font f)
{
   input_field.setFont(f);
}



/********************************************************************************/
/*										*/
/*	Event handlers								*/
/*										*/
/********************************************************************************/

private class InputAction implements ActionListener {

   public void actionPerformed(ActionEvent e) {
      addItem(input_field.getText());
    }

}	// end of inner class InputAction



private class RemoveAction implements ActionListener
{
    public void actionPerformed(ActionEvent e) {
       removeSelected();
     }
}	// end of inner class RemoveAction



private class KeyAction extends KeyAdapter {

   public void keyPressed(KeyEvent e) {
      int keyCode=e.getKeyCode();
      switch (keyCode) {
	 case KeyEvent.VK_BACK_SPACE :
	 case KeyEvent.VK_DELETE :
	    removeSelected();
	    break;
       }
    }

}	// end of inner class KeyAction




/********************************************************************************/
/*										*/
/*	Cell renderers								*/
/*										*/
/********************************************************************************/

private class PaddedListCellRenderer implements ListCellRenderer<StringBuffer>
{
   private Border instance_padding;
   private DefaultListCellRenderer default_renderer;


   PaddedListCellRenderer() {
      instance_padding = null;
      default_renderer = new DefaultListCellRenderer();
    }

   @Override public Component getListCellRendererComponent(JList<? extends StringBuffer> list, StringBuffer value, int index, boolean isSelected, boolean cellHasFocus) {
      JLabel renderer = (JLabel) default_renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      renderer.setBorder((instance_padding==null) ? cell_padding : instance_padding);

      return renderer;
    }

}	// end of inner class PaddedListCellRenderer



}	// end of class BwizListEntryComponent



/* end of BwizListEntryComponent.java */
