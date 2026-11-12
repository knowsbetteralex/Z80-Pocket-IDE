package com.alexzab.z80pocketide.editor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Compare contents rather than timestamps: Android document providers may
 * omit or delay last-modified metadata (particularly cloud or Git sync).
 */
public final class ExternalSourceState {
    private ExternalSourceState() {}

    public static String fingerprint(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((text == null ? "" : text)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b & 0xff) >>> 4, 16));
                hex.append(Character.forDigit(b & 15, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean changed(String lastSyncedHash, String diskText) {
        return lastSyncedHash != null && !lastSyncedHash.isEmpty()
                && !lastSyncedHash.equals(fingerprint(diskText));
    }
}
