/********************************************************************************/
/*										*/
/*		BaleTokenizer.java						*/
/*										*/
/*	Bubble Annotated Language Editor text tokenizer 			*/
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


package edu.brown.cs.bubbles.bale;


import javax.swing.text.Segment;

import java.util.*;


class BaleTokenizer implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		input_text;
private Segment 	input_segment;
private int		cur_offset;
private int		end_offset;
private BaleTokenState	token_state;
private int		token_start;
private boolean 	ignore_white;

private static Map<String,BaleTokenType> keyword_map;
private static Set<String>	op_set;

private static final String OP_CHARS = "=<!~?:>|&+-*/^%";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleTokenizer(String text,BaleTokenState start)
{
   input_text = (text == null ? "" : text);
   input_segment = null;
   cur_offset = 0;
   end_offset = (text == null ? 0 : input_text.length());
   token_state = start;
   ignore_white = false;
}



BaleTokenizer(Segment seg)
{
   this(seg,0,seg.length());
}


BaleTokenizer(Segment seg,int start,int end)
{
   input_segment = seg;
   cur_offset = start;
   end_offset = end;
   ignore_white = false;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setIgnoreWhitespace(boolean fg)		{ ignore_white = fg; }




/********************************************************************************/
/*										*/
/*	Scanning methods							*/
/*										*/
/********************************************************************************/

List<BaleToken> scan()
{
   List<BaleToken> rslt = new ArrayList<BaleToken>();

   while (cur_offset < end_offset) {
      rslt.add(getNextToken());
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Tokenizing methods							*/
/*										*/
/********************************************************************************/

BaleToken getNextToken(int start,int end)
{
   return getNextToken(start,end,BaleTokenState.NORMAL);
}



BaleToken getNextToken(int start,int end,BaleTokenState st)
{
   int poff = cur_offset;
   int peoff = end_offset;
   BaleTokenState pst = token_state;
   boolean pwh = ignore_white;

   cur_offset = start;
   end_offset = end;
   token_state = st;
   ignore_white = true;

   BaleToken t = getNextToken();

   cur_offset = poff;
   end_offset = peoff;
   token_state = pst;
   ignore_white = pwh;

   return t;
}




/********************************************************************************/
/*										*/
/*	Next token methods							*/
/*										*/
/********************************************************************************/

private BaleToken getNextToken()
{
   token_start = cur_offset;

   if (token_state != BaleTokenState.NORMAL) {
      if (token_state == BaleTokenState.IN_LINE_COMMENT) return scanLineComment(); //added by amc6
      else return scanComment(token_state == BaleTokenState.IN_FORMAL_COMMENT);
   }

   //TODO: keep a flag to identify unary operators
   //TODO: instanceof should be separate

   char ch = nextChar();
   if (ignore_white) {
      while (Character.isWhitespace(ch)) {
	 token_start = cur_offset;
	 ch = nextChar();
       }
    }

   if (ch == '\r') {
      ch = nextChar();
      if (ch != '\n') backup();
      return buildToken(BaleTokenType.EOL);
    }
   else if (ch == '\n') return buildToken(BaleTokenType.EOL);
   else if (Character.isWhitespace(ch)) {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch < 0 || ch == 0xffff || ch == '\n' || ch == '\r' || !Character.isWhitespace(ch))
	    break;
       }
      backup();
      return buildToken(BaleTokenType.SPACE);
    }
   else if (Character.isJavaIdentifierStart(ch)) {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch < 0 || ch == 0xffff || !Character.isJavaIdentifierPart(ch)) break;
       }
      backup();
      String text = getInputString(token_start,cur_offset);
      BaleTokenType tt = keyword_map.get(text);
      if (tt != null) return buildToken(tt);
      else return buildToken(BaleTokenType.IDENTIFIER);
    }
   else if (Character.isDigit(ch)) {
      if (ch == '0') {
	 ch = nextChar();
	 if (ch == 'x' || ch == 'X') {
	    int ct = 0;
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || Character.digit(ch,16) < 0) break;
	       ++ct;
	     }
	    backup();
	    if (ct == 0) return buildToken(BaleTokenType.BADNUMBER);
	    return buildToken(BaleTokenType.NUMBER);
	  }
	 backup();
       }
      for ( ; ; ) {
	 ch = nextChar();
	 if (!Character.isDigit(ch)) break;
       }
      if (ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {
	 return buildToken(BaleTokenType.NUMBER);
       }
      else if (ch != '.' && ch != 'e' || ch != 'E') {
	 backup();
	 return buildToken(BaleTokenType.NUMBER);
       }
      else {
	 int ct = 1;
	 if (ch == '.') {
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || !Character.isDigit(ch)) break;
	     }
	  }
	 if (ch == 'e' || ch == 'E') {
	    ch = nextChar();
	    if (ch == '+' || ch == '-') ch = nextChar();
	    ct = 0;
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || !Character.isDigit(ch)) break;
	       ++ct;
	     }
	  }
	 if (ch != 'f' && ch != 'F' && ch != 'd' && ch != 'D') backup();
	 if (ct == 0) return buildToken(BaleTokenType.BADNUMBER);
	 return buildToken(BaleTokenType.NUMBER);
       }
    }
   else if (ch == '"') {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch == '"') return buildToken(BaleTokenType.STRING);
	 else if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	    backup();
	    return buildToken(BaleTokenType.BADSTRING);
	  }
	 else if (ch == '\\') {
	    ch = nextChar();
	    if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	       backup();
	       return buildToken(BaleTokenType.BADSTRING);
	     }
	  }
       }
    }
   else if (ch == '\'') {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch == '\'') return buildToken(BaleTokenType.CHARLITERAL);
	 else if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	    backup();
	    return buildToken(BaleTokenType.BADCHARLIT);
	  }
	 else if (ch == '\\') {
	    ch = nextChar();
	    if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	       backup();
	       return buildToken(BaleTokenType.BADCHARLIT);
	     }
	  }
       }
    }
   else if (ch == '/') {
      ch = nextChar();
      if (ch == '/') {
	  return scanLineComment(); //added by amc6
       }
      else if (ch != '*') {
	 if (ch != '=') backup();
	 return buildToken(BaleTokenType.OP);		     // / or /=
       }
      else {
	 boolean formal = false;
	 ch = nextChar();
	 if (ch == '*') {
	    ch = nextChar();
	    if (ch == '/') {
	       backup();
	     }
	    else formal = true;
	    backup();
	  }
	 else backup();
	 return scanComment(formal);
       }
    }
   else if (ch == '{') return buildToken(BaleTokenType.LBRACE);
   else if (ch == '}') return buildToken(BaleTokenType.RBRACE);
   else if (ch == '(') return buildToken(BaleTokenType.LPAREN);
   else if (ch == ')') return buildToken(BaleTokenType.RPAREN);
   else if (ch == '[') return buildToken(BaleTokenType.LBRACKET);
   else if (ch == ']') return buildToken(BaleTokenType.RBRACKET);
   else if (ch == ';') return buildToken(BaleTokenType.SEMICOLON);
   else if (ch == ',') return buildToken(BaleTokenType.COMMA);
   else if (ch == '.') {
      if (nextChar() == '.') {
	 if (nextChar() == '.') return buildToken(BaleTokenType.OP);
	 else backup();
       }
      else backup();
      return buildToken(BaleTokenType.DOT);
    }
   else if (ch == '@') return buildToken(BaleTokenType.AT);
   else if (ch == '?') return buildToken(BaleTokenType.QUESTIONMARK);
   else if (ch == '<') return buildToken(BaleTokenType.LANGLE);
   else if (ch == '>') return buildToken(BaleTokenType.RANGLE);
   else if (ch == ':') return buildToken(BaleTokenType.COLON);
   else if (OP_CHARS.indexOf(ch) >= 0) {
      boolean eql = (ch == '=');
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch < 0 || ch == 0xffff || OP_CHARS.indexOf(ch) < 0) break;
	 String op = getInputString(token_start,cur_offset);
	 if (!op_set.contains(op)) break;
	 eql = (ch == '=');
       }
      backup();
      if (eql) return buildToken(BaleTokenType.EQUAL);
      return buildToken(BaleTokenType.OP);
    }
   else if (ch == -1 || ch == 0xffff) return buildToken(BaleTokenType.EOF);

   return buildToken(BaleTokenType.OTHER);
}


private Token scanLineComment()    //added by amc6
{
   token_state = BaleTokenState.IN_LINE_COMMENT;

   if (BALE_PROPERTIES.getBoolean(COMMENT_WRAPPING)) {
      for ( ; ; ) {
	 char ch = nextChar();
	 if (ch < 0 || ch == '\n' || ch == '\r' || ch == 0xffff) {
	    backup();
	    token_state = BaleTokenState.NORMAL;
	    return buildToken(BaleTokenType.LINECOMMENT);
	  }
	 else if (Character.isWhitespace(ch)) {
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || ch == '\n' || ch == '\r' || !Character.isWhitespace(ch)) break;
	     }
	    backup();
	    return buildToken(BaleTokenType.LINECOMMENT);
	  }
	 else {
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == '\n' || ch == 0xffff || ch == '\r' || Character.isWhitespace(ch)) {
		  backup();
		  return buildToken(BaleTokenType.LINECOMMENT);
		}
	     }
	  }
       }
    }
   else {
      for ( ; ; ) {
	 char ch = nextChar();
	 if (ch < 0 || ch == '\n' || ch == '\r' || ch == 0xffff) break;
       }
      backup();
      token_state = BaleTokenState.NORMAL;
      return buildToken(BaleTokenType.LINECOMMENT);
    }
}


private Token scanComment(boolean formalstart)
{
   boolean havestar = false;
   boolean formal = false;
   if (token_state == BaleTokenState.IN_FORMAL_COMMENT) formal = true;

   for ( ; ; ) {
      char ch = nextChar();
      if (havestar && ch == '/') {
	 token_state = BaleTokenState.NORMAL;
	 if (formal) return buildToken(BaleTokenType.ENDFORMALCOMMENT);
	 else return buildToken(BaleTokenType.ENDCOMMENT);
       }
      else if (ch < 0 || ch == '\n' || ch == 0xffff) {
	 if (formalstart) token_state = BaleTokenState.IN_FORMAL_COMMENT;
	 else token_state = BaleTokenState.IN_COMMENT;
	 if (formalstart) return buildToken(BaleTokenType.EOLFORMALCOMMENT);
	 else return buildToken(BaleTokenType.EOLCOMMENT);
       }
      else if (ch == '\r') {
	 ch = nextChar();
	 if (ch != '\n') backup();
	 if (formalstart) token_state = BaleTokenState.IN_FORMAL_COMMENT;
	 else token_state = BaleTokenState.IN_COMMENT;
	 if (formal) return buildToken(BaleTokenType.EOLFORMALCOMMENT);
	 else return buildToken(BaleTokenType.EOLCOMMENT);
       }
      else if (ch == '*') havestar = true;
      else {  //added by amc6
	 havestar = false;
	 if (formalstart) formal = true;
	 if (BALE_PROPERTIES.getBoolean(COMMENT_WRAPPING)) {
	    if (formalstart) token_state = BaleTokenState.IN_FORMAL_COMMENT;
	    else token_state = BaleTokenState.IN_COMMENT;

	    if (Character.isWhitespace(ch)) {
	       for ( ; ; ) {
		  ch = nextChar();
		  if (ch < 0 || ch == 0xffff || ch == '\n' || ch == '\r' || !Character.isWhitespace(ch)) break;
		}
	       backup();
	       if (formal) return buildToken(BaleTokenType.ENDFORMALCOMMENT);
	       else return buildToken(BaleTokenType.ENDCOMMENT);
	     }
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Character methods							*/
/*										*/
/********************************************************************************/

private char nextChar()
{
   if (cur_offset >= end_offset) {
      ++cur_offset;		// still need to do this to handle backup
      return (char) -1;
    }

   if (input_segment != null) return input_segment.charAt(cur_offset++);

   return input_text.charAt(cur_offset++);
}



private void backup()
{
   cur_offset--;
}



private String getInputString(int start,int end)
{
   if (input_segment != null) {
      return input_segment.subSequence(start,end).toString();
    }

   return input_text.substring(start,end);
}



/********************************************************************************/
/*										*/
/*	Token building methods							*/
/*										*/
/********************************************************************************/

private Token buildToken(BaleTokenType t)
{
   return new Token(t,token_start,cur_offset);
}



/********************************************************************************/
/*										*/
/*	Token subclass								*/
/*										*/
/********************************************************************************/

private static class Token implements BaleToken {

   private BaleTokenType token_type;
   private int start_offset;
   private int token_length;

   Token(BaleTokenType tt,int soff,int coff) {
      token_type = tt;
      start_offset = soff;
      token_length = coff - soff;
    }

   @Override public BaleTokenType getType()		{ return token_type; }
   @Override public int getStartOffset()		{ return start_offset; }
   @Override public int getLength()			{ return token_length; }

}	// end of inner class Token




/********************************************************************************/
/*										*/
/*	Static definitions							*/
/*										*/
/********************************************************************************/

static {
   keyword_map = new HashMap<String,BaleTokenType>();
   keyword_map.put("abstract",BaleTokenType.KEYWORD);
   keyword_map.put("assert",BaleTokenType.KEYWORD);
   keyword_map.put("boolean",BaleTokenType.KEYWORD);
   keyword_map.put("break",BaleTokenType.BREAK);
   keyword_map.put("byte",BaleTokenType.TYPEKEY);
   keyword_map.put("case",BaleTokenType.CASE);
   keyword_map.put("catch",BaleTokenType.CATCH);
   keyword_map.put("char",BaleTokenType.TYPEKEY);
   keyword_map.put("class",BaleTokenType.CLASS);
   keyword_map.put("const",BaleTokenType.KEYWORD);
   keyword_map.put("continue",BaleTokenType.KEYWORD);
   keyword_map.put("default",BaleTokenType.DEFAULT);
   keyword_map.put("do",BaleTokenType.DO);
   keyword_map.put("double",BaleTokenType.TYPEKEY);
   keyword_map.put("else",BaleTokenType.ELSE);
   keyword_map.put("enum",BaleTokenType.ENUM);
   keyword_map.put("extends",BaleTokenType.KEYWORD);
   keyword_map.put("false",BaleTokenType.KEYWORD);
   keyword_map.put("final",BaleTokenType.KEYWORD);
   keyword_map.put("finally",BaleTokenType.FINALLY);
   keyword_map.put("float",BaleTokenType.TYPEKEY);
   keyword_map.put("for",BaleTokenType.FOR);
   keyword_map.put("goto",BaleTokenType.GOTO);
   keyword_map.put("if",BaleTokenType.IF);
   keyword_map.put("implements",BaleTokenType.KEYWORD);
   keyword_map.put("import",BaleTokenType.KEYWORD);
   keyword_map.put("instanceof",BaleTokenType.KEYWORD);
   keyword_map.put("int",BaleTokenType.TYPEKEY);
   keyword_map.put("interface",BaleTokenType.INTERFACE);
   keyword_map.put("long",BaleTokenType.TYPEKEY);
   keyword_map.put("native",BaleTokenType.KEYWORD);
   keyword_map.put("new",BaleTokenType.NEW);
   keyword_map.put("null",BaleTokenType.KEYWORD);
   keyword_map.put("package",BaleTokenType.KEYWORD);
   keyword_map.put("private",BaleTokenType.KEYWORD);
   keyword_map.put("protected",BaleTokenType.KEYWORD);
   keyword_map.put("public",BaleTokenType.KEYWORD);
   keyword_map.put("return",BaleTokenType.RETURN);
   keyword_map.put("short",BaleTokenType.TYPEKEY);
   keyword_map.put("static",BaleTokenType.STATIC);
   keyword_map.put("strictfp",BaleTokenType.KEYWORD);
   keyword_map.put("super",BaleTokenType.KEYWORD);
   keyword_map.put("switch",BaleTokenType.SWITCH);
   keyword_map.put("synchronized",BaleTokenType.SYNCHRONIZED);
   keyword_map.put("this",BaleTokenType.KEYWORD);
   keyword_map.put("throw",BaleTokenType.KEYWORD);
   keyword_map.put("throws",BaleTokenType.KEYWORD);
   keyword_map.put("transient",BaleTokenType.KEYWORD);
   keyword_map.put("true",BaleTokenType.KEYWORD);
   keyword_map.put("try",BaleTokenType.TRY);
   keyword_map.put("void",BaleTokenType.TYPEKEY);
   keyword_map.put("volatile",BaleTokenType.KEYWORD);
   keyword_map.put("while",BaleTokenType.WHILE);

   op_set = new HashSet<String>();
   op_set.add("=");
   op_set.add("<");
   op_set.add("!");
   op_set.add("~");
   op_set.add("?");
   op_set.add(":");
   op_set.add("==");
   op_set.add("<=");
   op_set.add(">=");
   op_set.add("!=");
   op_set.add("||");
   op_set.add("&&");
   op_set.add("++");
   op_set.add("--");
   op_set.add("+");
   op_set.add("-");
   op_set.add("*");
   op_set.add("/");
   op_set.add("&");
   op_set.add("|");
   op_set.add("^");
   op_set.add("%");
   op_set.add("<<");
   op_set.add("+=");
   op_set.add("-=");
   op_set.add("*=");
   op_set.add("/=");
   op_set.add("&=");
   op_set.add("|=");
   op_set.add("^=");
   op_set.add("%=");
   op_set.add("<<=");
   op_set.add(">>=");
   op_set.add(">>>=");
   op_set.add(">>");
   op_set.add(">>>");
   op_set.add(">");
}




}	// end of class BaleTokenizer




/* end of BaleTokenizer.java */
