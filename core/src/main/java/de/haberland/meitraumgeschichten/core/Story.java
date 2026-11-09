package de.haberland.meitraumgeschichten.core;

import java.util.List;

/** Immutable content. Each scene has one cue anchored in its actual text. */
public final class Story {
    public final String id, title, subtitle, theme, character, question;
    public final int minimumAge;
    public final List<Scene> scenes;

    public Story(String id, String title, String subtitle, String theme, String character,
                 String question, int minimumAge, List<Scene> scenes) {
        this.id = id; this.title = title; this.subtitle = subtitle; this.theme = theme;
        this.character = character; this.question = question; this.minimumAge = minimumAge;
        this.scenes = List.copyOf(scenes);
    }

    public int wordCount() {
        int count = 0;
        for (Scene scene : scenes) count += Words.tokens(scene.text).size();
        return count;
    }

    /** Includes pauses and character voices: an estimate, not a timer. */
    public int minutes() { return Math.max(1, Math.round(wordCount() / 115f)); }

    public static final class Scene {
        public final String sound, anchor, label, text;
        public Scene(String sound, String anchor, String label, String text) {
            this.sound = sound; this.anchor = anchor; this.label = label; this.text = text;
        }
    }
}
