package de.hamid.lumatv.stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.KeyEvent;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Remote-control behaviour of the Stage, including the regressions found in the 0.4 review. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class StageViewTest {
    private static final int UP = KeyEvent.KEYCODE_DPAD_UP, DOWN = KeyEvent.KEYCODE_DPAD_DOWN,
            LEFT = KeyEvent.KEYCODE_DPAD_LEFT, RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT,
            OK = KeyEvent.KEYCODE_DPAD_CENTER, BACK = KeyEvent.KEYCODE_BACK, MENU = KeyEvent.KEYCODE_MENU;
    private Activity activity;
    private SharedPreferences prefs;
    private TestSupport.FakeHost host;

    @Before public void setUp() {
        Scene.hardware = false;
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        prefs = activity.getSharedPreferences("luma_home", Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean("stage_look_motion", false).commit();
        host = new TestSupport.FakeHost();
    }

    private StageView stage() {
        Look look = new Look(prefs);
        look.migrate();
        StageView v = new StageView(activity, host, look);
        activity.setContentView(v);
        return v;
    }

    private static void idle() { shadowOf(Looper.getMainLooper()).idle(); }

    @Test public void homeTakesNoKeysBeforeTheAppsAreKnown() {
        StageView v = stage();
        TestSupport.press(v, UP, OK);
        assertEquals("screen=0", v.where().substring(1, 9));
        v.setCatalog(TestSupport.items(TestSupport.DEFAULTS, "TV settings"));
        assertTrue("focus is on the apps once they arrive", v.where().contains("zone=0"));
    }

    @Test public void okLaunchesTheFocusedApp() {
        StageView v = stage();
        v.setCatalog(TestSupport.items(TestSupport.DEFAULTS, "TV settings"));
        TestSupport.press(v, RIGHT, OK);
        idle();
        assertEquals(1, host.launched.size());
    }

    /** Review M1: an app disappears while the editor is open; OK must not crash. */
    @Test public void catalogShrinksUnderTheEditor() {
        prefs.edit().putString("stage_home_v1", "[\"com.netflix.ninja\"]").commit();
        StageView v = stage();
        List<HomeItem> all = TestSupport.items(TestSupport.DEFAULTS, "TV settings");
        v.setCatalog(all);
        v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));          // editor at Netflix
        TestSupport.press(v, RIGHT, RIGHT);
        for (int i = 0; i < 10; i++) TestSupport.press(v, DOWN);               // last "not on Home"
        v.setCatalog(all.subList(0, 3));                                        // apps uninstalled
        TestSupport.press(v, OK, UP, OK, LEFT, OK, BACK);
    }

    /** Review M2: only visible apps count against the nine places on Home. */
    @Test public void missingDefaultAppsDoNotFillHome() {
        StageView v = stage();                                                  // six default keys, five missing
        String[][] apps = {{"com.netflix.ninja", "Netflix"}, {"x.aa", "Aa"}, {"x.ab", "Ab"}, {"x.ac", "Ac"},
                {"x.ad", "Ad"}, {"x.ae", "Ae"}, {"x.af", "Af"}, {"x.ag", "Ag"}, {"x.ah", "Ah"}, {"x.ai", "Ai"},
                {"x.aj", "Aj"}};
        v.setCatalog(TestSupport.items(apps, null));                           // grid: Aa..Aj, Netflix
        TestSupport.press(v, UP, OK);                                           // All apps
        v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));          // Aa
        for (int i = 0; i < 4; i++) {
            TestSupport.press(v, RIGHT);                                        // Ab..Ae
            v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));
        }
        TestSupport.press(v, DOWN);                                             // Aj
        v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));
        TestSupport.press(v, LEFT);                                             // Ai
        v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));
        assertTrue("seven added, eight visible, no 'Home is full': " + host.toasts, host.toasts.isEmpty());
    }

    @Test public void lastVisibleAppStaysOnHome() {
        prefs.edit().putString("stage_home_v1", "[\"com.netflix.ninja\",\"missing.app\"]").commit();
        StageView v = stage();
        v.setCatalog(TestSupport.items(TestSupport.DEFAULTS, "TV settings"));
        TestSupport.press(v, UP, OK);
        // Netflix is in the grid; holding OK on it would empty Home.
        for (int i = 0; i < 20 && !v.focusLabel().startsWith("Netflix"); i++) TestSupport.press(v, RIGHT);
        v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));
        assertEquals(1, host.toasts.size());
    }

    @Test public void emptyHomeIsFilledOnFirstStart() {
        StageView v = stage();
        String[][] others = {{"a.one", "One"}, {"b.two", "Two"}, {"c.three", "Three"}};
        v.setCatalog(TestSupport.items(others, null));
        assertTrue(v.focusLabel().contains("1"));
        assertTrue(v.where().contains("zone=0"));
    }

    @Test public void talkBackLabels() {
        StageView v = stage();
        v.setCatalog(TestSupport.items(TestSupport.DEFAULTS, "TV settings"));
        assertTrue(v.focusLabel(), v.focusLabel().matches(".+, 1 of \\d"));
        TestSupport.press(v, UP);
        assertEquals("All apps", v.focusLabel());
        TestSupport.press(v, RIGHT, OK);
        assertTrue(v.focusLabel().startsWith("Background"));
    }

    @Test public void longSettingsPagesScroll() {
        StageView v = stage();
        v.setCatalog(TestSupport.items(TestSupport.DEFAULTS, "TV settings"));
        TestSupport.press(v, UP, RIGHT, OK);                                    // Settings
        for (int i = 0; i < 5; i++) TestSupport.press(v, DOWN);
        TestSupport.press(v, OK);                                               // TV & Luma
        for (int i = 0; i < 8; i++) TestSupport.press(v, DOWN);
        assertTrue(v.focusLabel(), v.focusLabel().startsWith("Back up"));
        TestSupport.press(v, OK);
        assertTrue(v.focusLabel(), v.focusLabel().startsWith("Back up now"));
        TestSupport.press(v, OK);
        assertEquals("29.10.2026 14:00", host.backup);
    }
}
