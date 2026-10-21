package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.ui.UiStyle;

import java.util.List;

/** Bidirectional visual ZX Spectrum 48K keyboard half-row calculator. */
public final class SpectrumKeyboardPanel extends LinearLayout {
    public interface InsertAction { void insert(String source); }

    // Keep the live keyboard when the user closes/reopens the workshop.
    private static final SpectrumKeyboardMatrix SESSION = new SpectrumKeyboardMatrix();
    private static int lastRow = 3;

    private final boolean ru;
    private final InsertAction insertAction;
    private final SpectrumKeyboardView keyboard;
    private final TextView activeReadout, feedback;
    private final LinearLayout rows;
    private final EditText portField, hexField, binaryField;
    private boolean updating;

    public SpectrumKeyboardPanel(Context context, boolean ru, InsertAction insertAction) {
        super(context);
        this.ru = ru;
        this.insertAction = insertAction;
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(9), dp(2), dp(22));

        addView(title(t("ZX Spectrum · Keyboard", "ZX Spectrum · Клавиатура"), 19));
        addView(description(t(
                "Tap keys to press/release them. Multiple keys can be selected at once.",
                "Нажимай на клавиши для выбора и отпускания. Можно выбрать несколько одновременно.")));

        LinearLayout keyboardCard = card();
        keyboard = new SpectrumKeyboardView(context, SESSION, key -> {
            SESSION.toggle(key);
            lastRow = SpectrumKeyboardMatrix.rowOf(key);
            refresh(null);
        });
        keyboardCard.addView(keyboard, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(keyboardCard);

        LinearLayout summary = card();
        summary.addView(title(t("Read keyboard matrix", "Чтение матрицы клавиш"), 16));
        activeReadout = new TextView(context);
        activeReadout.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        activeReadout.setTextSize(15);
        activeReadout.setTextColor(0xFF22583D);
        activeReadout.setPadding(dp(4), dp(7), dp(4), dp(5));
        summary.addView(activeReadout);

        rows = new LinearLayout(context);
        rows.setOrientation(VERTICAL);
        summary.addView(rows);

        Button clear = smallButton(t("Release all keys", "Отпустить все клавиши"));
        summary.addView(clear, new LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));
        clear.setOnClickListener(v -> {
            SESSION.clear();
            refresh(null);
        });
        addView(summary);

        LinearLayout reverse = card();
        reverse.addView(title(t("Reverse · address and byte", "Обратно · адрес и байт"), 16));
        reverse.addView(description(t(
                "Enter one half-row port and the read byte in HEX or BIN. "
                + "The matching keys light up on the keyboard.",
                "Введи адрес одного полуряда и прочитанный байт в HEX или BIN. "
                + "Соответствующие клавиши подсветятся.")));

        portField = editField(reverse, t("Half-row port · HEX", "Порт полуряда · HEX"), "$F7FE");
        LinearLayout two = new LinearLayout(context);
        two.setOrientation(HORIZONTAL);
        LinearLayout h = new LinearLayout(context);
        h.setOrientation(VERTICAL);
        h.addView(title("HEX", 12));
        hexField = editor("$FF");
        h.addView(hexField, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        two.addView(h, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout b = new LinearLayout(context);
        b.setOrientation(VERTICAL);
        b.addView(title("BIN", 12));
        binaryField = editor("%11111111");
        b.addView(binaryField, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        LinearLayout.LayoutParams bp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.3f);
        bp.setMargins(dp(8), 0, 0, 0);
        two.addView(b, bp);
        reverse.addView(two);

        feedback = description("");
        feedback.setTextColor(0xFF58705F);
        reverse.addView(feedback);

        TextWatcher addressWatcher = watcher(0);
        TextWatcher hexWatcher = watcher(1);
        TextWatcher binaryWatcher = watcher(2);
        portField.addTextChangedListener(addressWatcher);
        hexField.addTextChangedListener(hexWatcher);
        binaryField.addTextChangedListener(binaryWatcher);
        addView(reverse);

        Button insert = new Button(context);
        insert.setText(t("Insert Z80 reading snippet", "Вставить код чтения Z80"));
        UiStyle.styleGreenButton(insert);
        addView(insert, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        insert.setOnClickListener(v -> {
            int bit = -1;
            for (int i = 0; i < SpectrumKeyboardMatrix.KEYS_PER_ROW; i++) {
                if (SESSION.isPressed(lastRow, i)) {
                    bit = i;
                    break;
                }
            }
            insertAction.insert(SpectrumKeyboardMatrix.asmSnippet(lastRow, bit));
        });

        addView(description(t(
                "Use LD BC,$F7FE / IN A,(C). Bits 0–4 are active-low (0 = pressed). "
                + "The displayed high bits 5–7 are normalized to 1; on real hardware "
                + "bit 6 may reflect EAR. A reverse lookup changes the complete selection.",
                "Для чтения: LD BC,$F7FE / IN A,(C). Биты 0–4 активны нулём "
                + "(0 = нажата). Старшие биты 5–7 условно показаны как 1; "
                + "на реальном железе бит 6 может отражать EAR. Обратный ввод "
                + "заменяет весь набор выбранных клавиш.")));

        refresh(null);
    }

    private TextWatcher watcher(int source) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable e) {
                if (updating) return;
                try {
                    int port = SpectrumKeyboardMatrix.parseHex(portField.getText().toString(), 0xFFFF);
                    int value = source == 2
                            ? SpectrumKeyboardMatrix.parseBinaryByte(binaryField.getText().toString())
                            : SpectrumKeyboardMatrix.parseHex(hexField.getText().toString(), 255);
                    lastRow = SESSION.loadPortByte(port, value);
                    refresh(source == 0 ? portField : source == 1 ? hexField : binaryField);
                } catch (IllegalArgumentException ex) {
                    feedback.setText(t("Invalid half-row/byte: ", "Неверный полуряд или байт: ")
                            + ex.getMessage());
                    feedback.setTextColor(0xFFB34548);
                }
            }
        };
    }

    private void refresh(EditText excluded) {
        if (updating) return;
        updating = true;
        int port = SpectrumKeyboardMatrix.port(lastRow);
        int value = SESSION.readByte(lastRow);
        if (portField != excluded) portField.setText(SpectrumKeyboardMatrix.hexPort(port));
        if (hexField != excluded) hexField.setText(SpectrumKeyboardMatrix.hexByte(value));
        if (binaryField != excluded) binaryField.setText(SpectrumKeyboardMatrix.binByte(value));

        List<String> selected = SESSION.pressedKeys(lastRow);
        activeReadout.setText(SpectrumKeyboardMatrix.hexPort(port) + "  →  "
                + SpectrumKeyboardMatrix.hexByte(value) + "  "
                + SpectrumKeyboardMatrix.binByte(value));

        rows.removeAllViews();
        boolean any = false;
        for (int row = 0; row < SpectrumKeyboardMatrix.ROW_COUNT; row++) {
            if (SESSION.rowHasKeys(row)) {
                addRowResult(row);
                any = true;
            }
        }
        if (!any) {
            TextView hint = description(t("No keys pressed · $FF in every half-row",
                    "Клавиши отпущены · $FF во всех полурядах"));
            rows.addView(hint);
        } else {
            rows.addView(description(t(
                    "Tap a result row to inspect its port and byte.",
                    "Нажми на строку результата, чтобы посмотреть её порт и байт.")));
        }
        feedback.setText(t("Linked to the highlighted keys", "Соответствует подсвеченным клавишам"));
        feedback.setTextColor(0xFF328153);
        keyboard.invalidate();
        updating = false;
    }

    private void addRowResult(int row) {
        int port = SpectrumKeyboardMatrix.port(row);
        int value = SESSION.readByte(row);
        StringBuilder joined = new StringBuilder();
        for (String key : SESSION.pressedKeys(row)) {
            if (joined.length() != 0) joined.append(", ");
            joined.append(key);
        }
        String names = joined.toString();
        Button button = smallButton(
                SpectrumKeyboardMatrix.hexPort(port) + "  " + SpectrumKeyboardMatrix.hexByte(value)
                + "  " + SpectrumKeyboardMatrix.binByte(value)
                + "\n" + names);
        button.setTextSize(12);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(dp(13), dp(4), dp(7), dp(4));
        button.setTypeface(Typeface.MONOSPACE);
        if (row == lastRow) UiStyle.styleGreenButton(button);
        rows.addView(button, new LayoutParams(LayoutParams.MATCH_PARENT, dp(59)));
        button.setOnClickListener(v -> {
            lastRow = row;
            refresh(null);
        });
    }

    private LinearLayout card() {
        LinearLayout result = new LinearLayout(getContext());
        result.setOrientation(VERTICAL);
        result.setPadding(dp(11), dp(11), dp(11), dp(11));
        result.setBackground(UiStyle.rounded(getContext(),
                0xFFFFFFFF, 16, 0xFFD0E2D6, 1));
        LayoutParams p = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(5), 0, dp(12));
        result.setLayoutParams(p);
        return result;
    }

    private EditText editField(LinearLayout container, String caption, String hint) {
        container.addView(title(caption, 12));
        EditText input = editor(hint);
        container.addView(input, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        return input;
    }

    private EditText editor(String hint) {
        EditText input = new EditText(getContext());
        input.setHint(hint);
        input.setTextColor(0xFF29392E);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setTypeface(Typeface.MONOSPACE);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        return input;
    }

    private Button smallButton(String label) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumHeight(0);
        UiStyle.styleSoftGreenButton(b);
        return b;
    }

    private TextView title(String s, int size) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFF276845);
        t.setPadding(dp(3), dp(4), dp(3), dp(6));
        return t;
    }

    private TextView description(String s) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(0xFF6A806F);
        t.setPadding(dp(4), dp(5), dp(4), dp(7));
        return t;
    }

    private String t(String en, String russian) { return ru ? russian : en; }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
