/********************************************************************************/
/*                                                                              */
/*              BoppOptionPanel.java                                            */
/*                                                                              */
/*      Panel for displaying options                                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.ivy.swing.*;

import javax.swing.*;
import java.awt.*;

import java.util.*;
import java.util.List;

class BoppOptionPanel implements BoppConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BoppOptionSet   option_set;
private List<SubPanel>  sub_panels;
private SwingGridPanel  display_panel;
private JTextField      search_field;
private JButton         revert_button;
private JButton         save_button;
private JButton         close_button;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BoppOptionPanel(BoppOptionSet opts)
{
   option_set = opts; 
   
   sub_panels = new ArrayList<SubPanel>();
   Set<String> oset = new TreeSet<String>(opts.getTabNames());
   for (String s : oset) {
      SubPanel sp = new SubPanel(s);
      sub_panels.add(sp);
      sp.addOptions(opts.getOptionsForTab(s));
    }
   
   setupDisplay();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

SwingGridPanel getPanel()                       { return display_panel; }

BudaBubble getBubble()
{
   return new OptionBubble();
}


private class OptionBubble extends BudaBubble {

   OptionBubble() {
      super(display_panel,BudaBorder.RECTANGLE);
   }
   
}




/********************************************************************************/
/*                                                                              */
/*      Layout methods                                                          */
/*                                                                              */
/********************************************************************************/

private void setupDisplay()
{
   display_panel = new SwingGridPanel();
   
   search_field = new JTextField();
   SwingGridPanel sbox = new SwingGridPanel();
   JLabel l1 = new JLabel("Search by keyword: ");
   l1.setFont(l1.getFont().deriveFont(Font.BOLD));
   sbox.addGBComponent(l1,0,0,1,1,0,0);
   sbox.addGBComponent(search_field,1,0,1,1,10,0);
   display_panel.addGBComponent(sbox,0,0,1,1,10,0);
   
   JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
   for (SubPanel pnl : sub_panels) {
      tabs.addTab(pnl.getName(),pnl.getDisplay()); 
      System.err.println("BOPP: " + pnl.getName() + " " + pnl.getDisplay().getPreferredSize() + " " + pnl.getDisplay());
    }
   JScrollPane jsp = new JScrollPane(tabs);
   display_panel.addGBComponent(jsp,0,1,0,1,10,10);
   
   Box bbx = Box.createHorizontalBox();
   bbx.add(Box.createHorizontalGlue());
   revert_button = new JButton("Revert");
   bbx.add(revert_button);
   bbx.add(Box.createHorizontalGlue());
   save_button = new JButton("Save");
   bbx.add(save_button);
   bbx.add(Box.createHorizontalGlue());
   close_button = new JButton("Close");
   bbx.add(close_button);
   bbx.add(Box.createHorizontalGlue());
   display_panel.addGBComponent(bbx,0,2,0,1,10,0);
}

/********************************************************************************/
/*                                                                              */
/*      Information about a subpanel                                            */
/*                                                                              */
/********************************************************************************/

private class SubPanel {
   
   private String panel_name;
   private List<BoppOptionNew> panel_options;
   
   SubPanel(String nm) {
      panel_name = nm;
      panel_options = new ArrayList<BoppOptionNew>();
    }
   
   void addOptions(List<BoppOptionNew> opts) {
      panel_options.addAll(opts);
    }
   
   String getName()                             { return panel_name; }
   
   SwingGridPanel getDisplay() {
      OptionPanel pnl = new OptionPanel();
      pnl.beginLayout();
      for (BoppOptionNew op : panel_options) {
         op.addButton(pnl);
       }
      return pnl;
    }
   
}       // end of inner class SubPanel




/********************************************************************************/
/*                                                                              */
/*      Panel for option display                                                */
/*                                                                              */
/********************************************************************************/

private static class OptionPanel extends SwingGridPanel implements Scrollable {
   
   @Override public Dimension getPreferredScrollableViewportSize() {
      return new Dimension(300,400);
    }
   
   @Override public int getScrollableBlockIncrement(Rectangle r,int o,int d) {
      return 24; 
    }
   
   @Override public int getScrollableUnitIncrement(Rectangle r,int o,int d) {
      return 12;
    }
   
   @Override public boolean getScrollableTracksViewportHeight() {
      return false;
    }
   
   @Override public boolean getScrollableTracksViewportWidth() {
      return true;
    }
   
}       // end of inner class OpitonPanel
   
   

}       // end of class BoppOptionPanel




/* end of BoppOptionPanel.java */

