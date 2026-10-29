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
    private AndroidIncludeFiles(){}

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
                        if(parts[i].equals(cursor.getString(nameCol))){
                            found=cursor.getString(idCol);mime=cursor.getString(mimeCol);break;
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
        Uri uri=locate(context,root,path);
        if(uri==null)return null;
        return read(context,uri);
    }
}
