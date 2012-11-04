/********************************************************************************/
/*                                                                              */
/*              BhelpContext.java                                               */
/*                                                                              */
/*      Global context for actions                                              */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.board.*;

import javax.swing.SwingUtilities;

import java.util.*;
import java.awt.*;


class BhelpContext implements BhelpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storag                                                          */
/*                                                                              */
/********************************************************************************/

private Map<String,Object>      value_map;   
private BudaBubbleArea          buda_area;
private Robot                   event_robot;
   

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BhelpContext(BudaBubbleArea bba)
{
   value_map = new HashMap<String,Object>();
   buda_area = bba;
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   try {
      event_robot = new Robot();
    }
   catch (AWTException e) {
      BoardLog.logE("BHELP","ROBOT not available");
    }
   
   Point pt = MouseInfo.getPointerInfo().getLocation();
   SwingUtilities.convertPointFromScreen(pt,br);
   setValue("StartPoint",pt);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BudaBubbleArea getBubbleArea()          { return buda_area; }
BudaRoot getBudaRoot()                  { return BudaRoot.findBudaRoot(buda_area); }
Robot getRobot() throws BhelpException 
{
   if (event_robot == null) throw new BhelpException("Event Simulator not available");
   return event_robot;
}


/********************************************************************************/
/*                                                                              */
/*      Value methos                                                            */
/*                                                                              */
/********************************************************************************/

void setValue(String name,BudaBubble bbl)
{
   if (name != null) value_map.put(name,bbl);
}


void setValue(String name,Point pt)
{
   if (name != null) value_map.put(name, pt);
}

/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

Point getPoint(String var)
{
   Object val = value_map.get(var);
   if (val == null) return null;
   if (val instanceof Point) return ((Point) val);
   else if (val instanceof Rectangle) {
      Rectangle r = (Rectangle) val;
      return new Point(r.x + r.width/2,r.y + r.height/2);
    }
   else if (val instanceof Component) {
      BudaRoot br = BudaRoot.findBudaRoot((Component) val);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(((Component) val));
      Rectangle r = BudaRoot.findBudaLocation(((Component) val));
      Point pt = new Point(r.x + r.width/2,r.y + r.height/2);
      pt = SwingUtilities.convertPoint(bba,pt,br);
    }
   return null;
}
      
      




}       // end of class BhelpContext




/* end of BhelpContext.java */
