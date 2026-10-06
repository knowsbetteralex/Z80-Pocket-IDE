package com.alexzab.z80pocketide.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.alexzab.z80pocketide.i18n.AppLanguage;

/** Visual 256x192 ZX Spectrum screen-memory map for the reference page. */
public final class SpectrumScreenMapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private AppLanguage language = AppLanguage.EN;

    public SpectrumScreenMapView(Context context) {
        super(context);
        setMinimumHeight(dp(235));
        setBackground(UiStyle.rounded(context, Color.WHITE, 18,
                Color.rgb(207, 222, 213), 1));
    }

    public void setLanguage(AppLanguage language) {
        this.language = language == null ? AppLanguage.EN : language;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = dp(14);
        float titleY = pad + dp(13);

        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(14));
        paint.setColor(Color.rgb(38, 102, 72));
        canvas.drawText(language == AppLanguage.RU
                ? "Экран 256×192 и адреса bitmap"
                : "256×192 display and bitmap addresses", pad, titleY, paint);

        float top = titleY + dp(12);
        float maxW = getWidth() - pad * 2;
        float screenW = Math.min(maxW, dp(300));
        float screenH = screenW * 192f / 256f;
        if (top + screenH > getHeight() - dp(42)) {
            screenH = Math.max(dp(95), getHeight() - top - dp(42));
            screenW = screenH * 256f / 192f;
        }
        float left = (getWidth() - screenW) / 2f;
        RectF screen = new RectF(left, top, left + screenW, top + screenH);

        int[] thirds = {0xFFE7F4EC, 0xFFDDECF7, 0xFFF5E8D9};
        String[] labels = {
                "$4000-$47FF · y 0-63",
                "$4800-$4FFF · y 64-127",
                "$5000-$57FF · y 128-191"
        };

        paint.setStyle(Paint.Style.FILL);
        for (int t = 0; t < 3; t++) {
            float y0 = screen.top + screen.height() * t / 3f;
            float y1 = screen.top + screen.height() * (t + 1) / 3f;
            paint.setColor(thirds[t]);
            canvas.drawRect(screen.left, y0, screen.right, y1, paint);

            paint.setColor(Color.rgb(45, 60, 52));
            paint.setTextSize(Math.max(dp(9), screen.height() / 19f));
            paint.setTypeface(android.graphics.Typeface.MONOSPACE);
            canvas.drawText(labels[t], screen.left + dp(7),
                    y0 + (y1 - y0) * 0.56f, paint);
        }

        // Character-cell grid: 32 columns x 24 rows; use subtle lines.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(0x553D6550);
        for (int x = 0; x <= 32; x += 4) {
            float px = screen.left + screen.width() * x / 32f;
            canvas.drawLine(px, screen.top, px, screen.bottom, paint);
        }
        for (int y = 0; y <= 24; y += 8) {
            float py = screen.top + screen.height() * y / 24f;
            canvas.drawLine(screen.left, py, screen.right, py, paint);
        }

        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(55, 95, 72));
        canvas.drawRect(screen, paint);
        for (int t = 1; t < 3; t++) {
            float y = screen.top + screen.height() * t / 3f;
            canvas.drawLine(screen.left, y, screen.right, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        paint.setTextSize(dp(11));
        paint.setColor(Color.rgb(65, 75, 69));
        String bits = "010 TT LLL RRR CCCCC";
        canvas.drawText(bits, pad, screen.bottom + dp(19), paint);

        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(dp(10));
        canvas.drawText(language == AppLanguage.RU
                        ? "TT=треть · LLL=линия пикселя · RRR=строка 8×8 · CCCCC=столбец байта"
                        : "TT=third · LLL=pixel line · RRR=8×8 row · CCCCC=byte column",
                pad, screen.bottom + dp(35), paint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
