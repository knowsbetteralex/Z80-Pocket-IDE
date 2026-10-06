package com.alexzab.z80pocketide;

import android.app.Activity;
import android.graphics.Color;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.LocalizedReference;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.reference.InstructionReference;
import com.alexzab.z80pocketide.reference.SpectrumReference;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReferenceActivity extends Activity {
    public static final String EXTRA_QUERY = "query";
    private static final String STATE_QUERY = "reference_query";
    private static final String STATE_SECTION = "reference_section";

    private static final int SECTION_Z80 = 0;
    private static final int SECTION_ZX = 1;
    private static final int SECTION_ROM = 2;

    private LinearLayout results;
    private TextView count;
    private EditText search;
    private Button z80Tab;
    private Button zxTab;
    private Button romTab;
    private AppLanguage language;
    private int section = SECTION_Z80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);

        getWindow().setStatusBarColor(Color.rgb(244, 248, 246));
        getWindow().setNavigationBarColor(Color.rgb(244, 248, 246));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244, 248, 246));

        final int side = dp(12);
        final int vertical = dp(8);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                int top = insets.getInsets(WindowInsets.Type.systemBars()).top;
                int bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                v.setPadding(side, vertical + top, side, vertical + bottom);
                return insets;
            });
        } else {
            root.setPadding(side, vertical, side, vertical);
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText("←");
        back.setTextSize(18);
        back.setMinWidth(0);
        UiStyle.styleSoftGreenButton(back);
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(t("Reference", "Справка"), 21, Color.rgb(28, 50, 40), false);
        count = text("", 12, Color.rgb(90, 105, 97), false);
        titles.addView(title);
        titles.addView(count);
        header.addView(titles, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button languageButton = new Button(this);
        languageButton.setText(language == AppLanguage.RU ? "RU" : "EN");
        languageButton.setMinWidth(0);
        languageButton.setMinimumWidth(0);
        languageButton.setPadding(dp(10), 0, dp(10), 0);
        UiStyle.styleSoftGreenButton(languageButton);
        header.addView(languageButton);
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, dp(9), 0, dp(5));

        z80Tab = tabButton("Z80");
        zxTab = tabButton("ZX 48K");
        romTab = tabButton("ROM");
        tabs.addView(z80Tab, tabParams(0, 3));
        tabs.addView(zxTab, tabParams(3, 3));
        tabs.addView(romTab, tabParams(3, 0));
        root.addView(tabs);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setPadding(dp(12), dp(8), dp(12), dp(8));
        search.setBackground(UiStyle.rounded(this, Color.WHITE, 16,
                Color.rgb(205, 222, 212), 1));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, dp(4), 0, dp(5));
        root.addView(search, searchParams);

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0, dp(3), 0, dp(12));
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        back.setOnClickListener(v -> finish());
        languageButton.setOnClickListener(v -> {
            LanguageSettings.set(this,
                    language == AppLanguage.RU ? AppLanguage.EN : AppLanguage.RU);
            recreate();
        });
        z80Tab.setOnClickListener(v -> setSection(SECTION_Z80));
        zxTab.setOnClickListener(v -> setSection(SECTION_ZX));
        romTab.setOnClickListener(v -> setSection(SECTION_ROM));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        String initial;
        if (savedInstanceState != null) {
            initial = savedInstanceState.getString(STATE_QUERY, "");
            section = savedInstanceState.getInt(STATE_SECTION, SECTION_Z80);
        } else {
            initial = getIntent().getStringExtra(EXTRA_QUERY);
            if (initial == null) initial = "";
        }
        search.setText(initial);
        search.setSelection(search.length());

        updateSectionUi();
        render();
        setContentView(root);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (search != null) outState.putString(STATE_QUERY, search.getText().toString());
        outState.putInt(STATE_SECTION, section);
    }

    private void setSection(int next) {
        if (section == next) return;
        section = next;
        if (section == SECTION_ZX) search.setText("");
        updateSectionUi();
        render();
    }

    private void updateSectionUi() {
        styleTab(z80Tab, section == SECTION_Z80);
        styleTab(zxTab, section == SECTION_ZX);
        styleTab(romTab, section == SECTION_ROM);

        search.setVisibility(section == SECTION_ZX ? View.GONE : View.VISIBLE);
        if (section == SECTION_Z80) {
            search.setHint(t("Search mnemonic, syntax, opcode, flags…",
                    "Поиск по команде, синтаксису, опкоду, флагам…"));
        } else if (section == SECTION_ROM) {
            search.setHint(t("Search ROM address, routine or purpose…",
                    "Поиск по адресу, рутине или назначению ПЗУ…"));
        }
    }

    private void render() {
        if (results == null) return;
        results.removeAllViews();
        if (section == SECTION_Z80) renderZ80();
        else if (section == SECTION_ZX) renderSpectrum();
        else renderRom();
    }

    private void renderZ80() {
        String query = search.getText().toString();
        List<InstructionReference.Entry> entries = LocalizedReference.search(query, language);
        count.setText(language == AppLanguage.RU
                ? entries.size() + " записей · команды Z80"
                : entries.size() + " entries · Z80 instructions");

        for (InstructionReference.Entry entry : entries) results.addView(instructionCard(entry));
        if (entries.isEmpty()) results.addView(empty(t("No matching instruction", "Совпадений не найдено")));
    }

    private void renderSpectrum() {
        count.setText(t("48K memory · display · attributes · system variables",
                "Память 48K · экран · атрибуты · системные переменные"));
        for (SpectrumReference.Topic topic : SpectrumReference.TOPICS) {
            results.addView(topicCard(topic));
        }
    }

    private void renderRom() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<SpectrumReference.RomRoutine> matching = new ArrayList<>();
        for (SpectrumReference.RomRoutine routine : SpectrumReference.ROM) {
            String haystack = (routine.address + " " + routine.name + " "
                    + routine.description(language) + " " + routine.io(language) + " "
                    + routine.note(language)).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || haystack.contains(q)) matching.add(routine);
        }

        count.setText(language == AppLanguage.RU
                ? matching.size() + " практичных точек входа стандартного ПЗУ 48K"
                : matching.size() + " practical entry points in the standard 48K ROM");
        for (SpectrumReference.RomRoutine routine : matching) results.addView(romCard(routine));
        if (matching.isEmpty()) results.addView(empty(t("No matching ROM routine", "Рутина ПЗУ не найдена")));
    }

    private View instructionCard(InstructionReference.Entry e) {
        LinearLayout card = baseCard();

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView mnemonic = text(e.mnemonic, 18, Color.rgb(38, 102, 72), true);
        mnemonic.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        head.addView(mnemonic, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView category = text(Texts.category(language, e.category), 12,
                Color.rgb(110, 70, 120), false);
        head.addView(category);
        card.addView(head);

        card.addView(text(e.syntax, 14, Color.rgb(25, 25, 25), true));
        card.addView(text(t("Opcode: ", "Опкод: ") + e.opcode,
                12, Color.rgb(80, 80, 80), true));
        card.addView(text(language == AppLanguage.RU
                        ? "Такты: " + e.tStates + "    Флаги: " + Texts.flags(language, e.flags)
                        : "T: " + e.tStates + "    Flags: " + e.flags,
                12, Color.rgb(80, 80, 80), false));
        card.addView(text(Texts.referenceDescription(language, e.description),
                13, Color.rgb(65, 65, 65), false));
        return card;
    }

    private View topicCard(SpectrumReference.Topic topic) {
        LinearLayout card = baseCard();
        TextView title = text(topic.title(language), 18, Color.rgb(38, 102, 72), false);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView subtitle = text(topic.subtitle(language), 13, Color.rgb(85, 99, 91), false);
        subtitle.setPadding(0, dp(2), 0, dp(7));
        card.addView(subtitle);

        for (String line : topic.lines(language)) {
            TextView item = text(line, 13, Color.rgb(30, 35, 32), true);
            item.setPadding(dp(6), dp(2), 0, dp(2));
            card.addView(item);
        }

        TextView note = text(topic.note(language), 13, Color.rgb(72, 82, 76), false);
        note.setPadding(0, dp(8), 0, 0);
        card.addView(note);
        return card;
    }

    private View romCard(SpectrumReference.RomRoutine routine) {
        LinearLayout card = baseCard();

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView address = text(routine.address, 16, Color.rgb(38, 102, 72), true);
        address.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        head.addView(address, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView name = text(routine.name, 13, Color.rgb(100, 75, 115), true);
        head.addView(name);
        card.addView(head);

        card.addView(text(routine.description(language), 14,
                Color.rgb(35, 40, 37), false));

        TextView io = text(routine.io(language), 13, Color.rgb(30, 88, 61), true);
        io.setPadding(0, dp(7), 0, 0);
        card.addView(io);

        TextView note = text(routine.note(language), 12, Color.rgb(85, 95, 89), false);
        note.setPadding(0, dp(6), 0, 0);
        card.addView(note);
        return card;
    }

    private LinearLayout baseCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(11), dp(13), dp(11));
        card.setBackground(UiStyle.rounded(this, Color.WHITE, 18,
                Color.rgb(207, 222, 213), 1));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, dp(4), 0, dp(5));
        card.setLayoutParams(cp);
        return card;
    }

    private TextView empty(String value) {
        TextView empty = text(value, 15, Color.rgb(110, 110, 110), false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(8), dp(32), dp(8), dp(32));
        return empty;
    }

    private Button tabButton(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(40));
        return button;
    }

    private LinearLayout.LayoutParams tabParams(int leftDp, int rightDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(leftDp), 0, dp(rightDp), 0);
        return p;
    }

    private void styleTab(Button button, boolean active) {
        if (active) UiStyle.styleGreenButton(button);
        else UiStyle.styleSoftGreenButton(button);
    }

    private TextView text(String value, int sp, int color, boolean mono) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(3), 0, 0);
        if (mono) v.setTypeface(android.graphics.Typeface.MONOSPACE);
        return v;
    }

    private String t(String en, String ru) {
        return Texts.pick(language, en, ru);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
