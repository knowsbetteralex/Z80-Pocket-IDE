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
}
