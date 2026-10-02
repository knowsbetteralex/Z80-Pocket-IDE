package com.alexzab.z80pocketide;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;
import com.alexzab.z80pocketide.zx.TapWriter;

import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_SAVE_TAP = 1001;

    private EditText editor;
    private TextView output;
    private Button saveTap;
    private byte[] lastTap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Z80 Pocket IDE");
        title.setTextSize(20);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        toolbar.addView(title, titleParams);

        Button build = new Button(this);
        build.setText("BUILD");
        toolbar.addView(build);

        saveTap = new Button(this);
        saveTap.setText("SAVE TAP");
        saveTap.setEnabled(false);
        toolbar.addView(saveTap);
        root.addView(toolbar);

        output = new TextView(this);
        output.setText("Ready · press BUILD to assemble");
        output.setTextSize(14);
        output.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(output, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(16);
        editor.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(true);
        editor.setText("ORG $8000\n\nSTART:\n    LD A,2\n    OUT (254),A\n    JP START\n");
        root.addView(editor, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        build.setOnClickListener(this::buildSource);
        saveTap.setOnClickListener(v -> saveTapFile());
        setContentView(root);
    }

    private void buildSource(View ignored) {
        hideKeyboard();
        output.setText("Building…");
        output.setTextColor(Color.DKGRAY);
        saveTap.setEnabled(false);
        lastTap = null;

        try {
            AssemblyResult result = new Assembler().assemble(editor.getText().toString());
            lastTap = new TapWriter().codeTap("PROGRAM", result.getOrigin(), result.getBytes());
            saveTap.setEnabled(true);

            String message = String.format(Locale.US,
                    "Build OK · %d bytes · ORG $%04X · TAP ready",
                    result.getBytes().length,
                    result.getOrigin() & 0xFFFF);
            output.setText(message);
            output.setTextColor(Color.rgb(0, 110, 45));
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException ex) {
            String message = "Build error: " + ex.getMessage();
            output.setText(message);
            output.setTextColor(Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            jumpToErrorLine(ex.getMessage());
        }
    }

    private void saveTapFile() {
        if (lastTap == null) {
            Toast.makeText(this, "Build the program first", Toast.LENGTH_SHORT).show();
            return;
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
        if (requestCode != REQUEST_SAVE_TAP || resultCode != RESULT_OK || data == null || lastTap == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) return;

        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IllegalStateException("cannot open output file");
            stream.write(lastTap);
            stream.flush();
            output.setText("TAP saved · " + uri.getLastPathSegment());
            output.setTextColor(Color.rgb(0, 110, 45));
            Toast.makeText(this, "TAP saved", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            String message = "Save error: " + ex.getMessage();
            output.setText(message);
            output.setTextColor(Color.rgb(180, 30, 30));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
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
