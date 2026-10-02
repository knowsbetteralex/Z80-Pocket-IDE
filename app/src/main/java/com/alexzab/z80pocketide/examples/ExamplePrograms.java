package com.alexzab.z80pocketide.examples;

public final class ExamplePrograms {
    private ExamplePrograms() {}

    public static final Example[] ALL = new Example[] {
            new Example(
                    "Border cycle",
                    "Cycles the Spectrum border through all 8 colours",
                    "ORG $8000\n" +
                    "\n" +
                    "START:\n" +
                    "    LD A,0\n" +
                    "LOOP:\n" +
                    "    OUT ($FE),A\n" +
                    "    INC A\n" +
                    "    AND 7\n" +
                    "    LD B,32\n" +
                    "DELAY:\n" +
                    "    DJNZ DELAY\n" +
                    "    JR LOOP\n"
            ),
            new Example(
                    "Rainbow paper",
                    "Fills the 32x24 attribute area with coloured vertical bands",
                    "ORG $8000\n" +
                    "\n" +
                    "    LD HL,$5800\n" +
                    "    LD D,3\n" +
                    "PAGE:\n" +
                    "    LD B,0\n" +
                    "CELL:\n" +
                    "    LD A,L\n" +
                    "    AND 7\n" +
                    "    ADD A,A\n" +
                    "    ADD A,A\n" +
                    "    ADD A,A\n" +
                    "    OR 7\n" +
                    "    LD (HL),A\n" +
                    "    INC HL\n" +
                    "    DJNZ CELL\n" +
                    "    DEC D\n" +
                    "    JR NZ,PAGE\n" +
                    "HOLD:\n" +
                    "    JR HOLD\n"
            ),
            new Example(
                    "Pixel stripes",
                    "Fills the 6144-byte bitmap with an AA/55 stripe pattern",
                    "ORG $8000\n" +
                    "\n" +
                    "    LD HL,$4000\n" +
                    "    LD D,24\n" +
                    "    LD A,$AA\n" +
                    "PAGE:\n" +
                    "    LD B,0\n" +
                    "PIXEL:\n" +
                    "    LD (HL),A\n" +
                    "    XOR $FF\n" +
                    "    INC HL\n" +
                    "    DJNZ PIXEL\n" +
                    "    DEC D\n" +
                    "    JR NZ,PAGE\n" +
                    "HOLD:\n" +
                    "    JR HOLD\n"
            )
    };

    public static final class Example {
        public final String title;
        public final String description;
        public final String source;

        public Example(String title, String description, String source) {
            this.title = title;
            this.description = description;
            this.source = source;
        }
    }
}
