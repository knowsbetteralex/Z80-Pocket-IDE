package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class LabelCompletionTest {
    private static final String SOURCE =
            "SPRITE:\n    DB 0\n"
            + "Start:\n    JP SPRITE\n"
            + "INNER_LOOP:\n    DJNZ INNER_LOOP\n"
            + "SecondSprite EQU $6000\n";

    @Test public void suggestsDeclaredLabelsByPrefixIgnoringCase() {
        String line = "    LD HL,spr";
        List<LabelCompletion.Match> found =
                LabelCompletion.suggest(SOURCE, line, line.length(), 7);
        assertEquals(1, found.size());
        assertEquals("SPRITE", found.get(0).label);
        assertEquals(line.indexOf("spr"), found.get(0).start);
        assertEquals(line.length(), found.get(0).end);
    }

    @Test public void preservesOriginalCaseAndEquLabels() {
        String line = "    JP sec";
        List<LabelCompletion.Match> found =
                LabelCompletion.suggest(SOURCE, line, line.length(), 5);
        assertEquals("SecondSprite", found.get(0).label);
        assertEquals("Start", LabelCompletion.suggest(SOURCE,
                "    CALL Sta", "    CALL Sta".length(), 5).get(0).label);
    }

    @Test public void finishesWholeTokenWhenCaretIsInTheMiddle() {
        String line = "    JP INNEsuffix";
        List<LabelCompletion.Match> found = LabelCompletion.suggest(
                SOURCE, line, "    JP INNE".length(), 7);
        assertEquals("INNER_LOOP", found.get(0).label);
        assertEquals(line.length(), found.get(0).end);
        assertEquals(line.indexOf("INNE"), found.get(0).start);
    }

    @Test public void respectsCommentsStringsAndDeclarations() {
        assertTrue(LabelCompletion.suggest(SOURCE,
                "    ; JP SPR", "    ; JP SPR".length(), 7).isEmpty());
        assertTrue(LabelCompletion.suggest(SOURCE,
                "    DB \"SPR", "    DB \"SPR".length(), 7).isEmpty());
        assertTrue(LabelCompletion.suggest(SOURCE,
                "    DB 'SPR", "    DB 'SPR".length(), 7).isEmpty());
        assertTrue(LabelCompletion.suggest(SOURCE, "SPR",
                3, 7).isEmpty());
        assertTrue(LabelCompletion.suggest(SOURCE, "    SPRITE:",
                "    SPRITE".length(), 7).isEmpty());
    }

    @Test public void hidesExactNamesAndSingleCharacters() {
        assertTrue(LabelCompletion.suggest(SOURCE,
                "    JP SPRITE", "    JP SPRITE".length(), 7).isEmpty());
        assertTrue(LabelCompletion.suggest(SOURCE,
                "    JP S", "    JP S".length(), 7).isEmpty());
    }

    @Test public void usesFullSourceEvenIfDeclarationIsFolded() {
        String line = "    JP inn";
        List<LabelCompletion.Match> found =
                LabelCompletion.suggest(SOURCE, line, line.length(), 7);
        assertEquals("INNER_LOOP", found.get(0).label);
    }

    @Test public void doesNotDuplicateCaseVariants() {
        List<String> labels = SourceSymbols.definedLabelNames(
                "HELLO:\nhello:\nHELLO EQU 1\nWORLD:\n");
        assertEquals(2, labels.size());
        assertEquals("HELLO", labels.get(0));
        assertEquals("WORLD", labels.get(1));
    }

    @Test public void capsSuggestionsAndHandlesInvalidInput() {
        String source="START:\nSTARDUST:\nSTARSHIP:\nSTARFISH:\n";
        List<LabelCompletion.Match> found =
                LabelCompletion.suggest(source, "    JP sta", 10, 2);
        assertEquals(2, found.size());
        assertTrue(LabelCompletion.suggest(source, null, 0, 5).isEmpty());
        assertTrue(LabelCompletion.suggest(source, "    JP sta", 20, 5).isEmpty());
        assertTrue(LabelCompletion.suggest(source, "    JP sta", 10, 0).isEmpty());
    }

    @Test public void labelMatchingSkipsStringAndCommentDefinitions() {
        String source = "; FAKE:\n    DB \"NOPE:\"\nREAL:\n    JR REAL\n";
        List<LabelCompletion.Match> found = LabelCompletion.suggest(source,
                "    JR fa", "    JR fa".length(), 6);
        assertTrue(found.isEmpty());
        assertFalse(LabelCompletion.suggest(source,
                "    JR re", "    JR re".length(), 6).isEmpty());
    }
}
