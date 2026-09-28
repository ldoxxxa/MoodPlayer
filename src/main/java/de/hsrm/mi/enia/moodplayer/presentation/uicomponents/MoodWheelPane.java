package de.hsrm.mi.enia.moodplayer.presentation.uicomponents;

import de.hsrm.mi.enia.moodplayer.business.Mood;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.VPos;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;

/**
 * MoodWheelPane als Wolke:
 * - rdnet die Moods als organische Wolke an 
 * - Hover + Click (Select/Unselect)
 */
public class MoodWheelPane extends Pane {

    public interface MoodConsumer {
        void accept(Mood mood);
    }

    private MoodConsumer onHoverChanged;
    private MoodConsumer onSelectedChanged;

    private final ObjectProperty<Mood> hoveredMood = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Mood> selectedMood = new SimpleObjectProperty<>(null);

    private static class MoodNode {
        Mood mood;
        Circle circle;
        Text label;

        double targetX; // 0..1 (normiert)
        double targetY; // 0..1 (normiert)

        double x; // px (aktuelles Zentrum)
        double y; // px (aktuelles Zentrum)
        double baseR; // px (Basisradius)
        double r; // px (aktueller Radius)
        Color baseColor;

        MoodNode(Mood mood) { this.mood = mood; }
    }

    private final List<MoodNode> nodes = new ArrayList<>();

    private final DropShadow selectedShadow = new DropShadow(18, Color.rgb(0, 0, 0, 0.18));
    private final DropShadow hoverShadow = new DropShadow(12, Color.rgb(0, 0, 0, 0.12));

    private final Map<Circle, Timeline> hoverAnims = new HashMap<>();

    private boolean hasLaidOutOnce = false;
    private double lastW = -1;
    private double lastH = -1;

    public MoodWheelPane() {
        setPickOnBounds(false);

        // Wolken-Anordnung 
        addMood(Mood.PEACEFUL,  0.28, 0.10, 110, Color.web("#AEEBFA"));
        addMood(Mood.GRIEF,     0.68, 0.14, 95, Color.web("#C7D0D9"));
        addMood(Mood.CALM,      0.15, 0.25, 125, Color.web("#BFF3DC"));
        addMood(Mood.OPTIMISTIC,0.82, 0.30, 100, Color.web("#FFF2A6"));
        addMood(Mood.NOSTALGIC, 0.48, 0.42, 105, Color.web("#D6C9FF"));
        addMood(Mood.SAD,       0.30, 0.47, 135, Color.web("#BFC9D8"));
        addMood(Mood.HAPPY,     0.64, 0.58, 140, Color.web("#FFE08A"));
        addMood(Mood.STRESSED,  0.20, 0.70, 110, Color.web("#C6C2FF"));
        addMood(Mood.JOYFUL,    0.75, 0.74, 115, Color.web("#FFE6A8"));
        addMood(Mood.FOCUSED,   0.45, 0.78, 120, Color.web("#BFEFC8"));
        addMood(Mood.ANGRY,     0.12, 0.88, 115, Color.web("#FFB3B3"));
        addMood(Mood.ENERGETIC, 0.85, 0.90, 130, Color.web("#FFC29A"));

        widthProperty().addListener((obs, ov, nv) -> layoutCloud());
        heightProperty().addListener((obs, ov, nv) -> layoutCloud());

        layoutCloud();

        hoveredMood.addListener((obs, oldV, newV) -> {
            if (onHoverChanged != null) onHoverChanged.accept(newV);
            updateSelectionVisuals();
        });
        selectedMood.addListener((obs, oldV, newV) -> {
            if (onSelectedChanged != null) onSelectedChanged.accept(newV);
            updateSelectionVisuals();
        });
    }

    public void setOnHoverChanged(MoodConsumer c) { 
    	this.onHoverChanged = c; 
    }
    
    public void setOnSelectedChanged(MoodConsumer c) { 
    	this.onSelectedChanged = c; 
    }

    public Mood getSelectedMood() { 
    	return selectedMood.get(); 
    }

    private void addMood(Mood mood, double tx, double ty, double size, Color c) {
        MoodNode n = new MoodNode(mood);
        n.targetX = tx;
        n.targetY = ty;
        n.baseColor = c;
        n.baseR = Math.max(22, size / 2.0);

        Circle circle = new Circle(10);
        circle.setFill(c.deriveColor(0, 1.0, 1.0, 0.90));
        circle.setStroke(Color.rgb(255, 255, 255, 0.20));
        circle.setStrokeWidth(1.0);

        Text label = new Text(mood.name());
        label.setTextOrigin(VPos.CENTER);
        label.setFill(Color.rgb(26, 26, 26, 0.92));
        label.setMouseTransparent(true);

        circle.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            hoveredMood.set(mood);
            animateHover(circle, true);
        });
        circle.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hoveredMood.set(null);
            animateHover(circle, false);
        });
        circle.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            Mood cur = selectedMood.get();
            Mood next = (cur == mood) ? null : mood;
            selectedMood.set(next);
            updateSelectionVisuals();
        });

        n.circle = circle;
        n.label = label;

        nodes.add(n);
        getChildren().addAll(circle, label);
    }

    private void layoutCloud() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        boolean sizeChanged = (Math.abs(w - lastW) > 0.5) || (Math.abs(h - lastH) > 0.5);
        lastW = w;
        lastH = h;

        double pad = Math.max(24, Math.min(w, h) * 0.06);
        double cloudW = w - 2 * pad;
        double cloudH = h - 2 * pad;

        double scale = Math.max(0.75, Math.min(1.25, Math.min(w, h) / 520.0));

        // Abstand zwischen Kreisen
        double gap = 22 * scale; 

        if (!hasLaidOutOnce || sizeChanged) {
            Random rnd = new Random(1337);

            for (MoodNode n : nodes) {
                double tx = pad + n.targetX * cloudW;
                double ty = pad + n.targetY * cloudH;

                // kleineres Jitter für präzisere Positionierung
                double jx = (rnd.nextDouble() - 0.5) * 8;
                double jy = (rnd.nextDouble() - 0.5) * 8;

                n.x = tx + jx;
                n.y = ty + jy;

                double baseR = n.baseR * scale;
                n.r = clamp(38 * scale, 92 * scale, baseR);

                n.circle.setRadius(n.r);

                // Schrift in den Kreisen
                double fs = (n.r > 78 * scale) ? 16 * scale : (n.r > 62 * scale) ? 14 * scale : 12 * scale;
                n.label.setFont(Font.font("Helvetica", FontWeight.BOLD, fs));  // war SEMI_BOLD
            }

            // Iterations für perfekte Kollisionsvermeidung
            int iters = 1000; 
            double attract = 0.004; 

            for (int iter = 0; iter < iters; iter++) {
                for (MoodNode n : nodes) {
                    double tx = pad + n.targetX * cloudW;
                    double ty = pad + n.targetY * cloudH;

                    n.x += (tx - n.x) * attract;
                    n.y += (ty - n.y) * attract;
                }

                boolean any = resolveCollisions(pad, cloudW, cloudH, gap);

                if (!any && iter > 100) break;
            }

            // Extra Safety-Pass
            for (int safety = 0; safety < 150; safety++) {
                boolean any = resolveCollisions(pad, cloudW, cloudH, gap);
                if (!any) break;
            }

            hasLaidOutOnce = true;
        } else {
            for (MoodNode n : nodes) {
                double baseR = n.baseR * scale;
                n.r = clamp(38 * scale, 92 * scale, baseR);
                n.circle.setRadius(n.r);

                // Schrift
                double fs = (n.r > 78 * scale) ? 16 * scale : (n.r > 62 * scale) ? 14 * scale : 12 * scale;
                n.label.setFont(Font.font("Helvetica", FontWeight.BOLD, fs));  // war SEMI_BOLD
            }

            for (int safety = 0; safety < 80; safety++) {
                boolean any = resolveCollisions(pad, cloudW, cloudH, gap);
                if (!any) break;
            }
        }

        for (MoodNode n : nodes) {
            n.circle.setCenterX(n.x);
            n.circle.setCenterY(n.y);

            n.label.setX(n.x - n.label.getLayoutBounds().getWidth() / 2.0);
            n.label.setY(n.y);
        }

        updateSelectionVisuals();
    }

    private boolean resolveCollisions(double pad, double cloudW, double cloudH, double gap) {
        boolean any = false;

        for (int i = 0; i < nodes.size(); i++) {
            MoodNode a = nodes.get(i);
            for (int j = i + 1; j < nodes.size(); j++) {
                MoodNode b = nodes.get(j);

                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                double minDist = a.r + b.r + gap;

                if (dist < 0.0001) {
                    dx = 1.0;
                    dy = 0.33;
                    dist = Math.sqrt(dx * dx + dy * dy);
                }

                if (dist < minDist) {
                    any = true;

                    double overlap = (minDist - dist);

                    double nx = dx / dist;
                    double ny = dy / dist;

                    double wa = 1.0 / (a.r);
                    double wb = 1.0 / (b.r);
                    double sum = wa + wb;

                    double moveA = overlap * (wa / sum) * 0.88; // etwas stärker
                    double moveB = overlap * (wb / sum) * 0.88;

                    a.x -= nx * moveA;
                    a.y -= ny * moveA;
                    b.x += nx * moveB;
                    b.y += ny * moveB;
                }
            }
        }

        for (MoodNode n : nodes) {
            n.x = clamp(pad + n.r, pad + cloudW - n.r, n.x);
            n.y = clamp(pad + n.r, pad + cloudH - n.r, n.y);
        }

        return any;
    }

    private void updateSelectionVisuals() {
        Mood sel = selectedMood.get();
        Mood hov = hoveredMood.get();

        for (MoodNode n : nodes) {
            boolean isSel = (sel != null && sel == n.mood);
            boolean isHov = (hov != null && hov == n.mood);

            if (isSel) {
                n.circle.setFill(n.baseColor.deriveColor(0, 1.0, 1.03, 0.96));
                n.circle.setEffect(selectedShadow);
            } else {
                n.circle.setFill(n.baseColor.deriveColor(0, 1.0, 1.0, isHov ? 0.96 : 0.88));
                if (isHov) n.circle.setEffect(hoverShadow);
                else n.circle.setEffect(null);
            }

            n.label.setOpacity(isSel ? 1.0 : 0.95);
            n.label.setFill(Color.rgb(26, 26, 26, isSel ? 0.95 : 0.90));
        }
    }

    private void animateHover(Circle c, boolean entering) {
        Timeline tl = hoverAnims.computeIfAbsent(c, k -> new Timeline());
        tl.stop();

        double to = entering ? 0.98 : 0.88;

        tl.getKeyFrames().setAll(
            new KeyFrame(Duration.millis(160),
                new KeyValue(c.opacityProperty(), to, Interpolator.EASE_BOTH)
            )
        );

        tl.playFromStart();
    }

    private static double clamp(double a, double b, double x) {
        return Math.max(a, Math.min(b, x));
    }
    
    public Color getColorForMood(Mood mood) {
        if (mood == null) return null;
        for (MoodNode n : nodes) {
            if (n.mood == mood) return n.baseColor;
        }
        return null;
    }
}