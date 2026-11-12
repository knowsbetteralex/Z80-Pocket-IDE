package com.alexzab.z80pocketide.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ExternalSourceStateTest {
    @Test public void unchangedContentDoesNotTriggerReload() {
        String source = "ORG $8000\nLD A,1\n";
        String baseline = ExternalSourceState.fingerprint(source);
        assertEquals(64, baseline.length());
        assertFalse(ExternalSourceState.changed(baseline, source));
        assertTrue(ExternalSourceState.changed(baseline, "ORG $8000\nLD A,2\n"));
    }

    @Test public void comparesActualUnicodeAndNewlinesNotFileTimestamps() {
        String saved = "Метка: DB 1\n";
        String fingerprint = ExternalSourceState.fingerprint(saved);
        assertTrue(ExternalSourceState.changed(fingerprint, "Метка: DB 1\r\n"));
        assertTrue(ExternalSourceState.changed(fingerprint, "Метка: DB 2\n"));
        assertFalse(ExternalSourceState.changed(fingerprint, saved));
    }

    @Test public void missingLegacyBaselineIsNotMisreportedAsExternalEdit() {
        assertFalse(ExternalSourceState.changed(null, "LD A,1"));
        assertFalse(ExternalSourceState.changed("", "LD A,1"));
    }
}
