package com.alexzab.z80pocketide.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NumberBaseConverterTest {
    @Test
    public void convertsAllThreeBases() {
        NumberBaseConverter.Values fromDec =
                NumberBaseConverter.convert("255", NumberBaseConverter.Base.DEC);
        assertEquals("255", fromDec.decimal);
        assertEquals("$FF", fromDec.hexadecimal);
        assertEquals("%11111111", fromDec.binary);

        NumberBaseConverter.Values fromHex =
                NumberBaseConverter.convert("$8000", NumberBaseConverter.Base.HEX);
        assertEquals("32768", fromHex.decimal);
        assertEquals("$8000", fromHex.hexadecimal);
        assertEquals("%1000000000000000", fromHex.binary);

        NumberBaseConverter.Values fromBin =
                NumberBaseConverter.convert("0b10101010", NumberBaseConverter.Base.BIN);
        assertEquals("170", fromBin.decimal);
        assertEquals("$AA", fromBin.hexadecimal);
        assertEquals("%10101010", fromBin.binary);
    }

    @Test
    public void acceptsAssemblerPrefixesAndSeparators() {
        assertEquals("65535",
                NumberBaseConverter.convert("#FF_FF", NumberBaseConverter.Base.HEX).decimal);
        assertEquals("$A",
                NumberBaseConverter.convert("%1010", NumberBaseConverter.Base.BIN).hexadecimal);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsValuesBeyond64Bits() {
        NumberBaseConverter.convert("$1FFFFFFFFFFFFFFFF", NumberBaseConverter.Base.HEX);
    }
}
