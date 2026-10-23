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

    @Test
    public void foldedDeclarationsStillColorReferencesOutsideCollapsedBody() {
        String source = "OUTER:\n"
                + "    JR NESTED\n"
                + "NESTED:\n"
                + "    INC B\n"
                + "\n"
                + "MAIN:\n"
                + "    CALL NESTED\n";
        String visible = "OUTER:\n"
                + "    ; ▶ 3 lines · fold#1\n"
                + "\n"
                + "MAIN:\n"
                + "    CALL NESTED\n";
        Set<String> allLabels = SourceSymbols.definedLabels(source);
        assertTrue(allLabels.contains("NESTED"));
        int[] pos = new int[]{visible.lastIndexOf("NESTED"),
                visible.lastIndexOf("NESTED") + "NESTED".length()};
        boolean found = false;
        for (int[] range : SourceSymbols.labelRanges(visible, allLabels))
            if (range[0] == pos[0] && range[1] == pos[1]) found = true;
        assertTrue("Reference to label hidden by folding must stay highlighted", found);
        assertFalse("Marker text must not become a symbol",
                SourceSymbols.definedLabels(visible).contains("FOLD"));
    }

    @Test
    public void nestedDefinitionRemainsVisibleInUnfoldedSource() {
        String original = "START:\n    JP INNER\nINNER:\n    RET\n";
        String folded = "START:\n    ; ▶ 3 lines · fold#7\n";
        Set<String> definitions = SourceSymbols.definedLabels(original);
        assertTrue(definitions.contains("INNER"));
        assertFalse(SourceSymbols.definedLabels(folded).contains("INNER"));
        assertEquals(1, SourceSymbols.labelRanges(folded, definitions).length);
    }
}
