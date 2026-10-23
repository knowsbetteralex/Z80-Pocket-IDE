package com.alexzab.z80pocketide.source;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class ExternalSourceIntentTest {
    @Test public void viewAndEditAcceptProviderAndFileUris() {
        assertTrue(ExternalSourceIntent.accepts("android.intent.action.VIEW", "content"));
        assertTrue(ExternalSourceIntent.accepts("android.intent.action.EDIT", "content"));
        assertTrue(ExternalSourceIntent.accepts("android.intent.action.VIEW", "file"));
        assertTrue(ExternalSourceIntent.accepts("android.intent.action.EDIT", "FILE"));
    }

    @Test public void otherActionsAndSchemesAreIgnored() {
        assertFalse(ExternalSourceIntent.accepts("android.intent.action.MAIN", "content"));
        assertFalse(ExternalSourceIntent.accepts("android.intent.action.SEND", "content"));
        assertFalse(ExternalSourceIntent.accepts("android.intent.action.VIEW", null));
        assertFalse(ExternalSourceIntent.accepts("android.intent.action.VIEW", "https"));
        assertFalse(ExternalSourceIntent.accepts(null, "content"));
    }

    @Test public void installedManifestAdvertisesExternalSourceFormats() throws Exception {
        File manifest = new File("src/main/AndroidManifest.xml");
        if (!manifest.exists()) manifest = new File("app/src/main/AndroidManifest.xml");
        assertTrue("Missing Android manifest", manifest.exists());
        String xml = new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("android:launchMode=\"singleTask\""));
        assertTrue(xml.contains("android:name=\"android.intent.action.VIEW\""));
        assertTrue(xml.contains("android:name=\"android.intent.action.EDIT\""));
        assertTrue(xml.contains("android:scheme=\"content\""));
        assertTrue(xml.contains("android:scheme=\"file\""));
        assertTrue(xml.contains("android:mimeType=\"text/plain\""));
        assertTrue(xml.contains("android:mimeType=\"text/x-asm\""));
        assertTrue(xml.contains("android:mimeType=\"application/octet-stream\""));
    }

    @Test public void writeGrantPickerRequiresMatchingOriginalFileName() {
        assertTrue(ExternalSourceIntent.matchingName("demo.asm", "demo.asm"));
        assertTrue(ExternalSourceIntent.matchingName("CODE.ASM", "code.asm"));
        assertTrue(ExternalSourceIntent.matchingName(" demo.asm ", "demo.asm"));
        assertFalse(ExternalSourceIntent.matchingName("demo.asm", "other.asm"));
        assertFalse(ExternalSourceIntent.matchingName("demo.asm", "demo.asm.txt"));
        assertFalse(ExternalSourceIntent.matchingName(null, "demo.asm"));
        assertFalse(ExternalSourceIntent.matchingName("demo.asm", null));
        assertFalse(ExternalSourceIntent.matchingName("", ""));
    }
}
