package org.graphstream.ui.javafx.util;


import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * a set of common icon management and lookup api
 * <p>
 * User: bowen
 * Date: 1/5/15
 */
public class IconManager
{
    private static final Logger logger = LoggerFactory.getLogger(IconManager.class);

    private static final IconManager instance = new IconManager();

    private IconCache cache = new MemoryIconCache();

    private final Map<String, IconEntry> icons = new TreeMap<>();


    public static IconManager getInstance()
    {
        return IconManager.instance;
    }


    private IconManager()
    {

    }


    public IconCache getCache()
    {
        return this.cache;
    }


    public void setCache(final IconCache cache)
    {
        if (null == cache)
        {
            throw new IllegalArgumentException("Cache cannot be null.");
        }
        this.cache = cache;
    }


    public boolean add(final String name, final URL iconPath)
    {
        if (null == name)
        {
            return false;
        }

        IconEntry entry = this.icons.get(name);
        if (null == entry)
        {
            entry = new IconEntry();
            this.icons.put(name, entry);
        }
        return entry.add(iconPath);
    }


    public boolean remove(final String name)
    {
        if (null == name)
        {
            return false;
        }
        return this.icons.remove(name) != null;
    }


    public void clear()
    {
        this.cache.clear();
        this.icons.clear();
    }


    public Image get(final String name)
    {
        return this.get(name, -1, -1);
    }


    public Image get(final String name, final int width, final int height)
    {
        if (null == name)
        {
            return null;
        }

        final String scaledId;
        if (width > 0 && height > 0)
        {
            scaledId = name + "#" + width + "x" + height;
        }
        else
        {
            scaledId = name;
        }
        final Image cached = this.cache.get(scaledId);
        if (cached != null)
        {
            return cached;
        }

        final IconEntry entry = this.icons.get(name);
        if (null == entry)
        {
            return null;
        }

        final Image scaled = entry.build(width, height);
        if (null == scaled)
        {
            return null;
        }
        this.cache.put(scaledId, scaled);
        return scaled;
    }


    private static class IconEntry
    {
        private final Set<ImageContext> icons = new TreeSet<>();


        public boolean add(final URL iconPath)
        {
            if (null == iconPath)
            {
                return false;
            }

            try (final InputStream input = iconPath.openStream())
            {
                final Image image = new Image(input);
                final int w = (int) Math.round(image.getWidth());
                final int h = (int) Math.round(image.getWidth());
                final ImageContext context = new ImageContext(iconPath, w, h);
                return this.icons.add(context);
            }
            catch (final Exception e)
            {
                logger.warn("Unable to load icon [" + iconPath + "].", e);
                return false;
            }
        }


        public Image build(final int width, final int height)
        {
            if (this.icons.isEmpty())
            {
                return null;
            }

            ImageContext best = this.icons.iterator().next();
            if (width <= 0 || height <= 0)
            {
                return best.readImage();
            }

            for (final ImageContext ctx : this.icons)
            {
                if (ctx.getWidth() == width || ctx.getHeight() == height)
                {
                    // same size
                    return ctx.readImage();
                }
                if (ctx.getWidth() < width || ctx.getHeight() < height)
                {
                    // image is too small to be rescaled
                    break;
                }
                else
                {
                    best = ctx;
                }
            }

            if (null == best)
            {
                return null;
            }

            return best.scaleImage(width, height);
        }
    }

    private static class ImageContext implements Comparable<ImageContext>
    {
        private final URL url;

        private final int width;

        private final int height;


        private ImageContext(final URL url, final int width, final int height)
        {
            this.url = url;
            this.width = width;
            this.height = height;
        }


        public URL getPath()
        {
            return this.url;
        }


        public int getWidth()
        {
            return this.width;
        }


        public int getHeight()
        {
            return this.height;
        }


        public Image readImage()
        {
            try (final InputStream input = this.url.openStream())
            {
                return new Image(input);
            }
            catch (final Exception e)
            {
                logger.warn("Unable to read icon [" + this.url + "].", e);
                return null;
            }
        }


        public Image scaleImage(final double w, final double h)
        {
            try (final InputStream input = this.url.openStream())
            {
                return new Image(input, w, h, true, true);
            }
            catch (final Exception e)
            {
                logger.warn("Unable to read icon [" + this.url + "].", e);
                return null;
            }
        }


        @Override
        public int compareTo(final ImageContext right)
        {
            // sort largest to smallest
            final int widthCompare = -Double.compare(this.getWidth(), right.getWidth());
            if (widthCompare != 0)
            {
                return widthCompare;
            }
            return -Double.compare(this.getHeight(), right.getHeight());
        }
    }
}
