package com.alexzab.z80pocketide.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class SpectrumKeyboardMatrixTest {
    @Test public void exactlyFortyKeysWithUniqueHalfRowBits() {
        Set<String> unique = new HashSet<>();
        int count = 0;
        for (String[] physicalRow : SpectrumKeyboardMatrix.PHYSICAL) {
            assertEquals(10, physicalRow.length);
            for (String key : physicalRow) {
                assertTrue(unique.add(key));
                int row = SpectrumKeyboardMatrix.rowOf(key);
                int bit = SpectrumKeyboardMatrix.bitOf(key);
                assertEquals(key, SpectrumKeyboardMatrix.key(row, bit));
                count++;
            }
        }
        assertEquals(40, count);
    }

    @Test public void standardPortAddressesInBitOrder() {
        int[] expected = {0xFEFE,0xFDFE,0xFBFE,0xF7FE,0xEFFE,0xDFFE,0xBFFE,0x7FFE};
        for (int r=0; r<8; r++) {
            assertEquals(expected[r], SpectrumKeyboardMatrix.port(r));
            assertEquals(r, SpectrumKeyboardMatrix.rowFromPort(expected[r]));
        }
        assertEquals(0, SpectrumKeyboardMatrix.bitOf("CAPS SHIFT"));
        assertEquals(4, SpectrumKeyboardMatrix.bitOf("V"));
        assertEquals(0, SpectrumKeyboardMatrix.bitOf("ENTER"));
        assertEquals(1, SpectrumKeyboardMatrix.bitOf("SYMBOL SHIFT"));
        assertEquals(0, SpectrumKeyboardMatrix.bitOf("SPACE"));
        assertEquals(0, SpectrumKeyboardMatrix.bitOf("0"));
        assertEquals(4, SpectrumKeyboardMatrix.bitOf("6"));
    }

    @Test public void activeLowSingleKeyAndMultiKeyValues() {
        SpectrumKeyboardMatrix m = new SpectrumKeyboardMatrix();
        for (int row=0; row<8; row++) assertEquals(0xFF, m.readByte(row));
        m.toggle("3");
        assertEquals(0xFB, m.readByte(3));
        m.toggle("3");
        assertEquals(0xFF, m.readByte(3));
        m.toggle("1");
        m.toggle("5");
        assertEquals(0xEE, m.readByte(3));
        m.toggle("SPACE");
        assertEquals(0xFE, m.readByte(7));
        assertEquals(0xEE, m.readByte(3)); // Other half-rows stay independent
        assertTrue(m.isPressed("SPACE"));
        assertFalse(m.isPressed("SYMBOL SHIFT"));
        assertEquals(2, m.pressedKeys(3).size());
    }

    @Test public void decodePortAndByteReplacesSelection() {
        SpectrumKeyboardMatrix m = new SpectrumKeyboardMatrix();
        m.toggle("CAPS SHIFT");
        int row = m.loadPortByte(0xF7FE, 0xEE);
        assertEquals(3, row);
        assertTrue(m.isPressed("1"));
        assertTrue(m.isPressed("5"));
        assertFalse(m.isPressed("CAPS SHIFT"));
        assertEquals(0xEE, m.readByte(row));
        assertEquals(0xFF, m.readByte(0));
        row = m.loadPortByte(0x7FFE, 0x00);
        assertEquals(7, row);
        assertEquals(0xE0, m.readByte(row));
        assertTrue(m.isPressed("SPACE"));
        assertTrue(m.isPressed("B"));
    }

    @Test public void highBitsOnReadAreNotKeyboardBits() {
        SpectrumKeyboardMatrix m = new SpectrumKeyboardMatrix();
        m.loadPortByte(0xFDFE, 0x3B); // 0011 1011, D6 low but no associated key
        assertEquals(0xFB, m.readByte(1));
        assertTrue(m.isPressed("D")); // bit 2 low
        assertFalse(m.isPressed("A"));
    }

    @Test public void hexBinParsingAndAsmSnippet() {
        assertEquals(0xF7FE, SpectrumKeyboardMatrix.parseHex("0xf7fe", 65535));
        assertEquals(0xFB, SpectrumKeyboardMatrix.parseHex("$FB", 255));
        assertEquals(0xEE, SpectrumKeyboardMatrix.parseBinaryByte("%1110 1110"));
        assertEquals("$F7FE", SpectrumKeyboardMatrix.hexPort(0xF7FE));
        assertEquals("$EE", SpectrumKeyboardMatrix.hexByte(0xEE));
        assertEquals("%11101110", SpectrumKeyboardMatrix.binByte(0xEE));
        String snippet = SpectrumKeyboardMatrix.asmSnippet(3,2);
        assertTrue(snippet.contains("LD BC,$F7FE"));
        assertTrue(snippet.contains("IN A,(C)"));
        assertTrue(snippet.contains("BIT 2,A"));
        assertTrue(snippet.contains("; 3"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsOtherPorts() {
        SpectrumKeyboardMatrix.rowFromPort(0x00FE);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidByte() {
        new SpectrumKeyboardMatrix().loadPortByte(0xFEFE, 0x100);
    }
}
