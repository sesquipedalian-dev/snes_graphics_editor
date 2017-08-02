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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.regex.MatchResult;

/*
 * The smallest component of sprite / background tile data in SNES. These 8x8 'character data' can make up
 * bigger sprites by tiling (e.g. 8x8, 16x16, 32x32, or 64x64).
 * CHR data selects a color index into some palette.  The number of colors in the palette is specified by the
 * bitDepth, which varies depending on how the tile is used (BG modes, sprites).
 *
 */
public class TileCHR {
    // implied from size of values in colorSelected?  probably better to truncate if color is too big,
    // or perhaps mod by the depth
    protected int bitDepth;
    protected int colorSelected[][];

    // SNES tiles are 8x8
    public static final int TILE_DIM = 8;
    public TileCHR(int bitDepth) {
        this.bitDepth = bitDepth;
        this.colorSelected = new int[TILE_DIM][TILE_DIM];
    }

    public int getColorSelected(int x, int y) {
        if(x >= TILE_DIM || y >= TILE_DIM || x < 0 || y < 0) {
            throw new IndexOutOfBoundsException("Pick a position within 0 < POSITION < TILE_DIM: " + TILE_DIM + "\n");
        }
        return colorSelected[x][y];
    }

    // set a specified pixel in this tile.  color is modded if it exceeds the amount allowed by bit depth.
    // the color that is actually set is returned.  Use 0-based x / y, 1-based color
    public int selectColor(int x, int y, int color) {
        if(x >= TILE_DIM || y >= TILE_DIM || x < 0 || y < 0) {
            throw new IndexOutOfBoundsException("Pick a position within 0 < POSITION < TILE_DIM: " + TILE_DIM + "\n");
        }
        int trueColor = (color) % (1 << (bitDepth));
        colorSelected[x][y] = trueColor;
        return trueColor;
    }

    public void serializeToStream(PrintStream out) {
        out.print(".db ");
        for (int x = 0; x < TILE_DIM; ++x) {
            for (int plane = 0; plane < bitDepth; ++plane) {
                byte accum = 0;
                for (int y = 0; y < TILE_DIM; ++y) {
                    int shiftBack = TILE_DIM - y - 1 - plane;
                    if(shiftBack < 0) {
                        accum |= (colorSelected[x][y] & (1 << plane)) >> -shiftBack;
                    } else {
                        accum |= (colorSelected[x][y] & (1 << plane)) << shiftBack;
                    }
                }

                out.print(String.format("$%02X", accum));
                if(!((plane == (bitDepth - 1)) && (x == (TILE_DIM - 1)))) {
                    out.print(", ");
                }
            }
        }
        out.print("\n");
    }

    public static TileCHR fromString(Scanner in, int bitDepth) throws IOException {
        TileCHR retVal = new TileCHR(bitDepth);

        // build a regex, basically just looking for a number of bytes equal to
        // TILE_DIM * bitDepth
        StringBuilder regex = new StringBuilder();
        regex.append("^.db ");
        for(int x = 0; x < TILE_DIM; ++x) {
            for (int plane = 0; plane < bitDepth; ++plane) {
                regex.append("\\$([A-Fa-f0-9]{2})");
                if(!((plane == (bitDepth - 1)) && (x == (TILE_DIM - 1)))) {
                    regex.append(",\\s*");
                }
            }
        }

        in.findInLine(regex.toString());
        MatchResult r = in.match();

        // get back the rows
        for(int x = 0; x < TILE_DIM; ++x) {

            // get all the planes for this row so we can calculate the color values at each y
            int[] planes = new int[bitDepth];
            for(int plane = 0; plane < bitDepth; ++plane) {
                String bString = r.group((x * bitDepth) + plane + 1);
                int b = Integer.parseInt(bString, 16);
                planes[plane] = b;
            }

            for(int y = 0; y < TILE_DIM; ++y) {
                byte accum = 0;
                for(int plane = 0; plane < bitDepth; ++plane) {
                    // bit at yMask of plane has one of our bits
                    int yMask = 1 << (TILE_DIM - 1 - y);
                    // move the bit back to position in the actual color value (based on which plane we parsing)
                    int shiftBackToPlane = TILE_DIM - 1 - y - plane;
                    int addToAccum;
                    if(shiftBackToPlane > 0) {
                        addToAccum = (planes[plane] & yMask) >> shiftBackToPlane;
                    } else {
                        addToAccum = (planes[plane] & yMask) << -shiftBackToPlane;
                    }
                    accum |= addToAccum;
                }

                // store the color we found; the method format takes 1-indexed values instead
                retVal.selectColor(x, y, accum);
            }
        }

        return retVal;
    }
}
