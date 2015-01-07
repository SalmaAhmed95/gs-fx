package org.graphstream.ui.javafx.util;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * an icon cache backed by map
 * <p>
 * User: bowen
 * Date: 1/5/15
 */
public class MemoryIconCache implements IconCache
{
    private final Map<String, Image> map = new HashMap<>();


    @Override
    public Image get(final String id)
    {
        if (null == id)
        {
            return null;
        }
        return this.map.get(id);
    }


    @Override
    public void put(final String id, final Image icon)
    {
        if (null == id || null == icon)
        {
            return;
        }
        this.map.put(id, icon);
    }


    @Override
    public void clear()
    {
        this.map.clear();
    }
}
