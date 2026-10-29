package de.hamid.lumatv.stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import de.hamid.lumatv.R;

/** Pure logic of the Stage: greeting, geometry, settings migration, crash guard, names. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LogicTest {
    private SharedPreferences prefs;

    @Before public void setUp() {
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences("luma_home", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    // ------------------------------------------------------------------ greeting

    @Test public void greetingByHour() {
        int[][] cases = {{0, R.string.greeting_night}, {4, R.string.greeting_night}, {5, R.string.greeting_morning},
                {10, R.string.greeting_morning}, {11, R.string.greeting_day}, {16, R.string.greeting_day},
                {17, R.string.greeting_evening}, {21, R.string.greeting_evening}, {22, R.string.greeting_night},
                {23, R.string.greeting_night}};
        for (int[] c : cases) assertEquals("hour " + c[0], c[1], Greeting.forHour(c[0]));
    }

    @Test public void nightIsAnEveningGreetingInGermanAndEnglish() {
        Context context = RuntimeEnvironment.getApplication();
        assertEquals("Good evening", context.getString(R.string.greeting_night));
    }

    @Test @Config(qualifiers = "de") public void nightGreetingGerman() {
        assertEquals("Guten Abend", RuntimeEnvironment.getApplication().getString(R.string.greeting_night));
    }

    @Test public void greetingText() {
        String fmt = "%1$s, %2$s";
        assertEquals("", Greeting.text(false, "Hamid", "Good evening", fmt));
        assertEquals("Good evening", Greeting.text(true, "   ", "Good evening", fmt));
        assertEquals("Good evening", Greeting.text(true, null, "Good evening", fmt));
        assertEquals("Good evening, Hamid", Greeting.text(true, " Hamid ", "Good evening", fmt));
    }

    // ------------------------------------------------------------------ geometry

    @Test public void sixNamedAppsFitTheDock() {
        HomePainter.Geom g = HomePainter.dock(Look.SHOW_BOTH, 6);
        assertFalse("six apps must not scroll", g.broad);
        assertTrue(6 * g.pitch - 8 <= HomePainter.SHELF_MAX + .01f);
        assertTrue("tiles shrink only a little", g.tw >= 276);
        assertEquals(g.shelf + 32, g.w, .01f);
    }

    @Test public void sevenNamedAppsScroll() {
        HomePainter.Geom g = HomePainter.dock(Look.SHOW_BOTH, 7);
        assertTrue(g.broad);
        assertEquals(296, g.pitch, .01f);
        assertEquals(HomePainter.SHELF_MAX, g.shelf, .01f);
    }

    @Test public void iconsDockIsAsWideAsItsIcons() {
        HomePainter.Geom g = HomePainter.dock(Look.SHOW_ICONS, 9);
        assertFalse(g.broad);
        assertEquals(9 * 184 - 8, g.shelf, .01f);
        assertEquals((1920 - g.w) / 2, g.x, 1f);
    }

    @Test public void listFitsSixRows() {
        assertTrue(HomePainter.listFits(6));
        assertFalse(HomePainter.listFits(7));
        assertEquals(0, HomePainter.listFirst(6, 5, 2));
        assertTrue(HomePainter.listFirst(9, 8, 0) > 0);
    }

    // ------------------------------------------------------------------ settings

    @Test public void migrationFreezesTheFirstRunHome() {
        Look look = new Look(prefs);
        look.migrate();
        assertTrue(prefs.contains("stage_home_v1"));
        assertTrue(look.homeIsAuto());
        assertEquals(Look.SCHEMA, prefs.getInt(Look.KEY_SCHEMA, 0));
        List<String> before = look.homeKeys();
        look.migrate();
        assertEquals(before, look.homeKeys());
        look.saveHomeKeys(Arrays.asList("com.netflix.ninja"));
        assertFalse("editing ends the automatic selection", look.homeIsAuto());
    }

    @Test public void migrationKeepsAnEditedHome() {
        prefs.edit().putString("stage_home_v1", "[\"com.netflix.ninja\"]").commit();
        Look look = new Look(prefs);
        look.migrate();
        assertEquals(Arrays.asList("com.netflix.ninja"), look.homeKeys());
        assertFalse(look.homeIsAuto());
    }

    @Test public void unknownValuesFallBack() {
        prefs.edit().putString("stage_look_layout", "grid").putString("stage_look_wallpaper", "gone").putInt("clock_mode_v1", 7).commit();
        Look look = new Look(prefs);
        assertEquals(Look.LAYOUT_DOCK, look.layout);
        assertEquals(Walls.DEFAULT_ID, look.wallpaper);
        assertEquals(0, look.clockMode());
        assertFalse(look.greetingOn());
    }

    @Test public void homeKeysAreDedupedAndCapped() {
        prefs.edit().putString("stage_home_v1", "[\"a\",\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\"]").commit();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i"), new Look(prefs).homeKeys());
        prefs.edit().putString("stage_home_v1", "{broken").commit();
        assertEquals(6, new Look(prefs).homeKeys().size());
    }

    @Test public void ps4MovesWithItsInput() {
        Look look = new Look(prefs);
        look.saveHomeKeys(Arrays.asList("com.netflix.ninja", "input:hdmi1"));
        look.movePs4Key("hdmi1", "hdmi2");
        assertEquals(Arrays.asList("com.netflix.ninja", "input:hdmi2"), look.homeKeys());
        look.movePs4Key("hdmi2", null);
        assertEquals(Arrays.asList("com.netflix.ninja"), look.homeKeys());
        look.movePs4Key(null, "hdmi3");
        assertEquals(Arrays.asList("com.netflix.ninja", "input:hdmi3"), look.homeKeys());
    }

    @Test public void safeModeDropsThePhotoUntilABackgroundIsChosen() {
        Look look = new Look(prefs, true);
        assertFalse(look.photo());
        assertFalse(look.materialApplies());
        assertTrue(look.set("bg", Look.BG_PHOTO));
        assertTrue(look.photo());
        assertFalse(look.safeMode());
    }

    // ------------------------------------------------------------------ crash guard

    @Test public void threeStartsWithoutAFrameStartSafely() {
        assertFalse(SafeStart.enter(prefs, 7, 1_000));
        assertFalse(SafeStart.enter(prefs, 7, 5_000));
        assertTrue(SafeStart.enter(prefs, 7, 9_000));
        assertTrue("stays safe for this boot", SafeStart.enter(prefs, 7, 400_000));
        assertFalse("a restart ends it", SafeStart.enter(prefs, 8, 2_000));
    }

    @Test public void aDrawnFrameResetsTheCount() {
        assertFalse(SafeStart.enter(prefs, 7, 1_000));
        assertFalse(SafeStart.enter(prefs, 7, 2_000));
        SafeStart.ok(prefs);
        assertFalse(SafeStart.enter(prefs, 7, 3_000));
        assertFalse(SafeStart.enter(prefs, 7, 4_000));
    }

    @Test public void slowStartsAreNotACrashLoop() {
        assertFalse(SafeStart.enter(prefs, 7, 1_000));
        assertFalse(SafeStart.enter(prefs, 7, 2_000));
        assertFalse(SafeStart.enter(prefs, 7, 2_000 + SafeStart.WINDOW_MS + 1));
    }

    // ------------------------------------------------------------------ names

    @Test public void duplicateNamesAreTold() {
        List<HomeItem> items = TestSupport.items(TestSupport.DEFAULTS, "TV settings");
        int youtube = 0;
        boolean tizen = false;
        for (HomeItem item : items) {
            if (item.label.equals("YouTube")) youtube++;
            if (item.listLabel.equals("YouTube · TizenTube")) tizen = true;
        }
        assertEquals(2, youtube);
        assertTrue(tizen);
    }

    @Test public void initialsIgnoreTheLocale() {
        HomeItem item = TestSupport.items(new String[][]{{"x.y", "istanbul iptv"}}, null).get(0);
        assertEquals("II", item.initials());
    }
}
