package com.alexzab.z80pocketide.screen;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import android.widget.FrameLayout.LayoutParams;
import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.project.*;
import com.alexzab.z80pocketide.source.AndroidIncludeFiles;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Dedicated full-screen, no-AndroidX Spectrum SCREEN$ editor. */
public final class ScreenEditorActivity extends Activity {
    public static final String EXTRA_PROJECT_PATH="screen_project_path";
    public static final String EXTRA_DOCUMENT_URI="screen_document_uri";
    public static final String EXTRA_START_ACTION="screen_start_action";
    private static final int OPEN=801,CREATE=802;
    private static final String STATE_SCREEN="scr_bytes",STATE_SAVED="scr_saved";
    private static final String STATE_URI="scr_uri",STATE_NAME="scr_name",STATE_ASM="scr_asm";
    private static final String STATE_SCALE="scr_scale",STATE_CENTER_X="scr_center_x";
    private static final String STATE_CENTER_Y="scr_center_y",STATE_COLLAPSED="scr_collapsed";
    private static final String STATE_LOCKED="scr_locked",STATE_LANDSCAPE="scr_landscape";
    private static final String STATE_COLOR="scr_color",STATE_SIDE="scr_side";
    private static final String STATE_PAPER_COLOR="scr_paper_color";
    private static final String STATE_BRIGHT_SET="scr_bright_set",STATE_FLASH_SET="scr_flash_set";
    private static final String STATE_TOOL="scr_tool",STATE_BRIGHT="scr_bright",STATE_FLASH="scr_flash";
    private static final String STATE_PREVIOUS_TOOL="scr_previous_tool";
    /** Shared data workspace between Tools tab and full-screen Activity. */
    public static final class Session {
        SpectrumScreenDocument doc=new SpectrumScreenDocument();
        ScreenEditHistory history=new ScreenEditHistory(doc.toRaw());
        Uri uri;
        String title="untitled.scr",projectPath;
        boolean importedAsm,locked=true,landscape,collapsed;
        float zoomFactor=1,centerX=128,centerY=96;
        SpectrumScreenDocument.Tool tool=SpectrumScreenDocument.Tool.DRAW;
        SpectrumScreenDocument.Tool previousTool=SpectrumScreenDocument.Tool.DRAW;
        SpectrumScreenDocument.Side side=SpectrumScreenDocument.Side.INK;
        int inkColor=7,paperColor=0;
        boolean bright,flash,applyBright,applyFlash;
    }
    private static Session retained;
    public static Session sharedSession(){
        if(retained==null)retained=new Session();
        return retained;
    }
    private Session state;
    private ScreenEditorCanvasView canvas;
    private ScreenPaletteOverlay palette;
    private TextView title,status;
    private Button undo,redo,lock;
    private boolean ru,closeAfterSave,orientationChanging;
    private String creatingType;
    private boolean initialLandscape;

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ru=LanguageSettings.get(this)==AppLanguage.RU;
        boolean explicitFile=getIntent().hasExtra(EXTRA_PROJECT_PATH)
                ||getIntent().hasExtra(EXTRA_DOCUMENT_URI);
        if(retained!=null && (savedInstanceState!=null || !explicitFile)) {
            state=retained;
        } else {
            state=new Session();
            if(savedInstanceState!=null)restoreState(savedInstanceState);
            else openInitialIntent();
        }
        retained=state;
        initialLandscape=state.landscape;
        if(state.landscape)setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        configureWindow();
        buildUi();
        updateUi();
        if(savedInstanceState==null){
            String startAction=getIntent().getStringExtra(EXTRA_START_ACTION);
            if("open".equals(startAction))canvas.post(this::requestOpen);
            else if("save".equals(startAction))canvas.post(this::save);
        }
    }

    private void configureWindow(){
        getWindow().setStatusBarColor(0xFF142B20);
        getWindow().setNavigationBarColor(0xFF15251B);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        if(state.landscape)getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF15251B);
        if(Build.VERSION.SDK_INT>=30) {
            root.setOnApplyWindowInsetsListener((view,insets)->{
                android.graphics.Insets safe=insets.getInsets(
                        WindowInsets.Type.systemBars());
                view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
                return insets;
            });
        }
        HorizontalScrollView toolbarScroll=new HorizontalScrollView(this);
        toolbarScroll.setHorizontalScrollBarEnabled(false);
        toolbarScroll.setFillViewport(true);
        LinearLayout bar=new LinearLayout(this);
        bar.setPadding(dp(3),dp(3),dp(3),dp(3));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFF223B2D);
        toolbarScroll.addView(bar,new HorizontalScrollView.LayoutParams(-2,-2));
        LinearLayout toolbar=new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFF223B2D);
        toolbar.addView(toolbarScroll,new LinearLayout.LayoutParams(0,dp(49),1));
        LinearLayout fixedActions=new LinearLayout(this);
        fixedActions.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(fixedActions,new LinearLayout.LayoutParams(dp(100),dp(49)));
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(49)));

        action(bar,"‹",ru?"Назад":"Back",v->requestExit());
        action(bar,"📂",ru?"Открыть":"Open",v->requestOpen());
        action(bar,"💾",ru?"Сохранить":"Save",v->save());
        action(bar,"⋯",ru?"Сохранить как":"Save as",v->chooseSaveAs());
        undo=action(bar,"↶",ru?"Отменить":"Undo",v->{
            if(state.history.undo(state.doc)){canvas.refreshPixels();updateUi();}
        });
        redo=action(bar,"↷",ru?"Повторить":"Redo",v->{
            if(state.history.redo(state.doc)){canvas.refreshPixels();updateUi();}
        });
        action(bar,"⊡",ru?"Вписать":"Fit",v->canvas.fitToScreen());
        action(fixedActions,"⤢",ru?"Сменить ориентацию":"Toggle orientation",v->toggleOrientation());
        lock=action(fixedActions,"🔒",ru?"Блокировка":"Lock",v->{
            state.locked=!state.locked;canvas.setLocked(state.locked);updateUi();
        });

        title=new TextView(this);
        title.setTextColor(Color.WHITE);title.setTextSize(14);
        title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(dp(10),dp(5),dp(8),dp(4));
        title.setBackgroundColor(0xFF1C3325);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));

        FrameLayout holder=new FrameLayout(this);
        root.addView(holder,new LinearLayout.LayoutParams(-1,0,1));
        canvas=new ScreenEditorCanvasView(this,state.doc);
        holder.addView(canvas,new FrameLayout.LayoutParams(-1,-1));
        canvas.setLocked(state.locked);
        canvas.setListener(new ScreenEditorCanvasView.Listener() {
            @Override public void strokeStart(){state.history.begin(state.doc);}
            @Override public void strokeEnd(){state.history.end(state.doc);updateUi();}
            @Override public void changed(int x,int y){
                updatePosition(x,y);
                updateUi();
            }
            @Override public void picked(int x,int y,int attr,boolean ink){
                palette.pick(attr,ink);
                updatePosition(x,y);
            }
        });

        palette=new ScreenPaletteOverlay(this,ru);
        FrameLayout.LayoutParams placement=new FrameLayout.LayoutParams(
                dp(211),-2,Gravity.TOP|Gravity.RIGHT);
        placement.setMargins(dp(6),dp(7),dp(7),0);
        holder.addView(palette,placement);
        palette.setCollapsed(state.collapsed);
        palette.setBrushColors(state.tool,state.side,state.inkColor,state.paperColor,
                state.bright,state.flash,state.applyBright,state.applyFlash);
        palette.setPreviousTool(state.previousTool);
        palette.setListener(()->{
            state.tool=palette.tool();
            state.previousTool=palette.previousTool();
            state.side=palette.side();
            state.inkColor=palette.inkColor();
            state.paperColor=palette.paperColor();
            state.bright=palette.bright();
            state.flash=palette.flash();
            state.applyBright=palette.applyBright();
            state.applyFlash=palette.applyFlash();
            canvas.setBrush(state.tool,state.side,palette.color(),
                    state.applyBright?state.bright:null,
                    state.applyFlash?state.flash:null);
        });
        palette.setFlagListener((isBright,enabled)->{
            if(canvas!=null)canvas.toggleFlagOnSelection(isBright,enabled);
        });
        holder.post(()->palette.setExpandedWidth(
                Math.min(dp(410),Math.max(dp(208),holder.getWidth()-dp(16)))));

        status=new TextView(this);
        status.setTextSize(11);
        status.setTextColor(0xFFD5E5D9);
        status.setBackgroundColor(0xFF243D30);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(10),0,dp(5),0);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(23)));
        setContentView(root);
        // Preserve the screen's center/zoom across portrait/landscape recreation.
        canvas.post(()->{
            if(canvas.getWidth()<=0 || canvas.getHeight()<=0)return;
            float fit=Math.min((canvas.getWidth()-dp(20))/256f,
                    (canvas.getHeight()-dp(20))/192f);
            float size=Math.max(.25f,state.zoomFactor*fit);
            canvas.setViewport(size,
                    canvas.getWidth()*.5f-state.centerX*size,
                    canvas.getHeight()*.5f-state.centerY*size);
        });
    }
    private Button action(LinearLayout row,String glyph,String description,
                          android.view.View.OnClickListener click){
        Button button=new Button(this);
        button.setText(glyph);button.setTextSize(20);
        button.setTextColor(0xFFF4FFF8);
        button.setContentDescription(description);
        button.setAllCaps(false);
        button.setPadding(dp(4),0,dp(4),0);
        button.setMinimumWidth(0);button.setMinWidth(0);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B6650));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(46),dp(42));
        p.setMargins(dp(2),0,dp(2),0);
        row.addView(button,p);button.setOnClickListener(click);
        return button;
    }
    private void updateUi(){
        if(state==null||title==null)return;
        boolean dirty=state.history.isDirty(state.doc);
        title.setText((dirty?"● ":"")+state.title+
                (state.importedAsm?(ru?" · импорт ASM":" · ASM import"):""));
        undo.setEnabled(state.history.canUndo());
        redo.setEnabled(state.history.canRedo());
        lock.setText(state.locked?"🔒":"🔓");
        lock.setContentDescription(state.locked
                ?(ru?"Разблокировать рисование":"Unlock drawing")
                :(ru?"Заблокировать рисование":"Lock drawing"));
        if(status!=null && status.getText().length()==0)
            status.setText(ru?"Экран 256×192 · 6912 байт":"Screen 256×192 · 6912 bytes");
    }
    private void updatePosition(int x,int y) {
        status.setText(String.format(Locale.ROOT,
                "X=%d Y=%d  |  Cell=%d,%d  |  %s=%d  BRIGHT=%d FLASH=%d",
                x,y,x/8,y/8,state.side,
                state.side==SpectrumScreenDocument.Side.INK?state.inkColor:state.paperColor,
                state.bright?1:0,state.flash?1:0));
    }
    private void saveViewport(){
        if(canvas==null || canvas.getHeight()==0)return;
        float fit=Math.max(.1f,Math.min((canvas.getWidth()-dp(20))/256f,
                (canvas.getHeight()-dp(20))/192f));
        state.zoomFactor=canvas.getPixelSize()/fit;
        state.centerX=(canvas.getWidth()*.5f-canvas.getOriginX())/canvas.getPixelSize();
        state.centerY=(canvas.getHeight()*.5f-canvas.getOriginY())/canvas.getPixelSize();
        state.collapsed=palette!=null&&palette.isCollapsed();
    }
    private void toggleOrientation(){
        saveViewport();
        state.landscape=!state.landscape;
        orientationChanging=true;
        setRequestedOrientation(state.landscape
                ?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                :ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void openInitialIntent(){
        Intent input=getIntent();
        String projectPath=input.getStringExtra(EXTRA_PROJECT_PATH);
        state.projectPath=projectPath;
        String provided=input.getStringExtra(EXTRA_DOCUMENT_URI);
        try {
            if(projectPath!=null&&!projectPath.isEmpty()){
                Uri u=AndroidIncludeFiles.locate(this,null,projectPath);
                if(u==null)throw new FileNotFoundException(projectPath);
                load(u,projectPath);
            }else if(provided!=null)load(Uri.parse(provided),null);
        }catch(Exception ex){showError(ex);}
    }
    private void requestOpen(){
        if(state.history.isDirty(state.doc)){
            new AlertDialog.Builder(this)
                    .setMessage(ru?"Текущие изменения будут потеряны. Открыть другой экран?"
                            :"Discard unsaved changes and open another screen?")
                    .setNegativeButton(ru?"Отмена":"Cancel",null)
                    .setPositiveButton(ru?"Открыть":"Open",(d,w)->chooseOpen())
                    .show();
        }else chooseOpen();
    }
    private void chooseOpen(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION|
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent,OPEN);
    }
    private void load(Uri uri,String projectPath) throws Exception{
        String filename=displayName(uri);
        String path=projectPath!=null?projectPath:filename;
        String lower=path.toLowerCase(Locale.ROOT);
        byte[] bytes;
        boolean asm=lower.endsWith(".asm")||lower.endsWith(".z80")||lower.endsWith(".s");
        if(asm) {
            byte[] data=readBounded(uri,500000);
            bytes=ScreenAsmCodec.importScreen(new String(data,StandardCharsets.UTF_8));
        }else if(lower.endsWith(".scr")||lower.endsWith(".bin")){
            bytes=readBounded(uri,6913);
            if(bytes.length!=6912)throw new IOException(
                    "SCREEN$ requires 6912 bytes; got "+bytes.length);
        }else throw new IOException(ru?"Поддерживаются .scr, .bin, .asm":
                "Supported files: .scr, .bin, .asm");
        state.doc=SpectrumScreenDocument.fromRaw(bytes);
        state.history=new ScreenEditHistory(bytes);
        state.uri=uri;
        state.projectPath=projectPath;
        state.importedAsm=asm;
        state.title=filename;
        state.zoomFactor=1;
        state.centerX=128;state.centerY=96;
        if(canvas!=null){canvas.setDocument(state.doc);updateUi();}
    }
    private byte[] readBounded(Uri uri,int max) throws IOException{
        try(InputStream input=getContentResolver().openInputStream(uri)){
            if(input==null)throw new IOException("Cannot open file");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] part=new byte[4096];int n;
            while((n=input.read(part))!=-1){
                bytes.write(part,0,n);
                if(bytes.size()>max)throw new IOException("File too large");
            }
            return bytes.toByteArray();
        }
    }
    private String displayName(Uri uri){
        try(Cursor c=getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst()) {
                String name=c.getString(0);
                if(name!=null&&!name.isEmpty())return name;
            }
        }catch(Exception ignored){}
        String segment=uri.getLastPathSegment();
        return segment==null?"untitled.scr":segment;
    }
    private void tryPersist(Intent result){
        if(result==null || result.getData()==null)return;
        int flags=result.getFlags() &
                (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(flags!=0)try{
            getContentResolver().takePersistableUriPermission(result.getData(),flags);
        }catch(Exception ignored){}
    }
    private void save(){
        if(state.importedAsm || state.uri==null){chooseSaveAs();return;}
        write(state.uri,"binary",false);
    }
    private void chooseSaveAs(){
        String[] options=ru?new String[]{"Сохранить .scr","Сохранить .bin","Экспортировать .asm"}
                :new String[]{"Save .scr","Save .bin","Export .asm"};
        new AlertDialog.Builder(this)
                .setTitle(ru?"Выберите формат":"Choose format")
                .setItems(options,(dialog,which)->{
                    creatingType=which==2?"asm":which==1?"bin":"scr";
                    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(which==2?"text/x-asm":"application/octet-stream");
                    String filename=state.title;
                    int dot=filename.lastIndexOf('.');
                    if(dot>0)filename=filename.substring(0,dot);
                    intent.putExtra(Intent.EXTRA_TITLE,filename+"."+creatingType);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(intent,CREATE);
                }).show();
    }
    private void write(Uri uri,String kind,boolean newFile){
        try {
            byte[] data=kind.equals("asm")
                    ?ScreenAsmCodec.exportScreen(state.doc.toRaw()).getBytes(StandardCharsets.UTF_8)
                    :state.doc.toRaw();
            try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")){
                if(stream==null)throw new IOException("Cannot write file");
                stream.write(data);
                stream.flush();
            }
            if(!kind.equals("asm")){
                state.uri=uri;
                state.importedAsm=false;
                if(newFile)state.projectPath=null;
                state.title=displayName(uri);
                state.history.markSaved(state.doc);
                updateUi();
            }
            if(!kind.equals("asm"))setResult(RESULT_OK);
            Toast.makeText(this,ru?"Файл сохранён":"File saved",Toast.LENGTH_SHORT).show();
            if(newFile && !kind.equals("asm") && !closeAfterSave)offerAsProjectScreen(uri);
            if(closeAfterSave && !kind.equals("asm"))finish();
            closeAfterSave=false;
        }catch(Exception ex){
            closeAfterSave=false;
            showError(ex);
        }
    }
    /** Offer project integration only if SAF returned a file inside the chosen tree. */
    private void offerAsProjectScreen(Uri saved) {
        Uri tree=AndroidIncludeFiles.projectTree(this);
        if(tree==null || !AndroidIncludeFiles.isProjectDocument(this,saved.toString()))return;
        final String relative;
        try {
            String root=DocumentsContract.getTreeDocumentId(tree);
            String id=DocumentsContract.getDocumentId(saved);
            if(!id.startsWith(root+"/"))return;
            relative=id.substring(root.length()+1);
            if(!ProjectConfiguration.safeRelativeFile(relative))return;
        }catch(Exception ex){return;}
        new AlertDialog.Builder(this)
                .setMessage(ru?"Использовать "+relative+" как заставку проекта?"
                        :"Use "+relative+" as the project loading screen?")
                .setNegativeButton(ru?"Нет":"No",null)
                .setPositiveButton(ru?"Использовать":"Use",(dialog,which)->{
                    try {
                        ProjectConfiguration old=ProjectSettingsStore.load(this);
                        ProjectConfiguration updated=new ProjectConfiguration(old.name,
                                old.root,true,relative,old.pauseFrames);
                        boolean projectSaved=ProjectSettingsStore.save(this,updated);
                        Toast.makeText(this,projectSaved
                                ?(ru?"Заставка проекта обновлена":"Project screen updated")
                                :(ru?"Сохранено локально: папка только для чтения"
                                    :"Settings saved locally: project folder read-only"),
                                Toast.LENGTH_LONG).show();
                        state.projectPath=relative;
                    }catch(Exception ex){showError(ex);}
                }).show();
    }

    private void requestExit(){
        if(!state.history.isDirty(state.doc)){finish();return;}
        new AlertDialog.Builder(this)
                .setTitle(ru?"Несохранённые изменения":"Unsaved screen")
                .setMessage(ru?"Сохранить изменения перед выходом?":"Save changes before closing?")
                .setPositiveButton(ru?"Сохранить":"Save",(d,w)->{
                    closeAfterSave=true;save();
                })
                .setNeutralButton(ru?"Не сохранять":"Discard",(d,w)->{
                    state.doc.replaceRaw(state.history.savedCopy());
                    state.history.reset(state.doc);
                    finish();
                })
                .setNegativeButton(ru?"Отмена":"Cancel",null).show();
    }
    @Override public void onBackPressed(){requestExit();}
    @Override protected void onActivityResult(int code,int resultCode,Intent data){
        super.onActivityResult(code,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null){
            closeAfterSave=false;return;
        }
        Uri uri=data.getData();
        tryPersist(data);
        if(code==OPEN){
            try{load(uri,null);updateUi();}
            catch(Exception ex){showError(ex);}
        }else if(code==CREATE){
            write(uri,creatingType==null?"binary":creatingType,true);
        }
    }
    private void showError(Exception ex){
        new AlertDialog.Builder(this)
                .setTitle(ru?"Ошибка SCR":"SCR error")
                .setMessage(ex.getMessage()==null?ex.toString():ex.getMessage())
                .setPositiveButton("OK",null).show();
    }
    @Override protected void onSaveInstanceState(Bundle out){
        saveViewport();
        out.putByteArray(STATE_SCREEN,state.doc.toRaw());
        out.putByteArray(STATE_SAVED,state.history.savedCopy());
        out.putString(STATE_URI,state.uri==null?null:state.uri.toString());
        out.putString(STATE_NAME,state.title);
        out.putString(EXTRA_PROJECT_PATH,state.projectPath);
        out.putBoolean(STATE_ASM,state.importedAsm);
        out.putBoolean(STATE_LOCKED,state.locked);
        out.putBoolean(STATE_COLLAPSED,state.collapsed);
        out.putBoolean(STATE_LANDSCAPE,state.landscape);
        out.putFloat(STATE_SCALE,state.zoomFactor);
        out.putFloat(STATE_CENTER_X,state.centerX);
        out.putFloat(STATE_CENTER_Y,state.centerY);
        out.putInt(STATE_COLOR,state.inkColor);
        out.putInt(STATE_PAPER_COLOR,state.paperColor);
        out.putString(STATE_TOOL,state.tool.name());
        out.putString(STATE_PREVIOUS_TOOL,state.previousTool.name());
        out.putString(STATE_SIDE,state.side.name());
        out.putBoolean(STATE_BRIGHT,state.bright);
        out.putBoolean(STATE_FLASH,state.flash);
        out.putBoolean(STATE_BRIGHT_SET,state.applyBright);
        out.putBoolean(STATE_FLASH_SET,state.applyFlash);
        super.onSaveInstanceState(out);
    }
    private void restoreState(Bundle b){
        byte[] bytes=b.getByteArray(STATE_SCREEN);
        if(bytes!=null && bytes.length==6912){
            state.doc=SpectrumScreenDocument.fromRaw(bytes);
            byte[] saved=b.getByteArray(STATE_SAVED);
            state.history=new ScreenEditHistory(
                    saved!=null&&saved.length==6912?saved:bytes);
        }
        String raw=b.getString(STATE_URI);
        if(raw!=null)state.uri=Uri.parse(raw);
        state.title=b.getString(STATE_NAME,"untitled.scr");
        state.projectPath=b.getString(EXTRA_PROJECT_PATH);
        state.importedAsm=b.getBoolean(STATE_ASM);
        state.locked=b.getBoolean(STATE_LOCKED,true);
        state.collapsed=b.getBoolean(STATE_COLLAPSED);
        state.landscape=b.getBoolean(STATE_LANDSCAPE);
        state.zoomFactor=b.getFloat(STATE_SCALE,1);
        state.centerX=b.getFloat(STATE_CENTER_X,128);
        state.centerY=b.getFloat(STATE_CENTER_Y,96);
        state.inkColor=b.getInt(STATE_COLOR,7);
        state.paperColor=b.getInt(STATE_PAPER_COLOR,0);
        state.bright=b.getBoolean(STATE_BRIGHT);
        state.flash=b.getBoolean(STATE_FLASH);
        state.applyBright=b.getBoolean(STATE_BRIGHT_SET);
        state.applyFlash=b.getBoolean(STATE_FLASH_SET);
        try {state.tool=SpectrumScreenDocument.Tool.valueOf(b.getString(STATE_TOOL,"DRAW"));}
        catch(Exception ignored){}
        try {state.previousTool=SpectrumScreenDocument.Tool.valueOf(
                b.getString(STATE_PREVIOUS_TOOL,"DRAW"));}
        catch(Exception ignored){}
        try {state.side=SpectrumScreenDocument.Side.valueOf(b.getString(STATE_SIDE,"INK"));}
        catch(Exception ignored){}
    }
    @Override protected void onDestroy(){
        // Keep only the plain Java/URI workspace; no Activity/View references.
        // This is also the temporary handoff to the Tools screen tab.
        super.onDestroy();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}
