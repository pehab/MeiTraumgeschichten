package de.haberland.meitraumgeschichten;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.io.IOException;

/** Vosk audio stays in memory on this device. There is no server recognition fallback. */
final class OfflineNarration {
    interface Listener {
        void status(String message, boolean listening);
        void words(String text);
    }
    private final Context context;
    private final Listener listener;
    private Model model;
    private Recognizer recognizer;
    private SpeechService service;
    private boolean requested, loading, closed;
    private int generation;

    OfflineNarration(Context context, Listener listener) {
        this.context = context.getApplicationContext(); this.listener = listener;
    }

    void start() {
        if (closed || requested) return;
        requested = true;
        if (model != null) { listen(); return; }
        listener.status("Spracherkennung wird vorbereitet …", false);
        if (loading) return;
        loading = true;
        StorageService.unpack(context, "model-de", "offline-speech", loaded -> {
            loading = false;
            if (closed) { loaded.close(); return; }
            model = loaded;
            if (requested) listen();
        }, error -> {
            loading = false;
            if (closed || !requested) return;
            requested = false;
            listener.status("Sprachmodell nicht bereit. Du kannst die Klangknöpfe nutzen und es erneut versuchen.", false);
        });
    }

    private void listen() {
        final int token = ++generation;
        try {
            recognizer = new Recognizer(model, 16000f);
            service = new SpeechService(recognizer, 16000f);
            service.startListening(new RecognitionListener() {
                private void result(String value, String field) {
                    if (!requested || closed || token != generation) return;
                    try { listener.words(new JSONObject(value).optString(field, "")); }
                    catch (JSONException ignored) { /* An incomplete hypothesis is harmless. */ }
                }
                @Override public void onPartialResult(String value) { result(value, "partial"); }
                @Override public void onResult(String value) { result(value, "text"); }
                @Override public void onFinalResult(String value) { result(value, "text"); }
                @Override public void onError(Exception error) {
                    if (closed || token != generation) return;
                    stop();
                    listener.status("Mikrofon nicht verfügbar. Bitte erneut starten oder Klangknöpfe nutzen.", false);
                }
                @Override public void onTimeout() {
                    if (closed || token != generation) return;
                    stop(); listener.status("Vorlesen pausiert", false);
                }
            });
            listener.status("Ich höre mit · alles bleibt auf diesem Gerät", true);
        } catch (IOException | RuntimeException error) {
            stop();
            listener.status("Mikrofon nicht verfügbar. Du kannst alle Klänge antippen.", false);
        }
    }

    boolean active() { return requested; }
    void stop() {
        requested = false; generation++;
        if (service != null) { service.cancel(); service.shutdown(); service = null; }
        if (recognizer != null) { recognizer.close(); recognizer = null; }
    }
    void close() {
        closed = true; stop();
        if (model != null) { model.close(); model = null; }
    }
}
