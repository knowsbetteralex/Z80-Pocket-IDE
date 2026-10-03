package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SourceFormatterTest {
    @Test
    public void uppercasesKeywordsButNotLabelsStringsOrComments() {
        String line = "loop: ld a,\"ld b\" ; ld c";
        assertEquals("loop: LD A,\"ld b\" ; ld c",
                SourceFormatter.uppercaseKeywordsInLine(line));
    }

    @Test
    public void formatsLabelsAndInstructionIndentation() {
        String source = "org $8000\nloop:\nld a,1\nout ($fe),a\n";
        String expected = "ORG $8000\nloop:\n    LD A,1\n    OUT ($fe),A\n";
        assertEquals(expected,
                SourceFormatter.formatDocument(source, true, true, 4, true));
    }

    @Test
    public void newlineAfterLabelGetsOneIndentLevel() {
        assertEquals("    ",
                SourceFormatter.indentationForNewLine("START:", 4, true));
        assertEquals("\t",
                SourceFormatter.indentationForNewLine("LOOP:", 4, false));
    }
}
