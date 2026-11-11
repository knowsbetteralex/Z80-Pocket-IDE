package com.alexzab.z80pocketide.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class HelpDocumentTest {
    private static String guide(String language) throws Exception {
        Path file = Paths.get("src/main/assets/help/guide_" + language + ".md");
        if (!Files.exists(file))
            file = Paths.get("app/src/main/assets/help/guide_" + language + ".md");
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void bundledGuidesHaveMatchingChaptersAndActualCodeBlocks() throws Exception {
        String ru = guide("ru"), en = guide("en");
        HelpDocument russian = HelpDocument.parse(ru);
        HelpDocument english = HelpDocument.parse(en);
        String fence = new String(new char[]{96, 96, 96});
        assertEquals(11, russian.chapters.size());
        assertEquals(russian.chapters.size(), english.chapters.size());
        assertTrue(russian.title.contains("Z80 Pocket IDE"));
        assertTrue(english.title.contains("Z80 Pocket IDE"));
        assertTrue(ru.contains(fence + "asm"));
        assertTrue(en.contains(fence + "asm"));
        for (int i = 0; i < russian.chapters.size(); i++) {
            assertFalse(russian.chapters.get(i).markdown.isEmpty());
            assertFalse(english.chapters.get(i).markdown.isEmpty());
            assertTrue(russian.chapters.get(i).title.startsWith((i + 1) + ". "));
            assertTrue(english.chapters.get(i).title.startsWith((i + 1) + ". "));
        }
    }

    @Test public void fencedHeadingsDoNotCreateChapters() {
        String fence = new String(new char[]{96, 96, 96});
        String source = "# Manual\n\nSummary\n"
                + "## One\n\nStart\n"
                + fence + "\n## This is example code\n" + fence + "\n"
                + "## Two\n\nFinish\n";
        HelpDocument parsed = HelpDocument.parse(source);
        assertEquals("Manual", parsed.title);
        assertEquals("Summary", parsed.introduction);
        assertEquals(2, parsed.chapters.size());
        assertTrue(parsed.chapters.get(0).markdown.contains("## This is example code"));
    }

    @Test public void invalidHelpDocumentsAreRejected() {
        String fence = new String(new char[]{96, 96, 96});
        assertThrows(IllegalArgumentException.class,
                () -> HelpDocument.parse("# No chapters"));
        assertThrows(IllegalArgumentException.class,
                () -> HelpDocument.parse("## Missing guide title\n\nText"));
        assertThrows(IllegalArgumentException.class,
                () -> HelpDocument.parse("# Manual\n## Chapter\n\n" + fence + "\nunfinished"));
    }
}
