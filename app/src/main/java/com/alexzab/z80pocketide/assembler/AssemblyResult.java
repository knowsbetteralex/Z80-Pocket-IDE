package com.alexzab.z80pocketide.assembler;

public final class AssemblyResult {
    private final int origin;
    private final byte[] bytes;
    private final int[] lineSizes;
    private final int[] lineMinCycles;
    private final int[] lineMaxCycles;
    private final boolean[] instructions;

    public AssemblyResult(int origin, byte[] bytes) {
        this(origin, bytes, new int[0], new int[0], new int[0], new boolean[0]);
    }

    public AssemblyResult(int origin, byte[] bytes, int[] lineSizes,
                          int[] lineMinCycles, int[] lineMaxCycles, boolean[] instructions) {
        this.origin = origin;
        this.bytes = bytes;
        this.lineSizes = lineSizes.clone();
        this.lineMinCycles = lineMinCycles.clone();
        this.lineMaxCycles = lineMaxCycles.clone();
        this.instructions = instructions.clone();
    }

    public int getOrigin() { return origin; }
    public byte[] getBytes() { return bytes; }
    public int getLineCount() { return lineSizes.length; }

    public int getLineSize(int lineIndex) { return valid(lineIndex) ? lineSizes[lineIndex] : 0; }
    public int getLineMinCycles(int lineIndex) { return valid(lineIndex) ? lineMinCycles[lineIndex] : 0; }
    public int getLineMaxCycles(int lineIndex) { return valid(lineIndex) ? lineMaxCycles[lineIndex] : 0; }
    public boolean isInstruction(int lineIndex) {
        return valid(lineIndex) && instructions[lineIndex];
    }

    public int getMinCycles() {
        int total = 0;
        for (int c : lineMinCycles) total += c;
        return total;
    }

    public int getMaxCycles() {
        int total = 0;
        for (int c : lineMaxCycles) total += c;
        return total;
    }

    private boolean valid(int index) { return index >= 0 && index < lineSizes.length; }
}
