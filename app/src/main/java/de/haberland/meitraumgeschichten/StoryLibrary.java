package de.haberland.meitraumgeschichten;

import android.content.Context;
import de.haberland.meitraumgeschichten.core.Story;
import de.haberland.meitraumgeschichten.core.StoryParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
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
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) != -1) bytes.write(buffer, 0, length);
                result.add(StoryParser.parse(name, new String(bytes.toByteArray(), StandardCharsets.UTF_8)));
            }
        }
        return result;
    }
}
