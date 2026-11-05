package com.alexzab.z80pocketide.screen;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import com.alexzab.z80pocketide.project.SpectrumScreenDocument;

/** Gesture-driven 256x192 SCREEN$ canvas. Floating controls live in its parent. */
public final class ScreenEditorCanvasView extends View {
    public interface Listener {
        void strokeStart();
        void strokeEnd();
        void changed(int x,int y);
        void picked(int x,int y,int attribute,boolean pixel);
    }
    private SpectrumScreenDocument doc;
    private Listener listener;
    private final Paint pixels=new Paint(),grid=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap image;
    private boolean pixelsDirty=true,locked=true,transforming,painting,pendingStroke;
    private float downX,downY;
    private final int touchSlop;
    private boolean flashPhase;
    private final Runnable flashTicker=new Runnable() {
        @Override public void run() {
            if(doc!=null && doc.hasFlashingCells()){
                flashPhase=!flashPhase;
                pixelsDirty=true;
                invalidate();
            }else flashPhase=false;
            postDelayed(this,320); // ZX Spectrum: FLASH flips every 16 frames.
        }
    };
    private SpectrumScreenDocument.Tool tool=SpectrumScreenDocument.Tool.DRAW;
    private SpectrumScreenDocument.Side side=SpectrumScreenDocument.Side.INK;
    private int color=7;
    private Boolean bright,flash;
    private float pixelSize=1,originX,originY,lastX,lastY,focusX,focusY;
    private int previousX=-1,previousY=-1;
    private int activeX=-1,activeY=-1;
    private final ScaleGestureDetector scaleDetector;

    public ScreenEditorCanvasView(Context context,SpectrumScreenDocument initial) {
        super(context);
        doc=initial;
        touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();
        setBackgroundColor(0xFF17241D);
        pixels.setFilterBitmap(false);
        scaleDetector=new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScaleBegin(ScaleGestureDetector d){
                    transforming=true;
                    return true;
                }
                @Override public boolean onScale(ScaleGestureDetector d){
                    zoomAt(d.getScaleFactor(),d.getFocusX(),d.getFocusY());
                    return true;
                }
            });
    }
    public void setListener(Listener listener){this.listener=listener;}
    public void setDocument(SpectrumScreenDocument value){
        this.doc=value;pixelsDirty=true;fitToScreen();
    }
    public void refreshPixels(){pixelsDirty=true;invalidate();}
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        removeCallbacks(flashTicker);
        postDelayed(flashTicker,320);
    }
    @Override protected void onDetachedFromWindow(){
        removeCallbacks(flashTicker);
        super.onDetachedFromWindow();
    }
    public void setLocked(boolean value){
        locked=value;painting=false;pendingStroke=false;previousX=previousY=-1;
    }
    public boolean isLocked(){return locked;}
    public void setBrush(SpectrumScreenDocument.Tool tool,
                         SpectrumScreenDocument.Side side,int color,Boolean bright,Boolean flash){
        this.tool=tool;this.side=side;this.color=color;this.bright=bright;this.flash=flash;
    }
    public float getPixelSize(){return pixelSize;}
    public float getOriginX(){return originX;}
    public float getOriginY(){return originY;}
    public void setViewport(float size,float ox,float oy){
        if(size<=0 || Float.isNaN(size))return;
        pixelSize=size;originX=ox;originY=oy;invalidate();
    }
    public void fitToScreen(){
        if(getWidth()<1||getHeight()<1)return;
        float padding=dp(10);
        pixelSize=Math.min((getWidth()-2*padding)/256f,(getHeight()-2*padding)/192f);
        pixelSize=Math.max(.05f,pixelSize);
        originX=(getWidth()-256*pixelSize)*.5f;
        originY=(getHeight()-192*pixelSize)*.5f;
        invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
        super.onSizeChanged(w,h,oldW,oldH);
        if(oldW==0||oldH==0)fitToScreen();
        else {
            float centerX=(oldW*.5f-originX)/pixelSize;
            float centerY=(oldH*.5f-originY)/pixelSize;
            originX=w*.5f-centerX*pixelSize;
            originY=h*.5f-centerY*pixelSize;
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(doc==null)return;
        if(pixelsDirty||image==null) {
            int[] colors=doc.pixels(flashPhase);
            if(image==null)image=Bitmap.createBitmap(256,192,Bitmap.Config.ARGB_8888);
            image.setPixels(colors,0,256,0,0,256,192);
            pixelsDirty=false;
        }
        canvas.save();
        canvas.clipRect(0,0,getWidth(),getHeight());
        canvas.drawBitmap(image,null,new RectF(originX,originY,originX+256*pixelSize,
                originY+192*pixelSize),pixels);
        if(pixelSize>=1.9f) {
            grid.setStyle(Paint.Style.STROKE);
            grid.setStrokeWidth(Math.max(.6f,dp(.7f)));
            grid.setColor(0x9986A492);
            for(int x=0;x<=32;x++)
                canvas.drawLine(originX+x*8*pixelSize,originY,
                        originX+x*8*pixelSize,originY+192*pixelSize,grid);
            for(int y=0;y<=24;y++)
                canvas.drawLine(originX,originY+y*8*pixelSize,
                        originX+256*pixelSize,originY+y*8*pixelSize,grid);
        }
        if(pixelSize>=8) {
            grid.setStyle(Paint.Style.STROKE);
            grid.setStrokeWidth(.6f);
            grid.setColor(0x77556655);
            int minX=Math.max(0,(int)Math.floor(-originX/pixelSize));
            int maxX=Math.min(256,(int)Math.ceil((getWidth()-originX)/pixelSize));
            int minY=Math.max(0,(int)Math.floor(-originY/pixelSize));
            int maxY=Math.min(192,(int)Math.ceil((getHeight()-originY)/pixelSize));
            for(int x=minX;x<=maxX;x++)
                canvas.drawLine(originX+x*pixelSize,originY,
                        originX+x*pixelSize,originY+192*pixelSize,grid);
            for(int y=minY;y<=maxY;y++)
                canvas.drawLine(originX,originY+y*pixelSize,
                        originX+256*pixelSize,originY+y*pixelSize,grid);
        }
        if(activeX>=0 && pixelSize>=2) {
            grid.setStyle(Paint.Style.STROKE);grid.setColor(0xFFEDA831);
            grid.setStrokeWidth(dp(1.5f));
            int cx=activeX/8,cy=activeY/8;
            canvas.drawRect(originX+cx*8*pixelSize,originY+cy*8*pixelSize,
                    originX+(cx+1)*8*pixelSize,originY+(cy+1)*8*pixelSize,grid);
        }
        grid.setStyle(Paint.Style.FILL);
        canvas.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        scaleDetector.onTouchEvent(event);
        if(action==MotionEvent.ACTION_DOWN) {
            transforming=false;
            painting=false;
            previousX=previousY=-1;
            lastX=downX=event.getX();
            lastY=downY=event.getY();
            pendingStroke=!locked;
            if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
            // Defer one-finger paint until movement or confirmed tap, so the
            // first finger of a pinch cannot leave an unwanted dot.
            return true;
        }
        if(action==MotionEvent.ACTION_POINTER_DOWN) {
            pendingStroke=false;
            finishStroke();
            transforming=true;
            focusX=averageX(event);focusY=averageY(event);
            return true;
        }
        if(action==MotionEvent.ACTION_MOVE) {
            if(event.getPointerCount()>=2) {
                float x=averageX(event),y=averageY(event);
                originX+=x-focusX;originY+=y-focusY;
                focusX=x;focusY=y;invalidate();
            }else if(locked) {
                originX+=event.getX()-lastX;
                originY+=event.getY()-lastY;
                invalidate();
            }else if(!transforming) {
                if(pendingStroke && (Math.abs(event.getX()-downX)>touchSlop
                        || Math.abs(event.getY()-downY)>touchSlop)) {
                    pendingStroke=false;
                    beginStroke(downX,downY);
                }
                if(painting)paintAt(event.getX(),event.getY());
            }
            lastX=event.getX();lastY=event.getY();
            return true;
        }
        if(action==MotionEvent.ACTION_POINTER_UP) {
            pendingStroke=false;
            transforming=true;finishStroke();return true;
        }
        if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_UP) {
            if(action==MotionEvent.ACTION_UP && pendingStroke && !transforming)
                beginStroke(downX,downY);
            pendingStroke=false;
            finishStroke();
            previousX=previousY=-1;
            transforming=false;
            if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
            if(action==MotionEvent.ACTION_UP)performClick();
            return true;
        }
        return true;
    }
    private void beginStroke(float fx,float fy) {
        int x=screenX(fx),y=screenY(fy);
        if(!inBounds(x,y))return;
        activeX=x;activeY=y;
        if(tool==SpectrumScreenDocument.Tool.PICK) {
            if(listener!=null)listener.picked(x,y,doc.attribute(x,y),doc.pixel(x,y));
            invalidate();return;
        }
        painting=true;
        if(listener!=null)listener.strokeStart();
        paintAt(fx,fy);
    }
    private void paintAt(float fx,float fy) {
        int x=screenX(fx),y=screenY(fy);
        if(!inBounds(x,y)){previousX=previousY=-1;return;}
        int sx=previousX<0?x:previousX,sy=previousY<0?y:previousY;
        int steps=Math.max(Math.abs(x-sx),Math.abs(y-sy));
        boolean any=false;
        for(int i=0;i<=steps;i++){
            int px=steps==0?x:sx+(x-sx)*i/steps;
            int py=steps==0?y:sy+(y-sy)*i/steps;
            any|=doc.paint(px,py,tool,side,color,bright,flash);
        }
        previousX=x;previousY=y;
        activeX=x;activeY=y;
        if(any){pixelsDirty=true;invalidate();if(listener!=null)listener.changed(x,y);}
        else invalidate();
    }
    private void finishStroke(){
        if(painting){painting=false;if(listener!=null)listener.strokeEnd();}
        previousX=previousY=-1;
    }
    private void zoomAt(float scale,float fx,float fy){
        if(scale<=0||Float.isNaN(scale))return;
        float min=Math.max(.25f,Math.min(getWidth()/256f,getHeight()/192f)*.35f);
        // 96dp per Spectrum pixel: comfortable even for fingertip editing.
        float next=Math.max(min,Math.min(dp(96),pixelSize*scale));
        float k=next/pixelSize;
        originX=fx-(fx-originX)*k;
        originY=fy-(fy-originY)*k;
        pixelSize=next;
        invalidate();
    }
    public int selectedPixelX(){return activeX;}
    public int selectedPixelY(){return activeY;}
    private int screenX(float x){return (int)Math.floor((x-originX)/pixelSize);}
    private int screenY(float y){return (int)Math.floor((y-originY)/pixelSize);}
    private boolean inBounds(int x,int y){return x>=0&&y>=0&&x<256&&y<192;}
    private static float averageX(MotionEvent e){
        float sum=0;for(int i=0;i<e.getPointerCount();i++)sum+=e.getX(i);
        return sum/e.getPointerCount();
    }
    private static float averageY(MotionEvent e){
        float sum=0;for(int i=0;i<e.getPointerCount();i++)sum+=e.getY(i);
        return sum/e.getPointerCount();
    }
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    @Override public boolean performClick(){super.performClick();return true;}
}
