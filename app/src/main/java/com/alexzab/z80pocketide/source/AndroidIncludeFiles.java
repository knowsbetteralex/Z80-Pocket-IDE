package com.alexzab.z80pocketide.source;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.content.SharedPreferences;
import com.alexzab.z80pocketide.editor.EditorDocument;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Reads source files from open tabs or an explicitly authorized SAF project tree. */
public final class AndroidIncludeFiles {
    private static final String PREFS="include_project";
    private static final String TREE="project_tree";
    private static final String ROOT="project_root";
    private AndroidIncludeFiles(){}


    public static String selectedRoot(Context context){
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(ROOT,null);
    }
    public static void setRoot(Context context,String relativePath){
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putString(ROOT,relativePath).apply();
    }
    public static Uri projectTree(Context context){
        String uri=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getString(TREE,null);
        return uri==null?null:Uri.parse(uri);
    }

    public static void rememberTree(Activity activity,Intent data){
        Uri tree=data.getData();
        if(tree==null)return;
        int flags=data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try{
            activity.getContentResolver().takePersistableUriPermission(tree,flags);
        }catch(SecurityException ignored){
            activity.getContentResolver().takePersistableUriPermission(
                    tree,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .edit().putString(TREE,tree.toString()).apply();
    }

    public static String relativePath(Context context,EditorDocument doc){
        if(doc.projectPath!=null&&!doc.projectPath.isEmpty())return doc.projectPath;
        Uri tree=projectTree(context);
        if(tree!=null&&doc.uriString!=null){
            try{
                String root=DocumentsContract.getTreeDocumentId(tree);
                String id=DocumentsContract.getDocumentId(Uri.parse(doc.uriString));
                if(id.startsWith(root+"/"))return id.substring(root.length()+1);
            }catch(RuntimeException ignored){}
        }
        return doc.title;
    }

    /** True for a document URI belonging to the currently granted folder. */
    public static boolean isProjectDocument(Context context,String uriString) {
        Uri tree=projectTree(context);
        if(tree==null || uriString==null)return false;
        try {
            Uri doc=Uri.parse(uriString);
            if(!tree.getAuthority().equals(doc.getAuthority()))return false;
            String root=DocumentsContract.getTreeDocumentId(tree);
            if(DocumentsContract.isTreeUri(doc)
                    && root.equals(DocumentsContract.getTreeDocumentId(doc)))return true;
            String id=DocumentsContract.getDocumentId(doc);
            return root.equals(id) || id.startsWith(root+"/");
        }catch(RuntimeException exception){return false;}
    }

    /** Lists ASM entry-point candidates from the granted project tree. */
    public static java.util.List<String> sourceFiles(Context context) throws Exception {
        Uri tree=projectTree(context);
        java.util.List<String> files=new java.util.ArrayList<>();
        if(tree==null)return files;
        String id=DocumentsContract.getTreeDocumentId(tree);
        java.util.Set<String> visited=new java.util.HashSet<>();
        collectSources(context,tree,id,"",0,visited,files);
        files.sort(String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    private static void collectSources(Context context,Uri tree,String folderId,
                                      String prefix,int depth,java.util.Set<String> visited,
                                      java.util.List<String> found) throws Exception {
        if(depth>8 || found.size()>=256 || !visited.add(folderId))return;
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,folderId);
        try(Cursor cursor=context.getContentResolver().query(children,
                new String[]{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_DISPLAY_NAME,
                        Document.COLUMN_MIME_TYPE},null,null,null)){
            if(cursor==null)throw new IOException("Cannot browse project folder");
            int id=cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID);
            int name=cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME);
            int mime=cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE);
            while(cursor.moveToNext() && found.size()<256){
                String filename=cursor.getString(name);
                String childId=cursor.getString(id);
                String type=cursor.getString(mime);
                if(filename==null || childId==null)continue;
                String path=prefix+filename;
                if(Document.MIME_TYPE_DIR.equals(type)){
                    if(depth<8)collectSources(context,tree,childId,path+"/",depth+1,
                            visited,found);
                }else if(filename.toLowerCase(java.util.Locale.ROOT).matches(
                        ".*\\.(asm|z80|s)$")){
                    found.add(path);
                }
            }
        }
    }

    /** Safely enumerates each folder segment rather than guessing document IDs. */
    public static Uri locate(Context context,EditorDocument root,String relative) throws Exception{
        Uri tree=projectTree(context);
        if(tree!=null){
            String id=DocumentsContract.getTreeDocumentId(tree);
            String[] parts=relative.split("/");
            for(int i=0;i<parts.length;i++){
                Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
                String found=null,mime=null;
                try(Cursor cursor=context.getContentResolver().query(children,
                        new String[]{Document.COLUMN_DOCUMENT_ID,Document.COLUMN_DISPLAY_NAME,
                                Document.COLUMN_MIME_TYPE},null,null,null)){
                    if(cursor==null)throw new IOException("Cannot read INCLUDE folder");
                    int idCol=cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID);
                    int nameCol=cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME);
                    int mimeCol=cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE);
                    while(cursor.moveToNext()){
                        String candidate=cursor.getString(nameCol);
                        if(parts[i].equals(candidate)){
                            found=cursor.getString(idCol);mime=cursor.getString(mimeCol);break;
                        }
                        if(found==null&&parts[i].equalsIgnoreCase(candidate)){
                            found=cursor.getString(idCol);mime=cursor.getString(mimeCol);
                        }
                    }
                }
                if(found==null)return null;
                if(i<parts.length-1&&!Document.MIME_TYPE_DIR.equals(mime))return null;
                if(i==parts.length-1&&Document.MIME_TYPE_DIR.equals(mime))return null;
                id=found;
            }
            return DocumentsContract.buildDocumentUriUsingTree(tree,id);
        }
        if(root!=null&&root.uriString!=null){
            Uri source=Uri.parse(root.uriString);
            if("file".equalsIgnoreCase(source.getScheme())){
                File folder=new File(source.getPath()).getParentFile();
                if(folder!=null)return Uri.fromFile(new File(folder,relative));
            }
        }
        return null;
    }

    public static String read(Context context,Uri uri) throws Exception{
        InputStream input=context.getContentResolver().openInputStream(uri);
        if(input==null)throw new IOException("INCLUDE cannot open: "+uri);
        try(BufferedReader reader=new BufferedReader(
                new InputStreamReader(input,StandardCharsets.UTF_8))){
            StringBuilder result=new StringBuilder();
            char[] buffer=new char[4096];int n;
            while((n=reader.read(buffer))!=-1){
                result.append(buffer,0,n);
                if(result.length()>4*1024*1024)throw new IOException("INCLUDE file too large");
            }
            return result.toString();
        }
    }

    public static String load(Context context,EditorDocument root,String path,
                              Map<String,String> opened) throws Exception{
        if(opened.containsKey(path))return opened.get(path);
        // All-uppercase editor mode can capitalize quoted INCLUDE filenames.
        // Reuse a uniquely matching open buffer without discarding unsaved work.
        String matching=null;
        for(String name:opened.keySet())if(name.equalsIgnoreCase(path)){
            if(matching!=null)throw new IOException("ambiguous INCLUDE: "+path);
            matching=name;
        }
        if(matching!=null)return opened.get(matching);
        Uri uri=locate(context,root,path);
        if(uri==null)return null;
        return read(context,uri);
    }
}
