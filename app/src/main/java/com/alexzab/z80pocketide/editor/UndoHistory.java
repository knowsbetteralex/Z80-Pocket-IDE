package com.alexzab.z80pocketide.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-document incremental source history. Stores changed fragments rather
 * than whole source snapshots so typing in large files stays responsive.
 *
 * A burst of adjacent edits is one undoable action; redo is discarded as soon
 * as the user types again after undo.
 */
public final class UndoHistory {
    private static final int MAX_GROUPS = 120;
    private static final long GROUP_WINDOW_MS = 650;

    public static final class Change {
        public final int start;
        public final String before;
        public final String after;
        Change(int start, String before, String after) {
            this.start = start;
            this.before = before;
            this.after = after;
        }
    }

    public static final class Group {
        public final List<Change> changes = new ArrayList<>();
        public final int cursorBefore;
        int cursorAfter;
        private long updatedAt;
        private int lastPosition;

        Group(int cursorBefore, int cursorAfter, long timestamp, int position) {
            this.cursorBefore = cursorBefore;
            this.cursorAfter = cursorAfter;
            this.updatedAt = timestamp;
            this.lastPosition = position;
        }

        public int getCursorAfter() { return cursorAfter; }
    }

    private final List<Group> groups = new ArrayList<>();
    private int position; // groups[0..position) are applied
    private int savePosition; // -1 means saved state is no longer reachable

    public boolean canUndo() { return position > 0; }
    public boolean canRedo() { return position < groups.size(); }
    public boolean isAtSavePoint() { return savePosition >= 0 && position == savePosition; }
    public void markSaved() { savePosition = position; }

    public void clear() {
        groups.clear();
        position = 0;
        savePosition = 0;
    }

    public void record(int start, String before, String after, int cursorBefore,
                       int cursorAfter, long now) {
        if (before.equals(after)) return;
        // Never merge typing with the last applied step after an Undo.
        boolean afterUndo = position < groups.size();
        if (afterUndo) {
            if (savePosition > position) savePosition = -1;
            groups.subList(position, groups.size()).clear();
        }

        Group active = position > 0 ? groups.get(position - 1) : null;
        boolean adjacent = active != null
                && Math.abs(start - active.lastPosition) <= 2;
        boolean recent = active != null && now >= active.updatedAt
                && now - active.updatedAt <= GROUP_WINDOW_MS;
        // Paste, formatting, multi-character replacement get their own group;
        // adjacent single-character typing/deleting can be merged.
        boolean smallChange = before.length() <= 1 && after.length() <= 1;
        boolean lastSmallChange = active != null && !active.changes.isEmpty()
                && active.changes.get(active.changes.size() - 1).before.length() <= 1
                && active.changes.get(active.changes.size() - 1).after.length() <= 1;
        boolean append = !afterUndo && recent && adjacent && smallChange && lastSmallChange;
        if (!append) {
            active = new Group(cursorBefore, cursorAfter, now, start);
            groups.add(active);
            position++;
            if (groups.size() > MAX_GROUPS) {
                groups.remove(0);
                position--;
                if (savePosition >= 0) savePosition--;
                if (savePosition < 0) savePosition = -1;
            }
        }

        active.changes.add(new Change(start, before, after));
        active.cursorAfter = cursorAfter;
        active.updatedAt = now;
        active.lastPosition = start + after.length();
    }

    /** A returned group is applied in reverse order for undo. */
    public Group takeUndo() {
        if (!canUndo()) return null;
        return groups.get(--position);
    }

    /** A returned group is applied in forward order for redo. */
    public Group takeRedo() {
        if (!canRedo()) return null;
        return groups.get(position++);
    }
}
