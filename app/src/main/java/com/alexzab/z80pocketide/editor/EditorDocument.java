package com.alexzab.z80pocketide.editor;

/** Runtime state for one source editor tab. */
public final class EditorDocument {
    public String title;
    public String text;
    public String uriString;
    public boolean dirty;
    public int cursor;
    public int scrollX;
    public int scrollY;

    // Build artifacts are kept per tab while the activity is alive.
    public String lastBuiltSource;
    public byte[] lastTap;

    public EditorDocument(String title, String text, String uriString, boolean dirty) {
        this.title = title;
        this.text = text == null ? "" : text;
        this.uriString = uriString;
        this.dirty = dirty;
    }

    public boolean hasFile() {
        return uriString != null && !uriString.isEmpty();
    }

    /**
     * Closing a modified file must ask, but so must a non-empty document that has
     * never had a backing file. The latter is important for starter/example tabs:
     * their source is valuable even when it has not yet been edited.
     */
    public boolean needsCloseConfirmation() {
        return dirty || (!hasFile() && text != null && !text.trim().isEmpty());
    }

    public boolean buildIsCurrent() {
        return lastTap != null && lastBuiltSource != null && lastBuiltSource.equals(text);
    }

    public void invalidateBuild() {
        lastTap = null;
        lastBuiltSource = null;
    }
}
