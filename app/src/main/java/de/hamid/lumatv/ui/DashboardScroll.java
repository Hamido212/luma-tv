package de.hamid.lumatv.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.widget.ScrollView;

/**
 * Vertical page. The focused row is always placed where the first row starts, so the
 * eye does not have to search for the focus after every press of up or down.
 */
final class DashboardScroll extends ScrollView {
    interface Aligner { int targetScrollY(View focused); }

    private Aligner aligner;
    private View lastFocused;

    DashboardScroll(Context context) {
        super(context);
        setFocusable(false);
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setSmoothScrollingEnabled(ValueAnimator.areAnimatorsEnabled());
    }

    void setAligner(Aligner aligner) { this.aligner = aligner; }

    @Override public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        lastFocused = focused;
        if (!isLayoutRequested()) align(focused, isSmoothScrollingEnabled());
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (lastFocused != null && lastFocused.isFocused()) align(lastFocused, false);
    }

    private void align(View focused, boolean smooth) {
        if (aligner == null || getChildCount() == 0) return;
        int target = aligner.targetScrollY(focused);
        if (target < 0) return;
        int max = Math.max(0, getChildAt(0).getHeight() - getHeight());
        target = Math.max(0, Math.min(target, max));
        if (target == getScrollY()) return;
        if (smooth) smoothScrollTo(0, target);
        else scrollTo(0, target);
    }
}
