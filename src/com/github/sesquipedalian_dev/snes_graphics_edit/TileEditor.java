/**
 * Copyright 2017-07-28 sesquipedalian.dev@gmail.com, All Rights Reserved.
 */
package com.github.sesquipedalian_dev.snes_graphics_edit;

/**
 * TileEditor
 */
public class TileEditor {
    protected int[][] colors;
    protected int dimension; // how much we want to edit at a time
    protected int whichTile;


    public TileEditor(int dimension) {
        this.dimension = dimension;
        colors = new int[dimension][dimension];
    }
}
