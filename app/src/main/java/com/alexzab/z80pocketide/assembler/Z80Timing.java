package com.alexzab.z80pocketide.assembler;

/**
 * Z80 instruction duration from already encoded bytes.
 * Timings are for one execution of an instruction and ignore bus contention.
 * Conditional instructions expose the not-taken/taken range; repeating ED
 * instructions show one terminal/repeat step (not a full BC iteration count).
 */
public final class Z80Timing {
    public static final class Cycles {
        public final int min;
        public final int max;
        public Cycles(int min, int max) { this.min = min; this.max = max; }
    }

    private Z80Timing() {}
    private static Cycles fixed(int t) { return new Cycles(t, t); }
    private static Cycles range(int a, int b) { return new Cycles(a, b); }

    public static Cycles decode(byte[] instruction) {
        if (instruction == null || instruction.length == 0) return fixed(0);
        int b = instruction[0] & 255;
        if (b == 0xCB && instruction.length >= 2) {
            int op = instruction[1] & 255;
            if ((op & 7) != 6) return fixed(8);
            return fixed((op & 0xC0) == 0x40 ? 12 : 15);
        }
        if (b == 0xED && instruction.length >= 2) return ed(instruction[1] & 255);
        if ((b == 0xDD || b == 0xFD) && instruction.length >= 2) {
            return indexed(instruction);
        }
        return main(b);
    }

    private static Cycles indexed(byte[] bytes) {
        int op = bytes[1] & 255;
        if (op == 0xCB && bytes.length >= 4) {
            return fixed(((bytes[3] & 0xC0) == 0x40) ? 20 : 23);
        }
        if (op == 0x21) return fixed(14);
        if (op == 0x22 || op == 0x2A) return fixed(20);
        if (op == 0x23 || op == 0x2B) return fixed(10);
        if ((op & 0xCF) == 0x09) return fixed(15);
        if (op == 0x34 || op == 0x35) return fixed(23);
        if (op == 0x36) return fixed(19);
        if ((op >= 0x40 && op <= 0x7F && op != 0x76)
                && (((op >> 3) & 7) == 6 || (op & 7) == 6)) return fixed(19);
        if (op >= 0x80 && op <= 0xBF && (op & 7) == 6) return fixed(19);
        if (op == 0xE1) return fixed(14);
        if (op == 0xE5) return fixed(15);
        if (op == 0xE3) return fixed(23);
        if (op == 0xE9) return fixed(8);
        if (op == 0xF9) return fixed(10);
        Cycles normal = main(op);
        return new Cycles(normal.min + 4, normal.max + 4);
    }

    private static Cycles ed(int op) {
        if ((op & 0xC7) == 0x40 || (op & 0xC7) == 0x41) return fixed(12);
        if ((op & 0xCF) == 0x42 || (op & 0xCF) == 0x4A) return fixed(15);
        if ((op & 0xCF) == 0x43 || (op & 0xCF) == 0x4B) return fixed(20);
        if ((op & 0xC7) == 0x44) return fixed(8); // NEG
        if ((op & 0xC7) == 0x45 || (op & 0xC7) == 0x4D) return fixed(14);
        if ((op & 0xC7) == 0x46 || (op & 0xC7) == 0x56
                || (op & 0xC7) == 0x5E) return fixed(8); // IM
        if (op == 0x47 || op == 0x4F || op == 0x57 || op == 0x5F) return fixed(9);
        if (op == 0x67 || op == 0x6F) return fixed(18);
        if ((op >= 0xA0 && op <= 0xAB) && (op & 3) <= 3) return fixed(16);
        if (op == 0xA8 || op == 0xA9 || op == 0xAA || op == 0xAB) return fixed(16);
        if (op == 0xB0 || op == 0xB1 || op == 0xB2 || op == 0xB3 ||
                op == 0xB8 || op == 0xB9 || op == 0xBA || op == 0xBB)
            return range(16,21);
        return fixed(8);
    }

    private static Cycles main(int op) {
        if (op >= 0x40 && op <= 0x7F) {
            if (op == 0x76) return fixed(4);
            return fixed(((op >> 3) & 7) == 6 || (op & 7) == 6 ? 7 : 4);
        }
        if (op >= 0x80 && op <= 0xBF) return fixed((op & 7) == 6 ? 7 : 4);

        if ((op & 0xC7) == 0x06) return fixed(((op >> 3) & 7) == 6 ? 10 : 7);
        if ((op & 0xC7) == 0x04 || (op & 0xC7) == 0x05)
            return fixed(((op >> 3) & 7) == 6 ? 11 : 4);
        if ((op & 0xCF) == 0x01) return fixed(10);
        if ((op & 0xCF) == 0x03 || (op & 0xCF) == 0x0B) return fixed(6);
        if ((op & 0xCF) == 0x09) return fixed(11);
        if (op == 0x02 || op == 0x12 || op == 0x0A || op == 0x1A) return fixed(7);
        if (op == 0x22 || op == 0x2A) return fixed(16);
        if (op == 0x32 || op == 0x3A) return fixed(13);
        if (op == 0x08 || op == 0xD9 || op == 0xEB || op == 0xF3 || op == 0xFB)
            return fixed(4);
        if (op == 0xE3) return fixed(19);
        if (op == 0xF9) return fixed(6);
        if ((op & 0xC7) == 0xC0) return range(5,11);
        if (op == 0xC9) return fixed(10);
        if ((op & 0xC7) == 0xC2 || op == 0xC3 || op == 0xE9) {
            return fixed(op == 0xE9 ? 4 : 10);
        }
        if ((op & 0xC7) == 0xC4) return range(10,17);
        if (op == 0xCD) return fixed(17);
        if ((op & 0xC7) == 0xC7) return fixed(11);
        if ((op & 0xCF) == 0xC1) return fixed(10);
        if ((op & 0xCF) == 0xC5) return fixed(11);
        if (op == 0x10) return range(8,13);
        if (op == 0x18) return fixed(12);
        if (op == 0x20 || op == 0x28 || op == 0x30 || op == 0x38)
            return range(7,12);
        if (op == 0xD3 || op == 0xDB) return fixed(11);
        if ((op & 0xC7) == 0xC6) return fixed(7);
        return fixed(4);
    }
}
