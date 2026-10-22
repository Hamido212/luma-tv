package de.hamid.lumatv.platform;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import java.util.ArrayList;
import java.util.List;

/** Adapted from TV Detox openInput, including its inputplayer and Live TV fallbacks. */
public final class HdmiLauncher {
    private HdmiLauncher() {}

    public static boolean open(Context context, String inputId) {
        Intent view = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId));
        PackageManager pm = context.getPackageManager();
        ResolveInfo handler = pm.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY);
        List<Intent> candidates = new ArrayList<>();
        if (handler != null && handler.activityInfo != null
                && "com.google.android.tv.inputplayer".equals(handler.activityInfo.packageName)) {
            candidates.add(new Intent(view).setClassName(handler.activityInfo.packageName, handler.activityInfo.name));
        }
        String[][] players = {{"com.android.tv", "com.android.tv.MainActivity"},
                {"com.droidlogic.android.tv", "com.android.tv.TvActivity"}};
        for (String[] player : players) {
            Intent explicit = new Intent(view).setClassName(player[0], player[1]);
            if (explicit.resolveActivity(pm) != null) candidates.add(explicit);
        }
        candidates.add(view);
        for (Intent candidate : candidates) {
            try {
                context.startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                // Try the next supported player; never alter a system component.
            }
        }
        return false;
    }
}
