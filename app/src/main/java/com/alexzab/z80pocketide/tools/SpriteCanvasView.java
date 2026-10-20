package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.alexzab.z80pocketide.ui.UiStyle;

/**
 * A one-finger stroke paints or erases depending on the starting pixel.
 * Two fingers always transform the viewport: move to pan and pinch to zoom.
 */
public final class SpriteCanvasView extends View {
    private final Paint pixels = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private SpriteData data;
    private float cell = 14f;
    private float originX;
    private float originY;
    private float focusX, focusY;
    private int previousPixelX = -1, previousPixelY = -1;
    private boolean strokeFills;
    private boolean transforming;
    private boolean maskMode;
    private Runnable onChange;

    public SpriteCanvasView(Context context, SpriteData initial) {
        super(context);
        data = initial;
        setBackground(UiStyle.rounded(context, Color.WHITE, 18, 0xFFCBDDD1, 1));
        setMinimumHeight(Math.round(dp(270)));
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                        transforming = true;
                        return true;
                    }
                    @Override public boolean onScale(ScaleGestureDetector detector) {
                        zoomAt(detector.getScaleFactor(),
                                detector.getFocusX(), detector.getFocusY());
                        return true;
                    }
                });
    }

    public void setModel(SpriteData model) {
        data = model;
        fitHeight();
    }

    public void setOnChange(Runnable callback) { onChange = callback; }

    public void setMaskMode(boolean enabled) {
        maskMode = enabled;
        previousPixelX = previousPixelY = -1;
        invalidate();
    }

    /** Fit to the entire available height, even if the resulting width exceeds the viewport. */
    public void fitHeight() {
        if (data == null || getHeight() <= 0 || getWidth() <= 0) {
            post(this::fitIfReady);
            return;
        }
        cell = Math.max(2.5f, (getHeight() - 2f * dp(10)) / data.height());
        originX = (getWidth() - data.width() * cell) / 2f;
        originY = (getHeight() - data.height() * cell) / 2f;
        invalidate();
    }

    private void fitIfReady() {
        if (getWidth() > 0 && getHeight() > 0) fitHeight();
    }

    private void zoomAt(float factor, float centerX, float centerY) {
        if (Float.isNaN(factor) || factor <= 0f) return;
        float next = Math.max(2.5f, Math.min(240f, cell * factor));
        originX = centerX - (centerX - originX) * (next / cell);
        originY = centerY - (centerY - originY) * (next / cell);
        cell = next;
        invalidate();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (oldWidth == 0 || oldHeight == 0) fitHeight();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;

        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        int startX = Math.max(0, (int)Math.floor(-originX / cell));
        int endX = Math.min(data.width(), (int)Math.ceil((getWidth() - originX) / cell));
        int startY = Math.max(0, (int)Math.floor(-originY / cell));
        int endY = Math.min(data.height(), (int)Math.ceil((getHeight() - originY) / cell));

        for (int y = startY; y < endY; y++) {
            float top = originY + y * cell;
            for (int x = startX; x < endX; x++) {
                float left = originX + x * cell;
                if (maskMode && !data.getMask(x, y)) {
                    // Two-by-two translucent checkerboard within a masked-out
                    // sprite pixel. Underlying INK/PAPER is not altered.
                    float half = cell / 2f;
                    pixels.setColor(0xFFD8D8D8);
                    canvas.drawRect(left, top, left + cell + .3f, top + cell + .3f, pixels);
                    pixels.setColor(0xFF606060);
                    canvas.drawRect(left, top, left + half, top + half, pixels);
                    canvas.drawRect(left + half, top + half, left + cell, top + cell, pixels);
                } else {
                    pixels.setColor(data.displayedColor(x, y));
                    canvas.drawRect(left, top, left + cell + .3f, top + cell + .3f, pixels);
                }
            }
        }

        float spriteW = data.width() * cell, spriteH = data.height() * cell;
        // Draw grid AFTER all pixels, including filled ones, for equal contrast.
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(dp(1.35f));
        grid.setColor(0xAF858E88);
        for (int x = Math.max(0,startX); x <= endX; x++) {
            float px = originX + x * cell;
            canvas.drawLine(px, originY, px, originY + spriteH, grid);
        }
        for (int y = Math.max(0,startY); y <= endY; y++) {
            float py = originY + y * cell;
            canvas.drawLine(originX, py, originX + spriteW, py, grid);
        }

        // 8x8 cell edges thicker; semi-transparent on INK and PAPER alike.
        grid.setStrokeWidth(dp(2.3f));
        grid.setColor(0xC17A9F88);
        for (int x = 0; x <= data.cellsX(); x++) {
            float px = originX + x * 8 * cell;
            canvas.drawLine(px, originY, px, originY + spriteH, grid);
        }
        for (int y = 0; y <= data.cellsY(); y++) {
            float py = originY + y * 8 * cell;
            canvas.drawLine(originX, py, originX + spriteW, py, grid);
        }
        grid.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        // The detector handles zoom; two-pointer centroid motion handles panning.
        scaleDetector.onTouchEvent(event);

        if (action == MotionEvent.ACTION_DOWN) {
            transforming = false;
            previousPixelX = previousPixelY = -1;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            int x = xPixel(event.getX()), y = yPixel(event.getY());
            if (valid(x,y)) {
                strokeFills = !(maskMode ? data.getMask(x, y) : data.getPixel(x, y));
                strokeTo(x,y);
            }
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            transforming = true;
            previousPixelX = previousPixelY = -1;
            focusX = averageX(event);
            focusY = averageY(event);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() >= 2) {
                float midX = averageX(event), midY = averageY(event);
                if (transforming) {
                    originX += midX - focusX;
                    originY += midY - focusY;
                    invalidate();
                }
                focusX = midX;
                focusY = midY;
                return true;
            }
            // Never accidentally draw when lifting one finger out of a pinch.
            if (!transforming) {
                int x = xPixel(event.getX()), y = yPixel(event.getY());
                if (valid(x,y)) strokeTo(x,y);
                else previousPixelX = previousPixelY = -1;
            }
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_UP) {
            transforming = true;
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_UP) performClick();
            previousPixelX = previousPixelY = -1;
            transforming = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private float averageX(MotionEvent event) {
        float value = 0f;
        for (int i = 0; i < event.getPointerCount(); i++) value += event.getX(i);
        return value / event.getPointerCount();
    }

    private float averageY(MotionEvent event) {
        float value = 0f;
        for (int i = 0; i < event.getPointerCount(); i++) value += event.getY(i);
        return value / event.getPointerCount();
    }

    private int xPixel(float x) { return (int)Math.floor((x - originX) / cell); }
    private int yPixel(float y) { return (int)Math.floor((y - originY) / cell); }
    private boolean valid(int x, int y) {
        return x >= 0 && y >= 0 && x < data.width() && y < data.height();
    }

    private void strokeTo(int x, int y) {
        if (previousPixelX == x && previousPixelY == y) return;
        int fromX = previousPixelX < 0 ? x : previousPixelX;
        int fromY = previousPixelY < 0 ? y : previousPixelY;
        int steps = Math.max(Math.abs(x - fromX), Math.abs(y - fromY));
        for (int i = 0; i <= steps; i++) {
            int xx = steps == 0 ? x : fromX + (x - fromX) * i / steps;
            int yy = steps == 0 ? y : fromY + (y - fromY) * i / steps;
            if (maskMode) data.setMask(xx, yy, strokeFills);
            else data.setPixel(xx, yy, strokeFills);
        }
        previousPixelX = x;
        previousPixelY = y;
        invalidate();
        if (onChange != null) onChange.run();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
