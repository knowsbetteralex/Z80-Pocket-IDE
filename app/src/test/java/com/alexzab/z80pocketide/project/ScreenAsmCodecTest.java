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
    @Test public void rejectsInstructionsUnsupportedIncludesAndShortData() {
        for(String source:new String[]{"LD A,1\n","INCLUDE \"other.asm\"\n","DB 1\n","ORG 16384\n"}) {
            assertThrows(IllegalArgumentException.class,()->ScreenAsmCodec.importScreen(source));
        }
    }
}
