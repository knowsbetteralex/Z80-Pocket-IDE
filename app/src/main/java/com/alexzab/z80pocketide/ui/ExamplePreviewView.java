package com.alexzab.z80pocketide.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.alexzab.z80pocketide.examples.ExamplePrograms;

/** Lightweight expected-result thumbnail for built-in examples. */
public final class ExamplePreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int previewType = ExamplePrograms.PREVIEW_BORDER;

    public ExamplePreviewView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(8, 12, 18));
    }

    public void setPreviewType(int previewType) {
        this.previewType = previewType;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float pad = Math.max(5f, Math.min(w, h) * 0.07f);
        RectF outer = new RectF(pad, pad, w - pad, h - pad);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(15, 22, 30));
        canvas.drawRoundRect(outer, 8f, 8f, paint);

        float border = Math.max(6f, Math.min(w, h) * 0.09f);
        RectF screen = new RectF(outer.left + border, outer.top + border,
                outer.right - border, outer.bottom - border);

        paint.setColor(Color.BLACK);
        canvas.drawRect(screen, paint);

        switch (previewType) {
            case ExamplePrograms.PREVIEW_BORDER:
                drawBorder(canvas, outer, screen);
                break;
            case ExamplePrograms.PREVIEW_RAINBOW:
                drawRainbow(canvas, screen);
                break;
            case ExamplePrograms.PREVIEW_STRIPES:
                drawStripes(canvas, screen);
                break;
            case ExamplePrograms.PREVIEW_CHECKER:
                drawChecker(canvas, screen);
                break;
            case ExamplePrograms.PREVIEW_SMILEY:
                drawSmiley(canvas, screen);
                break;
            case ExamplePrograms.PREVIEW_TEXT:
                drawTextDemo(canvas, screen);
                break;
            case ExamplePrograms.PREVIEW_WIPE:
                drawWipe(canvas, screen);
                break;
            case ExamplePrograms.PREVIEW_KEY:
                drawKey(canvas, outer, screen);
                break;
            default:
                break;
        }
    }

    private void drawBorder(Canvas canvas, RectF outer, RectF screen) {
        int[] colors = {0xFF0046C7, 0xFFE31B23, 0xFFB000B5, 0xFF00A650, 0xFF00B8E6, 0xFFF4D328};
        float band = (screen.top - outer.top) / colors.length;
        for (int i = 0; i < colors.length; i++) {
            paint.setColor(colors[i]);
            canvas.drawRect(outer.left, outer.top + i * band,
                    outer.right, outer.top + (i + 1) * band, paint);
        }
        paint.setColor(Color.BLACK);
        canvas.drawRect(screen, paint);
    }

    private void drawRainbow(Canvas canvas, RectF screen) {
        int[] colors = {0xFF001C7A,0xFFB00000,0xFF8A008A,0xFF008A28,0xFF00A8A8,0xFFD6C400,0xFFFFFFFF,0xFF111111};
        float cw = screen.width() / 8f;
        for (int x = 0; x < 8; x++) {
            paint.setColor(colors[x]);
            canvas.drawRect(screen.left + x * cw, screen.top,
                    screen.left + (x + 1) * cw, screen.bottom, paint);
        }
    }

    private void drawStripes(Canvas canvas, RectF screen) {
        float stripe = Math.max(2f, screen.width() / 28f);
        int i = 0;
        for (float x = screen.left; x < screen.right; x += stripe) {
            paint.setColor((i++ & 1) == 0 ? Color.WHITE : Color.rgb(35, 35, 35));
            canvas.drawRect(x, screen.top, Math.min(x + stripe, screen.right), screen.bottom, paint);
        }
    }

    private void drawChecker(Canvas canvas, RectF screen) {
        int cols = 8, rows = 6;
        float cw = screen.width() / cols;
        float ch = screen.height() / rows;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                paint.setColor(((x + y) & 1) == 0 ? 0xFF1C4FD7 : 0xFFE83B2E);
                canvas.drawRect(screen.left + x * cw, screen.top + y * ch,
                        screen.left + (x + 1) * cw, screen.top + (y + 1) * ch, paint);
            }
        }
    }

    private void drawSmiley(Canvas canvas, RectF screen) {
        paint.setColor(0xFFF4D328);
        float r = Math.min(screen.width(), screen.height()) * 0.31f;
        float cx = screen.centerX();
        float cy = screen.centerY();
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.2f, r * 0.09f, paint);
        canvas.drawCircle(cx + r * 0.35f, cy - r * 0.2f, r * 0.09f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, r * 0.09f));
        canvas.drawArc(new RectF(cx-r*0.48f, cy-r*0.15f, cx+r*0.48f, cy+r*0.55f), 15, 150, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTextDemo(Canvas canvas, RectF screen) {
        paint.setColor(Color.rgb(15, 15, 25));
        canvas.drawRect(screen, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.max(11f, screen.width() / 11f));
        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        canvas.drawText("HELLO", screen.left + 6, screen.centerY() - 3, paint);
        paint.setColor(0xFFF4D328);
        canvas.drawText("Z80!", screen.left + 6, screen.centerY() + paint.getTextSize() + 2, paint);
    }

    private void drawWipe(Canvas canvas, RectF screen) {
        paint.setColor(Color.WHITE);
        canvas.drawRect(screen.left, screen.top, screen.centerX() + screen.width() * 0.12f, screen.bottom, paint);
        paint.setColor(Color.rgb(40, 40, 40));
        canvas.drawRect(screen.centerX() + screen.width() * 0.12f, screen.top, screen.right, screen.bottom, paint);
    }

    private void drawKey(Canvas canvas, RectF outer, RectF screen) {
        paint.setColor(0xFF0046C7);
        canvas.drawRoundRect(outer, 8f, 8f, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(screen, paint);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        float kw = screen.width() * 0.42f;
        float kh = screen.height() * 0.26f;
        RectF key = new RectF(screen.centerX()-kw/2, screen.centerY()-kh/2,
                screen.centerX()+kw/2, screen.centerY()+kh/2);
        canvas.drawRoundRect(key, 5f, 5f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(10f, kh * 0.5f));
        canvas.drawText("KEY", screen.centerX(), screen.centerY()+paint.getTextSize()*0.33f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
