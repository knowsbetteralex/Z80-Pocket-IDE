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

    @Test
    public void assemblesCharacterOperandsWithoutChangingCase() {
        AssemblyResult result = new Assembler().assemble(
                "ORG $8000\n"
                + "LD A,'1'\n"
                + "LD B,'a'\n"
                + "LD C,'A'\n"
                + "CP 'z'\n"
                + "ADD A,'0'\n"
                + "LD (HL),'!'\n"
                + "LD (IX+2),'?'\n");
        assertArrayEquals(new byte[]{
                0x3e,0x31,
                0x06,0x61,
                0x0e,0x41,
                (byte)0xfe,0x7a,
                (byte)0xc6,0x30,
                0x36,0x21,
                (byte)0xdd,0x36,0x02,0x3f
        },result.getBytes());
    }

    @Test
    public void characterLiteralsWorkInExpressionsEquAndData() {
        AssemblyResult result = new Assembler().assemble(
                "ORG $8000\n"
                + "LOWER EQU 'a'\n"
                + "LD A,LOWER-32\n"
                + "DB 'A'+1,LOWER\n"
                + "DW '0'+1\n"
                + "DS 1,'!'\n");
        assertArrayEquals(new byte[]{
                0x3e,0x41,
                0x42,0x61,
                0x31,0x00,
                0x21
        },result.getBytes());
    }

    @Test
    public void escapedAndPunctuationCharactersWork() {
        AssemblyResult result = new Assembler().assemble(
                "ORG $8000\n"
                + "LD A,'\\n'\n"
                + "LD C,'\\''\n"
                + "LD B,'\\\\'\n"
                + "LD D,'\\x41'\n"
                + "CP ','\n"
                + "LD A,';'\n"
                + "LD E,' '\n");
        assertArrayEquals(new byte[]{
                0x3e,0x0a,
                0x0e,0x27,
                0x06,0x5c,
                0x16,0x41,
                (byte)0xfe,0x2c,
                0x3e,0x3b,
                0x1e,0x20
        },result.getBytes());
    }

    @Test
    public void invalidCharacterLiteralsShowLineNumber() {
        String[] invalid = {
                "LD A,''", "LD A,'AB'", "LD A,'\\q'", "LD A,'\\xG1'",
                "LD A,'あ'", "LD A,'A"
        };
        for(String operand : invalid) {
            try {
                new Assembler().assemble("ORG $8000\n"+operand+"\n");
                org.junit.Assert.fail("Expected invalid char literal: "+operand);
            } catch (IllegalArgumentException ex) {
                org.junit.Assert.assertTrue(ex.getMessage(),
                        ex.getMessage().startsWith("line 2:"));
            }
        }
    }

    @Test
    public void alternateAfRegisterStillWorksWithComments() {
        AssemblyResult result = new Assembler().assemble(
                "ORG $8000\nEX AF,AF'\n");
        assertArrayEquals(new byte[]{0x08},result.getBytes());
    }
}
