package com.alexzab.z80pocketide.source;

/**
 * Small platform-independent guard for external Android document-opening
 * intents. AndroidManifest handles MIME matching; this checks action/scheme
 * before resolving an untrusted URI with ContentResolver.
 */
public final class ExternalSourceIntent {
    private ExternalSourceIntent() {}

    public static boolean accepts(String action, String scheme) {
        if (!"android.intent.action.VIEW".equals(action)
                && !"android.intent.action.EDIT".equals(action)) return false;
        return "content".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme);
    }
}
