package com.alexzab.z80pocketide.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.widget.Button;

public final class UiStyle {
    private UiStyle() {}

    public static Drawable rounded(Context context, int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) shape.setStroke(dp(context, strokeDp), stroke);
        return shape;
    }

    public static Drawable ripple(Context context, int fill, int radiusDp, int stroke) {
        Drawable content = rounded(context, fill, radiusDp, stroke, 1);
        if (Build.VERSION.SDK_INT >= 21) {
            return new RippleDrawable(ColorStateList.valueOf(0x22000000), content, null);
        }
        return content;
    }

    public static void styleButton(Button button, int fill, int textColor, int radiusDp) {
        button.setBackground(ripple(button.getContext(), fill, radiusDp, 0x22000000));
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(0f);
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
