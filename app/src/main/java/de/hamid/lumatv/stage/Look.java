package de.hamid.lumatv.stage;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;

/**
 * Committed appearance and the apps on Home, stored locally in the same preferences file as the
 * classic dashboard (so the PS4 input assignment is shared). Every axis is independent, as in
 * CleanFrame: background, layout, app display, control display, font, focus, material, motion.
 */
final class Look {
    static final String BG_PHOTO = "photo", BG_LIGHT = "light", BG_DARK = "dark";
    static final String LAYOUT_DOCK = "dock", LAYOUT_LIST = "list";
    static final String SHOW_BOTH = "both", SHOW_ICONS = "icons", SHOW_TEXT = "text";
    static final String FOCUS_GLASS = "glass", FOCUS_TYPE = "type";
    static final String MAT_SOFT = "soft", MAT_FROST = "frost", MAT_CLEAR = "clear";
    static final String FONT_MANROPE = "manrope", FONT_GEIST = "geist", FONT_NEWSREADER = "newsreader";
    static final String ROT_FIXED = "fixed", ROT_OPEN = "open", ROT_5 = "5", ROT_15 = "15", ROT_60 = "60";

    static final List<String> BGS = Arrays.asList(BG_PHOTO, BG_LIGHT, BG_DARK);
    static final List<String> LAYOUTS = Arrays.asList(LAYOUT_LIST, LAYOUT_DOCK);
    static final List<String> SHOWS = Arrays.asList(SHOW_TEXT, SHOW_ICONS, SHOW_BOTH);
    static final List<String> FOCUSES = Arrays.asList(FOCUS_TYPE, FOCUS_GLASS);
    static final List<String> MATERIALS = Arrays.asList(MAT_SOFT, MAT_FROST, MAT_CLEAR);
    static final List<String> FONTS = Arrays.asList(FONT_MANROPE, FONT_GEIST, FONT_NEWSREADER);
    static final List<String> ROTATIONS = Arrays.asList(ROT_FIXED, ROT_OPEN, ROT_5, ROT_15, ROT_60);

    /** Home holds one to nine apps, like CleanFrame. */
    static final int HOME_MAX = 9;
    static final String INPUT_PREFIX = "input:";

    /** Old dashboard key for the PS4 HDMI input; read here so the assignment carries over. */
    private static final String KEY_PS4 = "ps4_input_id_v1";
    private static final String KEY_CLOCK = "clock_mode_v1";
    private static final String KEY_HOME = "stage_home_v1";
    private static final String KEY_NAME = "stage_user_name_v1";
    private static final String KEY_GREETING = "stage_greeting_v1";
    private static final String P = "stage_look_";

    private static final List<String> DEFAULT_HOME = Arrays.asList(
            "com.stremio.one", "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus", "io.gh.reisxd.tizentube.cobalt", "org.jellyfin.androidtv");

    String bg, layout, apps, controls, focus, material, font, wallpaper, rotation;
    boolean motion;
    private final SharedPreferences prefs;

    Look(SharedPreferences prefs) {
        this.prefs = prefs;
        bg = pick("bg", BGS, BG_PHOTO);
        layout = pick("layout", LAYOUTS, LAYOUT_DOCK);
        apps = pick("apps", SHOWS, SHOW_BOTH);
        controls = pick("controls", SHOWS, SHOW_BOTH);
        focus = pick("focus", FOCUSES, FOCUS_GLASS);
        material = pick("material", MATERIALS, MAT_SOFT);
        font = pick("font", FONTS, FONT_MANROPE);
        rotation = pick("rotation", ROTATIONS, ROT_FIXED);
        wallpaper = Walls.normalize(prefs.getString(P + "wallpaper", Walls.DEFAULT_ID));
        motion = prefs.getBoolean(P + "motion", true);
    }

    private String pick(String key, List<String> allowed, String fallback) {
        String value = prefs.getString(P + key, fallback);
        return allowed.contains(value) ? value : fallback;
    }

    /** Writes one axis. Returns false when storage refused, so the caller keeps the old value. */
    boolean set(String key, String value) {
        boolean ok = prefs.edit().putString(P + key, value).commit();
        if (!ok) return false;
        switch (key) {
            case "bg": bg = value; break;
            case "layout": layout = value; break;
            case "apps": apps = value; break;
            case "controls": controls = value; break;
            case "focus": focus = value; break;
            case "material": material = value; break;
            case "font": font = value; break;
            case "rotation": rotation = value; break;
            case "wallpaper": wallpaper = value; break;
            default: break;
        }
        return true;
    }

    boolean setMotion(boolean on) {
        if (!prefs.edit().putBoolean(P + "motion", on).commit()) return false;
        motion = on;
        return true;
    }

    /** Material applies to a photographic Dock, or Glass focus on a photographic List. */
    boolean materialApplies() {
        return BG_PHOTO.equals(bg) && (LAYOUT_DOCK.equals(layout) || FOCUS_GLASS.equals(focus));
    }

    boolean photo() { return BG_PHOTO.equals(bg); }
    boolean dock() { return LAYOUT_DOCK.equals(layout); }
    boolean glass() { return FOCUS_GLASS.equals(focus); }

    // ------------------------------------------------------------------ Home apps

    List<String> homeKeys() {
        String raw = prefs.getString(KEY_HOME, null);
        List<String> keys = new ArrayList<>();
        if (raw == null) {
            keys.addAll(DEFAULT_HOME);
            String ps4 = ps4InputId();
            if (ps4 != null) keys.add(INPUT_PREFIX + ps4);
            return keys;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length() && keys.size() < HOME_MAX; i++) {
                String key = array.optString(i, "");
                if (!key.isEmpty() && !keys.contains(key)) keys.add(key);
            }
        } catch (org.json.JSONException broken) {
            keys.addAll(DEFAULT_HOME);
        }
        return keys;
    }

    boolean saveHomeKeys(List<String> keys) {
        return prefs.edit().putString(KEY_HOME, new JSONArray(keys).toString()).commit();
    }

    String ps4InputId() { return prefs.getString(KEY_PS4, null); }

    boolean setPs4InputId(String inputId) {
        SharedPreferences.Editor edit = prefs.edit();
        if (inputId == null) edit.remove(KEY_PS4); else edit.putString(KEY_PS4, inputId);
        return edit.commit();
    }

    /** 0 = system, 1 = 24 h, 2 = 12 h; shared with the classic dashboard. */
    int clockMode() {
        int mode = prefs.getInt(KEY_CLOCK, 0);
        return mode == 1 || mode == 2 ? mode : 0;
    }

    boolean setClockMode(int mode) { return prefs.edit().putInt(KEY_CLOCK, mode).commit(); }

    // ------------------------------------------------------------------ greeting
    // Greeting with an optional name, contributed by danilll0001 (Horizon Home fork, MIT).

    /** Name shown in the greeting. Empty means the greeting is shown without a name. */
    String userName() { return prefs.getString(KEY_NAME, ""); }

    boolean setUserName(String name) {
        return prefs.edit().putString(KEY_NAME, name == null ? "" : name).commit();
    }

    /** The greeting under the clock is off by default, so Home stays as calm as before. */
    boolean greetingOn() { return prefs.getBoolean(KEY_GREETING, false); }

    boolean setGreetingOn(boolean on) { return prefs.edit().putBoolean(KEY_GREETING, on).commit(); }

    static List<String> unmodifiable(List<String> list) { return Collections.unmodifiableList(list); }
}
