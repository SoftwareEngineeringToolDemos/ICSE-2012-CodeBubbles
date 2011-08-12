/********************************************************************************/
/*										*/
/*		BudaMenu.java							*/
/*										*/
/*	BUblles Display Area bubble menu					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardMetrics;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;



class BudaMenu implements BudaConstants, BudaConstants.BubbleViewCallback {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,List<MenuItem>>  menu_groups;
private List<MenuData>		    active_menus;

private static final int	MENU_DELTA = 15;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaMenu()
{
   menu_groups = new LinkedHashMap<String,List<MenuItem>>();
   active_menus = new ArrayList<MenuData>();
   BudaRoot.addBubbleConfigurator("BUDAMENU",new MenuConfigurator());
   BudaRoot.addBubbleViewCallback(this);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addMenuItem(String id,ButtonListener callback,Icon icon)
{
   String pfx = "*";
   String name = id;

   int idx = id.indexOf(".");
   if (idx >= 0) {
      pfx = id.substring(0,idx);
      name = id.substring(idx+1);
    }

   List<MenuItem> itms = menu_groups.get(pfx);
   if (itms == null) {
      itms = new ArrayList<MenuItem>();
      menu_groups.put(pfx,itms);
    }

   itms.add(new MenuItem(id,name,callback,icon));
}




/********************************************************************************/
/*										*/
/*	Methods to handle creating search and menu together			*/
/*										*/
/********************************************************************************/

void createMenuAndSearch(BudaRoot br,Point pt,BudaBubble search)
{
   MenuPanel menu = createMenu(pt);
   if (menu != null) {
      menu.setTransparent();
      Dimension sz = menu.getPreferredSize();//search.getPreferredSize();
      //Point p1 = new Point(pt.x + sz.width + MENU_DELTA,pt.y);//-sz.height-MENU_DELTA);
      Point p1 = new Point(pt.x, pt.y-sz.height-MENU_DELTA);
      Rectangle viewport = br.getCurrentViewport();
      if (!viewport.contains(p1)) p1 = new Point(pt.x, pt.y+search.getSize().height+MENU_DELTA);//p1 = new Point(pt.x - menu.getSize().width - MENU_DELTA, pt.y);
      BudaBubble bb = new MenuBubble(menu);
      MenuData md = new MenuData(search,bb);
      active_menus.add(md);
      BudaConstraint mcnst = new BudaConstraint(BudaBubblePosition.DIALOG,p1);
      br.add(bb,mcnst);
    }

   BudaConstraint scnst = new BudaConstraint(BudaBubblePosition.DIALOG,pt);
   br.add(search,scnst);

   search.grabFocus();
}


void noteSearchUsed(BudaBubble bb)
{
   for (MenuData md : active_menus) {
      if (md.searchUsed(bb)) return;
    }
}


void noteMenuUsed(Component c)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   if (bb == null) return;

   for (MenuData md : active_menus) {
      if (md.menuUsed(bb)) break;
    }

   bb.setVisible(false);
}




/********************************************************************************/
/*										*/
/*	Methods to build the actual menu					*/
/*										*/
/********************************************************************************/

MenuPanel createMenu(Point pt)
{
   if (menu_groups.size() == 0) return null;

   MenuPanel pnl = new MenuPanel();
   Set<MenuItem> done = new HashSet<MenuItem>();

   int ct = 0;
   for (List<MenuItem> itms : menu_groups.values()) {
      if (ct > 0) pnl.add(new JSeparator());

      addPopupItems(pt,itms,pnl,null,done);
    }

   Dimension d = pnl.getPreferredSize();
   pnl.setBackground(BUDA_MENU_COLOR);
   pnl.setSize(d);
   // pnl.setVisible(true);

   return pnl;
}



private void addPopupItems(Point pt,List<MenuItem> itms,JComponent menu,String pfx,Set<MenuItem> done)
{
   int pln = (pfx == null ? 0 : pfx.length());

   for (MenuItem mi : itms) {
      if (done.contains(mi)) continue;
      String nm = mi.getName();
      String tnm = nm.substring(pln);
      int idx = tnm.indexOf(".");
      if (idx < 0) {
	 MenuBtn btn = new MenuBtn(mi,pt);
	 menu.add(btn);
       }
      else {
	 String xnm = tnm.substring(0,idx);
	 String npfx = (pfx == null ? xnm : pfx + xnm) + ".";
	 MenuSubBtn m = new MenuSubBtn(xnm);
	 if (menu instanceof MenuPanel) {
	    MenuSubBar mb = new MenuSubBar(m);
	    menu.add(mb);
	  }
	 else menu.add(m);
	 List<MenuItem> nitem = new ArrayList<MenuItem>();
	 for (MenuItem xmi : itms) {
	    if (xmi.getName().startsWith(npfx)) nitem.add(xmi);
	  }
	 addPopupItems(pt,nitem,m.getPopupMenu(),npfx,done);
       }
      done.add(mi);
    }
}



/********************************************************************************/
/*										*/
/*	View callback								*/
/*										*/
/********************************************************************************/

@Override public void focusChanged(BudaBubble bb,boolean set)	{ }

@Override public void bubbleAdded(BudaBubble bb)		{ }

@Override public void bubbleRemoved(BudaBubble bb)
{
   for (MenuData md : active_menus) {
      if (md.searchUsed(bb) || md.menuUsed(bb)) break;
    }
}

@Override public boolean bubbleActionDone(BudaBubble bb)	{ return false; }

@Override public void workingSetAdded(BudaWorkingSet ws)	{ }
@Override public void workingSetRemoved(BudaWorkingSet ws)	{ }

@Override public void doneConfiguration()			{ }




/********************************************************************************/
/*										*/
/*	Class to hold a menu item						*/
/*										*/
/********************************************************************************/

private class MenuItem {

   private String full_id;
   private String item_name;
   private Icon menu_icon;
   private ButtonListener call_back;

   MenuItem(String id,String nm,ButtonListener cb, Icon ii) {
	      full_id = id;
	      item_name = nm;
	      call_back = cb;
	      menu_icon = ii;
	    }

   String getId()				{ return full_id; }
   String getName()				{ return item_name; }
   Icon getIcon()				{ return menu_icon; }
   ButtonListener getCallback() 		{ return call_back; }

}	// end of inner class MenuItem




/********************************************************************************/
/*										*/
/*	MenuPanel class 							*/
/*										*/
/********************************************************************************/

private static class MenuPanel extends SwingGridPanel implements FocusListener,
		BudaBubbleOutputer
{

   private Color fg_color;
   private Color fgt_color;
   private int row_count;

   private static final long serialVersionUID = 1;

   MenuPanel() {
      fg_color = getForeground();
      fgt_color = transparent(fg_color);
      setInsets(1);
      setOpaque(false);

      addMouseListener(new FocusOnEntry());
      addFocusListener(this);
      setFocusable(true);
      row_count = 0;
    }

   @Override public Component add(Component c) {
      addGBComponent(c,0,row_count++,0,1,10,10);
      return c;
    }


   void setTransparent() {
      for (Component c : getComponents()) {
	 if (c instanceof MenuComponent) {
	    MenuComponent b = (MenuComponent) c;
	    b.setTransparent(fgt_color);
	  }
       }
    }

   void setNontransparent() {
      for (Component c : getComponents()) {
	 if (c instanceof MenuComponent) {
	    MenuComponent b = (MenuComponent) c;
	    b.setNontransparent(fg_color);
	  }
       }
    }

   private Color transparent(Color c) {
      int cx = c.getRGB();
      cx &= 0x00ffffff;
      cx |= 0x40000000;
      return new Color(cx,true);
    }

   @Override public void focusGained(FocusEvent e)	{ setNontransparent(); }
   @Override public void focusLost(FocusEvent e)	{ setTransparent(); }

   @Override public String getConfigurator()		{ return "BUDAMENU"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","MENU");
    }

}	// end of inner class MenuPanel




private static interface MenuComponent {

   void setTransparent(Color c);
   void setNontransparent(Color c);

}



private class MenuBtn extends JMenuItem implements MenuComponent, ActionListener {

   private Point start_point;
   private MenuItem for_item;

   private static final long serialVersionUID = 1;

   MenuBtn(MenuItem mi,Point pt) {
      String nm = mi.getName();
      int idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(idx+1);
      if (mi.getIcon() != null) setIcon(mi.getIcon());
      setText(nm);
      for_item = mi;
      start_point = pt;
      setBackground(BUDA_MENU_BACKGROUND_COLOR);
      setFont(BUBBLE_MENU_FONT);
      // setContentAreaFilled(false);
      // enabling this cause the buttons to be green on the mac
      // setBorderPainted(false);
      setActionCommand(mi.getId());
      addActionListener(this);
    }


   @Override public void setTransparent(Color fg) {
      setSelected(false);
      setContentAreaFilled(false);
      setOpaque(false);
      setForeground(fg);
    }

   @Override public void setNontransparent(Color fg) {
      setOpaque(true);
      setContentAreaFilled(true);
      setBackground(BUDA_MENU_BACKGROUND_COLOR);
      setForeground(fg);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      Component c = (Component) evt.getSource();
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
      ButtonListener bc = for_item.getCallback();
      if (bc != null) {
	 BoardMetrics.noteCommand("BUDA","menu_" + for_item.getName());
	 bc.buttonActivated(bba,for_item.getId(),start_point);
       }
      noteMenuUsed(this);
    }

}	// end of inner class MenuBtn



private class MenuSubBtn extends JMenu implements MenuComponent {

   private static final long serialVersionUID = 1;


   MenuSubBtn(String nm) {
      super(nm);
      putClientProperty("JButton.buttonType","toolbar");
      setFont(BUBBLE_MENU_FONT);
      setOpaque(false);
    }

   @Override public void setTransparent(Color fg) {
	   setOpaque(false);
	   setForeground(fg);
    }

   @Override public void setNontransparent(Color fg) {
      setOpaque(true);
      setForeground(fg);
    }
   @Override public void setPopupMenuVisible(boolean b) {
      boolean isVisible = isPopupMenuVisible();
      if (b != isVisible) {
	 if ((b==true) && isShowing()) {
	    getPopupMenu().show(this, getWidth(), 0);
	 } else {
	    getPopupMenu().setVisible(false);
	 }
      }
   }

}	// end of inner class MenuSubBtn



private static final char ARROW = '\u25b6';



private class MenuSubBar extends JMenuBar implements MenuComponent {

   private MenuSubBtn menu_btn;

   private static final long serialVersionUID = 1;

   MenuSubBar(MenuSubBtn btn) {
      menu_btn = btn;

      if (BUBBLE_MENU_FONT.canDisplay(ARROW)) {
	 menu_btn.setText(menu_btn.getText()+'\u25B6');
       }
      else {
	 menu_btn.setText(menu_btn.getText() + " >");
       }

      menu_btn.setHorizontalTextPosition(SwingConstants.LEFT);
      setMargin(new Insets(0,0,0,0));
      setBorderPainted(false);
      setFont(BUBBLE_MENU_FONT);
      add(btn);
    }

   @Override public void setTransparent(Color fg) {
      setOpaque(false);
      menu_btn.setTransparent(fg);
    }

   @Override public void setNontransparent(Color fg) {
      menu_btn.setNontransparent(fg);
    }

}	// end of inner class MenuSubBar




/********************************************************************************/
/*										*/
/*	Menu Bubble class							*/
/*										*/
/********************************************************************************/

private static class MenuBubble extends BudaBubble {

   private static final long serialVersionUID = 1;

   MenuBubble(JComponent cmp) {
      super(cmp,BudaBorder.NONE);
      setOpaque(false);
      setBackground(BUDA_MENU_COLOR);
      setTransient(true);
    }

}	// end of inner class MenuBubble



/********************************************************************************/
/*										*/
/*	MenuData : manage currently active menus and search boxes		*/
/*										*/
/********************************************************************************/

private class MenuData extends ComponentAdapter {

   private BudaBubble search_bubble;
   private BudaBubble menu_bubble;

   MenuData(BudaBubble sb,BudaBubble mb) {
      search_bubble = sb;
      menu_bubble = mb;
      search_bubble.addComponentListener(this);
      menu_bubble.addComponentListener(this);
    }

   boolean searchUsed(BudaBubble bb) {
      if (bb != search_bubble) return false;

      menu_bubble.setVisible(false);
      return true;
    }

   boolean menuUsed(BudaBubble bb) {
      if (bb != menu_bubble) return false;

      search_bubble.setVisible(false);
      return true;
    }

   @Override public void componentShown(ComponentEvent e) {
      if (e.getSource() == search_bubble) {
	 // doesn't work for some reason: wrong focus or invisible?
	 // need to give focus to text area
	 search_bubble.grabFocus();
       }
    }

   @Override public void componentHidden(ComponentEvent e) {
      if (e.getSource() == search_bubble) {
	 menu_bubble.setVisible(false);
       }
      else if (e.getSource() == menu_bubble) {
	 active_menus.remove(this);
       }
    }

}	// end of inner class MenuData



/********************************************************************************/
/*										*/
/*	Configurator for menu bubbles						*/
/*										*/
/********************************************************************************/

private class MenuConfigurator implements BubbleConfigurator {

   @Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml) {
      Element cnt = IvyXml.getChild(xml,"CONTENT");
      String typ = IvyXml.getAttrString(cnt,"TYPE");

      BudaBubble bb = null;

      if (typ.equals("MENU")) {
	 Point p = new Point(IvyXml.getAttrInt(xml,"X"),IvyXml.getAttrInt(xml,"Y"));
	 MenuPanel menu = createMenu(p);
	 if (menu != null) {
	    menu.setTransparent();
	    bb = new MenuBubble(menu);
	  }
       }

      return bb;
    }

   @Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
   @Override public void loadXml(BudaBubbleArea bba,Element root)	{ }


}	// end of inner class MenuConfigurator



}	// end of class BudaMenu




/* end of BudaMenu.java */
