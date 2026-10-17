package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class WorkspaceStoreTest {
    private static byte[] historyBytes(UndoHistory history) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        history.writeTo(new DataOutputStream(output));
        return output.toByteArray();
    }

    private static UndoHistory recoverHistory(byte[] bytes) throws IOException {
        return UndoHistory.readFrom(new DataInputStream(new ByteArrayInputStream(bytes)));
    }

    @Test public void recoversMultipleTabsIncludingUnsavedUnicodeDraftAndLocation() throws Exception {
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        WorkspaceStore.Tab file = new WorkspaceStore.Tab();
        file.title = "hero.asm";
        file.uri = "content://example/documents/hero";
        file.source = "ORG $8000\nSTART:\n  LD A,1\n";
        file.cursor = 18;
        file.scrollX = 25;
        file.scrollY = 48;
        UndoHistory fileHistory = new UndoHistory();
        fileHistory.record(18, "1", "2", 18, 19, 1000L, false);
        file.history = historyBytes(fileHistory);
        session.tabs.add(file);

        WorkspaceStore.Tab draft = new WorkspaceStore.Tab();
        draft.title = "Без имени 2.asm";
        draft.uri = "";
        draft.source = "ORG $4000\n; \u041d\u0435\u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0439 \ud83d\ude80\n";
        draft.dirty = true;
        draft.cursor = draft.source.length();
        UndoHistory draftHistory = new UndoHistory();
        draftHistory.record(0, "", "ORG $4000\n", 0, 10, 1000L, false);
        draftHistory.markUnsaved();
        draft.history = historyBytes(draftHistory);
        session.tabs.add(draft);
        session.activeIndex = 1;

        WorkspaceStore.Session recovered = WorkspaceStore.decode(WorkspaceStore.encode(session));
        assertEquals(2, recovered.tabs.size());
        assertEquals(1, recovered.activeIndex);
        assertEquals(file.uri, recovered.tabs.get(0).uri);
        assertEquals(file.source, recovered.tabs.get(0).source);
        assertEquals(25, recovered.tabs.get(0).scrollX);
        assertEquals(48, recovered.tabs.get(0).scrollY);
        assertEquals(draft.source, recovered.tabs.get(1).source);
        assertTrue(recovered.tabs.get(1).dirty);
        assertEquals(draft.cursor, recovered.tabs.get(1).cursor);
        assertEquals(1, recoverHistory(recovered.tabs.get(1).history).undoDepth());
    }

    @Test public void savesUndoRedoBranchAndCheckpointAcrossRestart() throws Exception {
        UndoHistory edits = new UndoHistory();
        edits.record(0, "", "L", 0, 1, 1000L, false);
        edits.markSaved();
        edits.record(1, "", "D", 1, 2, 2000L, false);
        edits.record(2, "", " ", 2, 3, 3000L, false);
        edits.takeUndo(); // Undo the space.
        edits.takeUndo(); // Undo D; we're now exactly at the saved "L" state.
        byte[] encoded = historyBytes(edits);
        UndoHistory restored = recoverHistory(encoded);
        assertEquals(1, restored.undoDepth());
        assertEquals(2, restored.redoDepth());
        assertTrue(restored.isAtSavePoint());
        assertEquals("D", restored.takeRedo().changes.get(0).after);
        assertEquals(" ", restored.takeRedo().changes.get(0).after);
        assertFalse(restored.isAtSavePoint());
        assertEquals(" ", restored.takeUndo().changes.get(0).after);
        assertEquals("D", restored.takeUndo().changes.get(0).after);
        assertTrue(restored.isAtSavePoint());
        assertArrayEquals(encoded, historyBytes(recoverHistory(encoded)));
    }

    @Test public void limitedHistoryRetainsOneHundredGroups() throws Exception {
        UndoHistory edits = new UndoHistory();
        for (int i = 0; i < 125; i++)
            edits.record(i, "", "X", i, i + 1, i * 2000L, false);
        assertEquals(100, edits.undoDepth());
        assertEquals(100, recoverHistory(historyBytes(edits)).undoDepth());
    }

    @Test public void brokenSnapshotIsRejectedWithoutReturningPartialWorkspace() throws Exception {
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        WorkspaceStore.Tab tab = new WorkspaceStore.Tab();
        tab.source = "ORG $8000\n";
        tab.history = historyBytes(new UndoHistory());
        session.tabs.add(tab);
        byte[] payload = WorkspaceStore.encode(session);
        payload[payload.length / 2] ^= 0x1;
        try {
            WorkspaceStore.decode(payload);
            throw new AssertionError("Expected checksum error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("CRC"));
        }
    }

    @Test public void invalidVersionAndTruncationCannotSilentlyCreateEmptySession() throws Exception {
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        WorkspaceStore.Tab tab = new WorkspaceStore.Tab();
        tab.history = historyBytes(new UndoHistory());
        session.tabs.add(tab);
        byte[] bytes = WorkspaceStore.encode(session);
        byte[] truncated = new byte[bytes.length - 2];
        System.arraycopy(bytes, 0, truncated, 0, truncated.length);
        try {
            WorkspaceStore.decode(truncated);
            throw new AssertionError("Truncation should fail");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("length"));
        }
    }
}
