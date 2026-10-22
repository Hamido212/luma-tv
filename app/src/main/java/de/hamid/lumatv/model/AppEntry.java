package de.hamid.lumatv.model;

import android.content.ComponentName;

/**
 * One launchable app as shown on the dashboard. The dashboard shows one tile per package;
 * the component remembers which activity is started (TV activity preferred).
 */
public final class AppEntry {
    public final ComponentName component;
    public final String label;
    public final boolean tvOptimized;

    public AppEntry(ComponentName component, String label, boolean tvOptimized) {
        this.component = component;
        this.label = label;
        this.tvOptimized = tvOptimized;
    }

    public String packageName() { return component.getPackageName(); }

    public String key() { return component.flattenToString(); }
}
