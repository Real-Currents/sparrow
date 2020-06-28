package com.sparrowwallet.sparrow.glyphfont;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.controlsfx.glyphfont.INamedCharacter;

import java.io.InputStream;
import java.util.Arrays;

public class FontAwesome5 extends GlyphFont {
    public static String FONT_NAME = "Font Awesome 5 Free Solid";

    /**
     * The individual glyphs offered by the FontAwesome5 font.
     */
    public static enum Glyph implements INamedCharacter {
        CHECK_CIRCLE('\uf058'),
        CIRCLE('\uf111'),
        COINS('\uf51e'),
        EXCLAMATION_CIRCLE('\uf06a'),
        ELLIPSIS_H('\uf141'),
        EYE('\uf06e'),
        KEY('\uf084'),
        LAPTOP('\uf109'),
        LOCK('\uf023'),
        LOCK_OPEN('\uf3c1'),
        QUESTION_CIRCLE('\uf059'),
        SD_CARD('\uf7c2'),
        SEARCH('\uf002'),
        WALLET('\uf555');

        private final char ch;

        /**
         * Creates a named Glyph mapped to the given character
         * @param ch
         */
        Glyph( char ch ) {
            this.ch = ch;
        }

        @Override
        public char getChar() {
            return ch;
        }
    }

    /**
     * Do not call this constructor directly - instead access the
     * {@link FontAwesome5.Glyph} public static enumeration method to create the glyph nodes), or
     * use the {@link GlyphFontRegistry} class to get access.
     *
     * Note: Do not remove this public constructor since it is used by the service loader!
     */
    public FontAwesome5() {
        this(FontAwesome5.class.getResourceAsStream("/font/fa-solid-900.ttf"));
    }

    /**
     * Creates a new FontAwesome instance which uses the provided font source.
     * @param is
     */
    public FontAwesome5(InputStream is){
        super(FONT_NAME, 14, is, true);
        registerAll(Arrays.asList(FontAwesome.Glyph.values()));
        registerAll(Arrays.asList(FontAwesome5.Glyph.values()));
    }
}