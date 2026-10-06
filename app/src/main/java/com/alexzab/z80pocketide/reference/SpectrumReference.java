package com.alexzab.z80pocketide.reference;

import com.alexzab.z80pocketide.i18n.AppLanguage;

/** ZX Spectrum 48K quick-reference material shown inside the app. */
public final class SpectrumReference {
    private SpectrumReference() {}

    public static final Topic[] TOPICS = {
            new Topic(
                    "48K memory map", "Карта памяти 48K",
                    "Physical address space", "Физическое адресное пространство",
                    new String[] {
                            "$0000-$3FFF  16K ROM",
                            "$4000-$57FF  Screen bitmap (6144 bytes)",
                            "$5800-$5AFF  Attributes (768 bytes)",
                            "$5B00-$5BFF  Printer buffer / workspace",
                            "$5C00-$FFFF  System variables + dynamic RAM"
                    },
                    new String[] {
                            "$0000-$3FFF  16 КБ ПЗУ",
                            "$4000-$57FF  Bitmap экрана (6144 байта)",
                            "$5800-$5AFF  Атрибуты (768 байт)",
                            "$5B00-$5BFF  Буфер принтера / рабочая область",
                            "$5C00-$FFFF  Системные переменные + динамическое ОЗУ"
                    },
                    "The 48K model has one fixed 16K ROM and 48K RAM. Above $5C00 the ROM uses RAM dynamically for system variables, BASIC program, variables, calculator stack, machine stack and user data, so those logical boundaries move.",
                    "В модели 48K одна фиксированная 16-КБ ПЗУ и 48 КБ ОЗУ. Выше $5C00 ПЗУ динамически использует память под системные переменные, BASIC-программу, переменные, стек калькулятора, машинный стек и пользовательские данные, поэтому логические границы там плавают."
            ),
            new Topic(
                    "Bitmap layout", "Структура bitmap",
                    "256 x 192 pixels · 32 bytes per raster line", "256 x 192 пикселя · 32 байта на строку",
                    new String[] {
                            "Bitmap: $4000-$57FF",
                            "3 thirds x 64 pixel lines",
                            "Each third: 8 character rows x 8 pixel lines",
                            "Each raster line: 32 bytes = 256 pixels"
                    },
                    new String[] {
                            "Bitmap: $4000-$57FF",
                            "3 трети по 64 пиксельные строки",
                            "В каждой трети: 8 строк знакомест x 8 линий пикселя",
                            "В каждой растровой строке: 32 байта = 256 пикселей"
                    },
                    "The screen is not stored as 192 consecutive raster lines. The ULA-friendly order groups equal pixel-line numbers from successive 8x8 character rows inside each 64-line third.",
                    "Экран не хранится как 192 последовательные растровые строки. Порядок удобен ULA: внутри каждой 64-строчной трети сначала идут одинаковые номера пиксельных линий разных строк знакомест."
            ),
            new Topic(
                    "Pixel address bits", "Биты адреса пикселей",
                    "Address bits: 010 TT LLL RRR CCCCC", "Биты адреса: 010 TT LLL RRR CCCCC",
                    new String[] {
                            "010     fixed bitmap prefix ($4000)",
                            "TT      third: Y7 Y6",
                            "LLL     pixel line in 8x8 cell: Y2 Y1 Y0",
                            "RRR     character row inside third: Y5 Y4 Y3",
                            "CCCCC   byte column: X4..X0 (X / 8)"
                    },
                    new String[] {
                            "010     фиксированный префикс bitmap ($4000)",
                            "TT      треть: Y7 Y6",
                            "LLL     линия пикселя внутри 8x8: Y2 Y1 Y0",
                            "RRR     строка знакомест внутри трети: Y5 Y4 Y3",
                            "CCCCC   байтовый столбец: X4..X0 (X / 8)"
                    },
                    "Formula for pixel coordinates x=0..255, y=0..191:\n$4000 | ((y & $C0) << 5) | ((y & 7) << 8) | ((y & $38) << 2) | (x >> 3)",
                    "Формула для координат x=0..255, y=0..191:\n$4000 | ((y & $C0) << 5) | ((y & 7) << 8) | ((y & $38) << 2) | (x >> 3)"
            ),
            new Topic(
                    "Screen thirds", "Трети экрана",
                    "Why $4000, $4800 and $5000 matter", "Почему важны $4000, $4800 и $5000",
                    new String[] {
                            "Top third:    $4000-$47FF  y=0..63",
                            "Middle third: $4800-$4FFF  y=64..127",
                            "Bottom third: $5000-$57FF  y=128..191",
                            "One third = $0800 = 2048 bytes"
                    },
                    new String[] {
                            "Верхняя треть: $4000-$47FF  y=0..63",
                            "Средняя треть:  $4800-$4FFF  y=64..127",
                            "Нижняя треть:   $5000-$57FF  y=128..191",
                            "Одна треть = $0800 = 2048 байт"
                    },
                    "The TT field is 00, 01 or 10. The combination 11 would point above bitmap RAM, so it is not a fourth screen third.",
                    "Поле TT принимает 00, 01 или 10. Комбинация 11 уже выводит адрес за bitmap, поэтому четвёртой трети экрана нет."
            ),
            new Topic(
                    "Attributes", "Атрибуты",
                    "32 x 24 cells at $5800-$5AFF", "32 x 24 знакоместа по $5800-$5AFF",
                    new String[] {
                            "Attribute address = $5800 + (y >> 3) * 32 + (x >> 3)",
                            "Byte bits: F B PPP III",
                            "F = FLASH",
                            "B = BRIGHT",
                            "PPP = PAPER 0..7",
                            "III = INK 0..7"
                    },
                    new String[] {
                            "Адрес атрибута = $5800 + (y >> 3) * 32 + (x >> 3)",
                            "Биты байта: F B PPP III",
                            "F = FLASH",
                            "B = BRIGHT",
                            "PPP = PAPER 0..7",
                            "III = INK 0..7"
                    },
                    "Unlike bitmap RAM, attribute RAM is linear: 32 bytes per character row and 24 rows total.",
                    "В отличие от bitmap, память атрибутов линейна: 32 байта на строку знакомест и всего 24 строки."
            ),
            new Topic(
                    "Useful system variables", "Полезные системные переменные",
                    "Common 48K ROM variables", "Часто нужные переменные ПЗУ 48K",
                    new String[] {
                            "$5C48 BORDCR  border colour / lower-screen attributes",
                            "$5C78 FRAMES  3-byte frame counter",
                            "$5C7B UDG     pointer to user-defined graphics",
                            "$5C8D ATTR_P  permanent print attribute",
                            "$5C8F ATTR_T  temporary print attribute"
                    },
                    new String[] {
                            "$5C48 BORDCR  цвет бордюра / атрибуты нижнего экрана",
                            "$5C78 FRAMES  3-байтовый счётчик кадров",
                            "$5C7B UDG     указатель на пользовательскую графику",
                            "$5C8D ATTR_P  постоянный атрибут печати",
                            "$5C8F ATTR_T  временный атрибут печати"
                    },
                    "These addresses belong to the standard 48K ROM environment. Code that replaces the ROM or system-variable layout should not assume them blindly.",
                    "Эти адреса относятся к стандартной среде ПЗУ 48K. Код с заменённым ПЗУ или иной раскладкой системных переменных не должен полагаться на них без проверки."
            )
    };

    public static final RomRoutine[] ROM = {
            new RomRoutine("$0010 / RST $10", "PRINT-A",
                    "Print one character through the current ROM channel.",
                    "Печатает один символ через текущий канал ПЗУ.",
                    "IN: A = character code",
                    "IN: A = код символа",
                    "Typical: LD A,'A' / RST $10. The current channel matters; stream 2 is normally the main screen.",
                    "Обычно: LD A,'A' / RST $10. Важен текущий канал; поток 2 обычно соответствует основному экрану."),
            new RomRoutine("$1601", "CHAN-OPEN",
                    "Make a ROM stream/channel current.",
                    "Делает поток/канал ПЗУ текущим.",
                    "IN: A = stream number",
                    "IN: A = номер потока",
                    "For predictable screen output: LD A,2 / CALL $1601 before RST $10.",
                    "Для предсказуемой печати на экран: LD A,2 / CALL $1601 перед RST $10."),
            new RomRoutine("$0D6B", "CLS",
                    "Clear the display using the ROM's current permanent attributes.",
                    "Очищает экран с использованием текущих постоянных атрибутов ПЗУ.",
                    "IN: none",
                    "IN: нет",
                    "This is the ROM CLS command entry. It also rebuilds the lower screen area.",
                    "Точка входа команды CLS ПЗУ. Она также восстанавливает нижнюю область экрана."),
            new RomRoutine("$028E", "KEY-SCAN",
                    "Scan the Spectrum keyboard matrix.",
                    "Сканирует матрицу клавиатуры Spectrum.",
                    "OUT: D = shift key/$FF, E = other key 0..$27/$FF; Z reset for invalid multi-key combination",
                    "OUT: D = shift/$FF, E = другая клавиша 0..$27/$FF; Z сброшен при недопустимой комбинации",
                    "DE=$FFFF means no key. This is a ROM-level scan result, not an ASCII character.",
                    "DE=$FFFF означает отсутствие клавиши. Результат — код сканирования ПЗУ, а не ASCII."),
            new RomRoutine("$03B5", "BEEPER",
                    "Low-level speaker tone generator.",
                    "Низкоуровневый генератор тона пищалки.",
                    "IN: DE = number of tone cycles, HL = loop-delay parameter",
                    "IN: DE = число периодов, HL = параметр задержки",
                    "The routine disables interrupts while sounding and re-enables them before returning.",
                    "На время звука рутина запрещает прерывания и снова разрешает их перед возвратом."),
            new RomRoutine("$0E9B", "CL-ADDR",
                    "Calculate a display-file address for a ROM text line.",
                    "Вычисляет адрес display file для строки текста ПЗУ.",
                    "IN: B = ROM line number; OUT: HL = display-file address",
                    "IN: B = номер строки ПЗУ; OUT: HL = адрес display file",
                    "This is useful when interfacing with ROM print/clear logic; its line numbering follows ROM conventions rather than raw y pixels.",
                    "Полезна при работе с логикой печати/очистки ПЗУ; нумерация строк соответствует соглашениям ПЗУ, а не сырым координатам y."),
            new RomRoutine("$0B24", "PO-ANY",
                    "ROM character/graphics printing worker.",
                    "Рабочая рутина печати символов/графики ПЗУ.",
                    "IN: A = character, B = line, C = column, HL = display/printer address",
                    "IN: A = символ, B = строка, C = столбец, HL = адрес экрана/буфера",
                    "More direct than RST $10 but requires the ROM print state to be coherent. Prefer RST $10 unless you specifically need this level.",
                    "Более низкоуровневая, чем RST $10, но требует корректного состояния подсистемы печати ПЗУ. Обычно лучше использовать RST $10.")
    };

    public static final PortEntry[] PORTS = {
            new PortEntry(
                    "$FE / any even ULA port", "$FE / любой чётный порт ULA",
                    "ULA: border, keyboard, EAR/MIC and beeper", "ULA: бордюр, клавиатура, EAR/MIC и пищалка",
                    new String[] {
                            "OUT: D0-D2 border colour 0..7",
                            "OUT: D3 MIC, D4 EAR/speaker",
                            "IN: D0-D4 keyboard columns, active low",
                            "IN: D6 EAR input",
                            "Keyboard rows are selected by A8..A15; A0 must be 0"
                    },
                    new String[] {
                            "OUT: D0-D2 цвет бордюра 0..7",
                            "OUT: D3 MIC, D4 EAR/динамик",
                            "IN: D0-D4 столбцы клавиатуры, активный 0",
                            "IN: D6 вход EAR",
                            "Строки клавиатуры выбираются A8..A15; A0 должен быть 0"
                    },
                    "The original 48K ULA only partially decodes the I/O address, so software conventionally writes port $FE but many even ports also reach it.",
                    "Оригинальная ULA 48K декодирует адрес порта неполностью, поэтому обычно используют $FE, хотя к ULA попадают и многие другие чётные адреса."),
            new PortEntry(
                    "Keyboard matrix", "Матрица клавиатуры",
                    "Eight half-rows, five keys per row", "Восемь полурядов по пять клавиш",
                    new String[] {
                            "$FEFE  CAPS SHIFT Z X C V",
                            "$FDFE  A S D F G",
                            "$FBFE  Q W E R T",
                            "$F7FE  1 2 3 4 5",
                            "$EFFE  0 9 8 7 6",
                            "$DFFE  P O I U Y",
                            "$BFFE  ENTER L K J H",
                            "$7FFE  SPACE SYMBOL SHIFT M N B"
                    },
                    new String[] {
                            "$FEFE  CAPS SHIFT Z X C V",
                            "$FDFE  A S D F G",
                            "$FBFE  Q W E R T",
                            "$F7FE  1 2 3 4 5",
                            "$EFFE  0 9 8 7 6",
                            "$DFFE  P O I U Y",
                            "$BFFE  ENTER L K J H",
                            "$7FFE  SPACE SYMBOL SHIFT M N B"
                    },
                    "Read with IN A,(C) after loading BC with the row address. A pressed key clears its corresponding bit among D0-D4.",
                    "Для чтения удобно загрузить адрес строки в BC и выполнить IN A,(C). Нажатая клавиша сбрасывает соответствующий бит D0-D4."),
            new PortEntry(
                    "$1F", "$1F",
                    "Kempston joystick", "Джойстик Kempston",
                    new String[] {
                            "Bit 0 = RIGHT",
                            "Bit 1 = LEFT",
                            "Bit 2 = DOWN",
                            "Bit 3 = UP",
                            "Bit 4 = FIRE",
                            "Bits are active high"
                    },
                    new String[] {
                            "Бит 0 = ВПРАВО",
                            "Бит 1 = ВЛЕВО",
                            "Бит 2 = ВНИЗ",
                            "Бит 3 = ВВЕРХ",
                            "Бит 4 = ОГОНЬ",
                            "Биты активны единицей"
                    },
                    "Classic Kempston interfaces are read with IN A,($1F). Some clones or modern emulators may expose extra bits or configurable mappings.",
                    "Классический Kempston читается через IN A,($1F). Клоны и современные эмуляторы могут иметь дополнительные биты или настраиваемую раскладку."),
            new PortEntry(
                    "$FFFD / $BFFD", "$FFFD / $BFFD",
                    "AY-3-8912/8910 on 128K machines and AY interfaces", "AY-3-8912/8910 на 128K и внешних AY-интерфейсах",
                    new String[] {
                            "OUT $FFFD: select AY register",
                            "OUT $BFFD: write selected register",
                            "IN  $FFFD: read selected register",
                            "Registers 0-5 tone periods; 6 noise; 7 mixer; 8-10 volumes",
                            "Registers 11-13 envelope period/shape"
                    },
                    new String[] {
                            "OUT $FFFD: выбрать регистр AY",
                            "OUT $BFFD: записать выбранный регистр",
                            "IN  $FFFD: прочитать выбранный регистр",
                            "Регистры 0-5 периоды тона; 6 шум; 7 микшер; 8-10 громкости",
                            "Регистры 11-13 период/форма огибающей"
                    },
                    "The plain 48K Spectrum has no built-in AY. These ports apply to the 128K family or a compatible external AY interface.",
                    "В обычном Spectrum 48K встроенного AY нет. Эти порты относятся к семейству 128K или совместимому внешнему AY-интерфейсу."),
            new PortEntry(
                    "EAR / MIC / speaker", "EAR / MIC / динамик",
                    "Audio and cassette bits share the ULA port", "Аудио и кассета делят порт ULA",
                    new String[] {
                            "OUT $FE bit 3 = MIC",
                            "OUT $FE bit 4 = EAR/speaker level",
                            "IN  $FE bit 6 = EAR cassette input",
                            "Changing border via OUT $FE should preserve audio bits when needed"
                    },
                    new String[] {
                            "OUT $FE бит 3 = MIC",
                            "OUT $FE бит 4 = EAR/уровень динамика",
                            "IN  $FE бит 6 = вход EAR с кассеты",
                            "При смене бордюра через OUT $FE при необходимости сохраняйте аудиобиты"
                    },
                    "Simple beeper music is generated by toggling bit 4 in software; ROM BEEPER $03B5 is a convenient higher-level alternative.",
                    "Простейший звук пищалки получается программным переключением бита 4; более удобная альтернатива — ROM BEEPER $03B5.")
    };

    public static final class Topic {
        public final String titleEn, titleRu;
        public final String subtitleEn, subtitleRu;
        public final String[] linesEn, linesRu;
        public final String noteEn, noteRu;

        Topic(String titleEn, String titleRu, String subtitleEn, String subtitleRu,
              String[] linesEn, String[] linesRu, String noteEn, String noteRu) {
            this.titleEn = titleEn;
            this.titleRu = titleRu;
            this.subtitleEn = subtitleEn;
            this.subtitleRu = subtitleRu;
            this.linesEn = linesEn;
            this.linesRu = linesRu;
            this.noteEn = noteEn;
            this.noteRu = noteRu;
        }

        public String title(AppLanguage l) { return l == AppLanguage.RU ? titleRu : titleEn; }
        public String subtitle(AppLanguage l) { return l == AppLanguage.RU ? subtitleRu : subtitleEn; }
        public String[] lines(AppLanguage l) { return l == AppLanguage.RU ? linesRu : linesEn; }
        public String note(AppLanguage l) { return l == AppLanguage.RU ? noteRu : noteEn; }
    }

    public static final class PortEntry {
        public final String portEn, portRu;
        public final String titleEn, titleRu;
        public final String[] linesEn, linesRu;
        public final String noteEn, noteRu;

        PortEntry(String portEn, String portRu, String titleEn, String titleRu,
                  String[] linesEn, String[] linesRu, String noteEn, String noteRu) {
            this.portEn = portEn;
            this.portRu = portRu;
            this.titleEn = titleEn;
            this.titleRu = titleRu;
            this.linesEn = linesEn;
            this.linesRu = linesRu;
            this.noteEn = noteEn;
            this.noteRu = noteRu;
        }

        public String port(AppLanguage l) { return l == AppLanguage.RU ? portRu : portEn; }
        public String title(AppLanguage l) { return l == AppLanguage.RU ? titleRu : titleEn; }
        public String[] lines(AppLanguage l) { return l == AppLanguage.RU ? linesRu : linesEn; }
        public String note(AppLanguage l) { return l == AppLanguage.RU ? noteRu : noteEn; }
    }

    public static final class RomRoutine {
        public final String address, name;
        public final String descriptionEn, descriptionRu;
        public final String ioEn, ioRu;
        public final String noteEn, noteRu;

        RomRoutine(String address, String name,
                   String descriptionEn, String descriptionRu,
                   String ioEn, String ioRu,
                   String noteEn, String noteRu) {
            this.address = address;
            this.name = name;
            this.descriptionEn = descriptionEn;
            this.descriptionRu = descriptionRu;
            this.ioEn = ioEn;
            this.ioRu = ioRu;
            this.noteEn = noteEn;
            this.noteRu = noteRu;
        }

        public String description(AppLanguage l) { return l == AppLanguage.RU ? descriptionRu : descriptionEn; }
        public String io(AppLanguage l) { return l == AppLanguage.RU ? ioRu : ioEn; }
        public String note(AppLanguage l) { return l == AppLanguage.RU ? noteRu : noteEn; }
    }
}
