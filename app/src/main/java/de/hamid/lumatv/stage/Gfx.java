package de.hamid.lumatv.stage;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;

/** Canvas helpers that reproduce the CSS used by CleanFrame on a 1920 x 1080 stage. */
final class Gfx {
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint bitmap = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();

    Gfx() {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    void rect(Canvas c, float l, float t, float rr, float b, int color) {
        fill.setShader(null);
        fill.setColor(color);
        c.drawRect(l, t, rr, b, fill);
    }

    void round(Canvas c, float l, float t, float w, float h, float radius, int color) {
        fill.setShader(null);
        fill.setColor(color);
        r.set(l, t, l + w, t + h);
        c.drawRoundRect(r, radius, radius, fill);
    }

    /** CSS linear-gradient(angle, ...) filling a rounded rect (radius 0 for a plain rect). */
    void gradient(Canvas c, float l, float t, float w, float h, float radius, float angleDeg, int[] colors, float[] stops) {
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.sin(a), dy = (float) -Math.cos(a);
        float len = Math.abs(w * dx) + Math.abs(h * dy);
        float cx = l + w / 2, cy = t + h / 2;
        fill.setShader(new LinearGradient(cx - dx * len / 2, cy - dy * len / 2, cx + dx * len / 2, cy + dy * len / 2,
                colors, stops, Shader.TileMode.CLAMP));
        r.set(l, t, l + w, t + h);
        if (radius > 0) c.drawRoundRect(r, radius, radius, fill); else c.drawRect(r, fill);
        fill.setShader(null);
    }

    /** Vertical gradient between absolute y positions, drawn over [l, t, rr, b]. */
    void vertical(Canvas c, float l, float t, float rr, float b, float y0, float y1, int[] colors, float[] stops) {
        fill.setShader(new LinearGradient(0, y0, 0, y1, colors, stops, Shader.TileMode.CLAMP));
        c.drawRect(l, t, rr, b, fill);
        fill.setShader(null);
    }

    /** Horizontal gradient between absolute x positions, drawn over [l, t, rr, b]. */
    void horizontal(Canvas c, float l, float t, float rr, float b, float x0, float x1, int[] colors, float[] stops) {
        fill.setShader(new LinearGradient(x0, 0, x1, 0, colors, stops, Shader.TileMode.CLAMP));
        c.drawRect(l, t, rr, b, fill);
        fill.setShader(null);
    }

    /** CSS radial-gradient(ellipse rx ry at cx cy, ...). */
    void ellipse(Canvas c, float cx, float cy, float rx, float ry, int[] colors, float[] stops) {
        RadialGradient g = new RadialGradient(0, 0, rx, colors, stops, Shader.TileMode.CLAMP);
        matrix.reset();
        matrix.setScale(1, ry / rx);
        matrix.postTranslate(cx, cy);
        g.setLocalMatrix(matrix);
        fill.setShader(g);
        c.drawRect(cx - rx, cy - ry, cx + rx, cy + ry, fill);
        fill.setShader(null);
    }

    /**
     * Draws a stage-registered glass field (quarter resolution) inside a rounded rect, so the
     * glass shows exactly the blurred picture behind it. {@code zoom} scales about (ox, oy).
     */
    void field(Canvas c, Bitmap field, float l, float t, float w, float h, float radius,
               float zoom, float ox, float oy) {
        if (field == null) return;
        BitmapShader shader = new BitmapShader(field, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        matrix.reset();
        matrix.setScale(Scene.STAGE_W / (float) field.getWidth(), Scene.STAGE_H / (float) field.getHeight());
        if (zoom != 1f) matrix.postScale(zoom, zoom, ox, oy);
        shader.setLocalMatrix(matrix);
        bitmap.setShader(shader);
        r.set(l, t, l + w, t + h);
        c.drawRoundRect(r, radius, radius, bitmap);
        bitmap.setShader(null);
    }

    /** Gradient that is registered to the stage (absolute y), clipped to a rounded rect. */
    void registeredVertical(Canvas c, float l, float t, float w, float h, float radius,
                            float y0, float y1, int[] colors, float[] stops) {
        fill.setShader(new LinearGradient(0, y0, 0, y1, colors, stops, Shader.TileMode.CLAMP));
        r.set(l, t, l + w, t + h);
        c.drawRoundRect(r, radius, radius, fill);
        fill.setShader(null);
    }

    /** CSS inset 0 1px 0 rgba(...): a top hairline that follows the radius. */
    void hairline(Canvas c, float l, float t, float w, float h, float radius, int color) {
        stroke.setStrokeWidth(1.5f);
        stroke.setShader(new LinearGradient(0, t, 0, t + Math.min(radius * .6f, h / 2),
                new int[]{color, color & 0x00FFFFFF}, new float[]{0, 1}, Shader.TileMode.CLAMP));
        r.set(l + .75f, t + .75f, l + w - .75f, t + h - .75f);
        c.drawRoundRect(r, radius, radius, stroke);
        stroke.setShader(null);
    }

    /** Thin uniform inner ring. */
    void ring(Canvas c, float l, float t, float w, float h, float radius, float width, int color) {
        stroke.setShader(null);
        stroke.setStrokeWidth(width);
        stroke.setColor(color);
        r.set(l + width / 2, t + width / 2, l + w - width / 2, t + h - width / 2);
        c.drawRoundRect(r, radius, radius, stroke);
    }

    // ------------------------------------------------------------------ text

    /** Baseline that centres the glyph box in a CSS line box starting at top. */
    float baseline(Paint p, float top, float lineHeight) {
        Paint.FontMetrics fm = p.getFontMetrics();
        return top + lineHeight / 2 - (fm.ascent + fm.descent) / 2;
    }

    /**
     * Shortens s with a trailing "…" so it fits width with the current text paint. Measured
     * directly (no Layout objects), so it costs nothing when the text already fits.
     */
    String ellipsize(String s, float width) {
        if (s == null || s.isEmpty() || text.measureText(s) <= width) return s == null ? "" : s;
        final String dots = "\u2026";
        float room = width - text.measureText(dots);
        if (room <= 0) return "";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (text.measureText(s, 0, mid) <= room) lo = mid; else hi = mid - 1;
        }
        // Never split a surrogate pair, and drop trailing spaces before the dots.
        if (lo > 0 && Character.isHighSurrogate(s.charAt(lo - 1))) lo--;
        while (lo > 0 && s.charAt(lo - 1) == ' ') lo--;
        return s.substring(0, lo) + dots;
    }

    /** Sets the text paint; letterSpacing in em. */
    TextPaint font(android.graphics.Typeface face, float size, float spacingEm, int color) {
        text.setTypeface(face);
        text.setTextSize(size);
        text.setLetterSpacing(spacingEm);
        text.setColor(color);
        text.setTextAlign(Paint.Align.LEFT);
        text.clearShadowLayer();
        text.setFontFeatureSettings(null);
        return text;
    }

    /** CSS text-shadow 0 dy blur color, converted to Skia's blur radius. */
    void shadow(float blur, float dy, int color) {
        float radius = Math.max(.5f, (blur / 2f - .5f) / .57735f);
        text.setShadowLayer(radius, 0, dy, color);
    }

    // ------------------------------------------------------------------ small outline glyphs (24 grid)

    void chevron(Canvas c, float x, float y, float size, int dir, float width, int color) {
        float k = size / 24f;
        path.reset();
        switch (dir) {
            case 0: path.moveTo(9, 6); path.lineTo(15, 12); path.lineTo(9, 18); break;       // right
            case 1: path.moveTo(15, 6); path.lineTo(9, 12); path.lineTo(15, 18); break;      // left
            case 2: path.moveTo(6, 15); path.lineTo(12, 9); path.lineTo(18, 15); break;      // up
            default: path.moveTo(6, 9); path.lineTo(12, 15); path.lineTo(18, 9); break;      // down
        }
        drawGlyph(c, x, y, k, width, color);
    }

    void check(Canvas c, float x, float y, float size, float width, int color) {
        path.reset();
        path.moveTo(5, 12.5f);
        path.lineTo(10, 17.5f);
        path.lineTo(19.5f, 7);
        drawGlyph(c, x, y, size / 24f, width, color);
    }

    void plus(Canvas c, float x, float y, float size, float width, int color) {
        path.reset();
        path.moveTo(12, 5);
        path.lineTo(12, 19);
        path.moveTo(5, 12);
        path.lineTo(19, 12);
        drawGlyph(c, x, y, size / 24f, width, color);
    }

    private void drawGlyph(Canvas c, float x, float y, float k, float width, int color) {
        int save = c.save();
        c.translate(x, y);
        c.scale(k, k);
        stroke.setShader(null);
        // Width in 24-grid units, like SVG stroke-width; the canvas scale applies it.
        stroke.setStrokeWidth(width);
        stroke.setColor(color);
        c.drawPath(path, stroke);
        c.restoreToCount(save);
    }

    /** A rounded pill containing a remote key name, like CleanFrame's remote-key hint. */
    float keyPill(Canvas c, String key, float x, float top, int color, android.graphics.Typeface face) {
        font(face, 21, 0, color);
        float w = Math.max(48, text.measureText(key) + 20);
        stroke.setShader(null);
        stroke.setStrokeWidth(1.5f);
        stroke.setColor(color);
        r.set(x + .75f, top + .75f, x + w - .75f, top + 34 - .75f);
        c.drawRoundRect(r, 17, 17, stroke);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(key, x + w / 2, baseline(text, top, 34), text);
        text.setTextAlign(Paint.Align.LEFT);
        return w;
    }
}
