package com.alexzab.z80pocketide.i18n;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class LanguageSettings {
    private static final String PREFS = "z80_pocket_ide_settings";
    private static final String KEY_LANGUAGE = "language";

    private LanguageSettings() {}

    public static AppLanguage get(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_LANGUAGE, null);
        if (saved != null) return AppLanguage.fromCode(saved);
        return "ru".equalsIgnoreCase(Locale.getDefault().getLanguage())
                ? AppLanguage.RU : AppLanguage.EN;
    }

    public static void set(Context context, AppLanguage language) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, language.code())
                .apply();
    }
}
