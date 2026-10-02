package com.alexzab.z80pocketide.assembler;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Two-pass Z80 assembler core independent of Android. */
public final class Assembler {
    public AssemblyResult assemble(String source) {
        List<ParsedLine> lines = parseSource(source);
        Map<String,Integer> symbols = new HashMap<>();
        Map<String,String> pendingEqu = new LinkedHashMap<>();
        int pc=0, origin=0;
        boolean originSeen=false;

        for (ParsedLine line : lines) {
            if (line.equName != null) {
                defineEqu(line, symbols, pendingEqu);
                continue;
            }
            if (line.label != null) define(symbols,line.label,pc,line.number);
            if (line.code.isEmpty()) continue;
            Head h=Head.of(line.code);
            try {
                switch (h.op) {
                    case "ORG": {
                        int v=evalRequired(h.tail,symbols,line.number);
                        if (!originSeen) { origin=v; originSeen=true; }
                        pc=v;
                        break;
                    }
                    case "DB": pc += dataBytes(h.tail,line.number).size; break;
                    case "DW": pc += splitArgs(h.tail).length*2; break;
                    case "DS": pc += dsCount(h.tail,symbols,line.number); break;
                    default: pc += Z80Encoder.size(line.code); break;
                }
            } catch (IllegalArgumentException ex) { throw wrap(line.number,ex); }
        }
        resolvePendingEqu(symbols,pendingEqu);

        ByteArrayOutputStream out=new ByteArrayOutputStream();
        pc=origin;
        boolean started=false;
        for (ParsedLine line : lines) {
            if (line.equName != null || line.code.isEmpty()) continue;
            Head h=Head.of(line.code);
            try {
                if (h.op.equals("ORG")) {
                    int next=evalRequired(h.tail,symbols,line.number);
                    if (!started) { pc=next; started=true; continue; }
                    if (next < pc) throw new IllegalArgumentException("backward ORG is not supported in a flat binary");
                    while (pc<next) { out.write(0); pc++; }
                    continue;
                }
                started=true;
                byte[] encoded;
                switch (h.op) {
                    case "DB": encoded=encodeDb(h.tail,symbols,line.number); break;
                    case "DW": encoded=encodeDw(h.tail,symbols,line.number); break;
                    case "DS": encoded=encodeDs(h.tail,symbols,line.number); break;
                    default: encoded=Z80Encoder.encode(line.code,pc,symbols); break;
                }
                out.write(encoded,0,encoded.length);
                pc+=encoded.length;
            } catch (IllegalArgumentException ex) { throw wrap(line.number,ex); }
        }
        return new AssemblyResult(originSeen?origin:0,out.toByteArray());
    }

    private static void defineEqu(ParsedLine line, Map<String,Integer> symbols, Map<String,String> pending) {
        String name=key(line.equName);
        if (symbols.containsKey(name)||pending.containsKey(name)) throw error(line.number,"duplicate symbol "+line.equName);
        try { symbols.put(name,ExpressionEvaluator.eval(line.equExpr,symbols)); }
        catch (ExpressionEvaluator.UnknownSymbolException ex) { pending.put(name,line.equExpr); }
        catch (IllegalArgumentException ex) { throw wrap(line.number,ex); }
    }

    private static void resolvePendingEqu(Map<String,Integer> symbols, Map<String,String> pending) {
        boolean progress=true;
        while (!pending.isEmpty() && progress) {
            progress=false;
            java.util.Iterator<Map.Entry<String,String>> it=pending.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String,String> e=it.next();
                try { symbols.put(e.getKey(),ExpressionEvaluator.eval(e.getValue(),symbols)); it.remove(); progress=true; }
                catch (ExpressionEvaluator.UnknownSymbolException ignored) { }
            }
        }
        if (!pending.isEmpty()) throw new IllegalArgumentException("unresolved EQU symbol(s): "+String.join(", ",pending.keySet()));
    }

    private static int evalRequired(String s, Map<String,Integer> symbols, int line) {
        try { return ExpressionEvaluator.eval(s,symbols); }
        catch (IllegalArgumentException ex) { throw wrap(line,ex); }
    }

    private static void define(Map<String,Integer> symbols,String name,int value,int line) {
        String k=key(name);
        if (symbols.put(k,value)!=null) throw error(line,"duplicate label "+name);
    }

    private static byte[] encodeDb(String tail, Map<String,Integer> symbols, int line) {
        List<DataItem> items=parseDataItems(tail,line);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for (DataItem item:items) {
            if (item.stringValue!=null) {
                byte[] bytes=item.stringValue.getBytes(StandardCharsets.ISO_8859_1);
                out.write(bytes,0,bytes.length);
            } else {
                int v=evalRequired(item.expression,symbols,line);
                if (v < -128 || v > 255) throw error(line,"DB value out of byte range: "+v);
                out.write(v & 0xFF);
            }
        }
        return out.toByteArray();
    }

    private static byte[] encodeDw(String tail, Map<String,Integer> symbols, int line) {
        String[] a=splitArgs(tail); ByteArrayOutputStream out=new ByteArrayOutputStream();
        for (String s:a) {
            int v=evalRequired(s,symbols,line);
            if (v < -32768 || v > 0xFFFF) throw error(line,"DW value out of word range: "+v);
            out.write(v&0xFF); out.write((v>>>8)&0xFF);
        }
        return out.toByteArray();
    }

    private static int dsCount(String tail, Map<String,Integer> symbols, int line) {
        String[] a=splitArgs(tail);
        if (a.length<1||a.length>2) throw error(line,"DS expects count[,fill]");
        int n=evalRequired(a[0],symbols,line);
        if (n<0) throw error(line,"DS count cannot be negative");
        return n;
    }

    private static byte[] encodeDs(String tail, Map<String,Integer> symbols, int line) {
        String[] a=splitArgs(tail); int n=dsCount(tail,symbols,line); int fill=0;
        if (a.length==2) fill=evalRequired(a[1],symbols,line);
        byte[] b=new byte[n]; java.util.Arrays.fill(b,(byte)fill); return b;
    }

    private static DataSize dataBytes(String tail,int line) {
        int n=0;
        for (DataItem i:parseDataItems(tail,line)) n += i.stringValue!=null ? i.stringValue.getBytes(StandardCharsets.ISO_8859_1).length : 1;
        return new DataSize(n);
    }

    private static List<DataItem> parseDataItems(String s,int line) {
        List<String> raw=splitArgsAware(s); List<DataItem> out=new ArrayList<>();
        for (String x:raw) {
            String t=x.trim();
            if (t.length()>=2 && ((t.startsWith("\"")&&t.endsWith("\""))||(t.startsWith("'")&&t.endsWith("'")))) {
                out.add(new DataItem(unescape(t.substring(1,t.length()-1)),null));
            } else if (!t.isEmpty()) out.add(new DataItem(null,t));
            else throw error(line,"empty DB item");
        }
        return out;
    }

    private static String unescape(String s) {
        StringBuilder b=new StringBuilder(); boolean esc=false;
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (esc) { if (c=='n') b.append('\n'); else if (c=='r') b.append('\r'); else if (c=='t') b.append('\t'); else b.append(c); esc=false; }
            else if (c=='\\') esc=true; else b.append(c);
        }
        if (esc) b.append('\\');
        return b.toString();
    }

    private static String[] splitArgs(String s) { return splitArgsAware(s).toArray(new String[0]); }
    private static List<String> splitArgsAware(String s) {
        List<String> out=new ArrayList<>(); int start=0,depth=0; char quote=0; boolean esc=false;
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (quote!=0) { if (esc) esc=false; else if (c=='\\') esc=true; else if (c==quote) quote=0; continue; }
            if (c=='\''||c=='\"') quote=c; else if (c=='(') depth++; else if (c==')') depth--;
            else if (c==','&&depth==0) { out.add(s.substring(start,i)); start=i+1; }
        }
        out.add(s.substring(start)); return out;
    }

    private static List<ParsedLine> parseSource(String source) {
        String[] raw=source.replace("\r","").split("\n",-1); List<ParsedLine> out=new ArrayList<>();
        for (int i=0;i<raw.length;i++) out.add(parseLine(raw[i],i+1));
        return out;
    }

    private static ParsedLine parseLine(String raw,int number) {
        String s=stripComment(raw).trim();
        if (s.isEmpty()) return new ParsedLine(number,null,"",null,null);
        String[] p=s.split("\\s+",3);
        if (p.length>=3 && p[1].equalsIgnoreCase("EQU")) return new ParsedLine(number,null,"",p[0],p[2]);
        int colon=s.indexOf(':');
        if (colon>=0) {
            String label=s.substring(0,colon).trim(); String code=s.substring(colon+1).trim();
            if (!validIdent(label)) throw error(number,"invalid label: "+label);
            Head h=code.isEmpty()?null:Head.of(code);
            if (h!=null&&h.op.equals("EQU")) return new ParsedLine(number,null,"",label,h.tail);
            return new ParsedLine(number,label,code,null,null);
        }
        return new ParsedLine(number,null,s,null,null);
    }

    private static String stripComment(String s) {
        char quote=0; boolean esc=false;
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if (quote!=0) { if (esc) esc=false; else if (c=='\\') esc=true; else if (c==quote) quote=0; }
            else if (c=='\''||c=='\"') quote=c; else if (c==';') return s.substring(0,i);
        }
        return s;
    }
    private static boolean validIdent(String s) { return s.matches("[A-Za-z_@.][A-Za-z0-9_@.$]*"); }
    private static String key(String s) { return s.toUpperCase(Locale.ROOT); }
    private static IllegalArgumentException error(int line,String message) { return new IllegalArgumentException("line "+line+": "+message); }
    private static IllegalArgumentException wrap(int line,IllegalArgumentException ex) {
        if (ex.getMessage()!=null&&ex.getMessage().startsWith("line ")) return ex;
        return error(line,ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage());
    }

    private static final class Head {
        final String op,tail;
        Head(String op,String tail){this.op=op;this.tail=tail;}
        static Head of(String code){String t=code.trim();int i=0;while(i<t.length()&&!Character.isWhitespace(t.charAt(i)))i++;return new Head(t.substring(0,i).toUpperCase(Locale.ROOT),t.substring(i).trim());}
    }
    private static final class ParsedLine {
        final int number; final String label,code,equName,equExpr;
        ParsedLine(int number,String label,String code,String equName,String equExpr){this.number=number;this.label=label;this.code=code;this.equName=equName;this.equExpr=equExpr;}
    }
    private static final class DataItem { final String stringValue,expression; DataItem(String s,String e){stringValue=s;expression=e;} }
    private static final class DataSize { final int size; DataSize(int size){this.size=size;} }
}
