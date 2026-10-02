package com.alexzab.z80pocketide.assembler;

import java.util.Locale;
import java.util.Map;

/** Small integer expression evaluator used by assembler operands and directives. */
final class ExpressionEvaluator {
    private final String text;
    private final Map<String, Integer> symbols;
    private int pos;

    private ExpressionEvaluator(String text, Map<String, Integer> symbols) {
        this.text = text;
        this.symbols = symbols;
    }

    static int eval(String expression, Map<String, Integer> symbols) {
        ExpressionEvaluator p = new ExpressionEvaluator(expression, symbols);
        int value = p.parseOr();
        p.skipWs();
        if (!p.eof()) throw new IllegalArgumentException("unexpected '" + p.peek() + "' in expression");
        return value;
    }

    private int parseOr() {
        int v = parseXor();
        while (true) {
            skipWs();
            if (match('|')) v |= parseXor(); else return v;
        }
    }

    private int parseXor() {
        int v = parseAnd();
        while (true) {
            skipWs();
            if (match('^')) v ^= parseAnd(); else return v;
        }
    }

    private int parseAnd() {
        int v = parseShift();
        while (true) {
            skipWs();
            if (match('&')) v &= parseShift(); else return v;
        }
    }

    private int parseShift() {
        int v = parseAdd();
        while (true) {
            skipWs();
            if (match("<<")) v <<= parseAdd();
            else if (match(">>")) v >>= parseAdd();
            else return v;
        }
    }

    private int parseAdd() {
        int v = parseMul();
        while (true) {
            skipWs();
            if (match('+')) v += parseMul();
            else if (match('-')) v -= parseMul();
            else return v;
        }
    }

    private int parseMul() {
        int v = parseUnary();
        while (true) {
            skipWs();
            if (match('*')) v *= parseUnary();
            else if (match('/')) {
                int d = parseUnary();
                if (d == 0) throw new IllegalArgumentException("division by zero");
                v /= d;
            } else if (match('%')) {
                int d = parseUnary();
                if (d == 0) throw new IllegalArgumentException("division by zero");
                v %= d;
            } else return v;
        }
    }

    private int parseUnary() {
        skipWs();
        if (match('+')) return parseUnary();
        if (match('-')) return -parseUnary();
        if (match('~')) return ~parseUnary();
        return parsePrimary();
    }

    private int parsePrimary() {
        skipWs();
        if (match('(')) {
            int v = parseOr();
            skipWs();
            if (!match(')')) throw new IllegalArgumentException("missing ')' in expression");
            return v;
        }
        if (eof()) throw new IllegalArgumentException("incomplete expression");

        if (peek() == '$' || peek() == '#') {
            char prefix = text.charAt(pos++);
            int start = pos;
            while (!eof() && isHex(text.charAt(pos))) pos++;
            if (start == pos) throw new IllegalArgumentException("hex digits expected after " + prefix);
            return Integer.parseUnsignedInt(text.substring(start, pos), 16);
        }
        if (peek() == '%' && pos + 1 < text.length() && (text.charAt(pos + 1) == '0' || text.charAt(pos + 1) == '1')) {
            pos++;
            int start = pos;
            while (!eof() && (peek() == '0' || peek() == '1')) pos++;
            return Integer.parseUnsignedInt(text.substring(start, pos), 2);
        }
        if (Character.isDigit(peek())) {
            int start = pos;
            while (!eof() && Character.isLetterOrDigit(peek())) pos++;
            String token = text.substring(start, pos);
            if (token.endsWith("h") || token.endsWith("H")) {
                return Integer.parseUnsignedInt(token.substring(0, token.length() - 1), 16);
            }
            if (token.endsWith("b") || token.endsWith("B")) {
                String body = token.substring(0, token.length() - 1);
                if (body.matches("[01]+")) return Integer.parseUnsignedInt(body, 2);
            }
            return Integer.parseInt(token);
        }
        if (isIdentStart(peek())) {
            int start = pos++;
            while (!eof() && isIdentPart(peek())) pos++;
            String name = text.substring(start, pos).toUpperCase(Locale.ROOT);
            Integer value = symbols.get(name);
            if (value == null) throw new UnknownSymbolException(name);
            return value;
        }
        throw new IllegalArgumentException("unexpected '" + peek() + "' in expression");
    }

    private boolean eof() { return pos >= text.length(); }
    private char peek() { return text.charAt(pos); }
    private void skipWs() { while (!eof() && Character.isWhitespace(peek())) pos++; }
    private boolean match(char c) {
        if (!eof() && peek() == c) { pos++; return true; }
        return false;
    }
    private boolean match(String s) {
        if (text.regionMatches(pos, s, 0, s.length())) { pos += s.length(); return true; }
        return false;
    }
    private static boolean isHex(char c) { return Character.digit(c, 16) >= 0; }
    private static boolean isIdentStart(char c) { return Character.isLetter(c) || c == '_' || c == '.' || c == '@'; }
    private static boolean isIdentPart(char c) { return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '@' || c == '$'; }

    static final class UnknownSymbolException extends IllegalArgumentException {
        final String symbol;
        UnknownSymbolException(String symbol) {
            super("unknown symbol: " + symbol);
            this.symbol = symbol;
        }
    }
}
