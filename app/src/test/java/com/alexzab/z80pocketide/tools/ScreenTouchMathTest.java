package com.alexzab.z80pocketide.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Test;

public final class ScreenTouchMathTest {
    @Test public void picksScreenCornersAndThirdBoundaries() {
        assertEquals(0, ScreenTouchMath.column(0f));
        assertEquals(0, ScreenTouchMath.characterRow(0f));
        assertEquals(31, ScreenTouchMath.column(1f));
        assertEquals(23, ScreenTouchMath.characterRow(1f));
        assertEquals(7, ScreenTouchMath.characterRow(7.5f / 24f));
        assertEquals(8, ScreenTouchMath.characterRow(8.5f / 24f));
        assertEquals(16, ScreenTouchMath.characterRow(16.5f / 24f));
        assertEquals(31, ScreenTouchMath.column(31.9f / 32f));
    }

    @Test public void pickedCellLinksToOriginalSpectrumAddressCalculations() {
        int col = ScreenTouchMath.column(19.5f / 32f);
        int row = ScreenTouchMath.characterRow(11.5f / 24f);
        int line = ScreenTouchMath.pixelLine(4.5f / 8f);
        ScreenAddress address = ScreenAddress.fromAttributeCoordinates(row, col, line);
        assertEquals(19, col);
        assertEquals(11, row);
        assertEquals(4, line);
        assertEquals(0x5800 + 11 * 32 + 19, address.attributeAddress());
        assertEquals(0x4800 + (4 << 8) + (3 << 5) + 19, address.pixelAddress());
    }

    @Test public void scanlineAndBitTrackEightPixelsPrecisely() {
        for(int i = 0; i < 8; i++) {
            float fraction = (i + .5f) / 8f;
            assertEquals(i, ScreenTouchMath.pixelLine(fraction));
            assertEquals(i, ScreenTouchMath.bit(fraction));
            assertEquals(0x80 >>> i, ScreenTouchMath.bitMask(i));
        }
        assertEquals(0, ScreenTouchMath.pixelLine(-2f));
        assertEquals(7, ScreenTouchMath.bit(5f));
        assertEquals(7, ScreenTouchMath.bit(10));
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidBitMaskRejected() {
        ScreenTouchMath.bitMask(8);
    }

    @Test(expected=IllegalArgumentException.class)
    public void invalidNormalizedCoordinateRejected() {
        ScreenTouchMath.column(Float.NaN);
    }
}
