package de.hamid.lumatv.stage;

import de.hamid.lumatv.R;

/**
 * Wallpaper gallery. Fifteen photographs from CleanFrame (Unsplash Licence, credits in NOTICE.md)
 * with their offline colour data, followed by Luma's three original vector scenes.
 * Colour data comes from CleanFrame's catalog.js and adaptive-ink.js; nothing samples pixels
 * at runtime.
 */
final class Walls {
    static final String DEFAULT_ID = "misty";

    /** Light/mist text gets a dark halo; dark text gets a pale halo. */
    static final int POL_LIGHT = 0, POL_MIST = 1, POL_DARK = 2;

    static final class Region {
        final int pol;
        final int ink;
        final float rest, peer;

        Region(int pol, int ink, float rest, float peer) {
            this.pol = pol;
            this.ink = ink;
            this.rest = rest;
            this.peer = peer;
        }
    }

    static final class Wall {
        final String id;
        final int name;
        /** Asset path for photos, or null for a vector scene drawn from {@link #vector}. */
        final String asset;
        final int vector;
        final boolean lightTone;
        /** Text colour for app names on this photo. */
        final int appInk;
        final Region tl, tr, br;
        final float frostVeil;
        /** Extra left-column shade in the List: 0, 2 or 3. */
        final int listColumn;
        final boolean dockQuiet, clockMist;

        Wall(String id, int name, String asset, int vector, boolean lightTone, int appInk,
             Region tl, Region tr, Region br, float frostVeil, int listColumn, boolean dockQuiet, boolean clockMist) {
            this.id = id;
            this.name = name;
            this.asset = asset;
            this.vector = vector;
            this.lightTone = lightTone;
            this.appInk = appInk;
            this.tl = tl;
            this.tr = tr;
            this.br = br;
            this.frostVeil = frostVeil;
            this.listColumn = listColumn;
            this.dockQuiet = dockQuiet;
            this.clockMist = clockMist;
        }

        String thumbAsset() { return asset == null ? null : asset.replace(".jpg", "-thumb.jpg"); }
    }

    private static Region light(int ink) { return new Region(POL_LIGHT, ink, .74f, .56f); }
    private static Region mistTr(int ink) { return new Region(POL_MIST, ink, .90f, .74f); }
    private static Region mistBr(int ink) { return new Region(POL_MIST, ink, .88f, .74f); }
    private static Region dark(int ink) { return new Region(POL_DARK, ink, 1f, 1f); }
    private static Region clock(int ink) { return new Region(POL_LIGHT, ink, 1f, 1f); }

    private static final int W = 0xFFF6F1E7, M = 0xFFFFFBF4;

    private static Wall photo(String id, int name, boolean lightTone, int appInk, Region tl, Region tr, Region br,
                              float fv, int lc, boolean quiet, boolean clockMist) {
        return new Wall(id, name, "wallpapers/" + id + ".jpg", 0, lightTone, appInk, tl, tr, br, fv, lc, quiet, clockMist);
    }

    private static Wall vector(String id, int name, int drawable) {
        return new Wall(id, name, null, drawable, false, W, clock(W), light(W), light(W), .2f, 0, false, false);
    }

    static final Wall[] ALL = {
            photo("misty", R.string.wp_misty, true, W, clock(W), mistTr(M), mistBr(M), .20f, 0, false, false),
            photo("cove", R.string.wp_cove, true, W, clock(W), light(W), mistBr(M), .12f, 0, false, true),
            photo("forest", R.string.wp_forest, false, W, clock(W), mistTr(M), light(W), .12f, 0, false, true),
            photo("valley", R.string.wp_valley, false, W, clock(W), mistTr(M), light(W), .12f, 3, false, false),
            photo("night", R.string.wp_night, true, 0xFFF2FCFF, clock(0xFFF2FCFF), light(0xFFF2FCFF), light(0xFFF2FCFF), .20f, 0, false, false),
            photo("peaks", R.string.wp_peaks, false, 0xFFF6FBFF, new Region(POL_DARK, 0xFF173348, 1f, 1f), dark(0xFF0C2131), mistBr(0xFFF6FBFF), .36f, 2, true, false),
            photo("bridge", R.string.wp_bridge, false, 0xFFFFF9F3, clock(0xFFFFF9F3), dark(0xFF2C1B0F), mistBr(0xFFFFF9F3), .44f, 0, false, false),
            photo("ridge", R.string.wp_ridge, false, W, clock(W), mistTr(M), light(W), .12f, 0, false, true),
            photo("golden", R.string.wp_golden, false, W, clock(W), mistTr(M), mistBr(M), .30f, 2, true, true),
            photo("summit", R.string.wp_summit, false, 0xFFFAFAFA, clock(0xFFFAFAFA), light(0xFFFAFAFA), light(0xFFFAFAFA), .20f, 0, false, false),
            photo("goldcoast", R.string.wp_goldcoast, true, 0xFFF2FCFF, clock(0xFFF2FCFF), mistTr(0xFFF2FCFF), mistBr(0xFFF2FCFF), .20f, 3, false, false),
            photo("sydney", R.string.wp_sydney, true, 0xFFF3FCFB, new Region(POL_MIST, 0xFFF3FCFB, 1f, 1f), mistTr(0xFFF3FCFB), mistBr(0xFFF3FCFB), .44f, 3, false, true),
            photo("amber", R.string.wp_amber, true, 0xFFFFF9F3, clock(0xFFFFF9F3), mistTr(0xFFFFF9F3), light(0xFFFFF9F3), .20f, 0, false, false),
            photo("mint", R.string.wp_mint, true, 0xFFF3FCFB, clock(0xFFF3FCFB), mistTr(0xFFF3FCFB), light(0xFFF3FCFB), .44f, 3, false, false),
            photo("alpine", R.string.wp_alpine, true, 0xFFF2FCFE, clock(0xFFF2FCFE), mistTr(0xFFF2FCFE), light(0xFFF2FCFE), .20f, 0, false, false),
            vector("fuji", R.string.wall_fuji, R.drawable.wall_fuji),
            vector("torii", R.string.wall_torii, R.drawable.wall_torii),
            vector("kiefern", R.string.wall_kiefern, R.drawable.wall_kiefern),
    };

    private Walls() {}

    static int indexOf(String id) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].id.equals(id)) return i;
        return 0;
    }

    static Wall get(String id) { return ALL[indexOf(id)]; }

    static String normalize(String id) { return ALL[indexOf(id)].id; }

    static String next(String id) { return ALL[(indexOf(id) + 1) % ALL.length].id; }
}
