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
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ColorPicker;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;


/**
 * PaletteCanvasController 
 */
public class PaletteCanvasController {
    public static final int COLORS_PER_ROW = 16;
    public static final int ROWS_OF_COLORS = 16;
    public static final int CANVAS_SIZE_OF_COLOR = 17;

    private Canvas canvas;
    private ColorPicker colorPicker;
    private int selectedPalette = 0;
    private int selectedColor = 0;
    private boolean enableColorPickerOnChange = true;

    public PaletteCanvasController(Canvas canvas, ColorPicker colorPicker) {
        this.canvas = canvas;
        this.colorPicker = colorPicker;

        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(
            javafx.util.Duration.millis(1000 / 60), // tick at 60 hz
            event -> drawPalette(event)
        );
        timeline.getKeyFrames().setAll(keyFrame);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        canvas.addEventHandler(
            MouseEvent.MOUSE_CLICKED,
            event -> handleMouse(event)
        );

        colorPicker.setOnAction(event -> {
            if(enableColorPickerOnChange) {
                changeSelectedColor();
            }
        });
    }

    private void changeSelectedColor() {
        Color c = colorPicker.getValue();
        EditingData ed = EditingData.getInstance();
        if(ed != null) {
            if(selectedPalette < ed.currentPalettes()) {
                Palette p = ed.getPalette(selectedPalette);
                p.selectColor(selectedColor, c);
            }
        }
    }

    private void handleMouse(MouseEvent e) {
        if(e.getButton() == MouseButton.PRIMARY) {
            // left click = select different palette / color
        } else if (e.getButton() == MouseButton.SECONDARY) {
            // right click = edit color

        }
    }

    private void drawPalette(ActionEvent event) {
        // clear the canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);
        gc.rect(0, 0, canvas.getWidth(), canvas.getHeight());

        EditingData ed = EditingData.getInstance();
        if(ed != null) {
            int bitDepth = ed.getBitDepth();
            int colorsPerPalette = (int) Math.pow(2, bitDepth);

            // group the palettes into 16 rows of 16 columns - this will be the most common subpalette arrangement
            for(int x = 0; x < ROWS_OF_COLORS; ++x) {
                for(int y = 0; y < COLORS_PER_ROW; ++y) {
                    int colorIndex = (x * COLORS_PER_ROW) + y;

                    int paletteIndex = colorIndex / colorsPerPalette;
                    if(paletteIndex >= ed.currentPalettes()) {
                        // if we get passed the available palettes we're done
                        return;
                    }

                    int indexInPalette = colorIndex % colorsPerPalette;
                    Color c = ed.getPalette(paletteIndex).getColor(indexInPalette);

                    boolean isSelectedPalette = (paletteIndex == selectedPalette);
                    boolean isSelectedColor = isSelectedPalette && indexInPalette == selectedColor;

                    int rectX = y * CANVAS_SIZE_OF_COLOR;
                    int rectY = x * CANVAS_SIZE_OF_COLOR;

                    gc.setStroke(c);
                    gc.setFill(c);
                    gc.fillRect(rectX, rectY, CANVAS_SIZE_OF_COLOR, CANVAS_SIZE_OF_COLOR);

                    if(isSelectedPalette) {
                        gc.setStroke(Color.RED);

                        // if first color in palette draw left side
                        if(indexInPalette == 0) {
                            gc.strokeLine(rectX, rectY, rectX, rectY + CANVAS_SIZE_OF_COLOR);
                        }

                        // draw top and bottom sides
                        gc.strokeLine(rectX, rectY, rectX + CANVAS_SIZE_OF_COLOR, rectY);
                        gc.strokeLine(rectX, rectY + CANVAS_SIZE_OF_COLOR, rectX + CANVAS_SIZE_OF_COLOR, rectY + CANVAS_SIZE_OF_COLOR);

                        // if last color in palette draw right side
                        if(indexInPalette == ((Math.pow(2, bitDepth)) - 1)) {
                            gc.strokeLine(rectX + CANVAS_SIZE_OF_COLOR, rectY, rectX + CANVAS_SIZE_OF_COLOR, rectY + CANVAS_SIZE_OF_COLOR);
                        }
                    }

                    if(isSelectedColor) {
                        gc.setStroke(Color.WHITE);
                        gc.setLineWidth(1);
                        gc.strokeRect(rectX, rectY + 1, CANVAS_SIZE_OF_COLOR, CANVAS_SIZE_OF_COLOR - 2);

                        // also take this opportunity to make sure the color picker is using the proper color
                        enableColorPickerOnChange = false;
                        colorPicker.setValue(c);
                        enableColorPickerOnChange = true;
                    }
                }
            }
        }
    }
}
