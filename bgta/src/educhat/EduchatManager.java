/********************************************************************************/
/*                                                                              */
/*              EduchatManager.java                                             */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/*      Written by                                                              */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.bubbles.bgta.educhat;

import java.util.List;

import javax.naming.OperationNotSupportedException;
import org.jivesoftware.smack.XMPPException;
import java.util.ArrayList;

public class EduchatManager
{
    private static List<TAXMPPClient> ta_sessions;
    private static CourseRepository course_repository;    

        //private static List<Course> courses;
    
    static {
       ta_sessions = new ArrayList<TAXMPPClient>();
    }
    
    /*public static TAXMPPClient startHoursForCourse(Course c, String resource_name) throws OperationNotSupportedException, XMPPException{
       if(!(c instanceof Course.TACourse))
          throw new OperationNotSupportedException("Tried to start hours for course when not a TA");
       
       Course.TACourse tc = (Course.TACourse)c;
       
       TAXMPPClient client =new TAXMPPClient(tc);
       ta_sessions.add(client);
             
       client.connectAndLogin(resource_name);
             
       return client;
    }*/
    
    public static TAXMPPClient getTAXMPPClientForCourse(Course.TACourse course)
    {
       TAXMPPClient the_client = null;
       for(TAXMPPClient c : ta_sessions)
       {
          if(c.getCourse().equals(course))
             the_client = c;
       }
       
       if(the_client == null)
       {
           the_client = new TAXMPPClient(course);
           ta_sessions.add(the_client);
       }
       
       return the_client;
       
    }


}       // end of class EduchatManager




/* end of EduchatManager.java */
