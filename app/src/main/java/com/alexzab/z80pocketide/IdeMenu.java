package com.alexzab.z80pocketide;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

/**
 * Single source of truth for the main menu layout and action IDs.
 * The Activity owns the actual actions; this class only describes navigation.
 */
public final class IdeMenu {
    public static final int NEW_TAB=1, OPEN_FILE=2, SAVE=3, SAVE_AS=4, CLOSE_TAB=5;
    public static final int EXAMPLES=10, REFERENCE=11, TOOLS=12, FORMAT=13;
    public static final int EDITOR_SETTINGS=15, EMULATOR_SETTINGS=16, UPPERCASE=17;
    public static final int BUILD=20, EXPORT_TAP=21, BUILD_RUN=22, FIND=23, EXPORT_SNA=24;
    public static final int OPEN_INCLUDE=25, OPEN_PROJECT=28, PROJECT_SETTINGS=29;
    public static final int SCREEN_EDITOR=31, MAIN_ASM=32, LANGUAGE=33, USER_GUIDE=34;
    public static final int CHECK_EXTERNAL=35;
    public static final int OPEN_GIT_PROJECT=36, SYNC_GIT=37, NEW_GIT_FILE=38;

    private IdeMenu() {}

    private static String t(boolean ru,String en,String russian) {
        return ru?russian:en;
    }

    private static MenuItem item(Menu menu,int id,int order,
                                 boolean ru,String en,String russian) {
        return menu.add(0,id,order,t(ru,en,russian));
    }

    public static void populate(Menu menu,boolean ru,boolean hasDocument,
                                String mainAsm,boolean isRussian) {
        SubMenu file=menu.addSubMenu(t(ru,"File","Файл"));
        item(file,NEW_TAB,0,ru,"New file","Новый файл");
        item(file,OPEN_FILE,1,ru,"Open file…","Открыть файл…");
        item(file,OPEN_INCLUDE,2,ru,"Open INCLUDE…","Открыть INCLUDE…")
                .setEnabled(hasDocument);
        item(file,SAVE,3,ru,"Save","Сохранить").setEnabled(hasDocument);
        item(file,SAVE_AS,4,ru,"Save as…","Сохранить как…").setEnabled(hasDocument);
        item(file,CLOSE_TAB,5,ru,"Close tab","Закрыть вкладку").setEnabled(hasDocument);
        item(file,CHECK_EXTERNAL,6,ru,"Check external file changes","Проверить внешние изменения")
                .setEnabled(hasDocument);

        SubMenu project=menu.addSubMenu(t(ru,"Project","Проект"));
        item(project,OPEN_PROJECT,0,ru,"Open project…","Открыть проект…");
        item(project,OPEN_GIT_PROJECT,1,ru,"Open Git project…","Открыть Git-проект…");
        item(project,SYNC_GIT,2,ru,"Git · Sync (Pull + Push)","Git · Синхронизировать (Pull + Push)");
        item(project,NEW_GIT_FILE,3,ru,"New file in Git project…","Новый файл в Git-проекте…");
        item(project,PROJECT_SETTINGS,1,ru,"Project settings…","Настройки проекта…");
        String root=mainAsm==null||mainAsm.isEmpty()?"main.asm":mainAsm;
        project.add(0,MAIN_ASM,2,t(ru,"Main ASM: ","Главный ASM: ")+root);

        SubMenu edit=menu.addSubMenu(t(ru,"Edit","Правка"));
        item(edit,FIND,0,ru,"Find / Replace…","Найти / Заменить…")
                .setEnabled(hasDocument);
        item(edit,FORMAT,1,ru,"Format source","Форматировать код")
                .setEnabled(hasDocument);
        item(edit,UPPERCASE,2,ru,"UPPERCASE source","Весь код заглавными")
                .setEnabled(hasDocument);

        SubMenu build=menu.addSubMenu(t(ru,"Build","Сборка"));
        item(build,BUILD,0,ru,"Build project","Собрать проект").setEnabled(hasDocument);
        item(build,BUILD_RUN,1,ru,"Build and run","Собрать и запустить")
                .setEnabled(hasDocument);
        item(build,EXPORT_TAP,2,ru,"Export TAP…","Экспорт TAP…")
                .setEnabled(hasDocument);
        item(build,EXPORT_SNA,3,ru,"Export 48K SNA…","Экспорт SNA 48K…")
                .setEnabled(hasDocument);

        SubMenu tools=menu.addSubMenu(t(ru,"Tools","Инструменты"));
        item(tools,SCREEN_EDITOR,0,ru,"Screen editor…","Редактор экранов…");
        item(tools,TOOLS,1,ru,"Number / Address / Sprite tools",
                "Калькулятор / Адреса / Спрайты");
        item(tools,EXAMPLES,2,ru,"Examples and routines","Примеры и подпрограммы");

        SubMenu settings=menu.addSubMenu(t(ru,"Settings","Настройки"));
        item(settings,EDITOR_SETTINGS,0,ru,"Editor settings…","Настройки редактора…");
        item(settings,EMULATOR_SETTINGS,1,ru,"TAP emulator…","Эмулятор TAP…");
        settings.add(0,LANGUAGE,2,
                t(ru,"Interface language: ","Язык интерфейса: ")+(isRussian?"RU":"EN"));

        item(menu,USER_GUIDE,6,ru,"User guide","Руководство");
        item(menu,REFERENCE,7,ru,"Z80 / Spectrum reference","Справочник Z80 / Spectrum");
    }
}
