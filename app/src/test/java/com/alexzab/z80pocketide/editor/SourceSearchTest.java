package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class SourceSearchTest {
    @Test public void findsForwardAndBackwardMatchesWithWrap() {
        String source = "LOOP:\n  JR LOOP\n  NOP\n  JR LOOP";
        List<SourceSearch.Match> matches = SourceSearch.findAll(source, "LOOP", true);
        assertEquals(3, matches.size());
        assertEquals(0, matches.get(0).start);
        assertEquals(1, SourceSearch.next(matches, 1));
        assertEquals(0, SourceSearch.next(matches, source.length()));
        assertEquals(2, SourceSearch.previous(matches, source.length()));
        assertEquals(2, SourceSearch.previous(matches, 0));
    }

    @Test public void defaultSearchIsCaseInsensitiveButToggleCanMatchCase() {
        String text = "start START Start starting";
        assertEquals(4, SourceSearch.findAll(text, "start", false).size());
        assertEquals(1, SourceSearch.findAll(text, "Start", true).size());
    }

    @Test public void replacementIsLiteralAndCanBeEmpty() {
        String source = "JR (LOOP); JR (LOOP)";
        List<SourceSearch.Match> matches = SourceSearch.findAll(source, "(LOOP)", true);
        assertEquals("JR $AB; JR $AB",
                SourceSearch.replaceAll(source, matches, "$AB"));
        assertEquals("JR ; JR ",
                SourceSearch.replaceAll(source, matches, ""));
    }

    @Test public void replacementDoesNotRecurseIntoInsertedText() {
        String source = "A A";
        List<SourceSearch.Match> matches = SourceSearch.findAll(source, "A", true);
        assertEquals("AA AA", SourceSearch.replaceAll(source, matches, "AA"));
        assertEquals(2, matches.size());
    }

    @Test public void emptySearchAndNullSourceAreSafe() {
        assertTrue(SourceSearch.findAll("test", "", false).isEmpty());
        assertTrue(SourceSearch.findAll(null, "a", false).isEmpty());
        assertEquals(-1, SourceSearch.next(SourceSearch.findAll("abc", "z", false), 0));
    }

    @Test public void unicodeOffsetsRemainValidForSpannable() {
        String source = "😀; LOOP\nJP LOOP";
        List<SourceSearch.Match> matches = SourceSearch.findAll(source, "LOOP", true);
        assertEquals(source.indexOf("LOOP"), matches.get(0).start);
        assertEquals(source.lastIndexOf("LOOP"), matches.get(1).start);
    }
}
