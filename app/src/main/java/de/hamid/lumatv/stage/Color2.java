package de.hamid.lumatv.stage;

/** Small colour helpers; CSS rgba values are written as rgba(r, g, b, a). */
final class Color2 {
    private Color2() {}

    static int rgba(int r, int g, int b, float a) {
        return (Math.round(clamp(a) * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    static int alpha(int color) { return color >>> 24; }

    static int withAlpha(int color, float a) {
        return (Math.round(clamp(a) * 255) << 24) | (color & 0x00FFFFFF);
    }

    /** Multiplies the colour's own alpha by the given opacity. */
    static int fade(int color, float opacity) {
        return withAlpha(color, alpha(color) / 255f * opacity);
    }

    private static float clamp(float a) { return a < 0 ? 0 : a > 1 ? 1 : a; }
}
