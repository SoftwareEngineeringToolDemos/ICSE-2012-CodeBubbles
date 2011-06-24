package edu.brown.cs.bubbles.bgta.educhat;

import javax.swing.Icon;

import org.jivesoftware.smack.XMPPException;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.bass.BassConstants.BassNameType;
import edu.brown.cs.bubbles.bgta.BgtaFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.board.BoardImage;

class Course extends BassNameBase {
   
   private String course_name;
   private String ta_chat_jid;
   private Role role;
   
   public Course(String a_course_name, String a_jid, Role r)
   {
      course_name = a_course_name;
      ta_chat_jid = a_jid;
      role = r;
   }
   @Override
   public BudaBubble createBubble() {
      switch(role)
      {
      case STUDENT:
	 return new EduchatTicketSubmitBubble();
      case TA:
	 TAXMPPClient c = new TAXMPPClient("codebubbles", "brownbears", "jabber.org", "TA1");
	 try {
	       c.connect();
	    } catch (XMPPException e) {
	       // TODO Auto-generated catch block
	       e.printStackTrace();
	    }
	 return new EduchatTicketListBubble(c.getTickets());
      default:
	 return null;
      }
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
      
      switch(role)
      {
      case STUDENT:
	 return BassConstants.BASS_COURSE_LIST_NAME + ".Get " + course_name + " help";
      case TA:
	 return BassConstants.BASS_COURSE_LIST_NAME + ".Enable " + course_name + " chat hours";
      default:
	 return BassConstants.BASS_COURSE_LIST_NAME + course_name;
      }
   }
   
   @Override
   public Icon getDisplayIcon()
   {
      switch(role)
      {
      case STUDENT:
	 return BoardImage.getIcon("question");
      case TA:
	 return BoardImage.getIcon("contents_view"); 
      default:
	 return BoardImage.getIcon("contents_view");
      }
      
   }
   
   /**
    * Defines what role the user 
    * has in the given class (TA or student)
    * @author akovacs
    *
    */
   enum Role {
      	STUDENT,
      	TA
   }
}
