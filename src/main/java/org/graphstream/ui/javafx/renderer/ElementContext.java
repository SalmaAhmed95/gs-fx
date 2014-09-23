package org.graphstream.ui.javafx.renderer;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * a context containing render size/position for a given element
 *
 * User: bowen
 * Date: 9/17/14
 */
public interface ElementContext
{
    String getId();
    Rectangle2D getBounds();
    Point2D getPosition();
    boolean contains(Point2D pos);
    Point2D intersects(double x0, double y0, double x1, double y1);
}
