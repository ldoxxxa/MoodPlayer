package de.hsrm.mi.enia.moodplayer.presentation.views;

import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.ControlPane;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.TimePane;
import de.hsrm.mi.enia.moodplayer.business.Track;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * UI-Klasse für die Playlist-Ansicht
 *
 * enthält:
 * - ListView mit Tracks
 * - Mini-Trackinfo
 * - ControlPane
 * - TimePane
 */
public class PlaylistView extends BorderPane {

    // Header Navigation
    public Button backToMoodButton;
    public Button toMoodButton;

    // Player-Button unten im Mini-Info-Bereich
    public Button toPlayerButton;

    // UI-Elemente für Playlist
    public ListView<Track> playlistListView;
    public ProgressIndicator loadingIndicator;

    public ControlPane controlPane;

    // Mini-Trackinfos über den Buttons
    public Label miniTitleLabel;
    public Label miniArtistLabel;
    public TimePane timePane;

    public PlaylistView() {

        /* Header */
        HBox header = new HBox();
        header.setId("header");
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 10, 20, 10));

        // Zurück-Button links
        backToMoodButton = new Button("←");
        backToMoodButton.getStyleClass().add("back-arrow-button");

        // Mood-Button mittig
        toMoodButton = new Button("Mood ändern");
        toMoodButton.getStyleClass().add("nav-button");

        // Spacer links & rechts 
        Region spacerLeft = new Region();
        Region spacerRight = new Region();
        HBox.setHgrow(spacerLeft, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(spacerRight, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(backToMoodButton, spacerLeft, toMoodButton, spacerRight);
        setTop(header);

        /* Center */
        playlistListView = new ListView<>();
        playlistListView.setPlaceholder(new Label("Keine Songs geladen"));

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);

        StackPane centerStack = new StackPane(playlistListView, loadingIndicator);

        VBox centerBox = new VBox(centerStack);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));
        setCenter(centerBox);

        /* Bottom */
        controlPane = new ControlPane();

        miniTitleLabel = new Label("Kein Song ausgewählt");
        miniTitleLabel.getStyleClass().add("mini-track-title");

        miniArtistLabel = new Label("");
        miniArtistLabel.getStyleClass().add("mini-track-artist");

        timePane = new TimePane();

        toPlayerButton = new Button("↑");
        toPlayerButton.getStyleClass().add("player-arrow-button");

        VBox trackInfo = new VBox(2, miniTitleLabel, miniArtistLabel);
        trackInfo.setAlignment(Pos.CENTER_LEFT);

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, javafx.scene.layout.Priority.ALWAYS);

        HBox miniInfoTop = new HBox(10, trackInfo, spacerBottom, toPlayerButton);
        miniInfoTop.setAlignment(Pos.CENTER_LEFT);

        VBox miniInfoBox = new VBox(8, miniInfoTop, timePane);
        miniInfoBox.setPadding(new Insets(8, 12, 6, 12));
        miniInfoBox.setId("mini-info-box");

        VBox bottomBox = new VBox(6, miniInfoBox, controlPane);
        bottomBox.getStyleClass().add("bottomBox");
        setBottom(bottomBox);
    }
}