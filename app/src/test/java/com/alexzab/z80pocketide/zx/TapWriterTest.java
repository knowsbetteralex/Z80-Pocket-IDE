package com.alexzab.z80pocketide.zx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TapWriterTest {
    @Test
    public void autorunTapStartsWithBasicProgramAndContainsCodeFile() {
        byte[] tap = new TapWriter().programTap("DEMO", 0x8000,
                new byte[] { 0x00, 0x18, (byte) 0xFD });

        int firstBlockLength = u16(tap, 0);
        assertEquals(19, firstBlockLength);
        assertEquals(0, tap[2] & 0xFF); // header flag
        assertEquals(0, tap[3] & 0xFF); // PROGRAM type
        assertEquals(10, u16(tap, 16)); // autostart line

        int basicDataOffset = 2 + firstBlockLength;
        int basicDataLength = u16(tap, basicDataOffset);
        int codeHeaderOffset = basicDataOffset + 2 + basicDataLength;

        assertTrue(codeHeaderOffset + 3 < tap.length);
        assertEquals(19, u16(tap, codeHeaderOffset));
        assertEquals(0, tap[codeHeaderOffset + 2] & 0xFF);
        assertEquals(3, tap[codeHeaderOffset + 3] & 0xFF); // CODE type
    }

    private static int u16(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }
}
