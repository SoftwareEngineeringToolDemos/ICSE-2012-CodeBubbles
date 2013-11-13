/********************************************************************************/
/*                                                                              */
/*              BwizNewInterfaceWizard.java                                     */
/*                                                                              */
/*      Wizard to create a new interface                                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bwiz;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoKey;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoType;
import edu.brown.cs.bubbles.bueno.*;

import java.awt.Point;


class BwizNewInterfaceWizard extends BwizNewWizard
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BwizNewInterfaceWizard()
{
   this(null,null);
}



BwizNewInterfaceWizard(String proj,String pkg)
{   
   super(proj,pkg,null);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected InfoStruct getInfoStruct()  { return new InterfaceInfo(); }

@Override protected Creator getCreator()        { return new InterfaceCreator(); }

@Override protected String getNameText()        { return "Interface Name"; }
@Override protected String getNameHoverText() 
{
   return "<html>Enter the new interface name." + 
   " Such names usually start with an uppercase letter";
}

@Override protected String getSecondText()      { return null; }
@Override protected String getSecondHoverText() { return null; }

@Override protected String getListText()        { return "Extends: "; }
@Override protected String getListHoverText()
{
   return "Any interfaces extened by this interface. Press ENTER" +
   "after you type the interface name to add an interface.";
}


/********************************************************************************/
/*                                                                              */
/*      Info Structure                                                          */
/*                                                                              */
/********************************************************************************/

private class InterfaceInfo extends InfoStruct {
   
   protected String getSignature(String pfx,String itms) {
      String rslt = pfx + " interface " + getMainName().toString().trim();
      if (itms != null) rslt += " extends " + itms;
      return rslt;
    }
   
}       // end of inner class MethodInfo



/********************************************************************************/
/*                                                                              */
/*      Interface Creator                                                       *//*                                                                              */
/********************************************************************************/

private class InterfaceCreator extends Creator {
   
   protected BudaBubble doCreate(BudaBubbleArea bba,Point pt,String fullname,BuenoProperties bp) {
      BudaBubble nbbl = null;
      BuenoLocation bl = at_location;
      BuenoFactory bf = BuenoFactory.getFactory();
      String proj = info_structure.getProjectName();
      String pkg = info_structure.getPackageName();
      
      bp.put(BuenoKey.KEY_EXTENDS,info_structure.getSet());
      if (bl == null) bl = bf.createLocation(proj,pkg,null,true);
      bf.createNew(BuenoType.NEW_INTERFACE,bl,bp);
      if (bubble_creator == null)
         nbbl = BaleFactory.getFactory().createFileBubble(proj,null,fullname);
      else
         bubble_creator.createBubble(proj,fullname,bba,pt);
      
      return nbbl;
    }
   
}       // end of inner class EnumCreator 


}       // end of class BwizNewInterfaceWizard




/* end of BwizNewInterfaceWizard.java */

