package com.alexzab.z80pocketide.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Original ZX Spectrum 40-key membrane matrix. One half-row is selected by
 * driving exactly one of port high-byte address lines A8..A15 low and A0..A7
 * to $FE. Bits D0..D4 of the input are active-low: 0 = pressed.
 *
 * D5..D7 of the displayed byte are normalized to 1. On real hardware,
 * notably D6 can contain the EAR signal instead of a constant one.
 */
public final class SpectrumKeyboardMatrix {
    public static final int ROW_COUNT = 8;
    public static final int KEYS_PER_ROW = 5;
    public static final int[] PORTS = {
            0xFEFE, 0xFDFE, 0xFBFE, 0xF7FE,
            0xEFFE, 0xDFFE, 0xBFFE, 0x7FFE
    };

    // Order within each half-row is bit 0, bit 1, ... bit 4.
    private static final String[][] KEYS = {
            {"CAPS SHIFT", "Z", "X", "C", "V"},
            {"A", "S", "D", "F", "G"},
            {"Q", "W", "E", "R", "T"},
            {"1", "2", "3", "4", "5"},
            {"0", "9", "8", "7", "6"},
            {"P", "O", "I", "U", "Y"},
            {"ENTER", "L", "K", "J", "H"},
            {"SPACE", "SYMBOL SHIFT", "M", "N", "B"}
    };

    public static final String[][] PHYSICAL = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L", "ENTER"},
            {"CAPS SHIFT", "Z", "X", "C", "V", "B", "N", "M", "SYMBOL SHIFT", "SPACE"}
    };

    private final boolean[][] pressed = new boolean[ROW_COUNT][KEYS_PER_ROW];

    public static int port(int row) {
        checkRow(row);
        return PORTS[row];
    }

    public static int rowFromPort(int address) {
        for (int i = 0; i < PORTS.length; i++)
            if (PORTS[i] == address) return i;
        throw new IllegalArgumentException("Select one half-row: $FEFE ... $7FFE");
    }

    public static String key(int row, int bit) {
        checkRow(row);
        checkBit(bit);
        return KEYS[row][bit];
    }

    public static int rowOf(String keyName) {
        for (int row = 0; row < ROW_COUNT; row++)
            for (int bit = 0; bit < KEYS_PER_ROW; bit++)
                if (KEYS[row][bit].equals(keyName)) return row;
        throw new IllegalArgumentException("Unknown Spectrum key: " + keyName);
    }

    public static int bitOf(String keyName) {
        for (int row = 0; row < ROW_COUNT; row++)
            for (int bit = 0; bit < KEYS_PER_ROW; bit++)
                if (KEYS[row][bit].equals(keyName)) return bit;
        throw new IllegalArgumentException("Unknown Spectrum key: " + keyName);
    }

    private static void checkRow(int row) {
        if (row < 0 || row >= ROW_COUNT) throw new IllegalArgumentException("Row 0..7");
    }

    private static void checkBit(int bit) {
        if (bit < 0 || bit >= KEYS_PER_ROW) throw new IllegalArgumentException("Bit 0..4");
    }

    public boolean isPressed(int row, int bit) {
        checkRow(row);
        checkBit(bit);
        return pressed[row][bit];
    }

    public boolean isPressed(String name) {
        return isPressed(rowOf(name), bitOf(name));
    }

    public void setPressed(int row, int bit, boolean down) {
        checkRow(row);
        checkBit(bit);
        pressed[row][bit] = down;
    }

    public void toggle(String name) {
        int row = rowOf(name), bit = bitOf(name);
        pressed[row][bit] = !pressed[row][bit];
    }

    public int readByte(int row) {
        checkRow(row);
        int result = 0xFF;
        for (int bit = 0; bit < KEYS_PER_ROW; bit++)
            if (pressed[row][bit]) result &= ~(1 << bit);
        return result;
    }

    public boolean rowHasKeys(int row) { return (readByte(row) & 0x1F) != 0x1F; }

    public List<String> pressedKeys(int row) {
        checkRow(row);
        List<String> result = new ArrayList<>();
        for (int bit = 0; bit < KEYS_PER_ROW; bit++)
            if (pressed[row][bit]) result.add(KEYS[row][bit]);
        return result;
    }

    public void clear() {
        for (int row = 0; row < ROW_COUNT; row++)
            for (int bit = 0; bit < KEYS_PER_ROW; bit++) pressed[row][bit] = false;
    }

    /**
     * Reverse decoding replaces the active keyboard selection entirely.
     * Only bits 0..4 are key inputs. High bits of a real port read may
     * vary (e.g. the EAR input on bit 6) and do not represent keys.
     */
    public int loadPortByte(int port, int value) {
        int row = rowFromPort(port);
        if (value < 0 || value > 255)
            throw new IllegalArgumentException("Port byte: 0..255");
        clear();
        for (int bit = 0; bit < KEYS_PER_ROW; bit++)
            pressed[row][bit] = (value & (1 << bit)) == 0;
        return row;
    }

    public static int parseHex(String raw, int max) {
        if (raw == null) throw new IllegalArgumentException("Empty HEX");
        String s = raw.trim().replace("_", "").replace(" ", "");
        if (s.startsWith("$") || s.startsWith("#")) s = s.substring(1);
        else if (s.toLowerCase(Locale.ROOT).startsWith("0x")) s = s.substring(2);
        if (s.length() == 0 || !s.matches("(?i)[0-9a-f]+"))
            throw new IllegalArgumentException("Invalid HEX");
        try {
            int v = Integer.parseInt(s, 16);
            if (v >= 0 && v <= max) return v;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("HEX out of range");
    }

    public static int parseBinaryByte(String raw) {
        if (raw == null) throw new IllegalArgumentException("Empty BIN");
        String s = raw.trim().replace(" ", "").replace("_", "");
        if (s.startsWith("%")) s = s.substring(1);
        else if (s.toLowerCase(Locale.ROOT).startsWith("0b")) s = s.substring(2);
        if (s.length() != 8 || !s.matches("[01]{8}"))
            throw new IllegalArgumentException("8 binary bits required");
        return Integer.parseInt(s, 2);
    }

    public static String hexPort(int value) { return String.format(Locale.ROOT, "$%04X", value); }
    public static String hexByte(int value) { return String.format(Locale.ROOT, "$%02X", value); }
    public static String binByte(int value) {
        return "%" + String.format(Locale.ROOT, "%8s", Integer.toBinaryString(value & 255))
                .replace(' ', '0');
    }

    public static String asmSnippet(int row, int bit) {
        checkRow(row);
        // If no key is selected, a safe port-read-only example is produced.
        StringBuilder code = new StringBuilder();
        code.append("    LD BC,").append(hexPort(port(row))).append('\n');
        code.append("    IN A,(C)");
        if (bit >= 0) {
            checkBit(bit);
            code.append('\n').append("    BIT ").append(bit)
                    .append(",A        ; ").append(key(row,bit))
                    .append(": Z=1 if pressed");
        }
        return code.toString();
    }
}
