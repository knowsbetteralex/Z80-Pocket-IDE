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
        assertArrayEquals(new byte[]{0x3E, 0x02, (byte)0xD3, (byte)0xFE, (byte)0xC3, 0x00, (byte)0x80}, r.getBytes());
    }

    @Test
    public void resolvesRelativeJump() {
        AssemblyResult r = new Assembler().assemble("ORG 32768\nloop:\nNOP\nJR loop\n");
        assertArrayEquals(new byte[]{0x00, 0x18, (byte)0xFD}, r.getBytes());
    }

    @Test
    public void supportsEquExpressionsAndDataDirectives() {
        AssemblyResult r = new Assembler().assemble(
                "ORG $8000\n" +
                "COUNT EQU 2+2\n" +
                "DS COUNT,$AA\n" +
                "msg: DB \"HI\",13\n" +
                "DW msg+1\n");
        assertArrayEquals(new byte[]{
                (byte)0xAA,(byte)0xAA,(byte)0xAA,(byte)0xAA,
                'H','I',13, 0x05,(byte)0x80
        }, r.getBytes());
    }

    @Test
    public void assemblesIndexedAndPrefixedInstructions() {
        AssemblyResult r = new Assembler().assemble(
                "ORG $9000\n" +
                "LD IX,$A000\n" +
                "LD A,(IX+5)\n" +
                "LD (IY-2),B\n" +
                "BIT 3,(IX+1)\n" +
                "SET 7,A\n" +
                "ADC HL,DE\n" +
                "LD ($8000),SP\n" +
                "LD BC,($8000)\n" +
                "LDIR\n");
        assertArrayEquals(new byte[]{
                (byte)0xDD,0x21,0x00,(byte)0xA0,
                (byte)0xDD,0x7E,0x05,
                (byte)0xFD,0x70,(byte)0xFE,
                (byte)0xDD,(byte)0xCB,0x01,0x5E,
                (byte)0xCB,(byte)0xFF,
                (byte)0xED,0x5A,
                (byte)0xED,0x73,0x00,(byte)0x80,
                (byte)0xED,0x4B,0x00,(byte)0x80,
                (byte)0xED,(byte)0xB0
        }, r.getBytes());
    }

    @Test
    public void reportsLineNumber() {
        try {
            new Assembler().assemble("ORG $8000\nLD A,1\nWAT A,B\n");
        } catch (IllegalArgumentException ex) {
            org.junit.Assert.assertTrue(ex.getMessage().startsWith("line 3:"));
            return;
        }
        org.junit.Assert.fail("expected assembly error");
    }
}
