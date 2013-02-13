/********************************************************************************/
/*										*/
/*		BhelpAction.java						*/
/*										*/
/*	Action for help demonstrations						*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaHelpRegion;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import javax.speech.*;
import javax.speech.synthesis.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;
import java.util.List;
import java.lang.reflect.*;



abstract class BhelpAction implements BhelpConstants
{


/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

static BhelpAction createAction(Element xml)
{
   BhelpAction rslt = null;

   String typ = IvyXml.getAttrString(xml,"TYPE");
   if (typ == null) return null;
   else if (typ.equals("FINDBUBBLE")) {
      rslt = new FindBubbleAction(xml);
    }
   else if (typ.equals("MOVE")) {
      rslt = new MoveMouseAction(xml);
    }
   else if (typ.equals("MOUSE")) {
      rslt = new MousePressAction(xml);
    }
   else if (typ.equals("SPEECH")) {
      rslt = new SpeechAction(xml);
    }
   else if (typ.equals("BACKGROUND")) {
      rslt = new FindBackgroundAction(xml);
    }
   else if (typ.equals("RESET")) {
      rslt = new ResetAction(xml);
    }
   else if (typ.equals("KEY")) {
      rslt = new KeyAction(xml);
    }

   return rslt;
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
      for (BudaBubble bb : ctx.getBubbleArea().getBubblesInRegion(rv)) {
	 Rectangle r1 = BudaRoot.findBudaLocation(bb);
	 if (!rv.contains(r1)) continue;
	 if (!checkBubbleType(bb)) continue;
	 double score = pt.distance(r1.x + r1.width/2, r1.y + r1.height/2);
	 if (rslt == null || score < best) {
	    rslt = bb;
	    best = score;
	  }
       }
      if (rslt == null) throw new BhelpException("No bubble found");
      ctx.setValue(result_variable,rslt);
    }

   private boolean checkBubbleType(BudaBubble bb) {
      if (bb.isTransient()) return false;
      if (bubble_type == null) return true;
      String cnm = bb.getClass().getName();
      if (cnm.contains(bubble_type)) return true;

      return false;
    }

}	// end of inner class FindBubbleAction




/********************************************************************************/
/*										*/
/*	FindBubble action							*/
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
         // pt = MouseInfo.getPointerInfo().getLocation();
         // SwingUtilities.convertPointFromScreen(pt,br);
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
   
      int delta = 1;		// pixel delta for search
      int incr = 1;		// current increment
      for (int i = 0; i < 100000; ++i) {
         if (checkPoint(ctx,pt)) break;
         pt.x += incr * delta;
         if (checkPoint(ctx,pt)) break;
         pt.y += incr * delta;
         int v = Math.abs(incr) + 1;
         if (incr > 0) incr = -v;
         else incr = v;
       }
   
      if (!checkPoint(ctx,pt)) throw new BhelpException("No space found");
   
      ctx.setValue(result_variable,pt);
    }

   private boolean checkPoint(BhelpContext ctx,Point pt) {
      BudaRoot br = ctx.getBudaRoot();
      Rectangle r = br.getBounds();
      if (pt.x < 0 || pt.x >= r.width) return false;
      if (pt.y < 0 || pt.y >= r.height) return false;
      Component c = SwingUtilities.getDeepestComponentAt(br,pt.x,pt.y);

      for ( ; c != null; c = c.getParent()) {
	 String cnm = c.getClass().getName();
	 BudaHelpRegion bhr = null;
	 if (c instanceof BudaBubbleArea) {
	    BudaBubbleArea bba = (BudaBubbleArea) c;
	    Point pt1 = SwingUtilities.convertPoint(br,pt.x,pt.y,bba);
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
	    if (bhr != null && bhr.getRegion() == BudaConstants.BudaRegion.GROUP_NAME)
	       return true;
	    else if (bhr != null) return false;
	  }
	 else if (area_type.equals("TOPBAR")) {
	    if (cnm.contains("BudaTopBar")) return true;
	  }
	 else if (area_type.equals("OVERVIEW")) {
	    if (cnm.contains("BudaOverviewBar")) return true;
	  }
	 else if (area_type.startsWith("BORDER") || area_type.equals("LINK")) {
	    if (bhr != null && bhr.getRegion().toString().equals(area_type)) return true;
	    else if (bhr != null)
	       return false;
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
/*                                                                              */
/*      KeyAction class                                                         */
/*                                                                              */
/********************************************************************************/

private static class KeyAction extends BhelpAction {
   
   private boolean do_control;
   private boolean do_shift;
   private boolean do_alt;
   private int     key_code;
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
   
}       // end of inner class KeyAction




/********************************************************************************/
/*										*/
/*	SpeechAction class							*/
/*										*/
/********************************************************************************/

private static final String VOICE_NAME = "kevin16";


private static class SpeechAction extends BhelpAction {

   private boolean wait_for;
   private String speech_text;
   private static Synthesizer speech_synth = null;

   SpeechAction(Element xml) {
      super(xml);
      wait_for = IvyXml.getAttrBool(xml,"WAIT");
      speech_text = IvyXml.getTextElement(xml,"TEXT");
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
/*                                                                              */
/*      Reset Action                                                            */
/*                                                                              */
/********************************************************************************/

private static class ResetAction extends BhelpAction {
   
   ResetAction(Element xml) {
      super(xml);
    }
   
   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      ctx.reset();
    }

}       // end of inner class ResetAction



}	// end of class BhelpAction




/* end of BhelpAction.java */
