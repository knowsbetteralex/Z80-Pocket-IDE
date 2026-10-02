package com.alexzab.z80pocketide.emulator;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists the user's preferred app for opening generated TAP files. */
public final class EmulatorSettings {
    private static final String PREFS = "emulator_settings";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_LABEL = "label";

    private EmulatorSettings() {}

    public static String getPackage(Context context) {
        return prefs(context).getString(KEY_PACKAGE, null);
    }

    public static String getLabel(Context context) {
        return prefs(context).getString(KEY_LABEL, null);
    }

    public static void set(Context context, String packageName, String label) {
        prefs(context).edit()
                .putString(KEY_PACKAGE, packageName)
                .putString(KEY_LABEL, label)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_PACKAGE).remove(KEY_LABEL).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
