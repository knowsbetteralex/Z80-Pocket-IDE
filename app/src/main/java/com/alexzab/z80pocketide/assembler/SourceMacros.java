package com.alexzab.z80pocketide.assembler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded, two-stage MACRO/ENDM preprocessor. Runs after INCLUDE expansion
 * and before the assembler's two passes (including local-dot-label resolution).
 * Macro definitions may be placed after calls or in any INCLUDED source.
 */
public final class SourceMacros {
    private static final String IDENT="[A-Za-z_@.][A-Za-z0-9_@.$]*";
    private static final Pattern NAMED=Pattern.compile(
            "^("+IDENT+")\\s+MACRO(?:\\s+(.+))?$",Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIX=Pattern.compile(
            "^\\.?MACRO\\s+("+IDENT+")(?:\\s*,\\s*|\\s+)(.+)?$",Pattern.CASE_INSENSITIVE);
    private static final Pattern END=Pattern.compile("^\\.?ENDM(?:ACRO)?$",Pattern.CASE_INSENSITIVE);
    private static final int MAX_DEPTH=24, MAX_LINES=100000, MAX_CHARACTERS=4*1024*1024;
    private SourceMacros() {}

    public static final class MacroError extends IllegalArgumentException {
        public final String path;
        public final int line;
        MacroError(SourceIncludes.Location at,String message) {
            super(at.path+":"+at.line+": "+message);
            path=at.path;
            line=at.line;
        }
    }

    private static final class Definition {
        final String name;
        final SourceIncludes.Location declaration;
        final List<String> params;
        final List<String> body=new ArrayList<>();
        final List<SourceIncludes.Location> positions=new ArrayList<>();
        Definition(String name,List<String> params,SourceIncludes.Location at) {
            this.name=name;
            this.params=params;
            this.declaration=at;
        }
    }
    private static final class Directive {
        final String name,args;
        Directive(String name,String args){this.name=name;this.args=args==null?"":args;}
    }

    public static SourceIncludes.Expanded expand(SourceIncludes.Expanded input) {
        String[] lines=input.source.replace("\r","").split("\n",-1);
        boolean[] definitions=new boolean[lines.length];
        Map<String,Definition> macros=new LinkedHashMap<>();
        Definition current=null;
        for(int i=0;i<lines.length;i++){
            SourceIncludes.Location at=input.location(i+1);
            String code=stripComment(lines[i]).trim();
            Directive d=definition(code);
            if(current!=null){
                definitions[i]=true;
                if(END.matcher(code).matches()) {
                    current=null;
                } else if(d!=null){
                    throw error(at,"nested MACRO definitions are not supported");
                } else {
                    current.body.add(lines[i]);
                    current.positions.add(at);
                }
                continue;
            }
            if(END.matcher(code).matches())throw error(at,"ENDM without MACRO");
            if(d==null)continue;
            definitions[i]=true;
            if(!d.name.matches(IDENT))throw error(at,"invalid macro name: "+d.name);
            String name=key(d.name);
            if(macros.containsKey(name))throw error(at,"duplicate MACRO: "+d.name);
            List<String> params=new ArrayList<>();
            if(!d.args.trim().isEmpty()){
                Set<String> seen=new HashSet<>();
                for(String value:splitArgs(d.args)){
                    String param=value.trim();
                    if(!param.matches(IDENT) || !seen.add(key(param)))
                        throw error(at,"invalid/duplicate MACRO parameter: "+param);
                    params.add(param);
                }
            }
            current=new Definition(d.name,params,at);
            macros.put(name,current);
        }
        if(current!=null)throw error(current.declaration,
                "MACRO "+current.name+" has no ENDM");

        List<String> out=new ArrayList<>();
        List<SourceIncludes.Location> positions=new ArrayList<>();
        int[] serial={0};
        Set<String> stack=new HashSet<>();
        int[] chars={0};
        for(int i=0;i<lines.length;i++){
            if(!definitions[i])
                emit(lines[i],input.location(i+1),macros,stack,serial,0,out,positions,chars);
        }
        return new SourceIncludes.Expanded(
                String.join("\n",out),input.root,input.rootLines,positions);
    }

    private static void emit(String line,SourceIncludes.Location at,
                             Map<String,Definition> macros,Set<String> stack,
                             int[] serial,int depth,List<String> out,
                             List<SourceIncludes.Location> locations,int[] chars){
        String code=stripComment(line).trim();
        if(code.isEmpty()){
            append(line,at,out,locations,chars);
            return;
        }
        // An invocation may carry a normal label: "NEXT: DRAW A,B".
        String label=null;
        int colon=code.indexOf(':');
        if(colon>0 && code.substring(0,colon).trim().matches(IDENT)){
            label=code.substring(0,colon).trim();
            code=code.substring(colon+1).trim();
        }
        int whitespace=0;
        while(whitespace<code.length()&&!Character.isWhitespace(code.charAt(whitespace)))
            whitespace++;
        String name=code.substring(0,whitespace);
        Definition macro=macros.get(key(name));
        if(macro==null){
            append(line,at,out,locations,chars);
            return;
        }
        if(label!=null)append(label+":",at,out,locations,chars);
        if(depth>=MAX_DEPTH || !stack.add(key(name)))
            throw error(at,"recursive MACRO invocation: "+name);
        try{
            String arguments=code.substring(whitespace).trim();
            List<String> values=arguments.isEmpty()
                    ? Collections.emptyList():splitArgs(arguments);
            if(values.size()!=macro.params.size())
                throw error(at,"MACRO "+name+" expects "+macro.params.size()
                        +" argument(s), got "+values.size());
            Map<String,String> replace=new HashMap<>();
            for(int j=0;j<values.size();j++){
                replace.put(key(macro.params.get(j)),values.get(j).trim());
                replace.put(String.valueOf(j+1),values.get(j).trim());
            }
            int id=++serial[0];
            for(int j=0;j<macro.body.size();j++){
                String expanded=substitute(macro.body.get(j),replace,id);
                // Attribute emitted bytes to call site, but errors to the macro's
                // actual definition file and line.
                SourceIncludes.Location origin=macro.positions.get(j);
                SourceIncludes.Location mapped=new SourceIncludes.Location(
                        origin.path,origin.line,at.rootLine);
                emit(expanded,mapped,macros,stack,serial,depth+1,
                        out,locations,chars);
            }
        }finally {
            stack.remove(key(name));
        }
    }

    private static void append(String value,SourceIncludes.Location at,
                               List<String> out,List<SourceIncludes.Location> locations,
                               int[] chars) {
        if(out.size()>=MAX_LINES || (chars[0]+=value.length()+1)>MAX_CHARACTERS)
            throw error(at,"MACRO expansion too large");
        out.add(value);
        locations.add(at);
    }

    /**
     * Parameters substitute as whole identifier tokens or \name / \1.
     * @@labels become invocation-unique global names. Do not touch strings,
     * comments, or quoted text.
     */
    private static String substitute(String source,Map<String,String> params,int serial){
        StringBuilder result=new StringBuilder();
        char quote=0;boolean escape=false;
        for(int i=0;i<source.length();){
            char c=source.charAt(i);
            if(quote!=0){
                result.append(c);
                if(escape)escape=false;
                else if(c=='\\')escape=true;
                else if(c==quote)quote=0;
                i++;continue;
            }
            if(c==';'){result.append(source.substring(i));break;}
            if(c=='"' || c=='\''){
                result.append(c);quote=c;i++;continue;
            }
            if(c=='@' && i+2<source.length() && source.charAt(i+1)=='@'
                    && isStart(source.charAt(i+2))){
                int end=i+3;
                while(end<source.length() && isPart(source.charAt(end)))end++;
                result.append("__MACRO_").append(serial).append('_')
                        .append(source,i+2,end);
                i=end;continue;
            }
            if(c=='\\' && i+1<source.length()
                    && (isStart(source.charAt(i+1)) || Character.isDigit(source.charAt(i+1)))){
                int end=i+2;
                while(end<source.length() && isPart(source.charAt(end)))end++;
                String original=source.substring(i+1,end);
                String replacement=params.get(key(original));
                if(replacement!=null){result.append(replacement);i=end;continue;}
            }
            if(isStart(c) && (i==0 || !isPart(source.charAt(i-1)))){
                int end=i+1;
                while(end<source.length() && isPart(source.charAt(end)))end++;
                String token=source.substring(i,end);
                String replacement=params.get(key(token));
                result.append(replacement==null?token:replacement);
                i=end;continue;
            }
            result.append(c);i++;
        }
        return result.toString();
    }

    private static boolean isStart(char c){
        return Character.isLetter(c) || c=='_' || c=='.' || c=='@';
    }
    private static boolean isPart(char c){
        return Character.isLetterOrDigit(c)
                || c=='_' || c=='.' || c=='@' || c=='$';
    }
    private static String key(String s){return s.toUpperCase(Locale.ROOT);}
    private static IllegalArgumentException error(SourceIncludes.Location at,String message){
        return new MacroError(at,message);
    }
    private static Directive definition(String code){
        Matcher prefix=PREFIX.matcher(code);
        if(prefix.matches())return new Directive(prefix.group(1),prefix.group(2));
        Matcher named=NAMED.matcher(code);
        if(named.matches())return new Directive(named.group(1),named.group(2));
        return null;
    }
    private static String stripComment(String src){
        char quote=0;boolean escape=false;
        for(int i=0;i<src.length();i++){
            char c=src.charAt(i);
            if(quote!=0){
                if(escape)escape=false;
                else if(c=='\\')escape=true;
                else if(c==quote)quote=0;
            }else if(c=='"' || c=='\''){quote=c;}
            else if(c==';')return src.substring(0,i);
        }
        return src;
    }
    private static List<String> splitArgs(String raw){
        List<String> values=new ArrayList<>();
        int start=0,depth=0;char quote=0;boolean escape=false;
        for(int i=0;i<raw.length();i++){
            char c=raw.charAt(i);
            if(quote!=0){
                if(escape)escape=false;
                else if(c=='\\')escape=true;
                else if(c==quote)quote=0;
            }else if(c=='"' || c=='\'')quote=c;
            else if(c=='(' || c=='[')depth++;
            else if(c==')' || c==']')depth--;
            else if(c==','&&depth==0){
                values.add(raw.substring(start,i).trim());
                start=i+1;
            }
        }
        values.add(raw.substring(start).trim());
        return values;
    }
}
