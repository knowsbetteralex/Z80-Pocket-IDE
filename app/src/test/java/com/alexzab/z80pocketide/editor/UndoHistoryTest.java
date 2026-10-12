package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UndoHistoryTest {
    @Test
    public void mergesFastTypingAndRestoresItInOneUndo() {
        UndoHistory history = new UndoHistory();
        history.record(0, "", "L", 0, 1, 1000, false);
        history.record(1, "", "D", 1, 2, 1100, false);
        history.record(2, "", " ", 2, 3, 1200, false);
        assertTrue(history.canUndo());
        UndoHistory.Group action = history.takeUndo();
        assertEquals(3, action.changes.size());
        assertEquals(0, action.cursorBefore);
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());
        assertEquals(3, history.takeRedo().changes.size());
        assertFalse(history.canRedo());
    }

    @Test
    public void undoThenTypeClearsRedoButNotEarlierHistory() {
        UndoHistory history = new UndoHistory();
        history.record(0, "", "A", 0, 1, 1000, false);
        history.record(1, "", "B", 1, 2, 3000, false);
        history.takeUndo();
        assertTrue(history.canRedo());
        history.record(1, "", "C", 1, 2, 3100, false);
        assertFalse(history.canRedo());
        assertTrue(history.canUndo());
        assertEquals("C", history.takeUndo().changes.get(0).after);
        assertEquals("A", history.takeUndo().changes.get(0).after);
    }

    @Test
    public void automaticCapitalizationStaysWithTypedCharacter() {
        UndoHistory history = new UndoHistory();
        history.record(0, "", "l", 0, 1, 1000, false);
        history.record(0, "l", "L", 1, 1, 1001, true);
        assertEquals(2, history.takeUndo().changes.size());
        assertFalse(history.canUndo());
    }

    @Test
    public void saveCheckpointSurvivesUndoRedo() {
        UndoHistory history = new UndoHistory();
        history.record(0, "", "A", 0, 1, 1000, false);
        history.markSaved();
        history.record(1, "", "B", 1, 2, 1050, false);
        assertFalse(history.isAtSavePoint());
        assertEquals(1, history.takeUndo().changes.size());
        assertTrue(history.isAtSavePoint());
        history.takeRedo();
        assertFalse(history.isAtSavePoint());
    }

    @Test
    public void separateDocumentsHaveSeparateTimelines() {
        EditorDocument a = new EditorDocument("a.asm", "", null, false);
        EditorDocument b = new EditorDocument("b.asm", "", null, false);
        a.history.record(0, "", "X", 0, 1, 1000, false);
        assertTrue(a.history.canUndo());
        assertFalse(b.history.canUndo());
    }

    @Test
    public void explicitFormatChangesAreSeparateFromLastTyping() {
        UndoHistory history = new UndoHistory();
        history.record(0, "", "l", 0, 1, 1000, false);
        history.separateNext();
        history.record(0, "l", "LD", 1, 2, 1001, true);
        assertEquals(1, history.takeUndo().changes.size());
        assertEquals(1, history.takeUndo().changes.size());
    }
}
