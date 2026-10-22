package de.hamid.lumatv.platform;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import de.hamid.lumatv.model.AppEntry;

/** Adapted from TV Detox appList/showAddMenu. No complete system-package inventory. */
public final class AppCatalog {
    /** Both Luma variants share this prefix and never show themselves as a tile. */
    private static final String OWN_PREFIX = "de.hamid.lumatv";

    private AppCatalog() {}

    /**
     * Run on a worker thread. Launchable activities only; never implies safe-to-disable.
     * Returns one entry per package, preferring the TV (leanback) activity, sorted by label.
     */
    public static List<AppEntry> discover(PackageManager pm, String ownPackage) {
        Map<String, AppEntry> found = new LinkedHashMap<>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                if (ri.activityInfo == null || !ri.activityInfo.exported || !ri.activityInfo.enabled
                        || !ri.activityInfo.applicationInfo.enabled) continue;
                String pkg = ri.activityInfo.packageName;
                if (ownPackage.equals(pkg) || pkg.startsWith(OWN_PREFIX)) continue;
                if (found.containsKey(pkg)) continue;
                ComponentName component = new ComponentName(pkg, ri.activityInfo.name);
                CharSequence label = ri.loadLabel(pm);
                found.put(pkg, new AppEntry(component,
                        label == null ? pkg : label.toString().trim(),
                        Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)));
            }
        }
        List<AppEntry> result = new ArrayList<>(found.values());
        result.sort(Comparator.comparing((AppEntry e) -> e.label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AppEntry::key));
        return result;
    }
}
