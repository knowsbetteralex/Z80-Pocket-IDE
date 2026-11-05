package com.alexzab.z80pocketide.project;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public final class SpectrumScreenDocumentTest {
    @Test public void rawBytesRoundTripAndAddressMapping() {
        byte[] original=new byte[6912];
        new Random(72341).nextBytes(original);
        SpectrumScreenDocument doc=SpectrumScreenDocument.fromRaw(original);
        assertArrayEquals(original,doc.toRaw());
        assertEquals(0,SpectrumScreenDocument.bitmapOffset(0,0));
        assertEquals(256,SpectrumScreenDocument.bitmapOffset(0,1));
        assertEquals(32,SpectrumScreenDocument.bitmapOffset(0,8));
        assertEquals(2048,SpectrumScreenDocument.bitmapOffset(0,64));
        assertEquals(4096,SpectrumScreenDocument.bitmapOffset(0,128));
        assertEquals(6143,SpectrumScreenDocument.bitmapOffset(255,191));
        assertEquals(6911,SpectrumScreenDocument.attributeOffset(255,191));
    }
    @Test public void inkPaperAndAttributesStayInOneTile() {
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        doc.setAttribute(4,4,0x80|0x40|0x38|0x03);
        int adjacent=doc.attribute(8,4);
        assertTrue(doc.paint(4,4,SpectrumScreenDocument.Tool.DRAW,
                SpectrumScreenDocument.Side.INK,6,null,null));
        assertTrue(doc.pixel(4,4));
        assertEquals(0x80|0x40|0x38|6,doc.attribute(4,4));
        assertEquals(adjacent,doc.attribute(8,4));
        assertTrue(doc.paint(4,4,SpectrumScreenDocument.Tool.DRAW,
                SpectrumScreenDocument.Side.PAPER,2,null,null));
        assertFalse(doc.pixel(4,4));
        assertEquals(0x80|0x40|(2<<3)|6,doc.attribute(4,4));
        byte[] bitmap=Arrays.copyOf(doc.toRaw(),6144);
        doc.paint(4,4,SpectrumScreenDocument.Tool.ATTRIBUTE,
                SpectrumScreenDocument.Side.PAPER,4,true,false);
        assertArrayEquals(bitmap,Arrays.copyOf(doc.toRaw(),6144));
        assertEquals(0x40|(4<<3)|6,doc.attribute(4,4));
        assertFalse(doc.paint(4,4,SpectrumScreenDocument.Tool.PICK,
                SpectrumScreenDocument.Side.INK,1,null,null));
    }
    @Test public void boundariesAndSourceAreDefensive() {
        byte[] src=new byte[6912];
        SpectrumScreenDocument doc=SpectrumScreenDocument.fromRaw(src);
        src[0]=42;
        assertEquals(0,doc.toRaw()[0]);
        assertThrows(IndexOutOfBoundsException.class,()->doc.pixel(256,4));
        assertThrows(IllegalArgumentException.class,()->SpectrumScreenDocument.fromRaw(new byte[6911]));
    }

    @Test public void eraserUsesPaperAndKeepsOtherCellAttributes() {
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        doc.setAttribute(0,0,0x80|0x40|0x03);
        doc.setAttribute(8,0,0x12);
        doc.setPixel(0,0,true);
        assertTrue(doc.paint(0,0,SpectrumScreenDocument.Tool.ERASE,
                SpectrumScreenDocument.Side.INK,4,null,null));
        assertFalse(doc.pixel(0,0));
        assertEquals(0x80|0x40|0x03|(4<<3),doc.attribute(0,0));
        assertEquals(0x12,doc.attribute(8,0));
        assertTrue(doc.hasFlashingCells());
        assertEquals(0xFF00FF00,doc.pixels(false)[0]); // bright green paper
        assertEquals(0xFF0000FF,doc.pixels(true)[0]); // flash => bright blue ink
    }
}
