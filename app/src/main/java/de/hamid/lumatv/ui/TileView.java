package de.hamid.lumatv.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A 16:9 tile. Apps with a TV banner show it edge to edge; others get an ink card with icon and name.
 * Focus: the card lifts, a slim ivory ring appears with a small gap, and the name shows underneath.
 * Unfocused cards are dimmed slightly so the focused one reads first. Created in code only.
 */
@SuppressLint("ViewConstructor")
final class TileView extends LinearLayout {
    static final int KIND_APP = 0;
    static final int KIND_INPUT = 1;

    static final int CARD_WIDTH_DP = 176;
    static final int CARD_HEIGHT_DP = 99;
    static final int RING_GAP_DP = 5;

    final String rowId;
    final String itemKey;
    final int kind;
    final String title;

    private final FrameLayout frame;
    private final FrameLayout card;
    private final TextView label;
    private final Drawable dimLayer;
    private final float density;

    TileView(Context context, String rowId, String itemKey, int kind, String title, String focusLabel,
             Drawable banner, Drawable icon, boolean tintIcon) {
        super(context);
        this.rowId = rowId;
        this.itemKey = itemKey;
        this.kind = kind;
        this.title = title;
        density = getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        setDefaultFocusHighlightEnabled(false);
        setClipChildren(false);
        setClipToPadding(false);
        setContentDescription(focusLabel);

        int gap = dp(RING_GAP_DP);
        float cardRadius = 12 * density;

        frame = new FrameLayout(context);
        frame.setPadding(gap, gap, gap, gap);
        frame.setClipChildren(false);
        frame.setDuplicateParentStateEnabled(true);
        StateListDrawable ring = new StateListDrawable();
        GradientDrawable ringOn = new GradientDrawable();
        ringOn.setCornerRadius(cardRadius + gap);
        ringOn.setColor(0x00000000);
        ringOn.setStroke(Math.round(2.5f * density), Palette.IVORY);
        ring.addState(new int[]{android.R.attr.state_focused}, ringOn);
        ring.addState(new int[]{}, new ColorDrawable(0x00000000));
        frame.setBackground(ring);

        card = new FrameLayout(context);
        card.setClipToOutline(true);
        if (banner != null) {
            GradientDrawable base = new GradientDrawable();
            base.setCornerRadius(cardRadius);
            base.setColor(0xFF1B1C30);
            card.setBackground(base);
            ImageView image = new ImageView(context);
            image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageDrawable(banner);
            card.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        } else {
            GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Palette.INK_CARD, Palette.INK_CARD_END});
            base.setCornerRadius(cardRadius);
            base.setStroke(Math.max(1, Math.round(density)), 0x26EDE6D3);
            card.setBackground(base);
            LinearLayout content = new LinearLayout(context);
            content.setOrientation(VERTICAL);
            content.setGravity(Gravity.CENTER);
            ImageView image = new ImageView(context);
            image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageDrawable(icon);
            if (tintIcon) image.setImageTintList(ColorStateList.valueOf(Palette.IVORY));
            int size = dp(kind == KIND_INPUT ? 34 : 40);
            content.addView(image, new LinearLayout.LayoutParams(size, size));
            TextView name = new TextView(context);
            name.setText(title);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            name.setTextColor(Palette.IVORY_SOFT);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setGravity(Gravity.CENTER);
            name.setPadding(dp(10), dp(6), dp(10), 0);
            name.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            content.addView(name, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            card.addView(content, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        GradientDrawable dim = new GradientDrawable();
        dim.setCornerRadius(cardRadius);
        dim.setColor(0x33070812);
        dimLayer = dim;
        frame.addView(card, new FrameLayout.LayoutParams(dp(CARD_WIDTH_DP), dp(CARD_HEIGHT_DP)));
        addView(frame, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        label = new TextView(context);
        label.setText(focusLabel);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setTextColor(Palette.IVORY);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(gap + dp(2), dp(4), gap, 0);
        label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(label, new LayoutParams(dp(CARD_WIDTH_DP) + 2 * gap, LayoutParams.WRAP_CONTENT));
        applyState(false);
    }

    @Override protected void onFocusChanged(boolean gainFocus, int direction, Rect previous) {
        super.onFocusChanged(gainFocus, direction, previous);
        applyState(true);
    }

    private void applyState(boolean animated) {
        boolean focused = isFocused();
        card.setForeground(focused ? null : dimLayer);
        label.setVisibility(focused ? VISIBLE : INVISIBLE);
        float scale = focused ? 1.06f : 1f;
        float lift = focused ? 14 * density : 0f;
        frame.animate().cancel();
        card.animate().cancel();
        if (animated && ValueAnimator.areAnimatorsEnabled()) {
            frame.animate().scaleX(scale).scaleY(scale).setDuration(140).start();
            card.animate().translationZ(lift).setDuration(140).start();
        } else {
            frame.setScaleX(scale);
            frame.setScaleY(scale);
            card.setTranslationZ(lift);
        }
    }

    String focusKey() { return rowId + "|" + itemKey; }

    private int dp(int value) { return Math.round(value * density); }
}
