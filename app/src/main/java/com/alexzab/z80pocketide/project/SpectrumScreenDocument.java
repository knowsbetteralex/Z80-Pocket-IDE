package com.alexzab.z80pocketide.project;

import java.util.Arrays;

/** Exact mutable 48K Spectrum SCREEN$ data, 6144 bitmap + 768 attributes. */
public final class SpectrumScreenDocument {
    public static final int WIDTH=256, HEIGHT=192, LENGTH=6912;
    public enum Tool { DRAW, ATTRIBUTE, PICK, ERASE, SELECT }
    public enum Side { INK, PAPER }
    private final byte[] raw;

    public SpectrumScreenDocument() {
        raw=new byte[LENGTH];
        Arrays.fill(raw,6144,LENGTH,(byte)7); // black paper / white ink
    }

    private SpectrumScreenDocument(byte[] bytes) { raw=bytes.clone(); }

    public static SpectrumScreenDocument fromRaw(byte[] bytes) {
        if(bytes==null||bytes.length!=LENGTH)
            throw new IllegalArgumentException("SCREEN$ must be exactly 6912 bytes");
        return new SpectrumScreenDocument(bytes);
    }

    public byte[] toRaw() { return raw.clone(); }
    public void replaceRaw(byte[] bytes) {
        if(bytes==null||bytes.length!=LENGTH)throw new IllegalArgumentException("Invalid SCREEN$ size");
        System.arraycopy(bytes,0,raw,0,LENGTH);
    }
    public static int bitmapOffset(int x,int y) {
        bounds(x,y);
        return ((y&0xC0)<<5)|((y&7)<<8)|((y&0x38)<<2)|(x>>3);
    }
    public static int attributeOffset(int x,int y) {
        bounds(x,y);
        return 6144+(y>>3)*32+(x>>3);
    }
    private static void bounds(int x,int y) {
        if(x<0||x>=WIDTH||y<0||y>=HEIGHT)
            throw new IndexOutOfBoundsException("Screen pixel "+x+","+y);
    }
    public boolean pixel(int x,int y) {
        return (raw[bitmapOffset(x,y)]&(0x80>>(x&7)))!=0;
    }
    public boolean setPixel(int x,int y,boolean ink) {
        int offset=bitmapOffset(x,y),mask=0x80>>(x&7);
        int old=raw[offset]&255;
        int next=ink?old|mask:old&~mask;
        raw[offset]=(byte)next;
        return next!=old;
    }
    public int attribute(int x,int y) { return raw[attributeOffset(x,y)]&255; }
    public boolean setAttribute(int x,int y,int attribute) {
        if(attribute<0||attribute>255)throw new IllegalArgumentException("Attribute must be 0..255");
        int i=attributeOffset(x,y),old=raw[i]&255;
        raw[i]=(byte)attribute;
        return old!=attribute;
    }
    /** Paint exactly one tile's selected bits; never globally recolor adjacent tiles. */
    public boolean paint(int x,int y,Tool tool,Side side,int color,
                         Boolean bright,Boolean flash) {
        if(tool==Tool.PICK||tool==Tool.SELECT)return false;
        if(color<0||color>7)throw new IllegalArgumentException("Spectrum color must be 0..7");
        // The eraser paints PAPER: it clears bitmap bits and can recolor only
        // that tile's paper. Existing independent ATTRIBUTE mode remains
        // available internally for existing tests/clients.
        if(tool==Tool.ERASE)side=Side.PAPER;
        int old=attribute(x,y),next=old;
        if(side==Side.INK)next=(next&~7)|color;
        else next=(next&~0x38)|(color<<3);
        if(bright!=null)next=bright?next|0x40:next&~0x40;
        if(flash!=null)next=flash?next|0x80:next&~0x80;
        boolean changed=setAttribute(x,y,next);
        if(tool==Tool.DRAW || tool==Tool.ERASE)
            changed=setPixel(x,y,side==Side.INK)||changed;
        return changed;
    }
    public int[] pixels() { return SpectrumScreen.decodePixels(raw); }
    public int[] pixels(boolean flashPhase){return SpectrumScreen.decodePixels(raw,flashPhase);}
    public boolean hasFlashingCells(){
        for(int i=6144;i<LENGTH;i++)if((raw[i]&0x80)!=0)return true;
        return false;
    }
}
