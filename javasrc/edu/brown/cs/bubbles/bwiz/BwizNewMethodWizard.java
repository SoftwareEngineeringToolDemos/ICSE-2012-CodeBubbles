/********************************************************************************/
/*                                                                              */
/*              BwizNewMethodWizard.java                                        */
/*                                                                              */
/*      Wizard to create a new method                                           */
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


class BwizNewMethodWizard extends BwizNewWizard
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

BwizNewMethodWizard(String cls,String proj,String pkg)
{   
   super(proj,pkg,cls);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected InfoStruct getInfoStruct()  { return new MethodInfo(); }

@Override protected int getAccessibilityInfo()
{
   return SHOW_PRIVATE|SHOW_PROTECTED|SHOW_ABSTRACT|SHOW_FINAL;
}

@Override protected IVerifier getVerifier()
{
   return new ParameterVerifier();
}

@Override protected Creator getCreator()        { return new MethodCreator(); }

@Override protected String getNameText()        { return "Method Name"; }
@Override protected String getNameHoverText() 
{
   return "<html>Enter the new method name.";
}

@Override protected String getSecondText()      { return "Return Type"; }
@Override protected String getSecondHoverText() 
{
   return "This method's return type";
}

@Override protected String getListText()        { return "Parameters: "; }
@Override protected String getListHoverText()
{
   return "Any method parameters. Press enter to add a parameter.";
}



/********************************************************************************/
/*                                                                              */
/*      Info Structure                                                          */
/*                                                                              */
/********************************************************************************/

private class MethodInfo extends InfoStruct {
   
   protected String getSetSeparator()           { return ","; }
   
   protected String getSignature(String pfx,String itms) {
      String rslt = pfx;
      rslt += " " + getSecondInfo().toString().trim();
      rslt += " " + getMainName().toString().trim() + "(";
      if (itms != null) rslt += itms;
      rslt += ")";
      return rslt;
    }   
   
}       // end of inner class MethodInfo


/********************************************************************************/
/*                                                                              */
/*      Method Creator                                                          *//*                                                                              */
/********************************************************************************/

private class MethodCreator extends Creator {
   
   protected BudaBubble doCreate(BudaBubbleArea bba,Point pt,String fullname,BuenoProperties bp) {
      BudaBubble nbbl = null;
      BuenoLocation bl = at_location;
      BuenoFactory bf = BuenoFactory.getFactory();
      String proj = info_structure.getProjectName();
      String pkg = info_structure.getPackageName();
      
      String cls = info_structure.getClassName();
      String mthd = info_structure.getMainName().toString();
      
      bp.put(BuenoKey.KEY_NAME,mthd);
      bp.put(BuenoKey.KEY_TYPE,fullname);
      bp.put(BuenoKey.KEY_RETURNS,info_structure.getSecondInfo().toString());
      bp.put(BuenoKey.KEY_PARAMETERS,info_structure.getSet());
      StringBuffer buf = new StringBuffer();
      buf.append(fullname);
      buf.append(".");
      buf.append(mthd);
      buf.append("(");
      int i = 0;
      for (String s : info_structure.getSet()) {
         if (i++ > 0) buf.append(",");
         int idx = s.lastIndexOf(" ");
         String typ = s.substring(0,idx);
         buf.append(typ);
       }
      buf.append(")");
      String fmthd = buf.toString();
      if (bl == null) bl = bf.createLocation(proj,pkg,cls,true);
      bf.createNew(BuenoType.NEW_METHOD,bl,bp);
      if (bubble_creator == null)
         nbbl = BaleFactory.getFactory().createMethodBubble(proj,fmthd);
      else
         bubble_creator.createBubble(proj,fmthd,bba,pt);
      
      return nbbl;
    }
   
}       // end of inner class EnumCreator 



}       // end of class BwizNewMethodWizard




/* end of BwizNewMethodWizard.java */

