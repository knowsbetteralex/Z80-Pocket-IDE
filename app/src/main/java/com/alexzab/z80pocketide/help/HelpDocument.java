package com.alexzab.z80pocketide.help;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tiny, Android-independent chapter parser for bundled user guide Markdown.
 * The first # header identifies the guide; ## headers identify navigable chapters.
 * Markdown formatting inside chapter bodies is left intact for UI rendering.
 */
public final class HelpDocument {
    public static final class Chapter {
        public final String title;
        public final String markdown;

        Chapter(String title, String markdown) {
            this.title = title;
            this.markdown = markdown;
        }
    }

    public final String title;
    public final String introduction;
    public final List<Chapter> chapters;

    private HelpDocument(String title, String introduction, List<Chapter> chapters) {
        this.title = title;
        this.introduction = introduction;
        this.chapters = Collections.unmodifiableList(chapters);
    }

    public static HelpDocument parse(String markdown) {
        if (markdown == null) throw new IllegalArgumentException("Missing help Markdown");
        String title = "";
        StringBuilder introduction = new StringBuilder();
        List<Chapter> chapters = new ArrayList<>();
        String currentTitle = null;
        StringBuilder chapterText = new StringBuilder();
        boolean codeBlock = false;
        final String fence = new String(new char[]{96, 96, 96});
        for (String line : markdown.replace("\r\n", "\n").replace('\r', '\n')
                .split("\n", -1)) {
            // Headings inside fenced code samples are not section boundaries.
            if (line.trim().startsWith(fence)) {
                codeBlock = !codeBlock;
            } else if (!codeBlock && line.startsWith("# ") && title.isEmpty()
                    && currentTitle == null) {
                title = line.substring(2).trim();
                continue;
            } else if (!codeBlock && line.startsWith("## ")) {
                if (currentTitle != null)
                    chapters.add(new Chapter(currentTitle, chapterText.toString().trim()));
                currentTitle = line.substring(3).trim();
                if (currentTitle.isEmpty())
                    throw new IllegalArgumentException("Empty chapter title");
                chapterText.setLength(0);
                continue;
            }
            StringBuilder target = currentTitle == null ? introduction : chapterText;
            if (target.length() != 0) target.append('\n');
            target.append(line);
        }
        if (codeBlock) throw new IllegalArgumentException("Unclosed code fence");
        if (currentTitle != null)
            chapters.add(new Chapter(currentTitle, chapterText.toString().trim()));
        if (title.isEmpty() || chapters.isEmpty())
            throw new IllegalArgumentException("Help requires a title and chapters");
        for (Chapter chapter : chapters)
            if (chapter.markdown.isEmpty())
                throw new IllegalArgumentException("Empty chapter: " + chapter.title);
        return new HelpDocument(title, introduction.toString().trim(), chapters);
    }
}
