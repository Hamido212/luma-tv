package de.hamid.lumatv.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stable row IDs survive renaming and reordering. Assignment is explicitly user-owned. */
public final class HomeRow {
    public final String id;
    public final String title;
    public final List<String> appKeys;

    public HomeRow(String id, String title, List<String> appKeys) {
        this.id = id;
        this.title = title;
        this.appKeys = Collections.unmodifiableList(new ArrayList<>(appKeys));
    }
}
