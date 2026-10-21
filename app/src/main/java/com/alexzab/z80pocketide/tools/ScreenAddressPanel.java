package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.alexzab.z80pocketide.ui.UiStyle;

/** Bidirectional ZX 48K bitmap and attribute address calculator. */
public final class ScreenAddressPanel extends LinearLayout {
    private static final int C_THIRD = 0xFF13865A;
    private static final int C_LINE = 0xFFCC6C1D;
    private static final int C_ROW = 0xFF9B4AA8;
    private static final int C_COL = 0xFF2869B0;
    private final boolean ru;
    private final EditText row, pixelLine, column;
    private final TextView third;
    private final EditText attrRow, attrCol, pixelHex, pixelBinary, attrHex, attrBinary;
    private final TextView status;
    private final ScreenGridView screenGrid;
    private final PixelByteView pixelPreview;
    private final TextView positionDetails;
    private ScreenAddress current = new ScreenAddress(0, 0, 0, 0);
    private boolean updating;

    public ScreenAddressPanel(Context context, boolean ru) {
        super(context);
        this.ru = ru;
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(7), dp(2), dp(15));

        addView(heading(t("Bitmap: 010 TT LLL RRR CCCCC",
                "Экран: 010 TT LLL RRR CCCCC"), 17, C_THIRD));
        addView(description(t("Row is 0–23 across the entire screen; third is calculated. Column is a byte (8 pixels).",
                "Строка 0–23 по всему экрану; треть вычисляется автоматически. Столбец — байт (8 пикселей).")));

        // Touchable overall 256×192 screen, subdivided into 32×24 characters.
        LinearLayout visualization = card();
        visualization.addView(heading(
                t("Screen map · tap a character cell", "Карта экрана · выбери знакоместо"),
                15, C_THIRD));
        visualization.addView(description(t(
                "Each square = 8×8 pixels / 1 attribute. Heavy lines separate the screen thirds.",
                "Каждая клетка = 8×8 пикселей / 1 атрибут. Толстые линии разделяют трети экрана.")));
        screenGrid = new ScreenGridView(context, (charRow, col) ->
                apply(ScreenAddress.fromAttributeCoordinates(
                        charRow, col, current.pixelLine), null));
        visualization.addView(screenGrid, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        visualization.addView(heading(
                t("Pixel scanline and bit in selected byte (8×8)",
                        "Пиксельная линия и бит в выбранном байте (8×8)"),
                14, C_LINE));
        LinearLayout detailRow = horizontal();
        detailRow.setGravity(Gravity.CENTER_VERTICAL);
        pixelPreview = new PixelByteView(context, (line, bit) ->
                apply(new ScreenAddress(current.third, current.row,
                        line, current.column), null));
        detailRow.addView(pixelPreview, new LayoutParams(dp(164), dp(164)));
        positionDetails = description("");
        positionDetails.setTypeface(Typeface.MONOSPACE);
        positionDetails.setTextSize(12);
        positionDetails.setPadding(dp(9), dp(4), dp(2), dp(4));
        detailRow.addView(positionDetails, new LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f));
        visualization.addView(detailRow);
        visualization.addView(description(t(
                "Tap a pixel in the large 8×8 cell: its row sets LLL (bitmap address). "
                        + "The bit column supplies a separate byte mask; it does not change the address.",
                "Выбери пиксель в крупной клетке: строка задаёт LLL (адрес bitmap). "
                        + "Столбец определяет маску бита внутри байта и не меняет адрес.")));
        addView(visualization);

        LinearLayout coords = card();
        LinearLayout one = horizontal();
        third = readOnlyField(one, t("Third TT · automatic", "Треть TT · автоматически"), C_THIRD);
        row = field(one, t("Char row 0–23", "Строка 0–23"), "0", C_ROW, true);
        coords.addView(one);
        LinearLayout two = horizontal();
        pixelLine = field(two, t("Pixel line LLL", "Пикс. линия LLL"), "0", C_LINE, true);
        column = field(two, t("Column CCCCC", "Столбец CCCCC"), "0", C_COL, true);
        coords.addView(two);
        addView(coords);

        LinearLayout pixelAddress = card();
        pixelAddress.addView(heading(t("Bitmap address · BIN", "Адрес bitmap · BIN"), 14, 0xFF333E38));
        pixelBinary = input(pixelAddress, "010 00 000 000 00000", false);
        pixelAddress.addView(description(t("010 (fixed)  TT (third)  LLL (pixel line)  RRR (row)  CCCCC (column)",
                "010 (пост.)  TT (треть)  LLL (пикс. линия)  RRR (строка)  CCCCC (столбец)")));
        pixelAddress.addView(heading(t("Bitmap address · HEX", "Адрес bitmap · HEX"), 14, 0xFF333E38));
        pixelHex = input(pixelAddress, "$4000", false);
        addView(pixelAddress);

        addView(heading(t("Attributes · 32 × 24", "Атрибуты · 32 × 24"), 17, C_THIRD));
        LinearLayout attrs = card();
        LinearLayout coordAttrs = horizontal();
        attrRow = field(coordAttrs, t("Char row 0–23", "Строка 0–23"), "0", C_ROW, true);
        attrCol = field(coordAttrs, t("Char column 0–31", "Столбец 0–31"), "0", C_COL, true);
        attrs.addView(coordAttrs);
        attrs.addView(heading("BIN · 010110 RRRRR CCCCC", 14, 0xFF333E38));
        attrBinary = input(attrs, "010110 00000 00000", false);
        attrs.addView(heading("HEX", 14, 0xFF333E38));
        attrHex = input(attrs, "$5800", false);
        addView(attrs);

        android.widget.Button reset = new android.widget.Button(context);
        reset.setText(t("Reset everything to zero", "Сбросить всё в ноль"));
        UiStyle.styleGreenButton(reset);
        addView(reset, new LayoutParams(LayoutParams.MATCH_PARENT, dp(46)));

        status = description("");
        status.setTextColor(0xFF977143);
        addView(status);

        watchCoordinate(row, 1);
        watchCoordinate(pixelLine, 2);
        watchCoordinate(column, 3);
        watchAttrCoordinate(attrRow, true);
        watchAttrCoordinate(attrCol, false);
        watchAddress(pixelHex, 0);
        watchAddress(pixelBinary, 1);
        watchAddress(attrHex, 2);
        watchAddress(attrBinary, 3);
        reset.setOnClickListener(v -> apply(new ScreenAddress(0, 0, 0, 0), null));
        apply(current, null);
    }

    private void watchCoordinate(EditText field, int component) {
        watch(field, raw -> {
            int value = Integer.parseInt(raw);
            switch (component) {
                case 1: return ScreenAddress.fromAttributeCoordinates(value, current.column, current.pixelLine);
                case 2: return new ScreenAddress(current.third, current.row, value, current.column);
                default: return new ScreenAddress(current.third, current.row, current.pixelLine, value);
            }
        });
    }

    private void watchAttrCoordinate(EditText field, boolean isRow) {
        watch(field, raw -> ScreenAddress.fromAttributeCoordinates(
                isRow ? Integer.parseInt(raw) : current.characterRow(),
                isRow ? current.column : Integer.parseInt(raw), current.pixelLine));
    }

    private void watchAddress(EditText field, int kind) {
        watch(field, raw -> {
            int value = (kind == 1 || kind == 3)
                    ? ScreenAddress.parseBinaryAddress(raw) : ScreenAddress.parseHexAddress(raw);
            return kind < 2 ? ScreenAddress.fromPixelAddress(value)
                    : ScreenAddress.fromAttributeAddress(value, current.pixelLine);
        });
    }

    private interface Decoder { ScreenAddress decode(String raw); }

    private void watch(EditText field, Decoder decoder) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (updating) return;
                try {
                    ScreenAddress next = decoder.decode(s.toString().trim());
                    apply(next, field);
                } catch (RuntimeException ex) {
                    status.setText(t("Invalid value or out of range", "Неверное значение или выход за диапазон")
                            + " · " + ex.getMessage());
                    status.setTextColor(0xFFBC3848);
                }
            }
        });
    }

    private void apply(ScreenAddress next, EditText excluded) {
        current = next;
        screenGrid.setSelection(current);
        pixelPreview.setSelection(current.pixelLine, pixelPreview.selectedBit());
        int bit = pixelPreview.selectedBit();
        positionDetails.setText(
                t("Cell: ", "Знакоместо: ") + current.column + ", " + current.characterRow()
                        + "\nX: " + (current.column * 8 + bit)
                        + "\nY: " + (current.characterRow() * 8 + current.pixelLine)
                        + "\nLLL: " + current.pixelLine
                        + "\nBIT: " + bit
                        + "\nMASK: " + String.format(java.util.Locale.ROOT,
                                "$%02X", ScreenTouchMath.bitMask(bit)));
        updating = true;
        third.setText(String.valueOf(current.third));
        setField(row, String.valueOf(current.characterRow()), excluded);
        setField(pixelLine, String.valueOf(current.pixelLine), excluded);
        setField(column, String.valueOf(current.column), excluded);
        setField(attrRow, String.valueOf(current.characterRow()), excluded);
        setField(attrCol, String.valueOf(current.column), excluded);
        setField(pixelHex, ScreenAddress.hex(current.pixelAddress()), excluded);
        setField(pixelBinary, ScreenAddress.pixelBits(current.pixelAddress()), excluded);
        setField(attrHex, ScreenAddress.hex(current.attributeAddress()), excluded);
        setField(attrBinary, ScreenAddress.attributeBits(current.attributeAddress()), excluded);
        colorBits(pixelBinary, new int[]{3, 2, 3, 3, 5},
                new int[]{0xFF737E79, C_THIRD, C_LINE, C_ROW, C_COL});
        colorBits(attrBinary, new int[]{6, 5, 5},
                new int[]{0xFF737E79, C_ROW, C_COL});
        updating = false;
        status.setText(t("Linked coordinates and addresses", "Координаты и адреса связаны"));
        status.setTextColor(0xFF39835B);
    }

    private void colorBits(EditText target, int[] widths, int[] colors) {
        Editable e = target.getText();
        ForegroundColorSpan[] old = e.getSpans(0, e.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : old) e.removeSpan(span);
        String value = e.toString();
        // Do not change text while the user is typing the binary field.
        int position = 0;
        for (int i = 0; i < widths.length; i++) {
            while (position < value.length() && value.charAt(position) == ' ') position++;
            int end = Math.min(position + widths[i], value.length());
            if (end > position) e.setSpan(new ForegroundColorSpan(colors[i]),
                    position, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            position = end;
        }
    }

    private void setField(EditText field, String value, EditText excluded) {
        if (field != excluded) field.setText(value);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(getContext());
        c.setOrientation(VERTICAL);
        c.setPadding(dp(13), dp(10), dp(13), dp(10));
        c.setBackground(UiStyle.rounded(getContext(), 0xFFFFFFFF, 16, 0xFFD4E2D8, 1));
        LayoutParams p = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(6), 0, dp(11));
        c.setLayoutParams(p);
        return c;
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        return row;
    }

    private TextView readOnlyField(LinearLayout row, String label, int color) {
        LinearLayout section = new LinearLayout(getContext());
        section.setOrientation(VERTICAL);
        section.addView(heading(label, 12, color));
        TextView value = new TextView(getContext());
        value.setText("0");
        value.setGravity(Gravity.CENTER_VERTICAL);
        value.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        value.setTextSize(19);
        value.setTextColor(color);
        value.setPadding(dp(14), 0, dp(10), 0);
        value.setBackground(UiStyle.rounded(getContext(), 0xFFE5F1E9, 12, 0xFFB6D5C1, 1));
        section.addView(value, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), 0, dp(3), dp(5));
        row.addView(section, params);
        return value;
    }

    private EditText field(LinearLayout row, String label, String hint, int color, boolean numeric) {
        LinearLayout section = new LinearLayout(getContext());
        section.setOrientation(VERTICAL);
        TextView heading = heading(label, 12, color);
        section.addView(heading);
        EditText field = input(section, hint, numeric);
        LayoutParams p = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(3), 0, dp(3), dp(5));
        row.addView(section, p);
        return field;
    }

    private EditText input(LinearLayout box, String hint, boolean numeric) {
        EditText f = new EditText(getContext());
        f.setSingleLine(true);
        f.setTypeface(Typeface.MONOSPACE);
        f.setTextSize(16);
        f.setHint(hint);
        f.setTextColor(0xFF263B2D);
        f.setSelectAllOnFocus(true);
        f.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        box.addView(f, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        return f;
    }

    private TextView heading(String title, int size, int color) {
        TextView v = new TextView(getContext());
        v.setText(title);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private TextView description(String title) {
        TextView v = new TextView(getContext());
        v.setText(title);
        v.setTextSize(12);
        v.setTextColor(0xFF697B6F);
        v.setPadding(dp(2), dp(4), dp(2), dp(6));
        return v;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private String t(String en, String russian) { return ru ? russian : en; }
}
