/**
 * Copyright 2017-07-28 sesquipedalian.dev@gmail.com, All Rights Reserved.
 */
package com.github.sesquipedalian_dev.snes_graphics_edit.util;

import javafx.scene.paint.Color;

/**
 * Util
 */
public class Util {
    // takes Java repr of one component of an RGB color, and converts to the 5-bit needed for the SNES representation
    public static int javaToSnesColor(double java) {
        int multi = (int) (java * 0x1F);
        return multi & 0x1F; // truncate any remainders; make sure it takes up correct number of bits
    }

    // convert from SNES BGR 5-bit representations to java % RGB
    public static Color snesToJavaColor(int snes) {
        int lsb = snes & 0xFF;
        int msb = (snes & 0xFF00) >> 8;

        // extract the 5-bit color values from the bytes
        int b5bit = (msb & 0x7C) >> 2;
        int g5bit = ((msb & 0x3 ) << 3) | ((lsb & 0xE0) >> 5);
        int r5bit = lsb & 0x1F;

        // convert to % values that java color uses
        double bPercent = (float) b5bit / 0x1F;
        double gPercent = (float) g5bit / 0x1F;
        double rPercent = (float) r5bit / 0x1F;

        return new Color(rPercent, gPercent, bPercent, 1);
    }
}
