package com.alexzab.z80pocketide.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Finds the FIRST label in each nonempty paragraph (blank-line delimited).
 * All subsequent labels in the paragraph are nested within that block.
 * The returned body ranges are in original source offsets, not visual rows.
 */
public final class TopLevelFoldPlanner {
    private static final Pattern LABEL_ONLY = Pattern.compile(
            "^[A-Za-z_.$?][A-Za-z0-9_.$?]*\\s*:\\s*(?:;.*)?$");

    public static final class Range {
        public final int bodyStart, bodyEnd;
        public Range(int bodyStart, int bodyEnd) {
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
        }
    }

    private TopLevelFoldPlanner() {}

    public static List<Range> plan(String source) {
        List<Range> result = new ArrayList<>();
        if (source == null || source.isEmpty()) return result;
        int n = source.length();
        int pos = 0;
        while (pos < n) {
            int paragraphStart = pos;
            int paragraphEnd = pos;
            while (paragraphEnd < n) {
                int eol = source.indexOf('\n', paragraphEnd);
                if (eol < 0) eol = n;
                if (source.substring(paragraphEnd, eol).trim().isEmpty()) break;
                paragraphEnd = eol < n ? eol + 1 : n;
            }
            if (paragraphEnd > paragraphStart) {
                int line = paragraphStart;
                while (line < paragraphEnd) {
                    int eol = source.indexOf('\n', line);
                    if (eol < 0 || eol > paragraphEnd) eol = paragraphEnd;
                    if (LABEL_ONLY.matcher(source.substring(line, eol).trim()).matches()) {
                        int bodyStart = eol < n ? eol + 1 : n;
                        if (bodyStart < paragraphEnd
                                && !source.substring(bodyStart, paragraphEnd).trim().isEmpty())
                            result.add(new Range(bodyStart, paragraphEnd));
                        break;
                    }
                    line = eol < paragraphEnd ? eol + 1 : paragraphEnd;
                }
            }
            if (paragraphEnd >= n) break;
            int blankEol = source.indexOf('\n', paragraphEnd);
            pos = blankEol < 0 ? n : blankEol + 1;
        }
        return result;
    }
}
