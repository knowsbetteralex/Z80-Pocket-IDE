package com.alexzab.z80pocketide.project;

import static org.junit.Assert.*;
import org.junit.Test;

public final class SpectrumScreenTest {
    @Test public void mapsBitmapRowAndAttributes() {
        byte[] screen=new byte[SpectrumScreen.LENGTH];
        screen[0]=(byte)0x80;
        screen[6144]=(byte)0x47; // BRIGHT, paper 0, ink 7
        screen[32]=1;            // y=8, rightmost pixel of first byte
        screen[6144+32]=(byte)0x0A; // normal: blue paper, red ink
        int[] colors=SpectrumScreen.decodePixels(screen);
        assertEquals(0xFFFFFFFF,colors[0]);
        assertEquals(0xFF000000,colors[1]);
        assertEquals(0xFF000000,colors[256]);
        assertEquals(0xFF0000D7,colors[8*256+0]);
        assertEquals(0xFFD70000,colors[8*256+7]);
    }

    @Test public void rejectsIncorrectImageSizes() {
        assertThrows(IllegalArgumentException.class,
                ()->SpectrumScreen.decodePixels(new byte[6144]));
        assertThrows(IllegalArgumentException.class,
                ()->SpectrumScreen.decodePixels(new byte[6913]));
    }

    @Test public void flashSwapsInkAndPaperOnlyForFlashTile() {
        byte[] bytes=new byte[SpectrumScreen.LENGTH];
        bytes[0]=(byte)0x80;
        bytes[6144]=(byte)(0x80|0x40|0x02); // flashing bright red on black
        bytes[1]=(byte)0x80;
        bytes[6145]=(byte)0x47; // non-flashing bright white
        int[] normal=SpectrumScreen.decodePixels(bytes,false);
        int[] flashed=SpectrumScreen.decodePixels(bytes,true);
        assertEquals(0xFFFF0000,normal[0]);
        assertEquals(0xFF000000,flashed[0]);
        assertEquals(0xFF000000,normal[1]);
        assertEquals(0xFFFF0000,flashed[1]);
        assertEquals(0xFFFFFFFF,normal[8]);
        assertEquals(normal[8],flashed[8]);
        assertEquals(bytes[6144]&255,0xC2);
    }
}
