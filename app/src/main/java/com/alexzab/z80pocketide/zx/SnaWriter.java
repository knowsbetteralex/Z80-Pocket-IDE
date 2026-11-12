package com.alexzab.z80pocketide.zx;

import java.util.Arrays;

/**
 * Constructs a 48K ZX Spectrum SNA from assembled RAM code.
 *
 * A SNA is a machine snapshot, not a tape loader: PC is supplied via a
 * two-byte return address on the snapshot stack. The initial machine state
 * is deliberately minimal (IM 1, interrupts enabled, IY=$5C3A); programs
 * must initialize any BASIC/ROM variables they depend on.
 */
public final class SnaWriter {
    public static final int RAM_START = 0x4000;
    public static final int RAM_SIZE = 49152;
    public static final int HEADER_SIZE = 27;
    public static final int SNAPSHOT_SIZE = HEADER_SIZE + RAM_SIZE;
    public static final int SCREEN_SIZE = 6912;

    public byte[] programSna(int origin, byte[] code, byte[] screen) {
        if (code == null || code.length == 0)
            throw new IllegalArgumentException("SNA requires non-empty assembled code");
        if (origin < RAM_START || origin > 0xFFFF
                || (long) origin + code.length > 0x10000L)
            throw new IllegalArgumentException(
                    "48K SNA requires CODE entirely in RAM ($4000-$FFFF)");
        if (screen != null && screen.length != SCREEN_SIZE)
            throw new IllegalArgumentException("SCREEN$ must be exactly 6912 bytes");
        int codeEnd = origin + code.length; // exclusive
        if (screen != null && origin < 0x5B00 && codeEnd > RAM_START)
            throw new IllegalArgumentException(
                    "CODE overlaps the configured SCREEN$ ($4000-$5AFF)");

        byte[] result = new byte[SNAPSHOT_SIZE];
        // White PAPER / black INK by default: an empty screen is legible.
        if (screen == null)
            Arrays.fill(result, HEADER_SIZE + 6144,
                    HEADER_SIZE + SCREEN_SIZE, (byte) 0x38);
        else System.arraycopy(screen, 0, result, HEADER_SIZE, SCREEN_SIZE);
        System.arraycopy(code, 0, result,
                HEADER_SIZE + origin - RAM_START, code.length);

        // 48K SNA has NO PC field. A synthetic RETN will pop this address.
        // Avoid altering the assembled program or the screen with the PC word.
        int stack = findStackWord(origin, codeEnd);
        word(result, 0x17, stack); // SP -> pushed PC
        word(result, HEADER_SIZE + stack - RAM_START, origin);
        word(result, 0x0F, 0x5C3A); // IY: conventional ROM system-vars base
        result[0] = 0x3F; // I, IM1 uses the fixed $0038 vector
        result[0x13] = 0x04; // IFF2 enabled (restored by RETN)
        result[0x19] = 1; // interrupt mode 1
        result[0x1A] = 0; // black border
        return result;
    }

    private static int findStackWord(int codeStart, int codeEnd) {
        // Prefer SP=$FFFC, giving runtime SP=$FFFE after RETN.
        // Search for a genuinely unused two-byte RAM pair outside the screen.
        for (int address = 0xFFFC; address >= 0x5B00; address -= 2) {
            if (address + 2 <= codeStart || address >= codeEnd)
                return address;
        }
        throw new IllegalArgumentException(
                "No free two-byte RAM stack slot for SNA entry PC");
    }

    private static void word(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
    }
}
