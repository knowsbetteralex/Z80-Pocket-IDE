package com.alexzab.z80pocketide.editor;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent editor behaviour preferences. */
public final class EditorPreferences {
    private static final String PREFS = "editor_preferences";
    private static final String AUTO_UPPER = "auto_upper";
    private static final String ALL_UPPER = "all_upper";
    private static final String AUTO_INDENT = "auto_indent";
    private static final String TAB_SIZE = "tab_size";
    private static final String USE_SPACES = "use_spaces";
    private static final String SHOW_TAB = "show_tab_button";
    private static final String FOLDING = "folding";

    private EditorPreferences() {}

    public static Snapshot get(Context context) {
        SharedPreferences p = prefs(context);
        return new Snapshot(
                p.getBoolean(AUTO_UPPER, true),
                p.getBoolean(ALL_UPPER, false),
                p.getBoolean(AUTO_INDENT, true),
                p.getInt(TAB_SIZE, 4),
                p.getBoolean(USE_SPACES, true),
                p.getBoolean(SHOW_TAB, true),
                p.getBoolean(FOLDING, true)
        );
    }

    public static void set(Context context, Snapshot value) {
        prefs(context).edit()
                .putBoolean(AUTO_UPPER, value.autoUppercase)
                .putBoolean(ALL_UPPER, value.allUppercase)
                .putBoolean(AUTO_INDENT, value.autoIndent)
                .putInt(TAB_SIZE, value.tabSize)
                .putBoolean(USE_SPACES, value.useSpaces)
                .putBoolean(SHOW_TAB, value.showTabButton)
                .putBoolean(FOLDING, value.folding)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class Snapshot {
        public final boolean autoUppercase;
        public final boolean allUppercase;
        public final boolean autoIndent;
        public final int tabSize;
        public final boolean useSpaces;
        public final boolean showTabButton;
        public final boolean folding;

        public Snapshot(boolean autoUppercase, boolean allUppercase, boolean autoIndent,
                        int tabSize, boolean useSpaces, boolean showTabButton, boolean folding) {
            this.autoUppercase = autoUppercase;
            this.allUppercase = allUppercase;
            this.autoIndent = autoIndent;
            this.tabSize = tabSize;
            this.useSpaces = useSpaces;
            this.showTabButton = showTabButton;
            this.folding = folding;
        }
    }
}
