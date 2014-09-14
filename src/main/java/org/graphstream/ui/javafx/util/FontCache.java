/*
 * Copyright 2006 - 2013
 *     Stefan Balev     <stefan.balev@graphstream-project.org>
 *     Julien Baudry    <julien.baudry@graphstream-project.org>
 *     Antoine Dutot    <antoine.dutot@graphstream-project.org>
 *     Yoann Pigné      <yoann.pigne@graphstream-project.org>
 *     Guilhelm Savin   <guilhelm.savin@graphstream-project.org>
 * 
 * This file is part of GraphStream <http://graphstream-project.org>.
 * 
 * GraphStream is a library whose purpose is to handle static or dynamic
 * graph, create them from scratch, file or any source and display them.
 * 
 * This program is free software distributed under the terms of two licenses, the
 * CeCILL-C license that fits European law, and the GNU Lesser General Public
 * License. You can  use, modify and/ or redistribute the software under the terms
 * of the CeCILL-C license as circulated by CEA, CNRS and INRIA at the following
 * URL <http://www.cecill.info> or under the terms of the GNU LGPL as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C and LGPL licenses and that you accept their terms.
 */
package org.graphstream.ui.javafx.util;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;

import java.util.Map;
import java.util.TreeMap;

/**
 * A cache for fonts.
 * <p>
 * <p>
 * This cache allows to avoid reloading fonts and allows to quickly lookup a
 * font based on its name, its style (bold, italic) and its size.
 * </p>
 */
public class FontCache
{
    protected Font defaultFont;

    protected final Map<String, FontSlot> cache = new TreeMap<>();

    private static FontCache defaultFontCache = new FontCache();


    public FontCache()
    {
        defaultFont = new Font("SansSerif", 11);
    }


    public Font getDefaultFont()
    {
        return defaultFont;
    }


    public void setDefaultFont(final Font f)
    {
        if (null == f)
        {
            throw new IllegalArgumentException("Font cannot be null.");
        }
        this.defaultFont = f;
    }


    public static FontCache defaultFontCache()
    {
        if (defaultFontCache == null)
        {
            defaultFontCache = new FontCache();
        }
        return defaultFontCache;
    }


    public Font getDefaultFont(StyleConstants.TextStyle style, int size)
    {
        return getFont(this.defaultFont.getName(), style, size);
    }


    public Font getFont(String name, StyleConstants.TextStyle style, int size)
    {
        FontSlot slot = cache.get(name);

        if (slot == null)
        {
            slot = new FontSlot(name, style, size);
            cache.put(name, slot);
        }

        return slot.get(style, size);
    }


    private static class FontSlot
    {
        private final String name;

        public final Map<Integer, Font> normal = new TreeMap<>();

        public final Map<Integer, Font> bold = new TreeMap<>();

        public final Map<Integer, Font> italic = new TreeMap<>();

        public final Map<Integer, Font> boldItalic = new TreeMap<>();


        public FontSlot(String name, StyleConstants.TextStyle style, int size)
        {
            this.name = name;
            get(style, size);
        }


        public Font get(StyleConstants.TextStyle style, int size)
        {
            final Map<Integer, Font> map;
            switch (style)
            {
                case BOLD:
                    map = bold;
                    break;
                case ITALIC:
                    map = italic;
                    break;
                case BOLD_ITALIC:
                    map = boldItalic;
                    break;
                case NORMAL:
                default:
                    map = normal;
                    break;
            }

            final Font cached = map.get(size);
            if (cached != null)
            {
                return cached;
            }

            final Font font;
            switch (style)
            {
                case BOLD:
                    font = Font.font(this.name, FontWeight.BOLD, FontPosture.REGULAR, size);
                    break;
                case ITALIC:
                    font = Font.font(this.name, FontWeight.NORMAL, FontPosture.ITALIC, size);
                    break;
                case BOLD_ITALIC:
                    font = Font.font(this.name, FontWeight.BOLD, FontPosture.ITALIC, size);
                    break;
                case NORMAL:
                default:
                    font = Font.font(this.name, size);
                    break;
            }
            map.put(size, font);
            return font;
        }
    }
}