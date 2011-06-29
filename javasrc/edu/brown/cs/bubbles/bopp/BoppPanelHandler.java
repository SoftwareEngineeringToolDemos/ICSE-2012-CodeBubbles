/********************************************************************************/
/*										*/
/*		BoppPanelHandler.java						*/
/*										*/
/*	Options/preferences panel						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* SVN: $Id$ */

package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 *
 * This class is the options panel, displaying all the preferences that can be
 * changed by the user.
 *
 **/

class BoppPanelHandler implements BoppConstants, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private ArrayList<BoppOption>	option_list;
private JPanel			inner_scroll;
private JPanel			tab_pane;
private JTabbedPane		panel_tabs;
private BudaBubbleArea		b_area;
private JPanel			main_panel;
private JPanel			search_panel;
private JTextField		search_box;
private TabPanel		search_tab;

private JScrollPane		tab_scroll_pane;


private static List<String>		changed_options  = new ArrayList<String>();
private static Map<TabName, String>	tabs_to_strings  = new HashMap<TabName, String>();
private static Map<String, TabName>	strings_to_tabs  = new HashMap<String, TabName>();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 * Constructor, uses BudaBubbleArea to define its location
 */

BoppPanelHandler(BudaBubbleArea b)
{
   b_area = b;
   option_list = new ArrayList<BoppOption>();
}



/********************************************************************************/
/*										*/
/*	Methods for updating recent options queries				*/
/*										*/
/********************************************************************************/

/**
 * Adds the given option to the top of the list of most recently changed
 * options, called whenever the user sets an option
 */

static void optionChanged(String pack,String name)
{
   if (changed_options.contains(pack + RECENT_OPTIONS_SPACER + name)) {
      changed_options.remove(pack + RECENT_OPTIONS_SPACER + name);
      changed_options.add(0, pack + RECENT_OPTIONS_SPACER + name);
   }
   else changed_options.add(0, pack + RECENT_OPTIONS_SPACER + name);
}


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 * Retrieves a new options panel
 */
JPanel getUIPanel()
{
   makeTabMaps();
   makeTabPanel();
   setupFromXml();
   makeSearchPanel();
   makeMainPanel();
   search_box.requestFocus();
   return main_panel;
}


/**
 * Makes the button panel at the bottom
 */
private JPanel makeButtonPanel()
{
   JPanel submitpanel = new JPanel();
   submitpanel.setLayout(new GridLayout(1,5,2,0));// need to implement a gridbag layout to
   // make buttons differing sizes
   Font buttonFont = BoardFont.getFont(Font.DIALOG,Font.PLAIN,11);
   JButton resetButton = new JButton("Reset to defaults");
   JButton undoButton = new JButton("Undo changes");
   JButton submitButton = new JButton("Save changes");
   JButton savecloseButton = new JButton("Save and close");
   JButton closeundoButton = new JButton("Close and undo");
   Insets border = new Insets(4,1,4,1);
   resetButton.setMargin(border);
   undoButton.setMargin(border);
   submitButton.setMargin(border);
   savecloseButton.setMargin(border);
   closeundoButton.setMargin(border);
   resetButton.setFont(buttonFont);
   undoButton.setFont(buttonFont);
   submitButton.setFont(buttonFont);
   savecloseButton.setFont(buttonFont);
   closeundoButton.setFont(buttonFont);
   resetButton.setVerticalAlignment(SwingConstants.CENTER);
   undoButton.setVerticalAlignment(SwingConstants.CENTER);
   submitButton.setVerticalAlignment(SwingConstants.CENTER);
   savecloseButton.setVerticalAlignment(SwingConstants.CENTER);
   closeundoButton.setVerticalAlignment(SwingConstants.CENTER);
   resetButton.addActionListener(new ResetListener());
   savecloseButton.addActionListener(new SaveCloseListener());
   closeundoButton.addActionListener(new CloseListener());
   submitButton.addActionListener(new SubmitListener());
   undoButton.addActionListener(new UndoListener());
   submitpanel.add(resetButton);
   submitpanel.add(undoButton);
   submitpanel.add(submitButton);
   submitpanel.add(closeundoButton);
   submitpanel.add(savecloseButton);
   submitpanel.setBackground(BACKGROUND_COLOR);
   // savecloseButton.setPreferredSize(new
   // Dimension((int)(savecloseButton.getPreferredSize().width*1.5),
   // (int)(savecloseButton.getPreferredSize().height*1.5)));
   savecloseButton.setFont(BoardFont.getFont(Font.DIALOG,Font.BOLD,12));
   return submitpanel;
}


/**
 * Makes the entire options panel
 */
private void makeMainPanel()
{
   main_panel = new OptionsPanel();
}


/**
 * makes the tabs
 */
private void makeTabPanel()
{
   panel_tabs = new JTabbedPane();
   panel_tabs.setUI(new OptionTabsUI());
   panel_tabs.setPreferredSize(TAB_PANEL_SIZE);
   panel_tabs.setTabPlacement(JTabbedPane.LEFT);
   panel_tabs.setOpaque(false);
   panel_tabs.setBorder(TAB_PANEL_BORDER);
   tab_pane = new JPanel();
   tab_pane.setOpaque(false);
   inner_scroll = setScroller(tab_pane);
}


/**
 * Makes the top search panel
 */
private void makeSearchPanel()
{
   search_panel = new JPanel();
   search_panel.setLayout(new BoxLayout(search_panel,BoxLayout.X_AXIS));
   JLabel searchTitle = new JLabel("Search by keyword: ");
   searchTitle.setHorizontalAlignment(JLabel.CENTER);
   searchTitle.setVerticalAlignment(JLabel.CENTER);
   searchTitle.setFont(OPTION_BOLD_FONT);
   search_panel.add(new JSeparator());
   search_panel.add(searchTitle);
   search_panel.setBackground(BACKGROUND_COLOR);
   search_box = new JTextField("",40);
   search_box.setMaximumSize(search_box.getPreferredSize());
   search_box.addKeyListener(new SearchKeyListener());
   search_panel.add(search_box);
   search_box.requestFocus();
}


/**
 * Generates the mappings of tabs to strings and vice versa
 */
private void makeTabMaps()
{
   tabs_to_strings.put(TabName.ALL, ALL_STRING);
   tabs_to_strings.put(TabName.BUBBLE_OPTIONS, BUBBLE_OPTIONS_STRING);
   tabs_to_strings.put(TabName.FONT_OPTIONS, FONT_OPTIONS_STRING);
   tabs_to_strings.put(TabName.POPULAR, POPULAR_TAB_STRING);
   tabs_to_strings.put(TabName.SYSTEM_OPTIONS, SYSTEM_OPTIONS_STRING);
   tabs_to_strings.put(TabName.USER_RECENT_OPTIONS, USER_RECENT_OPTIONS_STRING);
   tabs_to_strings.put(TabName.SEARCH, SEARCH_STRING);
   tabs_to_strings.put(TabName.VISUALIZATIONS, VISUALIZATIONS_TAB_STRING);
   tabs_to_strings.put(TabName.SEARCH_OPTIONS, SEARCH_OPTIONS_STRING);
   tabs_to_strings.put(TabName.TEXT_EDITOR_OPTIONS, TEXT_EDITOR_STRING);

   strings_to_tabs.put(ALL_STRING, TabName.ALL);
   strings_to_tabs.put(BUBBLE_OPTIONS_STRING, TabName.BUBBLE_OPTIONS);
   strings_to_tabs.put(FONT_OPTIONS_STRING, TabName.FONT_OPTIONS);
   strings_to_tabs.put(POPULAR_TAB_STRING, TabName.POPULAR);
   strings_to_tabs.put(SYSTEM_OPTIONS_STRING, TabName.SYSTEM_OPTIONS);
   strings_to_tabs.put(USER_RECENT_OPTIONS_STRING, TabName.USER_RECENT_OPTIONS);
   strings_to_tabs.put(SEARCH_STRING, TabName.SEARCH);
   strings_to_tabs.put(VISUALIZATIONS_TAB_STRING, TabName.VISUALIZATIONS);
   strings_to_tabs.put(SEARCH_OPTIONS_STRING, TabName.SEARCH_OPTIONS);

   strings_to_tabs.put(TEXT_EDITOR_STRING, TabName.TEXT_EDITOR_OPTIONS);

}


/**
 * Adds all the tabs to the tab panel
 */
private void setupTabs()
{
   TabName[] tabs = TabName.values();
   for (int i = 0; i < tabs.length; i++) {
      addTab(getStringFromTab(tabs[i]));
    }
   for (int i = 0; i < panel_tabs.getTabCount(); i++) {
      TabPanel p = (TabPanel) panel_tabs.getComponentAt(i);
      if (p != null) {
	 if (p.getTabName() == TabName.SEARCH) {
	    search_tab = p;
	  }
       }
    }
}


/**
 * Adds a specific tab to the panel
 */
private void addTab(String name)
{
   panel_tabs.addChangeListener(new TabChangeListener());
   TabPanel tp = new TabPanel(getTabFromString(name));
   tp.setLayout(new BoxLayout(tp,BoxLayout.PAGE_AXIS));
   tp.setBorder(TAB_BORDER);
   panel_tabs.add(name, tp);
}


/**
 * Creates an option
 */
private BoppOption setupOption(String n,ArrayList<TabName> tn,String d,String p,String t,
				  String examplestring,Element node)
{
   OptionType type = null;
   if (t.equals("INTEGER")) type = OptionType.INTEGER;
   if (t.equals("COLOR")) type = OptionType.COLOR;
   if (t.equals("STRING")) type = OptionType.STRING;
   if (t.equals("BOOLEAN")) type = OptionType.BOOLEAN;
   if (t.equals("DIVIDER")) type = OptionType.DIVIDER;
   if (t.equals("FONT")) type = OptionType.FONT;
   if (t.equals("COMBO")) type = OptionType.COMBO;
   // return null;
   BoppOption opt = BoppFactory.makeOption(n, tn, d, p, type, node);
   opt.setup();
   if (examplestring != null) {
      opt.addExample(examplestring);
    }
   option_list.add(opt);
   return opt;
}


/**
 * Sets the scroller to specified settings
 */
private JPanel setScroller(JPanel p)
{
   JScrollPane scroller = new JScrollPane();
   scroller.setMaximumSize(SCROLL_PANEL_SIZE);
   scroller.setMinimumSize(SCROLL_PANEL_SIZE);
   scroller.setPreferredSize(SCROLL_PANEL_SIZE);
   scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
   scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
   JScrollBar bar = scroller.getVerticalScrollBar();
   scroller.setOpaque(true);
   scroller.setBackground(BACKGROUND_COLOR);
   bar.setUnitIncrement(bar.getUnitIncrement() * 3);
   bar.setOpaque(false);
   JPanel toReturn = new JPanel();
   toReturn.setLayout(new BoxLayout(toReturn,BoxLayout.PAGE_AXIS));
   toReturn.setOpaque(true);
   toReturn.setBackground(BACKGROUND_COLOR);
   scroller.setViewportView(toReturn);
   p.setLayout(new BoxLayout(p,BoxLayout.PAGE_AXIS));
   p.add(scroller);
   tab_scroll_pane = scroller;
   return toReturn;
}



/********************************************************************************/
/*										*/
/*	XML parser and methods for generating options				*/
/*										*/
/********************************************************************************/

private void setupFromXml()
{
   Element x = IvyXml.loadXmlFromStream(BoardProperties
					   .getLibraryFile(PREFERENCES_XML_FILENAME));
   for (Element n : IvyXml.children(x,"PACKAGE")) {
      handleXmlPackage(n);
    }
   setupTabs();
   inner_scroll.revalidate();
}



private void handleXmlPackage(Element node)
{

   String packagename = IvyXml.getAttrString(node,"NAME");
   for (Element n : IvyXml.children(node,"OPT")) {
      try {
	 handleXmlOption(n,packagename);
       }
      catch (Exception e) {
	 BoardLog.logE("BOPP", "Preferences XML loading", e);
       }
    }
}



private void handleXmlOption(Element node,String packagename)
{
   String name = IvyXml.getAttrString(node,"NAME");
   String type = IvyXml.getAttrString(node,"TYPE");
   BoppOption opt = null;
   if (type.equals("DIVIDER")) {
      opt = setupOption(name, null, null, packagename, type, null, node);
    }
   else {
      String description = IvyXml.getAttrString(node, "DESCRIPTION");
      opt = setupOption(name, null, description, packagename, type, null, node);
    }
   if (opt != null) {
      for (Element n : IvyXml.children(node)) {
	 handleXmlOptionSettings(n, opt);
       }
    }
}



private void handleXmlOptionSettings(Element n,BoppOption opt)
{
   if (IvyXml.isElement(n,"TAB")) {
      opt.addToTab(getTabFromString(IvyXml.getAttrString(n, "NAME")));
    }
   else if (IvyXml.isElement(n,"EXAMPLE")) {
      opt.addExample(IvyXml.getAttrString(n, "TEXT"));
    }
   else if (IvyXml.isElement(n,"WARNING")) {
      opt.addWarning(IvyXml.getAttrString(n, "TEXT"));
    }
   else if (IvyXml.isElement(n,"KEYWORD")) {
      opt.addKeyword(IvyXml.getAttrString(n, "TEXT"));
    }
}



/********************************************************************************/
/*										*/
/*	Search methods								*/
/*										*/
/********************************************************************************/

private void search()
{
   for (Component c : inner_scroll.getComponents()) {
      if (!c.equals(search_panel)) {
	 inner_scroll.remove(c);
       }
    }

   String text = search_box.getText();
   if (!text.trim().isEmpty()) {
      String[] words = text.split(" ");
      Pattern[] patterns = new Pattern[words.length];

      for (int i = 0; i < words.length; i++) {
	 try {
	    patterns[i] = (Pattern.compile(words[i], Pattern.CASE_INSENSITIVE));
	  }
	 catch (PatternSyntaxException e) {
	    patterns[i] = null;
	  }
       }
      for (BoppOption opt : option_list) {
	 if (opt.search(patterns)) {
	    inner_scroll.add(opt);
	  }
       }
    }

   inner_scroll.revalidate();
   inner_scroll.repaint();
}



/********************************************************************************/
/*										*/
/*	Methods to handle tab changes						*/
/*										*/
/********************************************************************************/

private void changeToTab(TabPanel tp)
{
   inner_scroll.removeAll();
   if (tab_pane.getParent() != null) tab_pane.getParent().remove(tab_pane);
   tab_scroll_pane.getVerticalScrollBar().setValue(0);
   tp.add(tab_pane);
   if (tp.getTabName() != TabName.SEARCH) {
      getTabOptions(tp);
    }
   else {
      search();
    }
}



private void getTabOptions(TabPanel tp)
{
   if (tp.getTabName() == TabName.USER_RECENT_OPTIONS) {
      grabOptions(changed_options);
    }

   else for (BoppOption opt : option_list) {
      if (opt.getContainingTabs().contains(tp.tab_name)) {
	 inner_scroll.add(opt);
       }
    }
   inner_scroll.revalidate();
}



private BoppOption getOption(String pack,String name)
{
   for (BoppOption opt : option_list) {
      if (opt.getType() != OptionType.DIVIDER) {
	 if (opt.getOptionName().equals(name) && opt.getPack().equals(pack)) return opt;
       }
    }
   return null;
}



private void grabOptions(List<String> list)
{
   int i = 0;
   for (String s : list) {
      if (i > MAXIMUM_RECENTLY_DISPLAYED_OPTIONS) {
	 break;
       }
      i++;
      String[] packname = s.split(RECENT_OPTIONS_SPACER);
      String pack = packname[0];
      String name = packname[1];
      BoppOption opt = getOption(pack, name);
      if (opt != null) {
	 inner_scroll.add(opt);
       }
    }
}



private void refreshTabs()
{
   TabPanel tp = (TabPanel) panel_tabs.getSelectedComponent();
   changeToTab(tp);
}



/********************************************************************************/
/*										*/
/*	Helper methods for converting enums					*/
/*										*/
/********************************************************************************/

private TabName getTabFromString(String name)
{
   return strings_to_tabs.get(name);
}

private String getStringFromTab(TabName name)
{
   return tabs_to_strings.get(name);
}



/********************************************************************************/
/*										*/
/*	Class for alternate tab display 					*/
/*										*/
/********************************************************************************/

private class OptionTabsUI extends BasicTabbedPaneUI {

   @Override protected void paintTabBackground(Graphics g,int tabplacement,int tabindex,
						  int x,int y,int w,int h,boolean isselected) {
      if (isselected) {
	 GradientPaint gp;
	 gp = new GradientPaint(x,y,Color.WHITE,x,y + h / 2,new Color(173,214,226,225),true);
	 Graphics2D g2 = (Graphics2D) g;
	 g2.setPaint(gp);
	 RoundRectangle2D rr = new RoundRectangle2D.Double(x,y,w - 3,h,4,4);
	 g2.fill(rr);
       }
    }

   @Override protected void installDefaults() {
      super.installDefaults();
      tabAreaInsets.top = 4;
      selectedTabPadInsets = new Insets(0,0,0,0);
      tabInsets = selectedTabPadInsets;
    }

   @Override protected void paintFocusIndicator(Graphics g,int tabplacement,Rectangle[] rs,
						   int tabindex,Rectangle iconRect,
						   Rectangle textrect,boolean isselected) {}

   @Override protected void paintTabArea(Graphics g,int tabplacement,int selectedIndex) {
      int tw = tabPane.getBounds().width;
      int th = tabPane.getBounds().height;
      Graphics2D g2 = (Graphics2D) g;
      g2.setPaint(Color.white);
      Rectangle r = new Rectangle(0,0,tw,th);
      g2.fill(r);
      super.paintTabArea(g, tabplacement, selectedIndex);
    }

   @Override protected Insets getContentBorderInsets(int tabplacement) {
      return new Insets(0,0,0,0);
    }

   @Override protected int getTabLabelShiftY(int tabplacement,int tabindex,boolean isselected) {
      return 0;
    }

   @Override protected int calculateTabWidth(int tabplacement,int tabindex,FontMetrics metrics) {
      return super.calculateTabWidth(tabplacement, tabindex, metrics) + metrics.getHeight();
    }

   @Override protected void paintText(Graphics g,int tabplacement,Font font,
					 FontMetrics metrics,int tabindex,String title,Rectangle textrect,
					 boolean isselected) {

      if (isselected) {
	 Font f = new Font(font.getFontName(),font.getStyle() | Font.BOLD,font.getSize());

	 super.paintText(g, tabplacement, f, metrics, tabindex, title, textrect, isselected);
       }
      else super.paintText(g, tabplacement, TAB_UNSELECTED_FONT, metrics, tabindex, title,
			      textrect, isselected);
    }

   @Override protected void paintTabBorder(Graphics g,int tabplacement,int tabindex,int x,
					      int y,int w,int h,boolean isselected) { }

}	// end of inner class OptionsTabUI



/********************************************************************************/
/*										*/
/*	Panel classes for tabs							*/
/*										*/
/********************************************************************************/

private class TabPanel extends JPanel {

   private TabName    tab_name;
   private static final long serialVersionUID = 1;

   TabPanel(TabName name) {
      super();
      this.tab_name = name;
    }

   TabName getTabName() {
      return tab_name;
    }

}	// end of inner class TabPanel



/********************************************************************************/
/*										*/
/*	Saving methods								*/
/*										*/
/********************************************************************************/

void saveOptions()
{
   for (int i = 0; i < option_list.size(); i++) {
      BoppOption opt = option_list.get(i);
      opt.setOption();
    }
}



/********************************************************************************/
/*										*/
/*	Listeners for interaction						*/
/*										*/
/********************************************************************************/

/**
 * Listener to close the panel and undo
 */
private class CloseListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      JPanel p = (JPanel) ((JButton) arg0.getSource()).getParent().getParent();
      p.setVisible(false);
      for (int i = 0; i < option_list.size(); i++) {
	 BoppOption opt = option_list.get(i);
	 opt.resetOption();
       }
      refreshTabs();
    }

}	// end of class CloseListener



/**
 * Listener to close the panel and save
 */
private class SaveCloseListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      JPanel p = (JPanel) ((JButton) arg0.getSource()).getParent().getParent();
      p.setVisible(false);
      for (int i = 0; i < option_list.size(); i++) {
	 BoppOption opt = option_list.get(i);
	 opt.setOption();
       }
      refreshTabs();
    }

}	// end of class SaveCloseListener



/**
 * Listener for the submit button
 */
private class SubmitListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      for (int i = 0; i < option_list.size(); i++) {
	 BoppOption opt = option_list.get(i);
	 opt.setOption();
       }
      if (b_area != null) b_area.repaint();
    }

}	// end of class SubmitListener


/**
 * Listener for the undo button
 */
private class UndoListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      for (int i = 0; i < option_list.size(); i++) {
	 BoppOption opt = option_list.get(i);
	 opt.resetOption();
       }
    }

}	// end of class UndoListener



/**
 * Listener for the reset button
 */
private class ResetListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent arg0) {
      int option = JOptionPane.showConfirmDialog(main_panel,
						    "Do you really want to reset ALL your " + "customized options?", "Warning",
						    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      if (option == 0) {
	 BoardProperties.resetDefaultProperties();
	 for (int i = 0; i < option_list.size(); i++) {
	    BoppOption opt = option_list.get(i);
	    opt.resetOption();
	  }
       }
      JOptionPane.showMessageDialog(main_panel,
				       "Some options will not take effect until restart", "Message",
				       JOptionPane.INFORMATION_MESSAGE);
    }

}	// end of class ResetListener



/**
 * Listener for tab changes
 */
private class TabChangeListener implements ChangeListener {

   @Override public void stateChanged(ChangeEvent arg0) {
      TabPanel tp = (TabPanel) panel_tabs.getSelectedComponent();
      int index = panel_tabs.getSelectedIndex();
      for (int i = 0; i < panel_tabs.getTabCount(); i++) {
	 if (i == index) panel_tabs.setForegroundAt(i, Color.black);
	 else panel_tabs.setForegroundAt(i, Color.black);
       }
      changeToTab(tp);
    }

}	// end of class TabChangeListener



/**
 * Listener for the display of the panel
 */
private class VisibleListener extends ComponentAdapter {

   @Override public void componentShown(ComponentEvent arg0) {
      panel_tabs.setSelectedIndex(0);
      refreshTabs();
      search_box.requestFocus();
    }

}	// end of class VisibleListener



/**
 * Listener for mouseover
 */
private class MouseFocusListener extends MouseAdapter {

   @Override public void mouseEntered(MouseEvent arg0) {
      refreshTabs();
      BudaCursorManager.setTemporaryCursor(main_panel, new Cursor(Cursor.DEFAULT_CURSOR));
    }

}	// end of class MouseFocusListener


/**
 * Listener for the search box
 */
private class SearchKeyListener extends KeyAdapter {

   @Override public void keyReleased(KeyEvent e) {
      if (((TabPanel) panel_tabs.getSelectedComponent()).getTabName() != TabName.SEARCH) {
         panel_tabs.setSelectedComponent(search_tab);
         changeToTab(search_tab);
       }
      search();
    }

}	// end of class SearchKeyListener



/********************************************************************************/
/*										*/
/*	Options Panel								*/
/*										*/
/********************************************************************************/

private class OptionsPanel extends JPanel {

   OptionsPanel() {
      setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
      setPreferredSize(OPTIONS_PANEL_SIZE);
      setMaximumSize(OPTIONS_PANEL_SIZE);
      setMinimumSize(OPTIONS_PANEL_SIZE);
      add(search_panel);
      add(Box.createHorizontalGlue());
      add(panel_tabs);
      add(makeButtonPanel());
      addComponentListener(new VisibleListener());
      addMouseListener(new MouseFocusListener());
      setBorder(PANEL_BORDER);
      setBackground(BACKGROUND_COLOR);
    }

}	// end of inner class OptionsPanel




}	// end of class BoppPanelHandler



/* end of BoppPanelHandler.java */
