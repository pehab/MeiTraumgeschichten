package de.haberland.meitraumgeschichten;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BedtimeSmokeTest {
    private Context context;
    @Before public void clearProgress() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("bedtime", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().putBoolean("automatic", false).commit();
    }

    @Test public void aLibraryAndManualReadingWorkWithoutMicrophonePermission() throws Exception {
        assertEquals(3, StoryLibrary.load(context).size());
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Milo und die Laterne für alle")).check(matches(isDisplayed()));
            onView(allOf(withText("Geschichte öffnen  →"), withParent(hasDescendant(withText("Milo und die Laterne für alle"))))).perform(scrollTo(), click());
            onView(withText("Vorlesen starten")).check(matches(isDisplayed()));
            onView(withText("♪  Abendwind")).perform(scrollTo(), click());
            onView(withText("Nächster Klang: 2 von 12")).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
            onView(withText("Weiterlesen: Milo und die Laterne für alle")).perform(scrollTo(), click());
            onView(withText("Nächster Klang: 2 von 12")).check(matches(isDisplayed()));
            onView(withText("Vorlesen starten")).perform(click());
            onView(withText("Ohne Mikrofon")).perform(click());
            onView(withText("Vorlesen starten")).check(matches(isDisplayed()));
        }
    }

    @Test public void zPackagedGermanModelCanStartAndReleaseMicrophone() throws Exception {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            verifyOfflineMicrophone();
        }
    }

    private void verifyOfflineMicrophone() throws Exception {
        String pkg = context.getPackageName();
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(pkg, Manifest.permission.RECORD_AUDIO);
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<OfflineNarration> reference = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            OfflineNarration narration = new OfflineNarration(context, new OfflineNarration.Listener() {
                @Override public void status(String message, boolean listening) {
                    if (listening) started.countDown();
                    else if (!message.contains("vorbereitet")) { failure.set(message); started.countDown(); }
                }
                @Override public void words(String text) { }
            });
            reference.set(narration); narration.start();
        });
        try {
            assertTrue("Offline recognition should start within 90 seconds", started.await(90, TimeUnit.SECONDS));
            assertNull(failure.get(), failure.get());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertTrue(reference.get().active()); reference.get().stop(); assertFalse(reference.get().active());
            });
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> reference.get().close());
        }
    }
}
