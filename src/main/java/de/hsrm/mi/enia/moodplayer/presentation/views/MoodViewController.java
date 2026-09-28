package de.hsrm.mi.enia.moodplayer.presentation.views;

import java.util.EnumSet;

import de.hsrm.mi.enia.moodplayer.business.*;
import de.hsrm.mi.enia.moodplayer.presentation.MoodPlayerGUI;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.LavaBackground;
import javafx.scene.paint.Color;

public class MoodViewController extends BaseController<MoodView> {

    private final MoodPlayer player;
    private final Playlist basePlaylist;
    private final MoodFilter moodFilter;
    private final TagManager tagManager;
    private final LavaBackground lavaBackground;

    public MoodViewController(
        MoodPlayer player,
        Playlist basePlaylist,
        TagManager tagManager,
        LavaBackground lavaBackground
    ) {
        this.player = player;
        this.basePlaylist = basePlaylist;
        this.tagManager = tagManager;
        this.moodFilter = new MoodFilter(tagManager);
        this.lavaBackground = lavaBackground;

        root = new MoodView();
        initialize();
    }

    @Override
    public void initialize() {
        // Hover: verwende exakten Farben aus  Mood-Kreisen
        root.moodWheel.setOnHoverChanged(m -> {
            if (lavaBackground != null) {
                Color c;
                
                if (m != null) {
                    // Hover ist aktiv > Hover-Farbe
                    c = getMoodBackgroundColor(m);
                } else {
                    // kein Hover > Fallback auf selected Mood (falls vorhanden)
                    Mood selected = root.moodWheel.getSelectedMood();
                    c = (selected != null) ? getMoodBackgroundColor(selected) : null;
                }
                
                lavaBackground.setBaseColor(c);
            }
        });

        root.moodWheel.setOnSelectedChanged(m -> {
            if (m == null) {
                root.selectedLabel.setText("Keine Mood ausgewählt");
            } else {
                root.selectedLabel.setText("Ausgewählt: " + m.name());
            }
            
            // wenn Mood ausgewählt wird, sofort Hintergrund anpassen
            if (lavaBackground != null) {
                Color c = (m != null) ? getMoodBackgroundColor(m) : null;
                lavaBackground.setBaseColor(c);
            }
        });

        root.confirmMoodButton.setOnAction(e -> {
            var selected = root.moodWheel.getSelectedMood();
            System.out.println("[Mood] bestätigt: " + selected);

            // Mood im Player speichern
            player.setSelectedMood(selected);

            Playlist result;
            if (selected == null) {
                result = basePlaylist;
            } else {
                result = moodFilter.filter(
                    basePlaylist,
                    EnumSet.of(selected),
                    MoodMatchMode.ANY,
                    selected.name()
                );
            }

            player.setPlaylist(result);
            
            // Farbe setzen bevor zur Playlist wechseln
            if (lavaBackground != null && selected != null) {
                Color c = getMoodBackgroundColor(selected);
                lavaBackground.setBaseColor(c);
            }

            MoodPlayerGUI.switchRoot("playlistView");
        });

        root.backToStartButton.setOnAction(e -> MoodPlayerGUI.switchRoot("startView"));
        root.toPlayerButton.setOnAction(e -> MoodPlayerGUI.switchRoot("playerView"));
        root.toPlaylistButton.setOnAction(e -> MoodPlayerGUI.switchRoot("playlistView"));

        // wenn bereits eine Mood gesetzt ist, Hintergrund anpassen
        Mood currentMood = player.getSelectedMood();
        if (lavaBackground != null) {
            if (currentMood != null) {
                Color c = getMoodBackgroundColor(currentMood);
                lavaBackground.setBaseColor(c);
            } else {
                lavaBackground.setBaseColor(null);
            }
        }
    }

    /**
     * Gibt die EXAKTE Hintergrundfarbe für eine Mood zurück.
     * Diese Farben sind identisch mit denen in MoodWheelPane!
     */
    private Color getMoodBackgroundColor(Mood m) {
        if (m == null) return null;
        
        // EXAKT die gleichen Hex-Werte wie in MoodWheelPane
        return switch (m) {
            case PEACEFUL -> Color.web("#AEEBFA");   // türkis (pastell)
            case CALM -> Color.web("#BFF3DC");       // grün (pastell)
            case NOSTALGIC -> Color.web("#D6C9FF");  // lila (pastell)
            case GRIEF -> Color.web("#C7D0D9");      // grau-blau (pastell)
            case SAD -> Color.web("#BFC9D8");        // kühl-grau/blau (pastell)
            case HAPPY -> Color.web("#FFE08A");      // warm-gelb (pastell)
            case JOYFUL -> Color.web("#FFE6A8");     // hell-warm (pastell)
            case ENERGETIC -> Color.web("#FFC29A");  // orange (pastell)
            case OPTIMISTIC -> Color.web("#FFF2A6"); // sonnig (pastell)
            case STRESSED -> Color.web("#C6C2FF");   // blau-violett (pastell)
            case ANGRY -> Color.web("#FFB3B3");      // rot (pastell)
            case FOCUSED -> Color.web("#BFEFC8");    // grün (pastell)
        };
    }
}