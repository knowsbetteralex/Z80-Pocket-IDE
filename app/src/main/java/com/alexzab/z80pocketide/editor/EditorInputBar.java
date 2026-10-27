package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.ui.UiStyle;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Contextual accessory directly above the Android IME, with optional quick
 * punctuation and label-completion rows. Neither row can steal keyboard focus.
 */
public final class EditorInputBar extends LinearLayout {
    private static final String[] SYMBOLS = {"_", ",", "'", ":", "#", "%", "$"};
    private static final int SUGGESTION_LIMIT = 7;
    private static final long COMPLETION_IDLE_MS = 280L;

    private static final ExecutorService COMPLETION_WORKER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "z80-label-completion");
                t.setDaemon(true);
                return t;
            });

    private final CodeEditorView editor;
    private final boolean russian;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HorizontalScrollView symbolScroll;
    private final HorizontalScrollView suggestionScroll;
    private final LinearLayout suggestionItems;
    private final Runnable refresh = this::lookupSuggestions;
    private boolean keyboardVisible;
    private boolean quickSymbols;
    private boolean labelCompletion;
    private long generation;

    public EditorInputBar(Context context, CodeEditorView editor, boolean russian) {
        super(context);
        this.editor = editor;
        this.russian = russian;
        setOrientation(VERTICAL);
        setBackground(UiStyle.rounded(context,
                0xFFF8FBF8, 13, 0xFFBCD8C5, 1));
        setPadding(dp(5), dp(3), dp(5), dp(3));
        setVisibility(GONE);
        setFocusable(false);
        setFocusableInTouchMode(false);

        suggestionScroll = new HorizontalScrollView(context);
        suggestionScroll.setHorizontalScrollBarEnabled(false);
        suggestionScroll.setFocusable(false);
        suggestionScroll.setFocusableInTouchMode(false);
        suggestionScroll.setFillViewport(false);
        suggestionScroll.setVisibility(GONE);
        suggestionItems = new LinearLayout(context);
        suggestionItems.setGravity(Gravity.CENTER_VERTICAL);
        suggestionItems.setPadding(dp(4), 0, dp(4), 0);
        suggestionScroll.addView(suggestionItems);
        addView(suggestionScroll, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(41)));

        symbolScroll = new HorizontalScrollView(context);
        symbolScroll.setHorizontalScrollBarEnabled(false);
        symbolScroll.setFocusable(false);
        symbolScroll.setFocusableInTouchMode(false);
        symbolScroll.setFillViewport(true);
        LinearLayout symbols = new LinearLayout(context);
        symbols.setGravity(Gravity.CENTER_VERTICAL);
        for (String symbol : SYMBOLS) {
            TextView key = actionText(symbol, true);
            key.setTextSize(18);
            key.setContentDescription(
                    (russian ? "Вставить " : "Insert ") + symbol);
            symbols.addView(key, new LinearLayout.LayoutParams(
                    0, dp(41), 1f));
            key.setOnClickListener(v -> {
                // Explicit edits go through the normal Undo/save/analysis path.
                editor.replaceInline(editor.getSelectionStart(),
                        editor.getSelectionEnd(), symbol);
                editor.requestFocus();
                requestSuggestions();
            });
        }
        symbolScroll.addView(symbols,
                new HorizontalScrollView.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(symbolScroll, new LayoutParams(
                LayoutParams.MATCH_PARENT, dp(43)));
        refreshSettings();
    }

    public void refreshSettings() {
        EditorPreferences.Snapshot options = EditorPreferences.get(getContext());
        quickSymbols = options.quickSymbols;
        labelCompletion = options.labelCompletion;
        updateVisibility();
        requestSuggestions();
    }

    public void setKeyboardVisible(boolean visible) {
        if (keyboardVisible == visible) return;
        keyboardVisible = visible;
        updateVisibility();
        requestSuggestions();
    }

    public void onEditorFocusChanged() {
        updateVisibility();
        requestSuggestions();
    }

    private boolean active() {
        return keyboardVisible && editor.hasFocus();
    }

    private void updateVisibility() {
        boolean active = active();
        symbolScroll.setVisibility(active && quickSymbols ? VISIBLE : GONE);
        if (!active || !labelCompletion) suggestionScroll.setVisibility(GONE);
        setVisibility(active && (quickSymbols
                || labelCompletion && suggestionScroll.getVisibility() == VISIBLE)
                ? VISIBLE : GONE);
    }

    public void requestSuggestions() {
        generation++;
        handler.removeCallbacks(refresh);
        if (!labelCompletion || !active()) {
            suggestionItems.removeAllViews();
            suggestionScroll.setVisibility(GONE);
            updateVisibility();
            return;
        }
        handler.postDelayed(refresh, COMPLETION_IDLE_MS);
    }

    private void lookupSuggestions() {
        if (!active() || !labelCompletion || editor.isPresentationChange()) return;
        int caret = editor.getSelectionStart();
        if (caret < 0 || caret != editor.getSelectionEnd()) {
            hideSuggestions();
            return;
        }
        Editable text = editor.getText();
        int from = TextUtils.lastIndexOf(text, '\n', Math.max(0, caret - 1)) + 1;
        int to = TextUtils.indexOf(text, '\n', caret);
        if (to < 0) to = text.length();
        if (from > caret || caret > to) {
            hideSuggestions();
            return;
        }
        String line = text.subSequence(from, to).toString();
        int relativeCaret = caret - from;
        int prefixStart = relativeCaret;
        while (prefixStart > 0 && isLabelChar(line.charAt(prefixStart - 1)))
            prefixStart--;
        // Do not snapshot/parse a potentially large source when there is no
        // viable prefix in the user's current line.
        if (relativeCaret - prefixStart < 2
                || line.substring(0, prefixStart).trim().isEmpty()) {
            hideSuggestions();
            return;
        }

        final String completeSource = editor.getSourceText();
        final int expectedCaret = caret;
        final int lineStart = from;
        final long revision = generation;
        COMPLETION_WORKER.execute(() -> {
            List<LabelCompletion.Match> matches = LabelCompletion.suggest(
                    completeSource, line, relativeCaret, SUGGESTION_LIMIT);
            handler.post(() -> {
                if (revision != generation || !active() || !labelCompletion
                        || editor.getSelectionStart() != expectedCaret
                        || editor.getSelectionEnd() != expectedCaret) return;
                renderMatches(matches, lineStart, line);
            });
        });
    }

    private void renderMatches(List<LabelCompletion.Match> matches,
                               int lineStart, String originalLine) {
        suggestionItems.removeAllViews();
        if (matches.isEmpty()) {
            hideSuggestions();
            return;
        }
        TextView tag = new TextView(getContext());
        tag.setText(russian ? "Метки" : "Labels");
        tag.setTextColor(0xFF678575);
        tag.setTextSize(11);
        tag.setPadding(dp(5), 0, dp(8), 0);
        tag.setGravity(Gravity.CENTER_VERTICAL);
        tag.setFocusable(false);
        suggestionItems.addView(tag);

        for (LabelCompletion.Match match : matches) {
            TextView chip = actionText(match.label, false);
            chip.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            chip.setTextSize(13);
            chip.setSingleLine(true);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setMaxWidth(dp(210));
            chip.setContentDescription((russian ? "Вставить метку " : "Complete label ")
                    + match.label);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, dp(34));
            params.setMargins(dp(2), 0, dp(4), 0);
            suggestionItems.addView(chip, params);
            chip.setOnClickListener(v -> {
                Editable now = editor.getText();
                int start = lineStart + match.start;
                int end = lineStart + match.end;
                if (start < 0 || end > now.length() || end < start
                        || !originalLine.substring(match.start, match.end)
                        .contentEquals(now.subSequence(start, end))) {
                    requestSuggestions();
                    return;
                }
                editor.replaceInline(start, end, match.label);
                editor.requestFocus();
                requestSuggestions();
            });
        }
        suggestionScroll.setVisibility(VISIBLE);
        suggestionScroll.scrollTo(0, 0);
        updateVisibility();
    }

    private void hideSuggestions() {
        suggestionItems.removeAllViews();
        suggestionScroll.setVisibility(GONE);
        updateVisibility();
    }

    private TextView actionText(String label, boolean isSymbol) {
        TextView text = new TextView(getContext());
        text.setText(label);
        text.setTextColor(isSymbol ? 0xFF25573B : 0xFF255A46);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(5), 0, dp(5), 0);
        text.setBackground(UiStyle.ripple(getContext(),
                isSymbol ? 0xFFE6F1E9 : 0xFFF0EBDA, 9,
                isSymbol ? 0xFFB8D9C4 : 0xFFD4C6A7));
        text.setClickable(true);
        text.setFocusable(false);
        text.setFocusableInTouchMode(false);
        return text;
    }

    private static boolean isLabelChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                || c >= '0' && c <= '9' || c == '_' || c == '@'
                || c == '.' || c == '$';
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
