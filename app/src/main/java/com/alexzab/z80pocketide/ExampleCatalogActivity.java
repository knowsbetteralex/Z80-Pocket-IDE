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

/** Browseable example gallery inspired by compact electronics reference cards. */
public final class ExampleCatalogActivity extends Activity {
    public static final String EXTRA_SOURCE = "example_source";
    public static final String EXTRA_TITLE = "example_title";
    private static final int REQUEST_DETAIL = 2101;

    private AppLanguage language;

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
        TextView title = label(t("Examples", "Примеры"), 24, 0xFFFFD43B);
        TextView subtitle = label(t("Visual demos and reusable Z80 routines",
                "Наглядные примеры и готовые подпрограммы Z80"), 12, 0xFFA8BAC5);
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(10), 0, dp(18));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        String previousCategory = null;
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            String category = example.category(language);
            if (!category.equals(previousCategory)) {
                TextView heading = label(category, 21, 0xFFFFD43B);
                heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                heading.setPadding(0, dp(previousCategory == null ? 6 : 18), 0, dp(7));
                list.addView(heading);
                previousCategory = category;
            }
            list.addView(card(example));
        }

        back.setOnClickListener(v -> finish());
        setContentView(root);
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
                LinearLayout.LayoutParams.MATCH_PARENT, dp(112));
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
        desc.setMaxLines(3);
        textBlock.addView(title);
        textBlock.addView(desc);
        card.addView(textBlock, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        TextView arrow = label("›", 28, 0xFFB9C8D0);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(26),
                LinearLayout.LayoutParams.MATCH_PARENT));

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
