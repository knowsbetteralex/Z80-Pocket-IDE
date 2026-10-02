package com.alexzab.z80pocketide.assembler;

public final class AssemblyResult {
    private final int origin;
    private final byte[] bytes;

    public AssemblyResult(int origin, byte[] bytes) {
        this.origin = origin;
        this.bytes = bytes;
    }

    public int getOrigin() {
        return origin;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
