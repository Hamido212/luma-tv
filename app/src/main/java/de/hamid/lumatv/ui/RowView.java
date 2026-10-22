package de.hamid.lumatv.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.widget.HorizontalScrollView;

/**
 * Horizontal app row. Keeps the focused tile inside the TV-safe margin instead of letting it
 * touch the screen edge; scrolls instantly when system animations are off.
 * Created in code only, never inflated from XML.
 */
@SuppressLint("ViewConstructor")
final class RowView extends HorizontalScrollView {
    private final int edge;

    RowView(Context context, int edge) {
        super(context);
        this.edge = edge;
        setFocusable(false);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipChildren(false);
        setClipToPadding(false);
        setSmoothScrollingEnabled(ValueAnimator.areAnimatorsEnabled());
    }

    @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) return 0;
        int width = getWidth();
        int scrollX = getScrollX();
        int visibleLeft = scrollX + edge;
        int visibleRight = scrollX + width - edge;
        int delta = 0;
        if (rect.right > visibleRight) delta = rect.right - visibleRight;
        if (rect.left - delta < visibleLeft) delta = rect.left - visibleLeft;
        int maxScroll = Math.max(0, getChildAt(0).getWidth() - width);
        int target = Math.max(0, Math.min(scrollX + delta, maxScroll));
        return target - scrollX;
    }
}
