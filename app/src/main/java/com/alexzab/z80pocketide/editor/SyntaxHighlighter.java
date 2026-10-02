package com.alexzab.z80pocketide.editor;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight Z80 syntax highlighter that works directly on an EditText. */
public final class SyntaxHighlighter {
    private static final int MNEMONIC = Color.rgb(32, 78, 160);
    private static final int REGISTER = Color.rgb(157, 38, 140);
    private static final int DIRECTIVE = Color.rgb(0, 125, 92);
    private static final int NUMBER = Color.rgb(181, 92, 0);
    private static final int LABEL = Color.rgb(0, 118, 128);
    private static final int STRING = Color.rgb(180, 42, 42);
    private static final int COMMENT = Color.rgb(125, 125, 125);

    private static final Pattern P_MNEMONIC = Pattern.compile(
            "(?i)\\b(?:ADC|ADD|AND|BIT|CALL|CCF|CP|CPD|CPDR|CPI|CPIR|CPL|DAA|DEC|DI|DJNZ|EI|EX|EXX|HALT|IM|IN|INC|IND|INDR|INI|INIR|JP|JR|LD|LDD|LDDR|LDI|LDIR|NEG|NOP|OR|OTDR|OTIR|OUT|OUTD|OUTI|POP|PUSH|RES|RET|RETI|RETN|RL|RLA|RLC|RLCA|RLD|RR|RRA|RRC|RRCA|RRD|RST|SBC|SCF|SET|SLA|SRA|SRL|SUB|XOR)\\b");
    private static final Pattern P_REGISTER = Pattern.compile(
            "(?i)(?<![A-Z0-9_])(?:AF'|AF|BC|DE|HL|SP|IX|IY|A|B|C|D|E|H|L|I|R)(?![A-Z0-9_'])");
    private static final Pattern P_DIRECTIVE = Pattern.compile("(?i)\\b(?:ORG|EQU|DB|DW|DS)\\b");
    private static final Pattern P_NUMBER = Pattern.compile(
            "(?i)(?<![A-Z0-9_])(?:\\$[0-9A-F]+|#[0-9A-F]+|%[01]+|[0-9A-F]+H|[01]+B|[0-9]+)(?![A-Z0-9_])");
    private static final Pattern P_LABEL_COLON = Pattern.compile(
            "(?m)^\\s*([A-Za-z_.$?][A-Za-z0-9_.$?]*)(?=\\s*:)");
    private static final Pattern P_LABEL_EQU = Pattern.compile(
            "(?im)^\\s*([A-Za-z_.$?][A-Za-z0-9_.$?]*)(?=\\s+EQU\\b)");
    private static final Pattern P_STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern P_COMMENT = Pattern.compile("(?m);.*$");

    private SyntaxHighlighter() {}

    public static void attach(EditText editor) {
        apply(editor.getText());
        editor.addTextChangedListener(new TextWatcher() {
            private boolean busy;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                if (busy) return;
                busy = true;
                int start = editor.getSelectionStart();
                int end = editor.getSelectionEnd();
                apply(s);
                int length = s.length();
                if (start >= 0 && end >= 0) {
                    editor.setSelection(Math.min(start, length), Math.min(end, length));
                }
                busy = false;
            }
        });
    }

    public static void apply(Editable text) {
        ForegroundColorSpan[] colors = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : colors) text.removeSpan(span);
        StyleSpan[] styles = text.getSpans(0, text.length(), StyleSpan.class);
        for (StyleSpan span : styles) text.removeSpan(span);

        paint(text, P_MNEMONIC, MNEMONIC, true, 0);
        paint(text, P_REGISTER, REGISTER, false, 0);
        paint(text, P_DIRECTIVE, DIRECTIVE, true, 0);
        paint(text, P_NUMBER, NUMBER, false, 0);
        paint(text, P_LABEL_COLON, LABEL, true, 1);
        paint(text, P_LABEL_EQU, LABEL, true, 1);
        paint(text, P_STRING, STRING, false, 0);
        paint(text, P_COMMENT, COMMENT, false, 0);
    }

    private static void paint(Editable text, Pattern pattern, int color, boolean bold, int group) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = group == 0 ? matcher.start() : matcher.start(group);
            int end = group == 0 ? matcher.end() : matcher.end(group);
            text.setSpan(new ForegroundColorSpan(color), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bold) {
                text.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
