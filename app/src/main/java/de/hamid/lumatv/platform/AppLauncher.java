package de.hamid.lumatv.platform;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import de.hamid.lumatv.data.RecentStore;
import de.hamid.lumatv.model.AppEntry;

public final class AppLauncher {
    private AppLauncher() {}

    /** Starts the app and records it as recently used only after a successful start. */
    public static boolean open(Context context, AppEntry app, RecentStore recents) {
        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(app.component)
                .addCategory(app.tvOptimized ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            context.startActivity(intent);
            recents.record(app.packageName());
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            return false;
        }
    }
}
