package com.alexzab.z80pocketide.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AssemblyTimingTest {
    @Test
    public void recordsInstructionBytesAndTimingsBySourceLine() {
        String source = "ORG $8000\n"
                + "START:\n"
                + "    LD A,7\n"
                + "    OUT ($FE),A\n"
                + "    JR NZ,START\n"
                + "    RET\n";
        AssemblyResult r = new Assembler().assemble(source);
        assertEquals(7, r.getLineCount()); // trailing newline creates a blank source line
        assertEquals(0, r.getLineSize(0));
        assertFalse(r.isInstruction(1));
        assertEquals(2, r.getLineSize(2));
        assertEquals(7, r.getLineMinCycles(2));
        assertEquals(2, r.getLineSize(3));
        assertEquals(11, r.getLineMaxCycles(3));
        assertEquals(2, r.getLineSize(4));
        assertEquals(7, r.getLineMinCycles(4));
        assertEquals(12, r.getLineMaxCycles(4));
        assertEquals(10, r.getLineMinCycles(5));
        assertEquals(6 + 1, r.getBytes().length); // LD2 + OUT2 + JR2 + RET1
        assertEquals(35, r.getMinCycles());
        assertEquals(40, r.getMaxCycles());
    }

    @Test
    public void indexedBitAndRepeatTiming() {
        AssemblyResult r = new Assembler().assemble(
                "ORG $8000\n"
                + "    BIT 3,(IX+2)\n"
                + "    SET 4,(IY-1)\n"
                + "    LDIR\n");
        assertEquals(4, r.getLineSize(1));
        assertEquals(20, r.getLineMinCycles(1));
        assertEquals(4, r.getLineSize(2));
        assertEquals(23, r.getLineMinCycles(2));
        assertEquals(2, r.getLineSize(3));
        assertEquals(16, r.getLineMinCycles(3));
        assertEquals(21, r.getLineMaxCycles(3));
    }

    @Test
    public void dataConsumesBytesButNoRuntimeCycles() {
        AssemblyResult r = new Assembler().assemble(
                "ORG $8000\nDB 1,2,3\nDW $1234\nDS 4\n");
        assertEquals(9, r.getBytes().length);
        assertEquals(3, r.getLineSize(1));
        assertEquals(2, r.getLineSize(2));
        assertEquals(4, r.getLineSize(3));
        assertEquals(0, r.getMinCycles());
        assertFalse(r.isInstruction(1));
    }

    @Test
    public void basicOpcodesTiming() {
        assertEquals(4, Z80Timing.decode(new byte[]{(byte)0x7F}).min);
        assertEquals(7, Z80Timing.decode(new byte[]{(byte)0x46}).min);
        assertEquals(8, Z80Timing.decode(new byte[]{(byte)0xCB, (byte)0x11}).min);
        assertEquals(12, Z80Timing.decode(new byte[]{(byte)0xCB, (byte)0x46}).min);
        assertEquals(15, Z80Timing.decode(new byte[]{(byte)0xCB, (byte)0xC6}).min);
    }
}
