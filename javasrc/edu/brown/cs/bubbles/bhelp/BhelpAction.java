/********************************************************************************/
/*										*/
/*		BhelpAction.java						*/
/*										*/
/*	Action for help demonstrations						*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.xml.*;

import org.w3c.dom.Element;

import javax.speech.*;
import javax.speech.synthesis.*;
import java.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;
import java.util.List;



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

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/



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
         pt = MouseInfo.getPointerInfo().getLocation();
         SwingUtilities.convertPointFromScreen(pt,br);
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
      for (BudaBubble bb : ctx.getBubbleArea().getBubblesInRegion(r)) {
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
         pt = MouseInfo.getPointerInfo().getLocation();
         SwingUtilities.convertPointFromScreen(pt,br);
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
      
      int delta = 5;            // pixel delta for search
      int incr = 1;             // current increment
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
         if (area_type.equals("BUBBLEAREA")) {
            if (c instanceof BudaBubbleArea) return true;
            if (c instanceof BudaBubble) return false;
          }
         else if (area_type.equals("TOPBAR")) {
            if (cnm.contains("BudaTopBar")) return true;
          }
         else if (area_type.equals("OVERVIEW")) {
            if (cnm.contains("BudaOverviewBar")) return true;
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
   private int delay_time;
   private boolean is_jump;
   private List<Point> point_list;

   MoveMouseAction(Element xml) {
      super(xml);
      target_name = IvyXml.getAttrString(xml,"TARGET");
      delay_time = IvyXml.getAttrInt(xml,"DELAY",5);
      is_jump = IvyXml.getAttrBool(xml,"JUMP");
      point_list = null;
      for (Element pe : IvyXml.children(xml,"POINT")) {
         if (point_list == null) point_list = new ArrayList<Point>();
         Point p = new Point(IvyXml.getAttrInt(pe,"X"),IvyXml.getAttrInt(pe,"Y"));
         point_list.add(p);
      }
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      BudaRoot br = ctx.getBudaRoot();
      Point st = MouseInfo.getPointerInfo().getLocation();
      Point tg = ctx.getPoint(target_name);
      SwingUtilities.convertPointToScreen(tg,br);
   
      Path2D.Float path = new Path2D.Float();
      path.moveTo(st.getX(),st.getY());
      if (point_list != null) {
         for (Point p : point_list) {
            path.lineTo(st.getX() + p.x, st.getY() + p.y);
         }
      }
      if (tg != null) path.lineTo(tg.getX(),tg.getY());
   
      Robot r = ctx.getRobot();
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
               moveMouse(r,x0,y0,coords[0],coords[1]);
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

   private void moveMouse(Robot r,double x0,double y0,double x1,double y1) {
      if (is_jump) {
         r.mouseMove((int) x1,(int) y1);
         return;
       }
   
      double len = Point.distance(x0,y0,x1,y1);
      double steps = Math.ceil(len);
      for (int i = 0; i <= steps; ++i) {
         double d = i/steps;
         double x = x0 + d * (x1-x0);
         double y = y0 + d * (y1-y0);
         r.mouseMove((int) x,(int) y);
         if (delay_time > 0) r.delay(delay_time);
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
      mouse_buttons = IvyXml.getAttrInt(xml,"BUTTON",1);
      mouse_up = IvyXml.getAttrBool(xml,"UP");
      mouse_down = IvyXml.getAttrBool(xml,"DOWN",!mouse_up);
    }

   @Override void executeAction(BhelpContext ctx) throws BhelpException {
      Robot r = ctx.getRobot();
      if (mouse_down) r.mousePress(mouse_buttons);
      if (mouse_up) r.mouseRelease(mouse_buttons);
    }

}	// end of inner class MousePressAction



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
      BoardLog.logD("BHELP","ENGINE STATE: " + speech_synth.getEngineState() + " " +
		       Synthesizer.ALLOCATED + " " + Synthesizer.QUEUE_EMPTY + " " +
		       Synthesizer.DEALLOCATED + " " + Synthesizer.QUEUE_NOT_EMPTY);
      if (speech_synth.testEngineState(Synthesizer.DEALLOCATED)) {
	 return;
       }
      speech_synth.waitEngineState(Synthesizer.QUEUE_EMPTY);
      BoardLog.logD("BHELP","ENGINE STATE1: " + speech_synth.getEngineState());
    }



}	// end of inner class SpeechAction


}	// end of class BhelpAction




/* end of BhelpAction.java */
