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
import com.alexzab.z80pocketide.reference.InstructionDetails;
import com.alexzab.z80pocketide.reference.SpectrumReference;
import com.alexzab.z80pocketide.ui.SpectrumScreenMapView;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

public final class ReferenceActivity extends Activity {
    public static final String EXTRA_QUERY = "query";
    private static final String STATE_QUERY = "reference_query";
    private static final String STATE_SECTION = "reference_section";
    private static final String STATE_EXPANDED = "reference_expanded";

    private static final int SECTION_Z80 = 0;
    private static final int SECTION_ZX = 1;
    private static final int SECTION_ROM = 2;
    private static final int SECTION_IO = 3;

    private LinearLayout results;
    private TextView count;
    private EditText search;
    private Button z80Tab;
    private Button zxTab;
    private Button romTab;
    private Button ioTab;
    private AppLanguage language;
    private int section = SECTION_Z80;
    private final Set<String> expandedInstructions = new HashSet<>();

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
        ioTab = tabButton("I/O");
        tabs.addView(z80Tab, tabParams(0, 2));
        tabs.addView(zxTab, tabParams(2, 2));
        tabs.addView(romTab, tabParams(2, 2));
        tabs.addView(ioTab, tabParams(2, 0));
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
        ioTab.setOnClickListener(v -> setSection(SECTION_IO));

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
            ArrayList<String> expanded = savedInstanceState.getStringArrayList(STATE_EXPANDED);
            if (expanded != null) expandedInstructions.addAll(expanded);
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
        outState.putStringArrayList(STATE_EXPANDED, new ArrayList<>(expandedInstructions));
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
        styleTab(ioTab, section == SECTION_IO);

        search.setVisibility(section == SECTION_ZX ? View.GONE : View.VISIBLE);
        if (section == SECTION_Z80) {
            search.setHint(t("Search mnemonic, syntax, opcode, flags…",
                    "Поиск по команде, синтаксису, опкоду, флагам…"));
        } else if (section == SECTION_ROM) {
            search.setHint(t("Search ROM address, routine or purpose…",
                    "Поиск по адресу, рутине или назначению ПЗУ…"));
        } else if (section == SECTION_IO) {
            search.setHint(t("Search port, keyboard, joystick, AY…",
                    "Поиск по порту, клавиатуре, джойстику, AY…"));
        }
    }

    private void render() {
        if (results == null) return;
        results.removeAllViews();
        if (section == SECTION_Z80) renderZ80();
        else if (section == SECTION_ZX) renderSpectrum();
        else if (section == SECTION_ROM) renderRom();
        else renderIo();
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

        SpectrumScreenMapView map = new SpectrumScreenMapView(this);
        map.setLanguage(language);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(245));
        mp.setMargins(0, dp(4), 0, dp(5));
        results.addView(map, mp);

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

    private void renderIo() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<SpectrumReference.PortEntry> matching = new ArrayList<>();
        for (SpectrumReference.PortEntry entry : SpectrumReference.PORTS) {
            StringBuilder haystack = new StringBuilder();
            haystack.append(entry.port(language)).append(' ')
                    .append(entry.title(language)).append(' ')
                    .append(entry.note(language)).append(' ');
            for (String line : entry.lines(language)) haystack.append(line).append(' ');
            if (q.isEmpty() || haystack.toString().toLowerCase(Locale.ROOT).contains(q)) {
                matching.add(entry);
            }
        }

        count.setText(language == AppLanguage.RU
                ? matching.size() + " шпаргалок по портам и железу"
                : matching.size() + " port and hardware quick references");
        for (SpectrumReference.PortEntry entry : matching) results.addView(portCard(entry));
        if (matching.isEmpty()) results.addView(empty(t("No matching I/O entry", "Порт или устройство не найдено")));
    }

    private View portCard(SpectrumReference.PortEntry entry) {
        LinearLayout card = baseCard();

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView port = text(entry.port(language), 16, Color.rgb(38, 102, 72), true);
        port.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        head.addView(port, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = text(entry.title(language), 13, Color.rgb(100, 75, 115), false);
        title.setGravity(Gravity.END);
        head.addView(title);
        card.addView(head);

        for (String line : entry.lines(language)) {
            TextView item = text(line, 13, Color.rgb(30, 35, 32), true);
            item.setPadding(dp(6), dp(2), 0, dp(2));
            card.addView(item);
        }

        TextView note = text(entry.note(language), 12, Color.rgb(85, 95, 89), false);
        note.setPadding(0, dp(7), 0, 0);
        card.addView(note);
        return card;
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

        // The compact view stays lightweight. Expanded variants are only
        // materialized when requested, so search through all families is fast.
        final List<InstructionDetails.Variant> variants = InstructionDetails.variants(e);
        final boolean alreadyExpanded = expandedInstructions.contains(e.mnemonic);
        Button expand = new Button(this);
        expand.setTextSize(12);
        expand.setMinHeight(dp(37));
        expand.setMinWidth(0);
        expand.setAllCaps(false);
        UiStyle.styleSoftGreenButton(expand);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(39));
        buttonParams.setMargins(0, dp(8), 0, 0);
        card.addView(expand, buttonParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(alreadyExpanded ? View.VISIBLE : View.GONE);
        card.addView(details);
        if (alreadyExpanded) fillInstructionDetails(details, e, variants);
        updateExpandLabel(expand, alreadyExpanded, variants.size());
        expand.setOnClickListener(v -> {
            boolean show = !expandedInstructions.contains(e.mnemonic);
            if (show) {
                expandedInstructions.add(e.mnemonic);
                if (details.getChildCount() == 0)
                    fillInstructionDetails(details, e, variants);
            } else {
                expandedInstructions.remove(e.mnemonic);
            }
            details.setVisibility(show ? View.VISIBLE : View.GONE);
            updateExpandLabel(expand, show, variants.size());
        });
        return card;
    }

    private void updateExpandLabel(Button button, boolean show, int variantCount) {
        button.setText((show ? "▴ " : "▾ ") +
                t(show ? "Hide details" : "Show details",
                        show ? "Свернуть подробности" : "Подробности") +
                " · " + variantCount +
                t(" forms", " вариантов"));
        button.setContentDescription(t(
                show ? "Collapse instruction details" : "Expand instruction forms and flag effects",
                show ? "Свернуть подробности команды" : "Показать формы команды и значения флагов"));
    }

    private void fillInstructionDetails(LinearLayout area, InstructionReference.Entry e,
                                        List<InstructionDetails.Variant> variants) {
        area.addView(text(t("Every timing is in un-contended Z80 T-states. Conditional "
                                + "timings specify taken / not taken.",
                        "Все такты указаны без задержек от доступа к видеопамяти. "
                                + "В условных командах: переход выполнен / не выполнен."),
                12, 0xFF5B7663, false));

        TextView flagLegend = text(t(
                        "Flags F: S sign · Z zero · H half-carry · P/V parity or "
                                + "overflow · N subtraction · C carry. Unchanged means "
                                + "the old flag value is retained. F3/F5 are undocumented.",
                        "Флаги F: S знак · Z нуль · H полуперенос · P/V чётность "
                                + "или переполнение · N вычитание · C перенос. "
                                + "«Без изменения» означает сохранение прежнего бита. "
                                + "F3/F5 недокументированы."),
                12, 0xFF637368, false);
        flagLegend.setPadding(dp(5), dp(8), dp(5), dp(9));
        area.addView(flagLegend);

        for (InstructionDetails.Variant variant : variants) {
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(dp(10), dp(8), dp(10), dp(9));
            panel.setBackground(UiStyle.rounded(this,
                    0xFFF4F9F5, 12, 0xFFDBE9DF, 1));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, dp(3), 0, dp(5));
            area.addView(panel, p);

            LinearLayout titleLine = new LinearLayout(this);
            titleLine.setOrientation(LinearLayout.HORIZONTAL);
            titleLine.setGravity(Gravity.CENTER_VERTICAL);
            TextView form = text(variant.form, 14, 0xFF245F40, true);
            form.setTypeface(android.graphics.Typeface.MONOSPACE,
                    android.graphics.Typeface.BOLD);
            form.setSingleLine(false);
            titleLine.addView(form, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView cycles = text(variant.timing + " T", 12, 0xFF8B521B, true);
            cycles.setGravity(Gravity.RIGHT | Gravity.TOP);
            cycles.setPadding(dp(7), dp(3), 0, 0);
            titleLine.addView(cycles);
            panel.addView(titleLine);

            TextView desc = text(variant.description(language == AppLanguage.RU),
                    13, 0xFF354139, false);
            desc.setPadding(0, dp(6), 0, dp(7));
            panel.addView(desc);

            TextView flagsTitle = text(t("Status flags after execution:",
                    "Флаги после выполнения:"), 12, 0xFF5E7063, false);
            panel.addView(flagsTitle);
            TextView flagsBody = text(variant.flags(language == AppLanguage.RU),
                    12, 0xFF334C3D, true);
            flagsBody.setPadding(dp(5), dp(3), 0, dp(2));
            panel.addView(flagsBody);
        }
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
