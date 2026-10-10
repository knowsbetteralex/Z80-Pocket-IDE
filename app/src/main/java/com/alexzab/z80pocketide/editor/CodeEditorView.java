package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    private final Paint gutterPaint = new Paint();
    private final Paint lineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentLinePaint = new Paint();
    private final Paint selectedLinePaint = new Paint();
    private final Paint selectedGutterPaint = new Paint();
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect lineBounds = new Rect();
    private final Set<Integer> selectedLines = new TreeSet<>();
    private Runnable onLineSelectionChanged;
    private Runnable onCaretMoved;
    private int[] instructionSizes;
    private int[] instructionMinCycles;
    private int[] instructionMaxCycles;
    private boolean[] isInstruction;
    private int selectionAnchor = -1;
    private int gutterLastLine = -1;
    private boolean gutterDragging;
    private boolean gutterTouched;
    private int gutterWidth;

    private float downX;
    private float downY;
    private int downScrollX;
    private int downScrollY;
    private int downSelectionStart;
    private int downSelectionEnd;
    private boolean panning;
    private boolean scaling;
    private boolean nativeSelectionGesture;
    private boolean imeVisible;
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

        gutterWidth = dp(46);
        gutterPaint.setColor(Color.rgb(242, 245, 243));
        lineNumberPaint.setTextAlign(Paint.Align.RIGHT);
        lineNumberPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        currentLinePaint.setColor(Color.rgb(236, 247, 240));
        selectedLinePaint.setColor(Color.rgb(213, 234, 224));
        selectedGutterPaint.setColor(Color.rgb(130, 194, 154));
        hintPaint.setColor(Color.rgb(32, 96, 64));
        hintPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        hintBackgroundPaint.setColor(Color.rgb(227, 245, 233));
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

                if (editorPreferences.allUppercase && changeCount > 0) {
                    uppercaseInsertedSegment(s);
                }
                if (newline && editorPreferences.autoIndent) {
                    applyNewLineIndent(s, getSelectionStart());
                    return;
                }
                if (!editorPreferences.allUppercase && editorPreferences.autoUppercase) {
                    uppercaseCurrentLine(s, getSelectionStart());
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

    public void setOnLineSelectionChanged(Runnable listener) {
        onLineSelectionChanged = listener;
    }

    public void setOnCaretMoved(Runnable listener) {
        onCaretMoved = listener;
    }

    public List<Integer> getSelectedLines() {
        return new ArrayList<>(selectedLines);
    }

    public boolean hasLineSelection() {
        return !selectedLines.isEmpty();
    }

    public void clearLineSelection() {
        if (selectedLines.isEmpty()) return;
        selectedLines.clear();
        selectionAnchor = -1;
        gutterLastLine = -1;
        invalidate();
        notifyLineSelection();
    }

    public void setInstructionMetrics(int[] sizes, int[] minCycles, int[] maxCycles,
                                      boolean[] instructionFlags) {
        instructionSizes = sizes == null ? null : sizes.clone();
        instructionMinCycles = minCycles == null ? null : minCycles.clone();
        instructionMaxCycles = maxCycles == null ? null : maxCycles.clone();
        isInstruction = instructionFlags == null ? null : instructionFlags.clone();
        invalidate();
    }

    public void clearInstructionMetrics() {
        instructionSizes = null;
        instructionMinCycles = null;
        instructionMaxCycles = null;
        isInstruction = null;
        invalidate();
    }

    public int currentSourceLine() {
        Layout layout = getLayout();
        if (layout == null) return 0;
        return layout.getLineForOffset(Math.max(0, Math.min(getSelectionStart(), length())));
    }

    private void notifyLineSelection() {
        if (onLineSelectionChanged != null) onLineSelectionChanged.run();
    }

    private void selectGutterLine(int line, boolean drag) {
        Layout layout = getLayout();
        if (layout == null) return;
        line = clamp(line, 0, layout.getLineCount() - 1);
        if (!drag) {
            selectionAnchor = line;
            if (!selectedLines.add(line)) selectedLines.remove(line);
        } else {
            if (selectionAnchor < 0) selectionAnchor = line;
            int low = Math.min(selectionAnchor, line);
            int high = Math.max(selectionAnchor, line);
            for (int i = low; i <= high; i++) selectedLines.add(i);
        }
        gutterLastLine = line;
        invalidate();
        notifyLineSelection();
    }

    private int gestureLine(MotionEvent event) {
        Layout layout = getLayout();
        if (layout == null) return 0;
        int y = Math.round(event.getY() + getScrollY() - getTotalPaddingTop());
        return layout.getLineForVertical(Math.max(0, y));
    }

    /** Explicit command: uppercase the complete real source including labels and strings. */
    public boolean uppercaseEntireDocument() {
        String source = getSourceText();
        String upper = source.toUpperCase(Locale.ROOT);
        if (upper.equals(source)) return false;
        int cursor = Math.max(0, getSelectionStart());
        int oldScrollX = getScrollX();
        int oldScrollY = getScrollY();
        clearLineSelection();
        foldedBlocks.clear();
        autoFormatting = true;
        setText(upper);
        setSelection(Math.min(cursor, length()));
        autoFormatting = false;
        scrollTo(oldScrollX, oldScrollY);
        return true;
    }

    private void uppercaseInsertedSegment(Editable text) {
        int start = Math.max(0, Math.min(changeStart, text.length()));
        int end = Math.max(start, Math.min(start + changeCount, text.length()));
        if (end <= start) return;
        String original = text.subSequence(start, end).toString();
        String upper = original.toUpperCase(Locale.ROOT);
        if (original.equals(upper)) return;
        int caret = getSelectionStart();
        autoFormatting = true;
        text.replace(start, end, upper);
        if (caret >= 0) {
            int delta = upper.length() - original.length();
            setSelection(Math.max(0, Math.min(text.length(),
                    caret <= start ? caret : caret >= end ? caret + delta : start + upper.length())));
        }
        autoFormatting = false;
    }

    /** The keyboard changes the actual visible editor viewport, not the source. */
    public void setImeVisible(boolean visible) {
        if (imeVisible == visible) return;
        imeVisible = visible;
        if (visible) post(this::ensureCaretVisible);
    }

    /** Bring the caret into the resized editor viewport, preserving horizontal pan. */
    public void ensureCaretVisible() {
        if (!imeVisible || !hasFocus() || panning || scaling || gutterTouched) return;
        Layout layout = getLayout();
        if (layout == null || getHeight() <= 0) return;
        int offset = Math.max(0, Math.min(getSelectionStart(), length()));
        int line = layout.getLineForOffset(offset);
        int top = layout.getLineTop(line) + getTotalPaddingTop();
        int bottom = layout.getLineBottom(line) + getTotalPaddingTop();
        int visibleTop = getScrollY() + getCompoundPaddingTop();
        int visibleBottom = getScrollY() + getHeight() - getCompoundPaddingBottom() - dp(8);
        int newY = getScrollY();
        if (bottom > visibleBottom) newY += bottom - visibleBottom;
        else if (top < visibleTop) newY -= visibleTop - top;

        float caretX = layout.getPrimaryHorizontal(offset) + getCompoundPaddingLeft();
        int newX = getScrollX();
        int left = getScrollX() + getCompoundPaddingLeft();
        int right = getScrollX() + getWidth() - getCompoundPaddingRight() - dp(10);
        if (caretX > right) newX += Math.round(caretX - right);
        else if (caretX < left) newX -= Math.round(left - caretX);
        panTo(newX, newY);
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
        clearLineSelection();
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
        clearLineSelection();
        clearInstructionMetrics();
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
        clearLineSelection();
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
        clearLineSelection();
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

    public boolean isFolded() { return !foldedBlocks.isEmpty(); }

    private int countVisibleLines(String text) {
        if (text.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') count++;
        if (text.endsWith("\n")) count--;
        return Math.max(1, count);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        int currentLine = -1;
        int sx = getScrollX();
        int sy = getScrollY();

        if (layout != null) {
            int offset = Math.max(0, Math.min(getSelectionStart(), length()));
            currentLine = layout.getLineForOffset(offset);
            for (Integer line : selectedLines) {
                if (line >= 0 && line < layout.getLineCount()) {
                    int top = layout.getLineTop(line) + getTotalPaddingTop();
                    int bottom = layout.getLineBottom(line) + getTotalPaddingTop();
                    canvas.drawRect(sx, top, sx + getWidth(), bottom, selectedLinePaint);
                }
            }
            if (!selectedLines.contains(currentLine)) {
                int top = layout.getLineTop(currentLine) + getTotalPaddingTop();
                int bottom = layout.getLineBottom(currentLine) + getTotalPaddingTop();
                canvas.drawRect(sx, top, sx + getWidth(), bottom, currentLinePaint);
            }
        }

        super.onDraw(canvas);
        if (layout == null) return;

        canvas.drawRect(sx, sy, sx + gutterWidth, sy + getHeight(), gutterPaint);
        lineNumberPaint.setTextSize(getTextSize() * 0.67f);
        int first = layout.getLineForVertical(Math.max(0, sy - getTotalPaddingTop()));
        int last = layout.getLineForVertical(Math.max(0,
                sy + getHeight() - getTotalPaddingTop()));
        first = Math.max(0, first - 1);
        last = Math.min(layout.getLineCount() - 1, last + 1);

        for (int i = first; i <= last; i++) {
            int baseline = getLineBounds(i, lineBounds);
            if (selectedLines.contains(i)) {
                int top = layout.getLineTop(i) + getTotalPaddingTop();
                int bottom = layout.getLineBottom(i) + getTotalPaddingTop();
                canvas.drawRect(sx, top, sx + gutterWidth, bottom, selectedGutterPaint);
            }
            lineNumberPaint.setColor(i == currentLine
                    ? Color.rgb(47, 125, 88) : Color.rgb(125, 147, 133));
            lineNumberPaint.setFakeBoldText(i == currentLine || selectedLines.contains(i));
            canvas.drawText(String.valueOf(i + 1),
                    sx + gutterWidth - dp(8), baseline, lineNumberPaint);
        }

        if (!isFolded() && currentLine >= 0 && isInstruction != null
                && currentLine < isInstruction.length && isInstruction[currentLine]) {
            String duration = instructionMinCycles[currentLine] == instructionMaxCycles[currentLine]
                    ? String.valueOf(instructionMinCycles[currentLine])
                    : instructionMinCycles[currentLine] + "-" + instructionMaxCycles[currentLine];
            String label = instructionSizes[currentLine] + " B · " + duration + " T";
            hintPaint.setTextSize(Math.max(dp(10), getTextSize() * 0.63f));
            float width = hintPaint.measureText(label) + dp(14);
            float right = sx + getWidth() - dp(5);
            float top = layout.getLineTop(currentLine) + getTotalPaddingTop();
            float bottom = top + Math.min(dp(22), getTextSize() * 1.25f);
            canvas.drawRoundRect(right - width, top, right, bottom, dp(7), dp(7),
                    hintBackgroundPaint);
            canvas.drawText(label, right - width + dp(7), bottom - dp(5), hintPaint);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        invalidate();
        if (onCaretMoved != null) post(onCaretMoved);
        if (imeVisible && hasFocus() && !panning && !scaling && !gutterTouched)
            post(this::ensureCaretVisible);
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN
                && event.getX() <= gutterWidth && getLayout() != null) {
            // Multiple-line gutter selection never enters native cursor-drag mode.
            if (isFolded()) unfoldAll();
            gutterTouched = true;
            gutterDragging = false;
            selectGutterLine(gestureLine(event), false);
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }
        if (gutterTouched) {
            if (action == MotionEvent.ACTION_MOVE) {
                int line = gestureLine(event);
                if (line != gutterLastLine) {
                    gutterDragging = true;
                    selectGutterLine(line, true);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                gutterTouched = false;
                gutterDragging = false;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
            return true;
        }

        scaleDetector.onTouchEvent(event);

        switch (action) {
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
