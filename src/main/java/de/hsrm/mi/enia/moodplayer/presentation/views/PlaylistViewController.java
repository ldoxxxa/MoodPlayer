package de.hsrm.mi.enia.moodplayer.presentation.views;

import de.hsrm.mi.enia.moodplayer.business.Mood;

import de.hsrm.mi.enia.moodplayer.business.MoodPlayer;
import de.hsrm.mi.enia.moodplayer.business.Playlist;
import de.hsrm.mi.enia.moodplayer.business.PlaylistManager;
import de.hsrm.mi.enia.moodplayer.business.Track;

import de.hsrm.mi.enia.moodplayer.presentation.MoodPlayerGUI;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.LavaBackground;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.TimePane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;

public class PlaylistViewController extends BaseController<PlaylistView> {

    private ListView<Track> playlistView;
    private Playlist playlist;
    private PlaylistManager playlistManager;
    private MoodPlayer player;

    private Button playButton;
    private Button skipButton;
    private Button skipBackButton;
    private ToggleButton shuffleButton;
    private Slider volumeSlider;

    private Timeline playStateUpdater;
    private TimePane timePane;
    private ObservableList<Track> items;
    private final LavaBackground lavaBackground;

    // Konstruktor - initialisiert UI-Elemente und Playlist-Daten
    public PlaylistViewController(MoodPlayer player, PlaylistManager playlistManager, LavaBackground lavaBackground) {
        root = new PlaylistView();

        this.player = player;
        this.playlist = player.getCurrentPlaylist();
        this.playlistManager = playlistManager;
        this.lavaBackground = lavaBackground;

        playlistView = root.playlistListView;
        timePane = root.timePane;

        this.playButton = root.controlPane.playButton;
        this.skipButton = root.controlPane.skipButton;
        this.skipBackButton = root.controlPane.skipBackButton;
        this.shuffleButton = root.controlPane.shuffleButton;
        this.volumeSlider = root.controlPane.volumeSlider;

        initialize();
    }

    // Bindet alle Event-Listener und initialisiert Playlist-Ansicht
    @Override
    public void initialize() {

        playlistView.setCellFactory(new Callback<ListView<Track>, ListCell<Track>>() {
            private int counter = -1;

            @Override
            public ListCell<Track> call(ListView<Track> view) {
                counter++;
                return new TrackCell(counter);
            }
        });

        items = FXCollections.observableArrayList();
        if (playlist != null) {
            items.setAll(playlist.getTracks());
        }
        playlistView.setItems(items);

        Playlist initial = player.getCurrentPlaylist();
        if (initial != null) {
            playlist = initial;
            items.setAll(initial.getTracks());
        }

        player.playlistProperty().addListener((obs, oldPlaylist, newPlaylist) -> {
            if (newPlaylist == null) return;

            System.out.println("[PlaylistView] Playlist gewechselt: " + newPlaylist.getName());

            playlist = newPlaylist;
            items.setAll(newPlaylist.getTracks());

            playlistView.getSelectionModel().clearSelection();
            syncSelectionToCurrentTrack();
            updateTimePaneForCurrentTrack();

            if (root.getScene() != null) {
                applyMoodThemeToBackground();
            }
        });

        playlistView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Track>() {
            @Override
            public void changed(ObservableValue<? extends Track> observable, Track oldTrack, Track newTrack) {
                if (newTrack != null) {
                    updateMiniTrackInfo(newTrack);
                }
            }
        });

        root.backToMoodButton.setOnAction(e -> MoodPlayerGUI.switchRoot("moodView"));
        root.toMoodButton.setOnAction(e -> MoodPlayerGUI.switchRoot("moodView"));
        root.toPlayerButton.setOnAction(e -> MoodPlayerGUI.switchRoot("playerView"));

        playlistView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Track selected = playlistView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    player.setPlaylist(playlist);
                    player.playTrack(selected);
                    updateTimePaneForCurrentTrack();
                }
            }
        });

        playButton.setOnAction(event -> {
            if (player.isPlaying()) {
                player.pause();
                return;
            }

            if (player.getCurrentTrack() != null) {
                player.playOrResume();
                updateTimePaneForCurrentTrack();
                return;
            }

            Track selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected == null && playlist != null && !playlist.getTracks().isEmpty()) {
                selected = playlist.getTracks().get(0);
                playlistView.getSelectionModel().select(0);
            }

            if (selected != null) {
                player.setPlaylist(playlist);
                player.playTrack(selected);
                updateTimePaneForCurrentTrack();
            }
        });

        skipButton.setOnAction(event -> {
            player.skip();
           // updateTimePaneForCurrentTrack();
        });

        skipBackButton.setOnAction(event -> {
            player.skipBack();
            updateTimePaneForCurrentTrack();
        });

        //applyShuffleStyle(player.isShuffleOn(), shuffleButton);

        shuffleButton.selectedProperty().addListener((obs, oldValue, newValue) -> {
            player.shuffle(newValue);
            applyShuffleStyle(newValue, shuffleButton);
        });

        volumeSlider.valueProperty().bindBidirectional(player.volumePercentProperty());

       // setupSeekListener();
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

        player.currentTimeProperty().addListener((obs, oldV, newV) -> {
        	Platform.runLater(()-> {
        		if (!timePane.getSlider().isValueChanging()) {
                    timePane.setCurrentTime(newV.intValue());
                }
        	});
            
        });

        //startPlayStateUpdater();
        player.playingProperty().addListener((obs, wasPlaying, isPlaying) -> {
            Platform.runLater(() -> setPlayIcon(isPlaying));
        });
        
        player.currentTrackProperty().addListener((obs, oldTrack, newTrack) -> {
        	
            Platform.runLater(() -> {
            	syncSelectionToCurrentTrack();
                updateMiniTrackInfo(newTrack);
                updateTimePaneForCurrentTrack();// Titel/Artist/Duration etc.
                timePane.reset();             // oder setCurrentTime(0) + max
            });
        });
        
        player.selectedMoodProperty().addListener((obs, oldMood, newMood) -> {
        	Platform.runLater( () -> applyMoodThemeToBackground());
        	});

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
            	Platform.runLater( () -> {
            		setPlayIcon(player.isPlaying());
                    applyShuffleStyle(player.isShuffleOn(), shuffleButton);
                    syncSelectionToCurrentTrack();
                    updateTimePaneForCurrentTrack();
                    applyMoodThemeToBackground();
            	});
                

               /* if (playStateUpdater != null && playStateUpdater.getStatus() != Timeline.Status.RUNNING) {
                    playStateUpdater.play();
                }
            } else {
                if (playStateUpdater != null) {
                    playStateUpdater.stop();
                }*/
            }
        });
    }

    // Aktualisiert Shuffle-Button Style (aktiv/inaktiv)
    private void applyShuffleStyle(boolean shuffleIsOn, ToggleButton btn) {
        btn.getStyleClass().removeAll("shuffle", "shuffle-disabled");
        btn.getStyleClass().add(shuffleIsOn ? "shuffle-disabled" : "shuffle");
        btn.setSelected(shuffleIsOn);
    }

    private Track lastHighlighted = null;

    // Startet Timeline zur regelmäßigen Aktualisierung von Play-Status und Selektion
    /*private void startPlayStateUpdater() {
        playStateUpdater = new Timeline(
            new KeyFrame(Duration.millis(200), e -> {

                setPlayIcon(player.isPlaying());

                Track current = player.getCurrentTrack();
                if (current != null && current != lastHighlighted) {
                    lastHighlighted = current;
                    syncSelectionToCurrentTrack();
                }

                applyShuffleStyle(player.isShuffleOn(), shuffleButton);

                if (root.getScene() != null) {
                    applyMoodThemeToBackground();
                
                
                }
            })
        );
        playStateUpdater.setCycleCount(Timeline.INDEFINITE);
        playStateUpdater.play();
    }*/

    // Aktualisiert Mini-Track-Info mit Titel und Artist
    private void updateMiniTrackInfo(Track t) {
        if (t == null) {
            root.miniTitleLabel.setText("Kein Song ausgewählt");
            root.miniArtistLabel.setText("");
            return;
        }

        String title = (t.getTitle() != null && !t.getTitle().isBlank()) ? t.getTitle() : "Unbekannter Titel";
        String artist = (t.getArtist() != null && !t.getArtist().isBlank()) ? t.getArtist() : "Unbekannter Artist";

        root.miniTitleLabel.setText(title);
        root.miniArtistLabel.setText(artist);
    }

    // Synchronisiert ListView-Selektion mit aktuell spielendem Track
    private void syncSelectionToCurrentTrack() {
        Track current = player.getCurrentTrack();
        if (current == null || playlist == null) return;

        int idx = playlist.getTracks().indexOf(current);
        if (idx >= 0) {
            playlistView.getSelectionModel().select(idx);
            playlistView.scrollTo(idx);
           // updateMiniTrackInfo(current);
        }
    }

    // Setzt Play/Pause Icon basierend auf Player-Status
    private void setPlayIcon(boolean playing) {
        playButton.getStyleClass().removeAll("play", "pause");
        playButton.getStyleClass().add(playing ? "pause" : "play");
    }

    // Aktualisiert TimePane mit Länge und Position des aktuellen Tracks
    private void updateTimePaneForCurrentTrack() {
        Track t = player.getCurrentTrack();

        if (t == null) {
            timePane.reset();
            timePane.setMaxTime(0);
            return;
        }

        int len = t.getLengthSec();
        if (len <= 0) len = player.getCurrentTrackLengthSeconds();

        timePane.setMaxTime(Math.max(0, len));
        timePane.setCurrentTime(player.getCurrentTime());
    }

    /*// Richtet Seek-Listener für Slider ein (springen im Track)
    private void setupSeekListener() {
        timePane.getSlider().valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                int sek = (int) timePane.getSlider().getValue();
                player.seekToSeconds(sek);
                timePane.setCurrentTime(sek);
            }
        });
    }*/

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
}