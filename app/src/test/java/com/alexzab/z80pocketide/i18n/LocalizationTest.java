package com.alexzab.z80pocketide.i18n;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocalizationTest {
    @Test
    public void russianReferenceSearchUsesLocalizedText() {
        assertFalse(LocalizedReference.search("относительный переход", AppLanguage.RU).isEmpty());
        assertFalse(LocalizedReference.search("ввод-вывод", AppLanguage.RU).isEmpty());
        assertFalse(LocalizedReference.search("ED B0", AppLanguage.RU).isEmpty());
    }

    @Test
    public void assemblerErrorsAreLocalized() {
        String localized = Texts.localizeAssemblerError(
                AppLanguage.RU,
                "line 3: unsupported instruction or operands: WAT A,B");
        assertTrue(localized.startsWith("строка 3:"));
        assertTrue(localized.contains("неподдерживаемая команда"));
    }

    @Test
    public void exampleTitlesAreLocalized() {
        assertEquals("Перелив бордюра", Texts.exampleTitle(AppLanguage.RU, "Border cycle"));
        assertEquals("Border cycle", Texts.exampleTitle(AppLanguage.EN, "Border cycle"));
    }
}
