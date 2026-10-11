package com.alexzab.z80pocketide.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.List;

public class AssemblyDiagnosticsTest {
    @Test
    public void findsSeveralIndependentInstructionErrors() {
        String source = "ORG $8000\n"
                + "START:\n"
                + "    THISISNOTANOP\n"
                + "    LD A,1\n"
                + "    OTHERBADOP\n"
                + "    JR START\n";
        List<AssemblyDiagnostics.Issue> issues = AssemblyDiagnostics.collect(source, 8);
        assertEquals(2, issues.size());
        assertEquals(3, issues.get(0).line);
        assertEquals(5, issues.get(1).line);
    }

    @Test
    public void reportsNothingForValidSource() {
        assertTrue(AssemblyDiagnostics.collect("ORG $8000\n    LD A,1\n    RET\n",8).isEmpty());
    }
}
