package com.alexzab.z80pocketide.assembler;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssemblerTest {
    @Test
    public void assemblesBootstrapProgram() {
        AssemblyResult r = new Assembler().assemble(
                "ORG $8000\n" +
                "start:\n" +
                "LD A,2\n" +
                "OUT (254),A\n" +
                "JP start\n");
        assertEquals(0x8000, r.getOrigin());
        assertArrayEquals(new byte[]{0x3E, 0x02, (byte) 0xD3, (byte) 0xFE, (byte) 0xC3, 0x00, (byte) 0x80}, r.getBytes());
    }

    @Test
    public void resolvesRelativeJump() {
        AssemblyResult r = new Assembler().assemble("ORG 32768\nloop:\nNOP\nJR loop\n");
        assertArrayEquals(new byte[]{0x00, 0x18, (byte) 0xFD}, r.getBytes());
    }
}
