/**
 * Copyright 2017 sesquipedalian.dev@gmail.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.sesquipedalian_dev.snes_graphics_edit.data;

import com.github.sesquipedalian_dev.snes_graphics_edit.util.Util;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.regex.MatchResult;

/**
 * Palette - a set of colors to be used for some tile.  Colors are RGB.  # of colors available dependent on bit depth
 * intended for the data set.
 */
public class Palette {
    protected int bitDepth;
    protected Color colors[];
    protected int colorsSize;

    public Palette(int bitDepth) {
        this.bitDepth = bitDepth;

        colorsSize = (int) Math.pow(2, bitDepth);
        this.colors = new Color[colorsSize];
        for(int i = 0; i < colorsSize; ++i) {
            // start all colors black
            this.colors[i] = new Color(0, 0, 0, 1);
        }
    }

    // 0-indexed color selection
    public void selectColor(int index, Color color) {
        if(index < 0 || index >= colorsSize) {
            throw new IndexOutOfBoundsException("Palette index should be 0 < INDEX < (2 ^ bit depth) ({" + colorsSize + "})");
        }

        colors[index] = color;
    }

    public Color getColor(int index) {
        return colors[index];
    }

    public void serializeToStream(PrintStream out) {
        for(int i = 0; i < colorsSize; ++i) {
            // at beginning of line tell the assembler we got the datas
            if(i % 16 == 0) {
                out.print(".db ");
            }

            Color current = colors[i];

            // get the 5 bit values the SNES uses for the colors
            int b5bit = Util.javaToSnesColor(current.getBlue());
            int g5bit = Util.javaToSnesColor(current.getGreen());
            int r5bit = Util.javaToSnesColor(current.getRed());

            // pack them into 2 bytes
            int color16bit = (b5bit << 10) | (g5bit << 5) | (r5bit);

            // print out the LSB first then MSB
            out.print(String.format("$%02X, ", color16bit & 0xFF));
            out.print(String.format("$%02X", (color16bit & 0xFF00) >> 8));

            // unless last color or end of a block of 16, newline
            if((i % 16 == 15) || (i == colorsSize - 1)) {
                out.print("\n");
            } else {
                out.print(", ");
            }
        }
    }

    public static Palette fromString(Scanner in, int bitDepth) throws IOException {
        Palette retVal = new Palette(bitDepth);

        // build the regex where we'll pull our info from a line
        // basically $<hex value>, repeated a bunch of times
        StringBuilder regex = new StringBuilder();
        regex.append("^\\s*\\.db\\s*");
        for(int i = 0; i < (retVal.colorsSize * 2) - 1; ++i) {
            regex.append("\\$([A-Fa-f0-9]{2}),\\s*");
        }
        regex.append("\\$([A-Fa-f0-9]{2})\\s*$");

        in.findInLine(regex.toString());
        MatchResult r = in.match();
        for(int i = 1; i <= r.groupCount(); i += 2) {
            try {
                // parse colors data lsb first, then msb
                int lsb = Integer.parseInt(r.group(i), 16);
                int msb = Integer.parseInt(r.group(i + 1), 16);

                Color newColor = Util.snesToJavaColor((msb << 8) & lsb);
                retVal.selectColor(((i + 1) / 2 - 1), newColor);
            } catch(NumberFormatException e) {
                System.out.println("problem parsing a byte {" + e + "}");
            }
        }

        return retVal;
    }
}

