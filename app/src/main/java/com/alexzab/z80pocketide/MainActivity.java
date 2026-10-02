package com.alexzab.z80pocketide;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;

public class MainActivity extends Activity {
    private EditText editor;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.END);

        TextView title = new TextView(this);
        title.setText("Z80 Pocket IDE");
        title.setTextSize(20);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        toolbar.addView(title, titleParams);

        Button build = new Button(this);
        build.setText("BUILD");
        toolbar.addView(build);
        root.addView(toolbar);

        editor = new EditText(this);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(16);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(true);
        editor.setText("ORG $8000\n\nSTART:\n    LD A,2\n    OUT (254),A\n    JP START\n");
        root.addView(editor, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        output = new TextView(this);
        output.setText("Ready");
        output.setPadding(0, 12, 0, 0);
        root.addView(output);

        build.setOnClickListener(this::buildSource);
        setContentView(root);
    }

    private void buildSource(View ignored) {
        try {
            AssemblyResult result = new Assembler().assemble(editor.getText().toString());
            output.setText("Build OK · " + result.getBytes().length + " bytes · ORG $" + String.format("%04X", result.getOrigin()));
        } catch (RuntimeException ex) {
            output.setText("Build error: " + ex.getMessage());
        }
    }
}
