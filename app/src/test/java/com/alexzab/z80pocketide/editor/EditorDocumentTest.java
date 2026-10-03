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
}
