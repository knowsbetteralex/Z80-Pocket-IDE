package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Enlarged 8×8 cell for selecting pixel line (bitmap address) and bit position.
 * Pixel bit does not affect byte address; it controls the mask preview.
 */
public final class PixelByteView extends View {
    public interface OnPixelSelected { void onPixelSelected(int pixelLine, int bit); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OnPixelSelected listener;
    private int pixelLine;
    private int bit;
    private float x0, y0, cell;

    public PixelByteView(Context context, OnPixelSelected listener) {
        super(context);
        this.listener = listener;
        setClickable(true);
        setContentDescription("Selected character 8 by 8 pixel preview, tap scanline and bit");
    }

    public void setSelection(int line, int bitPosition) {
        pixelLine = line;
        bit = bitPosition;
        invalidate();
    }

    public int selectedBit() { return bit; }

    private void frame() {
        cell = Math.min(Math.max(1f, (getWidth() - dp(16)) / 8f),
                Math.max(1f, (getHeight() - dp(16)) / 8f));
        x0 = (getWidth() - cell * 8) / 2f;
        y0 = (getHeight() - cell * 8) / 2f;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        frame();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF9FBF9);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), dp(9), dp(9), paint);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                paint.setColor(y == pixelLine
                        ? (x == bit ? 0xFFF5A35C : 0xFFFFD4A9)
                        : ((x + y) % 2 == 0 ? 0xFFE9F2EC : 0xFFF5F8F5));
                float left = x0 + x * cell, top = y0 + y * cell;
                canvas.drawRect(left, top, left + cell, top + cell, paint);
            }
        paint.setColor(0xFF9CB9A7);
        paint.setStrokeWidth(dp(.75f));
        for (int x = 0; x <= 8; x++) {
            float xx = x0 + x * cell;
            canvas.drawLine(xx, y0, xx, y0 + 8 * cell, paint);
        }
        for (int y = 0; y <= 8; y++) {
            float yy = y0 + y * cell;
            canvas.drawLine(x0, yy, x0 + 8 * cell, yy, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.5f));
        paint.setColor(0xFFE8731F);
        canvas.drawRect(x0, y0 + pixelLine * cell,
                x0 + 8 * cell, y0 + (pixelLine + 1) * cell, paint);
        paint.setColor(0xFF9D4B1B);
        canvas.drawRect(x0 + bit * cell, y0 + pixelLine * cell,
                x0 + (bit + 1) * cell, y0 + (pixelLine + 1) * cell, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                pick(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                pick(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return true;
        }
    }

    private void pick(float x, float y) {
        frame();
        if (x < x0 || y < y0 || x > x0 + 8 * cell || y > y0 + 8 * cell)
            return;
        int newLine = ScreenTouchMath.pixelLine((y - y0) / (8 * cell));
        int newBit = ScreenTouchMath.bit((x - x0) / (8 * cell));
        if (newLine != pixelLine || newBit != bit) {
            pixelLine = newLine;
            bit = newBit;
            if (listener != null) listener.onPixelSelected(pixelLine, bit);
            invalidate();
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dp(float size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }
}
