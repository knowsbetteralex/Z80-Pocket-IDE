package com.alexzab.z80pocketide.zx;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import java.util.Arrays;

public final class SnaWriterTest {
    private final SnaWriter writer = new SnaWriter();

    private static int word(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset + 1] & 255) << 8);
    }

    @Test public void writes48kHeaderRegistersPcAndCodeAtCorrectAddress() {
        byte[] snap = writer.programSna(0x8000, new byte[]{
                (byte) 0x3E, 0x02, (byte) 0xC9}, null);
        assertEquals(49179, snap.length);
        assertEquals(0x3F, snap[0] & 255);
        assertEquals(0x5C3A, word(snap, 15));
        assertEquals(0x04, snap[19] & 255);
        assertEquals(1, snap[25] & 255);
        assertEquals(0, snap[26] & 255);
        assertEquals(0xFFFC, word(snap, 23));
        assertEquals(0x8000, word(snap, 27 + 0xFFFC - 0x4000));
        assertEquals(0x3E, snap[27 + 0x8000 - 0x4000] & 255);
        assertEquals(2, snap[27 + 0x8001 - 0x4000] & 255);
        assertEquals(0xC9, snap[27 + 0x8002 - 0x4000] & 255);
        assertEquals(0x38, snap[27 + 6144] & 255);
        assertEquals(0, snap[27] & 255);
    }

    @Test public void includesExactScreenAndRejectsOverlappingCode() {
        byte[] screen = new byte[6912];
        for (int i = 0; i < screen.length; i++) screen[i] = (byte) (i * 7);
        byte[] snap = writer.programSna(0x6000, new byte[]{0x00}, screen);
        assertArrayEquals(screen, Arrays.copyOfRange(snap, 27, 27 + 6912));
        assertThrows(IllegalArgumentException.class,
                () -> writer.programSna(0x4000, new byte[]{0}, screen));
        assertThrows(IllegalArgumentException.class,
                () -> writer.programSna(0x8000, new byte[]{0}, new byte[6144]));
    }

    @Test public void choosesUnusedStackWordWhenCodeReachesRamTop() {
        byte[] code = new byte[0x10000 - 0xFFFC];
        Arrays.fill(code, (byte) 0x76);
        byte[] snap = writer.programSna(0xFFFC, code, null);
        int stack = word(snap, 23);
        assertEquals(0xFFFA, stack);
        assertEquals(0xFFFC, word(snap, 27 + stack - 0x4000));
        assertEquals(0x76, snap[27 + 0xFFFC - 0x4000] & 255);
        assertEquals(0x76, snap[27 + 0xFFFF - 0x4000] & 255);
    }

    @Test public void rejectsCodeOutside48kRamAndInsufficientStackSpace() {
        assertThrows(IllegalArgumentException.class,
                () -> writer.programSna(0x3FFF, new byte[]{0}, null));
        assertThrows(IllegalArgumentException.class,
                () -> writer.programSna(0xFFFE, new byte[]{0, 0, 0}, null));
        assertThrows(IllegalArgumentException.class,
                () -> writer.programSna(0x8000, new byte[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> writer.programSna(0x5B00, new byte[0x10000 - 0x5B00], null));
    }
}
