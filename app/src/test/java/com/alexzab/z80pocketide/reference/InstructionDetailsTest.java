package com.alexzab.z80pocketide.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class InstructionDetailsTest {
    private List<InstructionDetails.Variant> forMnemonic(String name) {
        for (InstructionReference.Entry e : InstructionReference.ALL)
            if (e.mnemonic.equals(name)) return InstructionDetails.variants(e);
        throw new AssertionError("Missing mnemonic " + name);
    }

    private InstructionDetails.Variant form(String name,String contains) {
        for (InstructionDetails.Variant v : forMnemonic(name))
            if (v.form.equals(contains)) return v;
        throw new AssertionError(name + ": missing form " + contains);
    }

    @Test public void everyQuickReferenceFamilyHasExpandedEntries() {
        Set<String> seen=new HashSet<>();
        for(InstructionReference.Entry e : InstructionReference.ALL) {
            seen.add(e.mnemonic);
            List<InstructionDetails.Variant> detailed=InstructionDetails.variants(e);
            assertFalse("No detailed syntax " + e.mnemonic, detailed.isEmpty());
            for(InstructionDetails.Variant v : detailed) {
                assertFalse(v.form.isEmpty());
                assertFalse(v.descriptionRu.isEmpty());
                assertFalse(v.descriptionEn.isEmpty());
                assertFalse(v.timing.isEmpty());
                assertTrue(v.flags(true).contains("P/V"));
                assertTrue(v.flags(false).contains("S"));
            }
        }
        assertEquals(seen.size(),InstructionDetails.documentedFamilyCount());
    }

    @Test public void register16BitAddPreservesSignZeroAndParity() {
        InstructionDetails.Variant v=form("ADD","ADD HL,rr");
        assertEquals("11",v.timing);
        String f=v.flags(true);
        assertTrue(f.contains("H  Перенос из бита 11"));
        assertTrue(f.contains("C  Перенос из бита 15"));
        assertTrue(f.contains("S  Без изменения"));
        assertTrue(f.contains("Z  Без изменения"));
        assertTrue(f.contains("P/V  Без изменения"));
    }

    @Test public void incDec16And8HaveDifferentFlagSemantics() {
        assertTrue(form("INC","INC rr").flags(true).contains("C  Без изменения"));
        assertTrue(form("INC","INC rr").flags(true).contains("Z  Без изменения"));
        assertTrue(form("INC","INC r").flags(true).contains("P/V  1, если старое значение $7F"));
        assertTrue(form("DEC","DEC r").flags(true).contains("P/V  1, если старое значение $80"));
    }

    @Test public void specialCasesDescribeFlagsAccurately() {
        assertTrue(form("IN","IN A,(n)").flags(true).contains("S  Без изменения"));
        assertTrue(form("IN","IN r,(C)").flags(true).contains("S  Бит 7 результата"));
        assertTrue(form("BIT","BIT b,r").flags(true).contains("Z  1, если"));
        assertTrue(form("SCF","SCF").flags(true).contains("C  1"));
        assertTrue(form("CCF","CCF").flags(true).contains("H  Прежний C"));
        assertTrue(form("POP","POP AF").flags(true).contains("Загружается из байта F"));
        assertTrue(form("LD","LD A,I / LD A,R").flags(true).contains("Значение IFF2"));
    }

    @Test public void conditionalBranchVariantsHaveTwoTimingPaths() {
        assertEquals("12 taken / 7 not",form("JR","JR NZ/Z/NC/C,e").timing);
        assertEquals("13 taken / 8 not",form("DJNZ","DJNZ e").timing);
        assertEquals("17 taken / 10 not",form("CALL","CALL cc,nn").timing);
        assertEquals("11 taken / 5 not",form("RET","RET cc").timing);
        assertEquals("21 repeat / 16 last",form("LDIR","LDIR").timing);
    }

    @Test public void detailedVariantsSeparateAddressingCosts() {
        assertEquals("7",form("LD","LD r,n").timing);
        assertEquals("19",form("LD","LD r,(IX+d)/(IY+d)").timing);
        assertEquals("23",form("INC","INC (IX+d)/(IY+d)").timing);
        assertEquals("20",form("BIT","BIT b,(IX+d)/(IY+d)").timing);
        assertEquals("12",form("BIT","BIT b,(HL)").timing);
        assertEquals("15",form("ADC","ADC HL,rr").timing);
        assertEquals("23",form("RES","RES b,(IX+d)/(IY+d)").timing);
    }

    @Test public void flagsHaveAllSixDocumentedFields() {
        String effects=Z80FlagEffects.describe(Z80FlagEffects.ADD8,false);
        for(String name:new String[]{"S","Z","H","P/V","N","C"})
            assertTrue(effects.contains(name+"  "));
        assertTrue(effects.contains("Signed overflow"));
        assertTrue(Z80FlagEffects.describe(Z80FlagEffects.BLOCK_IO,true)
                .contains("Чётность"));
    }
}
