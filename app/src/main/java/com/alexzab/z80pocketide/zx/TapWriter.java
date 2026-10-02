package com.alexzab.z80pocketide.zx;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** ZX Spectrum .tap writer with an optional autostart BASIC loader. */
public final class TapWriter {
    private static final int TOKEN_CODE = 0xAF;
    private static final int TOKEN_VAL = 0xB0;
    private static final int TOKEN_USR = 0xC0;
    private static final int TOKEN_LOAD = 0xEF;
    private static final int TOKEN_RANDOMIZE = 0xF9;
    private static final int TOKEN_CLEAR = 0xFD;

    /** Writes only a CODE file to TAP. */
    public byte[] codeTap(String name, int loadAddress, byte[] code) {
        ByteArrayOutputStream tap = new ByteArrayOutputStream();
        writeFile(tap, 3, name, code, loadAddress, 0x8000);
        return tap.toByteArray();
    }

    /**
     * Writes an autorun BASIC loader followed by the CODE file.
     * Loader: CLEAR loadAddress-1 : LOAD "" CODE : RANDOMIZE USR loadAddress
     */
    public byte[] programTap(String name, int loadAddress, byte[] code) {
        ByteArrayOutputStream tap = new ByteArrayOutputStream();
        byte[] loader = basicLoader(loadAddress);
        writeFile(tap, 0, "RUN", loader, 10, loader.length);
        writeFile(tap, 3, name, code, loadAddress, 0x8000);
        return tap.toByteArray();
    }

    private static byte[] basicLoader(int address) {
        int clearAddress = Math.max(0, (address - 1) & 0xFFFF);
        ByteArrayOutputStream text = new ByteArrayOutputStream();

        text.write(TOKEN_CLEAR);
        text.write(' ');
        writeValString(text, clearAddress);
        text.write(':');

        text.write(TOKEN_LOAD);
        text.write(' ');
        text.write('"');
        text.write('"');
        text.write(' ');
        text.write(TOKEN_CODE);
        text.write(':');

        text.write(TOKEN_RANDOMIZE);
        text.write(' ');
        text.write(TOKEN_USR);
        text.write(' ');
        writeValString(text, address & 0xFFFF);

        byte[] body = text.toByteArray();
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        line.write(0x00); // line 10, big-endian
        line.write(0x0A);
        int length = body.length + 1; // includes line terminator
        line.write(length & 0xFF);
        line.write((length >>> 8) & 0xFF);
        line.write(body, 0, body.length);
        line.write(0x0D);
        return line.toByteArray();
    }

    private static void writeValString(ByteArrayOutputStream out, int value) {
        out.write(TOKEN_VAL);
        out.write(' ');
        out.write('"');
        byte[] digits = Integer.toString(value).getBytes(StandardCharsets.US_ASCII);
        out.write(digits, 0, digits.length);
        out.write('"');
    }

    private static void writeFile(ByteArrayOutputStream tap, int type, String name,
                                  byte[] data, int parameter1, int parameter2) {
        byte[] header = new byte[19];
        header[0] = 0x00;          // header flag
        header[1] = (byte) type;   // 0=PROGRAM, 3=CODE

        byte[] n = name.toUpperCase().getBytes(StandardCharsets.US_ASCII);
        Arrays.fill(header, 2, 12, (byte) ' ');
        System.arraycopy(n, 0, header, 2, Math.min(n.length, 10));

        put16(header, 12, data.length);
        put16(header, 14, parameter1);
        put16(header, 16, parameter2);
        header[18] = checksum(header, 0, 18);
        writeBlock(tap, header);

        byte[] payload = new byte[data.length + 2];
        payload[0] = (byte) 0xFF;
        System.arraycopy(data, 0, payload, 1, data.length);
        payload[payload.length - 1] = checksum(payload, 0, payload.length - 1);
        writeBlock(tap, payload);
    }

    private static void writeBlock(ByteArrayOutputStream out, byte[] block) {
        out.write(block.length & 0xFF);
        out.write((block.length >>> 8) & 0xFF);
        out.write(block, 0, block.length);
    }

    private static void put16(byte[] b, int p, int v) {
        b[p] = (byte) v;
        b[p + 1] = (byte) (v >>> 8);
    }

    private static byte checksum(byte[] b, int from, int toExclusive) {
        int c = 0;
        for (int i = from; i < toExclusive; i++) c ^= b[i] & 0xFF;
        return (byte) c;
    }
}
