package com.alexzab.z80pocketide.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.widget.Button;

public final class UiStyle {
    private static final int GREEN = 0xFF2F7D58;
    private static final int GREEN_DARK = 0xFF235E43;
    private static final int GREEN_SOFT = 0xFFE3F1E8;
    private static final int GREEN_SOFT_STROKE = 0xFFB8D7C5;
    private static final int DISABLED_BG = 0xFFDDE2DE;
    private static final int DISABLED_TEXT = 0xFF8A938D;

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
        common(button);
    }

    /** Primary action: saturated green while enabled, clearly grey while disabled. */
    public static void styleGreenButton(Button button) {
        Context context = button.getContext();
        StateListDrawable backgrounds = new StateListDrawable();
        backgrounds.addState(new int[] {-android.R.attr.state_enabled},
                rounded(context, DISABLED_BG, 16, 0xFFC7CEC9, 1));
        backgrounds.addState(new int[] {},
                ripple(context, GREEN, 16, GREEN_DARK));
        button.setBackground(backgrounds);

        int[][] states = {
                new int[] {-android.R.attr.state_enabled},
                new int[] {}
        };
        int[] colors = {DISABLED_TEXT, 0xFFFFFFFF};
        button.setTextColor(new ColorStateList(states, colors));
        common(button);
    }

    /** Secondary action: soft mint button, also with a real disabled state. */
    public static void styleSoftGreenButton(Button button) {
        Context context = button.getContext();
        StateListDrawable backgrounds = new StateListDrawable();
        backgrounds.addState(new int[] {-android.R.attr.state_enabled},
                rounded(context, 0xFFEEF0EE, 14, 0xFFD8DDDA, 1));
        backgrounds.addState(new int[] {},
                ripple(context, GREEN_SOFT, 14, GREEN_SOFT_STROKE));
        button.setBackground(backgrounds);

        int[][] states = {
                new int[] {-android.R.attr.state_enabled},
                new int[] {}
        };
        int[] colors = {DISABLED_TEXT, GREEN_DARK};
        button.setTextColor(new ColorStateList(states, colors));
        common(button);
    }

    private static void common(Button button) {
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(0f);
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
