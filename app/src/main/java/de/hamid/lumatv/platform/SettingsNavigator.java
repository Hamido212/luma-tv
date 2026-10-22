package de.hamid.lumatv.platform;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** Public settings activities first; vendor service adapters need a TCL hardware test. */
public final class SettingsNavigator {
    private SettingsNavigator() {}

    public static boolean open(Context context, String action) {
        String[] candidates = {action, Settings.ACTION_SETTINGS};
        for (String candidate : candidates) {
            if (candidate == null) continue;
            try {
                context.startActivity(new Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                // The general settings page is the fallback.
            }
        }
        return false;
    }
}
