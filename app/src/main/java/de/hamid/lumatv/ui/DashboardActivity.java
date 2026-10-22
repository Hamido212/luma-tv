package de.hamid.lumatv.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.hamid.lumatv.R;
import de.hamid.lumatv.data.HomeConfigStore;
import de.hamid.lumatv.data.RecentStore;
import de.hamid.lumatv.model.AppEntry;
import de.hamid.lumatv.model.HomeRow;
import de.hamid.lumatv.platform.AppCatalog;
import de.hamid.lumatv.platform.AppLauncher;
import de.hamid.lumatv.platform.HdmiLauncher;
import de.hamid.lumatv.platform.HdmiRepository;
import de.hamid.lumatv.platform.NetworkStatus;
import de.hamid.lumatv.platform.SettingsNavigator;

/**
 * Luma TV dashboard: categories as horizontal rows, a quiet header with inputs, network,
 * settings and clock. Remote navigation is explicit: left/right stay inside a row,
 * up/down return to the tile last used in the neighbouring row.
 */
public final class DashboardActivity extends Activity {
    private static final String STATE_FOCUS = "luma_focus_key";
    private static final String HEADER = "header";
    private static final String ROW_INPUTS = "inputs";
    private static final String ROW_RECENTS = "recents";
    private static final String ROW_MORE = "more";
    private static final String INPUT_PREFIX = "input:";
    private static final long LAUNCH_GUARD_MS = 1200;
    private static final int EDGE_DP = 56;
    /** Rows below the first one are scrolled so their title sits on this line. */
    private static final int FOCUS_LINE_DP = 118;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService painter = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable greetingTick = this::updateGreeting;
    private final List<NavRow> navRows = new ArrayList<>();
    private final IdentityHashMap<View, int[]> positions = new IdentityHashMap<>();
    private final Map<String, String> rememberedByRow = new HashMap<>();

    private HomeConfigStore config;
    private RecentStore recents;
    private NetworkStatus network;
    private boolean homeVariant;
    private float density;

    private DashboardScroll scroll;
    private LinearLayout rowsContainer;
    private TextView emptyView;
    private TextView caption;
    private TextView greeting;
    private Typeface mincho;
    private String paintedWallpaper;
    private ImageView networkButton;
    private TextClock clock;
    private TextClock amPm;

    private Map<String, AppEntry> apps = Collections.emptyMap();
    private Map<String, Drawable> banners = Collections.emptyMap();
    private Map<String, Drawable> icons = Collections.emptyMap();
    private Map<String, String> inputLabels = Collections.emptyMap();
    private List<String> shownRecents = Collections.emptyList();
    private String pendingFocusKey;
    private boolean catalogDirty = true;
    private boolean userNavigated;
    private boolean loading;
    private boolean reloadRequested;
    private boolean resumed;
    private long lastLaunch;
    private AlertDialog dialog;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            catalogDirty = true;
            if (resumed) loadCatalog();
        }
    };

    private final View.OnFocusChangeListener navFocus = (view, hasFocus) -> {
        int[] position = positions.get(view);
        if (position == null) return;
        if (hasFocus) {
            NavRow row = navRows.get(position[0]);
            row.remembered = position[1];
            if (view instanceof TileView) rememberedByRow.put(row.id, ((TileView) view).itemKey);
        }
        if (position[0] == 0) caption.setText(hasFocus ? view.getContentDescription() : "");
    };

    /** One focus row: the header shortcuts (index 0) or a row of tiles. */
    private static final class NavRow {
        final String id;
        final View section;
        final RowView strip;
        final List<View> items = new ArrayList<>();
        int remembered;

        NavRow(String id, View section, RowView strip) {
            this.id = id;
            this.section = section;
            this.strip = strip;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Since 0.3 the classic dashboard is a secondary screen opened from Luma's Settings;
        // Home belongs to StageActivity, so Back simply closes this view.
        homeVariant = false;
        density = getResources().getDisplayMetrics().density;
        SharedPreferences prefs = getSharedPreferences("luma_home", MODE_PRIVATE);
        config = new HomeConfigStore(prefs);
        recents = new RecentStore(prefs);
        network = new NetworkStatus(this);
        mincho = loadMincho();
        if (state != null) pendingFocusKey = state.getString(STATE_FOCUS);
        setContentView(buildPage());
        paintWallpaper();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, filter);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        network.start(this::showNetwork);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        applyClockMode();
        updateGreeting();
        if (catalogDirty) {
            loadCatalog();
        } else if (config.recentsEnabled() && !visibleRecents(config.hidden()).equals(shownRecents)) {
            rebuildRows();
        }
    }

    @Override protected void onPause() {
        resumed = false;
        main.removeCallbacks(greetingTick);
        super.onPause();
    }

    @Override protected void onStop() {
        network.stop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        dismissDialog();
        unregisterReceiver(packageReceiver);
        worker.shutdownNow();
        painter.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(STATE_FOCUS, currentFocusKey());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (homeVariant && intent.hasCategory(Intent.CATEGORY_HOME)) goHome();
    }

    // ---------------------------------------------------------------- layout

    private View buildPage() {
        FrameLayout root = new FrameLayout(this);
        scroll = new DashboardScroll(this);
        scroll.setAligner(this::targetScrollY);
        scroll.setVerticalFadingEdgeEnabled(true);
        scroll.setFadingEdgeLength(dp(96));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(34), 0, dp(300));
        page.setClipChildren(false);
        page.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowsContainer = new LinearLayout(this);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        rowsContainer.setClipChildren(false);
        page.addView(rowsContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        emptyView = text(getString(R.string.empty_home), 16, Palette.MIST, null);
        emptyView.setPadding(dp(EDGE_DP), dp(24), dp(EDGE_DP), 0);
        emptyView.setVisibility(View.GONE);
        page.addView(emptyView);
        scroll.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        header.setPadding(dp(EDGE_DP), 0, dp(EDGE_DP - 8), dp(26));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        greeting = text("", 18, Palette.IVORY_SOFT, null);
        greeting.setTypeface(mincho);
        hero.addView(greeting, wrap());
        LinearLayout timeLine = new LinearLayout(this);
        timeLine.setOrientation(LinearLayout.HORIZONTAL);
        timeLine.setBaselineAligned(true);
        clock = new TextClock(this);
        styleText(clock, 62, Palette.IVORY, null);
        clock.setTypeface(mincho);
        clock.setFontFeatureSettings("lnum, tnum");
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.02f);
        timeLine.addView(clock, wrap());
        amPm = new TextClock(this);
        amPm.setFormat12Hour("a");
        amPm.setFormat24Hour("a");
        styleText(amPm, 18, Palette.IVORY_SOFT, null);
        amPm.setTypeface(mincho);
        LinearLayout.LayoutParams amPmLp = wrap();
        amPmLp.setMarginStart(dp(8));
        timeLine.addView(amPm, amPmLp);
        LinearLayout.LayoutParams timeLp = wrap();
        timeLp.topMargin = dp(2);
        hero.addView(timeLine, timeLp);
        TextClock date = new TextClock(this);
        String datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM");
        date.setFormat12Hour(datePattern);
        date.setFormat24Hour(datePattern);
        styleText(date, 14, Palette.MIST, null);
        LinearLayout.LayoutParams dateLp = wrap();
        dateLp.topMargin = dp(6);
        hero.addView(date, dateLp);
        header.addView(hero, wrap());

        header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER_VERTICAL);
        caption = text("", 14, Palette.IVORY_SOFT, null);
        LinearLayout.LayoutParams captionLp = wrap();
        captionLp.setMarginEnd(dp(12));
        status.addView(caption, captionLp);

        NavRow headerRow = new NavRow(HEADER, null, null);
        navRows.add(headerRow);
        ImageView inputs = shortcut(R.drawable.ic_inputs, getString(R.string.shortcut_inputs), v -> showInputs());
        networkButton = shortcut(R.drawable.ic_wifi, getString(R.string.network_wifi), v -> openSettings(Settings.ACTION_WIFI_SETTINGS));
        ImageView settings = shortcut(R.drawable.ic_settings, getString(R.string.shortcut_settings), v -> showSettingsMenu());
        for (ImageView button : new ImageView[]{inputs, networkButton, settings}) {
            positions.put(button, new int[]{0, headerRow.items.size()});
            headerRow.items.add(button);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
            lp.setMarginStart(dp(4));
            status.addView(button, lp);
        }
        LinearLayout.LayoutParams statusLp = wrap();
        statusLp.topMargin = dp(4);
        header.addView(status, statusLp);
        return header;
    }

    private Typeface loadMincho() {
        try {
            return getResources().getFont(R.font.luma_mincho);
        } catch (RuntimeException unavailable) {
            return Typeface.SERIF;
        }
    }

    private void updateGreeting() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int text = hour < 5 ? R.string.greeting_night : hour < 11 ? R.string.greeting_morning
                : hour < 17 ? R.string.greeting_day : hour < 22 ? R.string.greeting_evening : R.string.greeting_night;
        greeting.setText(text);
        main.removeCallbacks(greetingTick);
        if (resumed) {
            long untilNextHour = (60 - now.get(Calendar.MINUTE)) * 60_000L - now.get(Calendar.SECOND) * 1000L + 500L;
            main.postDelayed(greetingTick, Math.max(1000L, untilNextHour));
        }
    }

    private void paintWallpaper() {
        String id = Wallpapers.normalize(config.wallpaper());
        if (id.equals(paintedWallpaper) || painter.isShutdown()) return;
        paintedWallpaper = id;
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        Context app = getApplicationContext();
        painter.execute(() -> {
            Bitmap bitmap = Wallpapers.render(app, id, width, height);
            runOnUiThread(() -> {
                if (isDestroyed() || bitmap == null || !id.equals(paintedWallpaper)) return;
                getWindow().setBackgroundDrawable(new BitmapDrawable(getResources(), bitmap));
            });
        });
    }

    private ImageView shortcut(int icon, String description, View.OnClickListener click) {
        ImageView button = new ImageView(this);
        button.setImageResource(icon);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        StateListDrawable background = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.OVAL);
        focused.setColor(Palette.IVORY);
        background.addState(new int[]{android.R.attr.state_focused}, focused);
        background.addState(new int[]{}, new ColorDrawable(0x00000000));
        button.setBackground(background);
        button.setImageTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{Palette.INK, Palette.IVORY_SOFT}));
        button.setFocusable(true);
        button.setClickable(true);
        button.setDefaultFocusHighlightEnabled(false);
        button.setContentDescription(description);
        button.setOnClickListener(click);
        button.setOnFocusChangeListener(navFocus);
        return button;
    }

    private TextView text(String value, int sp, int color, String family) {
        TextView view = new TextView(this);
        view.setText(value);
        styleText(view, sp, color, family);
        return view;
    }

    private void styleText(TextView view, int sp, int color, String family) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (family != null) view.setTypeface(Typeface.create(family, Typeface.NORMAL));
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) { return Math.round(value * density); }

    // ---------------------------------------------------------------- data

    private void loadCatalog() {
        if (loading) {
            reloadRequested = true;
            return;
        }
        if (worker.isShutdown()) return;
        loading = true;
        reloadRequested = false;
        PackageManager pm = getPackageManager();
        String own = getPackageName();
        Context app = getApplicationContext();
        worker.execute(() -> {
            Map<String, AppEntry> found = new LinkedHashMap<>();
            Map<String, Drawable> foundBanners = new HashMap<>();
            Map<String, Drawable> foundIcons = new HashMap<>();
            Map<String, String> foundInputs = new LinkedHashMap<>();
            try {
                for (AppEntry entry : AppCatalog.discover(pm, own)) {
                    String pkg = entry.packageName();
                    found.put(pkg, entry);
                    Drawable banner = null;
                    try {
                        banner = pm.getActivityBanner(entry.component);
                    } catch (PackageManager.NameNotFoundException | RuntimeException noBanner) {
                        // Fall back to the icon below.
                    }
                    if (banner != null) {
                        foundBanners.put(pkg, banner);
                    } else {
                        try {
                            foundIcons.put(pkg, pm.getActivityIcon(entry.component));
                        } catch (PackageManager.NameNotFoundException | RuntimeException noIcon) {
                            foundIcons.put(pkg, pm.getDefaultActivityIcon());
                        }
                    }
                }
            } catch (RuntimeException restricted) {
                // Show what could be read; the empty state covers the rest.
            }
            try {
                for (TvInputInfo input : HdmiRepository.discover(app)) {
                    foundInputs.put(input.getId(), HdmiRepository.label(app, input));
                }
            } catch (RuntimeException noInputs) {
                // Inputs stay unavailable on devices without the TV input framework.
            }
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                apps = found;
                banners = foundBanners;
                icons = foundIcons;
                inputLabels = foundInputs;
                loading = false;
                catalogDirty = false;
                rebuildRows();
                if (reloadRequested) loadCatalog();
            });
        });
    }

    private List<String> visibleRecents(Set<String> hidden) {
        List<String> result = new ArrayList<>();
        for (String pkg : recents.read()) {
            if (apps.containsKey(pkg) && !hidden.contains(pkg)) result.add(pkg);
        }
        return result;
    }

    private void rebuildRows() {
        // Until the user presses a key, focus belongs on the first app: the window itself puts it on
        // the first header button when it is shown, which must not count as a choice.
        String focusKey = pendingFocusKey != null ? pendingFocusKey : userNavigated ? currentFocusKey() : null;
        pendingFocusKey = null;
        rowsContainer.removeAllViews();
        NavRow header = navRows.get(0);
        navRows.clear();
        navRows.add(header);
        positions.entrySet().removeIf(entry -> entry.getValue()[0] != 0);

        Set<String> hidden = config.hidden();
        Set<String> placed = new HashSet<>();
        for (Map.Entry<String, List<String>> category : config.categories().entrySet()) {
            List<String> keys = new ArrayList<>();
            for (String pkg : category.getValue()) {
                if (apps.containsKey(pkg) && !hidden.contains(pkg) && placed.add(pkg)) keys.add(pkg);
            }
            addRow(new HomeRow(category.getKey(), rowTitle(category.getKey()), keys));
        }
        String ps4 = config.ps4InputId();
        if (ps4 != null && inputLabels.containsKey(ps4)) {
            addRow(new HomeRow(ROW_INPUTS, getString(R.string.row_inputs),
                    Collections.singletonList(INPUT_PREFIX + ps4)));
        }
        shownRecents = visibleRecents(hidden);
        if (config.recentsEnabled()) {
            addRow(new HomeRow(ROW_RECENTS, getString(R.string.row_recents), shownRecents));
        }
        List<String> more = new ArrayList<>();
        for (String pkg : apps.keySet()) {
            if (!placed.contains(pkg) && !hidden.contains(pkg)) more.add(pkg);
        }
        addRow(new HomeRow(ROW_MORE, getString(R.string.row_more), more));
        emptyView.setVisibility(navRows.size() == 1 && !loading ? View.VISIBLE : View.GONE);
        restoreFocus(focusKey);
    }

    private String rowTitle(String id) {
        switch (id) {
            case HomeConfigStore.SERIES: return getString(R.string.row_series);
            case HomeConfigStore.TV: return getString(R.string.row_tv);
            case HomeConfigStore.VIDEO: return getString(R.string.row_video);
            case HomeConfigStore.MEDIA: return getString(R.string.row_media);
            default: return getString(R.string.row_more);
        }
    }

    private void addRow(HomeRow row) {
        if (row.appKeys.isEmpty()) return;
        int rowIndex = navRows.size();
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setClipChildren(false);
        section.setClipToPadding(false);

        TextView title = text(row.title, 16, Palette.IVORY_SOFT, "sans-serif-medium");
        title.setPadding(dp(EDGE_DP), 0, dp(EDGE_DP), dp(2));
        title.setAccessibilityHeading(true);

        RowView strip = new RowView(this, dp(EDGE_DP - 8));
        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        int gap = dp(TileView.RING_GAP_DP);
        tiles.setPadding(dp(EDGE_DP) - gap, dp(6), dp(EDGE_DP), dp(4));
        tiles.setClipChildren(false);
        tiles.setClipToPadding(false);

        NavRow nav = new NavRow(row.id, section, strip);
        String remembered = rememberedByRow.get(row.id);
        for (String key : row.appKeys) {
            TileView tile = createTile(row.id, key);
            if (tile == null) continue;
            LinearLayout.LayoutParams lp = wrap();
            if (!nav.items.isEmpty()) lp.setMarginStart(dp(10));
            tiles.addView(tile, lp);
            if (key.equals(remembered)) nav.remembered = nav.items.size();
            positions.put(tile, new int[]{rowIndex, nav.items.size()});
            nav.items.add(tile);
        }
        if (nav.items.isEmpty()) return;
        strip.addView(tiles, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        section.addView(title, wrap());
        section.addView(strip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionLp.bottomMargin = dp(14);
        rowsContainer.addView(section, sectionLp);
        navRows.add(nav);
    }

    private TileView createTile(String rowId, String key) {
        TileView tile;
        if (key.startsWith(INPUT_PREFIX)) {
            String inputId = key.substring(INPUT_PREFIX.length());
            String port = inputLabels.get(inputId);
            if (port == null) return null;
            tile = new TileView(this, rowId, key, TileView.KIND_INPUT, getString(R.string.ps4),
                    getString(R.string.input_with_device, getString(R.string.ps4), port),
                    null, getDrawable(R.drawable.ic_gamepad), true);
            tile.setOnClickListener(v -> openInput(inputId));
        } else {
            AppEntry app = apps.get(key);
            if (app == null) return null;
            Drawable banner = copy(banners.get(key));
            tile = new TileView(this, rowId, key, TileView.KIND_APP, app.label, app.label,
                    banner, banner == null ? copy(icons.get(key)) : null, false);
            tile.setOnClickListener(v -> launch(app));
        }
        tile.setOnLongClickListener(v -> {
            showTileMenu((TileView) v);
            return true;
        });
        tile.setOnFocusChangeListener(navFocus);
        return tile;
    }

    /** Each tile gets its own drawable state; the decoded image itself is shared. */
    private static Drawable copy(Drawable source) {
        if (source == null) return null;
        Drawable.ConstantState state = source.getConstantState();
        return state == null ? source : state.newDrawable();
    }

    // ---------------------------------------------------------------- focus

    private String currentFocusKey() {
        View focused = getCurrentFocus();
        if (focused instanceof TileView) return ((TileView) focused).focusKey();
        int[] position = focused == null ? null : positions.get(focused);
        if (position != null && position[0] == 0) return HEADER + "|" + position[1];
        return null;
    }

    private void restoreFocus(String key) {
        View target = key == null ? null : findByKey(key);
        if (target == null && key != null && !key.startsWith(HEADER + "|")) {
            target = findItem(key.substring(key.indexOf('|') + 1));
        }
        if (target == null) target = defaultFocus();
        if (target != null) target.requestFocus();
    }

    private View findByKey(String key) {
        if (key.startsWith(HEADER + "|")) {
            try {
                List<View> items = navRows.get(0).items;
                return items.get(Math.max(0, Math.min(items.size() - 1,
                        Integer.parseInt(key.substring(HEADER.length() + 1)))));
            } catch (NumberFormatException broken) {
                return null;
            }
        }
        for (int i = 1; i < navRows.size(); i++) {
            for (View item : navRows.get(i).items) {
                if (key.equals(((TileView) item).focusKey())) return item;
            }
        }
        return null;
    }

    private View findItem(String itemKey) {
        for (int i = 1; i < navRows.size(); i++) {
            if (ROW_RECENTS.equals(navRows.get(i).id)) continue;
            for (View item : navRows.get(i).items) {
                if (itemKey.equals(((TileView) item).itemKey)) return item;
            }
        }
        return null;
    }

    private View defaultFocus() {
        if (navRows.size() > 1) return navRows.get(1).items.get(0);
        List<View> header = navRows.isEmpty() ? Collections.emptyList() : navRows.get(0).items;
        return header.isEmpty() ? null : header.get(0);
    }

    private int targetScrollY(View focused) {
        int[] position = positions.get(focused);
        if (position == null || position[0] <= 1) return 0;
        return rowsContainer.getTop() + navRows.get(position[0]).section.getTop() - dp(FOCUS_LINE_DP);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !userNavigated && !(getCurrentFocus() instanceof TileView)) {
            View first = defaultFocus();
            if (first instanceof TileView) first.requestFocus();
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (down) userNavigated = true;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                View focused = getCurrentFocus();
                int[] position = focused == null ? null : positions.get(focused);
                if (position == null) {
                    if (focused != null) break;
                    if (down) {
                        View first = defaultFocus();
                        if (first != null) first.requestFocus();
                    }
                    return true;
                }
                if (down) moveFocus(position[0], position[1], event.getKeyCode());
                return true;
            }
            case KeyEvent.KEYCODE_MENU: {
                View focused = getCurrentFocus();
                if (focused instanceof TileView) {
                    if (down && event.getRepeatCount() == 0) showTileMenu((TileView) focused);
                    return true;
                }
                break;
            }
            case KeyEvent.KEYCODE_BACK:
                if (homeVariant) {
                    if (!down) goHome();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    private void moveFocus(int rowIndex, int itemIndex, int keyCode) {
        NavRow row = navRows.get(rowIndex);
        View target = null;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && itemIndex > 0) {
            target = row.items.get(itemIndex - 1);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && itemIndex < row.items.size() - 1) {
            target = row.items.get(itemIndex + 1);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && rowIndex > 0) {
            target = entryOf(navRows.get(rowIndex - 1));
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && rowIndex < navRows.size() - 1) {
            target = entryOf(navRows.get(rowIndex + 1));
        }
        if (target != null) target.requestFocus();
    }

    private static View entryOf(NavRow row) {
        return row.items.get(Math.max(0, Math.min(row.remembered, row.items.size() - 1)));
    }

    private void goHome() {
        dismissDialog();
        for (int i = 1; i < navRows.size(); i++) {
            NavRow row = navRows.get(i);
            row.remembered = 0;
            rememberedByRow.remove(row.id);
            if (row.strip != null) row.strip.scrollTo(0, 0);
        }
        View first = defaultFocus();
        if (first != null) first.requestFocus();
        scroll.scrollTo(0, 0);
    }

    // ---------------------------------------------------------------- actions

    private boolean launchAllowed() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastLaunch < LAUNCH_GUARD_MS) return false;
        lastLaunch = now;
        return true;
    }

    private void launch(AppEntry app) {
        if (!launchAllowed()) return;
        if (!AppLauncher.open(this, app, recents)) {
            lastLaunch = 0;
            toast(getString(R.string.app_unavailable, app.label));
        }
    }

    private void openInput(String inputId) {
        if (!launchAllowed()) return;
        if (!HdmiLauncher.open(this, inputId)) {
            lastLaunch = 0;
            toast(getString(R.string.input_unavailable));
        }
    }

    private void openSettings(String action) {
        if (!SettingsNavigator.open(this, action)) toast(getString(R.string.settings_unavailable));
    }

    private void openAppInfo(String pkg) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            toast(getString(R.string.settings_unavailable));
        }
    }

    private void showNetwork(NetworkStatus.Kind kind) {
        int icon;
        int label;
        switch (kind) {
            case WIFI: icon = R.drawable.ic_wifi; label = R.string.network_wifi; break;
            case ETHERNET: icon = R.drawable.ic_ethernet; label = R.string.network_ethernet; break;
            case OTHER: icon = R.drawable.ic_ethernet; label = R.string.network_other; break;
            default: icon = R.drawable.ic_offline; label = R.string.network_offline; break;
        }
        networkButton.setImageResource(icon);
        networkButton.setContentDescription(getString(label));
        if (networkButton.isFocused()) caption.setText(getString(label));
    }

    private void applyClockMode() {
        int mode = config.clockMode();
        boolean twelve = mode == HomeConfigStore.CLOCK_12
                || (mode == HomeConfigStore.CLOCK_SYSTEM && !DateFormat.is24HourFormat(this));
        String pattern = twelve ? "h:mm" : "HH:mm";
        clock.setFormat12Hour(pattern);
        clock.setFormat24Hour(pattern);
        amPm.setVisibility(twelve ? View.VISIBLE : View.GONE);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ---------------------------------------------------------------- dialogs

    private void showList(String title, List<String> labels, List<Runnable> actions) {
        dismissDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels.toArray(new CharSequence[0]), (d, which) -> actions.get(which).run())
                .create();
        dialog.show();
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = null;
    }

    private void showTileMenu(TileView tile) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (tile.kind == TileView.KIND_INPUT) {
            String inputId = tile.itemKey.substring(INPUT_PREFIX.length());
            labels.add(getString(R.string.action_open));
            actions.add(() -> openInput(inputId));
            labels.add(getString(R.string.input_change_ps4));
            actions.add(this::choosePs4Input);
            labels.add(getString(R.string.input_remove_ps4));
            actions.add(this::removePs4);
            showList(tile.title, labels, actions);
            return;
        }
        String pkg = tile.itemKey;
        AppEntry app = apps.get(pkg);
        int[] position = positions.get(tile);
        if (app == null || position == null) return;
        NavRow row = navRows.get(position[0]);
        boolean category = HomeConfigStore.CATEGORY_IDS.contains(tile.rowId);
        labels.add(getString(R.string.action_open));
        actions.add(() -> launch(app));
        if (category && position[1] > 0) {
            labels.add(getString(R.string.action_left));
            actions.add(() -> moveWithin(tile, -1));
        }
        if (category && position[1] < row.items.size() - 1) {
            labels.add(getString(R.string.action_right));
            actions.add(() -> moveWithin(tile, 1));
        }
        if (ROW_RECENTS.equals(tile.rowId)) {
            labels.add(getString(R.string.action_remove_recent));
            actions.add(() -> {
                recents.remove(pkg);
                pendingFocusKey = neighbourKey(tile);
                rebuildRows();
            });
        } else {
            labels.add(getString(R.string.action_move));
            actions.add(() -> chooseCategory(app));
            labels.add(getString(R.string.action_hide));
            actions.add(() -> hideApp(tile, app));
        }
        labels.add(getString(R.string.action_app_info));
        actions.add(() -> openAppInfo(pkg));
        showList(app.label, labels, actions);
    }

    private void moveWithin(TileView tile, int direction) {
        Set<String> hidden = config.hidden();
        if (config.moveWithin(tile.rowId, tile.itemKey, direction,
                pkg -> apps.containsKey(pkg) && !hidden.contains(pkg))) {
            pendingFocusKey = tile.focusKey();
            rebuildRows();
        }
    }

    private void chooseCategory(AppEntry app) {
        String current = config.categoryOf(app.packageName());
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (String id : HomeConfigStore.CATEGORY_IDS) {
            if (id.equals(current)) continue;
            labels.add(rowTitle(id));
            actions.add(() -> moveToCategory(app, id));
        }
        if (current != null) {
            labels.add(getString(R.string.row_more));
            actions.add(() -> moveToCategory(app, null));
        }
        showList(getString(R.string.move_title, app.label), labels, actions);
    }

    private void moveToCategory(AppEntry app, String categoryId) {
        config.moveTo(app.packageName(), categoryId);
        pendingFocusKey = (categoryId == null ? ROW_MORE : categoryId) + "|" + app.packageName();
        rebuildRows();
    }

    private void hideApp(TileView tile, AppEntry app) {
        config.setHidden(app.packageName(), true);
        pendingFocusKey = neighbourKey(tile);
        rebuildRows();
        toast(getString(R.string.hidden_toast, app.label));
    }

    private String neighbourKey(TileView tile) {
        int[] position = positions.get(tile);
        if (position == null) return null;
        List<View> items = navRows.get(position[0]).items;
        if (position[1] + 1 < items.size()) return ((TileView) items.get(position[1] + 1)).focusKey();
        if (position[1] > 0) return ((TileView) items.get(position[1] - 1)).focusKey();
        return null;
    }

    private void showSettingsMenu() {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add(getString(R.string.menu_tv_settings));
        actions.add(() -> openSettings(Settings.ACTION_SETTINGS));
        labels.add(getString(R.string.menu_wallpaper, getString(Wallpapers.title(Wallpapers.normalize(config.wallpaper())))));
        actions.add(this::chooseWallpaper);
        labels.add(getString(R.string.menu_clock, clockName(config.clockMode())));
        actions.add(this::chooseClock);
        boolean recentsOn = config.recentsEnabled();
        labels.add(getString(recentsOn ? R.string.menu_recents_hide : R.string.menu_recents_show));
        actions.add(() -> {
            config.setRecentsEnabled(!recentsOn);
            rebuildRows();
        });
        if (!recents.read().isEmpty()) {
            labels.add(getString(R.string.menu_recents_clear));
            actions.add(() -> {
                recents.clear();
                rebuildRows();
            });
        }
        Set<String> hidden = config.hidden();
        if (!hidden.isEmpty()) {
            labels.add(getString(R.string.menu_hidden, hidden.size()));
            actions.add(this::showHidden);
        }
        labels.add(getString(config.ps4InputId() == null ? R.string.input_assign_ps4 : R.string.input_change_ps4));
        actions.add(this::choosePs4Input);
        labels.add(getString(R.string.menu_reset));
        actions.add(this::confirmReset);
        showList(getString(R.string.menu_title), labels, actions);
    }

    private String clockName(int mode) {
        if (mode == HomeConfigStore.CLOCK_24) return getString(R.string.clock_24);
        if (mode == HomeConfigStore.CLOCK_12) return getString(R.string.clock_12);
        return getString(R.string.clock_system);
    }

    private void chooseClock() {
        dismissDialog();
        String[] names = {clockName(HomeConfigStore.CLOCK_SYSTEM), clockName(HomeConfigStore.CLOCK_24),
                clockName(HomeConfigStore.CLOCK_12)};
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.clock_title)
                .setSingleChoiceItems(names, config.clockMode(), (d, which) -> {
                    config.setClockMode(which);
                    applyClockMode();
                    d.dismiss();
                })
                .create();
        dialog.show();
    }

    private void chooseWallpaper() {
        dismissDialog();
        String current = Wallpapers.normalize(config.wallpaper());
        String[] names = new String[Wallpapers.IDS.length];
        int checked = 0;
        for (int i = 0; i < names.length; i++) {
            names[i] = getString(Wallpapers.title(Wallpapers.IDS[i]));
            if (Wallpapers.IDS[i].equals(current)) checked = i;
        }
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_title)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    config.setWallpaper(Wallpapers.IDS[which]);
                    paintWallpaper();
                    d.dismiss();
                })
                .create();
        dialog.show();
    }

    private void showHidden() {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        List<String> hidden = new ArrayList<>(config.hidden());
        Collections.sort(hidden);
        for (String pkg : hidden) {
            AppEntry app = apps.get(pkg);
            labels.add(app == null ? pkg : app.label);
            actions.add(() -> {
                config.setHidden(pkg, false);
                String category = config.categoryOf(pkg);
                pendingFocusKey = (category == null ? ROW_MORE : category) + "|" + pkg;
                rebuildRows();
            });
        }
        showList(getString(R.string.hidden_title), labels, actions);
    }

    private void confirmReset() {
        dismissDialog();
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.menu_reset)
                .setMessage(R.string.reset_message)
                .setPositiveButton(R.string.reset_confirm, (d, which) -> {
                    config.resetLayout();
                    rebuildRows();
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void refreshInputs() {
        Map<String, String> found = new LinkedHashMap<>();
        try {
            for (TvInputInfo input : HdmiRepository.discover(this)) {
                found.put(input.getId(), HdmiRepository.label(this, input));
            }
        } catch (RuntimeException noInputs) {
            // Keep the list empty; the dialog says so.
        }
        inputLabels = found;
    }

    private void showInputs() {
        refreshInputs();
        if (inputLabels.isEmpty()) {
            dismissDialog();
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.shortcut_inputs)
                    .setMessage(R.string.inputs_none)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            dialog.show();
            return;
        }
        String ps4 = config.ps4InputId();
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (Map.Entry<String, String> input : inputLabels.entrySet()) {
            String id = input.getKey();
            labels.add(id.equals(ps4) ? getString(R.string.input_with_device, input.getValue(), getString(R.string.ps4))
                    : input.getValue());
            actions.add(() -> openInput(id));
        }
        labels.add(getString(ps4 == null ? R.string.input_assign_ps4 : R.string.input_change_ps4));
        actions.add(this::choosePs4Input);
        if (ps4 != null) {
            labels.add(getString(R.string.input_remove_ps4));
            actions.add(this::removePs4);
        }
        showList(getString(R.string.shortcut_inputs), labels, actions);
    }

    private void choosePs4Input() {
        refreshInputs();
        if (inputLabels.isEmpty()) {
            toast(getString(R.string.inputs_none));
            return;
        }
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (Map.Entry<String, String> input : inputLabels.entrySet()) {
            String id = input.getKey();
            labels.add(input.getValue());
            actions.add(() -> {
                config.setPs4InputId(id);
                pendingFocusKey = ROW_INPUTS + "|" + INPUT_PREFIX + id;
                rebuildRows();
            });
        }
        showList(getString(R.string.input_choose_ps4), labels, actions);
    }

    private void removePs4() {
        config.setPs4InputId(null);
        rebuildRows();
    }
}
