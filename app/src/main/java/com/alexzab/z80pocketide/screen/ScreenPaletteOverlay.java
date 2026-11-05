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
 * separate INK/PAPER targets, eight filled swatches and the selection tick.
 */
public final class ScreenPaletteOverlay extends LinearLayout {
    public interface OnBrushChange { void changed(); }
    public interface OnFlagChange { void toggled(boolean isBright,boolean enabled); }

    private final boolean ru;
    private final LinearLayout content;
    private final TextView[] swatches=new TextView[8];
    private final Button collapse,ink,paper,pen,eraser,picker;
    private final CheckBox bright,flash;
    private boolean collapsed,applying,applyBright,applyFlash;
    private int expandedWidth;
    private int inkColor=7,paperColor=0;
    private SpectrumScreenDocument.Side side=SpectrumScreenDocument.Side.INK;
    private SpectrumScreenDocument.Tool tool=SpectrumScreenDocument.Tool.DRAW;
    private SpectrumScreenDocument.Tool beforePick=SpectrumScreenDocument.Tool.DRAW;
    private OnBrushChange listener;
    private OnFlagChange flagListener;

    public ScreenPaletteOverlay(Context context,boolean russian) {
        super(context);
        ru=russian;
        expandedWidth=dp(336);
        setOrientation(VERTICAL);
        setClickable(true);
        setPadding(dp(6),dp(5),dp(6),dp(7));
        setBackground(round(0xDFF3F9F4,13,0xB19FC8B1,1));
        setElevation(dp(5));

        collapse=button(ru?"▴  Палитра":"▴  Palette",12);
        collapse.setContentDescription(ru?"Свернуть палетку":"Collapse palette");
        addView(collapse,new LayoutParams(-1,dp(30)));
        collapse.setOnClickListener(v->setCollapsed(!collapsed));

        content=new LinearLayout(context);
        content.setOrientation(VERTICAL);
        addView(content,new LayoutParams(-1,-2));

        LinearLayout titles=row();
        ink=button("INK · 7",15);
        paper=button("PAPER · 0",15);
        titles.addView(ink,weighted(0,dp(40)));
        titles.addView(paper,weighted(0,dp(40)));
        content.addView(titles);
        ink.setOnClickListener(v->selectSide(SpectrumScreenDocument.Side.INK));
        paper.setOnClickListener(v->selectSide(SpectrumScreenDocument.Side.PAPER));

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
        modes.addView(pen,weighted(0,dp(38)));
        modes.addView(eraser,weighted(0,dp(38)));
        modes.addView(picker,weighted(0,dp(38)));
        content.addView(modes);
        pen.setOnClickListener(v->{
            tool=SpectrumScreenDocument.Tool.DRAW;
            beforePick=tool;
            side=SpectrumScreenDocument.Side.INK;
            refresh();
        });
        eraser.setOnClickListener(v->{
            tool=SpectrumScreenDocument.Tool.ERASE;
            beforePick=tool;
            side=SpectrumScreenDocument.Side.PAPER;
            refresh();
        });
        picker.setOnClickListener(v->{
            if(tool!=SpectrumScreenDocument.Tool.PICK)beforePick=tool;
            tool=SpectrumScreenDocument.Tool.PICK;
            refresh();
        });
        refresh();
    }

    public void setListener(OnBrushChange change){listener=change;notifyBrush();}
    public void setFlagListener(OnFlagChange change){flagListener=change;}
    private void notifyBrush(){if(listener!=null)listener.changed();}
    public SpectrumScreenDocument.Side side(){return side;}
    public SpectrumScreenDocument.Tool tool(){return tool;}
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
        collapse.setText(value?(ru?"▾  Палитра":"▾  Palette"):
                (ru?"▴  Палитра":"▴  Palette"));
        collapse.setContentDescription(value?
                (ru?"Развернуть палетку":"Expand palette"):
                (ru?"Свернуть палетку":"Collapse palette"));
        updateWidth();
    }
    private void updateWidth(){
        android.view.ViewGroup.LayoutParams layout=getLayoutParams();
        if(layout!=null) {
            layout.width=collapsed?dp(112):expandedWidth;
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

    private void selectSide(SpectrumScreenDocument.Side selected){
        side=selected;
        tool=selected==SpectrumScreenDocument.Side.INK
                ?SpectrumScreenDocument.Tool.DRAW
                :SpectrumScreenDocument.Tool.ERASE;
        beforePick=tool;
        refresh();
    }

    private void refresh(){
        ink.setText("INK · "+inkColor);
        paper.setText("PAPER · "+paperColor);
        applyModeStyle(ink,side==SpectrumScreenDocument.Side.INK);
        applyModeStyle(paper,side==SpectrumScreenDocument.Side.PAPER);
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
