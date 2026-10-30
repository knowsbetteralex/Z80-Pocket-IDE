package com.alexzab.z80pocketide.assembler;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public final class LocalLabelsTest {
    @Test public void repeatedDotLocalNamesHaveIndependentScopes() {
        String source = "ORG 32768\n"
                + "FIRST:\n.loop:\nDJNZ .loop\n"
                + "SECOND:\n.loop:\nJR .loop\n"
                + "JP FIRST.loop\nJP SECOND.loop";
        assertArrayEquals(new byte[]{
                0x10,(byte)0xFE,0x18,(byte)0xFE,
                (byte)0xC3,0x00,(byte)0x80,
                (byte)0xC3,0x02,(byte)0x80},
                new Assembler().assemble(source).getBytes());
    }

    @Test public void forwardLocalsWorkInExpressionsAndEqu() {
        String source = "ORG 32768\nMOVE:\n"
                + "GLOBAL_VALUE EQU 1\n"
                + "JR .later\n"
                + "DB '.loop'\n"
                + ".value EQU 9\n"
                + "DB .value + GLOBAL_VALUE\n"
                + ".later: RET";
        assertArrayEquals(new byte[]{
                0x18,0x06,
                '.', 'l','o','o','p',
                10,(byte)0xC9},
                new Assembler().assemble(source).getBytes());
    }

    @Test public void dottedGlobalNamesAndLeadingDotLocalsCoexist() {
        String source = "ORG 32768\nsprite.draw:\n"
                + ".done: RET\nJP sprite.draw.done\n"
                + "enemy.draw:\n.done: NOP\nJP enemy.draw.done";
        assertArrayEquals(new byte[]{
                (byte)0xC9,(byte)0xC3,0,(byte)0x80,
                0,(byte)0xC3,4,(byte)0x80},
                new Assembler().assemble(source).getBytes());
    }

    @Test public void nestedIncludesShareLocalContextAndAllowReuse() {
        Map<String,String> files = new HashMap<>();
        files.put("ship.asm", ".step: NOP\nJR .step");
        files.put("enemy.asm", ".step: RET\nJP .step");
        String root="ORG 32768\nSHIP:\nINCLUDE \"ship.asm\"\n"
                + "ENEMY:\nINCLUDE \"enemy.asm\"";
        SourceIncludes.Expanded expanded=SourceIncludes.expand(root,"main.asm",files::get);
        assertArrayEquals(new byte[]{
                0,0x18,(byte)0xFD,
                (byte)0xC9,(byte)0xC3,3,(byte)0x80},
                new Assembler().assemble(expanded.source).getBytes());
    }

    @Test public void referencesBeforeGlobalLabelReportExplicitError() {
        try {
            new Assembler().assemble("ORG 32768\nJP .missing\nROOT:\n.missing: RET");
            fail("local must have a parent global label");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage(),ex.getMessage().contains("line 2"));
            assertTrue(ex.getMessage(),ex.getMessage().contains(".missing"));
        }
    }
}
