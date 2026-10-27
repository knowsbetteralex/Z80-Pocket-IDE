package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The old settings constructor and independently switchable helpers coexist. */
public final class EditorPreferencesSnapshotTest {
    @Test public void oldSnapshotDefaultsNewFeaturesToEnabled() {
        EditorPreferences.Snapshot old = new EditorPreferences.Snapshot(
                true, false, true, 4, true, true, true);
        assertTrue(old.quickSymbols);
        assertTrue(old.labelCompletion);
    }

    @Test public void helpersCanBeDisabledIndependently() {
        EditorPreferences.Snapshot noSymbols = new EditorPreferences.Snapshot(
                true, false, true, 4, true, true, true, false, true);
        assertFalse(noSymbols.quickSymbols);
        assertTrue(noSymbols.labelCompletion);
        EditorPreferences.Snapshot noCompletion = new EditorPreferences.Snapshot(
                true, false, true, 4, true, true, true, true, false);
        assertTrue(noCompletion.quickSymbols);
        assertFalse(noCompletion.labelCompletion);
    }
}
