package de.hamid.lumatv.stage;

import de.hamid.lumatv.R;

/** The greeting under the clock. Late evening and night say "Good evening", never "Good night". */
final class Greeting {
    private Greeting() {}

    static int forHour(int hour) {
        if (hour >= 5 && hour < 11) return R.string.greeting_morning;
        if (hour >= 11 && hour < 17) return R.string.greeting_day;
        if (hour >= 17 && hour < 22) return R.string.greeting_evening;
        return R.string.greeting_night;
    }

    /**
     * The line to show: empty when switched off, the greeting alone without a name, otherwise
     * {@code withName} ("%1$s, %2$s") filled with greeting and name.
     */
    static String text(boolean on, String name, String hello, String withName) {
        if (!on) return "";
        String n = name == null ? "" : name.trim();
        return n.isEmpty() ? hello : String.format(withName, hello, n);
    }
}
