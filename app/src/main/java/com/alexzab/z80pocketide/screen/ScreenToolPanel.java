package com.alexzab.z80pocketide.screen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexzab.z80pocketide.project.SpectrumScreenDocument;
import com.alexzab.z80pocketide.ui.UiStyle;

/**
 * The same editable SCREEN$ workspace within the Tools tab.
 * The full-screen editor shares the byte document/history and performs SAF I/O.
 */
public final class ScreenToolPanel extends LinearLayout {
    private final Activity activity;
    private final boolean ru;
    private ScreenEditorActivity.Session session;
    private ScreenEditorCanvasView canvas;
    private ScreenPaletteOverlay palette;
    private final TextView title,hint;
    private final Button lock,undo,redo;

    public ScreenToolPanel(Activity activity,boolean ru){
        super(activity);
        this.activity=activity;
        this.ru=ru;
        session=ScreenEditorActivity.sharedSession();
        setOrientation(VERTICAL);
        setPadding(dp(3),dp(7),dp(3),dp(7));

        LinearLayout heading=row();
        title=new TextView(activity);
        title.setTextSize(17);
        title.setTextColor(0xFF245C3D);
        title.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(title,new LayoutParams(0,dp(37),1f));
        Button full=button("⤢",ru?"Во весь экран":"Full screen");
        UiStyle.styleGreenButton(full);
        heading.addView(full,new LayoutParams(dp(63),dp(42)));
        full.setOnClickListener(v->openFullScreen(null));
        addView(heading);

        LinearLayout actions=row();
        Button open=button("▤",ru?"Открыть SCR, BIN, ASM":"Open SCR, BIN, ASM");
        Button save=button("▣",ru?"Сохранить SCR/BIN":"Save SCR/BIN");
        undo=button("↶",ru?"Отменить":"Undo");
        redo=button("↷",ru?"Повторить":"Redo");
        Button fit=button("⊡",ru?"Вписать":"Fit");
        lock=button("🔒",ru?"Блокировка":"Lock");
        for(Button b:new Button[]{open,save,undo,redo,fit,lock}) {
            UiStyle.styleSoftGreenButton(b);
            LayoutParams lp=new LayoutParams(0,dp(43),1f);
            lp.setMargins(dp(1),0,dp(1),0);
            actions.addView(b,lp);
        }
        addView(actions);
        open.setOnClickListener(v->openFullScreen("open"));
        save.setOnClickListener(v->openFullScreen("save"));
        undo.setOnClickListener(v->{
            if(session.history.undo(session.doc)){canvas.refreshPixels();refreshTitle();}
        });
        redo.setOnClickListener(v->{
            if(session.history.redo(session.doc)){canvas.refreshPixels();refreshTitle();}
        });
        fit.setOnClickListener(v->canvas.fitToScreen());
        lock.setOnClickListener(v->{
            session.locked=!session.locked;
            canvas.setLocked(session.locked);
            refreshTitle();
        });

        FrameLayout canvasHolder=new FrameLayout(activity);
        LayoutParams frameParams=new LayoutParams(-1,dp(390));
        frameParams.setMargins(0,dp(6),0,dp(3));
        addView(canvasHolder,frameParams);
        canvas=new ScreenEditorCanvasView(activity,session.doc);
        canvasHolder.addView(canvas,new FrameLayout.LayoutParams(-1,-1));
        canvas.setLocked(session.locked);
        canvas.setListener(new ScreenEditorCanvasView.Listener() {
            @Override public void strokeStart(){session.history.begin(session.doc);}
            @Override public void strokeEnd(){
                session.history.end(session.doc);
                refreshTitle();
            }
            @Override public void changed(int x,int y){refreshTitle();}
            @Override public void picked(int x,int y,int attribute,boolean ink){
                palette.pick(attribute,ink);
                refreshTitle();
            }
        });

        palette=new ScreenPaletteOverlay(activity,ru);
        FrameLayout.LayoutParams palParams=new FrameLayout.LayoutParams(
                dp(320),-2,Gravity.TOP|Gravity.RIGHT);
        palParams.setMargins(dp(3),dp(5),dp(5),0);
        canvasHolder.addView(palette,palParams);
        palette.setCollapsed(session.collapsed);
        palette.setBrushColors(session.tool,session.side,
                session.inkColor,session.paperColor,
                session.bright,session.flash,session.applyBright,session.applyFlash);
        palette.setListener(()->{
            session.tool=palette.tool();
            session.side=palette.side();
            session.inkColor=palette.inkColor();
            session.paperColor=palette.paperColor();
            session.bright=palette.bright();
            session.flash=palette.flash();
            session.applyBright=palette.applyBright();
            session.applyFlash=palette.applyFlash();
            canvas.setBrush(session.tool,session.side,palette.color(),
                    session.applyBright?session.bright:null,
                    session.applyFlash?session.flash:null);
        });
        palette.setFlagListener((isBright,enabled)->
                canvas.toggleFlagOnSelection(isBright,enabled));
        canvasHolder.post(()->palette.setExpandedWidth(
                Math.min(dp(410),Math.max(dp(208),canvasHolder.getWidth()-dp(12)))));

        hint=new TextView(activity);
        hint.setTextSize(12);
        hint.setTextColor(0xFF63806F);
        hint.setPadding(dp(4),dp(5),dp(4),0);
        hint.setText(ru
                ?"Рисование: снимите замок. Два пальца — перемещение и масштаб. Пипетка возвращает прежний инструмент."
                :"Unlock to draw. Two fingers pan and zoom. Picker returns to the previous tool.");
        addView(hint);
        refreshTitle();
    }

    /** Called on returning from full-screen editor or after Activity recreation. */
    public void refreshFromSession() {
        session=ScreenEditorActivity.sharedSession();
        canvas.setDocument(session.doc);
        canvas.setLocked(session.locked);
        palette.setBrushColors(session.tool,session.side,
                session.inkColor,session.paperColor,
                session.bright,session.flash,session.applyBright,session.applyFlash);
        palette.setCollapsed(session.collapsed);
        refreshTitle();
    }

    private void openFullScreen(String action){
        session.collapsed=palette.isCollapsed();
        if(canvas.getHeight()>0){
            float fit=Math.max(.1f,Math.min(
                    (canvas.getWidth()-dp(20))/256f,
                    (canvas.getHeight()-dp(20))/192f));
            session.zoomFactor=canvas.getPixelSize()/fit;
            session.centerX=(canvas.getWidth()*.5f-canvas.getOriginX())/canvas.getPixelSize();
            session.centerY=(canvas.getHeight()*.5f-canvas.getOriginY())/canvas.getPixelSize();
        }
        Intent intent=new Intent(activity,ScreenEditorActivity.class);
        if(action!=null)intent.putExtra(ScreenEditorActivity.EXTRA_START_ACTION,action);
        activity.startActivity(intent);
    }
    private void refreshTitle(){
        title.setText((session.history.isDirty(session.doc)?"● ":"")
                +(ru?"Экран":"Screen")+" · "+session.title);
        lock.setText(session.locked?"🔒":"🔓");
        undo.setEnabled(session.history.canUndo());
        redo.setEnabled(session.history.canRedo());
    }
    private LinearLayout row(){
        LinearLayout group=new LinearLayout(activity);
        group.setGravity(Gravity.CENTER_VERTICAL);
        group.setOrientation(HORIZONTAL);
        return group;
    }
    private Button button(String text,String accessibility){
        Button b=new Button(activity);
        b.setText(text);b.setTextSize(20);b.setAllCaps(false);
        b.setMinWidth(0);b.setMinimumWidth(0);
        b.setPadding(dp(2),0,dp(2),0);
        b.setContentDescription(accessibility);
        return b;
    }
    private int dp(int amount){
        return (int)(amount*getResources().getDisplayMetrics().density+.5f);
    }
}
