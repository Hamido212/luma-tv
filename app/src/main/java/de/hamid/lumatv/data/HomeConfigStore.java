package de.hamid.lumatv.data;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User-owned dashboard layout: which package sits in which category, hidden tiles,
 * clock format, the recents row and the HDMI input the PS4 is connected to.
 * Stored locally in SharedPreferences; nothing is sent anywhere.
 */
public final class HomeConfigStore {
    public static final String SERIES = "series";
    public static final String TV = "tv";
    public static final String VIDEO = "video";
    public static final String MEDIA = "media";
    /** Display order of the user categories. "More apps" is computed, not stored. */
    public static final List<String> CATEGORY_IDS =
            Collections.unmodifiableList(Arrays.asList(SERIES, TV, VIDEO, MEDIA));

    public static final int CLOCK_SYSTEM = 0;
    public static final int CLOCK_24 = 1;
    public static final int CLOCK_12 = 2;

    private static final String KEY_ROWS = "category_rows_v1";
    private static final String KEY_HIDDEN = "hidden_packages_v1";
    private static final String KEY_CLOCK = "clock_mode_v1";
    private static final String KEY_RECENTS = "recents_enabled_v1";
    private static final String KEY_PS4 = "ps4_input_id_v1";
    private static final String KEY_WALLPAPER = "wallpaper_v1";

    private final SharedPreferences prefs;

    public HomeConfigStore(SharedPreferences prefs) { this.prefs = prefs; }

    /**
     * Defaults from the TCL inventory of 19.10.2026: real package names, not guessed from labels.
     * Packages that are not installed are simply skipped when the rows are drawn.
     */
    private static LinkedHashMap<String, List<String>> defaults() {
        LinkedHashMap<String, List<String>> rows = new LinkedHashMap<>();
        rows.put(SERIES, new ArrayList<>(Arrays.asList("com.stremio.one", "com.nuvio.tv",
                "com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus")));
        rows.put(TV, new ArrayList<>(Arrays.asList("de.prosiebensat1digital.seventv", "de.rtli.tvnow")));
        rows.put(VIDEO, new ArrayList<>(Collections.singletonList("io.gh.reisxd.tizentube.cobalt")));
        rows.put(MEDIA, new ArrayList<>(Arrays.asList("org.jellyfin.androidtv", "org.videolan.vlc")));
        return rows;
    }

    /** Category id to ordered package names. Each package appears at most once. */
    public synchronized LinkedHashMap<String, List<String>> categories() {
        String raw = prefs.getString(KEY_ROWS, null);
        if (raw == null) return defaults();
        LinkedHashMap<String, List<String>> rows = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        try {
            JSONObject stored = new JSONObject(raw);
            for (String id : CATEGORY_IDS) {
                List<String> packages = new ArrayList<>();
                JSONArray array = stored.optJSONArray(id);
                for (int i = 0; array != null && i < array.length(); i++) {
                    String pkg = array.optString(i, "");
                    if (!pkg.isEmpty() && seen.add(pkg)) packages.add(pkg);
                }
                rows.put(id, packages);
            }
            return rows;
        } catch (JSONException broken) {
            return defaults();
        }
    }

    /** Swaps the app with its nearest visible neighbour in the given direction (-1 or +1). */
    public synchronized boolean moveWithin(String rowId, String pkg, int direction, Predicate<String> visible) {
        LinkedHashMap<String, List<String>> rows = categories();
        List<String> row = rows.get(rowId);
        if (row == null) return false;
        int from = row.indexOf(pkg);
        if (from < 0) return false;
        int to = from + direction;
        while (to >= 0 && to < row.size() && !visible.test(row.get(to))) to += direction;
        if (to < 0 || to >= row.size()) return false;
        Collections.swap(row, from, to);
        saveRows(rows);
        return true;
    }

    /** Moves the app to the end of a category, or to "more apps" when targetRowId is null. */
    public synchronized void moveTo(String pkg, String targetRowId) {
        LinkedHashMap<String, List<String>> rows = categories();
        for (List<String> row : rows.values()) row.remove(pkg);
        List<String> target = targetRowId == null ? null : rows.get(targetRowId);
        if (target != null) target.add(pkg);
        saveRows(rows);
    }

    /** Category id the package is assigned to, or null for "more apps". */
    public synchronized String categoryOf(String pkg) {
        for (Map.Entry<String, List<String>> row : categories().entrySet()) {
            if (row.getValue().contains(pkg)) return row.getKey();
        }
        return null;
    }

    public synchronized Set<String> hidden() {
        return new HashSet<>(prefs.getStringSet(KEY_HIDDEN, Collections.emptySet()));
    }

    public synchronized void setHidden(String pkg, boolean hide) {
        Set<String> hidden = hidden();
        if (hide ? hidden.add(pkg) : hidden.remove(pkg)) prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply();
    }

    /** Restores categories and hidden tiles; clock, recents and PS4 input stay as they are. */
    public synchronized void resetLayout() {
        prefs.edit().remove(KEY_ROWS).remove(KEY_HIDDEN).apply();
    }

    public int clockMode() {
        int mode = prefs.getInt(KEY_CLOCK, CLOCK_SYSTEM);
        return mode == CLOCK_24 || mode == CLOCK_12 ? mode : CLOCK_SYSTEM;
    }

    public void setClockMode(int mode) { prefs.edit().putInt(KEY_CLOCK, mode).apply(); }

    public boolean recentsEnabled() { return prefs.getBoolean(KEY_RECENTS, true); }

    public void setRecentsEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_RECENTS, enabled).apply(); }

    /** Wallpaper scene id; unknown values fall back to the default scene in the UI layer. */
    public String wallpaper() { return prefs.getString(KEY_WALLPAPER, null); }

    public void setWallpaper(String id) { prefs.edit().putString(KEY_WALLPAPER, id).apply(); }

    public String ps4InputId() { return prefs.getString(KEY_PS4, null); }

    public void setPs4InputId(String inputId) {
        if (inputId == null) prefs.edit().remove(KEY_PS4).apply();
        else prefs.edit().putString(KEY_PS4, inputId).apply();
    }

    private void saveRows(Map<String, List<String>> rows) {
        JSONObject stored = new JSONObject();
        try {
            for (String id : CATEGORY_IDS) {
                List<String> row = rows.get(id);
                stored.put(id, new JSONArray(row == null ? Collections.<String>emptyList() : row));
            }
        } catch (JSONException impossible) {
            return;
        }
        prefs.edit().putString(KEY_ROWS, stored.toString()).apply();
    }
}
