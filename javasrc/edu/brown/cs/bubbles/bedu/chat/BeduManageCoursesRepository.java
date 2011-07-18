/********************************************************************************/
/*                                                                              */
/*          BeduManageCoursesRepository.java                                    */
/*                                                                              */
/*    Bubbles for Education                                                     */
/*    Repository for providing the ability to pull up a bubble to manage courses*/
/*                                                                              */
/********************************************************************************/
/*    Copyright 2011 Brown University -- Andrew Kovacs                          */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

package edu.brown.cs.bubbles.bedu.chat;

import java.util.ArrayList;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaBubble;

class BeduManageCoursesRepository implements BassConstants.BassRepository{

   @Override
   public Iterable<BassName> getAllNames()
   {
      ArrayList<BassName> l = new ArrayList<BassName>();
      l.add(new BeduAddCoursesName());
      return l;
   }

   @Override
   public boolean includesRepository(BassRepository br)
   {
      return br == this;
   }
   
   class BeduAddCoursesName extends BassNameBase
   {
      private static final String name = "Manage courses";
      @Override
      public BudaBubble createBubble()
      {
         return new BeduManageCoursesBubble();
      }

      @Override
      protected String getKey()
      {
         return name;
      }

      @Override
      protected String getParameters()
      {
         return null;
      }

      @Override
      public String getProject()
      {
         return null;
      }

      @Override
      protected String getSymbolName()
      {
         return BassConstants.BASS_COURSE_LIST_NAME + "." + name;
      }
   
   }

}
