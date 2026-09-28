package de.hsrm.mi.enia.moodplayer.business;

public enum Mood {
	HAPPY, 
	SAD, 
	ENERGETIC, 
	FOCUSED, 
	ANGRY, 
	JOYFUL, 
	CALM,  
	PEACEFUL, 
	STRESSED,
	OPTIMISTIC,
	NOSTALGIC,
	GRIEF;

	// konvertiert eine String-Eingabe in Mood (enum)
    public static Mood fromString(String s) {
        return Mood.valueOf(s.trim().toUpperCase());
    }
}
