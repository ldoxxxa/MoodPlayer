package de.hsrm.mi.enia.moodplayer.presentation.views;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class StartView extends BorderPane {

    // Content Buttons
    public Button moodSelectButton;

    public StartView() {
        setPadding(new Insets(0));

        // Center Content
        StackPane rootStack = new StackPane();
        rootStack.setAlignment(Pos.CENTER);
        rootStack.getStyleClass().add("start-root");

        // Aura Layer (hinter Content)
        Region glow = new Region();
        glow.getStyleClass().add("start-glow");
        glow.setMouseTransparent(true);

        // Content Box
        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("start-content");

        content.setMaxWidth(420);  
        content.setMinWidth(280); 

        // Icon-Kreis mit Animation
        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("start-iconCircle");
        iconCircle.setMinSize(130, 130);
        iconCircle.setPrefSize(130, 130);
        iconCircle.setMaxSize(130, 130);

        // Musiknoten-Symbol (Unicode) mit farbigem Gradient
        Label musicIcon = new Label("♪");
        musicIcon.setStyle("-fx-font-size: 60px; -fx-text-fill: linear-gradient(to bottom, #FF8BA7, #FFA07A); -fx-font-weight: bold;");
        musicIcon.getStyleClass().add("music-note-icon");
        
        iconCircle.getChildren().add(musicIcon);
        
        // Pulsier-Animation für Icon
        Timeline pulseAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(iconCircle.scaleXProperty(), 1.0),
                new KeyValue(iconCircle.scaleYProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(1000), 
                new KeyValue(iconCircle.scaleXProperty(), 1.08),
                new KeyValue(iconCircle.scaleYProperty(), 1.08)
            ),
            new KeyFrame(Duration.millis(2000), 
                new KeyValue(iconCircle.scaleXProperty(), 1.0),
                new KeyValue(iconCircle.scaleYProperty(), 1.0)
            )
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();

        // Texte 
        Label welcome = new Label("Welcome");
        welcome.getStyleClass().add("start-welcomeBig");
        welcome.setAlignment(Pos.CENTER);

        Label intro = new Label("Entdecke Musik, die zu deiner\nStimmung passt.\nWähle deine Mood und lass dich von der perfekten Playlist begleiten.");
        intro.getStyleClass().add("start-intro");
        intro.setAlignment(Pos.CENTER);
        intro.setWrapText(true);
        intro.setMaxWidth(450);
        intro.setStyle("-fx-text-alignment: center;");

        moodSelectButton = new Button("Wie fühlst du dich?");
        moodSelectButton.getStyleClass().add("start-cta");
        moodSelectButton.setStyle("-fx-font-weight: 800;"); 

        // Alle Elemente zentriert in VBox
        content.getChildren().addAll(iconCircle, welcome, intro, moodSelectButton);

        // Padding am Rand
        rootStack.setPadding(new Insets(30));

        rootStack.getChildren().addAll(glow, content);
        setCenter(rootStack);
    }
}