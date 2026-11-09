package de.haberland.meitraumgeschichten.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StoryParser {
    public static final Set<String> SOUNDS = Set.of("wind", "rain", "steps", "knock", "owl",
            "river", "chime", "rustle", "snore", "bell", "creak", "breathe");
    private StoryParser() { }

    public static Story parse(String id, String source) {
        if (source == null) throw new IllegalArgumentException("Missing story");
        String[] sections = source.replace("\r\n", "\n").strip().split("\n---\n");
        if (sections.length < 2) throw new IllegalArgumentException("No scenes: " + id);
        Map<String, String> meta = new HashMap<>();
        for (String line : sections[0].split("\n")) {
            String[] pair = line.split("=", 2);
            if (pair.length != 2 || pair[1].isBlank()) throw new IllegalArgumentException("Bad metadata: " + id);
            meta.put(pair[0], pair[1]);
        }
        for (String required : List.of("title", "subtitle", "theme", "character", "question", "age"))
            if (!meta.containsKey(required)) throw new IllegalArgumentException("Missing " + required);
        int age = Integer.parseInt(meta.get("age"));
        if (age < 3 || age > 12) throw new IllegalArgumentException("Invalid age");
        List<Story.Scene> scenes = new ArrayList<>();
        Set<String> anchors = new java.util.HashSet<>();
        for (int i = 1; i < sections.length; i++) {
            String[] scene = sections[i].strip().split("\n", 2);
            String[] cue = scene[0].split("\\|", -1);
            if (scene.length != 2 || cue.length != 3 || !SOUNDS.contains(cue[0])
                    || cue[2].isBlank() || Words.tokens(cue[1]).size() < 3
                    || !Words.containsCue(scene[1], cue[1]) || !anchors.add(cue[1]))
                throw new IllegalArgumentException("Invalid cue in " + id + " scene " + i);
            scenes.add(new Story.Scene(cue[0], cue[1], cue[2], scene[1].strip()));
        }
        return new Story(id, meta.get("title"), meta.get("subtitle"), meta.get("theme"),
                meta.get("character"), meta.get("question"), age, scenes);
    }
}
