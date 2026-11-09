package de.haberland.meitraumgeschichten;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import de.haberland.meitraumgeschichten.core.ReadingSession;
import de.haberland.meitraumgeschichten.core.Story;
import de.meiapps.updates.GitHubUpdates;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements OfflineNarration.Listener {
    private static final String REPOSITORY = "pehab/MeiTraumgeschichten";
    private static final int INK = 0xFFF4E7D2, MUTED = 0xFFB8B5CD, GOLD = 0xFFE8BF7A;
    private SharedPreferences prefs;
    private List<Story> stories = List.of();
    private Story story;
    private ReadingSession session;
    private StoryAudio audio;
    private OfflineNarration narration;
    private GitHubUpdates updates;
    private AppUpdateManager playUpdates;
    private LinearLayout page;
    private ScrollView scroll;
    private TextView status, progress;
    private Button microphone;
    private final List<View> sceneViews = new ArrayList<>();
    private final List<TextView> paragraphs = new ArrayList<>();
    private int fontSize;
    private boolean foreground, quiet;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        prefs = getSharedPreferences("bedtime", MODE_PRIVATE);
        fontSize = prefs.getInt("font", 21); quiet = prefs.getBoolean("quiet", false);
        audio = new StoryAudio(this); audio.volume(prefs.getFloat("volume", .45f));
        audio.onInterrupt(this::pauseReading);
        narration = new OfflineNarration(this, this);
        updates = GitHubUpdates.attach(this, REPOSITORY, this::pauseReading);
        if (updates.fromPlay()) playUpdates = AppUpdateManagerFactory.create(this);
        try { stories = StoryLibrary.load(this); }
        catch (IOException | IllegalArgumentException error) {
            new AlertDialog.Builder(this).setTitle("Geschichten konnten nicht geöffnet werden")
                    .setMessage("Bitte installiere die App erneut. Deine Stimme wurde nicht aufgenommen.")
                    .setPositiveButton("Schließen", (d, n) -> finish()).setCancelable(false).show();
        }
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::back);
        String selected = state == null ? null : state.getString("story");
        for (Story candidate : stories) if (candidate.id.equals(selected)) { open(candidate, true); return; }
        library();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private LinearLayout row() { LinearLayout layout = new LinearLayout(this); layout.setGravity(Gravity.CENTER_VERTICAL); return layout; }
    private GradientDrawable surface(int color, int stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(20));
        if (stroke != 0) shape.setStroke(dp(1), stroke); return shape;
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setLineSpacing(dp(3), 1.12f); return view;
    }
    private TextView heading(String value, int size) {
        TextView view = text(value, size, INK); view.setTypeface(Typeface.create("serif", Typeface.NORMAL)); return view;
    }
    private void gap(LinearLayout target, int height) { View gap = new View(this); target.addView(gap, new LinearLayout.LayoutParams(1, dp(height))); }
    private Button button(String label, boolean primary, Runnable click) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(14);
        view.setTextColor(primary ? 0xFF26243B : INK); view.setMinHeight(dp(48)); view.setMinimumHeight(dp(48));
        view.setPadding(dp(14), dp(6), dp(14), dp(6)); view.setBackground(surface(primary ? GOLD : 0xFF30334D, 0));
        view.setOnClickListener(v -> click.run()); return view;
    }
    private void weighted(LinearLayout row, View view) { row.addView(view, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); }

    private void root() {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                quiet ? new int[]{0xFF101222, 0xFF161827} : new int[]{0xFF20243F, 0xFF171A2F}));
        page = column(); page.setPadding(dp(22), dp(16), dp(22), dp(12));
        FrameLayout.LayoutParams content = new FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels, dp(760)), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        outer.addView(page, content);
        outer.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                outer.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else outer.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(outer); outer.requestApplyInsets();
        if (quiet && story != null) {
            WindowManager.LayoutParams params = getWindow().getAttributes(); params.screenBrightness = .12f; getWindow().setAttributes(params);
        } else {
            WindowManager.LayoutParams params = getWindow().getAttributes(); params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; getWindow().setAttributes(params);
        }
    }
    private LinearLayout scrolling() {
        scroll = new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        LinearLayout body = column(); scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return body;
    }

    private void library() {
        pauseReading(); story = null; session = null; sceneViews.clear(); paragraphs.clear(); root();
        LinearLayout top = row();
        weighted(top, text("✦  MEI TRAUMGESCHICHTEN", 12, GOLD)); top.addView(button("⋯", false, this::menu)); page.addView(top);
        LinearLayout body = scrolling(); gap(body, 18);
        body.addView(heading("Ein kleines Abenteuer.\nEine gute Nacht.", 33)); gap(body, 9);
        body.addView(text("Deine Stimme erzählt. Der Wald hört mit.", 15, MUTED));
        body.addView(new NightArt(this, "owl"), new LinearLayout.LayoutParams(-1, dp(155)));
        String last = prefs.getString("last", "");
        for (Story item : stories) if (item.id.equals(last)) {
            ReadingSession saved = new ReadingSession(item); saved.restore(prefs.getString("session:" + item.id, ""));
            if (saved.nextScene() > 0 && !saved.finished()) {
                body.addView(button("Weiterlesen: " + item.title, true, () -> open(item, true))); gap(body, 18);
            }
        }
        body.addView(text("FÜR EUREN VORLESEABEND", 12, GOLD)); gap(body, 12);
        for (Story item : stories) {
            LinearLayout card = column(); card.setPadding(dp(18), dp(17), dp(18), dp(18));
            card.setBackground(surface(0xFF292D49, 0xFF41435D));
            LinearLayout title = row();
            weighted(title, text(item.theme + "  ·  ab " + item.minimumAge, 12, GOLD));
            title.addView(text("~ " + item.minutes() + " Min.", 12, MUTED)); card.addView(title); gap(card, 9);
            card.addView(heading(item.title, 24)); gap(card, 6); card.addView(text(item.subtitle, 15, MUTED)); gap(card, 14);
            card.addView(button("Geschichte öffnen  →", false, () -> choose(item)));
            body.addView(card); gap(body, 14);
        }
        gap(body, 8); body.addView(text("Drei eigene Geschichten · alle Klänge offline\nFür Kinder ab etwa 4 Jahren und ihre Vorlesemenschen.", 13, MUTED)); gap(body, 20);
    }

    private void choose(Story item) {
        ReadingSession saved = new ReadingSession(item); saved.restore(prefs.getString("session:" + item.id, ""));
        if (saved.nextScene() > 0 && !saved.finished()) {
            new AlertDialog.Builder(this).setTitle(item.title).setMessage("Hier habt ihr schon angefangen.")
                    .setPositiveButton("Weiterlesen", (d, n) -> open(item, true))
                    .setNeutralButton("Von vorn", (d, n) -> open(item, false)).setNegativeButton("Zurück", null).show();
        } else open(item, false);
    }
    private void open(Story item, boolean restore) {
        pauseReading(); story = item; session = new ReadingSession(story);
        if (restore) session.restore(prefs.getString("session:" + story.id, ""));
        save(); reader();
    }
    private void reader() {
        root(); sceneViews.clear(); paragraphs.clear();
        LinearLayout top = row(); top.addView(button("‹", false, this::back));
        weighted(top, text("   " + story.theme, 13, GOLD)); top.addView(button("⋯", false, this::menu)); page.addView(top); gap(page, 10);
        microphone = button("Vorlesen starten", true, this::toggleMicrophone); page.addView(microphone);
        status = text("Klänge antippen oder das Mikrofon starten.", 12, MUTED); gap(page, 6); page.addView(status);
        progress = text("", 12, GOLD); page.addView(progress); gap(page, 8);
        LinearLayout body = scrolling();
        body.addView(new NightArt(this, story.character), new LinearLayout.LayoutParams(-1, dp(140)));
        body.addView(heading(story.title, 30)); gap(body, 8);
        body.addView(text("~ " + story.minutes() + " Minuten · Macht Pausen, wann immer ihr möchtet.", 13, MUTED)); gap(body, 20);
        for (int i = 0; i < story.scenes.size(); i++) {
            final int index = i; Story.Scene scene = story.scenes.get(i);
            LinearLayout card = column(); card.setPadding(dp(14), dp(12), dp(14), dp(14));
            TextView paragraph = text(scene.text, fontSize, INK); paragraph.setTypeface(Typeface.create("serif", Typeface.NORMAL));
            paragraph.setLineSpacing(dp(7), 1.15f); card.addView(paragraph); paragraphs.add(paragraph); gap(card, 10);
            LinearLayout controls = row();
            weighted(controls, button("♪  " + scene.label, false, () -> manual(index)));
            Button seek = button("Hier weiterlesen", false, () -> seek(index));
            seek.setTextSize(11); controls.addView(seek); card.addView(controls);
            body.addView(card); sceneViews.add(card); gap(body, 10);
        }
        LinearLayout end = column(); end.setPadding(dp(16), dp(20), dp(16), dp(20)); end.setBackground(surface(0xFF30334C, 0));
        end.addView(heading("Noch ein leiser Gedanke", 23)); gap(end, 8); end.addView(text(story.question, 18, INK));
        gap(end, 8); end.addView(text("Nur wenn ihr mögt. Ihr dürft auch einfach einschlafen.", 13, MUTED)); gap(end, 14);
        end.addView(button("Gute Nacht · Geschichte beenden", true, () -> { session.mark(story.scenes.size() - 1); save(); library(); }));
        body.addView(end); gap(body, 20);
        LinearLayout footer = row(); weighted(footer, button("A−", false, () -> font(-2)));
        weighted(footer, button("A+", false, () -> font(2)));
        weighted(footer, button(quiet ? "Nachtmodus an" : "Nachtmodus", false, () -> {
            pauseReading(); quiet = !quiet; prefs.edit().putBoolean("quiet", quiet).apply(); reader();
        })); page.addView(footer);
        refresh();
        if (session.nextScene() > 0) scroll.post(() -> scrollTo(Math.max(0, session.nextScene() - 1)));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void font(int change) {
        fontSize = Math.max(17, Math.min(33, fontSize + change)); prefs.edit().putInt("font", fontSize).apply();
        for (TextView paragraph : paragraphs) paragraph.setTextSize(fontSize);
    }
    private void refresh() {
        if (story == null || progress == null) return;
        progress.setText(session.finished() ? "Alle Klänge gespielt · schlaft schön" : "Nächster Klang: " + (session.nextScene() + 1) + " von " + story.scenes.size());
        for (int i = 0; i < sceneViews.size(); i++) sceneViews.get(i).setBackground(surface(
                i == session.nextScene() ? 0xFF292D45 : Color.TRANSPARENT, i == session.nextScene() ? 0xFF655C54 : 0));
    }
    private void scrollTo(int index) {
        if (index >= 0 && index < sceneViews.size()) scroll.smoothScrollTo(0, sceneViews.get(index).getTop() - dp(12));
    }
    private void seek(int index) {
        boolean resume = narration.active(); narration.stop(); audio.stop(); session.seek(index); save(); refresh();
        status("Ab hier weiterlesen. Bereits gespielte Klänge kannst du antippen.", false);
        if (resume) narration.start();
    }
    private void manual(int index) {
        session.mark(index); save(); refresh(); play(index);
    }
    private void play(int index) {
        if (!audio.play(story.scenes.get(index).sound, index >= story.scenes.size() - 2))
            Toast.makeText(this, "Klang konnte gerade nicht abgespielt werden.", Toast.LENGTH_SHORT).show();
        if (session.finished()) { narration.stop(); status("Alle Klänge gespielt · gute Nacht", false); }
    }
    @Override public void words(String value) {
        if (!foreground || story == null || !narration.active()) return;
        int index = session.accept(value);
        if (index >= 0) { save(); refresh(); play(index); scrollTo(index); }
    }
    @Override public void status(String message, boolean listening) {
        if (story == null || status == null) return;
        status.setText(message);
        microphone.setText(narration.active() || listening ? "Vorlesen pausieren" : "Vorlesen starten");
    }
    private void toggleMicrophone() {
        if (narration.active()) { pauseReading(); return; }
        if (session.finished()) {
            Toast.makeText(this, "Alle Klänge sind gespielt. Für eine neue Runde die Geschichte von vorn öffnen.", Toast.LENGTH_LONG).show(); return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setTitle("Mit Geräuschen vorlesen")
                    .setMessage("Das Mikrofon erkennt passende Textstellen direkt auf diesem Gerät. Deine Stimme wird weder gespeichert noch hochgeladen. Ohne Freigabe kannst du jeden Klang antippen.")
                    .setPositiveButton("Mikrofon freigeben", (d, n) -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 7))
                    .setNegativeButton("Ohne Mikrofon", null).show(); return;
        }
        narration.start();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 7 && story != null && foreground) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) narration.start();
            else status("Ohne Mikrofon · tippe auf die Klangknöpfe.", false);
        }
    }
    private void pauseReading() {
        if (narration != null) narration.stop();
        if (audio != null) audio.stop();
        if (story != null && status != null) status("Vorlesen pausiert · tippe zum Fortsetzen", false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        save();
    }
    private void save() {
        if (story != null && session != null) prefs.edit().putString("last", story.id)
                .putString("session:" + story.id, session.save()).apply();
    }
    private void menu() {
        pauseReading();
        new AlertDialog.Builder(this).setTitle("MeiTraumgeschichten")
                .setItems(new String[]{"Lautstärke der Klänge", "Updates", "Über die App & Datenschutz", "Lesefortschritt zurücksetzen"}, (d, which) -> {
                    if (which == 0) volume();
                    if (which == 1) { if (updates.fromPlay()) checkPlay(true); else updates.settings(); }
                    if (which == 2) about();
                    if (which == 3) new AlertDialog.Builder(this).setTitle("Lesefortschritt zurücksetzen?")
                            .setMessage("Alle Geschichten beginnen wieder von vorn. Einstellungen bleiben erhalten.")
                            .setNegativeButton("Abbrechen", null).setPositiveButton("Zurücksetzen", (dialog, n) -> {
                                SharedPreferences.Editor edit = prefs.edit().remove("last");
                                for (Story item : stories) edit.remove("session:" + item.id); edit.apply();
                                story = null; session = null; library();
                            }).show();
                }).setNegativeButton("Schließen", null).show();
    }
    private void volume() {
        LinearLayout box = column(); box.setPadding(dp(24), dp(10), dp(24), dp(12));
        box.addView(text("Die Medienlautstärke stellst du mit den Gerätetasten ein. Hier passt du die Klänge dazu an.", 15, INK));
        SeekBar slider = new SeekBar(this); slider.setMax(80); slider.setProgress(Math.round(prefs.getFloat("volume", .45f) * 100)); box.addView(slider);
        slider.setContentDescription("Lautstärke der Klänge");
        new AlertDialog.Builder(this).setTitle("Leise Klänge").setView(box).setPositiveButton("Speichern", (d, n) -> {
            float value = slider.getProgress() / 100f; prefs.edit().putFloat("volume", value).apply(); audio.volume(value);
        }).setNegativeButton("Abbrechen", null).show();
    }
    private void about() {
        new AlertDialog.Builder(this).setTitle("MeiTraumgeschichten " + BuildConfig.VERSION_NAME)
                .setMessage("Eigene Geschichten für gemeinsame Vorleseabende.\n\nAlle Geschichten, Geräusche und die deutsche Spracherkennung sind offline enthalten. Mikrofonzugriff gibt es nur während des gestarteten Vorlesens. Audio und erkannte Texte werden nicht gespeichert oder versendet. Keine Konten, Werbung oder Analyse.\n\nNur die Update-Prüfung verbindet sich mit GitHub beziehungsweise Google Play.\n\nSpracherkennung: Vosk / Alpha Cephei, deutsches Modell small-de-0.15 (Apache 2.0). JNA: LGPL 2.1 oder Apache 2.0. Weitere Hinweise im Repository unter docs/THIRD_PARTY.md.")
                .setPositiveButton("Schließen", null).show();
    }
    private void checkPlay(boolean manual) {
        if (playUpdates == null) { GitHubUpdates.openPlay(this); return; }
        playUpdates.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (isFinishing() || isDestroyed() || !foreground) return;
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                new AlertDialog.Builder(this).setTitle("Update bereit").setMessage("Die App zum Aktualisieren neu starten?")
                        .setNegativeButton("Später", null).setPositiveButton("Neu starten", (d, n) -> { pauseReading(); playUpdates.completeUpdate(); }).show();
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                new AlertDialog.Builder(this).setTitle("Update über Google Play").setMessage("Eine neue Version ist verfügbar.")
                        .setNegativeButton("Später", null).setPositiveButton("Aktualisieren", (d, n) -> {
                            pauseReading();
                            try { playUpdates.startUpdateFlowForResult(info, this, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(), 20); }
                            catch (IntentSender.SendIntentException error) { GitHubUpdates.openPlay(this); }
                        }).show();
            } else if (manual) GitHubUpdates.openPlay(this);
        }).addOnFailureListener(error -> { if (manual && foreground) GitHubUpdates.openPlay(this); });
    }
    private void back() { if (story != null) library(); else finish(); }
    @SuppressWarnings("deprecation") @Override public void onBackPressed() { back(); }
    @Override protected void onResume() {
        super.onResume(); foreground = true;
        if (story == null && playUpdates != null) checkPlay(false);
        if (story != null) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override protected void onPause() { foreground = false; pauseReading(); super.onPause(); }
    @Override protected void onDestroy() { narration.close(); audio.stop(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        save(); if (story != null) state.putString("story", story.id); super.onSaveInstanceState(state);
    }
}
