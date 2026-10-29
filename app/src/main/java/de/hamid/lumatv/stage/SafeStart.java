package de.hamid.lumatv.stage;

import android.content.SharedPreferences;

/**
 * Crash-loop guard. On many TVs Luma is the only Home; if it crashes on every start (for example
 * while decoding a photo on a TV that is short of memory), the screen would stay unusable until
 * someone fixes it with adb. After three starts within a minute that never drew a frame, Luma
 * starts in a safe mode without photo and glass, and stays there until the TV restarts or the
 * background is changed in Settings.
 */
final class SafeStart {
    static final String KEY_COUNT = "safe_count_v1";
    static final String KEY_FIRST = "safe_first_v1";
    static final String KEY_BOOT = "safe_boot_v1";
    static final int LIMIT = 3;
    static final long WINDOW_MS = 60_000;

    private SafeStart() {}

    /**
     * Counts this start. Returns true when Luma should start safely.
     *
     * @param boot the system boot count (Settings.Global.BOOT_COUNT), or -1 when unknown
     * @param now  SystemClock.elapsedRealtime()
     */
    static boolean enter(SharedPreferences prefs, int boot, long now) {
        int safeBoot = prefs.getInt(KEY_BOOT, Integer.MIN_VALUE);
        if (safeBoot != Integer.MIN_VALUE && safeBoot == boot && boot >= 0) return true;
        long first = prefs.getLong(KEY_FIRST, -1);
        int count = prefs.getInt(KEY_COUNT, 0);
        if (first < 0 || first > now || now - first > WINDOW_MS) {
            first = now;
            count = 0;
        }
        count++;
        boolean safe = count >= LIMIT;
        SharedPreferences.Editor edit = prefs.edit();
        if (safe) {
            edit.putInt(KEY_COUNT, 0).remove(KEY_FIRST);
            if (boot >= 0) edit.putInt(KEY_BOOT, boot); else edit.remove(KEY_BOOT);
        } else {
            edit.putInt(KEY_COUNT, count).putLong(KEY_FIRST, first).remove(KEY_BOOT);
        }
        // Synchronous: the next crash may follow immediately.
        edit.commit();
        return safe;
    }

    /** A frame reached the screen: this start was fine. */
    static void ok(SharedPreferences prefs) {
        if (prefs.getInt(KEY_COUNT, 0) == 0 && !prefs.contains(KEY_FIRST)) return;
        prefs.edit().putInt(KEY_COUNT, 0).remove(KEY_FIRST).apply();
    }

    /** The viewer chose a background again: leave safe mode. */
    static void leave(SharedPreferences prefs) {
        prefs.edit().remove(KEY_BOOT).putInt(KEY_COUNT, 0).remove(KEY_FIRST).commit();
    }
}
