package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.widget.EditText;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Touch-friendly source editor with panning, pinch zoom, light auto-formatting,
 * a configurable TAB action and reversible label-block folding.
 */
public class CodeEditorView extends EditText {
    private static final String PREFS = "code_editor";
    private static final String PREF_FONT_SP = "font_sp";
    private static final float DEFAULT_FONT_SP = 16f;
    private static final float MIN_FONT_SP = 10f;
    private static final float MAX_FONT_SP = 30f;
    private static final Pattern LABEL_ONLY = Pattern.compile(
            "^[A-Za-z_.$?][A-Za-z0-9_.$?]*\\s*:\\s*(?:;.*)?$");

    private final ScaleGestureDetector scaleDetector;
    private final int touchSlop;
    private final Map<String, String> foldedBlocks = new LinkedHashMap<>();

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

    private EditorPreferences.Snapshot editorPreferences;
    private boolean autoFormatting;
    private boolean presentationChange;
    private int changeStart;
    private int changeBefore;
    private int changeCount;
    private int nextFoldId = 1;

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
        refreshPreferences();

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                changeStart = start;
                changeBefore = count;
                changeCount = after;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changeStart = start;
                changeBefore = before;
                changeCount = count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (autoFormatting || presentationChange || editorPreferences == null) return;
                int cursor = getSelectionStart();
                if (cursor < 0) return;

                boolean newline = changeCount == 1 && changeStart >= 0
                        && changeStart < s.length() && s.charAt(changeStart) == '\n';

                if (newline && editorPreferences.autoIndent) {
                    applyNewLineIndent(s, cursor);
                    return;
                }
                if (editorPreferences.autoUppercase) {
                    uppercaseCurrentLine(s, cursor);
                }
            }
        });

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

    public void refreshPreferences() {
        editorPreferences = EditorPreferences.get(getContext());
    }

    public EditorPreferences.Snapshot getEditorPreferences() {
        return editorPreferences;
    }

    public void insertTabFromSettings() {
        refreshPreferences();
        String unit = SourceFormatter.indentUnit(
                editorPreferences.tabSize, editorPreferences.useSpaces);
        int start = Math.max(0, getSelectionStart());
        int end = Math.max(start, getSelectionEnd());
        getText().replace(start, end, unit);
        setSelection(start + unit.length());
    }

    public void formatAllFromSettings() {
        refreshPreferences();
        String source = getSourceText();
        int oldCursor = Math.max(0, getSelectionStart());
        foldedBlocks.clear();
        String formatted = SourceFormatter.formatDocument(
                source,
                editorPreferences.autoUppercase,
                editorPreferences.autoIndent,
                editorPreferences.tabSize,
                editorPreferences.useSpaces);
        autoFormatting = true;
        setText(formatted);
        setSelection(Math.min(oldCursor, length()));
        autoFormatting = false;
    }

    /**
     * Returns real source text, expanding visual fold marker lines in memory
     * without changing what the user currently sees.
     */
    public String getSourceText() {
        String source = getText().toString();
        for (Map.Entry<String, String> entry : foldedBlocks.entrySet()) {
            source = source.replace(entry.getKey(), entry.getValue());
        }
        return source;
    }

    public void setSourceText(String source) {
        foldedBlocks.clear();
        presentationChange = true;
        setText(source == null ? "" : source);
        presentationChange = false;
    }

    /**
     * Folds the block that starts at the nearest label above the cursor and ends
     * immediately before the next blank line. Toggling on a fold marker unfolds it.
     */
    public boolean toggleFoldAtCursor() {
        refreshPreferences();
        if (!editorPreferences.folding) return false;
        Editable editable = getText();
        if (editable == null || editable.length() == 0) return false;
        int cursor = Math.max(0, Math.min(getSelectionStart(), editable.length()));
        String text = editable.toString();

        for (Map.Entry<String, String> entry : new LinkedHashMap<>(foldedBlocks).entrySet()) {
            int at = text.indexOf(entry.getKey());
            if (at >= 0 && cursor >= at && cursor <= at + entry.getKey().length()) {
                presentationChange = true;
                editable.replace(at, at + entry.getKey().length(), entry.getValue());
                presentationChange = false;
                foldedBlocks.remove(entry.getKey());
                setSelection(Math.min(at, length()));
                return true;
            }
        }

        int labelStart = findNearestLabelStart(text, cursor);
        if (labelStart < 0) return false;
        int labelEnd = lineEnd(text, labelStart);
        if (labelEnd >= text.length()) return false;

        int bodyStart = labelEnd + 1;
        int bodyEnd = findNextBlankLineStart(text, bodyStart);
        if (bodyEnd < 0) bodyEnd = text.length();
        if (bodyEnd <= bodyStart) return false;

        String hidden = text.substring(bodyStart, bodyEnd);
        if (hidden.trim().isEmpty() || hidden.contains("· fold#")) return false;

        int lines = countVisibleLines(hidden);
        String indent = SourceFormatter.indentUnit(
                editorPreferences.tabSize, editorPreferences.useSpaces);
        boolean endsWithNewline = hidden.endsWith("\n");
        String marker = indent + "; ▶ " + lines + (lines == 1 ? " line" : " lines")
                + " · fold#" + (nextFoldId++) + (endsWithNewline ? "\n" : "");

        foldedBlocks.put(marker, hidden);
        presentationChange = true;
        editable.replace(bodyStart, bodyEnd, marker);
        presentationChange = false;
        setSelection(Math.min(bodyStart, length()));
        return true;
    }

    public void unfoldAll() {
        if (foldedBlocks.isEmpty()) return;
        String source = getSourceText();
        foldedBlocks.clear();
        presentationChange = true;
        setText(source);
        presentationChange = false;
    }

    private void applyNewLineIndent(Editable text, int cursor) {
        if (cursor <= 0) return;
        int previousEnd = cursor - 1;
        int previousStart = previousEnd > 0
                ? text.toString().lastIndexOf('\n', previousEnd - 1) + 1 : 0;
        String previousLine = text.subSequence(previousStart, previousEnd).toString();
        String indent = SourceFormatter.indentationForNewLine(
                previousLine, editorPreferences.tabSize, editorPreferences.useSpaces);
        if (indent.isEmpty()) return;

        autoFormatting = true;
        text.insert(cursor, indent);
        setSelection(cursor + indent.length());
        autoFormatting = false;
    }

    private void uppercaseCurrentLine(Editable text, int cursor) {
        String all = text.toString();
        int lineStart = all.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1;
        int nextNewLine = all.indexOf('\n', cursor);
        int lineEnd = nextNewLine < 0 ? all.length() : nextNewLine;
        if (lineStart > lineEnd) return;

        String line = all.substring(lineStart, lineEnd);
        String formatted = SourceFormatter.uppercaseKeywordsInLine(line);
        if (line.equals(formatted)) return;

        int relative = Math.max(0, cursor - lineStart);
        autoFormatting = true;
        text.replace(lineStart, lineEnd, formatted);
        setSelection(Math.min(lineStart + relative, text.length()));
        autoFormatting = false;
    }

    private int findNearestLabelStart(String text, int cursor) {
        int start = lineStart(text, cursor);
        while (start >= 0) {
            int end = lineEnd(text, start);
            String line = text.substring(start, end).trim();
            if (LABEL_ONLY.matcher(line).matches()) return start;
            if (line.isEmpty()) return -1;
            if (start == 0) return -1;
            start = lineStart(text, start - 1);
        }
        return -1;
    }

    private int findNextBlankLineStart(String text, int start) {
        int line = start;
        while (line < text.length()) {
            int end = lineEnd(text, line);
            if (text.substring(line, end).trim().isEmpty()) return line;
            if (end >= text.length()) return text.length();
            line = end + 1;
        }
        return text.length();
    }

    private int lineStart(String text, int position) {
        int p = Math.max(0, Math.min(position, text.length()));
        if (p == 0) return 0;
        int i = text.lastIndexOf('\n', Math.max(0, p - 1));
        return i + 1;
    }

    private int lineEnd(String text, int start) {
        int i = text.indexOf('\n', Math.max(0, start));
        return i < 0 ? text.length() : i;
    }

    private int countVisibleLines(String text) {
        if (text.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') count++;
        if (text.endsWith("\n")) count--;
        return Math.max(1, count);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                panning = false;
                scaling = false;
                nativeSelectionGesture = hasActiveSelection();
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

                if (nativeSelectionGesture || hasActiveSelection()) {
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

    private boolean hasActiveSelection() {
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
