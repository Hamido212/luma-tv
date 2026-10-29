package de.hamid.lumatv.stage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Map;
import de.hamid.lumatv.R;

/**
 * Monochrome service marks (Simple Icons, CC0; Disney+ from Custom Brand Icons, MIT; utility
 * outlines from CleanFrame, MIT), drawn as tinted vectors. Each mark is fitted by its measured
 * ink box with CleanFrame's optical weighting, so a thin smile and a solid N look equally heavy.
 */
final class Marks {
    private static final Map<String, String> BY_PACKAGE = new HashMap<>();
    private static final Map<String, Integer> DRAWABLE = new HashMap<>();
    /** Ink box in the 240 square: x, y, w, h, fill fraction. Measured by tools/icons/measure.py. */
    private static final Map<String, float[]> METRICS = new HashMap<>();
    /** Friendlier names than some package labels. */
    private static final Map<String, String> LABELS = new HashMap<>();

    static {
        pkg("com.netflix.ninja", "netflix");
        pkg("com.amazon.amazonvideo.livingroom", "primevideo");
        pkg("com.disney.disneyplus", "disneyplus");
        pkg("com.stremio.one", "stremio");
        pkg("io.gh.reisxd.tizentube.cobalt", "youtube");
        pkg("com.google.android.youtube.tv", "youtube");
        pkg("com.google.android.youtube.tvkids", "youtube");
        pkg("org.jellyfin.androidtv", "jellyfin");
        pkg("org.videolan.vlc", "vlcmediaplayer");
        pkg("de.rtli.tvnow", "rtl");
        pkg("com.android.vending", "googleplay");
        pkg("com.adguard.android", "adguard");
        pkg("com.plexapp.android", "plex");
        pkg("org.xbmc.kodi", "kodi");
        pkg("tv.twitch.android.app", "twitch");
        pkg("com.crunchyroll.crunchyroid", "crunchyroll");
        pkg("com.dazn", "dazn");
        pkg("com.spotify.tv.android", "spotify");
        pkg("com.apple.atve.androidtv.appletv", "appletv");

        LABELS.put("io.gh.reisxd.tizentube.cobalt", "YouTube");
        LABELS.put("com.amazon.amazonvideo.livingroom", "Prime Video");
        LABELS.put("de.prosiebensat1digital.seventv", "Joyn");
        LABELS.put("de.rtli.tvnow", "RTL+");
        LABELS.put("org.jellyfin.androidtv", "Jellyfin");
        LABELS.put("org.videolan.vlc", "VLC");
        LABELS.put("com.nuvio.tv", "Nuvio");
        LABELS.put("com.stremio.one", "Stremio");
        LABELS.put("com.disney.disneyplus", "Disney+");

        mark("adguard", R.drawable.mark_adguard, 0, 0, 240, 240, .6769f);
        mark("appletv", R.drawable.mark_appletv, 0, 61, 240, 118, .4206f);
        mark("apps", R.drawable.mark_apps, 22, 22, 196, 196, .4222f);
        mark("crunchyroll", R.drawable.mark_crunchyroll, 0, 0, 240, 240, .5626f);
        mark("dazn", R.drawable.mark_dazn, 0, 0, 240, 240, .4066f);
        mark("disneyplus", R.drawable.mark_disneyplus, 20, 28, 200, 184, .2638f);
        mark("googleplay", R.drawable.mark_googleplay, 12, 0, 216, 240, .4527f);
        mark("home", R.drawable.mark_home, 22, 21, 196, 197, .3151f);
        mark("jellyfin", R.drawable.mark_jellyfin, 0, 0, 240, 240, .449f);
        mark("kodi", R.drawable.mark_kodi, 0, 0, 240, 240, .3422f);
        mark("netflix", R.drawable.mark_netflix, 54, 0, 132, 240, .8215f);
        mark("playstation", R.drawable.mark_playstation, 0, 26, 240, 188, .3833f);
        mark("plex", R.drawable.mark_plex, 0, 64, 240, 112, .4374f);
        mark("primevideo", R.drawable.mark_primevideo, 4, 91, 230, 55, .2939f);
        mark("rtl", R.drawable.mark_rtl, 0, 99, 240, 42, .8526f);
        mark("settings", R.drawable.mark_settings, 12, 22, 216, 196, .3405f);
        mark("spotify", R.drawable.mark_spotify, 0, 0, 240, 240, .6419f);
        mark("stremio", R.drawable.mark_stremio, 0, 0, 240, 240, .4901f);
        mark("twitch", R.drawable.mark_twitch, 17, 0, 206, 240, .4424f);
        mark("vlcmediaplayer", R.drawable.mark_vlcmediaplayer, 14, 0, 212, 240, .385f);
        mark("youtube", R.drawable.mark_youtube, 0, 35, 240, 170, .881f);
    }

    private static void pkg(String pkg, String mark) { BY_PACKAGE.put(pkg, mark); }

    private static void mark(String id, int drawable, float x, float y, float w, float h, float fill) {
        DRAWABLE.put(id, drawable);
        METRICS.put(id, new float[]{x, y, w, h, fill});
    }

    static String forPackage(String pkg) { return BY_PACKAGE.get(pkg); }

    static String label(String pkg, String fallback) {
        String label = LABELS.get(pkg);
        return label != null ? label : fallback;
    }

    /**
     * The TV's own settings app. Its label is "Settings" like Luma's own Settings control, so
     * All apps names it "TV settings" instead.
     */
    static boolean isTvSettings(String pkg) {
        return "com.android.tv.settings".equals(pkg) || "com.android.settings".equals(pkg);
    }

    private final Context context;
    private final Map<String, Drawable> drawables = new HashMap<>();
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    Marks(Context context) {
        this.context = context;
        ring.setStyle(Paint.Style.STROKE);
    }

    private Drawable drawable(String id) {
        Drawable cached = drawables.get(id);
        if (cached != null) return cached;
        Integer res = DRAWABLE.get(id);
        if (res == null) return null;
        Drawable d = context.getDrawable(res);
        if (d == null) return null;
        d = d.mutate();
        drawables.put(id, d);
        return d;
    }

    /** Draws a utility outline (apps, settings) into a square of the given side. */
    void glyph(Canvas canvas, String id, float x, float y, float side, int color) {
        Drawable d = drawable(id);
        if (d == null) return;
        d.setTint(color | 0xFF000000);
        d.setAlpha(Color2.alpha(color));
        d.setBounds(Math.round(x), Math.round(y), Math.round(x + side), Math.round(y + side));
        d.draw(canvas);
    }

    /**
     * Draws the item's mark centred in the box (w x h) with CleanFrame's optical size rule.
     * Falls back to the installed icon (when allowed) or to an initials monogram.
     */
    void item(Canvas canvas, HomeItem item, float x, float y, float w, float h, float size, int color,
              boolean allowNativeIcon, Paint monogramText) {
        if (item.mark != null && drawMark(canvas, item.mark, x, y, w, h, size, color)) return;
        if (allowNativeIcon && item.icon != null) {
            float side = Math.min(w, h);
            float left = x + (w - side) / 2, top = y + (h - side) / 2;
            item.icon.setBounds(Math.round(left), Math.round(top), Math.round(left + side), Math.round(top + side));
            item.icon.setAlpha(Color2.alpha(color));
            item.icon.draw(canvas);
            return;
        }
        monogram(canvas, item.initials(), x, y, w, h, size, color, monogramText);
    }

    boolean drawMark(Canvas canvas, String id, float x, float y, float w, float h, float size, int color) {
        float[] m = METRICS.get(id);
        Drawable d = drawable(id);
        if (m == null || d == null) return false;
        float weight = clamp((float) Math.pow(.45f / m[4], .3f), .84f, 1.25f);
        float k = Math.min(Math.min(w / m[2], h / m[3]), size * weight / (float) Math.sqrt(m[2] * m[3]));
        float s = 240 * k;
        float left = x + w / 2 - (m[0] + m[2] / 2) * k;
        float top = y + h / 2 - (m[1] + m[3] / 2) * k;
        int save = canvas.save();
        canvas.translate(left, top);
        canvas.scale(s / 240f, s / 240f);
        d.setTint(color | 0xFF000000);
        d.setAlpha(Color2.alpha(color));
        d.setBounds(0, 0, 240, 240);
        d.draw(canvas);
        canvas.restoreToCount(save);
        return true;
    }

    /** Initials in a rounded square of 1.65 em with a 2 px ring at 38 % opacity. */
    void monogram(Canvas canvas, String letters, float x, float y, float w, float h, float size, int color, Paint text) {
        float em = Math.round(size * .65f);
        float side = 1.65f * em;
        float cx = x + w / 2, cy = y + h / 2;
        rect.set(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2);
        ring.setStrokeWidth(2);
        ring.setColor(Color2.withAlpha(color, Color2.alpha(color) / 255f * .38f));
        rect.inset(1, 1);
        canvas.drawRoundRect(rect, .4f * em - 1, .4f * em - 1, ring);
        text.setTextSize(em);
        text.setColor(color);
        text.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = text.getFontMetrics();
        canvas.drawText(letters, cx, cy - (fm.ascent + fm.descent) / 2, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
}
