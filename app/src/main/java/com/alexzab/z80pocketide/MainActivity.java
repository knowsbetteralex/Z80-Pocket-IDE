package com.alexzab.z80pocketide;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;
import com.alexzab.z80pocketide.editor.CodeEditorView;
import com.alexzab.z80pocketide.editor.EditorDocument;
import com.alexzab.z80pocketide.editor.EditorPreferences;
import com.alexzab.z80pocketide.editor.EditorSettingsDialog;
import com.alexzab.z80pocketide.editor.SyntaxHighlighter;
import com.alexzab.z80pocketide.emulator.EmulatorSettings;
import com.alexzab.z80pocketide.examples.ExamplePrograms;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;
import com.alexzab.z80pocketide.zx.TapWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE_TAP = 1001;
    private static final int REQUEST_EXAMPLE = 1002;
    private static final int REQUEST_OPEN_SOURCE = 1003;
    private static final int REQUEST_SAVE_SOURCE_AS = 1004;

    private static final String STATE_TITLES = "tab_titles";
    private static final String STATE_TEXTS = "tab_texts";
    private static final String STATE_URIS = "tab_uris";
    private static final String STATE_DIRTY = "tab_dirty";
    private static final String STATE_CURSORS = "tab_cursors";
    private static final String STATE_SCROLL_X = "tab_scroll_x";
    private static final String STATE_SCROLL_Y = "tab_scroll_y";
    private static final String STATE_ACTIVE = "tab_active";
    private static final String STATE_PENDING_SAVE = "pending_save";
    private static final String STATE_PENDING_CLOSE = "pending_close";

    private final List<EditorDocument> documents = new ArrayList<>();

    private CodeEditorView editor;
    private TextView status;
    private TextView statistics;
    private Button clearLinesButton;
    private Button mainMenuButton;
    private AssemblyResult liveAnalysis;
    private String analyzedSource;
    private final Handler analyzeHandler = new Handler(Looper.getMainLooper());
    private final Runnable delayedAnalyze = this::performLiveAnalysis;
    private Button runTap;
    private Button saveTap;
    private Button emulatorButton;
    private Button tabKeyButton;
    private HorizontalScrollView tabStrip;
    private LinearLayout tabRow;
    private AppLanguage language;

    private int activeIndex = -1;
    private int pendingSaveIndex = -1;
    private boolean pendingCloseAfterSave;
    private boolean loadingDocument;
    private byte[] pendingTapToSave;
    private int untitledCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);

        getWindow().setStatusBarColor(Color.rgb(250, 250, 250));
        getWindow().setNavigationBarColor(Color.rgb(250, 250, 250));

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

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(4), 0, dp(4), dp(4));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Z80 Pocket IDE");
        title.setTextSize(20);
        title.setTextColor(Color.rgb(25, 25, 25));

        TextView subtitle = new TextView(this);
        subtitle.setText(t("for ZX Spectrum · v0.14", "для ZX Spectrum · v0.14"));
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(100, 100, 100));

        titleBlock.addView(title);
        titleBlock.addView(subtitle);
        topBar.addView(titleBlock, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button languageButton = compactButton(language == AppLanguage.RU ? "RU" : "EN");
        mainMenuButton = compactButton("☰");
        mainMenuButton.setContentDescription(t("Menu", "Меню"));
        topBar.addView(languageButton);
        topBar.addView(mainMenuButton);
        root.addView(topBar);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        LinearLayout workspaceBar = new LinearLayout(this);
        workspaceBar.setOrientation(LinearLayout.HORIZONTAL);
        workspaceBar.setGravity(Gravity.CENTER_VERTICAL);
        workspaceBar.setPadding(dp(4), dp(4), dp(4), dp(4));
        workspaceBar.setBackground(UiStyle.rounded(this,
                Color.rgb(235, 243, 238), 18, Color.rgb(205, 222, 212), 1));

        Button newTabButton = compactButton("+");
        newTabButton.setContentDescription(t("New tab", "Новая вкладка"));
        newTabButton.setOnClickListener(v -> newTab());
        workspaceBar.addView(newTabButton);

        tabStrip = new HorizontalScrollView(this);
        tabStrip.setHorizontalScrollBarEnabled(false);
        tabStrip.setFillViewport(false);
        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER_VERTICAL);
        tabStrip.addView(tabRow, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        workspaceBar.addView(tabStrip, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams workspaceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        workspaceParams.setMargins(0, dp(5), 0, dp(4));
        root.addView(workspaceBar, workspaceParams);

        editor = new CodeEditorView(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextColor(Color.rgb(25, 25, 25));
        editor.setBackground(UiStyle.rounded(this,
                Color.rgb(252, 253, 252), 20, Color.rgb(207, 222, 213), 1));
        editor.setPadding(dp(54), dp(12), dp(14), dp(12));
        editor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(true);

        LinearLayout editorTools = new LinearLayout(this);
        editorTools.setOrientation(LinearLayout.HORIZONTAL);
        editorTools.setGravity(Gravity.CENTER_VERTICAL);
        editorTools.setPadding(dp(2), dp(2), dp(2), dp(4));

        tabKeyButton = compactButton("TAB ⇥");
        editorTools.addView(tabKeyButton);
        TextView gestureHint = new TextView(this);
        gestureHint.setText(t("Tap/drag line numbers to select",
                "Выбор строк: тап/свайп по номерам"));
        gestureHint.setTextColor(Color.rgb(88, 113, 98));
        gestureHint.setTextSize(11);
        gestureHint.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        editorTools.addView(gestureHint, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(editorTools);

        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        editorParams.setMargins(0, 0, 0, dp(5));
        root.addView(editor, editorParams);

        SyntaxHighlighter.attach(editor);

        tabKeyButton.setOnClickListener(v -> editor.insertTabFromSettings());
        editor.setOnLineSelectionChanged(this::updateStatistics);
        editor.setOnCaretMoved(this::updateStatistics);
        refreshEditorTools();

        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(10), dp(7), dp(10), dp(5));
        bottomPanel.setBackground(UiStyle.rounded(this,
                Color.rgb(240, 247, 243), 20, Color.rgb(205, 222, 212), 1));
        if (Build.VERSION.SDK_INT >= 21) bottomPanel.setElevation(dp(4));

        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);

        status = new TextView(this);
        status.setText(t("Tabs ready · File opens and saves .asm sources",
                "Вкладки готовы · Файл открывает и сохраняет исходники .asm"));
        status.setTextSize(13);
        status.setTextColor(Color.rgb(90, 90, 90));
        status.setPadding(dp(4), dp(3), dp(4), dp(5));
        infoRow.addView(status, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        bottomPanel.addView(infoRow);

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setGravity(Gravity.CENTER_VERTICAL);
        statistics = new TextView(this);
        statistics.setText(t("Size/T: analyzing…", "Байты/такты: анализ…"));
        statistics.setTextSize(12);
        statistics.setTextColor(Color.rgb(35, 105, 66));
        statistics.setPadding(dp(4), 0, dp(4), dp(5));
        statsRow.addView(statistics, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        clearLinesButton = compactButton(t("Clear selection ✕", "Снять выбор ✕"));
        clearLinesButton.setVisibility(View.GONE);
        clearLinesButton.setOnClickListener(v -> editor.clearLineSelection());
        statsRow.addView(clearLinesButton);
        bottomPanel.addView(statsRow);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);

        Button build = actionButton(t("Build", "Собрать"));
        runTap = actionButton(t("Run", "Запуск"));
        saveTap = actionButton(t("Save .tap", "Сохранить .tap"));

        runTap.setEnabled(false);
        saveTap.setEnabled(false);

        actions.addView(build, weightedButtonParams());
        actions.addView(runTap, weightedButtonParams());
        actions.addView(saveTap, weightedButtonParams());
        bottomPanel.addView(actions);
        root.addView(bottomPanel);

        build.setOnClickListener(v -> buildSource());
        runTap.setOnClickListener(v -> runTapInEmulator());
        saveTap.setOnClickListener(v -> saveTapFile());
        mainMenuButton.setOnClickListener(v -> showMainMenu());
        languageButton.setOnClickListener(v -> toggleLanguage());

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (loadingDocument || !hasActiveDocument()) return;
                EditorDocument doc = currentDocument();
                String next = editor.getSourceText();
                if (next.equals(doc.text)) return;

                boolean wasDirty = doc.dirty;
                doc.text = next;
                doc.dirty = true;
                doc.invalidateBuild();
                runTap.setEnabled(false);
                saveTap.setEnabled(false);
                scheduleAnalysis();
                setStatus(t("Modified · source file not saved",
                        "Изменено · исходный файл не сохранён"), Color.rgb(120, 85, 0));
                if (!wasDirty) renderTabs();
            }
        });

        setContentView(root);
        restoreWorkspace(savedInstanceState);
        scheduleAnalysis();
    }

    @Override
    protected void onDestroy() {
        analyzeHandler.removeCallbacks(delayedAnalyze);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        captureCurrentDocument();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        captureCurrentDocument();
        super.onSaveInstanceState(outState);

        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> texts = new ArrayList<>();
        ArrayList<String> uris = new ArrayList<>();
        ArrayList<Integer> dirty = new ArrayList<>();
        ArrayList<Integer> cursors = new ArrayList<>();
        ArrayList<Integer> scrollX = new ArrayList<>();
        ArrayList<Integer> scrollY = new ArrayList<>();

        for (EditorDocument doc : documents) {
            titles.add(doc.title);
            texts.add(doc.text);
            uris.add(doc.uriString == null ? "" : doc.uriString);
            dirty.add(doc.dirty ? 1 : 0);
            cursors.add(doc.cursor);
            scrollX.add(doc.scrollX);
            scrollY.add(doc.scrollY);
        }

        outState.putStringArrayList(STATE_TITLES, titles);
        outState.putStringArrayList(STATE_TEXTS, texts);
        outState.putStringArrayList(STATE_URIS, uris);
        outState.putIntegerArrayList(STATE_DIRTY, dirty);
        outState.putIntegerArrayList(STATE_CURSORS, cursors);
        outState.putIntegerArrayList(STATE_SCROLL_X, scrollX);
        outState.putIntegerArrayList(STATE_SCROLL_Y, scrollY);
        outState.putInt(STATE_ACTIVE, activeIndex);
        outState.putInt(STATE_PENDING_SAVE, pendingSaveIndex);
        outState.putBoolean(STATE_PENDING_CLOSE, pendingCloseAfterSave);
    }

    @Override
    public void onBackPressed() {
        if (!hasDocumentsNeedingSave()) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(t("Unsaved tabs", "Несохранённые вкладки"))
                .setMessage(t("Some source tabs are not saved. Exit without saving them?",
                        "Некоторые вкладки не сохранены. Выйти без сохранения?"))
                .setPositiveButton(t("Exit", "Выйти"), (d, w) -> MainActivity.super.onBackPressed())
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void restoreWorkspace(Bundle state) {
        documents.clear();
        if (state != null) {
            ArrayList<String> titles = state.getStringArrayList(STATE_TITLES);
            ArrayList<String> texts = state.getStringArrayList(STATE_TEXTS);
            ArrayList<String> uris = state.getStringArrayList(STATE_URIS);
            ArrayList<Integer> dirty = state.getIntegerArrayList(STATE_DIRTY);
            ArrayList<Integer> cursors = state.getIntegerArrayList(STATE_CURSORS);
            ArrayList<Integer> scrollX = state.getIntegerArrayList(STATE_SCROLL_X);
            ArrayList<Integer> scrollY = state.getIntegerArrayList(STATE_SCROLL_Y);

            if (titles != null && texts != null && titles.size() == texts.size()) {
                for (int i = 0; i < titles.size(); i++) {
                    String uri = uris != null && i < uris.size() ? uris.get(i) : "";
                    EditorDocument doc = new EditorDocument(
                            titles.get(i), texts.get(i), uri == null || uri.isEmpty() ? null : uri,
                            dirty != null && i < dirty.size() && dirty.get(i) != 0);
                    doc.cursor = cursors != null && i < cursors.size() ? cursors.get(i) : 0;
                    doc.scrollX = scrollX != null && i < scrollX.size() ? scrollX.get(i) : 0;
                    doc.scrollY = scrollY != null && i < scrollY.size() ? scrollY.get(i) : 0;
                    documents.add(doc);
                }
            }
            activeIndex = state.getInt(STATE_ACTIVE, 0);
            pendingSaveIndex = state.getInt(STATE_PENDING_SAVE, -1);
            pendingCloseAfterSave = state.getBoolean(STATE_PENDING_CLOSE, false);
        }

        if (documents.isEmpty()) {
            documents.add(new EditorDocument(nextUntitledName(), starterSource(), null, false));
            activeIndex = 0;
        }
        activeIndex = Math.max(0, Math.min(activeIndex, documents.size() - 1));
        updateUntitledCounter();
        renderTabs();
        loadActiveDocument();
    }

    private String starterSource() {
        return "ORG $8000\n\nSTART:\n    NOP\n    RET\n";
    }

    private String untitledName(int number, AppLanguage lang) {
        return Texts.pick(lang, "Untitled ", "Без имени ") + number + ".asm";
    }

    private String nextUntitledName() {
        return untitledName(untitledCounter++, language);
    }

    private int untitledNumber(String title) {
        if (title == null) return -1;
        String lower = title.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("untitled ") && !lower.startsWith("без имени ")) return -1;
        int dot = lower.lastIndexOf(".asm");
        int space = lower.lastIndexOf(' ', dot >= 0 ? dot : lower.length());
        if (space < 0) return -1;
        String number = lower.substring(space + 1, dot >= 0 ? dot : lower.length()).trim();
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void updateUntitledCounter() {
        int max = 0;
        for (EditorDocument doc : documents) {
            int number = untitledNumber(doc.title);
            if (number > max) max = number;
        }
        untitledCounter = Math.max(1, max + 1);
    }

    private String t(String en, String ru) {
        return Texts.pick(language, en, ru);
    }

    private Button compactButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(11), 0, dp(11), 0);
        button.setMinHeight(dp(38));
        UiStyle.styleSoftGreenButton(button);
        return button;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setMinWidth(0);
        button.setMinHeight(dp(46));
        UiStyle.styleGreenButton(button);
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private void renderTabs() {
        if (tabRow == null) return;
        tabRow.removeAllViews();
        final View[] activeCell = new View[1];

        for (int i = 0; i < documents.size(); i++) {
            final int index = i;
            EditorDocument doc = documents.get(i);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.setPadding(dp(2), 0, dp(1), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(i == activeIndex ? Color.rgb(220, 240, 228) : Color.rgb(238, 241, 239));
            bg.setStroke(dp(1), i == activeIndex
                    ? Color.rgb(92, 153, 118) : Color.rgb(207, 214, 210));
            cell.setBackground(bg);

            TextView tabTitle = new TextView(this);
            tabTitle.setText((doc.dirty ? "● " : "") + doc.title);
            tabTitle.setTextSize(13);
            tabTitle.setTextColor(Color.rgb(35, 35, 35));
            tabTitle.setTypeface(Typeface.DEFAULT,
                    i == activeIndex ? Typeface.BOLD : Typeface.NORMAL);
            tabTitle.setPadding(dp(9), dp(7), dp(6), dp(7));
            tabTitle.setSingleLine(true);
            tabTitle.setOnClickListener(v -> selectTab(index));
            tabTitle.setOnLongClickListener(v -> {
                showTabMenu(index);
                return true;
            });
            cell.addView(tabTitle);

            TextView close = new TextView(this);
            close.setText("×");
            close.setTextSize(20);
            close.setGravity(Gravity.CENTER);
            close.setTextColor(Color.rgb(100, 100, 100));
            close.setPadding(dp(6), dp(2), dp(7), dp(2));
            close.setOnClickListener(v -> requestCloseTab(index));
            cell.addView(close, new LinearLayout.LayoutParams(dp(34), dp(38)));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(dp(2), 0, dp(2), 0);
            tabRow.addView(cell, cp);
            if (i == activeIndex) activeCell[0] = cell;
        }

        if (activeCell[0] != null) {
            tabStrip.post(() -> tabStrip.smoothScrollTo(
                    Math.max(0, activeCell[0].getLeft() - dp(24)), 0));
        }
    }

    private void selectTab(int index) {
        if (index < 0 || index >= documents.size() || index == activeIndex) return;
        captureCurrentDocument();
        activeIndex = index;
        renderTabs();
        loadActiveDocument();
    }

    private void loadActiveDocument() {
        if (!hasActiveDocument()) return;
        EditorDocument doc = currentDocument();
        loadingDocument = true;
        liveAnalysis = null;
        analyzedSource = null;
        analyzeHandler.removeCallbacks(delayedAnalyze);
        editor.setSourceText(doc.text);
        editor.setSelection(Math.max(0, Math.min(doc.cursor, editor.length())));
        loadingDocument = false;
        editor.post(() -> editor.scrollTo(doc.scrollX, doc.scrollY));

        boolean built = doc.buildIsCurrent();
        runTap.setEnabled(built);
        saveTap.setEnabled(built);
        scheduleAnalysis();
        setStatus(doc.dirty
                        ? t("Modified · not saved", "Изменено · не сохранено")
                        : (doc.hasFile()
                            ? t("File · ", "Файл · ") + doc.title
                            : t("Tab · ", "Вкладка · ") + doc.title),
                doc.dirty ? Color.rgb(120, 85, 0) : Color.rgb(90, 90, 90));
    }

    private void captureCurrentDocument() {
        if (!hasActiveDocument() || editor == null || loadingDocument) return;
        EditorDocument doc = currentDocument();
        doc.text = editor.getSourceText();
        doc.cursor = Math.max(0, editor.getSelectionStart());
        doc.scrollX = editor.getScrollX();
        doc.scrollY = editor.getScrollY();
    }

    private boolean hasActiveDocument() {
        return activeIndex >= 0 && activeIndex < documents.size();
    }

    private EditorDocument currentDocument() {
        return documents.get(activeIndex);
    }

    private boolean hasDocumentsNeedingSave() {
        for (EditorDocument doc : documents) {
            if (doc.needsCloseConfirmation()) return true;
        }
        return false;
    }

    private void scheduleAnalysis() {
        analyzeHandler.removeCallbacks(delayedAnalyze);
        liveAnalysis = null;
        analyzedSource = null;
        if (editor != null) editor.clearInstructionMetrics();
        updateStatistics();
        analyzeHandler.postDelayed(delayedAnalyze, 280);
    }

    private void performLiveAnalysis() {
        if (!hasActiveDocument() || editor == null || loadingDocument) return;
        String source = editor.getSourceText();
        try {
            AssemblyResult value = new Assembler().assemble(source);
            if (!source.equals(editor.getSourceText())) return;
            liveAnalysis = value;
            analyzedSource = source;
            editor.setInstructionMetrics(value.getLineSizes(),
                    value.getLineMinCyclesArray(), value.getLineMaxCyclesArray(),
                    value.getInstructionFlags());
        } catch (RuntimeException ignored) {
            liveAnalysis = null;
            analyzedSource = null;
            editor.clearInstructionMetrics();
        }
        updateStatistics();
    }

    private void updateStatistics() {
        if (statistics == null || editor == null) return;
        List<Integer> picked = editor.getSelectedLines();
        if (clearLinesButton != null)
            clearLinesButton.setVisibility(picked.isEmpty() ? View.GONE : View.VISIBLE);

        if (liveAnalysis == null || !editor.getSourceText().equals(analyzedSource)) {
            statistics.setText(t("Size/T: — (check source)", "Байты/такты: — (проверьте код)"));
            return;
        }

        boolean selection = picked.size() > 1;
        int bytes = selection ? 0 : liveAnalysis.getBytes().length;
        int min = selection ? 0 : liveAnalysis.getMinCycles();
        int max = selection ? 0 : liveAnalysis.getMaxCycles();
        if (selection) {
            for (int line : picked) {
                bytes += liveAnalysis.getLineSize(line);
                min += liveAnalysis.getLineMinCycles(line);
                max += liveAnalysis.getLineMaxCycles(line);
            }
        }
        String label = selection
                ? (language == AppLanguage.RU ? "Выбрано " : "Selected ") + picked.size()
                + (language == AppLanguage.RU ? " строк" : " lines")
                : t("Whole source", "Весь код");
        String cycles = min == max ? String.valueOf(min) : min + "–" + max;
        statistics.setText(label + "  ·  " + bytes + " B  ·  " + cycles + " T");
    }

    private void showMainMenu() {
        PopupMenu popup = new PopupMenu(this, mainMenuButton);
        Menu menu = popup.getMenu();

        Menu files = menu.addSubMenu(t("Files", "Файлы"));
        files.add(0, 1, 0, t("New tab", "Новая вкладка"));
        files.add(0, 2, 1, t("Open .asm…", "Открыть .asm…"));
        files.add(0, 3, 2, t("Save source", "Сохранить исходник"));
        files.add(0, 4, 3, t("Save as…", "Сохранить как…"));
        files.add(0, 5, 4, t("Close tab", "Закрыть вкладку"));

        menu.add(0, 10, 10, t("Examples / Routines", "Примеры / Подпрограммы"));
        menu.add(0, 11, 11, t("Z80 / Spectrum reference", "Справочник Z80 / Spectrum"));

        Menu tools = menu.addSubMenu(t("Tools", "Инструменты"));
        tools.add(0, 12, 0, t("DEC / HEX / BIN converter", "Конвертер DEC / HEX / BIN"));
        tools.add(0, 13, 1, t("Format document", "Форматировать код"));
        tools.add(0, 14, 2, t("Fold / unfold block", "Свернуть / развернуть блок"));
        tools.add(0, 15, 3, t("Editor settings", "Настройки редактора"));
        tools.add(0, 16, 4, t("Choose TAP app", "Выбрать эмулятор TAP"));

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: newTab(); break;
                case 2: openSourceFilePicker(); break;
                case 3: saveDocument(activeIndex, false); break;
                case 4: requestSaveAs(activeIndex, false); break;
                case 5: requestCloseTab(activeIndex); break;
                case 10: showExamples(); break;
                case 11: openReference(); break;
                case 12: startActivity(new Intent(this, NumberConverterActivity.class)); break;
                case 13:
                    editor.formatAllFromSettings();
                    setStatus(t("Document formatted", "Документ отформатирован"),
                            Color.rgb(45, 100, 72));
                    scheduleAnalysis();
                    break;
                case 14:
                    if (!editor.toggleFoldAtCursor()) {
                        Toast.makeText(this, t("Place cursor under a label",
                                "Поставьте курсор под меткой"), Toast.LENGTH_SHORT).show();
                    }
                    scheduleAnalysis();
                    break;
                case 15: showEditorSettings(); break;
                case 16: showEmulatorDialog(); break;
                default: return false;
            }
            return true;
        });
        popup.show();
    }

    private void showFileMenu() {
        String[] items = {
                t("New tab", "Новая вкладка"),
                t("Open .asm…", "Открыть .asm…"),
                t("Save source", "Сохранить исходник"),
                t("Save source as…", "Сохранить исходник как…"),
                t("Close tab", "Закрыть вкладку"),
                t("Editor settings…", "Настройки редактора…")
        };
        new AlertDialog.Builder(this)
                .setTitle(t("File", "Файл"))
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: newTab(); break;
                        case 1: openSourceFilePicker(); break;
                        case 2: saveDocument(activeIndex, false); break;
                        case 3: requestSaveAs(activeIndex, false); break;
                        case 4: requestCloseTab(activeIndex); break;
                        case 5: showEditorSettings(); break;
                        default: break;
                    }
                })
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void showEditorSettings() {
        EditorSettingsDialog.show(this, language, () -> {
            editor.refreshPreferences();
            refreshEditorTools();
            setStatus(t("Editor settings applied", "Настройки редактора применены"),
                    Color.rgb(45, 80, 150));
        });
    }

    private void refreshEditorTools() {
        if (editor == null) return;
        editor.refreshPreferences();
        EditorPreferences.Snapshot prefs = editor.getEditorPreferences();
        if (tabKeyButton != null)
            tabKeyButton.setVisibility(prefs.showTabButton ? View.VISIBLE : View.GONE);
    }

    private void showTabMenu(int index) {
        if (index < 0 || index >= documents.size()) return;
        String[] items = {
                t("Save", "Сохранить"),
                t("Save as…", "Сохранить как…"),
                t("Close", "Закрыть")
        };
        new AlertDialog.Builder(this)
                .setTitle(documents.get(index).title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) saveDocument(index, false);
                    else if (which == 1) requestSaveAs(index, false);
                    else if (which == 2) requestCloseTab(index);
                })
                .show();
    }

    private void newTab() {
        captureCurrentDocument();
        documents.add(new EditorDocument(nextUntitledName(), starterSource(), null, false));
        activeIndex = documents.size() - 1;
        renderTabs();
        loadActiveDocument();
    }

    private void openSourceFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "text/plain", "application/octet-stream", "text/x-asm", "application/x-asm"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OPEN_SOURCE);
    }

    private void requestSaveAs(int index, boolean closeAfterSave) {
        if (index < 0 || index >= documents.size()) return;
        if (index == activeIndex) captureCurrentDocument();
        pendingSaveIndex = index;
        pendingCloseAfterSave = closeAfterSave;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, ensureAsmName(documents.get(index).title));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_SAVE_SOURCE_AS);
    }

    private void saveDocument(int index, boolean closeAfterSave) {
        if (index < 0 || index >= documents.size()) return;
        if (index == activeIndex) captureCurrentDocument();
        EditorDocument doc = documents.get(index);
        if (!doc.hasFile()) {
            requestSaveAs(index, closeAfterSave);
            return;
        }

        try {
            writeSource(Uri.parse(doc.uriString), doc.text);
            doc.dirty = false;
            renderTabs();
            setStatus(t("Saved · ", "Сохранено · ") + doc.title, Color.rgb(0, 110, 45));
            Toast.makeText(this, t("Source saved", "Исходник сохранён"), Toast.LENGTH_SHORT).show();
            if (closeAfterSave) closeTabImmediately(index);
        } catch (Exception ex) {
            setStatus(t("Save error · ", "Ошибка сохранения · ") + safeMessage(ex),
                    Color.rgb(180, 30, 30));
            Toast.makeText(this,
                    t("Cannot overwrite this file; try Save as…",
                            "Не удалось перезаписать файл; попробуйте «Сохранить как…»"),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestCloseTab(int index) {
        if (index < 0 || index >= documents.size()) return;
        if (index == activeIndex) captureCurrentDocument();
        EditorDocument doc = documents.get(index);
        if (!doc.needsCloseConfirmation()) {
            closeTabImmediately(index);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(t("Save changes?", "Сохранить изменения?"))
                .setMessage(doc.title)
                .setPositiveButton(t("Save", "Сохранить"),
                        (dialog, which) -> saveDocument(index, true))
                .setNegativeButton(t("Don't save", "Не сохранять"),
                        (dialog, which) -> closeTabImmediately(index))
                .setNeutralButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void closeTabImmediately(int index) {
        if (index < 0 || index >= documents.size()) return;
        documents.remove(index);
        if (documents.isEmpty()) {
            // Keep one quiet, truly blank scratch tab. Repeatedly closing the last tab
            // must not just count Untitled 1, 2, 3... in front of the user.
            untitledCounter = 2;
            documents.add(new EditorDocument(untitledName(1, language), "", null, false));
            activeIndex = 0;
        } else if (index < activeIndex) {
            activeIndex--;
        } else if (index == activeIndex) {
            activeIndex = Math.min(index, documents.size() - 1);
        }
        updateUntitledCounter();
        renderTabs();
        loadActiveDocument();
    }

    private void addSourceTab(String title, String source, String uriString, boolean dirty) {
        captureCurrentDocument();
        EditorDocument doc = new EditorDocument(title, source, uriString, dirty);
        documents.add(doc);
        activeIndex = documents.size() - 1;
        renderTabs();
        loadActiveDocument();
    }

    private void openSourceUri(Uri uri, Intent resultIntent) {
        if (uri == null) return;
        String uriText = uri.toString();
        for (int i = 0; i < documents.size(); i++) {
            if (uriText.equals(documents.get(i).uriString)) {
                selectTab(i);
                setStatus(t("File is already open", "Файл уже открыт"), Color.rgb(90, 90, 90));
                return;
            }
        }

        try {
            takePersistablePermission(uri, resultIntent);
            String text = readSource(uri);
            String name = displayName(uri);
            if (name == null || name.trim().isEmpty()) name = t("Opened file.asm", "Открытый файл.asm");
            addSourceTab(name, text, uriText, false);
            setStatus(t("Opened · ", "Открыт · ") + name, Color.rgb(0, 110, 45));
        } catch (Exception ex) {
            String message = t("Open error · ", "Ошибка открытия · ") + safeMessage(ex);
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private String readSource(Uri uri) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new IllegalStateException(t("cannot open input file",
                "не удалось открыть входной файл"));
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) out.append(buffer, 0, count);
        }
        return out.toString();
    }

    private void writeSource(Uri uri, String text) throws Exception {
        OutputStream out = getContentResolver().openOutputStream(uri, "wt");
        if (out == null) throw new IllegalStateException(t("cannot open output file",
                "не удалось открыть выходной файл"));
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write(text == null ? "" : text);
            writer.flush();
        }
    }

    private void takePersistablePermission(Uri uri, Intent data) {
        if (data == null) return;
        int flags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (flags == 0) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // Some providers grant access only for the current session.
        }
    }

    private String displayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return uri.getLastPathSegment();
    }

    private String ensureAsmName(String title) {
        if (title == null || title.trim().isEmpty()) return "program.asm";
        String clean = title.trim();
        return clean.toLowerCase(Locale.ROOT).endsWith(".asm") ? clean : clean + ".asm";
    }

    private void toggleLanguage() {
        captureCurrentDocument();
        AppLanguage next = language == AppLanguage.RU ? AppLanguage.EN : AppLanguage.RU;

        // Generated untitled names follow the UI language too. Real file/example titles stay intact.
        for (EditorDocument doc : documents) {
            if (doc.hasFile()) continue;
            int number = untitledNumber(doc.title);
            if (number > 0) doc.title = untitledName(number, next);
        }

        LanguageSettings.set(this, next);
        recreate();
    }

    private void showEmulatorDialog() {
        PackageManager pm = getPackageManager();
        Intent probe = createTapViewIntent(tapUri());
        List<ResolveInfo> found = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY);

        Map<String, ResolveInfo> unique = new LinkedHashMap<>();
        for (ResolveInfo info : found) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                unique.put(info.activityInfo.packageName, info);
            }
        }

        List<ResolveInfo> apps = new ArrayList<>(unique.values());
        Collections.sort(apps, (a, b) -> appLabel(pm, a).compareToIgnoreCase(appLabel(pm, b)));

        String[] labels = new String[apps.size() + 1];
        labels[0] = t("Ask every time", "Спрашивать каждый раз");
        int checked = 0;
        String selectedPackage = EmulatorSettings.getPackage(this);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            labels[i + 1] = appLabel(pm, info);
            if (selectedPackage != null
                    && selectedPackage.equals(info.activityInfo.packageName)) {
                checked = i + 1;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(t("Open TAP automatically in", "Автоматически открывать TAP в"))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (which == 0) {
                        EmulatorSettings.clear(this);
                        setStatus(t("Run will ask which app to use",
                                "При запуске будет предлагаться выбор приложения"),
                                Color.rgb(90, 90, 90));
                    } else {
                        ResolveInfo info = apps.get(which - 1);
                        String label = appLabel(pm, info);
                        EmulatorSettings.set(this, info.activityInfo.packageName, label);
                        setStatus(t("Default TAP app: ", "Приложение для TAP: ") + label,
                                Color.rgb(45, 80, 150));
                    }
                    updateEmulatorButton();
                    dialog.dismiss();
                })
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private String appLabel(PackageManager pm, ResolveInfo info) {
        CharSequence label = info.loadLabel(pm);
        if (label != null && label.length() > 0) return label.toString();
        return info.activityInfo.packageName;
    }

    private void updateEmulatorButton() {
        if (emulatorButton == null) return;
        String label = EmulatorSettings.getLabel(this);
        if (label == null || label.trim().isEmpty()) {
            emulatorButton.setText(t("App: ask", "Прил.: выбор"));
        } else {
            String shortLabel = label.length() > 15 ? label.substring(0, 14) + "…" : label;
            emulatorButton.setText(t("App: ", "Прил.: ") + shortLabel);
        }
    }

    private void openReference() {
        Intent intent = new Intent(this, ReferenceActivity.class);
        String word = wordAtCursor();
        if (!word.isEmpty()) intent.putExtra(ReferenceActivity.EXTRA_QUERY, word);
        startActivity(intent);
    }

    private void showExamples() {
        startActivityForResult(new Intent(this, ExampleCatalogActivity.class), REQUEST_EXAMPLE);
    }

    private String wordAtCursor() {
        int cursor = Math.max(0, editor.getSelectionStart());
        String text = editor.getText().toString();
        if (text.isEmpty()) return "";
        cursor = Math.min(cursor, text.length());
        int start = cursor;
        int end = cursor;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        while (end < text.length() && isWordChar(text.charAt(end))) end++;
        return start < end ? text.substring(start, end) : "";
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '\'';
    }

    private boolean buildSource() {
        hideKeyboard();
        captureCurrentDocument();
        if (!hasActiveDocument()) return false;
        EditorDocument doc = currentDocument();
        setStatus(t("Building…", "Сборка…"), Color.DKGRAY);
        runTap.setEnabled(false);
        saveTap.setEnabled(false);
        doc.invalidateBuild();

        try {
            AssemblyResult result = new Assembler().assemble(doc.text);
            doc.lastTap = new TapWriter().programTap(
                    tapProgramName(doc.title), result.getOrigin(), result.getBytes());
            doc.lastBuiltSource = doc.text;
            liveAnalysis = result;
            analyzedSource = doc.text;
            editor.setInstructionMetrics(result.getLineSizes(),
                    result.getLineMinCyclesArray(), result.getLineMaxCyclesArray(),
                    result.getInstructionFlags());
            updateStatistics();
            runTap.setEnabled(true);
            saveTap.setEnabled(true);

            String message = language == AppLanguage.RU
                    ? String.format(Locale.US,
                    "Сборка успешна · %d байт · ORG $%04X · autorun TAP готов",
                    result.getBytes().length, result.getOrigin() & 0xFFFF)
                    : String.format(Locale.US,
                    "Build OK · %d bytes · ORG $%04X · autorun TAP ready",
                    result.getBytes().length, result.getOrigin() & 0xFFFF);
            setStatus(message, Color.rgb(0, 110, 45));
            Toast.makeText(this, t("Build OK", "Сборка успешна"), Toast.LENGTH_SHORT).show();
            return true;
        } catch (RuntimeException ex) {
            String raw = ex.getMessage();
            String detail = Texts.localizeAssemblerError(language, raw);
            String message = t("Build error · ", "Ошибка сборки · ") + detail;
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            jumpToErrorLine(raw);
            return false;
        }
    }

    private String tapProgramName(String title) {
        String name = title == null ? "PROGRAM" : title;
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        name = name.replaceAll("[^A-Za-z0-9_-]", "_");
        if (name.isEmpty()) name = "PROGRAM";
        return name.length() > 10 ? name.substring(0, 10) : name;
    }

    private Uri tapUri() {
        return Uri.parse("content://" + getPackageName() + ".tap/program.tap");
    }

    private Intent createTapViewIntent(Uri uri) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/octet-stream");
        view.setClipData(ClipData.newRawUri("ZX Spectrum TAP", uri));
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return view;
    }

    private void runTapInEmulator() {
        captureCurrentDocument();
        if (!hasActiveDocument()) return;
        EditorDocument doc = currentDocument();
        if (!doc.buildIsCurrent()) {
            if (!buildSource()) return;
            doc = currentDocument();
        }

        try {
            File dir = new File(getCacheDir(), "shared");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException(t("cannot create cache directory",
                        "не удалось создать временную папку"));
            }

            File file = new File(dir, "program.tap");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(doc.lastTap);
            }

            Uri uri = tapUri();
            Intent view = createTapViewIntent(uri);
            String preferredPackage = EmulatorSettings.getPackage(this);
            String preferredLabel = EmulatorSettings.getLabel(this);

            if (preferredPackage != null) {
                view.setPackage(preferredPackage);
                if (view.resolveActivity(getPackageManager()) != null) {
                    setStatus(t("Opening TAP in ", "Открытие TAP в ")
                                    + (preferredLabel == null ? preferredPackage : preferredLabel),
                            Color.rgb(45, 80, 150));
                    startActivity(view);
                    return;
                }

                EmulatorSettings.clear(this);
                updateEmulatorButton();
                Toast.makeText(this,
                        t("Saved TAP app is unavailable; choose another app",
                                "Сохранённое приложение для TAP недоступно; выберите другое"),
                        Toast.LENGTH_LONG).show();
                view.setPackage(null);
            }

            setStatus(t("Opening TAP · choose your ZX Spectrum emulator",
                    "Открытие TAP · выберите эмулятор ZX Spectrum"), Color.rgb(45, 80, 150));
            startActivity(Intent.createChooser(view, t("Open TAP with", "Открыть TAP в")));
        } catch (ActivityNotFoundException ex) {
            setStatus(t("No app found for .tap files · install/configure a ZX Spectrum emulator",
                    "Не найдено приложение для .tap · установите или настройте эмулятор ZX Spectrum"),
                    Color.rgb(180, 30, 30));
            Toast.makeText(this, t("No app can open TAP files",
                    "Нет приложения, которое может открыть TAP"), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            String message = t("Run error · ", "Ошибка запуска · ") + safeMessage(ex);
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void saveTapFile() {
        captureCurrentDocument();
        if (!hasActiveDocument()) return;
        EditorDocument doc = currentDocument();
        if (!doc.buildIsCurrent()) {
            if (!buildSource()) return;
            doc = currentDocument();
        }

        pendingTapToSave = doc.lastTap;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, tapProgramName(doc.title).toLowerCase(Locale.ROOT) + ".tap");
        startActivityForResult(intent, REQUEST_SAVE_TAP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EXAMPLE) {
            if (resultCode == RESULT_OK && data != null) {
                String source = data.getStringExtra(ExampleCatalogActivity.EXTRA_SOURCE);
                String title = data.getStringExtra(ExampleCatalogActivity.EXTRA_TITLE);
                if (source != null) {
                    String displayTitle = title == null ? t("Example", "Пример") : title;
                    addSourceTab(ensureAsmName(displayTitle), source, null, false);
                    setStatus(language == AppLanguage.RU
                                    ? "Пример «" + displayTitle + "» открыт в новой вкладке"
                                    : displayTitle + " opened in a new tab",
                            Color.rgb(45, 80, 150));
                }
            }
            return;
        }

        if (requestCode == REQUEST_OPEN_SOURCE) {
            if (resultCode == RESULT_OK && data != null) openSourceUri(data.getData(), data);
            return;
        }

        if (requestCode == REQUEST_SAVE_SOURCE_AS) {
            int index = pendingSaveIndex;
            boolean closeAfter = pendingCloseAfterSave;
            pendingSaveIndex = -1;
            pendingCloseAfterSave = false;

            if (resultCode != RESULT_OK || data == null || index < 0 || index >= documents.size()) {
                return;
            }
            Uri uri = data.getData();
            if (uri == null) return;

            try {
                takePersistablePermission(uri, data);
                EditorDocument doc = documents.get(index);
                writeSource(uri, doc.text);
                doc.uriString = uri.toString();
                String name = displayName(uri);
                if (name != null && !name.trim().isEmpty()) doc.title = name;
                doc.dirty = false;
                renderTabs();
                setStatus(t("Saved · ", "Сохранено · ") + doc.title, Color.rgb(0, 110, 45));
                Toast.makeText(this, t("Source saved", "Исходник сохранён"), Toast.LENGTH_SHORT).show();
                if (closeAfter) closeTabImmediately(index);
            } catch (Exception ex) {
                String message = t("Save error · ", "Ошибка сохранения · ") + safeMessage(ex);
                setStatus(message, Color.rgb(180, 30, 30));
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode != REQUEST_SAVE_TAP || resultCode != RESULT_OK
                || data == null || pendingTapToSave == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) return;
        byte[] bytes = pendingTapToSave;
        pendingTapToSave = null;

        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException(t("cannot open output file",
                    "не удалось открыть выходной файл"));
            stream.write(bytes);
            stream.flush();
            setStatus(t("TAP saved · ", "TAP сохранён · ") + uri.getLastPathSegment(),
                    Color.rgb(0, 110, 45));
            Toast.makeText(this, t("TAP saved", "TAP сохранён"), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            String message = t("Save error · ", "Ошибка сохранения · ") + safeMessage(ex);
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void setStatus(String message, int color) {
        status.setText(message);
        status.setTextColor(color);
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
    }

    private void jumpToErrorLine(String message) {
        if (message == null || !message.startsWith("line ")) return;
        int colon = message.indexOf(':');
        if (colon < 6) return;
        try {
            int line = Integer.parseInt(message.substring(5, colon).trim());
            String text = editor.getText().toString();
            int position = 0;
            for (int i = 1; i < line && position < text.length(); i++) {
                int next = text.indexOf('\n', position);
                if (next < 0) break;
                position = next + 1;
            }
            editor.requestFocus();
            editor.setSelection(Math.min(position, editor.length()));
        } catch (NumberFormatException ignored) {
        }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty()
                ? ex.getClass().getSimpleName() : message;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
