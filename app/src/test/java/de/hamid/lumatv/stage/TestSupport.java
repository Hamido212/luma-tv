package de.hamid.lumatv.stage;

import android.content.ComponentName;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import de.hamid.lumatv.model.AppEntry;

/** Shared helpers for the Stage tests: a synchronous host, app lists and key presses. */
final class TestSupport {
    private TestSupport() {}

    static final String[][] DEFAULTS = {{"com.netflix.ninja", "Netflix"},
            {"com.amazon.amazonvideo.livingroom", "Prime Video"}, {"com.disney.disneyplus", "Disney+"},
            {"com.stremio.one", "Stremio"}, {"io.gh.reisxd.tizentube.cobalt", "TizenTube"},
            {"org.jellyfin.androidtv", "Jellyfin"}, {"org.videolan.vlc", "VLC"},
            {"com.google.android.youtube.tv", "YouTube"}, {"com.android.tv.settings", "Settings"}};

    static final String[][] LONG = {{"a.ard", "ARD Mediathek – Filme, Serien und Dokus"},
            {"b.kino", "Кинопоиск: фильмы и сериалы"}, {"c.joyn", "Joyn | Deine Streaming-App"},
            {"d.zdf", "ZDFmediathek"}, {"e.waipu", "WAIPU.TV – Fernsehen live"},
            {"f.plex", "Plex: Stream Movies & TV"}, {"g.magenta", "MagentaTV"},
            {"h.spotify", "Spotify – Music and Podcasts"}, {"i.kids", "YouTube Kids"}};

    static List<HomeItem> items(String[][] apps, String tvSettingsLabel) {
        List<HomeItem> items = new ArrayList<>();
        for (String[] a : apps) {
            AppEntry entry = new AppEntry(new ComponentName(a[0], a[0] + ".Main"), a[1], true);
            String label = Marks.isTvSettings(a[0]) && tvSettingsLabel != null ? tvSettingsLabel : Marks.label(a[0], a[1]);
            items.add(new HomeItem(a[0], label, entry, null, Marks.forPackage(a[0]), null));
        }
        items.sort(Comparator.comparing((HomeItem h) -> h.label, String.CASE_INSENSITIVE_ORDER));
        HomeItem.disambiguate(items);
        return items;
    }

    static String keysJson(String[][] apps) {
        StringBuilder b = new StringBuilder("[");
        for (String[] a : apps) b.append(b.length() > 1 ? "," : "").append('"').append(a[0]).append('"');
        return b.append(']').toString();
    }

    static void press(View v, int... keys) {
        for (int k : keys) {
            v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, k));
            v.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, k));
        }
    }

    /** Synchronous host: background work runs at once, nothing leaves the test. */
    static final class FakeHost implements StageView.Host {
        final List<String> toasts = new ArrayList<>();
        final List<String> launched = new ArrayList<>();
        String backup;
        int frames;

        @Override public void launch(HomeItem item) { launched.add(item.key); }
        @Override public void openTvSettings() {}
        @Override public void openInput(String inputId) {}
        @Override public void openClassic() {}
        @Override public Map<String, String> inputs() { return Collections.singletonMap("hdmi1", "HDMI 1"); }
        @Override public void work(Runnable task) { task.run(); }
        @Override public boolean homeVariant() { return true; }
        @Override public void leave() {}
        @Override public void catalogChanged() {}
        @Override public void toast(String text) { toasts.add(text); }
        @Override public void editName() {}
        @Override public void clockChanged() {}
        @Override public void firstFrameDrawn() { frames++; }
        @Override public String backupInfo() { return backup; }
        @Override public void backupSettings() { backup = "29.10.2026 14:00"; }
        @Override public void restoreSettings() {}
        @Override public void ps4Changed(String inputId) {}
    }
}
