package de.hsrm.mi.enia.moodplayer.presentation.views;

import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.ControlPane;

import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.TimePane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;/**
 * UI-Klasse für die Player-Ansicht
 *
 * enthält:
 * - Header mit Switch-Button
 * - Rotierendes Vinyl-Icon
 * - Trackinfos
 * - TimePane
 * - ControlPane
 */
public class PlayerView extends BorderPane {

    // Header
    public Button backToPlaylistButton;
    public Button toMoodButton;

    // animiertes Icon
    public StackPane musicIconCircle;
    public Label musicIcon;

    // Now-Playing Infos
    public Label trackTitleLabel;
    public Label trackArtistLabel;
    public Label trackAlbumLabel;

    // Controls
    public ControlPane controlPane;
    public TimePane timePane;

    public PlayerView() {

        // Header - Button mittig, Pfeil rechts
        HBox header = new HBox();
        header.setId("header");
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 10, 20, 10));
        header.setSpacing(10);

        // Spacer links 
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        
        // Button in der Mitte
        toMoodButton = new Button("Mood ändern");
        toMoodButton.getStyleClass().add("nav-button");
        toMoodButton.setAlignment(Pos.CENTER);
        
        // Spacer rechts 
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);
        
        // Pfeil nach unten (rechts)
        backToPlaylistButton = new Button("↓");
        backToPlaylistButton.getStyleClass().add("down-arrow-button");
        
        header.getChildren().addAll(spacerLeft, toMoodButton, spacerRight, backToPlaylistButton);
        

        // Center - Icon + Track Infos (zentriert)
        
        // rotierende Schallplatte
        musicIconCircle = new StackPane();
        musicIconCircle.setMinSize(110, 110);
        musicIconCircle.setPrefSize(110, 110);
        musicIconCircle.setMaxSize(110, 110);
        
        // äußerer Kreis (Schallplatte schwarz)
        Circle vinylOuter = new Circle(55);
        vinylOuter.setFill(Color.web("#1a1a1a"));
        vinylOuter.setStroke(Color.web("#000000"));
        vinylOuter.setStrokeWidth(2);
        
        // mittlerer Ring (Label-Bereich)
        Circle vinylLabel = new Circle(30);
        vinylLabel.setFill(Color.web("#E8D5F2")); // Sanftes Pastell-Lila
        vinylLabel.setStroke(Color.web("#D4BEE4"));
        vinylLabel.setStrokeWidth(1.5);
        
        // innerer Kreis 
        Circle vinylHole = new Circle(8);
        vinylHole.setFill(Color.web("#f0f0f0"));
        vinylHole.setStroke(Color.web("#cccccc"));
        vinylHole.setStrokeWidth(1);
        
        musicIconCircle.getChildren().addAll(vinylOuter, vinylLabel, vinylHole);
        musicIconCircle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0.3, 0, 4);");
        
        musicIcon = new Label(""); // nicht mehr verwendet, aber behalten für Kompatibilität
        
        trackTitleLabel = new Label("Titel");
        trackTitleLabel.getStyleClass().add("main-text");

        trackArtistLabel = new Label("Artist");
        trackAlbumLabel = new Label("Album");

        VBox metaBox = new VBox(8, trackTitleLabel, trackArtistLabel, trackAlbumLabel);
        metaBox.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(20, musicIconCircle, metaBox);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40));
        
        // Bottom
        timePane = new TimePane();
        timePane.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(timePane, Priority.NEVER);

        controlPane = new ControlPane();
        controlPane.setPadding(new Insets(10, 10, 20, 10));
        
        VBox bottomBox = new VBox(10, timePane, controlPane);
        bottomBox.setPadding(new Insets(0, 20, 0, 20));
        bottomBox.setFillWidth(true);
        bottomBox.getStyleClass().add("bottomBox");        

        // BorderPane zusammenschrauben
        setTop(header);
        setCenter(centerBox);
        setBottom(bottomBox);
        setPadding(new Insets(0));
    }
}