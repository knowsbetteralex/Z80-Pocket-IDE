package com.alexzab.z80pocketide.screen;

import com.alexzab.z80pocketide.project.SpectrumScreenDocument;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenSelectionTest {
    @Test public void pixelCopyAtArbitraryOffsetPreservesUnselectedPixels(){
        SpectrumScreenDocument screen=new SpectrumScreenDocument();
        screen.setPixel(3,9,true);screen.setPixel(4,10,true);
        screen.setPixel(200,130,true);
        ScreenSelectionClip clip=ScreenSelectionClip.capture(screen,3,9,4,3);
        assertTrue(clip.pixel(0,0));assertTrue(clip.pixel(1,1));
        assertFalse(clip.pixel(2,1));
        clip.place(screen,17,30,false);
        assertTrue(screen.pixel(17,30));assertTrue(screen.pixel(18,31));
        assertFalse(screen.pixel(19,31));
        assertTrue(screen.pixel(3,9));assertTrue(screen.pixel(200,130));
    }

    @Test public void attributeMovementIsAnchoredAtOriginalCharacterStart(){
        SpectrumScreenDocument original=new SpectrumScreenDocument();
        original.setAttribute(0,0,0x57);
        original.setAttribute(8,0,0x61);
        original.setAttribute(16,0,0x12);
        original.setPixel(3,0,true);
        ScreenSelectionClip clip=ScreenSelectionClip.capture(original,3,0,4,1);

        // x=3 selection: displacements +1..+7 must NOT move attr 0 to attr 1.
        for(int dx=1;dx<=7;dx++){
            SpectrumScreenDocument target=SpectrumScreenDocument.fromRaw(original.toRaw());
            clip.place(target,3+dx,0,true);
            assertEquals("dx="+dx,0x61,target.attribute(8,0));
            assertEquals("dx="+dx,0x57,target.attribute(0,0));
            assertTrue(target.pixel(3+dx,0));
        }
        SpectrumScreenDocument shifted=SpectrumScreenDocument.fromRaw(original.toRaw());
        clip.place(shifted,11,0,true); // +8 moves one full character
        assertEquals(0x57,shifted.attribute(8,0));
        assertEquals(0x57,shifted.attribute(0,0));
        assertEquals(0x12,shifted.attribute(16,0));
        SpectrumScreenDocument keep=SpectrumScreenDocument.fromRaw(original.toRaw());
        clip.place(keep,11,0,false);
        assertEquals(0x61,keep.attribute(8,0));
    }

    @Test public void attributesOfPartiallySelectedCellsMoveAsWholeTiles(){
        SpectrumScreenDocument screen=new SpectrumScreenDocument();
        screen.setAttribute(0,0,0x21);screen.setAttribute(8,0,0x62);
        screen.setAttribute(0,8,0x17);screen.setAttribute(8,8,0x48);
        ScreenSelectionClip clip=ScreenSelectionClip.capture(screen,6,6,5,5);
        SpectrumScreenDocument target=new SpectrumScreenDocument();
        clip.place(target,14,14,true); // both axes +8
        assertEquals(0x21,target.attribute(8,8));
        assertEquals(0x62,target.attribute(16,8));
        assertEquals(0x17,target.attribute(8,16));
        assertEquals(0x48,target.attribute(16,16));
    }

    @Test public void clipboardEncodingAndDecodingPreservePixelsAndAttrs() throws Exception{
        SpectrumScreenDocument screen=new SpectrumScreenDocument();
        screen.setAttribute(8,8,0x9B);
        screen.setPixel(9,9,true);
        ScreenSelectionClip clip=ScreenSelectionClip.capture(screen,9,9,11,12);
        ScreenSelectionClip loaded=ScreenSelectionClip.decode(clip.encode());
        assertEquals(9,loaded.x);assertEquals(12,loaded.height);
        assertTrue(loaded.pixel(0,0));assertFalse(loaded.pixel(1,0));
        SpectrumScreenDocument dst=new SpectrumScreenDocument();
        loaded.place(dst,20,18,true);
        assertTrue(dst.pixel(20,18));
        assertEquals(0x9B,dst.attribute(16,16)); // +11,+9 -> source tile (8,8) to (16,16)
    }

    @Test public void arrowsPreviewFromSameBaseAndConfirmOneUndo(){
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        doc.setPixel(6,7,true);
        doc.setPixel(33,35,true);
        ScreenEditHistory history=new ScreenEditHistory(doc.toRaw());
        ScreenSelection selection=new ScreenSelection();
        selection.select(6,7,8,9);
        assertTrue(selection.move(doc,history,1,0));
        assertTrue(selection.move(doc,history,1,0));
        assertTrue(selection.move(doc,history,0,1));
        assertFalse(doc.pixel(6,7));
        assertTrue(doc.pixel(8,8));
        assertTrue(doc.pixel(33,35));
        assertTrue(selection.commit(doc,history));
        assertTrue(history.canUndo());
        assertTrue(history.undo(doc));
        assertTrue(doc.pixel(6,7));assertFalse(doc.pixel(8,8));
        assertTrue(history.redo(doc));assertTrue(doc.pixel(8,8));
    }

    @Test public void cancelRestoresBaselineAndDoesNotAddUndo(){
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        doc.setPixel(20,20,true);
        byte[] before=doc.toRaw();
        ScreenSelection selection=new ScreenSelection();
        ScreenEditHistory history=new ScreenEditHistory(before);
        selection.select(20,20,20,20);
        selection.move(doc,history,2,3);
        assertTrue(selection.cancel(doc,history));
        assertArrayEquals(before,doc.toRaw());
        assertFalse(history.canUndo());
        ScreenSelectionClip clip=ScreenSelectionClip.capture(doc,20,20,1,1);
        assertTrue(selection.paste(doc,history,clip));
        selection.move(doc,history,5,6);
        selection.cancel(doc,history);
        assertArrayEquals(before,doc.toRaw());
    }

    @Test public void pastesCopiedPixelsToOtherDocumentWithoutPriorSelection(){
        SpectrumScreenDocument a=new SpectrumScreenDocument();
        a.setAttribute(3,3,0x55);a.setPixel(3,3,true);
        ScreenSelectionClip clip=ScreenSelectionClip.capture(a,3,3,3,3);
        SpectrumScreenDocument b=new SpectrumScreenDocument();
        ScreenEditHistory history=new ScreenEditHistory(b.toRaw());
        ScreenSelection selection=new ScreenSelection();
        assertTrue(selection.paste(b,history,clip));
        assertTrue(selection.move(b,history,8,0));
        selection.setMoveAttributes(b,true);
        assertTrue(b.pixel(11,3));
        assertEquals(0x55,b.attribute(8,0));
        selection.commit(b,history);
        assertTrue(history.isDirty(b));
    }
}
