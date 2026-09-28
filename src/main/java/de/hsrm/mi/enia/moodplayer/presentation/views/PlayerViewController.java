package de.hsrm.mi.enia.moodplayer.presentation.views;

import de.hsrm.mi.enia.moodplayer.business.Mood;

import de.hsrm.mi.enia.moodplayer.business.MoodPlayer;
import de.hsrm.mi.enia.moodplayer.business.Track;
import de.hsrm.mi.enia.moodplayer.presentation.MoodPlayerGUI;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.LavaBackground;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.TimePane;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.util.Duration;

public class PlayerViewController extends BaseController<PlayerView> {

    Button playButton;
    Button skipButton;
    Button skipBackButton;
    ToggleButton shuffleButton;
    Slider volumeSlider;
    TimePane timePane;

    StackPane musicIconCircle;
    Timeline pulseAnimation;

    Label trackTitleLabel;
    Label trackArtistLabel;
    Label trackAlbumLabel;

    private final MoodPlayer player;
    private final LavaBackground lavaBackground;

    // Event-Handler für Skip-Button
    public class SkipHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            player.skip();
            timePane.reset();
            updateTrackInfo();
            syncPlayPauseIcon();
        }
    }
    
  


    // Konstruktor - initialisiert UI-Elemente und startet Vinyl-Animation
    public PlayerViewController(MoodPlayer player, LavaBackground lavaBackground) {
        root = new PlayerView();

        playButton = root.controlPane.playButton;
        skipButton = root.controlPane.skipButton;
        skipBackButton = root.controlPane.skipBackButton;
        shuffleButton = root.controlPane.shuffleButton;
        volumeSlider = root.controlPane.volumeSlider;
        timePane = root.timePane;

        musicIconCircle = root.musicIconCircle;

        trackTitleLabel = root.trackTitleLabel;
        trackArtistLabel = root.trackArtistLabel;
        trackAlbumLabel = root.trackAlbumLabel;

        this.player = player;
        this.lavaBackground = lavaBackground;

        startPulseAnimation();
        initialize();
    }

    // Bindet alle Event-Listener und Property-Bindings
    @Override
    public void initialize() {

        root.backToPlaylistButton.setOnAction(e -> MoodPlayerGUI.switchRoot("playlistView"));
        root.toMoodButton.setOnAction(e -> MoodPlayerGUI.switchRoot("moodView"));

        syncPlayPauseIcon();
        applyShuffleStyle(player.isShuffleOn(), shuffleButton);

        skipButton.addEventHandler(ActionEvent.ACTION, new SkipHandler());

        playButton.addEventHandler(ActionEvent.ACTION, event -> {
            if (!player.isPlaying()) {
                player.playOrResume();
                updateTrackInfo();
            } else {
                player.pause();
            }
            syncPlayPauseIcon();
        });

        skipBackButton.addEventHandler(ActionEvent.ACTION, event -> {
            player.skipBack();
            timePane.reset();
            updateTrackInfo();
            syncPlayPauseIcon();
        });

        shuffleButton.selectedProperty().addListener((obs, oldValue, newValue) -> {
            player.shuffle(newValue);
            applyShuffleStyle(newValue, shuffleButton);
        });

        volumeSlider.valueProperty().bindBidirectional(player.volumePercentProperty());

        updateTrackInfo();
/*
        player.currentTimeProperty().addListener((obs, oldV, newV) -> {
            if (!timePane.getSlider().isValueChanging()) {
                timePane.setCurrentTime(newV.intValue());
            }
        });*/
        
        player.currentTimeProperty().addListener((obs, oldV, newV) -> {
          
            Platform.runLater(() -> {
                if (!timePane.getSlider().isValueChanging()) {
                    timePane.setCurrentTime(newV.intValue());
                }
            });
        });

        player.currentTrackProperty().addListener((obs, oldT, newT) -> {
            timePane.reset();
            updateTrackInfo();
            syncPlayPauseIcon();
        });

        timePane.getSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                int sekunden = (int) timePane.getSlider().getValue();

                // UI darf sofort reagieren (wir sind hier im FX-Thread)
                timePane.setCurrentTime(sekunden);

              

                // Seeken in Background-Thread auslagern
                Thread thread = player.new SeekThread(sekunden);
                thread.start();
            }
        });

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                syncPlayPauseIcon();
                applyShuffleStyle(player.isShuffleOn(), shuffleButton);
                updateTrackInfo();
                timePane.setCurrentTime(player.getCurrentTime());
                applyMoodThemeToBackground();
            }
        });
    }

    // Aktualisiert Shuffle-Button Style (aktiv/inaktiv)
    private void applyShuffleStyle(boolean shuffleIson, ToggleButton btn) {
        btn.getStyleClass().removeAll("shuffle", "shuffle-disabled");
        btn.getStyleClass().add(shuffleIson ? "shuffle-disabled" : "shuffle");
        btn.setSelected(shuffleIson);
    }

    // Aktualisiert Track-Informationen (Titel, Artist, Album, Länge)
    private void updateTrackInfo() {
        Track t = player.getCurrentTrack();

        if (t == null) {
            trackTitleLabel.setText("Kein Titel");
            trackArtistLabel.setText("");
            trackAlbumLabel.setText("");
            timePane.reset();
            timePane.setMaxTime(0);
            return;
        }

        trackTitleLabel.setText(
            (t.getTitle() != null && !t.getTitle().isBlank()) ? t.getTitle() : "Unbekannter Titel"
        );
        trackArtistLabel.setText(
            (t.getArtist() != null && !t.getArtist().isBlank()) ? t.getArtist() : "Unbekannter Artist"
        );
        trackAlbumLabel.setText(
            (t.getAlbum() != null && !t.getAlbum().isBlank()) ? t.getAlbum() : "Unbekanntes Album"
        );

        int len = t.getLengthSec();
        if (len <= 0) len = player.getCurrentTrackLengthSeconds();
        timePane.setMaxTime(Math.max(0, len));

        timePane.setCurrentTime(player.getCurrentTime());
    }

    // Synchronisiert Play/Pause Icon mit Player-Status
    private void syncPlayPauseIcon() {
        boolean playingNow = player.isPlaying();
        playButton.getStyleClass().removeAll("play", "pause");
        playButton.getStyleClass().add(playingNow ? "pause" : "play");
    }

    // Passt Hintergrundfarbe an gewählte Mood an
    private void applyMoodThemeToBackground() {
        if (lavaBackground == null) return;

        Mood m = player.getSelectedMood();

        if (m == null) {
            lavaBackground.setBaseColor(null);
            return;
        }

        Color c = getMoodBackgroundColor(m);
        lavaBackground.setBaseColor(c);
    }

    // Gibt die Hintergrundfarbe für eine bestimmte Mood zurück
    private Color getMoodBackgroundColor(Mood m) {
        if (m == null) return null;

        return switch (m) {
            case PEACEFUL -> Color.web("#AEEBFA");
            case CALM -> Color.web("#BFF3DC");
            case NOSTALGIC -> Color.web("#D6C9FF");
            case GRIEF -> Color.web("#C7D0D9");
            case SAD -> Color.web("#BFC9D8");
            case HAPPY -> Color.web("#FFE08A");
            case JOYFUL -> Color.web("#FFE6A8");
            case ENERGETIC -> Color.web("#FFC29A");
            case OPTIMISTIC -> Color.web("#FFF2A6");
            case STRESSED -> Color.web("#C6C2FF");
            case ANGRY -> Color.web("#FFB3B3");
            case FOCUSED -> Color.web("#BFEFC8");
        };
    }

    // Startet Rotation-Animation für Vinyl-Icon (dreht nur wenn Musik spielt)
    private void startPulseAnimation() {
        if (musicIconCircle == null) return;

        javafx.animation.RotateTransition rotateTransition =
            new javafx.animation.RotateTransition(Duration.seconds(3), musicIconCircle);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(javafx.animation.Animation.INDEFINITE);
        rotateTransition.setInterpolator(javafx.animation.Interpolator.LINEAR);

        player.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            if (isPlaying) {
                rotateTransition.play();
            } else {
                rotateTransition.pause();
            }
        });

        if (player.isPlaying()) {
            rotateTransition.play();
        }
    }
}