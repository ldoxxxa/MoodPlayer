package de.hsrm.mi.enia.moodplayer.business;

import java.util.EnumSet;

/* filtert Tracks anhand MoodProfile */

public class MoodFilter {
	
	private final TagManager tagManager;

    public MoodFilter(TagManager tagManager) {
        this.tagManager = tagManager;
    }
	

    public Playlist filter(Playlist source, EnumSet<Mood> wanted, MoodMatchMode mode, String nameSuffix) {
        // wenn kein Mood gewählt > komplette Playlist zurück
        if (wanted == null || wanted.isEmpty()) return source;

        Playlist out = new Playlist(source.getName() + " - " + nameSuffix);

        for (Track t : source.getTracks()) {		
			MoodProfile mp = tagManager.getForM3ULine(t.getTagKey());
            System.out.println("[Filter] file=" + t.getTagKey() + "' len=" + t.getTagKey().length() + " tagManagermoods=" + mp.getMoods());
            if (matches(mp, wanted, mode)) {
                out.addTrack(t);
                System.out.println("[Filter] ✓ hinzugefügt");
            } else {
                System.out.println("[Filter] ✗ verworfen");
            }
            
            
        }
        return out;
    }

        private boolean matches(MoodProfile mp,EnumSet<Mood> wanted, MoodMatchMode mode) {
                
            if (mp == null || mp.isEmpty()) return false;

            EnumSet<Mood> trackMoods = mp.getMoods();
            EnumSet<Mood> inter = EnumSet.copyOf(trackMoods);
            inter.retainAll(wanted);

            return switch (mode) {
                case ANY -> !inter.isEmpty();
                case ALL -> trackMoods.containsAll(wanted);
                case EXACTLY_ONE_OF -> inter.size() == 1;
            };
        }


}
