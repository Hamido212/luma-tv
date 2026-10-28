package de.hamid.lumatv.stage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;
import java.util.List;

/**
 * Draws Home (Dock or List) on the 1920 x 1080 stage. The same painter draws the live screen and
 * the scaled preview in Settings. Geometry, opacities and gradients follow CleanFrame 0.6.1
 * (style.css / main.js), adapted to Android canvas drawing.
 */
final class HomePainter {
    static final int Z_APPS = 0, Z_UTIL = 1, Z_PREVIEW = 2, Z_NONE = 3;
    static final int UTIL_APPS = 0, UTIL_SETTINGS = 1;

    static final float PH_TOP = 220, PH_PITCH = 128;

    /** Everything one frame of Home needs. Animated values are already resolved. */
    static final class Frame {
        Look look;
        Scene scene;
        Bitmap backdrop;
        List<HomeItem> items;
        int anchor;
        int zone;
        int util;
        /** Dock: track offset in px. List: list offset in px. */
        float off;
        /** Animated anchor position (fractional while moving). */
        float plate;
        /** Animated util lens position (fractional while moving). */
        float lens;
        String clock;
        /** Greeting under the clock; empty when switched off. */
        String greeting;
        String[] utilLabels;
        String emptyText;
    }

    static final class Geom {
        float x, y, w, h, shelf, tw, th, pitch;
        int vis;
        boolean broad;
    }

    private final Gfx g;
    private final Type type;
    private final Marks marks;
    private final TextPaint mono = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layer = new Paint();
    private final Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float[] utilX = new float[2], utilW = new float[2], utilY = new float[2];

    HomePainter(Gfx g, Type type, Marks marks) {
        this.g = g;
        this.type = type;
        this.marks = marks;
        mono.setTypeface(type.manrope(600));
        mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    // ------------------------------------------------------------------ geometry

    static Geom dock(String apps, int n) {
        Geom geom = new Geom();
        if (Look.SHOW_TEXT.equals(apps)) { geom.tw = 288; geom.th = 104; geom.pitch = 296; }
        else if (Look.SHOW_ICONS.equals(apps)) { geom.tw = 176; geom.th = 144; geom.pitch = 184; }
        else { geom.tw = 288; geom.th = 200; geom.pitch = 296; }
        geom.vis = Math.max(1, Math.min(5, n));
        geom.shelf = geom.vis * geom.pitch - 8;
        geom.w = geom.shelf + 32;
        geom.broad = n > 5;
        if (n <= 0) { geom.w = 912; geom.shelf = 880; }
        else if (geom.broad) { geom.w = 1728; geom.shelf = 1696; }
        geom.x = Math.round((1920 - geom.w) / 2);
        geom.y = 908 - geom.th - 32;
        geom.h = geom.th + 32;
        return geom;
    }

    /** Dock scroll target: a broad dock keeps half a tile of the neighbours in view. */
    static float dockOffset(Geom geom, int n, int i, float current) {
        if (!geom.broad) return 0;
        float off = current, left = i * geom.pitch, glimpse = geom.tw / 2;
        if (left - off < (i > 0 ? glimpse : 0)) off = left - (i > 0 ? glimpse : 0);
        if (left + geom.tw - off > geom.shelf - (i < n - 1 ? glimpse : 0)) off = left + geom.tw - geom.shelf + (i < n - 1 ? glimpse : 0);
        return clamp(off, 0, Math.max(0, n * geom.pitch - 8 - geom.shelf));
    }

    /** List scroll target in rows: focus stays within the second to fourth slot. */
    static int listFirst(int n, int anchor, int current) {
        int first = clampInt(current, anchor - 3, anchor - 1);
        return clampInt(first, 0, Math.max(0, n - 5));
    }

    // ------------------------------------------------------------------ colours

    static int ground(Look look) {
        return Look.BG_LIGHT.equals(look.bg) ? 0xFFF2EFE8 : 0xFF0D0D0D;
    }

    private static int appInk(Frame f) {
        if (Look.BG_LIGHT.equals(f.look.bg)) return 0xFF1D1A16;
        if (f.look.photo() && f.scene != null) return f.scene.wall.appInk;
        return 0xFFF6F1E7;
    }

    // ------------------------------------------------------------------ backdrop

    /**
     * Photo plus the layout's fixed shades, composed once into a GPU bitmap so a frame costs
     * one textured quad. Returns null when composition is unavailable.
     */
    Bitmap composeBackdrop(Scene scene, Look look) {
        if (scene == null) return null;
        Picture picture = new Picture();
        Canvas c = picture.beginRecording(Scene.STAGE_W, Scene.STAGE_H);
        drawBackdrop(c, scene, look);
        picture.endRecording();
        try {
            return Bitmap.createBitmap(picture);
        } catch (RuntimeException unsupported) {
            return null;
        }
    }

    void drawBackdrop(Canvas c, Scene scene, Look look) {
        Walls.Wall wall = scene.wall;
        rect.set(0, 0, 1920, 1080);
        c.drawBitmap(scene.picture, null, rect, g.bitmap);
        boolean lightTone = wall.lightTone;
        int k = 0xFF0A0806; // rgb(10,8,6)
        if (look.dock()) {
            g.vertical(c, 0, 560, 1920, 1080, 560, 1080,
                    new int[]{Color2.rgba(4, 10, 12, 0), Color2.rgba(4, 10, 12, .42f), Color2.rgba(4, 10, 12, .52f)},
                    new float[]{0, .7f, 1});
        } else {
            g.horizontal(c, 0, 0, 1920, 1080, 0, 1920,
                    new int[]{Color2.withAlpha(k, lightTone ? .34f : .26f), Color2.withAlpha(k, lightTone ? .24f : .16f), Color2.withAlpha(k, lightTone ? .14f : .08f)},
                    new float[]{0, .5f, 1});
            g.vertical(c, 0, 600, 1920, 1080, 600, 1080,
                    new int[]{Color2.withAlpha(k, 0), Color2.withAlpha(k, .26f), Color2.withAlpha(k, .4f)},
                    new float[]{0, .55f, 1});
            float col = wall.listColumn == 3 ? .32f : wall.listColumn == 2 || lightTone ? .28f : .18f;
            int save = c.saveLayer(0, 160, 1040, 980, null);
            g.horizontal(c, 0, 160, 1040, 980, 0, 1040,
                    new int[]{Color2.withAlpha(k, col), Color2.withAlpha(k, col), Color2.withAlpha(k, 0)},
                    new float[]{0, 600 / 1040f, 1});
            mask.setShader(new LinearGradient(0, 160, 0, 980, new int[]{0, 0xFF000000, 0xFF000000, 0},
                    new float[]{0, 120 / 820f, 700 / 820f, 1}, Shader.TileMode.CLAMP));
            c.drawRect(0, 160, 1040, 980, mask);
            mask.setShader(null);
            c.restoreToCount(save);
        }
        // Top shade for the clock and controls.
        float a0 = lightTone ? .42f : .32f, a1 = lightTone ? .2f : .14f, y1 = lightTone ? 160 : 150;
        g.vertical(c, 0, 0, 1920, 300, 0, 300,
                new int[]{Color2.rgba(4, 10, 12, a0), Color2.rgba(4, 10, 12, a1), Color2.rgba(4, 10, 12, 0)},
                new float[]{0, y1 / 300f, 1});
        if (look.dock()) {
            if (wall.tl.pol == Walls.POL_DARK) {
                g.ellipse(c, 188, 104, 560, 200, new int[]{Color2.rgba(255, 252, 248, .12f), Color2.rgba(255, 252, 248, .06f), 0x00FFFCF8}, new float[]{0, .55f, 1});
            } else if (wall.clockMist) {
                g.ellipse(c, 188, 104, 560, 200, new int[]{Color2.withAlpha(k, .24f), Color2.withAlpha(k, .14f), 0}, new float[]{0, .55f, 1});
            }
        }
        regionDim(c, wall.tr, lightTone, 1500, 104);
        if (!look.dock()) regionDim(c, wall.br, lightTone, 1560, 878);
    }

    private void regionDim(Canvas c, Walls.Region region, boolean lightTone, float cx, float cy) {
        int k = 0xFF0A0806;
        if (region.pol == Walls.POL_DARK) {
            g.ellipse(c, cx, cy, 900, 260, new int[]{Color2.rgba(255, 252, 248, .12f), Color2.rgba(255, 252, 248, .06f), 0x00FFFCF8}, new float[]{0, .55f, 1});
            return;
        }
        boolean mist = region.pol == Walls.POL_MIST;
        float a = mist ? (lightTone ? .44f : .34f) : (lightTone ? .30f : .20f);
        float b = mist ? (lightTone ? .28f : .20f) : (lightTone ? .18f : .12f);
        g.ellipse(c, cx, cy, 900, 260, new int[]{Color2.withAlpha(k, a), Color2.withAlpha(k, b), 0}, new float[]{0, .55f, 1});
    }

    // ------------------------------------------------------------------ frame

    void draw(Canvas c, Frame f) {
        if (f.look.photo() && f.scene != null) {
            if (f.backdrop != null) {
                rect.set(0, 0, 1920, 1080);
                c.drawBitmap(f.backdrop, null, rect, g.bitmap);
            } else {
                drawBackdrop(c, f.scene, f.look);
            }
        } else {
            c.drawColor(ground(f.look));
        }
        if (f.look.dock()) drawDock(c, f); else drawList(c, f);
        drawClock(c, f);
        drawUtils(c, f);
    }

    // ------------------------------------------------------------------ material

    /** Glass fill for the dock, list plate and control lens. */
    private void material(Canvas c, Frame f, float l, float t, float w, float h, float radius, boolean dockMaterial) {
        Scene scene = f.scene;
        String m = f.look.material;
        boolean lightTone = scene.wall.lightTone;
        if (Look.MAT_CLEAR.equals(m)) {
            g.field(c, scene.clear, l, t, w, h, radius, 1.02f, 960, t + h / 2);
            if (dockMaterial) scrimInside(c, l, t, w, h, radius);
            g.round(c, l, t, w, h, radius, Color2.rgba(8, 14, 18, lightTone ? .38f : .3f));
            g.ring(c, l, t, w, h, radius, 1, Color2.rgba(255, 252, 246, .12f));
            highlight(c, l, t, w, h, radius);
            g.hairline(c, l, t, w, h, radius, Color2.rgba(255, 252, 246, .30f));
            return;
        }
        if (Look.MAT_FROST.equals(m)) {
            g.field(c, scene.frost, l, t, w, h, radius, 1, 0, 0);
            float veil = dockMaterial ? scene.wall.frostVeil : .06f;
            g.round(c, l, t, w, h, radius, Color2.rgba(12, 10, 8, veil));
        } else {
            g.field(c, scene.soft, l, t, w, h, radius, 1, 0, 0);
            if (dockMaterial) {
                scrimInside(c, l, t, w, h, radius);
                g.round(c, l, t, w, h, radius, Color2.rgba(8, 14, 18, lightTone ? .27f : .16f));
            } else {
                g.round(c, l, t, w, h, radius, Color2.rgba(12, 10, 8, lightTone ? .2f : .12f));
            }
        }
        g.hairline(c, l, t, w, h, radius, Color2.rgba(255, 255, 255, dockMaterial ? .24f : .24f));
    }

    private void scrimInside(Canvas c, float l, float t, float w, float h, float radius) {
        g.registeredVertical(c, l, t, w, h, radius, 560, 1080,
                new int[]{Color2.rgba(4, 10, 12, 0), Color2.rgba(4, 10, 12, .42f), Color2.rgba(4, 10, 12, .52f)},
                new float[]{0, .7f, 1});
    }

    /** radial-gradient(120% 90% at 22% 0%, rgba(255,252,246,.16), transparent 55%). */
    private void highlight(Canvas c, float l, float t, float w, float h, float radius) {
        android.graphics.RadialGradient rg = new android.graphics.RadialGradient(0, 0, 1.2f * w,
                new int[]{Color2.rgba(255, 252, 246, .16f), 0x00FFFCF6}, new float[]{0, .55f}, Shader.TileMode.CLAMP);
        android.graphics.Matrix mtx = new android.graphics.Matrix();
        mtx.setScale(1, .9f * h / (1.2f * w));
        mtx.postTranslate(l + .22f * w, t);
        rg.setLocalMatrix(mtx);
        g.fill.setShader(rg);
        rect.set(l, t, l + w, t + h);
        c.drawRoundRect(rect, radius, radius, g.fill);
        g.fill.setShader(null);
    }

    /** Focus plate: glass gradient on photos, a quiet tint on flat backgrounds. */
    private void plate(Canvas c, Frame f, float l, float t, float w, float h, float radius, float opacity, boolean selected) {
        Look look = f.look;
        if (!look.photo()) {
            boolean light = Look.BG_LIGHT.equals(look.bg);
            int color = light ? Color2.rgba(29, 26, 22, selected ? .05f : .07f) : Color2.rgba(246, 241, 231, selected ? .05f : .08f);
            g.round(c, l, t, w, h, radius, Color2.fade(color, opacity));
            if (!light) g.hairline(c, l, t, w, h, radius, Color2.rgba(255, 250, 240, .1f * opacity));
            return;
        }
        if (selected) {
            g.round(c, l, t, w, h, radius, Color2.rgba(255, 255, 255, .11f * opacity));
            return;
        }
        boolean quiet = f.scene != null && f.scene.wall.dockQuiet;
        int[] colors;
        float[] stops;
        if (Look.MAT_FROST.equals(look.material)) {
            colors = quiet ? new int[]{white(.24f), white(0), Color2.rgba(12, 10, 8, .08f)} : new int[]{white(.26f), white(.08f), white(.10f)};
            stops = quiet ? new float[]{0, .52f, 1} : new float[]{0, .58f, 1};
        } else if (quiet) {
            colors = new int[]{white(.30f), white(.10f), white(.08f)};
            stops = new float[]{0, .52f, 1};
        } else {
            colors = new int[]{white(.30f), white(.16f), white(.22f)};
            stops = new float[]{0, .58f, 1};
        }
        if (opacity < 1) for (int i = 0; i < colors.length; i++) colors[i] = Color2.fade(colors[i], opacity);
        g.gradient(c, l, t, w, h, radius, 170, colors, stops);
        g.hairline(c, l, t, w, h, radius, white(.34f * opacity));
    }

    private static int white(float a) { return Color2.rgba(255, 255, 255, a); }

    // ------------------------------------------------------------------ dock

    private void drawDock(Canvas c, Frame f) {
        List<HomeItem> items = f.items;
        int n = items.size();
        Geom geom = dock(f.look.apps, n);
        if (f.look.photo() && f.scene != null) material(c, f, geom.x, geom.y, geom.w, geom.h, 48, true);
        if (n == 0) {
            TextPaint p = g.font(type.manrope(500), 36, -.6f / 36, appInk(f));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(f.emptyText, 960, g.baseline(p, geom.y + geom.h / 2 - 22, 44), p);
            p.setTextAlign(Paint.Align.LEFT);
            return;
        }
        boolean glass = f.look.glass();
        boolean tilesFocus = f.zone == Z_APPS, selected = f.zone == Z_UTIL || f.zone == Z_PREVIEW;
        float shelfL = geom.x + 16, shelfR = shelfL + geom.shelf;
        float off = f.off;
        boolean fadeL = geom.broad && off > 0.5f, fadeR = geom.broad && off + geom.shelf < n * geom.pitch - 8 - 0.5f;
        int save = c.save();
        c.clipRect(shelfL, geom.y, shelfR, geom.y + geom.h);
        if (glass && f.zone != Z_NONE) {
            float px = shelfL + f.plate * geom.pitch - off;
            boolean sel = f.zone == Z_UTIL;
            float opacity = f.zone == Z_PREVIEW ? .5f : 1f;
            plate(c, f, px, geom.y + 16, geom.tw, geom.th, 32, opacity, sel);
        }
        int ink = appInk(f);
        for (int i = 0; i < n; i++) {
            float left = shelfL + i * geom.pitch - off;
            if (left + geom.tw < shelfL - 1 || left > shelfR + 1) continue;
            boolean focus = tilesFocus && i == f.anchor, sel = selected && i == f.anchor;
            // CleanFrame masks the shelf edges (to 20 % over 132 px). Fading each tile by its
            // centre avoids an offscreen layer on every frame.
            float edge = 1f;
            float center = left + geom.tw / 2;
            if (fadeL && center < shelfL + 132) edge = Math.min(edge, .2f + .8f * clamp((center - shelfL) / 132f, 0, 1));
            if (fadeR && center > shelfR - 132) edge = Math.min(edge, .2f + .8f * clamp((shelfR - center) / 132f, 0, 1));
            drawTile(c, f, items.get(i), left, geom.y + 16, geom, focus, sel, ink, edge);
        }
        c.restoreToCount(save);
        // Icons-only: the focused app's name appears under the dock.
        if (Look.SHOW_ICONS.equals(f.look.apps) && tilesFocus && f.anchor < n) {
            HomeItem item = items.get(f.anchor);
            TextPaint p = homeText(f, Type.PRIMARY, 36, -.6f / 36, ink);
            float w = Math.min(1640, p.measureText(item.label) + 8);
            float center = shelfL + f.anchor * geom.pitch - off + geom.tw / 2;
            float x = clamp(center - w / 2, 140, 1780 - w);
            shadowed(f, p, false);
            c.drawText(g.ellipsize(item.label, w), x + 4, g.baseline(p, 932, 44), p);
        }
    }

    private void drawTile(Canvas c, Frame f, HomeItem item, float left, float top, Geom geom,
                          boolean focus, boolean sel, int ink, float edge) {
        boolean glass = f.look.glass();
        String apps = f.look.apps;
        float opacity;
        float scale = 1f;
        if (glass) {
            opacity = focus ? 1f : sel ? .94f : .84f;
        } else {
            boolean photo = f.look.photo();
            opacity = focus ? 1f : sel ? (photo ? .92f : .78f) : (photo ? .84f : .62f);
            if (focus) scale = 1.06f;
        }
        opacity *= edge;
        int save = c.save();
        if (scale != 1f) c.scale(scale, scale, left + geom.tw / 2, top + geom.th / 2);
        int color = Color2.fade(ink, opacity);
        if (!Look.SHOW_TEXT.equals(apps)) {
            float bx, by, bw, bh, size;
            if (Look.SHOW_ICONS.equals(apps)) { bx = 20; by = 28; bw = 136; bh = 88; size = 72; }
            else { bx = 84; by = 34; bw = 120; bh = 72; size = 58; }
            int ms = c.save();
            if (glass && focus) c.scale(1.04f, 1.04f, left + bx + bw / 2, top + by + bh / 2 - 3);
            if (glass && focus) c.translate(0, -3);
            marks.item(c, item, left + bx, top + by, bw, bh, size, color, false, mono);
            c.restoreToCount(ms);
        }
        if (!Look.SHOW_ICONS.equals(apps)) {
            boolean text = Look.SHOW_TEXT.equals(apps);
            float size = text ? 40 : 36;
            TextPaint p = homeText(f, Type.PRIMARY, size, text ? -.8f / 40 : -.8f / 36, color);
            if (text ? p.measureText(item.label) > 264 : p.measureText(item.label) > 280) {
                size = text ? 34 : 32;
                p.setTextSize(size);
            }
            if (text) p.setTypeface(type.home(f.look.font, Type.DISPLAY));
            float pad = text ? 12 : 4;
            String label = g.ellipsize(item.label, geom.tw - 2 * pad);
            p.setTextAlign(Paint.Align.CENTER);
            shadowed(f, p, false);
            c.drawText(label, left + geom.tw / 2, g.baseline(p, top + (text ? 28 : 128), text ? 48 : 44), p);
            p.setTextAlign(Paint.Align.LEFT);
            p.clearShadowLayer();
        }
        c.restoreToCount(save);
    }

    // ------------------------------------------------------------------ list

    private float listNameSize(Frame f, HomeItem item) {
        TextPaint p = homeText(f, Type.DISPLAY, 88, -1.8f / 88, 0);
        return p.measureText(item.label) > 992 ? 76 : 88;
    }

    private float listPlateWidth(Frame f, HomeItem item) {
        if (Look.SHOW_ICONS.equals(f.look.apps)) return 144;
        boolean text = Look.SHOW_TEXT.equals(f.look.apps);
        TextPaint p = homeText(f, Type.DISPLAY, listNameSize(f, item), -1.8f / 88, 0);
        float width = Math.min(992, p.measureText(item.label));
        return clamp((float) Math.ceil(width + (text ? 84 : 184)), text ? 280 : 360, text ? 1076 : 1176);
    }

    private void drawList(Canvas c, Frame f) {
        List<HomeItem> items = f.items;
        int n = items.size();
        int ink = appInk(f);
        if (n == 0) {
            TextPaint p = homeText(f, Type.DISPLAY, 64, -1.8f / 64, ink);
            c.drawText(f.emptyText, 140, g.baseline(p, 220, 88), p);
            return;
        }
        boolean glass = f.look.glass();
        boolean rowFocus = f.zone == Z_APPS, remembered = f.zone == Z_UTIL || f.zone == Z_PREVIEW;
        // Glass plate under the focused row, holding the registered field.
        if (glass && f.zone != Z_NONE) {
            int a = clampInt((int) Math.floor(f.plate), 0, n - 1), b = clampInt(a + 1, 0, n - 1);
            float t = f.plate - a;
            float width = listPlateWidth(f, items.get(a)) * (1 - t) + listPlateWidth(f, items.get(b)) * t;
            float y = PH_TOP + 1 + f.plate * PH_PITCH - f.off;
            float opacity = f.zone == Z_PREVIEW || f.zone == Z_UTIL ? .5f : 1f;
            if (f.look.photo() && f.scene != null && f.look.materialApplies()) {
                int save = opacity < 1 ? c.saveLayerAlpha(112, y, 112 + width, y + 134, Math.round(opacity * 255)) : c.save();
                material(c, f, 112, y, width, 134, 38, false);
                c.restoreToCount(save);
            } else {
                plate(c, f, 112, y, width, 134, 38, opacity, false);
            }
        }
        int save = c.save();
        c.clipRect(112, 72, 1312, 1016);
        for (int i = 0; i < n; i++) {
            float y = PH_TOP + i * PH_PITCH - f.off;
            if (y + PH_PITCH < 72 || y > 1016) continue;
            boolean focus = rowFocus && i == f.anchor, sel = remembered && i == f.anchor;
            drawRow(c, f, items.get(i), y, focus, sel, ink, listFade(y + 64));
        }
        c.restoreToCount(save);
    }

    /**
     * CleanFrame fades the list over 164 px at both ends with a squared ramp (its mask stops are
     * t squared). Evaluated at the row centre so no offscreen layer is needed.
     */
    private static float listFade(float y) {
        float top = clamp((y - 72) / 164f, 0, 1), bottom = clamp((1016 - y) / 164f, 0, 1);
        float t = Math.min(top, bottom);
        return t * t;
    }

    private void drawRow(Canvas c, Frame f, HomeItem item, float y, boolean focus, boolean sel, int ink, float fade) {
        boolean glass = f.look.glass(), photo = f.look.photo();
        float opacity = photo ? .84f : glass ? .8f : .62f;
        float scale = 1;
        if (glass) {
            if (focus || sel) opacity = 1;
        } else {
            if (focus) { opacity = 1; scale = 1.05f; }
            else if (sel) opacity = photo ? .92f : .78f;
        }
        opacity *= fade;
        if (opacity <= 0.01f) return;
        int color = Color2.fade(ink, opacity);
        int save = c.save();
        if (scale != 1) c.scale(scale, scale, 112 + 28, y + 64);
        String apps = f.look.apps;
        if (!Look.SHOW_TEXT.equals(apps)) {
            boolean icons = Look.SHOW_ICONS.equals(apps);
            marks.item(c, item, 112 + 28, y + (icons ? 20 : 32), icons ? 88 : 84, icons ? 88 : 64, icons ? 72 : 54, color, false, mono);
        }
        if (!Look.SHOW_ICONS.equals(apps)) {
            float size = listNameSize(f, item);
            TextPaint p = homeText(f, Type.DISPLAY, size, -1.8f / 88, color);
            float x = 112 + (Look.SHOW_TEXT.equals(apps) ? 28 : 128);
            if (photo) {
                if (glass && focus) g.shadow(2, 1, Color2.rgba(14, 10, 6, .22f));
                else g.shadow(14, 3, Color2.rgba(14, 10, 6, .30f));
            }
            c.drawText(g.ellipsize(item.label, 992), x, g.baseline(p, y + (size == 76 ? 10.36f : 9.68f), 104), p);
            p.clearShadowLayer();
        }
        c.restoreToCount(save);
    }

    // ------------------------------------------------------------------ clock and controls

    private void drawClock(Canvas c, Frame f) {
        Walls.Region region = f.look.dock() ? (f.scene != null ? f.scene.wall.tl : null) : (f.scene != null ? f.scene.wall.tr : null);
        boolean photo = f.look.photo() && f.scene != null;
        int ink;
        if (!f.look.photo()) ink = Color2.fade(Look.BG_LIGHT.equals(f.look.bg) ? 0xFF1D1A16 : 0xFFF6F1E7, .72f);
        else ink = region != null ? region.ink : 0xFFF6F1E7;
        TextPaint p = homeText(f, Type.CLOCK, 48, -1.5f / 48, ink);
        if (photo) {
            if (region != null && region.pol == Walls.POL_DARK) g.shadow(2, 1, Color2.rgba(255, 248, 236, .24f));
            else g.shadow(3, 1, Color2.rgba(0, 0, 0, .22f));
        }
        if (f.look.dock()) {
            c.drawText(f.clock, 140, g.baseline(p, 72, 64), p);
        } else {
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(f.clock, 1780, g.baseline(p, 112, 64), p);
            p.setTextAlign(Paint.Align.LEFT);
        }
        p.clearShadowLayer();

        // Optional greeting: a smaller line under the clock, same ink and shadow as the clock.
        if (f.greeting != null && !f.greeting.isEmpty()) {
            TextPaint gp = homeText(f, Type.PRIMARY, 30, 0f, Color2.fade(ink, .86f));
            if (photo) {
                if (region != null && region.pol == Walls.POL_DARK) g.shadow(2, 1, Color2.rgba(255, 248, 236, .24f));
                else g.shadow(3, 1, Color2.rgba(0, 0, 0, .22f));
            }
            String line = g.ellipsize(f.greeting, 900);
            if (f.look.dock()) {
                c.drawText(line, 142, g.baseline(gp, 132, 40), gp);
            } else {
                gp.setTextAlign(Paint.Align.RIGHT);
                c.drawText(line, 1780, g.baseline(gp, 172, 40), gp);
                gp.setTextAlign(Paint.Align.LEFT);
            }
            gp.clearShadowLayer();
        }
    }

    private void drawUtils(Canvas c, Frame f) {
        boolean dock = f.look.dock();
        String mode = f.look.controls;
        boolean icons = Look.SHOW_ICONS.equals(mode), text = Look.SHOW_TEXT.equals(mode);
        Walls.Region region = f.scene != null ? (dock ? f.scene.wall.tr : f.scene.wall.br) : null;
        boolean photo = f.look.photo() && region != null;
        boolean light = Look.BG_LIGHT.equals(f.look.bg);
        int ink = photo ? region.ink : light ? 0xFF1D1A16 : 0xFFF6F1E7;
        float rest = photo ? region.rest : .62f, peer = photo ? region.peer : light ? .52f : .434f;
        boolean awake = f.zone == Z_UTIL;
        TextPaint p = homeText(f, Type.UI, 28, -.2f / 28, ink);
        // Measure both controls.
        for (int i = 0; i < 2; i++) {
            float icon = i == UTIL_SETTINGS ? 34 : 32;
            if (icons) utilW[i] = 64;
            else if (text) utilW[i] = 14 + p.measureText(f.utilLabels[i]) + 14;
            else utilW[i] = 14 + (dock ? icon : 36) + (dock ? 10 : 17.9f) + p.measureText(f.utilLabels[i]) + 14 + (i == UTIL_SETTINGS ? 1 : 0);
        }
        if (dock) {
            float right = text ? 1794 : 1798, gap = icons ? 16 : text ? 28 : 20;
            utilX[1] = right - utilW[1];
            utilX[0] = utilX[1] - gap - utilW[0];
            utilY[0] = utilY[1] = 72;
        } else {
            for (int i = 0; i < 2; i++) utilX[i] = 1798 - utilW[i];
            utilY[0] = 840;
            utilY[1] = 904;
        }
        // Glass lens behind the focused control.
        if (awake && f.look.glass()) {
            int a = clampInt((int) Math.floor(f.lens), 0, 1), b = clampInt(a + 1, 0, 1);
            float t = f.lens - a;
            float lx = utilX[a] * (1 - t) + utilX[b] * t, lw = utilW[a] * (1 - t) + utilW[b] * t;
            float ly = utilY[a] * (1 - t) + utilY[b] * t + 2;
            if (f.look.photo() && f.scene != null) material(c, f, lx, ly, lw, 60, 30, false);
            else plate(c, f, lx, ly, lw, 60, 30, 1, false);
        }
        for (int i = 0; i < 2; i++) {
            boolean focus = awake && f.util == i;
            float opacity = focus ? 1 : awake ? peer : rest;
            int color = Color2.fade(ink, opacity);
            float x = utilX[i], y = utilY[i];
            int save = c.save();
            if (focus && !f.look.glass()) c.scale(1.05f, 1.05f, dock ? x + utilW[i] / 2 : x + utilW[i], y + 32);
            p.setColor(color);
            if (photo) {
                if (region.pol == Walls.POL_DARK) g.shadow(2, 1, Color2.rgba(255, 248, 236, .24f));
                else g.shadow(2, 1, Color2.rgba(14, 10, 6, .40f));
            }
            String glyph = i == UTIL_SETTINGS ? "settings" : "apps";
            float iconSize = icons ? 36 : dock ? (i == UTIL_SETTINGS ? 34 : 32) : (i == UTIL_SETTINGS ? 34 : 36);
            if (icons) {
                marks.glyph(c, glyph, x + (64 - iconSize) / 2, y + 32 - iconSize / 2, iconSize, color);
            } else if (text) {
                c.drawText(f.utilLabels[i], x + 14, g.baseline(p, y + 14, 36), p);
            } else if (dock) {
                marks.glyph(c, glyph, x + 14, y + 32 - iconSize / 2, iconSize, color);
                c.drawText(f.utilLabels[i], x + 14 + iconSize + 10, g.baseline(p, y + 14, 36), p);
            } else {
                float labelW = p.measureText(f.utilLabels[i]);
                c.drawText(f.utilLabels[i], x + 14, g.baseline(p, y + 13, 36), p);
                marks.glyph(c, glyph, x + 14 + labelW + 17.9f, y + 32 - iconSize / 2, iconSize, color);
            }
            p.clearShadowLayer();
            c.restoreToCount(save);
        }
        // Icons only: the focused control names itself.
        if (icons && awake) {
            String label = f.utilLabels[f.util];
            p.setColor(ink);
            if (photo) g.shadow(2, 1, Color2.rgba(14, 10, 6, .40f));
            float w = p.measureText(label);
            if (dock) {
                float cx = utilX[f.util] + 32;
                c.drawText(label, Math.min(1798 - w, cx - w / 2), g.baseline(p, 140, 36), p);
            } else {
                c.drawText(label, utilX[f.util] - 16 - w, g.baseline(p, utilY[f.util] + 14, 36), p);
            }
            p.clearShadowLayer();
        }
    }

    private void shadowed(Frame f, TextPaint p, boolean strong) {
        if (!f.look.photo() || f.scene == null) return;
        if (strong) g.shadow(14, 3, Color2.rgba(14, 10, 6, .24f));
        else g.shadow(2, 1, Color2.rgba(14, 10, 6, .32f));
    }

    private TextPaint homeText(Frame f, int role, float size, float manropeEm, int color) {
        return g.font(type.home(f.look.font, role), size, Type.spacing(f.look.font, role, manropeEm), color);
    }

    static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }

    static int clampInt(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }
}
