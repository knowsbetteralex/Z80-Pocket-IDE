package com.alexzab.z80pocketide.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.Texts;

public final class EditorSettingsDialog {
    private EditorSettingsDialog() {}

    public static void show(Activity activity, AppLanguage language, Runnable onChanged) {
        EditorPreferences.Snapshot current = EditorPreferences.get(activity);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 20), dp(activity, 8), dp(activity, 20), 0);

        CheckBox upper = check(activity, t(language, "Uppercase Z80 keywords while typing",
                "Ключевые слова Z80 заглавными при наборе"), current.autoUppercase);
        CheckBox allUpper = check(activity, t(language,
                "UPPERCASE ALL text while typing (including strings/comments)",
                "ВЕСЬ текст заглавными при наборе (включая строки/комментарии)"),
                current.allUppercase);
        CheckBox indent = check(activity, t(language, "Automatic indentation",
                "Автоматические отступы"), current.autoIndent);
        CheckBox spaces = check(activity, t(language, "Use spaces instead of TAB character",
                "Использовать пробелы вместо символа TAB"), current.useSpaces);
        CheckBox tabButton = check(activity, t(language, "Show TAB button above editor",
                "Показывать кнопку TAB над редактором"), current.showTabButton);
        CheckBox folding = check(activity, t(language, "Enable label block folding",
                "Разрешить сворачивание блоков под метками"), current.folding);

        box.addView(upper);
        box.addView(allUpper);
        box.addView(indent);
        box.addView(spaces);

        TextView widthTitle = new TextView(activity);
        widthTitle.setText(t(language, "Tab width", "Ширина табуляции"));
        widthTitle.setTextSize(14);
        widthTitle.setTextColor(Color.rgb(60, 60, 60));
        widthTitle.setPadding(0, dp(activity, 8), 0, 0);
        box.addView(widthTitle);

        RadioGroup widths = new RadioGroup(activity);
        widths.setOrientation(RadioGroup.HORIZONTAL);
        int[] values = {2, 4, 8};
        for (int value : values) {
            RadioButton radio = new RadioButton(activity);
            radio.setId(value);
            radio.setText(String.valueOf(value));
            radio.setChecked(current.tabSize == value);
            widths.addView(radio, new RadioGroup.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(widths);
        box.addView(tabButton);
        box.addView(folding);

        new AlertDialog.Builder(activity)
                .setTitle(t(language, "Editor settings", "Настройки редактора"))
                .setView(box)
                .setPositiveButton(t(language, "Apply", "Применить"), (dialog, which) -> {
                    int checked = widths.getCheckedRadioButtonId();
                    int tabSize = checked == 2 || checked == 8 ? checked : 4;
                    EditorPreferences.set(activity, new EditorPreferences.Snapshot(
                            upper.isChecked(), allUpper.isChecked(), indent.isChecked(), tabSize,
                            spaces.isChecked(), tabButton.isChecked(), folding.isChecked()));
                    if (onChanged != null) onChanged.run();
                })
                .setNegativeButton(t(language, "Cancel", "Отмена"), null)
                .show();
    }

    private static CheckBox check(Activity activity, String text, boolean checked) {
        CheckBox box = new CheckBox(activity);
        box.setText(text);
        box.setChecked(checked);
        box.setPadding(0, dp(activity, 3), 0, dp(activity, 3));
        return box;
    }

    private static String t(AppLanguage language, String en, String ru) {
        return Texts.pick(language, en, ru);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
