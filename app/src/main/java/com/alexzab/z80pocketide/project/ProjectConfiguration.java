package com.alexzab.z80pocketide.project;

import java.util.Locale;

/** Portable, project-relative settings used by the editor, builder and TAP export. */
public final class ProjectConfiguration {
    public static final String FILE_NAME=".z80pocketide.json";
    public static final int SCREEN_SIZE=6912;
    public final String name;
    public final String root;
    public final boolean screenEnabled;
    public final String screenPath;
    /** -1 means "wait for key", 0 means start immediately, otherwise frames at 50 Hz. */
    public final int pauseFrames;

    public ProjectConfiguration(String name,String root,boolean screenEnabled,
                                String screenPath,int pauseFrames) {
        this.name=name==null?"":name.trim();
        this.root=root==null||root.trim().isEmpty()?"main.asm":root.trim();
        this.screenEnabled=screenEnabled;
        this.screenPath=screenPath==null?"":screenPath.trim();
        this.pauseFrames=pauseFrames;
    }

    public static ProjectConfiguration defaults(String projectName,String root) {
        return new ProjectConfiguration(projectName,root,false,"",100);
    }

    public ProjectConfiguration withRoot(String path) {
        return new ProjectConfiguration(name,path,screenEnabled,screenPath,pauseFrames);
    }

    public String validationError() {
        if(name.isEmpty())return "Project name cannot be empty";
        if(!safeRelativeFile(root))return "Invalid root ASM path: "+root;
        if(screenEnabled && !safeRelativeFile(screenPath))
            return "Choose a SCREEN$ file inside the project folder";
        if(pauseFrames < -1 || pauseFrames > 32767)
            return "Invalid screen delay";
        return null;
    }

    public static boolean safeRelativeFile(String path) {
        if(path==null || path.isEmpty() || path.length()>1024 || path.startsWith("/")
                || path.startsWith("\\") || path.indexOf('\\')>=0
                || path.indexOf('\0')>=0 || path.contains(":"))return false;
        for(String part:path.split("/",-1))
            if(part.isEmpty()||part.equals(".")||part.equals(".."))return false;
        return true;
    }

    /** Reject code that will overwrite the screen or run past 64K. */
    public static void checkScreenConflict(int loadAddress,int codeLength) {
        if(loadAddress < 0 || loadAddress > 65535 || codeLength < 0
                || (long)loadAddress + codeLength > 65536)
            throw new IllegalArgumentException("Machine code is outside the 64K address space");
        long end=(long)loadAddress+codeLength;
        if(loadAddress < 0x5B00 && end > 0x4000)
            throw new IllegalArgumentException(
                    "Game code overlaps SCREEN$ ($4000-$5AFF); change ORG or disable loading screen");
    }

    public String pauseDescription() {
        return pauseFrames==-1?"key":String.format(Locale.US,"%d",pauseFrames);
    }
}
