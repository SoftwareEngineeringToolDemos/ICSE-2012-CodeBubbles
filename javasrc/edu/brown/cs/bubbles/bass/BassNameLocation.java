/********************************************************************************/
/*										*/
/*		BassNameLocation.java						*/
/*										*/
/*	Bubble Augmented Search Strategies name based on set of locations	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.Icon;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class BassNameLocation extends BassNameBase implements BassConstants, BumpConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<BumpLocation>	location_set;
private BumpLocation		base_location;
private Icon			appropriate_icon;
private String			method_params;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassNameLocation(BumpLocation bl)
{
   location_set = new ArrayList<BumpLocation>();
   base_location = bl;
   location_set.add(bl);

   switch (bl.getSymbolType()) {
      case PACKAGE :
	 name_type = BassNameType.PACKAGE;
	 break;
      case CLASS :
	 name_type = BassNameType.CLASS;
	 break;
      case THROWABLE :
	 name_type = BassNameType.THROWABLE;
	 break;
      case INTERFACE :
	 name_type = BassNameType.INTERFACE;
	 break;
      case ENUM :
	 name_type = BassNameType.ENUM;
	 break;
      case FUNCTION :
	 name_type = BassNameType.METHOD;
	 break;
      case CONSTRUCTOR :
	 name_type = BassNameType.CONSTRUCTOR;
	 break;
      case ENUM_CONSTANT :
      case FIELD :
	 name_type = BassNameType.FIELDS;
	 break;
      case STATIC_INITIALIZER :
	 name_type = BassNameType.STATICS;
	 break;
      default :
	 name_type = BassNameType.NONE;
	 break;
    }

   method_params = base_location.getParameters();
   if (name_type == BassNameType.METHOD) {
      if(Modifier.isPublic(getModifiers())) appropriate_icon = BoardImage.getIcon("method");
      else if (Modifier.isPrivate(getModifiers())) appropriate_icon = BoardImage.getIcon("private_method");
      else if (Modifier.isProtected(getModifiers())) appropriate_icon = BoardImage.getIcon("protected_method");
      else appropriate_icon = BoardImage.getIcon("default_method");
    }
   else if (name_type == BassNameType.CONSTRUCTOR) {
      if (Modifier.isPublic(getModifiers())) appropriate_icon = BoardImage.getIcon("constructor");
      else if (Modifier.isPrivate(getModifiers())) appropriate_icon = BoardImage.getIcon("private_constructor");
      else if (Modifier.isProtected(getModifiers())) appropriate_icon = BoardImage.getIcon("protected_constructor");
      else appropriate_icon = BoardImage.getIcon("default_constructor");
    }
}



BassNameLocation(BumpLocation bl,BassNameType typ)
{
   location_set = new ArrayList<BumpLocation>();
   base_location = bl;
   location_set.add(bl);
   name_type = typ;
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addLocation(BumpLocation bl)
{
   if (base_location == null) base_location = bl;

   location_set.add(bl);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProject()			{ return base_location.getSymbolProject(); }

@Override public int getModifiers()			{ return base_location.getModifiers(); }

@Override protected String getKey()			{ return base_location.getKey(); }
@Override protected String getSymbolName()		{ return base_location.getSymbolName(); }
@Override protected String getParameters()		{ return method_params; }

File getFile()						{ return base_location.getFile(); }

int getEclipseStartOffset() {
   switch (name_type) {
      case HEADER :
	 if (!base_location.getKey().contains("$")) return 0;
	 break;
      case FILE :
	 return 0;
      default :
	 break;
   }
   return base_location.getDefinitionOffset();
}
int getEclipseEndOffset()				{ return base_location.getDefinitionEndOffset(); }
BumpSymbolType getSymbolType()				{ return base_location.getSymbolType(); }

@Override public BumpLocation getLocation()		{ return base_location; }



@Override public String getLocalName()
{
   switch (name_type) {
      case FIELDS :
	 return "< FIELDS >";
      case STATICS :
	 return "< INITIALIZERS >";
      case HEADER :
	 return "< PREFIX >";
      case FILE :
	 return "< FILE >";
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
	 return "< BODY OF " + super.getLocalName() + " >";
    }

   return super.getLocalName();
}


@Override public String getNameHead()
{
   switch (name_type) {
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case HEADER :
      case FILE :
      case OTHER_CLASS :
	 String nm = getUserSymbolName();
	 int idx = nm.indexOf("<");
	 if (idx > 0) nm = nm.substring(0,idx);
	 return nm;			// we add <PREFIX> or <BODY> for classes
    }

   return super.getNameHead();
}



@Override public String getFullName()
{
   switch (name_type) {
      case STATICS :
      case FIELDS :
      case CLASS :
      case THROWABLE :
      case ENUM :
      case INTERFACE :
      case HEADER :
      case OTHER_CLASS :
      case FILE :
	 return getNameHead() + ". " + getLocalName();
    }

   return super.getFullName();
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble()
{
   BudaBubble bb = null;
   BaleFactory bf = BaleFactory.getFactory();

   //TODO: This should be done in BALE, possibly indirect through BUDA

   switch (name_type) {
      case CONSTRUCTOR :
      case METHOD :
	 bb = bf.createMethodBubble(getProject(),getFullName());
	 break;
      case FIELDS :
	 bb = bf.createFieldsBubble(getProject(),getNameHead());
	 break;
      case STATICS :
	 bb = bf.createStaticsBubble(getProject(),getNameHead());
	 break;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
	 bb = bf.createClassBubble(getProject(),getNameHead());
	 break;
      case HEADER :
	 bb = bf.createClassPrefixBubble(getProject(),getNameHead());
	 break;
      case FILE :
	 bb = bf.createFileBubble(getProject(),getNameHead());
	 break;
      default :
	 BoardLog.logW("BASS","NO BUBBLE FOR " + getKey());
	 break;
    }

   return bb;
}



@Override public BudaBubble createPreviewBubble()
{
   switch (name_type) {
      case PACKAGE :
      case FILE :
      default :
	 break;
      case CONSTRUCTOR :
      case METHOD :
      case FIELDS :
      case STATICS :
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case HEADER :
	 return createBubble();
    }

   return null;
}


@Override public String createPreviewString()
{
   switch (name_type) {
      case FILE :
	 return "Show file bubble for " + getNameHead();
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Display methods 							*/
/*										*/
/********************************************************************************/

@Override public Icon getDisplayIcon()
{
   switch (name_type) {
      case CONSTRUCTOR :
      case METHOD :
	 return appropriate_icon;
   }

   return null;
}




}	// end of class BassNameLocation




/* end of BassNameLocation.java */
