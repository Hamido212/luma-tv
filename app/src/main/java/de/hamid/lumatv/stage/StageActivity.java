package de.hamid.lumatv.stage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.hamid.lumatv.R;
import de.hamid.lumatv.data.RecentStore;
import de.hamid.lumatv.model.AppEntry;
import de.hamid.lumatv.platform.AppCatalog;
import de.hamid.lumatv.platform.AppLauncher;
import de.hamid.lumatv.platform.HdmiLauncher;
import de.hamid.lumatv.platform.HdmiRepository;
import de.hamid.lumatv.platform.SettingsNavigator;
import de.hamid.lumatv.ui.DashboardActivity;

/**
 * Luma's Home screen since 0.3: a quiet photo Home after CleanFrame (Dock or List), with
 * Settings, All apps and a Home editor. The classic category dashboard stays reachable from
 * Settings → TV &amp; Luma.
 */
public final class StageActivity extends Activity implements StageView.Host {
    private static final long LAUNCH_GUARD_MS = 1200;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private StageView stage;
    private Look look;
    private RecentStore recents;
    private boolean homeVariant;
    private boolean catalogDirty;
    private boolean resumed;
    private long lastLaunch;
    private int catalogToken;
    private Map<String, String> inputCache = new LinkedHashMap<>();

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            long now = System.currentTimeMillis();
            main.postDelayed(this, 60000 - now % 60000 + 50);
        }
    };

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (resumed) loadCatalog(); else catalogDirty = true;
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        homeVariant = getResources().getBoolean(R.bool.is_home_variant);
        android.content.SharedPreferences prefs = getSharedPreferences("luma_home", MODE_PRIVATE);
        look = new Look(prefs);
        recents = new RecentStore(prefs);
        stage = new StageView(this, this, look);
        setContentView(stage);
        stage.requestFocus();
        if (state == null) stage.onLauncherStart();
        refreshInputs();
        loadCatalog();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packages, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packages, filter);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        main.removeCallbacks(clockTick);
        clockTick.run();
        if (catalogDirty) loadCatalog();
        stage.setHomeVisible(true);
        stage.requestFocus();
    }

    @Override protected void onPause() {
        resumed = false;
        main.removeCallbacks(clockTick);
        stage.setHomeVisible(false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        unregisterReceiver(packages);
        worker.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)) {
            stage.goHome();
        }
    }

    // ------------------------------------------------------------------ data

    private void updateClock() {
        int mode = look.clockMode();
        boolean h24 = mode == 1 || (mode == 0 && DateFormat.is24HourFormat(this));
        // CleanFrame shows hours and minutes only, without AM/PM or seconds.
        stage.setClock(DateFormat.format(h24 ? "H:mm" : "h:mm", Calendar.getInstance()).toString());
    }

    private void refreshInputs() {
        Map<String, String> labels = new LinkedHashMap<>();
        try {
            for (TvInputInfo input : HdmiRepository.discover(this)) labels.put(input.getId(), HdmiRepository.label(this, input));
        } catch (RuntimeException unavailable) {
            // No TV input service: the inputs page says so.
        }
        inputCache = labels;
    }

    private void loadCatalog() {
        catalogDirty = false;
        final int token = ++catalogToken;
        final PackageManager pm = getPackageManager();
        final String own = getPackageName();
        final String ps4 = look.ps4InputId();
        final String ps4Label = getString(R.string.st_ps4_item);
        worker.execute(() -> {
            List<HomeItem> items = new ArrayList<>();
            for (AppEntry entry : AppCatalog.discover(pm, own)) {
                String pkg = entry.packageName();
                String mark = Marks.forPackage(pkg);
                Drawable icon = null;
                if (mark == null) {
                    try {
                        icon = pm.getActivityIcon(entry.component);
                    } catch (PackageManager.NameNotFoundException | RuntimeException gone) {
                        icon = null;
                    }
                }
                items.add(new HomeItem(pkg, Marks.label(pkg, entry.label), entry, null, mark, icon));
            }
            if (ps4 != null) {
                items.add(new HomeItem(Look.INPUT_PREFIX + ps4, ps4Label, null, ps4, "playstation", null));
            }
            items.sort(Comparator.comparing((HomeItem h) -> h.label, String.CASE_INSENSITIVE_ORDER));
            main.post(() -> {
                if (token != catalogToken || isDestroyed()) return;
                stage.setCatalog(items);
            });
        });
    }

    // ------------------------------------------------------------------ Host

    @Override public void launch(HomeItem item) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastLaunch < LAUNCH_GUARD_MS) return;
        lastLaunch = now;
        boolean ok = item.isInput() ? HdmiLauncher.open(this, item.inputId) : AppLauncher.open(this, item.app, recents);
        if (!ok) {
            lastLaunch = 0;
            toast(item.isInput() ? getString(R.string.input_unavailable) : getString(R.string.app_unavailable, item.label));
        }
    }

    @Override public void openTvSettings() {
        if (!SettingsNavigator.open(this, Settings.ACTION_SETTINGS)) toast(getString(R.string.settings_unavailable));
    }

    @Override public void openInput(String inputId) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastLaunch < LAUNCH_GUARD_MS) return;
        lastLaunch = now;
        if (!HdmiLauncher.open(this, inputId)) {
            lastLaunch = 0;
            toast(getString(R.string.input_unavailable));
        }
    }

    @Override public void openClassic() {
        startActivity(new Intent(this, DashboardActivity.class));
    }

    @Override public Map<String, String> inputs() {
        refreshInputs();
        return inputCache;
    }

    @Override public void work(Runnable task) {
        if (!worker.isShutdown()) worker.execute(task);
    }

    @Override public boolean homeVariant() { return homeVariant; }

    @Override public void leave() { finish(); }

    @Override public void catalogChanged() {
        updateClock();
        loadCatalog();
    }

    @Override public void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
