package de.haberland.meitraumgeschichten;

import android.content.Context;
import de.haberland.meitraumgeschichten.core.Story;
import de.haberland.meitraumgeschichten.core.StoryParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class StoryLibrary {
    private StoryLibrary() { }
    static List<Story> load(Context context) throws IOException {
        String[] names = context.getAssets().list("stories");
        if (names == null || names.length == 0) throw new IOException("No bundled stories");
        Arrays.sort(names);
        List<Story> result = new ArrayList<>();
        for (String name : names) {
            if (!name.endsWith(".story")) continue;
            try (InputStream in = context.getAssets().open("stories/" + name)) {
                result.add(StoryParser.parse(name, new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            }
        }
        return result;
    }
}
