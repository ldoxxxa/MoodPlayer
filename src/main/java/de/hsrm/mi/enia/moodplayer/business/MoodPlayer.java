package de.hsrm.mi.enia.moodplayer.business;

import de.hsrm.mi.eibo.simpleplayer.SimpleAudioPlayer;

import de.hsrm.mi.eibo.simpleplayer.SimpleMinim;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 *  - hält intern eine Playlist + aktuellen Trackindex
 *  - steuert Wiedergabe (play/pause/resume/stop/skip/seek/volume) wie bisher
 *  - zusätzlich: hält MoodProfile selectedMood
 *  - kann eine Mood-Playlist erzeugen (getMoodPlaylist()) durch filtern
 */
public class MoodPlayer {

    // Library-Objekte für Audio
    private final SimpleMinim minim;
    private SimpleAudioPlayer audioPlayer;

    // Playlist-Zustand
    private int currentIndex = 0;

    // Modi
    private boolean shuffle = false;
    private boolean repeat = false;

    // Thread-Flags
    private PlayThread playThread;
    private volatile boolean wasStopped = false;
    private volatile boolean isPaused = false;
    private volatile int playToken = 0; // damit nicht zwei Wiedergaben gleichzeitig
    private Thread timeThread;
    
    private final Object audioLock = new Object();

    // aktuelle Zeit in Sekunden (für TimePane)
    private final IntegerProperty currentTime = new SimpleIntegerProperty(0);
    // damit volume als Property gilt > für Slider-Binding
    private final DoubleProperty volumeValue = new SimpleDoubleProperty(80.0); // 0..100

    // Properties für GUI
    private final ObjectProperty<Playlist> playlistProperty = new SimpleObjectProperty<>();
    private final BooleanProperty playingProperty = new SimpleBooleanProperty(false);

    // aktueller Track als Property (GUI kann darauf reagieren)
    private final ObjectProperty<Track> currentTrackProperty = new SimpleObjectProperty<>();

    // aktuelle Mood als Property, damit GUI reagieren kann
    private final ObjectProperty<Mood> selectedMoodProperty = new SimpleObjectProperty<>(null);

    public ObjectProperty<Mood> selectedMoodProperty() {
        return selectedMoodProperty;
    }

    public Mood getSelectedMood() {
        return selectedMoodProperty.get();
    }

    public void setSelectedMood(Mood mood) {
        selectedMoodProperty.set(mood);
    }

    /** SimpleMinim mit blockierendem play() */
    public MoodPlayer() {
        minim = new SimpleMinim(false); // false = man steuert Threading selbst

        // sobald sich der volume-Wert ändert > direkt am AudioPlayer anwenden
        volumeValue.addListener((obs, oldV, newV) -> applyVolumeToAudioPlayer());
    }

    public ObjectProperty<Playlist> playlistProperty() {
        return playlistProperty;
    }

    public Playlist getPlaylist() {
        return playlistProperty.get();
    }

    public BooleanProperty playingProperty() {
        return playingProperty;
    }

    public ObjectProperty<Track> currentTrackProperty() {
        return currentTrackProperty;
    }

    /** gibt den aktuellen Player frei, pausiert und setzt audioPlayer auf null */
    private void releasePlayer() {
    	
        if (audioPlayer != null) {
            try {
                audioPlayer.pause();
            } catch (Exception ignored) {
            }
            audioPlayer = null;
        }
    	
    }

    // Playlist
    public Playlist getCurrentPlaylist() {
        return playlistProperty.get();
    }

    /** setzt eine neue Playlist */
    public void setPlaylist(Playlist playlist) {
        if (playlist == null) return;

        // wenn es dieselbe Playlist ist: nicht currentIndex resetten
        if (this.playlistProperty.get() == playlist) {
            return;
        }

        currentIndex = 0;

        // GUI informieren
        playlistProperty.set(playlist);
        System.out.println("Playlist gesetzt: " + playlist.getName());
    }

    // Time Property
    public IntegerProperty currentTimeProperty() {
        return currentTime;
    }

    public int getCurrentTime() {
        return currentTime.get();
    }

    private void setCurrentTime(int seconds) {
        this.currentTime.set(seconds);
        
    }

    // Volume Property
    /** Property für Lautstärke (0-100) damit beide Views senselben Wert binden können */
    public DoubleProperty volumePercentProperty() {
        return volumeValue;
    }

    public double getVolumePercent() {
        return volumeValue.get();
    }

    /** setzt Lautstärke als Prozentwert 0-100 und clamped den Bereich */
    public void setVolumePercent(double v) {
        if (v < 0) v = 0;
        if (v > 100) v = 100;
        volumeValue.set(v);
    }

    /** Lautstärke einstellen > wird von Controllern aufgerufen */
    public void volume(double value) {
        setVolumePercent(value); // triggert applyVolumeToAudioPlayer() automatisch
    }

    /**
     * apply-Methode für db Lautstärke > private Hilfsmethode für volume()
     * > wendet den aktuellen Volume-Wert auf AudioPlayer an
     */
    private void applyVolumeToAudioPlayer() {
        double v = getVolumePercent(); // 0..100

        if (audioPlayer == null) {
            System.out.println("[VOLUME] audioPlayer=null, gespeicherter Wert=" + v);
            return;
        }

        // Slider 0..100 -> 0..1
        double x = v / 100.0;

        // Sonderfall: komplett stumm
        if (x <= 0.0001) {
            try {
                audioPlayer.setGain(-80f); // effektiv stumm
                System.out.println("[VOLUME] Slider=0 -> gainDb=-80 (stumm)");
            } catch (Exception e) {
                System.out.println("[VOLUME] setGain FEHLER (mute): " + e.getMessage());
            }
            return;
        }

        // logarithmische Lautstärke
        // x = 1.0 ->   0 dB
        // x = 0.1 -> -20 dB
        // x = 0.01 -> -40 dB
        double gainDb = 20.0 * Math.log10(x);

        // Untere Grenze (sehr leise, aber nicht -Infinity)
        if (gainDb < -60.0) {
            gainDb = -60.0;
        }

        System.out.println("[VOLUME] Slider=" + v + " -> x=" + x + " -> gainDb=" + gainDb);

        try {
            audioPlayer.setGain((float) gainDb);
            System.out.println("[VOLUME] setGain OK");
        } catch (Exception e) {
            System.out.println("[VOLUME] setGain FEHLER: " + e.getMessage());
        }

        // optionaler Fallback
        try {
            float lin = (float) x;
            // falls es setVolume nicht gibt > catch macht nichts kaputt
            audioPlayer.getClass().getMethod("setVolume", float.class).invoke(audioPlayer, lin);
            System.out.println("[VOLUME] setVolume OK (Fallback), lin=" + lin);
        } catch (Exception ignored) {
            // ignorieren, wenn es die Methode nicht gibt
        }
    }

    // Playback
    /** aktuellen Song abspielen, lädt den Track neu, startet Zeit-Thread & Play-Thread */
    public void play() {
        Playlist pl = playlistProperty.get();
        if (pl == null || pl.size() == 0) {
            System.out.println("Keine Playlist oder leer!");
            return;
        }

        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= pl.size()) currentIndex = 0;

        int myToken = ++playToken;

        Track track = pl.getTracks().get(currentIndex);
        currentTrackProperty.set(track); // GUI informieren: Track hat sich geändert
        System.out.println("Spiele: " + track);

        int lenSec = 0;
        
        synchronized (audioLock){
	        releasePlayer();
	
	        audioPlayer = minim.loadMP3File(track.getPlayPath());
	        applyVolumeToAudioPlayer();
	
	        
	        try {
	            lenSec = Math.max(0, audioPlayer.length() / 1000);
	        } catch (Exception ignored) {
	        }
        }

        if (lenSec > 0) track.setLengthSec(lenSec);

        wasStopped = false;
        isPaused = false;

        playingProperty.set(true);

        setCurrentTime(0);

        startTimeThread();
        playThread = new PlayThread(myToken);
        playThread.start();
    }

    /** spielt eine einzelne Datei direkt ab > ohne Playlist */
    public void play(String filename) {
        releasePlayer();

        audioPlayer = minim.loadMP3File(filename);
        applyVolumeToAudioPlayer();

        wasStopped = false;
        isPaused = false;

        playingProperty.set(true);

        setCurrentTime(0);

        int myToken = ++playToken; // neuen Token erzeugen

        startTimeThread();

        playThread = new PlayThread(myToken);
        playThread.start();
    }

    /** pausiert die Wiedergabe */
    public void pause() {
        isPaused = true;

        playingProperty.set(false);

        playToken++; // damit nicht doppelte Wiedergabe > alte Threads weg

        if (audioPlayer != null) {
            audioPlayer.pause();
        }

        stopTimeThread(false); // Zeit-Thread stoppen, Zeit aber nicht resetten
    }

    /** setzt die Wiedergabe nach Pause fort, ohne neu zu laden */
    public void resume() {
        // nur wenn wir pausiert und Player existiert
        if (!isPaused || audioPlayer == null) {
            return;
        }

        // neuer Token, damit alter Thread veraltet ist
        int myToken = ++playToken;

        wasStopped = false;
        isPaused = false;

        playingProperty.set(true);

        // Timer weiterlaufen lassen (ohne Parameter)
        startTimeThread();

        // neuen PlayThread starten (audioPlayer.play() läuft ab aktueller Position weiter)
        playThread = new PlayThread(myToken);
        playThread.start();
    }

    /** Konfort-Methode - Controller müssen nicht raten */
    public void playOrResume() {
        if (isPaused && audioPlayer != null) {
            resume();
        } else {
            play(); // normaler Start
        }
    }

    /** stoppt die Wiedergabe komplett und setzt Zeit zurück */
    public void stop() {
        wasStopped = true;
        isPaused = false;

        playingProperty.set(false);

        playToken++; // damit nicht doppelte Wiedergabe

        releasePlayer();

        stopTimeThread(true); // Timer stoppen und auf 0 zurücksetzen
        System.out.println("Gestoppt - Auto-Play unterbrochen");
    }

    // Navigation
    /** nächster Song */
    public void skip() {
        Playlist pl = playlistProperty.get();
        if (pl == null || pl.size() == 0) return;

        if (shuffle) {
            currentIndex = (int) (Math.random() * pl.size());
        } else {
            currentIndex++;
            if (currentIndex >= pl.size()) {
                if (repeat) {
                    currentIndex = 0;
                } else {
                    System.out.println("Ende der Playlist erreicht.");
                    stop(); // sicherheitshalber
                    return;
                }
            }
        }

        wasStopped = false;
        isPaused = false;
        play();
    }

    /** vorheriger Song */
    public void skipBack() {
        Playlist pl = playlistProperty.get();
        if (pl == null || pl.size() == 0) return;

        currentIndex--;
        if (currentIndex < 0) {
            if (repeat) {
                currentIndex = pl.size() - 1;
            } else {
                currentIndex = 0;
            }
        }

        wasStopped = false;
        isPaused = false;
        play();
    }

    // Modi
    /** Shuffle-Modus an/aus */
    public void shuffle(boolean on) {
        this.shuffle = on;
        System.out.println("Shuffle: " + (on ? "aktiv" : "aus"));
    }

    public boolean isShuffleOn() {
        return shuffle;
    }

    /** Repeat-Modus an/aus */
    public void repeat(boolean on) {
        this.repeat = on;
        System.out.println("Repeat: " + (on ? "aktiv" : "aus"));
    }

    // Track/Seek/Time
    /** aktuellen Track für GUI */
    public Track getCurrentTrack() {
        Playlist pl = playlistProperty.get();
        if (pl != null && currentIndex >= 0 && currentIndex < pl.size()) {
            return pl.getTracks().get(currentIndex);
        }
        return null;
    }

    /** springt im Track auf eine bestimmte Sekunde > für den Zeit-Slider */
    public void seekToSeconds(int seconds) {
    	synchronized (audioLock) {
	    	
	        if (audioPlayer == null) return;
	
	        // clamp
	        if (seconds < 0) seconds = 0;
	
	        int targetMillis = seconds * 1000;
	        boolean playingNow = playingProperty.get() && !isPaused && !wasStopped;
	        
	        if(!playingNow) {
		        try {
		            // absolute Position setzen (wie Minim AudioPlayer)
		            audioPlayer.cue(targetMillis);
		
		            // Time-Property sofort synchron
		            setCurrentTime(seconds);
		            return;
		
		        } catch (Exception e) {
		            // fallback, falls cue() nicht verfügbar ist
		        }
	        }
	
	        int currentMillis = audioPlayer.position();
	        int diff = targetMillis - currentMillis;
	
	        System.out.println("Seek (fallback) zu " + seconds + "s (" + targetMillis
	                + " ms), aktuell " + currentMillis + " ms, diff=" + diff + " ms");
	
	        audioPlayer.skip(diff);
	        setCurrentTime(seconds);
    	}
    }

    /** Timer-Thread für currentTime */
    private void startTimeThread() {
        stopTimeThread(false);

        timeThread = new Thread(() -> {
        	int lastSec = -1;
        	
            while (!Thread.currentThread().isInterrupted()
                    && !wasStopped
                    && !isPaused
                    && playingProperty.get()) {

                int sec = 0;
                SimpleAudioPlayer ap;
                
                synchronized(audioLock) {
                	ap = audioPlayer;
                }
                
                if (ap != null) {
                    try {
                        sec = Math.max(0, ap.position() / 1000);
                    } catch (Exception ignored) {
                    }
                }

                if(sec != lastSec) {
                	lastSec = sec;
                	setCurrentTime(sec);
                }
                

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        timeThread.setDaemon(true);
        timeThread.start();
    }

    private void stopTimeThread(boolean resetTime) {
        if (timeThread != null && timeThread.isAlive()) {
            timeThread.interrupt();
        }
        timeThread = null;
        if (resetTime) {
            setCurrentTime(0);
        }
    }

    /**
     * Thread, der blockierendes audioPlayer.play() ausführt
     * danach wird Auto-Play gemacht (nächster Track), solange nicht Stop/Pause dazwischenkam
     */
    private class PlayThread extends Thread {
        private final int token;

        public PlayThread(int token) {
            this.token = token;
        }

        @Override
        public void run() {
            SimpleAudioPlayer ap;
            
            synchronized(audioLock) {
            	ap = audioPlayer;
            }
            
            if (ap == null) return;
            
            if (token != playToken) return;

            ap.play(); // blockiert bis Ende
            
            
            // Auto-Play noch nicht ansetzten bis sicher das song zu ende
            int pos = 0;
            int len = 0;
            try {
                pos = ap.position();
                len = ap.length();
            } catch (Exception ignored) {}
            
            boolean finishedByPos = (len > 0 && pos >= len - 300); // 300ms Puffer
            
            Track t = getCurrentTrack();
            int ct = getCurrentTime(); // oder getter, falls vorhanden
            int trackLenSec = (t != null) ? t.getLengthSec() : 0;
            boolean finishedByTime = (trackLenSec > 0 && ct >= trackLenSec - 1);

            
            // Nur wenn wirklich am Ende, gilt das als "Song zu Ende"
            boolean reallyFinished = finishedByPos || finishedByTime;

            if (!reallyFinished) {
                // play() wurde durch seek/cue/skip/pause/etc. beendet -> NICHT als Song-Ende behandeln
                return;
            }

            

            // wenn inzwischen eine neue Wiedergabe gestartet wurde -> ignorieren
            if (token != playToken) {
                System.out.println("→ Thread ignoriert (Token veraltet)");
                return;
            }

            // Song ist fertig -> Status sauber setzen
            //playingProperty.set(false);

            if (wasStopped) {
                System.out.println("→ Auto-Play NICHT gestartet (Stop)");
                playingProperty.set(false);
                return;
            }
            if (isPaused) {
                System.out.println("→ Auto-Play NICHT gestartet (Pause)");
                playingProperty.set(false);
                return;
            }
            
            System.out.println("Song zu Ende: " + getCurrentTrack());

            // Auto-Play: nächsten Index bestimmen
            Playlist pl = playlistProperty.get();
            if (pl == null || pl.size() == 0) {
            	playingProperty.set(false);
            	return;
            }

            int nextIndex;
            if (shuffle) {
                nextIndex = (int) (Math.random() * pl.size());
            } else {
                nextIndex = currentIndex + 1;
                if (nextIndex >= pl.size()) {
                    if (repeat) nextIndex = 0;
                    else {
                    	playingProperty.set(false);
                    	return;
                    }
                }
            }

            currentIndex = nextIndex;

            // nur weitermachen (Auto-Play), wenn Token immer noch aktuell
            if (token == playToken) {
                play();
            }
        }
    }
    
    private final Object seekLock = new Object();
    private volatile long seekToken = 0;

    /**
     * SeekThread lagert den potenziell blockierenden seekToSeconds()-Aufruf aus,
     * damit der JavaFX Application Thread nicht einfriert.
     * Token sorgt dafür, dass nur der letzte Seek zählt ("last one wins").
     */
    public class SeekThread extends Thread {
        private final int targetSeconds;
        private final long myToken;

        public SeekThread(int targetSeconds) {
            this.targetSeconds = targetSeconds;
            this.myToken = ++seekToken;
            setDaemon(true);
        }

        @Override
        public void run() {
            // Wenn schon veraltet, gar nicht erst arbeiten
            if (myToken != seekToken) return;

            // Audio-Zugriff serialisieren (verhindert parallele cue/skip Aufrufe)
            synchronized (seekLock) {
                // Falls während des Wartens ein neuer Seek kam -> abbrechen
                if (myToken != seekToken) return;

                seekToSeconds(targetSeconds);
            }

           
            setCurrentTime(targetSeconds);
            
        }
    }

    /** gibt die aktuelle Position des Players in Sekunden zurück */
    public int getCurrentPositionSeconds() {
        SimpleAudioPlayer ap = audioPlayer;
        if (ap == null) return 0;

        try {
            return ap.position() / 1000;
        } catch (Exception e) { // Null Pointer abfangen
            return 0;
        }
    }

    /** Länge des aktuellen Tracks in Sekunden (für Slider-Max) */
    public int getCurrentTrackLengthSeconds() {
        SimpleAudioPlayer ap = audioPlayer;
        if (ap != null) {
            try {
                return ap.length() / 1000;
            } catch (Exception ignored) {
            }
        }

        Playlist pl = playlistProperty.get();
        if (pl != null && currentIndex >= 0 && currentIndex < pl.size()) {
            int len = pl.getTracks().get(currentIndex).getLengthSec();
            if (len > 0) return len;
        }
        return 0;
    }

    public boolean isPlaying() {
        return playingProperty.get() && !isPaused && !wasStopped;
    }

    /** bestimmten Track aus der aktuellen Playlist abspielen, z.B. durch Klick */
    public void playTrack(Track track) {
        if (playlistProperty.get() == null || track == null) {
            System.out.println("Keine Playlist oder Track ist null");
            return;
        }

        int index = playlistProperty.get().getTracks().indexOf(track);
        if (index < 0) {
            System.out.println("Track nicht in aktueller Playlist gefunden");
            return;
        }

        // aktuellen Index setzen
        currentIndex = index;

        // Flags für Auto-Play richtig setzen
        wasStopped = false;
        isPaused = false;

        System.out.println("Spiele Track aus Playlist: " + track);

        // normalen Play-Mechanismus benutzen
        play();
    }

    /**
     * wenn neues Lied gewählt wird > immer erst stop()
     * currentIndex wird auf geklickten Eintrag gesetzt
     * danach startet genau ein PlayThread + AudioPlayer
     */
    public void playTrackAtIndex(int index) {
        Playlist pl = playlistProperty.get();
        if (pl == null || pl.size() == 0) {
            System.out.println("Keine Playlist gesetzt.");
            return;
        }

        if (index < 0 || index >= pl.size()) {
            System.out.println("Index außerhalb der Playlist: " + index);
            return;
        }

        // beendet laufende Wiedergabe
        stop();  // setzt wasStopped=true, pausiert aktuellen Player

        currentIndex = index;
        play();  // nutzt bestehende play()-Logik mit Auto-Play
    }
}