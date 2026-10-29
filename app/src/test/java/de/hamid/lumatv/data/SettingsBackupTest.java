package de.hamid.lumatv.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsBackupTest {
    private SharedPreferences prefs;
    private File file;

    @Before public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        prefs = context.getSharedPreferences("luma_home", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        file = new File(context.getFilesDir(), SettingsBackup.FILE_NAME);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Test public void roundTrip() {
        prefs.edit()
                .putString("stage_look_layout", "list")
                .putString("stage_home_v1", "[\"com.netflix.ninja\"]")
                .putBoolean("stage_greeting_v1", true)
                .putString("stage_user_name_v1", "Hamid")
                .putInt("clock_mode_v1", 1)
                .putString("ps4_input_id_v1", "hdmi2")
                .putInt("safe_count_v1", 2)          // runtime state, not part of a backup
                .commit();
        assertTrue(SettingsBackup.write(prefs, file, "0.4.0"));
        prefs.edit().clear().putString("stage_look_layout", "dock").commit();
        assertEquals(6, SettingsBackup.read(prefs, file));
        assertEquals("list", prefs.getString("stage_look_layout", null));
        assertEquals("[\"com.netflix.ninja\"]", prefs.getString("stage_home_v1", null));
        assertTrue(prefs.getBoolean("stage_greeting_v1", false));
        assertEquals("Hamid", prefs.getString("stage_user_name_v1", null));
        assertEquals(1, prefs.getInt("clock_mode_v1", 0));
        assertEquals("hdmi2", prefs.getString("ps4_input_id_v1", null));
        assertFalse(prefs.contains("safe_count_v1"));
    }

    @Test public void brokenFileChangesNothing() throws Exception {
        prefs.edit().putString("stage_look_layout", "dock").commit();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("{\"app\":\"luma-tv\",\"schema\":1,\"values\":{\"stage_look_layout\":{\"type\":\"int\",\"value\":\"x\"}}}"
                    .getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(-1, SettingsBackup.read(prefs, file));
        assertEquals("dock", prefs.getString("stage_look_layout", null));
    }

    @Test public void foreignOrNewerFilesAreRefused() {
        assertNull(SettingsBackup.fromJson("{\"app\":\"other\",\"schema\":1,\"values\":{}}"));
        assertNull(SettingsBackup.fromJson("{\"app\":\"luma-tv\",\"schema\":99,\"values\":{}}"));
        assertNull(SettingsBackup.fromJson("not json"));
        assertEquals(-1, SettingsBackup.read(prefs, new File(file.getParentFile(), "missing.json")));
    }

    @Test public void unknownKeysAreIgnored() {
        String json = "{\"app\":\"luma-tv\",\"schema\":1,\"values\":{"
                + "\"stage_look_font\":{\"type\":\"string\",\"value\":\"geist\"},"
                + "\"evil_key\":{\"type\":\"string\",\"value\":\"x\"}}}";
        assertEquals(1, SettingsBackup.fromJson(json).size());
        assertTrue(SettingsBackup.allowed("stage_look_font"));
        assertFalse(SettingsBackup.allowed("safe_boot_v1"));
    }
}
