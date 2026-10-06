package com.alexzab.z80pocketide;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzab.z80pocketide.emulator.TapRunHelper;
import com.alexzab.z80pocketide.examples.ExamplePrograms;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.ui.ExamplePreviewView;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

/** Detailed example page with direct Run and optional editor handoff. */
public final class ExampleDetailActivity extends Activity {
    public static final String EXTRA_ID = "example_id";

    private AppLanguage language;
    private ExamplePrograms.Example example;
    private TextView code;
    private TextView runStatus;
    private CheckBox comments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);
        example = ExamplePrograms.findById(getIntent().getStringExtra(EXTRA_ID));

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

        TextView headerTitle = text(example.title(language), 20, 0xFFFFD43B);
        headerTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(headerTitle, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(10), 0, dp(10));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        ExamplePreviewView preview = new ExamplePreviewView(this);
        preview.setPreviewType(example.previewType);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(210));
        pp.setMargins(0, 0, 0, dp(12));
        content.addView(preview, pp);

        TextView category = text(example.category(language), 13, 0xFFA9BAC4);
        category.setText(example.category(language).toUpperCase());
        content.addView(category);

        TextView title = text(example.title(language), 25, 0xFFFFD43B);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        content.addView(title);

        TextView description = text(example.description(language), 16, 0xFFE9ECEE);
        description.setPadding(0, dp(4), 0, dp(14));
        content.addView(description);

        TextView howTitle = text(t("How it works", "Как это работает"), 19, 0xFFFFD43B);
        howTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        content.addView(howTitle);

        TextView details = text(example.details(language), 15, 0xFFD5DEE3);
        details.setPadding(0, dp(4), 0, dp(14));
        content.addView(details);

        if (example.hasIo()) {
            TextView ioTitle = text(t("Registers", "Регистры"), 19, 0xFFFFD43B);
            ioTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            content.addView(ioTitle);

            TextView io = text(example.io(language), 14, 0xFFE8EEF2);
            io.setTypeface(android.graphics.Typeface.MONOSPACE);
            io.setPadding(dp(12), dp(10), dp(12), dp(10));
            io.setBackground(UiStyle.rounded(this, Color.rgb(7, 12, 17),
                    14, Color.rgb(45, 62, 72), 1));
            LinearLayout.LayoutParams iop = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            iop.setMargins(0, dp(5), 0, dp(14));
            content.addView(io, iop);
        }

        LinearLayout codeHeader = new LinearLayout(this);
        codeHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView codeTitle = text(t("Source", "Код"), 19, 0xFFFFD43B);
        codeTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        codeHeader.addView(codeTitle, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        comments = new CheckBox(this);
        comments.setText(t("With comments", "С комментариями"));
        comments.setTextColor(0xFFE9ECEE);
        comments.setChecked(true);
        codeHeader.addView(comments);
        content.addView(codeHeader);

        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setBackground(UiStyle.rounded(this, Color.rgb(7, 12, 17),
                14, Color.rgb(45, 62, 72), 1));
        code = text("", 13, 0xFFE8EEF2);
        code.setTypeface(android.graphics.Typeface.MONOSPACE);
        code.setPadding(dp(10), dp(9), dp(10), dp(9));
        code.setTextIsSelectable(true);
        hScroll.addView(code);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.setMargins(0, dp(5), 0, dp(8));
        content.addView(hScroll, hp);

        TextView hint = text(t(
                "Run builds a temporary autorun TAP without creating an editor tab. Open in editor only when you want to modify the source.",
                "«Запустить» собирает временный autorun TAP без создания вкладки. Открывайте в редакторе только если хотите менять код."),
                12, 0xFF95A8B2);
        content.addView(hint);

        runStatus = text("", 12, 0xFF9ED9B5);
        runStatus.setPadding(dp(2), dp(5), dp(2), 0);
        content.addView(runStatus);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(7), 0, 0);

        Button run = new Button(this);
        run.setText(t("Run", "Запустить"));
        UiStyle.styleGreenButton(run);

        Button open = new Button(this);
        open.setText(t("Open in editor", "Открыть в редакторе"));
        UiStyle.styleButton(open, 0xFFFFD43B, Color.rgb(25, 25, 25), 18);

        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        left.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        right.setMargins(dp(4), 0, 0, 0);
        actions.addView(run, left);
        actions.addView(open, right);
        root.addView(actions);

        comments.setOnCheckedChangeListener((buttonView, isChecked) -> refreshCode());
        back.setOnClickListener(v -> finish());
        open.setOnClickListener(v -> returnExample());
        run.setOnClickListener(v -> runExample());

        refreshCode();
        refreshBuildSummary();
        setContentView(root);
    }

    private void refreshCode() {
        code.setText(example.source(language, comments.isChecked()));
    }

    private void refreshBuildSummary() {
        try {
            runStatus.setText(t("Ready · ", "Готово · ")
                    + TapRunHelper.buildSummary(example.source(language, false), language));
            runStatus.setTextColor(0xFF9ED9B5);
        } catch (RuntimeException ex) {
            String detail = Texts.localizeAssemblerError(language, ex.getMessage());
            runStatus.setText(t("Build error · ", "Ошибка сборки · ") + detail);
            runStatus.setTextColor(0xFFFF8585);
        }
    }

    private void runExample() {
        try {
            String message = TapRunHelper.buildAndLaunch(
                    this,
                    example.source(language, comments.isChecked()),
                    example.title(language),
                    language);
            runStatus.setText(message);
            runStatus.setTextColor(0xFF9ED9B5);
        } catch (ActivityNotFoundException ex) {
            String message = t(
                    "No app can open TAP files",
                    "Нет приложения, которое может открыть TAP");
            runStatus.setText(message);
            runStatus.setTextColor(0xFFFF8585);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (RuntimeException ex) {
            String detail = Texts.localizeAssemblerError(language, ex.getMessage());
            String message = t("Build error · ", "Ошибка сборки · ") + detail;
            runStatus.setText(message);
            runStatus.setTextColor(0xFFFF8585);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            String message = t("Run error · ", "Ошибка запуска · ")
                    + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            runStatus.setText(message);
            runStatus.setTextColor(0xFFFF8585);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void returnExample() {
        Intent result = new Intent();
        result.putExtra(ExampleCatalogActivity.EXTRA_SOURCE,
                example.source(language, comments.isChecked()));
        result.putExtra(ExampleCatalogActivity.EXTRA_TITLE, example.title(language));
        setResult(RESULT_OK, result);
        finish();
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
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
