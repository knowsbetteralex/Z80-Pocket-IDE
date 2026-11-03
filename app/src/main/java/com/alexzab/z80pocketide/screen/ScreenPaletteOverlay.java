package com.alexzab.z80pocketide.screen;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.alexzab.z80pocketide.project.SpectrumScreenDocument;

/** Collapsible semitransparent Spectrum palette pinned over screen viewport. */
public final class ScreenPaletteOverlay extends LinearLayout {
    public interface OnBrushChange { void changed(); }
    private final boolean ru;
    private final LinearLayout content;
    private final Button collapse,ink,paper;
    private final Button[] swatches=new Button[8];
    private final CheckBox bright,flash;
    private final Button[] tools=new Button[4];
    private boolean collapsed;
    private SpectrumScreenDocument.Side side=SpectrumScreenDocument.Side.INK;
    private SpectrumScreenDocument.Tool tool=SpectrumScreenDocument.Tool.DRAW;
    private int color=7;
    private OnBrushChange listener;

    public ScreenPaletteOverlay(Context context,boolean ru) {
        super(context);this.ru=ru;
        setOrientation(VERTICAL);
        setPadding(dp(6),dp(5),dp(6),dp(6));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xDDF5FAF6);
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1),0xAA8DBD9F);
        setBackground(bg);setElevation(dp(5));
        collapse=button("▴");
        collapse.setContentDescription(ru?"Свернуть палетку":"Collapse palette");
        addView(collapse,new LayoutParams(-1,dp(31)));
        content=new LinearLayout(context);
        content.setOrientation(VERTICAL);
        addView(content);
        collapse.setOnClickListener(v->setCollapsed(!collapsed));

        LinearLayout sideRow=row();
        ink=button("INK");paper=button("PAPER");
        sideRow.addView(ink,new LayoutParams(0,dp(35),1));
        sideRow.addView(paper,new LayoutParams(0,dp(35),1));
        content.addView(sideRow);
        ink.setOnClickListener(v->{side=SpectrumScreenDocument.Side.INK;refresh();});
        paper.setOnClickListener(v->{side=SpectrumScreenDocument.Side.PAPER;refresh();});

        LinearLayout colors1=row(),colors2=row();
        final int[] rgb={0xFF242424,0xFF1111DD,0xFFDD2222,0xFFDD22DD,
                0xFF22BB22,0xFF22BBBB,0xFFDDDD22,0xFFFFFFFF};
        for(int i=0;i<8;i++) {
            final int n=i;
            Button b=button(i==0?"■":"●");
            b.setTextColor(rgb[i]);b.setTextSize(20);
            b.setContentDescription((ru?"Цвет ":"Color ")+i);
            b.setPadding(0,0,0,0);
            swatches[i]=b;
            (i<4?colors1:colors2).addView(b,new LayoutParams(0,dp(31),1f));
            b.setOnClickListener(v->{color=n;refresh();});
        }
        content.addView(colors1);content.addView(colors2);
        LinearLayout attrRow=row();
        bright=new CheckBox(context);bright.setText("BRIGHT");bright.setTextSize(11);
        flash=new CheckBox(context);flash.setText("FLASH");flash.setTextSize(11);
        attrRow.addView(bright,new LayoutParams(0,dp(36),1));
        attrRow.addView(flash,new LayoutParams(0,dp(36),1));
        content.addView(attrRow);
        bright.setOnCheckedChangeListener((v,checked)->notifyBrush());
        flash.setOnCheckedChangeListener((v,checked)->notifyBrush());

        LinearLayout toolRow=row();
        String[] names=ru?new String[]{"Перо","Атриб.","Пип.","Ласт."}
                :new String[]{"Draw","Attr","Pick","Erase"};
        SpectrumScreenDocument.Tool[] modes=SpectrumScreenDocument.Tool.values();
        for(int i=0;i<4;i++){
            final int which=i;
            tools[i]=button(names[i]);
            tools[i].setTextSize(10);
            toolRow.addView(tools[i],new LayoutParams(0,dp(35),1f));
            tools[i].setOnClickListener(v->{tool=modes[which];refresh();});
        }
        content.addView(toolRow);
        refresh();
    }
    public void setListener(OnBrushChange change){listener=change;notifyBrush();}
    private void notifyBrush(){if(listener!=null)listener.changed();}
    public void setCollapsed(boolean folded) {
        collapsed=folded;
        content.setVisibility(folded?View.GONE:View.VISIBLE);
        collapse.setText(folded?"▾  "+(ru?"Цвета":"Colors"):"▴");
        collapse.setContentDescription(folded
                ?(ru?"Развернуть палетку":"Expand palette")
                :(ru?"Свернуть палетку":"Collapse palette"));
    }
    public boolean isCollapsed(){return collapsed;}
    public SpectrumScreenDocument.Side side(){return side;}
    public SpectrumScreenDocument.Tool tool(){return tool;}
    public int color(){return color;}
    public boolean bright(){return bright.isChecked();}
    public boolean flash(){return flash.isChecked();}
    public void setBrush(SpectrumScreenDocument.Tool tool,
                         SpectrumScreenDocument.Side side,int color,boolean bright,boolean flash) {
        this.tool=tool;this.side=side;this.color=color;
        this.bright.setChecked(bright);
        this.flash.setChecked(flash);
        refresh();
    }
    public void pick(int attr,boolean pixel) {
        side=pixel?SpectrumScreenDocument.Side.INK:SpectrumScreenDocument.Side.PAPER;
        color=pixel?attr&7:(attr>>3)&7;
        bright.setChecked((attr&0x40)!=0);
        flash.setChecked((attr&0x80)!=0);
        refresh();
    }
    private void refresh(){
        ink.setAlpha(side==SpectrumScreenDocument.Side.INK?1:.43f);
        paper.setAlpha(side==SpectrumScreenDocument.Side.PAPER?1:.43f);
        for(int i=0;i<8;i++){
            swatches[i].setAlpha(i==color?1:.46f);
            swatches[i].setTypeface(Typeface.DEFAULT,i==color?Typeface.BOLD:Typeface.NORMAL);
        }
        SpectrumScreenDocument.Tool[] modes=SpectrumScreenDocument.Tool.values();
        for(int i=0;i<4;i++)tools[i].setAlpha(modes[i]==tool?1:.5f);
        notifyBrush();
    }
    private Button button(String title){
        Button b=new Button(getContext());b.setText(title);b.setAllCaps(false);
        b.setTextSize(12);b.setPadding(dp(2),0,dp(2),0);
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
        return b;
    }
    private LinearLayout row(){
        LinearLayout l=new LinearLayout(getContext());
        l.setOrientation(HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
