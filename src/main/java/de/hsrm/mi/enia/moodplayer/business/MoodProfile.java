package de.hsrm.mi.enia.moodplayer.business;

import java.util.EnumSet;

/* speichert aktuelle Auswahl (eine Mood) */

public class MoodProfile {
	private final EnumSet<Mood> moods = EnumSet.noneOf(Mood.class);

    public void add(Mood m) { 
    	moods.add(m); 
    	}
    
    public boolean has(Mood m) { 
    	return moods.contains(m); 
    	}
    
    public boolean isEmpty() {
        return moods.isEmpty();
    }
    

    public EnumSet<Mood> getMoods() { 
        return EnumSet.copyOf(moods); 
    }
    
    @Override
    public String toString() {
        return moods.toString(); // oder "MoodProfile" + moods
    }

}
