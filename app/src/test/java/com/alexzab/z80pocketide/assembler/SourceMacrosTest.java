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
        assertEquals(2,expanded.project(result).getLineSize(8));
        assertEquals(9,expanded.rootLines);
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

    @Test public void conditionalMacroChoosesCorrectBranchAndNestedBlocks() {
        String source="ORG 32768\n"
                +"EMIT MACRO count\n"
                +" IF count > 0\n"
                +"  IF count == 2\n"
                +"   LD B,2\n"
                +"  ELSE\n"
                +"   LD B,count\n"
                +"  ENDIF\n"
                +" ELSE\n"
                +"  LD B,0\n"
                +" ENDIF\n"
                +"ENDM\n"
                +"EMIT 2\nEMIT 5\nEMIT 0";
        assertArrayEquals(new byte[]{6,2,6,5,6,0},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void elseifAndBitwiseConditionsWorkWithConstantEqu() {
        String source="ORG 32768\nFLAG EQU 2\n"
                +"IF FLAG = 1\n DB 11\n"
                +"ELSEIF FLAG >= 2\n DB 22\n"
                +"ELSE\n DB 33\nENDIF\n"
                +"IF (FLAG & 2)\n DB 44\nENDIF\n"
                +"IF FLAG << 1\n DB 55\nENDIF";
        assertArrayEquals(new byte[]{22,44,55},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void ifdefAndIfndefReflectAlreadyResolvedEqu() {
        String source="ORG 32768\nVALUE EQU 7\n"
                +"IFDEF VALUE\n DB VALUE\nENDIF\n"
                +"IFNDEF ABSENT\n DB 8\nENDIF\n"
                +"IFDEF ABSENT\n BADOP\nELSE\n DB 9\nENDIF";
        assertArrayEquals(new byte[]{7,8,9},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void excludedBranchesDoNotEvaluateUnknownSymbolsOrRecurse() {
        String source="ORG 32768\n"
                +"FOREVER MACRO\n FOREVER\nENDM\n"
                +"IF 0\n IF UNKNOWN >= 999\n FOREVER\n ENDIF\n"
                +"ELSE\n DB 1\nENDIF\n"
                +"IF 1\n DB 2\nELSE\n UNKNOWN\nENDIF";
        assertArrayEquals(new byte[]{1,2},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void conditionsInIncludedMacroAndSourceLineMapping() {
        Map<String,String> files=new HashMap<>();
        files.put("macros.asm","CHOOSE MACRO flag\nIF flag\n DB 10\nELSE\n DB 20\nENDIF\nENDM");
        String source="ORG 32768\nINCLUDE \"macros.asm\"\nCHOOSE 0\nCHOOSE 1";
        SourceIncludes.Expanded expanded=SourceMacros.expand(
                SourceIncludes.expand(source,"main.asm",files::get));
        AssemblyResult binary=new Assembler().assemble(expanded.source);
        assertArrayEquals(new byte[]{20,10},binary.getBytes());
        assertEquals(1,expanded.project(binary).getLineSize(2));
        assertEquals(1,expanded.project(binary).getLineSize(3));
    }

    @Test public void malformedConditionalBlocksAreRejected() {
        String[] invalid={
                "ORG 32768\nIF 1\nNOP",
                "ORG 32768\nENDIF",
                "ORG 32768\nELSE",
                "ORG 32768\nIF 1\nELSE\nELSE\nENDIF",
                "ORG 32768\nIF MISSING\nNOP\nENDIF",
                "ORG 32768\nIF 0\nELSEIF 1\nELSEIF 2\nELSE\nENDIF\nENDIF",
                "ORG 32768\nIF 1\nELSE\nELSEIF 1\nENDIF"
        };
        for(String source:invalid)
            assertThrows(IllegalArgumentException.class,
                    () -> assemble(source,new HashMap<>()));
    }

    @Test public void prefixMacroWithoutParametersAndConditionalBody() {
        String source="ORG 32768\n"
                +"MACRO WAVE\n"
                +" IF 1\n  NOP\n ENDIF\n"
                +"ENDM\n"
                +"WAVE\nWAVE";
        assertArrayEquals(new byte[]{0,0},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void ifnAssemblesOnlyWhenExpressionIsZero() {
        String source = "ORG 32768\n"
                + "IFN 0\n DB 11\nELSE\n DB 99\nENDIF\n"
                + "IFN 1\n DB 99\nELSE\n DB 22\nENDIF\n"
                + "IFN 2-2\n DB 33\nENDIF\n";
        assertArrayEquals(new byte[]{11,22,33},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void ifnSupportsComparisonsElseifAndDefinedConstants() {
        String source="ORG 32768\n"
                + "FLAG EQU 5\n"
                + "IFN FLAG=5\n DB 99\nELSEIF FLAG>4\n DB 44\nENDIF\n"
                + "IFN FLAG-4\n DB 99\nELSE\n DB 55\nENDIF\n"
                + ".IFN FLAG-5\n DB 66\n.ENDIF\n";
        assertArrayEquals(new byte[]{44,55,66},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void ifnWorksWithMacroArgumentsNestedIfAndIncludes() {
        Map<String,String> files = new HashMap<>();
        files.put("logic.inc","VALUE MACRO arg\n"
                + "  IFN arg\n"
                + "    DB 0\n"
                + "  ELSE\n"
                + "    IFN arg-3\n"
                + "      DB 3\n"
                + "    ELSE\n"
                + "      DB arg\n"
                + "    ENDIF\n"
                + "  ENDIF\n"
                + "ENDM");
        String source="ORG 32768\n"
                + "INCLUDE \"logic.inc\"\n"
                + "VALUE 0\nVALUE 3\nVALUE 7";
        assertArrayEquals(new byte[]{0,3,7},assemble(source,files).getBytes());
    }

    @Test public void ifnDoesNotEvaluateInactiveNestedExpression() {
        String source="ORG 32768\n"
                + "IF 0\n IFN UNDEFINED\n BADOP\n ENDIF\n"
                + "ELSE\n DB 10\nENDIF";
        assertArrayEquals(new byte[]{10},
                assemble(source,new HashMap<>()).getBytes());
    }

    @Test public void ifnWithMissingOrUnresolvedConditionShowsError() {
        for(String source:new String[]{
                "ORG 32768\nIFN\n NOP\nENDIF",
                "ORG 32768\nIFN UNDEFINED\n NOP\nENDIF",
                "ORG 32768\nIFN 1\n NOP"
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> assemble(source,new HashMap<>()));
        }
    }

    @Test public void characterLiteralArgumentWorksInMacroAndIf() {
        String source="ORG 32768\n"
                + "WRITE MACRO value\n"
                + " IF value = 'a'\n"
                + "  LD A,value\n"
                + " ELSE\n"
                + "  LD A,'?'\n"
                + " ENDIF\n"
                + "ENDM\n"
                + "WRITE 'a'\n"
                + "WRITE 'b'\n";
        assertArrayEquals(new byte[]{0x3e,0x61,0x3e,0x3f},
                assemble(source,new HashMap<>()).getBytes());
    }
}
