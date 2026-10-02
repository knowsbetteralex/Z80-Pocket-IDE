package com.alexzab.z80pocketide.i18n;

public final class Texts {
    private Texts() {}

    public static String pick(AppLanguage language, String en, String ru) {
        return language == AppLanguage.RU ? ru : en;
    }

    public static String category(AppLanguage language, String category) {
        if (language != AppLanguage.RU) return category;
        switch (category) {
            case "Data": return "Данные";
            case "Arithmetic": return "Арифметика";
            case "Logic": return "Логика";
            case "CPU": return "Процессор";
            case "Interrupt": return "Прерывания";
            case "Flow": return "Переходы";
            case "Stack": return "Стек";
            case "Bit": return "Биты";
            case "I/O": return "Ввод-вывод";
            case "Block": return "Блочные";
            case "Block I/O": return "Блочный ввод-вывод";
            case "Directive": return "Директива";
            default: return category;
        }
    }

    public static String flags(AppLanguage language, String flags) {
        if (language != AppLanguage.RU) return flags;
        String out = flags;
        out = out.replace("AF changes flags when popped", "AF изменяет флаги при извлечении");
        out = out.replace("IFF1/IFF2 set after next instruction", "IFF1/IFF2 устанавливаются после следующей команды");
        out = out.replace("IFF1/IFF2 cleared", "IFF1/IFF2 сброшены");
        out = out.replace("IFF handling", "работа с IFF");
        out = out.replace("updates", "изменяет");
        out = out.replace("unchanged", "без изменений");
        out = out.replace("complex", "сложные");
        out = out.replace(" set", " установлены");
        out = out.replace(" cleared", " сброшены");
        return out;
    }

    public static String referenceDescription(AppLanguage language, String en) {
        if (language != AppLanguage.RU) return en;
        switch (en) {
            case "Copy 8-bit or 16-bit data between registers and memory.": return "Копирует 8- или 16-битные данные между регистрами и памятью.";
            case "Increment operand by one.": return "Увеличивает операнд на единицу.";
            case "Decrement operand by one.": return "Уменьшает операнд на единицу.";
            case "Add operand to accumulator or 16-bit index/pair.": return "Прибавляет операнд к аккумулятору или 16-битной регистровой паре/индексному регистру.";
            case "Add operand plus carry.": return "Прибавляет операнд вместе с флагом переноса.";
            case "Subtract operand from A.": return "Вычитает операнд из A.";
            case "Subtract operand and carry.": return "Вычитает операнд и флаг переноса.";
            case "Bitwise AND with A.": return "Выполняет побитовое AND с A.";
            case "Bitwise OR with A.": return "Выполняет побитовое OR с A.";
            case "Bitwise exclusive OR with A.": return "Выполняет побитовое XOR с A.";
            case "Compare A with operand without changing A.": return "Сравнивает A с операндом, не изменяя A.";
            case "Decimal-adjust A after BCD arithmetic.": return "Корректирует A после BCD-арифметики.";
            case "Complement all bits in A.": return "Инвертирует все биты A.";
            case "Two's-complement negate A.": return "Меняет знак A в дополнительном коде.";
            case "Complement carry flag.": return "Инвертирует флаг переноса C.";
            case "Set carry flag.": return "Устанавливает флаг переноса C.";
            case "Do nothing for one instruction cycle.": return "Не выполняет действий в течение одного цикла команды.";
            case "Pause instruction execution until an interrupt/reset condition.": return "Останавливает выполнение команд до прерывания или сброса.";
            case "Disable maskable interrupts.": return "Запрещает маскируемые прерывания.";
            case "Enable maskable interrupts.": return "Разрешает маскируемые прерывания.";
            case "Select interrupt mode.": return "Выбирает режим прерываний.";
            case "Absolute jump, optionally conditional.": return "Абсолютный переход, при необходимости условный.";
            case "Relative jump within -128..+127 bytes.": return "Относительный переход в диапазоне -128..+127 байт.";
            case "Decrement B and jump while B is non-zero.": return "Уменьшает B и выполняет переход, пока B не равен нулю.";
            case "Call subroutine by pushing return address.": return "Вызывает подпрограмму, помещая адрес возврата в стек.";
            case "Return from subroutine.": return "Возвращается из подпрограммы.";
            case "Return from interrupt; intended for maskable interrupt service.": return "Возврат из обработчика маскируемого прерывания.";
            case "Return from non-maskable interrupt.": return "Возврат из немаскируемого прерывания.";
            case "One-byte call to a fixed low-memory vector.": return "Однобайтный вызов фиксированного вектора в нижней области памяти.";
            case "Push register pair onto stack.": return "Помещает регистровую пару в стек.";
            case "Pop register pair from stack.": return "Извлекает регистровую пару из стека.";
            case "Exchange register sets or register pair with stack word.": return "Обменивает наборы регистров либо регистровую пару со словом в стеке.";
            case "Exchange BC/DE/HL with alternate register set.": return "Обменивает BC/DE/HL с альтернативным набором регистров.";
            case "Test one bit without changing operand.": return "Проверяет отдельный бит, не изменяя операнд.";
            case "Set one bit.": return "Устанавливает выбранный бит.";
            case "Reset one bit.": return "Сбрасывает выбранный бит.";
            case "Rotate an 8-bit operand.": return "Вращает 8-битный операнд.";
            case "Shift an 8-bit operand.": return "Сдвигает 8-битный операнд.";
            case "Fast accumulator-only rotate instructions.": return "Быстрые команды вращения только аккумулятора.";
            case "Rotate nibbles between A and (HL) left.": return "Вращает полубайты между A и (HL) влево.";
            case "Rotate nibbles between A and (HL) right.": return "Вращает полубайты между A и (HL) вправо.";
            case "Read a byte from an I/O port.": return "Читает байт из порта ввода-вывода.";
            case "Write a byte to an I/O port.": return "Записывает байт в порт ввода-вывода.";
            case "Copy (HL) to (DE), increment pointers, decrement BC.": return "Копирует (HL) в (DE), увеличивает указатели и уменьшает BC.";
            case "Repeat LDI until BC becomes zero.": return "Повторяет LDI, пока BC не станет равен нулю.";
            case "Copy (HL) to (DE), decrement pointers, decrement BC.": return "Копирует (HL) в (DE), уменьшает указатели и BC.";
            case "Repeat LDD until BC becomes zero.": return "Повторяет LDD, пока BC не станет равен нулю.";
            case "Compare A with (HL), increment HL, decrement BC.": return "Сравнивает A с (HL), увеличивает HL и уменьшает BC.";
            case "Repeat CPI until match or BC=0.": return "Повторяет CPI до совпадения или пока BC не станет равен нулю.";
            case "Compare A with (HL), decrement HL and BC.": return "Сравнивает A с (HL), уменьшает HL и BC.";
            case "Repeat CPD until match or BC=0.": return "Повторяет CPD до совпадения или пока BC не станет равен нулю.";
            case "Input through port (C) to (HL), adjust B and HL.": return "Читает через порт (C) в (HL), изменяя B и HL.";
            case "Repeat block input until B reaches zero.": return "Повторяет блочный ввод, пока B не станет равен нулю.";
            case "Output (HL) through port (C), adjust B and HL.": return "Выводит (HL) через порт (C), изменяя B и HL.";
            case "Repeat block output until B reaches zero.": return "Повторяет блочный вывод, пока B не станет равен нулю.";
            case "Set assembly address.": return "Устанавливает адрес ассемблирования.";
            case "Define a constant symbol.": return "Определяет символьную константу.";
            case "Emit bytes or string data.": return "Записывает байты или строковые данные.";
            case "Emit little-endian 16-bit words.": return "Записывает 16-битные слова в формате little-endian.";
            case "Reserve/fill a number of bytes.": return "Резервирует или заполняет указанное число байт.";
            default: return en;
        }
    }

    public static String exampleTitle(AppLanguage language, String en) {
        if (language != AppLanguage.RU) return en;
        switch (en) {
            case "Border cycle": return "Перелив бордюра";
            case "Rainbow paper": return "Радужные атрибуты";
            case "Pixel stripes": return "Полосы в пикселях";
            default: return en;
        }
    }

    public static String exampleDescription(AppLanguage language, String en) {
        if (language != AppLanguage.RU) return en;
        switch (en) {
            case "Cycles the Spectrum border through all 8 colours": return "Перебирает все 8 цветов бордюра Spectrum";
            case "Fills the 32x24 attribute area with coloured vertical bands": return "Заполняет область атрибутов 32x24 цветными вертикальными полосами";
            case "Fills the 6144-byte bitmap with an AA/55 stripe pattern": return "Заполняет 6144 байта экрана чередующимся узором AA/55";
            default: return en;
        }
    }

    public static String localizeAssemblerError(AppLanguage language, String error) {
        if (error == null || language != AppLanguage.RU) return error;
        String out = error.replaceFirst("^line (\\d+):", "строка $1:");
        out = out.replace("unsupported instruction or operands:", "неподдерживаемая команда или операнды:");
        out = out.replace("undefined symbol:", "неизвестный символ:");
        out = out.replace("duplicate symbol:", "повторное определение символа:");
        out = out.replace("relative jump out of range", "относительный переход вне диапазона");
        out = out.replace("8-bit value out of range", "8-битное значение вне диапазона");
        out = out.replace("16-bit value out of range", "16-битное значение вне диапазона");
        out = out.replace("displacement out of range", "смещение вне диапазона");
        out = out.replace("division by zero", "деление на ноль");
        out = out.replace("invalid expression", "некорректное выражение");
        out = out.replace("RST vector must be 0,8,...,$38", "вектор RST должен быть 0,8,...,$38");
        out = out.replace("IM mode must be 0, 1 or 2", "режим IM должен быть 0, 1 или 2");
        return out;
    }
}
