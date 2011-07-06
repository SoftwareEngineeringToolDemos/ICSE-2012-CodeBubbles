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

import javax.naming.OperationNotSupportedException;
import java.net.InetAddress;
import java.net.UnknownHostException;

abstract class Course extends BassNameBase {
   
   private String course_name;
   private String ta_chat_jid;
   
   protected Course(String a_course_name, String a_jid){
      course_name = a_course_name;
      ta_chat_jid = a_jid;
   }
   
   @Override protected String getParameters() {
      return null;
   }

   @Override public String getProject() {
      return null;
   }
   
   @Override
   protected String getKey() {
         return course_name;
   }

   static class TACourse extends Course{
      private TicketList ticket_list;
      private String xmpp_password;
      private String xmpp_server;
      private TAXMPPClient client;
      
      protected TACourse(String a_course_name, String a_jid, String a_password, String a_server) {
         super(a_course_name, a_jid);
         ticket_list = new TicketList();
         xmpp_password = a_password;
         xmpp_server = a_server;
      }
      
      public TicketList getTicketList()
      {
	 return ticket_list;
      }
      @Override public BudaBubble createBubble()
      {
         System.out.println("Creating TA bubble");
         client = EduchatManager.getTAXMPPClientForCourse(this);
        
      
         //TODO: maybe we want a bubble where the TA
         //can select a resource name, but then again
         //as its planned right now that wouldn't 
         //appear on the other end  
         try{
            if(!client.isLoggedIn())
            {
               client.connectAndLogin(InetAddress.getLocalHost().getHostName());
            }
         }catch(UnknownHostException hostE)
         {
            hostE.printStackTrace();
         }catch(XMPPException xmppE)
         {
            xmppE.printStackTrace();
         }
         
         return new EduchatTicketListBubble(client.getTickets(), client);
      }
   
      @Override protected String getSymbolName() {
       return BassConstants.BASS_COURSE_LIST_NAME + ".Enable " + getCourseName() + " chat hours";
      }
   
      @Override public Icon getDisplayIcon(){
         return BoardImage.getIcon("contents_view");
      }
      
      public String getXMPPServer(){
         return xmpp_server;
      }
      
      public String getXMPPPassword(){
         return xmpp_password;
      }
   }

   static class StudentCourse extends Course
   {
	  private String ta_chat_jid;
      public StudentCourse(String a_course_name, String a_jid) {
         super(a_course_name, a_jid);
         ta_chat_jid = a_jid;
      }
   
     
      @Override public BudaBubble createBubble(){
          return new EduchatTicketSubmitBubble(ta_chat_jid);

      }
  
      @Override protected String getSymbolName(){
         return BassConstants.BASS_COURSE_LIST_NAME + ".Get " + getCourseName() + " help";
      }
   
      @Override public Icon getDisplayIcon(){
         return BoardImage.getIcon("question");
      }     
   }

public String getTAJID()
{
   return ta_chat_jid;
}

public String getCourseName()
{
   return course_name;
} 


@Override
public boolean equals(Object o)
{
   if(o instanceof Course)
   {            
      Course c = (Course)o;
      return (course_name == c.course_name &&
              ta_chat_jid == c.ta_chat_jid);
   }
   else 
      return false;
}
}
