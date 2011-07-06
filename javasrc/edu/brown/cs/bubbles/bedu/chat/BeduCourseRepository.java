package edu.brown.cs.bubbles.bedu.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.board.BoardProperties;

public class BeduCourseRepository implements BassRepository {
   private static List<BeduCourse> courses;
   private static BeduCourseRepository instance;
   
   public static BeduCourseRepository getInstance()
   {
      if(instance == null)
        setup();
      return instance;
   }
   public static void setup()
   {
      instance = new BeduCourseRepository();
      courses = new ArrayList<BeduCourse>();
      
      //grab courses out of the props file 
      BoardProperties bp = BoardProperties.getProperties("Educhat");
      Set<String> courseNames = new HashSet<String>();
      
      //gather course names
      for(String s : bp.stringPropertyNames())
      {
         System.out.println(s.split("\\.")[2]);
   
         //Property name should be in the form
         //Educhat.course.CS15.ta_jid
         if(s.startsWith("Educhat.course."))
             courseNames.add(s.split("\\.")[2]);
      }
   
      for(String courseName : courseNames)
      {
         BeduCourse c = null;
         String strRole = bp.getProperty("Educhat.course." + courseName + ".role");
         
         String ta_jid = bp.getProperty("Educhat.course." + courseName + ".ta_jid");
         
         if(strRole.equals("STUDENT")){
            c = new BeduCourse.StudentCourse(courseName, ta_jid);      
         }
         
         if(strRole.equals("TA")){
            String xmpp_password = bp.getProperty("Educhat.course." + courseName + ".xmpp_password");
            String server = bp.getProperty("Educhat.course." + courseName + ".server"); 
            
            c = new BeduCourse.TACourse(courseName, ta_jid, xmpp_password, server); 
         }
         
         if(c != null)
            courses.add(c);
      } 
      BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER, instance);
      BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_COURSES, instance);
   }
   
   @Override
   public Iterable<BassName> getAllNames() {
      return (List<BassName>)(List<?>)courses;
   }

   @Override
   public boolean includesRepository(BassRepository br) {
      // TODO Auto-generated method stub
      return false;
   }
   
   public void addCourse(BeduCourse c)
   {
       BoardProperties bp = BoardProperties.getProperties("Educhat");
       courses.add(c);
       bp.setProperty("Educhat.course." + c.getName() + ".ta_jid", c.getTAJID());
       if(c instanceof BeduCourse.StudentCourse){
         bp.setProperty("Educhat.course." + c.getName() + ".role", "Student");   
       }
       if(c instanceof BeduCourse.TACourse){
         BeduCourse.TACourse tc = (BeduCourse.TACourse)c;
         bp.setProperty("Educhat.course." + c.getName() + ".role", "TA");   
         bp.setProperty("Educhat.course." + c.getName() + ".xmpp_password", tc.getXMPPPassword());
         bp.setProperty("Educhat.course." + c.getName() + ".server", tc.getXMPPServer());
       }
   }
}
