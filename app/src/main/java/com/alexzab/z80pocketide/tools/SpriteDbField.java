package com.alexzab.z80pocketide.tools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.R;
import com.alexzab.z80pocketide.ui.UiStyle;

/**
 * DB preview with a small pencil in its upper-right corner. During editing
 * a text buffer replaces the preview; only OK validates and applies it.
 * Cancel never changes the underlying bitmap, mask or attributes.
 */
public final class SpriteDbField extends LinearLayout {
    public interface Apply { void apply(String code) throws IllegalArgumentException; }

    private final TextView preview;
    private final EditText editor;
    private final ImageButton editButton;
    private final LinearLayout editActions;
    private final Apply apply;
    private final Runnable onCommitted;
    private boolean editing;

    public SpriteDbField(Context context, boolean ru, Apply apply, Runnable onCommitted) {
        super(context);
        this.apply = apply;
        this.onCommitted = onCommitted;
        setOrientation(VERTICAL);

        FrameLayout frame = new FrameLayout(context);
        frame.setBackground(UiStyle.rounded(context, Color.WHITE, 14, 0xFFC7DDCD, 1));
        LayoutParams frameParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        frameParams.setMargins(0, dp(5), 0, dp(5));
        addView(frame, frameParams);

        preview = new TextView(context);
        preview.setTypeface(Typeface.MONOSPACE);
        preview.setTextSize(12);
        preview.setTextColor(0xFF243D2D);
        preview.setTextIsSelectable(true);
        preview.setPadding(dp(11), dp(12), dp(43), dp(10));
        frame.addView(preview, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        editor = new EditText(context);
        editor.setTag("sprite-db-edit");
        editor.setTextSize(12);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextColor(0xFF243D2D);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setHorizontallyScrolling(false);
        editor.setPadding(dp(11), dp(9), dp(11), dp(9));
        editor.setBackgroundColor(Color.TRANSPARENT);
        editor.setVisibility(GONE);
        frame.addView(editor, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(214)));

        editButton = new ImageButton(context);
        editButton.setImageResource(R.drawable.ic_sprite_edit);
        editButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        editButton.setContentDescription(ru ? "Редактировать байты DB" : "Edit DB bytes");
        editButton.setPadding(dp(6), dp(6), dp(6), dp(6));
        editButton.setBackground(UiStyle.ripple(context, 0xFFE3F1E8, 10, 0xFFC2DCC9));
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                dp(34), dp(34), Gravity.TOP | Gravity.RIGHT);
        iconParams.setMargins(0, dp(4), dp(4), 0);
        frame.addView(editButton, iconParams);
        editButton.setOnClickListener(v -> enterEdit());

        editActions = new LinearLayout(context);
        editActions.setOrientation(HORIZONTAL);
        editActions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        editActions.setVisibility(GONE);
        Button cancel = new Button(context);
        cancel.setText("✕");
        cancel.setContentDescription(ru ? "Отменить изменения DB" : "Cancel DB changes");
        cancel.setAllCaps(false);
        cancel.setMinWidth(0);
        UiStyle.styleSoftGreenButton(cancel);
        Button ok = new Button(context);
        ok.setText("✓ OK");
        ok.setAllCaps(false);
        ok.setMinWidth(0);
        ok.setContentDescription(ru ? "Проверить и применить DB" : "Validate and apply DB");
        UiStyle.styleGreenButton(ok);
        editActions.addView(cancel, new LayoutParams(dp(70), dp(42)));
        editActions.addView(ok, new LayoutParams(dp(100), dp(42)));
        addView(editActions);

        cancel.setOnClickListener(v -> exitEdit(false));
        ok.setOnClickListener(v -> {
            try {
                apply.apply(editor.getText().toString());
                exitEdit(true);
                onCommitted.run();
            } catch (IllegalArgumentException error) {
                // Keep the user's working buffer and edit mode on failure.
                editor.setError(error.getMessage());
            }
        });
    }

    public void setCode(String code) {
        preview.setText(code);
    }

    public String getCode() { return preview.getText().toString(); }
    public boolean isEditing() { return editing; }

    private void enterEdit() {
        if (editing) return;
        editing = true;
        editor.setText(preview.getText());
        editor.setVisibility(VISIBLE);
        preview.setVisibility(GONE);
        editButton.setVisibility(GONE);
        editActions.setVisibility(VISIBLE);
        editor.requestFocus();
        editor.setSelection(Math.min(editor.length(), editor.length()));
        editor.post(() -> {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && editor.isFocused())
                imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void exitEdit(boolean saved) {
        editing = false;
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(editor.getWindowToken(), 0);
        editor.clearFocus();
        editor.setVisibility(GONE);
        preview.setVisibility(VISIBLE);
        editButton.setVisibility(VISIBLE);
        editActions.setVisibility(GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
