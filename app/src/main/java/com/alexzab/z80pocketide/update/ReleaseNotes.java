package com.alexzab.z80pocketide.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Android-free release-note parsing and installed-version comparison. */
public final class ReleaseNotes {
    private static final Pattern RELEASE_VERSION =
            Pattern.compile("\\bv(\\d+(?:\\.\\d+){1,3})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_BUILD = Pattern.compile("^build-(\\d+)-\\d+$");

    private ReleaseNotes() {}

    /**
     * Show a release until an APK at least as new is installed.
     * For the same semantic version, CI run numbers track successive release builds.
     * A previously viewed dialog never suppresses the update.
     */
    public static boolean isNewerRelease(String releaseName, String tag, String releaseCommit,
                                         String installedVersion, long installedBuild,
                                         String installedCommit) {
        if (tag == null || tag.isEmpty()) return false;
        int[] remoteVersion = releaseVersion(releaseName);
        int[] localVersion = versionNumbers(installedVersion);
        if (remoteVersion != null && localVersion != null) {
            for (int i = 0; i < 4; i++) {
                if (remoteVersion[i] != localVersion[i])
                    return remoteVersion[i] > localVersion[i];
            }
        }
        long remoteBuild = releaseBuildNumber(tag);
        if (remoteBuild > 0 && installedBuild > 0) return remoteBuild > installedBuild;
        // Fallback for pre-build-number APKs. Locally built APKs cannot be compared by SHA.
        return releaseCommit != null && !releaseCommit.isEmpty()
                && installedCommit != null && installedCommit.length() >= 7
                && !installedCommit.equals("local")
                && !releaseCommit.equalsIgnoreCase(installedCommit);
    }

    private static int[] releaseVersion(String releaseName) {
        if (releaseName == null) return null;
        Matcher match = RELEASE_VERSION.matcher(releaseName);
        return match.find() ? versionNumbers(match.group(1)) : null;
    }

    private static int[] versionNumbers(String version) {
        if (version == null || !version.matches("\\d+(?:\\.\\d+){1,3}")) return null;
        String[] pieces = version.split("\\.");
        int[] parts = new int[4];
        try {
            for (int i = 0; i < pieces.length; i++)
                parts[i] = Integer.parseInt(pieces[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        return parts;
    }

    private static long releaseBuildNumber(String tag) {
        Matcher match = RELEASE_BUILD.matcher(tag);
        if (!match.matches()) return -1;
        try {
            return Long.parseLong(match.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
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
            if (inFeatures && line.isEmpty() && result.length() > 0)
                inFeatures = false;
            if (!inFeatures || !line.startsWith("- ")) continue;
            String item = line.substring(2).replace("**", "")
                    .replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
            if (result.length() > 0) result.append("\n");
            result.append("• ").append(item);
        }
        return result.toString();
    }
}
