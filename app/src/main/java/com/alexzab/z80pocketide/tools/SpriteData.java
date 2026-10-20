package com.alexzab.z80pocketide.tools;

import java.util.Locale;

/**
 * Lossless 64x64 backing store with a movable, resizable 8..64-pixel viewport.
 *
 * Both the sprite bitmap and the mask are stored in source coordinates.
 * Translation and canvas-size changes affect only projection / DB export,
 * so clipped content returns when the offset or dimensions are restored.
 * Mask convention in the editor: 1 = visible, 0 = transparent.
 */
public final class SpriteData {
    public static final int MAX_SIZE = 64;

    private int width, height;
    private int offsetX, offsetY;
    private final boolean[][] dots = new boolean[MAX_SIZE][MAX_SIZE];
    private final boolean[][] mask = new boolean[MAX_SIZE][MAX_SIZE];
    // Attributes are attached to the visible character grid (0..7 in each axis).
    // Import may contain independent colors/FLASH for each 8x8 cell.
    private final int[][] attributes = new int[MAX_SIZE / 8][MAX_SIZE / 8];

    private int inkColor = 7;
    private int paperColor;
    private boolean bright;

    public SpriteData(int width, int height) {
        resize(width, height);
        resetMask();
        recolorAll();
    }

    /** Resizing never erases or translates either backing layer. */
    public void resize(int width, int height) {
        validSize(width);
        validSize(height);
        this.width = width;
        this.height = height;
    }

    private static void validSize(int size) {
        if (size < 8 || size > MAX_SIZE || (size & 7) != 0)
            throw new IllegalArgumentException("Sprite dimensions: 8..64, multiples of 8");
    }

    public int width() { return width; }
    public int height() { return height; }
    public int cellsX() { return width / 8; }
    public int cellsY() { return height / 8; }

    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }

    public void moveBy(int dx, int dy) {
        offsetX = Math.max(-MAX_SIZE, Math.min(MAX_SIZE, offsetX + dx));
        offsetY = Math.max(-MAX_SIZE, Math.min(MAX_SIZE, offsetY + dy));
    }

    public void resetOffset() { offsetX = offsetY = 0; }

    private boolean sourceInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < MAX_SIZE && y < MAX_SIZE;
    }

    private boolean visibleInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private int sourceX(int x) { return x - offsetX; }
    private int sourceY(int y) { return y - offsetY; }

    /** Read a pixel in current exported (shifted and clipped) coordinates. */
    public boolean getPixel(int x, int y) {
        int sx = sourceX(x), sy = sourceY(y);
        return visibleInBounds(x, y) && sourceInBounds(sx, sy) && dots[sy][sx];
    }

    /** Drawing changes the stored source point; moving away never destroys it. */
    public void setPixel(int x, int y, boolean filled) {
        int sx = sourceX(x), sy = sourceY(y);
        if (visibleInBounds(x, y) && sourceInBounds(sx, sy)) dots[sy][sx] = filled;
    }

    /** Mask=1 means opaque/visible, mask=0 means transparent/checkerboard. */
    public boolean getMask(int x, int y) {
        int sx = sourceX(x), sy = sourceY(y);
        return visibleInBounds(x, y) && sourceInBounds(sx, sy) && mask[sy][sx];
    }

    public void setMask(int x, int y, boolean visible) {
        int sx = sourceX(x), sy = sourceY(y);
        if (visibleInBounds(x, y) && sourceInBounds(sx, sy)) mask[sy][sx] = visible;
    }

    public void resetMask() {
        for (int y = 0; y < MAX_SIZE; y++)
            for (int x = 0; x < MAX_SIZE; x++) mask[y][x] = true;
    }

    public void invertMask() {
        for (int y = 0; y < MAX_SIZE; y++)
            for (int x = 0; x < MAX_SIZE; x++) mask[y][x] = !mask[y][x];
    }

    public void clearPixels() {
        for (int y = 0; y < MAX_SIZE; y++)
            for (int x = 0; x < MAX_SIZE; x++) dots[y][x] = false;
    }

    public void clear() {
        clearPixels();
        resetMask();
        resetOffset();
        inkColor = 7;
        paperColor = 0;
        bright = false;
        recolorAll();
    }

    public void setInkColor(int color) {
        checkColor(color);
        inkColor = color & 7;
        bright = (color & 8) != 0;
        recolorAll();
    }

    public void setPaperColor(int color) {
        checkColor(color);
        paperColor = color & 7;
        bright = (color & 8) != 0;
        recolorAll();
    }

    private void recolorAll() {
        int base = (bright ? 0x40 : 0) | (paperColor << 3) | inkColor;
        for (int y = 0; y < attributes.length; y++)
            for (int x = 0; x < attributes[y].length; x++)
                attributes[y][x] = (attributes[y][x] & 0x80) | base;
    }

    private void checkColor(int color) {
        if (color < 0 || color > 15)
            throw new IllegalArgumentException("Spectrum color: 0..15");
    }

    public int getInkColor() { return inkColor | (bright ? 8 : 0); }
    public int getPaperColor() { return paperColor | (bright ? 8 : 0); }

    /** Compatibility with existing editor/test callers. */
    public enum Mode { INK, PAPER, ERASE }

    public void paint(int x, int y, int color, Mode mode) {
        if (!visibleInBounds(x, y)) return;
        if (mode == Mode.ERASE) setPixel(x, y, false);
        else if (mode == Mode.INK) {
            setPixel(x, y, true);
            setInkColor(color);
        } else if (mode == Mode.PAPER) setPaperColor(color);
    }

    public int attribute(int cellX, int cellY) {
        if (cellX < 0 || cellY < 0 || cellX >= cellsX() || cellY >= cellsY())
            throw new IndexOutOfBoundsException();
        return attributes[cellY][cellX];
    }

    /**
     * Import precisely one visible bitmap or mask layer.
     * Write through current viewport offset, preserving offscreen backing data.
     */
    public void loadBitmap(int[] bytes) { loadBitLayer(bytes, false); }
    public void loadMask(int[] bytes) { loadBitLayer(bytes, true); }

    private void loadBitLayer(int[] bytes, boolean maskLayer) {
        requireLength(bytes, cellsX() * height);
        // Source coordinates outside the 64x64 backing area are intentionally
        // clipped, just like interactive painting at the same offset.
        for (int y = 0; y < height; y++)
            for (int byteX = 0; byteX < cellsX(); byteX++) {
                int value = bytes[y * cellsX() + byteX];
                for (int bit = 0; bit < 8; bit++) {
                    boolean enabled = ((value >>> (7 - bit)) & 1) != 0;
                    if (maskLayer) setMask(byteX * 8 + bit, y, enabled);
                    else setPixel(byteX * 8 + bit, y, enabled);
                }
            }
    }

    /** Import all attribute bytes, including per-tile INK/PAPER/BRIGHT/FLASH. */
    public void loadAttributes(int[] bytes) {
        requireLength(bytes, cellsX() * cellsY());
        for (int y = 0; y < cellsY(); y++)
            for (int x = 0; x < cellsX(); x++)
                attributes[y][x] = bytes[y * cellsX() + x];
        int first = bytes[0];
        inkColor = first & 7;
        paperColor = (first >>> 3) & 7;
        bright = (first & 0x40) != 0;
    }

    private static void requireLength(int[] bytes, int expected) {
        if (bytes == null || bytes.length != expected)
            throw new IllegalArgumentException("Expected " + expected
                    + " DB bytes, got " + (bytes == null ? 0 : bytes.length));
        for (int value : bytes)
            if (value < 0 || value > 255)
                throw new IllegalArgumentException("DB value must be 0..255");
    }

    private int exportedByte(int byteX, int y, boolean maskLayer) {
        int value = 0;
        for (int bit = 0; bit < 8; bit++) {
            boolean on = maskLayer ? getMask(byteX * 8 + bit, y)
                    : getPixel(byteX * 8 + bit, y);
            if (on) value |= 0x80 >> bit;
        }
        return value;
    }

    public int pixelByte(int byteX, int y) { return exportedByte(byteX, y, false); }
    public int maskByte(int byteX, int y) { return exportedByte(byteX, y, true); }

    private int[] exportBytes(boolean maskLayer) {
        int[] bytes = new int[cellsX() * height];
        int i = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < cellsX(); x++)
                bytes[i++] = exportedByte(x, y, maskLayer);
        return bytes;
    }

    public int[] bitmapBytes() { return exportBytes(false); }
    public int[] maskBytes() { return exportBytes(true); }

    public int[] attributeBytes() {
        int[] bytes = new int[cellsX() * cellsY()];
        for (int y = 0; y < cellsY(); y++)
            for (int x = 0; x < cellsX(); x++)
                bytes[y * cellsX() + x] = attributes[y][x];
        return bytes;
    }

    public String dbPixels(boolean binary) { return toDb(bitmapBytes(), cellsX(), binary); }
    public String dbMask(boolean binary) { return toDb(maskBytes(), cellsX(), binary); }
    public String dbAttributes(boolean binary) { return toDb(attributeBytes(), cellsX(), binary); }

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

    /** Standard Spectrum palette. BRIGHT is shared for INK and PAPER. */
    public static int palette(int color) {
        int base = color & 7;
        int level = (color & 8) != 0 ? 255 : 205;
        int red = (base & 2) != 0 ? level : 0;
        int green = (base & 4) != 0 ? level : 0;
        int blue = (base & 1) != 0 ? level : 0;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public int displayedColor(int x, int y) {
        int attr = attribute(x / 8, y / 8);
        int color = getPixel(x, y) ? (attr & 7) : ((attr >>> 3) & 7);
        return palette(color | ((attr & 0x40) != 0 ? 8 : 0));
    }
}
