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
    public static final int PREVIEW_FILL = 7;
    public static final int PREVIEW_KEY = 8;
    public static final int PREVIEW_FLASH = 9;
    public static final int PREVIEW_DIAGONAL = 10;
    public static final int PREVIEW_MARQUEE = 11;
    public static final int PREVIEW_BEEPER = 12;
    public static final int PREVIEW_THIRDS = 13;
    public static final int PREVIEW_NOISE = 14;
    public static final int PREVIEW_ROUTINE = 15;

    public static final Example[] ALL = new Example[] {
            ex("border-cycle", "Colour & attributes", "Цвет и атрибуты",
                    "Border cycle", "Перелив бордюра",
                    "Changes the border once per video frame through all 8 colours.",
                    "Меняет бордюр раз в кадр, перебирая все 8 цветов.",
                    "Bits 0..2 written to port $FE select the border colour. EI + HALT synchronises each change to the 50 Hz frame interrupt, so the colours are visibly distinct instead of changing thousands of times per second.",
                    "Биты 0..2 значения, записанного в порт $FE, задают цвет бордюра. EI + HALT синхронизируют смену с кадровым прерыванием 50 Гц, поэтому цвета видны отдельно, а не мелькают с килогерцовой частотой.",
                    PREVIEW_BORDER,
                    "ORG $8000\n\n    EI\n    LD A,0\nLOOP:\n    OUT ($FE),A\n    HALT\n    INC A\n    AND 7\n    JR LOOP\n",
                    "; One border colour per video frame\nORG $8000\n\n    EI              ; make sure frame interrupts are enabled\n    LD A,0\nLOOP:\n    OUT ($FE),A     ; bits 0..2 = border colour\n    HALT            ; wait for the next 50 Hz interrupt\n    INC A\n    AND 7           ; keep A in range 0..7\n    JR LOOP\n",
                    "; Один цвет бордюра на один видеокадр\nORG $8000\n\n    EI              ; разрешаем кадровые прерывания\n    LD A,0\nLOOP:\n    OUT ($FE),A     ; биты 0..2 задают цвет бордюра\n    HALT            ; ждём следующее прерывание 50 Гц\n    INC A\n    AND 7           ; оставляем диапазон 0..7\n    JR LOOP\n"),

            ex("rainbow-paper", "Colour & attributes", "Цвет и атрибуты",
                    "Rainbow paper", "Радужные атрибуты",
                    "Fills all 32x24 attribute cells with repeating vertical colour bands.",
                    "Заполняет все 32x24 знакоместа повторяющимися вертикальными цветными полосами.",
                    "Attribute RAM is linear at $5800..$5AFF. Because every row is 32 bytes wide, using the low three bits of L repeats the same eight PAPER colours in the same columns on every row.",
                    "Память атрибутов линейна и занимает $5800..$5AFF. Строка имеет 32 байта, поэтому младшие три бита L повторяют восемь цветов PAPER в одних и тех же столбцах каждой строки.",
                    PREVIEW_RAINBOW,
                    "ORG $8000\n\n    LD HL,$5800\n    LD D,3\nPAGE:\n    LD B,0\nCELL:\n    LD A,L\n    AND 7\n    ADD A,A\n    ADD A,A\n    ADD A,A\n    OR 7\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Fill all 768 Spectrum attribute cells\nORG $8000\n\n    LD HL,$5800     ; first attribute\n    LD D,3          ; 3 x 256 bytes = 768 cells\nPAGE:\n    LD B,0\nCELL:\n    LD A,L\n    AND 7           ; repeating column colour 0..7\n    ADD A,A\n    ADD A,A\n    ADD A,A         ; move colour into PAPER bits\n    OR 7            ; white INK\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Заполняем все 768 атрибутов Spectrum\nORG $8000\n\n    LD HL,$5800     ; первый атрибут\n    LD D,3          ; 3 x 256 байт = 768 знакомест\nPAGE:\n    LD B,0\nCELL:\n    LD A,L\n    AND 7           ; повторяющийся цвет 0..7\n    ADD A,A\n    ADD A,A\n    ADD A,A         ; переносим цвет в PAPER\n    OR 7            ; белый INK\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("checkerboard", "Colour & attributes", "Цвет и атрибуты",
                    "Attribute checkerboard", "Шахматка атрибутов",
                    "Creates a real two-colour 32x24 checkerboard in attribute RAM.",
                    "Создаёт настоящую двухцветную шахматку 32x24 в памяти атрибутов.",
                    "The value toggles for every cell and is toggled once more at the end of each 32-cell row. That extra XOR changes the starting phase of the next row, producing a checkerboard rather than vertical stripes.",
                    "Значение переключается в каждом знакоместе и ещё раз в конце строки из 32 ячеек. Дополнительный XOR меняет фазу следующей строки, поэтому получается шахматка, а не вертикальные полосы.",
                    PREVIEW_CHECKER,
                    "ORG $8000\n\n    LD HL,$5800\n    LD C,24\n    LD A,$17\nROW:\n    LD B,32\nCELL:\n    LD (HL),A\n    XOR $38\n    INC HL\n    DJNZ CELL\n    XOR $38\n    DEC C\n    JR NZ,ROW\nHOLD:\n    JR HOLD\n",
                    "; Two-colour attribute checkerboard\nORG $8000\n\n    LD HL,$5800     ; attribute RAM\n    LD C,24         ; 24 rows\n    LD A,$17        ; first colour pair\nROW:\n    LD B,32         ; 32 cells per row\nCELL:\n    LD (HL),A\n    XOR $38         ; alternate the PAPER bits\n    INC HL\n    DJNZ CELL\n    XOR $38         ; reverse phase for the next row\n    DEC C\n    JR NZ,ROW\nHOLD:\n    JR HOLD\n",
                    "; Двухцветная шахматка атрибутов\nORG $8000\n\n    LD HL,$5800     ; память атрибутов\n    LD C,24         ; 24 строки\n    LD A,$17        ; первая пара цветов\nROW:\n    LD B,32         ; 32 знакоместа в строке\nCELL:\n    LD (HL),A\n    XOR $38         ; чередуем PAPER\n    INC HL\n    DJNZ CELL\n    XOR $38         ; меняем фазу следующей строки\n    DEC C\n    JR NZ,ROW\nHOLD:\n    JR HOLD\n"),

            ex("pixel-stripes", "Graphics", "Графика",
                    "1-pixel vertical stripes", "Вертикальные полосы в 1 пиксель",
                    "Fills the 6144-byte bitmap with $AA, producing true one-pixel vertical stripes.",
                    "Заполняет 6144 байта bitmap значением $AA, создавая настоящие вертикальные полосы шириной в один пиксель.",
                    "$AA is binary 10101010. Repeating the same byte across every screen byte keeps the pixel phase at byte boundaries, so the result is a continuous one-pixel stripe pattern.",
                    "$AA — это 10101010. Один и тот же байт повторяется по всей bitmap-памяти, поэтому фаза не сбивается на границах байтов и полосы действительно имеют ширину один пиксель.",
                    PREVIEW_STRIPES,
                    "ORG $8000\n\n    LD HL,$4000\n    LD D,24\n    LD A,$AA\nPAGE:\n    LD B,0\nPIXEL:\n    LD (HL),A\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; $AA = 10101010: one-pixel vertical stripes\nORG $8000\n\n    LD HL,$4000     ; first bitmap byte\n    LD D,24         ; 24 x 256 = 6144 bytes\n    LD A,$AA\nPAGE:\n    LD B,0\nPIXEL:\n    LD (HL),A\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; $AA = 10101010: вертикальные полосы в один пиксель\nORG $8000\n\n    LD HL,$4000     ; первый байт bitmap\n    LD D,24         ; 24 x 256 = 6144 байта\n    LD A,$AA\nPAGE:\n    LD B,0\nPIXEL:\n    LD (HL),A\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("smiley-sprite", "Graphics", "Графика",
                    "8x8 smiley sprite", "Смайлик 8x8",
                    "Draws an 8x8 sprite correctly in the top-left Spectrum character cell.",
                    "Правильно рисует спрайт 8x8 в левом верхнем знакоместе Spectrum.",
                    "The eight scanlines of one 8x8 character are at $4000,$4100,...,$4700, not in eight consecutive bytes. D is incremented to move by $100 while HL advances through the sprite data.",
                    "Восемь строк одного знакоместа лежат по адресам $4000,$4100,...,$4700, а не в восьми соседних байтах. Увеличиваем D, чтобы переходить на $100, а HL идёт по данным спрайта.",
                    PREVIEW_SMILEY,
                    "ORG $8000\n\n    LD HL,SPRITE\n    LD DE,$4000\n    LD B,8\nROW:\n    LD A,(HL)\n    LD (DE),A\n    INC HL\n    INC D\n    DJNZ ROW\n    LD A,$46\n    LD ($5800),A\nHOLD:\n    JR HOLD\n\nSPRITE:\n    DB %00111100\n    DB %01000010\n    DB %10100101\n    DB %10000001\n    DB %10100101\n    DB %10011001\n    DB %01000010\n    DB %00111100\n",
                    "; Spectrum scanlines inside a character cell are $100 apart\nORG $8000\n\n    LD HL,SPRITE    ; source data\n    LD DE,$4000     ; first scanline of top-left cell\n    LD B,8\nROW:\n    LD A,(HL)\n    LD (DE),A\n    INC HL\n    INC D           ; DE += $100\n    DJNZ ROW\n    LD A,$46\n    LD ($5800),A    ; colour of the same cell\nHOLD:\n    JR HOLD\n\nSPRITE:\n    DB %00111100\n    DB %01000010\n    DB %10100101\n    DB %10000001\n    DB %10100101\n    DB %10011001\n    DB %01000010\n    DB %00111100\n",
                    "; Строки знакоместа в bitmap Spectrum разнесены на $100\nORG $8000\n\n    LD HL,SPRITE    ; данные спрайта\n    LD DE,$4000     ; первая строка левого верхнего знакоместа\n    LD B,8\nROW:\n    LD A,(HL)\n    LD (DE),A\n    INC HL\n    INC D           ; DE += $100\n    DJNZ ROW\n    LD A,$46\n    LD ($5800),A    ; цвет этого знакоместа\nHOLD:\n    JR HOLD\n\nSPRITE:\n    DB %00111100\n    DB %01000010\n    DB %10100101\n    DB %10000001\n    DB %10100101\n    DB %10011001\n    DB %01000010\n    DB %00111100\n"),

            ex("bitmap-fill", "Graphics", "Графика",
                    "Bitmap memory fill", "Заливка bitmap-памяти",
                    "Sets every pixel bit in the 6144-byte bitmap memory.",
                    "Устанавливает все пиксельные биты в 6144 байтах bitmap-памяти.",
                    "This deliberately walks memory linearly from $4000 to $57FF. The final bitmap is completely set; the order in which areas appear while it runs reflects the Spectrum's interleaved screen layout rather than a simple top-to-bottom wipe.",
                    "Программа намеренно идёт по памяти линейно от $4000 до $57FF. В итоге вся bitmap заполнена; порядок появления областей во время работы показывает чересстрочную организацию экрана Spectrum и не является простой заливкой сверху вниз.",
                    PREVIEW_FILL,
                    "ORG $8000\n\n    LD HL,$4000\n    LD D,24\n    LD A,$FF\nPAGE:\n    LD B,0\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Linear fill of bitmap RAM $4000..$57FF\nORG $8000\n\n    LD HL,$4000\n    LD D,24         ; 24 x 256 = 6144 bytes\n    LD A,$FF        ; all eight pixel bits set\nPAGE:\n    LD B,0\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Линейная заливка bitmap $4000..$57FF\nORG $8000\n\n    LD HL,$4000\n    LD D,24         ; 24 x 256 = 6144 байта\n    LD A,$FF        ; все восемь пикселей включены\nPAGE:\n    LD B,0\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("rom-message", "ROM & text", "ПЗУ и текст",
                    "ROM text output", "Текст через ПЗУ",
                    "Prints HELLO Z80! through the Spectrum ROM character routine.",
                    "Печатает HELLO Z80! через символьную процедуру ПЗУ Spectrum.",
                    "RST $10 prints the character in A. The example preserves BC and HL around the ROM call so its own loop counter and string pointer remain reliable even if the ROM routine uses those registers internally.",
                    "RST $10 печатает символ из A. Пример сохраняет BC и HL вокруг вызова ПЗУ, поэтому счётчик и указатель строки не зависят от того, какие регистры использует внутренняя процедура ROM.",
                    PREVIEW_TEXT,
                    "ORG $8000\n\n    LD HL,MESSAGE\n    LD B,10\nNEXT:\n    LD A,(HL)\n    PUSH BC\n    PUSH HL\n    RST $10\n    POP HL\n    POP BC\n    INC HL\n    DJNZ NEXT\nHOLD:\n    JR HOLD\n\nMESSAGE:\n    DB \"HELLO Z80!\"\n",
                    "; Print safely through ROM RST $10\nORG $8000\n\n    LD HL,MESSAGE\n    LD B,10\nNEXT:\n    LD A,(HL)\n    PUSH BC         ; preserve our loop counter\n    PUSH HL         ; preserve our string pointer\n    RST $10         ; print character in A\n    POP HL\n    POP BC\n    INC HL\n    DJNZ NEXT\nHOLD:\n    JR HOLD\n\nMESSAGE:\n    DB \"HELLO Z80!\"\n",
                    "; Безопасная печать через ROM RST $10\nORG $8000\n\n    LD HL,MESSAGE\n    LD B,10\nNEXT:\n    LD A,(HL)\n    PUSH BC         ; сохраняем счётчик\n    PUSH HL         ; сохраняем указатель строки\n    RST $10         ; печатаем символ из A\n    POP HL\n    POP BC\n    INC HL\n    DJNZ NEXT\nHOLD:\n    JR HOLD\n\nMESSAGE:\n    DB \"HELLO Z80!\"\n"),

            ex("space-border", "Input", "Ввод",
                    "SPACE changes border", "SPACE меняет бордюр",
                    "Reads the keyboard matrix row containing SPACE and changes the border while SPACE is held.",
                    "Читает строку клавиатурной матрицы с SPACE и меняет бордюр, пока SPACE удерживается.",
                    "BC=$7FFE selects the keyboard half-row containing SPACE. IN A,(C) reads it and bit 0 is active low: 0 means SPACE is pressed. This avoids the unstable row selection of IN A,($FE), whose high port byte comes from A.",
                    "BC=$7FFE выбирает полуряд клавиатуры со SPACE. IN A,(C) читает его, а бит 0 активен нулём: 0 означает нажатый SPACE. Это устраняет нестабильный выбор строки у IN A,($FE), где старший байт адреса порта берётся из A.",
                    PREVIEW_KEY,
                    "ORG $8000\n\n    LD BC,$7FFE\nLOOP:\n    IN A,(C)\n    AND 1\n    JR Z,PRESSED\n    LD A,1\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2\n    OUT ($FE),A\n    JR LOOP\n",
                    "; SPACE is bit 0 of keyboard port $7FFE\nORG $8000\n\n    LD BC,$7FFE     ; row: B N M SymbolShift SPACE\nLOOP:\n    IN A,(C)        ; read keyboard matrix\n    AND 1           ; bit 0 is SPACE, active low\n    JR Z,PRESSED\n    LD A,1          ; blue when released\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2          ; red while SPACE is held\n    OUT ($FE),A\n    JR LOOP\n",
                    "; SPACE — бит 0 порта клавиатуры $7FFE\nORG $8000\n\n    LD BC,$7FFE     ; ряд: B N M SymbolShift SPACE\nLOOP:\n    IN A,(C)        ; читаем матрицу\n    AND 1           ; бит 0 = SPACE, активный ноль\n    JR Z,PRESSED\n    LD A,1          ; синий, когда отпущен\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2          ; красный при удержании SPACE\n    OUT ($FE),A\n    JR LOOP\n"),

            ex("flash-cell", "Colour & attributes", "Цвет и атрибуты",
                    "FLASH attribute", "Атрибут FLASH",
                    "Draws a solid 8x8 block and lets the ULA flash its INK/PAPER automatically.",
                    "Рисует блок 8x8 и позволяет ULA автоматически менять местами его INK/PAPER по FLASH.",
                    "Bit 7 of an attribute enables FLASH. The pixel rows are written at $4000,$4100,...,$4700 and attribute $C7 enables FLASH + BRIGHT with white ink on black paper.",
                    "Бит 7 атрибута включает FLASH. Строки блока пишутся в $4000,$4100,...,$4700, а атрибут $C7 задаёт FLASH + BRIGHT, белый INK и чёрный PAPER.",
                    PREVIEW_FLASH,
                    "ORG $8000\n\n    LD DE,$4000\n    LD B,8\n    LD A,$FF\nROW:\n    LD (DE),A\n    INC D\n    DJNZ ROW\n    LD A,$C7\n    LD ($5800),A\nHOLD:\n    JR HOLD\n",
                    "; Hardware FLASH needs only attribute bit 7\nORG $8000\n\n    LD DE,$4000\n    LD B,8\n    LD A,$FF        ; solid eight-pixel row\nROW:\n    LD (DE),A\n    INC D           ; next scanline inside this character cell\n    DJNZ ROW\n    LD A,$C7        ; FLASH + BRIGHT + white INK\n    LD ($5800),A\nHOLD:\n    JR HOLD\n",
                    "; Для аппаратного FLASH достаточно бита 7 атрибута\nORG $8000\n\n    LD DE,$4000\n    LD B,8\n    LD A,$FF        ; сплошная строка из 8 пикселей\nROW:\n    LD (DE),A\n    INC D           ; следующая строка знакоместа\n    DJNZ ROW\n    LD A,$C7        ; FLASH + BRIGHT + белый INK\n    LD ($5800),A\nHOLD:\n    JR HOLD\n"),

            ex("diagonal-cell", "Graphics", "Графика",
                    "8x8 diagonal", "Диагональ 8x8",
                    "Draws a single-pixel diagonal across one character cell using RRC.",
                    "Рисует однопиксельную диагональ через одно знакоместо с помощью RRC.",
                    "A starts as %10000000. After each scanline RRC moves the set bit one position right, while INC D moves to the next Spectrum scanline address $100 bytes away.",
                    "A начинается с %10000000. После каждой строки RRC сдвигает единичный бит вправо, а INC D переходит к следующей строке экрана Spectrum на $100 байт дальше.",
                    PREVIEW_DIAGONAL,
                    "ORG $8000\n\n    LD DE,$4000\n    LD B,8\n    LD A,%10000000\nROW:\n    LD (DE),A\n    RRC A\n    INC D\n    DJNZ ROW\n    LD A,$47\n    LD ($5800),A\nHOLD:\n    JR HOLD\n",
                    "; Rotate one set bit across eight scanlines\nORG $8000\n\n    LD DE,$4000\n    LD B,8\n    LD A,%10000000\nROW:\n    LD (DE),A\n    RRC A           ; move the pixel one column right\n    INC D           ; next scanline in the character cell\n    DJNZ ROW\n    LD A,$47        ; BRIGHT white ink\n    LD ($5800),A\nHOLD:\n    JR HOLD\n",
                    "; Перемещаем единичный бит по восьми строкам\nORG $8000\n\n    LD DE,$4000\n    LD B,8\n    LD A,%10000000\nROW:\n    LD (DE),A\n    RRC A           ; пиксель на столбец вправо\n    INC D           ; следующая строка знакоместа\n    DJNZ ROW\n    LD A,$47        ; яркий белый INK\n    LD ($5800),A\nHOLD:\n    JR HOLD\n"),

            ex("attribute-marquee", "Animation", "Анимация",
                    "Moving colour bar", "Бегущая цветная полоса",
                    "Animates a rainbow across the top attribute row at one step per frame.",
                    "Анимирует радугу в верхней строке атрибутов с шагом один раз за кадр.",
                    "C is a phase counter. Each frame the program rebuilds the 32 attributes at $5800 using column+phase, then HALT waits for the next frame before the phase is advanced.",
                    "C хранит фазу. Каждый кадр программа заново строит 32 атрибута с $5800 из суммы столбца и фазы, затем HALT ждёт следующий кадр перед увеличением фазы.",
                    PREVIEW_MARQUEE,
                    "ORG $8000\n\n    EI\n    LD C,0\nFRAME:\n    LD HL,$5800\n    LD B,32\nCELL:\n    LD A,L\n    ADD A,C\n    AND 7\n    ADD A,A\n    ADD A,A\n    ADD A,A\n    OR 7\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    HALT\n    INC C\n    JR FRAME\n",
                    "; Rebuild the top attribute row once per frame\nORG $8000\n\n    EI\n    LD C,0          ; animation phase\nFRAME:\n    LD HL,$5800\n    LD B,32\nCELL:\n    LD A,L          ; current column in low address bits\n    ADD A,C         ; add animation phase\n    AND 7\n    ADD A,A\n    ADD A,A\n    ADD A,A         ; PAPER bits\n    OR 7            ; white INK\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    HALT            ; one update per 50 Hz frame\n    INC C\n    JR FRAME\n",
                    "; Перестраиваем верхнюю строку атрибутов раз за кадр\nORG $8000\n\n    EI\n    LD C,0          ; фаза анимации\nFRAME:\n    LD HL,$5800\n    LD B,32\nCELL:\n    LD A,L          ; номер столбца из младших битов адреса\n    ADD A,C         ; прибавляем фазу\n    AND 7\n    ADD A,A\n    ADD A,A\n    ADD A,A         ; биты PAPER\n    OR 7            ; белый INK\n    LD (HL),A\n    INC HL\n    DJNZ CELL\n    HALT            ; одно обновление на кадр 50 Гц\n    INC C\n    JR FRAME\n"),

            ex("beeper-tone", "Sound", "Звук",
                    "Simple beeper tone", "Простой звук пищалки",
                    "Generates a continuous tone by toggling the Spectrum speaker bit on port $FE.",
                    "Создаёт непрерывный тон, переключая бит динамика Spectrum в порту $FE.",
                    "Bit 4 of port $FE drives the 48K beeper. XOR $10 flips that bit and the DJNZ loop controls the half-period. This is intentionally a minimal software-generated square wave.",
                    "Бит 4 порта $FE управляет пищалкой 48K. XOR $10 переключает его, а цикл DJNZ задаёт полупериод. Это намеренно минимальный программный меандр.",
                    PREVIEW_BEEPER,
                    "ORG $8000\n\n    LD A,0\nTONE:\n    XOR $10\n    OUT ($FE),A\n    LD B,40\nDELAY:\n    DJNZ DELAY\n    JR TONE\n",
                    "; Toggle speaker bit 4 on port $FE\nORG $8000\n\n    LD A,0\nTONE:\n    XOR $10         ; flip beeper output bit\n    OUT ($FE),A\n    LD B,40         ; simple pitch delay\nDELAY:\n    DJNZ DELAY\n    JR TONE\n",
                    "; Переключаем бит 4 пищалки в порту $FE\nORG $8000\n\n    LD A,0\nTONE:\n    XOR $10         ; меняем состояние динамика\n    OUT ($FE),A\n    LD B,40         ; простая задержка, задающая высоту тона\nDELAY:\n    DJNZ DELAY\n    JR TONE\n"),

            ex("screen-thirds",
                    "Graphics",
                    "Графика",
                    "Three bitmap thirds",
                    "Три трети экрана",
                    "Fills the three 64-line Spectrum bitmap thirds with different patterns.",
                    "Заполняет три 64-строчные трети bitmap Spectrum разными узорами.",
                    "$4000..$47FF, $4800..$4FFF and $5000..$57FF correspond to the top, middle and bottom screen thirds. The example makes that unusual layout immediately visible.",
                    "$4000..$47FF, $4800..$4FFF и $5000..$57FF соответствуют верхней, средней и нижней трети экрана. Пример наглядно показывает необычную организацию видеопамяти.",
                    PREVIEW_THIRDS,
                    "ORG $8000\n\n    LD HL,$4000\n    LD A,$FF\n    LD D,8\n    CALL FILL\n    LD A,$AA\n    LD D,8\n    CALL FILL\n    LD A,$55\n    LD D,8\n    CALL FILL\nHOLD:\n    JR HOLD\n\nFILL:\n    LD B,0\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D\n    JR NZ,FILL\n    RET\n",
                    "; Show the three 2048-byte bitmap thirds\nORG $8000\n\n    LD HL,$4000\n    LD A,$FF\n    LD D,8\n    CALL FILL\n    LD A,$AA\n    LD D,8\n    CALL FILL\n    LD A,$55\n    LD D,8\n    CALL FILL\nHOLD:\n    JR HOLD\n\nFILL:\n    LD B,0          ; 256 bytes per page\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D           ; eight pages per screen third\n    JR NZ,FILL\n    RET\n",
                    "; Показываем три части bitmap по 2048 байт\nORG $8000\n\n    LD HL,$4000\n    LD A,$FF\n    LD D,8\n    CALL FILL\n    LD A,$AA\n    LD D,8\n    CALL FILL\n    LD A,$55\n    LD D,8\n    CALL FILL\nHOLD:\n    JR HOLD\n\nFILL:\n    LD B,0          ; 256 байт на страницу\nBYTE:\n    LD (HL),A\n    INC HL\n    DJNZ BYTE\n    DEC D           ; восемь страниц на треть экрана\n    JR NZ,FILL\n    RET\n"),

            ex("r-register-noise",
                    "Graphics",
                    "Графика",
                    "R-register noise",
                    "Шум из регистра R",
                    "Uses the Z80 refresh register R as a quick changing source for a screen pattern.",
                    "Использует регистр регенерации Z80 R как быстро меняющийся источник узора.",
                    "LD A,R exposes the refresh counter. It is not random in the strict sense, but while filling 6144 bytes it creates a characteristic pseudo-noise bitmap.",
                    "LD A,R читает счётчик регенерации. Это не настоящий генератор случайных чисел, но при заполнении 6144 байт получается характерный псевдошум.",
                    PREVIEW_NOISE,
                    "ORG $8000\n\n    LD HL,$4000\n    LD D,24\nPAGE:\n    LD B,0\nPIXEL:\n    LD A,R\n    LD (HL),A\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Fill bitmap using the changing Z80 R register\nORG $8000\n\n    LD HL,$4000\n    LD D,24\nPAGE:\n    LD B,0\nPIXEL:\n    LD A,R          ; refresh counter, changes continuously\n    LD (HL),A\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n",
                    "; Заполняем bitmap меняющимся регистром R\nORG $8000\n\n    LD HL,$4000\n    LD D,24\nPAGE:\n    LD B,0\nPIXEL:\n    LD A,R          ; счётчик регенерации постоянно меняется\n    LD (HL),A\n    INC HL\n    DJNZ PIXEL\n    DEC D\n    JR NZ,PAGE\nHOLD:\n    JR HOLD\n"),

            ex("moving-attribute",
                    "Colour & attributes",
                    "Цвет и атрибуты",
                    "Moving attribute cell",
                    "Бегущее знакоместо",
                    "Moves one bright attribute cell across the top row at 50 Hz.",
                    "Перемещает одно яркое знакоместо по верхней строке с частотой кадров.",
                    "Only attribute RAM is touched. HALT gives stable frame timing, while the old cell is restored before HL advances to the next one.",
                    "Меняется только память атрибутов. HALT даёт стабильную кадровую синхронизацию, а старое знакоместо восстанавливается перед переходом к следующему.",
                    PREVIEW_MARQUEE,
                    "ORG $8000\n\n    EI\nSTART:\n    LD HL,$5800\n    LD B,32\nMOVE:\n    LD (HL),$47\n    HALT\n    LD (HL),$07\n    INC HL\n    DJNZ MOVE\n    JR START\n",
                    "; Move a bright cell across the first attribute row\nORG $8000\n\n    EI\nSTART:\n    LD HL,$5800     ; first attribute\n    LD B,32\nMOVE:\n    LD (HL),$47     ; bright white/blue cell\n    HALT            ; one position per frame\n    LD (HL),$07     ; restore normal attribute\n    INC HL\n    DJNZ MOVE\n    JR START\n",
                    "; Двигаем яркое знакоместо по первой строке атрибутов\nORG $8000\n\n    EI\nSTART:\n    LD HL,$5800     ; первый атрибут\n    LD B,32\nMOVE:\n    LD (HL),$47     ; яркое знакоместо\n    HALT            ; одна позиция за кадр\n    LD (HL),$07     ; возвращаем обычный атрибут\n    INC HL\n    DJNZ MOVE\n    JR START\n"),

            ex("invert-cell",
                    "Graphics",
                    "Графика",
                    "Animated inverse cell",
                    "Инверсия знакоместа",
                    "Draws an 8x8 stripe cell and inverts all eight scanlines every frame.",
                    "Рисует полосатое знакоместо 8x8 и инвертирует все восемь строк каждый кадр.",
                    "The example deliberately increments H rather than HL because the eight scanlines of one Spectrum character cell are separated by $100 bytes.",
                    "Здесь специально увеличивается H, а не HL: восемь строк одного знакоместа Spectrum разделены шагом $100.",
                    PREVIEW_SMILEY,
                    "ORG $8000\n\n    EI\n    LD HL,$4000\n    LD B,8\nDRAW:\n    LD (HL),$AA\n    INC H\n    DJNZ DRAW\n\nLOOP:\n    HALT\n    LD HL,$4000\n    LD B,8\nINVERT:\n    LD A,(HL)\n    XOR $FF\n    LD (HL),A\n    INC H\n    DJNZ INVERT\n    JR LOOP\n",
                    "; Invert one 8x8 cell every video frame\nORG $8000\n\n    EI\n    LD HL,$4000\n    LD B,8\nDRAW:\n    LD (HL),$AA\n    INC H           ; next scanline is +$100\n    DJNZ DRAW\n\nLOOP:\n    HALT\n    LD HL,$4000\n    LD B,8\nINVERT:\n    LD A,(HL)\n    XOR $FF\n    LD (HL),A\n    INC H\n    DJNZ INVERT\n    JR LOOP\n",
                    "; Инвертируем одно знакоместо 8x8 каждый кадр\nORG $8000\n\n    EI\n    LD HL,$4000\n    LD B,8\nDRAW:\n    LD (HL),$AA\n    INC H           ; следующая строка находится через $100\n    DJNZ DRAW\n\nLOOP:\n    HALT\n    LD HL,$4000\n    LD B,8\nINVERT:\n    LD A,(HL)\n    XOR $FF\n    LD (HL),A\n    INC H\n    DJNZ INVERT\n    JR LOOP\n"),

            ex("space-prints-star",
                    "Input",
                    "Ввод",
                    "SPACE prints a star",
                    "SPACE печатает звёздочку",
                    "Waits for SPACE and prints one '*' through the ROM for every key press.",
                    "Ждёт SPACE и печатает одну '*' через ПЗУ на каждое нажатие.",
                    "BC=$7FFE selects the keyboard half-row containing SPACE. The routine waits for a press and then for release, so holding the key does not flood the screen.",
                    "BC=$7FFE выбирает полуряд клавиатуры со SPACE. После нажатия программа ждёт отпускания, поэтому удержание клавиши не забивает экран символами.",
                    PREVIEW_KEY,
                    "ORG $8000\n\n    LD BC,$7FFE\nWAIT_PRESS:\n    IN A,(C)\n    AND 1\n    JR NZ,WAIT_PRESS\n    PUSH BC\n    LD A,42\n    RST $10\n    POP BC\nWAIT_RELEASE:\n    IN A,(C)\n    AND 1\n    JR Z,WAIT_RELEASE\n    JR WAIT_PRESS\n",
                    "; Print one star for each SPACE press\nORG $8000\n\n    LD BC,$7FFE     ; keyboard row containing SPACE\nWAIT_PRESS:\n    IN A,(C)\n    AND 1           ; SPACE is active low\n    JR NZ,WAIT_PRESS\n    PUSH BC\n    LD A,42         ; '*'\n    RST $10\n    POP BC\nWAIT_RELEASE:\n    IN A,(C)\n    AND 1\n    JR Z,WAIT_RELEASE\n    JR WAIT_PRESS\n",
                    "; Печатаем одну звёздочку на каждое нажатие SPACE\nORG $8000\n\n    LD BC,$7FFE     ; полуряд клавиатуры со SPACE\nWAIT_PRESS:\n    IN A,(C)\n    AND 1           ; SPACE активен нулём\n    JR NZ,WAIT_PRESS\n    PUSH BC\n    LD A,42         ; '*'\n    RST $10\n    POP BC\nWAIT_RELEASE:\n    IN A,(C)\n    AND 1\n    JR Z,WAIT_RELEASE\n    JR WAIT_PRESS\n"),

            snippet("routine-clear-bitmap",
                    "Clear bitmap",
                    "Очистить bitmap",
                    "Fast 6144-byte screen clear using LDIR.",
                    "Быстрая очистка 6144 байт экрана через LDIR.",
                    "Seeds the first byte with zero and copies it forward over the remaining 6143 bitmap bytes.",
                    "В первый байт записывается ноль, после чего он размножается вперёд на оставшиеся 6143 байта.",
                    "IN: none\nOUT: bitmap $4000..$57FF = 0\nDESTROYS: BC, DE, HL, flags",
                    "IN: нет\nOUT: bitmap $4000..$57FF = 0\nПОРТИТ: BC, DE, HL, флаги",
                    "ORG $8000\n    CALL CLEAR_BITMAP\nHOLD:\n    JR HOLD\n\nCLEAR_BITMAP:\n    LD HL,$4000\n    LD (HL),0\n    LD DE,$4001\n    LD BC,6143\n    LDIR\n    RET\n",
                    "; IN: none\n; OUT: bitmap $4000..$57FF = 0\n; DESTROYS: BC, DE, HL, flags\nORG $8000\n    CALL CLEAR_BITMAP\nHOLD:\n    JR HOLD\n\nCLEAR_BITMAP:\n    LD HL,$4000\n    LD (HL),0\n    LD DE,$4001\n    LD BC,6143\n    LDIR\n    RET\n",
                    "; IN: нет\n; OUT: bitmap $4000..$57FF = 0\n; ПОРТИТ: BC, DE, HL, флаги\nORG $8000\n    CALL CLEAR_BITMAP\nHOLD:\n    JR HOLD\n\nCLEAR_BITMAP:\n    LD HL,$4000\n    LD (HL),0\n    LD DE,$4001\n    LD BC,6143\n    LDIR\n    RET\n"),

            snippet("routine-clear-attrs",
                    "Fill attributes",
                    "Заполнить атрибуты",
                    "Fills all 768 attribute cells with one value in A.",
                    "Заполняет все 768 атрибутов одним значением из A.",
                    "Stores A at $5800 and uses LDIR to duplicate it over the rest of attribute RAM.",
                    "Записывает A в $5800 и размножает значение через LDIR по всей памяти атрибутов.",
                    "IN: A = attribute byte\nOUT: $5800..$5AFF filled\nDESTROYS: BC, DE, HL, flags",
                    "IN: A = байт атрибута\nOUT: $5800..$5AFF заполнены\nПОРТИТ: BC, DE, HL, флаги",
                    "ORG $8000\n    LD A,$47\n    CALL FILL_ATTRS\nHOLD:\n    JR HOLD\n\nFILL_ATTRS:\n    LD HL,$5800\n    LD (HL),A\n    LD DE,$5801\n    LD BC,767\n    LDIR\n    RET\n",
                    "; IN: A = attribute value\n; OUT: all 768 attribute cells filled\n; DESTROYS: BC, DE, HL, flags\nORG $8000\n    LD A,$47\n    CALL FILL_ATTRS\nHOLD:\n    JR HOLD\n\nFILL_ATTRS:\n    LD HL,$5800\n    LD (HL),A\n    LD DE,$5801\n    LD BC,767\n    LDIR\n    RET\n",
                    "; IN: A = значение атрибута\n; OUT: заполнены все 768 атрибутов\n; ПОРТИТ: BC, DE, HL, флаги\nORG $8000\n    LD A,$47\n    CALL FILL_ATTRS\nHOLD:\n    JR HOLD\n\nFILL_ATTRS:\n    LD HL,$5800\n    LD (HL),A\n    LD DE,$5801\n    LD BC,767\n    LDIR\n    RET\n"),

            snippet("routine-wait-frames",
                    "Wait N frames",
                    "Ждать N кадров",
                    "Waits an exact number of 50 Hz frame interrupts.",
                    "Ждёт заданное число кадровых прерываний 50 Гц.",
                    "B is decremented once per HALT. This is a compact timing primitive for animation and debounce code.",
                    "B уменьшается один раз на каждый HALT. Это удобная основа для анимации, задержек и подавления дребезга.",
                    "IN: B = frames (1..255)\nOUT: B = 0\nDESTROYS: B",
                    "IN: B = число кадров (1..255)\nOUT: B = 0\nПОРТИТ: B",
                    "ORG $8000\n    EI\n    LD B,50\n    CALL WAIT_FRAMES\n    LD A,2\n    OUT ($FE),A\nHOLD:\n    JR HOLD\n\nWAIT_FRAMES:\nWAIT_LOOP:\n    HALT\n    DJNZ WAIT_LOOP\n    RET\n",
                    "; IN: B = number of frames\n; OUT: B = 0\nORG $8000\n    EI\n    LD B,50\n    CALL WAIT_FRAMES\n    LD A,2\n    OUT ($FE),A\nHOLD:\n    JR HOLD\n\nWAIT_FRAMES:\nWAIT_LOOP:\n    HALT\n    DJNZ WAIT_LOOP\n    RET\n",
                    "; IN: B = число кадров\n; OUT: B = 0\nORG $8000\n    EI\n    LD B,50\n    CALL WAIT_FRAMES\n    LD A,2\n    OUT ($FE),A\nHOLD:\n    JR HOLD\n\nWAIT_FRAMES:\nWAIT_LOOP:\n    HALT\n    DJNZ WAIT_LOOP\n    RET\n"),

            snippet("routine-set-border",
                    "Set border",
                    "Установить бордюр",
                    "Small reusable routine that limits A to 0..7 and writes the border.",
                    "Небольшая подпрограмма ограничивает A диапазоном 0..7 и задаёт бордюр.",
                    "AND 7 makes the routine tolerant of higher bits in A. Note that writing port $FE also affects MIC/EAR output bits.",
                    "AND 7 позволяет передавать значение с лишними старшими битами. Запись в $FE также затрагивает линии MIC/EAR.",
                    "IN: A = colour\nOUT: A = colour & 7\nDESTROYS: flags",
                    "IN: A = цвет\nOUT: A = цвет & 7\nПОРТИТ: флаги",
                    "ORG $8000\n    LD A,6\n    CALL SET_BORDER\nHOLD:\n    JR HOLD\n\nSET_BORDER:\n    AND 7\n    OUT ($FE),A\n    RET\n",
                    "; IN: A = border colour\n; OUT: A = colour & 7\n; DESTROYS: flags\nORG $8000\n    LD A,6\n    CALL SET_BORDER\nHOLD:\n    JR HOLD\n\nSET_BORDER:\n    AND 7\n    OUT ($FE),A\n    RET\n",
                    "; IN: A = цвет бордюра\n; OUT: A = цвет & 7\n; ПОРТИТ: флаги\nORG $8000\n    LD A,6\n    CALL SET_BORDER\nHOLD:\n    JR HOLD\n\nSET_BORDER:\n    AND 7\n    OUT ($FE),A\n    RET\n"),

            snippet("routine-test-space",
                    "Test SPACE",
                    "Проверить SPACE",
                    "Returns Z=1 while SPACE is pressed.",
                    "Возвращает Z=1, пока нажата клавиша SPACE.",
                    "The keyboard is active-low. AND 1 leaves zero exactly when the SPACE bit is pulled low.",
                    "Клавиатура активна нулём. После AND 1 флаг Z устанавливается именно при нажатом SPACE.",
                    "IN: none\nOUT: Z=1 pressed, Z=0 released; A=0/1\nDESTROYS: A, BC, flags",
                    "IN: нет\nOUT: Z=1 нажата, Z=0 отпущена; A=0/1\nПОРТИТ: A, BC, флаги",
                    "ORG $8000\nLOOP:\n    CALL TEST_SPACE\n    JR Z,PRESSED\n    LD A,1\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2\n    OUT ($FE),A\n    JR LOOP\n\nTEST_SPACE:\n    LD BC,$7FFE\n    IN A,(C)\n    AND 1\n    RET\n",
                    "; OUT: Z=1 when SPACE is pressed\n; DESTROYS: A, BC, flags\nORG $8000\nLOOP:\n    CALL TEST_SPACE\n    JR Z,PRESSED\n    LD A,1\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2\n    OUT ($FE),A\n    JR LOOP\n\nTEST_SPACE:\n    LD BC,$7FFE\n    IN A,(C)\n    AND 1\n    RET\n",
                    "; OUT: Z=1, когда SPACE нажата\n; ПОРТИТ: A, BC, флаги\nORG $8000\nLOOP:\n    CALL TEST_SPACE\n    JR Z,PRESSED\n    LD A,1\n    OUT ($FE),A\n    JR LOOP\nPRESSED:\n    LD A,2\n    OUT ($FE),A\n    JR LOOP\n\nTEST_SPACE:\n    LD BC,$7FFE\n    IN A,(C)\n    AND 1\n    RET\n"),

            snippet("routine-print-string",
                    "Print fixed string",
                    "Печать строки",
                    "Prints B characters from HL through ROM RST $10.",
                    "Печатает B символов из HL через ПЗУ RST $10.",
                    "BC and HL are protected around each ROM call because ROM services are allowed to modify working registers.",
                    "BC и HL сохраняются вокруг каждого вызова ПЗУ, поскольку ROM-процедуры могут менять рабочие регистры.",
                    "IN: HL = text, B = length\nOUT: HL += length, B = 0\nDESTROYS: A, flags",
                    "IN: HL = строка, B = длина\nOUT: HL += длина, B = 0\nПОРТИТ: A, флаги",
                    "ORG $8000\n    LD HL,MESSAGE\n    LD B,12\n    CALL PRINT_STRING\nHOLD:\n    JR HOLD\n\nPRINT_STRING:\nNEXT_CHAR:\n    LD A,(HL)\n    PUSH BC\n    PUSH HL\n    RST $10\n    POP HL\n    POP BC\n    INC HL\n    DJNZ NEXT_CHAR\n    RET\n\nMESSAGE:\n    DB \"USEFUL Z80!\"\n",
                    "; IN: HL = string, B = length\n; OUT: HL advanced, B = 0\n; DESTROYS: A, flags\nORG $8000\n    LD HL,MESSAGE\n    LD B,12\n    CALL PRINT_STRING\nHOLD:\n    JR HOLD\n\nPRINT_STRING:\nNEXT_CHAR:\n    LD A,(HL)\n    PUSH BC\n    PUSH HL\n    RST $10\n    POP HL\n    POP BC\n    INC HL\n    DJNZ NEXT_CHAR\n    RET\n\nMESSAGE:\n    DB \"USEFUL Z80!\"\n",
                    "; IN: HL = строка, B = длина\n; OUT: HL продвинут, B = 0\n; ПОРТИТ: A, флаги\nORG $8000\n    LD HL,MESSAGE\n    LD B,12\n    CALL PRINT_STRING\nHOLD:\n    JR HOLD\n\nPRINT_STRING:\nNEXT_CHAR:\n    LD A,(HL)\n    PUSH BC\n    PUSH HL\n    RST $10\n    POP HL\n    POP BC\n    INC HL\n    DJNZ NEXT_CHAR\n    RET\n\nMESSAGE:\n    DB \"USEFUL Z80!\"\n"),

            snippet("routine-draw-8x8",
                    "Draw 8x8 cell",
                    "Нарисовать 8x8",
                    "Copies eight sprite bytes to one Spectrum character cell.",
                    "Копирует восемь байт спрайта в одно знакоместо Spectrum.",
                    "DE must point to the first scanline of the target cell. Incrementing D steps by $100 to the next scanline inside the same character row.",
                    "DE должен указывать на первую строку целевого знакоместа. INC D даёт шаг $100 к следующей строке внутри этого знакоместа.",
                    "IN: HL = 8 sprite bytes, DE = first scanline address\nOUT: HL += 8, D += 8\nDESTROYS: A, B, flags",
                    "IN: HL = 8 байт спрайта, DE = адрес первой строки\nOUT: HL += 8, D += 8\nПОРТИТ: A, B, флаги",
                    "ORG $8000\n    LD HL,SPRITE\n    LD DE,$4000\n    CALL DRAW_8X8\nHOLD:\n    JR HOLD\n\nDRAW_8X8:\n    LD B,8\nROW:\n    LD A,(HL)\n    LD (DE),A\n    INC HL\n    INC D\n    DJNZ ROW\n    RET\n\nSPRITE:\n    DB $3C,$42,$A5,$81,$A5,$99,$42,$3C\n",
                    "; IN: HL = sprite, DE = first scanline address\n; OUT: HL += 8, D += 8\n; DESTROYS: A, B, flags\nORG $8000\n    LD HL,SPRITE\n    LD DE,$4000\n    CALL DRAW_8X8\nHOLD:\n    JR HOLD\n\nDRAW_8X8:\n    LD B,8\nROW:\n    LD A,(HL)\n    LD (DE),A\n    INC HL\n    INC D           ; next scanline = +$100\n    DJNZ ROW\n    RET\n\nSPRITE:\n    DB $3C,$42,$A5,$81,$A5,$99,$42,$3C\n",
                    "; IN: HL = спрайт, DE = адрес первой строки\n; OUT: HL += 8, D += 8\n; ПОРТИТ: A, B, флаги\nORG $8000\n    LD HL,SPRITE\n    LD DE,$4000\n    CALL DRAW_8X8\nHOLD:\n    JR HOLD\n\nDRAW_8X8:\n    LD B,8\nROW:\n    LD A,(HL)\n    LD (DE),A\n    INC HL\n    INC D           ; следующая строка = +$100\n    DJNZ ROW\n    RET\n\nSPRITE:\n    DB $3C,$42,$A5,$81,$A5,$99,$42,$3C\n"),

            snippet("routine-memfill",
                    "Fill memory",
                    "Заполнить память",
                    "Fills BC bytes from HL with the value passed in A.",
                    "Заполняет BC байт от HL значением, переданным в A.",
                    "D keeps the fill byte while A is temporarily used to test whether BC reached zero.",
                    "D хранит байт заполнения, пока A временно используется для проверки BC на ноль.",
                    "IN: HL = address, BC = length, A = value\nOUT: HL += length, BC = 0\nDESTROYS: A, D, flags",
                    "IN: HL = адрес, BC = длина, A = значение\nOUT: HL += длина, BC = 0\nПОРТИТ: A, D, флаги",
                    "ORG $8000\n    LD HL,$5800\n    LD BC,768\n    LD A,$45\n    CALL MEMFILL\nHOLD:\n    JR HOLD\n\nMEMFILL:\n    LD D,A\nFILL_LOOP:\n    LD A,D\n    LD (HL),A\n    INC HL\n    DEC BC\n    LD A,B\n    OR C\n    JR NZ,FILL_LOOP\n    RET\n",
                    "; IN: HL = address, BC = length, A = fill byte\n; OUT: HL advanced, BC = 0\n; DESTROYS: A, D, flags\nORG $8000\n    LD HL,$5800\n    LD BC,768\n    LD A,$45\n    CALL MEMFILL\nHOLD:\n    JR HOLD\n\nMEMFILL:\n    LD D,A\nFILL_LOOP:\n    LD A,D\n    LD (HL),A\n    INC HL\n    DEC BC\n    LD A,B\n    OR C\n    JR NZ,FILL_LOOP\n    RET\n",
                    "; IN: HL = адрес, BC = длина, A = байт заполнения\n; OUT: HL продвинут, BC = 0\n; ПОРТИТ: A, D, флаги\nORG $8000\n    LD HL,$5800\n    LD BC,768\n    LD A,$45\n    CALL MEMFILL\nHOLD:\n    JR HOLD\n\nMEMFILL:\n    LD D,A\nFILL_LOOP:\n    LD A,D\n    LD (HL),A\n    INC HL\n    DEC BC\n    LD A,B\n    OR C\n    JR NZ,FILL_LOOP\n    RET\n")
    };

    private static Example ex(String id, String categoryEn, String categoryRu,
                              String titleEn, String titleRu,
                              String descriptionEn, String descriptionRu,
                              String detailsEn, String detailsRu,
                              int previewType, String source,
                              String commentedEn, String commentedRu) {
        return new Example(id, categoryEn, categoryRu, titleEn, titleRu,
                descriptionEn, descriptionRu, detailsEn, detailsRu,
                previewType, source, commentedEn, commentedRu, null, null);
    }

    private static Example snippet(String id,
                                   String titleEn, String titleRu,
                                   String descriptionEn, String descriptionRu,
                                   String detailsEn, String detailsRu,
                                   String ioEn, String ioRu,
                                   String source, String commentedEn, String commentedRu) {
        return new Example(id, "Useful routines", "Полезные подпрограммы",
                titleEn, titleRu, descriptionEn, descriptionRu, detailsEn, detailsRu,
                PREVIEW_ROUTINE, source, commentedEn, commentedRu, ioEn, ioRu);
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
        public final String ioEn;
        public final String ioRu;

        Example(String id, String categoryEn, String categoryRu,
                String title, String titleRu, String description, String descriptionRu,
                String detailsEn, String detailsRu, int previewType, String source,
                String commentedEn, String commentedRu, String ioEn, String ioRu) {
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
            this.ioEn = ioEn;
            this.ioRu = ioRu;
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

        public String io(AppLanguage language) {
            return language == AppLanguage.RU ? ioRu : ioEn;
        }

        public boolean hasIo() {
            return ioEn != null && !ioEn.trim().isEmpty();
        }
    }
}
