package edu.brown.cs.bubbles.bgta.educhat;

import javax.swing.Icon;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.bass.BassConstants.BassNameType;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.board.BoardImage;

class Course extends BassNameBase {
   
   private String course_name;
   private String ta_chat_jid;
   
   public Course(String a_course_name, String a_jid)
   {
      course_name = a_course_name;
      ta_chat_jid = a_jid;
   }
   @Override
   public BudaBubble createBubble() {
      return new EduchatTicketBubble();
   }

   @Override
   protected String getKey() {
      return "key";
   }

   @Override
   protected String getParameters() {
      return null;
   }

   @Override
   public String getProject() {
      //return "Courses";
      return null;
   }

   @Override
   protected String getSymbolName() {
      return BassConstants.BASS_COURSE_LIST_NAME + "." + course_name;
   }
   
   @Override
   public Icon getDisplayIcon()
   {
      return BoardImage.getIcon("contents_view");
   }
}
