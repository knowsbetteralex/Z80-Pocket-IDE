package com.alexzab.z80pocketide.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alexzab.z80pocketide.i18n.AppLanguage;

import org.junit.Test;

public class SpectrumReferenceTest {
    @Test
    public void includesPixelAddressBitLayout() {
        boolean found = false;
        for (SpectrumReference.Topic topic : SpectrumReference.TOPICS) {
            if (topic.subtitle(AppLanguage.EN).contains("010 TT LLL RRR CCCCC")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void includesCoreRomEntryPoints() {
        boolean print = false;
        boolean cls = false;
        boolean keyScan = false;
        boolean beeper = false;
        for (SpectrumReference.RomRoutine routine : SpectrumReference.ROM) {
            if ("$0010 / RST $10".equals(routine.address)) print = true;
            if ("$0D6B".equals(routine.address)) cls = true;
            if ("$028E".equals(routine.address)) keyScan = true;
            if ("$03B5".equals(routine.address)) beeper = true;
        }
        assertTrue(print && cls && keyScan && beeper);
    }

    @Test
    public void includesCoreIoReferences() {
        boolean ula = false;
        boolean keyboard = false;
        boolean kempston = false;
        boolean ay = false;
        for (SpectrumReference.PortEntry entry : SpectrumReference.PORTS) {
            String text = entry.port(AppLanguage.EN) + " " + entry.title(AppLanguage.EN);
            if (text.contains("$FE")) ula = true;
            if (text.contains("Keyboard")) keyboard = true;
            if (text.contains("$1F")) kempston = true;
            if (text.contains("$FFFD")) ay = true;
        }
        assertTrue(ula && keyboard && kempston && ay);
    }

    @Test
    public void hasExpectedReferenceSections() {
        assertTrue(SpectrumReference.TOPICS.length >= 6);
        assertTrue(SpectrumReference.ROM.length >= 7);
        assertTrue(SpectrumReference.PORTS.length >= 5);
        assertEquals("Карта памяти 48K",
                SpectrumReference.TOPICS[0].title(AppLanguage.RU));
    }
}
