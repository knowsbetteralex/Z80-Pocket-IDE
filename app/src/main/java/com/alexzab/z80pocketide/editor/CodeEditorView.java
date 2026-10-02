package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.widget.EditText;

/**
 * EditText tuned for source code on a touch screen.
 *
 * A tap keeps the normal EditText cursor/selection behaviour. Once a one-finger
 * gesture crosses touch slop it becomes a pan gesture instead of dragging the
 * insertion cursor. Native selection mode is deliberately left untouched so
 * Android's selection handles can still be dragged. A two-finger pinch changes
 * the editor font size.
 */
public class CodeEditorView extends EditText {
    private static final String PREFS = "code_editor";
    private static final String PREF_FONT_SP = "font_sp";
    private static final float DEFAULT_FONT_SP = 16f;
    private static final float MIN_FONT_SP = 10f;
    private static final float MAX_FONT_SP = 30f;

    private final ScaleGestureDetector scaleDetector;
    private final int touchSlop;

    private float downX;
    private float downY;
    private int downScrollX;
    private int downScrollY;
    private int downSelectionStart;
    private int downSelectionEnd;
    private boolean panning;
    private boolean scaling;
    private boolean nativeSelectionGesture;
    private float fontSp;

    public CodeEditorView(Context context) {
        this(context, null);
    }

    public CodeEditorView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public CodeEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        fontSp = preferences().getFloat(PREF_FONT_SP, DEFAULT_FONT_SP);
        fontSp = clamp(fontSp, MIN_FONT_SP, MAX_FONT_SP);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp);
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);
        setOverScrollMode(OVER_SCROLL_NEVER);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        scaling = true;
                        panning = false;
                        cancelNativeTouch();
                        restoreSelection();
                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float next = clamp(fontSp * detector.getScaleFactor(),
                                MIN_FONT_SP, MAX_FONT_SP);
                        if (Math.abs(next - fontSp) < 0.02f) return true;
                        fontSp = next;
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSp);
                        post(CodeEditorView.this::clampScrollPosition);
                        restoreSelection();
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        preferences().edit().putFloat(PREF_FONT_SP, fontSp).apply();
                        post(CodeEditorView.this::clampScrollPosition);
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                panning = false;
                scaling = false;
                nativeSelectionGesture = hasSelection();
                downX = event.getX();
                downY = event.getY();
                downScrollX = getScrollX();
                downScrollY = getScrollY();
                downSelectionStart = getSelectionStart();
                downSelectionEnd = getSelectionEnd();
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    scaling = true;
                    panning = false;
                    cancelNativeTouch();
                    restoreSelection();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (scaling || event.getPointerCount() >= 2 || scaleDetector.isInProgress()) {
                    scaling = true;
                    restoreSelection();
                    return true;
                }

                // While text is selected, keep Android's native selection-handle
                // gestures intact. This is the one case where a drag is not
                // converted to canvas panning.
                if (nativeSelectionGesture || hasSelection()) {
                    return super.onTouchEvent(event);
                }

                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (!panning && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    panning = true;
                    cancelNativeTouch();
                    restoreSelection();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (panning) {
                    panTo(downScrollX - Math.round(dx), downScrollY - Math.round(dy));
                    restoreSelection();
                    return true;
                }
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_POINTER_UP:
                if (scaling) {
                    restoreSelection();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (panning || scaling) {
                    restoreSelection();
                    panning = false;
                    scaling = false;
                    nativeSelectionGesture = false;
                    return true;
                }
                nativeSelectionGesture = false;
                // A genuine tap reaches normal EditText handling and positions
                // the cursor at the tapped character.
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (panning || scaling) restoreSelection();
                panning = false;
                scaling = false;
                nativeSelectionGesture = false;
                return super.onTouchEvent(event);

            default:
                break;
        }

        return scaling || panning || super.onTouchEvent(event);
    }

    private boolean hasSelection() {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        return start >= 0 && end >= 0 && start != end;
    }

    private void restoreSelection() {
        if (downSelectionStart < 0 || downSelectionEnd < 0) return;
        int length = length();
        int start = Math.min(downSelectionStart, length);
        int end = Math.min(downSelectionEnd, length);
        try {
            setSelection(start, end);
        } catch (IndexOutOfBoundsException ignored) {
            // Text may have changed between gesture events.
        }
    }

    private void cancelNativeTouch() {
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now,
                MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    private void panTo(int x, int y) {
        Layout layout = getLayout();
        if (layout == null) {
            scrollTo(0, 0);
            return;
        }

        int maxX = maxScrollX(layout);
        int maxY = maxScrollY(layout);
        scrollTo(clamp(x, 0, maxX), clamp(y, 0, maxY));
    }

    private void clampScrollPosition() {
        panTo(getScrollX(), getScrollY());
    }

    private int maxScrollX(Layout layout) {
        float widest = 0f;
        for (int i = 0; i < layout.getLineCount(); i++) {
            widest = Math.max(widest, layout.getLineWidth(i));
        }
        int content = (int) Math.ceil(widest)
                + getCompoundPaddingLeft() + getCompoundPaddingRight();
        return Math.max(0, content - getWidth());
    }

    private int maxScrollY(Layout layout) {
        int content = layout.getHeight()
                + getCompoundPaddingTop() + getCompoundPaddingBottom();
        return Math.max(0, content - getHeight());
    }

    private SharedPreferences preferences() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
