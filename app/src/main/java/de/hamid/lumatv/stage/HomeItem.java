package de.hamid.lumatv.stage;

import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import de.hamid.lumatv.model.AppEntry;

/** An app or an HDMI input as shown on Home, in All apps and in the editor. */
final class HomeItem {
    final String key;
    final String label;
    /** Installed app, or null for an HDMI input. */
    final AppEntry app;
    final String inputId;
    /** Bundled monochrome mark id, or null for a monogram / native icon. */
    final String mark;
    /** The installed app icon, used in All apps when there is no bundled mark. */
    final Drawable icon;
    /** Name in All apps and the editor; longer than {@link #label} only when two apps share a name. */
    String listLabel;

    HomeItem(String key, String label, AppEntry app, String inputId, String mark, Drawable icon) {
        this.key = key;
        this.label = label;
        this.app = app;
        this.inputId = inputId;
        this.mark = mark;
        this.icon = icon;
        this.listLabel = label;
    }

    boolean isInput() { return inputId != null; }

    /**
     * Two apps can end up with the same name (TizenTube is shown as "YouTube" next to the real
     * YouTube app). Lists that show both (All apps, the editor) then add the app's own name:
     * "YouTube · TizenTube". Home keeps the short name the viewer is used to.
     */
    static void disambiguate(List<HomeItem> items) {
        Map<String, Integer> count = new HashMap<>();
        for (HomeItem item : items) count.merge(item.label.toLowerCase(Locale.ROOT), 1, Integer::sum);
        for (HomeItem item : items) {
            if (count.get(item.label.toLowerCase(Locale.ROOT)) < 2 || item.app == null) continue;
            String own = item.app.label;
            if (own == null || own.trim().isEmpty() || own.trim().equalsIgnoreCase(item.label)) continue;
            item.listLabel = item.label + " · " + own.trim();
        }
    }

    /** One or two initials, like CleanFrame's monogram. */
    String initials() {
        String[] words = label.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        if (words.length > 0 && !words[0].isEmpty()) out.appendCodePoint(words[0].codePointAt(0));
        if (words.length > 1 && !words[1].isEmpty()) out.appendCodePoint(words[1].codePointAt(0));
        return out.toString().toUpperCase(Locale.ROOT);
    }
}
