package com.alexzab.z80pocketide.project;

import com.alexzab.z80pocketide.assembler.Assembler;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/** Strict data-only assembler interchange for complete 6912-byte screens. */
public final class ScreenAsmCodec {
    private static final String NAME="[A-Za-z_@.][A-Za-z0-9_@.$]*";
    private static final Pattern LABEL=Pattern.compile("(?i)^"+NAME+":(?:\\s+.*)?$");
    private static final Pattern EQU=Pattern.compile("(?i)^"+NAME+"\\s+EQU\\s+.+$");
    private static final String RLE_MARKER="; Z80_POCKET_IDE_SCREEN_RLE_V1";
    private ScreenAsmCodec() {}

    /** Only a dedicated marker enables RLE decoding; ordinary DB/DEFB ASM stays raw. */
    public static boolean isRleScreen(String source) {
        if(source==null)return false;
        for(String line:source.replace("\r","").split("\n"))
            if(line.trim().equals(RLE_MARKER))return true;
        return false;
    }

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
        if(isRleScreen(source))return decodeRle(bytes);
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

    /**
     * Export an assembler-friendly, data-only RLE stream.
     * Control 00..7F: copy the next control+1 literal bytes.
     * Control 80..FF: repeat the following byte (control&7F)+2 times.
     * The decoded stream is always the whole 6912-byte SCREEN$.
     */
    public static String exportRleScreen(byte[] bytes) {
        if(bytes==null||bytes.length!=SpectrumScreen.LENGTH)
            throw new IllegalArgumentException("Expected exactly 6912 bytes");
        byte[] encoded=encodeRle(bytes);
        StringBuilder out=new StringBuilder(encoded.length*5+256);
        out.append(RLE_MARKER).append('\n');
        out.append("; ZX Spectrum SCREEN$ RLE v1, expanded: 6912 bytes at $4000\n");
        out.append("; 00..7F: copy control+1 bytes; 80..FF: repeat next byte (control&127)+2 times\n");
        out.append("; Compact data stream only; decompress before displaying the screen.\n");
        append(out,"SCREEN_RLE:",encoded,0,encoded.length);
        return out.toString();
    }

    static byte[] encodeRle(byte[] bytes) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int i=0;i<bytes.length;) {
            int run=runLength(bytes,i);
            if(run>=3) {
                out.write(0x80|(run-2));
                out.write(bytes[i]&255);
                i+=run;
            } else {
                int start=i;
                i+=run;
                while(i<bytes.length && i-start<128) {
                    run=runLength(bytes,i);
                    if(run>=3)break;
                    i+=Math.min(run,128-(i-start));
                }
                int count=i-start;
                out.write(count-1);
                out.write(bytes,start,count);
            }
        }
        return out.toByteArray();
    }

    private static int runLength(byte[] bytes,int pos) {
        int run=1;
        while(run<129 && pos+run<bytes.length && bytes[pos+run]==bytes[pos])run++;
        return run;
    }

    static byte[] decodeRle(byte[] encoded) {
        byte[] result=new byte[SpectrumScreen.LENGTH];
        int input=0,output=0;
        while(input<encoded.length) {
            int control=encoded[input++]&255;
            if((control&128)!=0) {
                if(input>=encoded.length)
                    throw new IllegalArgumentException("SCREEN$ RLE: missing repeated byte");
                int count=(control&127)+2;
                if(output+count>result.length)
                    throw new IllegalArgumentException("SCREEN$ RLE: decoded screen exceeds 6912 bytes");
                byte value=encoded[input++];
                java.util.Arrays.fill(result,output,output+count,value);
                output+=count;
            } else {
                int count=control+1;
                if(input+count>encoded.length)
                    throw new IllegalArgumentException("SCREEN$ RLE: truncated literal sequence");
                if(output+count>result.length)
                    throw new IllegalArgumentException("SCREEN$ RLE: decoded screen exceeds 6912 bytes");
                System.arraycopy(encoded,input,result,output,count);
                input+=count;
                output+=count;
            }
        }
        if(output!=result.length)
            throw new IllegalArgumentException("SCREEN$ RLE: expected 6912 bytes, decoded "+output);
        return result;
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
