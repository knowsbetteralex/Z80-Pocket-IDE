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
    private static final Pattern EQU=Pattern.compile(
            "^("+IDENT+")\\s*:?\\s+EQU\\s+(.+)$",Pattern.CASE_INSENSITIVE);
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
        List<String> visible=new ArrayList<>();
        List<SourceIncludes.Location> origins=new ArrayList<>();
        for(int i=0;i<lines.length;i++){
            if(!definitions[i]){
                visible.add(lines[i]);
                origins.add(input.location(i+1));
            }
        }
        processBlock(visible,origins,macros,stack,serial,0,out,positions,chars,
                new HashMap<String,Integer>());
        return new SourceIncludes.Expanded(
                String.join("\n",out),input.root,input.rootLines,positions);
    }


    /** State is per expanded block: a macro's IF cannot consume its caller's ENDIF. */
    private static final class Condition {
        final boolean parent;
        boolean active, taken, elseSeen;
        Condition(boolean parent,boolean selected) {
            this.parent=parent;
            this.active=parent && selected;
            this.taken=this.active;
        }
    }

    private static void processBlock(List<String> lines,
                                     List<SourceIncludes.Location> from,
                                     Map<String,Definition> macros,Set<String> stack,
                                     int[] serial,int depth,List<String> out,
                                     List<SourceIncludes.Location> locations,
                                     int[] chars,Map<String,Integer> symbols) {
        Deque<Condition> conditions=new ArrayDeque<>();
        for(int i=0;i<lines.size();i++){
            String line=lines.get(i);
            SourceIncludes.Location at=from.get(i);
            String code=stripComment(line).trim();
            int space=0;
            while(space<code.length()&&!Character.isWhitespace(code.charAt(space)))space++;
            String word=code.substring(0,space).toUpperCase(Locale.ROOT);
            if(word.startsWith("."))word=word.substring(1);
            String args=code.substring(space).trim();
            boolean active=conditions.isEmpty() || conditions.peek().active;
            if(word.equals("IF") || word.equals("IFDEF") || word.equals("IFNDEF")){
                if(args.isEmpty())throw error(at,word+" requires a condition");
                boolean matches=active && condition(word,args,symbols,at);
                conditions.push(new Condition(active,matches));
                continue;
            }
            if(word.equals("ELSEIF") || word.equals("ELIF")){
                if(conditions.isEmpty())throw error(at,word+" without IF");
                Condition state=conditions.peek();
                if(state.elseSeen)throw error(at,word+" after ELSE");
                if(args.isEmpty())throw error(at,word+" requires an expression");
                boolean matches=state.parent && !state.taken
                        && condition("IF",args,symbols,at);
                state.active=matches;
                state.taken|=matches;
                continue;
            }
            if(word.equals("ELSE")){
                if(conditions.isEmpty())throw error(at,"ELSE without IF");
                if(!args.isEmpty())throw error(at,"ELSE cannot have arguments");
                Condition state=conditions.peek();
                if(state.elseSeen)throw error(at,"duplicate ELSE");
                state.elseSeen=true;
                state.active=state.parent && !state.taken;
                state.taken|=state.active;
                continue;
            }
            if(word.equals("ENDIF")){
                if(conditions.isEmpty())throw error(at,"ENDIF without IF");
                if(!args.isEmpty())throw error(at,"ENDIF cannot have arguments");
                conditions.pop();
                continue;
            }
            if(active)emit(line,at,macros,stack,serial,depth,out,locations,chars,symbols);
        }
        if(!conditions.isEmpty() && !from.isEmpty())
            throw error(from.get(from.size()-1),"missing ENDIF");
    }

    private static boolean condition(String op,String args,
                                     Map<String,Integer> symbols,SourceIncludes.Location at) {
        if(op.equals("IFDEF") || op.equals("IFNDEF")){
            if(!args.matches(IDENT))throw error(at,op+" needs a symbol name");
            boolean exists=symbols.containsKey(key(args));
            return op.equals("IFDEF")?exists:!exists;
        }
        try {
            int depth=0;
            for(int i=0;i<args.length();i++){
                char c=args.charAt(i);
                if(c=='('){depth++;continue;}
                if(c==')'){depth--;continue;}
                if(depth!=0)continue;
                String comparison=null;
                for(String candidate:new String[]{"==","!=",">=","<=","<>","=","<",">"}){
                    if(!args.startsWith(candidate,i))continue;
                    if((candidate.equals("<")||candidate.equals(">"))
                            && ((i+1<args.length()&&args.charAt(i+1)==c)
                                || (i>0&&args.charAt(i-1)==c)))continue;
                    comparison=candidate;
                    break;
                }
                if(comparison==null)continue;
                int left=ExpressionEvaluator.eval(args.substring(0,i),symbols);
                int right=ExpressionEvaluator.eval(
                        args.substring(i+comparison.length()),symbols);
                switch(comparison){
                    case "=":case "==":return left==right;
                    case "!=":case "<>":return left!=right;
                    case ">":return left>right;
                    case "<":return left<right;
                    case ">=":return left>=right;
                    case "<=":return left<=right;
                    default:break;
                }
            }
            return ExpressionEvaluator.eval(args,symbols)!=0;
        }catch(IllegalArgumentException problem) {
            throw error(at,"IF condition '"+args+"': "+problem.getMessage());
        }
    }

    /** Only already-resolved EQU constants affect preprocessing IF expressions. */
    private static void rememberEqu(String line, Map<String,Integer> symbols) {
        Matcher match=EQU.matcher(stripComment(line).trim());
        if(!match.matches())return;
        try {
            symbols.put(key(match.group(1)),
                    ExpressionEvaluator.eval(match.group(2),symbols));
        }catch(IllegalArgumentException ignored) {
            // The assembler's second pass still validates unresolved EQU.
        }
    }

    private static void emit(String line,SourceIncludes.Location at,
                             Map<String,Definition> macros,Set<String> stack,
                             int[] serial,int depth,List<String> out,
                             List<SourceIncludes.Location> locations,int[] chars,
                             Map<String,Integer> symbols){
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
            rememberEqu(line,symbols);
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
            List<String> expandedLines=new ArrayList<>();
            List<SourceIncludes.Location> mappedLines=new ArrayList<>();
            for(int j=0;j<macro.body.size();j++){
                expandedLines.add(substitute(macro.body.get(j),replace,id));
                SourceIncludes.Location origin=macro.positions.get(j);
                mappedLines.add(new SourceIncludes.Location(
                        origin.path,origin.line,at.rootLine));
            }
            processBlock(expandedLines,mappedLines,macros,stack,serial,depth+1,
                    out,locations,chars,symbols);
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
