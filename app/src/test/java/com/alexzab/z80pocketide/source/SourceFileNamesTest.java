package com.alexzab.z80pocketide.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SourceFileNamesTest {
    @Test
    public void preservesAsmSuffixWithoutAppendingTxt() {
        assertEquals("game.asm", SourceFileNames.suggestedName("game.asm"));
        assertEquals("sprite.ASM", SourceFileNames.suggestedName("sprite.ASM"));
        assertEquals("game.asm", SourceFileNames.suggestedName("game.asm.txt"));
        assertEquals("program.asm", SourceFileNames.suggestedName(""));
        assertEquals("game.asm", SourceFileNames.suggestedName("game"));
    }

    @Test
    public void recognizesProviderAddedTextSuffix() {
        assertTrue(SourceFileNames.hasUnwantedTextSuffix("game.asm.txt"));
        assertTrue(SourceFileNames.hasUnwantedTextSuffix("GAME.ASM.TXT"));
        assertFalse(SourceFileNames.hasUnwantedTextSuffix("game.asm"));
        assertFalse(SourceFileNames.hasUnwantedTextSuffix(null));
    }
}
