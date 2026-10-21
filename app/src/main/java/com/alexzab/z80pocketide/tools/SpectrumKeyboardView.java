package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * A touchable, visually faithful four-row ZX Spectrum 40-key keyboard.
 * Key placement is physical; scan-port ordering stays in SpectrumKeyboardMatrix.
 */
public final class SpectrumKeyboardView extends View {
    public interface OnKeyTap { void onKeyTap(String name); }

    private final SpectrumKeyboardMatrix matrix;
    private final OnKeyTap callback;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int touchRow = -1, touchColumn = -1;
    private float left, top, cellW, cellH;

    public SpectrumKeyboardView(Context context, SpectrumKeyboardMatrix matrix, OnKeyTap callback) {
        super(context);
        this.matrix = matrix;
        this.callback = callback;
        setClickable(true);
        setContentDescription("ZX Spectrum keyboard; tap a key to toggle it");
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (width <= 0) width = dp(340);
        int height = Math.round(dp(63) + 4f * Math.max(dp(33), (width - dp(22)) / 10f + dp(9)));
        setMeasuredDimension(width, resolveSize(height, heightSpec));
    }

    private void geometry() {
        left = dp(11);
        top = dp(43);
        cellW = Math.max(1f, (getWidth() - dp(22)) / 10f);
        cellH = Math.max(1f, (getHeight() - top - dp(14)) / 4f);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        geometry();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF242B2A);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, dp(16), dp(16), paint);

        paint.setColor(0xFFDFE8DE);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(15));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("ZX SPECTRUM", dp(15), dp(25), paint);
        paint.setFakeBoldText(false);

        int[] rainbow = {0xFFE65C56, 0xFFF5AD52, 0xFFF4D95C, 0xFF6EBA88, 0xFF61A1E8};
        float rainbowX = getWidth() - dp(82);
        for (int i = 0; i < rainbow.length; i++) {
            paint.setColor(rainbow[i]);
            canvas.drawRoundRect(rainbowX + i * dp(12), dp(15),
                    rainbowX + i * dp(12) + dp(12), dp(24),
                    dp(1), dp(1), paint);
        }

        for (int r = 0; r < SpectrumKeyboardMatrix.PHYSICAL.length; r++) {
            for (int k = 0; k < SpectrumKeyboardMatrix.PHYSICAL[r].length; k++) {
                String name = SpectrumKeyboardMatrix.PHYSICAL[r][k];
                boolean selected = matrix.isPressed(name);
                float x = left + k * cellW + dp(1.2f);
                float y = top + r * cellH + dp(1.8f);
                float right = x + cellW - dp(3);
                float bottom = y + cellH - dp(4);
                float radius = dp(4.5f);

                paint.setColor(0xFF111716);
                rect.set(x + dp(1.3f), y + dp(2.3f), right + dp(1.3f), bottom + dp(2.3f));
                canvas.drawRoundRect(rect, radius, radius, paint);

                paint.setColor(selected ? 0xFFFBC66D : 0xFFDCE0D8);
                rect.set(x, y, right, bottom);
                canvas.drawRoundRect(rect, radius, radius, paint);

                paint.setColor(selected ? 0xFFE9872D : 0xFFB5C0B5);
                canvas.drawRect(x + dp(2), bottom - dp(3.5f), right - dp(2), bottom, paint);

                String[] legend;
                if ("CAPS SHIFT".equals(name)) legend = new String[]{"CAPS", "SHIFT"};
                else if ("SYMBOL SHIFT".equals(name)) legend = new String[]{"SYM", "SHIFT"};
                else legend = new String[]{name};

                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                paint.setColor(0xFF22352A);
                float available = Math.max(dp(12), right - x - dp(3));
                for (int line = 0; line < legend.length; line++) {
                    String text = legend[line];
                    float font = dp(legend.length == 2 ? 8 : ("ENTER".equals(name) || "SPACE".equals(name) ? 9 : 13));
                    paint.setTextSize(font);
                    if (paint.measureText(text) > available)
                        paint.setTextSize(Math.max(dp(5.5f), font * available / paint.measureText(text)));
                    float baseline = (y + bottom) / 2f -
                            (paint.ascent() + paint.descent()) / 2f +
                            (line - (legend.length - 1) / 2f) * dp(9);
                    canvas.drawText(text, (x + right) / 2f, baseline, paint);
                }
                if (selected) {
                    paint.setColor(0xFFAD5419);
                    rect.set(x, y, right, bottom);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1.5f));
                    canvas.drawRoundRect(rect, radius, radius, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
        }
    }

    private int[] hit(float x, float y) {
        geometry();
        int col = (int)Math.floor((x - left) / cellW);
        int row = (int)Math.floor((y - top) / cellH);
        if (x < left || y < top || row < 0 || row >= 4 || col < 0 || col >= 10)
            return null;
        return new int[]{row, col};
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                int[] hit = hit(event.getX(), event.getY());
                touchRow = hit == null ? -1 : hit[0];
                touchColumn = hit == null ? -1 : hit[1];
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_UP: {
                int[] hit = hit(event.getX(), event.getY());
                if (hit != null && hit[0] == touchRow && hit[1] == touchColumn
                        && callback != null)
                    callback.onKeyTap(SpectrumKeyboardMatrix.PHYSICAL[hit[0]][hit[1]]);
                touchRow = touchColumn = -1;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                touchRow = touchColumn = -1;
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

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
