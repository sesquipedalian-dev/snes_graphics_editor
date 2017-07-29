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

import javafx.application.Application;
import javafx.fxml.FXML;

/**
 * GUIController
 */
public class GUIController {
    public GUIController() {

    }

    public void initialize() {
        System.out.println("Initializing GUIController");
    }

    @FXML
    public void menuExit() {
        System.out.println("Exit menu");
        Main.theStage.close();
    }

    @FXML
    public void menuNew() {
        System.out.println("New menu");
        Main.editingData = new EditingData(5);
    }

    @FXML
    public void menuSave() {
        System.out.println("Save menu");
    }

    @FXML
    public void menuOpen() {
        System.out.println("Open menu");
    }
}
