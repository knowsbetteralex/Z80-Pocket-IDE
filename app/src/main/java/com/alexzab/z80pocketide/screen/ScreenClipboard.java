package com.alexzab.z80pocketide.screen;

import android.content.Context;
import java.io.*;

/** App-local clipboard survives opening a second SCR and process restarts. */
public final class ScreenClipboard {
    private static final String FILE="z80_screen_clipboard_v1.bin";
    private static ScreenSelectionClip current;
    private ScreenClipboard(){}

    public static void store(Context context,ScreenSelectionClip clip) throws IOException{
        if(clip==null)throw new IllegalArgumentException("No pixels selected");
        byte[] raw=clip.encode();
        try(FileOutputStream output=context.openFileOutput(FILE,Context.MODE_PRIVATE)){
            output.write(raw);
        }
        current=clip;
    }
    public static ScreenSelectionClip get(Context context) throws IOException{
        if(current!=null)return current;
        try(FileInputStream input=context.openFileInput(FILE)){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] buffer=new byte[4096];int n;
            while((n=input.read(buffer))!=-1){
                bytes.write(buffer,0,n);
                if(bytes.size()>53000)throw new IOException("Invalid screen clipboard size");
            }
            current=ScreenSelectionClip.decode(bytes.toByteArray());
            return current;
        }catch(FileNotFoundException missing){return null;}
    }
}
