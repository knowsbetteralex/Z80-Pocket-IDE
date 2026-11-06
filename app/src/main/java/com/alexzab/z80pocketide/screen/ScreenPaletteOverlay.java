package com.alexzab.z80pocketide.screen;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.alexzab.z80pocketide.project.SpectrumScreenDocument;
import com.alexzab.z80pocketide.tools.SpriteData;

/**
 * Floating palette visually consistent with SpriteToolPanel:
 * eight filled swatches, tool-driven INK/PAPER selection and the selection tick.
 */
public final class ScreenPaletteOverlay extends LinearLayout {
    public interface OnBrushChange { void changed(); }
    public interface OnFlagChange { void toggled(boolean isBright,boolean enabled); }
    public interface OnCollapseChange { void changed(boolean collapsed); }
    public interface OnLockChange { void changed(boolean locked); }

    private final boolean ru;
    private final LinearLayout content,compactTools;
    private final TextView[] swatches=new TextView[8];
    private final Button collapse,pen,eraser,picker,selectionButton,expandedLock;
    private final Button compactPen,compactEraser,compactPicker,compactLock;
    private final CheckBox bright,flash;
    private boolean collapsed,applying,applyBright,applyFlash,locked=true;
    private int expandedWidth;
    private int inkColor=7,paperColor=0;
    private SpectrumScreenDocument.Side side=SpectrumScreenDocument.Side.INK;
    private SpectrumScreenDocument.Tool tool=SpectrumScreenDocument.Tool.DRAW;
    private SpectrumScreenDocument.Tool beforePick=SpectrumScreenDocument.Tool.DRAW;
    private OnBrushChange listener;
    private OnFlagChange flagListener;
    private OnCollapseChange collapseListener;
    private OnLockChange lockListener;

    public ScreenPaletteOverlay(Context context,boolean russian) {
        super(context);
        ru=russian;
        expandedWidth=dp(336);
        setOrientation(VERTICAL);
        setClickable(true);
        setPadding(dp(6),dp(5),dp(6),dp(7));
        setBackground(round(0xDFF3F9F4,13,0xB19FC8B1,1));
        setElevation(dp(5));

        LinearLayout paletteHeader=row();
        collapse=button(ru?"▴  Палитра":"▴  Palette",12);
        collapse.setContentDescription(ru?"Свернуть палетку":"Collapse palette");
        expandedLock=button("🔒",19);
        paletteHeader.addView(collapse,new LayoutParams(0,dp(30),1f));
        paletteHeader.addView(expandedLock,new LayoutParams(dp(38),dp(30)));
        addView(paletteHeader,new LayoutParams(-1,dp(30)));
        collapse.setOnClickListener(v->setCollapsed(!collapsed));
        expandedLock.setOnClickListener(v->toggleLock());

        compactTools=row();
        compactTools.setGravity(Gravity.CENTER);
        compactPen=button("✎",20);
        compactEraser=button("▱",20);
        compactPicker=button("⌖",20);
        compactLock=button("🔒",18);
        compactPen.setContentDescription(ru?"Перо (INK)":"Pen (INK)");
        compactEraser.setContentDescription(ru?"Ластик (PAPER)":"Eraser (PAPER)");
        compactPicker.setContentDescription(ru?"Пипетка":"Picker");
        compactLock.setOnClickListener(v->toggleLock());
        for(Button b:new Button[]{compactPen,compactEraser,compactPicker,compactLock}) {
            LinearLayout.LayoutParams lp=weighted(0,dp(34));
            lp.setMargins(dp(1),dp(2),dp(1),dp(1));
            compactTools.addView(b,lp);
        }
        compactTools.setVisibility(View.GONE);
        addView(compactTools,new LayoutParams(-1,dp(39)));

        content=new LinearLayout(context);
        content.setOrientation(VERTICAL);
        addView(content,new LayoutParams(-1,-2));

        LinearLayout colors=row();
        for(int i=0;i<8;i++) {
            final int color=i;
            TextView swatch=new TextView(context);
            swatch.setGravity(Gravity.CENTER);
            swatch.setTextSize(19);
            swatch.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            swatch.setContentDescription((ru?"Цвет ":"Color ")+i);
            LinearLayout.LayoutParams params=weighted(0,dp(40));
            params.setMargins(dp(2),dp(3),dp(2),dp(3));
            colors.addView(swatch,params);
            swatch.setOnClickListener(v->{
                if(side==SpectrumScreenDocument.Side.INK)inkColor=color;
                else paperColor=color;
                // A color choice exits the picker and edits the selected side.
                if(tool==SpectrumScreenDocument.Tool.PICK)
                    tool=side==SpectrumScreenDocument.Side.INK
                            ?SpectrumScreenDocument.Tool.DRAW
                            :SpectrumScreenDocument.Tool.ERASE;
                refresh();
            });
            swatches[i]=swatch;
        }
        content.addView(colors);

        LinearLayout switches=row();
        bright=new CheckBox(context);
        bright.setText("BRIGHT");
        bright.setTextSize(12);
        flash=new CheckBox(context);
        flash.setText("FLASH");
        flash.setTextSize(12);
        switches.addView(bright,weighted(0,dp(38)));
        switches.addView(flash,weighted(0,dp(38)));
        content.addView(switches);
        bright.setOnCheckedChangeListener((v,enabled)->{
            if(applying)return;
            applyBright=true;
            refresh();
            if(flagListener!=null)flagListener.toggled(true,enabled);
        });
        flash.setOnCheckedChangeListener((v,enabled)->{
            if(applying)return;
            applyFlash=true;
            refresh();
            if(flagListener!=null)flagListener.toggled(false,enabled);
        });

        LinearLayout modes=row();
        pen=button(ru?"Перо (INK)":"Pen (INK)",11);
        eraser=button(ru?"Ластик (PAPER)":"Eraser (PAPER)",11);
        picker=button(ru?"Пипетка":"Picker",11);
        selectionButton=button(ru?"Выдел.":"Select",11);
        modes.addView(pen,weighted(0,dp(38)));
        modes.addView(eraser,weighted(0,dp(38)));
        modes.addView(picker,weighted(0,dp(38)));
        modes.addView(selectionButton,weighted(0,dp(38)));
        content.addView(modes);
        pen.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.DRAW));
        eraser.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.ERASE));
        picker.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.PICK));
        selectionButton.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.SELECT));
        compactPen.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.DRAW));
        compactEraser.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.ERASE));
        compactPicker.setOnClickListener(v->selectTool(SpectrumScreenDocument.Tool.PICK));
        refreshLock();
        refresh();
    }

    public void setListener(OnBrushChange change){listener=change;notifyBrush();}
    public void setFlagListener(OnFlagChange change){flagListener=change;}
    public void setCollapseListener(OnCollapseChange change){collapseListener=change;}
    public void setLockListener(OnLockChange change){lockListener=change;}
    /** Lock state is shared by the Tools tab and full-screen screen editor. */
    public void setLocked(boolean value){
        locked=value;
        refreshLock();
    }
    private void toggleLock(){
        setLocked(!locked);
        if(lockListener!=null)lockListener.changed(locked);
    }
    private void refreshLock(){
        String icon=locked?"🔒":"🔓";
        String description=locked
                ?(ru?"Разблокировать рисование":"Unlock drawing")
                :(ru?"Заблокировать рисование":"Lock drawing");
        for(Button button:new Button[]{expandedLock,compactLock}){
            button.setText(icon);
            button.setContentDescription(description);
            applyModeStyle(button,!locked);
        }
    }
    private void notifyBrush(){if(listener!=null)listener.changed();}
    public SpectrumScreenDocument.Side side(){return side;}
    public SpectrumScreenDocument.Tool tool(){return tool;}
    public void activateSelection(){selectTool(SpectrumScreenDocument.Tool.SELECT);}
    public SpectrumScreenDocument.Tool previousTool(){return beforePick;}
    public void setPreviousTool(SpectrumScreenDocument.Tool previous){
        if(previous==SpectrumScreenDocument.Tool.DRAW
                ||previous==SpectrumScreenDocument.Tool.ERASE)
            beforePick=previous;
    }
    public int inkColor(){return inkColor;}
    public int paperColor(){return paperColor;}
    public int color(){return side==SpectrumScreenDocument.Side.INK?inkColor:paperColor;}
    public boolean bright(){return bright.isChecked();}
    public boolean flash(){return flash.isChecked();}
    public boolean applyBright(){return applyBright;}
    public boolean applyFlash(){return applyFlash;}
    public boolean isCollapsed(){return collapsed;}

    public void setExpandedWidth(int widthPixels){
        expandedWidth=Math.max(dp(208),widthPixels);
        updateWidth();
    }
    public void setCollapsed(boolean value){
        collapsed=value;
        content.setVisibility(value?View.GONE:View.VISIBLE);
        expandedLock.setVisibility(value?View.GONE:View.VISIBLE);
        compactTools.setVisibility(value?View.VISIBLE:View.GONE);
        collapse.setText(value?"▾":(ru?"▴  Палитра":"▴  Palette"));
        collapse.setContentDescription(value?
                (ru?"Развернуть палетку":"Expand palette"):
                (ru?"Свернуть палетку":"Collapse palette"));
        updateWidth();
        if(collapseListener!=null)collapseListener.changed(value);
    }
    private void updateWidth(){
        android.view.ViewGroup.LayoutParams layout=getLayoutParams();
        if(layout!=null) {
            layout.width=collapsed?dp(168):expandedWidth;
            setLayoutParams(layout);
        }
    }

    public void setBrush(SpectrumScreenDocument.Tool newTool,
                         SpectrumScreenDocument.Side newSide,int color,
                         boolean newBright,boolean newFlash,
                         boolean brightSet,boolean flashSet) {
        setBrushColors(newTool,newSide,
                newSide==SpectrumScreenDocument.Side.INK?color:inkColor,
                newSide==SpectrumScreenDocument.Side.PAPER?color:paperColor,
                newBright,newFlash,brightSet,flashSet);
    }
    public void setBrushColors(SpectrumScreenDocument.Tool newTool,
                               SpectrumScreenDocument.Side newSide,
                               int newInk,int newPaper,
                               boolean newBright,boolean newFlash,
                               boolean brightSet,boolean flashSet){
        tool=newTool;side=newSide;
        if(newTool!=SpectrumScreenDocument.Tool.PICK)
            beforePick=newTool;
        inkColor=newInk&7;paperColor=newPaper&7;
        applying=true;
        bright.setChecked(newBright);
        flash.setChecked(newFlash);
        applying=false;
        applyBright=brightSet;applyFlash=flashSet;
        refresh();
    }

    /** Pick both tile colors but restore the paint tool/target from before picking. */
    public void pick(int attr,boolean pixel){
        inkColor=attr&7;
        paperColor=(attr>>3)&7;
        applying=true;
        bright.setChecked((attr&0x40)!=0);
        flash.setChecked((attr&0x80)!=0);
        applying=false;
        applyBright=true;
        applyFlash=true;
        tool=beforePick==SpectrumScreenDocument.Tool.PICK
                ?SpectrumScreenDocument.Tool.DRAW:beforePick;
        side=tool==SpectrumScreenDocument.Tool.ERASE
                ?SpectrumScreenDocument.Side.PAPER
                :SpectrumScreenDocument.Side.INK;
        refresh();
    }

    /** Select the same tool from either the expanded or compact palette. */
    private void selectTool(SpectrumScreenDocument.Tool selected){
        if(selected==SpectrumScreenDocument.Tool.PICK){
            if(tool!=SpectrumScreenDocument.Tool.PICK)beforePick=tool;
        }else{
            beforePick=selected;
            side=selected==SpectrumScreenDocument.Tool.ERASE
                    ?SpectrumScreenDocument.Side.PAPER
                    :SpectrumScreenDocument.Side.INK;
        }
        tool=selected;
        refresh();
    }

    private void refresh(){
        int selected=color();
        for(int i=0;i<8;i++){
            int color=SpriteData.palette(i|(bright.isChecked()?8:0));
            GradientDrawable background=round(color,9,
                    i==selected?0xFFFFA51C:0xFF8EAA9B,i==selected?3:1);
            swatches[i].setBackground(background);
            swatches[i].setText(i==selected?"✓":"");
            swatches[i].setTextColor((i==0||i==1||i==2)?Color.WHITE:Color.BLACK);
        }
        applyModeStyle(pen,tool==SpectrumScreenDocument.Tool.DRAW);
        applyModeStyle(eraser,tool==SpectrumScreenDocument.Tool.ERASE
                ||tool==SpectrumScreenDocument.Tool.ATTRIBUTE);
        applyModeStyle(picker,tool==SpectrumScreenDocument.Tool.PICK);
        applyModeStyle(selectionButton,tool==SpectrumScreenDocument.Tool.SELECT);
        applyModeStyle(compactPen,tool==SpectrumScreenDocument.Tool.DRAW);
        applyModeStyle(compactEraser,tool==SpectrumScreenDocument.Tool.ERASE
                ||tool==SpectrumScreenDocument.Tool.ATTRIBUTE);
        applyModeStyle(compactPicker,tool==SpectrumScreenDocument.Tool.PICK);
        notifyBrush();
    }
    private void applyModeStyle(Button button,boolean selected){
        button.setBackground(round(selected?0xFF00835D:0xFFE3F4EA,14,
                selected?0xFF006347:0xFFA8D0BB,1));
        button.setTextColor(selected?Color.WHITE:0xFF185E44);
        button.setAlpha(1f);
    }
    private GradientDrawable round(int fill,int radius,int stroke,int width){
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(width),stroke);
        return drawable;
    }
    private Button button(String text,int textSize){
        Button b=new Button(getContext());
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(textSize);
        b.setPadding(dp(1),0,dp(1),0);
        b.setMinHeight(0);b.setMinimumHeight(0);
        b.setMinWidth(0);b.setMinimumWidth(0);
        return b;
    }
    private LinearLayout row(){
        LinearLayout layout=new LinearLayout(getContext());
        layout.setOrientation(HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }
    private LayoutParams weighted(int width,int height){
        LayoutParams p=new LayoutParams(width,height,1f);
        p.setMargins(dp(2),dp(1),dp(2),dp(1));
        return p;
    }
    private int dp(int value){
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
}
