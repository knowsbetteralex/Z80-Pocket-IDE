package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.Set;

public class SourceSymbolsTest {
    @Test
    public void recognisesForwardAndEquLabelsCaseInsensitively() {
        String code = "ORG $8000\n"
                + "    jr nz,loop\n"
                + "    ld hl,SPRITE\n"
                + "LOOP:\n"
                + "    djnz LOOP\n"
                + "SPRITE: DB 1,2\n"
                + "LIMIT EQU 32\n"
                + "    LD B,limit\n";
        Set<String> labels = SourceSymbols.definedLabels(code);
        assertTrue(labels.contains("LOOP"));
        assertTrue(labels.contains("SPRITE"));
        assertTrue(labels.contains("LIMIT"));
        assertEquals(7, SourceSymbols.labelRanges(code).length);
    }

    @Test
    public void ignoresFakeSymbolsInStringsAndComments() {
        String code = "REAL:\n"
                + "    DB \"REAL: fake\",'REAL'\n"
                + "    JR REAL ; fake: REAL\n"
                + "    LD A,'x'\n";
        Set<String> labels = SourceSymbols.definedLabels(code);
        assertTrue(labels.contains("REAL"));
        assertFalse(labels.contains("FAKE"));
        assertEquals(2, SourceSymbols.labelRanges(code).length);
    }

    @Test
    public void masksLiteralButLeavesInstructionPositions() {
        String source = "    LD A,'C' ; LD A,5";
        String masked = SourceSymbols.maskLiterals(source);
        assertTrue(masked.contains("LD A,"));
        assertFalse(masked.contains(";"));
        assertEquals(source.length(), masked.length());
    }
}
