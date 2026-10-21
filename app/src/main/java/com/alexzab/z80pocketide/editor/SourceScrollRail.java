package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/**
 * Touch-width vertical scrollbar for the source editor.
 *
 * Arrow tap: 15% of viewport. Hold: 330ms pause then repeat every 95ms.
 * Thumb dragging maps linearly to editor's true scroll range. The rail
 * stays in the narrow editor-right overlay, ending ABOVE the Run button.
 */
public final class SourceScrollRail extends View {
    private final CodeEditorView editor;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable idle = () -> {
        if (!dragging && !holding) {
            bright = false;
            invalidate();
        }
    };
    private final Runnable repeated = new Runnable() {
        @Override public void run() {
            if (!holding || heldDirection == 0) return;
            editor.scrollByEditorStep(heldDirection);
            postDelayed(this, 95L);
        }
    };

    private boolean bright;
    private boolean dragging;
    private boolean holding;
    private int heldDirection;
    private float dragInsideThumb;

    public SourceScrollRail(Context context, CodeEditorView editor) {
        super(context);
        this.editor = editor;
        setContentDescription("Source scrollbar: drag thumb, tap or hold arrows");
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
    }

    public void onEditorScroll() {
        bright = true;
        invalidate();
        removeCallbacks(idle);
        postDelayed(idle, 1800L);
    }

    private float arrowHeight() { return dp(44); }
    private float trackTop() { return arrowHeight() + dp(4); }
    private float trackBottom() { return getHeight() - arrowHeight() - dp(4); }
    private float trackLength() { return Math.max(0, trackBottom() - trackTop()); }

    private float thumbLength() {
        float viewport = Math.max(1, editor.getHeight());
        float content = viewport + editor.maxVerticalScroll();
        return Math.min(trackLength(),
                Math.max(dp(60), trackLength() * viewport / content));
    }

    private float thumbTop() {
        int maxScroll = editor.maxVerticalScroll();
        if (maxScroll <= 0) return trackTop();
        return trackTop() + (trackLength() - thumbLength())
                * Math.min(1f, Math.max(0f, (float) editor.getScrollY() / maxScroll));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float cx = w / 2f;
        boolean canScroll = editor.maxVerticalScroll() > 0;
        int alpha = canScroll ? (bright ? 235 : 130) : 65;

        fill.setColor(0xFFE8F2EA);
        fill.setAlpha(bright ? 235 : 155);
        canvas.drawRoundRect(dp(2), dp(2), w - dp(2),
                getHeight() - dp(2), dp(14), dp(14), fill);
        fill.setColor(0xFF9CC5A9);
        fill.setAlpha(alpha);
        canvas.drawRoundRect(cx - dp(3), trackTop(), cx + dp(3),
                trackBottom(), dp(3), dp(3), fill);

        fill.setColor(0xFF35875B);
        fill.setAlpha(alpha);
        drawArrow(canvas, cx, arrowHeight() / 2f, true);
        drawArrow(canvas, cx, getHeight() - arrowHeight() / 2f, false);

        if (canScroll && trackLength() > 0) {
            float top = thumbTop();
            float height = thumbLength();
            fill.setColor(0xFF2E8053);
            fill.setAlpha(bright ? 248 : 174);
            canvas.drawRoundRect(dp(5), top, w - dp(5), top + height,
                    dp(11), dp(11), fill);
            fill.setColor(0xFFE7F5EA);
            fill.setAlpha(bright ? 220 : 145);
            for (int k = -1; k <= 1; k++) {
                float cy = top + height / 2f + k * dp(6);
                canvas.drawRoundRect(cx - dp(7), cy - dp(1), cx + dp(7),
                        cy + dp(1), dp(1), dp(1), fill);
            }
        }
    }

    private void drawArrow(Canvas canvas, float x, float y, boolean up) {
        android.graphics.Path path = new android.graphics.Path();
        float d = dp(8);
        if (up) {
            path.moveTo(x - d, y + d / 2);
            path.lineTo(x, y - d / 2);
            path.lineTo(x + d, y + d / 2);
        } else {
            path.moveTo(x - d, y - d / 2);
            path.lineTo(x, y + d / 2);
            path.lineTo(x + d, y - d / 2);
        }
        path.close();
        canvas.drawPath(path, fill);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (editor.maxVerticalScroll() <= 0) return true;
                getParent().requestDisallowInterceptTouchEvent(true);
                bright = true;
                removeCallbacks(idle);
                float y = event.getY();
                if (y <= arrowHeight() || y >= getHeight() - arrowHeight()) {
                    holding = true;
                    heldDirection = y <= arrowHeight() ? -1 : 1;
                    editor.scrollByEditorStep(heldDirection);
                    postDelayed(repeated, 330L);
                } else {
                    dragging = true;
                    float top = thumbTop();
                    if (y >= top && y <= top + thumbLength()) {
                        dragInsideThumb = y - top;
                    } else {
                        dragInsideThumb = thumbLength() / 2f;
                        scrollThumbTo(y);
                    }
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                if (dragging) scrollThumbTo(event.getY());
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeCallbacks(repeated);
                dragging = false;
                holding = false;
                heldDirection = 0;
                if (getParent() != null)
                    getParent().requestDisallowInterceptTouchEvent(false);
                removeCallbacks(idle);
                postDelayed(idle, 1800L);
                performClick();
                return true;
            default:
                return true;
        }
    }

    private void scrollThumbTo(float y) {
        float available = trackLength() - thumbLength();
        if (available <= 0f) return;
        float fraction = (y - dragInsideThumb - trackTop()) / available;
        int scrollY = Math.round(Math.max(0f, Math.min(1f, fraction))
                * editor.maxVerticalScroll());
        editor.scrollToVerticalOffset(scrollY);
        invalidate();
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(idle);
        removeCallbacks(repeated);
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
