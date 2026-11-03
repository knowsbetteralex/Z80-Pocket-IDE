package com.alexzab.z80pocketide.project;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;

import com.alexzab.z80pocketide.source.AndroidIncludeFiles;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

/** Per-project JSON settings; SAF project file first, local backup as a fallback. */
public final class ProjectSettingsStore {
    private static final String PREFS="project_configuration_cache";
    private ProjectSettingsStore() {}

    public static ProjectConfiguration load(Context context) throws Exception {
        Uri tree=AndroidIncludeFiles.projectTree(context);
        String legacyRoot=AndroidIncludeFiles.selectedRoot(context);
        if(tree==null)return ProjectConfiguration.defaults("PROJECT",legacyRoot);
        String defaultName=tree.getLastPathSegment();
        if(defaultName==null||defaultName.isEmpty())defaultName="PROJECT";
        int slash=Math.max(defaultName.lastIndexOf('/'),defaultName.lastIndexOf(':'));
        defaultName=defaultName.substring(slash+1);
        if(defaultName.isEmpty())defaultName="PROJECT";

        SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String json=prefs.getBoolean("override:"+tree.toString(),false)
                ?prefs.getString(tree.toString(),null):null;
        try {
            Uri file=json==null
                    ?AndroidIncludeFiles.locate(context,null,ProjectConfiguration.FILE_NAME):null;
            if(file!=null)json=AndroidIncludeFiles.read(context,file);
        } catch(SecurityException | IOException ex) {
            // A provider may temporarily be read-only; use the per-folder backup.
        }
        if(json==null)json=prefs.getString(tree.toString(),null);
        if(json==null)return ProjectConfiguration.defaults(defaultName,legacyRoot);
        try {
            JSONObject root=new JSONObject(json);
            JSONObject screen=root.optJSONObject("loadingScreen");
            ProjectConfiguration result=new ProjectConfiguration(
                    root.optString("name",defaultName),
                    root.optString("root",legacyRoot==null?"main.asm":legacyRoot),
                    screen!=null&&screen.optBoolean("enabled",false),
                    screen==null?"":screen.optString("file",""),
                    screen==null?100:screen.optInt("pauseFrames",100));
            String problem=result.validationError();
            if(problem!=null)throw new IOException(problem);
            return result;
        }catch(JSONException ex){
            throw new IOException("Invalid "+ProjectConfiguration.FILE_NAME+": "+ex.getMessage(),ex);
        }
    }

    /** Returns true if the JSON was written into the project folder. */
    public static boolean save(Context context,ProjectConfiguration settings) throws Exception {
        String problem=settings.validationError();
        if(problem!=null)throw new IllegalArgumentException(problem);
        Uri tree=AndroidIncludeFiles.projectTree(context);
        if(tree==null)throw new IOException("Open a project folder first");
        JSONObject json=new JSONObject();
        json.put("name",settings.name);
        json.put("root",settings.root);
        JSONObject screen=new JSONObject();
        screen.put("enabled",settings.screenEnabled);
        screen.put("file",settings.screenPath);
        screen.put("pauseFrames",settings.pauseFrames);
        json.put("loadingScreen",screen);
        String content=json.toString(2)+"\n";
        SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        prefs.edit().putString(tree.toString(),content).apply();

        try {
            Uri file=AndroidIncludeFiles.locate(context,null,ProjectConfiguration.FILE_NAME);
            if(file==null) {
                Uri parent=DocumentsContract.buildDocumentUriUsingTree(
                        tree,DocumentsContract.getTreeDocumentId(tree));
                file=DocumentsContract.createDocument(context.getContentResolver(),parent,
                        "application/json",ProjectConfiguration.FILE_NAME);
                if(file==null)throw new IOException("Cannot create project settings file");
            }
            try(OutputStream output=context.getContentResolver().openOutputStream(file,"wt")) {
                if(output==null)throw new IOException("Cannot open settings for writing");
                output.write(content.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
            prefs.edit().putBoolean("override:"+tree.toString(),false).apply();
            return true;
        } catch(SecurityException | IOException | UnsupportedOperationException ex) {
            // Keep the local edit active after app restarts even if an old JSON
            // file exists but the SAF provider no longer permits writes.
            prefs.edit().putBoolean("override:"+tree.toString(),true).apply();
            return false;
        }
    }

    public static byte[] readScreen(Context context,String relativePath) throws Exception {
        if(!ProjectConfiguration.safeRelativeFile(relativePath))
            throw new IOException("Invalid SCREEN$ project path");
        Uri uri=AndroidIncludeFiles.locate(context,null,relativePath);
        if(uri==null)throw new FileNotFoundException("SCREEN$ not found: "+relativePath);
        try(InputStream input=context.getContentResolver().openInputStream(uri)){
            if(input==null)throw new IOException("Cannot read SCREEN$: "+relativePath);
            ByteArrayOutputStream buffer=new ByteArrayOutputStream(ProjectConfiguration.SCREEN_SIZE);
            byte[] chunk=new byte[2048];
            int n;
            while((n=input.read(chunk))!=-1){
                buffer.write(chunk,0,n);
                if(buffer.size()>ProjectConfiguration.SCREEN_SIZE)
                    throw new IOException("SCREEN$ must be exactly 6912 bytes (file is larger)");
            }
            if(buffer.size()!=ProjectConfiguration.SCREEN_SIZE)
                throw new IOException("SCREEN$ must be exactly 6912 bytes (got "+buffer.size()+")");
            return buffer.toByteArray();
        }
    }
}
