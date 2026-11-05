package com.alexzab.z80pocketide.screen;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.alexzab.z80pocketide.project.SpectrumScreenDocument;

/** Shared compact UI for rectangle selection, clipboard and one-pixel nudges. */
public final class ScreenSelectionBar extends LinearLayout {
    private final Activity activity;
    private final boolean ru;
    private ScreenEditorActivity.Session session;
    private final ScreenEditorCanvasView canvas;
    private final ScreenPaletteOverlay palette;
    private final Runnable onRefresh;
    private final Button copy,paste,left,right,up,down,confirm,cancel,select;
    private final CheckBox attrs;

    public ScreenSelectionBar(Activity activity,boolean russian,
                              ScreenEditorActivity.Session session,
                              ScreenEditorCanvasView canvas,ScreenPaletteOverlay palette,
                              Runnable onRefresh){
        super(activity);
        this.activity=activity;
        this.ru=russian;
        this.session=session;
        this.canvas=canvas;
        this.palette=palette;
        this.onRefresh=onRefresh;
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF264336);
        setPadding(dp(3),dp(2),dp(3),dp(3));

        LinearLayout top=row();
        select=button("▭",ru?"Нарисовать выделение":"Draw selection");
        copy=button(ru?"Копия":"Copy",ru?"Копировать область":"Copy rectangle");
        paste=button(ru?"Встав.":"Paste",ru?"Вставить область":"Paste rectangle");
        attrs=new CheckBox(activity);
        attrs.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFFB2EDD0));
        attrs.setText(ru?"Сдвиг атр. 8px":"Shift attrs 8px");
        attrs.setTextColor(Color.WHITE);
        attrs.setTextSize(12);
        attrs.setChecked(session.selection.moveAttributes());
        top.addView(select,new LayoutParams(dp(45),dp(39)));
        top.addView(copy,new LayoutParams(0,dp(39),1f));
        top.addView(paste,new LayoutParams(0,dp(39),1f));
        top.addView(attrs,new LayoutParams(0,dp(39),1.5f));
        addView(top);

        LinearLayout bottom=row();
        left=button("←",ru?"Влево на один пиксель":"Move one pixel left");
        right=button("→",ru?"Вправо на один пиксель":"Move one pixel right");
        up=button("↑",ru?"Вверх на один пиксель":"Move one pixel up");
        down=button("↓",ru?"Вниз на один пиксель":"Move one pixel down");
        confirm=button("✓",ru?"Применить перенос":"Apply move");
        cancel=button("×",ru?"Отменить перенос или выделение":"Cancel move or selection");
        for(Button b:new Button[]{left,right,up,down,confirm,cancel})
            bottom.addView(b,new LayoutParams(0,dp(37),1f));
        addView(bottom);

        select.setOnClickListener(v->{
            if(session.selection.preview()){
                toast(ru?"Сначала примените или отмените перенос":
                        "Apply or cancel the current move first");
                return;
            }
            palette.activateSelection();
            refresh();
        });
        copy.setOnClickListener(v->{
            if(session.selection.preview()) {
                toast(ru?"Сначала примените перенос":"Apply move before copying");
                return;
            }
            ScreenSelectionClip clip=session.selection.copy(session.doc);
            if(clip==null){toast(ru?"Выделите прямоугольник":"Select a rectangle");return;}
            try {
                ScreenClipboard.store(activity,clip);
                toast(ru?"Скопировано. Откройте другой SCR и нажмите «Вставить»":
                        "Copied. Open another SCR and tap Paste");
            } catch(Exception ex){toast(ex.getMessage());}
            refresh();
        });
        paste.setOnClickListener(v->{
            if(session.selection.preview()){
                toast(ru?"Сначала примените или отмените перенос":
                        "Apply or cancel the current move first");
                return;
            }
            if(session.locked){lockedMessage();return;}
            try {
                ScreenSelectionClip clip=ScreenClipboard.get(activity);
                if(clip==null){toast(ru?"Буфер выделения пуст":"Screen clipboard is empty");return;}
                session.selection.paste(session.doc,session.history,clip);
                canvas.refreshPixels();
                afterChange();
            }catch(Exception ex){toast(ex.getMessage());}
        });
        attrs.setOnCheckedChangeListener((v,checked)->{
            session.selection.setMoveAttributes(session.doc,checked);
            canvas.refreshPixels();
            afterChange();
        });
        left.setOnClickListener(v->nudge(-1,0));
        right.setOnClickListener(v->nudge(1,0));
        up.setOnClickListener(v->nudge(0,-1));
        down.setOnClickListener(v->nudge(0,1));
        confirm.setOnClickListener(v->{
            if(session.selection.commit(session.doc,session.history)){
                canvas.refreshPixels();
                afterChange();
            }else refresh();
        });
        cancel.setOnClickListener(v->{
            if(session.selection.preview())
                session.selection.cancel(session.doc,session.history);
            else if(session.selection.selected())
                session.selection.clear();
            canvas.refreshPixels();
            afterChange();
        });
        refresh();
    }

    public void bind(ScreenEditorActivity.Session next){
        session=next;
        refresh();
    }
    public void refresh(){
        boolean has=session.selection.selected();
        boolean editing=session.selection.preview();
        boolean selectable=!editing;
        select.setEnabled(selectable);
        copy.setEnabled(has&&!editing);
        left.setEnabled(has);right.setEnabled(has);
        up.setEnabled(has);down.setEnabled(has);
        confirm.setEnabled(editing);
        cancel.setEnabled(has);
        attrs.setChecked(session.selection.moveAttributes());
        select.setAlpha(palette.tool()==SpectrumScreenDocument.Tool.SELECT?1:.70f);
    }
    private void nudge(int dx,int dy){
        if(session.locked){lockedMessage();return;}
        if(!session.selection.selected()){
            toast(ru?"Сначала выделите прямоугольник":"Select a rectangle first");
            return;
        }
        if(session.selection.move(session.doc,session.history,dx,dy)){
            canvas.refreshPixels();
            afterChange();
        } else toast(ru?"Граница экрана":"Screen edge");
    }
    private void afterChange(){
        canvas.refreshSelection();
        if(onRefresh!=null)onRefresh.run();
        refresh();
    }
    private void lockedMessage(){
        toast(ru?"Снимите замок перед перемещением":"Unlock drawing to move a selection");
    }
    private void toast(String text){
        Toast.makeText(activity,text,Toast.LENGTH_SHORT).show();
    }
    private LinearLayout row(){
        LinearLayout r=new LinearLayout(activity);
        r.setOrientation(HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }
    private Button button(String text,String description){
        Button b=new Button(activity);
        b.setText(text);b.setTextSize(14);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setContentDescription(description);
        b.setMinWidth(0);b.setMinimumWidth(0);
        b.setMinHeight(0);b.setMinimumHeight(0);
        b.setPadding(dp(2),0,dp(2),0);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xFF37725B);bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1),0xFF619780);
        b.setBackground(bg);
        return b;
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}
