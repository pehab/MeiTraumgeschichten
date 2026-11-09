package de.haberland.meitraumgeschichten;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import java.io.IOException;

/** One short, gently faded effect at a time; no sounds leak into the background. */
final class StoryAudio {
    private final Context context;
    private final AudioManager manager;
    private final AudioFocusRequest focus;
    private MediaPlayer player;
    private float volume = .45f;
    private Runnable interrupted = () -> { };

    StoryAudio(Context context) {
        this.context = context;
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes()).setOnAudioFocusChangeListener(change -> {
                    if (change < 0) { stop(); interrupted.run(); }
                }).build();
    }
    private static AudioAttributes attributes() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    }
    void onInterrupt(Runnable callback) { interrupted = callback; }
    void volume(float value) { volume = Math.max(0f, Math.min(.8f, value)); }
    boolean play(String sound, boolean ending) {
        stop();
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false;
        try (AssetFileDescriptor asset = context.getAssets().openFd("sounds/" + sound + ".wav")) {
            MediaPlayer current = new MediaPlayer(); player = current;
            current.setAudioAttributes(attributes());
            current.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
            float level = ending ? volume * .65f : volume;
            current.setVolume(level, level);
            current.setOnCompletionListener(done -> { if (player == done) stop(); });
            current.setOnErrorListener((done, what, extra) -> { if (player == done) stop(); return true; });
            current.prepare(); current.start(); return true;
        } catch (IOException | RuntimeException error) { stop(); return false; }
    }
    void stop() {
        if (player != null) { player.release(); player = null; }
        manager.abandonAudioFocusRequest(focus);
    }
}
