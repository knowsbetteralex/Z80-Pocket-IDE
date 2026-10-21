package com.alexzab.z80pocketide.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Instruction-by-instruction expanded forms, including variants which used
 * to be merged into one syntax string. Timings are un-contended Z80 T-states.
 * The legacy InstructionReference quick table remains intact.
 */
public final class InstructionDetails {
    public static final class Variant {
        public final String form, timing, descriptionEn, descriptionRu, flagProfile;
        private Variant(String form,String timing,String en,String ru,String flags) {
            this.form=form;this.timing=timing;this.descriptionEn=en;
            this.descriptionRu=ru;this.flagProfile=flags;
        }
        public String description(boolean ru) { return ru?descriptionRu:descriptionEn; }
        public String flags(boolean ru) { return Z80FlagEffects.describe(flagProfile,ru); }
    }
    private static final Map<String,List<Variant>> CATALOG=new LinkedHashMap<>();

    private static Variant v(String form,String t,String en,String ru) {
        return new Variant(form,t,en,ru,"");
    }
    private static Variant f(String form,String t,String en,String ru,String flags) {
        return new Variant(form,t,en,ru,flags);
    }
    private static void add(String mnemonic,String defaultFlags,Variant... variants) {
        List<Variant> result=new ArrayList<>();
        for(Variant v:variants) {
            if(v.form.isEmpty()||v.timing.isEmpty()||v.descriptionEn.isEmpty()
                    ||v.descriptionRu.isEmpty())throw new IllegalArgumentException(mnemonic);
            result.add(new Variant(v.form,v.timing,v.descriptionEn,v.descriptionRu,
                    v.flagProfile.isEmpty()?defaultFlags:v.flagProfile));
        }
        if(CATALOG.put(mnemonic,Collections.unmodifiableList(result))!=null)
            throw new IllegalStateException("Repeated "+mnemonic);
    }

    static {
        String unchanged=Z80FlagEffects.NONE, add8=Z80FlagEffects.ADD8,
                sub8=Z80FlagEffects.SUB8, add16=Z80FlagEffects.ADD16,
                add16carry=Z80FlagEffects.ADCSBC16_ADD,
                sub16carry=Z80FlagEffects.ADCSBC16_SUB;
        add("LD",unchanged,
            v("LD r,r'","4","Copy one 8-bit register into another (r excludes (HL)).","Копирует один 8-битный регистр в другой, без обращения к (HL)."),
            v("LD r,n","7","Load an 8-bit immediate constant into a register.","Загружает 8-битную константу в регистр."),
            v("LD rr,nn","10","Load a 16-bit immediate word into BC, DE, HL or SP.","Загружает 16-битную константу в BC, DE, HL или SP."),
            v("LD r,(HL)","7","Read the byte at address HL into a register.","Читает байт по адресу HL в регистр."),
            v("LD (HL),r","7","Write a register byte to the address in HL.","Записывает байт регистра по адресу HL."),
            v("LD (HL),n","10","Write an immediate byte into (HL).","Записывает константу по адресу HL."),
            v("LD r,(IX+d)/(IY+d)","19","Read a byte using signed indexed displacement −128..127.","Читает байт по IX/IY со знаковым смещением −128..127."),
            v("LD (IX+d)/(IY+d),r","19","Write a byte at IX/IY plus signed offset.","Записывает байт по IX/IY со знаковым смещением."),
            v("LD (IX+d)/(IY+d),n","19","Store an immediate byte at indexed address.","Записывает константу по индексному адресу."),
            v("LD IX/IY,nn","14","Load a 16-bit immediate word into an index register.","Загружает слово-константу в IX или IY."),
            v("LD A,(nn) / LD (nn),A","13","Transfer A to/from an absolute 16-bit memory address.","Обмен значением A с памятью по абсолютному адресу."),
            v("LD HL,(nn) / LD (nn),HL","16","Load/store HL as a little-endian memory word.","Читает/пишет HL как слово с младшим байтом первым."),
            v("LD rr,(nn) / LD (nn),rr","20","ED-prefixed load/store for BC, DE or SP in memory.","ED-префикс: чтение/запись BC, DE или SP в памяти."),
            v("LD IX/IY,(nn) / LD (nn),IX/IY","20","Load/store an index register from/to an absolute address.","Читает/пишет индексный регистр по абсолютному адресу."),
            v("LD SP,HL","6","Copy HL to stack pointer, no memory access.","Копирует HL в указатель стека без обращения к памяти."),
            v("LD SP,IX/IY","10","Copy IX/IY into the stack pointer.","Копирует IX/IY в указатель стека."),
            v("LD I,A / LD R,A","9","Set interrupt-vector base I or refresh register R from A.","Записывает A в базу вектора прерываний I или регистр регенерации R."),
            f("LD A,I / LD A,R","9","Read I or R into A. P/V reflects IFF2, not arithmetic parity.","Читает I или R в A. P/V получает IFF2, а не чётность.",Z80FlagEffects.LD_A_IR)
        );
        add("INC",Z80FlagEffects.INC8,
            v("INC r","4","Increment 8-bit register modulo 256.","Увеличивает 8-битный регистр с переполнением по модулю 256."),
            f("INC rr","6","Increment BC, DE, HL or SP modulo 65536. Flags unaffected.","Увеличивает BC, DE, HL или SP по модулю 65536. Флаги не меняются.",unchanged),
            v("INC (HL)","11","Read, increment and write back the byte addressed by HL.","Читает, увеличивает и записывает обратно байт по HL."),
            v("INC (IX+d)/(IY+d)","23","Read-modify-write indexed byte; flags describe the new byte.","Увеличивает байт с индексным адресом; флаги по результату."),
            f("INC IX/IY","10","Increment an index register without changing flags.","Увеличивает индексный регистр без изменения флагов.",unchanged));
        add("DEC",Z80FlagEffects.DEC8,
            v("DEC r","4","Decrement an 8-bit register modulo 256.","Уменьшает 8-битный регистр по модулю 256."),
            f("DEC rr","6","Decrement BC, DE, HL or SP without changing flags.","Уменьшает BC, DE, HL или SP без изменения флагов.",unchanged),
            v("DEC (HL)","11","Read, decrement and write back byte at (HL).","Уменьшает байт по HL с записью результата."),
            v("DEC (IX+d)/(IY+d)","23","Decrement indexed memory byte.","Уменьшает байт по индексному адресу."),
            f("DEC IX/IY","10","Decrement IX/IY without changing flags.","Уменьшает IX/IY без изменения флагов.",unchanged));
        add("ADD",add8,
            v("ADD A,r","4","A ← A + register; sets full 8-bit arithmetic flags.","A ← A + регистр; все арифметические флаги."),
            v("ADD A,n","7","Add immediate 8-bit value to A.","Прибавляет константу к A."),
            v("ADD A,(HL)","7","Add byte at HL to A.","Прибавляет к A байт по HL."),
            v("ADD A,(IX+d)/(IY+d)","19","Add indexed memory byte to A.","Прибавляет к A индексный байт."),
            f("ADD HL,rr","11","HL ← HL + BC/DE/HL/SP. S, Z, P/V preserved.", "HL ← HL + BC/DE/HL/SP. S, Z, P/V сохраняются.",add16),
            f("ADD IX/IY,rr","15","IX/IY plus matching 16-bit pair, including itself or SP.","IX/IY плюс допустимая пара, включая себя или SP.",add16));
        add("ADC",add8,
            v("ADC A,r","4","A ← A + r + old carry.","A ← A + r + прежний перенос."),
            v("ADC A,n","7","A ← A + immediate + old carry.","A ← A + константа + прежний перенос."),
            v("ADC A,(HL)","7","A ← A + (HL) + old carry.","A ← A + (HL) + прежний перенос."),
            v("ADC A,(IX+d)/(IY+d)","19","A ← A + indexed byte + old carry.","A ← A + индексный байт + прежний перенос."),
            f("ADC HL,rr","15","HL ← HL + rr + old carry. All six flags updated.", "HL ← HL + rr + прежний перенос. Все шесть флагов.",add16carry));
        add("SUB",sub8,
            v("SUB r","4","A ← A − r. The source register is not changed.","A ← A − r. Регистр-источник не меняется."),
            v("SUB n","7","Subtract an immediate byte from A.","Вычитает константу из A."),
            v("SUB (HL)","7","Subtract byte addressed by HL from A.","Вычитает из A байт по HL."),
            v("SUB (IX+d)/(IY+d)","19","Subtract indexed memory byte from A.","Вычитает из A индексный байт."));
        add("SBC",sub8,
            v("SBC A,r","4","A ← A − r − old carry.","A ← A − r − прежний перенос."),
            v("SBC A,n","7","Subtract immediate and old carry from A.","Вычитает константу и прежний перенос."),
            v("SBC A,(HL)","7","Subtract memory byte and old carry.","Вычитает байт по HL и прежний перенос."),
            v("SBC A,(IX+d)/(IY+d)","19","Subtract indexed byte and old carry.","Вычитает индексный байт и прежний перенос."),
            f("SBC HL,rr","15","HL ← HL − rr − old carry, with all 16-bit flags.","HL ← HL − rr − прежний перенос, с 16-битными флагами.",sub16carry));
        add("AND",Z80FlagEffects.AND,
            v("AND r","4","A ← A AND r; H is set, C cleared.","A ← A И r; H=1, C=0."),
            v("AND n","7","Mask A with an immediate value.","Побитовое И A с константой."),
            v("AND (HL)","7","Mask A using byte at HL.","Побитовое И A с байтом по HL."),
            v("AND (IX+d)/(IY+d)","19","Mask A with byte at indexed address.","Побитовое И A с индексным байтом."));
        add("OR",Z80FlagEffects.LOGIC,
            v("OR r","4","A ← A OR register.","A ← A ИЛИ регистр."),
            v("OR n","7","A ← A OR constant.","A ← A ИЛИ константа."),
            v("OR (HL)","7","OR A with byte at HL.","ИЛИ A с байтом по HL."),
            v("OR (IX+d)/(IY+d)","19","OR A with indexed byte.","ИЛИ A с индексным байтом."));
        add("XOR",Z80FlagEffects.LOGIC,
            v("XOR r","4","A ← A XOR register; XOR A clears A and carry.","A ← A ИСКЛ.ИЛИ регистр; XOR A обнуляет A и перенос."),
            v("XOR n","7","XOR A with immediate constant.","Исключающее ИЛИ A с константой."),
            v("XOR (HL)","7","XOR A with byte at HL.","Исключающее ИЛИ A с байтом по HL."),
            v("XOR (IX+d)/(IY+d)","19","XOR A with indexed byte.","Исключающее ИЛИ A с индексным байтом."));
        add("CP",Z80FlagEffects.CP,
            v("CP r","4","Compare A − r; discard result, leave A unchanged.","Сравнивает A − r, результат не сохраняет; A не меняется."),
            v("CP n","7","Compare A to immediate byte without changing A.","Сравнивает A с константой, не меняя A."),
            v("CP (HL)","7","Compare A with memory at HL.","Сравнивает A с байтом по HL."),
            v("CP (IX+d)/(IY+d)","19","Compare A with indexed memory byte.","Сравнивает A с индексным байтом."));
        add("DAA",Z80FlagEffects.DAA,
            v("DAA","4","Adjust A into packed BCD after addition/subtraction using old N,H,C. Valid for BCD operands.","Коррекция A в упакованный BCD после сложения/вычитания по прежним N,H,C; при BCD-операндах."));
        add("CPL",Z80FlagEffects.CPL,
            v("CPL","4","A ← NOT A. The other four documented flags stay unchanged.","A ← инверсия A. Остальные четыре документированных флага без изменений."));
        add("NEG",Z80FlagEffects.NEG,
            v("NEG","8","A ← 0 − A. C=1 unless A originally zero; P/V=1 for $80.","A ← 0 − A. C=1 кроме A=0; P/V=1 для $80."));
        add("CCF",Z80FlagEffects.CCF,
            v("CCF","4","Invert carry; H receives old C; N clears.","Инвертирует перенос, H получает прежний C, N сбрасывается."));
        add("SCF",Z80FlagEffects.SCF,
            v("SCF","4","Set C, clear H/N. Does not affect S/Z/P/V.","Устанавливает C, сбрасывает H/N, не трогает S/Z/P/V."));
        add("NOP",unchanged,v("NOP","4","No operation; PC advances one byte.","Нет действия, PC переходит к следующему байту."));
        add("HALT",unchanged,v("HALT","4 per refresh cycle","Wait for interrupt/reset, repeatedly performing M1 refresh cycles.","Ожидает прерывания/сброса, выполняя повторные циклы M1/регенерации."));
        add("DI",unchanged,v("DI","4","Clear IFF1 and IFF2, disabling maskable interrupts. F unchanged.","Сбрасывает IFF1 и IFF2, запрещая маскируемые прерывания. F не меняется."));
        add("EI",unchanged,v("EI","4","Set IFF1/IFF2; maskable interrupt acceptance is deferred through the following instruction.","Устанавливает IFF1/IFF2; маскируемое прерывание принимается не раньше завершения следующей команды."));
        add("IM",unchanged,
            v("IM 0","8","Use device-supplied instruction on maskable interrupt.","Выполнить команду, предоставленную внешним устройством."),
            v("IM 1","8","Jump to $0038 on accepted maskable interrupt.","Переход к $0038 при маскируемом прерывании."),
            v("IM 2","8","Fetch vector via I high byte and device low byte, then jump through vector table.","Вектор по I и байту устройства; адрес обработчика читается из таблицы."));
        add("JP",unchanged,
            v("JP nn","10","Load absolute address nn into PC.","Загружает nn в PC."),
            v("JP cc,nn","10","Jump if NZ/Z/NC/C/PO/PE/P/M is true; 10 T either way.","Переходит при NZ/Z/NC/C/PO/PE/P/M; 10 T в обоих случаях."),
            v("JP (HL)","4","PC ← HL; no memory dereference.","PC ← HL; НЕ читает память по адресу HL."),
            v("JP (IX)/(IY)","8","PC ← IX/IY; no memory dereference.","PC ← IX/IY; НЕ читает память по индексному адресу."));
        add("JR",unchanged,
            v("JR e","12","PC ← PC after instruction + signed offset −128..127.","PC ← PC после команды + знаковое смещение −128..127."),
            v("JR NZ/Z/NC/C,e","12 taken / 7 not","Relative conditional branch only on Z or C conditions.","Относительный переход только по условиям Z/C."));
        add("DJNZ",unchanged,v("DJNZ e","13 taken / 8 not","Decrement B then jump on nonzero B; flags untouched.","Уменьшает B и переходит, если B не 0; флаги не меняет."));
        add("CALL",unchanged,
            v("CALL nn","17","Push address of next instruction, then PC ← nn.","Кладёт адрес возврата в стек и переходит к nn."),
            v("CALL cc,nn","17 taken / 10 not","Conditional call; the return address is pushed only if taken.","Условный вызов; адрес возврата помещается в стек только при переходе."));
        add("RET",unchanged,
            v("RET","10","Pop return address from stack into PC.","Снимает адрес возврата из стека в PC."),
            v("RET cc","11 taken / 5 not","Return only if condition is met; when false SP is unchanged.","Возврат при выполнении условия; иначе SP не меняется."));
        add("RETI",unchanged,v("RETI","14","Return from an interrupt; signals end-of-interrupt to daisy-chain peripherals.","Возвращается из прерывания; уведомляет периферию daisy-chain об окончании."));
        add("RETN",unchanged,v("RETN","14","Return from NMI and restore IFF1 from IFF2; F unchanged.","Возврат из NMI с восстановлением IFF1 из IFF2; F неизменён."));
        add("RST",unchanged,v("RST $00/$08/$10/$18/$20/$28/$30/$38","11",
            "One-byte CALL to one of eight fixed low-memory vectors; pushes return PC.",
            "Однобайтный CALL на один из восьми векторов ПЗУ; кладёт PC возврата в стек."));
        add("PUSH",unchanged,
            v("PUSH AF/BC/DE/HL","11","SP decreases by two; store high then low register byte in memory.","SP уменьшается на 2; пара записывается в стек, младший байт по новому SP."),
            v("PUSH IX/IY","15","Push index register onto stack in little-endian order.","Помещает IX/IY в стек младшим байтом по младшему адресу."));
        add("POP",unchanged,
            f("POP AF","10","Load A and F from the two bytes on stack; all status flags are restored.","Загружает A и F из стека; все флаги восстановлены.",Z80FlagEffects.POP_AF),
            v("POP BC/DE/HL","10","Read a register pair from (SP) and (SP+1), then SP += 2.","Загружает пару из (SP),(SP+1), затем SP += 2."),
            v("POP IX/IY","14","Read index register from stack, then SP += 2.","Извлекает индексный регистр из стека, затем SP += 2."));
        add("EX",unchanged,
            v("EX DE,HL","4","Swap DE and HL without accessing memory.","Обмен DE и HL без обращения к памяти."),
            f("EX AF,AF'","4","Swap main A,F pair with shadow A',F', including all flags.","Обмен основной пары A,F с альтернативной A',F', включая флаги.",Z80FlagEffects.EX_AF),
            v("EX (SP),HL","19","Exchange HL with two bytes at top of stack, SP itself unchanged.","Обмен HL с двумя байтами по вершине стека; SP неизменён."),
            v("EX (SP),IX/IY","23","Exchange IX or IY with word at (SP).","Обмен IX/IY со словом по (SP)."));
        add("EXX",unchanged,v("EXX","4","Swap BC/DE/HL with shadow BC'/DE'/HL'. AF is not exchanged.","Обменивает BC/DE/HL с альтернативными; AF не затрагивается."));
        add("BIT",Z80FlagEffects.BIT,
            v("BIT b,r","8","Check bit b (0..7) of a register; operand unchanged.","Проверяет бит b (0..7) регистра, не изменяя его."),
            v("BIT b,(HL)","12","Read and test bit b of byte at HL.","Читает и проверяет бит b по HL."),
            v("BIT b,(IX+d)/(IY+d)","20","Test bit b of indexed byte. Undocumented F3/F5 reflect effective address high byte.","Проверяет бит индексного байта; недокументированные F3/F5 берутся из старшего байта адреса."));
        add("SET",unchanged,
            v("SET b,r","8","Set bit b of 8-bit register to 1.","Устанавливает бит b регистра в 1."),
            v("SET b,(HL)","15","Set one bit of byte at HL; write byte back.","Устанавливает бит в байте по HL с записью назад."),
            v("SET b,(IX+d)/(IY+d)","23","Set bit of indexed memory byte.","Устанавливает бит индексного байта."));
        add("RES",unchanged,
            v("RES b,r","8","Clear bit b of a register to 0.","Сбрасывает бит b регистра в 0."),
            v("RES b,(HL)","15","Clear bit in memory at HL.","Сбрасывает бит в байте по HL."),
            v("RES b,(IX+d)/(IY+d)","23","Clear bit in indexed memory byte.","Сбрасывает бит в индексном байте."));
        add("RLC/RRC/RL/RR",Z80FlagEffects.ROTATE,
            v("RLC r / RRC r","8","Rotate bit 7 to bit 0 (RLC), or bit 0 to bit 7 (RRC). Outgoing bit → C.","Циклический поворот влево RLC или вправо RRC; вытесненный бит → C."),
            v("RL r / RR r","8","Rotate through carry: old C enters one edge, outgoing bit enters C.","Поворот через перенос: прежний C входит, вытесненный бит уходит в C."),
            v("RLC/RRC/RL/RR (HL)","15","Rotate the byte at HL and write it back.","Поворачивает байт по HL с записью обратно."),
            v("RLC/RRC/RL/RR (IX+d)/(IY+d)","23","Rotate indexed byte. This is the memory-target form.","Поворачивает байт по индексному адресу."));
        add("SLA/SRA/SRL",Z80FlagEffects.ROTATE,
            v("SLA r","8","Shift left, zero fills bit 0; old bit 7 → C.","Сдвиг влево с нулём в бите 0; старый бит 7 → C."),
            v("SRA r","8","Arithmetic shift right, preserving old sign bit 7.","Арифметический сдвиг вправо с сохранением знакового бита 7."),
            v("SRL r","8","Logical shift right, zero fills bit 7.","Логический сдвиг вправо с нулём в бите 7."),
            v("SLA/SRA/SRL (HL)","15","Read-modify-write byte at HL.","Сдвигает байт по HL с записью обратно."),
            v("SLA/SRA/SRL (IX+d)/(IY+d)","23","Indexed read-modify-write shift.","Сдвигает индексный байт с записью обратно."));
        add("RLCA/RRCA/RLA/RRA",Z80FlagEffects.ACC_ROTATE,
            v("RLCA","4","Rotate A left circularly. Outgoing bit 7 → C and bit 0.","Циклический поворот A влево; бит 7 → C и бит 0."),
            v("RRCA","4","Rotate A right circularly. Outgoing bit 0 → C and bit 7.","Циклический поворот A вправо; бит 0 → C и бит 7."),
            v("RLA","4","Rotate A left through carry, old C → bit 0.","Поворот A влево через перенос, прежний C → бит 0."),
            v("RRA","4","Rotate A right through carry, old C → bit 7.","Поворот A вправо через перенос, прежний C → бит 7."));
        add("RLD",Z80FlagEffects.RLD,v("RLD","18","Rotate low nibble of A through two nibbles of (HL) to the left; high nibble of A unchanged.","Циклический сдвиг младшей тетрады A и двух тетрад (HL) влево; старшая тетрада A неизменна."));
        add("RRD",Z80FlagEffects.RLD,v("RRD","18","Rotate low nibble of A and the two nibbles of (HL) to the right.","Циклический сдвиг младшей тетрады A и двух тетрад (HL) вправо."));
        add("IN",Z80FlagEffects.INPUT,
            f("IN A,(n)","11","Read from port with high byte = current A, low byte = immediate n; flags unchanged.","Читает порт с A в старшем байте адреса и n в младшем; флаги неизменны.",unchanged),
            v("IN r,(C)","12","Read from 16-bit port BC into register, updating S/Z/PV from byte.","Читает порт BC в регистр; S/Z/PV зависят от полученного байта."),
            v("IN (C)","12","Undocumented input, discards byte but sets S/Z/PV like IN r,(C).","Недокументированное чтение без сохранения байта; S/Z/PV как для IN r,(C)."));
        add("OUT",unchanged,
            v("OUT (n),A","11","Output A to port whose high address byte = A and low byte = n.","Выводит A в порт со старшим байтом адреса A и младшим n."),
            v("OUT (C),r","12","Output register to 16-bit port BC.","Выводит регистр в 16-битный порт BC."),
            v("OUT (C),0","12","Undocumented zero output form; supported on real Z80.","Недокументированный вывод нуля в порт BC на реальном Z80."));
        add("LDI",Z80FlagEffects.LDI,
            v("LDI","16","(DE) ← (HL); HL++, DE++, BC--; P/V=1 while BC ≠ 0.","(DE) ← (HL); HL++, DE++, BC--; P/V=1, пока BC ≠ 0."));
        add("LDIR",Z80FlagEffects.LDI,
            v("LDIR","21 repeat / 16 last","Repeat LDI while BC≠0. 21 T per continued iteration; 16 T final.","Повторяет LDI, пока BC≠0. По 21 T до последней итерации, последняя 16 T."));
        add("LDD",Z80FlagEffects.LDI,
            v("LDD","16","(DE) ← (HL); HL--, DE--, BC--.","(DE) ← (HL); HL--, DE--, BC--."));
        add("LDDR",Z80FlagEffects.LDI,
            v("LDDR","21 repeat / 16 last","Repeat LDD until BC reaches zero; backwards block copy.","Повторяет LDD до BC=0; копирование от старших адресов к младшим."));
        add("CPI",Z80FlagEffects.CPI,
            v("CPI","16","Compute A − (HL) for flags; HL++, BC--; preserve A and carry.","Сравнивает A − (HL) для флагов; HL++, BC--; A и C сохраняет."));
        add("CPIR",Z80FlagEffects.CPI,
            v("CPIR","21 repeat / 16 last","Repeat CPI while BC≠0 and no match (Z=0).","Повторяет CPI, пока BC≠0 и совпадения нет (Z=0)."));
        add("CPD",Z80FlagEffects.CPI,
            v("CPD","16","Compare A against (HL), then decrement HL and BC.","Сравнивает A с (HL), после чего уменьшает HL и BC."));
        add("CPDR",Z80FlagEffects.CPI,
            v("CPDR","21 repeat / 16 last","Repeat CPD backwards while BC≠0 and Z=0.","Повторяет CPD назад, пока BC≠0 и Z=0."));
        add("INI/IND",Z80FlagEffects.BLOCK_IO,
            v("INI","16","Read byte from port BC into (HL); increment HL, decrement B.","Читает байт из порта BC в (HL), увеличивает HL, уменьшает B."),
            v("IND","16","Read byte from port BC into (HL); decrement HL and B.","Читает байт из порта BC в (HL), уменьшает HL и B."));
        add("INIR/INDR",Z80FlagEffects.BLOCK_IO,
            v("INIR","21 repeat / 16 last","Repeat INI until B=0; I/O flags follow special block-input rules.","Повторяет INI до B=0; флаги по особым правилам блочного ввода."),
            v("INDR","21 repeat / 16 last","Repeat IND until B=0.","Повторяет IND до B=0."));
        add("OUTI/OUTD",Z80FlagEffects.BLOCK_IO,
            v("OUTI","16","Output (HL) to port BC, then increment HL and decrement B.","Выводит (HL) в порт BC, затем увеличивает HL и уменьшает B."),
            v("OUTD","16","Output (HL) to port BC, then decrement HL and B.","Выводит (HL) в порт BC, затем уменьшает HL и B."));
        add("OTIR/OTDR",Z80FlagEffects.BLOCK_IO,
            v("OTIR","21 repeat / 16 last","Repeat OUTI until B=0.","Повторяет OUTI до B=0."),
            v("OTDR","21 repeat / 16 last","Repeat OUTD until B=0.","Повторяет OUTD до B=0."));
        add("ORG",unchanged,v("ORG expression","—","Assembler directive: set the assembly address, not executed by Z80.","Директива ассемблера: задаёт адрес сборки, CPU её не выполняет."));
        add("EQU",unchanged,v("name EQU expression","—","Assign an assembly-time numeric constant to a symbol.","Присваивает символу константу на этапе сборки."));
        add("DB",unchanged,
            v("DB value,...","—","Emit one or more 8-bit values into program image.","Помещает один или несколько байтов в образ программы."),
            v("DB \"text\"","—","Emit character bytes of a quoted string.","Помещает байты строковой константы."));
        add("DW",unchanged,v("DW value,...","—","Emit 16-bit words little-endian (low byte first).","Помещает 16-битные слова младшим байтом вперёд."));
        add("DS",unchanged,v("DS count[,fill]","—","Reserve count bytes, optionally fill with constant byte.","Резервирует count байтов с необязательным заполнением."));
    }

    public static List<Variant> variants(InstructionReference.Entry entry) {
        List<Variant> result=CATALOG.get(entry.mnemonic);
        if(result==null)throw new IllegalArgumentException("No expanded instruction data: "+entry.mnemonic);
        return result;
    }

    public static int documentedFamilyCount() { return CATALOG.size(); }
    private InstructionDetails(){}
}
