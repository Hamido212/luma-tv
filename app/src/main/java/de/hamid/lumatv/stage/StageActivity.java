package de.hamid.lumatv.stage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
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
import de.hamid.lumatv.data.SettingsBackup;
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
    static final String PREFS = "luma_home";
    private static final long LAUNCH_GUARD_MS = 1200;
    /** Package broadcasts arrive in bursts (an update sends three); reload once after the burst. */
    private static final long CATALOG_DEBOUNCE_MS = 500;
    /** After this long in another app, Home opens on Home again instead of an open panel. */
    private static final long BACK_TO_HOME_MS = 10 * 60 * 1000;

    /** App list and icons. Kept apart from the image worker so Home gets its apps first. */
    private final ExecutorService catalogWorker = Executors.newSingleThreadExecutor();
    /** Photos, glass fields and thumbnails. */
    private final ExecutorService imageWorker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private StageView stage;
    private Look look;
    private RecentStore recents;
    private boolean homeVariant;
    private boolean catalogDirty;
    private boolean resumed;
    private boolean started;
    private long stoppedAt = -1;
    private long lastLaunch;
    private int catalogToken;
    private Map<String, String> inputCache = new LinkedHashMap<>();
    private AlertDialog nameDialog;
    /** PS4 input as last seen here; the classic view can change it behind Home's back. */
    private String ps4Seen;

    private final Runnable reloadCatalog = this::loadCatalog;

    /** Minute ticks, manual time changes, time zone and 12/24 h changes. */
    private final BroadcastReceiver time = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateClock(); }
    };

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // The removal half of an update is followed by ADDED/REPLACED; react to those.
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            catalogChanged(false);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        homeVariant = getResources().getBoolean(R.bool.is_home_variant);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean safe = SafeStart.enter(prefs, bootCount(), SystemClock.elapsedRealtime());
        look = new Look(prefs, safe);
        look.migrate();
        ps4Seen = look.ps4InputId();
        recents = new RecentStore(prefs);
        stage = new StageView(this, this, look);
        setContentView(stage);
        stage.requestFocus();
        refreshInputs();
        loadCatalog();
        stage.begin(state == null);
        if (safe) toast(getString(R.string.safe_mode));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        register(packages, filter);
        // Apps on USB storage arrive after boot; suspended apps are not launchable.
        IntentFilter bulk = new IntentFilter();
        bulk.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        bulk.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        bulk.addAction(Intent.ACTION_PACKAGES_SUSPENDED);
        bulk.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
        register(packages, bulk);
    }

    private int bootCount() {
        try {
            return Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (RuntimeException unavailable) {
            return -1;
        }
    }

    private void register(BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        if (started) {
            // Back from an app or from standby.
            boolean longAway = stoppedAt >= 0 && SystemClock.elapsedRealtime() - stoppedAt > BACK_TO_HOME_MS;
            if (longAway) stage.goHome();
            stage.onLauncherStart();
        }
        started = true;
        stoppedAt = -1;
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        updateClock();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        register(time, filter);
        String ps4 = look.ps4InputId();
        if (ps4 == null ? ps4Seen != null : !ps4.equals(ps4Seen)) {
            look.movePs4Key(ps4Seen, ps4);
            ps4Seen = ps4;
            catalogDirty = true;
        }
        if (catalogDirty) loadCatalog();
        stage.setHomeVisible(true);
        stage.requestFocus();
    }

    @Override protected void onPause() {
        resumed = false;
        unregisterReceiver(time);
        stage.setHomeVisible(false);
        super.onPause();
    }

    @Override protected void onStop() {
        stoppedAt = SystemClock.elapsedRealtime();
        super.onStop();
    }

    @Override protected void onDestroy() {
        unregisterReceiver(packages);
        if (nameDialog != null) nameDialog.dismiss();
        catalogWorker.shutdownNow();
        imageWorker.shutdownNow();
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)) {
            if (nameDialog != null) nameDialog.dismiss();
            stage.goHome();
        }
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) stage.trimMemory();
    }

    // ------------------------------------------------------------------ data

    private void updateClock() {
        int mode = look.clockMode();
        boolean h24 = mode == 1 || (mode == 0 && DateFormat.is24HourFormat(this));
        // CleanFrame shows hours and minutes only, without AM/PM or seconds.
        Calendar now = Calendar.getInstance();
        stage.setClock(DateFormat.format(h24 ? "H:mm" : "h:mm", now).toString());
        stage.setGreeting(greetingText(now.get(Calendar.HOUR_OF_DAY)));
    }

    /** "Good evening, Name" by time of day; empty when the greeting is switched off. */
    private String greetingText(int hour) {
        if (!look.greetingOn()) return "";
        return Greeting.text(true, look.userName(), getString(Greeting.forHour(hour)), getString(R.string.greeting_with_name));
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

    private void catalogChanged(boolean now) {
        main.removeCallbacks(reloadCatalog);
        if (!resumed) { catalogDirty = true; return; }
        if (now) loadCatalog(); else main.postDelayed(reloadCatalog, CATALOG_DEBOUNCE_MS);
    }

    private void loadCatalog() {
        catalogDirty = false;
        final int token = ++catalogToken;
        final PackageManager pm = getPackageManager();
        final String own = getPackageName();
        final String ps4 = look.ps4InputId();
        final String ps4Label = getString(R.string.st_ps4_item);
        final String tvSettingsLabel = getString(R.string.tv_settings_app);
        if (catalogWorker.isShutdown()) return;
        catalogWorker.execute(() -> {
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
                String label = Marks.isTvSettings(pkg) ? tvSettingsLabel : Marks.label(pkg, entry.label);
                items.add(new HomeItem(pkg, label, entry, null, mark, icon));
            }
            if (ps4 != null) {
                items.add(new HomeItem(Look.INPUT_PREFIX + ps4, ps4Label, null, ps4, "playstation", null));
            }
            items.sort(Comparator.comparing((HomeItem h) -> h.label, String.CASE_INSENSITIVE_ORDER));
            HomeItem.disambiguate(items);
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
        if (!imageWorker.isShutdown()) imageWorker.execute(task);
    }

    @Override public boolean homeVariant() { return homeVariant; }

    @Override public void leave() { finish(); }

    @Override public void catalogChanged() {
        updateClock();
        catalogChanged(true);
    }

    @Override public void firstFrameDrawn() {
        SafeStart.ok(prefs);
    }

    @Override public void clockChanged() {
        updateClock();
    }

    @Override public void ps4Changed(String inputId) {
        ps4Seen = inputId;
    }

    @Override public void editName() {
        if (nameDialog != null && nameDialog.isShowing()) return;
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(24)});
        input.setText(look.userName());
        input.setSelection(input.getText().length());
        input.setHint(R.string.st_name_hint);
        // "Done" on the TV keyboard saves, like OK in the dialog.
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE || nameDialog == null) return false;
            nameDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
        nameDialog = new AlertDialog.Builder(this, R.style.LumaDialog)
                .setTitle(R.string.st_name_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (look.setUserName(name)) {
                        updateClock();
                        stage.nameChanged();
                    } else {
                        toast(getString(R.string.st_save_failed));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(dialog -> {
                    nameDialog = null;
                    stage.requestFocus();
                })
                .show();
    }

    // ------------------------------------------------------------------ backup

    private File backupFile() {
        File dir = getExternalFilesDir(null);
        return dir == null ? null : new File(dir, SettingsBackup.FILE_NAME);
    }

    @Override public String backupInfo() {
        File file = backupFile();
        if (file == null || !file.isFile()) return null;
        return DateFormat.getMediumDateFormat(this).format(file.lastModified()) + " "
                + DateFormat.getTimeFormat(this).format(file.lastModified());
    }

    @Override public void backupSettings() {
        File file = backupFile();
        if (file == null || !SettingsBackup.write(prefs, file, versionName())) {
            toast(getString(R.string.st_backup_failed));
            return;
        }
        toast(getString(R.string.st_backup_done));
        stage.nameChanged();
    }

    @Override public void restoreSettings() {
        File file = backupFile();
        int restored = file == null ? -1 : SettingsBackup.read(prefs, file);
        if (restored < 0) {
            toast(getString(R.string.st_restore_failed));
            return;
        }
        toast(getString(R.string.st_restore_done));
        // Every setting is read when the screen starts; start it afresh with the restored values.
        recreate();
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException impossible) {
            return "";
        }
    }

    @Override public void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
