package de.hsrm.mi.enia.moodplayer.business;

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.Map;
import java.util.HashMap;

/* lädt/verwaltet Zuordnung Track -> Mood */

public class TagManager {
    private final Map<String, MoodProfile> byFile = new HashMap<>();// path: moods

    public void loadCsv(String csvPath) throws IOException {
        System.out.println("[TagManager] Lade CSV: " + csvPath);

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            int lineNo = 0;

            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                // Header überspringen (file,moods)
                String lower = line.toLowerCase();
                if (lower.startsWith("file") && lower.contains("mood")) {
                    System.out.println("[TagManager] Header übersprungen");
                    continue;
                }

                // Trennung: , oder ;
                String[] parts = line.contains(";")
                        ? line.split(";", 2)
                        : line.split(",", 2);

                if (parts.length < 2) {
                    System.out.println("[TagManager] Ungültige Zeile @" + lineNo + ": " + line);
                    continue;
                }

                String file = normalize(parts[0]).trim();
                String moodsStr = parts[1].trim();

                System.out.println("[TagManager] Zeile @" + lineNo);
                System.out.println("  file raw : " + parts[0]);
                System.out.println("  file key : " + file);
                System.out.println("  moods raw: " + moodsStr);

                MoodProfile mp = new MoodProfile();

                // akzeptiert | oder , als Trenner
                String[] tokens = moodsStr.split("\\s*[\\|,]\\s*");
                for (String tok : tokens) {
                    if (tok.isBlank()) continue;

                    Mood mood = null;
                    try {
                        mood = Mood.fromString(tok);
                    } catch (Exception e) {
                        System.out.println("  [ERROR] fromString failed for '" + tok + "'");
                    }

                    System.out.println("  token='" + tok + "' -> " + mood);
                    mp.add(mood);
                }

                System.out.println("  ==> gespeicherte moods: " + mp.getMoods());
                byFile.put(file, mp);
            }
        }

        System.out.println("[TagManager] CSV geladen, Einträge: " + byFile.size());
    }


    public int size() {
    	return byFile.size();
    	
    }
    // normalize um CSV Datei Betriebssystem abhängig ablesen zu können, da sie jenachdem unterschiedliche Zeichen enthält
    private String normalize(String p) {
        if (p == null) return "";
        return p
            .replace("\uFEFF", "") // BOM entfernen
            .replace("\\", "/") // Backslashes vereinheitlichen
            .replaceAll("[\\r\\n\\t]", "") // CR/LF/Tabs killen
            .trim()
            .replace("\"", ""); // Anführungszeichen entfernen
    }

    // wenn ein song keine Moods hat, 
    // wird genau der Pfad des Songs von der ursprünglichen playlist (weil er gleich dem Pfad in der CSV Datei ist)genommen und in der HashMap mit leerem MoodProfile gesetzt
    public MoodProfile getForM3ULine(String m3uLinePath) {
        // return byFile.getOrDefault(normalize(m3uLinePath), new MoodProfile());
    	String key = normalize(m3uLinePath);
        boolean found = byFile.containsKey(key);
        MoodProfile mp = byFile.getOrDefault(key, new MoodProfile());
        System.out.println("[TagManager] lookup=" + key + " found=" + found + " moods=" + mp.getMoods());
        return mp;
    }

}





