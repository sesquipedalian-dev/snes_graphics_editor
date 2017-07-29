/**
 * Copyright 2017-07-28 sesquipedalian.dev@gmail.com, All Rights Reserved.
 */
package com.github.sesquipedalian_dev.snes_graphics_edit;

/**
 * Util
 */
public class Util {
    // takes Java repr of one component of an RGB color, and converts to the 5-bit needed for the SNES representation
    public static int javaToSnesColor(double java) {
        int multi = (int) (java * 0x1F);
        return multi & 0x1F; // truncate any remainders; make sure it takes up correct number of bits
    }
}
