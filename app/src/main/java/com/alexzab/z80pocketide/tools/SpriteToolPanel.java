package com.alexzab.z80pocketide.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
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

/**
 * Volatile sprite workspace: bitmap + separate opaque-by-default mask,
 * lossless canvas resizing and clipped pixel shift.
 */
public final class SpriteToolPanel extends LinearLayout {
    public interface InsertAction { void insert(String source); }

    private static final Integer[] SIZES = {8, 16, 24, 32, 48, 64};
    private final boolean ru;
    private final InsertAction insertAction;
    private final SpriteData sprite;
    private final SpriteCanvasView canvas;
    private final Button bitmapLayerButton, maskLayerButton;
    private final Button inkButton, paperButton;
    private final View[] swatches = new View[16];
    private final Spinner widthChooser, heightChooser;
    private final SpriteDbField bitmapPreview, maskPreview, attrPreview;
    private final TextView offsetLabel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayedPreview = this::refreshExport;
    private boolean sizing;
    private final String suggestedImport;

    public SpriteToolPanel(Context context, boolean ru, InsertAction insertAction) {
        this(context, ru, insertAction, null);
    }

    public SpriteToolPanel(Context context, boolean ru, InsertAction insertAction,
                           String selectedSource) {
        super(context);
        suggestedImport = selectedSource == null ? "" : selectedSource;
        this.ru = ru;
        this.insertAction = insertAction;
        sprite = SpriteSession.sprite();
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(8), dp(2), dp(18));

        addView(title(t("Spectrum sprite editor", "Редактор спрайтов Spectrum"), 19));
        addView(detail(t("Canvas starts locked. Tap the padlock to paint; locked mode allows one-finger pan and two-finger pinch.",
                "По умолчанию холст заблокирован. Замочек разрешает рисование; при блокировке один палец двигает, два масштабируют.")));

        LinearLayout dimensions = row();
        widthChooser = spinner();
        heightChooser = spinner();
        dimensions.addView(labeledSpinner(t("Width (px)", "Ширина (пкс)"), widthChooser));
        dimensions.addView(labeledSpinner(t("Height (px)", "Высота (пкс)"), heightChooser));
        addView(dimensions);

        addView(title(t("Layer", "Слой"), 15));
        LinearLayout layers = row();
        bitmapLayerButton = smallButton(t("● Sprite", "● Рисунок"));
        maskLayerButton = smallButton(t("◩ Mask", "◩ Маска"));
        layers.addView(bitmapLayerButton, new LayoutParams(0, dp(43), 1f));
        layers.addView(maskLayerButton, new LayoutParams(0, dp(43), 1f));
        addView(layers);

        bitmapLayerButton.setOnClickListener(v -> {
            SpriteSession.setMaskEditing(false);
            refreshLayer();
        });
        maskLayerButton.setOnClickListener(v -> {
            SpriteSession.setMaskEditing(true);
            refreshLayer();
        });

        Button fit = greenButton(t("Fit to height ↕", "Вписать по высоте ↕"));
        LayoutParams fitParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(41));
        fitParams.setMargins(0, dp(5), 0, dp(7));
        addView(fit, fitParams);

        canvas = new SpriteCanvasView(context, sprite);
        FrameLayout canvasHolder = new FrameLayout(context);
        LayoutParams canvasParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(315));
        canvasParams.setMargins(0, 0, 0, dp(7));
        addView(canvasHolder, canvasParams);
        canvasHolder.addView(canvas, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ImageButton padlock = new ImageButton(context);
        padlock.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        padlock.setPadding(dp(9), dp(9), dp(9), dp(9));
        padlock.setElevation(dp(3));
        padlock.setBackground(UiStyle.rounded(context, 0xFF2D8052, 13, 0xFF266744, 1));
        FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(
                dp(43), dp(43), Gravity.TOP | Gravity.RIGHT);
        lockParams.setMargins(0, dp(8), dp(8), 0);
        canvasHolder.addView(padlock, lockParams);
        canvas.setLocked(SpriteSession.canvasLocked());
        updatePadlock(padlock);
        padlock.setOnClickListener(v -> {
            boolean next = !SpriteSession.canvasLocked();
            SpriteSession.setCanvasLocked(next);
            canvas.setLocked(next);
            updatePadlock(padlock);
        });
        fit.setOnClickListener(v -> canvas.fitHeight());
        canvas.setOnChange(this::schedulePreview);

        addView(detail(t("Mask starts at all 1s (visible). Tap on a visible mask pixel to "
                        + "make a checkerboard hole. Tap a hole to restore it.",
                "Маска изначально вся из единиц (пиксели видны). Касание стирает бит в шахматку; "
                        + "повторное касание возвращает его.")));

        addView(title(t("Shift drawing inside grid (1 pixel)", "Сместить рисунок в сетке (1 пиксель)"), 15));
        LinearLayout movement = row();
        Button left = smallButton("←");
        Button up = smallButton("↑");
        Button down = smallButton("↓");
        Button right = smallButton("→");
        Button center = smallButton("0");
        offsetLabel = new TextView(context);
        offsetLabel.setGravity(Gravity.CENTER);
        offsetLabel.setTextColor(0xFF356548);
        offsetLabel.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        offsetLabel.setTextSize(12);
        movement.addView(left, new LayoutParams(0, dp(40), 1f));
        movement.addView(up, new LayoutParams(0, dp(40), 1f));
        movement.addView(down, new LayoutParams(0, dp(40), 1f));
        movement.addView(right, new LayoutParams(0, dp(40), 1f));
        movement.addView(offsetLabel, new LayoutParams(0, dp(40), 2.2f));
        movement.addView(center, new LayoutParams(0, dp(40), 1f));
        addView(movement);
        left.setOnClickListener(v -> shift(-1, 0));
        right.setOnClickListener(v -> shift(1, 0));
        up.setOnClickListener(v -> shift(0, -1));
        down.setOnClickListener(v -> shift(0, 1));
        center.setOnClickListener(v -> {
            sprite.resetOffset();
            refreshProjection();
        });
        updateOffsets();

        addView(detail(t("Shifting and resizing clip only the preview/export; hidden pixels return "
                        + "when you move back or enlarge the canvas.",
                "Смещение и изменение размера лишь обрезают видимую область/экспорт. "
                        + "Скрытые пиксели вернутся при обратном смещении или увеличении холста.")));

        addView(title(t("INK / PAPER", "Цвета INK / PAPER"), 16));
        LinearLayout colors = row();
        inkButton = smallButton("INK");
        paperButton = smallButton("PAPER");
        colors.addView(inkButton, new LayoutParams(0, dp(43), 1f));
        colors.addView(paperButton, new LayoutParams(0, dp(43), 1f));
        addView(colors);

        inkButton.setOnClickListener(v -> {
            SpriteSession.setPaperTarget(false);
            refreshPalette();
        });
        paperButton.setOnClickListener(v -> {
            SpriteSession.setPaperTarget(true);
            refreshPalette();
        });

        for (int brightRow = 0; brightRow < 2; brightRow++) {
            LinearLayout paletteRow = row();
            for (int base = 0; base < 8; base++) {
                int index = brightRow * 8 + base;
                TextView swatch = new TextView(context);
                swatch.setGravity(Gravity.CENTER);
                swatch.setTextSize(16);
                swatches[index] = swatch;
                LayoutParams params = new LayoutParams(0, dp(38), 1f);
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                paletteRow.addView(swatch, params);
                swatch.setOnClickListener(v -> chooseColor(index));
            }
            addView(paletteRow);
        }
        addView(detail(t("Hardware BRIGHT is shared by INK and PAPER on each 8×8 tile.",
                "Аппаратный BRIGHT общий для INK и PAPER каждого знакоместа 8×8.")));

        LinearLayout options = row();
        Button format = smallButton("HEX ⇄ BIN");
        Button clear = smallButton(t("Clear pixels", "Очистить рисунок"));
        options.addView(format, new LayoutParams(0, dp(43), 1f));
        options.addView(clear, new LayoutParams(0, dp(43), 1f));
        addView(options);
        format.setOnClickListener(v -> {
            SpriteSession.setBinaryExport(!SpriteSession.binaryExport());
            refreshExport();
        });
        clear.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                .setTitle(t("Clear all drawn pixels?", "Очистить все нарисованные пиксели?"))
                .setMessage(t("INK, PAPER and mask are preserved.",
                        "Цвета INK, PAPER и маска сохранятся."))
                .setPositiveButton(t("Clear", "Очистить"), (d, w) -> {
                    sprite.clearPixels();
                    canvas.invalidate();
                    refreshExport();
                })
                .setNegativeButton(t("Cancel", "Отмена"), null).show());

        LinearLayout maskActions = row();
        Button resetMask = smallButton(t("Mask → all 1", "Маска → все 1"));
        Button invertMask = smallButton(t("Invert mask", "Инвертировать маску"));
        maskActions.addView(resetMask, new LayoutParams(0, dp(43), 1f));
        maskActions.addView(invertMask, new LayoutParams(0, dp(43), 1f));
        addView(maskActions);
        resetMask.setOnClickListener(v -> {
            sprite.resetMask();
            canvas.invalidate();
            refreshExport();
        });
        invertMask.setOnClickListener(v -> {
            sprite.invertMask();
            canvas.invalidate();
            refreshExport();
        });

        addView(title(t("Bitmap · DB", "Рисунок · DB"), 15));
        bitmapPreview = dbField(0);
        addView(bitmapPreview);
        LinearLayout bitmapActions = row();
        Button importBitmap = smallButton(t("Load DB into sprite", "Загрузить DB в рисунок"));
        Button insertBitmap = greenButton(t("Insert sprite DB", "Вставить DB рисунка"));
        bitmapActions.addView(importBitmap, new LayoutParams(0, dp(46), 1f));
        bitmapActions.addView(insertBitmap, new LayoutParams(0, dp(46), 1f));
        addView(bitmapActions);
        importBitmap.setOnClickListener(v -> showImportDialog(0));
        insertBitmap.setOnClickListener(v ->
                insertAction.insert(sprite.dbPixels(SpriteSession.binaryExport())));

        addView(title(t("Mask · DB (1 = visible)", "Маска · DB (1 = видно)"), 15));
        maskPreview = dbField(1);
        addView(maskPreview);
        LinearLayout maskDbActions = row();
        Button importMask = smallButton(t("Load DB into mask", "Загрузить DB в маску"));
        Button insertMask = greenButton(t("Insert mask DB", "Вставить DB маски"));
        maskDbActions.addView(importMask, new LayoutParams(0, dp(46), 1f));
        maskDbActions.addView(insertMask, new LayoutParams(0, dp(46), 1f));
        addView(maskDbActions);
        importMask.setOnClickListener(v -> showImportDialog(1));
        insertMask.setOnClickListener(v ->
                insertAction.insert(sprite.dbMask(SpriteSession.binaryExport())));

        addView(title(t("Attributes 8×8 · DB", "Атрибуты 8×8 · DB"), 15));
        attrPreview = dbField(2);
        addView(attrPreview);
        LinearLayout attributeActions = row();
        Button importAttrs = smallButton(t("Load attribute DB", "Загрузить DB атрибутов"));
        Button insertAttrs = greenButton(t("Insert attribute DB", "Вставить DB атрибутов"));
        attributeActions.addView(importAttrs, new LayoutParams(0, dp(46), 1f));
        attributeActions.addView(insertAttrs, new LayoutParams(0, dp(46), 1f));
        addView(attributeActions);
        importAttrs.setOnClickListener(v -> showImportDialog(2));
        insertAttrs.setOnClickListener(v ->
                insertAction.insert(sprite.dbAttributes(SpriteSession.binaryExport())));

        addView(detail(t("Sprite and mask bytes are exported in visible row-major order, after "
                        + "offset/cropping. Exported mask bit 1 means visible, 0 means transparent. "
                        + "Convert polarity if your blitter uses an AND-clear mask.",
                "Рисунок и маска экспортируются построчно из видимой области с учётом смещения. "
                        + "Маска: 1 — видно, 0 — прозрачно. Для AND-маски очистки биты надо инвертировать.")));

        sizing = true;
        widthChooser.setSelection(index(sprite.width()));
        heightChooser.setSelection(index(sprite.height()));
        sizing = false;
        bindSize(widthChooser);
        bindSize(heightChooser);
        refreshLayer();
        refreshPalette();
        refreshExport();
    }

    /**
     * Load bytes from the clipboard, a copied DB listing, or a selection made
     * in the main ASM editor before opening Tools. Reject incomplete data
     * without partially overwriting the current sprite.
     */
    private void showImportDialog(int kind) {
        final int expected = kind == 2 ? sprite.cellsX() * sprite.cellsY()
                : sprite.cellsX() * sprite.height();
        String layer = kind == 0 ? t("sprite", "рисунка")
                : kind == 1 ? t("mask", "маски") : t("attributes", "атрибутов");

        LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(VERTICAL);
        wrapper.setPadding(dp(17), dp(4), dp(17), dp(2));
        TextView help = detail((ru ? "Байтов: " : "Required bytes: ") + expected
                + t(". Supports DB $3C,%00111100, 0x3C, decimal and DEFB.",
                ". Форматы: DB $3C,%00111100, 0x3C, десятичный, DEFB."));
        wrapper.addView(help);
        EditText source = new EditText(getContext());
        source.setGravity(Gravity.TOP | Gravity.START);
        source.setTypeface(Typeface.MONOSPACE);
        source.setTextSize(13);
        source.setMinLines(5);
        source.setMaxLines(11);
        source.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        source.setHint("    DB $3C,$42,$81,$FF");
        source.setText(suggestedImport);
        source.setSelectAllOnFocus(false);
        wrapper.addView(source, new LayoutParams(LayoutParams.MATCH_PARENT, dp(195)));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(t("Import DB: ", "Импорт DB: ") + layer)
                .setView(wrapper)
                .setPositiveButton(t("Import", "Загрузить"), null)
                .setNegativeButton(t("Cancel", "Отмена"), null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    int[] bytes = SpriteDbParser.parse(source.getText().toString());
                    if (bytes.length != expected)
                        throw new IllegalArgumentException((ru ? "Ожидается " : "Expected ")
                                + expected + (ru ? " байт, получено " : " bytes, got ")
                                + bytes.length);
                    if (kind == 0) sprite.loadBitmap(bytes);
                    else if (kind == 1) sprite.loadMask(bytes);
                    else {
                        sprite.loadAttributes(bytes);
                        refreshPalette();
                    }
                    canvas.invalidate();
                    refreshExport();
                    Toast.makeText(getContext(),
                            t("Imported ", "Загружено байт: ") + bytes.length,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (IllegalArgumentException problem) {
                    // Keep the dialog open so the user can fix byte count/syntax.
                    source.setError(problem.getMessage());
                }
            });
        });
        dialog.show();
    }

    private void updatePadlock(ImageButton button) {
        boolean locked = SpriteSession.canvasLocked();
        button.setImageResource(locked
                ? com.alexzab.z80pocketide.R.drawable.ic_sprite_locked
                : com.alexzab.z80pocketide.R.drawable.ic_sprite_unlocked);
        button.setContentDescription(locked
                ? t("Unlock sprite canvas for drawing", "Разблокировать холст для рисования")
                : t("Lock sprite canvas", "Заблокировать рисование"));
    }

    /** DB edits stay in a temporary input buffer until explicit OK. */
    private SpriteDbField dbField(int kind) {
        return new SpriteDbField(getContext(), ru, source -> {
            int[] bytes = SpriteDbParser.parse(source);
            int expected = kind == 2 ? sprite.cellsX() * sprite.cellsY()
                    : sprite.cellsX() * sprite.height();
            if (bytes.length != expected)
                throw new IllegalArgumentException(
                        (ru ? "Ожидается " : "Expected ") + expected
                                + (ru ? " байт, получено " : " bytes, got ") + bytes.length);
            if (kind == 0) sprite.loadBitmap(bytes);
            else if (kind == 1) sprite.loadMask(bytes);
            else sprite.loadAttributes(bytes);
        }, () -> {
            canvas.invalidate();
            refreshPalette();
            refreshExport();
        });
    }

    private void refreshLayer() {
        boolean mask = SpriteSession.maskEditing();
        canvas.setMaskMode(mask);
        if (mask) {
            UiStyle.styleSoftGreenButton(bitmapLayerButton);
            UiStyle.styleGreenButton(maskLayerButton);
        } else {
            UiStyle.styleGreenButton(bitmapLayerButton);
            UiStyle.styleSoftGreenButton(maskLayerButton);
        }
    }

    private void shift(int dx, int dy) {
        sprite.moveBy(dx, dy);
        refreshProjection();
    }

    private void refreshProjection() {
        updateOffsets();
        canvas.invalidate();
        refreshExport();
    }

    private void updateOffsets() {
        offsetLabel.setText("X:" + sprite.offsetX() + "\nY:" + sprite.offsetY());
    }

    private void chooseColor(int index) {
        if (SpriteSession.paperTarget()) sprite.setPaperColor(index);
        else sprite.setInkColor(index);
        canvas.invalidate();
        refreshPalette();
        refreshExport();
    }

    private void refreshPalette() {
        inkButton.setText("INK · " + sprite.getInkColor());
        paperButton.setText("PAPER · " + sprite.getPaperColor());
        if (SpriteSession.paperTarget()) {
            UiStyle.styleSoftGreenButton(inkButton);
            UiStyle.styleGreenButton(paperButton);
        } else {
            UiStyle.styleGreenButton(inkButton);
            UiStyle.styleSoftGreenButton(paperButton);
        }
        int selected = SpriteSession.paperTarget() ? sprite.getPaperColor() : sprite.getInkColor();
        for (int i = 0; i < 16; i++) {
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(SpriteData.palette(i));
            shape.setCornerRadius(dp(7));
            shape.setStroke(dp(i == selected ? 3 : 1),
                    i == selected ? 0xFFFFAD21 : 0xFF82988A);
            swatches[i].setBackground(shape);
            TextView swatch = (TextView) swatches[i];
            swatch.setText(i == selected ? "✓" : "");
            swatch.setTextColor((i & 7) == 0 || (i & 7) == 1 || (i & 7) == 2
                    ? Color.WHITE : Color.BLACK);
        }
    }

    private void schedulePreview() {
        handler.removeCallbacks(delayedPreview);
        handler.postDelayed(delayedPreview, 110);
    }

    private void refreshExport() {
        handler.removeCallbacks(delayedPreview);
        bitmapPreview.setCode(sprite.dbPixels(SpriteSession.binaryExport()));
        maskPreview.setCode(sprite.dbMask(SpriteSession.binaryExport()));
        attrPreview.setCode(sprite.dbAttributes(SpriteSession.binaryExport()));
    }

    private void bindSize(Spinner field) {
        field.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (sizing) return;
                int width = SIZES[widthChooser.getSelectedItemPosition()];
                int height = SIZES[heightChooser.getSelectedItemPosition()];
                if (width == sprite.width() && height == sprite.height()) return;
                sprite.resize(width, height);
                canvas.fitHeight();
                refreshExport();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override protected void onDetachedFromWindow() {
        handler.removeCallbacks(delayedPreview);
        super.onDetachedFromWindow();
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
        Spinner view = new Spinner(getContext());
        view.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, SIZES));
        return view;
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

    private Button smallButton(String label) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setPadding(dp(3), 0, dp(3), 0);
        button.setAllCaps(false);
        UiStyle.styleSoftGreenButton(button);
        return button;
    }

    private Button greenButton(String label) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setAllCaps(false);
        UiStyle.styleGreenButton(button);
        return button;
    }

    private TextView title(String label, int sp) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(0xFF2D6344);
        view.setPadding(dp(2), dp(10), dp(2), dp(5));
        return view;
    }

    private TextView detail(String text) {
        TextView view = new TextView(getContext());
        view.setText(text);
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

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private String t(String en, String russian) { return ru ? russian : en; }
}
