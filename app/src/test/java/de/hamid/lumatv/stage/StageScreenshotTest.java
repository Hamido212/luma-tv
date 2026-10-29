package de.hamid.lumatv.stage;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import de.hamid.lumatv.R;

/**
 * Renders Home, Settings, All apps and the editor at 1920 x 1080 into PNGs (build/screenshots),
 * so a design change can be reviewed without a TV. No pixel comparison yet; the pictures are the
 * review material, uploaded by CI.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w960dp-h540dp-land-television-xhdpi")
public class StageScreenshotTest {
    private static final int UP = KeyEvent.KEYCODE_DPAD_UP, DOWN = KeyEvent.KEYCODE_DPAD_DOWN,
            RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT, OK = KeyEvent.KEYCODE_DPAD_CENTER, BACK = KeyEvent.KEYCODE_BACK,
            MENU = KeyEvent.KEYCODE_MENU;
    private Activity activity;
    private SharedPreferences prefs;

    @Before public void setUp() {
        Scene.hardware = false;
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        prefs = activity.getSharedPreferences("luma_home", Context.MODE_PRIVATE);
    }

    @Test public void english() throws IOException { shootAll(); }

    @Test @Config(qualifiers = "de-w960dp-h540dp-land-television-xhdpi") public void german() throws IOException { shootAll(); }

    @Test @Config(qualifiers = "ru-w960dp-h540dp-land-television-xhdpi") public void russian() throws IOException { shootAll(); }

    private void shootAll() throws IOException {
        reset();
        StageView v = stage(TestSupport.DEFAULTS, false);
        snap(v, "home-dock");
        TestSupport.press(v, UP);
        snap(v, "home-dock-controls");

        reset();
        prefs.edit().putBoolean("stage_greeting_v1", true).putString("stage_user_name_v1", "Hamid").commit();
        snap(stage(TestSupport.DEFAULTS, true), "home-dock-greeting");

        reset();
        prefs.edit().putString("stage_home_v1", TestSupport.keysJson(TestSupport.LONG)).commit();
        snap(stage(TestSupport.LONG, false), "home-dock-9-long");
        prefs.edit().putString("stage_look_layout", "list").commit();
        snap(stage(TestSupport.LONG, false), "home-list-9-long");

        reset();
        prefs.edit().putString("stage_look_layout", "list").commit();
        snap(stage(TestSupport.DEFAULTS, true), "home-list-6");
        prefs.edit().putString("stage_look_apps", "icons").putString("stage_look_layout", "dock").commit();
        snap(stage(TestSupport.DEFAULTS, false), "home-dock-icons");
        prefs.edit().putString("stage_look_bg", "light").putString("stage_look_apps", "both").putString("stage_look_focus", "type").commit();
        snap(stage(TestSupport.DEFAULTS, false), "home-light-type");

        reset();
        prefs.edit().putString("stage_home_v1", "[]").commit();
        snap(stage(new String[0][], false), "home-empty");

        reset();
        v = stage(TestSupport.DEFAULTS, false);
        TestSupport.press(v, UP, OK);
        snap(v, "all-apps");
        TestSupport.press(v, BACK, RIGHT, OK);
        snap(v, "settings");
        TestSupport.press(v, DOWN, DOWN, DOWN, OK);
        snap(v, "settings-focus");
        TestSupport.press(v, BACK, DOWN, DOWN, OK);
        snap(v, "settings-tv");
        for (int i = 0; i < 8; i++) TestSupport.press(v, DOWN);
        snap(v, "settings-tv-scrolled");
        TestSupport.press(v, OK);
        snap(v, "settings-backup");
        TestSupport.press(v, BACK, BACK, BACK, DOWN);
        v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, MENU));
        TestSupport.press(v, RIGHT);
        snap(v, "editor-remove");
    }

    private void reset() {
        prefs.edit().clear().putBoolean("stage_look_motion", false).commit();
    }

    private StageView stage(String[][] apps, boolean greeting) {
        Look look = new Look(prefs);
        look.migrate();
        StageView v = new StageView(activity, new TestSupport.FakeHost(), look);
        activity.setContentView(v);
        v.begin(false);
        idle();
        v.setCatalog(TestSupport.items(apps, activity.getString(R.string.tv_settings_app)));
        v.setClock("20:15");
        if (greeting) {
            v.setGreeting(Greeting.text(true, "Hamid", activity.getString(Greeting.forHour(20)),
                    activity.getString(R.string.greeting_with_name)));
        }
        return v;
    }

    private void snap(StageView v, String name) throws IOException {
        idle();
        v.measure(View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, 1920, 1080);
        Bitmap out = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(out));
        File dir = new File(System.getProperty("luma.screenshots", "build/screenshots"));
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        String lang = activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
        try (FileOutputStream os = new FileOutputStream(new File(dir, lang + "-" + name + ".png"))) {
            out.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
    }

    private static void idle() { shadowOf(Looper.getMainLooper()).idle(); }
}
