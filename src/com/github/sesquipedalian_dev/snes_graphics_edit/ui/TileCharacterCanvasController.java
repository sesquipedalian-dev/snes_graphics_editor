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
            MouseEvent.MOUSE_PRESSED,
            event -> handleMousePressed(event)
        );

        canvas.addEventHandler(
            MouseEvent.MOUSE_DRAGGED,
            event -> handleMouseDragged(event)
        );

        canvas.addEventHandler(
                MouseEvent.MOUSE_RELEASED,
                event -> handleMouseReleased(event)
        );
    }

    private int pixelsPerRow() {
        int defaultPixPerRow = TILES_PER_ROW * TileCHR.TILE_DIM;

        switch(zoomSel.getSelectionModel().getSelectedIndex()) {
            case 1:
                return defaultPixPerRow / 4;
            case 2:
                return defaultPixPerRow / 8;
            case 3:
                return defaultPixPerRow / 16;
            default:
                return defaultPixPerRow / 2;
        }
    }

    private boolean mouseDragEnabled = false;
    private int storedPixelX = -1;
    private int storedPixelY = -1;
    private boolean mouseDragHandled = false;
    private void handleMouseDragged(MouseEvent e) {
        if(mouseDragEnabled) {
            mouseDragHandled = true;

            // switch any pixels the mouse is dragged through to the selected color
            int mouseX = (int) e.getX();
            int mouseY = (int) e.getY();

            int pixelX = mouseY / (((int) canvas.getHeight()) / pixelsPerRow());
            int pixelY = mouseX / (((int) canvas.getWidth()) / pixelsPerRow());

            EditingData ed = EditingData.getInstance();
            int tileX = pixelX / TileCHR.TILE_DIM;
            int tileY = pixelY / TileCHR.TILE_DIM;

            System.out.println(String.format("Mouse dragged event %s; mouseCoord: %d/%d, pixelCoord %d/%d, tileCoord: %d %d",
                    e.toString(), mouseX, mouseY, pixelX, pixelY, tileX, tileY
            ));

            try {
                TileCHR currentTile = ed.getTile(tileX, tileY);
                currentTile.selectColor(pixelX % TileCHR.TILE_DIM, pixelY % TileCHR.TILE_DIM, paletteCanvasController.getSelectedColor());
            } catch(Exception ex) {
            }
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        mouseDragEnabled = false;
        if(!mouseDragHandled) {
            EditingData ed = EditingData.getInstance();
            int tileX = storedPixelX / TileCHR.TILE_DIM;
            int tileY = storedPixelY / TileCHR.TILE_DIM;

            try {
                TileCHR currentTile = ed.getTile(tileX, tileY);
                currentTile.selectColor(
                    storedPixelX % TileCHR.TILE_DIM,
                    storedPixelY % TileCHR.TILE_DIM,
                    paletteCanvasController.getSelectedColor()
                );
            } catch(Exception ex) {
            }
        }
    }

    private void handleMousePressed(MouseEvent e) {
        mouseDragEnabled = true;
        mouseDragHandled = false;

        int mouseX = (int) e.getX();
        int mouseY = (int) e.getY();

        storedPixelX = mouseY / (((int) canvas.getHeight()) / pixelsPerRow());
        storedPixelY = mouseX / (((int) canvas.getWidth()) / pixelsPerRow());
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
                    int rectX = y * pixelSize;
                    int rectY = x * pixelSize;

                    if(rectX > canvas.getWidth() || rectY > canvas.getHeight()) {
                        // skip stuff that's 'off screen'
                        break;
                    }

                    // when we get into the next tile load it up
                    if(y % TileCHR.TILE_DIM == 0) {
                        try {
                            currentTile = ed.getTile(selectedTileRow + (x / TileCHR.TILE_DIM), selectedTileCol + (y / TileCHR.TILE_DIM));
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
