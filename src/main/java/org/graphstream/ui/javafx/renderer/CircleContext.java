package org.graphstream.ui.javafx.renderer;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.graphstream.ui.graphicGraph.GraphicElement;


/**
 * node rendering context
 * <p>
 * User: bowen
 * Date: 9/17/14
 */
public class CircleContext extends AstractContext
{
    private final GraphicElement element;

    private final Point2D pos;

    private final double radiusx;

    private final double radiusy;


    public CircleContext(GraphicElement element, Point2D pos, double radiusx, double radiusy)
    {
        this.element = element;
        this.pos = pos;
        this.radiusx = radiusx;
        this.radiusy = radiusy;
    }


    @Override
    public String getId()
    {
        return this.element.getId();
    }


    @Override
    public Rectangle2D getBounds()
    {
        double x = pos.getX() - this.radiusx;
        double y = pos.getY() - this.radiusy;
        return new Rectangle2D(x, y, this.radiusx * 2d, this.radiusy * 2d);
    }


    @Override
    public Point2D getPosition()
    {
        return this.pos;
    }
}
