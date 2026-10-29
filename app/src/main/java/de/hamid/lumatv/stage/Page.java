package de.hamid.lumatv.stage;

import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;

/**
 * A settings page as data: sections of choices, drawn by {@link SheetPainter}. Built fresh from
 * the committed look after every change, so a tick always shows what is saved.
 */
final class Page {
    static final int SW_NONE = 0, SW_THUMB = 1, SW_LIGHT = 2, SW_DARK = 3;

    static final class Opt {
        final String label;
        String description;
        boolean checked;
        boolean enabled = true;
        boolean forward;
        int swatch = SW_NONE;
        /** Gallery thumbnail key when swatch == SW_THUMB. */
        String thumb;
        /** Face for the label (font page); null uses Manrope. */
        Typeface face;
        /** Indent text to align with labels that have a swatch. */
        boolean alignWithSwatch;
        Runnable action;

        Opt(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    static final class Section {
        final String label;
        final boolean horizontal;
        final List<Opt> opts = new ArrayList<>();
        /** Shown under the section when its choices are inactive. */
        String reason;

        Section(String label, boolean horizontal) {
            this.label = label;
            this.horizontal = horizontal;
        }

        Opt add(Opt opt) {
            opts.add(opt);
            return opt;
        }
    }

    final int id;
    final String back;
    final String title;
    final List<Section> sections = new ArrayList<>();
    /** Whether the right half shows the Home preview. */
    final boolean preview;
    float top = 244;
    float sectionGap = 32;
    float rowHeight = 78;
    /** Height of a choice that carries a description line. */
    float descHeight = 120;
    float width = 640;
    String hint;
    String footnote;
    /** Draws the footnote under the choices instead of in the right column. */
    boolean footnoteBelow;

    Page(int id, String back, String title, boolean preview) {
        this.id = id;
        this.back = back;
        this.title = title;
        this.preview = preview;
    }

    Section section(String label, boolean horizontal) {
        Section s = new Section(label, horizontal);
        sections.add(s);
        return s;
    }

    /** Focus rows: each vertical choice is a row; a horizontal section is one row. */
    static final class Row {
        final Section section;
        final int index;

        Row(Section section, int index) {
            this.section = section;
            this.index = index;
        }
    }

    List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (Section s : sections) {
            if (s.horizontal) rows.add(new Row(s, -1));
            else for (int i = 0; i < s.opts.size(); i++) rows.add(new Row(s, i));
        }
        return rows;
    }
}
