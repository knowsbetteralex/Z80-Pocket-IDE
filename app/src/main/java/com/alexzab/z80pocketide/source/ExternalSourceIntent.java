package com.alexzab.z80pocketide.source;

/**
 * Small platform-independent guard for external Android document-opening
 * intents. AndroidManifest handles MIME matching; this checks action/scheme
 * before resolving an untrusted URI with ContentResolver.
 */
public final class ExternalSourceIntent {
    private ExternalSourceIntent() {}

    /**
     * A write-access retry must not silently overwrite a different document.
     * Exact file names are required when Android returns a different provider
     * URI for the selected original file. The user explicitly chooses that file.
     */
    public static boolean matchingName(String originalName, String selectedName) {
        return originalName != null && selectedName != null
                && !originalName.trim().isEmpty()
                && originalName.trim().equalsIgnoreCase(selectedName.trim());
    }

    public static boolean accepts(String action, String scheme) {
        if (!"android.intent.action.VIEW".equals(action)
                && !"android.intent.action.EDIT".equals(action)) return false;
        return "content".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme);
    }
}
