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
}
