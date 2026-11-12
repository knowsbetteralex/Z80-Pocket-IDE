package com.alexzab.z80pocketide.update;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseNotesTest {
    private static final String BODY =
            "## Русский\n\n### Что нового\n\n- **Колокольчик** на панели.\n- Ссылка на GitHub.\n\n"
            + "---\n\n## English\n\n### What's new\n\n"
            + "- **Update bell** in header.\n- GitHub release link.\n\n"
            + "### Other\n- Should not show.\n";

    @Test public void extractsOnlyChosenFeatureSection() {
        assertEquals("• Колокольчик на панели.\n• Ссылка на GitHub.",
                ReleaseNotes.features(BODY, true));
        assertEquals("• Update bell in header.\n• GitHub release link.",
                ReleaseNotes.features(BODY, false));
    }

    @Test public void noFeatureHeaderDoesNotShowGenericApkText() {
        assertEquals("", ReleaseNotes.features("## Русский\n- APK: download.\n## English", true));
    }

    @Test public void onlyNewerReleaseShowsBellUntilInstalled() {
        assertFalse(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.48.0 (build #644)",
                "build-644-1", "abc", "0.48.0", 644, "abc"));
        assertTrue(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.48.0 (build #645)",
                "build-645-1", "def", "0.48.0", 644, "abc"));
        assertTrue(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.49.0 (build #646)",
                "build-646-1", "ghi", "0.48.0", 644, "abc"));
        assertFalse(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.48.0 (build #644)",
                "build-644-1", "abc", "0.49.0", 645, "def"));
        assertFalse(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.48.0 (build #644)",
                "build-644-1", "abc", "0.48.0", 646, "ghi"));
        assertFalse(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.48.0",
                "build-644-1", "abcdef1234567890", "0.48.0", 0, "abcdef1234567890"));
        assertTrue(ReleaseNotes.isNewerRelease("Z80 Pocket IDE v0.48.0",
                "build-644-1", "abcdef1234567890", "0.48.0", 0, "1234567890abcdef"));
    }
}
