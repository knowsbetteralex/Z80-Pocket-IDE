package com.alexzab.z80pocketide.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Versioned, checksummed snapshot of all open source tabs.
 *
 * Contains the actual source buffer even for unsaved tabs or inaccessible
 * document-provider URIs. Android's AtomicFile handles crash-safe replacement.
 * Pure Java for host-side roundtrip and corruption tests.
 */
public final class WorkspaceStore {
    private static final int MAGIC = 0x5A383057; // "Z80W"
    private static final int VERSION = 2;
    private static final int MAX_TABS = 64;
    private static final int MAX_PAYLOAD = 48 * 1024 * 1024;
    private static final int MAX_STRING = 24 * 1024 * 1024;
    private static final int MAX_HISTORY = 24 * 1024 * 1024;

    public static final class Tab {
        public String title = "";
        public String source = "";
        public String uri = "";
        public String projectPath = "";
        public boolean dirty;
        public int cursor;
        public int scrollX, scrollY;
        public byte[] history = new byte[0];
    }

    public static final class Session {
        public final List<Tab> tabs = new ArrayList<>();
        public int activeIndex;
    }

    private WorkspaceStore() {}

    public static byte[] encode(Session session) throws IOException {
        if (session == null || session.tabs.isEmpty() || session.tabs.size() > MAX_TABS
                || session.activeIndex < 0 || session.activeIndex >= session.tabs.size())
            throw new IOException("Invalid workspace session");

        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bodyBytes);
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeInt(session.tabs.size());
        output.writeInt(session.activeIndex);
        for (Tab tab : session.tabs) {
            writeString(output, tab.title);
            writeString(output, tab.source);
            writeString(output, tab.uri);
            writeString(output, tab.projectPath);
            output.writeBoolean(tab.dirty);
            output.writeInt(tab.cursor);
            output.writeInt(tab.scrollX);
            output.writeInt(tab.scrollY);
            if (tab.history == null || tab.history.length > MAX_HISTORY)
                throw new IOException("Undo history exceeds limit");
            output.writeInt(tab.history.length);
            output.write(tab.history);
        }
        output.flush();
        byte[] body = bodyBytes.toByteArray();
        if (body.length > MAX_PAYLOAD)
            throw new IOException("Workspace exceeds 48 MiB");
        CRC32 crc = new CRC32();
        crc.update(body);
        ByteArrayOutputStream full = new ByteArrayOutputStream(body.length + 8);
        DataOutputStream result = new DataOutputStream(full);
        result.writeInt(body.length);
        result.write(body);
        result.writeInt((int)crc.getValue());
        result.flush();
        return full.toByteArray();
    }

    public static Session decode(byte[] data) throws IOException {
        if (data == null || data.length < 24 || data.length > MAX_PAYLOAD + 8)
            throw new IOException("Invalid workspace data length");
        DataInputStream wrapper = new DataInputStream(new ByteArrayInputStream(data));
        int length = wrapper.readInt();
        if (length < 16 || length > MAX_PAYLOAD || length != data.length - 8)
            throw new IOException("Invalid workspace payload length");
        byte[] body = new byte[length];
        wrapper.readFully(body);
        int checksum = wrapper.readInt();
        CRC32 crc = new CRC32();
        crc.update(body);
        if ((int)crc.getValue() != checksum) throw new IOException("Workspace CRC mismatch");

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(body));
        if (input.readInt() != MAGIC) throw new IOException("Not a workspace snapshot");
        int version = input.readInt();
        if (version != 1 && version != VERSION) throw new IOException("Unsupported workspace version");
        int count = input.readInt();
        int active = input.readInt();
        if (count < 1 || count > MAX_TABS || active < 0 || active >= count)
            throw new IOException("Invalid workspace tab count/index");
        Session restored = new Session();
        restored.activeIndex = active;
        for (int i = 0; i < count; i++) {
            Tab tab = new Tab();
            tab.title = readString(input);
            tab.source = readString(input);
            tab.uri = readString(input);
            if (version >= 2) tab.projectPath = readString(input);
            tab.dirty = input.readBoolean();
            tab.cursor = input.readInt();
            tab.scrollX = input.readInt();
            tab.scrollY = input.readInt();
            int historyLength = input.readInt();
            if (historyLength < 0 || historyLength > MAX_HISTORY
                    || historyLength > input.available())
                throw new IOException("Invalid stored history");
            tab.history = new byte[historyLength];
            input.readFully(tab.history);
            restored.tabs.add(tab);
        }
        if (input.available() != 0) throw new IOException("Unexpected trailing workspace data");
        return restored;
    }

    private static void writeString(DataOutputStream stream, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING) throw new IOException("Workspace field too large");
        stream.writeInt(bytes.length);
        stream.write(bytes);
    }

    private static String readString(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        if (length < 0 || length > MAX_STRING || length > stream.available())
            throw new IOException("Invalid workspace string");
        byte[] bytes = new byte[length];
        stream.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
