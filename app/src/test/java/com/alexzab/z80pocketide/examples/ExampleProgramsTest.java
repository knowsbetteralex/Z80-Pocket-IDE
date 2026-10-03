package com.alexzab.z80pocketide.examples;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.alexzab.z80pocketide.assembler.Assembler;
import com.alexzab.z80pocketide.assembler.AssemblyResult;
import com.alexzab.z80pocketide.i18n.AppLanguage;

import org.junit.Test;

public class ExampleProgramsTest {
    @Test
    public void everyExampleAssemblesPlainAndCommented() {
        Assembler assembler = new Assembler();
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            assertAssembles(assembler, example.source(AppLanguage.EN, false));
            assertAssembles(assembler, example.source(AppLanguage.EN, true));
            assertAssembles(assembler, example.source(AppLanguage.RU, true));
        }
    }

    @Test
    public void examplesHaveDetailsAndStableIds() {
        assertFalse(ExamplePrograms.ALL.length < 12);
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            assertNotNull(example.id);
            assertFalse(example.title(AppLanguage.EN).isEmpty());
            assertFalse(example.title(AppLanguage.RU).isEmpty());
            assertFalse(example.details(AppLanguage.EN).isEmpty());
            assertFalse(example.details(AppLanguage.RU).isEmpty());
            assertNotNull(ExamplePrograms.findById(example.id));
        }
    }

    @Test
    public void usefulRoutinesDescribeRegisterContracts() {
        int routines = 0;
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            if ("Useful routines".equals(example.categoryEn)) {
                routines++;
                assertTrue(example.hasIo());
                assertTrue(example.io(AppLanguage.EN).contains("IN:"));
                assertTrue(example.io(AppLanguage.EN).contains("OUT:"));
                assertTrue(example.io(AppLanguage.RU).contains("IN:"));
                assertTrue(example.io(AppLanguage.RU).contains("OUT:"));
            }
        }
        assertTrue(routines >= 7);
    }

    private static void assertAssembles(Assembler assembler, String source) {
        AssemblyResult result = assembler.assemble(source);
        assertNotNull(result);
        assertFalse(result.getBytes().length == 0);
    }
}
