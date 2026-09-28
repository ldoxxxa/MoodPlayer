package de.hsrm.mi.enia.moodplayer.business;

/**
 * Datenklasse für einen Track/Song
 * - playPath: echter Pfad zur MP3 (zum Abspielen)
 * - tagKey: Key aus der M3U-Zeile (für Mood/CSV lookup)
 */
public class Track {

    private final String playPath; // fürs Abspielen
    private final String tagKey; // für moods.csv Matching

    private final String title;
    private final String artist;
    private final String album;

    private int lengthSec;
    private MoodProfile moodProfile = new MoodProfile();

    public Track(String playPath, String tagKey, String title, String artist, String album, int lengthSec) {
        this.playPath = playPath;
        this.tagKey = tagKey;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.lengthSec = lengthSec;
    }

    public String getPlayPath() {
        return playPath;
    }

    public String getTagKey() {
        return tagKey;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public int getLengthSec() {
        return lengthSec;
    }

    public void setLengthSec(int lengthSec) {
        this.lengthSec = lengthSec;
    }

    public MoodProfile getMoodProfile() {
        return moodProfile;
    }

    public void setMoodProfile(MoodProfile mp) {
        this.moodProfile = (mp != null) ? mp : new MoodProfile();
    }

    @Override
    public String toString() {
        String base = (title != null && !title.isBlank()) ? title : tagKey;
        if (artist != null && !artist.isBlank()) return base + " – " + artist;
        return base;
    }
}
