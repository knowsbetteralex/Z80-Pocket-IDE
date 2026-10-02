package com.alexzab.z80pocketide.assembler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Table/family-driven encoder for the documented Z80 instruction set.
 * Includes CB/ED prefixes and documented IX/IY indexed forms.
 */
final class Z80Encoder {
    private static final Map<String, Integer> R8 = mapOf(
            "B",0,"C",1,"D",2,"E",3,"H",4,"L",5,"(HL)",6,"A",7);
    private static final Map<String, Integer> RP = mapOf("BC",0,"DE",1,"HL",2,"SP",3);
    private static final Map<String, Integer> QQ = mapOf("BC",0,"DE",1,"HL",2,"AF",3);
    private static final Map<String, Integer> CC = mapOf(
            "NZ",0,"Z",1,"NC",2,"C",3,"PO",4,"PE",5,"P",6,"M",7);
    private static final Map<String, Integer> JRCC = mapOf("NZ",0,"Z",1,"NC",2,"C",3);

    static int size(String code) {
        return encodeInternal(code, 0, new HashMap<>(), true).length;
    }

    static byte[] encode(String code, int pc, Map<String, Integer> symbols) {
        return encodeInternal(code, pc, symbols, false);
    }

    private static byte[] encodeInternal(String code, int pc, Map<String, Integer> symbols, boolean sizeOnly) {
        Parsed p = Parsed.parse(code);
        String m = p.mnemonic;
        String[] o = p.operands;

        Integer fixed = fixedOpcode(m, o);
        if (fixed != null) return opcode(fixed);

        if (m.equals("LD") && o.length == 2) {
            byte[] v = encodeLd(o[0], o[1], symbols, sizeOnly);
            if (v != null) return v;
        }

        if ((m.equals("INC") || m.equals("DEC")) && o.length == 1) {
            boolean dec = m.equals("DEC");
            Integer r = R8.get(o[0]);
            if (r != null) return opcode((dec ? 0x05 : 0x04) + (r << 3));
            Indexed ix = Indexed.parse(o[0]);
            if (ix != null) return bytes(ix.prefix, dec ? 0x35 : 0x34, disp(ix, symbols, sizeOnly));
            Integer rp = RP.get(o[0]);
            if (rp != null) return opcode((dec ? 0x0B : 0x03) + (rp << 4));
            if (o[0].equals("IX") || o[0].equals("IY")) return bytes(prefix(o[0]), dec ? 0x2B : 0x23);
        }

        if (m.equals("ADD") && o.length == 2 && o[0].equals("HL")) {
            Integer rp = RP.get(o[1]);
            if (rp != null) return opcode(0x09 + (rp << 4));
        }
        if (m.equals("ADD") && o.length == 2 && (o[0].equals("IX") || o[0].equals("IY"))) {
            int pre = prefix(o[0]);
            int q;
            if (o[1].equals("BC")) q = 0;
            else if (o[1].equals("DE")) q = 1;
            else if (o[1].equals(o[0])) q = 2;
            else if (o[1].equals("SP")) q = 3;
            else q = -1;
            if (q >= 0) return bytes(pre, 0x09 + (q << 4));
        }
        if ((m.equals("ADC") || m.equals("SBC")) && o.length == 2 && o[0].equals("HL")) {
            Integer rp = RP.get(o[1]);
            if (rp != null) return bytes(0xED, (m.equals("ADC") ? 0x4A : 0x42) + (rp << 4));
        }

        byte[] alu = encodeAlu(m, o, symbols, sizeOnly);
        if (alu != null) return alu;

        if (m.equals("JP")) {
            if (o.length == 1) {
                if (o[0].equals("(HL)")) return opcode(0xE9);
                if (o[0].equals("(IX)")) return bytes(0xDD, 0xE9);
                if (o[0].equals("(IY)")) return bytes(0xFD, 0xE9);
                int nn = imm(o[0], symbols, sizeOnly);
                return bytes(0xC3, lo(nn), hi(nn));
            }
            if (o.length == 2 && CC.containsKey(o[0])) {
                int nn = imm(o[1], symbols, sizeOnly);
                return bytes(0xC2 + (CC.get(o[0]) << 3), lo(nn), hi(nn));
            }
        }
        if (m.equals("JR")) {
            if (o.length == 1) return relative(0x18, o[0], pc, 2, symbols, sizeOnly);
            if (o.length == 2 && JRCC.containsKey(o[0]))
                return relative(0x20 + (JRCC.get(o[0]) << 3), o[1], pc, 2, symbols, sizeOnly);
        }
        if (m.equals("DJNZ") && o.length == 1) return relative(0x10, o[0], pc, 2, symbols, sizeOnly);
        if (m.equals("CALL")) {
            if (o.length == 1) {
                int nn = imm(o[0], symbols, sizeOnly);
                return bytes(0xCD, lo(nn), hi(nn));
            }
            if (o.length == 2 && CC.containsKey(o[0])) {
                int nn = imm(o[1], symbols, sizeOnly);
                return bytes(0xC4 + (CC.get(o[0]) << 3), lo(nn), hi(nn));
            }
        }
        if (m.equals("RET") && o.length == 1 && CC.containsKey(o[0])) return opcode(0xC0 + (CC.get(o[0]) << 3));
        if (m.equals("RST") && o.length == 1) {
            int n = imm(o[0], symbols, sizeOnly);
            if (!sizeOnly && (n < 0 || n > 0x38 || (n & 7) != 0)) throw new IllegalArgumentException("RST vector must be 0,8,...,$38");
            return opcode(0xC7 + (n & 0x38));
        }

        if ((m.equals("PUSH") || m.equals("POP")) && o.length == 1) {
            boolean push = m.equals("PUSH");
            Integer qq = QQ.get(o[0]);
            if (qq != null) return opcode((push ? 0xC5 : 0xC1) + (qq << 4));
            if (o[0].equals("IX") || o[0].equals("IY")) return bytes(prefix(o[0]), push ? 0xE5 : 0xE1);
        }

        if (m.equals("IN") && o.length == 2) {
            if (o[0].equals("A") && isParenExpr(o[1])) return bytes(0xDB, byteImm(inner(o[1]), symbols, sizeOnly));
            Integer r = R8.get(o[0]);
            if (r != null && r != 6 && o[1].equals("(C)")) return bytes(0xED, 0x40 + (r << 3));
        }
        if (m.equals("OUT") && o.length == 2) {
            if (isParenExpr(o[0]) && o[1].equals("A") && !o[0].equals("(C)")) return bytes(0xD3, byteImm(inner(o[0]), symbols, sizeOnly));
            Integer r = R8.get(o[1]);
            if (o[0].equals("(C)") && r != null && r != 6) return bytes(0xED, 0x41 + (r << 3));
        }

        byte[] cb = encodeCb(m, o, symbols, sizeOnly);
        if (cb != null) return cb;

        if (m.equals("IM") && o.length == 1) {
            int n = imm(o[0], symbols, sizeOnly);
            if (sizeOnly) return bytes(0xED, 0x46);
            if (n == 0) return bytes(0xED, 0x46);
            if (n == 1) return bytes(0xED, 0x56);
            if (n == 2) return bytes(0xED, 0x5E);
            throw new IllegalArgumentException("IM mode must be 0, 1 or 2");
        }

        throw new IllegalArgumentException("unsupported instruction or operands: " + code.trim());
    }

    private static byte[] encodeLd(String dst, String src, Map<String,Integer> symbols, boolean sizeOnly) {
        Integer d = R8.get(dst), s = R8.get(src);
        if (d != null && s != null) {
            if (d == 6 && s == 6) return null;
            return opcode(0x40 + (d << 3) + s);
        }
        if (d != null && d != 6 && !looksLikeRegister(src) && !isParen(src)) {
            return bytes(0x06 + (d << 3), byteImm(src, symbols, sizeOnly));
        }
        if (dst.equals("(HL)") && !looksLikeRegister(src) && !isParen(src)) return bytes(0x36, byteImm(src, symbols, sizeOnly));

        Indexed dix = Indexed.parse(dst), six = Indexed.parse(src);
        if (d != null && d != 6 && six != null) return bytes(six.prefix, 0x46 + (d << 3), disp(six, symbols, sizeOnly));
        if (dix != null && s != null && s != 6) return bytes(dix.prefix, 0x70 + s, disp(dix, symbols, sizeOnly));
        if (dix != null && !looksLikeRegister(src) && !isParen(src)) return bytes(dix.prefix, 0x36, disp(dix, symbols, sizeOnly), byteImm(src, symbols, sizeOnly));

        Integer rp = RP.get(dst);
        if (rp != null && !isParen(src) && !looksLikeRegister(src)) {
            int nn = wordImm(src, symbols, sizeOnly);
            return bytes(0x01 + (rp << 4), lo(nn), hi(nn));
        }
        if ((dst.equals("IX") || dst.equals("IY")) && !isParen(src) && !looksLikeRegister(src)) {
            int nn = wordImm(src, symbols, sizeOnly);
            return bytes(prefix(dst), 0x21, lo(nn), hi(nn));
        }

        if (dst.equals("A") && src.equals("(BC)")) return opcode(0x0A);
        if (dst.equals("A") && src.equals("(DE)")) return opcode(0x1A);
        if (dst.equals("(BC)") && src.equals("A")) return opcode(0x02);
        if (dst.equals("(DE)") && src.equals("A")) return opcode(0x12);

        if (dst.equals("A") && isParenExpr(src)) {
            int nn = wordImm(inner(src), symbols, sizeOnly);
            return bytes(0x3A, lo(nn), hi(nn));
        }
        if (isParenExpr(dst) && src.equals("A")) {
            int nn = wordImm(inner(dst), symbols, sizeOnly);
            return bytes(0x32, lo(nn), hi(nn));
        }
        if (dst.equals("HL") && isParenExpr(src)) {
            int nn = wordImm(inner(src), symbols, sizeOnly);
            return bytes(0x2A, lo(nn), hi(nn));
        }
        if (isParenExpr(dst) && src.equals("HL")) {
            int nn = wordImm(inner(dst), symbols, sizeOnly);
            return bytes(0x22, lo(nn), hi(nn));
        }
        if ((dst.equals("BC") || dst.equals("DE") || dst.equals("SP")) && isParenExpr(src)) {
            int nn = wordImm(inner(src), symbols, sizeOnly);
            int q = RP.get(dst);
            return bytes(0xED, 0x4B + (q << 4), lo(nn), hi(nn));
        }
        if (isParenExpr(dst) && (src.equals("BC") || src.equals("DE") || src.equals("SP"))) {
            int nn = wordImm(inner(dst), symbols, sizeOnly);
            int q = RP.get(src);
            return bytes(0xED, 0x43 + (q << 4), lo(nn), hi(nn));
        }
        if ((dst.equals("IX") || dst.equals("IY")) && isParenExpr(src)) {
            int nn = wordImm(inner(src), symbols, sizeOnly);
            return bytes(prefix(dst), 0x2A, lo(nn), hi(nn));
        }
        if (isParenExpr(dst) && (src.equals("IX") || src.equals("IY"))) {
            int nn = wordImm(inner(dst), symbols, sizeOnly);
            return bytes(prefix(src), 0x22, lo(nn), hi(nn));
        }
        if (dst.equals("SP") && src.equals("HL")) return opcode(0xF9);
        if (dst.equals("SP") && (src.equals("IX") || src.equals("IY"))) return bytes(prefix(src), 0xF9);

        if (dst.equals("I") && src.equals("A")) return bytes(0xED,0x47);
        if (dst.equals("R") && src.equals("A")) return bytes(0xED,0x4F);
        if (dst.equals("A") && src.equals("I")) return bytes(0xED,0x57);
        if (dst.equals("A") && src.equals("R")) return bytes(0xED,0x5F);
        return null;
    }

    private static byte[] encodeAlu(String m, String[] o, Map<String,Integer> symbols, boolean sizeOnly) {
        String src;
        int base, immOp;
        if (m.equals("ADD") && o.length == 2 && o[0].equals("A")) { src=o[1]; base=0x80; immOp=0xC6; }
        else if (m.equals("ADC") && o.length == 2 && o[0].equals("A")) { src=o[1]; base=0x88; immOp=0xCE; }
        else if (m.equals("SUB") && o.length == 1) { src=o[0]; base=0x90; immOp=0xD6; }
        else if (m.equals("SBC") && o.length == 2 && o[0].equals("A")) { src=o[1]; base=0x98; immOp=0xDE; }
        else if (m.equals("AND") && o.length == 1) { src=o[0]; base=0xA0; immOp=0xE6; }
        else if (m.equals("XOR") && o.length == 1) { src=o[0]; base=0xA8; immOp=0xEE; }
        else if (m.equals("OR")  && o.length == 1) { src=o[0]; base=0xB0; immOp=0xF6; }
        else if (m.equals("CP")  && o.length == 1) { src=o[0]; base=0xB8; immOp=0xFE; }
        else return null;

        Integer r = R8.get(src);
        if (r != null) return opcode(base + r);
        Indexed ix = Indexed.parse(src);
        if (ix != null) return bytes(ix.prefix, base + 6, disp(ix, symbols, sizeOnly));
        if (!isParen(src) && !looksLikeRegister(src)) return bytes(immOp, byteImm(src, symbols, sizeOnly));
        return null;
    }

    private static byte[] encodeCb(String m, String[] o, Map<String,Integer> symbols, boolean sizeOnly) {
        Integer rot = null;
        if (m.equals("RLC")) rot=0; else if (m.equals("RRC")) rot=1; else if (m.equals("RL")) rot=2;
        else if (m.equals("RR")) rot=3; else if (m.equals("SLA")) rot=4; else if (m.equals("SRA")) rot=5; else if (m.equals("SRL")) rot=7;
        if (rot != null && o.length == 1) {
            Integer r = R8.get(o[0]);
            if (r != null) return bytes(0xCB, (rot << 3) + r);
            Indexed ix = Indexed.parse(o[0]);
            if (ix != null) return bytes(ix.prefix, 0xCB, disp(ix, symbols, sizeOnly), (rot << 3) + 6);
        }
        if ((m.equals("BIT") || m.equals("RES") || m.equals("SET")) && o.length == 2) {
            int bit = imm(o[0], symbols, sizeOnly);
            if (!sizeOnly && (bit < 0 || bit > 7)) throw new IllegalArgumentException("bit number must be 0..7");
            int group = m.equals("BIT") ? 0x40 : (m.equals("RES") ? 0x80 : 0xC0);
            Integer r = R8.get(o[1]);
            if (r != null) return bytes(0xCB, group + ((bit & 7) << 3) + r);
            Indexed ix = Indexed.parse(o[1]);
            if (ix != null) return bytes(ix.prefix, 0xCB, disp(ix, symbols, sizeOnly), group + ((bit & 7) << 3) + 6);
        }
        return null;
    }

    private static Integer fixedOpcode(String m, String[] o) {
        if (o.length != 0) {
            if (m.equals("EX") && o.length == 2) {
                if (o[0].equals("DE") && o[1].equals("HL")) return 0xEB;
                if (o[0].equals("AF") && o[1].equals("AF'")) return 0x08;
                if (o[0].equals("(SP)") && o[1].equals("HL")) return 0xE3;
                if (o[0].equals("(SP)") && o[1].equals("IX")) return 0xDDE3;
                if (o[0].equals("(SP)") && o[1].equals("IY")) return 0xFDE3;
            }
            return null;
        }
        switch (m) {
            case "NOP": return 0x00; case "RLCA": return 0x07; case "RRCA": return 0x0F;
            case "RLA": return 0x17; case "RRA": return 0x1F; case "DAA": return 0x27;
            case "CPL": return 0x2F; case "SCF": return 0x37; case "CCF": return 0x3F;
            case "HALT": return 0x76; case "RET": return 0xC9; case "EXX": return 0xD9;
            case "DI": return 0xF3; case "EI": return 0xFB;
            case "NEG": return 0xED44; case "RETN": return 0xED45; case "RETI": return 0xED4D;
            case "RRD": return 0xED67; case "RLD": return 0xED6F;
            case "LDI": return 0xEDA0; case "CPI": return 0xEDA1; case "INI": return 0xEDA2; case "OUTI": return 0xEDA3;
            case "LDD": return 0xEDA8; case "CPD": return 0xEDA9; case "IND": return 0xEDAA; case "OUTD": return 0xEDAB;
            case "LDIR": return 0xEDB0; case "CPIR": return 0xEDB1; case "INIR": return 0xEDB2; case "OTIR": return 0xEDB3;
            case "LDDR": return 0xEDB8; case "CPDR": return 0xEDB9; case "INDR": return 0xEDBA; case "OTDR": return 0xEDBB;
            default: return null;
        }
    }

    private static byte[] relative(int op, String expr, int pc, int len, Map<String,Integer> symbols, boolean sizeOnly) {
        if (sizeOnly) return bytes(op,0);
        int target = imm(expr, symbols, false);
        int d = target - (pc + len);
        if (d < -128 || d > 127) throw new IllegalArgumentException("relative target out of range: " + d);
        return bytes(op,d);
    }

    private static int imm(String expr, Map<String,Integer> symbols, boolean sizeOnly) {
        if (sizeOnly) {
            try { return ExpressionEvaluator.eval(expr, symbols); }
            catch (ExpressionEvaluator.UnknownSymbolException ex) { return 0; }
        }
        return ExpressionEvaluator.eval(expr, symbols);
    }

    private static int byteImm(String expr, Map<String,Integer> symbols, boolean sizeOnly) {
        int v = imm(expr, symbols, sizeOnly);
        if (!sizeOnly && (v < -128 || v > 255)) throw new IllegalArgumentException("8-bit value out of range: " + v);
        return v;
    }

    private static int wordImm(String expr, Map<String,Integer> symbols, boolean sizeOnly) {
        int v = imm(expr, symbols, sizeOnly);
        if (!sizeOnly && (v < -32768 || v > 0xFFFF)) throw new IllegalArgumentException("16-bit value out of range: " + v);
        return v;
    }

    private static int disp(Indexed ix, Map<String,Integer> symbols, boolean sizeOnly) {
        int d = imm(ix.displacement, symbols, sizeOnly);
        if (!sizeOnly && (d < -128 || d > 127)) throw new IllegalArgumentException("index displacement out of range: " + d);
        return d;
    }

    private static boolean looksLikeRegister(String s) {
        return R8.containsKey(s) || RP.containsKey(s) || QQ.containsKey(s) || s.equals("IX") || s.equals("IY") || s.equals("I") || s.equals("R");
    }
    private static boolean isParen(String s) { return s.startsWith("(") && s.endsWith(")"); }
    private static boolean isParenExpr(String s) { return isParen(s) && !s.equals("(HL)") && !s.equals("(BC)") && !s.equals("(DE)") && !s.equals("(SP)") && !s.equals("(C)") && !s.equals("(IX)") && !s.equals("(IY)") && Indexed.parse(s) == null; }
    private static String inner(String s) { return s.substring(1,s.length()-1).trim(); }
    private static int prefix(String r) { return r.equals("IX") ? 0xDD : 0xFD; }
    private static int lo(int v) { return v & 0xFF; }
    private static int hi(int v) { return (v >>> 8) & 0xFF; }

    private static byte[] opcode(int opcode) {
        if (opcode <= 0xFF) return bytes(opcode);
        return bytes((opcode >>> 8) & 0xFF, opcode & 0xFF);
    }
    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i=0;i<values.length;i++) b[i]=(byte)values[i];
        return b;
    }

    private static Map<String,Integer> mapOf(Object... a) {
        Map<String,Integer> m = new HashMap<>();
        for (int i=0;i<a.length;i+=2) m.put((String)a[i], (Integer)a[i+1]);
        return m;
    }

    private static final class Indexed {
        final int prefix;
        final String displacement;
        Indexed(int prefix, String displacement) { this.prefix=prefix; this.displacement=displacement; }
        static Indexed parse(String operand) {
            if (!operand.startsWith("(") || !operand.endsWith(")")) return null;
            String s = operand.substring(1, operand.length()-1).replace(" ", "").toUpperCase(Locale.ROOT);
            int pre;
            if (s.startsWith("IX")) pre=0xDD; else if (s.startsWith("IY")) pre=0xFD; else return null;
            String rest=s.substring(2);
            if (rest.isEmpty()) return null;
            if (rest.charAt(0)=='+') rest=rest.substring(1);
            else if (rest.charAt(0)!='-') return null;
            return new Indexed(pre, rest);
        }
    }

    private static final class Parsed {
        final String mnemonic;
        final String[] operands;
        Parsed(String mnemonic, String[] operands) { this.mnemonic=mnemonic; this.operands=operands; }
        static Parsed parse(String code) {
            String t=code.trim();
            int sp=-1;
            for (int i=0;i<t.length();i++) if (Character.isWhitespace(t.charAt(i))) { sp=i; break; }
            String m=(sp<0?t:t.substring(0,sp)).toUpperCase(Locale.ROOT);
            if (sp<0) return new Parsed(m,new String[0]);
            String tail=t.substring(sp).trim();
            if (tail.isEmpty()) return new Parsed(m,new String[0]);
            String[] raw=splitOperands(tail);
            for (int i=0;i<raw.length;i++) raw[i]=raw[i].trim().toUpperCase(Locale.ROOT);
            return new Parsed(m,raw);
        }
        private static String[] splitOperands(String s) {
            int depth=0; boolean quote=false;
            java.util.List<String> out=new java.util.ArrayList<>();
            int start=0;
            for (int i=0;i<s.length();i++) {
                char c=s.charAt(i);
                if (c=='\'' || c=='\"') quote=!quote;
                if (!quote) {
                    if (c=='(') depth++; else if (c==')') depth--;
                    else if (c==',' && depth==0) { out.add(s.substring(start,i)); start=i+1; }
                }
            }
            out.add(s.substring(start));
            return out.toArray(new String[0]);
        }
    }
}
