package de.hamid.lumatv.stage;

import android.graphics.drawable.Drawable;
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

    HomeItem(String key, String label, AppEntry app, String inputId, String mark, Drawable icon) {
        this.key = key;
        this.label = label;
        this.app = app;
        this.inputId = inputId;
        this.mark = mark;
        this.icon = icon;
    }

    boolean isInput() { return inputId != null; }

    /** One or two initials, like CleanFrame's monogram. */
    String initials() {
        String[] words = label.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        if (words.length > 0 && !words[0].isEmpty()) out.appendCodePoint(words[0].codePointAt(0));
        if (words.length > 1 && !words[1].isEmpty()) out.appendCodePoint(words[1].codePointAt(0));
        return out.toString().toUpperCase();
    }
}
