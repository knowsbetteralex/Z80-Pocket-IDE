package com.alexzab.z80pocketide.assembler;
import java.util.*;
import java.util.regex.*;
/** Expands INCLUDE before assembling and maps output lines to their source. */
public final class SourceIncludes {
 private static final Pattern INC=Pattern.compile("^\\s*\\.?INCLUDE\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s;]+))\\s*(?:;.*)?$",2);
 private static final Pattern START=Pattern.compile("^\\s*\\.?INCLUDE(?:\\s|$)",2);
 private static final Pattern ERROR=Pattern.compile("^line (\\d+):\\s*(.*)$");
 private SourceIncludes(){}
 public interface Loader { String load(String path) throws Exception; }
 public static final class Location {
  public final String path; public final int line, rootLine;
  Location(String p,int l,int r){path=p;line=l;rootLine=r;}
 }
 public static final class Expanded {
  public final String source,root; public final int rootLines;
  private final List<Location> positions;
  Expanded(String s,String r,int n,List<Location> p){source=s;root=r;rootLines=n;positions=p;}
  public Location location(int line){return line>0&&line<=positions.size()?positions.get(line-1):null;}
  public String explainError(String message){
   if(message==null)return "Assembler error";
   Matcher m=ERROR.matcher(message);
   if(!m.matches())return message;
   Location l=location(Integer.parseInt(m.group(1)));
   if(l==null)return message;
   return "line "+l.rootLine+": "+(root.equals(l.path)&&l.rootLine==l.line?"":l.path+":"+l.line+": ")+m.group(2);
  }
  public AssemblyResult project(AssemblyResult result){
   int[] sizes=new int[rootLines],lo=new int[rootLines],hi=new int[rootLines];
   boolean[] inst=new boolean[rootLines];
   for(int i=0;i<positions.size();i++){
    int k=positions.get(i).rootLine-1;
    sizes[k]+=result.getLineSize(i);lo[k]+=result.getLineMinCycles(i);
    hi[k]+=result.getLineMaxCycles(i);inst[k]|=result.isInstruction(i);
   }
   return new AssemblyResult(result.getOrigin(),result.getBytes(),sizes,lo,hi,inst);
  }
 }
 public static String includePath(String line){
  Matcher m=INC.matcher(line==null?"":line);
  if(!m.matches())return null;
  for(int i=1;i<4;i++)if(m.group(i)!=null)return m.group(i);
  return null;
 }
 public static boolean containsInclude(String source){
  if(source==null)return false;
  for(String line:source.split("\n",-1))if(START.matcher(line).find())return true;
  return false;
 }
 public static String resolve(String parent,String requested){
  if(requested==null||requested.trim().isEmpty())throw new IllegalArgumentException("empty INCLUDE filename");
  String name=requested.replace('\\','/');
  if(name.startsWith("/")||name.matches("(?i)^[a-z]:.*")||name.indexOf('\0')>=0)
   throw new IllegalArgumentException("absolute INCLUDE filename");
  String base=parent==null?"":parent.replace('\\','/');
  int pos=base.lastIndexOf('/');
  String path=(pos<0?"":base.substring(0,pos+1))+name;
  List<String> parts=new ArrayList<>();
  for(String p:path.split("/")){
   if(p.isEmpty()||p.equals("."))continue;
   if(p.equals("..")){
    if(parts.isEmpty())throw new IllegalArgumentException("INCLUDE escapes project folder");
    parts.remove(parts.size()-1);
   }else parts.add(p);
  }
  if(parts.isEmpty())throw new IllegalArgumentException("empty INCLUDE filename");
  return String.join("/",parts);
 }
 public static Expanded expand(String text,String root,Loader loader){
  if(root==null||root.isEmpty())root="main.asm";
  String src=text==null?"":text.replace("\r","");
  List<String> lines=new ArrayList<>();List<Location> locations=new ArrayList<>();
  Set<String> active=new HashSet<>();active.add(root);
  append(src,root,0,0,loader,active,lines,locations);
  return new Expanded(String.join("\n",lines),root,src.split("\n",-1).length,locations);
 }
 private static void append(String src,String path,int rootLine,int depth,Loader loader,
                            Set<String> active,List<String> out,List<Location> locations){
  String[] lines=src.replace("\r","").split("\n",-1);
  for(int i=0;i<lines.length;i++){
   int fromRoot=rootLine==0?i+1:rootLine;String value=lines[i];
   String name=includePath(value);
   if(name==null&&START.matcher(value).find())
    throw new IllegalArgumentException("line "+fromRoot+": invalid INCLUDE in "+path+":"+(i+1));
   if(name==null){
    if(out.size()>100000)throw new IllegalArgumentException("line "+fromRoot+": INCLUDE too large");
    out.add(value);locations.add(new Location(path,i+1,fromRoot));
   }else{
    try{
     String child=resolve(path,name);
     if(active.contains(child))throw new IllegalArgumentException("cyclic INCLUDE: "+child);
     if(depth>=24)throw new IllegalArgumentException("INCLUDE nesting too deep");
     String childText=loader.load(child);
     if(childText==null)throw new IllegalArgumentException("missing "+child+" (choose INCLUDE folder)");
     active.add(child);
     try{append(childText,child,fromRoot,depth+1,loader,active,out,locations);}
     finally{active.remove(child);}
    }catch(Exception ex){throw new IllegalArgumentException("line "+fromRoot+": "+path+":"+(i+1)+": "+ex.getMessage(),ex);}
   }
  }
 }
}
