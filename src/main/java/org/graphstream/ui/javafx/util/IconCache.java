package org.graphstream.ui.javafx.util;

import javafx.scene.image.Image;

/**
 * an icon cache used to render node/edge icons
 * <p>
 * User: trajar
 * Date: 1/5/15
 */
public interface IconCache
{
    Image get(String id);
    void put(String id, Image icon);
    void clear();
}
