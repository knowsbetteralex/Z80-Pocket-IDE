package com.alexzab.z80pocketide;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alexzab.z80pocketide.examples.ExamplePrograms;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.ui.ExamplePreviewView;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

/** Browseable example gallery with separate demos and reusable-routines tabs. */
public final class ExampleCatalogActivity extends Activity {
    public static final String EXTRA_SOURCE = "example_source";
    public static final String EXTRA_TITLE = "example_title";
    private static final int REQUEST_DETAIL = 2101;

    private AppLanguage language;
    private LinearLayout list;
    private Button examplesTab;
    private Button routinesTab;
    private boolean showRoutines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);

        final int bg = Color.rgb(16, 25, 32);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        final int side = dp(12);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                int top = insets.getInsets(WindowInsets.Type.systemBars()).top;
                int bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                v.setPadding(side, dp(6) + top, side, dp(6) + bottom);
                return insets;
            });
        } else {
            root.setPadding(side, dp(6), side, dp(6));
        }

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText("←");
        back.setTextSize(19);
        back.setMinWidth(0);
        UiStyle.styleButton(back, Color.rgb(27, 45, 57), 0xFFFFD43B, 16);
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = label(t("Library", "Библиотека"), 24, 0xFFFFD43B);
        TextView subtitle = label(t("Visual demos and reusable Z80 building blocks",
                "Наглядные примеры и готовые блоки Z80"), 12, 0xFFA8BAC5);
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, dp(10), 0, dp(8));

        examplesTab = new Button(this);
        examplesTab.setText(t("Examples", "Примеры"));
        examplesTab.setMinHeight(dp(42));
        examplesTab.setMinWidth(0);

        routinesTab = new Button(this);
        routinesTab.setText(t("Routines", "Подпрограммы"));
        routinesTab.setMinHeight(dp(42));
        routinesTab.setMinWidth(0);

        LinearLayout.LayoutParams tp1 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp1.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams tp2 = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp2.setMargins(dp(4), 0, 0, 0);
        tabs.addView(examplesTab, tp1);
        tabs.addView(routinesTab, tp2);
        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, dp(18));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        examplesTab.setOnClickListener(v -> {
            showRoutines = false;
            renderTab();
        });
        routinesTab.setOnClickListener(v -> {
            showRoutines = true;
            renderTab();
        });
        back.setOnClickListener(v -> finish());

        renderTab();
        setContentView(root);
    }

    private void renderTab() {
        list.removeAllViews();
        updateTabButtons();

        String introText = showRoutines
                ? t("Ready-to-copy routines with IN / OUT / DESTROYS contracts.",
                    "Готовые подпрограммы с контрактами IN / OUT / ПОРТИТ.")
                : t("Small runnable programs that demonstrate Spectrum hardware and Z80 techniques.",
                    "Небольшие запускаемые программы, показывающие железо Spectrum и приёмы Z80.");
        TextView intro = label(introText, 13, 0xFFA8BAC5);
        intro.setPadding(dp(4), 0, dp(4), dp(10));
        list.addView(intro);

        String previousCategory = null;
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            boolean routine = "Useful routines".equals(example.categoryEn);
            if (routine != showRoutines) continue;

            String category = example.category(language);
            if (!showRoutines && !category.equals(previousCategory)) {
                TextView heading = label(category, 21, 0xFFFFD43B);
                heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                heading.setPadding(0, dp(previousCategory == null ? 2 : 18), 0, dp(7));
                list.addView(heading);
                previousCategory = category;
            }
            list.addView(card(example));
        }
    }

    private void updateTabButtons() {
        styleTab(examplesTab, !showRoutines);
        styleTab(routinesTab, showRoutines);
    }

    private void styleTab(Button button, boolean active) {
        if (active) {
            UiStyle.styleButton(button, 0xFFFFD43B, Color.rgb(20, 28, 34), 16);
        } else {
            UiStyle.styleButton(button, Color.rgb(27, 45, 57), 0xFFD9E0E4, 16);
        }
    }

    private View card(ExamplePrograms.Example example) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(0, 0, dp(8), 0);
        card.setBackground(UiStyle.rounded(this, Color.rgb(27, 45, 57),
                18, Color.rgb(42, 63, 75), 1));
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(dp(1));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(showRoutines ? 124 : 112));
        cardParams.setMargins(0, 0, 0, dp(9));
        card.setLayoutParams(cardParams);

        ExamplePreviewView preview = new ExamplePreviewView(this);
        preview.setPreviewType(example.previewType);
        card.addView(preview, new LinearLayout.LayoutParams(dp(140),
                LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(dp(12), dp(9), dp(3), dp(7));

        TextView title = label(example.title(language), 18, 0xFFFFD43B);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView desc = label(example.description(language), 13, 0xFFE5E8EA);
        desc.setMaxLines(showRoutines ? 2 : 3);
        textBlock.addView(title);
        textBlock.addView(desc);

        if (showRoutines && example.hasIo()) {
            String io = example.io(language);
            String first = io == null ? "" : io.split("\n", 2)[0];
            TextView contract = label(first, 11, 0xFF9ED9B5);
            contract.setTypeface(android.graphics.Typeface.MONOSPACE);
            contract.setPadding(0, dp(4), 0, 0);
            contract.setMaxLines(1);
            textBlock.addView(contract);
        }

        card.addView(textBlock, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        TextView arrow = label("›", 28, 0xFFB9C8D0);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(
                dp(26), LinearLayout.LayoutParams.MATCH_PARENT));

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExampleDetailActivity.class);
            intent.putExtra(ExampleDetailActivity.EXTRA_ID, example.id);
            startActivityForResult(intent, REQUEST_DETAIL);
        });
        return card;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DETAIL && resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private TextView label(String text, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private String t(String en, String ru) {
        return Texts.pick(language, en, ru);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
