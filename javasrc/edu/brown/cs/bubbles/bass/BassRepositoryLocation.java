/********************************************************************************/
/*										*/
/*		BassRepositoryLocation.java					*/
/*										*/
/*	Bubble Augmented Search Strategies store for all possible names 	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bump.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BassRepositoryLocation implements BassConstants.BassRepository, BassConstants,
			BumpConstants.BumpChangeHandler
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<BassName>	all_names;
private boolean 	is_ready;

private Pattern 	anonclass_pattern = Pattern.compile("\\$[0-9]");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassRepositoryLocation()
{
   all_names = new HashSet<BassName>();
   is_ready = false;

   initialize();

   BumpClient.getBump().addChangeHandler(this);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Iterable<BassName> getAllNames()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
      return new ArrayList<BassName>(all_names);
    }
}



@Override public boolean includesRepository(BassRepository br)	{ return br == this; }




void waitForNames()
{
   synchronized (this) {
      while (!is_ready) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
    }
}




BassName findBubbleName(File f,int eclipsepos)
{
   BassNameLocation best = null;
   int bestlen = 0;

   waitForNames();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 if (bnl.getFile().equals(f)) {
	    int spos = bnl.getEclipseStartOffset();
	    int epos = bnl.getEclipseEndOffset();
	    if (best == null || epos - spos <= bestlen) {
	       if (best != null && epos - spos == bestlen) {
		  if (best.getNameType() == BassNameType.HEADER && bnl.getNameType() == BassNameType.CLASS) ;
		  else continue;
		}
	       if (spos-16 <= eclipsepos && epos+16 > eclipsepos) {	// allow for indentations
		  best = bnl;
		  bestlen = epos - spos;
		}
	     }
	  }
       }
    }

   // TODO: handle fields, prefix ??

   return best;
}



File findActualFile(File f)
{
   waitForNames();

   synchronized (this) {
      for (BassName bn : all_names) {
	 BassNameLocation bnl = (BassNameLocation) bn;
	 if (bnl.getFile().equals(f)) return f;
	 if (bnl.getFile().getName().equals(f.getName())) return bnl.getFile();
       }
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void initialize()
{							
   synchronized (this) {
      all_names.clear();
      is_ready = false;
    }

   Searcher s = new Searcher();
   BoardThreadPool.start(s);
}



private synchronized void loadNames()
{
   Map<String,BassNameLocation> fieldmap = new HashMap<String,BassNameLocation>();
   Map<String,BassNameLocation> staticmap = new HashMap<String,BassNameLocation>();

   BumpClient bc = BumpClient.getBump();
   Collection<BumpLocation> locs = bc.findAllNames(null);
   if (locs != null) {
      for (BumpLocation bl : locs) {
	 addLocation(bl,fieldmap,staticmap);
       }
    }

   is_ready = true;
   notifyAll();
}



private void addLocation(BumpLocation bl,Map<String,BassNameLocation> fieldmap,
			    Map<String,BassNameLocation> staticmap)
{
   if (!isRelevant(bl)) return;

   BassNameLocation bn = new BassNameLocation(bl);
   switch (bn.getNameType()) {
      case FIELDS :
	 BassNameLocation fbn = fieldmap.get(bn.getNameHead());
	 if (fbn != null) {
	    fbn.addLocation(bl);
	    bn = null;
	  }
	 else fieldmap.put(bn.getNameHead(),bn);
	 break;
      case STATICS :
	 BassNameLocation sbn = staticmap.get(bn.getNameHead());
	 if (sbn != null) {
	    sbn.addLocation(bl);
	    bn = null;
	  }
	 else staticmap.put(bn.getNameHead(),bn);
	 break;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
	 all_names.add(bn);
	 if (!bn.getKey().contains("$")) {
	    BassNameLocation fnm = new BassNameLocation(bl,BassNameType.FILE);
	    all_names.add(fnm);
	  }
	 bn = new BassNameLocation(bl,BassNameType.HEADER);
	 break;
      case PROJECT :
	 // if (all_names.size() != 0) bn = null;
	 break;
    }

   if (bn != null) all_names.add(bn);
}




private boolean isRelevant(BumpLocation bl)
{
   switch (bl.getSymbolType()) {
      case PACKAGE :
      case LOCAL :
      case UNKNOWN :
	 return false;
    }

   if (bl.getKey() == null) return false;

   Matcher m = anonclass_pattern.matcher(bl.getKey());
   if (m.find()) return false;

   return true;
}



private class Searcher implements Runnable {

   @Override public void run() {
      loadNames();
    }

   @Override public String toString()		{ return "BASS_LocationSearcher"; }

}	// end of inner class Searcher




/********************************************************************************/
/*										*/
/*	Change detection methods						*/
/*										*/
/********************************************************************************/

@Override public void handleFileChanged(String proj,String file)
{
   addNamesForFile(proj,file,true);
}



@Override public void handleFileAdded(String proj,String file)
{
   addNamesForFile(proj,file,false);
}



@Override public void handleFileRemoved(String proj,String file)
{
   removeNamesForFile(proj,file);

   BassFactory.reloadRepository(this);
}


private void removeNamesForFile(String proj,String file)
{
   File f = new File(file);

   synchronized (this) {
      for (Iterator<BassName> it = all_names.iterator(); it.hasNext(); ) {
	 BassName bn = it.next();
	 BumpLocation bl = bn.getLocation();
	 if (bl != null && f.equals(bl.getFile()) &&
		(proj == null || proj.equals(bl.getProject())))
	    it.remove();
       }
    }
}



private void addNamesForFile(String proj,String file,boolean rem)
{
   Map<String,BassNameLocation> fieldmap = new HashMap<String,BassNameLocation>();
   Map<String,BassNameLocation> staticmap = new HashMap<String,BassNameLocation>();
   List<String> fls = new ArrayList<String>();
   fls.add(file);

   Collection<BumpLocation> locs = BumpClient.getBump().findAllNames(proj,fls,true);

   synchronized (this) {
      if (rem) removeNamesForFile(proj,file);
      if (locs != null) {
	 for (BumpLocation bl : locs) {
	    addLocation(bl,fieldmap,staticmap);
	  }
       }
    }

   BassFactory.reloadRepository(this);
}



}	// end of class BassRepositoryLocation




/* end of BassRepositoryLocation.java */
