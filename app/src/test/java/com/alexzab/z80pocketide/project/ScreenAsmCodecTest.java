package com.alexzab.z80pocketide.project;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Random;

public final class ScreenAsmCodecTest {
    @Test public void exportImportIsLosslessForAnyFullScreen() {
        byte[] data=new byte[6912];
        new Random(0x5A77).nextBytes(data);
        String asm=ScreenAsmCodec.exportScreen(data);
        assertTrue(asm.contains("SCREEN_BITMAP:"));
        assertTrue(asm.contains("SCREEN_ATTRS:"));
        assertArrayEquals(data,ScreenAsmCodec.importScreen(asm));
    }
    @Test public void acceptsDefbEquAndSingleQuotedStrings() {
        StringBuilder data=new StringBuilder("FIRST EQU 65\nENTRY:\nDEFB 'A',FIRST,';' ; comment\n");
        for(int i=3;i<6912;i++)data.append(" DB 0\n");
        byte[] bytes=ScreenAsmCodec.importScreen(data.toString());
        assertEquals(6912,bytes.length);
        assertEquals(65,bytes[0]&255);
        assertEquals(65,bytes[1]&255);
        assertEquals(59,bytes[2]&255);
    }
    @Test public void rleRoundTripKeepsPixelsAndAttributes() {
        byte[][] samples=new byte[4][6912];
        new Random(0x515252).nextBytes(samples[0]);
        java.util.Arrays.fill(samples[1],(byte)0);
        for(int i=0;i<6912;i++) {
            samples[2][i]=(byte)(i%2==0?0xAA:0x55);
            samples[3][i]=(byte)(i/257);
        }
        for(byte[] sample:samples) {
            String asm=ScreenAsmCodec.exportRleScreen(sample);
            assertTrue(ScreenAsmCodec.isRleScreen(asm));
            assertTrue(asm.contains("SCREEN_RLE:"));
            assertArrayEquals(sample,ScreenAsmCodec.importScreen(asm));
        }
        assertFalse(ScreenAsmCodec.isRleScreen(ScreenAsmCodec.exportScreen(samples[0])));
        assertTrue(ScreenAsmCodec.exportRleScreen(samples[1]).length()
                < ScreenAsmCodec.exportScreen(samples[1]).length()/10);
    }

    @Test public void rleBoundariesAndMalformedDataAreRejected() {
        byte[] pattern=new byte[6912];
        for(int i=0;i<pattern.length;i++)pattern[i]=(byte)(i%129);
        assertArrayEquals(pattern,ScreenAsmCodec.decodeRle(ScreenAsmCodec.encodeRle(pattern)));

        assertThrows(IllegalArgumentException.class,
                ()->ScreenAsmCodec.decodeRle(new byte[]{(byte)0x80}));
        assertThrows(IllegalArgumentException.class,
                ()->ScreenAsmCodec.decodeRle(new byte[]{0x01,0x42}));
        assertThrows(IllegalArgumentException.class,
                ()->ScreenAsmCodec.decodeRle(new byte[]{(byte)0xFF,0x42}));
        byte[] overflow=new byte[108];
        for(int i=0;i<overflow.length;i+=2) {
            overflow[i]=(byte)0xFF;
            overflow[i+1]=0x55;
        }
        assertThrows(IllegalArgumentException.class,
                ()->ScreenAsmCodec.decodeRle(overflow));
        String marked="; Z80_POCKET_IDE_SCREEN_RLE_V1\nSCREEN_RLE: DB $80\n";
        assertThrows(IllegalArgumentException.class,()->ScreenAsmCodec.importScreen(marked));
    }

    @Test public void rejectsInstructionsUnsupportedIncludesAndShortData() {
        for(String source:new String[]{"LD A,1\n","INCLUDE \"other.asm\"\n","DB 1\n","ORG 16384\n"}) {
            assertThrows(IllegalArgumentException.class,()->ScreenAsmCodec.importScreen(source));
        }
    }
}
