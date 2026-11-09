package de.haberland.meitraumgeschichten.core;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CoreTest {
    private static Story small() {
        List<Story.Scene> scenes = new ArrayList<>();
        for (String cue : List.of("der wind streicht", "die maus klopft", "das wasser fliesst", "der drache schlaft"))
            scenes.add(new Story.Scene("wind", cue, "Wind", cue));
        return new Story("test", "Titel", "Untertitel", "Mut", "dragon", "Und du?", 4, scenes);
    }
    private static String content() {
        return "title=Titel\nsubtitle=Eine Geschichte\ntheme=Mut\ncharacter=owl\nquestion=Und du?\nage=4\n---\nwind|der wind streicht|Wind\nDer Wind streicht durch die Bäume.";
    }
    private static void rejects(String value) {
        assertThrows(IllegalArgumentException.class, () -> StoryParser.parse("test", value));
    }

    @Test public void normalizesGermanCaseUmlautsAndPunctuation() {
        assertEquals(List.of("grosse", "fusse", "mude", "eule"), Words.tokens("GROẞE Füße, müde Eule!"));
        assertEquals(List.of(), Words.tokens(null));
        assertEquals(List.of(), Words.tokens("   "));
        assertEquals(List.of(), Words.tokens("!?"));
    }
    @Test public void acceptsExactAndInsertedWords() {
        assertTrue(Words.containsCue("und dann der wind streicht durch den Wald", "der wind streicht"));
        assertTrue(Words.containsCue("der leise wind streicht", "der wind streicht"));
        assertTrue(Words.containsCue("der ganz leise wind streicht", "der wind streicht"));
        assertTrue(Words.containsCue("der irgendwas der wind streicht", "der wind streicht"));
    }
    @Test public void rejectsSubstringsReorderedOrDistantWords() {
        assertFalse(Words.containsCue("der wind streicht", "wind"));
        assertFalse(Words.containsCue("anderer wind streicht", "der wind streicht"));
        assertFalse(Words.containsCue("wind der streicht", "der wind streicht"));
        assertFalse(Words.containsCue("der ganz besonders sehr leise wind streicht", "der wind streicht"));
        assertFalse(Words.containsCue("der wind", "der wind streicht"));
        assertFalse(Words.containsCue(null, "der wind streicht"));
    }
    @Test public void parsesActualScenesAndDuration() {
        Story parsed = StoryParser.parse("test", content().replace("\n", "\r\n"));
        assertEquals("test", parsed.id); assertEquals("Titel", parsed.title);
        assertEquals("Eine Geschichte", parsed.subtitle); assertEquals("Mut", parsed.theme);
        assertEquals("owl", parsed.character); assertEquals("Und du?", parsed.question);
        assertEquals(4, parsed.minimumAge); assertEquals(6, parsed.wordCount());
        assertEquals(1, parsed.minutes()); assertEquals("Wind", parsed.scenes.get(0).label);
        assertThrows(UnsupportedOperationException.class, () -> parsed.scenes.clear());
    }
    @Test public void rejectsMissingMetadataAndScenes() {
        rejects(null); rejects(""); rejects("title=Test"); rejects(content().replace("title=Titel\n", ""));
        rejects(content().replace("theme=Mut", "theme="));
        rejects(content().replace("question=Und du?", "broken"));
        rejects(content().replace("age=4", "age=12x"));
        rejects(content().replace("age=4", "age=2")); rejects(content().replace("age=4", "age=13"));
    }
    @Test public void rejectsInvalidCueDefinition() {
        rejects(content().replace("wind|der wind streicht|Wind", "unknown|der wind streicht|Wind"));
        rejects(content().replace("wind|der wind streicht|Wind", "wind|der wind streicht"));
        rejects(content().replace("|Wind\n", "|\n"));
        rejects(content().replace("|der wind streicht|", "|wind streicht|"));
        rejects(content().replace("|der wind streicht|", "|der bach fliesst|"));
        rejects(content().replace("\nDer Wind streicht durch die Bäume.", ""));
        rejects(content() + "\n---\nwind|der wind streicht|Wind\nDer Wind streicht.");
    }
    @Test public void onlyLooksNearCurrentPosition() {
        ReadingSession reader = new ReadingSession(small());
        assertEquals(-1, reader.accept("das wasser fliesst"));
        assertEquals(-1, reader.accept("hast du die brotdose gesehen"));
        assertEquals(0, reader.nextScene()); assertFalse(reader.finished());
        assertEquals(0, reader.accept("der wind streicht")); assertTrue(reader.played(0));
        assertEquals(1, reader.nextScene());
    }
    @Test public void partialAndFinalHypothesesNeverRepeatEffects() {
        ReadingSession reader = new ReadingSession(small());
        assertEquals(-1, reader.accept("der wind"));
        assertEquals(0, reader.accept("der wind streicht"));
        assertEquals(-1, reader.accept("der wind streicht durch den wald"));
        assertEquals(-1, reader.accept("der wind streicht"));
    }
    @Test public void canSkipOneMissedCue() {
        ReadingSession reader = new ReadingSession(small());
        assertEquals(1, reader.accept("die maus klopft")); assertFalse(reader.played(0));
        assertEquals(2, reader.nextScene());
        assertEquals(3, reader.accept("der drache schlaft")); assertTrue(reader.finished());
    }
    @Test public void manualPlaybackAndSeekHaveStableSemantics() {
        ReadingSession reader = new ReadingSession(small());
        reader.mark(1); assertEquals(2, reader.nextScene());
        reader.mark(0); assertEquals(2, reader.nextScene());
        reader.seek(0); assertEquals(2, reader.nextScene());
        assertEquals(-1, reader.accept("die maus klopft"));
        reader.mark(3); assertTrue(reader.finished());
        reader.seek(2); assertEquals(2, reader.nextScene());
        reader.mark(2); assertTrue(reader.finished());
        assertEquals(-1, reader.accept("der drache schlaft"));
    }
    @Test public void rejectsInvalidPositions() {
        ReadingSession reader = new ReadingSession(small());
        assertThrows(IllegalArgumentException.class, () -> reader.seek(-1));
        assertThrows(IllegalArgumentException.class, () -> reader.seek(4));
        assertThrows(IllegalArgumentException.class, () -> reader.mark(-1));
        assertThrows(IllegalArgumentException.class, () -> reader.mark(4));
    }
    @Test public void persistsProgressWithoutRepeatingPastCues() {
        ReadingSession original = new ReadingSession(small()); original.mark(0); original.mark(1);
        String state = original.save();
        ReadingSession reader = new ReadingSession(small()); reader.restore(state);
        assertEquals(state, reader.save()); assertEquals(2, reader.nextScene());
        assertTrue(reader.played(0)); assertTrue(reader.played(1)); assertFalse(reader.played(2));
        assertEquals(-1, reader.accept("die maus klopft"));
        assertEquals(2, reader.accept("das wasser fliesst"));
        assertEquals(3, reader.accept("der drache schlaft"));
        ReadingSession completed = new ReadingSession(small()); completed.restore(reader.save());
        assertTrue(completed.finished());
    }
    @Test public void corruptProgressIsRecoverable() {
        for (String value : new String[]{null, "", " ", "bad", "-1", "5", "2,bad", "2,-1", "2,4", "2,"}) {
            ReadingSession reader = new ReadingSession(small()); reader.mark(1); reader.restore(value);
            assertEquals(0, reader.nextScene()); assertFalse(reader.played(1));
        }
        ReadingSession reader = new ReadingSession(small()); reader.restore("0,0,1");
        assertEquals(2, reader.nextScene());
    }
    @Test public void allBundledStoriesAreFullLengthAndPlayable() throws Exception {
        Path directory = Path.of(System.getProperty("stories"));
        List<Path> paths;
        try (var files = Files.list(directory)) { paths = files.filter(p -> p.toString().endsWith(".story")).sorted().toList(); }
        assertEquals(3, paths.size());
        for (Path path : paths) {
            Story story = StoryParser.parse(path.getFileName().toString(), Files.readString(path));
            assertTrue(story.title + " has " + story.wordCount() + " words", story.wordCount() >= 1000);
            assertTrue(story.wordCount() < 1600); assertTrue(story.minutes() >= 9 && story.minutes() <= 14);
            assertEquals(12, story.scenes.size());
            ReadingSession reader = new ReadingSession(story);
            for (int i = 0; i < story.scenes.size(); i++) {
                Story.Scene scene = story.scenes.get(i);
                assertEquals(story.title + " scene " + i, i, reader.accept(scene.text));
                assertEquals(-1, reader.accept(scene.text));
                assertEquals(-1, reader.accept(scene.anchor));
            }
            assertTrue(reader.finished());
            System.out.println(story.title + ": " + story.wordCount() + " Wörter, ~" + story.minutes() + " Minuten, 12 Klänge");
        }
    }
}
