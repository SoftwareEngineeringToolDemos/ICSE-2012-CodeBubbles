package edu.brown.cs.bubbles.bgta.educhat;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;

public class CourseRepository implements BassRepository {
   static
   {
      BassFactory.reloadRepository(new CourseRepository());
   }
   @Override
   public Iterable<BassName> getAllNames() {
      ArrayList<BassName> l = new ArrayList<BassName>();
      l.add(new Course());
      return l;
   }

   @Override
   public boolean includesRepository(BassRepository br) {
      // TODO Auto-generated method stub
      return false;
   }
}
