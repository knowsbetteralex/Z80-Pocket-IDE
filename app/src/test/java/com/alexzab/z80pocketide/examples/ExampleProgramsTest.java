package com.alexzab.z80pocketide.examples;

import static org.junit.Assert.assertTrue;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;

import org.junit.Test;

public class ExampleProgramsTest {
    @Test
    public void allBuiltInExamplesAssemble() {
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            AssemblyResult result = new Assembler().assemble(example.source);
            assertTrue(example.title, result.getBytes().length > 0);
        }
    }
}
