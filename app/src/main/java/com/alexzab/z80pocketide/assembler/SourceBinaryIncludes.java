package com.alexzab.z80pocketide.assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves binary INCBIN directives after INCLUDE and macro/IF expansion.
 * Kept separate from the Z80 assembler so its two-pass source offsets,
 * line timings, error navigation and forward labels keep working unchanged.
 */
public final class SourceBinaryIncludes {
    private static final int MAX_EMITTED_BYTES = 131072;
    private static final int BYTES_PER_LINE = 24;
    private static final Pattern IDENT = Pattern.compile("[A-Za-z_@.][A-Za-z0-9_@.$]*");
    private static final Pattern EQU = Pattern.compile(
            "(?i)^([A-Za-z_@.][A-Za-z0-9_@.$]*)\\s*:?\\s+EQU\\s+(.+)$");
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private SourceBinaryIncludes() {}

    /** Receives a project-relative, normalized and traversal-safe path. */
    public interface Loader { byte[] load(String path) throws Exception; }

    public static boolean containsIncbin(String source) {
        if (source == null) return false;
        for (String line : source.replace("\r", "").split("\n", -1))
            if (directive(stripComment(line)) != null) return true;
        return false;
    }

    public static SourceIncludes.Expanded expand(SourceIncludes.Expanded source, Loader loader) {
        String[] lines = source.source.split("\n", -1);
        List<String> output = new ArrayList<>();
        List<SourceIncludes.Location> locations = new ArrayList<>();
        Map<String, Integer> constants = new HashMap<>();
        int written = 0;

        for (int i=0; i<lines.length; i++) {
            String line=lines[i];
            SourceIncludes.Location at=source.location(i+1);
            String code=stripComment(line).trim();
            ParsedDirective directive=directive(code);
            if (directive==null) {
                output.add(line);
                locations.add(at);
                Matcher equ=EQU.matcher(code);
                if(equ.matches()) {
                    try {
                        constants.put(equ.group(1).toUpperCase(Locale.ROOT),
                                ExpressionEvaluator.eval(equ.group(2),constants));
                    } catch (IllegalArgumentException ignored) {
                        // Normal assembler validates unresolved/malformed EQU.
                    }
                }
                continue;
            }
            try {
                List<String> args=splitArgs(directive.args);
                if(args.size()<1 || args.size()>3)
                    throw new IllegalArgumentException("INCBIN expects file[, offset[, length]]");
                String name=parseName(args.get(0));
                String path=SourceIncludes.resolve(at.path,name);
                byte[] data=loader.load(path);
                if(data==null)
                    throw new IllegalArgumentException("missing "+path+" (choose project folder)");
                int offset=args.size()>=2?value(args.get(1),constants,"offset"):0;
                int count=args.size()==3?value(args.get(2),constants,"length"):
                        data.length-offset;
                if(offset<0 || offset>data.length)
                    throw new IllegalArgumentException("INCBIN offset outside file: "+offset+" / "+data.length);
                if(count<0 || count>data.length-offset)
                    throw new IllegalArgumentException("INCBIN length outside file: "+count);
                if(written+count>MAX_EMITTED_BYTES)
                    throw new IllegalArgumentException("INCBIN total binary data exceeds "
                            +MAX_EMITTED_BYTES+" bytes");
                written+=count;
                if(count==0) {
                    output.add(directive.label.isEmpty() ? "; empty INCBIN" : directive.label+":");
                    locations.add(at);
                } else {
                    for(int from=offset;from<offset+count;from+=BYTES_PER_LINE){
                        int end=Math.min(offset+count,from+BYTES_PER_LINE);
                        StringBuilder db=new StringBuilder(BYTES_PER_LINE*4+16);
                        if(from==offset && !directive.label.isEmpty())
                            db.append(directive.label).append(": ");
                        db.append("DB ");
                        for(int j=from;j<end;j++){
                            if(j>from)db.append(',');
                            int x=data[j]&255;
                            db.append('$').append(HEX[x>>>4]).append(HEX[x&15]);
                        }
                        output.add(db.toString());
                        locations.add(at);
                    }
                }
            } catch (Exception ex) {
                throw new SourceIncludes.IncludeError(at.path,at.line,
                        "INCBIN: "+(ex.getMessage()==null?ex.toString():ex.getMessage()));
            }
        }
        return new SourceIncludes.Expanded(String.join("\n",output),source.root,
                source.rootLines,locations);
    }

    private static int value(String expression,Map<String,Integer> constants,String kind) {
        if(expression.trim().isEmpty())
            throw new IllegalArgumentException("INCBIN "+kind+" is empty");
        try {return ExpressionEvaluator.eval(expression.trim(),constants);}
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid INCBIN "+kind+
                    " expression '"+expression.trim()+"': "+ex.getMessage());
        }
    }

    private static String parseName(String text) {
        String name=text.trim();
        if(name.length()>=2 && ((name.charAt(0)=='"' && name.endsWith("\""))
                || (name.charAt(0)=='\'' && name.endsWith("'"))))
            name=name.substring(1,name.length()-1);
        else if(name.isEmpty() || name.indexOf(' ')>=0 || name.indexOf('\t')>=0
                || name.indexOf('"')>=0 || name.indexOf('\'')>=0)
            throw new IllegalArgumentException("invalid INCBIN filename");
        if(name.isEmpty())
            throw new IllegalArgumentException("empty INCBIN filename");
        return name;
    }

    private static final class ParsedDirective {
        final String label,args;
        ParsedDirective(String label,String args) {this.label=label;this.args=args;}
    }

    private static ParsedDirective directive(String line) {
        String s=line.trim();
        String label="";
        int colon=s.indexOf(':');
        if(colon>0 && IDENT.matcher(s.substring(0,colon).trim()).matches()){
            label=s.substring(0,colon).trim();
            s=s.substring(colon+1).trim();
        }
        int space=0;
        while(space<s.length() && !Character.isWhitespace(s.charAt(space)))space++;
        String op=s.substring(0,space);
        if(op.startsWith("."))op=op.substring(1);
        if(!op.equalsIgnoreCase("INCBIN"))return null;
        return new ParsedDirective(label,s.substring(space).trim());
    }

    private static List<String> splitArgs(String args) {
        List<String> out=new ArrayList<>();
        if(args.trim().isEmpty())return out;
        int start=0,depth=0;
        char quote=0;
        for(int i=0;i<args.length();i++){
            char ch=args.charAt(i);
            if(quote!=0){
                if(ch==quote)quote=0;
                continue;
            }
            if(ch=='"' || ch=='\''){quote=ch;continue;}
            if(ch=='(')depth++;
            if(ch==')')depth--;
            if(ch==',' && depth==0) {
                out.add(args.substring(start,i).trim());
                start=i+1;
            }
        }
        if(quote!=0 || depth!=0)
            throw new IllegalArgumentException("INCBIN has unbalanced quotes or parentheses");
        out.add(args.substring(start).trim());
        return out;
    }

    private static String stripComment(String s) {
        char quote=0;
        for(int i=0;i<s.length();i++){
            char ch=s.charAt(i);
            if(quote!=0){
                if(ch==quote)quote=0;
            }else if(ch=='"' || ch=='\'')quote=ch;
            else if(ch==';')return s.substring(0,i);
        }
        return s;
    }
}
