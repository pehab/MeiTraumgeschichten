package de.haberland.meitraumgeschichten.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Words {
    private Words() { }
    public static List<String> tokens(String text) {
        if (text == null || text.trim().isEmpty()) return java.util.Collections.emptyList();
        String clean = Normalizer.normalize(text.toLowerCase(Locale.GERMAN)
                .replace("ß", "ss"), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        List<String> result = new ArrayList<>();
        for (String token : clean.split("[^a-z0-9]+")) if (!token.isEmpty()) result.add(token);
        return result;
    }

    /** Whole-word phrase with a maximum of two inserted recognition words. */
    public static boolean containsCue(String speech, String anchor) {
        List<String> words = tokens(speech), target = tokens(anchor);
        if (target.size() < 3) return false;
        for (int start = 0; start < words.size(); start++) {
            if (!words.get(start).equals(target.get(0))) continue;
            int matched = 1, extra = 0;
            for (int at = start + 1; at < words.size() && matched < target.size(); at++) {
                if (words.get(at).equals(target.get(matched))) matched++;
                else if (++extra > 2) break;
            }
            if (matched == target.size()) return true;
        }
        return false;
    }
}
