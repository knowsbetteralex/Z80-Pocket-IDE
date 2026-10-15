package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.ui.UiStyle;

/** Collapsible find/replace toolbar directly above the source editor. */
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
    private final CheckBox caseSensitive;
    private final TextView counter;
    private final boolean ru;

    public SearchReplacePanel(Context context, boolean ru, Actions actions) {
        super(context);
        this.ru = ru;
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(6), dp(8), dp(7));
        setBackground(UiStyle.rounded(context, 0xFFF0F7F2, 14, 0xFFC6DACC, 1));

        LinearLayout searchRow = new LinearLayout(context);
        searchRow.setOrientation(HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        query = edit(t("Find in code", "Найти в коде"));
        searchRow.addView(query, weighted(1));
        Button previous = button("↑");
        previous.setContentDescription(t("Previous match", "Предыдущее совпадение"));
        searchRow.addView(previous, fixed(41));
        Button next = button("↓");
        next.setContentDescription(t("Next match", "Следующее совпадение"));
        searchRow.addView(next, fixed(41));
        Button close = button("×");
        close.setContentDescription(t("Close search", "Закрыть поиск"));
        searchRow.addView(close, fixed(41));
        addView(searchRow);

        LinearLayout replaceRow = new LinearLayout(context);
        replaceRow.setOrientation(HORIZONTAL);
        replaceRow.setGravity(Gravity.CENTER_VERTICAL);
        replacement = edit(t("Replace with", "Заменить на"));
        replaceRow.addView(replacement, weighted(1));
        Button replaceOne = button(t("Replace", "Заменить"));
        replaceRow.addView(replaceOne, fixed(90));
        Button replaceAll = button(t("All", "Все"));
        replaceRow.addView(replaceAll, fixed(53));
        addView(replaceRow);

        LinearLayout optionsRow = new LinearLayout(context);
        optionsRow.setGravity(Gravity.CENTER_VERTICAL);
        caseSensitive = new CheckBox(context);
        caseSensitive.setText(t("Match case", "Учитывать регистр"));
        caseSensitive.setTextSize(12);
        caseSensitive.setTextColor(0xFF356548);
        optionsRow.addView(caseSensitive, weighted(1));

        counter = new TextView(context);
        counter.setTextSize(12);
        counter.setTypeface(Typeface.MONOSPACE);
        counter.setTextColor(0xFF456E53);
        counter.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        optionsRow.addView(counter);
        addView(optionsRow);

        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                actions.onQueryChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        caseSensitive.setOnCheckedChangeListener((view, checked) -> actions.onQueryChanged());
        previous.setOnClickListener(v -> actions.onPrevious());
        next.setOnClickListener(v -> actions.onNext());
        replaceOne.setOnClickListener(v -> actions.onReplace());
        replaceAll.setOnClickListener(v -> actions.onReplaceAll());
        close.setOnClickListener(v -> actions.onClose());
        updateCount(0, -1);
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
        view.setBackground(UiStyle.rounded(getContext(), Color.WHITE, 10, 0xFFBDDAC6, 1));
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
        UiStyle.styleSoftGreenButton(button);
        return button;
    }

    private LayoutParams fixed(int width) {
        LayoutParams params = new LayoutParams(dp(width), dp(41));
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private LayoutParams weighted(int weight) {
        LayoutParams params = new LayoutParams(0, dp(41), weight);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    public String getQuery() { return query.getText().toString(); }
    public String getReplacement() { return replacement.getText().toString(); }
    public boolean isCaseSensitive() { return caseSensitive.isChecked(); }

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
            counter.setText(t("0 matches", "0 совпадений"));
        } else {
            counter.setText((index < 0 ? "–" : String.valueOf(index + 1)) + "/" + count);
        }
    }

    private String t(String en, String russian) { return ru ? russian : en; }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
