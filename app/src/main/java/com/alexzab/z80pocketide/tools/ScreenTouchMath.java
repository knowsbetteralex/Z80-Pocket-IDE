package com.alexzab.z80pocketide.tools;

/** Pure hit testing for the 32x24 byte/attribute grid and 8x8 pixel detail. */
public final class ScreenTouchMath {
    private ScreenTouchMath() {}

    private static int index(float fraction, int count) {
        if (Float.isNaN(fraction) || Float.isInfinite(fraction))
            throw new IllegalArgumentException("Invalid screen coordinate");
        return Math.max(0, Math.min(count - 1, (int) Math.floor(fraction * count)));
    }

    public static int column(float normalizedX) { return index(normalizedX, 32); }
    public static int characterRow(float normalizedY) { return index(normalizedY, 24); }
    public static int bit(int pixelFractionX) {
        return Math.max(0, Math.min(7, pixelFractionX));
    }
    public static int bit(float normalizedX) { return index(normalizedX, 8); }
    public static int pixelLine(float normalizedY) { return index(normalizedY, 8); }
    public static int bitMask(int bit) {
        if (bit < 0 || bit > 7)
            throw new IllegalArgumentException("Bit index must be 0..7");
        return 0x80 >>> bit;
    }
}
