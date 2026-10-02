package com.alexzab.z80pocketide.i18n;

public enum AppLanguage {
    EN("en"),
    RU("ru");

    private final String code;

    AppLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AppLanguage fromCode(String code) {
        return "ru".equalsIgnoreCase(code) ? RU : EN;
    }
}
