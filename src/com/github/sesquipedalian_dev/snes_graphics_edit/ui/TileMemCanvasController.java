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
import javafx.scene.paint.Color;

/**
 * Created by Scott on 8/1/2017.
 */
public class TileMemCanvasController {
    private Canvas canvas;
    private PaletteCanvasController paletteCanvasController;
    private Button minusBtn;
    private Button plusBtn;

    // TODO manipulate these with the BRICK... MOUSE...
    private int selectedTileRow = 0;
    private int selectedTileCol = 0;

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

            int pixelSize = (int) canvas.getWidth() / pixelsPerRow();
            TileCHR currentTile = null;

            int maxPixelsX = ed.getTileRows() * TileCHR.TILE_DIM;
            for(int x = 0; x < maxPixelsX; ++x) {
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

                    // only show pixel grid for sufficiently zoomed modes
//                    if(zoomSelection > 1) {
//                        gc.setStroke(Color.BROWN);
//                        gc.strokeRect(rectX, rectY, pixelSize, pixelSize);
//                    }
                }
            }
        }
    }
}
