package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class SyntaxTokenPlannerTest {
    private boolean has(List<SyntaxTokenPlanner.Token> tokens,int kind,
                        int start,int end) {
        for(SyntaxTokenPlanner.Token token:tokens)
            if(token.kind==kind && token.start==start && token.end==end)return true;
        return false;
    }

    @Test public void rendersBasicZ80KeywordsAndRegisterOperands() {
        String source="LOOP:\n    LD A,$10\n    JR NZ,LOOP\n";
        List<SyntaxTokenPlanner.Token> tokens=
                SyntaxTokenPlanner.plan(source,source,0);
        assertTrue(has(tokens,SyntaxTokenPlanner.LABEL,0,4));
        assertTrue(has(tokens,SyntaxTokenPlanner.MNEMONIC,
                source.indexOf("LD"),source.indexOf("LD")+2));
        assertTrue(has(tokens,SyntaxTokenPlanner.REGISTER,
                source.indexOf(" A,")+1,source.indexOf(" A,")+2));
        assertTrue(has(tokens,SyntaxTokenPlanner.NUMBER,
                source.indexOf("$10"),source.indexOf("$10")+3));
    }

    @Test public void foldedDefinitionStillColorsOutsideReference() {
        String complete="START:\n    JR INNER\nINNER:\n    RET\n\nEND:\n    JP INNER\n";
        String visible="START:\n    ; ▶ 3 lines · fold#1\n\nEND:\n    JP INNER\n";
        int offset=730;
        List<SyntaxTokenPlanner.Token> tokens=
                SyntaxTokenPlanner.plan(visible,complete,offset);
        int inner=visible.lastIndexOf("INNER");
        assertTrue(has(tokens,SyntaxTokenPlanner.LABEL,
                offset+inner,offset+inner+5));
        int fake=visible.indexOf("fold#1");
        assertFalse(has(tokens,SyntaxTokenPlanner.LABEL,
                offset+fake,offset+fake+4));
    }

    @Test public void visibleWindowUsesAbsoluteEditorOffsets() {
        String full="HEADER:\n    NOP\n\nTARGET:\n    INC B\n";
        String window="TARGET:\n    INC B\n";
        int offset=full.indexOf("TARGET");
        List<SyntaxTokenPlanner.Token> tokens=
                SyntaxTokenPlanner.plan(window,full,offset);
        assertTrue(has(tokens,SyntaxTokenPlanner.LABEL,offset,offset+6));
        assertTrue(has(tokens,SyntaxTokenPlanner.MNEMONIC,
                offset+window.indexOf("INC"),offset+window.indexOf("INC")+3));
        assertTrue(has(tokens,SyntaxTokenPlanner.REGISTER,
                offset+window.indexOf("B"),offset+window.indexOf("B")+1));
    }

    @Test public void blankVisibleWindowReturnsNoTokens() {
        assertEquals(0,SyntaxTokenPlanner.plan("","CODE:\n",120).size());
    }
}
