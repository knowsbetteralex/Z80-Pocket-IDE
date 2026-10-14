package com.alexzab.z80pocketide.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.alexzab.z80pocketide.ui.UiStyle;

/** Draw and erase by touch; palette changes recolor the entire sprite. */
public final class SpriteToolPanel extends LinearLayout {
    public interface InsertAction { void insert(String source); }

    private enum ColorTarget { INK, PAPER }

    private static final Integer[] SIZES = {8, 16, 24, 32, 48, 64};
    private final boolean ru;
    private final InsertAction insertAction;
    private final SpriteCanvasView canvas;
    private SpriteData sprite;
    private ColorTarget colorTarget = ColorTarget.INK;
    private final Button inkButton;
    private final Button paperButton;
    private final View[] swatches = new View[16];
    private final Spinner widthChooser, heightChooser;
    private final TextView bitmapPreview, attrPreview;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayedPreview = this::refreshExport;
    private boolean exportBinary;
    private boolean sizing;

    public SpriteToolPanel(Context context, boolean ru, InsertAction insertAction) {
        super(context);
        this.ru = ru;
        this.insertAction = insertAction;
        sprite = new SpriteData(16, 16);
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(8), dp(2), dp(18));

        addView(title(t("Spectrum sprite editor", "Редактор спрайтов Spectrum"), 19));
        addView(detail(t("Tap an empty pixel to paint INK, tap a filled pixel to erase. "
                        + "Drag to continue the same stroke.",
                "Касание пустого пикселя рисует INK, закрашенного — стирает. "
                        + "Скольжение продолжает то же действие.")));

        LinearLayout dimensions = row();
        widthChooser = spinner();
        heightChooser = spinner();
        dimensions.addView(labeledSpinner(t("Width (px)", "Ширина (пкс)"), widthChooser));
        dimensions.addView(labeledSpinner(t("Height (px)", "Высота (пкс)"), heightChooser));
        addView(dimensions);

        Button fit = greenButton(t("Fit to height ↕", "Вписать по высоте ↕"));
        LayoutParams fitParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(42));
        fitParams.setMargins(0, dp(6), 0, dp(7));
        addView(fit, fitParams);

        canvas = new SpriteCanvasView(context, sprite);
        LayoutParams canvasParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(315));
        canvasParams.setMargins(0, 0, 0, dp(8));
        addView(canvas, canvasParams);
        fit.setOnClickListener(v -> canvas.fitHeight());
        canvas.setOnChange(this::schedulePreview);

        addView(detail(t("Two fingers: move and pinch to zoom. The image may extend outside the canvas.",
                "Два пальца: перемещение и масштабирование. Изображение может выходить за область рисования.")));

        addView(title(t("Colours · INK / PAPER", "Цвета · INK / PAPER"), 16));
        LinearLayout targets = row();
        inkButton = smallButton("INK");
        paperButton = smallButton("PAPER");
        targets.addView(inkButton, new LayoutParams(0, dp(46), 1f));
        targets.addView(paperButton, new LayoutParams(0, dp(46), 1f));
        addView(targets);

        inkButton.setOnClickListener(v -> {
            colorTarget = ColorTarget.INK;
            refreshPalette();
        });
        paperButton.setOnClickListener(v -> {
            colorTarget = ColorTarget.PAPER;
            refreshPalette();
        });

        addView(detail(t("Select INK or PAPER, then tap a palette colour. "
                        + "All existing pixels update immediately.",
                "Выберите INK или PAPER, затем цвет в палитре. "
                        + "Весь рисунок перекрашивается сразу.")));

        for (int brightRow = 0; brightRow < 2; brightRow++) {
            LinearLayout paletteRow = row();
            for (int base = 0; base < 8; base++) {
                int index = brightRow * 8 + base;
                TextView swatch = new TextView(context);
                swatch.setGravity(Gravity.CENTER);
                swatch.setTextSize(16);
                swatches[index] = swatch;
                LayoutParams buttonParams = new LayoutParams(0, dp(39), 1f);
                buttonParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                paletteRow.addView(swatch, buttonParams);
                swatch.setOnClickListener(v -> chooseColor(index));
            }
            addView(paletteRow);
        }

        addView(detail(t("ZX Spectrum has one BRIGHT bit per 8×8 tile, shared by INK and PAPER. "
                        + "Choosing a normal/bright swatch changes brightness of both.",
                "У Spectrum один бит BRIGHT на знакоместо 8×8, общий для INK и PAPER. "
                        + "Обычная/яркая палитра меняет яркость обоих цветов.")));

        LinearLayout options = row();
        Button format = smallButton("HEX ⇄ BIN");
        Button clear = smallButton(t("Clear pixels", "Очистить пиксели"));
        options.addView(format, new LayoutParams(0, dp(44), 1f));
        options.addView(clear, new LayoutParams(0, dp(44), 1f));
        addView(options);
        format.setOnClickListener(v -> {
            exportBinary = !exportBinary;
            refreshExport();
        });
        clear.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle(t("Clear all drawn pixels?", "Очистить все нарисованные пиксели?"))
                .setMessage(t("INK and PAPER colours will be preserved.",
                        "Цвета INK и PAPER сохранятся."))
                .setPositiveButton(t("Clear", "Очистить"), (d, w) -> {
                    sprite.clearPixels();
                    canvas.invalidate();
                    refreshExport();
                })
                .setNegativeButton(t("Cancel", "Отмена"), null).show());

        addView(title(t("Bitmap bytes · DB", "Байты bitmap · DB"), 15));
        bitmapPreview = codePreview();
        addView(bitmapPreview);
        Button insertBitmap = greenButton(t("Insert bitmap DB at cursor",
                "Вставить DB пикселей в код"));
        addView(insertBitmap, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        insertBitmap.setOnClickListener(v -> insertAction.insert(sprite.dbPixels(exportBinary)));

        addView(title(t("8×8 attribute bytes · DB", "Байты атрибутов 8×8 · DB"), 15));
        attrPreview = codePreview();
        addView(attrPreview);
        Button insertAttrs = greenButton(t("Insert attribute DB at cursor",
                "Вставить DB атрибутов в код"));
        addView(insertAttrs, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));
        insertAttrs.setOnClickListener(v -> insertAction.insert(sprite.dbAttributes(exportBinary)));

        addView(detail(t("Sprite bytes are exported row-by-row, not in the Spectrum screen's "
                        + "interleaved memory order. Attribute bytes follow 8×8 cell order.",
                "Байты спрайта выводятся по строкам, а не в чередующемся порядке экранной "
                        + "памяти Spectrum. Атрибуты идут по знакоместам 8×8.")));

        widthChooser.setSelection(1);
        heightChooser.setSelection(1);
        bindSize(widthChooser);
        bindSize(heightChooser);
        refreshPalette();
        refreshExport();
    }

    private void chooseColor(int index) {
        if (colorTarget == ColorTarget.INK) sprite.setInkColor(index);
        else sprite.setPaperColor(index);
        canvas.invalidate();
        refreshPalette();
        refreshExport(); // Attribute DB also changes immediately.
    }

    private void refreshPalette() {
        inkButton.setText("INK · " + sprite.getInkColor());
        paperButton.setText("PAPER · " + sprite.getPaperColor());
        if (colorTarget == ColorTarget.INK) {
            UiStyle.styleGreenButton(inkButton);
            UiStyle.styleSoftGreenButton(paperButton);
        } else {
            UiStyle.styleSoftGreenButton(inkButton);
            UiStyle.styleGreenButton(paperButton);
        }

        int selected = colorTarget == ColorTarget.INK
                ? sprite.getInkColor() : sprite.getPaperColor();
        for (int i = 0; i < 16; i++) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(SpriteData.palette(i));
            background.setCornerRadius(dp(7));
            background.setStroke(dp(i == selected ? 3 : 1),
                    i == selected ? 0xFFFFAD21 : 0xFF82988A);
            swatches[i].setBackground(background);
            if (swatches[i] instanceof TextView) {
                TextView view = (TextView)swatches[i];
                view.setText(i == selected ? "✓" : "");
                view.setTextColor((i & 7) == 0 || (i & 7) == 1 || (i & 7) == 2
                        ? Color.WHITE : Color.BLACK);
            }
        }
    }

    /** Throttle only text formatting; pixels themselves repaint on every touch event. */
    private void schedulePreview() {
        handler.removeCallbacks(delayedPreview);
        handler.postDelayed(delayedPreview, 110);
    }

    private void refreshExport() {
        handler.removeCallbacks(delayedPreview);
        bitmapPreview.setText(sprite.dbPixels(exportBinary));
        attrPreview.setText(sprite.dbAttributes(exportBinary));
    }

    private void bindSize(Spinner chooser) {
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (sizing) return;
                int width = SIZES[widthChooser.getSelectedItemPosition()];
                int height = SIZES[heightChooser.getSelectedItemPosition()];
                if (width == sprite.width() && height == sprite.height()) return;
                new AlertDialog.Builder(getContext())
                        .setTitle(t("Resize sprite?", "Изменить размер спрайта?"))
                        .setMessage(t("Resizing clears pixels but keeps your colour choices.",
                                "Изменение размера очистит пиксели, но сохранит выбранные цвета."))
                        .setPositiveButton(t("Resize", "Изменить"), (dialog, which) -> {
                            int ink = sprite.getInkColor();
                            int paper = sprite.getPaperColor();
                            sprite = new SpriteData(width, height);
                            sprite.setInkColor(ink);
                            sprite.setPaperColor(paper);
                            canvas.setModel(sprite);
                            refreshPalette();
                            refreshExport();
                        })
                        .setNegativeButton(t("Cancel", "Отмена"), (dialog, which) -> {
                            sizing = true;
                            widthChooser.setSelection(index(sprite.width()));
                            heightChooser.setSelection(index(sprite.height()));
                            sizing = false;
                        }).show();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int index(int size) {
        for (int i = 0; i < SIZES.length; i++) if (SIZES[i] == size) return i;
        return 1;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private Spinner spinner() {
        Spinner spinner = new Spinner(getContext());
        spinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, SIZES));
        return spinner;
    }

    private LinearLayout labeledSpinner(String label, Spinner spinner) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(VERTICAL);
        layout.addView(detail(label));
        layout.addView(spinner);
        layout.setPadding(dp(3), 0, dp(3), 0);
        layout.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return layout;
    }

    private Button smallButton(String title) {
        Button button = new Button(getContext());
        button.setText(title);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        UiStyle.styleSoftGreenButton(button);
        return button;
    }

    private Button greenButton(String title) {
        Button button = new Button(getContext());
        button.setText(title);
        UiStyle.styleGreenButton(button);
        return button;
    }

    private TextView title(String value, int sp) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(0xFF2D6344);
        view.setPadding(dp(2), dp(10), dp(2), dp(5));
        return view;
    }

    private TextView detail(String value) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(12);
        view.setTextColor(0xFF68796D);
        view.setPadding(dp(2), dp(4), dp(2), dp(6));
        return view;
    }

    private TextView codePreview() {
        TextView view = new TextView(getContext());
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(12);
        view.setTextColor(0xFF243D2D);
        view.setTextIsSelectable(true);
        view.setPadding(dp(11), dp(10), dp(11), dp(10));
        view.setBackground(UiStyle.rounded(getContext(), Color.WHITE, 14, 0xFFC7DDCD, 1));
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(5), 0, dp(8));
        view.setLayoutParams(params);
        return view;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private String t(String en, String russian) { return ru ? russian : en; }
}
