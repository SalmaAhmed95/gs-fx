package org.graphstream.ui.javafx.renderer;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.graphstream.ui.graphicGraph.GraphicElement;

/**
 * a element context bounded by square
 * <p>
 * User: bowen
 * Date: 9/20/14
 */
public class SquareContext extends AstractContext
{
    private final GraphicElement element;

    private final Rectangle2D bounds;

    private final Point2D pos;


    public SquareContext(final GraphicElement element, final Point2D pos, final Rectangle2D bounds)
    {
        this.element = element;
        this.bounds = bounds;
        this.pos = pos;
    }


    @Override
    public String getId()
    {
        return this.element.getId();
    }


    @Override
    public Rectangle2D getBounds()
    {
        return this.bounds;
    }


    @Override
    public Point2D getPosition()
    {
        return this.pos;
    }
}
