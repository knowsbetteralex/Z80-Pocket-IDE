package com.alexzab.z80pocketide.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java syntax scanner. May run on a worker thread: all inputs are
 * immutable String snapshots, and results are integer ranges (no Android spans).
 * The caller supplies only a small window of visible lines, not the whole file.
 */
public final class SyntaxTokenPlanner {
    public static final int MNEMONIC=1, REGISTER=2, DIRECTIVE=3, NUMBER=4,
            LABEL=5, STRING=6, COMMENT=7;

    public static final class Token {
        public final int start, end, kind;
        public final boolean bold;
        public Token(int start,int end,int kind,boolean bold) {
            this.start=start;this.end=end;this.kind=kind;this.bold=bold;
        }
    }

    private static final Pattern P_MNEMONIC = Pattern.compile(
            "(?i)\\b(?:ADC|ADD|AND|BIT|CALL|CCF|CP|CPD|CPDR|CPI|CPIR|CPL|DAA|DEC|DI|DJNZ|EI|EX|EXX|HALT|IM|IN|INC|IND|INDR|INI|INIR|JP|JR|LD|LDD|LDDR|LDI|LDIR|NEG|NOP|OR|OTDR|OTIR|OUT|OUTD|OUTI|POP|PUSH|RES|RET|RETI|RETN|RL|RLA|RLC|RLCA|RLD|RR|RRA|RRC|RRCA|RRD|RST|SBC|SCF|SET|SLA|SRA|SRL|SUB|XOR)\\b");
    private static final Pattern P_REGISTER = Pattern.compile(
            "(?i)(?<![A-Z0-9_])(?:AF'|AF|BC|DE|HL|SP|IX|IY|A|B|C|D|E|H|L|I|R)(?![A-Z0-9_'])");
    private static final Pattern P_DIRECTIVE = Pattern.compile("(?i)\\b(?:ORG|EQU|DB|DW|DS|INCLUDE|MACRO|ENDM|ENDMACRO|IF|IFN|ELSE|ELSEIF|ELIF|ENDIF|IFDEF|IFNDEF)\\b");
    private static final Pattern P_NUMBER = Pattern.compile(
            "(?i)(?<![A-Z0-9_])(?:\\$[0-9A-F]+|#[0-9A-F]+|%[01]+|[0-9A-F]+H|[01]+B|[0-9]+)(?![A-Z0-9_])");
    private static final Pattern P_LABEL_COLON = Pattern.compile(
            "(?m)^\\s*([A-Za-z_.$?][A-Za-z0-9_.$?]*)(?=\\s*:)");
    private static final Pattern P_LABEL_EQU = Pattern.compile(
            "(?im)^\\s*([A-Za-z_.$?][A-Za-z0-9_.$?]*)(?=\\s+EQU\\b)");
    private static final Pattern P_STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern P_SINGLE_QUOTED = Pattern.compile(
            "'(?:\\\\.|[^'\\\\\\r\\n])*'");
    private static final Pattern P_COMMENT = Pattern.compile("(?m);.*$");

    private SyntaxTokenPlanner() {}

    public static List<Token> plan(String visibleWindow, String completeSource, int windowOffset) {
        List<Token> result=new ArrayList<>();
        if(visibleWindow == null || visibleWindow.isEmpty())return result;
        // Same drawing priority as the original reference highlighter.
        paint(result, visibleWindow, windowOffset, P_MNEMONIC, MNEMONIC,true,0);
        paint(result, visibleWindow, windowOffset, P_REGISTER, REGISTER,false,0);
        paint(result, visibleWindow, windowOffset, P_DIRECTIVE, DIRECTIVE,true,0);
        paint(result, visibleWindow, windowOffset, P_NUMBER, NUMBER,false,0);
        paint(result, visibleWindow, windowOffset, P_LABEL_COLON, LABEL,true,1);
        paint(result, visibleWindow, windowOffset, P_LABEL_EQU, LABEL,true,1);
        paint(result, visibleWindow, windowOffset, P_STRING, STRING,false,0);
        paint(result, visibleWindow, windowOffset, P_SINGLE_QUOTED, STRING,false,0);
        paint(result, visibleWindow, windowOffset, P_COMMENT, COMMENT,false,0);
        Set<String> known=SourceSymbols.definedLabels(completeSource);
        for(int[] range:SourceSymbols.labelRanges(visibleWindow,known))
            result.add(new Token(windowOffset+range[0],windowOffset+range[1],LABEL,true));
        return result;
    }

    private static void paint(List<Token> result,String source,int offset,
                              Pattern pattern,int kind,boolean bold,int group) {
        Matcher match=pattern.matcher(source);
        while(match.find()) {
            int from=group==0?match.start():match.start(group);
            int to=group==0?match.end():match.end(group);
            if(to>from)result.add(new Token(offset+from,offset+to,kind,bold));
        }
    }
}
