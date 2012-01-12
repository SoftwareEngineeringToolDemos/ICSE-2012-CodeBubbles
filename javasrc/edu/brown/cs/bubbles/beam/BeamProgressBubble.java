/********************************************************************************/
/*										*/
/*		BeamProgressBubble.java 					*/
/*										*/
/*	Bubble to show progress of long-running tasks				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bump.*;
import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.*;
import java.awt.*;
import java.util.*;


class BeamProgressBubble implements BeamConstants, BumpConstants.BumpProgressHandler
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot	buda_root;
private Map<String,ProgressDisplay> all_displays;
private Map<String,Long> last_id;

private Color		done_color;
private Color		todo_color;

private static final int MAX_RANGE = 512;
private static final long DONE_ID = Long.MAX_VALUE;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamProgressBubble(BudaRoot br)
{
   buda_root = br;

   done_color = new Color(0xffc0ffc0,true);
   todo_color = new Color(0xffc0c0c0,true);

   all_displays = new HashMap<String,ProgressDisplay>();
   last_id = new HashMap<String,Long>();

   BumpClient.getBump().addProgressHandler(this);
}



/********************************************************************************/
/*										*/
/*	Setup bubbles								*/
/*										*/
/********************************************************************************/

private synchronized void startProgress(String id,String task,long sid)
{
   BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
   if (bba == null) return;

   int ct = 1;
   for (ProgressDisplay pd : all_displays.values()) {
      BudaBubbleArea xbba = BudaRoot.findBudaBubbleArea(pd);
      if (xbba == bba) ++ct;
    }

   ProgressDisplay pd = new ProgressDisplay(id,task,sid);
   Dimension d = pd.getPreferredSize();
   Rectangle r = buda_root.getCurrentViewport();
   BudaConstraint bc = new BudaConstraint(BudaConstants.BudaBubblePosition.STATIC,
	 r.x,r.y + r.height - d.height*ct);
   bba.add(pd,bc,0);
   all_displays.put(id,pd);
   pd.setVisible(true);
   BudaBubble bb = BudaRoot.findBudaBubble(pd);
   bb.setVisible(true);
}



/********************************************************************************/
/*										*/
/*	Progress message handling						*/
/*										*/
/********************************************************************************/

@Override synchronized public void handleProgress(long sid,String id,String kind,
      String task,String subtask,double work)
{
   String tnm = task;
   if (subtask != null && subtask.length() > 0) tnm += " (" + subtask + ")";

   // ensure we ignore items that are out of order
   Long lid = last_id.get(id);
   if (lid != null &&  sid <= lid) return;
   last_id.put(id,sid);
   if (last_id.size() > MAX_RANGE) {
      for (Iterator<Long> it = last_id.values().iterator(); it.hasNext(); ) {
	 Long l = it.next();
	 if (sid - l > MAX_RANGE) it.remove();
       }
    }

   if (kind.equals("BEGIN")) {
      ProgressDisplay pd = all_displays.get(id);
      if (pd == null) startProgress(id,tnm,sid);
      else pd.set(tnm,work,sid);
    }
   else if (kind.equals("WORKED") || kind.equals("SUBTASK") || kind.equals("ENDSUBTASK")) {
      ProgressDisplay pd = all_displays.get(id);
      if (pd != null) pd.set(tnm,work,sid);
    }
   else if (kind.equals("DONE")) {
      ProgressDisplay pd = all_displays.remove(id);
      last_id.put(id,DONE_ID);
      if (pd != null) {
	 pd.setVisible(false);
	 Remover rm = new Remover(pd);
	 SwingUtilities.invokeLater(rm);
       }
   }
   else {
      BoardLog.logE("BEAM","Unknown progress message " + kind);
   }

   // handle cancel and uncancel?
}



private static class Remover implements Runnable {

   private ProgressDisplay for_display;

   Remover(ProgressDisplay pd) {
      for_display = pd;
    }

   @Override public void run() {
      for_display.setVisible(false);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(for_display);
      BudaBubble bb = BudaRoot.findBudaBubble(for_display);
      if (bba != null && bb != null) bba.removeBubble(bb);
      else if (bba != null) bba.remove(for_display);
    }

}	// end of inner class Remover



/********************************************************************************/
/*										*/
/*	Progress Display							*/
/*										*/
/********************************************************************************/

private class ProgressDisplay extends JLabel {

   private double part_done;
   private long last_serial;

   private static final long serialVersionUID = 1;

   ProgressDisplay(String id,String txt,long sid) {
      super(txt);
      part_done = 0;
      setOpaque(true);
      setBackground(new Color(0x00000000,true));
      last_serial = sid;
    }

   void set(String text,double done,long sid) {
      if (sid < last_serial) return;
      last_serial = sid;

      if (text != null && !text.equals(getText())) {
	 setText(text);
	 Dimension sz = getPreferredSize();
	 BudaBubble bb = BudaRoot.findBudaBubble(this);
	 if (bb != null) bb.setSize(sz);
	 else setSize(sz);
       }
      part_done = done;
      repaint();
    }

   @Override public void paintComponent(Graphics g) {
      Dimension sz = getSize();
      int w = (int)(sz.width * part_done / 100.0);

      g.setColor(todo_color);
      g.fillRect(0,w,sz.width-w,sz.height);

      g.setColor(done_color);
      g.fillRect(0,0,w,sz.height);

      super.paintComponent(g);
    }

}	// end of inner class Progress Display





}	// end of class BeamProgressBubble




/* end of BeamProgressBubble.java */

