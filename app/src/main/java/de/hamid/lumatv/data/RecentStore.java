package de.hamid.lumatv.data;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

/**
 * Package names of apps successfully started from this home screen, newest first.
 * No task snapshots, usage access or background service.
 */
public final class RecentStore {
    private static final String KEY = "recent_packages_v2";
    private static final int LIMIT = 8;
    private final SharedPreferences prefs;

    public RecentStore(SharedPreferences prefs) { this.prefs = prefs; }

    public synchronized List<String> read() {
        List<String> result = new ArrayList<>();
        for (String key : prefs.getString(KEY, "").split("\n")) {
            if (!key.isEmpty() && !result.contains(key) && result.size() < LIMIT) result.add(key);
        }
        return result;
    }

    public synchronized void record(String packageName) {
        if (packageName == null || packageName.isEmpty() || packageName.contains("\n")) return;
        List<String> items = read();
        items.remove(packageName);
        items.add(0, packageName);
        if (items.size() > LIMIT) items = new ArrayList<>(items.subList(0, LIMIT));
        write(items);
    }

    public synchronized void remove(String packageName) {
        List<String> items = read();
        if (items.remove(packageName)) write(items);
    }

    public synchronized void clear() { prefs.edit().remove(KEY).apply(); }

    private void write(List<String> items) {
        prefs.edit().putString(KEY, android.text.TextUtils.join("\n", items)).apply();
    }
}
