package de.hamid.lumatv.data;

import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Saves Luma's settings (look, apps on Home, PS4 input, clock, greeting, classic view) as one
 * JSON file in the app's own folder on the TV, so they survive a new TV, a factory reset or a
 * reinstall: copy the file off with {@code adb pull} first, push it back, then restore. No
 * permission and no network are needed. Only known keys with the expected type are restored;
 * the screen validates every value again when it reads it.
 */
public final class SettingsBackup {
    public static final String FILE_NAME = "luma-settings.json";
    static final String APP = "luma-tv";
    static final int SCHEMA = 1;
    /** Larger files are not Luma backups. */
    private static final int MAX_BYTES = 256 * 1024;

    private SettingsBackup() {}

    /** Keys that belong in a backup. Crash-guard counters and similar runtime state stay out. */
    static boolean allowed(String key) {
        if (key == null) return false;
        if (key.startsWith("stage_")) return true;
        switch (key) {
            case "ps4_input_id_v1":
            case "clock_mode_v1":
            case "category_rows_v1":
            case "hidden_packages_v1":
            case "recents_enabled_v1":
            case "wallpaper_v1":
            case "recent_packages_v2":
            case "prefs_schema":
                return true;
            default:
                return false;
        }
    }

    /** The backup document for the given values. */
    static JSONObject toJson(Map<String, ?> all, String version, long created) throws JSONException {
        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> e : all.entrySet()) {
            if (!allowed(e.getKey())) continue;
            Object v = e.getValue();
            JSONObject item = new JSONObject();
            if (v instanceof String) item.put("type", "string");
            else if (v instanceof Boolean) item.put("type", "boolean");
            else if (v instanceof Integer) item.put("type", "int");
            else if (v instanceof Long) item.put("type", "long");
            else if (v instanceof Float) { item.put("type", "float"); v = ((Float) v).doubleValue(); }
            else continue; // string sets are not used by Luma
            item.put("value", v);
            values.put(e.getKey(), item);
        }
        JSONObject root = new JSONObject();
        root.put("app", APP);
        root.put("schema", SCHEMA);
        root.put("version", version == null ? "" : version);
        root.put("created", created);
        root.put("values", values);
        return root;
    }

    /**
     * Reads a backup document. Returns the values to restore, or null when the document is not a
     * Luma backup, is from a newer schema, or holds a value of the wrong type.
     */
    static Map<String, Object> fromJson(String text) {
        try {
            JSONObject root = new JSONObject(text);
            if (!APP.equals(root.optString("app")) || root.optInt("schema", -1) < 1 || root.optInt("schema") > SCHEMA) return null;
            JSONObject values = root.getJSONObject("values");
            Map<String, Object> out = new LinkedHashMap<>();
            for (Iterator<String> it = values.keys(); it.hasNext(); ) {
                String key = it.next();
                if (!allowed(key)) continue;
                JSONObject item = values.getJSONObject(key);
                String type = item.getString("type");
                switch (type) {
                    case "string": out.put(key, item.getString("value")); break;
                    case "boolean": out.put(key, item.getBoolean("value")); break;
                    case "int": out.put(key, item.getInt("value")); break;
                    case "long": out.put(key, item.getLong("value")); break;
                    case "float": out.put(key, (float) item.getDouble("value")); break;
                    default: return null;
                }
            }
            return out;
        } catch (JSONException broken) {
            return null;
        }
    }

    /** Writes the backup. Returns false when the file could not be written completely. */
    public static boolean write(SharedPreferences prefs, File file, String version) {
        File tmp = new File(file.getPath() + ".tmp");
        try {
            byte[] data = toJson(prefs.getAll(), version, System.currentTimeMillis()).toString(2).getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream(tmp)) {
                out.write(data);
            }
            return tmp.renameTo(file) || (file.delete() && tmp.renameTo(file));
        } catch (IOException | JSONException | RuntimeException failed) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            return false;
        }
    }

    /**
     * Restores the backup in one commit. Returns the number of restored values, or -1 when the
     * file is missing, unreadable or not a valid backup; then nothing changes.
     */
    public static int read(SharedPreferences prefs, File file) {
        if (file == null || !file.isFile() || file.length() > MAX_BYTES) return -1;
        Map<String, Object> values;
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) buffer.write(chunk, 0, n);
            values = fromJson(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException failed) {
            return -1;
        }
        if (values == null) return -1;
        SharedPreferences.Editor edit = prefs.edit();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String) edit.putString(e.getKey(), (String) v);
            else if (v instanceof Boolean) edit.putBoolean(e.getKey(), (Boolean) v);
            else if (v instanceof Integer) edit.putInt(e.getKey(), (Integer) v);
            else if (v instanceof Long) edit.putLong(e.getKey(), (Long) v);
            else if (v instanceof Float) edit.putFloat(e.getKey(), (Float) v);
        }
        return edit.commit() ? values.size() : -1;
    }
}
