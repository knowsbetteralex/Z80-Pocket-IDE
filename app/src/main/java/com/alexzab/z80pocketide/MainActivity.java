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
import com.alexzab.z80pocketide.examples.ExamplePrograms;
import com.alexzab.z80pocketide.zx.TapWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE_TAP = 1001;

    private EditText editor;
    private TextView status;
    private Button runTap;
    private Button saveTap;

    private byte[] lastTap;
    private String lastBuiltSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        topBar.setPadding(dp(4), 0, dp(4), dp(6));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Z80 Pocket IDE");
        title.setTextSize(20);
        title.setTextColor(Color.rgb(30, 30, 30));

        TextView subtitle = new TextView(this);
        subtitle.setText("ZX Spectrum · v0.3");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(100, 100, 100));

        titleBlock.addView(title);
        titleBlock.addView(subtitle);
        topBar.addView(titleBlock, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button examples = new Button(this);
        examples.setText("EXAMPLES");
        examples.setAllCaps(false);
        topBar.addView(examples);
        root.addView(topBar);

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(16);
        editor.setTextColor(Color.rgb(25, 25, 25));
        editor.setBackgroundColor(Color.rgb(248, 248, 248));
        editor.setPadding(dp(12), dp(10), dp(12), dp(10));
        editor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(true);
        editor.setText(ExamplePrograms.ALL[0].source);
        editor.setSelection(0);
        root.addView(editor, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottomPanel = new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(8), dp(6), dp(8), 0);
        bottomPanel.setBackgroundColor(Color.rgb(245, 245, 245));
        if (Build.VERSION.SDK_INT >= 21) bottomPanel.setElevation(dp(4));

        status = new TextView(this);
        status.setText("Border cycle example · Build to assemble");
        status.setTextSize(13);
        status.setTextColor(Color.rgb(90, 90, 90));
        status.setPadding(dp(4), dp(3), dp(4), dp(5));
        bottomPanel.addView(status);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);

        Button build = actionButton("Build");
        runTap = actionButton("Run");
        saveTap = actionButton("Save .tap");

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

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (lastBuiltSource != null && !s.toString().equals(lastBuiltSource)) {
                    invalidateBuild("Modified · Build required");
                }
            }
        });

        setContentView(root);
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

    private boolean buildSource() {
        hideKeyboard();
        setStatus("Building…", Color.DKGRAY);
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

            String message = String.format(Locale.US,
                    "Build OK · %d bytes · ORG $%04X · autorun TAP ready",
                    result.getBytes().length,
                    result.getOrigin() & 0xFFFF);
            setStatus(message, Color.rgb(0, 110, 45));
            Toast.makeText(this, "Build OK", Toast.LENGTH_SHORT).show();
            return true;
        } catch (RuntimeException ex) {
            String message = "Build error · " + ex.getMessage();
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            jumpToErrorLine(ex.getMessage());
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
                throw new IllegalStateException("cannot create cache directory");
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

            setStatus("Opening TAP · choose your ZX Spectrum emulator",
                    Color.rgb(45, 80, 150));
            startActivity(Intent.createChooser(view, "Open TAP with"));
        } catch (ActivityNotFoundException ex) {
            setStatus("No app found for .tap files · install/configure a ZX Spectrum emulator",
                    Color.rgb(180, 30, 30));
            Toast.makeText(this, "No app can open TAP files", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            String message = "Run error · " + ex.getMessage();
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
            if (stream == null) throw new IllegalStateException("cannot open output file");
            stream.write(lastTap);
            stream.flush();
            setStatus("TAP saved · " + uri.getLastPathSegment(),
                    Color.rgb(0, 110, 45));
            Toast.makeText(this, "TAP saved", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            String message = "Save error · " + ex.getMessage();
            setStatus(message, Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void showExamples() {
        String[] labels = new String[ExamplePrograms.ALL.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = ExamplePrograms.ALL[i].title + " — "
                    + ExamplePrograms.ALL[i].description;
        }

        new AlertDialog.Builder(this)
                .setTitle("Built-in ZX examples")
                .setItems(labels, (dialog, which) -> {
                    ExamplePrograms.Example example = ExamplePrograms.ALL[which];
                    editor.setText(example.source);
                    editor.setSelection(0);
                    invalidateBuild(example.title + " loaded · Build, then Run");
                })
                .setNegativeButton("Cancel", null)
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
