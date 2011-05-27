/********************************************************************************/
/*										*/
/*		BanalPackageGraph.java						*/
/*										*/
/*	Bubbles ANALysis package visitor to create a package graph		*/
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



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import edu.brown.cs.bubbles.org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.*;


class BanalPackageGraph extends BanalDefaultVisitor implements BanalConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String			project_name;
private String			package_name;
private Map<String,ClassData>	class_nodes;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BanalPackageGraph(String proj,String pkg)
{
   project_name = proj;
   package_name = pkg;
   class_nodes = new HashMap<String,ClassData>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Collection<BanalPackageClass> getClassNodes()
{
   return new ArrayList<BanalPackageClass>(class_nodes.values());
}



/********************************************************************************/
/*										*/
/*	Visitors to handle start/stop/checking relevance			*/
/*										*/
/********************************************************************************/

@Override public boolean checkUseProject(String proj)
{
   if (project_name == null) return true;

   return project_name.equals(proj);
}

								


@Override public boolean checkUseClass(String cls)
{
   if (package_name == null) return true;

   if (!cls.startsWith(package_name)) return false;

   int ln = package_name.length();
   if (ln == cls.length() || cls.charAt(ln) == '.') return true;

   return false;
}



/********************************************************************************/
/*										*/
/*	Basic class data							*/
/*										*/
/********************************************************************************/

@Override public void visitClass(BanalClass bc,String sign,int access)
{
   ClassData cd = findClass(bc);

   cd.setAccess(access);
}



private ClassData findClass(BanalClass bc)
{
   String k = bc.getInternalName();

   ClassData cd = class_nodes.get(k);
   if (cd == null) {
      cd = new ClassData(bc.getJavaName());
      class_nodes.put(k,cd);
    }

   return cd;
}



private boolean isClassRelevant(BanalClass bc)
{
   String nm = bc.getJavaName();

   return checkUseClass(nm);
}



private LinkData findLink(BanalClass frm,BanalClass to)
{
   ClassData fc = findClass(frm);
   ClassData tc = findClass(to);

   LinkData ld = fc.createLinkTo(tc);

   return ld;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("PACKAGE_GRAPH");
   xw.field("PROJECT",project_name);
   xw.field("PACKAGE",package_name);
   for (ClassData cd : class_nodes.values()) {
      cd.outputXml(xw);
    }
   xw.end("PACKAGE_GRAPH");
}




/********************************************************************************/
/*										*/
/*	Hierarchy visiting methods						*/
/*										*/
/********************************************************************************/

@Override public void visitSuper(BanalClass cls,BanalClass sup,boolean isiface)
{
   ClassData cd = findClass(cls);
   LinkData ld = findLink(cls,sup);

   PackageRelationType rtyp = PackageRelationType.SUPERCLASS;
   if (isiface) rtyp = PackageRelationType.IMPLEMENTS;
   if (cd.isInterface()) rtyp = PackageRelationType.EXTENDS;

   ld.addRelation(rtyp);
}


@Override public void visitInnerClass(BanalClass cls,BanalClass icls,int acc)
{
   LinkData ld = findLink(cls,icls);
   if (ld == null) return;
   PackageRelationType rtyp = PackageRelationType.INNERCLASS;

   ld.addRelation(rtyp);
}



/********************************************************************************/
/*										*/
/*	Access visiting methods 					*/
/*										*/
/********************************************************************************/

@Override public void visitRemoteFieldAccess(BanalMethod bm,BanalField bf)
{
   BanalClass frm = bm.getOwnerClass();
   BanalClass to = bf.getOwnerClass();
   if (frm == to) return;
   if (!isClassRelevant(to)) return;

   LinkData ld = findLink(frm,to);
   if (ld != null) ld.addRelation(PackageRelationType.ACCESSES);
}



@Override public void visitRemoteTypeAccess(BanalMethod bm,BanalClass bc)
{
   BanalClass frm = bm.getOwnerClass();
   if (!isClassRelevant(bc)) return;
   if (frm == bc) return;

   LinkData ld = findLink(frm,bc);
   if (ld != null) ld.addRelation(PackageRelationType.FIELD);
}



@Override public void visitLocalVariable(BanalMethod bm, BanalClass bc,
      String sgn,boolean prm)
{
   BanalClass frm = bm.getOwnerClass();
   if (!isClassRelevant(bc)) return;
   if (frm == bc) return;

   LinkData ld = findLink(frm,bc);
   if (ld != null) ld.addRelation(PackageRelationType.LOCAL);
}



@Override public void visitCall(BanalMethod frm,BanalMethod cld)
{
   BanalClass fc = frm.getOwnerClass();
   BanalClass tc = cld.getOwnerClass();
   if (!isClassRelevant(tc) || tc == fc) return;

   LinkData ld = findLink(fc,tc);
   if (ld != null) ld.addRelation(PackageRelationType.CALLS);
}



@Override public void visitAlloc(BanalMethod bm,BanalClass allocd)
{
   BanalClass fc = bm.getOwnerClass();
   if (!isClassRelevant(allocd) || fc == allocd) return;

   LinkData ld = findLink(fc,allocd);
   if (ld != null) ld.addRelation(PackageRelationType.ALLOCATES);
}



@Override public void visitCatch(BanalMethod bm,BanalClass exc)
{
   BanalClass fc = bm.getOwnerClass();
   if (!isClassRelevant(exc) || fc == exc) return;

   LinkData ld = findLink(fc,exc);
   if (ld != null) ld.addRelation(PackageRelationType.CATCHES);
}




/********************************************************************************/
/*										*/
/*	Class information							*/
/*										*/
/********************************************************************************/

private class ClassData implements BanalPackageClass {

   private String class_name;
   private Map<ClassData,LinkData> out_links;
   private Map<ClassData,LinkData> in_links;
   private int class_access;
   private Set<ClassType> class_types;

   ClassData(String nm) {
      class_name = nm;
      out_links = new HashMap<ClassData,LinkData>();
      in_links = new HashMap<ClassData,LinkData>();
      class_access = -1;
      class_types = null;
    }

   @Override public String getName()		{ return class_name; }
   @Override public int getModifiers()		{ return class_access; }
   @Override public Collection<BanalPackageLink> getInLinks() {
      return new ArrayList<BanalPackageLink>(in_links.values());
    }
   @Override public Collection<BanalPackageLink> getOutLinks() {
      return new ArrayList<BanalPackageLink>(out_links.values());
    }

   @Override public Set<ClassType> getTypes() {
      if (class_types != null) return class_types;
      int mod = class_access;
      if (mod == -1) mod = Modifier.PUBLIC;
      class_types = EnumSet.noneOf(ClassType.class);
      if ((mod & Opcodes.ACC_PRIVATE) != 0) class_types.add(ClassType.PRIVATE);
      else if ((mod & Opcodes.ACC_PROTECTED) != 0) class_types.add(ClassType.PROTECTED);
      else if ((mod & Opcodes.ACC_PUBLIC) != 0) class_types.add(ClassType.PUBLIC);
      else class_types.add(ClassType.PACKAGE_PROTECTED);
      if ((mod & Opcodes.ACC_ENUM) != 0) class_types.add(ClassType.ENUM);
      else if ((mod & Opcodes.ACC_INTERFACE) != 0) class_types.add(ClassType.INTERFACE);
      else if ((mod & Opcodes.ACC_ANNOTATION) != 0) class_types.add(ClassType.ANNOTATION);
      else class_types.add(ClassType.CLASS);
      if ((mod & Opcodes.ACC_STATIC) != 0) class_types.add(ClassType.STATIC);
      if ((mod & Opcodes.ACC_ABSTRACT) != 0) class_types.add(ClassType.ABSTRACT);
      if ((mod & Opcodes.ACC_FINAL) != 0) class_types.add(ClassType.FINAL);

      String s = getName();
      int idx = s.indexOf("<");
      if (idx >= 0) s = s.substring(0,idx);
      idx = s.indexOf("$");
      if (idx >= 0) class_types.add(ClassType.INNER);
      if (isThrowable()) class_types.add(ClassType.THROWABLE);
      return class_types;
    }

   void setAccess(int acc) {
      class_access = acc;
      class_types = null;
    }
   boolean isInterface() {
      if (class_access == -1) return false;
      return Modifier.isInterface(class_access);
    }

   LinkData createLinkTo(ClassData cd) {
      if (cd == this || cd == null) return null;
      LinkData ld = out_links.get(cd);
      if (ld == null) {
	 ld = new LinkData(this,cd);
	 out_links.put(cd,ld);
	 cd.in_links.put(this,ld);
       }
      return ld;
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("CLASS");
      xw.field("NAME",class_name);
      if (class_access != -1) xw.field("MOD",class_access);
      for (LinkData ld : out_links.values()) {
	 ld.outputXml(xw);
       }
      xw.end("CLASS");
    }

   @Override public String toString() {
      return "[" + class_name + "]";
    }

   private boolean isThrowable() {
      if (class_name.equals("java.lang.Throwable")) return true;
      if (class_name.equals("java.lang.Error")) return true;
      if (class_name.equals("java.lang.Exception")) return true;
      for (LinkData ld : out_links.values()) {
	 if (ld.getTypes().containsKey(PackageRelationType.SUPERCLASS)) {
	    ClassData cd =  (ClassData) ld.getToClass();
	    return cd.isThrowable();
	  }
       }
      return false;
    }

}	// end of inner class ClassData



/********************************************************************************/
/*										*/
/*	Link information							*/
/*										*/
/********************************************************************************/

private class LinkData implements BanalPackageLink {

   private ClassData from_class;
   private ClassData to_class;
   private Map<PackageRelationType,Integer> type_count;

   LinkData(ClassData fn,ClassData tn) {
      from_class = fn;
      to_class = tn;
      type_count = new EnumMap<PackageRelationType,Integer>(PackageRelationType.class);
    }

   @Override public BanalPackageClass getFromClass()		{ return from_class; }
   @Override public BanalPackageClass getToClass()		{ return to_class; }
   @Override public Map<PackageRelationType,Integer> getTypes() { return type_count; }

   void addRelation(PackageRelationType rt) {
      Integer id = type_count.get(rt);
      int idv = 0;
      if (id != null) idv = id;
      type_count.put(rt,idv+1);
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("LINK");
      xw.field("FROM",from_class.getName());
      xw.field("TO",to_class.getName());
      for (Map.Entry<PackageRelationType,Integer> ent : type_count.entrySet()) {
	 PackageRelationType rt = ent.getKey();
	 int ct = ent.getValue();
	 xw.field(rt.toString(),ct);
       }
      xw.end("LINK");
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<");
      buf.append(from_class.getName());
      buf.append("--(");
      int ctr = 0;
      for (Map.Entry<PackageRelationType,Integer> ent : type_count.entrySet()) {
	 if (ent.getValue() > 0) {
	    if (ctr++ > 0) buf.append(",");
	    buf.append(ent.getKey());
	  }
       }
      buf.append(")-->");
      buf.append(to_class.getName());
      buf.append(">");
      return buf.toString();
    }

}	// end of inner class LinkData




}	// end of class BanalPackageGraph




/* end of BanalPackageGraph.java */
