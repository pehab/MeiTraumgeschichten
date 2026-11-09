package de.haberland.meitraumgeschichten.core;

import java.util.BitSet;

/** Only the current and immediately following cue can fire. Never scans a whole story. */
public final class ReadingSession {
    private final Story story;
    private final BitSet played = new BitSet();
    private int next;

    public ReadingSession(Story story) { this.story = story; }
    public int nextScene() { return next; }
    public boolean finished() { return next == story.scenes.size(); }
    public boolean played(int index) { return played.get(index); }

    public int accept(String speech) {
        for (int index = next; index < Math.min(story.scenes.size(), next + 2); index++) {
            if (!played.get(index) && Words.containsCue(speech, story.scenes.get(index).anchor)) {
                mark(index);
                return index;
            }
        }
        return -1;
    }

    /** Explicit manual playback counts as played, avoiding a second automatic effect. */
    public void mark(int index) {
        validate(index);
        played.set(index);
        next = Math.max(next, index + 1);
        advancePastPlayed();
    }

    /** A user can deliberately skip ahead or resume an earlier section. */
    public void seek(int index) { validate(index); next = index; advancePastPlayed(); }
    private void advancePastPlayed() {
        while (next < story.scenes.size() && played.get(next)) next++;
    }
    private void validate(int index) {
        if (index < 0 || index >= story.scenes.size()) throw new IllegalArgumentException("Invalid scene");
    }

    public String save() {
        StringBuilder result = new StringBuilder(Integer.toString(next));
        for (int i = played.nextSetBit(0); i >= 0; i = played.nextSetBit(i + 1)) result.append(',').append(i);
        return result.toString();
    }

    public void restore(String state) {
        next = 0; played.clear();
        if (state == null || state.isBlank()) return;
        try {
            String[] values = state.split(",", -1);
            int cursor = Integer.parseInt(values[0]);
            if (cursor < 0 || cursor > story.scenes.size()) return;
            BitSet restored = new BitSet();
            for (int i = 1; i < values.length; i++) {
                int index = Integer.parseInt(values[i]); validate(index); restored.set(index);
            }
            played.or(restored); next = cursor; advancePastPlayed();
        } catch (IllegalArgumentException ignored) { next = 0; played.clear(); }
    }
}
