package com.alexzab.z80pocketide.project;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ProjectConfigurationTest {
    @Test public void defaultsStayCompatibleWithExistingProjects() {
        ProjectConfiguration config=ProjectConfiguration.defaults("SpaceCan",null);
        assertEquals("main.asm",config.root);
        assertFalse(config.screenEnabled);
        assertNull(config.validationError());
        assertEquals(100,config.pauseFrames);
    }

    @Test public void acceptsNestedPathsAndRejectsTraversal() {
        ProjectConfiguration settings=new ProjectConfiguration(
                "SpaceCan","src/main.asm",true,"assets/loading.scr",100);
        assertNull(settings.validationError());
        for(String path:new String[]{"../screen.scr","assets/../bad.scr",
                "/absolute.scr","C:/screen.scr","../main.asm","", "assets\\screen.scr"}) {
            assertFalse(path,ProjectConfiguration.safeRelativeFile(path));
        }
        assertNotNull(new ProjectConfiguration("Demo","main.asm",true,"",0).validationError());
    }

    @Test public void checkScreenOverlapAndBounds() {
        ProjectConfiguration.checkScreenConflict(0x8000, 4096);
        ProjectConfiguration.checkScreenConflict(0x5B00, 2048);
        ProjectConfiguration.checkScreenConflict(0x2000, 2048);
        for(int[] pair:new int[][]{{0x4000,6912},{0x5800,768},
                {0x3000,5000},{0x5AFF,2},{0xFFFF,2}}) {
            assertThrows(IllegalArgumentException.class,
                    ()->ProjectConfiguration.checkScreenConflict(pair[0],pair[1]));
        }
    }
}
