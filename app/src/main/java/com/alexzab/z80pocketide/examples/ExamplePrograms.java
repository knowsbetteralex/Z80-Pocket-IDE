package com.alexzab.z80pocketide.examples;

import com.alexzab.z80pocketide.i18n.AppLanguage;

/** Built-in, self-contained ZX Spectrum examples used by the gallery and tests. */
public final class ExamplePrograms {
    private ExamplePrograms() {}

    public static final int PREVIEW_BORDER = 1;
    public static final int PREVIEW_RAINBOW = 2;
    public static final int PREVIEW_STRIPES = 3;
    public static final int PREVIEW_CHECKER = 4;
    public static final int PREVIEW_SMILEY = 5;
    public static final int PREVIEW_TEXT = 6;
    public static final int PREVIEW_WIPE = 7;
    public static final int PREVIEW_KEY = 8;

    public static final Example[] ALL = new Example[] {
            ex("border-cycle", "Colour & attributes", "Цвет и атрибуты",
                    "Border cycle", "Перелив бордюра",
                    "Cycles the Spectrum border through all 8 colours.",
                    "Перебирает все 8 цветов бордюра Spectrum.",
                    "The value sent to port $FE contains the border colour in bits 0..2. A small DJNZ delay makes the changes visible and AND 7 wraps the value back to 0.",
                    "Цвет бордюра находится в битах 0..2 значения, выводимого в порт $FE. Небольшая задержка на DJNZ делает смену цветов заметной, а AND 7 возвращает счётчик в диапазон 0..7.",
                    PREVIEW_BORDER,
                    "ORG $8000\n\nSTART:\n    LD A,0\nLOOP:\n    OUT ($FE),A\n    INC A\n    AND 7\n    LD B,32\nDELAY:\n    DJNZ DELAY\n    JR LOOP\n",
                    "; Cycle all 8 border colours\nORG $8000\n\nSTART:\n    LD A,0          ; first border colour\nLOOP:\n    OUT ($FE),A     ; bits 0..2 select border colour\n    INC A\n    AND 7           ; wrap 8 back to 0\n    LD B,32         ; visible delay\nDELAY:\n    DJNZ DELAY\n    JR LOOP\n",
                    "; Перебор всех 8 цветов бордюра\nORG $8000\n\nSTART:\n    LD A,0          ; начинаем с цвета 0\nLOOP:\n    OUT ($FE),A     ; биты 0..2 задают цвет бордюра\n    INC A\n    AND 7           ; после 7 возвращаемся к 0\n    LD B,32         ; небольшая видимая задержка\nDELAY:\n    DJNZ DELAY\n    JR LOOP\n"),

            ex("rainbow-paper", "Colour & attributes", "Цвет и атрибуты",
                    "Rainbow paper", "Радужные атрибуты",
                    "Fills the 32x24 attribute area with coloured vertical bands.",
                    "Заполняет поле атрибутов 32x24 вертикальными цветными полосами.",
                    "The program writes directly to attribute RAM at $5800. The low three bits of L select a repeating column colour; three 256-byte pages cover all 768 attribute cells.",
                    "Программа пишет прямо в память атрибутов с $5800. Младшие три бита L дают повторяющийся цвет столбца; три страницы по 256 байт покрывают все 768 знакомест.",
                    PREVIEW_RAINBOW,
                    "ORG $8000\n\n    LD HL,$5800\n    LD D,3\nPAGE:\n    LD B,0\nCELL:\n    LD A,L\n    AND 7\n    ADD A,A\n    ADD A,A\n    ADD A,A\n    OR 7\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Attribute memory starts at $5800\nORG $8000\n\n    LD HL,$5800     ; first attribute cell\n    LD D,3          ; 3 x 256 = 768 cells\nPAGE:\n    LD B,0          ; DJNZ makes 256 iterations from zero\nCELL:\n    LD A,L\n    AND 7           ; repeating column number 0..7\n    ADD A,A\n    ADD A,A\n    ADD A,A         ; move colour into PAPER bits\n    OR 7            ; white INK\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Память атрибутов начинается с $5800\nORG $8000\n\n    LD HL,$5800     ; первое знакоместо\n    LD D,3          ; 3 x 256 = 768 атрибутов\nPAGE:\n    LD B,0          ; DJNZ от нуля даёт 256 проходов\nCELL:\n    LD A,L\n    AND 7           ; номер цвета 0..7\n    ADD A,A\n    ADD A,A\n    ADD A,A         ; переносим цвет в биты PAPER\n    OR 7            ; белый INK\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("checkerboard", "Colour & attributes", "Цвет и атрибуты",
                    "Attribute checkerboard", "Шахматка атрибутов",
                    "Creates a two-colour checkerboard using only attribute RAM.",
                    "Создаёт двухцветную шахматку, работая только с памятью атрибутов.",
                    "Each cell alternates between two PAPER/INK combinations by XORing $38. It is a compact example of attribute memory, XOR and nested page-sized loops.",
                    "Каждое знакоместо переключается между двумя сочетаниями PAPER/INK командой XOR $38. Это компактный пример работы с атрибутами, XOR и циклами по страницам.",
                    PREVIEW_CHECKER,
                    "ORG $8000\n\n    LD HL,$5800\n    LD D,3\n    LD A,$17\nPAGE:\n    LD B,0\nCELL:\n    LD (HL),A\n    XOR $38\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Alternate two attribute values\nORG $8000\n\n    LD HL,$5800     ; attribute RAM\n    LD D,3          ; all 768 cells\n    LD A,$17        ; one INK/PAPER combination\nPAGE:\n    LD B,0\nCELL:\n    LD (HL),A\n    XOR $38         ; flip PAPER colour bits\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Чередуем два значения атрибута\nORG $8000\n\n    LD HL,$5800     ; память атрибутов\n    LD D,3          ; все 768 знакомест\n    LD A,$17        ; первая комбинация INK/PAPER\nPAGE:\n    LD B,0\nCELL:\n    LD (HL),A\n    XOR $38         ; переключаем цвет PAPER\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("pixel-stripes", "Graphics", "Графика",
                    "Pixel stripes", "Пиксельные полосы",
                    "Fills the 6144-byte bitmap with an AA/55 stripe pattern.",
                    "Заполняет 6144 байта bitmap-памяти чередующимся узором AA/55.",
                    "Bitmap RAM occupies $4000..$57FF. Alternating $AA and $55 produces a dense one-pixel pattern while the 24 page loop demonstrates a fast 6144-byte fill.",
                    "Bitmap-память занимает $4000..$57FF. Чередование $AA и $55 создаёт частый однопиксельный узор, а цикл по 24 страницам показывает простую заливку 6144 байт.",
                    PREVIEW_STRIPES,
                    "ORG $8000\n\n    LD HL,$4000\n    LD D,24\n    LD A,$AA\nPAGE:\n    LD B,0\nPIXEL:\n    LD (HL),A\n    XOR $FF\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Fill the entire Spectrum bitmap\nORG $8000\n\n    LD HL,$4000     ; first bitmap byte\n    LD D,24         ; 24 x 256 = 6144 bytes\n    LD A,$AA        ; 10101010 pattern\nPAGE:\n    LD B,0\nPIXEL:\n    LD (HL),A\n    XOR $FF         ; alternate AA <-> 55\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Заполняем всю bitmap-память Spectrum\nORG $8000\n\n    LD HL,$4000     ; первый байт экрана\n    LD D,24         ; 24 x 256 = 6144 байта\n    LD A,$AA        ; узор 10101010\nPAGE:\n    LD B,0\nPIXEL:\n    LD (HL),A\n    XOR $FF         ; чередуем AA <-> 55\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("smiley-sprite", "Graphics", "Графика",
                    "8x8 smiley sprite", "Смайлик 8x8",
                    "Copies an 8-byte sprite to screen memory with LDIR.",
                    "Копирует восьмибайтовый спрайт в экранную память командой LDIR.",
                    "The sprite is stored as eight binary bytes. LDIR copies it to $4000 and an attribute byte at $5800 gives the cell a bright colour. This introduces sprite data and block copying.",
                    "Спрайт хранится как восемь двоичных байтов. LDIR копирует его в $4000, а байт атрибута в $5800 задаёт яркий цвет. Пример знакомит с данными спрайта и блочным копированием.",
                    PREVIEW_SMILEY,
                    "ORG $8000\n\n    LD HL,SPRITE\n    LD DE,$4000\n    LD BC,8\n    LDIR\n    LD A,$46\n    LD ($5800),A\nHOLD:\n    JR HOLD\n\nSPRITE:\n    DB %00111100\n    DB %01000010\n    DB %10100101\n    DB %10000001\n    DB %10100101\n    DB %10011001\n    DB %01000010\n    DB %00111100\n",
                    "; Copy an 8x8 sprite into the top-left screen cell\nORG $8000\n\n    LD HL,SPRITE    ; source bytes\n    LD DE,$4000     ; top-left bitmap address\n    LD BC,8         ; one byte per pixel row\n    LDIR            ; block copy BC bytes\n    LD A,$46        ; bright yellow/blue-ish attribute\n    LD ($5800),A    ; attribute for the same cell\nHOLD:\n    JR HOLD\n\nSPRITE:\n    DB %00111100    ; ..####..\n    DB %01000010    ; .#....#.\n    DB %10100101    ; #.#..#.#\n    DB %10000001    ; #......#\n    DB %10100101    ; #.#..#.#\n    DB %10011001    ; #..##..#\n    DB %01000010    ; .#....#.\n    DB %00111100    ; ..####..\n",
                    "; Копируем спрайт 8x8 в левое верхнее знакоместо\nORG $8000\n\n    LD HL,SPRITE    ; адрес данных спрайта\n    LD DE,$4000     ; начало bitmap-памяти\n    LD BC,8         ; по байту на строку спрайта\n    LDIR            ; блочное копирование BC байт\n    LD A,$46        ; яркий цвет атрибута\n    LD ($5800),A    ; атрибут того же знакоместа\nHOLD:\n    JR HOLD\n\nSPRITE:\n    DB %00111100    ; ..####..\n    DB %01000010    ; .#....#.\n    DB %10100101    ; #.#..#.#\n    DB %10000001    ; #......#\n    DB %10100101    ; #.#..#.#\n    DB %10011001    ; #..##..#\n    DB %01000010    ; .#....#.\n    DB %00111100    ; ..####..\n"),

            ex("screen-wipe", "Graphics", "Графика",
                    "Screen wipe", "Заливка экрана",
                    "Fills bitmap RAM from start to end, producing a simple wipe.",
                    "Последовательно заполняет bitmap-память, создавая простую экранную заливку.",
                    "HL walks from $4000 through 24 pages of bitmap memory. Writing $FF sets all eight pixels represented by each byte. The example demonstrates screen layout and nested loops.",
                    "HL проходит от $4000 через 24 страницы bitmap-памяти. Запись $FF включает все восемь пикселей соответствующего байта. Пример показывает размер экрана и вложенные циклы.",
                    PREVIEW_WIPE,
                    "ORG $8000\n\n    LD HL,$4000\n    LD D,24\n    LD A,$FF\nPAGE:\n    LD B,0\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Fill all 6144 bitmap bytes with $FF\nORG $8000\n\n    LD HL,$4000     ; bitmap start\n    LD D,24         ; 24 pages\n    LD A,$FF        ; eight set pixels per byte\nPAGE:\n    LD B,0          ; 256 bytes in this page\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Заполняем все 6144 байта экрана значением $FF\nORG $8000\n\n    LD HL,$4000     ; начало bitmap-памяти\n    LD D,24         ; 24 страницы\n    LD A,$FF        ; восемь включённых пикселей в байте\nPAGE:\n    LD B,0          ; 256 байт текущей страницы\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("rom-message", "ROM & text", "ПЗУ и текст",
                    "ROM text output", "Текст через ПЗУ",
                    "Prints HELLO Z80! with the Spectrum ROM character routine.",
                    "Печатает HELLO Z80! через символьную процедуру ПЗУ Spectrum.",
                    "RST $10 invokes the standard ROM character output routine. HL points to a zero-free message and B counts its ten characters, showing how machine code can reuse Spectrum ROM services.",
                    "RST $10 вызывает стандартную процедуру вывода символа из ПЗУ. HL указывает на строку, а B отсчитывает её десять символов — пример показывает, как машинный код может использовать сервисы Spectrum ROM.",
                    PREVIEW_TEXT,
                    "ORG $8000\n\n    LD HL,MESSAGE\n    LD B,10\nNEXT:\n    LD A,(HL)\n    RST $10\n    INC HL\n    DJNZ NEXT\nHOLD:\n    JR HOLD\n\nMESSAGE:\n    DB \"HELLO Z80!\"\n",
                    "; Print a message through the Spectrum ROM\nORG $8000\n\n    LD HL,MESSAGE   ; source string\n    LD B,10         ; number of characters\nNEXT:\n    LD A,(HL)       ; character code in A\n    RST $10         ; ROM: print character\n    INC HL\n    DJNZ NEXT\nHOLD:\n    JR HOLD\n\nMESSAGE:\n    DB \"HELLO Z80!\"\n",
                    "; Печать строки через ПЗУ Spectrum\nORG $8000\n\n    LD HL,MESSAGE   ; адрес строки\n    LD B,10         ; количество символов\nNEXT:\n    LD A,(HL)       ; код символа в A\n    RST $10         ; ПЗУ: вывести символ\n    INC HL\n    DJNZ NEXT\nHOLD:\n    JR HOLD\n\nMESSAGE:\n    DB \"HELLO Z80!\"\n"),

            ex("key-border", "Input", "Ввод",
                    "Keyboard to border", "Клавиша меняет бордюр",
                    "Reads keyboard port $FE and changes the border while a key is held.",
                    "Читает порт клавиатуры $FE и меняет бордюр, пока удерживается клавиша.",
                    "IN A,($FE) reads the keyboard matrix lines. This compact demo watches bit 0 and selects one of two border colours, illustrating port input, masking and conditional branches.",
                    "IN A,($FE) читает линии клавиатурной матрицы. Пример проверяет бит 0 и выбирает один из двух цветов бордюра, показывая ввод из порта, маску и условный переход.",
                    PREVIEW_KEY,
                    "ORG $8000\n\nLOOP:\n    IN A,($FE)\n    AND 1\n    JR Z,PRESSED\n    LD A,1\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2\n    OUT ($FE),A\n    JR LOOP\n",
                    "; Read the keyboard and reflect a key state in the border\nORG $8000\n\nLOOP:\n    IN A,($FE)      ; keyboard matrix input\n    AND 1           ; inspect one active-low key bit\n    JR Z,PRESSED\n    LD A,1          ; blue border when released\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2          ; red border when pressed\n    OUT ($FE),A\n    JR LOOP\n",
                    "; Читаем клавиатуру и показываем состояние цветом бордюра\nORG $8000\n\nLOOP:\n    IN A,($FE)      ; чтение клавиатурной матрицы\n    AND 1           ; проверяем один активный нулём бит\n    JR Z,PRESSED\n    LD A,1          ; синий бордюр — клавиша отпущена\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2          ; красный бордюр — клавиша нажата\n    OUT ($FE),A\n    JR LOOP\n")
    };

    private static Example ex(String id, String categoryEn, String categoryRu,
                              String titleEn, String titleRu,
                              String descriptionEn, String descriptionRu,
                              String detailsEn, String detailsRu,
                              int previewType, String source,
                              String commentedEn, String commentedRu) {
        return new Example(id, categoryEn, categoryRu, titleEn, titleRu,
                descriptionEn, descriptionRu, detailsEn, detailsRu,
                previewType, source, commentedEn, commentedRu);
    }

    public static Example findById(String id) {
        if (id != null) {
            for (Example e : ALL) if (e.id.equals(id)) return e;
        }
        return ALL[0];
    }

    public static final class Example {
        public final String id;
        public final String categoryEn;
        public final String categoryRu;
        public final String title;
        public final String titleRu;
        public final String description;
        public final String descriptionRu;
        public final String detailsEn;
        public final String detailsRu;
        public final int previewType;
        public final String source;
        public final String commentedEn;
        public final String commentedRu;

        Example(String id, String categoryEn, String categoryRu,
                String title, String titleRu, String description, String descriptionRu,
                String detailsEn, String detailsRu, int previewType, String source,
                String commentedEn, String commentedRu) {
            this.id = id;
            this.categoryEn = categoryEn;
            this.categoryRu = categoryRu;
            this.title = title;
            this.titleRu = titleRu;
            this.description = description;
            this.descriptionRu = descriptionRu;
            this.detailsEn = detailsEn;
            this.detailsRu = detailsRu;
            this.previewType = previewType;
            this.source = source;
            this.commentedEn = commentedEn;
            this.commentedRu = commentedRu;
        }

        public String category(AppLanguage language) {
            return language == AppLanguage.RU ? categoryRu : categoryEn;
        }

        public String title(AppLanguage language) {
            return language == AppLanguage.RU ? titleRu : title;
        }

        public String description(AppLanguage language) {
            return language == AppLanguage.RU ? descriptionRu : description;
        }

        public String details(AppLanguage language) {
            return language == AppLanguage.RU ? detailsRu : detailsEn;
        }

        public String source(AppLanguage language, boolean comments) {
            if (!comments) return source;
            return language == AppLanguage.RU ? commentedRu : commentedEn;
        }
    }
}
