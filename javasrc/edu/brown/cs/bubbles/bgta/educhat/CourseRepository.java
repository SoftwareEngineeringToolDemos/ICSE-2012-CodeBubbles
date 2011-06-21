package edu.brown.cs.bubbles.bgta.educhat;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaConstants;

public class CourseRepository implements BassRepository {
   public static void setup()
   {
      CourseRepository cr = new CourseRepository();
      BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER, cr);
      BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_COURSES, cr);
      //BassFactory.reloadRepository(cr);
   }
   
   @Override
   public Iterable<BassName> getAllNames() {
      ArrayList<BassName> l = new ArrayList<BassName>();
      l.add(new Course("CS019", "codebubbles@jabber.org"));
      return l;
   }

   @Override
   public boolean includesRepository(BassRepository br) {
      // TODO Auto-generated method stub
      return false;
   }
}
