package com.alexzab.z80pocketide.assembler;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class SourceIncludesTest {
    @Test public void nestedFilesShareLabelsAndAccountForDirectiveBytes() {
        Map<String,String> files=new HashMap<>();
        files.put("src/part.asm","VALUE EQU 7\nINCLUDE \"../lib/data.asm\"");
        files.put("lib/data.asm","DB VALUE");
        SourceIncludes.Expanded expanded=SourceIncludes.expand(
                "ORG 32768\nINCLUDE \"part.asm\"\nJP finish\nfinish: NOP",
                "src/main.asm",files::get);
        AssemblyResult result=new Assembler().assemble(expanded.source);
        assertArrayEquals(new byte[]{7,(byte)0xC3,4,(byte)0x80,0},result.getBytes());
        AssemblyResult projected=expanded.project(result);
        assertEquals(4,projected.getLineCount());
        assertEquals(1,projected.getLineSize(1));
        assertEquals(3,projected.getLineSize(2));
        assertEquals(1,projected.getLineSize(3));
        assertEquals(2,expanded.location(3).rootLine);
    }

    @Test public void recognizeQuotedAndUnquotedWithoutComments() {
        assertEquals("foo.asm",SourceIncludes.includePath(" INCLUDE 'foo.asm' ; note"));
        assertEquals("folder/bar.asm",SourceIncludes.includePath(".include folder/bar.asm"));
        assertNull(SourceIncludes.includePath("; INCLUDE \"unused.asm\""));
        assertFalse(SourceIncludes.containsInclude("; INCLUDE \"unused.asm\""));
    }

    @Test public void cyclesAndRootTraversalAreRejected() {
        assertThrows(IllegalArgumentException.class,
                ()->SourceIncludes.resolve("main.asm","../escape.asm"));
        assertThrows(IllegalArgumentException.class,
                ()->SourceIncludes.expand("INCLUDE \"main.asm\"","main.asm",s->""));
        Map<String,String> files=new HashMap<>();
        files.put("a.asm","INCLUDE \"b.asm\"");
        files.put("b.asm","INCLUDE \"a.asm\"");
        assertThrows(IllegalArgumentException.class,
                ()->SourceIncludes.expand("INCLUDE \"a.asm\"","main.asm",files::get));
    }

    @Test public void metricsAndErrorsMapToIncludeDirective() {
        SourceIncludes.Expanded expanded=SourceIncludes.expand(
                "INCLUDE \"code.asm\"","main.asm",s->"NOP\nNOP");
        AssemblyResult result=expanded.project(new Assembler().assemble(expanded.source));
        assertEquals(2,result.getLineSize(0));
        assertEquals(8,result.getLineMinCycles(0));
        SourceIncludes.Expanded broken=SourceIncludes.expand(
                "ORG 32768\nINCLUDE \"broken.asm\"","main.asm",s->"BADOP");
        try {
            new Assembler().assemble(broken.source);
            fail("assembler must reject BADOP");
        }catch(IllegalArgumentException e){
            assertTrue(broken.explainError(e.getMessage())
                    .startsWith("line 2: broken.asm:1:"));
        }
    }

    @Test public void rootSymbolsAreVisibleInsideNestedModulesAndForwardReferences() {
        Map<String,String> files = new HashMap<>();
        files.put("lib/ship.asm", "SHIP_DRAW: JP MAIN_END\nDB VALUE");
        files.put("lib/constants.asm", "VALUE EQU 9");
        String root = "ORG 32768\nJP SHIP_DRAW\nINCLUDE \"lib/ship.asm\"\nINCLUDE \"lib/constants.asm\"\nMAIN_END: RET";
        SourceIncludes.Expanded project =
                SourceIncludes.expand(root, "main.asm", files::get);
        AssemblyResult built = new Assembler().assemble(project.source);
        assertArrayEquals(new byte[]{
                (byte)0xC3,3,(byte)0x80,
                (byte)0xC3,7,(byte)0x80,
                9,(byte)0xC9},built.getBytes());
        assertTrue(com.alexzab.z80pocketide.editor.SourceSymbols
                .definedLabels(project.source).contains("MAIN_END"));
        assertTrue(com.alexzab.z80pocketide.editor.SourceSymbols
                .definedLabels(project.source).contains("SHIP_DRAW"));
        assertTrue(com.alexzab.z80pocketide.editor.SourceSymbols
                .definedLabels(project.source).contains("VALUE"));
        AssemblyResult ship = project.projectFor(built,"lib/ship.asm",2);
        assertEquals(3,ship.getLineSize(0));
        assertEquals(1,ship.getLineSize(1));
        assertEquals(4,ship.getLineCount()==2?ship.getLineSize(0)+ship.getLineSize(1):0);
        assertTrue(project.containsPath("LIB/SHIP.ASM"));
    }

    @Test public void errorsNavigateOnlyWithinActualSourceFile() {
        SourceIncludes.Expanded project = SourceIncludes.expand(
                "ORG 32768\nINCLUDE \"lib/ship.asm\"",
                "main.asm",name -> "JP ABSENT");
        String mapped = project.explainErrorFor("line 2: unknown symbol ABSENT","lib/ship.asm");
        assertEquals("line 1: unknown symbol ABSENT",mapped);
        assertEquals("lib/ship.asm:1: unknown symbol ABSENT",
                project.explainErrorFor("line 2: unknown symbol ABSENT","main.asm"));
    }
}
