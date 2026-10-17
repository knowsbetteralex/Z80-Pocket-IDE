package com.alexzab.z80pocketide.editor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private static final int MAX_GROUPS = 100;
    private static final int MAX_EDITS_PER_GROUP = 20000;
    private static final int MAX_FIELD_BYTES = 24 * 1024 * 1024;
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
    private boolean separateNext;

    public boolean canUndo() { return position > 0; }
    public boolean canRedo() { return position < groups.size(); }
    public boolean isAtSavePoint() { return savePosition >= 0 && position == savePosition; }
    public void markSaved() { savePosition = position; separateNext = true; }
    public void markUnsaved() { savePosition = -1; }
    public void separateNext() { separateNext = true; }

    public int undoDepth() { return position; }
    public int redoDepth() { return groups.size() - position; }

    public void clear() {
        groups.clear();
        position = 0;
        savePosition = 0;
        separateNext = true;
    }

    public void record(int start, String before, String after, int cursorBefore,
                       int cursorAfter, long now, boolean automatic) {
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
        boolean append = !separateNext && !afterUndo && recent
                && (automatic || (adjacent && smallChange && lastSmallChange));
        separateNext = false;
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

    /**
     * Persist the last 100 undo groups, the redo branch and savepoint.
     * Monotonic time is deliberately not persisted: reopening starts a new group.
     */
    public void writeTo(DataOutput output) throws IOException {
        output.writeInt(groups.size());
        output.writeInt(position);
        output.writeInt(savePosition);
        for (Group group : groups) {
            output.writeInt(group.cursorBefore);
            output.writeInt(group.cursorAfter);
            output.writeInt(group.changes.size());
            for (Change change : group.changes) {
                output.writeInt(change.start);
                writeString(output, change.before);
                writeString(output, change.after);
            }
        }
    }

    public static UndoHistory readFrom(DataInput input) throws IOException {
        int count = input.readInt();
        int position = input.readInt();
        int savepoint = input.readInt();
        if (count < 0 || count > MAX_GROUPS || position < 0 || position > count
                || savepoint < -1 || savepoint > count)
            throw new IOException("Invalid undo history index");
        UndoHistory history = new UndoHistory();
        for (int i = 0; i < count; i++) {
            int cursorBefore = input.readInt();
            int cursorAfter = input.readInt();
            int changes = input.readInt();
            if (cursorBefore < 0 || cursorAfter < 0
                    || changes <= 0 || changes > MAX_EDITS_PER_GROUP)
                throw new IOException("Invalid undo history group");
            Group group = new Group(cursorBefore, cursorAfter, 0L, 0);
            for (int j = 0; j < changes; j++) {
                int start = input.readInt();
                if (start < 0) throw new IOException("Invalid undo edit start");
                group.changes.add(new Change(start,
                        readString(input), readString(input)));
            }
            group.lastPosition = 0;
            history.groups.add(group);
        }
        history.position = position;
        history.savePosition = savepoint;
        history.separateNext = true;
        return history;
    }

    public void copyFrom(UndoHistory other) {
        groups.clear();
        groups.addAll(other.groups);
        position = other.position;
        savePosition = other.savePosition;
        separateNext = true;
    }

    private static void writeString(DataOutput output, String source) throws IOException {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FIELD_BYTES) throw new IOException("Undo text too large");
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInput input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_FIELD_BYTES)
            throw new IOException("Invalid undo text length");
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** A returned group is applied in reverse order for undo. */
    public Group takeUndo() {
        if (!canUndo()) return null;
        separateNext = true;
        return groups.get(--position);
    }

    /** A returned group is applied in forward order for redo. */
    public Group takeRedo() {
        if (!canRedo()) return null;
        separateNext = true;
        return groups.get(position++);
    }
}
