package de.hsrm.mi.enia.moodplayer.business;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import de.hsrm.mi.eibo.simpleplayer.SimpleAudioPlayer;
import de.hsrm.mi.eibo.simpleplayer.SimpleMinim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * - lädt und verwaltet Playlists aus Dateien (z.B. M3U)
 * - liest außerdem Metadaten aus MP3-Dateien und bestimmt Track-Längen
 */
public class PlaylistManager {

    private final SimpleMinim minim = new SimpleMinim(false);
    private final TagManager tagManager;

    public PlaylistManager(TagManager tagManager) {
        this.tagManager = tagManager;
    }

    /**
     * Lädt eine Playlist aus einer M3U-Datei.
     * - unterstützt #EXTINF Zeilen & relative Pfade zur M3U-Datei
     * - Track enthält:
     *    playPath = absoluter/aufgelöster Pfad zur MP3 (Abspielen)
     *    tagKey = Originalzeile aus M3U (Mood lookup)
     */
    public Playlist loadM3U(String m3uFilePath) {
        System.out.println("[PlaylistManager] Lade M3U: " + m3uFilePath);

        File m3uFile = new File(m3uFilePath);
        System.out.println("[PlaylistManager] Absoluter Pfad: " + m3uFile.getAbsolutePath());

        if (!m3uFile.exists()) {
            System.err.println("[PlaylistManager] M3U-Datei nicht gefunden: " + m3uFilePath);
            new Exception("Playlist load call trace").printStackTrace();
            return null;
        }

        String playlistName = m3uFile.getName();
        if (playlistName.toLowerCase().endsWith(".m3u")) {
            playlistName = playlistName.substring(0, playlistName.length() - 4);
        }

        Playlist playlist = new Playlist(playlistName);

        try (BufferedReader reader = new BufferedReader(new FileReader(m3uFile))) {

            String line;
            String extinfLine = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("#EXTM3U")) continue;

                if (line.startsWith("#EXTINF:")) {
                    extinfLine = line;
                    continue;
                }

                if (line.startsWith("#")) continue;

                String m3uPathKey = line;

                File mp3File = resolveAgainstM3U(m3uFile, m3uPathKey);
                if (!mp3File.exists()) {
                    System.err.println("[PlaylistManager] MP3 nicht gefunden: " + mp3File.getAbsolutePath());
                    extinfLine = null;
                    continue;
                }

                Track track = createTrackFromMP3(mp3File, extinfLine, m3uPathKey);
                if (track != null) {

                    if (tagManager != null) {
                        MoodProfile mp = tagManager.getForM3ULine(track.getTagKey());
                        track.setMoodProfile(mp);
                        System.out.println("[Tags] " + track.getTagKey() + " -> " + mp);
                    }

                    if (track.getLengthSec() <= 0) {
                        int lenSec = readLengthSeconds(mp3File);
                        track.setLengthSec(lenSec);
                    }

                    playlist.addTrack(track);
                    System.out.println("[PlaylistManager] Track hinzugefügt: " + track + " (" + track.getLengthSec() + "s)");
                }

                extinfLine = null;
            }

        } catch (IOException e) {
            System.err.println("[PlaylistManager] Fehler beim Lesen der M3U-Datei: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[PlaylistManager] Playlist geladen: " + playlist.size() + " Tracks");
        return playlist;
    }

    private File resolveAgainstM3U(File m3uFile, String pathFromM3U) {
        File f = new File(pathFromM3U);
        if (f.isAbsolute()) return f;

        File m3uDir = m3uFile.getParentFile();
        if (m3uDir == null) return f;
        return new File(m3uDir, pathFromM3U);
    }

    /**
     * erzeugt ein Track-Objekt aus MP3-Datei und liest ID3-Tags (Title/Artist/Album)
     * falls ID3 fehlt, wird #EXTINF als Fallback genutzt
     */
    private Track createTrackFromMP3(File mp3File, String extinfLine, String tagKey) {

        String playPath = mp3File.getPath();
        String title = mp3File.getName();
        String artist = "Unbekannt";
        String album = "Unbekannt";
        int lengthSec = 0;

        try {
            Mp3File mp3 = new Mp3File(mp3File);

            int sec = (int) mp3.getLengthInSeconds();
            if (sec > 0) lengthSec = sec;

            if (mp3.hasId3v2Tag()) {
                ID3v2 id3v2 = mp3.getId3v2Tag();

                if (id3v2.getTitle() != null && !id3v2.getTitle().isEmpty()) title = id3v2.getTitle();
                if (id3v2.getArtist() != null && !id3v2.getArtist().isEmpty()) artist = id3v2.getArtist();
                if (id3v2.getAlbum() != null && !id3v2.getAlbum().isEmpty()) album = id3v2.getAlbum();

            } else if (mp3.hasId3v1Tag()) {
                ID3v1 id3v1 = mp3.getId3v1Tag();

                if (id3v1.getTitle() != null && !id3v1.getTitle().isEmpty()) title = id3v1.getTitle();
                if (id3v1.getArtist() != null && !id3v1.getArtist().isEmpty()) artist = id3v1.getArtist();
                if (id3v1.getAlbum() != null && !id3v1.getAlbum().isEmpty()) album = id3v1.getAlbum();
            }

        } catch (Exception e) {
            System.err.println("[PlaylistManager] Fehler beim Lesen der ID3-Tags: " + mp3File.getName());
        }

        if (extinfLine != null && (title.equals(mp3File.getName()) || "Unbekannt".equals(artist))) {
            try {
                String info = extinfLine.substring(extinfLine.indexOf(',') + 1).trim();

                if (info.contains(" - ")) {
                    String[] parts = info.split(" - ", 2);
                    if ("Unbekannt".equals(artist)) artist = parts[0].trim();
                    if (title.equals(mp3File.getName())) title = parts[1].trim();
                } else {
                    if (title.equals(mp3File.getName())) title = info;
                }
            } catch (Exception ignored) {}
        }

        return new Track(playPath, tagKey, title, artist, album, lengthSec);
    }

    private Track createTrackFromMP3(File mp3File, String extinfLine) {
        String key = mp3File.getPath();
        return createTrackFromMP3(mp3File, extinfLine, key);
    }

    public Playlist getAllTracks(String folderPath, String playlistName) {
        System.out.println("[PlaylistManager] Scanne Ordner: " + folderPath);

        Playlist playlist = new Playlist(playlistName);
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("[PlaylistManager] Ordner nicht gefunden: " + folderPath);
            return playlist;
        }

        scanFolder(folder, playlist);

        System.out.println("[PlaylistManager] Scan abgeschlossen: " + playlist.size() + " Tracks");
        return playlist;
    }

    private void scanFolder(File folder, Playlist playlist) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanFolder(file, playlist);
            } else if (file.getName().toLowerCase().endsWith(".mp3")) {
                Track track = createTrackFromMP3(file, null);
                if (track != null) {
                    if (tagManager != null) {
                        track.setMoodProfile(tagManager.getForM3ULine(track.getTagKey()));
                    }
                    if (track.getLengthSec() <= 0) {
                        track.setLengthSec(readLengthSeconds(file));
                    }
                    playlist.addTrack(track);
                }
            }
        }
    }

    private int readLengthSeconds(File mp3File) {
        try {
            SimpleAudioPlayer p = minim.loadMP3File(mp3File.getPath());
            int ms = p.length();
            p.pause();
            return Math.max(0, ms / 1000);
        } catch (Exception e) {
            System.out.println("[PlaylistManager] Länge nicht lesbar: " + mp3File.getPath());
            return 0;
        }
    }
}
