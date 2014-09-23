package org.graphstream.ui.javafx.renderer;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.graphstream.ui.javafx.util.Geometries;

import java.awt.geom.Line2D;
import java.util.Arrays;


/**
 * common element context operations
 * <p>
 * User: bowen
 * Date: 9/20/14
 */
abstract public class AstractContext implements ElementContext
{
    @Override
    public boolean contains(Point2D pos)
    {
        if (null == pos)
        {
            return false;
        }
        final Rectangle2D bounds = this.getBounds();
        if (null == bounds)
        {
            return false;
        }
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0)
        {
            return false;
        }
        return bounds.contains(pos.getX(), pos.getY());
    }


    @Override
    public Point2D intersects(final double x0, final double y0, final double x1, final double y1)
    {
        final Rectangle2D bounds = this.getBounds();
        if (null == bounds)
        {
            return null;
        }

        final Line2D line = new Line2D.Double(x0, y0, x1, y1);
        final Line2D left = new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds.getMinX(), bounds.getMaxY());
        final Line2D right = new Line2D.Double(bounds.getMaxX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
        final Line2D top = new Line2D.Double(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMinY());
        final Line2D bottom = new Line2D.Double(bounds.getMinX(), bounds.getMaxY(), bounds.getMaxX(), bounds.getMaxY());

        double minDistance = Double.MAX_VALUE;
        Point2D minPoint = null;
        for (final Line2D test : Arrays.asList(left, right, top, bottom))
        {
            final Point2D p = Geometries.intersection(line, test);
            if (p != null)
            {
                final double d = Math.pow(p.getX() - x0, 2) + Math.pow(p.getY() - y0, 2);
                if (d <= minDistance || null == minPoint)
                {
                    minDistance = d;
                    minPoint = p;
                }
            }
        }
        return minPoint;
    }
}
