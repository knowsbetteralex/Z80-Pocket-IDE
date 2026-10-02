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

import com.alexzab.z80pocketide.reference.InstructionReference;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;

import java.util.List;

public final class ReferenceActivity extends Activity {
    public static final String EXTRA_QUERY = "query";

    private LinearLayout results;
    private TextView count;
    private EditText search;

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

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = new Button(this);
        back.setText("←");
        back.setTextSize(18);
        back.setMinWidth(0);
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Z80 Reference");
        title.setTextSize(20);
        title.setTextColor(Color.rgb(25, 25, 25));
        count = new TextView(this);
        count.setTextSize(12);
        count.setTextColor(Color.rgb(95, 95, 95));
        titles.addView(title);
        titles.addView(count);
        header.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        SpectrumStripeView stripe = new SpectrumStripeView(this);
        root.addView(stripe, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search mnemonic, syntax, opcode, flags…");
        search.setTextSize(15);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, dp(8), 0, dp(4));
        root.addView(search, searchParams);

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0, dp(4), 0, dp(12));
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        back.setOnClickListener(v -> finish());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                render(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        String initial = getIntent().getStringExtra(EXTRA_QUERY);
        if (initial != null && !initial.isEmpty()) {
            search.setText(initial);
            search.setSelection(search.length());
        } else {
            render("");
        }

        setContentView(root);
    }

    private void render(String query) {
        List<InstructionReference.Entry> entries = InstructionReference.search(query);
        count.setText(entries.size() + " entries · documented Z80 + assembler directives");
        results.removeAllViews();
        for (InstructionReference.Entry entry : entries) {
            results.addView(entryView(entry));
        }
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No matching instruction");
            empty.setTextSize(15);
            empty.setTextColor(Color.rgb(110, 110, 110));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(8), dp(32), dp(8), dp(32));
            results.addView(empty);
        }
    }

    private View entryView(InstructionReference.Entry e) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundColor(Color.rgb(247, 247, 247));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(cp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView mnemonic = new TextView(this);
        mnemonic.setText(e.mnemonic);
        mnemonic.setTextSize(18);
        mnemonic.setTextColor(Color.rgb(32, 78, 160));
        mnemonic.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        head.addView(mnemonic, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView category = new TextView(this);
        category.setText(e.category);
        category.setTextSize(12);
        category.setTextColor(Color.rgb(110, 70, 120));
        head.addView(category);
        card.addView(head);

        card.addView(text(e.syntax, 14, Color.rgb(25, 25, 25), true));
        card.addView(text("Opcode: " + e.opcode, 12, Color.rgb(80, 80, 80), true));
        card.addView(text("T: " + e.tStates + "    Flags: " + e.flags,
                12, Color.rgb(80, 80, 80), false));
        card.addView(text(e.description, 13, Color.rgb(65, 65, 65), false));
        return card;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
