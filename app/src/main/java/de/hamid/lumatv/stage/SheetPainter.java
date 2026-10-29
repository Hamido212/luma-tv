package de.hamid.lumatv.stage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import java.util.List;
import java.util.Map;

/**
 * Settings, All apps and the Home editor: one matte layer over a blurred copy of the photo
 * (or the flat ground), Manrope throughout, solid plates for commands and glass for app
 * focus — CleanFrame's sheet styles on the 1920 x 1080 stage.
 */
final class SheetPainter {
    static final float PREVIEW_X = 858, PREVIEW_Y = 288, PREVIEW_SCALE = .48f;

    /** Sheet colours for the committed background. */
    static final class Ink {
        int ground, ink, mute, faint, line, solid, solidInk, solidMute, warn;
        boolean photo, light, clearTint;

        static Ink of(Look look) {
            Ink k = new Ink();
            k.photo = look.photo();
            k.light = Look.BG_LIGHT.equals(look.bg);
            k.clearTint = k.photo && Look.MAT_CLEAR.equals(look.material);
            if (k.light) {
                k.ground = 0xFFF2EFE8;
                k.ink = 0xFF1D1A16;
                k.mute = Color2.rgba(29, 26, 22, .70f);
                k.faint = Color2.rgba(29, 26, 22, .52f);
                k.line = Color2.rgba(29, 26, 22, .14f);
                k.solid = 0xFF1D1A16;
                k.solidInk = 0xFFF2EFE8;
                k.solidMute = Color2.rgba(242, 239, 232, .72f);
                k.warn = 0xFF8A4B00;
            } else {
                k.ground = 0xFF0D0D0D;
                k.ink = 0xFFF6F1E7;
                k.mute = Color2.rgba(246, 241, 231, k.photo ? .76f : .72f);
                k.faint = Color2.rgba(246, 241, 231, k.photo ? .58f : .50f);
                k.line = Color2.rgba(246, 241, 231, k.photo ? .18f : .14f);
                k.solid = 0xFFF3F3EF;
                k.solidInk = 0xFF141618;
                k.solidMute = Color2.rgba(29, 26, 22, .68f);
                k.warn = k.photo ? 0xFFFFD9A8 : 0xFFE8B36A;
            }
            return k;
        }
    }

    private final Gfx g;
    private final Type type;
    private final Marks marks;
    private final HomePainter home;
    private final TextPaint mono = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clear = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    SheetPainter(Gfx g, Type type, Marks marks, HomePainter home) {
        this.g = g;
        this.type = type;
        this.marks = marks;
        this.home = home;
        mono.setTypeface(type.manrope(600));
        clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    // ------------------------------------------------------------------ shared pieces

    void background(Canvas c, Look look, Scene scene) {
        if (look.photo() && scene != null) {
            g.field(c, scene.frost, 0, 0, 1920, 1080, 0, 1, 0, 0);
            g.rect(c, 0, 0, 1920, 1080, Color2.rgba(8, 12, 14, .42f));
        } else {
            c.drawColor(HomePainter.ground(look));
        }
    }

    void title(Canvas c, Ink k, String text, float top, float lineHeight) {
        TextPaint p = g.font(type.manrope(500), 56, -1.6f / 56, k.ink);
        c.drawText(text, 140, g.baseline(p, top, lineHeight), p);
    }

    /** Title followed by a small count, like "All apps 23". */
    void titleWithCount(Canvas c, Ink k, String text, String count) {
        TextPaint p = g.font(type.manrope(500), 56, -1.6f / 56, k.ink);
        float w = p.measureText(text);
        c.drawText(text, 140, g.baseline(p, 72, 64), p);
        if (count == null) return;
        TextPaint q = g.font(type.manrope(500), 24, 0, k.faint);
        c.drawText(count, 140 + w + 20, g.baseline(q, 84, 40), q);
    }

    void sub(Canvas c, int color, String text) {
        TextPaint p = g.font(type.manrope(500), 30, -.2f / 30, color);
        c.drawText(g.ellipsize(text, 1500), 140, g.baseline(p, 148, 40), p);
    }

    void backButton(Canvas c, Ink k, String label, boolean focus) {
        TextPaint p = g.font(type.manrope(500), 28, -.2f / 28, focus ? k.solidInk : k.mute);
        float w = 16 + 24 + 8 + p.measureText(label) + 16;
        if (focus) g.round(c, 124, 60, w, 52, 16, k.solid);
        g.chevron(c, 124 + 16, 60 + 14, 24, 1, 2, focus ? k.solidInk : k.mute);
        c.drawText(label, 124 + 16 + 24 + 8, g.baseline(p, 60 + 8, 36), p);
    }

    /** Header command at the top right (s-head .btn): glass or type focus, never a solid plate. */
    void headButton(Canvas c, Look look, Ink k, Scene scene, String label, String glyph, boolean focus, boolean awake) {
        TextPaint p = g.font(type.manrope(500), 30, -.3f / 30, focus ? k.ink : awake ? k.faint : k.mute);
        float w = 24 + (glyph != null ? 30 + 12 : 0) + p.measureText(label) + 30;
        float x = 1920 - 112 - w, y = 72;
        int save = c.save();
        if (focus && look.glass()) sheetPlate(c, k, x, y + 2, w, 60, 30, false);
        if (focus && !look.glass()) c.scale(1.05f, 1.05f, x + w / 2, y + 32);
        float tx = x + 24;
        if (glyph != null) {
            if ("plus".equals(glyph)) g.plus(c, tx, y + 17, 30, 1.6f, p.getColor());
            else marks.glyph(c, glyph, tx, y + 17, 30, p.getColor());
            tx += 42;
        }
        c.drawText(label, tx, g.baseline(p, y + 14, 36), p);
        c.restoreToCount(save);
    }

    void hint(Canvas c, Ink k, String key, String text) {
        if (text == null) return;
        float x = 140;
        if (key != null) x += g.keyPill(c, key, 140, 969, k.mute, type.manrope(600)) + 16;
        TextPaint p = g.font(type.manrope(500), 26, 0, k.mute);
        c.drawText(g.ellipsize(text, HINT_RIGHT - x), x, g.baseline(p, 968, 36), p);
    }

    /** Right end of the bottom hint; the "more" cue of a scrolling grid sits beyond it. */
    static final float HINT_RIGHT = 1700;

    /** var(--plate) inside sheets. */
    void sheetPlate(Canvas c, Ink k, float l, float t, float w, float h, float r, boolean lifted) {
        if (k.light) {
            g.round(c, l, t, w, h, r, Color2.rgba(29, 26, 22, lifted ? .13f : .07f));
        } else if (!k.photo) {
            g.round(c, l, t, w, h, r, Color2.rgba(246, 241, 231, lifted ? .15f : .08f));
            g.hairline(c, l, t, w, h, r, Color2.rgba(255, 250, 240, .1f));
        } else if (k.clearTint) {
            g.round(c, l, t, w, h, r, Color2.rgba(14, 12, 10, lifted ? .62f : .42f));
        } else {
            int[] colors = lifted
                    ? new int[]{Color2.rgba(255, 255, 255, .44f), Color2.rgba(255, 255, 255, .27f), Color2.rgba(255, 255, 255, .34f)}
                    : new int[]{Color2.rgba(255, 255, 255, .30f), Color2.rgba(255, 255, 255, .16f), Color2.rgba(255, 255, 255, .22f)};
            g.gradient(c, l, t, w, h, r, 170, colors, new float[]{0, .58f, 1});
            g.hairline(c, l, t, w, h, r, Color2.rgba(255, 255, 255, .34f));
        }
    }

    /** The committed (or candidate) Home, scaled to .48 with rounded corners. */
    void preview(Canvas c, Ink k, HomePainter.Frame frame) {
        float w = 1920 * PREVIEW_SCALE, h = 1080 * PREVIEW_SCALE;
        int save = c.saveLayer(PREVIEW_X, PREVIEW_Y, PREVIEW_X + w, PREVIEW_Y + h, null);
        c.translate(PREVIEW_X, PREVIEW_Y);
        c.scale(PREVIEW_SCALE, PREVIEW_SCALE);
        home.draw(c, frame);
        path.reset();
        rect.set(0, 0, 1920, 1080);
        float r = 32 / PREVIEW_SCALE;
        path.addRoundRect(rect, r, r, Path.Direction.CW);
        path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        c.drawPath(path, clear);
        c.restoreToCount(save);
        if (!k.photo) g.ring(c, PREVIEW_X - 4, PREVIEW_Y - 4, w + 8, h + 8, 36, 4, k.line);
    }

    // ------------------------------------------------------------------ settings overview

    void overview(Canvas c, Ink k, String title, String[] names, String[] values, int focus, String hint) {
        title(c, k, title, 72, 64);
        for (int i = 0; i < names.length; i++) {
            float y = 212 + i * 120;
            boolean f = i == focus;
            if (f) g.round(c, 112, y, 1056, 112, 20, k.solid);
            TextPaint p = g.font(type.manrope(500), 40, -.6f / 40, f ? k.solidInk : k.ink);
            c.drawText(names[i], 112 + 28, g.baseline(p, y + 15, 48), p);
            TextPaint q = g.font(type.manrope(500), 26, 0, f ? k.solidMute : k.mute);
            c.drawText(g.ellipsize(values[i], 900), 112 + 28, g.baseline(q, y + 67, 34), q);
            g.chevron(c, 112 + 1056 - 32 - 30, y + 41, 30, 0, 1.8f, f ? k.solidMute : k.mute);
        }
        hint(c, k, "OK", hint);
    }

    // ------------------------------------------------------------------ appearance pages

    /** Lowest y a settings page may use; the hint line starts at 968. */
    static final float PAGE_BOTTOM = 944;
    private final float[] pageTops = new float[64];
    private int pageTopCount;
    private boolean pageCut;

    void page(Canvas c, Ink k, Page page, int focusRow, int focusCol, Map<String, Bitmap> thumbs) {
        backButton(c, k, page.back, focusRow == -1);
        title(c, k, page.title, 132, 68);
        // Long pages (many HDMI inputs, long translations) scroll by whole rows so the focused
        // row stays in view; rows that do not fit completely are not drawn.
        float[] span = {-1, -1};
        float end = sections(c, k, page, focusRow, focusCol, thumbs, false, span, -1e9f, 1e9f);
        float scroll = 0;
        if (end > PAGE_BOTTOM && span[1] > PAGE_BOTTOM) {
            float needed = span[1] - PAGE_BOTTOM;
            scroll = needed;
            for (int i = 0; i < pageTopCount; i++) {
                float b = pageTops[i] - page.top;
                if (b >= needed) { scroll = b; break; }
            }
        }
        boolean scrolling = end > PAGE_BOTTOM;
        int save = c.save();
        c.translate(0, -scroll);
        pageCut = false;
        end = sections(c, k, page, focusRow, focusCol, thumbs, true, span,
                scrolling ? page.top + scroll : -1e9f, scrolling ? PAGE_BOTTOM + scroll : 1e9f);
        c.restoreToCount(save);
        float mid = 140 + page.width / 2 - 16;
        if (scroll > 0.5f) g.chevron(c, mid, page.top - 36, 32, 2, 1.8f, k.mute);
        if (pageCut) g.chevron(c, mid, PAGE_BOTTOM - 8, 32, 3, 1.8f, k.mute);
        if (page.footnote != null) {
            TextPaint p = g.font(type.manrope(400), 24, 0, k.faint);
            if (page.footnoteBelow) drawWrapped(c, p, page.footnote, 140, Math.min(end - scroll + 40, PAGE_BOTTOM - 72), page.width, 34);
            else if (page.preview) drawWrapped(c, p, page.footnote, 140, Math.min(Math.max(end - scroll + 40, 900), PAGE_BOTTOM - 40), 640, 32);
            // Pages without a preview keep the right column for quiet notes such as credits.
            else drawWrapped(c, p, page.footnote, 1300, page.top + 48, 480, 34);
        }
        hint(c, k, "OK", page.hint);
    }

    /**
     * Lays out (and with draw, paints) the sections. Returns the bottom; span gets the focused
     * row; pageTops collects where rows and labels start. Only items fully inside [winTop, winBottom]
     * are painted.
     */
    private float sections(Canvas c, Ink k, Page page, int focusRow, int focusCol, Map<String, Bitmap> thumbs,
                           boolean draw, float[] span, float winTop, float winBottom) {
        float y = page.top;
        int rowIndex = 0;
        if (!draw) pageTopCount = 0;
        for (Page.Section s : page.sections) {
            if (s != page.sections.get(0)) y += page.sectionGap;
            if (s.label != null) {
                mark(draw, y);
                if (draw && visible(y, y + 36, winTop, winBottom)) {
                    TextPaint p = g.font(type.manrope(500), 28, -.2f / 28, k.mute);
                    c.drawText(s.label, 140, g.baseline(p, y, 36), p);
                }
                y += 36 + 12;
            }
            if (s.horizontal) {
                int n = s.opts.size();
                float w = (page.width - 12 * (n - 1)) / n;
                if (s.label == null) mark(draw, y);
                if (focusRow == rowIndex) { span[0] = y; span[1] = y + 92; }
                if (draw && visible(y, y + 92, winTop, winBottom)) {
                    for (int i = 0; i < n; i++) {
                        boolean f = focusRow == rowIndex && focusCol == i;
                        option(c, k, s.opts.get(i), 140 + i * (w + 12), y, w, 92, f, true, thumbs);
                    }
                }
                y += 92;
                rowIndex++;
            } else {
                for (int i = 0; i < s.opts.size(); i++) {
                    Page.Opt o = s.opts.get(i);
                    float h = o.description != null ? page.descHeight : page.rowHeight;
                    if (i > 0 || s.label == null) mark(draw, y);
                    if (focusRow == rowIndex) { span[0] = y; span[1] = y + h; }
                    if (draw && visible(y, y + h, winTop, winBottom)) {
                        option(c, k, o, 140, y, page.width, h, focusRow == rowIndex, false, thumbs);
                    }
                    y += h + 4;
                    rowIndex++;
                }
                y -= 4;
            }
            if (s.reason != null) {
                y += 16;
                if (draw && visible(y, y + 72, winTop, winBottom)) {
                    drawWrapped(c, g.font(type.manrope(400), 26, 0, k.mute), s.reason, 140, y, page.width, 36);
                }
                y += 72;
            }
        }
        return y;
    }

    private void mark(boolean draw, float y) {
        if (!draw && pageTopCount < pageTops.length) pageTops[pageTopCount++] = y;
    }

    private boolean visible(float top, float bottom, float winTop, float winBottom) {
        if (top >= winTop - .5f && bottom <= winBottom + .5f) return true;
        if (top >= winTop - .5f) pageCut = true;
        return false;
    }

    private void option(Canvas c, Ink k, Page.Opt o, float x, float y, float w, float h, boolean focus,
                        boolean horizontal, Map<String, Bitmap> thumbs) {
        if (focus) g.round(c, x, y, w, h, 18, k.solid);
        int color = focus ? (o.enabled ? k.solidInk : k.solidMute) : (o.enabled ? k.ink : k.faint);
        float size = horizontal ? 36 : 38;
        TextPaint p = g.font(o.face != null ? o.face : type.manrope(500), size, -.5f / 38, color);
        float tx = x + (horizontal ? 20 : 24);
        float nameTop = o.description != null ? y + (h - 88) / 2 - 2 : y + (h - 48) / 2;
        if (o.swatch != Page.SW_NONE) {
            float sy = y + (h - 50) / 2;
            int save = c.saveLayerAlpha(tx, sy, tx + 80, sy + 50, o.enabled ? 255 : 128);
            if (o.swatch == Page.SW_THUMB) {
                Bitmap thumb = thumbs.get(o.thumb);
                if (thumb != null) {
                    android.graphics.BitmapShader shader = new android.graphics.BitmapShader(thumb,
                            android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    float s = Math.max(80f / thumb.getWidth(), 50f / thumb.getHeight());
                    m.setScale(s, s);
                    m.postTranslate(tx + (80 - thumb.getWidth() * s) / 2, sy + (50 - thumb.getHeight() * s) / 2);
                    shader.setLocalMatrix(m);
                    g.bitmap.setShader(shader);
                    rect.set(tx, sy, tx + 80, sy + 50);
                    c.drawRoundRect(rect, 8, 8, g.bitmap);
                    g.bitmap.setShader(null);
                } else {
                    g.round(c, tx, sy, 80, 50, 8, k.line);
                }
            } else {
                boolean light = o.swatch == Page.SW_LIGHT;
                g.round(c, tx, sy, 80, 50, 8, light ? 0xFFF2EFE8 : 0xFF0D0D0D);
                g.ring(c, tx, sy, 80, 50, 8, 1, light ? Color2.rgba(29, 26, 22, .12f) : Color2.rgba(246, 241, 231, .15f));
            }
            c.restoreToCount(save);
            tx += 80 + 8 + 16;
        } else if (o.alignWithSwatch) {
            tx += 104;
        }
        float maxName = x + w - tx - (o.forward ? 28 + 24 + 16 : 24) - (o.checked ? 46 : 0);
        float full = p.measureText(o.label);
        if (full > maxName) p.setTextSize(Math.max(28f, size * maxName / full));
        String name = g.ellipsize(o.label, maxName);
        c.drawText(name, tx, g.baseline(p, nameTop, 48), p);
        float nameW = p.measureText(name);
        if (o.checked) {
            float tick = horizontal ? 26 : 30;
            g.check(c, tx + nameW + 16, nameTop + (48 - tick) / 2, tick, 2.2f, color);
        }
        if (o.forward) g.chevron(c, x + w - 24 - 28, y + (h - 28) / 2, 28, 0, 2, color);
        if (o.description != null) {
            TextPaint q = g.font(type.manrope(400), 26, 0, focus ? k.solidMute : k.mute);
            c.drawText(g.ellipsize(o.description, x + w - tx - 24), tx, g.baseline(q, nameTop + 52, 36), q);
        }
    }

    private void drawWrapped(Canvas c, TextPaint p, String text, float x, float y, float width, float line) {
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        float top = y;
        for (String word : words) {
            String next = current.length() == 0 ? word : current + " " + word;
            if (p.measureText(next) > width && current.length() > 0) {
                c.drawText(current.toString(), x, g.baseline(p, top, line), p);
                top += line;
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(next);
            }
        }
        if (current.length() > 0) c.drawText(current.toString(), x, g.baseline(p, top, line), p);
    }

    // ------------------------------------------------------------------ wallpaper gallery

    void wallpapers(Canvas c, Ink k, String back, String title, String[] names, String[] ids, int applied,
                    int focus, float first, boolean backFocus, Map<String, Bitmap> thumbs,
                    String status, String count, String state, String hint) {
        backButton(c, k, back, backFocus);
        title(c, k, title, 132, 68);
        float listTop = 260;
        int save = c.save();
        c.clipRect(140, listTop - 4, 140 + 640, listTop + 552);
        for (int i = 0; i < names.length; i++) {
            float y = listTop + i * 92 - first * 92;
            if (y + 84 < listTop - 4 || y > listTop + 552) continue;
            boolean f = !backFocus && i == focus;
            if (f) g.round(c, 140, y, 640, 84, 18, k.solid);
            int color = f ? k.solidInk : k.ink;
            Bitmap thumb = thumbs.get(ids[i]);
            float tx = 140 + 24;
            if (thumb != null) {
                android.graphics.BitmapShader shader = new android.graphics.BitmapShader(thumb,
                        android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
                android.graphics.Matrix m = new android.graphics.Matrix();
                float s = Math.max(96f / thumb.getWidth(), 54f / thumb.getHeight());
                m.setScale(s, s);
                m.postTranslate(tx, y + 15);
                shader.setLocalMatrix(m);
                g.bitmap.setShader(shader);
                rect.set(tx, y + 15, tx + 96, y + 69);
                c.drawRoundRect(rect, 8, 8, g.bitmap);
                g.bitmap.setShader(null);
            } else {
                g.round(c, tx, y + 15, 96, 54, 8, k.line);
            }
            TextPaint p = g.font(type.manrope(500), 36, -.5f / 38, color);
            String name = g.ellipsize(names[i], 640 - 144 - 70);
            c.drawText(name, 140 + 144, g.baseline(p, y + 18, 48), p);
            if (i == applied) g.check(c, 140 + 144 + p.measureText(name) + 16, y + 27, 30, 2.2f, color);
        }
        c.restoreToCount(save);
        if (first > 0.01f) g.chevron(c, 140 + 300, listTop - 32, 28, 2, 2, Color2.fade(k.mute, .7f));
        if (first + 6 < names.length - 0.01f) g.chevron(c, 140 + 300, listTop + 560, 28, 3, 2, Color2.fade(k.mute, .7f));
        TextPaint p = g.font(type.manrope(500), 32, 0, k.ink);
        float w = p.measureText(status);
        c.drawText(status, PREVIEW_X, g.baseline(p, 842, 42), p);
        TextPaint q = g.font(type.manrope(500), 26, 0, k.mute);
        c.drawText(count, PREVIEW_X + w + 24, g.baseline(q, 846, 36), q);
        if (state != null) c.drawText(state, PREVIEW_X, g.baseline(q, 888, 36), q);
        hint(c, k, "OK", hint);
    }

    // ------------------------------------------------------------------ all apps

    static final int GRID_COLS = 5;
    static final float GRID_COL = 344, GRID_ROW = 240, GRID_TOP = 232, GRID_LEFT = 112, GRID_VIEW = 720;

    void allApps(Canvas c, Look look, Ink k, List<HomeItem> items, java.util.Set<String> onHome, int focus,
                 boolean headFocus, float firstRow, String onHomeLabel, String emptyLabel) {
        boolean glass = look.glass();
        if (items.isEmpty()) {
            TextPaint p = g.font(type.manrope(500), 36, -.6f / 36, k.mute);
            c.drawText(emptyLabel, 140, g.baseline(p, GRID_TOP + 40, 48), p);
            return;
        }
        int save = c.save();
        c.clipRect(GRID_LEFT - 24, GRID_TOP - 8, GRID_LEFT + 1696 + 24, GRID_TOP + GRID_VIEW);
        for (int i = 0; i < items.size(); i++) {
            int row = i / GRID_COLS, col = i % GRID_COLS;
            float x = GRID_LEFT + col * GRID_COL, y = GRID_TOP + (row - firstRow) * GRID_ROW;
            if (y + 216 < GRID_TOP - 8 || y > GRID_TOP + GRID_VIEW) continue;
            boolean f = !headFocus && i == focus;
            HomeItem item = items.get(i);
            int ts = c.save();
            float opacity = 1;
            if (f && glass) sheetPlate(c, k, x, y, 320, 216, 32, false);
            if (!glass) {
                opacity = f ? 1 : .70f;
                if (f) c.scale(1.05f, 1.05f, x + 160, y + 108);
            }
            int ink = Color2.fade(k.ink, opacity);
            float markOpacity = glass ? (f ? 1 : .88f) : 1;
            int ms = c.save();
            if (glass && f) {
                c.translate(0, -3);
                c.scale(1.04f, 1.04f, x + 160, y + 66);
            }
            marks.item(c, item, x + 100, y + 30, 120, 72, 58, Color2.fade(ink, markOpacity), true, mono);
            c.restoreToCount(ms);
            TextPaint p = g.font(type.manrope(500), 34, -.6f / 34, ink);
            // Long names first step down to 30 px, only then are they shortened.
            if (p.measureText(item.label) > 296) p.setTextSize(30);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(g.ellipsize(item.label, 296), x + 160, g.baseline(p, y + 114, 44), p);
            p.setTextAlign(Paint.Align.LEFT);
            if (onHome.contains(item.key)) {
                TextPaint q = g.font(type.manrope(500), 28, 0, f ? k.ink : k.mute);
                float tw = 26 + 8 + q.measureText(onHomeLabel);
                float sx = x + 160 - tw / 2;
                g.check(c, sx, y + 169, 26, 2.4f, q.getColor());
                c.drawText(onHomeLabel, sx + 34, g.baseline(q, y + 164, 36), q);
            }
            c.restoreToCount(ts);
        }
        c.restoreToCount(save);
        int rows = (items.size() + GRID_COLS - 1) / GRID_COLS;
        if (firstRow > 0.01f) g.chevron(c, 940, 192, 40, 2, 1.8f, k.mute);
        if (firstRow + 3 < rows - 0.01f) g.chevron(c, 1756, 964, 40, 3, 1.8f, k.mute);
    }

    // ------------------------------------------------------------------ Home editor

    static final float ED_PITCH = 80;

    void editor(Canvas c, Look look, Ink k, List<HomeItem> left, List<HomeItem> right, int zone, int leftIndex,
                int rightIndex, boolean lifted, float rightFirst, String onLabel, String offLabel,
                String removeLabel, String addLabel, String count) {
        TextPaint label = g.font(type.manrope(600), 30, -.2f / 30, k.faint);
        c.drawText(onLabel, 140, g.baseline(label, 216, 40), label);
        float lw = label.measureText(onLabel);
        TextPaint cnt = g.font(type.manrope(500), 24, 0, k.faint);
        c.drawText(count, 140 + lw + 20, g.baseline(cnt, 216, 40), cnt);
        label = g.font(type.manrope(600), 30, -.2f / 30, k.faint);
        c.drawText(offLabel, 1068, g.baseline(label, 216, 40), label);

        TextPaint rb = g.font(type.manrope(500), 30, -.3f / 30, k.mute);
        float removeW = Math.max(304, (float) Math.ceil(rb.measureText(removeLabel)) + 48);
        for (int i = 0; i < left.size(); i++) {
            float y = 264 + i * ED_PITCH;
            boolean active = (zone == StageView.ED_LEFT || zone == StageView.ED_REMOVE) && i == leftIndex;
            boolean nameFocus = zone == StageView.ED_LEFT && i == leftIndex;
            float nameW = active && !lifted ? 880 - 8 - removeW : 880;
            if (nameFocus) sheetPlate(c, k, 112, y, nameW, 72, 24, lifted);
            else if (active && !lifted) g.round(c, 112, y, nameW, 72, 24, k.line); // the row the button acts on
            TextPaint num = g.font(type.manrope(500), 28, 0, nameFocus ? k.ink : k.mute);
            num.setTextAlign(Paint.Align.RIGHT);
            c.drawText(String.valueOf(i + 1), 112 + 16 + 36, g.baseline(num, y + 18, 36), num);
            num.setTextAlign(Paint.Align.LEFT);
            HomeItem item = left.get(i);
            marks.item(c, item, 112 + 16 + 36 + 20, y + 14, 56, 44, 36, k.ink, true, mono);
            TextPaint p = g.font(type.manrope(500), 34, -.5f / 34, k.ink);
            float tx = 112 + 16 + 36 + 20 + 56 + 20;
            c.drawText(g.ellipsize(item.label, nameW - (tx - 112) - 64), tx, g.baseline(p, y + 14, 44), p);
            if (lifted && nameFocus) {
                if (i > 0) g.chevron(c, 112 + nameW - 52, y + 2, 30, 2, 1.8f, k.ink);
                if (i < left.size() - 1) g.chevron(c, 112 + nameW - 52, y + 40, 30, 3, 1.8f, k.ink);
            }
            if (active && !lifted) {
                boolean f = zone == StageView.ED_REMOVE;
                float bx = 112 + nameW + 8;
                if (f) g.round(c, bx, y, removeW, 72, 24, k.solid);
                TextPaint b = g.font(type.manrope(500), 30, -.3f / 30, f ? k.solidInk : k.mute);
                b.setTextAlign(Paint.Align.CENTER);
                c.drawText(removeLabel, bx + removeW / 2, g.baseline(b, y + 16, 40), b);
                b.setTextAlign(Paint.Align.LEFT);
            }
        }

        int save = c.save();
        // Nine whole rows; the cue below marks more.
        c.clipRect(1040 - 8, 264 - 4, 1040 + 768 + 8, 264 + 8 * ED_PITCH + 72 + 4);
        for (int i = 0; i < right.size(); i++) {
            float y = 264 + (i - rightFirst) * ED_PITCH;
            if (y + 72 < 260 || y > 264 + 752) continue;
            boolean f = zone == StageView.ED_RIGHT && i == rightIndex;
            if (f) sheetPlate(c, k, 1040, y, 768, 72, 24, false);
            HomeItem item = right.get(i);
            marks.item(c, item, 1040 + 24, y + 14, 56, 44, 36, k.ink, true, mono);
            TextPaint p = g.font(type.manrope(500), 34, -.5f / 34, k.ink);
            float tx = 1040 + 24 + 56 + 20;
            float verbW = 0;
            if (f) {
                TextPaint v = g.font(type.manrope(600), 30, 0, k.ink);
                verbW = v.measureText(addLabel);
                c.drawText(addLabel, 1040 + 768 - 16 - verbW, g.baseline(v, y + 16, 40), v);
            }
            p = g.font(type.manrope(500), 34, -.5f / 34, k.ink);
            c.drawText(g.ellipsize(item.label, 768 - (tx - 1040) - verbW - 40), tx, g.baseline(p, y + 14, 44), p);
        }
        c.restoreToCount(save);
        if (rightFirst > 0.01f) g.chevron(c, 1408, 228, 32, 2, 1.8f, k.mute);
        if (rightFirst + 9 < right.size() - 0.01f) g.chevron(c, 1408, 990, 32, 3, 1.8f, k.mute);
    }
}
