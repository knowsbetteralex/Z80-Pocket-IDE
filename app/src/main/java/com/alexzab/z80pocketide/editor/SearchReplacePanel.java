package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.ui.UiStyle;

/** Two-row Find / Replace toolbar that leaves more room for code and keyboard. */
public final class SearchReplacePanel extends LinearLayout {
    public interface Actions {
        void onQueryChanged();
        void onNext();
        void onPrevious();
        void onReplace();
        void onReplaceAll();
        void onClose();
    }

    private final EditText query;
    private final EditText replacement;
    private final Button caseButton;
    private final TextView counter;
    private final boolean ru;
    private boolean caseSensitive;

    public SearchReplacePanel(Context context, boolean ru, Actions actions) {
        super(context);
        this.ru = ru;
        setOrientation(VERTICAL);
        setPadding(dp(5), dp(3), dp(5), dp(3));
        setBackground(UiStyle.rounded(context, 0xFFF0F7F2, 12, 0xFFC6DACC, 1));

        // Find, current/total matches, navigation, close.
        LinearLayout searchRow = new LinearLayout(context);
        searchRow.setOrientation(HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        query = edit(t("Find in code", "Найти в коде"));
        searchRow.addView(query, weighted());
        counter = new TextView(context);
        counter.setTextSize(11);
        counter.setTypeface(Typeface.MONOSPACE);
        counter.setTextColor(0xFF456E53);
        counter.setGravity(Gravity.CENTER);
        counter.setSingleLine(true);
        searchRow.addView(counter, fixed(43));

        Button previous = button("↑");
        previous.setContentDescription(t("Previous match", "Предыдущее совпадение"));
        searchRow.addView(previous, fixed(36));
        Button next = button("↓");
        next.setContentDescription(t("Next match", "Следующее совпадение"));
        searchRow.addView(next, fixed(36));
        Button close = button("×");
        close.setContentDescription(t("Close search", "Закрыть поиск"));
        searchRow.addView(close, fixed(36));
        addView(searchRow);

        // Replace, replace-all and toggle case sensitivity. No third row.
        LinearLayout replaceRow = new LinearLayout(context);
        replaceRow.setOrientation(HORIZONTAL);
        replaceRow.setGravity(Gravity.CENTER_VERTICAL);
        replacement = edit(t("Replace with", "Заменить на"));
        replaceRow.addView(replacement, weighted());
        Button replaceOne = button(t("Replace", "Заменить"));
        replaceRow.addView(replaceOne, fixed(78));
        Button replaceAll = button(t("All", "Все"));
        replaceRow.addView(replaceAll, fixed(44));
        caseButton = button("Aa");
        caseButton.setTextSize(14);
        caseButton.setTypeface(Typeface.DEFAULT_BOLD);
        replaceRow.addView(caseButton, fixed(36));
        addView(replaceRow);
        refreshCaseButton();

        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                actions.onQueryChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        caseButton.setOnClickListener(v -> {
            caseSensitive = !caseSensitive;
            refreshCaseButton();
            actions.onQueryChanged();
        });
        previous.setOnClickListener(v -> actions.onPrevious());
        next.setOnClickListener(v -> actions.onNext());
        replaceOne.setOnClickListener(v -> actions.onReplace());
        replaceAll.setOnClickListener(v -> actions.onReplaceAll());
        close.setOnClickListener(v -> actions.onClose());
        updateCount(0, -1);
    }

    private void refreshCaseButton() {
        if (caseSensitive) UiStyle.styleGreenButton(caseButton);
        else UiStyle.styleSoftGreenButton(caseButton);
        caseButton.setContentDescription(caseSensitive
                ? t("Match case on", "Учёт регистра включён")
                : t("Match case off", "Учёт регистра выключен"));
        caseButton.setSelected(caseSensitive);
    }

    private EditText edit(String hint) {
        EditText view = new EditText(getContext());
        view.setSingleLine(true);
        view.setHint(hint);
        view.setSelectAllOnFocus(false);
        view.setTextSize(14);
        view.setTextColor(0xFF243D30);
        view.setTypeface(Typeface.MONOSPACE);
        view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        view.setPadding(dp(7), 0, dp(7), 0);
        view.setBackground(UiStyle.rounded(getContext(), Color.WHITE, 9, 0xFFBDDAC6, 1));
        return view;
    }

    private Button button(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextSize(12);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setAllCaps(false);
        UiStyle.styleSoftGreenButton(button);
        return button;
    }

    private LayoutParams fixed(int width) {
        LayoutParams params = new LayoutParams(dp(width), dp(38));
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private LayoutParams weighted() {
        LayoutParams params = new LayoutParams(0, dp(38), 1f);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    public String getQuery() { return query.getText().toString(); }
    public String getReplacement() { return replacement.getText().toString(); }
    public boolean isCaseSensitive() { return caseSensitive; }

    public void setQuery(String value) {
        query.setText(value);
        query.setSelection(query.length());
    }

    public void focusQuery() {
        query.requestFocus();
        query.selectAll();
    }

    public void updateCount(int count, int index) {
        if (count == 0) {
            counter.setText(ru ? "0" : "0");
        } else {
            counter.setText((index < 0 ? "–" : String.valueOf(index + 1)) + "/" + count);
        }
    }

    private String t(String en, String russian) { return ru ? russian : en; }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
