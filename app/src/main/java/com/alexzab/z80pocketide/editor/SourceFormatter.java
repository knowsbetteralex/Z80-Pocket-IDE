package com.alexzab.z80pocketide.editor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java formatting helpers shared by live editing and Format document. */
public final class SourceFormatter {
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "ADC","ADD","AND","BIT","CALL","CCF","CP","CPD","CPDR","CPI","CPIR",
            "CPL","DAA","DEC","DI","DJNZ","EI","EX","EXX","HALT","IM","IN","INC",
            "IND","INDR","INI","INIR","JP","JR","LD","LDD","LDDR","LDI","LDIR",
            "NEG","NOP","OR","OTDR","OTIR","OUT","OUTD","OUTI","POP","PUSH","RES",
            "RET","RETI","RETN","RL","RLA","RLC","RLCA","RLD","RR","RRA","RRC",
            "RRCA","RRD","RST","SBC","SCF","SET","SLA","SRA","SRL","SUB","XOR",
            "ORG","EQU","DB","DW","DS",
            "AF","AF'","BC","DE","HL","SP","IX","IY","A","B","C","D","E","H","L","I","R",
            "NZ","Z","NC","PO","PE","P","M"
    ));

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z0-9']*");
    private static final Pattern LABEL_ONLY = Pattern.compile(
            "^[A-Za-z_.$?][A-Za-z0-9_.$?]*\\s*:\\s*(?:;.*)?$");
    private static final Pattern EQU_LINE = Pattern.compile(
            "(?i)^[A-Za-z_.$?][A-Za-z0-9_.$?]*\\s+EQU\\b.*$");

    private SourceFormatter() {}

    public static String uppercaseKeywordsInLine(String line) {
        if (line == null || line.isEmpty()) return line == null ? "" : line;
        int comment = commentStart(line);
        String code = comment < 0 ? line : line.substring(0, comment);
        String suffix = comment < 0 ? "" : line.substring(comment);

        Matcher matcher = WORD.matcher(code);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();
            String upper = token.toUpperCase(Locale.ROOT);
            matcher.appendReplacement(out, Matcher.quoteReplacement(
                    KEYWORDS.contains(upper) ? upper : token));
        }
        matcher.appendTail(out);
        return out + suffix;
    }

    public static String formatDocument(String source, boolean uppercase, boolean indentation,
                                        int tabSize, boolean useSpaces) {
        if (source == null || source.isEmpty()) return source == null ? "" : source;
        String unit = indentUnit(tabSize, useSpaces);
        String[] lines = source.split("\\n", -1);
        StringBuilder out = new StringBuilder(source.length() + 32);

        for (int i = 0; i < lines.length; i++) {
            String original = lines[i];
            String line = uppercase ? uppercaseKeywordsInLine(original) : original;

            if (indentation && !line.trim().isEmpty()) {
                String trimmed = line.trim();
                if (trimmed.startsWith(";")) {
                    line = unit + trimmed;
                } else if (LABEL_ONLY.matcher(trimmed).matches() || EQU_LINE.matcher(trimmed).matches()
                        || startsWithDirective(trimmed, "ORG")) {
                    line = trimmed;
                } else if (hasLeadingLabel(trimmed)) {
                    line = formatLabelAndInstruction(trimmed, unit);
                } else {
                    line = unit + trimmed;
                }
            }

            out.append(line);
            if (i + 1 < lines.length) out.append('\n');
        }
        return out.toString();
    }

    public static String indentationForNewLine(String previousLine, int tabSize, boolean useSpaces) {
        String previous = previousLine == null ? "" : previousLine;
        StringBuilder leading = new StringBuilder();
        for (int i = 0; i < previous.length(); i++) {
            char c = previous.charAt(i);
            if (c == ' ' || c == '\t') leading.append(c);
            else break;
        }
        String trimmed = previous.trim();
        if (LABEL_ONLY.matcher(trimmed).matches()) {
            return leading + indentUnit(tabSize, useSpaces);
        }
        return leading.toString();
    }

    public static String indentUnit(int tabSize, boolean useSpaces) {
        int size = tabSize == 2 || tabSize == 8 ? tabSize : 4;
        if (!useSpaces) return "\t";
        StringBuilder out = new StringBuilder(size);
        for (int i = 0; i < size; i++) out.append(' ');
        return out.toString();
    }

    private static boolean startsWithDirective(String line, String directive) {
        return line.equalsIgnoreCase(directive)
                || line.toUpperCase(Locale.ROOT).startsWith(directive + " ");
    }

    private static boolean hasLeadingLabel(String trimmed) {
        int colon = trimmed.indexOf(':');
        if (colon <= 0) return false;
        String candidate = trimmed.substring(0, colon).trim();
        return candidate.matches("[A-Za-z_.$?][A-Za-z0-9_.$?]*");
    }

    private static String formatLabelAndInstruction(String trimmed, String unit) {
        int colon = trimmed.indexOf(':');
        String label = trimmed.substring(0, colon + 1);
        String rest = trimmed.substring(colon + 1).trim();
        if (rest.isEmpty()) return label;
        return label + "\n" + unit + rest;
    }

    private static int commentStart(String line) {
        boolean quote = false;
        char quoteChar = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if ((c == '\'' || c == '"')) {
                if (!quote) { quote = true; quoteChar = c; }
                else if (quoteChar == c) quote = false;
            }
            if (c == ';' && !quote) return i;
        }
        return -1;
    }
}
