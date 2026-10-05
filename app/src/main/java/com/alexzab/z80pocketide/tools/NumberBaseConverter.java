package com.alexzab.z80pocketide.tools;

import java.math.BigInteger;
import java.util.Locale;

/** Pure-Java DEC/HEX/BIN conversion helpers. */
public final class NumberBaseConverter {
    public enum Base {
        DEC(10), HEX(16), BIN(2);
        final int radix;
        Base(int radix) { this.radix = radix; }
    }

    public static final class Values {
        public final String decimal;
        public final String hexadecimal;
        public final String binary;

        Values(String decimal, String hexadecimal, String binary) {
            this.decimal = decimal;
            this.hexadecimal = hexadecimal;
            this.binary = binary;
        }
    }

    private NumberBaseConverter() {}

    public static Values convert(String raw, Base base) {
        BigInteger value = parse(raw, base);
        if (value.bitLength() > 64) {
            throw new IllegalArgumentException("value exceeds 64 bits");
        }
        String sign = value.signum() < 0 ? "-" : "";
        BigInteger abs = value.abs();
        return new Values(
                value.toString(10),
                sign + "$" + abs.toString(16).toUpperCase(Locale.ROOT),
                sign + "%" + abs.toString(2)
        );
    }

    public static BigInteger parse(String raw, Base base) {
        if (raw == null) throw new IllegalArgumentException("empty value");
        String text = raw.trim().replace("_", "");
        if (text.isEmpty()) throw new IllegalArgumentException("empty value");

        boolean negative = text.startsWith("-");
        if (negative || text.startsWith("+")) text = text.substring(1);
        if (text.isEmpty()) throw new IllegalArgumentException("empty value");

        if (base == Base.HEX) {
            if (text.startsWith("$") || text.startsWith("#")) text = text.substring(1);
            else if (text.toLowerCase(Locale.ROOT).startsWith("0x")) text = text.substring(2);
        } else if (base == Base.BIN) {
            if (text.startsWith("%")) text = text.substring(1);
            else if (text.toLowerCase(Locale.ROOT).startsWith("0b")) text = text.substring(2);
        }
        if (text.isEmpty()) throw new IllegalArgumentException("empty value");

        BigInteger value;
        try {
            value = new BigInteger(text, base.radix);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid " + base.name().toLowerCase(Locale.ROOT) + " value");
        }
        return negative ? value.negate() : value;
    }
}
