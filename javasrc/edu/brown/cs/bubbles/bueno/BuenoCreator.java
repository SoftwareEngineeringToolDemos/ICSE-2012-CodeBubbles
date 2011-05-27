/********************************************************************************/
/*										*/
/*		BuenoCreator.java						*/
/*										*/
/*	BUbbles Environment New Objects creator abstract creation methods	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.bump.BumpClient;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.Date;


abstract class BuenoCreator implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int	tab_size;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BuenoCreator()
{
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   tab_size = bp.getInt(BUENO_TEMPLATE_TAB_SIZE,8);
}



/********************************************************************************/
/*										*/
/*	Creation methods for packages						*/
/*										*/
/********************************************************************************/

protected void createPackage(BuenoLocation where,BuenoProperties props)
{
   BumpClient bc = BumpClient.getBump();

   String pkg = props.getStringProperty(BuenoKey.KEY_PACKAGE);

   File dir = bc.createNewPackage(where.getProject(),pkg,false);

   setupPackage(dir);
}



protected void setupPackage(File packagedir)
{ }





/********************************************************************************/
/*										*/
/*	Creation methods for classes, interfaces, enums 			*/
/*										*/
/********************************************************************************/

protected void createType(BuenoType typ,BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   switch (typ) {
      case NEW_TYPE :
      case NEW_CLASS :
	 setupClass(buf,props);
	 break;
      case NEW_INTERFACE :
	 setupInterface(buf,props);
	 break;
      case NEW_ENUM :
	 setupEnum(buf,props);
	 break;
      case NEW_ANNOTATION :
	 setupAnnotation(buf,props);
	 break;
    }

   String nm = props.getStringProperty(BuenoKey.KEY_PACKAGE) + "." +
      props.getStringProperty(BuenoKey.KEY_NAME);

   BumpClient bc = BumpClient.getBump();
   bc.saveAll();
   File cf = bc.createNewClass(where.getProject(),nm,false,buf.toString());
   where.setFile(cf);
   // bc.compile(false,false,true);
}



protected void setupClass(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"class");

   classText(buf,props);
}




protected void setupInterface(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"interface");

   classText(buf,props);
}




protected void setupEnum(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"enum");

   classText(buf,props);
}




protected void setupAnnotation(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"annotation");

   classText(buf,props);
}




/********************************************************************************/
/*										*/
/*	Creation methods for inner classes, etc.				*/
/*										*/
/********************************************************************************/

protected void createInnerType(BuenoType typ,BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   switch (typ) {
      case NEW_INNER_TYPE :
      case NEW_INNER_CLASS :
	 setupInnerClass(buf,props);
	 break;
      case NEW_INNER_INTERFACE :
	 setupInnerInterface(buf,props);
	 break;
      case NEW_INNER_ENUM :
	 setupInnerEnum(buf,props);
	 break;
    }

   BuenoFactory.getFactory().insertText(where,buf.toString());
}



protected void setupInnerClass(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"class");

   innerClassText(buf,props);
}




protected void setupInnerInterface(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"interface");

   innerClassText(buf,props);
}




protected void setupInnerEnum(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"enum");

   innerClassText(buf,props);
}




/********************************************************************************/
/*										*/
/*	Creation methods for methods						*/
/*										*/
/********************************************************************************/

protected void createMethod(BuenoType typ,BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();
   switch (typ) {
      case NEW_CONSTRUCTOR :
	 setupConstructor(buf,props);
	 break;
      case NEW_METHOD :
	 setupMethod(buf,props);
	 break;
      case NEW_GETTER :
	 setupGetter(buf,props);
	 break;
      case NEW_SETTER :
	 setupGetter(buf,props);
	 break;
      case NEW_GETTER_SETTER :
	 setupGetterSetter(buf,props);
	 break;
    }

   BuenoFactory.getFactory().insertText(where,buf.toString());
}



protected void setupConstructor(StringBuffer buf,BuenoProperties props)
{
   if (props.getStringProperty(BuenoKey.KEY_RETURNS) != null) {
      props.remove(BuenoKey.KEY_RETURNS);
    }

   if (props.getStringProperty(BuenoKey.KEY_CONTENTS) == null) {
      props.put(BuenoKey.KEY_CONTENTS,"// constructor body goes here");
    }

   methodText(buf,props);
}


protected void setupMethod(StringBuffer buf,BuenoProperties props)
{
   String returns = props.getStringProperty(BuenoKey.KEY_RETURNS);

   if (returns == null) {
      props.put(BuenoKey.KEY_RETURNS,"void");
    }

   if (props.getStringProperty(BuenoKey.KEY_CONTENTS) == null) {
      props.put(BuenoKey.KEY_CONTENTS,"// method body goes here");
    }

   if (returns != null && !returns.equals("void")) {
      String rstmt = null;
      if (returns.equals("boolean")) rstmt = "return false;";
      else if (returns.equals("int") || returns.equals("float") ||
		  returns.equals("double") || returns.equals("short") ||
		  returns.equals("byte") || returns.equals("char"))
	 rstmt = "return 0;";
      else rstmt = "return null;";
      props.put(BuenoKey.KEY_RETURN_STMT,rstmt);
    }

   methodText(buf,props);
}


protected void setupGetter(StringBuffer buf,BuenoProperties props)
{
   buf.append("<Insert getter here>");
}


protected void setupSetter(StringBuffer buf,BuenoProperties props)
{
   buf.append("<Insert setter here>");
}


protected void setupGetterSetter(StringBuffer buf,BuenoProperties props)
{
   setupGetter(buf,props);
   setupSetter(buf,props);
}



/********************************************************************************/
/*										*/
/*	Creation methods for fields						*/
/*										*/
/********************************************************************************/

protected void createField(BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   setupField(buf,props);

   BuenoFactory.getFactory().insertText(where,buf.toString());
}



protected void setupField(StringBuffer buf,BuenoProperties props)
{
   fieldText(buf,props);
}



/********************************************************************************/
/*										*/
/*	Creation methods for comments						*/
/*										*/
/********************************************************************************/

protected void createComment(BuenoType typ,BuenoLocation where,BuenoProperties props)
{
   String txt = (String) props.get(BuenoKey.KEY_COMMENT);
   if (txt == null) {
      props.put(BuenoKey.KEY_COMMENT,"<comment here>");
    }

   StringBuffer buf = new StringBuffer();

   switch (typ) {
      case NEW_MARQUIS_COMMENT :
	 setupMarquisComment(buf,props);
	 break;
      case NEW_BLOCK_COMMENT :
	 setupBlockComment(buf,props);
	 break;
      case NEW_JAVADOC_COMMENT :
	 setupJavadocComment(buf,props);
	 break;
    }

   BuenoFactory.getFactory().insertText(where,buf.toString());
}



protected void setupMarquisComment(StringBuffer buf,BuenoProperties props)
{
   StringBuffer pbuf = new StringBuffer();
   pbuf.append("/********************************************************************************/\n");
   pbuf.append("/*                                                                              */\n");
   pbuf.append("/*       ${COMMENT}                                                             */\n");
   pbuf.append("/*                                                                              */\n");
   pbuf.append("/********************************************************************************/\n");

   StringReader sr = new StringReader(pbuf.toString());
   try {
      expand(sr,props,null,buf);
    }
   catch (IOException e) { }
}




protected void setupBlockComment(StringBuffer buf,BuenoProperties props)
{
   StringBuffer pbuf = new StringBuffer();
   pbuf.append("/*\n");
   pbuf.append(" * ${COMMENT}");
   pbuf.append("\n");
   pbuf.append(" */\n");

   StringReader sr = new StringReader(pbuf.toString());
   try {
      expand(sr,props,null,buf);
    }
   catch (IOException e) { }
}




protected void setupJavadocComment(StringBuffer buf,BuenoProperties props)
{
   String txt = (String) props.get(BuenoKey.KEY_COMMENT);

   buf.append("/**\n");
   buf.append(" *");
   if (txt != null) buf.append("      " + txt);
   buf.append("\n");
   buf.append("**/\n");
}



private void setupBlockComment(StringBuffer buf,BuenoProperties props,String text)
{
   String p = props.getStringProperty(BuenoKey.KEY_COMMENT);
   props.put(BuenoKey.KEY_COMMENT,text);

   setupBlockComment(buf,props);

   if (p == null) props.remove(BuenoKey.KEY_COMMENT);
   else props.put(BuenoKey.KEY_COMMENT,p);
}



private void setupJavadocComment(StringBuffer buf,BuenoProperties props,String text)
{
   String p = props.getStringProperty(BuenoKey.KEY_COMMENT);
   props.put(BuenoKey.KEY_COMMENT,text);

   setupJavadocComment(buf,props);

   if (p == null) props.remove(BuenoKey.KEY_COMMENT);
   else props.put(BuenoKey.KEY_COMMENT,p);
}



/********************************************************************************/
/*										*/
/*	Simple method creation							*/
/*										*/
/********************************************************************************/

protected void methodText(StringBuffer buf,BuenoProperties props)
{
   StringBuffer pbuf = new StringBuffer();
   pbuf.append("\n\n");
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      pbuf.append("/*\n");
      pbuf.append(" * ${COMMENT}\n");
      pbuf.append(" */\n\n");
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      pbuf.append("/**");
      pbuf.append(" * ${COMMENT}\n");
      pbuf.append("**/\n\n");
    }
   pbuf.append("$(ITAB)$(MODIFIERS)$(RETURNS) $(NAME)($(PARAMETERS))");

   int mods = props.getModifiers();
   if (Modifier.isAbstract(mods) || Modifier.isNative(mods)) {
      pbuf.append(";\n");
    }
   else {
      pbuf.append("\n{\n$(TAB)${CONTENTS}\n");
      String rstmt = props.getStringProperty(BuenoKey.KEY_RETURN_STMT);
      if (rstmt != null) {
	 pbuf.append("\t$(TAB)$(RETURN_STMT)\n");
       }
      pbuf.append("$(ITAB)}\n");
    }

   StringReader sr = new StringReader(pbuf.toString());
   try {
      expand(sr,props,null,buf);
    }
   catch (IOException e) { }
}




/********************************************************************************/
/*										*/
/*	Simple Class creation							*/
/*										*/
/********************************************************************************/

protected void classText(StringBuffer buf,BuenoProperties props)
{
   String pkg = props.getStringProperty(BuenoKey.KEY_PACKAGE);

   if (pkg != null) {
      buf.append("package " + pkg + ";\n");
    }

   buf.append("\n");

   String [] imps = props.getImports();
   if (imps != null && imps.length > 0) {
      for (String s : imps) {
	 buf.append(s + "\n");
       }
    }
   buf.append("\n");

   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      setupJavadocComment(buf,props,cmmt);
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   int mods = props.getModifiers();
   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
   ct = addModifier(buf,"native",Modifier.isNative(mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
   if (ct > 0) buf.append(" ");

   String typ = props.getStringProperty(BuenoKey.KEY_TYPE);
   if (typ == null) typ = "class";
   String nam = props.getStringProperty(BuenoKey.KEY_NAME);

   buf.append(typ + " " + nam);
   String ext = props.getStringProperty(BuenoKey.KEY_EXTENDS);
   if (ext != null) buf.append(" extends " + ext);
   String [] impl = props.getImplements();
   if (impl != null && impl.length > 0) {
      ct = 0;
      for (String im : impl) {
	 if (ct++ == 0) buf.append(" implements ");
	 else buf.append(", ");
	 buf.append(im);
       }
     }
   buf.append(" {\n");
   buf.append("\n");
   buf.append("\n");
   buf.append("}\n");
}



/********************************************************************************/
/*										*/
/*	Simple inner class creation						*/
/*										*/
/********************************************************************************/

protected void innerClassText(StringBuffer buf,BuenoProperties props)
{
   String iind = props.getInitialIndentString();

   buf.append("\n\n");

   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      setupJavadocComment(buf,props,cmmt);
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   buf.append(iind);
   int mods = props.getModifiers();
   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
   ct = addModifier(buf,"native",Modifier.isNative(mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
   if (ct > 0) buf.append(" ");

   String typ = props.getStringProperty(BuenoKey.KEY_TYPE);
   if (typ == null) typ = "class";
   String nam = props.getStringProperty(BuenoKey.KEY_NAME);

   buf.append(typ + " " + nam);
   String ext = props.getStringProperty(BuenoKey.KEY_EXTENDS);
   if (ext != null) buf.append(" extends " + ext);
   String [] impl = props.getImplements();
   if (impl != null && impl.length > 0) {
      ct = 0;
      for (String im : impl) {
	 if (ct++ == 0) buf.append(" implements ");
	 else buf.append(", ");
	 buf.append(im);
       }
     }
   buf.append(" {\n");
   buf.append("\n");
   buf.append("\n");
   buf.append("}\n");
}



/********************************************************************************/
/*										*/
/*	Simple field creation							*/
/*										*/
/********************************************************************************/

protected void fieldText(StringBuffer buf,BuenoProperties props)
{
   String iind = props.getInitialIndentString();

   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      setupJavadocComment(buf,props,cmmt);
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   buf.append(iind);
   int mods = props.getModifiers();
   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
   ct = addModifier(buf,"strictfp",Modifier.isStrict(mods),ct);
   if (ct > 0) buf.append(" ");

   String typ = props.getStringProperty(BuenoKey.KEY_RETURNS);
   if (typ == null) typ = "int";
   String nam = props.getStringProperty(BuenoKey.KEY_NAME);

   buf.append(typ + " " + nam);

   String val = props.getStringProperty(BuenoKey.KEY_INITIAL_VALUE);
   if (val != null) {
      buf.append(" = ");
      buf.append(val);
    }

   buf.append(";\n");
}



/********************************************************************************/
/*										*/
/*	Utility routines							*/
/*										*/
/********************************************************************************/

private int addModifier(StringBuffer buf,String txt,boolean add,int ct)
{
   if (!add) return ct;

   if (ct > 0) buf.append(" ");
   buf.append(txt);
   return ct+1;
}



/********************************************************************************/
/*										*/
/*	Template expansion routines						*/
/*										*/
/********************************************************************************/

protected void expand(Reader from,BuenoProperties props,String eol,StringBuffer buf) throws IOException
{
   BufferedReader br = new BufferedReader(from);
   if (eol == null) eol = System.getProperty("line.separator");

   for ( ; ; ) {
      String ln = br.readLine();
      if (ln == null) break;
      ln = expandTabs(ln,tab_size);
      for (int i = 0; i < ln.length(); ++i) {
	 char c = ln.charAt(i);
	 if (c == '$' && ln.charAt(i+1) == '(') {
	    StringBuffer tok = new StringBuffer();
	    for (i = i+2; i < ln.length() && ln.charAt(i) != ')'; ++i) {
	       tok.append(ln.charAt(i));
	     }
	    if (i >= ln.length()) throw new IOException("Unterminated token");
	    String rslt = getValue(tok.toString(),props,eol);
	    if (rslt != null) buf.append(rslt);
	  }
	 else if (c == '$' && ln.charAt(i+1) == '{') {
	    StringBuffer tok = new StringBuffer();
	    for (i = i+2; i < ln.length() && ln.charAt(i) != '}'; ++i) {
	       tok.append(ln.charAt(i));
	     }
	    if (i >= ln.length()) throw new IOException("Unterminated token");
	    String sfx = ln.substring(i+1);
	    int lst = buf.length()-1;
	    while (lst > 0 && buf.charAt(lst-1) != '\n') --lst;
	    String pfx = buf.substring(lst);
	    getFormattedValue(tok.toString(),props,pfx,sfx,eol,buf);
	    break;
	  }
	 else buf.append(c);
       }
      buf.append(eol);
    }
}



private String getValue(String key,BuenoProperties props,String eol)
{
   StringBuffer buf = new StringBuffer();
   int ct = 0;

   if (key.equals("MODIFIERS")) {
      int mods = props.getModifiers();
      ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
      ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
      ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
      ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
      ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
      ct = addModifier(buf,"native",Modifier.isNative(mods),ct);
      ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
      ct = addModifier(buf,"strictfp",Modifier.isStrict(mods),ct);
      ct = addModifier(buf,"synchronized",Modifier.isSynchronized(mods),ct);
      ct = addModifier(buf,"transient",Modifier.isTransient(mods),ct);
      ct = addModifier(buf,"volatile",Modifier.isVolatile(mods),ct);
      if (ct > 0) buf.append(" ");
   }
   else if (key.equals("PARAMETERS")) {
      outputList(props.getParameters(),null,",",buf);
   }
   else if (key.equals("IMPORTS")) {
      outputList(props.getImports(),null,eol,buf);
   }
   else if (key.equals("THROWS")) {
      outputList(props.getThrows(),"throws ",", ",buf);
   }
   else if (key.equals("EXTENDS")) {
      String v = props.getStringProperty(BuenoKey.KEY_EXTENDS);
      if (v != null) buf.append(" extends " + v);
   }
   else if (key.equals("IMPLEMENTS")) {
      outputList(props.getImplements()," implements ",", ",buf);
   }
   else if (key.equals("DATE")) {
      buf.append(new Date().toString());
    }
   else if (key.equals("PACKAGE")) {
      String v = props.getStringProperty(BuenoKey.KEY_PACKAGE);
      if (v != null) buf.append("package " + v + ";");
    }
   else if (key.equals("ITAB")) {
      buf.append(props.getInitialIndentString());
    }
   else if (key.equals("TAB")) {
      buf.append(props.getInitialIndentString());
      buf.append(props.getIndentString());
    }
   else {
      String pnm = BUENO_PROPERTY_HEAD + key;
      BoardProperties bp = BoardProperties.getProperties("Bueno");
      String v = bp.getProperty(pnm);
      if (v != null) buf.append(v);
      else {
	 String nm = "KEY_" + key;
	 try {
	    BuenoKey k = BuenoKey.valueOf(nm);
	    v = props.getStringProperty(k);
	    if (v != null) buf.append(v);
	  }
	 catch (IllegalArgumentException e) { }
       }
   }

   return buf.toString();
}


private void outputList(String [] itms,String pfx,String sep,StringBuffer buf)
{
   if (itms == null) return;
   int ct = 0;
   for (String s : itms) {
      if (ct++ != 0) buf.append(sep);
      else if (pfx != null) buf.append(pfx);
      buf.append(s.trim());
   }
}



private void getFormattedValue(String key,BuenoProperties props,String pfx,String sfx,String eol,StringBuffer buf)
{
   int p0 = pfx.length() + key.length() + 3;   // ${ ... }
   int p2 = 0;
   while (p2 < sfx.length() && !Character.isWhitespace(sfx.charAt(p2))) ++p2;
   String adder = sfx.substring(0,p2);
   int p1 = p2;
   while (p1 < sfx.length() && Character.isWhitespace(sfx.charAt(p1))) ++p1;
   if (p1 >= sfx.length()) sfx = null;
   else sfx = sfx.substring(p1);
   p0 += p1;					// this is where sfx starts

   String value = getValue(key,props,eol);
   if (value == null) value = "";
   value += adder;
   value = value.replace(eol,"\n");

   int idx = value.indexOf("\n");
   while (idx >= 0) {
      String v0 = value.substring(0,idx);
      value = value.substring(idx+1);
      v0 = v0.trim();
      buf.append(v0);
      int v1 = v0.length() + pfx.length();
      if (sfx != null) {
	 if (v1 >= p0) buf.append(" ");
	 else for (int i = v1; i < p0; ++i) buf.append(" ");
	 buf.append(sfx);
      }
      buf.append(eol);
      buf.append(pfx);
      idx = value.indexOf("\n");
   }

   value = value.trim();
   buf.append(value);
   int v2 = value.length() + pfx.length();
   if (sfx != null) {
      if (v2 >= p0) buf.append(" ");
      else for (int i = v2; i < p0; ++i) buf.append(" ");
      buf.append(sfx);
   }
}




private String expandTabs(String self,int tabstop)
{
   int index;

   while ((index = self.indexOf('\t')) != - 1) {
      StringBuilder builder = new StringBuilder(self);
      int count = tabstop - index % tabstop;
      builder.deleteCharAt(index);
      for (int i = 0; i < count; i++)
	 builder.insert(index," ");
      self = builder.toString();
    }

   return self;
}




}	// end of class BuenoCreator




/* end of BuenoCreator.java */
