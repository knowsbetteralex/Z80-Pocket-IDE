package com.alexzab.z80pocketide.examples;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
        assertFalse(ExamplePrograms.ALL.length < 8);
        for (ExamplePrograms.Example example : ExamplePrograms.ALL) {
            assertNotNull(example.id);
            assertFalse(example.title(AppLanguage.EN).isEmpty());
            assertFalse(example.title(AppLanguage.RU).isEmpty());
            assertFalse(example.details(AppLanguage.EN).isEmpty());
            assertFalse(example.details(AppLanguage.RU).isEmpty());
            assertNotNull(ExamplePrograms.findById(example.id));
        }
    }

    private static void assertAssembles(Assembler assembler, String source) {
        AssemblyResult result = assembler.assemble(source);
        assertNotNull(result);
        assertFalse(result.getBytes().length == 0);
    }
}
