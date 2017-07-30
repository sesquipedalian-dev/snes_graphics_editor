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
package com.github.sesquipedalian_dev.snes_graphics_edit;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.MatchResult;

/**
 * EditingData
 * Container for the things the user can edit using this program.  We'll have some number of palettes,
 * some tile space we're manipulating, etc.
 */
public class EditingData {
    public EditingData(int bitDepth) {
        this.bitDepth = bitDepth;
        this.palettes = new ArrayList<>(maxPalettes());
        this.tiles = new Stack<>();
        this.filename = "";
    }

    private String filename;
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getFilename() {
        return filename;
    }

    public static EditingData fromFile(String filename) throws IOException {
        EditingData retVal = null;

        File f = new File(filename);
        String shortName = f.getName().replaceAll("\\..*$", "");

        FileInputStream fIn = new FileInputStream(f);
        BufferedReader in = new BufferedReader(new InputStreamReader(fIn));
        int state = 0;
        int nextTileY = 0;
        String line;
        while((line = in.readLine()) != null) {
            Scanner s = new Scanner(line);
            if(state == 0) { // haven't found starting data yet
                String found = s.findInLine("; bitDepth: (\\d+)");
                if(found != null) {
                    MatchResult r = s.match();
                    String group = r.group(1);
                    int bitDepth = Integer.parseInt(group);

                    retVal = new EditingData(bitDepth);
                    state = 1;
                }
            } else if(state == 1) { // found bit depth
                String found = s.findInLine("(" + shortName + "Palettes:|" + shortName + "Tiles:)");
                if(found != null) {
                    MatchResult r = s.match();
                    String group = r.group(1);
                    if(group.equals(shortName + "Palettes:")) {
                        // start parsing palettes
                        state = 2;
                    } else if (group.equals(shortName + "Tiles:")) {
                        // start parsing tiles
                        state = 3;
                    }
                }
            } else if (state == 2) { // parsing palettes
                if(line.matches("^\\s*\\.db ")) {
                    // if we find a data row, add it as a palette
                    Palette p = Palette.fromString(new Scanner(line), retVal.getBitDepth());
                    retVal.addPalette(p);
                } else if (line.matches("^\\s*;end")) {
                    // if we get to the end, go back to looking for the next data type
                    state = 1;
                }
            } else if (state == 3) { // parsing tiles
                if(line.matches("; tile row \\d+")) {
                    // if we get a new row, add the new row
                    retVal.addTileRow();
                    nextTileY = 0;
                } else if (line.matches("^\\s*\\.db")) {
                    // if we get a new tile data, add it
                    Tile t = Tile.fromString(new Scanner(line), retVal.getBitDepth());
                    retVal.setTile(retVal.getTileRows() - 1, nextTileY, t);
                    nextTileY++;
                } else if (line.matches("^\\s*;end")) {
                    // if we get to the end, go back to looking for next data type
                    state = 1;
                }
            }
        }

        retVal.filename = filename;
        return retVal;
    }

    public void toFile(String filename) throws IOException {
        File f = new File(filename);
        String shortName = f.getName().replaceAll("\\..*$", "");

        FileOutputStream fOut = new FileOutputStream(f);
        PrintStream out = new PrintStream(fOut);
        out.println("; Created by snes graphics editor https://github.com/sesquipedalian-dev/snes_graphics_editor ;");
        out.println("; bitDepth: " + bitDepth);
        out.println(";");
        out.println();

        out.println(shortName + "Palettes:");
        out.println();
        for(Palette p : palettes) {
            p.serializeToStream(out);
        }
        out.println(";end");

        out.println(shortName + "Tiles:");
        out.println();
        for(int x = 0; x < tiles.size(); ++x) {
            out.println("; tile row " + x);
            ArrayList<Tile> row = tiles.get(x);

            for(int y = 0; y < TILES_PER_ROW; ++y) {
                out.println("; tile " + ((x * TILES_PER_ROW) + y));
                row.get(y).serializeToStream(out);
            }
            out.println();
        }
        out.println(";end");

        out.println("; Thanks for playing! ;");
    }

    private int bitDepth;
    public int getBitDepth() {
        return bitDepth;
    }

    // max memory available to SNES in CGRAM for a scene (256 colors)
    public final int MAX_PALETTE_MEM = 512;

    private ArrayList<Palette> palettes;

    public int maxPalettes() {
        return MAX_PALETTE_MEM / bitDepth / 2;
    }

    public void addPalette(Palette p) {
        if(palettes.size() != maxPalettes()) {
            palettes.add(p);
        }
    }

    public void deletePalette(int index) {
        palettes.remove(index);
    }

    public int currentPalettes() {
        return palettes.size();
    }

    public Palette getPalette(int index) {
        return palettes.get(index);
    }


    // SNES stores this many 8x8 tiles in a 'row' in VRAM.  relevant to wrapping bigger tiles than 8x8
    // e.g. the tiles involved in a 16x16 sprite starting at <tile> are: tile, tile+1, tile+16, tile+17.
    public final int TILES_PER_ROW = 16;
    private Stack<ArrayList<Tile>> tiles;

    public int getTileRows() {
        return tiles.size();
    }

    public void addTileRow() {
        ArrayList<Tile> newRow = new ArrayList<Tile>(TILES_PER_ROW);
        for(int i = 0; i < TILES_PER_ROW; ++i) {
            Tile newTile = new Tile(bitDepth);
            newRow.add(newTile);
        }
        tiles.push(newRow);
    }

    public void subtractTileRow() {
        tiles.pop();
    }

    // 0-indexed
    public Tile getTile(int x, int y) {
        if(x < 0 || x > getTileRows()) {
            throw new IndexOutOfBoundsException("X between [0, " + getTileRows() + ")");
        }
        if(y < 0 || y > TILES_PER_ROW) {
            throw new IndexOutOfBoundsException("Y between [0, " + TILES_PER_ROW + ")");
        }
        return tiles.get(x).get(y);
    }

    // 0-indexed
    public void setTile(int x, int y, Tile t) {
        if(x < 0 || x > getTileRows()) {
            throw new IndexOutOfBoundsException("X between [0, " + getTileRows() + ")");
        }
        if(y < 0 || y > TILES_PER_ROW) {
            throw new IndexOutOfBoundsException("Y between [0, " + TILES_PER_ROW + ")");
        }
        tiles.get(x).set(y, t);
    }
}
