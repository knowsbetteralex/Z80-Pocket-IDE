package com.alexzab.z80pocketide.screen;

import com.alexzab.z80pocketide.project.SpectrumScreenDocument;

/**
 * A non-destructive "lift" operation: arrows redraw from the same immutable
 * pre-move screen rather than repeatedly overwriting background on each step.
 * Confirm records one undo step; Cancel restores the exact original bytes.
 */
public final class ScreenSelection {
    private int x,y,width,height;
    private boolean selected,moveAttributes;
    private boolean preview,cutSource;
    private byte[] baseline;
    private ScreenSelectionClip overlay;
    private int fromX,fromY,previousX,previousY,previousW,previousH;
    private boolean hadPrevious;

    public boolean selected(){return selected;}
    public boolean preview(){return preview;}
    public boolean moveAttributes(){return moveAttributes;}
    public int x(){return x;}
    public int y(){return y;}
    public int width(){return width;}
    public int height(){return height;}

    public void select(int x1,int y1,int x2,int y2){
        if(preview)return;
        x1=clamp(x1,0,255);y1=clamp(y1,0,191);
        x2=clamp(x2,0,255);y2=clamp(y2,0,191);
        x=Math.min(x1,x2);y=Math.min(y1,y2);
        width=Math.abs(x2-x1)+1;height=Math.abs(y2-y1)+1;
        selected=true;
    }
    public void clear(){
        if(preview)throw new IllegalStateException("Confirm/cancel pending selection first");
        selected=false;
    }
    public ScreenSelectionClip copy(SpectrumScreenDocument doc){
        if(!selected)return null;
        return ScreenSelectionClip.capture(doc,x,y,width,height);
    }

    public boolean paste(SpectrumScreenDocument doc,ScreenEditHistory history,
                         ScreenSelectionClip clip){
        if(preview||clip==null)return false;
        previousX=x;previousY=y;previousW=width;previousH=height;
        hadPrevious=selected;
        x=selected?Math.min(x,256-clip.width):clamp(clip.x,0,256-clip.width);
        y=selected?Math.min(y,192-clip.height):clamp(clip.y,0,192-clip.height);
        width=clip.width;height=clip.height;selected=true;
        fromX=x;fromY=y;overlay=clip;
        baseline=doc.toRaw();
        cutSource=false;preview=true;
        history.begin(doc);
        repaint(doc);
        return true;
    }

    public boolean move(SpectrumScreenDocument doc,ScreenEditHistory history,
                        int dx,int dy){
        if(!selected)return false;
        int nextX=x+dx,nextY=y+dy;
        if(nextX<0||nextY<0||nextX+width>256||nextY+height>192)return false;
        if(!preview){
            baseline=doc.toRaw();
            overlay=ScreenSelectionClip.capture(doc,x,y,width,height);
            fromX=x;fromY=y;
            cutSource=true;preview=true;
            history.begin(doc);
        }
        x=nextX;y=nextY;
        repaint(doc);
        return true;
    }
    public void setMoveAttributes(SpectrumScreenDocument doc,boolean enabled){
        moveAttributes=enabled;
        if(preview)repaint(doc);
    }
    public boolean commit(SpectrumScreenDocument doc,ScreenEditHistory history){
        if(!preview)return false;
        boolean changed=history.end(doc);
        preview=false;
        baseline=null;overlay=null;
        return changed;
    }
    public boolean cancel(SpectrumScreenDocument doc,ScreenEditHistory history){
        if(!preview)return false;
        doc.replaceRaw(baseline);
        history.end(doc); // no-op relative to original baseline
        if(!cutSource){
            selected=hadPrevious;
            x=previousX;y=previousY;width=previousW;height=previousH;
        }else{
            x=fromX;y=fromY;
        }
        preview=false;baseline=null;overlay=null;
        return true;
    }

    private void repaint(SpectrumScreenDocument doc){
        if(!preview)return;
        doc.replaceRaw(baseline);
        if(cutSource){
            for(int sy=0;sy<overlay.height;sy++)
                for(int sx=0;sx<overlay.width;sx++)
                    doc.setPixel(fromX+sx,fromY+sy,false);
        }
        overlay.place(doc,x,y,moveAttributes);
    }
    private static int clamp(int n,int lo,int hi){
        return Math.max(lo,Math.min(hi,n));
    }
}
