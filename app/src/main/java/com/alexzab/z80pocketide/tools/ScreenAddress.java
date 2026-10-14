package com.alexzab.z80pocketide.tools;

import java.util.Locale;

/** Byte-level ZX Spectrum 48K screen layout. No emulator state is required. */
public final class ScreenAddress {
    public static final int BITMAP_BASE = 0x4000;
    public static final int ATTR_BASE = 0x5800;

    public final int third;       // 0..2
    public final int row;         // character row within third: 0..7
    public final int pixelLine;   // pixel line within character row: 0..7
    public final int column;      // byte/attribute column: 0..31

    public ScreenAddress(int third, int row, int pixelLine, int column) {
        check("third", third, 2);
        check("row", row, 7);
        check("pixel line", pixelLine, 7);
        check("column", column, 31);
        this.third = third;
        this.row = row;
        this.pixelLine = pixelLine;
        this.column = column;
    }

    private static void check(String name, int value, int max) {
        if (value < 0 || value > max) throw new IllegalArgumentException(name + ": 0.." + max);
    }

    public int characterRow() { return third * 8 + row; }

    public int pixelAddress() {
        return BITMAP_BASE | (third << 11) | (pixelLine << 8) | (row << 5) | column;
    }

    public int attributeAddress() {
        return ATTR_BASE + (characterRow() << 5) + column;
    }

    public static ScreenAddress fromPixelAddress(int address) {
        if (address < 0x4000 || address > 0x57FF)
            throw new IllegalArgumentException("Bitmap: $4000..$57FF");
        int value = address & 0x1FFF;
        return new ScreenAddress((value >> 11) & 3, (value >> 5) & 7,
                (value >> 8) & 7, value & 31);
    }

    public static ScreenAddress fromAttributeAddress(int address, int pixelLine) {
        if (address < 0x5800 || address > 0x5AFF)
            throw new IllegalArgumentException("Attributes: $5800..$5AFF");
        int offset = address - ATTR_BASE;
        return fromAttributeCoordinates(offset >> 5, offset & 31, pixelLine);
    }

    public static ScreenAddress fromAttributeCoordinates(int attrRow, int attrCol, int pixelLine) {
        check("attribute row", attrRow, 23);
        check("attribute column", attrCol, 31);
        return new ScreenAddress(attrRow >> 3, attrRow & 7, pixelLine, attrCol);
    }

    public static int parseHexAddress(String text) {
        if (text == null) throw new IllegalArgumentException("empty address");
        String value = text.trim().replace("_", "").replace(" ", "");
        if (value.startsWith("$") || value.startsWith("#")) value = value.substring(1);
        else if (value.toLowerCase(Locale.ROOT).startsWith("0x")) value = value.substring(2);
        if (value.isEmpty()) throw new IllegalArgumentException("empty address");
        try { return Integer.parseInt(value, 16); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("invalid HEX address"); }
    }

    public static int parseBinaryAddress(String text) {
        if (text == null) throw new IllegalArgumentException("empty address");
        String value = text.trim().replace(" ", "").replace("_", "");
        if (value.startsWith("%")) value = value.substring(1);
        else if (value.toLowerCase(Locale.ROOT).startsWith("0b")) value = value.substring(2);
        if (value.isEmpty()) throw new IllegalArgumentException("empty address");
        try { return Integer.parseInt(value, 2); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("invalid binary address"); }
    }

    public static String hex(int address) { return String.format(Locale.ROOT, "$%04X", address); }

    public static String pixelBits(int address) {
        String bits = String.format(Locale.ROOT, "%16s",
                Integer.toBinaryString(address & 0xFFFF)).replace(' ', '0');
        // 010 TT LLL RRR CCCCC = 3 + 2 + 3 + 3 + 5
        return bits.substring(0, 3) + " " + bits.substring(3, 5) + " "
                + bits.substring(5, 8) + " " + bits.substring(8, 11)
                + " " + bits.substring(11);
    }

    public static String attributeBits(int address) {
        String bits = String.format(Locale.ROOT, "%16s",
                Integer.toBinaryString(address & 0xFFFF)).replace(' ', '0');
        // Prefix 010110 + 10 bits of row and column: 010110 RRRRR CCCCC
        return bits.substring(0, 6) + " " + bits.substring(6, 11)
                + " " + bits.substring(11);
    }
}
