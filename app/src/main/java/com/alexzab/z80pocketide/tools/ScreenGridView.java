package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * ZX Spectrum 32x24 character/attribute locator. 8x8 screen thirds are
 * shaded alternately and separated by prominent horizontal lines.
 * A touch picks a character cell (one bitmap byte column / attribute byte).
 */
public final class ScreenGridView extends View {
    public interface OnCellSelected { void onCellSelected(int row, int col); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OnCellSelected listener;
    private int charRow, column, pixelLine;
    private float x0, y0, step;

    public ScreenGridView(Context context, OnCellSelected listener) {
        super(context);
        this.listener = listener;
        setContentDescription("ZX Spectrum screen 32 by 24 character positions");
        setClickable(true);
        setMinimumHeight(dp(160));
    }

    public void setSelection(ScreenAddress selection) {
        charRow = selection.characterRow();
        column = selection.column;
        pixelLine = selection.pixelLine;
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = View.MeasureSpec.getSize(widthSpec);
        if (w <= 0) w = dp(320);
        int preferredH = Math.round((w - dp(20)) * 24f / 32f) + dp(20);
        setMeasuredDimension(w, resolveSize(preferredH, heightSpec));
    }

    private void frame() {
        step = Math.min(Math.max(1f, (getWidth() - dp(20)) / 32f),
                Math.max(1f, (getHeight() - dp(20)) / 24f));
        x0 = (getWidth() - step * 32f) / 2f;
        y0 = (getHeight() - step * 24f) / 2f;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        frame();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF9FBF9);
        c.drawRoundRect(0, 0, getWidth(), getHeight(), dp(10), dp(10), paint);

        for (int third = 0; third < 3; third++) {
            paint.setColor(third == 1 ? 0xFFE6F1E9 : 0xFFF0F7F1);
            c.drawRect(x0, y0 + third * 8 * step, x0 + 32 * step,
                    y0 + (third + 1) * 8 * step, paint);
        }

        // Faint row and column guides for the selected byte position.
        paint.setColor(0x22238B58);
        c.drawRect(x0 + column * step, y0, x0 + (column + 1) * step,
                y0 + 24 * step, paint);
        c.drawRect(x0, y0 + charRow * step, x0 + 32 * step,
                y0 + (charRow + 1) * step, paint);

        for (int col = 0; col <= 32; col++) {
            paint.setColor(col % 8 == 0 ? 0xFF65927A : 0x8097B7A3);
            paint.setStrokeWidth(col % 8 == 0 ? dp(1.2f) : dp(.5f));
            float x = x0 + step * col;
            c.drawLine(x, y0, x, y0 + 24 * step, paint);
        }
        for (int row = 0; row <= 24; row++) {
            paint.setColor(row % 8 == 0 ? 0xFF338B5D : 0x8097B7A3);
            paint.setStrokeWidth(row % 8 == 0 ? dp(2) : dp(.5f));
            float y = y0 + step * row;
            c.drawLine(x0, y, x0 + 32 * step, y, paint);
        }

        float left = x0 + column * step;
        float top = y0 + charRow * step;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFCF8A);
        c.drawRect(left + dp(.8f), top + dp(.8f),
                left + step - dp(.8f), top + step - dp(.8f), paint);
        // Position of one of the eight pixel scanlines within the selected cell.
        float py = top + (pixelLine + .5f) * step / 8f;
        paint.setColor(0xFFB54D13);
        paint.setStrokeWidth(Math.max(dp(1f), step / 8f));
        c.drawLine(left + dp(1), py, left + step - dp(1), py, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xFFE8731F);
        c.drawRect(left, top, left + step, top + step, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private boolean select(float x, float y) {
        frame();
        if (x < x0 || x > x0 + 32 * step
                || y < y0 || y > y0 + 24 * step) return false;
        int c = ScreenTouchMath.column((x - x0) / (32f * step));
        int r = ScreenTouchMath.characterRow((y - y0) / (24f * step));
        if (charRow != r || column != c) {
            charRow = r;
            column = c;
            if (listener != null) listener.onCellSelected(r, c);
            invalidate();
        }
        return true;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                select(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                select(event.getX(), event.getY());
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

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
