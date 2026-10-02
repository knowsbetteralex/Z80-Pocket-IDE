package com.alexzab.z80pocketide.zx;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Minimal ZX Spectrum .tap CODE block writer. */
public final class TapWriter {
    public byte[] codeTap(String name, int loadAddress, byte[] code) {
        ByteArrayOutputStream tap = new ByteArrayOutputStream();
        byte[] header = new byte[19];
        header[0] = 0x00;
        header[1] = 0x03; // CODE
        byte[] n = name.toUpperCase().getBytes(StandardCharsets.US_ASCII);
        Arrays.fill(header, 2, 12, (byte) ' ');
        System.arraycopy(n, 0, header, 2, Math.min(n.length, 10));
        put16(header, 12, code.length);
        put16(header, 14, loadAddress);
        put16(header, 16, 0x8000);
        header[18] = checksum(header, 0, 18);
        writeBlock(tap, header);

        byte[] data = new byte[code.length + 2];
        data[0] = (byte) 0xFF;
        System.arraycopy(code, 0, data, 1, code.length);
        data[data.length - 1] = checksum(data, 0, data.length - 1);
        writeBlock(tap, data);
        return tap.toByteArray();
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
