package de.hamid.lumatv.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import de.hamid.lumatv.R;

/**
 * Original Japanese-inspired vector wallpapers (see tools/wallpapers). Each scene is rendered once
 * into a bitmap of the window size and handed to the GPU, so drawing the home screen costs one
 * textured quad instead of dozens of paths, and nothing is animated in the background.
 */
final class Wallpapers {
    static final String FUJI = "fuji";
    static final String TORII = "torii";
    static final String PINES = "kiefern";
    static final String[] IDS = {FUJI, TORII, PINES};

    private Wallpapers() {}

    static int drawable(String id) {
        if (TORII.equals(id)) return R.drawable.wall_torii;
        if (PINES.equals(id)) return R.drawable.wall_kiefern;
        return R.drawable.wall_fuji;
    }

    static int title(String id) {
        if (TORII.equals(id)) return R.string.wall_torii;
        if (PINES.equals(id)) return R.string.wall_kiefern;
        return R.string.wall_fuji;
    }

    static String normalize(String id) {
        for (String known : IDS) if (known.equals(id)) return known;
        return FUJI;
    }

    /** Worker thread. Returns a GPU-only bitmap where supported, or null if rendering failed. */
    static Bitmap render(Context context, String id, int width, int height) {
        if (width <= 0 || height <= 0) return null;
        try {
            Drawable scene = context.getDrawable(drawable(id));
            if (scene == null) return null;
            Bitmap software = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(software);
            scene.setBounds(0, 0, width, height);
            scene.draw(canvas);
            Bitmap gpu = software.copy(Bitmap.Config.HARDWARE, false);
            if (gpu == null) return software;
            software.recycle();
            return gpu;
        } catch (RuntimeException | OutOfMemoryError failed) {
            return null;
        }
    }
}
