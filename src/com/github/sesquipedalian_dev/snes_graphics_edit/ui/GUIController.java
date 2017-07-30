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
import com.github.sesquipedalian_dev.snes_graphics_edit.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

/**
 * GUIController
 */
public class GUIController {
    public final FileChooser.ExtensionFilter SAVE_FILE_FILTER = new FileChooser.ExtensionFilter(
            "assembler include files", "*.inc"
    );

    // singleton pattern
    private static GUIController instance;
    public GUIController getInstance() {
        return instance;
    }
    public GUIController() {
        instance = this;
    }

    class BitDepthListener implements ChangeListener<Number> {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if(enabled) {
                newEditingData(newValue.intValue());
            }
        }

        boolean enabled = true;
    }
    private BitDepthListener bitDepthListener = new BitDepthListener();

    @FXML
    public ChoiceBox bitDepthSel;

    public void initialize() {
        System.out.println("Initializing GUIController");
        ObservableList<String> items = FXCollections.observableArrayList(
            "1 (2 color)", "2 (4 color)", "4 (16 color)", "8 (256 color)"
        );
        bitDepthSel.setItems(items);
        SingleSelectionModel<String> sm = bitDepthSel.getSelectionModel();
        sm.select(0);
        sm.selectedIndexProperty().addListener(bitDepthListener);
    }

    @FXML
    public void menuExit() {
        System.out.println("Exit menu");
        Main.theStage.close();
    }

    @FXML
    public void menuNew() {
        System.out.println("New menu");
        newEditingData(bitDepthSel.getSelectionModel().getSelectedIndex());
    }

    @FXML
    public void menuSave() {
        System.out.println("Save menu");

        EditingData ed = EditingData.getInstance();
        if(ed != null ) {
            String fn = ed.getFilename();
            if (fn != null && !fn.equals("")) {
                try {
                    ed.toFile(fn);
                } catch (IOException e) {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("ERROR!");
                    a.setHeaderText(String.format("Error saving file: %s", fn));
                    a.setContentText(e.toString());
                    a.showAndWait();
                }
            }
        }
    }

    @FXML
    public void menuSaveAs() {
        System.out.println("Save As menu");

        FileChooser saveDialog = new FileChooser();
        saveDialog.setTitle("select location to save");
        saveDialog.setInitialDirectory(new File(System.getProperty("user.dir")));
        saveDialog.setSelectedExtensionFilter(SAVE_FILE_FILTER);

        File f = saveDialog.showSaveDialog(Main.theStage);
        if(f != null) {
            try {
                EditingData ed = EditingData.getInstance();
                if(ed != null) {
                    ed.toFile(f.getAbsolutePath());
                    ed.setFilename(f.getAbsolutePath());

                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Saved!");
                    a.setHeaderText("");
                    a.setContentText(String.format("Succesfully saved to %s", ed.getFilename()));
                    a.showAndWait();
                }
            } catch(IOException e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("ERROR!");
                a.setHeaderText("Error saving file");
                a.setContentText(e.toString());
                a.showAndWait();
            }
        }
    }

    @FXML
    public void menuOpen() {
        System.out.println("Open menu");

        FileChooser openDialog = new FileChooser();
        openDialog.setTitle("Select a file to open");
        openDialog.setInitialDirectory(new File(System.getProperty("user.dir")));
        openDialog.setSelectedExtensionFilter(SAVE_FILE_FILTER);

        File f = openDialog.showOpenDialog(Main.theStage);
        if(f != null) {
            try {
                EditingData newData = EditingData.fromFile(f.getAbsolutePath());

                bitDepthListener.enabled = false;
                switch(newData.getBitDepth()) {
                    case 1:
                        bitDepthSel.getSelectionModel().select(0);
                        break;
                    case 2:
                        bitDepthSel.getSelectionModel().select(1);
                        break;
                    case 4:
                        bitDepthSel.getSelectionModel().select(2);
                        break;
                    case 8:
                        bitDepthSel.getSelectionModel().select(3);
                        break;
                }
                bitDepthListener.enabled = true;
            } catch (IOException e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("ERROR!");
                a.setHeaderText("Error loading file");
                a.setContentText(e.toString());
                a.showAndWait();
            }
        }
    }


    public void newEditingData(int bitDepthSel) {
        // TODO check with user if we should save current data maybe?

        // translate the index to the actual bit depth
        int bitDepth = 1;
        switch(bitDepthSel) {
            case 1:
                bitDepth = 2;
                break;
            case 2:
                bitDepth = 4;
                break;
            case 3:
                bitDepth = 8;
                break;
            default:
                break;
        }

        // make new data to edit
        new EditingData(bitDepth);
    }
}
