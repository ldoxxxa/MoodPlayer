package de.hsrm.mi.enia.moodplayer.presentation.views;

import javafx.geometry.Insets;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.MoodWheelPane;

public class MoodView extends BorderPane {

    // Navigation Buttons
    public Button backToStartButton;  
    public Button toPlayerButton;
    public Button toPlaylistButton;

    // MoodWheel UI
    public MoodWheelPane moodWheel;
    public Label selectedLabel;
    public Button confirmMoodButton;

    public MoodView() {

        // Header mit Navigation
        HBox header = new HBox();
        header.setId("header");
        header.setAlignment(Pos.CENTER_LEFT);  
        header.setPadding(new Insets(20, 10, 20, 10));
        header.setSpacing(15);

        // Zurück-Button zum Start 
        backToStartButton = new Button("←");
        backToStartButton.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-min-width: 40px; -fx-min-height: 35px; -fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 10; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.5); -fx-border-width: 2; -fx-border-radius: 10;");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Andere Buttons (rechts)
        toPlayerButton = new Button("Player");
        toPlayerButton.getStyleClass().add("nav-button");
        
        toPlaylistButton = new Button("Playlist");
        toPlaylistButton.getStyleClass().add("nav-button");

        header.getChildren().addAll(backToStartButton, spacer, toPlayerButton, toPlaylistButton);

        setTop(header);

        // Info-Sektion 
        Label infoTitle = new Label("Wähle deine Stimmung");
        infoTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0.3, 0, 1);");
        infoTitle.setAlignment(Pos.CENTER);
        
        Label infoText = new Label("Klicke auf einen Mood-Kreis und erhalte eine personalisierte Playlist,\ndie perfekt zu deiner aktuellen Stimmung passt.");
        infoText.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-text-alignment: center; -fx-font-weight: 500; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0.3, 0, 1);");
        infoText.setWrapText(true);
        infoText.setMaxWidth(500);
        infoText.setAlignment(Pos.CENTER);
        
        VBox infoBox = new VBox(8, infoTitle, infoText);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(20, 20, 15, 20));

        // Center: Mood Wheel 
        moodWheel = new MoodWheelPane();
        moodWheel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox centerBox = new VBox();
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(0, 10, 10, 10));
        centerBox.setFillWidth(true);
        VBox.setVgrow(moodWheel, javafx.scene.layout.Priority.ALWAYS);
        
        centerBox.getChildren().addAll(infoBox, moodWheel);

        setCenter(centerBox);

        // Bottom: Selected Label + Button 
        selectedLabel = new Label("Keine Mood ausgewählt");
        selectedLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: white; -fx-font-weight: 700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0.3, 0, 1);");

        confirmMoodButton = new Button("Playlist erstellen");
        confirmMoodButton.getStyleClass().add("confirm-button");

        VBox bottomBox = new VBox(10, selectedLabel, confirmMoodButton);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(10, 20, 25, 20));

        setBottom(bottomBox);

        setPadding(new Insets(0)); // inhalt des MoodViews soll bis zum Rand des Borderpanes reichen
    }
}