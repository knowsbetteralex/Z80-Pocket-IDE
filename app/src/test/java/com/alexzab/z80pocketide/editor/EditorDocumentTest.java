package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EditorDocumentTest {
    @Test
    public void buildStateIsPerDocumentAndInvalidatesCleanly() {
        EditorDocument doc = new EditorDocument("test.asm", "NOP\n", null, false);
        assertFalse(doc.hasFile());
        assertFalse(doc.buildIsCurrent());

        doc.lastBuiltSource = "NOP\n";
        doc.lastTap = new byte[] {1, 2, 3};
        assertTrue(doc.buildIsCurrent());

        doc.text = "NOP\nRET\n";
        assertFalse(doc.buildIsCurrent());

        doc.invalidateBuild();
        assertFalse(doc.buildIsCurrent());
    }

    @Test
    public void nonEmptyUntitledMustAskBeforeClose() {
        EditorDocument untitled = new EditorDocument(
                "Untitled 1.asm", "ORG $8000\nNOP\n", null, false);
        assertTrue(untitled.needsCloseConfirmation());

        EditorDocument blankScratch = new EditorDocument(
                "Untitled 1.asm", "", null, false);
        assertFalse(blankScratch.needsCloseConfirmation());

        EditorDocument saved = new EditorDocument(
                "main.asm", "NOP\n", "content://files/main.asm", false);
        assertFalse(saved.needsCloseConfirmation());

        saved.dirty = true;
        assertTrue(saved.needsCloseConfirmation());
    }
}
