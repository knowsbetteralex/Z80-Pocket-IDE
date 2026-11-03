package com.alexzab.z80pocketide.project;

import com.alexzab.z80pocketide.assembler.Assembler;
import java.util.Locale;
import java.util.regex.Pattern;

/** Strict data-only assembler interchange for complete 6912-byte screens. */
public final class ScreenAsmCodec {
    private static final String NAME="[A-Za-z_@.][A-Za-z0-9_@.$]*";
    private static final Pattern LABEL=Pattern.compile("(?i)^"+NAME+":(?:\\s+.*)?$");
    private static final Pattern EQU=Pattern.compile("(?i)^"+NAME+"\\s+EQU\\s+.+$");
    private ScreenAsmCodec() {}

    public static byte[] importScreen(String source) {
        if(source==null)throw new IllegalArgumentException("Empty ASM screen");
        String[] lines=source.replace("\r","").split("\n",-1);
        StringBuilder normalized=new StringBuilder(source.length());
        for(int i=0;i<lines.length;i++) {
            String code=stripComment(lines[i]).trim();
            if(code.isEmpty()) { normalized.append('\n');continue; }
            if(EQU.matcher(code).matches()) {
                normalized.append(code).append('\n');continue;
            }
            String label="";
            int colon=code.indexOf(':');
            if(colon>0 && code.substring(0,colon).trim().matches(NAME)){
                label=code.substring(0,colon+1);
                code=code.substring(colon+1).trim();
            }
            if(code.isEmpty()){normalized.append(label).append('\n');continue;}
            int space=0;
            while(space<code.length()&&!Character.isWhitespace(code.charAt(space)))space++;
            String op=code.substring(0,space).toUpperCase(Locale.ROOT);
            if(!op.equals("DB")&&!op.equals("DEFB"))
                throw new IllegalArgumentException("line "+(i+1)+
                        ": only DB/DEFB, labels and EQU are allowed in screen ASM (got "+op+")");
            String operands=code.substring(space).trim();
            if(operands.isEmpty())
                throw new IllegalArgumentException("line "+(i+1)+": missing DB data");
            normalized.append(label);
            if(!label.isEmpty())normalized.append(' ');
            normalized.append("DB ").append(operands).append('\n');
        }
        byte[] bytes=new Assembler().assemble(normalized.toString()).getBytes();
        if(bytes.length!=SpectrumScreen.LENGTH)
            throw new IllegalArgumentException("SCREEN$ ASM must assemble to 6912 bytes, got "+bytes.length);
        return bytes;
    }

    public static String exportScreen(byte[] bytes) {
        if(bytes==null||bytes.length!=SpectrumScreen.LENGTH)
            throw new IllegalArgumentException("Expected exactly 6912 bytes");
        StringBuilder out=new StringBuilder(46000);
        out.append("; ZX Spectrum SCREEN$ 256x192 (6912 bytes)\n");
        append(out,"SCREEN_BITMAP:",bytes,0,6144);
        out.append('\n');
        append(out,"SCREEN_ATTRS:",bytes,6144,6912);
        return out.toString();
    }
    private static void append(StringBuilder out,String label,byte[] bytes,int from,int to){
        out.append(label).append('\n');
        char[] hex="0123456789ABCDEF".toCharArray();
        for(int i=from;i<to;i++){
            if((i-from)%16==0)out.append("    DB ");
            else out.append(',');
            int value=bytes[i]&255;
            out.append('$').append(hex[value>>4]).append(hex[value&15]);
            if((i-from)%16==15)out.append('\n');
        }
    }

    private static String stripComment(String line) {
        char quote=0;
        boolean escape=false;
        for(int i=0;i<line.length();i++){
            char c=line.charAt(i);
            if(quote!=0) {
                if(escape)escape=false;
                else if(c=='\\')escape=true;
                else if(c==quote)quote=0;
            } else if(c=='"' || c=='\'')quote=c;
            else if(c==';')return line.substring(0,i);
        }
        return line;
    }
}
