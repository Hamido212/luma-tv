package de.hamid.lumatv.stage;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.Map;

/**
 * Variable fonts from assets (SIL OFL 1.1): Manrope, Geist and Newsreader. The chosen Home font
 * applies to app names, the clock and Home controls; Settings, All apps and the editor keep
 * Manrope, as in CleanFrame.
 */
final class Type {
    static final int DISPLAY = 0, CLOCK = 1, PRIMARY = 2, UI = 3;

    private final AssetManager assets;
    private final Map<String, Typeface> cache = new HashMap<>();

    Type(AssetManager assets) { this.assets = assets; }

    /** Settings face: always Manrope. */
    Typeface manrope(int weight) { return face("Manrope", weight, 0); }

    /** Home face for a role in the given font. */
    Typeface home(String font, int role) {
        if (Look.FONT_GEIST.equals(font)) {
            return face("Geist", role == DISPLAY || role == CLOCK ? 300 : 400, 0);
        }
        if (Look.FONT_NEWSREADER.equals(font)) {
            int opsz = role == DISPLAY ? 72 : role == CLOCK ? 48 : role == UI ? 28 : 36;
            return face("Newsreader", 400, opsz);
        }
        return face("Manrope", role == DISPLAY ? 600 : 500, 0);
    }

    /** Letter spacing in em: Manrope keeps CleanFrame's tight values, the others -0.01/0. */
    static float spacing(String font, int role, float manropeEm) {
        if (Look.FONT_MANROPE.equals(font)) return manropeEm;
        return role == DISPLAY || role == CLOCK ? -.01f : 0f;
    }

    private Typeface face(String family, int weight, int opsz) {
        String key = family + weight + "/" + opsz;
        Typeface cached = cache.get(key);
        if (cached != null) return cached;
        Typeface face;
        try {
            String axes = "'wght' " + weight + (opsz > 0 ? ", 'opsz' " + opsz : "");
            face = new Typeface.Builder(assets, "fonts/" + family + ".ttf").setFontVariationSettings(axes).build();
        } catch (RuntimeException broken) {
            face = null;
        }
        if (face == null) face = Typeface.create(Typeface.SANS_SERIF, weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
        cache.put(key, face);
        return face;
    }
}
