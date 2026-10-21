package com.alexzab.z80pocketide.reference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explicit, human-readable architectural Z80 flag effects.
 * F3/F5 (undocumented bits) are intentionally explained separately:
 * their sourcing is instruction-specific and not part of this six-flag table.
 */
public final class Z80FlagEffects {
    private Z80FlagEffects() {}

    public static final String NONE="NONE", ADD8="ADD8", SUB8="SUB8",
            INC8="INC8", DEC8="DEC8", ADD16="ADD16", ADCSBC16_ADD="ADC16",
            ADCSBC16_SUB="SBC16", AND="AND", LOGIC="LOGIC", BIT="BIT",
            ROTATE="ROTATE", ACC_ROTATE="ACC_ROTATE", CP="CP", DAA="DAA",
            NEG="NEG", CPL="CPL", SCF="SCF", CCF="CCF", INPUT="INPUT",
            LDI="LDI", CPI="CPI", BLOCK_IO="BLOCK_IO", RLD="RLD",
            POP_AF="POP_AF", EX_AF="EX_AF", LD_A_IR="LD_A_IR";

    public static String describe(String profile, boolean ru) {
        Map<String,String[]> m = new LinkedHashMap<>();
        put(m, "S", "Sign", "Знак", "Preserved", "Без изменения");
        put(m, "Z", "Zero", "Нуль", "Preserved", "Без изменения");
        put(m, "H", "Half carry", "Полуперенос", "Preserved", "Без изменения");
        put(m, "P/V", "Parity/overflow", "Чётность/переполнение", "Preserved", "Без изменения");
        put(m, "N", "Subtract", "Вычитание", "Preserved", "Без изменения");
        put(m, "C", "Carry", "Перенос", "Preserved", "Без изменения");

        switch(profile) {
            case ADD8:
                result(m, ru); set(m,"H", "Carry out of bit 3", "Перенос из бита 3");
                set(m,"P/V","Signed overflow","Знаковое переполнение");
                set(m,"N","0 (addition)","0 (сложение)");
                set(m,"C","Carry out of bit 7","Перенос из бита 7");
                break;
            case SUB8: case CP:
                result(m, ru); set(m,"H","Borrow from bit 4","Заём из бита 4");
                set(m,"P/V","Signed overflow","Знаковое переполнение");
                set(m,"N","1 (subtraction)","1 (вычитание)");
                set(m,"C","Borrow out of bit 7 (A < operand)","Заём из бита 7 (A меньше операнда)");
                break;
            case INC8:
                result(m, ru); set(m,"H","Carry out of bit 3","Перенос из бита 3");
                set(m,"P/V","1 if old operand = $7F","1, если старое значение $7F");
                set(m,"N","0","0");break;
            case DEC8:
                result(m, ru); set(m,"H","Borrow from bit 4","Заём из бита 4");
                set(m,"P/V","1 if old operand = $80","1, если старое значение $80");
                set(m,"N","1","1");break;
            case ADD16:
                set(m,"H","Carry out of bit 11","Перенос из бита 11");
                set(m,"N","0","0");
                set(m,"C","Carry out of bit 15","Перенос из бита 15");break;
            case ADCSBC16_ADD:
                result(m, ru); set(m,"H","Carry out of bit 11","Перенос из бита 11");
                set(m,"P/V","Signed 16-bit overflow","Знаковое переполнение 16 бит");
                set(m,"N","0","0");
                set(m,"C","Carry out of bit 15","Перенос из бита 15");break;
            case ADCSBC16_SUB:
                result(m, ru); set(m,"H","Borrow from bit 12","Заём из бита 12");
                set(m,"P/V","Signed 16-bit overflow","Знаковое переполнение 16 бит");
                set(m,"N","1","1");
                set(m,"C","Borrow out of bit 15","Заём из бита 15");break;
            case AND:
                result(m, ru); set(m,"H","1","1");parity(m);
                set(m,"N","0","0");set(m,"C","0","0");break;
            case LOGIC:
                result(m, ru); set(m,"H","0","0");parity(m);
                set(m,"N","0","0");set(m,"C","0","0");break;
            case BIT:
                set(m,"S","Tested bit 7, if b=7; otherwise 0","Значение бита 7 при b=7, иначе 0");
                set(m,"Z","1 if tested bit is 0","1, если проверяемый бит равен 0");
                set(m,"H","1","1");set(m,"P/V","Same as Z","Как Z");
                set(m,"N","0","0");break;
            case ROTATE:
                result(m, ru); set(m,"H","0","0");parity(m);
                set(m,"N","0","0");set(m,"C","Bit shifted/rotated out","Вытесненный бит");break;
            case ACC_ROTATE:
                set(m,"H","0","0");set(m,"N","0","0");
                set(m,"C","Bit shifted/rotated out","Вытесненный бит");break;
            case NEG:
                result(m, ru);set(m,"H","Borrow from bit 4","Заём из бита 4");
                set(m,"P/V","1 when old A = $80","1, если старое A = $80");
                set(m,"N","1","1");
                set(m,"C","1 unless old A = 0","1, если старое A не 0");break;
            case CPL:
                set(m,"H","1","1");set(m,"N","1","1");break;
            case SCF:
                set(m,"H","0","0");set(m,"N","0","0");set(m,"C","1","1");break;
            case CCF:
                set(m,"H","Old C","Прежний C");set(m,"N","0","0");
                set(m,"C","Inverse of old C","Инверсия прежнего C");break;
            case INPUT: case RLD:
                result(m, ru);set(m,"H","0","0");parity(m);
                set(m,"N","0","0");break;
            case LDI:
                set(m,"H","0","0");
                set(m,"P/V","1 while BC after decrement ≠ 0","1, пока BC после уменьшения не 0");
                set(m,"N","0","0");break;
            case CPI:
                result(m, ru);set(m,"H","Borrow in A − (HL)","Заём при A − (HL)");
                set(m,"P/V","1 while BC after decrement ≠ 0","1, пока BC после уменьшения не 0");
                set(m,"N","1","1");break;
            case BLOCK_IO:
                set(m,"S","From B after decrement","По B после уменьшения");
                set(m,"Z","1 if B after decrement = 0","1, если B после уменьшения равен 0");
                set(m,"H","From 9-bit intermediate sum","По промежуточной 9-битной сумме");
                set(m,"P/V","Parity of ((sum & 7) XOR B')","Чётность ((сумма & 7) XOR B')");
                set(m,"N","Input/output byte bit 7","Бит 7 переданного байта");
                set(m,"C","Same intermediate carry as H","По переносу промежуточной суммы");break;
            case POP_AF:
                for(String k:m.keySet())set(m,k,"Loaded from stack F byte","Загружается из байта F в стеке");break;
            case EX_AF:
                for(String k:m.keySet())set(m,k,"Exchanged with alternate F'","Обмен с альтернативным F'");break;
            case LD_A_IR:
                result(m,ru);set(m,"H","0","0");
                set(m,"P/V","Value of IFF2","Значение IFF2");set(m,"N","0","0");break;
            case DAA:
                result(m, ru);set(m,"H","BCD correction half-carry","Полуперенос при BCD-коррекции");
                parity(m);set(m,"C","Corrected decimal carry","Десятичный перенос после коррекции");break;
            case NONE: break;
            default: throw new IllegalArgumentException("Unknown flag profile " + profile);
        }
        StringBuilder text=new StringBuilder();
        for(Map.Entry<String,String[]> e:m.entrySet()) {
            String[] v=e.getValue();
            if(text.length()>0)text.append('\n');
            text.append(e.getKey()).append("  ").append(ru?v[3]:v[2]);
        }
        return text.toString();
    }

    private static void put(Map<String,String[]> m,String key,String enLabel,String ruLabel,String en,String ru) {
        m.put(key,new String[]{enLabel,ruLabel,en,ru});
    }
    private static void set(Map<String,String[]> m,String key,String en,String ru) {
        String[] a=m.get(key);a[2]=en;a[3]=ru;
    }
    private static void result(Map<String,String[]> m,boolean ru) {
        set(m,"S","Result bit 7","Бит 7 результата");
        set(m,"Z","1 if result is zero","1, если результат равен 0");
    }
    private static void parity(Map<String,String[]> m) {
        set(m,"P/V","1 for even parity of result","1 при чётном числе единиц");
    }
}
