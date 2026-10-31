package com.alexzab.z80pocketide.assembler;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public final class SourceMacrosTest {
    private static AssemblyResult assemble(String main,Map<String,String> files) {
        SourceIncludes.Expanded incl=SourceIncludes.expand(main,"main.asm",files::get);
        return new Assembler().assemble(SourceMacros.expand(incl).source);
    }

    @Test public void supportsNamedParametersAndForwardMacroCalls() {
        String source="ORG 32768\n"
                +"SETVALUE B,7\n"
                +"SETVALUE MACRO dst,value\n"
                +"  LD dst,value\n"
                +"ENDM\n"
                +"SETVALUE C,9";
        assertArrayEquals(new byte[]{6,7,14,9},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void macroDefinitionFromIncludeIsVisibleAcrossProject() {
        Map<String,String> files=new HashMap<>();
        files.put("lib/macros.asm","MACRO PUSHVALUE, value\nLD A,\\value\nENDM");
        String root="ORG 32768\nINCLUDE \"lib/macros.asm\"\nPUSHVALUE 42";
        SourceIncludes.Expanded included=SourceIncludes.expand(root,"main.asm",files::get);
        SourceIncludes.Expanded expanded=SourceMacros.expand(included);
        AssemblyResult result=new Assembler().assemble(expanded.source);
        assertArrayEquals(new byte[]{62,42},result.getBytes());
        assertEquals(2,expanded.project(result).getLineSize(2));
        assertEquals(0,expanded.project(result).getLineSize(1));
    }

    @Test public void invocationLocalLabelsDoNotCollideAcrossExpansions() {
        String source="ORG 32768\n"
                +"SPIN MACRO count\n"
                +"    LD B,count\n"
                +"@@again: DJNZ @@again\n"
                +"ENDM\n"
                +"SPIN 2\nSPIN 3";
        assertArrayEquals(new byte[]{6,2,16,(byte)0xFE,6,3,16,(byte)0xFE},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void localDotLabelScopeAcrossMacroExpansion() {
        String source="ORG 32768\n"
                +"TAIL MACRO\n"
                +"    JR .done\n"
                +"ENDM\n"
                +"ROOT:\nTAIL\n.done: RET";
        assertArrayEquals(new byte[]{24,0,(byte)0xC9},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void parenthesizedCommasInArgumentsStayTogether() {
        String source="ORG 32768\n"
                +"LOADBYTE MACRO address,value\n"
                +"  LD address,value\n"
                +"ENDM\n"
                +"LOADBYTE (IX+2),7";
        assertArrayEquals(new byte[]{(byte)0xDD,0x36,2,7},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void commentsStringsAndPartialIdentifiersAreNotReplaced() {
        String source="ORG 32768\n"
                +"EMIT MACRO val\n"
                +"  DB \"val\",val ; val comment\n"
                +"ENDM\n"
                +"EMIT 65";
        assertArrayEquals(new byte[]{'v','a','l',65},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void nestedMacroCallsExpandAndMappingIsPreserved() {
        String source="ORG 32768\n"
                +"INNER MACRO arg\n DB arg\nENDM\n"
                +"OUTER MACRO one,two\n INNER one\n INNER two\nENDM\n"
                +"OUTER 1,2";
        SourceIncludes.Expanded included=SourceIncludes.expand(source,"main.asm",path->null);
        SourceIncludes.Expanded expanded=SourceMacros.expand(included);
        AssemblyResult result=new Assembler().assemble(expanded.source);
        assertArrayEquals(new byte[]{1,2},result.getBytes());
        assertEquals(2,expanded.project(result).getLineSize(7));
        assertEquals(8,expanded.rootLines);
    }

    @Test public void invalidMacroIsReportedWithoutHanging() {
        String recursive="LOOP MACRO\nLOOP\nENDM\nLOOP";
        assertThrows(IllegalArgumentException.class,
                () -> assemble(recursive,new HashMap<>()));
        String unmatched="FOO MACRO a\n LD A,a";
        assertThrows(IllegalArgumentException.class,
                () -> assemble(unmatched,new HashMap<>()));
        String invalid="FOO MACRO a,b\n LD A,a\nENDM\nFOO 1";
        assertThrows(IllegalArgumentException.class,
                () -> assemble(invalid,new HashMap<>()));
    }
}
