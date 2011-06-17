/********************************************************************************/
/*										*/
/*		BaleFactory.java						*/
/*										*/
/*	Bubble Annotated Language Editor factory for creating editors		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardAttributes;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.*;
import edu.brown.cs.bubbles.bueno.*;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import javax.swing.JPopupMenu;
import javax.swing.text.*;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 *	This class provides access to bubbles and editor components for code
 *	fragments.
 **/

public class BaleFactory implements BaleConstants, BudaConstants, BuenoConstants,
		BudaConstants.BudaFileHandler
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<File,BaleDocumentIde>	file_documents;
private StyleContext			style_context;
private BoardAttributes 		bale_attributes;
private BaleHighlightContext		global_highlights;
private SwingEventListenerList<BaleAnnotationListener> annot_listeners;
private Set<BaleAnnotation>		active_annotations;
private SwingEventListenerList<BaleContextListener> context_listeners;

private static BaleFactory	the_factory;

private static BumpClient      bump_client = null;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BaleFactory()
{
   bump_client.startIDE();

   file_documents = new HashMap<File,BaleDocumentIde>();
   style_context = new StyleContext();
   bale_attributes = new BoardAttributes("Bale");
   annot_listeners = new SwingEventListenerList<BaleAnnotationListener>(BaleAnnotationListener.class);
   active_annotations = new HashSet<BaleAnnotation>();
   context_listeners = new SwingEventListenerList<BaleContextListener>(BaleContextListener.class);
   BudaRoot.addFileHandler(this);

   addContextListener(new ProblemHover());
}


/**
 *	Return the singular instance of the BaleFactory object.
 **/

public synchronized static BaleFactory getFactory()
{
   if (the_factory == null) the_factory = new BaleFactory();

   return the_factory;
}


/**
 *	This routine initializes the Bale package.  It is called automatically at startup.
 **/

public static void setup()
{
   BaleConfigurator bc = new BaleConfigurator();
   BudaRoot.addBubbleConfigurator("BALE",bc);
   BudaRoot.addPortConfigurator("BALE",bc);

   BuenoFactory.getFactory().addInsertionHandler(new BaleInserter());
}



/**
 *	Called to initialize once BudaRoot is setup
 **/

public static void initialize(BudaRoot br)
{
   bump_client = BumpClient.getBump();

   bump_client.addOpenEditorBubbleHandler(new BaleOpenEditorHandler(br));
}



/********************************************************************************/
/*										*/
/*	Factory methods for method editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createMethodFragmentEditor(String proj,String method)
{
   List<BumpLocation> locs = bump_client.findMethod(proj,method,false);

   return getEditorFromLocations(locs);
}




BaleFragmentEditor createMethodFragmentEditor(BumpLocation loc)
{
   if (loc == null) return null;

   List<BumpLocation> locs = new ArrayList<BumpLocation>();
   locs.add(loc);

   return getEditorFromLocations(locs);
}



/********************************************************************************/
/*										*/
/*	Factory methods for field editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createFieldFragmentEditor(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findFields(proj,cls);

   return getEditorFromLocations(locs);
}



/********************************************************************************/
/*										*/
/*	Factory methods for class editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createStaticsFragmentEditor(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findClassInitializers(proj,cls);

   return getEditorFromLocations(locs,BaleFragmentType.STATICS,cls + ".<INITIALIZERS>");
}



BaleFragmentEditor createClassPrefixFragmentEditor(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findClassHeader(proj,cls);

   return getEditorFromLocations(locs,BaleFragmentType.HEADER,cls + ".<PREFIX>");
}



BaleFragmentEditor createClassFragmentEditor(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findClassDefinition(proj,cls);

   return getEditorFromLocations(locs,BaleFragmentType.CLASS,cls);
}



/********************************************************************************/
/*										*/
/*	Factory methods for file editors					*/
/*										*/
/********************************************************************************/

BaleFragmentEditor createFileEditor(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findCompilationUnit(proj,cls);

   return getEditorFromLocations(locs,BaleFragmentType.FILE,cls + ".<FILE>");
}



/********************************************************************************/
/*										*/
/*	Generic methods for editors						*/
/*										*/
/********************************************************************************/

/**
 *	Create a code bubble corresponding to the current location.  The bubble
 *	should be for the same bubble area as the given src component.	If uselnk
 *	is true, then a link will be created from the bubble associated with the
 *	source component.  If the position p is given, this link will be associated
 *	with the line containing p, otherwise it will be a general link from the source
 *	bubble.  The at point if non-null specifies where to locate the bubble.  If it
 *	is null, the bubble will be located near the source bubble, close enough to be
 *	in the same group if the near flag is set, a little further away otherwise.
 *	Finally, if the add flag is set, the bubble will be added to the bubble area
 *	and displayed.
 *	@param src component identifying the source bubble
 *	@param p optional location in the source for creating a link
 *	@param at optional display location for the new bubble
 *	@param near flag indicating whether the new bubble should be close (same group) or
 *	further away from the src bubble if not specific point is given
 *	@param bl location identifying the code fragment to create a bubble for
 *	@param uselnk if true, then a link will be created between the source bubble and the
 *	new bubble
 *	@param add if true, then the bubble will be added to the bubble area and displayed
 *	@param marknew if true, then the bubble will be marked as new
 **/
// Modified By Hsu-Sheng Ko

BudaBubble createLocationEditorBubble(Component src,Position p,Point at,
						      boolean near,
						      BumpLocation bl,
						      boolean uselnk,boolean add, boolean marknew)
{
   if (bl == null) return null;
   BudaRoot root = BudaRoot.findBudaRoot(src);
   BudaBubble obbl = BudaRoot.findBudaBubble(src);
   Rectangle loc = BudaRoot.findBudaLocation(src);

   int offset = (near ? BUBBLE_CREATION_NEAR_SPACE : BUBBLE_CREATION_SPACE);

   if (root == null) return null;

   BaleFragmentEditor fed = null;
   switch (bl.getSymbolType()) {
      case FUNCTION :
      case CONSTRUCTOR :
	 fed = createMethodFragmentEditor(bl);
	 break;
      case FIELD :
      case ENUM_CONSTANT :
	 String fnm = bl.getSymbolName();
	 int idx = fnm.lastIndexOf(".");
	 if (idx > 0) {
	    String cnm = fnm.substring(0,idx);
	    fed = createFieldFragmentEditor(bl.getSymbolProject(),cnm);
	  }
	 break;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
	 fed = createClassFragmentEditor(bl.getSymbolProject(),bl.getSymbolName());
	 break;
      case STATIC_INITIALIZER :
	 fnm = bl.getSymbolName();
	 idx = fnm.lastIndexOf(".");
	 if (idx > 0) {
	    String cnm = fnm.substring(0,idx);
	    fed = createStaticsFragmentEditor(bl.getSymbolProject(),cnm);
	  }
	 break;
    }

   if (fed == null) return null;

   BaleEditorBubble bb = new BaleEditorBubble(fed);
   if (add) {
      if (at != null) root.add(bb,new BudaConstraint(at));
      if (uselnk && obbl != null) {
	 BudaConstants.LinkPort port0;
	 if (p == null) port0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);
	 else {
	    port0 = new BaleLinePort(src,p,null);
	    loc.y += port0.getLinkPoint(BudaRoot.findBudaBubble(src),
					   BudaRoot.findBudaBubble(src).getLocation()).y;
	 }
	 if (at == null) root.add(bb,new BudaConstraint(loc.x+loc.width+offset,loc.y));
	 BudaConstants.LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	 BudaBubbleLink lnk = new BudaBubbleLink(obbl,port0,bb,port1);
	 root.addLink(lnk);
       }
      else if (at == null) root.add(bb,new BudaConstraint(loc.x+loc.width+offset,loc.y));
      if (marknew) bb.markBubbleAsNew();
    }

   return bb;
}




/********************************************************************************/
/*										*/
/*	Generic methods for creating bubbles/bubble stack from locations	*/
/*										*/
/********************************************************************************/

/**
 *	Creates an appropriate bubble for the given set of locations.  If the set
 *	is small (i.e. 1 or 2 elements), the explicit bubbles are created.  Otherwise
 *	a bubble stack is created.  The stack/bubbles are linked to the given source
 *	bubble at the given position if the link flag is set.  The position of the
 *	new bubbles is either at the explicit point or close to the source bubble, with
 *	the closeness dependent on the near flag.
 **/

public void createBubbleStack(Component src,Position p,Point pt,boolean near,
			     Collection<BumpLocation> locs,BudaLinkStyle link)
{
   BaleBubbleStack.createBubbles(src,p,pt,near,locs,link);
}



/********************************************************************************/
/*										*/
/*	Methods for creating bubbles for code fragments 			*/
/*										*/
/********************************************************************************/

/**
 *	Return the code bubble for a method code fragment.   The fragment is
 *	given by its fully qualified name (including parameter types).	If the
 *	method is ambiguous, the corresponding editor will contain code fragments
 *	for all instances separated by budding lines.
 **/

public BudaBubble createMethodBubble(String proj,String fct)
{
   if (fct.contains(".<clinit>()")) {
      BoardLog.logD("BALE","Creating method bubble for static initializer");
      int idx = fct.indexOf(".<clinit>()");
      String cls = fct.substring(0,idx);
      return createStaticsBubble(proj,cls);
    }

   BaleFragmentEditor bfe = createMethodFragmentEditor(proj,fct);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}





/**
 *	Return the code bubble for a system code fragment.   The fragment is
 *	given by its fully qualified name (including parameter types).	The source
 *	file to be used is passed in.
 **/

public BudaBubble createSystemMethodBubble(String proj,String fct,File src)
{
   // this doesn't do what we want it to
   List<BumpLocation> locs = bump_client.findMethod(proj,fct,true);

   if (locs == null || locs.isEmpty()) return null;

   // need to create a bubble using the given file here

   return null;
}








/**
 *	Return the code bubble for all the fields of the given class.
 **/

public BudaBubble createFieldsBubble(String proj,String cls)
{
   BaleFragmentEditor bfe = createFieldFragmentEditor(proj,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}




/**
 *	Return the code bubble for all static initializers of the given class.
 **/

public BudaBubble createStaticsBubble(String proj,String cls)
{
   BaleFragmentEditor bfe = createStaticsFragmentEditor(proj,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}




/**
 *	Return the code bubble for the class prefix of the given class.  This includes
 *	the header information (e.g. package, imports) as well as the class declaration.
 **/

public BudaBubble createClassPrefixBubble(String proj,String cls)
{
   BaleFragmentEditor bfe = createClassPrefixFragmentEditor(proj,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}




/**
 *	Return the class code bubble for the given class.
 **/

public BudaBubble createClassBubble(String proj,String cls)
{
   if (cls == null) return null;

   BaleFragmentEditor bfe = createClassFragmentEditor(proj,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}



/**
 *	Return the file code bubble for the given class.
 **/

public BudaBubble createFileBubble(String proj,String cls)
{
   BaleFragmentEditor bfe = createFileEditor(proj,cls);
   if (bfe == null) return null;

   return new BaleEditorBubble(bfe);
}



/********************************************************************************/
/*										*/
/*	Factory methods for file access 					*/
/*										*/
/********************************************************************************/

/**
 *	Return a handle to the internal representation of an open file based on
 *	the project and file name.
 **/

public BaleFileOverview getFileOverview(String proj,File file)
{
   return getDocument(proj,file);
}



/********************************************************************************/
/*										*/
/*	Calls for creating new methods						*/
/*										*/
/********************************************************************************/

/**
 *	Create a new method and a bubble displaying that method.  The bubble is
 *	added to the display
 *
 *	@param proj project containing the class
 *	@param name fully qualified name of the new method
 *	@param params list of parameter types (and names if desired)
 *	@param returns return type
 *	@param modifiers modifier flags (in Java reflection format)
 *	@param comment insert a comment before the method if true
 *	@param after element in the source that the new method should follow. If null, then
 *	the new method is inserted at the end of the class.
 *	@param source component identifying the source bubble and bubble area
 *	@param pos the optional position in the source component to be used for linking
 *	@param link if true, then a link is created between the source bubble and the newly
 *	created bubble.
 **/

public BudaBubble createNewMethod(String proj,
					   String name,
					   String params,
					   String returns,
					   int modifiers,
					   boolean comment,
					   String after,
					   Component source,
					   Position pos,
					   boolean link,
					   boolean add)
{
   String clsnm,mthdnm;
   int idx = name.lastIndexOf(".");
   if (idx < 0) {
      clsnm = null;
      mthdnm = name;
   }
   else {
      clsnm = name.substring(0,idx);
      mthdnm = name.substring(idx+1);
   }

   BuenoProperties bp = new BuenoProperties();
   bp.put(BuenoKey.KEY_NAME,mthdnm);
   if (params != null) bp.put(BuenoKey.KEY_PARAMETERS,params);
   if (returns != null) bp.put(BuenoKey.KEY_RETURNS,returns);
   bp.put(BuenoKey.KEY_MODIFIERS,modifiers);
   if (comment) bp.put(BuenoKey.KEY_ADD_COMMENT,Boolean.TRUE);
   BuenoLocation bl = BuenoFactory.getFactory().createLocation(proj,clsnm,after,true);

   BuenoFactory.getFactory().createNew(BuenoType.NEW_METHOD,bl,bp);
   if (bl.getInsertionFile() == null) return null;

   BaleDocumentIde doc = BaleFactory.getFactory().getDocument(proj,bl.getInsertionFile());
   List<BumpLocation> blocs = bump_client.findMethod(proj,name,false);
   if (blocs == null || blocs.size() == 0) return null;

   BumpLocation loc = null;
   for (BumpLocation bloc : blocs) {
      BaleRegion rgn = doc.getRegionFromLocation(bloc);
      if (rgn == null) continue;
      int rs = rgn.getStart();
      int re = rgn.getEnd();
      if (bl.getInsertionOffset() < re && bl.getInsertionOffset() + bl.getInsertionLength() > rs) {
	 loc = bloc;
	 break;
      }
   }

   if (loc == null) return null;

   return createLocationEditorBubble(source,pos,null,true,loc,link,add,true);
}




/********************************************************************************/
/*										*/
/*	Methods for creating links						*/
/*										*/
/********************************************************************************/

/**
 *	Create a link for a bale bubble given at the given line
 **/

public BudaConstants.LinkPort findPortForLine(BudaBubble bb,int line)
{
   if (!(bb instanceof BaleEditorBubble)) return null;

   BaleFragmentEditor bfe = (BaleFragmentEditor) bb.getContentPane();
   BaleDocument bd = bfe.getDocument();
   int loff = bd.findLineOffset(line);

   try {
      Position p = bd.createPosition(loff);
      return new BaleLinePort(bb,p,null);
    }
   catch (BadLocationException e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	Style methods								*/
/*										*/
/********************************************************************************/

StyleContext getStyleContext()			{ return style_context; }



/********************************************************************************/
/*										*/
/*	Attribute methods							*/
/*										*/
/********************************************************************************/

AttributeSet getAttributes(String id)
{
   return bale_attributes.getAttributes(id);
}




/********************************************************************************/
/*										*/
/*	Highlighting methods							*/
/*										*/
/********************************************************************************/

synchronized BaleHighlightContext getGlobalHighlightContext()
{
   if (global_highlights == null) {
      global_highlights = new BaleHighlightContext();
    }

   return global_highlights;
}




/********************************************************************************/
/*										*/
/*	IDE File Document methods						*/
/*										*/
/********************************************************************************/

BaleDocumentIde getDocument(String proj,File f)
{
   BaleDocumentIde fdoc = null;

   if (f == null) return null;

   synchronized (file_documents) {
      fdoc = file_documents.get(f);
      if (fdoc == null) {
	 fdoc = new BaleDocumentIde(proj,f);
	 file_documents.put(f,fdoc);
       }
    }

   return fdoc;
}


@Override public void handleSaveRequest()
{
   Collection<BaleDocumentIde> docs;
   synchronized (file_documents) {
      docs = new ArrayList<BaleDocumentIde>(file_documents.values());
    }
   for (BaleDocumentIde doc : docs) {
      if (doc.canSave()) doc.save();
   }
}




@Override public void handleCheckpointRequest()
{
   Collection<BaleDocumentIde> docs;
   synchronized (file_documents) {
      docs = new ArrayList<BaleDocumentIde>(file_documents.values());
    }
   for (BaleDocumentIde doc : docs) {
      if (doc.canSave()) doc.checkpoint();
   }
}




@Override public boolean handleQuitRequest()
{
   return true;
}



/********************************************************************************/
/*										*/
/*	Annotation methods							*/
/*										*/
/********************************************************************************/

void addAnnotationListener(BaleAnnotationListener bal)
{
   annot_listeners.add(bal);
}


void removeAnnotationListener(BaleAnnotationListener bal)
{
   annot_listeners.remove(bal);
}


List<BaleAnnotation> getAnnotations(BaleDocument bd)
{
   List<BaleAnnotation> rslt = new ArrayList<BaleAnnotation>();
   synchronized (active_annotations) {
      for (BaleAnnotation ba : active_annotations) {
	 if (ba.getFile() == null) continue;
	 if (bd == null || ba.getFile().equals(bd.getFile())) rslt.add(ba);
       }
    }

   return rslt;
}



/**
 *	Add a new annotation
 **/

public void addAnnotation(BaleAnnotation ba)
{
   synchronized (active_annotations) {
      if (active_annotations.add(ba)) {
	 for (BaleAnnotationListener bal : annot_listeners) {
	    bal.annotationAdded(ba);
	  }
       }
    }
}



/**
 *	Remove an annotation
 **/

public void removeAnnotation(BaleAnnotation ba)
{
   synchronized (active_annotations) {
      if (active_annotations.remove(ba)) {
	 for (BaleAnnotationListener bal : annot_listeners) {
	    bal.annotationRemoved(ba);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Popup/Hover context methods						*/
/*										*/
/********************************************************************************/

/**
 *	Add a context listener for popup and hover events
 **/

public void addContextListener(BaleContextListener bcl)
{
   context_listeners.add(bcl);
}


/**
 *	Remove a context listener for popup and hover events
 **/

public void removeContextListener(BaleContextListener bcl)
{
   context_listeners.remove(bcl);
}



BudaBubble getContextHoverBubble(BaleContextConfig cfg)
{
   for (BaleContextListener bcl : context_listeners) {
      BudaBubble bb = bcl.getHoverBubble(cfg);
      if (bb != null) return bb;
    }

   return null;
}



void addContextMenuItems(BaleContextConfig cfg,JPopupMenu menu)
{
   for (BaleContextListener bcl : context_listeners) {
      bcl.addPopupMenuItems(cfg,menu);
    }
}



String getContextToolTip(BaleContextConfig cfg)
{
   StringBuffer buf = null;

   for (BaleContextListener bcl : context_listeners) {
      String t = bcl.getToolTipHtml(cfg);
      if (t != null) {
	 if (buf == null) {
	    buf = new StringBuffer();
	    buf.append("<html><body>");
	  }
	 else buf.append("<br>");
	 buf.append(t);
       }
    }

   if (buf == null) return null;
   buf.append("\n<br>\n");

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Methods to handle reset 						*/
/*										*/
/********************************************************************************/

List<BaleRegion> getFragmentRegions(BaleDocument doc)
{
   List<BumpLocation> locs = null;
   String proj = doc.getProjectName();
   String nam = doc.getFragmentName();
   if (nam == null) return null;
   int idx = nam.lastIndexOf(".");
   String cnam = nam;
   if (idx > 0) cnam = nam.substring(0,idx);

   switch (doc.getFragmentType()) {
      case METHOD :
	 locs = bump_client.findMethod(proj,nam,false);
	 break;
      case CLASS :
	 locs = bump_client.findClassDefinition(proj,nam);
	 break;
      case FILE :
	 locs = bump_client.findCompilationUnit(proj,cnam);
	 break;
      case FIELDS :
	 locs = bump_client.findFields(proj,cnam);
	 break;
      case STATICS :
	 locs = bump_client.findClassInitializers(proj,cnam);
	 break;
      case HEADER :
	 locs = bump_client.findClassHeader(proj,cnam);
	 break;
      default :
	 BoardLog.logE("BALE","Unknown fragment type : " + doc.getFragmentType());
	 break;
    }

   if (locs == null) return null;

   List<BaleRegion> rgns = getRegionsFromLocations(locs);

   return rgns;
}




/********************************************************************************/
/*										*/
/*	Methods to create editor from a list of Bump Locations			*/
/*										*/
/********************************************************************************/

private BaleFragmentEditor getEditorFromLocations(List<BumpLocation> locs)
{
   if (locs == null || locs.size() == 0) return null;

   BumpLocation loc0 = locs.get(0);

   BaleFragmentType ftyp;
   String fragname = loc0.getSymbolName();

   switch (loc0.getSymbolType()) {
      case FUNCTION :
      case CONSTRUCTOR :
	 ftyp = BaleFragmentType.METHOD;
	 fragname += loc0.getParameters();
	 break;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
	 ftyp = BaleFragmentType.CLASS;
	 break;
      case STATIC_INITIALIZER :
	 ftyp = BaleFragmentType.STATICS;
	 break;
      case ENUM_CONSTANT :
      case FIELD :
	 ftyp = BaleFragmentType.FIELDS;
	 int idx = fragname.lastIndexOf(".");
	 fragname = fragname.substring(0,idx) + ".< FIELDS >";
	 break;
      default :
	 return null;
    }

   return getEditorFromLocations(locs,ftyp,fragname);
}



private List<BaleRegion> getRegionsFromLocations(List<BumpLocation> locs)
{
   if (locs == null || locs.size() == 0) return null;

   BumpLocation loc0 = locs.get(0);
   String proj = loc0.getSymbolProject();
   File f = loc0.getFile();
   BaleDocumentIde fdoc = getDocument(proj,f);

   Segment s = new Segment();
   try {
      fdoc.getText(0,fdoc.getLength(),s);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Bad document: " + e);
      return null;
    }

   List<BaleRegion> rgns = new ArrayList<BaleRegion>();
   BaleRegion lastrgn = null;

   for (BumpLocation bl : locs) {
      if (bl.getDefinitionOffset() < 0) continue;
      File f1 = bl.getFile();
      if (f1 != null && !f1.equals(f)) continue;

      int soffset = fdoc.mapOffsetToJava(bl.getDefinitionOffset());
      int eoffset = fdoc.mapOffsetToJava(bl.getDefinitionEndOffset())+1;
      if (soffset >= s.length()) continue;

      // extend the logical regions and note if it ends with a new line
      while (soffset > 0) {
	 if (s.charAt(soffset-1) == '\n') break;
	 else if (!Character.isWhitespace(s.charAt(soffset-1))) break;
	 --soffset;
       }
      boolean havecmmt = false;
      if (eoffset > 0 && eoffset < s.length() && s.charAt(eoffset-1) != '\n') {        // extend if we don't end on eol
	 while (eoffset < fdoc.getLength()) {
	    if (s.charAt(eoffset) == '\n') {
	       ++eoffset;
	       break;
	     }
	    else if (havecmmt) ;
	    else if (Character.isWhitespace(s.charAt(eoffset))) ;
	    else if (s.charAt(eoffset) == '/' && s.charAt(eoffset+1) == '/') {
	       havecmmt = true;
	     }
	    else break;
	    ++eoffset;
	  }
       }

      boolean haveeol;
      if (eoffset > s.length()) {
	 haveeol = true;
	 eoffset = s.length();
       }
      else haveeol = s.charAt(eoffset-1) == '\n';

      BaleRegion br = null;

      try {
	 Position spos = BaleStartPosition.createStartPosition(fdoc,soffset);
	 Position epos = fdoc.createPosition(eoffset);
	 br = new BaleRegion(spos,epos,haveeol);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Bad location from eclipse for fragment: " + e);
	 continue;
       }

      if (lastrgn != null) {
	 BaleRegion nbr = mergeRegions(lastrgn,br,s);
	 if (nbr != null) {
	    rgns.remove(lastrgn);
	    br = nbr;
	  }
       }

      rgns.add(br);
      lastrgn = br;
    }

   return rgns;
}



private BaleFragmentEditor getEditorFromLocations(List<BumpLocation> locs,
						     BaleFragmentType ftyp,
						     String fragname)
{
   if (locs == null || locs.size() == 0) return null;

   List<BaleRegion> rgns = getRegionsFromLocations(locs);

   BumpLocation loc0 = locs.get(0);
   String proj = loc0.getSymbolProject();
   File f = loc0.getFile();
   BaleDocumentIde fdoc = getDocument(proj,f);

   return new BaleFragmentEditor(proj,f,fragname,fdoc,ftyp,rgns);
}



BaleFragmentEditor getEditorFromRegions(String proj,File f,String fragname,
					   List<BaleRegion> locs,
					   BaleFragmentType ftyp)
{
   if (locs == null || locs.size() == 0) return null;

   List<BaleRegion> rgns = new ArrayList<BaleRegion>();
   BaleRegion lastrgn = null;

   BaleDocumentIde fdoc = getDocument(proj,f);

   Segment s = new Segment();
   try {
      fdoc.getText(0,fdoc.getLength(),s);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Bad document: " + e);
      return null;
    }

   for (BaleRegion br : locs) {
      if (lastrgn != null) {
	 BaleRegion nbr = mergeRegions(lastrgn,br,s);
	 if (nbr != null) {
	    rgns.remove(lastrgn);
	    br = nbr;
	  }
       }
      else {
	 int soff = br.getStart();
	 int eoff = br.getEnd();
	 int soff1 = soff;
	 while (soff1 > 0) {
	    if (s.charAt(soff1) == '\n') {
	       ++soff1;
	       break;
	    }
	    --soff1;
	 }
	 int eoff1 = eoff;
	 while (eoff1 < s.length()) {
	    if (s.charAt(eoff1) == '\n') {
	       ++eoff1;
	       break;
	    }
	    else if (Character.isWhitespace(s.charAt(eoff1))) {
	       --eoff1;
	    }
	    else {
	       eoff1 = eoff;
	       break;
	    }
	 }
	 if (soff1 != soff || eoff1 != eoff) {
	    try {
	       br = fdoc.createDocumentRegion(soff1, eoff1, true);
	    }
	    catch (BadLocationException ex) { }
	 }
      }

      rgns.add(br);
      lastrgn = br;
    }

   return new BaleFragmentEditor(proj,f,fragname,fdoc,ftyp,rgns);
}





private BaleRegion mergeRegions(BaleRegion r1,BaleRegion r2,Segment txt)
{
   int s1 = r1.getStart();
   int e1 = r1.getEnd();
   int s2 = r2.getStart();
   int e2 = r2.getEnd();

   if (e2 < s1) {		// second ends before first, try opposite order
      return mergeRegions(r2,r1,txt);
    }
   if (s2 < s1) return null;
   int delta = s2-e1;
   if (delta > 4) return null;	// not even close -- don't merge

   boolean merge = false;
   if (e1 > s2 || e1 == s2 || e1+1 == s2) merge = true; 	// easy cases
   else {
      merge = true;
      for (int i = e1; merge && i < s2; ++i) {
	 char c = txt.charAt(i);
	 // Might want to allow comments here (with a larger delta)
	 if (!Character.isWhitespace(c)) merge = false;
       }
    }

   if (!merge) return null;

   BaleRegion br = null;

   if (e2 < e1) br = r1;
   else br = new BaleRegion(r1.getStartPosition(),r2.getEndPosition(),r2.includesEol());

   return br;
}




/********************************************************************************/
/*										*/
/*	Class to handle hovering over problems					*/
/*										*/
/********************************************************************************/

private static class ProblemHover implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg)	{ return null; }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      BaleDocument bd = (BaleDocument) cfg.getDocument();
      List<BumpProblem> probs = bd.getProblemsAtLocation(cfg.getOffset());
      if (probs != null) {
	 for (BumpProblem bp : probs) {
	    if (bp.getFixes() != null) {
	       for (BumpFix bf : bp.getFixes()) {
		  BaleFixer fixer = new BaleFixer(bp,bf);
		  if (fixer.isValid()) menu.add(fixer);
		}
	     }
	  }
       }
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      BaleDocument bd = (BaleDocument) cfg.getDocument();
      List<BumpProblem> probs = bd.getProblemsAtLocation(cfg.getOffset());
      if (probs == null || probs.size() == 0) return null;

      StringBuffer buf = new StringBuffer();
      for (BumpProblem bp : probs) {
	 buf.append("<p>");
	 buf.append(bp.getMessage());
       }
      return buf.toString();
    }

}	// end of inner class ProblemHover





}	// end of class BaleFactory




/* end of BaleFactory.java */
