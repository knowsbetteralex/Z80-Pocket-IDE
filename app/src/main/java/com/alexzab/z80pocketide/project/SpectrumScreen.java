package com.alexzab.z80pocketide.project;

/** Stateless ZX Spectrum 48K bitmap + attribute decoder (pure Java). */
public final class SpectrumScreen {
    public static final int WIDTH=256, HEIGHT=192, LENGTH=6912;
    private static final int[] NORMAL={
            0xFF000000,0xFF0000D7,0xFFD70000,0xFFD700D7,
            0xFF00D700,0xFF00D7D7,0xFFD7D700,0xFFD7D7D7
    };
    private static final int[] BRIGHT={
            0xFF000000,0xFF0000FF,0xFFFF0000,0xFFFF00FF,
            0xFF00FF00,0xFF00FFFF,0xFFFFFF00,0xFFFFFFFF
    };
    private SpectrumScreen() {}

    public static int[] decodePixels(byte[] screen) {
        return decodePixels(screen,false);
    }

    /** ZX FLASH swaps INK and PAPER every 16 video frames (~320 ms). */
    public static int[] decodePixels(byte[] screen,boolean inverseFlash) {
        if(screen==null||screen.length!=LENGTH)
            throw new IllegalArgumentException("SCREEN$ must contain exactly 6912 bytes");
        int[] pixels=new int[WIDTH*HEIGHT];
        for(int y=0;y<HEIGHT;y++){
            int bitmapRow=((y&0xC0)<<5)|((y&7)<<8)|((y&0x38)<<2);
            for(int xByte=0;xByte<32;xByte++){
                int bits=screen[bitmapRow+xByte]&255;
                int attr=screen[6144+(y>>3)*32+xByte]&255;
                int[] palette=(attr&0x40)!=0?BRIGHT:NORMAL;
                int ink=palette[attr&7];
                int paper=palette[(attr>>3)&7];
                if(inverseFlash && (attr&0x80)!=0) {
                    int swap=ink;ink=paper;paper=swap;
                }
                int base=y*WIDTH+xByte*8;
                // The displayed phase never mutates underlying SCREEN$ bytes.
                for(int bit=0;bit<8;bit++)
                    pixels[base+bit]=(bits&(0x80>>bit))!=0?ink:paper;
            }
        }
        return pixels;
    }
}
