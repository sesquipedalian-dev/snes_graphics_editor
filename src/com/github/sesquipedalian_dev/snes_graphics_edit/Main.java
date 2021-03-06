package com.github.sesquipedalian_dev.snes_graphics_edit;

import com.github.sesquipedalian_dev.snes_graphics_edit.data.EditingData;
import com.github.sesquipedalian_dev.snes_graphics_edit.data.Palette;
import com.github.sesquipedalian_dev.snes_graphics_edit.data.TileCHR;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Scanner;

public class Main extends Application {
    // provide access to the scene for lookups
    public static Scene theScene;
    public static Stage theStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        theStage = primaryStage;
        // stop manual close attempts - make 'em go through the menu
        theStage.setOnCloseRequest(event -> {
            event.consume(); // make the event NOP
        });

        Parent root = FXMLLoader.load(getClass().getResource("fxml/window.fxml"));
        primaryStage.setTitle("SNES Graphics Editor");
        theScene = new Scene(root, 625, 445);
        primaryStage.setScene(theScene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) throws Exception {
        if((args.length > 0) && args[0].equals("-t")) {
            runTests();
        } else {
            launch(args);
        }
    }

    public static void runTests() throws Exception {
        TileCHR tile = new TileCHR(2);
        for(int x = 0; x < TileCHR.TILE_DIM; ++x) {
            tile.selectColor(x, 0, 3);
            tile.selectColor(x, 1, 3);
        }

        tile.serializeToStream(System.out);

        Palette p = new Palette(2);
        p.selectColor(0, Color.BLACK);
        p.selectColor(1, new Color(0, 1, 1, 1));
        p.selectColor(2, new Color(1, 0, 0, 1));
        p.selectColor(3, new Color(1, 1, 0, 1));

        p.serializeToStream(System.out);

        String paletteInput = ".db $00, $00, $E0, $7F, $1F, $00, $FF, $03";

        try {
            Palette p2 = Palette.fromString(new Scanner(paletteInput), 2);
            p2.serializeToStream(System.out);
        } catch (Exception e) {
            System.out.println("exception: " + e + ":" + e.getMessage());
        }


        String tileInput = ".db $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0, $C0";
        try {
            TileCHR tile2 = TileCHR.fromString(new Scanner(tileInput), 2);
            tile2.serializeToStream(System.out);
        } catch (Exception e) {
            System.out.println("exception: " + e);
        }


        // test editing data
        EditingData ed = new EditingData(2);

        Palette p3 = Palette.fromString(new Scanner(paletteInput), 2);
        for(int i = 0; i < ed.maxPalettes(); ++i) {
            ed.addPalette(p3);
        }

        TileCHR t3 = TileCHR.fromString(new Scanner(tileInput), 2);
        ed.addTileRow();
        ed.setTile(0, 0, t3);

        ed.toFile("HornyGoat.inc");

        EditingData ed2 = EditingData.fromFile("HornyGoat.inc");
        System.out.println(String.format("read in editing data: %d %d %d", ed2.getTileRows(), ed.getBitDepth(), ed.currentPalettes()));
        for(int i = 0; i < ed.currentPalettes(); ++i) {
            Palette p4 = ed.getPalette(i);
            for(int c = 0; c < p4.colorsSize(); ++c) {
                System.out.println(String.format("Palette %d Color %d = %s", i, c, p4.getColor(c)));
            }
        }
    }
}
