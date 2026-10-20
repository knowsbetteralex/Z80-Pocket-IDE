package com.alexzab.z80pocketide.tools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class SpriteImportTest {
    @Test public void parsesAssemblerDbHexBinAndDecimalComments() {
        assertArrayEquals(new int[]{0x3C,0x42,0xA5,0x81,0xFF,0},
                SpriteDbParser.parse("SPRITE: DB $3C, %01000010, 0xA5 ; comment\n"
                        + "DEFB #81,255,0\n"));
    }

    @Test public void supportsBareValuesAndMixedPrefixes() {
        assertArrayEquals(new int[]{60, 0x81, 0xFF, 0x3C},
                SpriteDbParser.parse("60 0b10000001 FFh 3Ch"));
        assertArrayEquals(new int[]{0xAA, 0x55},
                SpriteDbParser.parse("PixelData:\n.BYTE 0AAh, 0x55"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnsupportedAssemblerExpressions() {
        SpriteDbParser.parse("DB TABLE+1,$FF");
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsValuesExceedingAByte() {
        SpriteDbParser.parse("DB $100");
    }

    @Test public void bitmapAndMaskDbRoundTripForEditedImage() {
        SpriteData original = new SpriteData(16, 16);
        original.setPixel(0, 0, true);
        original.setPixel(15, 15, true);
        original.setPixel(6, 10, true);
        original.setMask(1, 0, false);
        original.setMask(15, 15, false);
        SpriteData copy = new SpriteData(16, 16);
        copy.loadBitmap(SpriteDbParser.parse(original.dbPixels(false)));
        copy.loadMask(SpriteDbParser.parse(original.dbMask(true)));
        assertArrayEquals(original.bitmapBytes(), copy.bitmapBytes());
        assertArrayEquals(original.maskBytes(), copy.maskBytes());
        assertFalse(copy.getMask(1, 0));
        assertTrue(copy.getPixel(0, 0));
    }

    @Test public void importedPerCellAttributesKeepAllColourAndFlashBits() {
        SpriteData s = new SpriteData(16, 16);
        int[] attrs = {0x47, 0x02, 0xAF, 0x51};
        s.loadAttributes(attrs);
        assertArrayEquals(attrs, s.attributeBytes());
        assertEquals(0x02, s.attribute(1, 0));
        assertEquals(0xAF, s.attribute(0, 1));
        assertEquals(SpriteData.palette(15), s.displayedColor(0, 0));
        assertEquals(SpriteData.palette(2), s.displayedColor(8, 0));
        assertEquals(15, s.getInkColor());
        // Changing palette applies globally while preserving imported FLASH.
        s.setInkColor(10);
        assertEquals(0x80, s.attribute(0, 1) & 0x80);
        assertEquals(0x02, s.attribute(1, 1) & 7);
    }

    @Test public void importOnlyOverwritesCurrentVisibleGrid() {
        SpriteData s = new SpriteData(16, 16);
        s.setPixel(0, 0, true);
        s.setMask(0, 0, false);
        s.resize(8, 8);
        int[] pixels = new int[8];
        int[] mask = new int[8];
        java.util.Arrays.fill(mask, 0xFF);
        s.loadBitmap(pixels);
        s.loadMask(mask);
        s.resize(16, 16);
        assertFalse(s.getPixel(0, 0));
        assertTrue(s.getMask(0, 0));
        s.setPixel(15, 15, true);
        s.resize(8, 8);
        s.resize(16, 16);
        assertTrue(s.getPixel(15, 15));
    }

    @Test public void invalidLengthDoesNotPartiallyChangeSprite() {
        SpriteData s = new SpriteData(16, 8);
        s.setPixel(0, 0, true);
        int[] invalid = {0x00, 0xFF};
        try {
            s.loadBitmap(invalid);
            throw new AssertionError("Wrong size accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("16"));
        }
        assertEquals(0x80, s.pixelByte(0, 0));
    }
}
