package com.alexzab.z80pocketide.git;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.ui.UiStyle;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** GitHub authorization, repository picker and one-button Pull + Push. */
public final class GitHubProjectActivity extends Activity {
    public static final String EXTRA_SYNC_ONLY = "git_sync_only";
    public static final String EXTRA_REPO = "git_repo";
    public static final String EXTRA_BRANCH = "git_branch";
    public static final String EXTRA_ROOT = "git_root";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "z80-github-sync");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String,GitSyncEngine.Resolution> answers = new LinkedHashMap<>();
    private boolean ru, syncOnly, busy;
    private String repo, branch, token;
    private LinearLayout body, controls;
    private TextView message, current;
    private ProgressBar spinner;
    private Button syncButton, chooseButton, signOutButton;
    private List<GitHubApi.Repo> repositories = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ru = LanguageSettings.get(this) == AppLanguage.RU;
        syncOnly = getIntent().getBooleanExtra(EXTRA_SYNC_ONLY, false);
        repo = syncOnly ? GitProjectState.repo(this) : null;
        branch = syncOnly ? GitProjectState.branch(this) : null;
        token = GitHubAuth.load(this);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFFF4F8F6);
        page.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            page.setOnApplyWindowInsetsListener((v, insets) -> {
                int top = insets.getInsets(WindowInsets.Type.systemBars()).top;
                int bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                v.setPadding(dp(14), top + dp(12), dp(14), bottom + dp(12));
                return insets;
            });
        }

        TextView heading = new TextView(this);
        heading.setText(t("GitHub projects", "GitHub-проекты"));
        heading.setTextSize(21);
        heading.setTextColor(0xFF245B43);
        heading.setPadding(0, 0, 0, dp(10));
        page.addView(heading);

        current = new TextView(this);
        current.setTextSize(14);
        current.setTextColor(0xFF4A6252);
        page.addView(current);

        LinearLayout progress = new LinearLayout(this);
        progress.setOrientation(LinearLayout.HORIZONTAL);
        progress.setGravity(Gravity.CENTER_VERTICAL);
        spinner = new ProgressBar(this);
        spinner.setIndeterminate(true);
        spinner.setVisibility(View.GONE);
        progress.addView(spinner, new LinearLayout.LayoutParams(dp(32), dp(32)));
        message = new TextView(this);
        message.setTextSize(14);
        message.setPadding(dp(8), dp(4), 0, dp(4));
        progress.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(progress);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(controls);
        syncButton = button(t("Sync · Pull + Push", "Синхронизировать · Pull + Push"));
        syncButton.setOnClickListener(v -> sync(repo, branch, false));
        controls.addView(syncButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        chooseButton = button(t("Repositories", "Репозитории"));
        chooseButton.setOnClickListener(v -> showRepositories());
        controls.addView(chooseButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        signOutButton = button(t("Log out", "Выйти"));
        signOutButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(t("Disconnect GitHub?", "Отключить GitHub?"))
                .setMessage(t("This removes the token from this device; downloaded projects stay saved.",
                        "Токен будет удалён с устройства; рабочие копии проектов останутся."))
                .setPositiveButton(t("Disconnect", "Отключить"), (d, w) -> {
                    GitHubAuth.clear(this);
                    token = null;
                    repo = null;
                    showLogin();
                })
                .setNegativeButton(t("Cancel", "Отмена"), null).show());
        controls.addView(signOutButton, new LinearLayout.LayoutParams(dp(86), dp(48)));
        for (int i = 1; i < controls.getChildCount(); i++) {
            LinearLayout.LayoutParams margins = (LinearLayout.LayoutParams) controls.getChildAt(i).getLayoutParams();
            margins.leftMargin = dp(5);
            controls.getChildAt(i).setLayoutParams(margins);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(12), 0, 0);
        scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
        if (token == null) showLogin();
        else if (syncOnly && repo != null) sync(repo, branch, false);
        else showRepositories();
    }

    private String t(String en, String russian) { return ru ? russian : en; }

    private void setBusy(boolean value, String text) {
        busy = value;
        spinner.setVisibility(value ? View.VISIBLE : View.GONE);
        message.setText(text);
        syncButton.setEnabled(!value && repo != null && token != null);
        chooseButton.setEnabled(!value && token != null);
        signOutButton.setEnabled(!value && token != null);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(12);
        UiStyle.styleSoftGreenButton(b);
        return b;
    }

    private void showLogin() {
        repo = null;
        controls.setVisibility(View.GONE);
        current.setText("");
        body.removeAllViews();
        message.setText(t("Connect GitHub once to list your repositories.",
                "Подключите GitHub для выбора репозитория."));

        TextView guide = new TextView(this);
        guide.setText(t("Create a fine-grained GitHub token for the repositories you want to edit.\n"
                        + "Repository permission: Contents — Read and write. Metadata is read-only by default.\n"
                        + "Paste the token below. It is encrypted in Android Keystore.",
                "Создайте fine-grained токен GitHub только для нужных репозиториев.\n"
                        + "Разрешение: Contents — Read and write. Metadata доступно только для чтения.\n"
                        + "Вставьте токен ниже. Он будет зашифрован в Android Keystore."));
        guide.setTextColor(Color.DKGRAY);
        guide.setTextSize(14);
        body.addView(guide);

        EditText input = new EditText(this);
        input.setHint("github_pat_…");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(input);

        Button connect = button(t("Connect GitHub", "Подключить GitHub"));
        connect.setOnClickListener(v -> {
            String candidate = input.getText().toString().trim();
            if (candidate.isEmpty()) {
                message.setText(t("Paste a GitHub token.", "Вставьте GitHub-токен."));
                return;
            }
            setBusy(true, t("Checking GitHub access…", "Проверяем доступ к GitHub…"));
            worker.execute(() -> {
                try {
                    String username = new GitHubApi(candidate).username();
                    if (username.isEmpty()) throw new java.io.IOException("GitHub account unavailable");
                    GitHubAuth.save(this, candidate);
                    handler.post(() -> {
                        if (isFinishing()) return;
                        token = candidate;
                        input.setText("");
                        showRepositories();
                    });
                } catch (Exception error) { showError(error); }
            });
        });
        body.addView(connect);

        Button link = button(t("Create a limited-access token in browser",
                "Создать токен с ограниченным доступом"));
        link.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://github.com/settings/personal-access-tokens/new"))); }
            catch (ActivityNotFoundException e) { message.setText(t("No browser found", "Браузер не найден")); }
        });
        body.addView(link);
    }

    private void showRepositories() {
        if (token == null) { showLogin(); return; }
        controls.setVisibility(View.VISIBLE);
        current.setText(t("Account connected", "Аккаунт подключён"));
        setBusy(true, t("Loading GitHub repositories…", "Загружаем репозитории…"));
        worker.execute(() -> {
            try {
                List<GitHubApi.Repo> found = new GitHubApi(token).repositories();
                handler.post(() -> {
                    if (isFinishing()) return;
                    repositories = found;
                    setBusy(false, t("Choose a repository to open and sync",
                            "Выберите репозиторий для открытия и синхронизации"));
                    body.removeAllViews();
                    if (found.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText(t("No available repositories. Check token permissions.",
                                "Нет доступных репозиториев. Проверьте разрешения токена."));
                        body.addView(empty);
                    }
                    for (GitHubApi.Repo item : found) {
                        Button entry = button((item.privateRepo ? "🔒 " : "")
                                + item.fullName + " · " + item.branch);
                        entry.setTextSize(14);
                        entry.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                        entry.setPadding(dp(12), 0, dp(8), 0);
                        entry.setOnClickListener(v -> sync(item.fullName, item.branch, true));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
                        params.bottomMargin = dp(5);
                        body.addView(entry, params);
                    }
                });
            } catch (Exception error) { showError(error); }
        });
    }

    private void sync(String selectedRepo, String selectedBranch, boolean openAfter) {
        if (selectedRepo == null || token == null || busy) return;
        repo = selectedRepo;
        branch = selectedBranch;
        answers.clear();
        performSync(openAfter);
    }

    private void performSync(boolean openAfter) {
        if (busy) return;
        current.setText(repo + " · " + branch);
        controls.setVisibility(View.VISIBLE);
        setBusy(true, t("Comparing local and GitHub files…",
                "Сравниваем файлы с GitHub…"));
        worker.execute(() -> {
            try {
                GitSyncEngine.Result result = new GitSyncEngine(this, new GitHubApi(token))
                        .sync(repo, branch, new LinkedHashMap<>(answers));
                handler.post(() -> {
                    if (isFinishing()) return;
                    setBusy(false, result.success()
                            ? t("Pull: " + result.pulled + " · Push: " + result.pushed,
                                "Получено: " + result.pulled + " · Отправлено: " + result.pushed)
                            : t("Conflicts: " + result.conflicts.size(),
                                "Конфликты: " + result.conflicts.size()));
                    if (!result.success()) showConflicts(result.conflicts, 0, openAfter);
                    else {
                        answers.clear();
                        if (openAfter) selectRoot();
                        else {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                });
            } catch (Exception error) { showError(error); }
        });
    }

    private void showConflicts(List<GitSyncEngine.Conflict> conflicts, int index,
                               boolean openAfter) {
        if (index >= conflicts.size()) {
            performSync(openAfter);
            return;
        }
        GitSyncEngine.Conflict conflict = conflicts.get(index);
        String[] choices = {
                t("Keep my file (overwrite GitHub)", "Мой файл (перезаписать GitHub)"),
                t("Use GitHub file (discard my version)", "Файл GitHub (отбросить мою версию)"),
                t("Compare and merge manually", "Сравнить и объединить вручную")
        };
        boolean manual = textFile(conflict.local) && textFile(conflict.remote)
                && conflict.local.length <= 160 * 1024 && conflict.remote.length <= 160 * 1024;
        if (!manual) choices = Arrays.copyOf(choices, 2);
        new AlertDialog.Builder(this)
                .setTitle(t("Conflict · ", "Конфликт · ") + conflict.path)
                .setMessage(t("Both you and GitHub changed this file. Choose which version to keep.",
                        "Файл изменён и у вас, и на GitHub. Выберите вариант разрешения."))
                .setItems(choices, (d, which) -> {
                    if (which == 2) {
                        showManualMerge(conflict, conflicts, index, openAfter);
                        return;
                    }
                    answers.put(conflict.path, new GitSyncEngine.Resolution(
                            which == 0 ? GitSyncEngine.Choice.LOCAL : GitSyncEngine.Choice.REMOTE,
                            conflict.remoteSha, null));
                    showConflicts(conflicts, index + 1, openAfter);
                })
                .setNegativeButton(t("Cancel", "Отмена"), null).show();
    }

    private void showManualMerge(GitSyncEngine.Conflict conflict,
                                 List<GitSyncEngine.Conflict> conflicts, int index,
                                 boolean openAfter) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), 0, dp(12), 0);
        TextView remote = new TextView(this);
        remote.setText(t("GitHub:\n", "Версия GitHub:\n")
                + new String(conflict.remote, StandardCharsets.UTF_8));
        remote.setTextIsSelectable(true);
        remote.setTextSize(12);
        remote.setPadding(0, 0, 0, dp(8));
        layout.addView(remote);
        TextView local = new TextView(this);
        local.setText(t("Your version:\n", "Ваша версия:\n")
                + new String(conflict.local, StandardCharsets.UTF_8));
        local.setTextIsSelectable(true);
        local.setTextSize(12);
        local.setPadding(0, 0, 0, dp(8));
        layout.addView(local);
        EditText merged = new EditText(this);
        merged.setText(new String(conflict.local, StandardCharsets.UTF_8));
        merged.setTextSize(12);
        merged.setGravity(Gravity.TOP);
        merged.setMinLines(10);
        merged.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        layout.addView(merged);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        new AlertDialog.Builder(this)
                .setTitle(t("Merged result: ", "Результат объединения: ") + conflict.path)
                .setView(scroll)
                .setPositiveButton(t("Use merged text", "Применить объединение"), (d, w) -> {
                    answers.put(conflict.path, new GitSyncEngine.Resolution(
                            GitSyncEngine.Choice.MANUAL, conflict.remoteSha,
                            merged.getText().toString().getBytes(StandardCharsets.UTF_8)));
                    showConflicts(conflicts, index + 1, openAfter);
                })
                .setNegativeButton(t("Back", "Назад"),
                        (d, w) -> showConflicts(conflicts, index, openAfter))
                .show();
    }

    private static boolean textFile(byte[] bytes) {
        if (bytes == null) return false;
        String text = new String(bytes, StandardCharsets.UTF_8);
        return Arrays.equals(bytes, text.getBytes(StandardCharsets.UTF_8))
                && text.indexOf('\0') < 0;
    }

    private void selectRoot() {
        worker.execute(() -> {
            try {
                List<String> asm = new GitSyncEngine(this, new GitHubApi(token))
                        .sourceFiles(repo, branch);
                handler.post(() -> {
                    if (isFinishing()) return;
                    if (asm.isEmpty()) {
                        message.setText(t("No ASM source files were found in this repository.",
                                "В этом репозитории нет ASM-файлов."));
                        return;
                    }
                    if (asm.size() == 1) { finishOpen(asm.get(0)); return; }
                    // Read the selected repository's config, not a previously open project.
                    try {
                        File configuration = GitProjectState.file(
                                GitProjectState.folder(this, repo, branch), ".z80pocketide.json");
                        if (configuration.isFile()) {
                            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                            try (java.io.FileInputStream input = new java.io.FileInputStream(configuration)) {
                                byte[] buffer = new byte[1024];
                                int count;
                                while ((count = input.read(buffer)) != -1) {
                                    if (stream.size() + count > 8192) throw new java.io.IOException("Config too large");
                                    stream.write(buffer, 0, count);
                                }
                            }
                            String entry = new org.json.JSONObject(new String(
                                    stream.toByteArray(), StandardCharsets.UTF_8)).optString("root");
                            if (asm.contains(entry)) { finishOpen(entry); return; }
                        }
                    } catch (Exception ignored) { }
                    for (String filename : asm)
                        if (filename.equalsIgnoreCase("main.asm")) {
                            finishOpen(filename);
                            return;
                        }
                    new AlertDialog.Builder(this)
                            .setTitle(t("Select main ASM source", "Выберите главный ASM-файл"))
                            .setItems(asm.toArray(new String[0]), (d, i) -> finishOpen(asm.get(i)))
                            .setNegativeButton(t("Cancel", "Отмена"), null).show();
                });
            } catch (Exception error) { showError(error); }
        });
    }

    private void finishOpen(String rootFile) {
        File directory = GitProjectState.folder(this, repo, branch);
        try {
            File root = GitProjectState.file(directory, rootFile);
            if (!root.isFile()) throw new java.io.IOException("Main ASM is missing");
            Intent result = new Intent();
            result.putExtra(EXTRA_REPO, repo).putExtra(EXTRA_BRANCH, branch)
                    .putExtra(EXTRA_ROOT, rootFile);
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception error) { showError(error); }
    }

    private void showError(Exception error) {
        handler.post(() -> {
            if (isFinishing()) return;
            setBusy(false, t("GitHub error: ", "Ошибка GitHub: ")
                    + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
        });
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
