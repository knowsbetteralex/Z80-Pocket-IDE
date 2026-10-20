package com.alexzab.z80pocketide.tools;

/**
 * Volatile workspace shared by successive ToolsActivity instances.
 * No Activity/Context references: safe during window recreation or tool close.
 * Deliberately resets when Android terminates the app process.
 */
public final class SpriteSession {
    private static SpriteData sprite = new SpriteData(16, 16);
    private static boolean maskEditing;
    private static boolean paperTarget;
    private static boolean binaryExport;

    private SpriteSession() {}

    public static SpriteData sprite() { return sprite; }
    public static boolean maskEditing() { return maskEditing; }
    public static void setMaskEditing(boolean enabled) { maskEditing = enabled; }
    public static boolean paperTarget() { return paperTarget; }
    public static void setPaperTarget(boolean enabled) { paperTarget = enabled; }
    public static boolean binaryExport() { return binaryExport; }
    public static void setBinaryExport(boolean enabled) { binaryExport = enabled; }

    public static void clear() {
        sprite = new SpriteData(16, 16);
        maskEditing = false;
        paperTarget = false;
        binaryExport = false;
    }
}
