package com.alexzab.z80pocketide;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.tools.NumberBaseConverter;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

/** Live three-way number-system converter for Z80 work. */
public final class NumberConverterActivity extends Activity {
    private AppLanguage language;
    private EditText dec;
    private EditText hex;
    private EditText bin;
    private TextView status;
    private boolean updating;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        language = LanguageSettings.get(this);

        getWindow().setStatusBarColor(Color.rgb(245, 248, 246));
        getWindow().setNavigationBarColor(Color.rgb(245, 248, 246));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 248, 246));

        final int side = dp(16);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                int top = insets.getInsets(WindowInsets.Type.systemBars()).top;
                int bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                v.setPadding(side, top + dp(10), side, bottom + dp(12));
                return insets;
            });
        } else {
            root.setPadding(side, dp(10), side, dp(12));
        }

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText("←");
        back.setTextSize(19);
        back.setMinWidth(0);
        UiStyle.styleSoftGreenButton(back);
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(44)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(10), 0, 0, 0);

        TextView title = text(t("Number converter", "Системы счисления"), 23,
                Color.rgb(30, 52, 42));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView subtitle = text(t("DEC · HEX · BIN update instantly",
                "DEC · HEX · BIN пересчитываются сразу"), 12, Color.rgb(88, 105, 96));
        titleBox.addView(title);
        titleBox.addView(subtitle);
        header.addView(titleBox, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        sp.setMargins(0, dp(8), 0, dp(16));
        root.addView(stripe, sp);

        dec = numberField("DEC", "12345");
        hex = numberField("HEX", "$3039");
        bin = numberField("BIN", "%11000000111001");

        root.addView(fieldCard("DEC", t("Decimal", "Десятичная"), dec));
        root.addView(fieldCard("HEX", t("Hexadecimal", "Шестнадцатеричная"), hex));
        root.addView(fieldCard("BIN", t("Binary", "Двоичная"), bin));

        status = text(t("Type in any field", "Введите число в любое поле"), 13,
                Color.rgb(88, 105, 96));
        status.setPadding(dp(4), dp(5), dp(4), dp(8));
        root.addView(status);

        Button clear = new Button(this);
        clear.setText(t("Clear", "Очистить"));
        clear.setMinHeight(dp(46));
        UiStyle.styleGreenButton(clear);
        root.addView(clear, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        watch(dec, NumberBaseConverter.Base.DEC);
        watch(hex, NumberBaseConverter.Base.HEX);
        watch(bin, NumberBaseConverter.Base.BIN);

        clear.setOnClickListener(v -> clearAll());
        back.setOnClickListener(v -> finish());
        setContentView(root);
    }

    private LinearLayout fieldCard(String shortName, String longName, EditText field) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(12));
        card.setBackground(UiStyle.rounded(this, Color.WHITE, 18,
                Color.rgb(216, 226, 220), 1));

        TextView label = text(shortName + " · " + longName, 13, Color.rgb(45, 102, 72));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(label);
        card.addView(field, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(p);
        return card;
    }

    private EditText numberField(String tag, String hint) {
        EditText field = new EditText(this);
        field.setTag(tag);
        field.setHint(hint);
        field.setTextSize(19);
        field.setSingleLine(true);
        field.setTypeface(Typeface.MONOSPACE);
        field.setTextColor(Color.rgb(28, 35, 31));
        field.setHintTextColor(Color.rgb(160, 170, 164));
        field.setBackgroundColor(Color.TRANSPARENT);
        field.setPadding(0, dp(5), 0, 0);
        field.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        return field;
    }

    private void watch(EditText field, NumberBaseConverter.Base base) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (updating) return;
                String raw = s.toString().trim();
                if (raw.isEmpty()) {
                    clearOthers(field);
                    status.setText(t("Type in any field", "Введите число в любое поле"));
                    status.setTextColor(Color.rgb(88, 105, 96));
                    return;
                }

                try {
                    NumberBaseConverter.Values values = NumberBaseConverter.convert(raw, base);
                    updating = true;
                    if (field != dec) dec.setText(values.decimal);
                    if (field != hex) hex.setText(values.hexadecimal);
                    if (field != bin) bin.setText(values.binary);
                    updating = false;
                    status.setText(t("Converted · up to 64-bit values",
                            "Пересчитано · значения до 64 бит"));
                    status.setTextColor(Color.rgb(45, 102, 72));
                } catch (IllegalArgumentException ex) {
                    updating = false;
                    status.setText(t("Invalid value for this base",
                            "Некорректное число для этой системы"));
                    status.setTextColor(Color.rgb(180, 45, 45));
                }
            }
        });
    }

    private void clearOthers(EditText source) {
        updating = true;
        if (source != dec) dec.setText("");
        if (source != hex) hex.setText("");
        if (source != bin) bin.setText("");
        updating = false;
    }

    private void clearAll() {
        updating = true;
        dec.setText("");
        hex.setText("");
        bin.setText("");
        updating = false;
        dec.requestFocus();
        status.setText(t("Cleared", "Очищено"));
        status.setTextColor(Color.rgb(88, 105, 96));
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private String t(String en, String ru) {
        return Texts.pick(language, en, ru);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
