package com.alexzab.z80pocketide;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;
import com.alexzab.z80pocketide.editor.SyntaxHighlighter;
import com.alexzab.z80pocketide.examples.ExamplePrograms;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.zx.TapWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE_TAP = 1001;
    private static final String STATE_SOURCE = "source";
    private static final String STATE_CURSOR = "cursor";

    private EditText editor;
    private TextView status;
    private Button runTap;
    private Button saveTap;
    private AppLanguage language;

    private byte[] lastTap;
    private String lastBuiltSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = LanguageSettings.get(this);

        getWindow().setStatusBarColor(Color.rgb(250, 250, 250));
        getWindow().setNavigationBarColor(Color.rgb(250, 250, 250));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

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
        subtitle.setText(t("for ZX Spectrum · v0.5", "для ZX Spectrum · v0.5"));
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(100, 100, 100));

        titleBlock.addView(title);
        titleBlock.addView(subtitle);
        topBar.addView(titleBlock, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button languageButton = compactButton(language == AppLanguage.RU ? "RU" : "EN");
        Button reference = compactButton(t("Ref", "Справка"));
        Button examples = compactButton(t("Examples", "Примеры"));
        topBar.addView(languageButton);
        topBar.addView(reference);
        topBar.addView(examples);
        root.addView(topBar);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(16);
        editor.setTypeface(android.graphics.Typeface.MONOSPACE);
        editor.setTextColor(Color.rgb(25, 25, 25));
        editor.setBackgroundColor(Color.rgb(248, 248, 248));
        editor.setPadding(dp(12), dp(10), dp(12), dp(10));
        editor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(true);

        String source = savedInstanceState == null
                ? ExamplePrograms.ALL[0].source
                : savedInstanceState.getString(STATE_SOURCE, ExamplePrograms.ALL[0].source);
        int cursor = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_CURSOR, 0);
        editor.setText(source);
        editor.setSelection(Math.min(Math.max(cursor, 0), editor.length()));
        root.addView(editor, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        SyntaxHighlighter.attach(editor);

        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(8), dp(6), dp(8), 0);
        bottomPanel.setBackgroundColor(Color.rgb(245, 245, 245));
        if (Build.VERSION.SDK_INT >= 21) bottomPanel.setElevation(dp(4));

        status = new TextView(this);
        status.setText(t("Syntax highlighting on · Build to assemble",
                "Подсветка синтаксиса включена · Соберите программу"));
        status.setTextSize(13);
        status.setTextColor(Color.rgb(90, 90, 90));
        status.setPadding(dp(4), dp(3), dp(4), dp(5));
        bottomPanel.addView(status);

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
        examples.setOnClickListener(v -> showExamples());
        reference.setOnClickListener(v -> openReference());
        languageButton.setOnClickListener(v -> showLanguageDialog());

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (lastBuiltSource != null && !s.toString().equals(lastBuiltSource)) {
                    invalidateBuild(t("Modified · Build required",
                            "Изменено · требуется новая сборка"));
                }
            }
        });

        setContentView(root);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (editor != null) {
            outState.putString(STATE_SOURCE, editor.getText().toString());
            outState.putInt(STATE_CURSOR, editor.getSelectionStart());
        }
    }

    private String t(String en, String ru) {
        return Texts.pick(language, en, ru);
    }

    private Button compactButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(9), 0, dp(9), 0);
        return button;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinWidth(0);
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private void showLanguageDialog() {
        String[] labels = {"English", "Русский"};
        int selected = language == AppLanguage.RU ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle(t("Language", "Язык"))
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    AppLanguage newLanguage = which == 1 ? AppLanguage.RU : AppLanguage.EN;
                    dialog.dismiss();
                    if (newLanguage != language) {
                        LanguageSettings.set(this, newLanguage);
                        recreate();
                    }
                })
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void openReference() {
        Intent intent = new Intent(this, ReferenceActivity.class);
        String word = wordAtCursor();
        if (!word.isEmpty()) intent.putExtra(ReferenceActivity.EXTRA_QUERY, word);
        startActivity(intent);
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
        setStatus(t("Building…", "Сборка…"), Color.DKGRAY);
        runTap.setEnabled(false);
        saveTap.setEnabled(false);
        lastTap = null;
        lastBuiltSource = null;

        try {
            String source = editor.getText().toString();
            AssemblyResult result = new Assembler().assemble(source);
            lastTap = new TapWriter().programTap(
                    "PROGRAM", result.getOrigin(), result.getBytes());
            lastBuiltSource = source;
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

    private void runTapInEmulator() {
        String source = editor.getText().toString();
        if (lastTap == null || lastBuiltSource == null || !source.equals(lastBuiltSource)) {
            if (!buildSource()) return;
        }

        try {
            File dir = new File(getCacheDir(), "shared");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException(t("cannot create cache directory",
                        "не удалось создать временную папку"));
            }

            File file = new File(dir, "program.tap");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(lastTap);
            }

            Uri uri = Uri.parse("content://" + getPackageName() + ".tap/program.tap");
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "application/octet-stream");
            view.setClipData(ClipData.newRawUri("ZX Spectrum TAP", uri));
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

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
            String message = t("Run error · ", "Ошибка запуска · ") + ex.getMessage();
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void saveTapFile() {
        String source = editor.getText().toString();
        if (lastTap == null || lastBuiltSource == null || !source.equals(lastBuiltSource)) {
            if (!buildSource()) return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "program.tap");
        startActivityForResult(intent, REQUEST_SAVE_TAP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SAVE_TAP || resultCode != RESULT_OK
                || data == null || lastTap == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) return;

        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException(t("cannot open output file",
                    "не удалось открыть выходной файл"));
            stream.write(lastTap);
            stream.flush();
            setStatus(t("TAP saved · ", "TAP сохранён · ") + uri.getLastPathSegment(),
                    Color.rgb(0, 110, 45));
            Toast.makeText(this, t("TAP saved", "TAP сохранён"), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            String message = t("Save error · ", "Ошибка сохранения · ") + ex.getMessage();
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void showExamples() {
        String[] labels = new String[ExamplePrograms.ALL.length];
        for (int i = 0; i < labels.length; i++) {
            String title = Texts.exampleTitle(language, ExamplePrograms.ALL[i].title);
            String description = Texts.exampleDescription(language, ExamplePrograms.ALL[i].description);
            labels[i] = title + " — " + description;
        }

        new AlertDialog.Builder(this)
                .setTitle(t("Built-in ZX examples", "Встроенные примеры ZX"))
                .setItems(labels, (dialog, which) -> {
                    ExamplePrograms.Example example = ExamplePrograms.ALL[which];
                    editor.setText(example.source);
                    editor.setSelection(0);
                    String localizedTitle = Texts.exampleTitle(language, example.title);
                    invalidateBuild(language == AppLanguage.RU
                            ? "Загружен пример «" + localizedTitle + "» · Соберите и запустите"
                            : localizedTitle + " loaded · Build, then Run");
                })
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .show();
    }

    private void invalidateBuild(String message) {
        lastTap = null;
        lastBuiltSource = null;
        runTap.setEnabled(false);
        saveTap.setEnabled(false);
        setStatus(message, Color.rgb(90, 90, 90));
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
            // Keep the textual error visible even if a line number cannot be parsed.
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
