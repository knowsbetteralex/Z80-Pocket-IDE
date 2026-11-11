package com.alexzab.z80pocketide;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alexzab.z80pocketide.help.HelpDocument;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Offline, bilingual user manual with a chapter table of contents and
 * previous/next navigation. Content is the same Markdown shipped in the repo.
 * No WebView, external network, or third-party UI libraries.
 */
public final class HelpActivity extends Activity {
    private static final String STATE_CHAPTER = "help_chapter";
    private AppLanguage language;
    private HelpDocument manual;
    private ScrollView scroller;
    private LinearLayout page;
    private Button chaptersButton;
    private int currentChapter = -1;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);
        try {
            String path = language == AppLanguage.RU ? "help/guide_ru.md" : "help/guide_en.md";
            manual = HelpDocument.parse(readAsset(path));
        } catch (Exception ex) {
            showError(ex);
            return;
        }
        if (savedInstanceState != null)
            currentChapter = savedInstanceState.getInt(STATE_CHAPTER, -1);
        if (currentChapter < 0 || currentChapter >= manual.chapters.size())
            currentChapter = -1;

        getWindow().setStatusBarColor(UiStyle.APP_BACKGROUND);
        getWindow().setNavigationBarColor(UiStyle.APP_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiStyle.APP_BACKGROUND);

        int side = dp(12);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int top = insets.getInsets(WindowInsets.Type.systemBars()).top;
                int bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                view.setPadding(side, dp(8) + top, side, dp(8) + bottom);
                return insets;
            });
        } else {
            root.setPadding(side, dp(8), side, dp(8));
        }

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = button("←", false);
        back.setContentDescription(t("Back", "Назад"));
        back.setTextSize(21);
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(44)));
        back.setOnClickListener(view -> onBackPressed());

        TextView heading = label(t("User guide", "Руководство"), 21,
                UiStyle.TEXT_PRIMARY);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setPadding(dp(12), 0, 0, 0);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));

        chaptersButton = button(t("Chapters", "Главы"), false);
        chaptersButton.setOnClickListener(view -> showChapter(-1));
        header.addView(chaptersButton, new LinearLayout.LayoutParams(dp(100), dp(44)));
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        LinearLayout.LayoutParams stripeParams = new LinearLayout.LayoutParams(-1, dp(5));
        stripeParams.setMargins(0, dp(6), 0, dp(10));
        root.addView(stripe, stripeParams);

        scroller = new ScrollView(this);
        scroller.setFillViewport(false);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(2), 0, dp(2), dp(18));
        scroller.addView(page);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        render();
    }

    private void render() {
        page.removeAllViews();
        chaptersButton.setVisibility(currentChapter == -1 ? View.GONE : View.VISIBLE);
        if (currentChapter == -1) renderContents();
        else renderChapter(currentChapter);
        scroller.post(() -> scroller.scrollTo(0, 0));
    }

    private void renderContents() {
        TextView title = label(manual.title, 23, UiStyle.ACCENT_GREEN);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(title);
        TextView intro = label(manual.introduction, 14, UiStyle.TEXT_SECONDARY);
        intro.setPadding(0, dp(9), 0, dp(18));
        page.addView(intro);

        TextView caption = label(t("Contents", "Содержание"), 16, UiStyle.TEXT_PRIMARY);
        caption.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(caption);
        for (int i = 0; i < manual.chapters.size(); i++) {
            final int index = i;
            HelpDocument.Chapter chapter = manual.chapters.get(i);
            Button action = button(chapter.title + "  ›", false);
            action.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            action.setPadding(dp(16), dp(7), dp(9), dp(7));
            action.setTextSize(15);
            action.setContentDescription(chapter.title);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(53));
            p.setMargins(0, dp(8), 0, 0);
            page.addView(action, p);
            action.setOnClickListener(view -> showChapter(index));
        }
    }

    private void renderChapter(int index) {
        HelpDocument.Chapter chapter = manual.chapters.get(index);
        TextView counter = label(t("Chapter ", "Глава ") + (index + 1) + " / " +
                manual.chapters.size(), 12, UiStyle.TEXT_SECONDARY);
        page.addView(counter);

        TextView title = label(chapter.title, 21, UiStyle.ACCENT_GREEN);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(7), 0, dp(12));
        page.addView(title);

        TextView body = label("", 15, UiStyle.TEXT_PRIMARY);
        body.setLineSpacing(dp(4), 1.06f);
        body.setText(renderMarkdown(chapter.markdown));
        body.setTextIsSelectable(true);
        body.setPadding(dp(4), dp(8), dp(4), dp(12));
        body.setBackground(UiStyle.rounded(this, UiStyle.PANEL_BACKGROUND,
                16, UiStyle.PANEL_STROKE, 1));
        page.addView(body, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, dp(16), 0, dp(5));
        Button previous = button(t("‹ Previous", "‹ Назад"), false);
        previous.setEnabled(index > 0);
        previous.setOnClickListener(view -> showChapter(index - 1));
        Button contents = button(t("Contents", "Главы"), false);
        contents.setOnClickListener(view -> showChapter(-1));
        Button next = button(t("Next ›", "Далее ›"), true);
        next.setEnabled(index < manual.chapters.size() - 1);
        next.setOnClickListener(view -> showChapter(index + 1));
        for (Button b : new Button[]{previous, contents, next}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(46), 1);
            p.setMargins(dp(2), 0, dp(2), 0);
            nav.addView(b, p);
        }
        page.addView(nav);
    }

    private SpannableStringBuilder renderMarkdown(String source) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        final String fence = new String(new char[]{96, 96, 96});
        boolean inCode = false;
        for (String line : source.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(fence)) {
                if (!inCode && out.length() > 0) out.append('\n');
                inCode = !inCode;
                continue;
            }
            int start = out.length();
            if (inCode) {
                out.append("  ").append(line).append('\n');
                out.setSpan(new TypefaceSpan("monospace"), start, out.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new BackgroundColorSpan(0xFFEAF3EC),
                        start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }
            boolean subheading = line.startsWith("### ");
            String text = subheading ? line.substring(4) : line;
            if (text.startsWith("- ")) text = "  • " + text.substring(2);
            appendInline(out, text);
            if (subheading) {
                out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(1.1f), start, out.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(UiStyle.ACCENT_GREEN),
                        start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            out.append('\n');
        }
        return out;
    }

    private static void appendInline(SpannableStringBuilder out, String text) {
        final char tick = (char) 96;
        int i = 0;
        while (i < text.length()) {
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end > i + 2) {
                    int start = out.length();
                    out.append(text.substring(i + 2, end));
                    out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 2;
                    continue;
                }
            } else if (text.charAt(i) == tick) {
                int end = text.indexOf(tick, i + 1);
                if (end > i + 1) {
                    int start = out.length();
                    out.append(text.substring(i + 1, end));
                    out.setSpan(new TypefaceSpan("monospace"), start, out.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new BackgroundColorSpan(0xFFEAF3EC),
                            start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i = end + 1;
                    continue;
                }
            }
            out.append(text.charAt(i++));
        }
    }

    private void showChapter(int index) {
        if (index < -1 || (manual != null && index >= manual.chapters.size())) return;
        currentChapter = index;
        render();
    }

    private String readAsset(String path) throws IOException {
        try (InputStream input = getAssets().open(path);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) bytes.write(buffer, 0, read);
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void showError(Exception ex) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(t("Cannot open guide", "Не удалось открыть руководство"))
                .setMessage(ex.getMessage())
                .setPositiveButton(t("Close", "Закрыть"), (dialog, which) -> finish())
                .show();
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        if (primary) UiStyle.styleGreenButton(b);
        else UiStyle.styleSoftGreenButton(b);
        return b;
    }

    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private String t(String en, String ru) {
        return language == AppLanguage.RU ? ru : en;
    }

    private int dp(int value) {
        return UiStyle.dp(this, value);
    }

    @Override public void onBackPressed() {
        if (currentChapter != -1) showChapter(-1);
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt(STATE_CHAPTER, currentChapter);
        super.onSaveInstanceState(out);
    }
}
