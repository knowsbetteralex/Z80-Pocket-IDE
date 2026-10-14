package com.alexzab.z80pocketide.tools;

import java.util.Locale;

/**
 * Bitmap pixels and one ZX attribute byte per 8x8 tile.
 * Each tile has one INK and one PAPER, as on Spectrum hardware.
 */
public final class SpriteData {
    private final int width, height, cellsX, cellsY;
    private final boolean[][] dots;
    private final int[][] attributes;
    // Whole-sprite palette. Hardware has one BRIGHT bit shared by INK and PAPER.
    private int inkColor = 7;
    private int paperColor = 0;
    private boolean bright;

    public SpriteData(int width, int height) {
        if (width < 8 || width > 64 || (width & 7) != 0
                || height < 8 || height > 64 || (height & 7) != 0)
            throw new IllegalArgumentException("Sprite dimensions: 8..64, multiples of 8");
        this.width = width;
        this.height = height;
        cellsX = width / 8;
        cellsY = height / 8;
        dots = new boolean[height][width];
        attributes = new int[cellsY][cellsX];
        for (int y = 0; y < cellsY; y++)
            for (int x = 0; x < cellsX; x++) attributes[y][x] = 0x07;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int cellsX() { return cellsX; }
    public int cellsY() { return cellsY; }


    /** Global INK/PAPER changes recolor existing pixels immediately, without changing bitmap. */
    public void setInkColor(int paletteIndex) {
        checkColor(paletteIndex);
        inkColor = paletteIndex & 7;
        bright = (paletteIndex & 8) != 0;
        recolorAll();
    }

    public void setPaperColor(int paletteIndex) {
        checkColor(paletteIndex);
        paperColor = paletteIndex & 7;
        bright = (paletteIndex & 8) != 0;
        recolorAll();
    }

    public int getInkColor() { return inkColor | (bright ? 8 : 0); }
    public int getPaperColor() { return paperColor | (bright ? 8 : 0); }

    private void checkColor(int color) {
        if (color < 0 || color > 15)
            throw new IllegalArgumentException("Spectrum color: 0..15");
    }

    private void recolorAll() {
        int attribute = (bright ? 0x40 : 0) | (paperColor << 3) | inkColor;
        for (int y = 0; y < cellsY; y++)
            for (int x = 0; x < cellsX; x++) attributes[y][x] = attribute;
    }

    public void setPixel(int x, int y, boolean filled) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        dots[y][x] = filled;
    }

    public void clearPixels() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) dots[y][x] = false;
    }

    public boolean getPixel(int x, int y) { return dots[y][x]; }

    public void paint(int x, int y, int color, Mode mode) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        int cx = x / 8, cy = y / 8;
        int attr = attributes[cy][cx];
        if (mode == Mode.ERASE) {
            dots[y][x] = false;
        } else if (mode == Mode.INK) {
            dots[y][x] = true;
            attributes[cy][cx] = (attr & 0xF8) | (color & 7);
            if ((color & 8) != 0) attributes[cy][cx] |= 0x40;
            else attributes[cy][cx] &= ~0x40;
        } else if (mode == Mode.PAPER) {
            attributes[cy][cx] = (attr & 0xC7) | ((color & 7) << 3);
            if ((color & 8) != 0) attributes[cy][cx] |= 0x40;
            else attributes[cy][cx] &= ~0x40;
        }
    }

    public enum Mode { INK, PAPER, ERASE }

    public int attribute(int cellX, int cellY) { return attributes[cellY][cellX]; }

    public int pixelByte(int byteX, int y) {
        int value = 0;
        for (int bit = 0; bit < 8; bit++) if (dots[y][byteX * 8 + bit])
            value |= (0x80 >> bit);
        return value;
    }

    public int[] bitmapBytes() {
        int[] bytes = new int[(width / 8) * height];
        int i = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < cellsX; x++) bytes[i++] = pixelByte(x, y);
        return bytes;
    }

    public int[] attributeBytes() {
        int[] bytes = new int[cellsY * cellsX];
        int i = 0;
        for (int y = 0; y < cellsY; y++)
            for (int x = 0; x < cellsX; x++) bytes[i++] = attributes[y][x];
        return bytes;
    }

    public String dbPixels(boolean binary) { return toDb(bitmapBytes(), cellsX, binary); }
    public String dbAttributes(boolean binary) { return toDb(attributeBytes(), cellsX, binary); }

    private static String toDb(int[] values, int perLine, boolean binary) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i % perLine == 0) {
                if (i != 0) code.append('\n');
                code.append("    DB ");
            } else code.append(',');
            if (binary) {
                code.append('%');
                for (int bit = 7; bit >= 0; bit--)
                    code.append(((values[i] >> bit) & 1) != 0 ? '1' : '0');
            } else code.append(String.format(Locale.ROOT, "$%02X", values[i]));
        }
        return code.toString();
    }

    /** Standard Spectrum palette. Index 8..15 sets BRIGHT; black is unchanged. */
    public static int palette(int color) {
        int base = color & 7;
        int level = (color & 8) != 0 ? 255 : 205;
        int r = (base & 2) != 0 ? level : 0;
        int g = (base & 4) != 0 ? level : 0;
        int b = (base & 1) != 0 ? level : 0;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public int displayedColor(int x, int y) {
        int a = attributes[y / 8][x / 8];
        int index = dots[y][x] ? (a & 7) : ((a >> 3) & 7);
        return palette(index | ((a & 0x40) != 0 ? 8 : 0));
    }

    public void clear() {
        clearPixels();
        inkColor = 7;
        paperColor = 0;
        bright = false;
        recolorAll();
    }
}
