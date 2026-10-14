package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/** Pinch-zoomable pixel grid; paint with one finger or use Hand mode to pan. */
public final class SpriteCanvasView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lines = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector pinch;
    private SpriteData data;
    private SpriteData.Mode brushMode = SpriteData.Mode.INK;
    private int brushColor = 15;
    private boolean handMode;
    private float cell = 14f, originX = 16f, originY = 16f;
    private float lastTouchX, lastTouchY;
    private int lastDotX = -1, lastDotY = -1;
    private boolean pinching;
    private Runnable onChange;

    public SpriteCanvasView(Context context, SpriteData initial) {
        super(context);
        data = initial;
        setBackground(UiStyle.rounded(context, Color.WHITE, 18, 0xFFCBDDD1, 1));
        setMinimumHeight(dp(270));
        pinch = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScaleBegin(ScaleGestureDetector d) {
                        pinching = true;
                        return true;
                    }
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        zoomAt(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                        return true;
                    }
                });
    }

    public void setModel(SpriteData model) {
        data = model;
        resetView();
        invalidate();
    }

    public void setBrush(SpriteData.Mode mode, int color, boolean hand) {
        brushMode = mode;
        brushColor = color;
        handMode = hand;
    }

    public void setOnChange(Runnable callback) { onChange = callback; }

    public void resetView() {
        if (data == null || getWidth() <= 0) return;
        cell = Math.max(3f, Math.min(24f,
                Math.min((getWidth() - dp(20f)) / data.width(),
                        (getHeight() - dp(20f)) / data.height())));
        originX = (getWidth() - data.width() * cell) / 2f;
        originY = (getHeight() - data.height() * cell) / 2f;
        invalidate();
    }

    public void zoom(float factor) { zoomAt(factor, getWidth()/2f, getHeight()/2f); }

    private void zoomAt(float factor, float fx, float fy) {
        float next = Math.max(3f, Math.min(80f, cell * factor));
        originX = fx - (fx - originX) * next / cell;
        originY = fy - (fy - originY) * next / cell;
        cell = next;
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw == 0 || oldh == 0) resetView();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;
        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        for (int y = 0; y < data.height(); y++) {
            for (int x = 0; x < data.width(); x++) {
                float left = originX + x * cell, top = originY + y * cell;
                if (left + cell < 0 || top + cell < 0 || left > getWidth() || top > getHeight()) continue;
                fill.setColor(data.displayedColor(x, y));
                canvas.drawRect(left, top, left + cell + .25f, top + cell + .25f, fill);
            }
        }
        float w = data.width() * cell, h = data.height() * cell;
        if (cell > 6) {
            lines.setStyle(Paint.Style.STROKE);
            lines.setStrokeWidth(.65f);
            lines.setColor(0x55808080);
            for (int x = 1; x < data.width(); x++) {
                float xx = originX + x * cell;
                if (xx >= 0 && xx <= getWidth())
                    canvas.drawLine(xx, originY, xx, originY + h, lines);
            }
            for (int y = 1; y < data.height(); y++) {
                float yy = originY + y * cell;
                if (yy >= 0 && yy <= getHeight())
                    canvas.drawLine(originX, yy, originX + w, yy, lines);
            }
        }
        lines.setColor(0xFF426C51);
        lines.setStrokeWidth(dp(1.6f));
        for (int x = 0; x <= data.cellsX(); x++) {
            float xx = originX + x * 8 * cell;
            canvas.drawLine(xx, originY, xx, originY + h, lines);
        }
        for (int y = 0; y <= data.cellsY(); y++) {
            float yy = originY + y * 8 * cell;
            canvas.drawLine(originX, yy, originX + w, yy, lines);
        }
        lines.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        pinch.onTouchEvent(event);
        if (action == MotionEvent.ACTION_DOWN) {
            pinching = false;
            lastTouchX = event.getX(); lastTouchY = event.getY();
            lastDotX = lastDotY = -1;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            if (!handMode) drawAt(event.getX(), event.getY());
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            pinching = true;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (pinching || event.getPointerCount() > 1 || pinch.isInProgress()) return true;
            if (handMode) {
                originX += event.getX() - lastTouchX;
                originY += event.getY() - lastTouchY;
                invalidate();
            } else drawAt(event.getX(), event.getY());
            lastTouchX = event.getX();
            lastTouchY = event.getY();
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            pinching = false;
            lastDotX = lastDotY = -1;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return true;
    }

    private void drawAt(float px, float py) {
        int x = (int)Math.floor((px - originX) / cell);
        int y = (int)Math.floor((py - originY) / cell);
        if (x < 0 || y < 0 || x >= data.width() || y >= data.height()) return;
        if (lastDotX == x && lastDotY == y) return;
        // Draw a continuous line even when the finger crosses pixels quickly.
        int sx = lastDotX < 0 ? x : lastDotX;
        int sy = lastDotY < 0 ? y : lastDotY;
        int count = Math.max(Math.abs(x-sx), Math.abs(y-sy));
        for (int i = 0; i <= count; i++) {
            int xx = count == 0 ? x : sx + (x-sx)*i/count;
            int yy = count == 0 ? y : sy + (y-sy)*i/count;
            data.paint(xx, yy, brushColor, brushMode);
        }
        lastDotX = x; lastDotY = y;
        invalidate();
        if (onChange != null) onChange.run();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
