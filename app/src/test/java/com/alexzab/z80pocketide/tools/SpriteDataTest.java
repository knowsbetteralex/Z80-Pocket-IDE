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

    @Test(expected=IllegalArgumentException.class)
    public void widthMustBeMultipleOfEight(){new SpriteData(13,16);}
}
