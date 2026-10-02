package com.alexzab.z80pocketide.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small six-band accent inspired by the classic ZX Spectrum palette. */
public final class SpectrumStripeView extends View {
    private static final int[] COLORS = {
            Color.rgb(0, 0, 215),
            Color.rgb(215, 0, 0),
            Color.rgb(215, 0, 215),
            Color.rgb(0, 190, 0),
            Color.rgb(0, 190, 190),
            Color.rgb(215, 190, 0)
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SpectrumStripeView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float band = width / COLORS.length;
        float slant = Math.min(height * 1.4f, band * 0.45f);

        for (int i = 0; i < COLORS.length; i++) {
            float left = i * band;
            float right = (i + 1) * band;
            Path path = new Path();
            path.moveTo(left + slant, 0);
            path.lineTo(right + slant, 0);
            path.lineTo(right - slant, height);
            path.lineTo(left - slant, height);
            path.close();
            paint.setColor(COLORS[i]);
            canvas.drawPath(path, paint);
        }
    }
}
