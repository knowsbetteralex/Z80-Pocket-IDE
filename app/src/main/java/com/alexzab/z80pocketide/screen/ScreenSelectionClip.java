package com.alexzab.z80pocketide.screen;

import com.alexzab.z80pocketide.project.SpectrumScreenDocument;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy of a pixel-accurate rectangle plus all 8x8 tiles it touches.
 * Attribute sources are anchored at the first pixel (column 0) of each tile.
 * A displacement of 1..7 pixels therefore keeps attributes in their tile;
 * displacement 8 moves them by one entire tile.
 */
public final class ScreenSelectionClip {
    private static final int MAGIC=0x53435231; // SCR1
    public final int x,y,width,height;
    private final byte[] pixels; // One 0/1 byte per selected pixel.
    private final int firstTileX,firstTileY,tilesWide,tilesHigh;
    private final byte[] attributes;

    private ScreenSelectionClip(int x,int y,int width,int height,byte[] pixels,
                                int firstTileX,int firstTileY,int tilesWide,
                                int tilesHigh,byte[] attributes){
        this.x=x;this.y=y;this.width=width;this.height=height;
        this.pixels=pixels;this.firstTileX=firstTileX;this.firstTileY=firstTileY;
        this.tilesWide=tilesWide;this.tilesHigh=tilesHigh;
        this.attributes=attributes;
    }

    public static ScreenSelectionClip capture(SpectrumScreenDocument doc,
                                               int x,int y,int width,int height){
        validate(x,y,width,height);
        byte[] pixels=new byte[width*height];
        for(int row=0;row<height;row++)
            for(int col=0;col<width;col++)
                pixels[row*width+col]=(byte)(doc.pixel(x+col,y+row)?1:0);
        int tx=x/8,ty=y/8;
        int w=(x+width-1)/8-tx+1,h=(y+height-1)/8-ty+1;
        byte[] attrs=new byte[w*h];
        for(int ay=0;ay<h;ay++)for(int ax=0;ax<w;ax++)
            attrs[ay*w+ax]=(byte)doc.attribute((tx+ax)*8,(ty+ay)*8);
        return new ScreenSelectionClip(x,y,width,height,pixels,tx,ty,w,h,attrs);
    }

    public boolean pixel(int px,int py){
        if(px<0||px>=width||py<0||py>=height)throw new IndexOutOfBoundsException();
        return pixels[py*width+px]!=0;
    }
    public int tilesWide(){return tilesWide;}
    public int tilesHigh(){return tilesHigh;}
    public int originalTileX(int index){return firstTileX+index;}
    public int originalTileY(int index){return firstTileY+index;}
    public int tileAttribute(int tx,int ty){
        return attributes[ty*tilesWide+tx]&255;
    }

    /** Paste with a *pixel* offset. Attributes change only across full 8-pixel boundaries. */
    public void place(SpectrumScreenDocument doc,int destX,int destY,boolean moveAttributes){
        for(int row=0;row<height;row++){
            int py=destY+row;
            if(py<0||py>=192)continue;
            for(int col=0;col<width;col++){
                int px=destX+col;
                if(px<0||px>=256)continue;
                doc.setPixel(px,py,pixels[row*width+col]!=0);
            }
        }
        if(!moveAttributes)return;
        int dx=destX-x,dy=destY-y;
        for(int ty=0;ty<tilesHigh;ty++){
            for(int tx=0;tx<tilesWide;tx++){
                // The starting column of each 8x8 source cell is the anchor.
                int targetTileX=Math.floorDiv(originalTileX(tx)*8+dx,8);
                int targetTileY=Math.floorDiv(originalTileY(ty)*8+dy,8);
                if(targetTileX>=0&&targetTileX<32&&targetTileY>=0&&targetTileY<24)
                    doc.setAttribute(targetTileX*8,targetTileY*8,tileAttribute(tx,ty));
            }
        }
    }

    public byte[] encode(){
        try {
            ByteArrayOutputStream output=new ByteArrayOutputStream(50000);
            DataOutputStream data=new DataOutputStream(output);
            data.writeInt(MAGIC);
            data.writeInt(x);data.writeInt(y);data.writeInt(width);data.writeInt(height);
            data.writeInt(firstTileX);data.writeInt(firstTileY);
            data.writeInt(tilesWide);data.writeInt(tilesHigh);
            data.write(pixels);data.write(attributes);data.flush();
            return output.toByteArray();
        }catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public static ScreenSelectionClip decode(byte[] raw) throws IOException {
        if(raw==null||raw.length<36||raw.length>53000)
            throw new IOException("Invalid screen clipboard length");
        DataInputStream input=new DataInputStream(new ByteArrayInputStream(raw));
        if(input.readInt()!=MAGIC)throw new IOException("Unknown screen clipboard format");
        int x=input.readInt(),y=input.readInt(),w=input.readInt(),h=input.readInt();
        validate(x,y,w,h);
        int tx=input.readInt(),ty=input.readInt(),tw=input.readInt(),th=input.readInt();
        int expectedX=x/8,expectedY=y/8;
        int expectedW=(x+w-1)/8-expectedX+1;
        int expectedH=(y+h-1)/8-expectedY+1;
        if(tx!=expectedX||ty!=expectedY||tw!=expectedW||th!=expectedH)
            throw new IOException("Invalid screen clipboard attributes");
        byte[] pixels=new byte[w*h],attrs=new byte[tw*th];
        input.readFully(pixels);input.readFully(attrs);
        if(input.read()!=-1)throw new IOException("Trailing screen clipboard bytes");
        for(byte px:pixels)if(px!=0&&px!=1)throw new IOException("Bad pixel clipboard data");
        return new ScreenSelectionClip(x,y,w,h,pixels,tx,ty,tw,th,attrs);
    }

    private static void validate(int x,int y,int width,int height){
        if(x<0||y<0||width<1||height<1
                ||x+width>256||y+height>192)
            throw new IllegalArgumentException("Selection must be within 256x192");
    }
}
