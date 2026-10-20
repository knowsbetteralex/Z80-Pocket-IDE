package com.alexzab.z80pocketide.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SpriteLayersTest {
    @Test public void defaultMaskIsOpaqueAndExportsFfInBothFormats() {
        SpriteData s = new SpriteData(16, 8);
        assertTrue(s.getMask(0, 0));
        assertTrue(s.getMask(15, 7));
        assertEquals(0xFF, s.maskByte(0, 0));
        assertEquals(16, s.maskBytes().length);
        assertTrue(s.dbMask(false).startsWith("    DB $FF,$FF"));
        assertTrue(s.dbMask(true).startsWith("    DB %11111111,%11111111"));
    }

    @Test public void erasingMaskDoesNotAlterSpriteBitsAndCanRestoreColour() {
        SpriteData s = new SpriteData(16, 8);
        s.setPixel(0, 0, true);
        s.setPixel(8, 0, true);
        int colour = s.displayedColor(0, 0);
        s.setMask(0, 0, false);
        s.setMask(8, 0, false);
        assertFalse(s.getMask(0, 0));
        assertTrue(s.getPixel(0, 0));
        assertEquals(colour, s.displayedColor(0, 0));
        assertEquals(0x7F, s.maskByte(0, 0));
        assertEquals(0x7F, s.maskByte(1, 0));
        s.setMask(0, 0, true);
        assertEquals(0xFF, s.maskByte(0, 0));
        s.resetMask();
        assertEquals(0xFF, s.maskByte(1, 0));
    }

    @Test public void canvasShrinkAndExpandDoNotDestroyHiddenLayers() {
        SpriteData s = new SpriteData(32, 32);
        s.setPixel(31, 31, true);
        s.setMask(31, 31, false);
        s.resize(8, 8);
        assertFalse(s.getPixel(31, 31)); // outside visible canvas
        assertFalse(s.getMask(31, 31));
        assertEquals(8, s.bitmapBytes().length);
        s.resize(32, 32);
        assertTrue(s.getPixel(31, 31));
        assertFalse(s.getMask(31, 31));
        assertEquals(0x01, s.pixelByte(3, 31));
        assertEquals(0xFE, s.maskByte(3, 31));
    }

    @Test public void negativeShiftClipsPixelsAndReturningRestoresThem() {
        SpriteData s = new SpriteData(16, 8);
        s.setPixel(0, 0, true);
        s.setPixel(15, 0, true);
        s.setMask(0, 0, false);
        s.moveBy(-3, 0);
        assertFalse(s.getPixel(0, 0)); // the leftmost pixel is clipped
        assertTrue(s.getPixel(12, 0)); // x15 maps to 12
        assertFalse(s.getMask(0, 0) == false && s.getPixel(0, 0));
        s.moveBy(3, 0);
        assertTrue(s.getPixel(0, 0));
        assertTrue(s.getPixel(15, 0));
        assertFalse(s.getMask(0, 0));
    }

    @Test public void positiveShiftExportsEmptyClippedBorderAndRestoresOriginal() {
        SpriteData s = new SpriteData(16, 8);
        s.setPixel(0, 0, true);
        s.moveBy(1, 0);
        assertTrue(s.getPixel(1, 0));
        assertFalse(s.getPixel(0, 0));
        assertEquals(0x40, s.pixelByte(0, 0));
        assertEquals(0x7F, s.maskByte(0, 0));
        s.resetOffset();
        assertEquals(0x80, s.pixelByte(0, 0));
        assertEquals(0xFF, s.maskByte(0, 0));
    }

    @Test public void redrawUsesViewportOffsetAsBackerCoordinates() {
        SpriteData s = new SpriteData(16, 16);
        s.moveBy(3, 0);
        s.setPixel(6, 4, true);  // source coordinate 3,4
        s.setMask(6, 4, false);
        s.resetOffset();
        assertTrue(s.getPixel(3, 4));
        assertFalse(s.getMask(3, 4));
    }

    @Test public void spriteStateIsSharedWithinOneProcessUntilReset() {
        SpriteSession.clear();
        SpriteData held = SpriteSession.sprite();
        held.setPixel(2, 2, true);
        held.setMask(1, 1, false);
        held.moveBy(2, -1);
        held.resize(24, 16);
        SpriteSession.setMaskEditing(true);
        SpriteSession.setPaperTarget(true);
        SpriteSession.setBinaryExport(true);

        assertTrue(SpriteSession.sprite() == held);
        assertEquals(24, SpriteSession.sprite().width());
        assertEquals(2, SpriteSession.sprite().offsetX());
        assertTrue(SpriteSession.maskEditing());
        assertTrue(SpriteSession.paperTarget());
        assertTrue(SpriteSession.binaryExport());
        SpriteSession.clear();
        assertFalse(SpriteSession.maskEditing());
        assertFalse(SpriteSession.binaryExport());
        assertEquals(16, SpriteSession.sprite().width());
        assertTrue(SpriteSession.sprite().getMask(0, 0));
    }
}
