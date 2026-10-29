package de.hamid.lumatv.stage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import java.io.IOException;
import java.io.InputStream;

/**
 * One loaded wallpaper: the full picture plus three small glass fields (Clear, Soft, Frost).
 * CleanFrame renders these fields offline; Luma renders them once per wallpaper on a worker
 * thread at quarter resolution (480 x 270), so any scene, including the vector ones, gets
 * matching glass. Navigation never blurs or samples pixels.
 */
final class Scene {
    static final int STAGE_W = 1920, STAGE_H = 1080, FIELD_SCALE = 4;

    final Walls.Wall wall;
    /** Full-screen picture, GPU-backed where possible. Half resolution for previews. */
    final Bitmap picture;
    final Bitmap clear, soft, frost;

    private Scene(Walls.Wall wall, Bitmap picture, Bitmap clear, Bitmap soft, Bitmap frost) {
        this.wall = wall;
        this.picture = picture;
        this.clear = clear;
        this.soft = soft;
        this.frost = frost;
    }

    /**
     * GPU bitmaps for the photo and the composed backdrop. Tests that draw into a software canvas
     * switch this off, because a software canvas cannot draw HARDWARE bitmaps.
     */
    static boolean hardware = true;

    Bitmap field(String material) {
        if (Look.MAT_FROST.equals(material)) return frost;
        if (Look.MAT_CLEAR.equals(material)) return clear;
        return soft;
    }

    /** Worker thread. Returns null when the picture cannot be decoded. */
    static Scene load(Context context, Walls.Wall wall, boolean halfResolution) {
        try {
            int sample = halfResolution ? 2 : 1;
            Bitmap picture = wall.asset != null
                    ? decode(context, wall.asset, sample, hardware)
                    : gpu(renderVector(context, wall.vector, STAGE_W / sample, STAGE_H / sample));
            Bitmap small = wall.asset != null
                    ? decode(context, wall.asset, FIELD_SCALE, false)
                    : renderVector(context, wall.vector, STAGE_W / FIELD_SCALE, STAGE_H / FIELD_SCALE);
            if (picture == null || small == null) return null;
            // CSS blur radii at 1920 px (Clear 2, Soft 14, Frost 42) become sigma / 4 here.
            Bitmap clear = colour(small, 1.06f);
            Bitmap soft = colour(blur(small, 3), .92f);
            Bitmap frost = colour(blur(small, 10), .92f);
            small.recycle();
            return new Scene(wall, picture, clear, soft, frost);
        } catch (IOException | RuntimeException | OutOfMemoryError failed) {
            return null;
        }
    }

    /** Small gallery picture, 256 x 144. */
    static Bitmap thumb(Context context, Walls.Wall wall) {
        try {
            if (wall.asset == null) return renderVector(context, wall.vector, 256, 144);
            return decode(context, wall.thumbAsset(), 1, false);
        } catch (IOException | RuntimeException | OutOfMemoryError failed) {
            return null;
        }
    }

    private static Bitmap decode(Context context, String asset, int sample, boolean hardware) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = hardware ? Bitmap.Config.HARDWARE : Bitmap.Config.ARGB_8888;
        try (InputStream in = context.getAssets().open(asset)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
            if (bitmap == null && hardware) return decode(context, asset, sample, false);
            return bitmap;
        }
    }

    private static Bitmap renderVector(Context context, int drawable, int width, int height) {
        Drawable scene = context.getDrawable(drawable);
        if (scene == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        scene.setBounds(0, 0, width, height);
        scene.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static Bitmap gpu(Bitmap software) {
        if (software == null || !hardware) return software;
        Bitmap gpu = software.copy(Bitmap.Config.HARDWARE, false);
        if (gpu == null) return software;
        software.recycle();
        return gpu;
    }

    private static Bitmap colour(Bitmap source, float saturation) {
        Bitmap out = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(saturation);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        new Canvas(out).drawBitmap(source, 0, 0, paint);
        return out;
    }

    /** Three box passes per axis approximate a Gaussian; edges clamp, like mirrored padding. */
    private static Bitmap blur(Bitmap source, int radius) {
        int w = source.getWidth(), h = source.getHeight();
        int[] a = new int[w * h], b = new int[w * h];
        source.getPixels(a, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            box(a, b, w, h, radius, true);
            box(b, a, w, h, radius, false);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(a, 0, w, 0, 0, w, h);
        return out;
    }

    private static void box(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w, length = horizontal ? w : h;
        int step = horizontal ? 1 : w;
        float div = 2 * r + 1;
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line;
            int sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int p = in[base + clamp(i, length) * step];
                sr += (p >> 16) & 0xFF;
                sg += (p >> 8) & 0xFF;
                sb += p & 0xFF;
            }
            for (int i = 0; i < length; i++) {
                out[base + i * step] = 0xFF000000 | (Math.round(sr / div) << 16) | (Math.round(sg / div) << 8) | Math.round(sb / div);
                int add = in[base + clamp(i + r + 1, length) * step];
                int sub = in[base + clamp(i - r, length) * step];
                sr += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                sg += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                sb += (add & 0xFF) - (sub & 0xFF);
            }
        }
    }

    private static int clamp(int i, int length) { return i < 0 ? 0 : i >= length ? length - 1 : i; }
}
