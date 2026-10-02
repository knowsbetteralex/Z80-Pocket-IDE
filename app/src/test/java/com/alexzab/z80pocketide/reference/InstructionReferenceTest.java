package com.alexzab.z80pocketide.reference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InstructionReferenceTest {
    @Test
    public void referenceContainsCoreFamiliesAndDirectives() {
        assertFalse(InstructionReference.ALL.isEmpty());
        assertFalse(InstructionReference.search("LD").isEmpty());
        assertFalse(InstructionReference.search("JR").isEmpty());
        assertFalse(InstructionReference.search("LDIR").isEmpty());
        assertFalse(InstructionReference.search("ORG").isEmpty());
    }

    @Test
    public void searchFindsOpcodeAndDescriptionTerms() {
        assertFalse(InstructionReference.search("ED B0").isEmpty());
        assertFalse(InstructionReference.search("relative jump").isEmpty());
        assertTrue(InstructionReference.search("definitely-not-a-z80-token").isEmpty());
    }
}
