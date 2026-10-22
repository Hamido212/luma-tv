package de.hamid.lumatv.stage;

import android.os.SystemClock;

/** One animated float with CleanFrame's ease, cubic-bezier(.2, .7, .2, 1). */
final class Anim {
    private float from, to;
    private long start;
    private long duration;

    Anim(float value) {
        from = to = value;
    }

    void to(float target, long durationMs) {
        if (target == to) return;
        long now = SystemClock.uptimeMillis();
        from = value(now);
        to = target;
        start = now;
        duration = durationMs;
    }

    void jump(float value) {
        from = to = value;
        duration = 0;
    }

    float target() { return to; }

    float value(long now) {
        if (duration <= 0 || now >= start + duration) return to;
        float t = (now - start) / (float) duration;
        return from + (to - from) * ease(t);
    }

    boolean running(long now) { return duration > 0 && now < start + duration; }

    /** Solves the bezier for x = t, then returns y. */
    static float ease(float x) {
        final float x1 = .2f, y1 = .7f, x2 = .2f, y2 = 1f;
        float t = x;
        for (int i = 0; i < 8; i++) {
            float cx = bezier(t, x1, x2) - x;
            float d = derivative(t, x1, x2);
            if (Math.abs(cx) < 1e-4f || d == 0) break;
            t -= cx / d;
            if (t < 0) t = 0; else if (t > 1) t = 1;
        }
        return bezier(t, y1, y2);
    }

    private static float bezier(float t, float p1, float p2) {
        float u = 1 - t;
        return 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t;
    }

    private static float derivative(float t, float p1, float p2) {
        float u = 1 - t;
        return 3 * u * u * p1 + 6 * u * t * (p2 - p1) + 3 * t * t * (1 - p2);
    }
}
