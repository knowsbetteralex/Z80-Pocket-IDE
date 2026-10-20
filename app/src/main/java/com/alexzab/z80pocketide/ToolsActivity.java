package com.alexzab.z80pocketide;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alexzab.z80pocketide.i18n.AppLanguage;
import com.alexzab.z80pocketide.i18n.LanguageSettings;
import com.alexzab.z80pocketide.i18n.Texts;
import com.alexzab.z80pocketide.tools.NumberBaseConverter;
import com.alexzab.z80pocketide.tools.ScreenAddressPanel;
import com.alexzab.z80pocketide.tools.SpriteToolPanel;
import com.alexzab.z80pocketide.ui.SpectrumStripeView;
import com.alexzab.z80pocketide.ui.UiStyle;

/** One mobile workshop with number, screen address and sprite tabs. */
public final class ToolsActivity extends Activity {
    public static final String EXTRA_INSERT_SOURCE = "insert_source";
    public static final String EXTRA_SELECTED_TAB = "selected_tab";
    private static final String STATE_TAB = "tools_tab";
    private AppLanguage language;
    private boolean ru;
    private LinearLayout page;
    private StableToolsScrollView scroller;
    private final int[] savedScrollY = new int[3];
    private final Button[] tabs = new Button[3];
    private View[] panels = new View[3];
    private int activeTab;
    private EditText dec, hex, bin;
    private TextView numberStatus;
    private boolean updating;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        language = LanguageSettings.get(this);
        ru = language == AppLanguage.RU;
        getWindow().setStatusBarColor(0xFFF6FAF7);
        getWindow().setNavigationBarColor(0xFFF6FAF7);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6FAF7);
        int padding = dp(11);

        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v,insets)->{
                int top=insets.getInsets(WindowInsets.Type.systemBars()).top;
                int bottom=Math.max(insets.getInsets(WindowInsets.Type.systemBars()).bottom,
                        insets.getInsets(WindowInsets.Type.ime()).bottom);
                v.setPadding(padding,top+dp(8),padding,bottom+dp(8));
                return insets;
            });
        }else root.setPadding(padding,dp(8),padding,dp(8));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("←",false);
        header.addView(back,new LinearLayout.LayoutParams(dp(46),dp(45)));
        TextView title=new TextView(this);
        title.setText(t("Z80 workshop","Инструменты Z80"));
        title.setTextColor(0xFF245C3D);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(12),0,0,0);
        header.addView(title,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        root.addView(header);
        back.setOnClickListener(v->finish());

        SpectrumStripeView stripe=new SpectrumStripeView(this);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,dp(5));
        sp.setMargins(0,dp(7),0,dp(10));root.addView(stripe,sp);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] captions = ru ?
                new String[]{"123","Адреса","Спрайт"} :
                new String[]{"123","Addresses","Sprite"};
        for(int i=0;i<3;i++){
            final int index=i;
            tabs[i]=button(captions[i],false);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1f);
            p.setMargins(dp(2),0,dp(2),0);
            tabRow.addView(tabs[i],p);
            tabs[i].setOnClickListener(v->selectTab(index));
        }
        root.addView(tabRow);

        scroller=new StableToolsScrollView(this);
        scroller.setFillViewport(false);
        scroller.setClipToPadding(false);
        page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(page);
        LinearLayout.LayoutParams body=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,0,1f);
        body.setMargins(0,dp(9),0,0);
        root.addView(scroller,body);

        panels[0]=createNumberConverter();
        panels[1]=new ScreenAddressPanel(this,ru);
        panels[2]=new SpriteToolPanel(this,ru,source->{
            Intent result=new Intent();
            result.putExtra(EXTRA_INSERT_SOURCE,source);
            setResult(RESULT_OK,result);
            finish();
        },getIntent().getStringExtra("sprite_selected_source"));

        activeTab=state!=null?state.getInt(STATE_TAB,0):
                getIntent().getIntExtra(EXTRA_SELECTED_TAB,0);
        selectTab(Math.max(0,Math.min(activeTab,2)));
        setContentView(root);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt(STATE_TAB,activeTab);
        super.onSaveInstanceState(out);
    }

    private void selectTab(int index){
        if (scroller != null) savedScrollY[activeTab] = scroller.getScrollY();
        activeTab=index;
        page.removeAllViews();
        View content=panels[index];
        if(content.getParent() instanceof android.view.ViewGroup)
            ((android.view.ViewGroup)content.getParent()).removeView(content);
        page.addView(content);
        // Avoid Android auto-scrolling to selectable DB previews or action buttons.
        scroller.setSpriteFocusScrollDisabled(index == 2);
        final int restoreY = savedScrollY[index];
        scroller.post(() -> scroller.scrollTo(0, restoreY));
        for(int i=0;i<tabs.length;i++){
            if(i==index)UiStyle.styleGreenButton(tabs[i]);
            else UiStyle.styleSoftGreenButton(tabs[i]);
        }
    }

    /**
     * TextView.setText() on a selectable DB preview can request focus and
     * force ScrollView to jump to that preview after every brush stroke.
     * Ignore programmatic focus-driven scrolling on the sprite tab, while
     * preserving direct touch scrolling and pinch gestures.
     */
    private static final class StableToolsScrollView extends ScrollView {
        private boolean spriteFocusScrollDisabled;
        StableToolsScrollView(android.content.Context context) { super(context); }

        void setSpriteFocusScrollDisabled(boolean disabled) {
            spriteFocusScrollDisabled = disabled;
        }

        @Override public void requestChildFocus(View child, View focused) {
            if (!spriteFocusScrollDisabled) super.requestChildFocus(child, focused);
        }

        @Override public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
                                                               boolean immediate) {
            if (spriteFocusScrollDisabled) return false;
            return super.requestChildRectangleOnScreen(child, rectangle, immediate);
        }
    }

    private LinearLayout createNumberConverter(){
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(2),dp(8),dp(2),dp(18));
        TextView info=label(t("DEC, HEX and BIN update instantly",
                "DEC, HEX и BIN пересчитываются при вводе"),14,0xFF426A50);
        body.addView(info);

        dec = numberInput(body,"DEC",t("Decimal","Десятичная"),"12345");
        hex = numberInput(body,"HEX",t("Hexadecimal","Шестнадцатеричная"),"$3039");
        bin = numberInput(body,"BIN",t("Binary","Двоичная"),"%11000000111001");

        numberStatus=label(t("Type into any field","Вводите число в любое поле"),
                12,0xFF668471);
        numberStatus.setPadding(dp(5),dp(4),dp(5),dp(12));
        body.addView(numberStatus);
        Button clear=button(t("Clear","Очистить"),true);
        body.addView(clear,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,dp(46)));

        track(dec,NumberBaseConverter.Base.DEC);
        track(hex,NumberBaseConverter.Base.HEX);
        track(bin,NumberBaseConverter.Base.BIN);
        clear.setOnClickListener(v->{
            updating=true;dec.setText("");hex.setText("");bin.setText("");
            updating=false;
            numberStatus.setText(t("Cleared","Очищено"));
        });
        return body;
    }

    private EditText numberInput(LinearLayout parent,String code,String description,String hint){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(11),dp(14),dp(12));
        card.setBackground(UiStyle.rounded(this,Color.WHITE,17,0xFFD0E3D4,1));
        card.addView(label(code+" · "+description,14,0xFF2D8556));
        EditText input=new EditText(this);
        input.setSingleLine(true);input.setTypeface(Typeface.MONOSPACE);
        input.setTextColor(0xFF2D3A30);input.setTextSize(18);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        card.addView(input);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0,0,0,dp(10));
        parent.addView(card,p);
        return input;
    }

    private void track(EditText source,NumberBaseConverter.Base base){
        source.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
            @Override public void afterTextChanged(Editable s){
                if(updating)return;
                updating=true;
                if(s.toString().trim().isEmpty()){
                    if(source!=dec)dec.setText("");
                    if(source!=hex)hex.setText("");
                    if(source!=bin)bin.setText("");
                    numberStatus.setText(t("Type into any field","Вводите число в любое поле"));
                    numberStatus.setTextColor(0xFF668471);
                }else{
                    try{
                        NumberBaseConverter.Values v=NumberBaseConverter.convert(s.toString(),base);
                        if(source!=dec)dec.setText(v.decimal);
                        if(source!=hex)hex.setText(v.hexadecimal);
                        if(source!=bin)bin.setText(v.binary);
                        numberStatus.setText(t("Converted (up to 64 bits)","Пересчитано (до 64 бит)"));
                        numberStatus.setTextColor(0xFF2D8556);
                    }catch(IllegalArgumentException ex){
                        if(source!=dec)dec.setText("");
                        if(source!=hex)hex.setText("");
                        if(source!=bin)bin.setText("");
                        numberStatus.setText(t("Invalid number","Некорректное число"));
                        numberStatus.setTextColor(0xFFB73D4E);
                    }
                }
                updating=false;
            }
        });
    }

    private Button button(String title,boolean primary){
        Button b=new Button(this);
        b.setText(title);
        b.setMinWidth(0);b.setPadding(dp(5),0,dp(5),0);
        if(primary) UiStyle.styleGreenButton(b);else UiStyle.styleSoftGreenButton(b);
        return b;
    }

    private TextView label(String text,int size,int color){
        TextView v=new TextView(this);
        v.setText(text);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT_BOLD);return v;
    }
    private String t(String en,String russian){return ru?russian:en;}
    private int dp(int size){return Math.round(size*getResources().getDisplayMetrics().density);}
}
