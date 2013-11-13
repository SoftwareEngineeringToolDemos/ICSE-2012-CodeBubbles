/********************************************************************************/
/*										*/
/*		BhelpAction.java						*/
/*										*/
/*	Action for help demonstrations						*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaHelpRegion;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.speech.Central;
import javax.speech.synthesis.*;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;



abstract class BhelpAction implements BhelpConstants
{


/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static List<BhelpAction> createAction(Element xml)
{
   List<BhelpAction> res = new ArrayList<BhelpAction>();

   String typ = IvyXml.getAttrString(xml,"TYPE");
   if (typ == null) return null;
   switch(typ) {
      case "FINDBUBBLE":
	 res.add(new FindBubbleAction(xml));
	 break;
      case "FINDGROUP":
	 res.add(new FindGroupAction(xml));
	 break;
      case "MOVE":
	 res.add(new MoveMouseAction(xml));
	 break;
      case "MOUSE":
	 res.add(new MousePressAction(xml));
	 break;
      case "SPEECH":
	 res.add(new SpeechAction(xml));
	 break;
      case "BACKGROUND":
	 res.add(new FindBackgroundAction(xml));
	 break;
      case "RESET":
	 res.add(new ResetAction(xml));
	 break;
      case "KEY":
	 res.add(new KeyAction(xml));
	 break;
      case "TYPE":
	 res.add(new TypeAction(xml));
	 break;
      case "PAUSE":
	 res.add(new PauseAction(xml));
	 break;
      case "LOOP":
	 res.addAll(handleLoopingActions(xml));
	 break;
      default:
	 return null;
    }
   return res;
}



/**
 *  recursively (mutually w/ createAction) populates actions list with looped actions
 */

private static List<BhelpAction> handleLoopingActions(Element xml)
{
   List<BhelpAction> res = new ArrayList<BhelpAction>();
   Integer iters = IvyXml.getAttrInteger(xml, "ITERS");
   for(int i = 0; i < iters; ++i) {
      for(Element element : IvyXml.children(xml,"ACTION")) {
	 List<BhelpAction> act = createAction(element);
	 if (act != null) res.addAll(act);
       }
    }
   return res;
}




static PauseAction speechToPause(BhelpAction ba)
{
   SpeechAction sa = (SpeechAction) ba;
   Integer p_duration = sa.getEquivalentPause();
   p_duration = p_duration == null ? 0 : p_duration;
   String new_xml = "<ACTION TYPE='PAUSE' DURATION='" + p_duration + "' />";
   Element x = IvyXml.convertStringToXml(new_xml);

   return new PauseAction(x);
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static double	speed_delta = 1.0;

private final static double	MAC_DELTA = 2.0;

static {
   String osv = System.getProperty("java.vm.vendor");
   if (osv.contains("Apple")) speed_delta = MAC_DELTA;
   speed_delta = BoardProperties.getProperties("Bhelp").getDouble("Bhelp.speed.delta",speed_delta);
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BhelpAction(Element xml)
{
}



/********************************************************************************/
/*										*/
/*	Evaluation methods							*/
/*										*/
/********************************************************************************/

abstract void executeAction(BhelpContext ctx) throws BhelpException;

void executeStopped(BhelpContext ctx) throws BhelpException
{ }



private static boolean isChangeableBubble(BudaBubble bb,String type)
{
   if (bb.isTransient()) return false;
   if (bb.isDocked()) return false;
   if (bb.isUserPos()) return false;
   if (type == null) return true;
   String cnm = bb.getClass().getName();
   if (cnm.contains(type)) return true;

   return false;
 }



/********************************************************************************/
/*										*/
/*	FindGroup actio 							*/
/*										*/
/********************************************************************************/

private static class FindGroupAction extends BhelpAction {

   private boolean near_current;
   private boolean near_left;
   private boolean near_right;
   private boolean near_top;
   private boolean near_bottom;
   private String  near_var;
   private String result_variable;

   FindGroupAction(Element xml) {
      super(xml);
      near_current = IvyXml.getAttrBool(xml,"MOUSE");
      near_left = IvyXml.getAttrBool(xml,"LEFT");
      near_right = IvyXml.getAttrBool(xml,"RIGHT");
      near_top = IvyXml.getAttrBool(xml,"TOP");
      near_bottom = IvyXml.getAttrBool(xml,"BOTTOM");
      result_variable = IvyXml.getAttrString(xml,"SET");
      near_var = IvyXml.getAttrString(xml,"NEAR");
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      BudaBubble result = null;
      double best = 0;

      BudaBubbleArea bba = ctx.getBubbleArea();
      BudaRoot br = ctx.getBudaRoot();
      Rectangle r = br.getBounds();
      Rectangle rv = bba.getViewport();

      Point pt = null;
      if (near_current) {
	 pt = ctx.getMouse();
       }
      else if (near_var != null) {
	 pt = ctx.getPoint(near_var);
       }

      if (pt == null) {
	 int x = r.x + r.width/2;
	 int y = r.y + r.height/2;
	 if (near_left) x = 0;
	 else if (near_right) x = r.x + r.width;
	 if (near_top) y = 0;
	 else if (near_bottom) y = r.y + r.height;
	 pt = new Point(x, y);
       }

      pt = SwingUtilities.convertPoint(br,pt,bba);
      for(BudaBubbleGroup bbg : ctx.getBubbleArea().getBubbleGroups()) {
	 for(BudaBubble bb : bbg.getBubbles()) {
	    Rectangle r1 = BudaRoot.findBudaLocation(bb);
	    if (!rv.contains(r1)) continue;
	    double score = pt.distance(r1.x + r1.width/2, r1.y + r1.height/2);
	    if (result == null || score < best) {
	       result = bb;
	       best = score;
	     }
	  }
       }
      if (result == null) throw new BhelpException("No bubble group found");

      ctx.setValue(result_variable, result);
    }

}	// end of inner class FindGroupAction




/********************************************************************************/
/*										*/
/*	FindBubble action							*/
/*										*/
/********************************************************************************/

private static class FindBubbleAction extends BhelpAction {

   private boolean near_current;
   private boolean near_left;
   private boolean near_right;
   private boolean near_top;
   private boolean near_bottom;
   private String  near_var;
   private boolean use_other;
   private String bubble_type;
   private String result_variable;

   FindBubbleAction(Element xml) {
      super(xml);
      near_current = IvyXml.getAttrBool(xml,"MOUSE");
      near_left = IvyXml.getAttrBool(xml,"LEFT");
      near_right = IvyXml.getAttrBool(xml,"RIGHT");
      near_top = IvyXml.getAttrBool(xml,"TOP");
      near_bottom = IvyXml.getAttrBool(xml,"BOTTOM");
      bubble_type = IvyXml.getAttrString(xml,"CLASS");
      result_variable = IvyXml.getAttrString(xml,"SET");
      near_var = IvyXml.getAttrString(xml,"NEAR");
      use_other = IvyXml.getAttrBool(xml,"OTHER");
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      BudaBubble rslt = null;
      double best = 0;

      BudaBubbleArea bba = ctx.getBubbleArea();
      BudaRoot br = ctx.getBudaRoot();
      Rectangle r = br.getBounds();
      Rectangle rv = bba.getViewport();

      Point pt = null;
      if (near_current) {
	 pt = ctx.getMouse();
       }
      else if (near_var != null) pt = ctx.getPoint(near_var);

      if (pt == null) {
	 int x = r.x + r.width/2;
	 int y = r.y + r.height/2;
	 if (near_left) x = 0;
	 else if (near_right) x = r.x + r.width;
	 if (near_top) y = 0;
	 else if (near_bottom) y = r.y + r.height;
	 pt = new Point(x,y);
       }

      pt = SwingUtilities.convertPoint(br,pt,bba);

      BudaBubble me = null;
      if (use_other) {
	 Rectangle rgn = new Rectangle(pt.x,pt.y,1,1);
	 Collection<BudaBubble> us = ctx.getBubbleArea().getBubblesInRegion(rgn);
	 if (us != null && !us.isEmpty()) {
	    for (BudaBubble xbb : us) {
	       me = xbb;
	       break;
	     }
	  }
       }

      for (BudaBubble bb : ctx.getBubbleArea().getBubblesInRegion(rv)) {
	 Rectangle r1 = BudaRoot.findBudaLocation(bb);
	 if (!rv.contains(r1)) continue;
	 if (!checkBubbleType(bb)) continue;
	 if (bb == me) continue;
	 // should detect if bb is actually visible on the screen or if it is obscured
	 double score = pt.distance(r1.x + r1.width/2, r1.y + r1.height/2);
	 if (rslt == null || score < best) {
	    rslt = bb;
	    best = score;
	  }
       }
      if (rslt == null)
	 throw new BhelpException("No bubble found: " + pt + " " + rv + " " + r + " " +
				     ctx.getMouse() + " " +
				     SwingUtilities.convertPoint(br,ctx.getMouse(),bba));
      ctx.setValue(result_variable,rslt);
    }

   private boolean checkBubbleType(BudaBubble bb) {
      return isChangeableBubble(bb,bubble_type);
    }

}	// end of inner class FindBubbleAction




/********************************************************************************/
/*										*/
/*	FindBackground action							*/
/*										*/
/********************************************************************************/

private static class FindBackgroundAction extends BhelpAction {

   private boolean near_current;
   private boolean near_left;
   private boolean near_right;
   private boolean near_top;
   private boolean near_bottom;
   private String  near_var;
   private String area_type;
   private String result_variable;
   private String bubble_type;

   FindBackgroundAction(Element xml) {
      super(xml);
      near_current = IvyXml.getAttrBool(xml,"MOUSE");
      near_left = IvyXml.getAttrBool(xml,"LEFT");
      near_right = IvyXml.getAttrBool(xml,"RIGHT");
      near_top = IvyXml.getAttrBool(xml,"TOP");
      near_bottom = IvyXml.getAttrBool(xml,"BOTTOM");
      area_type = IvyXml.getAttrString(xml,"AREA");
      result_variable = IvyXml.getAttrString(xml,"SET");
      near_var = IvyXml.getAttrString(xml,"NEAR");
      bubble_type = IvyXml.getAttrString(xml,"CLASS");
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      BudaRoot br = ctx.getBudaRoot();
      Rectangle r = br.getBounds();

      Point pt = null;
      if (near_current) {
	 pt = ctx.getMouse();
	 if (pt.x < 0) pt.x = 0;
	 if (pt.y < 0) pt.y = 0;
	 if (pt.x > br.getWidth()) pt.x = br.getWidth();
	 if (pt.y > br.getHeight()) pt.y = br.getHeight();
       }
      else if (near_var != null) pt = ctx.getPoint(near_var);
      if (pt == null) {
	 int x = r.x + r.width/2;
	 int y = r.y + r.height/2;
	 if (near_left) x = 0;
	 else if (near_right) x = r.x + r.width;
	 if (near_top) y = 0;
	 else if (near_bottom) y = r.y + r.height;
	 pt = new Point(x,y);
       }

      Point pts = new Point(pt);
      int delta = 1;		// pixel delta for search
      int incr = 1;		// current increment
      for (int i = 0; i < 40000; ++i) {
	 if (checkPoint(ctx,pt)) break;
	 boolean fnd = false;
	 for (int j = 0; j < Math.abs(incr); ++j) {
	    pt.x += (incr > 0 ? delta : -delta);
	    if (checkPoint(ctx,pt)) { fnd = true; break; }
	  }
	 if (fnd) break;
	 for (int j = 0; j < Math.abs(incr); ++j) {
	    pt.y += (incr > 0 ? delta : -delta);
	    if (checkPoint(ctx,pt)) { fnd = true; break; }
	  }
	 if (fnd) break;
	 int v = Math.abs(incr) + 1;
	 if (incr > 0) incr = -v;
	 else incr = v;
       }

      if (!checkPoint(ctx,pt)) throw new BhelpException("No space found starting at " + pts);

      ctx.setValue(result_variable,pt);
    }

   private boolean checkPoint(BhelpContext ctx,Point pt) {
      BudaRoot br = ctx.getBudaRoot();
      Rectangle r = br.getBounds();
      Point pt1 = null;
      BudaBubbleArea bba = null;
      if (pt.x < 0 || pt.x >= r.width) return false;
      if (pt.y < 0 || pt.y >= r.height) return false;
      Component c = SwingUtilities.getDeepestComponentAt(br,pt.x,pt.y);

      for ( ; c != null; c = c.getParent()) {
	 String cnm = c.getClass().getName();
	 BudaHelpRegion bhr = null;
	 if (c instanceof BudaBubbleArea) {
	    bba = (BudaBubbleArea) c;
	    pt1 = SwingUtilities.convertPoint(br,pt.x,pt.y,bba);
	    bhr = bba.getHelpRegion(pt1);
	  }

	 if (area_type.equals("BUBBLEAREA")) {
	    if (bhr != null && bhr.getRegion() == BudaConstants.BudaRegion.NONE) return true;
	    else if (bhr != null) return false;
	  }
	 else if (area_type.equals("GROUP")) {
	    if (bhr != null && bhr.getRegion() == BudaConstants.BudaRegion.GROUP &&
		   bhr.getGroup() != null && bhr.getBubble() == null) return true;
	    else if (bhr != null) return false;
	  }
	 else if (area_type.equals("GROUPNAME")) {
	    if (bhr != null && bhr.getRegion() == BudaConstants.BudaRegion.GROUP_NAME) {
	       //Make sure points around this point are ALSO in the group name area.
	       //This is to ensure that if this action is followed by a typing action,
	       //the mouse is ACTUALLY in the title box and not slightly outside.
	       if (bba != null && pt1 != null) {
		  for (int xdrift = -5; xdrift < 5; ++xdrift) {
		     for (int ydrift = -5; ydrift < 5; ++ydrift) {
			Point check = new Point(pt1.x + xdrift, pt1.y + ydrift);
			bhr = bba.getHelpRegion(check);
			if (bhr == null || bhr.getRegion() != BudaConstants.BudaRegion.GROUP_NAME) {
			   return false;
			 }
		      }
		   }
		  return true;
		}
	       else return false;
	     }
	    else if (bhr != null) return false;
	  }
	 else if (area_type.equals("TOPBAR")) {
	    if (cnm.contains("BudaTopBar")) return true;
	  }
	 else if (area_type.startsWith("TOPBARWORKINGSET")) {
	    if (cnm.contains("BudaTopBar")) {
	       BudaTopBar temp_tb = c instanceof BudaTopBar ? (BudaTopBar) c : null;
	       if (temp_tb != null) {
		  if (temp_tb.isOverWorkingSet(pt.x)) {
		     //Make sure we're at edge
		     if (area_type.equals("TOPBARWORKINGSET_EAST")) {
			Point check = new Point(pt.x, pt.y);
			int count = 0;
			for ( ; temp_tb.isOverWorkingSet(check.x) ; ++check.x) {
			   if (++count > 1) return false;
			 }
		      }
		     //Or the other edge
		     else if (area_type.equals("TOPBARWORKINGSET_WEST")) {
			Point check = new Point(pt.x, pt.y);
			int count = 0;
			for ( ; temp_tb.isOverWorkingSet(check.x) ; --check.x) {
			   if (++count > 1) return false;
			 }
		      }
		     //Make sure there is a five pixel buffer on both sides
		     else if (area_type.equals("TOPBARWORKINGSET_INSIDE")) {
			Point check = new Point(pt.x, pt.y);
			for (int offset = -5; offset <= 5; ++offset) {
			   if (!temp_tb.isOverWorkingSet(check.x + offset)) return false;
			 }
		      }

		     return true;
		   }
		}
	     }
	  }
	 else if (area_type.equals("POPUPMENU")) {
	    if (c instanceof JPopupMenu) {
	       Point where = SwingUtilities.convertPoint(c, c.getLocation(), br);
	       return pt.equals(new Point(where.x + 5, where.y + 10));
	     }
	  }
	 else if (area_type.equals("BUBBLEMENU")) {
	    JPopupMenu bubble_menu = br.getTopBar().getBubbleMenu();
	    if (bubble_menu != null) {
	       Point where = SwingUtilities.convertPoint(bubble_menu, bubble_menu.getLocation(), br);
	       return where.equals(pt);
	     }
	  }
	 else if (area_type.equals("WORKINGSETMENU")) {
	    JPopupMenu workingset_menu = br.getTopBar().getWorkingsetMenu();
	    if (workingset_menu != null && workingset_menu.isVisible()) {
	       Point where = SwingUtilities.convertPoint(workingset_menu, workingset_menu.getLocation(), br);
	       return where.equals(pt);
	     }
	  }
	 else if (area_type.equals("OVERVIEW")) {
	    if (cnm.contains("BudaOverviewBar")) return true;
	  }
	 else if (area_type.startsWith("BORDER") || area_type.equals("LINK")) {
	    if (bhr != null && bhr.getRegion().toString().equals(area_type)) {
	       if (area_type.startsWith("BORDER")) {
		  BudaBubble bb = bhr.getBubble();
		  if (bb != null && isChangeableBubble(bb,bubble_type))
		     return true;
		}
	       else if (area_type.startsWith("LINK")) {
		  return true;
		}
	     }
	    else if (bhr != null) {
	       return false;
	    }
	  }
       }
      return false;
    }

}	// end of inner class FindBackgroundAction




/********************************************************************************/
/*										*/
/*	MoveMouseAction 							*/
/*										*/
/********************************************************************************/

private static class MoveMouseAction extends BhelpAction {

   private String target_name;
   private double delay_time;
   private boolean is_jump;
   private List<Point> point_list;

   MoveMouseAction(Element xml) {
      super(xml);
      target_name = IvyXml.getAttrString(xml,"TARGET");
      double v0 = BoardProperties.getProperties("Bhelp").getDouble("Bhelp.move.delay",1);
      delay_time = IvyXml.getAttrDouble(xml,"DELAY",v0) * speed_delta;
      // BoardLog.logD("BHELP","MOVE SPEED " + delay_time);
      is_jump = IvyXml.getAttrBool(xml,"JUMP");
      point_list = null;
      for (Element pe : IvyXml.children(xml,"POINT")) {
	 if (point_list == null) point_list = new ArrayList<Point>();
	 Point p = new Point(IvyXml.getAttrInt(pe,"X"),IvyXml.getAttrInt(pe,"Y"));
	 point_list.add(p);
      }
    }

   @Override void executeStopped(BhelpContext ctx) throws BhelpException {
      Point tg = ctx.getPoint(target_name);
      if (tg != null) ctx.mouseMove(tg.x,tg.y);
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      Point st = ctx.getMouse();
      Point tg = ctx.getPoint(target_name);

      Path2D.Float path = new Path2D.Float();
      path.moveTo(st.getX(),st.getY());
      if (point_list != null) {
	 for (Point p : point_list) {
	    path.lineTo(st.getX() + p.x, st.getY() + p.y);
	 }
      }
      if (tg != null) path.lineTo(tg.getX(),tg.getY());

      double x0 = 0;
      double y0 = 0;
      double [] coords = new double[6];
      for (PathIterator pi = path.getPathIterator(null,1); !pi.isDone(); pi.next()) {
	 switch (pi.currentSegment(coords)) {
	    case PathIterator.SEG_MOVETO :
	       x0 = coords[0];
	       y0 = coords[1];
	       break;
	    case PathIterator.SEG_LINETO :
	       moveMouse(ctx,x0,y0,coords[0],coords[1]);
	       x0 = coords[0];
	       y0 = coords[1];
	       break;
	    case PathIterator.SEG_CLOSE :
	       break;
	    default :
	       break;
	  }
       }
    }

   private void moveMouse(BhelpContext ctx,double x0,double y0,double x1,double y1)
	throws BhelpException {
      if (is_jump) {
	 ctx.mouseMove((int) x1,(int) y1);
	 return;
       }

      double delay = 0;
      double len = Point.distance(x0,y0,x1,y1);
      double steps = Math.ceil(len);
      for (int i = 0; i <= steps; ++i) {
	 double d = i;
	 if (steps > 0) d /= steps;
	 double x = x0 + d * (x1-x0);
	 double y = y0 + d * (y1-y0);
	 ctx.mouseMove((int) x,(int) y);
	 delay += delay_time;
	 if (delay >= 1) {
	    int di = (int) delay;
	    delay -= di;
	    ctx.delay(di);
	  }
	 if (ctx.isStopped()) break;
       }
    }

}	// end of inner class MoveMouseAction



/********************************************************************************/
/*										*/
/*	MousePressAction class							*/
/*										*/
/********************************************************************************/

private static class MousePressAction extends BhelpAction {

   private int mouse_buttons;
   private boolean mouse_down;
   private boolean mouse_up;

   MousePressAction(Element xml) {
      super(xml);
      String btns = IvyXml.getAttrString(xml,"BUTTON");
      if (btns != null && btns.length() > 0 && Character.isDigit(btns.charAt(0))) {
	 mouse_buttons = IvyXml.getAttrInt(xml,"BUTTON",1);
       }
      else if (btns != null) {
	 mouse_buttons = 0;
	 if (btns.contains("LEFT")) mouse_buttons |= InputEvent.BUTTON1_MASK;
	 if (btns.contains("RIGHT")) mouse_buttons |= InputEvent.BUTTON3_MASK;
	 if (btns.contains("MIDDLE")) mouse_buttons |= InputEvent.BUTTON2_MASK;
       }
      mouse_up = IvyXml.getAttrBool(xml,"UP");
      mouse_down = IvyXml.getAttrBool(xml,"DOWN",!mouse_up);
    }

   @Override void executeStopped(BhelpContext ctx) throws BhelpException {
      if (mouse_up && !mouse_down) ctx.mouseRelease(mouse_buttons);
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      Point pt = ctx.getMouse();
      ctx.mouseMove(pt.x,pt.y);
      if (mouse_down) ctx.mousePress(mouse_buttons);
      if (mouse_up) ctx.mouseRelease(mouse_buttons);
    }

}	// end of inner class MousePressAction





/********************************************************************************/
/*										*/
/*	KeyAction class 							*/
/*										*/
/********************************************************************************/

private static class KeyAction extends BhelpAction {

   private boolean do_control;
   private boolean do_shift;
   private boolean do_alt;
   private int	   key_code;
   private boolean do_press;
   private boolean do_release;

   KeyAction(Element xml) {
      super(xml);
      do_control = IvyXml.getAttrBool(xml,"CONTROL");
      do_shift = IvyXml.getAttrBool(xml,"SHIFT");
      do_alt = IvyXml.getAttrBool(xml,"ALT");
      key_code = IvyXml.getAttrInt(xml,"CODE",0);
      do_press = IvyXml.getAttrBool(xml,"DOWN");
      do_release = IvyXml.getAttrBool(xml,"UP");
      if (!do_press && !do_release) {
	 do_press = true;
	 do_release = true;
       }
      String knm = IvyXml.getAttrString(xml,"KEY");
      if (key_code == 0 && knm != null) {
	 if (!knm.startsWith("VK_")) knm = "VK_" + knm;
	 try {
	    Field f = KeyEvent.class.getField(knm);
	    key_code = f.getInt(null);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BHELP","Problem with key name: " + knm + ": " + t);
	  }
       }
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      if (do_press) {
	 if (do_control) ctx.keyPress(KeyEvent.VK_CONTROL);
	 if (do_shift) ctx.keyPress(KeyEvent.VK_SHIFT);
	 if (do_alt) ctx.keyPress(KeyEvent.VK_ALT);
	 if (key_code != 0) ctx.keyPress(key_code);
       }
      if (do_release) {
	 if (key_code != 0) ctx.keyRelease(key_code);
	 if (do_alt) ctx.keyRelease(KeyEvent.VK_ALT);
	 if (do_shift) ctx.keyRelease(KeyEvent.VK_SHIFT);
	 if (do_control) ctx.keyRelease(KeyEvent.VK_CONTROL);
       }
    }

}	// end of inner class KeyAction




/********************************************************************************/
/*										*/
/*	TypeAction class							*/
/*										*/
/********************************************************************************/

private static class TypeAction extends BhelpAction {

   private String text_to_enter;
   private long delay_milis;
   private int key_code;
   private boolean upper_case;
   private boolean back_space;

   TypeAction(Element xml) {
      super(xml);
      text_to_enter = IvyXml.getAttrString(xml,"TEXT");
      delay_milis = IvyXml.getAttrLong(xml,"DELAY");
      back_space = IvyXml.getAttrBool(xml,"BACKSPACE");
      upper_case = false;
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      pause(50);
      String knm = "";
      for (char c : (text_to_enter).toCharArray()) {
	 if (Character.isLowerCase(c)) {  //Lowercase letters
	    knm = "VK_" + (char)(c - 32);
	  }
	 else if (Character.isUpperCase(c)) {	    //Uppercase letters
	    knm = "VK_" + c;
	    upper_case = true;
	  }
	 else if (Character.isSpaceChar(c)) {	    //Space
	    knm = "VK_SPACE";
	  }
	 try {
	    Field f = KeyEvent.class.getField(knm);
	    key_code = f.getInt(null);
	  }
	 catch (Throwable t) {
	    BoardLog.logE("BHELP", "(TypeAction) Problem with key name: " + knm + ": " + t);
	  }

	 if (upper_case) ctx.keyPress(KeyEvent.VK_SHIFT);
	 if (key_code != 0) ctx.keyPress(key_code);

	 pause(20);

	 if (upper_case) ctx.keyRelease(KeyEvent.VK_SHIFT);
	 if (key_code != 0) ctx.keyRelease(key_code);

	 pause(delay_milis);
       }

      pause(500);

      if (back_space) {
	 for(int i = 0; i < text_to_enter.length(); ++i) {
	    ctx.keyPress(KeyEvent.VK_BACK_SPACE);
	    pause(20);
	    ctx.keyRelease(KeyEvent.VK_BACK_SPACE);
	  }
       }
    }

   private void pause(long milliseconds) {
      long time = System.currentTimeMillis();
      while(true) {
	 long test = System.currentTimeMillis();
	 if (test - time >= milliseconds) {
	    break;
	  }
       }
    }

}	// end of inner class TypeAction




/********************************************************************************/
/*										*/
/*	PauseAction class							*/
/*										*/
/********************************************************************************/

private static class PauseAction extends BhelpAction {

   private long duration;

   PauseAction(Element xml) {
      super(xml);
      duration = IvyXml.getAttrLong(xml,"DURATION");
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      try {
	 Thread.sleep(duration);
       }
      catch (InterruptedException e) {
	 BoardLog.logE("BHELP", "(PauseAction) Thread interrupted.");
       }
    }

}	// end of inner class PauseAction



/********************************************************************************/
/*										*/
/*	SpeechAction class							*/
/*										*/
/********************************************************************************/

private static final String VOICE_NAME = "kevin16";


private static class SpeechAction extends BhelpAction {

   private boolean wait_for;
   private int	equiv_pause;
   private String speech_text;
   private static Synthesizer speech_synth = null;

   SpeechAction(Element xml) {
      super(xml);
      wait_for = IvyXml.getAttrBool(xml,"WAIT");
      speech_text = IvyXml.getTextElement(xml,"TEXT");
      equiv_pause = IvyXml.getAttrInt(xml,"PAUSEFOR",0);

      File f = new File(System.getProperty("user.home"));
      File f1 = new File(f,"speech.properties");
      if (!f1.exists()) {
	 File f2 = new File(BoardSetup.getSetup().getLibraryPath("speech.properties"));
	 try {
	    FileReader fr = new FileReader(f2);
	    FileWriter fw = new FileWriter(f1);
	    char [] buf = new char[16384];
	    for ( ; ; ) {
	       int ln = fr.read(buf);
	       if (ln <= 0) break;
	       fw.write(buf,0,ln);
	     }
	    fr.close();
	    fw.close();
	  }
	 catch (IOException e) {
	    BoardLog.logE("BHELP","Problem setting up speech.properties",e);
	  }
       }
      try {
	 if (speech_synth == null) {
	    SynthesizerModeDesc desc = new SynthesizerModeDesc(null,"general",Locale.US,null,null);
	    speech_synth = Central.createSynthesizer(desc);
	    if (speech_synth == null) throw new BhelpException("Speech not available");
	    speech_synth.allocate();
	    speech_synth.resume();
	    setVoice();
	  }
       }
      catch (Exception e) {
	 BoardLog.logE("BHELP","Problem setting up speech synthesizer",e);
       }
    }

   int getEquivalentPause()		{ return equiv_pause; }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      if (speech_synth == null) return;
      try {
	 waitFor();
	 if (!ctx.checkMouse()) return;
	 if (speech_text != null) {
	    speech_synth.speakPlainText(speech_text,null);
	  }
	 if (wait_for) waitFor();
       }
      catch (Exception e) {
	 throw new BhelpException("Problem with speech",e);
       }
    }

   private void setVoice() throws Exception {
      SynthesizerModeDesc desc = (SynthesizerModeDesc) speech_synth.getEngineModeDesc();
      Voice [] voices = desc.getVoices();
      Voice voice = null;
      for (int i = 0; i < voices.length; ++i) {
	 if (voices[i].getName().equals(VOICE_NAME)) {
	    voice = voices[i];
	    break;
	  }
       }
      if (voice == null) return;
      speech_synth.getSynthesizerProperties().setVoice(voice);
    }

   private void waitFor() throws Exception {
      if (speech_synth.testEngineState(Synthesizer.DEALLOCATED)) {
	 return;
       }
      speech_synth.waitEngineState(Synthesizer.QUEUE_EMPTY);
    }

}	// end of inner class SpeechAction



/********************************************************************************/
/*										*/
/*	Reset Action								*/
/*										*/
/********************************************************************************/

private static class ResetAction extends BhelpAction {

   ResetAction(Element xml) {
      super(xml);
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      ctx.reset();
    }

}	// end of inner class ResetAction



}	// end of class BhelpAction




/* end of BhelpAction.java */
