package de.hsrm.mi.enia.moodplayer.presentation;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

import de.hsrm.mi.enia.moodplayer.business.MoodPlayer;
import de.hsrm.mi.enia.moodplayer.business.Playlist;
import de.hsrm.mi.enia.moodplayer.business.PlaylistManager;
import de.hsrm.mi.enia.moodplayer.business.TagManager;
import de.hsrm.mi.enia.moodplayer.presentation.uicomponents.LavaBackground;
import de.hsrm.mi.enia.moodplayer.presentation.views.PlayerViewController;
import de.hsrm.mi.enia.moodplayer.presentation.views.PlaylistViewController;
import de.hsrm.mi.enia.moodplayer.presentation.views.MoodViewController;
import de.hsrm.mi.enia.moodplayer.presentation.views.StartViewController;

public class MoodPlayerGUI extends Application{

    // View-Switching System
    private static Stage stage;
    private static Map<String, Pane> views;
    private static StackPane contentHost;
    private static String lastViewName = null;

    // Business-Objekte
    private MoodPlayer player;
    private Playlist basePlaylist;
    private PlaylistManager playlistManager;
    private TagManager tagManager;

    // Controller der Views
    private PlayerViewController playerViewController;
    private PlaylistViewController playlistViewController;
    private MoodViewController moodViewController;
    private StartViewController startViewController;

    // Hintergrund
    private LavaBackground lavaBackground;

    // initialisiert Business-Logik (Player, Playlist, Tags) vor GUI-Start
    @Override
    public void init() {
        System.out.println("[App] Initialisierung...");

        views = new HashMap<>();

        player = new MoodPlayer();

        tagManager = new TagManager();
        try {
            tagManager.loadCsv("Playliste/moods.csv");
        } catch (Exception e) {
            System.err.println("Mood CSV nicht geladen: " + e.getMessage());
        }

        playlistManager = new PlaylistManager(tagManager);
        basePlaylist = playlistManager.loadM3U("Playliste/Playlist.m3u");
        
       
        
        if (basePlaylist == null) {
            basePlaylist = new Playlist("Meine Playlist");
            System.err.println("[App] Playlist.m3u nicht geladen, nutze leere Playlist");
        }

        player.setPlaylist(basePlaylist);
        System.out.println("[App] Business-Logik initialisiert");
    }

    // erstellt und zeigt Hauptfenster mit allen Views
    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        System.out.println("[App] Starte GUI...");

        StackPane appRoot = new StackPane();

        lavaBackground = new LavaBackground();
        contentHost = new StackPane();

        appRoot.getChildren().addAll(lavaBackground, contentHost);

        startViewController = new StartViewController();
        views.put("startView", startViewController.getRoot());

        playerViewController = new PlayerViewController(player, lavaBackground);
        views.put("playerView", playerViewController.getRoot());

        playlistViewController = new PlaylistViewController(player, playlistManager, lavaBackground);
        views.put("playlistView", playlistViewController.getRoot());

        moodViewController = new MoodViewController(player, basePlaylist, tagManager, lavaBackground);
        views.put("moodView", moodViewController.getRoot());

        contentHost.getChildren().setAll(views.get("startView"));

        Scene scene = new Scene(appRoot, 800, 750);

        var cssUrl = getClass().getResource("/style.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("MoodPlayer");
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();

        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            event.consume();
        });

        primaryStage.show();

        System.out.println("[App] GUI gestartet");
    }

    // wechselt zwischen Views mit animierten Übergängen
    public static void switchRoot(String viewName) {
        Pane next = views.get(viewName);
        if (next == null) {
            System.err.println("[App] View nicht gefunden: " + viewName);
            return;
        }
        
        Pane current = contentHost.getChildren().isEmpty() ? null : 
                      (Pane) contentHost.getChildren().get(0);
        
        if (current == next) return;
        
        boolean isVertical = isVerticalTransition(lastViewName, viewName);
        boolean goingDown = isGoingDown(lastViewName, viewName);
        
        next.setOpacity(0);
        
        if (isVertical) {
            next.setTranslateY(goingDown ? -30 : 30); 
            next.setScaleX(1.0);
            next.setScaleY(1.0);
        } else {
            next.setTranslateY(0);
            next.setScaleX(0.97); 
            next.setScaleY(0.97);
        }
        
        if (current != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), current);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            ParallelTransition exitTransition;
            
            if (isVertical) {
                TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), current);
                slideOut.setFromY(0);
                slideOut.setToY(goingDown ? 30 : -30); 
                exitTransition = new ParallelTransition(fadeOut, slideOut);
            } else {
                ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), current);
                scaleOut.setFromX(1.0);
                scaleOut.setFromY(1.0);
                scaleOut.setToX(0.97); 
                scaleOut.setToY(0.97);
                exitTransition = new ParallelTransition(fadeOut, scaleOut);
            }
            
            exitTransition.setOnFinished(e -> {
                current.setScaleX(1.0);
                current.setScaleY(1.0);
                current.setTranslateY(0);
                
                contentHost.getChildren().setAll(next);
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), next);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                
                ParallelTransition enterTransition;
                
                if (isVertical) {
                    TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), next);
                    slideIn.setFromY(goingDown ? -30 : 30); 
                    slideIn.setToY(0);
                    enterTransition = new ParallelTransition(fadeIn, slideIn);
                } else {
                    ScaleTransition scaleIn = new ScaleTransition(Duration.millis(250), next);
                    scaleIn.setFromX(0.97); 
                    scaleIn.setFromY(0.97);
                    scaleIn.setToX(1.0);
                    scaleIn.setToY(1.0);
                    enterTransition = new ParallelTransition(fadeIn, scaleIn);
                }
                
                enterTransition.play();
            });
            
            exitTransition.play();
        } else {
            contentHost.getChildren().setAll(next);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), next);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), next);
            scaleIn.setFromX(0.97); 
            scaleIn.setFromY(0.97);
            scaleIn.setToX(1.0);
            scaleIn.setToY(1.0);
            
            ParallelTransition enterTransition = new ParallelTransition(fadeIn, scaleIn);
            enterTransition.play();
        }
        
        lastViewName = viewName;
        System.out.println("[App] View gewechselt zu: " + viewName);
    }
    
    // Prüft ob Übergang vertikal (PlayerView ↔ PlaylistView) ist
    private static boolean isVerticalTransition(String from, String to) {
        if (from == null || to == null) return false;
        
        return (from.equals("playlistView") && to.equals("playerView")) ||
               (from.equals("playerView") && to.equals("playlistView"));
    }
    
    // Prüft ob Übergang nach unten (PlayerView → PlaylistView) geht
    private static boolean isGoingDown(String from, String to) {
        if (from == null || to == null) return false;
        
        return from.equals("playerView") && to.equals("playlistView");
    }

    // stoppt Player und beendet Anwendung
    @Override
    public void stop() {
        if (player != null) player.stop();
        Platform.exit();
    }

    // startet die Anwendung
    public static void main(String[] args) {
        launch(args);
    }
}