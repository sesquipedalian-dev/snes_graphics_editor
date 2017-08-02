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
package com.github.sesquipedalian_dev.snes_graphics_edit.ui;

import com.github.sesquipedalian_dev.snes_graphics_edit.data.EditingData;
import com.github.sesquipedalian_dev.snes_graphics_edit.data.Palette;
import com.github.sesquipedalian_dev.snes_graphics_edit.data.TileCHR;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * UI controller for the UI that displays the tile mem we're manipulating.  This gives a view of the entire
 * tile character data that we're editing.
  */
public class TileMemCanvasController {
    private Canvas canvas;
    private PaletteCanvasController paletteCanvasController;
    private Button minusBtn;
    private Button plusBtn;

    private int selectedTileRow = 0;
    private int selectedTileCol = 0;

    public int getSelectedTileRow() {
        return selectedTileRow;
    }

    public int getSelectedTileCol() {
        return selectedTileCol;
    }

    public TileMemCanvasController(
        Canvas canvas,
        PaletteCanvasController paletteCanvasController,
        Button minusBtn,
        Button plusBtn
    ) {
        this.canvas = canvas;
        this.paletteCanvasController = paletteCanvasController;
        this.minusBtn = minusBtn;
        this.plusBtn = plusBtn;

        // set up loop to call draw periodically
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(
            javafx.util.Duration.millis(1000 / 60), // tick at 60 hz
            event -> draw(event)
        );
        timeline.getKeyFrames().setAll(keyFrame);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        minusBtn.setOnAction(event -> handleMinusBtn(event));
        plusBtn.setOnAction(event -> handlePlusBtn(event));

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleMouseClick(event));
    }

    private void handleMouseClick(MouseEvent e) {
        double mouseX = e.getX();
        double mouseY = e.getY();

        double tileSizeInPx = canvas.getWidth() / EditingData.TILES_PER_ROW;
        int tileX = (int) (mouseX / tileSizeInPx);
        int tileY = (int) (mouseY / tileSizeInPx);

        selectedTileRow = tileX;
        selectedTileCol = tileY;
    }

    private void handleMinusBtn(ActionEvent e) {
        EditingData ed = EditingData.getInstance();
        if(ed != null) {
            ed.subtractTileRow();
        }
    }

    private void handlePlusBtn(ActionEvent e) {
        EditingData ed = EditingData.getInstance();
        if(ed != null) {
            ed.addTileRow();
        }
    }

    private int pixelsPerRow() {
        return 128; // we always show tile mem 16x8
    }

    private void draw(ActionEvent event) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2);

        EditingData ed = EditingData.getInstance();
        if(ed != null) {
            int bitDepth = ed.getBitDepth();
            int selectedPaletteIndex = paletteCanvasController.getSelectedPalette();
            Palette palette= null;
            try {
                palette = ed.getPalette(selectedPaletteIndex);
            } catch (Exception e) {
                // use dummy palette if not available
                palette = new Palette(bitDepth);
            }

            // just draw a big 'ol rect for the rest of the space that won't get filled with tiles
            Color bgColor = palette.getColor(0);
            gc.setStroke(bgColor);
            gc.fillRect(0, 0, canvas.getHeight(), canvas.getWidth());

            double pixelSize = canvas.getWidth() / pixelsPerRow();

            TileCHR currentTile = null;

            int maxPixelsX = ed.getTileRows() * TileCHR.TILE_DIM;
            // draw pixels
            for(int x = 0; x < maxPixelsX; ++x) {
                for (int y = 0; y < pixelsPerRow(); ++y) {
                    double rectX = y * pixelSize;
                    double rectY = x * pixelSize;

                    if(rectX > canvas.getWidth() || rectY > canvas.getHeight()) {
                        // skip stuff that's 'off screen'
                        break;
                    }

                    // when we get into the next tile load it up
                    if(y % TileCHR.TILE_DIM == 0) {
                        try {
                            currentTile = ed.getTile(x / TileCHR.TILE_DIM, y / TileCHR.TILE_DIM);
                        } catch(Exception e) {
                            // ignore tiles we can't find
                            currentTile = null;
                        }
                    }

                    Color tileColor;
                    if(currentTile != null) {
                        int indexInTileX = x % TileCHR.TILE_DIM;
                        int indexInTileY = y % TileCHR.TILE_DIM;
                        int paletteIndex = currentTile.getColorSelected(indexInTileX, indexInTileY);
                        tileColor = palette.getColor(paletteIndex);
                    } else {
                        tileColor = palette.getColor(0);
                    }
                    gc.setStroke(tileColor);
                    gc.setFill(tileColor);
                    gc.fillRect(rectX, rectY, pixelSize, pixelSize);

                }
            }

            // draw tile grid
            gc.setStroke(Color.BROWN);
            for(int x = 0; x < EditingData.TILES_PER_ROW; ++x) {
                for(int y = 0; y < ed.getTileRows(); ++y) {
                    double rectX = x * TileCHR.TILE_DIM * pixelSize;
                    double rectY = y * TileCHR.TILE_DIM * pixelSize;

                    gc.strokeRect(rectX, rectY, TileCHR.TILE_DIM * pixelSize, TileCHR.TILE_DIM * pixelSize);
                }
            }

            // draw selection window
            gc.setLineWidth(3);
            gc.setStroke(Color.WHITE);

            double selectionXStart = selectedTileRow * TileCHR.TILE_DIM * pixelSize;
            double selectionYStart = selectedTileCol * TileCHR.TILE_DIM * pixelSize;
            int selectionSize = 0;
            switch(bitDepth) {
                case 1:
                    selectionSize = 1;
                    break;
                case 2:
                    selectionSize = 2;
                    break;
                case 3:
                    selectionSize = 4;
                    break;
                default:
                    selectionSize = 8;
                    break;
            }
            gc.strokeRect(selectionXStart, selectionYStart, selectionSize * TileCHR.TILE_DIM * pixelSize, selectionSize * TileCHR.TILE_DIM * pixelSize);
        }
    }
}
