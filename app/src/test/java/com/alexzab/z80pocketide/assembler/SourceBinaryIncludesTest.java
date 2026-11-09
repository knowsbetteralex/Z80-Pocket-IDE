package com.alexzab.z80pocketide.assembler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SourceBinaryIncludesTest {
    private static SourceIncludes.Expanded build(String source, String path,
                                                 Map<String,String> asmFiles,
                                                 Map<String,byte[]> assets) {
        SourceIncludes.Expanded included=SourceIncludes.expand(source,path,asmFiles::get);
        SourceIncludes.Expanded macros=SourceMacros.expand(included);
        return SourceBinaryIncludes.expand(macros,assets::get);
    }

    @Test public void insertsRawBytesAndCreditsOriginalDirectiveLine() {
        Map<String,byte[]> assets=new HashMap<>();
        byte[] data=new byte[57];
        for(int i=0;i<data.length;i++)data[i]=(byte)(i+128);
        assets.put("graphics/title.scr",data);
        String main="ORG $8000\nSTART:\n"
                +"INCBIN \"graphics/title.scr\"\n"
                +"END:\nASSERT END-START == 57\n";
        SourceIncludes.Expanded expanded=build(main,"main.asm",new HashMap<>(),assets);
        AssemblyResult full=new Assembler().assemble(expanded.source);
        assertArrayEquals(data,full.getBytes());
        AssemblyResult projected=expanded.project(full);
        assertEquals(57,projected.getLineSize(2));
        assertEquals(0,projected.getLineMinCycles(2));
        assertFalse(projected.isInstruction(2));
        assertEquals(0,projected.getLineSize(4));
    }

    @Test public void supportsSlicesWithPriorEquAndRelativeNestedPaths() {
        Map<String,String> asm=new HashMap<>();
        Map<String,byte[]> bytes=new HashMap<>();
        asm.put("src/data/code.asm",
                "SKIP EQU 2\nSPLASH: .incbin '../../assets/screen image.scr',SKIP+1,4\n");
        bytes.put("assets/screen image.scr",new byte[]{10,11,12,13,14,15,16});
        SourceIncludes.Expanded expanded=build(
                "ORG $9000\nINCLUDE 'data/code.asm'\nJP SPLASH",
                "src/main.asm",asm,bytes);
        AssemblyResult full=new Assembler().assemble(expanded.source);
        assertArrayEquals(new byte[]{13,14,15,16,(byte)0xC3,0,(byte)0x90},full.getBytes());
        assertEquals(4,expanded.project(full).getLineSize(1));
        assertEquals(4,expanded.projectFor(full,"src/data/code.asm",2).getLineSize(1));
    }

    @Test public void supportsConditionalAndMacroIncbinWithoutReadingSkippedFile() {
        Map<String,byte[]> binaries=new HashMap<>();
        binaries.put("assets/music.bin",new byte[]{0,1,2,3,4});
        String asm="INSERT MACRO path\n"
                +"  INCBIN path,1,3\nENDM\n"
                +"IF 0\n"
                +"  INCBIN \"missing.bin\"\n"
                +"ENDIF\n"
                +"INSERT \"assets/music.bin\"\n";
        SourceIncludes.Expanded expanded=build(asm,"main.asm",new HashMap<>(),binaries);
        AssemblyResult result=new Assembler().assemble(expanded.source);
        assertArrayEquals(new byte[]{1,2,3},result.getBytes());
        assertEquals(3,expanded.project(result).getLineSize(6));
    }

    @Test public void validatesPathCountAndMissingFileWithSourceLocation() {
        Map<String,String> files=new HashMap<>();
        files.put("lib/inc.asm","DB 1\nINCBIN '../absent.bin'\n");
        SourceIncludes.IncludeError missing=assertThrows(SourceIncludes.IncludeError.class,
                ()->build("INCLUDE 'lib/inc.asm'","main.asm",files,path->null));
        assertEquals("lib/inc.asm",missing.path);
        assertEquals(2,missing.line);
        assertTrue(missing.getMessage().contains("absent.bin"));
        assertThrows(IllegalArgumentException.class,
                ()->build("INCBIN '../escape.bin'","main.asm",files,path->new byte[2]));
        Map<String,byte[]> bin=new HashMap<>();
        bin.put("a.bin",new byte[]{1,2,3});
        for(String directive:new String[]{"INCBIN", "INCBIN 'a.bin',4",
                "INCBIN 'a.bin',0,4", "INCBIN 'a.bin',-1,1",
                "INCBIN 'a.bin',0,-1", "INCBIN 'a.bin',0,1,2",
                "INCBIN 'a.bin',BAD,1", "INCBIN 'a.bin',,"}) {
            assertThrows("should reject "+directive,SourceIncludes.IncludeError.class,
                    ()->build(directive,"main.asm",new HashMap<>(),bin));
        }
    }

    @Test public void emptyRangeKeepsLabelAtCorrectAddress() {
        Map<String,byte[]> assets=new HashMap<>();
        assets.put("empty.bin",new byte[]{1,2,3});
        SourceIncludes.Expanded expanded=build("ORG 32768\nHERE: INCBIN empty.bin,2,0\n"
                +"ASSERT HERE == 32768\nRET","main.asm",new HashMap<>(),assets);
        assertArrayEquals(new byte[]{(byte)0xC9},new Assembler().assemble(expanded.source).getBytes());
    }

    @Test public void detectsBareAndDottedDirectiveWithoutMistakingComments() {
        assertTrue(SourceBinaryIncludes.containsIncbin("  INCBIN \"asset.bin\""));
        assertTrue(SourceBinaryIncludes.containsIncbin("title: .incbin 'asset.bin'"));
        assertFalse(SourceBinaryIncludes.containsIncbin("; INCBIN 'asset.bin'"));
        assertFalse(SourceBinaryIncludes.containsIncbin("LABEL: NOP ; INCBIN x.bin"));
    }

    @Test public void limitsExpandedBinaryBytes() {
        Map<String,byte[]> data=new HashMap<>();
        data.put("large.bin",new byte[131073]);
        SourceIncludes.IncludeError e=assertThrows(SourceIncludes.IncludeError.class,
                ()->build("INCBIN \"large.bin\"","main.asm",new HashMap<>(),data));
        assertTrue(e.getMessage().contains("exceeds"));
    }
}
