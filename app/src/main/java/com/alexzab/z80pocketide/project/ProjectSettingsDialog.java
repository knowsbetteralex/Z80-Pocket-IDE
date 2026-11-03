package com.alexzab.z80pocketide.project;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Native Android settings dialog with ZX SCREEN$ preview; no AndroidX. */
public final class ProjectSettingsDialog {
    public interface ScreenLoader { byte[] load(String relativePath) throws Exception; }
    public interface ScreenEditAction { void edit(String relativePath); }
    public interface OnSave { boolean save(ProjectConfiguration config); }
    private ProjectSettingsDialog() {}

    public static void show(Activity activity,boolean ru,ProjectConfiguration initial,
                            List<String> roots,List<String> screens,
                            ScreenLoader loader,ScreenEditAction editAction,OnSave onSave) {
        int pad=dp(activity,14);
        ScrollView scroll=new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout body=new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(pad,pad,pad,pad);
        scroll.addView(body);

        heading(activity,body,ru?"Название проекта":"Project name");
        EditText name=new EditText(activity);
        name.setSingleLine(true);
        name.setText(initial.name);
        name.setSelectAllOnFocus(true);
        body.addView(name,new LinearLayout.LayoutParams(-1,dp(activity,48)));

        heading(activity,body,ru?"Основной ASM-файл":"Root ASM file");
        final String[] rootPath={initial.root};
        Button root=new Button(activity);
        root.setAllCaps(false);
        root.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        root.setText("  "+rootPath[0]+"  ▾");
        body.addView(root,new LinearLayout.LayoutParams(-1,dp(activity,48)));
        root.setOnClickListener(v -> {
            if(roots.isEmpty()){
                Toast.makeText(activity,ru?"В проекте нет ASM-файлов":"No ASM files in project",
                        Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle(ru?"Основной ASM-файл":"Root ASM file")
                    .setItems(roots.toArray(new String[0]),(dialog,which)->{
                        rootPath[0]=roots.get(which);
                        root.setText("  "+rootPath[0]+"  ▾");
                    })
                    .show();
        });

        heading(activity,body,ru?"Заставка при загрузке":"Loading screen");
        CheckBox enabled=new CheckBox(activity);
        enabled.setText(ru?"Показывать заставку при запуске":"Show screen while loading");
        enabled.setChecked(initial.screenEnabled);
        body.addView(enabled);

        final String[] screenPath={initial.screenPath};
        Button selectScreen=new Button(activity);
        selectScreen.setAllCaps(false);
        selectScreen.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        body.addView(selectScreen,new LinearLayout.LayoutParams(-1,dp(activity,48)));
        TextView status=new TextView(activity);
        status.setTextSize(12);
        status.setTextColor(0xFF66766F);
        LinearLayout.LayoutParams statusParams=new LinearLayout.LayoutParams(-1,-2);
        statusParams.setMargins(0,dp(activity,5),0,dp(activity,6));
        body.addView(status,statusParams);

        ImageView preview=new ImageView(activity);
        preview.setBackgroundColor(Color.BLACK);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        LinearLayout.LayoutParams previewParams=
                new LinearLayout.LayoutParams(-1,dp(activity,180));
        previewParams.setMargins(0,dp(activity,4),0,dp(activity,4));
        body.addView(preview,previewParams);

        Runnable updatePreview=() -> {
            String file=screenPath[0];
            selectScreen.setText("  "+(file==null||file.isEmpty()
                    ?(ru?"Выбрать .scr / .bin…":"Choose .scr / .bin…")
                    :file)+"  ▾");
            if(file==null||file.isEmpty()) {
                preview.setImageDrawable(null);
                status.setText(ru?"Файл не выбран":"No SCREEN$ selected");
                status.setTextColor(0xFF66766F);
                return;
            }
            try {
                byte[] bytes=loader.load(file);
                int[] pixels=SpectrumScreen.decodePixels(bytes);
                Bitmap image=Bitmap.createBitmap(pixels,
                        SpectrumScreen.WIDTH,SpectrumScreen.HEIGHT,Bitmap.Config.ARGB_8888);
                preview.setImageBitmap(image);
                status.setText(ru
                        ?"Экран 256×192 · 6144 байта графики + 768 атрибутов"
                        :"256×192 screen · 6144 bitmap + 768 attributes");
                status.setTextColor(0xFF29774A);
            }catch(Exception exception){
                preview.setImageDrawable(null);
                status.setText(exception.getMessage());
                status.setTextColor(0xFFB73737);
            }
        };
        selectScreen.setOnClickListener(v -> {
            if(screens.isEmpty()){
                Toast.makeText(activity,ru
                        ?"В проекте нет файлов .scr или .bin"
                        :"No .scr or .bin files in the project",
                        Toast.LENGTH_LONG).show();
                return;
            }
            List<String> choices=new ArrayList<>();
            choices.add(ru?"Без заставки":"No screen");
            choices.addAll(screens);
            new AlertDialog.Builder(activity)
                    .setTitle(ru?"Файл заставки":"Loading SCREEN$ file")
                    .setItems(choices.toArray(new String[0]),(dialog,which)->{
                        screenPath[0]=which==0?"":choices.get(which);
                        updatePreview.run();
                    }).show();
        });
        updatePreview.run();
        Button editScreen=new Button(activity);
        editScreen.setText(ru?"✎ Редактировать заставку":"✎ Edit loading screen");
        editScreen.setAllCaps(false);
        body.addView(editScreen,new LinearLayout.LayoutParams(-1,dp(activity,44)));
        editScreen.setOnClickListener(v->{
            // Open current selection without silently saving changed project settings.
            editAction.edit(screenPath[0]);
        });

        heading(activity,body,ru?"Когда запускать игру":"Start game after loading");
        final int[] intervals={0,50,100,150,250,-1};
        String[] labels=ru
                ?new String[]{"Сразу","Через 1 секунду","Через 2 секунды",
                        "Через 3 секунды","Через 5 секунд","По нажатию клавиши"}
                :new String[]{"Immediately","After 1 second","After 2 seconds",
                        "After 3 seconds","After 5 seconds","On key press"};
        List<String> delays=new ArrayList<>();
        List<Integer> values=new ArrayList<>();
        for(int i=0;i<intervals.length;i++){
            delays.add(labels[i]);
            values.add(intervals[i]);
        }
        if(!values.contains(initial.pauseFrames)){
            delays.add(initial.pauseFrames+" "+(ru?"кадров":"frames"));
            values.add(initial.pauseFrames);
        }
        Spinner delay=new Spinner(activity);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item,delays);
        delay.setAdapter(adapter);
        delay.setSelection(values.indexOf(initial.pauseFrames));
        body.addView(delay,new LinearLayout.LayoutParams(-1,dp(activity,48)));
        TextView note=new TextView(activity);
        note.setText(ru
                ?"Настройки сохраняются в .z80pocketide.json внутри проекта. При отсутствии права записи сохраняется локальная копия."
                :"Settings are stored in .z80pocketide.json in the project; a local backup is used if the folder is read-only.");
        note.setTextColor(0xFF708178);
        note.setTextSize(12);
        LinearLayout.LayoutParams noteParams=new LinearLayout.LayoutParams(-1,-2);
        noteParams.setMargins(0,dp(activity,12),0,0);
        body.addView(note,noteParams);

        AlertDialog dialog=new AlertDialog.Builder(activity)
                .setTitle(ru?"Настройки проекта":"Project settings")
                .setView(scroll)
                .setNegativeButton(ru?"Отмена":"Cancel",null)
                .setPositiveButton(ru?"Сохранить":"Save",null)
                .create();
        dialog.setOnShowListener(ignored->dialog.getButton(
                AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            ProjectConfiguration selected=new ProjectConfiguration(
                    name.getText().toString(),rootPath[0],enabled.isChecked(),
                    screenPath[0],values.get(delay.getSelectedItemPosition()));
            String validation=selected.validationError();
            if(validation!=null) {
                error(activity,validation);
                return;
            }
            if(selected.screenEnabled){
                try { loader.load(selected.screenPath); }
                catch(Exception exception) {
                    error(activity,exception.getMessage());return;
                }
            }
            if(onSave.save(selected))dialog.dismiss();
        }));
        dialog.show();
    }

    private static void error(Activity activity,String text){
        new AlertDialog.Builder(activity).setTitle("Project settings")
                .setMessage(text==null?"Invalid project configuration":text)
                .setPositiveButton("OK",null).show();
    }

    private static void heading(Activity activity,LinearLayout body,String title) {
        TextView caption=new TextView(activity);
        caption.setText(title);
        caption.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        caption.setTextColor(0xFF385443);
        caption.setTextSize(14);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(activity,12),0,dp(activity,5));
        body.addView(caption,lp);
    }

    private static int dp(Activity activity,int value){
        return (int)(value*activity.getResources().getDisplayMetrics().density+0.5f);
    }
}
