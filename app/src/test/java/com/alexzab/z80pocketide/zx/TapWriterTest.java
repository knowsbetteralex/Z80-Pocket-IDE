package com.alexzab.z80pocketide.zx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TapWriterTest {
    @Test
    public void autorunTapStartsWithBasicProgramAndContainsCodeFile() {
        byte[] tap = new TapWriter().programTap("DEMO", 0x8000,
                new byte[] { 0x00, 0x18, (byte) 0xFD });

        int firstBlockLength = u16(tap, 0);
        assertEquals(19, firstBlockLength);
        assertEquals(0, tap[2] & 0xFF); // header flag
        assertEquals(0, tap[3] & 0xFF); // PROGRAM type
        assertEquals(10, u16(tap, 16)); // autostart line

        int basicDataOffset = 2 + firstBlockLength;
        int basicDataLength = u16(tap, basicDataOffset);
        int codeHeaderOffset = basicDataOffset + 2 + basicDataLength;

        assertTrue(codeHeaderOffset + 3 < tap.length);
        assertEquals(19, u16(tap, codeHeaderOffset));
        assertEquals(0, tap[codeHeaderOffset + 2] & 0xFF);
        assertEquals(3, tap[codeHeaderOffset + 3] & 0xFF); // CODE type
    }

    private static int u16(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }
    @Test
    public void loadingScreenTapHasBasicScreenAndGameBlocksInOrder() {
        byte[] screen=new byte[6912];
        screen[0]=42;
        byte[] tap=new TapWriter().programTapWithScreen(
                "SPACE",0x8000,new byte[]{0, (byte)0xC9},screen,100);
        int basicHeader=0;
        int basicData=basicHeader+2+u16(tap,basicHeader);
        int basicEnd=basicData+2+u16(tap,basicData);
        int screenHeader=basicEnd;
        assertEquals(19,u16(tap,screenHeader));
        assertEquals(3,tap[screenHeader+3]&255);
        assertEquals(6912,u16(tap,screenHeader+14));
        assertEquals(0x4000,u16(tap,screenHeader+16));
        int screenData=screenHeader+2+19;
        assertEquals(6914,u16(tap,screenData));
        assertEquals(0xFF,tap[screenData+2]&255);
        assertEquals(42,tap[screenData+3]&255);
        int gameHeader=screenData+2+u16(tap,screenData);
        assertEquals(19,u16(tap,gameHeader));
        assertEquals(3,tap[gameHeader+3]&255);
        assertEquals(0x8000,u16(tap,gameHeader+16));
        assertEquals(2,u16(tap,gameHeader+14));

        assertTrue(contains(tap,basicData,basicEnd,0xAA)); // SCREEN$
        assertTrue(contains(tap,basicData,basicEnd,0xEF)); // LOAD
        assertTrue(contains(tap,basicData,basicEnd,0xAF)); // CODE
        assertTrue(contains(tap,basicData,basicEnd,0xF2)); // PAUSE
        assertTrue(contains(tap,basicData,basicEnd,0xF9)); // RANDOMIZE
    }

    @Test
    public void immediateScreenStartOmitsPauseAndKeyWaitIncludesPause() {
        byte[] image=new byte[6912];
        TapWriter writer=new TapWriter();
        byte[] instant=writer.programTapWithScreen("GAME",0x8000,new byte[]{0},image,0);
        int from=2+19,to=from+2+u16(instant,from);
        assertFalse(contains(instant,from,to,0xF2));
        byte[] press=writer.programTapWithScreen("GAME",0x8000,new byte[]{0},image,-1);
        from=2+19;to=from+2+u16(press,from);
        assertTrue(contains(press,from,to,0xF2));
    }

    @Test
    public void loadingScreenTapRejectsInvalidSizesAndOverlap() {
        TapWriter writer=new TapWriter();
        org.junit.Assert.assertThrows(IllegalArgumentException.class,
                ()->writer.programTapWithScreen("A",0x8000,new byte[]{0},new byte[6911],100));
        org.junit.Assert.assertThrows(IllegalArgumentException.class,
                ()->writer.programTapWithScreen("A",0x4000,new byte[]{0},new byte[6912],100));
    }

    private static boolean contains(byte[] bytes,int from,int to,int value){
        for(int i=from;i<to;i++)if((bytes[i]&255)==value)return true;
        return false;
    }

}
