package de.hamid.lumatv.ui;

/**
 * Colour tokens. The wallpaper carries the colour; the interface stays in ink and ivory
 * so it fits every scene.
 */
final class Palette {
    /** Warm paper white for text and the focus ring. */
    static final int IVORY = 0xFFEDE6D3;
    static final int IVORY_SOFT = 0xD9EDE6D3;
    static final int IVORY_DIM = 0xB3EDE6D3;
    /** Muted lavender grey for secondary text. */
    static final int MIST = 0xFFA9A6BC;
    /** Ink used on ivory (focused glyph buttons). */
    static final int INK = 0xFF16172A;
    /** Translucent ink for tiles without a TV banner. */
    static final int INK_CARD = 0xE01B1C30;
    static final int INK_CARD_END = 0xE0121322;

    private Palette() {}
}
