package de.hamid.lumatv.platform;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Adapted from TV Detox hdmiInputs/inputLabel. No arbitrary four-input limit. */
public final class HdmiRepository {
    private HdmiRepository() {}

    public static List<TvInputInfo> discover(Context context) {
        TvInputManager manager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (manager == null) return Collections.emptyList();
        List<TvInputInfo> result = new ArrayList<>();
        for (TvInputInfo input : manager.getTvInputList()) {
            if (input.getType() == TvInputInfo.TYPE_HDMI && input.getParentId() == null
                    && !input.isHidden(context)) result.add(input);
        }
        result.sort(Comparator.comparing(TvInputInfo::getId));
        return result;
    }

    public static String label(Context context, TvInputInfo input) {
        CharSequence label = input.loadCustomLabel(context);
        if (label == null || label.length() == 0) label = input.loadLabel(context);
        return label == null || label.length() == 0 ? "HDMI" : label.toString();
    }
}
