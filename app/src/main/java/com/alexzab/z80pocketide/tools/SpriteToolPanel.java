package com.alexzab.z80pocketide.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;

import com.alexzab.z80pocketide.ui.UiStyle;

/** Touch sprite painter with per-tile INK/PAPER/BRIGHT and export to DB. */
public final class SpriteToolPanel extends LinearLayout {
    public interface InsertAction { void insert(String source); }
    private final boolean ru;
    private final InsertAction insertAction;
    private final SpriteCanvasView canvas;
    private SpriteData sprite;
    private SpriteData.Mode brushMode = SpriteData.Mode.INK;
    private int color = 15;
    private boolean panMode;
    private boolean exportBinary;
    private final TextView bitmapPreview;
    private final TextView attrPreview;
    private final TextView modeLabel;
    private final Button[] modeButtons = new Button[4];
    private final View[] paletteButtons = new View[16];
    private final Spinner widthChooser, heightChooser;
    private boolean sizing;
    private static final Integer[] SIZES = {8, 16, 24, 32, 48, 64};

    public SpriteToolPanel(Context context, boolean ru, InsertAction action) {
        super(context);
        this.ru = ru;
        this.insertAction = action;
        this.sprite = new SpriteData(16,16);
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(8), dp(2), dp(18));

        addView(title(t("Spectrum sprite studio", "Редактор спрайтов Spectrum"), 19));
        addView(detail(t("Pixel art with authentic 8×8 tile attributes. Touch to draw; pinch to zoom.",
                "Рисование с настоящими атрибутами 8×8. Палец — рисование, два — масштаб.")));

        LinearLayout sizeRow = row();
        widthChooser = spinner();
        heightChooser = spinner();
        sizeRow.addView(labeledSpinner(t("Width (px)", "Ширина (пкс)"),widthChooser));
        sizeRow.addView(labeledSpinner(t("Height (px)", "Высота (пкс)"),heightChooser));
        addView(sizeRow);

        LinearLayout zoom = row();
        Button minus = smallButton("−");
        Button plus = smallButton("+");
        Button resetZoom = smallButton(t("Fit", "Вписать"));
        zoom.addView(minus, new LayoutParams(0,dp(42),1f));
        zoom.addView(plus, new LayoutParams(0,dp(42),1f));
        zoom.addView(resetZoom, new LayoutParams(0,dp(42),1f));
        addView(zoom);
        canvas = new SpriteCanvasView(context, sprite);
        LayoutParams cp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(315));
        cp.setMargins(0,dp(7),0,dp(10));
        addView(canvas, cp);
        minus.setOnClickListener(v->canvas.zoom(.8f));
        plus.setOnClickListener(v->canvas.zoom(1.25f));
        resetZoom.setOnClickListener(v->canvas.resetView());

        addView(detail(t("Brush: INK draws pixels, PAPER recolors the 8×8 cell, ERASE clears pixels; HAND pans.",
                "INK рисует пиксели, PAPER красит фон знакоместа, ERASE стирает, РУКА двигает сетку.")));
        LinearLayout modes = row();
        String[] en = {"INK","PAPER","ERASE","HAND"};
        String[] russian = {"INK","PAPER","СТЕРЕТЬ","РУКА"};
        for(int i=0;i<4;i++){
            final int id=i;
            Button b=smallButton(ru?russian[i]:en[i]);
            modeButtons[i]=b;
            modes.addView(b,new LayoutParams(0,dp(42),1f));
            b.setOnClickListener(v->{
                panMode=id==3;
                if(id==0)brushMode=SpriteData.Mode.INK;
                else if(id==1)brushMode=SpriteData.Mode.PAPER;
                else if(id==2)brushMode=SpriteData.Mode.ERASE;
                refreshBrush();
            });
        }
        addView(modes);
        modeLabel = detail("");
        addView(modeLabel);

        addView(title(t("Spectrum palette (normal / bright)",
                "Палитра Spectrum (обычная / яркая)"),15));
        for(int line=0;line<2;line++){
            LinearLayout colors = row();
            for(int k=0;k<8;k++){
                final int choice=line*8+k;
                TextView swatch=new TextView(context);
                swatch.setText(choice==15 ? "✓" : "");
                swatch.setGravity(Gravity.CENTER);
                swatch.setTextColor(Color.BLACK);
                paletteButtons[choice]=swatch;
                colors.addView(swatch,new LayoutParams(0,dp(37),1f));
                swatch.setOnClickListener(v->{color=choice;refreshBrush();});
            }
            addView(colors);
        }

        LinearLayout exportRow = row();
        Button formatButton = smallButton("HEX ⇄ BIN");
        Button clear = smallButton(t("Clear sprite","Очистить"));
        exportRow.addView(formatButton,new LayoutParams(0,dp(42),1f));
        exportRow.addView(clear,new LayoutParams(0,dp(42),1f));
        addView(exportRow);
        formatButton.setOnClickListener(v->{exportBinary=!exportBinary;refreshExport();});
        clear.setOnClickListener(v->new AlertDialog.Builder(getContext())
                .setTitle(t("Clear pixels and attributes?","Очистить пиксели и атрибуты?"))
                .setPositiveButton(t("Clear","Очистить"),(d,w)->{sprite.clear();canvas.invalidate();refreshExport();})
                .setNegativeButton(t("Cancel","Отмена"),null).show());

        addView(title(t("Bitmap DB (row-major bytes)", "Байты bitmap DB (по строкам)"),15));
        bitmapPreview = codePreview();
        addView(bitmapPreview);
        Button insertBitmap = greenButton(t("Insert bitmap DB at cursor","Вставить DB пикселей в код"));
        addView(insertBitmap, new LayoutParams(LayoutParams.MATCH_PARENT,dp(48)));
        insertBitmap.setOnClickListener(v->insertAction.insert(sprite.dbPixels(exportBinary)));

        addView(title(t("Touched tiles / attribute DB", "Атрибуты знакомест DB"),15));
        attrPreview = codePreview();
        addView(attrPreview);
        Button insertAttrs = greenButton(t("Insert attributes DB at cursor","Вставить DB атрибутов в код"));
        addView(insertAttrs, new LayoutParams(LayoutParams.MATCH_PARENT,dp(48)));
        insertAttrs.setOnClickListener(v->insertAction.insert(sprite.dbAttributes(exportBinary)));

        addView(detail(t("HEX uses $xx; BIN uses %xxxxxxxx. Sprite bytes are linear rows, "
                        + "not Spectrum's interleaved display-address order. "
                        + "Attributes are exported row by row.",
                "HEX использует $xx, BIN — %xxxxxxxx. Биты спрайта идут по строкам, "
                        + "а не в чередующемся порядке видеопамяти Spectrum. "
                        + "Атрибуты — по строкам знакомест.")));

        canvas.setOnChange(this::refreshExport);
        widthChooser.setSelection(1);
        heightChooser.setSelection(1);
        bindSize(widthChooser);
        bindSize(heightChooser);
        refreshBrush();
        refreshExport();
    }

    private void bindSize(Spinner chooser) {
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (sizing) return;
                int w=SIZES[widthChooser.getSelectedItemPosition()];
                int h=SIZES[heightChooser.getSelectedItemPosition()];
                if(w==sprite.width() && h==sprite.height()) return;
                new AlertDialog.Builder(getContext())
                        .setTitle(t("Resize sprite?","Изменить размер спрайта?"))
                        .setMessage(t("Changing size clears the current drawing.",
                                "Изменение размера очистит рисунок."))
                        .setPositiveButton(t("Resize","Изменить"),(d,i)->{
                            sprite=new SpriteData(w,h);canvas.setModel(sprite);refreshExport();
                        })
                        .setNegativeButton(t("Cancel","Отмена"),(d,i)->{
                            sizing=true;
                            widthChooser.setSelection(index(sprite.width()));
                            heightChooser.setSelection(index(sprite.height()));
                            sizing=false;
                        }).show();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int index(int size) {for(int i=0;i<SIZES.length;i++)if(SIZES[i]==size)return i;return 1;}

    private void refreshBrush() {
        canvas.setBrush(brushMode,color,panMode);
        int index=panMode?3:brushMode==SpriteData.Mode.INK?0:
                brushMode==SpriteData.Mode.PAPER?1:2;
        for(int i=0;i<4;i++){
            if(i==index) UiStyle.styleGreenButton(modeButtons[i]);
            else UiStyle.styleSoftGreenButton(modeButtons[i]);
        }
        for(int i=0;i<16;i++){
            GradientDrawable d=new GradientDrawable();
            d.setColor(SpriteData.palette(i));
            d.setCornerRadius(dp(7));
            d.setStroke(dp(i==color?3:1),i==color?Color.rgb(255,173,20):0xFFB4C9BA);
            paletteButtons[i].setBackground(d);
        }
        modeLabel.setText((ru?"Выбран цвет ":"Color ")+color
                +(panMode?t(" · move grid"," · перемещение"):brushMode==SpriteData.Mode.ERASE
                ?t(" · eraser"," · ластик"):brushMode==SpriteData.Mode.PAPER
                ? " · PAPER":" · INK"));
    }

    private void refreshExport() {
        bitmapPreview.setText(sprite.dbPixels(exportBinary));
        attrPreview.setText(sprite.dbAttributes(exportBinary));
    }

    private LinearLayout row() {
        LinearLayout l=new LinearLayout(getContext());l.setOrientation(HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);return l;
    }
    private Spinner spinner() {
        Spinner s=new Spinner(getContext());
        ArrayAdapter<Integer> adapter=new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,SIZES);
        s.setAdapter(adapter);return s;
    }
    private LinearLayout labeledSpinner(String label, Spinner spinner) {
        LinearLayout box=new LinearLayout(getContext());box.setOrientation(VERTICAL);
        box.addView(detail(label));
        box.addView(spinner);
        box.setPadding(dp(3),0,dp(3),0);
        box.setLayoutParams(new LayoutParams(0,LayoutParams.WRAP_CONTENT,1f));
        return box;
    }
    private Button greenButton(String title) {
        Button b=new Button(getContext());b.setText(title);UiStyle.styleGreenButton(b);return b;
    }
    private Button smallButton(String title) {
        Button b=new Button(getContext());b.setText(title);
        b.setTextSize(12);b.setMinWidth(0);b.setPadding(0,0,0,0);
        UiStyle.styleSoftGreenButton(b);return b;
    }
    private TextView title(String text,int sp) {
        TextView t=new TextView(getContext());t.setText(text);t.setTextSize(sp);
        t.setTextColor(0xFF2D6344);t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(2),dp(10),dp(2),dp(5));return t;
    }
    private TextView detail(String text){
        TextView v=new TextView(getContext());v.setText(text);v.setTextSize(12);
        v.setTextColor(0xFF68796D);v.setPadding(dp(2),dp(4),dp(2),dp(6));return v;
    }
    private TextView codePreview() {
        TextView t=new TextView(getContext());
        t.setTypeface(Typeface.MONOSPACE);t.setTextSize(12);
        t.setTextColor(0xFF243D2D);t.setTextIsSelectable(true);
        t.setPadding(dp(11),dp(10),dp(11),dp(10));
        t.setBackground(UiStyle.rounded(getContext(),Color.WHITE,14,0xFFC7DDCD,1));
        LayoutParams p=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        p.setMargins(0,dp(5),0,dp(8));t.setLayoutParams(p);
        return t;
    }
    private String t(String en,String ruText){return ru?ruText:en;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
