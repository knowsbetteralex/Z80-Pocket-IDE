package com.alexzab.z80pocketide.editor;

import org.junit.Test;
import static org.junit.Assert.*;

public class QuoteTypingTest {
    @Test public void pairsOnlyOpeningDoubleQuotes() {
        assertTrue(QuoteTyping.shouldPair("LD A,\"", 5));
        assertFalse(QuoteTyping.shouldPair("DB \"hello\"", 9));
        assertFalse(QuoteTyping.shouldPair("DB \"x\"\"", 6));
        assertFalse(QuoteTyping.shouldPair("; comment \"", 10));
        assertTrue(QuoteTyping.shouldSkipClosing("DB \"text\"\"", 8));
    }

    @Test public void keepsTypedCaseInsideStringsWithUpperMode() {
        assertEquals("LD A,\"hello World\"",
                QuoteTyping.uppercaseOutsideLiterals("ld a,\"hello World\"", 0, 18));
        assertEquals("LD A,\"hello\"\nJR NZ,LOOP",
                QuoteTyping.uppercaseOutsideLiterals("ld a,\"hello\"\njr nz,loop", 0, 23));
        assertEquals("INCLUDE \"lib/sprites.asm\"",
                QuoteTyping.uppercaseOutsideLiterals("include \"lib/sprites.asm\"", 0, 25));
        assertEquals("DB 'a',\"Hello\"",
                QuoteTyping.uppercaseOutsideLiterals("db 'a',\"Hello\"", 0, 14));
    }

    @Test public void typedOneCharacterAtCursorUsesExistingQuotes() {
        String text = "LD A,\"abC\"";
        assertEquals("d", QuoteTyping.uppercaseOutsideLiterals("LD A,\"abdC\"", 7, 8));
        assertEquals("B", QuoteTyping.uppercaseOutsideLiterals("LD B,\"abdC\"", 3, 4));
        assertTrue(QuoteTyping.isInsideDoubleQuotes(text, 6));
        assertFalse(QuoteTyping.isInsideDoubleQuotes(text, 10));
    }

    @Test public void handlesRegisterApostropheWithoutOpeningString() {
        String asm = "EX AF,AF'\nDB \"Mixed\"";
        assertEquals("EX AF,AF'\nDB \"Mixed\"",
                QuoteTyping.uppercaseOutsideLiterals(asm, 0, asm.length()));
    }
}
