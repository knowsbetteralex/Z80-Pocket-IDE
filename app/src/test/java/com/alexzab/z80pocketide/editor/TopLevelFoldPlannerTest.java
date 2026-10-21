package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class TopLevelFoldPlannerTest {
    @Test public void collapsesFirstLabelAndIncludesNestedLabelsWithinParagraph() {
        String s = "MAIN:\n  LD A,1\nINNER:\n  INC A\n\nNEXT:\n  RET\n";
        List<TopLevelFoldPlanner.Range> ranges = TopLevelFoldPlanner.plan(s);
        assertEquals(2, ranges.size());
        assertEquals("  LD A,1\nINNER:\n  INC A\n",
                s.substring(ranges.get(0).bodyStart, ranges.get(0).bodyEnd));
        assertEquals("  RET\n",
                s.substring(ranges.get(1).bodyStart, ranges.get(1).bodyEnd));
    }

    @Test public void commentsBeforeLabelStillFindTopMostLabel() {
        String s = "; routine\n; metadata\nFIRST: ; comment\n DB $01\nNEST:\n RET\n\n";
        List<TopLevelFoldPlanner.Range> ranges = TopLevelFoldPlanner.plan(s);
        assertEquals(1, ranges.size());
        assertEquals(" DB $01\nNEST:\n RET\n",
                s.substring(ranges.get(0).bodyStart, ranges.get(0).bodyEnd));
    }

    @Test public void neverFoldsDanglingLabelsOrInstructionsWithoutLabels() {
        assertTrue(TopLevelFoldPlanner.plan("ONE:\n\nTWO:\n").isEmpty());
        assertTrue(TopLevelFoldPlanner.plan("  LD A,1\n  RET\n\n").isEmpty());
    }

    @Test public void tabsAndBlankLinesSeparateParagraphs() {
        String s = "TOP:\n\tNOP\n \t \nSECOND:\n  INC A\n\nTHIRD:\n  RET";
        List<TopLevelFoldPlanner.Range> ranges = TopLevelFoldPlanner.plan(s);
        assertEquals(3, ranges.size());
        assertEquals("\tNOP\n", s.substring(ranges.get(0).bodyStart,
                ranges.get(0).bodyEnd));
        assertEquals("  INC A\n", s.substring(ranges.get(1).bodyStart,
                ranges.get(1).bodyEnd));
        assertEquals("  RET", s.substring(ranges.get(2).bodyStart,
                ranges.get(2).bodyEnd));
    }

    @Test public void plannerReturnsOriginalRangesInAscendingOrder() {
        String s = "MAIN:\n  JP A\nSUB:\n  RET\n\nFINAL:\n  RET";
        List<TopLevelFoldPlanner.Range> ranges = TopLevelFoldPlanner.plan(s);
        assertEquals(2, ranges.size());
        assertTrue(ranges.get(0).bodyEnd <= ranges.get(1).bodyStart);
        assertEquals(s.indexOf("  JP A"), ranges.get(0).bodyStart);
    }

    @Test public void handlesNoTrailingNewline() {
        String s = "START:\n LD A,1";
        List<TopLevelFoldPlanner.Range> ranges = TopLevelFoldPlanner.plan(s);
        assertEquals(1, ranges.size());
        assertEquals(" LD A,1", s.substring(ranges.get(0).bodyStart,
                ranges.get(0).bodyEnd));
    }
}
