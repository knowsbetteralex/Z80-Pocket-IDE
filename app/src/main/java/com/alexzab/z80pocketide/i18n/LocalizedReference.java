package com.alexzab.z80pocketide.i18n;

import com.alexzab.z80pocketide.reference.InstructionReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LocalizedReference {
    private LocalizedReference() {}

    public static List<InstructionReference.Entry> search(String query, AppLanguage language) {
        if (language == AppLanguage.EN) return InstructionReference.search(query);
        if (query == null || query.trim().isEmpty()) return InstructionReference.ALL;

        String q = query.trim().toUpperCase(Locale.ROOT);
        List<InstructionReference.Entry> result = new ArrayList<>();
        for (InstructionReference.Entry entry : InstructionReference.ALL) {
            String haystack = (entry.mnemonic + " " + entry.syntax + " " + entry.opcode + " "
                    + entry.tStates + " " + Texts.category(language, entry.category) + " "
                    + Texts.flags(language, entry.flags) + " "
                    + Texts.referenceDescription(language, entry.description))
                    .toUpperCase(Locale.ROOT);
            if (haystack.contains(q)) result.add(entry);
        }
        return result;
    }
}
