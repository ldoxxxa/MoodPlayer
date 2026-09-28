package de.hsrm.mi.enia.moodplayer.presentation.uicomponents;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LavaBackground extends Region {

    private final Canvas canvas = new Canvas();
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private final Random rnd = new Random();

    private final List<Blob> blobs = new ArrayList<>();

    private Color targetColor = null;
    private Color currentColor = null;
    private double intensity = 1.0;

    // einheitliche, sanfte Geschwindigkeit
    private double colorLerpSpeed = 2.8;

    private final AnimationTimer timer = new AnimationTimer() {
        private long last = -1;

        @Override
        public void handle(long now) {
            if (last < 0) last = now;
            double dt = (now - last) / 1_000_000_000.0;
            last = now;

            update(dt);
            render();
        }
    };

    public LavaBackground() {
        getChildren().add(canvas);
        canvas.setEffect(new GaussianBlur(32));

        blobs.add(new Blob(0.18, 0.20, 0.03, 0.02, 0.28));
        blobs.add(new Blob(0.70, 0.60, -0.02, 0.02, 0.32));
        blobs.add(new Blob(0.85, 0.35, -0.02, -0.03, 0.22));

        timer.start();
    }

    /**
     * Setzt die Ziel-Farbe für den Hintergrund.
     * - null > Default Auto-Pastel-Cycling
     * - Farbe > Überblendung zu dieser Farbe
     */
    public void setBaseColor(Color c) {
        this.targetColor = c;
        
        if (currentColor == null) {
            currentColor = (c != null) ? c : autoPastel();
        }
    }

    public void setIntensity(double intensity) {
        this.intensity = clamp(0.4, 1.6, intensity);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(Math.max(0, w));
        canvas.setHeight(Math.max(0, h));
        canvas.relocate(0, 0);
    }

    private void update(double dt) {
        // Blob-Animation läuft immer weiter (egal ob Hover oder eingeloggt)
        for (Blob b : blobs) {
            b.x += b.vx * dt;
            b.y += b.vy * dt;

            if (b.x < -0.2) b.x = 1.2;
            if (b.x > 1.2) b.x = -0.2;
            if (b.y < -0.2) b.y = 1.2;
            if (b.y > 1.2) b.y = -0.2;
        }

        // Farbübergang mit einheitlicher Geschwindigkeit
        Color desired = (targetColor != null) ? targetColor : autoPastel();
        if (currentColor == null) currentColor = desired;

        double t = 1.0 - Math.exp(-dt * colorLerpSpeed);

        currentColor = lerpHSB(currentColor, desired, t);
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        Color bg = (currentColor != null) ? currentColor : autoPastel();

        gc.setGlobalAlpha(1.0);
        gc.setFill(bg);
        gc.fillRect(0, 0, w, h);

        for (int i = 0; i < blobs.size(); i++) {
            Blob b = blobs.get(i);

            Color c = blobColor(i, bg);
            double alpha = 0.26 * intensity;

            gc.setGlobalAlpha(alpha);
            gc.setFill(c);

            double r = b.r * Math.min(w, h);
            double cx = b.x * w;
            double cy = b.y * h;

            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        gc.setGlobalAlpha(1.0);
    }

    private Color blobColor(int idx, Color bg) {
        double shift = (idx == 0 ? 0.12 : idx == 1 ? -0.08 : 0.05);
        return bg.deriveColor(shift * 360, 1.0, 1.05, 1.0);
    }

    private Color autoPastel() {
        double t = (System.currentTimeMillis() % 18000) / 18000.0;
        double hue = 200 + 140 * Math.sin(2 * Math.PI * t);
        return Color.hsb(hue, 0.25, 0.92);
    }

    private static Color lerpHSB(Color a, Color b, double t) {
        double ha = a.getHue();
        double hb = b.getHue();
        double dh = ((hb - ha + 540) % 360) - 180;
        double h = (ha + dh * t + 360) % 360;

        double s = a.getSaturation() + (b.getSaturation() - a.getSaturation()) * t;
        double v = a.getBrightness() + (b.getBrightness() - a.getBrightness()) * t;
        double o = a.getOpacity() + (b.getOpacity() - a.getOpacity()) * t;

        return Color.hsb(h, s, v, o);
    }

    private static double clamp(double a, double b, double x) {
        return Math.max(a, Math.min(b, x));
    }

    private static class Blob {
        double x, y;
        double vx, vy;
        double r;

        Blob(double x, double y, double vx, double vy, double r) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.r = r;
        }
    }
}