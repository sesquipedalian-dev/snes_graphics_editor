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
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * TileCharacterCanvasController 
 */
public class TileCharacterCanvasController {
    public static final int TILES_PER_ROW = 16;

    private ChoiceBox zoomSel;
    private Canvas canvas;
    private PaletteCanvasController paletteCanvasController;
    // upper left tile of data?
    private int selectedTileRow = 0;
    private int selectedTileCol = 0;

    public TileCharacterCanvasController(ChoiceBox zoomSel, Canvas canvas, PaletteCanvasController paletteCanvasController) {
        this.zoomSel = zoomSel;
        this.canvas = canvas;
        this.paletteCanvasController = paletteCanvasController;

        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(
            javafx.util.Duration.millis(1000 / 60), // tick at 60 hz
            event -> draw(event)
        );
        timeline.getKeyFrames().setAll(keyFrame);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        canvas.addEventHandler(
            MouseEvent.MOUSE_CLICKED,
            event -> handleMouse(event)
        );
    }

    private int pixelsPerRow() {
        int defaultPixPerRow = TILES_PER_ROW * TileCHR.TILE_DIM;

        switch(zoomSel.getSelectionModel().getSelectedIndex()) {
            case 1:
                return defaultPixPerRow / 2;
            case 2:
                return defaultPixPerRow / 4;
            case 3:
                return defaultPixPerRow / 8;
            case 4:
                return defaultPixPerRow / 16;
            default:
                return defaultPixPerRow;
        }
    }

    private void handleMouse(MouseEvent e) {
        if(e.getButton() == MouseButton.PRIMARY) {
            // left click = select different palette / color
            int mouseX = (int) e.getX();
            int mouseY = (int) e.getY();

            int x = mouseY / pixelsPerRow();
            int y = mouseX / pixelsPerRow();

            EditingData ed = EditingData.getInstance();

        }
    }

    private void draw(ActionEvent event) {
        // clear the canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);
        gc.rect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setLineWidth(2);

        int zoomSelection = zoomSel.getSelectionModel().getSelectedIndex();

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

            int pixelSize = (int) canvas.getWidth() / pixelsPerRow();
            TileCHR currentTile = null;

            for(int x = 0; x < pixelsPerRow(); ++x) {
                for (int y = 0; y < pixelsPerRow(); ++y) {
                    int rectX = x * pixelSize;
                    int rectY = y * pixelSize;

                    // when we get into the next tile load it up
                    if(y % TileCHR.TILE_DIM == 0) {
                        try {
                            currentTile = ed.getTile(selectedTileRow + (x % TileCHR.TILE_DIM), selectedTileCol + (y % TileCHR.TILE_DIM));
                        } catch(Exception e) {
                            // ignore tiles we can't find
                        }
                    }

                    Color tileColor;
                    if(currentTile != null) {
                        int paletteIndex = currentTile.getColorSelected(x % TileCHR.TILE_DIM, y % TileCHR.TILE_DIM);
                        tileColor = palette.getColor(paletteIndex);
                    } else {
                        tileColor = new Color(0, 0, 0, 0);
                    }
                    gc.setStroke(tileColor);
                    gc.setFill(tileColor);
                    gc.fillRect(rectX, rectY, pixelSize, pixelSize);


                    // only show pixel grid for sufficiently zoomed modes
                    if(zoomSelection > 1) {
                        gc.setStroke(Color.BROWN);
                        gc.strokeRect(rectX, rectY, pixelSize, pixelSize);
                    }
                }
            }

            // show base tile outlines on indices that line up with those
            for(int x = 0; x < pixelsPerRow(); ++ x) {
                for (int y = 0; y < pixelsPerRow(); ++y) {
                    int rectX = x * pixelSize;
                    int rectY = y * pixelSize;

                    if ((zoomSelection < 4) &&
                        ((x % TileCHR.TILE_DIM) == 0) &
                        ((y % TileCHR.TILE_DIM) == 0)
                    ) {
                        gc.setStroke(Color.YELLOW);
                        gc.strokeRect(rectX, rectY, pixelSize * TileCHR.TILE_DIM, pixelSize * TileCHR.TILE_DIM);
                    }
                }
            }
        }
    }
}
