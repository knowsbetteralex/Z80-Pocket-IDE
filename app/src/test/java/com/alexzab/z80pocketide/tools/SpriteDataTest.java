package com.alexzab.z80pocketide.tools;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class SpriteDataTest {
    @Test public void spriteIsBigEndianWithinEachByte() {
        SpriteData sprite=new SpriteData(16,8);
        sprite.paint(0,0,15,SpriteData.Mode.INK);
        sprite.paint(7,0,15,SpriteData.Mode.INK);
        sprite.paint(8,0,15,SpriteData.Mode.INK);
        assertEquals(0x81,sprite.pixelByte(0,0));
        assertEquals(0x80,sprite.pixelByte(1,0));
        assertEquals(16,sprite.bitmapBytes().length);
        assertEquals(2,sprite.attributeBytes().length);
        assertTrue(sprite.dbPixels(false).startsWith("    DB $81,$80"));
        assertTrue(sprite.dbPixels(true).startsWith("    DB %10000001,%10000000"));
    }

    @Test public void attributesFollowHardwareColorLimits() {
        SpriteData s=new SpriteData(16,16);
        assertEquals(0x07,s.attribute(0,0));
        s.paint(2,5,10,SpriteData.Mode.INK); // bright red
        assertEquals(0x42,s.attribute(0,0));
        s.paint(2,5,13,SpriteData.Mode.PAPER); // bright cyan background
        assertEquals(0x6A,s.attribute(0,0));
        assertTrue(s.getPixel(2,5));
        s.paint(2,5,0,SpriteData.Mode.ERASE);
        assertFalse(s.getPixel(2,5));
        assertEquals(0x6A,s.attribute(0,0));
        assertEquals(4,s.attributeBytes().length);
    }

    @Test public void recoloursEveryCellButKeepsBitmapUntouched() {
        SpriteData s = new SpriteData(16, 16);
        s.setPixel(0, 0, true);
        s.setPixel(15, 15, true);
        s.setInkColor(10); // bright red; BRIGHT is shared across INK/PAPER.
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 2; col++)
                assertEquals(0x42, s.attribute(col, row));
        assertEquals(SpriteData.palette(10), s.displayedColor(0, 0));
        assertEquals(SpriteData.palette(8), s.displayedColor(2, 2));
        s.setPaperColor(13); // bright cyan; now all attributes should be 0x6A.
        for (int attr : s.attributeBytes()) assertEquals(0x6A, attr);
        assertEquals(SpriteData.palette(13), s.displayedColor(1, 0));
        assertEquals(SpriteData.palette(10), s.displayedColor(0, 0));
        assertTrue(s.getPixel(0, 0));
        assertTrue(s.getPixel(15, 15));
        assertTrue(s.dbAttributes(false).contains("$6A"));
        s.setPaperColor(5); // normal cyan dims INK too because BRIGHT is shared.
        for (int attr : s.attributeBytes()) assertEquals(0x2A, attr);
        assertEquals(SpriteData.palette(2), s.displayedColor(0, 0));
    }

    @Test public void strokeEraseAndClearOnlyPixelsKeepsColours() {
        SpriteData s = new SpriteData(16, 8);
        s.setInkColor(15);
        s.setPaperColor(8);
        s.setPixel(1, 2, true);
        s.setPixel(7, 2, true);
        assertEquals(0x40 | 0x07, s.attribute(0,0));
        s.setPixel(1, 2, false);
        assertFalse(s.getPixel(1, 2));
        assertTrue(s.getPixel(7, 2));
        s.clearPixels();
        assertFalse(s.getPixel(7, 2));
        assertEquals(0x47, s.attribute(0, 0));
        assertEquals(8, s.getPaperColor());
        assertEquals(15, s.getInkColor());
    }

    @Test(expected=IllegalArgumentException.class)
    public void widthMustBeMultipleOfEight(){new SpriteData(13,16);}
}
