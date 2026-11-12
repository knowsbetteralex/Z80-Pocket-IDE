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
    @Test public void unreadRequiresNewCommitAndNewTag() {
        assertFalse(ReleaseNotes.isUnread("build-7", "abc", "abc", ""));
        assertFalse(ReleaseNotes.isUnread("build-7", "def", "abc", "build-7"));
        assertTrue(ReleaseNotes.isUnread("build-8", "def", "abc", "build-7"));
    }
}
