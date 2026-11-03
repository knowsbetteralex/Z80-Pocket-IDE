package com.alexzab.z80pocketide.screen;
import com.alexzab.z80pocketide.project.SpectrumScreenDocument;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ScreenEditHistoryTest {
    @Test public void strokeIsSingleUndoWithCorrectSavedRevision() {
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        ScreenEditHistory history=new ScreenEditHistory(doc.toRaw());
        history.begin(doc);
        for(int x=0;x<100;x++)doc.setPixel(x,1,true);
        assertTrue(history.end(doc));
        assertTrue(history.isDirty(doc));
        assertTrue(history.undo(doc));
        assertFalse(history.isDirty(doc));
        assertFalse(doc.pixel(99,1));
        assertTrue(history.redo(doc));
        assertTrue(doc.pixel(99,1));
        history.markSaved(doc);
        assertFalse(history.isDirty(doc));
    }
    @Test public void repeatedNoopStrokeDoesNotConsumeUndo() {
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        ScreenEditHistory history=new ScreenEditHistory(doc.toRaw());
        history.begin(doc);
        doc.setPixel(0,0,false);
        assertFalse(history.end(doc));
        assertFalse(history.canUndo());
    }
}
