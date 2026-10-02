package com.alexzab.z80pocketide.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Searchable compact reference for the documented Z80 instruction families. */
public final class InstructionReference {
    public static final class Entry {
        public final String mnemonic;
        public final String category;
        public final String syntax;
        public final String opcode;
        public final String tStates;
        public final String flags;
        public final String description;

        Entry(String mnemonic, String category, String syntax, String opcode,
              String tStates, String flags, String description) {
            this.mnemonic = mnemonic;
            this.category = category;
            this.syntax = syntax;
            this.opcode = opcode;
            this.tStates = tStates;
            this.flags = flags;
            this.description = description;
        }

        boolean matches(String q) {
            String haystack = (mnemonic + " " + category + " " + syntax + " " +
                    opcode + " " + tStates + " " + flags + " " + description)
                    .toUpperCase(Locale.ROOT);
            return haystack.contains(q);
        }
    }

    private static Entry e(String m, String c, String s, String o,
                           String t, String f, String d) {
        return new Entry(m, c, s, o, t, f, d);
    }

    public static final List<Entry> ALL = Collections.unmodifiableList(Arrays.asList(
            e("LD", "Data", "LD r,r' | LD r,n | LD rr,nn | LD r,(HL) | LD (HL),r | LD r,(IX+d) | LD (IX+d),r | LD A,(nn) | LD (nn),A",
                    "40+8*d+s / 06+8*r n / 01+16*rr nn / indexed: DD/FD ... d",
                    "4; immediate 7/10; (HL) 7; absolute 13–20; indexed 19",
                    "unchanged", "Copy 8-bit or 16-bit data between registers and memory."),
            e("INC", "Arithmetic", "INC r | INC rr | INC (HL) | INC (IX+d)",
                    "04+8*r / 03+16*rr / 34 / DD/FD 34 d", "4 / 6 / 11 / 23",
                    "S Z H P/V N; C unchanged", "Increment operand by one."),
            e("DEC", "Arithmetic", "DEC r | DEC rr | DEC (HL) | DEC (IX+d)",
                    "05+8*r / 0B+16*rr / 35 / DD/FD 35 d", "4 / 6 / 11 / 23",
                    "S Z H P/V N; C unchanged", "Decrement operand by one."),
            e("ADD", "Arithmetic", "ADD A,r | ADD A,n | ADD A,(HL) | ADD A,(IX+d) | ADD HL,rr | ADD IX/IY,rr",
                    "80+r / C6 n / 86 / DD/FD 86 d / 09+16*rr", "4 / 7 / 7 / 19 / 11 / 15",
                    "S Z H P/V N C (8-bit); H N C (16-bit)", "Add operand to accumulator or 16-bit index/pair."),
            e("ADC", "Arithmetic", "ADC A,r | ADC A,n | ADC A,(HL) | ADC A,(IX+d) | ADC HL,rr",
                    "88+r / CE n / 8E / DD/FD 8E d / ED 4A+16*rr", "4 / 7 / 7 / 19 / 15",
                    "S Z H P/V N C", "Add operand plus carry."),
            e("SUB", "Arithmetic", "SUB r | SUB n | SUB (HL) | SUB (IX+d)",
                    "90+r / D6 n / 96 / DD/FD 96 d", "4 / 7 / 7 / 19",
                    "S Z H P/V N C", "Subtract operand from A."),
            e("SBC", "Arithmetic", "SBC A,r | SBC A,n | SBC A,(HL) | SBC A,(IX+d) | SBC HL,rr",
                    "98+r / DE n / 9E / DD/FD 9E d / ED 42+16*rr", "4 / 7 / 7 / 19 / 15",
                    "S Z H P/V N C", "Subtract operand and carry."),
            e("AND", "Logic", "AND r | AND n | AND (HL) | AND (IX+d)",
                    "A0+r / E6 n / A6 / DD/FD A6 d", "4 / 7 / 7 / 19",
                    "S Z H P/V N C", "Bitwise AND with A."),
            e("OR", "Logic", "OR r | OR n | OR (HL) | OR (IX+d)",
                    "B0+r / F6 n / B6 / DD/FD B6 d", "4 / 7 / 7 / 19",
                    "S Z H P/V N C", "Bitwise OR with A."),
            e("XOR", "Logic", "XOR r | XOR n | XOR (HL) | XOR (IX+d)",
                    "A8+r / EE n / AE / DD/FD AE d", "4 / 7 / 7 / 19",
                    "S Z H P/V N C", "Bitwise exclusive OR with A."),
            e("CP", "Logic", "CP r | CP n | CP (HL) | CP (IX+d)",
                    "B8+r / FE n / BE / DD/FD BE d", "4 / 7 / 7 / 19",
                    "S Z H P/V N C", "Compare A with operand without changing A."),
            e("DAA", "CPU", "DAA", "27", "4", "S Z H P/V N C", "Decimal-adjust A after BCD arithmetic."),
            e("CPL", "CPU", "CPL", "2F", "4", "H N set", "Complement all bits in A."),
            e("NEG", "CPU", "NEG", "ED 44", "8", "S Z H P/V N C", "Two's-complement negate A."),
            e("CCF", "CPU", "CCF", "3F", "4", "H N C", "Complement carry flag."),
            e("SCF", "CPU", "SCF", "37", "4", "H N C", "Set carry flag."),
            e("NOP", "CPU", "NOP", "00", "4", "unchanged", "Do nothing for one instruction cycle."),
            e("HALT", "CPU", "HALT", "76", "4 per refresh cycle", "unchanged", "Pause instruction execution until an interrupt/reset condition."),
            e("DI", "Interrupt", "DI", "F3", "4", "IFF1/IFF2 cleared", "Disable maskable interrupts."),
            e("EI", "Interrupt", "EI", "FB", "4", "IFF1/IFF2 set after next instruction", "Enable maskable interrupts."),
            e("IM", "Interrupt", "IM 0 | IM 1 | IM 2", "ED 46 / ED 56 / ED 5E", "8", "unchanged", "Select interrupt mode."),
            e("JP", "Flow", "JP nn | JP cc,nn | JP (HL) | JP (IX) | JP (IY)",
                    "C3 nn / C2+8*cc nn / E9 / DD E9 / FD E9", "10 / 10 / 4 / 8",
                    "unchanged", "Absolute jump, optionally conditional."),
            e("JR", "Flow", "JR e | JR NZ/Z/NC/C,e", "18 e / 20+8*cc e", "12; conditional 12 taken / 7 not",
                    "unchanged", "Relative jump within -128..+127 bytes."),
            e("DJNZ", "Flow", "DJNZ e", "10 e", "13 taken / 8 not", "unchanged", "Decrement B and jump while B is non-zero."),
            e("CALL", "Flow", "CALL nn | CALL cc,nn", "CD nn / C4+8*cc nn", "17; conditional 17 taken / 10 not",
                    "unchanged", "Call subroutine by pushing return address."),
            e("RET", "Flow", "RET | RET cc", "C9 / C0+8*cc", "10; conditional 11 taken / 5 not",
                    "unchanged", "Return from subroutine."),
            e("RETI", "Flow", "RETI", "ED 4D", "14", "IFF handling", "Return from interrupt; intended for maskable interrupt service."),
            e("RETN", "Flow", "RETN", "ED 45", "14", "IFF1←IFF2", "Return from non-maskable interrupt."),
            e("RST", "Flow", "RST 0,8,$10,$18,$20,$28,$30,$38", "C7 + vector", "11", "unchanged", "One-byte call to a fixed low-memory vector."),
            e("PUSH", "Stack", "PUSH AF/BC/DE/HL | PUSH IX/IY", "C5+16*qq / DD/FD E5", "11 / 15",
                    "unchanged", "Push register pair onto stack."),
            e("POP", "Stack", "POP AF/BC/DE/HL | POP IX/IY", "C1+16*qq / DD/FD E1", "10 / 14",
                    "AF changes flags when popped", "Pop register pair from stack."),
            e("EX", "Data", "EX DE,HL | EX AF,AF' | EX (SP),HL | EX (SP),IX/IY",
                    "EB / 08 / E3 / DD/FD E3", "4 / 4 / 19 / 23", "unchanged", "Exchange register sets or register pair with stack word."),
            e("EXX", "Data", "EXX", "D9", "4", "unchanged", "Exchange BC/DE/HL with alternate register set."),
            e("BIT", "Bit", "BIT b,r | BIT b,(HL) | BIT b,(IX+d)", "CB 40+8*b+r / DD/FD CB d 46+8*b",
                    "8 / 12 / 20", "S Z H P/V N; C unchanged", "Test one bit without changing operand."),
            e("SET", "Bit", "SET b,r | SET b,(HL) | SET b,(IX+d)", "CB C0+8*b+r / DD/FD CB d C6+8*b",
                    "8 / 15 / 23", "unchanged", "Set one bit."),
            e("RES", "Bit", "RES b,r | RES b,(HL) | RES b,(IX+d)", "CB 80+8*b+r / DD/FD CB d 86+8*b",
                    "8 / 15 / 23", "unchanged", "Reset one bit."),
            e("RLC/RRC/RL/RR", "Bit", "RLC/RRC/RL/RR r | (HL) | (IX+d)", "CB group; indexed DD/FD CB d op",
                    "8 / 15 / 23", "S Z H P/V N C", "Rotate an 8-bit operand."),
            e("SLA/SRA/SRL", "Bit", "SLA/SRA/SRL r | (HL) | (IX+d)", "CB group; indexed DD/FD CB d op",
                    "8 / 15 / 23", "S Z H P/V N C", "Shift an 8-bit operand."),
            e("RLCA/RRCA/RLA/RRA", "Bit", "RLCA | RRCA | RLA | RRA", "07 / 0F / 17 / 1F", "4",
                    "H N C; S Z P/V unchanged", "Fast accumulator-only rotate instructions."),
            e("RLD", "Bit", "RLD", "ED 6F", "18", "S Z H P/V N; C unchanged", "Rotate nibbles between A and (HL) left."),
            e("RRD", "Bit", "RRD", "ED 67", "18", "S Z H P/V N; C unchanged", "Rotate nibbles between A and (HL) right."),
            e("IN", "I/O", "IN A,(n) | IN r,(C)", "DB n / ED 40+8*r", "11 / 12",
                    "IN r,(C) updates S Z H P/V N; C unchanged", "Read a byte from an I/O port."),
            e("OUT", "I/O", "OUT (n),A | OUT (C),r", "D3 n / ED 41+8*r", "11 / 12",
                    "unchanged", "Write a byte to an I/O port."),
            e("LDI", "Block", "LDI", "ED A0", "16", "H P/V N", "Copy (HL) to (DE), increment pointers, decrement BC."),
            e("LDIR", "Block", "LDIR", "ED B0", "21 while repeating / 16 last", "H P/V N", "Repeat LDI until BC becomes zero."),
            e("LDD", "Block", "LDD", "ED A8", "16", "H P/V N", "Copy (HL) to (DE), decrement pointers, decrement BC."),
            e("LDDR", "Block", "LDDR", "ED B8", "21 while repeating / 16 last", "H P/V N", "Repeat LDD until BC becomes zero."),
            e("CPI", "Block", "CPI", "ED A1", "16", "S Z H P/V N; C unchanged", "Compare A with (HL), increment HL, decrement BC."),
            e("CPIR", "Block", "CPIR", "ED B1", "21 while repeating / 16 last", "S Z H P/V N; C unchanged", "Repeat CPI until match or BC=0."),
            e("CPD", "Block", "CPD", "ED A9", "16", "S Z H P/V N; C unchanged", "Compare A with (HL), decrement HL and BC."),
            e("CPDR", "Block", "CPDR", "ED B9", "21 while repeating / 16 last", "S Z H P/V N; C unchanged", "Repeat CPD until match or BC=0."),
            e("INI/IND", "Block I/O", "INI | IND", "ED A2 / ED AA", "16", "complex", "Input through port (C) to (HL), adjust B and HL."),
            e("INIR/INDR", "Block I/O", "INIR | INDR", "ED B2 / ED BA", "21 while repeating / 16 last", "complex", "Repeat block input until B reaches zero."),
            e("OUTI/OUTD", "Block I/O", "OUTI | OUTD", "ED A3 / ED AB", "16", "complex", "Output (HL) through port (C), adjust B and HL."),
            e("OTIR/OTDR", "Block I/O", "OTIR | OTDR", "ED B3 / ED BB", "21 while repeating / 16 last", "complex", "Repeat block output until B reaches zero."),
            e("ORG", "Directive", "ORG expression", "assembler directive", "—", "—", "Set assembly address."),
            e("EQU", "Directive", "name EQU expression", "assembler directive", "—", "—", "Define a constant symbol."),
            e("DB", "Directive", "DB value,... | DB \"text\"", "assembler directive", "—", "—", "Emit bytes or string data."),
            e("DW", "Directive", "DW value,...", "assembler directive", "—", "—", "Emit little-endian 16-bit words."),
            e("DS", "Directive", "DS count[,fill]", "assembler directive", "—", "—", "Reserve/fill a number of bytes.")
    ));

    private InstructionReference() {}

    public static List<Entry> search(String query) {
        if (query == null || query.trim().isEmpty()) return ALL;
        String q = query.trim().toUpperCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry entry : ALL) if (entry.matches(q)) result.add(entry);
        return result;
    }
}
