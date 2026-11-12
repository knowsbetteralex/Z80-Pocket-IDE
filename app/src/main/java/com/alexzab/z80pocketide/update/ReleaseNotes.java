package com.alexzab.z80pocketide.update;

/** Android-free release-note parsing and unread-release detection. */
public final class ReleaseNotes {
    private ReleaseNotes() {}

    public static boolean isUnread(String tag, String commit, String installedCommit, String readTag) {
        if (tag == null || tag.isEmpty() || tag.equals(readTag)) return false;
        return commit == null || !commit.equalsIgnoreCase(installedCommit);
    }

    public static String features(String markdown, boolean russian) {
        if (markdown == null || markdown.isEmpty()) return "";
        String wanted = russian ? "## Русский" : "## English";
        boolean inLanguage = false, inFeatures = false;
        StringBuilder result = new StringBuilder();
        for (String sourceLine : markdown.replace("\r", "").split("\n")) {
            String line = sourceLine.trim();
            if (line.startsWith("## ")) {
                inLanguage = line.equals(wanted);
                inFeatures = false;
                continue;
            }
            if (!inLanguage) continue;
            if (line.startsWith("### ")) {
                inFeatures = line.equals(russian ? "### Что нового" : "### What's new");
                continue;
            }
            // The following release metadata (APK, checksum) is not a new feature.
            if (inFeatures && line.isEmpty() && result.length() > 0) {
                inFeatures = false;
            }
            if (!inFeatures || !line.startsWith("- ")) continue;
            String item = line.substring(2).replace("**", "")
                    .replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
            if (result.length() > 0) result.append("\n");
            result.append("• ").append(item);
        }
        return result.toString();
    }
}
