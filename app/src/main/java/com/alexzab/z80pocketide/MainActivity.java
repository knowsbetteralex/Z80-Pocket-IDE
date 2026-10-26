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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import android.util.Log;
import android.provider.OpenableColumns;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyDiagnostics;
import com.alexzab.z80pocketide.assembler.AssemblyResult;
import com.alexzab.z80pocketide.editor.CodeEditorView;
import com.alexzab.z80pocketide.editor.SourceScrollRail;
import com.alexzab.z80pocketide.editor.SourceSearch;
import com.alexzab.z80pocketide.editor.SearchReplacePanel;
import com.alexzab.z80pocketide.editor.EditorDocument;
import com.alexzab.z80pocketide.editor.EditorPreferences;
import com.alexzab.z80pocketide.editor.EditorSettingsDialog;
import com.alexzab.z80pocketide.editor.UndoHistory;
import com.alexzab.z80pocketide.editor.WorkspaceStore;
import com.alexzab.z80pocketide.tools.SpriteSession;
import com.alexzab.z80pocketide.editor.SyntaxHighlighter;
import com.alexzab.z80pocketide.emulator.EmulatorSettings;
import com.alexzab.z80pocketide.examples.ExamplePrograms;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.source.SourceFileNames;
import com.alexzab.z80pocketide.source.ExternalSourceIntent;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;
import com.alexzab.z80pocketide.zx.TapWriter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE_TAP = 1001;
    private static final int REQUEST_EXAMPLE = 1002;
    private static final int REQUEST_OPEN_SOURCE = 1003;
    private static final int REQUEST_SAVE_SOURCE_AS = 1004;
    private static final int REQUEST_TOOLS = 1005;
    private static final int REQUEST_RESELECT_WRITABLE_SOURCE = 1006;

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
    private static final String STATE_PENDING_WRITE_GRANT = "pending_write_grant";
    private static final String STATE_PENDING_GRANT_CLOSE = "pending_grant_close";

    private final List<EditorDocument> documents = new ArrayList<>();
    private static final String WORKSPACE_FILE = "workspace-session.bin";
    private static final long WORKSPACE_IDLE_MS = 650L;
    private final Handler workspaceHandler = new Handler(Looper.getMainLooper());
    private final Object workspaceWriteLock = new Object();
    private final ExecutorService workspaceWriter =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "z80-workspace-autosave");
                thread.setDaemon(true);
                return thread;
            });
    private final AtomicLong workspaceRevision = new AtomicLong();
    private final Runnable delayedWorkspaceSave = this::saveWorkspaceAsync;
    private AtomicFile workspaceFile;

    private CodeEditorView editor;
    private TextView status;
    private TextView errorJumpLink;
    private TextView statistics;
    private Button clearLinesButton;
    private Button mainMenuButton;
    private AssemblyResult liveAnalysis;
    private String analyzedSource;
    private String analysisError;
    private static final Pattern ERROR_LINE = Pattern.compile("^line (\\d+):");
    private static final long ANALYSIS_IDLE_MS = 720;
    private final Handler analyzeHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "z80-source-analysis");
        t.setDaemon(true);
        return t;
    });
    private Future<?> runningAnalysis;
    private volatile int analysisRevision;
    private int resultRevision = -1;
    private final Runnable delayedAnalyze = this::performLiveAnalysis;
    private ImageButton runTap;
    private Button undoButton;
    private Button redoButton;
    private Button saveTap;
    private Button emulatorButton;
    private Button tabKeyButton;
    private LinearLayout statusDetails;
    private TextView statusExpandIcon;
    private TextView diagnosticsView;
    private ScrollView diagnosticScroll;
    private boolean detailsExpanded;
    private String statusMessage = "";
    private int statusColor = Color.DKGRAY;
    private List<AssemblyDiagnostics.Issue> analysisIssues = new ArrayList<>();
    private HorizontalScrollView tabStrip;
    private SourceScrollRail sourceScrollRail;
    private ImageButton foldAllButton;
    private SearchReplacePanel searchPanel;
    private final List<SourceSearch.Match> searchMatches = new ArrayList<>();
    private int searchMatchIndex = -1;
    private final Runnable delayedSearch = this::refreshSearchMatches;
    private LinearLayout tabRow;
    private AppLanguage language;

    private int activeIndex = -1;
    private int pendingSaveIndex = -1;
    private boolean pendingCloseAfterSave;
    private int pendingWriteGrantIndex = -1;
    private boolean pendingGrantCloseAfterSave;
    private boolean loadingDocument;
    private byte[] pendingTapToSave;
    private int untitledCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);
        workspaceFile = new AtomicFile(new File(getNoBackupFilesDir(), WORKSPACE_FILE));

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
                int barBottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                int imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom;
                int bottom = Math.max(barBottom, imeBottom);
                v.setPadding(side, vertical + top, side, vertical + bottom);
                if (editor != null) {
                    editor.setImeVisible(insets.isVisible(WindowInsets.Type.ime()));
                    editor.post(editor::ensureCaretVisible);
                }
                return insets;
            });
        } else {
            root.setPadding(side, vertical, side, vertical);
            // adjustResize on API 23-29 shrinks the visible window directly.
            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (editor == null) return;
                Rect frame = new Rect();
                root.getWindowVisibleDisplayFrame(frame);
                boolean keyboard = root.getRootView().getHeight() - frame.bottom > dp(140);
                editor.setImeVisible(keyboard);
                if (keyboard) editor.post(editor::ensureCaretVisible);
            });
        }

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(4), 0, dp(4), dp(4));

        // Quick actions scroll on narrow phones; the main menu stays pinned at right.
        HorizontalScrollView quickScroll = new HorizontalScrollView(this);
        quickScroll.setHorizontalScrollBarEnabled(false);
        quickScroll.setFillViewport(false);
        LinearLayout quickActions = new LinearLayout(this);
        quickActions.setOrientation(LinearLayout.HORIZONTAL);
        quickActions.setGravity(Gravity.CENTER_VERTICAL);
        quickScroll.addView(quickActions);
        topBar.addView(quickScroll, new LinearLayout.LayoutParams(0, dp(43), 1f));

        mainMenuButton = headerButton("☰", t("Menu", "Меню"), false);
        ImageButton saveSourceButton = new ImageButton(this);
        saveSourceButton.setContentDescription(t("Save source", "Сохранить исходник"));
        saveSourceButton.setImageResource(R.drawable.ic_header_save);
        saveSourceButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        saveSourceButton.setPadding(dp(7), dp(7), dp(7), dp(7));
        saveSourceButton.setBackground(UiStyle.ripple(this,
                0xFFE3F1E8, 14, 0xFFB8D7C5));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(34), dp(42));
        saveParams.setMargins(dp(1), 0, dp(1), 0);
        saveSourceButton.setLayoutParams(saveParams);
        ImageButton openSourceButton = new ImageButton(this);
        openSourceButton.setContentDescription(t("Open .asm", "Открыть .asm"));
        openSourceButton.setImageResource(R.drawable.ic_header_open);
        openSourceButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        openSourceButton.setPadding(dp(7), dp(7), dp(7), dp(7));
        openSourceButton.setBackground(UiStyle.ripple(this, 0xFFE3F1E8, 14, 0xFFB8D7C5));
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(dp(34), dp(42));
        openParams.setMargins(dp(1), 0, dp(1), 0);
        openSourceButton.setLayoutParams(openParams);
        undoButton = headerButton("↶", t("Undo", "Отменить"), false);
        redoButton = headerButton("↷", t("Redo", "Повторить"), false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        ImageButton findButton = new ImageButton(this);
        findButton.setContentDescription(t("Find / Replace", "Найти / Заменить"));
        findButton.setImageResource(R.drawable.ic_header_search);
        findButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        findButton.setPadding(dp(7), dp(7), dp(7), dp(7));
        findButton.setBackground(UiStyle.ripple(this,
                0xFFE3F1E8, 14, 0xFFB8D7C5));
        LinearLayout.LayoutParams findButtonParams =
                new LinearLayout.LayoutParams(dp(34), dp(42));
        findButtonParams.setMargins(dp(1), 0, dp(1), 0);
        findButton.setLayoutParams(findButtonParams);
        Button referenceButton = headerButton("?", t("Reference", "Справочник"), false);
        ImageButton converterButton = new ImageButton(this);
        converterButton.setContentDescription(t("Tools", "Инструменты"));
        converterButton.setImageResource(R.drawable.ic_header_tools);
        converterButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        converterButton.setPadding(dp(7), dp(7), dp(7), dp(7));
        converterButton.setBackground(UiStyle.ripple(this, 0xFFE3F1E8, 14, 0xFFB8D7C5));
        LinearLayout.LayoutParams workshopParams = new LinearLayout.LayoutParams(dp(34), dp(42));
        workshopParams.setMargins(dp(1), 0, dp(1), 0);
        converterButton.setLayoutParams(workshopParams);
        tabKeyButton = headerButton("⇥", t("Insert TAB", "Вставить TAB"), false);
        Button languageButton = headerButton(language == AppLanguage.RU ? "RU" : "EN",
                t("Switch language", "Переключить язык"), false);
        quickActions.addView(openSourceButton);
        quickActions.addView(saveSourceButton);
        quickActions.addView(undoButton);
        quickActions.addView(redoButton);
        quickActions.addView(findButton);
        quickActions.addView(referenceButton);
        quickActions.addView(converterButton);
        quickActions.addView(tabKeyButton);
        quickActions.addView(languageButton);
        topBar.addView(mainMenuButton);
        root.addView(topBar);

        referenceButton.setOnClickListener(v -> openReference());
        converterButton.setOnClickListener(v -> openTools());
        languageButton.setOnClickListener(v -> toggleLanguage());
        tabKeyButton.setOnClickListener(v -> editor.insertTabFromSettings());
        mainMenuButton.setOnClickListener(v -> showMainMenu());
        saveSourceButton.setOnClickListener(v -> saveDocument(activeIndex, false));
        openSourceButton.setOnClickListener(v -> openSourceFilePicker());
        undoButton.setOnClickListener(v -> undoOrRedo(true));
        redoButton.setOnClickListener(v -> undoOrRedo(false));
        findButton.setOnClickListener(v -> {
            if (searchPanel != null && searchPanel.getVisibility() == View.VISIBLE)
                hideSearchPanel();
            else showSearchPanel();
        });

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        LinearLayout workspaceBar = new LinearLayout(this);
        workspaceBar.setOrientation(LinearLayout.HORIZONTAL);
        workspaceBar.setGravity(Gravity.CENTER_VERTICAL);
        workspaceBar.setPadding(dp(4), dp(4), dp(4), dp(4));
        workspaceBar.setBackground(UiStyle.rounded(this,
                Color.rgb(235, 243, 238), 18, Color.rgb(205, 222, 212), 1));

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

        foldAllButton = new ImageButton(this);
        foldAllButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        foldAllButton.setPadding(dp(6), dp(6), dp(6), dp(6));
        foldAllButton.setBackground(UiStyle.ripple(this,
                0xFFE3F1E8, 12, 0xFFB8D7C5));
        LinearLayout.LayoutParams foldParams = new LinearLayout.LayoutParams(
                dp(40), dp(40));
        foldParams.setMargins(dp(3), 0, dp(1), 0);
        workspaceBar.addView(foldAllButton, foldParams);
        foldAllButton.setOnClickListener(v -> {
            if (editor == null) return;
            boolean changed;
            if (editor.isFolded()) {
                editor.unfoldAll();
                changed = true;
            } else {
                changed = editor.foldTopLevelLabels();
            }
            if (changed) {
                sourceScrollRail.onViewportChanged();
                scheduleAnalysis();
                scheduleSearchRefresh();
            }
            updateFoldAllButton();
        });
        LinearLayout.LayoutParams workspaceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        workspaceParams.setMargins(0, dp(5), 0, dp(4));
        root.addView(workspaceBar, workspaceParams);

        searchPanel = new SearchReplacePanel(this, language == AppLanguage.RU,
                new SearchReplacePanel.Actions() {
                    @Override public void onQueryChanged() { scheduleSearchRefresh(); }
                    @Override public void onNext() { jumpSearch(true); }
                    @Override public void onPrevious() { jumpSearch(false); }
                    @Override public void onReplace() { replaceSearchMatch(); }
                    @Override public void onReplaceAll() { replaceAllSearchMatches(); }
                    @Override public void onClose() { hideSearchPanel(); }
                });
        searchPanel.setVisibility(View.GONE);
        LinearLayout.LayoutParams findParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        findParams.setMargins(0, 0, 0, dp(3));
        root.addView(searchPanel, findParams);

        editor = new CodeEditorView(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextColor(Color.rgb(25, 25, 25));
        editor.setBackground(UiStyle.rounded(this,
                Color.rgb(252, 253, 252), 20, Color.rgb(207, 222, 213), 1));
        // Keep normal editor padding: the Run button overlays only its own
        // circular bounds, without a full-width empty strip below the code.
        editor.setPadding(dp(54), dp(12), dp(14), dp(12));
        editor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(true);
        // A dedicated 42dp thumb is easier to grab than Android's thin system
        // scroll indicator. Avoid duplicate bars on the right.
        editor.setVerticalScrollBarEnabled(false);

        FrameLayout editorHost = new FrameLayout(this);
        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        editorParams.setMargins(0, 0, 0, dp(5));
        root.addView(editorHost, editorParams);
        editorHost.addView(editor, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        sourceScrollRail = new SourceScrollRail(this, editor);
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(
                dp(42), FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.TOP | Gravity.RIGHT);
        // Run FAB occupies bottom 71dp and must never overlap this control.
        railParams.setMargins(0, dp(6), dp(5), dp(87));
        editorHost.addView(sourceScrollRail, railParams);
        editor.setOnViewportScrolled(sourceScrollRail::onViewportChanged);
        editor.setOnUserFingerScroll(sourceScrollRail::onUserScroll);
        editor.setOnFoldStateChanged(this::updateFoldAllButton);
        updateFoldAllButton();

        // Build + Run sits over the code canvas instead of consuming header width.
        runTap = new ImageButton(this);
        runTap.setContentDescription(t("Build and run TAP", "Собрать и запустить TAP"));
        runTap.setImageResource(R.drawable.ic_run_play);
        runTap.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        runTap.setPadding(dp(16), dp(16), dp(13), dp(16));
        GradientDrawable runCircle = new GradientDrawable();
        runCircle.setShape(GradientDrawable.OVAL);
        runCircle.setColor(0xFF2D8556);
        runCircle.setStroke(dp(2), 0xFFD8F0DD);
        runTap.setBackground(runCircle);
        runTap.setElevation(dp(5));
        FrameLayout.LayoutParams runParams = new FrameLayout.LayoutParams(
                dp(58), dp(58), Gravity.BOTTOM | Gravity.RIGHT);
        runParams.setMargins(0, 0, dp(13), dp(13));
        editorHost.addView(runTap, runParams);
        runTap.setOnClickListener(v -> runTapInEmulator());

        SyntaxHighlighter.attach(editor);

        editor.setOnLineSelectionChanged(this::updateStatistics);
        editor.setOnCaretMoved(this::updateStatistics);
        refreshEditorTools();

        // One compact status bar, expandable for diagnostics and byte/T totals.
        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(7), dp(3), dp(7), dp(3));
        bottomPanel.setBackground(UiStyle.rounded(this,
                Color.rgb(229, 241, 234), 13, Color.rgb(192, 216, 201), 1));

        LinearLayout compactStatus = new LinearLayout(this);
        compactStatus.setOrientation(LinearLayout.HORIZONTAL);
        compactStatus.setGravity(Gravity.CENTER_VERTICAL);
        compactStatus.setMinimumHeight(dp(31));

        status = new TextView(this);
        status.setTextSize(12);
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(6), 0, dp(2), 0);
        errorJumpLink = new TextView(this);
        errorJumpLink.setTextSize(12);
        errorJumpLink.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        errorJumpLink.setGravity(Gravity.CENTER_VERTICAL);
        errorJumpLink.setPadding(dp(7), 0, dp(7), 0);
        errorJumpLink.setBackground(UiStyle.ripple(this,
                0xFFE0EAFB, 8, 0xFFB3C6EC));
        errorJumpLink.setVisibility(View.GONE);
        errorJumpLink.setOnClickListener(v -> {
            if (analysisIssues == null) return;
            for (AssemblyDiagnostics.Issue issue : analysisIssues) {
                int target = issue.line > 0 ? issue.line : diagnosticLine(issue.message);
                if (target > 0) {
                    jumpToErrorLine(target);
                    return;
                }
            }
        });
        compactStatus.addView(errorJumpLink, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(29)));
        compactStatus.addView(status, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        statusExpandIcon = new TextView(this);
        statusExpandIcon.setText("⌄");
        statusExpandIcon.setTextSize(19);
        statusExpandIcon.setTextColor(Color.rgb(36, 100, 65));
        statusExpandIcon.setGravity(Gravity.CENTER);
        compactStatus.addView(statusExpandIcon, new LinearLayout.LayoutParams(dp(38), dp(31)));
        bottomPanel.addView(compactStatus);

        statusDetails = new LinearLayout(this);
        statusDetails.setOrientation(LinearLayout.VERTICAL);
        statusDetails.setPadding(dp(8), dp(4), dp(8), dp(6));
        statusDetails.setVisibility(View.GONE);

        diagnosticScroll = new ScrollView(this);
        diagnosticsView = new TextView(this);
        diagnosticsView.setTextSize(12);
        diagnosticsView.setTextColor(Color.rgb(170, 48, 58));
        diagnosticsView.setTextIsSelectable(true);
        diagnosticsView.setPadding(0, dp(4), 0, dp(4));
        diagnosticScroll.addView(diagnosticsView);
        statusDetails.addView(diagnosticScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(94)));

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setGravity(Gravity.CENTER_VERTICAL);
        statistics = new TextView(this);
        statistics.setTextSize(12);
        statistics.setTextColor(Color.rgb(35, 105, 66));
        statsRow.addView(statistics, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        clearLinesButton = compactButton(t("Clear selection", "Снять выделение"));
        clearLinesButton.setVisibility(View.GONE);
        clearLinesButton.setOnClickListener(v -> editor.clearLineSelection());
        statsRow.addView(clearLinesButton);
        statusDetails.addView(statsRow);

        bottomPanel.addView(statusDetails);
        root.addView(bottomPanel);
        compactStatus.setOnClickListener(v -> setStatusDetailsExpanded(!detailsExpanded));

        // TAP saving remains under File; the floating Run button auto-builds.
        saveTap = new Button(this);
        saveTap.setEnabled(false);
        runTap.setEnabled(true);
        setStatus(t("Ready · tap for details", "Готово · нажмите для подробностей"),
                Color.rgb(64, 98, 77));

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (loadingDocument || !hasActiveDocument()
                        || editor.isPresentationChange()
                        || editor.isReplayingHistory()) return;
                EditorDocument doc = currentDocument();
                // Avoid copying/rechecking the entire source on every keystroke.
                // captureCurrentDocument() persists the actual buffer before
                // tab changes, saving, building and Activity state persistence.
                boolean wasDirty = doc.dirty;
                doc.dirty = !doc.history.isAtSavePoint();
                doc.invalidateBuild();
                saveTap.setEnabled(false);
                scheduleAnalysis();
                scheduleSearchRefresh();
                setStatus(t("Modified · source file not saved",
                        "Изменено · исходный файл не сохранён"), Color.rgb(120, 85, 0));
                if (wasDirty != doc.dirty) renderTabs();
                scheduleWorkspaceSave();
            }
        });

        setContentView(root);
        restoreWorkspace(savedInstanceState);
        // Only consume a launch document on first creation. Rotation restores
        // the workspace and should not reopen/switch to the original file.
        if (savedInstanceState == null) openExternalSourceIntent(getIntent());
        scheduleAnalysis();
        scheduleWorkspaceSave();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // MainActivity is singleTask: a second system "Open with" request
        // reuses existing source tabs, including their unsaved edits/Undo.
        openExternalSourceIntent(intent);
    }

    private void openExternalSourceIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = intent.getData();
        if (uri == null
                || !ExternalSourceIntent.accepts(intent.getAction(), uri.getScheme())) return;
        openSourceUri(uri, intent);
    }

    @Override
    protected void onDestroy() {
        ++analysisRevision;
        analyzeHandler.removeCallbacks(delayedAnalyze);
        analyzeHandler.removeCallbacks(delayedSearch);
        workspaceHandler.removeCallbacks(delayedWorkspaceSave);
        workspaceWriter.shutdown();
        if (runningAnalysis != null) runningAnalysis.cancel(true);
        analysisExecutor.shutdownNow();
        // Finishing the IDE ends the in-memory sprite editing session;
        // configuration changes and closing only ToolsActivity do not.
        if (isFinishing() && !isChangingConfigurations()) SpriteSession.clear();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // System may kill our process after onPause. Flush synchronously here;
        // normal typing saves are debounced and use the background writer.
        flushWorkspaceNow();
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
        outState.putInt(STATE_PENDING_WRITE_GRANT, pendingWriteGrantIndex);
        outState.putBoolean(STATE_PENDING_GRANT_CLOSE, pendingGrantCloseAfterSave);
    }

    @Override
    public void onBackPressed() {
        if (searchPanel != null && searchPanel.getVisibility() == View.VISIBLE) {
            hideSearchPanel();
            return;
        }
        if (!hasDocumentsNeedingSave()) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(t("Unsaved tabs", "Несохранённые вкладки"))
                .setMessage(t("Changes are not saved to ASM files, but local drafts and Undo history will be restored next time. Exit?",
                        "Изменения не записаны в файлы ASM, но локальные черновики и Undo восстановятся при следующем запуске. Выйти?"))
                .setPositiveButton(t("Exit", "Выйти"), (d, w) -> {
                    flushWorkspaceNow();
                    MainActivity.super.onBackPressed();
                })
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void restoreWorkspace(Bundle state) {
        if (state != null) {
            pendingWriteGrantIndex = state.getInt(STATE_PENDING_WRITE_GRANT, -1);
            pendingGrantCloseAfterSave = state.getBoolean(STATE_PENDING_GRANT_CLOSE, false);
        }
        documents.clear();
        if (restorePersistedWorkspace()) {
            updateUntitledCounter();
            renderTabs();
            loadActiveDocument();
            return;
        }
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

    /**
     * Snapshot runs on the UI thread so EditText / UndoHistory are not accessed
     * concurrently. The immutable source strings and history bytes are then
     * encoded and atomically written on a worker thread.
     */
    private WorkspaceStore.Session snapshotWorkspace() throws IOException {
        captureCurrentDocument();
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        if (documents.isEmpty()) throw new IOException("No open source tabs");
        session.activeIndex = Math.max(0, Math.min(activeIndex, documents.size() - 1));
        for (EditorDocument doc : documents) {
            WorkspaceStore.Tab tab = new WorkspaceStore.Tab();
            tab.title = doc.title;
            tab.source = doc.text;
            tab.uri = doc.uriString == null ? "" : doc.uriString;
            tab.dirty = doc.dirty;
            tab.cursor = doc.cursor;
            tab.scrollX = doc.scrollX;
            tab.scrollY = doc.scrollY;
            ByteArrayOutputStream undoBytes = new ByteArrayOutputStream();
            DataOutputStream historyStream = new DataOutputStream(undoBytes);
            doc.history.writeTo(historyStream);
            historyStream.flush();
            tab.history = undoBytes.toByteArray();
            session.tabs.add(tab);
        }
        return session;
    }

    private void scheduleWorkspaceSave() {
        if (workspaceFile == null || documents.isEmpty()) return;
        workspaceRevision.incrementAndGet();
        workspaceHandler.removeCallbacks(delayedWorkspaceSave);
        workspaceHandler.postDelayed(delayedWorkspaceSave, WORKSPACE_IDLE_MS);
    }

    private void saveWorkspaceAsync() {
        if (workspaceWriter.isShutdown()) return;
        final long revision = workspaceRevision.get();
        final WorkspaceStore.Session snapshot;
        try {
            snapshot = snapshotWorkspace();
        } catch (IOException exception) {
            Log.e("Z80Workspace", "Cannot snapshot editor state", exception);
            return;
        }
        workspaceWriter.execute(() -> saveWorkspaceSnapshot(snapshot, revision));
    }

    private void flushWorkspaceNow() {
        workspaceHandler.removeCallbacks(delayedWorkspaceSave);
        long revision = workspaceRevision.incrementAndGet();
        try {
            saveWorkspaceSnapshot(snapshotWorkspace(), revision);
        } catch (IOException exception) {
            Log.e("Z80Workspace", "Cannot flush workspace", exception);
        }
    }

    private void saveWorkspaceSnapshot(WorkspaceStore.Session session, long revision) {
        if (workspaceFile == null) return;
        try {
            byte[] bytes = WorkspaceStore.encode(session);
            synchronized (workspaceWriteLock) {
                // A delayed worker must never overwrite a newer onPause snapshot.
                if (revision != workspaceRevision.get()) return;
                FileOutputStream stream = null;
                try {
                    stream = workspaceFile.startWrite();
                    stream.write(bytes);
                    stream.flush();
                    stream.getFD().sync();
                    workspaceFile.finishWrite(stream);
                } catch (IOException failure) {
                    if (stream != null) workspaceFile.failWrite(stream);
                    throw failure;
                }
            }
        } catch (IOException exception) {
            Log.e("Z80Workspace", "Autosave failed; previous snapshot remains intact", exception);
        }
    }

    private boolean restorePersistedWorkspace() {
        if (workspaceFile == null) return false;
        // Android AtomicFile may have a recoverable .bak after interrupted write.
        File base = workspaceFile.getBaseFile();
        if (!base.exists() && !new File(base.getPath() + ".bak").exists()) return false;
        try (FileInputStream stream = workspaceFile.openRead();
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (bytes.size() + count > 48 * 1024 * 1024 + 8)
                    throw new IOException("Workspace file exceeds size limit");
                bytes.write(buffer, 0, count);
            }
            WorkspaceStore.Session session = WorkspaceStore.decode(bytes.toByteArray());
            List<EditorDocument> loaded = new ArrayList<>();
            for (WorkspaceStore.Tab tab : session.tabs) {
                EditorDocument doc = new EditorDocument(tab.title, tab.source,
                        tab.uri.isEmpty() ? null : tab.uri, tab.dirty);
                doc.cursor = Math.max(0, Math.min(tab.cursor, tab.source.length()));
                doc.scrollX = Math.max(0, tab.scrollX);
                doc.scrollY = Math.max(0, tab.scrollY);
                try (DataInputStream input =
                             new DataInputStream(new ByteArrayInputStream(tab.history))) {
                    doc.history.copyFrom(UndoHistory.readFrom(input));
                } catch (IOException historyFailure) {
                    // Recover actual source even if history was unreadable.
                    Log.w("Z80Workspace", "Undo state not recovered for " + doc.title,
                            historyFailure);
                }
                loaded.add(doc);
            }
            documents.clear();
            documents.addAll(loaded);
            activeIndex = session.activeIndex;
            pendingSaveIndex = -1;
            pendingCloseAfterSave = false;
            return true;
        } catch (IOException exception) {
            Log.e("Z80Workspace", "Cannot restore workspace; using Android state", exception);
            return false;
        }
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

    private Button headerButton(String glyph, String description, boolean primary) {
        Button button = new Button(this);
        button.setText(glyph);
        button.setContentDescription(description);
        button.setTextSize(glyph.length() > 2 ? 12 : 18);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(0, 0, 0, 0);
        if (primary) UiStyle.styleGreenButton(button);
        else UiStyle.styleSoftGreenButton(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(42));
        params.setMargins(dp(1), 0, dp(1), 0);
        button.setLayoutParams(params);
        return button;
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

    private void updateFoldAllButton() {
        if (foldAllButton == null || editor == null) return;
        boolean folded = editor.isFolded();
        foldAllButton.setImageResource(folded
                ? R.drawable.ic_unfold_all : R.drawable.ic_fold_all);
        foldAllButton.setContentDescription(folded
                ? t("Expand all label blocks", "Развернуть все блоки меток")
                : t("Collapse top-level labels", "Свернуть верхние метки"));
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
        analysisError = null;
        analyzeHandler.removeCallbacks(delayedAnalyze);
        editor.setSourceText(doc.text);
        editor.setUndoHistory(doc.history, this::updateUndoRedoButtons);
        editor.setSelection(Math.max(0, Math.min(doc.cursor, editor.length())));
        loadingDocument = false;
        updateFoldAllButton();
        editor.post(() -> {
            editor.scrollToVerticalOffset(doc.scrollY);
            editor.scrollTo(doc.scrollX, editor.getScrollY());
            sourceScrollRail.invalidate();
        });

        boolean built = doc.buildIsCurrent();
        // Run always builds stale or never-built sources; only Save TAP depends on Build.
        runTap.setEnabled(true);
        saveTap.setEnabled(built);
        scheduleAnalysis();
        scheduleSearchRefresh();
        scheduleWorkspaceSave();
        setStatus(doc.dirty
                        ? t("Modified · not saved", "Изменено · не сохранено")
                        : (doc.hasFile()
                            ? t("File · ", "Файл · ") + doc.title
                            : t("Tab · ", "Вкладка · ") + doc.title),
                doc.dirty ? Color.rgb(120, 85, 0) : Color.rgb(90, 90, 90));
    }

    private void updateUndoRedoButtons() {
        if (undoButton == null || redoButton == null) return;
        boolean active = hasActiveDocument();
        undoButton.setEnabled(active && currentDocument().history.canUndo());
        redoButton.setEnabled(active && currentDocument().history.canRedo());
    }

    private void undoOrRedo(boolean undo) {
        if (!hasActiveDocument() || editor == null) return;
        EditorDocument doc = currentDocument();
        boolean wasDirty = doc.dirty;
        boolean changed = undo ? editor.undoSourceEdit() : editor.redoSourceEdit();
        if (!changed) return;
        doc.dirty = !doc.history.isAtSavePoint();
        doc.invalidateBuild();
        saveTap.setEnabled(false);
        if (wasDirty != doc.dirty) renderTabs();
        scheduleAnalysis();
        updateUndoRedoButtons();
        scheduleWorkspaceSave();
        setStatus(undo ? t("Undo", "Изменение отменено")
                : t("Redo", "Изменение повторено"), Color.rgb(45, 100, 72));
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
        ++analysisRevision;
        resultRevision = -1;
        analyzeHandler.removeCallbacks(delayedAnalyze);
        if (runningAnalysis != null) runningAnalysis.cancel(true);
        liveAnalysis = null;
        analyzedSource = null;
        analysisError = null;
        analysisIssues.clear();
        if (editor != null) {
            editor.clearInstructionMetrics();
            SyntaxHighlighter.setErrorLine(editor, 0);
        }
        updateStatistics();
        analyzeHandler.postDelayed(delayedAnalyze, ANALYSIS_IDLE_MS);
    }

    /** Snapshot only after the user stops typing; execute all parsing off the UI thread. */
    private void performLiveAnalysis() {
        if (!hasActiveDocument() || editor == null || loadingDocument) return;
        final int revision = analysisRevision;
        final int tabIndex = activeIndex;
        final EditorDocument tab = currentDocument();
        final String source = editor.getSourceText();

        runningAnalysis = analysisExecutor.submit(() -> {
            if (Thread.currentThread().isInterrupted() || revision != analysisRevision) return;
            AssemblyResult result = null;
            String error = null;
            List<AssemblyDiagnostics.Issue> issues = new ArrayList<>();
            try {
                result = new Assembler().assemble(source);
            } catch (RuntimeException ex) {
                error = ex.getMessage() == null ? "Assembler error" : ex.getMessage();
                if (revision == analysisRevision && !Thread.currentThread().isInterrupted()) {
                    issues = AssemblyDiagnostics.collect(source, 8);
                    if (issues.isEmpty()) issues.add(new AssemblyDiagnostics.Issue(0, error));
                }
            }

            final AssemblyResult finished = result;
            final String finishedError = error;
            final List<AssemblyDiagnostics.Issue> found = issues;
            analyzeHandler.post(() -> {
                if (isFinishing() || isDestroyed()
                        || revision != analysisRevision
                        || !hasActiveDocument() || activeIndex != tabIndex
                        || currentDocument() != tab || loadingDocument) return;

                liveAnalysis = finished;
                resultRevision = revision;
                analyzedSource = finished == null ? null : source;
                analysisError = finishedError == null
                        ? null : Texts.localizeAssemblerError(language, finishedError);
                analysisIssues = found;
                SyntaxHighlighter.setErrorLine(editor,
                        finishedError == null || editor.isFolded()
                                ? 0 : diagnosticLine(finishedError));
                if (finished == null) {
                    editor.clearInstructionMetrics();
                } else {
                    editor.setInstructionMetrics(finished.getLineSizes(),
                            finished.getLineMinCyclesArray(), finished.getLineMaxCyclesArray(),
                            finished.getInstructionFlags());
                }
                updateStatistics();
            });
        });
    }

    private void updateStatistics() {
        if (statistics == null || editor == null) return;
        List<Integer> picked = editor.getSelectedLines();
        if (clearLinesButton != null)
            clearLinesButton.setVisibility(picked.isEmpty() ? View.GONE : View.VISIBLE);

        if (analysisError != null) {
            statistics.setText("⚠ " + analysisError);
            statistics.setTextColor(Color.rgb(183, 48, 58));
            refreshStatusPanel();
            return;
        }
        statistics.setTextColor(Color.rgb(35, 105, 66));
        if (liveAnalysis == null || resultRevision != analysisRevision) {
            statistics.setText(t("Size/T: — (check source)", "Байты/такты: — (проверьте код)"));
            refreshStatusPanel();
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
        if (editor.isFolded() && selection) {
            statistics.setText(t("Selection analysis unavailable while folded",
                    "Расчёт выбранных строк недоступен при свёрнутом коде"));
        } else {
            statistics.setText(label + "  ·  " + bytes + " B  ·  " + cycles + " T");
        }
        refreshStatusPanel();
    }

    private int diagnosticLine(String message) {
        if (message == null) return 0;
        Matcher matcher = ERROR_LINE.matcher(message);
        if (!matcher.find()) return 0;
        try { return Integer.parseInt(matcher.group(1)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    /** Opens find/replace below the tab strip without obscuring source code. */
    private void showSearchPanel() {
        if (searchPanel == null || editor == null) return;
        boolean opening = searchPanel.getVisibility() != View.VISIBLE;
        if (editor.isFolded()) editor.unfoldAll();
        if (opening) {
            int a = Math.max(0, editor.getSelectionStart());
            int b = Math.max(0, editor.getSelectionEnd());
            String selected = editor.getText().subSequence(
                    Math.min(a, b), Math.max(a, b)).toString();
            searchPanel.setVisibility(View.VISIBLE);
            if (!selected.isEmpty() && selected.length() <= 100
                    && selected.indexOf('\n') < 0) searchPanel.setQuery(selected);
        }
        refreshSearchMatches();
        searchPanel.focusQuery();
    }

    private void hideSearchPanel() {
        analyzeHandler.removeCallbacks(delayedSearch);
        if (searchPanel != null) searchPanel.setVisibility(View.GONE);
        searchMatches.clear();
        searchMatchIndex = -1;
        if (editor != null) editor.highlightSearchMatches(null, -1);
        editor.requestFocus();
        hideKeyboard();
    }

    private void scheduleSearchRefresh() {
        analyzeHandler.removeCallbacks(delayedSearch);
        if (searchPanel != null && searchPanel.getVisibility() == View.VISIBLE)
            analyzeHandler.postDelayed(delayedSearch, 160);
    }

    private void refreshSearchMatches() {
        if (searchPanel == null || searchPanel.getVisibility() != View.VISIBLE
                || editor == null) return;
        if (editor.isFolded()) editor.unfoldAll();
        searchMatches.clear();
        searchMatches.addAll(SourceSearch.findAll(editor.getText().toString(),
                searchPanel.getQuery(), searchPanel.isCaseSensitive()));
        int selectedStart = editor.getSelectionStart();
        int selectedEnd = editor.getSelectionEnd();
        searchMatchIndex = -1;
        for (int i = 0; i < searchMatches.size(); i++) {
            SourceSearch.Match match = searchMatches.get(i);
            if (match.start == selectedStart && match.end == selectedEnd) {
                searchMatchIndex = i;
                break;
            }
        }
        updateSearchHighlights();
    }

    private void updateSearchHighlights() {
        if (searchPanel == null || editor == null) return;
        editor.highlightSearchMatches(searchMatches, searchMatchIndex);
        searchPanel.updateCount(searchMatches.size(), searchMatchIndex);
    }

    private void jumpSearch(boolean next) {
        if (searchPanel == null || searchPanel.getVisibility() != View.VISIBLE) return;
        refreshSearchMatches();
        if (searchMatches.isEmpty()) {
            setStatus(t("No matches", "Совпадений нет"), Color.rgb(145, 76, 43));
            return;
        }
        int cursor = next ? editor.getSelectionEnd() : editor.getSelectionStart();
        int index = next ? SourceSearch.next(searchMatches, cursor)
                : SourceSearch.previous(searchMatches, cursor);
        focusSearchResult(index);
    }

    private void focusSearchResult(int index) {
        if (index < 0 || index >= searchMatches.size()) return;
        searchMatchIndex = index;
        SourceSearch.Match match = searchMatches.get(index);
        editor.revealSourceMatch(match.start, match.end);
        updateSearchHighlights();
    }

    private void replaceSearchMatch() {
        if (searchPanel == null || searchPanel.getVisibility() != View.VISIBLE) return;
        refreshSearchMatches();
        if (searchMatches.isEmpty()) return;
        int index = searchMatchIndex >= 0 ? searchMatchIndex
                : SourceSearch.next(searchMatches, editor.getSelectionStart());
        SourceSearch.Match match = searchMatches.get(index);
        String replacement = searchPanel.getReplacement();
        editor.replaceFromSearch(match.start, match.end, replacement);
        // Find the following occurrence after the inserted text, wrapping to start.
        refreshSearchMatches();
        if (!searchMatches.isEmpty()) {
            int following = SourceSearch.next(searchMatches,
                    match.start + replacement.length());
            focusSearchResult(following);
        }
        setStatus(t("Replaced one occurrence", "Заменено одно совпадение"),
                Color.rgb(37, 110, 72));
    }

    private void replaceAllSearchMatches() {
        if (searchPanel == null || searchPanel.getVisibility() != View.VISIBLE) return;
        refreshSearchMatches();
        int count = searchMatches.size();
        if (count == 0) {
            setStatus(t("No matches to replace", "Нет совпадений для замены"),
                    Color.rgb(145, 76, 43));
            return;
        }
        String source = editor.getText().toString();
        String changed = SourceSearch.replaceAll(source, searchMatches,
                searchPanel.getReplacement());
        if (!changed.equals(source))
            editor.replaceFromSearch(0, source.length(), changed);
        refreshSearchMatches();
        setStatus((language == AppLanguage.RU ? "Заменено: " : "Replaced: ") + count,
                Color.rgb(37, 110, 72));
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
        files.add(0, 20, 6, t("Build source", "Собрать"));
        files.add(0, 21, 7, t("Save .tap", "Сохранить .tap"));
        files.add(0, 22, 8, t("Build and run", "Собрать и запустить"));

        menu.add(0, 10, 10, t("Examples / Routines", "Примеры / Подпрограммы"));
        menu.add(0, 11, 11, t("Z80 / Spectrum reference", "Справочник Z80 / Spectrum"));

        Menu edit = menu.addSubMenu(t("Edit", "Правка"));
        edit.add(0, 23, 0, t("Find / Replace…", "Найти / Заменить…"));
        edit.add(0, 13, 1, t("Format document", "Форматировать код"));
        edit.add(0, 17, 2, t("UPPERCASE entire source", "ВЕСЬ КОД → ЗАГЛАВНЫМИ"));
        edit.add(0, 12, 3, t("Number / Address / Sprite tools", "Калькулятор / Адреса / Спрайты"));
        edit.add(0, 16, 4, t("Choose TAP app", "Выбрать эмулятор TAP"));

        menu.add(0, 15, 100, t("Editor settings", "Настройки редактора"));

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: newTab(); break;
                case 2: openSourceFilePicker(); break;
                case 3: saveDocument(activeIndex, false); break;
                case 4: requestSaveAs(activeIndex, false); break;
                case 5: requestCloseTab(activeIndex); break;
                case 20: buildSource(); break;
                case 21: saveTapFile(); break;
                case 22: runTapInEmulator(); break;
                case 10: showExamples(); break;
                case 11: openReference(); break;
                case 12: openTools(); break;
                case 23: showSearchPanel(); break;
                case 13:
                    editor.formatAllFromSettings();
                    setStatus(t("Document formatted", "Документ отформатирован"),
                            Color.rgb(45, 100, 72));
                    scheduleAnalysis();
                    break;
                case 15: showEditorSettings(); break;
                case 16: showEmulatorDialog(); break;
                case 17:
                    if (editor.uppercaseEntireDocument()) {
                        scheduleAnalysis();
                        setStatus(t("Uppercased all text (including strings/comments)",
                                        "Весь текст заглавными (включая строки и комментарии)"),
                                Color.rgb(45, 100, 72));
                    } else {
                        setStatus(t("Already uppercase", "Текст уже заглавными"),
                                Color.rgb(45, 100, 72));
                    }
                    break;
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
        // TAB is a permanent top-bar shortcut even if older preferences hid it.
        if (tabKeyButton != null) tabKeyButton.setVisibility(View.VISIBLE);
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
        // text/plain makes some document providers append a second .txt extension.
        intent.setType("text/x-asm");
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
            markSourceSaved(index, closeAfterSave);
        } catch (Exception ex) {
            // External ACTION_VIEW grants may be read-only. Request write
            // access to the EXISTING document instead of creating another file.
            if (doc.uriString != null) {
                setStatus(t("Write access needed · ", "Нужен доступ на запись · ")
                        + safeMessage(ex), Color.rgb(164, 92, 19));
                showWritableSourceRequest(index, closeAfterSave);
            }
        }
    }

    private void markSourceSaved(int index, boolean closeAfterSave) {
        if (index < 0 || index >= documents.size()) return;
        EditorDocument doc = documents.get(index);
        doc.history.markSaved();
        doc.dirty = false;
        scheduleWorkspaceSave();
        updateUndoRedoButtons();
        renderTabs();
        setStatus(t("Saved to original file · ", "Сохранено в исходный файл · ")
                + doc.title, Color.rgb(0, 110, 45));
        Toast.makeText(this, t("Source saved", "Исходник сохранён"), Toast.LENGTH_SHORT).show();
        if (closeAfterSave) closeTabImmediately(index);
    }

    private void showWritableSourceRequest(int index, boolean closeAfterSave) {
        if (index < 0 || index >= documents.size()) return;
        final EditorDocument doc = documents.get(index);
        new AlertDialog.Builder(this)
                .setTitle(t("Allow editing the original file",
                        "Разрешить запись в исходный файл"))
                .setMessage(t(
                        "The file manager opened \"" + doc.title + "\" without writable "
                                + "permission (or its provider rejected the write). "
                                + "Select this SAME existing file once in Android's "
                                + "document picker. Your changes will be saved there "
                                + "automatically, and future saves can use its writable URI.",
                        "Файловый менеджер открыл «" + doc.title + "» без прав на запись "
                                + "(или провайдер отклонил запись). Один раз выбери "
                                + "ЭТОТ ЖЕ существующий файл в системном окне Android. "
                                + "Изменения автоматически запишутся в него; следующие "
                                + "сохранения будут использовать полученный URI."))
                .setPositiveButton(t("Select original file", "Выбрать исходный файл"),
                        (dialog, which) -> requestWritableSource(index, closeAfterSave))
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void requestWritableSource(int index, boolean closeAfterSave) {
        pendingWriteGrantIndex = index;
        pendingGrantCloseAfterSave = closeAfterSave;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Unknown .asm MIME values must remain selectable in DocumentsUI.
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_RESELECT_WRITABLE_SOURCE);
        } catch (ActivityNotFoundException ex) {
            pendingWriteGrantIndex = -1;
            pendingGrantCloseAfterSave = false;
            setStatus(t("Android document picker unavailable",
                    "Системный выбор документов недоступен"), Color.rgb(180, 30, 30));
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
        if (flags == 0 || !"content".equalsIgnoreCase(uri.getScheme())) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // A provider may offer persistable read but temporary write. Try
            // to preserve whichever individual rights Android actually granted.
            for (int candidate : new int[]{
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION}) {
                if ((flags & candidate) == 0) continue;
                try {
                    getContentResolver().takePersistableUriPermission(uri, candidate);
                } catch (SecurityException alsoIgnored) {
                    // Some file-manager providers grant only session access.
                }
            }
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
        return SourceFileNames.suggestedName(title);
    }

    private Uri correctCreatedAsmUri(Uri uri) {
        String name = displayName(uri);
        if (!SourceFileNames.hasUnwantedTextSuffix(name)
                || !DocumentsContract.isDocumentUri(this, uri)) return uri;
        try {
            Uri renamed = DocumentsContract.renameDocument(
                    getContentResolver(), uri, SourceFileNames.suggestedName(name));
            return renamed == null ? uri : renamed;
        } catch (Exception ignored) {
            // Not all cloud/document providers allow renames. Keep the valid URI
            // rather than break source saving, and show the real name in status.
            return uri;
        }
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
        flushWorkspaceNow();
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

    private void openTools() {
        Intent intent = new Intent(this, ToolsActivity.class);
        if (editor != null) {
            int a = editor.getSelectionStart(), b = editor.getSelectionEnd();
            if (a >= 0 && b >= 0 && a != b) {
                String selected = editor.getText().subSequence(
                        Math.min(a, b), Math.max(a, b)).toString();
                // Clipboard also remains usable from the dialog.
                if (selected.length() <= 16384)
                    intent.putExtra("sprite_selected_source", selected);
            }
        }
        startActivityForResult(intent, REQUEST_TOOLS);
    }

    private void insertToolSource(String source) {
        if (source == null || source.isEmpty() || editor == null || !hasActiveDocument()) return;
        // An insertion must use the real expanded text, not the visual fold markers.
        if (editor.isFolded()) editor.unfoldAll();
        android.text.Editable text = editor.getText();
        int a = Math.max(0, Math.min(editor.getSelectionStart(), text.length()));
        int b = Math.max(0, Math.min(editor.getSelectionEnd(), text.length()));
        int start = Math.min(a, b), end = Math.max(a, b);

        String before = start > 0 && text.charAt(start - 1) != '\n' ? "\n" : "";
        String after = end < text.length() && text.charAt(end) != '\n' ? "\n" : "";
        String inserted = before + source + after;
        text.replace(start, end, inserted);
        editor.setSelection(Math.min(text.length(), start + before.length() + source.length()));
        editor.requestFocus();
        setStatus(t("DB data inserted at cursor", "DB вставлены в позицию курсора"),
                Color.rgb(35, 119, 68));
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
        saveTap.setEnabled(false);
        doc.invalidateBuild();

        try {
            AssemblyResult result = new Assembler().assemble(doc.text);
            doc.lastTap = new TapWriter().programTap(
                    tapProgramName(doc.title), result.getOrigin(), result.getBytes());
            doc.lastBuiltSource = doc.text;
            liveAnalysis = result;
            resultRevision = analysisRevision;
            analyzedSource = doc.text;
            analysisError = null;
            analysisIssues.clear();
            SyntaxHighlighter.setErrorLine(editor, 0);
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
            runTap.setEnabled(true);
            String raw = ex.getMessage();
            analysisError = Texts.localizeAssemblerError(language, raw);
            analysisIssues = AssemblyDiagnostics.collect(doc.text, 8);
            if (analysisIssues.isEmpty())
                analysisIssues.add(new AssemblyDiagnostics.Issue(0, raw == null ? "Error" : raw));
            SyntaxHighlighter.setErrorLine(editor, editor.isFolded() ? 0 : diagnosticLine(raw));
            updateStatistics();
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

        if (requestCode == REQUEST_TOOLS) {
            if (resultCode == RESULT_OK && data != null)
                insertToolSource(data.getStringExtra(ToolsActivity.EXTRA_INSERT_SOURCE));
            return;
        }

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

        if (requestCode == REQUEST_RESELECT_WRITABLE_SOURCE) {
            int index = pendingWriteGrantIndex;
            boolean closeAfter = pendingGrantCloseAfterSave;
            pendingWriteGrantIndex = -1;
            pendingGrantCloseAfterSave = false;
            if (resultCode != RESULT_OK || data == null
                    || index < 0 || index >= documents.size()) return;
            Uri selected = data.getData();
            if (selected == null) return;
            EditorDocument doc = documents.get(index);
            String selectedName = displayName(selected);
            if (!ExternalSourceIntent.matchingName(doc.title, selectedName)) {
                String msg = t("Different file selected. Select the existing \"",
                                "Выбран другой файл. Нужно выбрать исходный «")
                        + doc.title + "»";
                setStatus(msg, Color.rgb(180, 30, 30));
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                takePersistablePermission(selected, data);
                if (index == activeIndex) captureCurrentDocument();
                writeSource(selected, doc.text);
                // Store the new writable URI (often an Android DocumentsUI
                // URI replacing the read-only FileProvider URI).
                doc.uriString = selected.toString();
                markSourceSaved(index, closeAfter);
            } catch (Exception ex) {
                String msg = t("The selected provider cannot write this file · ",
                        "Провайдер не разрешает записать выбранный файл · ")
                        + safeMessage(ex);
                setStatus(msg, Color.rgb(180, 30, 30));
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
                // The system picker is allowed to alter names. Correct legacy
                // .asm.txt if its provider supports renameDocument.
                Uri actualUri = correctCreatedAsmUri(uri);
                if (!actualUri.equals(uri)) takePersistablePermission(actualUri, data);
                doc.uriString = actualUri.toString();
                String name = displayName(actualUri);
                if (name != null && !name.trim().isEmpty()) doc.title = name;
                doc.history.markSaved();
                doc.dirty = false;
                scheduleWorkspaceSave();
                updateUndoRedoButtons();
                renderTabs();
                boolean doubleExtension = SourceFileNames.hasUnwantedTextSuffix(name);
                setStatus((doubleExtension
                        ? t("Saved, but file provider kept .asm.txt · ", "Сохранено, но провайдер оставил .asm.txt · ")
                        : t("Saved · ", "Сохранено · ")) + doc.title,
                        doubleExtension ? Color.rgb(160, 100, 22) : Color.rgb(0, 110, 45));
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
        statusMessage = message;
        statusColor = color;
        refreshStatusPanel();
    }

    private void setStatusDetailsExpanded(boolean expanded) {
        detailsExpanded = expanded;
        if (statusDetails != null)
            statusDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (statusExpandIcon != null)
            statusExpandIcon.setText(expanded ? "⌃" : "⌄");
        refreshStatusPanel();
    }

    private void refreshStatusPanel() {
        if (status == null) return;
        int n = analysisIssues == null ? 0 : analysisIssues.size();
        if (n > 0) {
            int firstLine = 0;
            for (AssemblyDiagnostics.Issue issue : analysisIssues) {
                firstLine = issue.line > 0 ? issue.line : diagnosticLine(issue.message);
                if (firstLine > 0) break;
            }
            if (firstLine > 0 && errorJumpLink != null) {
                String label = "↗ " + t("line ", "стр. ") + firstLine;
                SpannableString link = new SpannableString(label);
                link.setSpan(new UnderlineSpan(), 0, label.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                link.setSpan(new ForegroundColorSpan(0xFF205BAE),
                        0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                errorJumpLink.setText(link);
                errorJumpLink.setContentDescription(t("Go to error on line ",
                        "Перейти к ошибке в строке ") + firstLine);
                errorJumpLink.setVisibility(View.VISIBLE);
            } else if (errorJumpLink != null) {
                errorJumpLink.setVisibility(View.GONE);
            }
            String brief = language == AppLanguage.RU ? " ошибок" : " errors";
            status.setText("⚠ " + n + brief + " · " +
                    Texts.localizeAssemblerError(language, analysisIssues.get(0).message));
            status.setTextColor(Color.rgb(174, 43, 53));
        } else {
            if (errorJumpLink != null) errorJumpLink.setVisibility(View.GONE);
            status.setText(statusMessage);
            status.setTextColor(statusColor);
        }
        if (diagnosticsView != null) {
            if (n > 0) {
                StringBuilder items = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    AssemblyDiagnostics.Issue issue = analysisIssues.get(i);
                    if (i > 0) items.append('\n');
                    items.append("• ")
                            .append(Texts.localizeAssemblerError(language, issue.message));
                }
                diagnosticsView.setText(items.toString());
                if (diagnosticScroll != null) diagnosticScroll.setVisibility(View.VISIBLE);
            } else {
                diagnosticsView.setText(t("No assembler errors", "Ошибок ассемблера нет"));
                if (diagnosticScroll != null) diagnosticScroll.setVisibility(View.GONE);
            }
        }
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
        jumpToErrorLine(diagnosticLine(message));
    }

    /** Error line numbers always refer to the full source, not fold markers. */
    private void jumpToErrorLine(int oneBasedLine) {
        if (editor == null || oneBasedLine < 1) return;
        if (editor.isFolded()) editor.unfoldAll();
        CharSequence source = editor.getText();
        int position = 0;
        for (int line = 1; line < oneBasedLine; line++) {
            int next = TextUtils.indexOf(source, '\n', position);
            if (next < 0) {
                position = source.length();
                break;
            }
            position = next + 1;
        }
        editor.clearLineSelection();
        editor.requestFocus();
        editor.setSelection(Math.min(position, editor.length()));
        SyntaxHighlighter.setErrorLine(editor, oneBasedLine);
        editor.post(editor::ensureCaretVisible);
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
