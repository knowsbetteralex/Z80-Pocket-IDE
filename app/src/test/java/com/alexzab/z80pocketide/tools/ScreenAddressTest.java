package com.alexzab.z80pocketide.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import org.junit.Test;

public final class ScreenAddressTest {
    @Test public void screenOriginAndLastByte() {
        ScreenAddress origin = new ScreenAddress(0,0,0,0);
        assertEquals(0x4000,origin.pixelAddress());
        assertEquals(0x5800,origin.attributeAddress());
        ScreenAddress last = new ScreenAddress(2,7,7,31);
        assertEquals(0x57FF,last.pixelAddress());
        assertEquals(0x5AFF,last.attributeAddress());
    }

    @Test public void zxInterleavingAndReverse() {
        assertEquals(0x4100,new ScreenAddress(0,0,1,0).pixelAddress());
        assertEquals(0x4020,new ScreenAddress(0,1,0,0).pixelAddress());
        assertEquals(0x4800,new ScreenAddress(1,0,0,0).pixelAddress());
        for (int third=0;third<3;third++)
            for(int row=0;row<8;row++)
                for(int px=0;px<8;px++)
                    for(int col=0;col<32;col++) {
                        ScreenAddress s = new ScreenAddress(third,row,px,col);
                        ScreenAddress decoded = ScreenAddress.fromPixelAddress(s.pixelAddress());
                        assertEquals(s.pixelAddress(),decoded.pixelAddress());
                        assertEquals(s.attributeAddress(),decoded.attributeAddress());
                    }
    }

    @Test public void attributeToPixelLinksAndKeepsPixelLine() {
        ScreenAddress mid=ScreenAddress.fromAttributeCoordinates(11,19,4);
        assertEquals(1,mid.third);
        assertEquals(3,mid.row);
        assertEquals(4,mid.pixelLine);
        assertEquals(19,mid.column);
        assertEquals(0x5800+11*32+19,mid.attributeAddress());
        assertEquals(mid.pixelAddress(),
                ScreenAddress.fromAttributeAddress(mid.attributeAddress(),4).pixelAddress());
    }

    @Test public void binaryGroupsAndParsing() {
        assertEquals("010 00 000 000 00000",ScreenAddress.pixelBits(0x4000));
        assertEquals("010110 00000 00000",ScreenAddress.attributeBits(0x5800));
        assertEquals(0x57FF,ScreenAddress.parseBinaryAddress("%0101011111111111"));
        assertEquals(0x4000,ScreenAddress.parseHexAddress("0x4000"));
        assertEquals("$5800",ScreenAddress.hex(0x5800));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsThirdThree(){new ScreenAddress(3,0,0,0);}
    @Test(expected=IllegalArgumentException.class)
    public void rejectsOutOfScreen(){ScreenAddress.fromPixelAddress(0x5800);}
}
