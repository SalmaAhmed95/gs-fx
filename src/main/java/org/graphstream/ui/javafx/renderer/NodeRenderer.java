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
package org.graphstream.ui.javafx.renderer;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.transform.Affine;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;


public class NodeRenderer extends ElementRenderer
{
    @Override
    protected ElementContext computeElement(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicElement element)
    {
        final GraphicNode node = (GraphicNode) element;
        final Point2D pos = camera.graphToScreen(new Point2D(node.x, node.y));
        if (null == pos)
        {
            return null;
        }

        final double halfWidth = this.getWidth() / 2d;
        final double halfHeight = this.getHeight() / 2d;

        switch (group.getShape())
        {
            case BOX:
            case ROUNDED_BOX:
                final double minx = pos.getX() - halfWidth - this.getPaddingX();
                final double miny = pos.getY() - halfHeight - this.getPaddingY();
                return new SquareContext(element, pos, new Rectangle2D(minx, miny, this.getWidth() + this.getPaddingX() * 2d, this.getHeight() + this.getPaddingY() * 2));
            case CIRCLE:
            default:
                final double sqrt2 = Math.sqrt(2);
                final double radiusx = halfWidth * sqrt2 + this.getPaddingX();
                final double radiusy = halfHeight * sqrt2 + this.getPaddingY();
                return new CircleContext(element, pos, radiusx, radiusy);
        }
    }


    @Override
    protected void elementInvisible(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        // do nothing
    }


    @Override
    protected void renderElement(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        final GraphicNode node = (GraphicNode) element;
        final Point2D pos = camera.graphToScreen(new Point2D(node.x, node.y));
        this.renderElement(group, g, camera, node, pos);
    }


    protected void renderElement(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element, Point2D pos)
    {
        if (null == group || null == element)
        {
            return;
        }

        final ElementContext ctx = camera.getElement(element.getId());
        if (null == ctx)
        {
            return;
        }

        final Rectangle2D bounds = ctx.getBounds();
        if (null == pos || null == bounds)
        {
            return;
        }

        final Affine transform = new Affine();
        transform.appendTranslation(ctx.getPosition().getX(), ctx.getPosition().getY());
        switch (group.getShape())
        {
            case DIAMOND:
                transform.appendRotation(45d);
        }
        g.setTransform(transform);

        if (!StyleConstants.FillMode.NONE.equals(group.getFillMode()))
        {
            switch (group.getShape())
            {
                case BOX:
                    g.fillRect(-bounds.getWidth() / 2d, -bounds.getHeight() / 2d, bounds.getWidth(), bounds.getHeight());
                    break;
                case DIAMOND:
                case ROUNDED_BOX:
                    g.fillRoundRect(-bounds.getWidth() / 2d, -bounds.getHeight() / 2d, bounds.getWidth(), bounds.getHeight(), 4, 4);
                    break;
                case CIRCLE:
                default:
                    g.fillOval(-bounds.getWidth() / 2d, -bounds.getHeight() / 2d, bounds.getWidth(), bounds.getHeight());
            }
        }

        if (!StyleConstants.StrokeMode.NONE.equals(group.getStrokeMode()))
        {
            switch (group.getShape())
            {
                case BOX:
                    g.strokeRect(-bounds.getWidth() / 2d, -bounds.getHeight() / 2d, bounds.getWidth(), bounds.getHeight());
                    break;
                case DIAMOND:
                case ROUNDED_BOX:
                    g.strokeRoundRect(-bounds.getWidth() / 2d, -bounds.getHeight() / 2d, bounds.getWidth(), bounds.getHeight(), 4, 4);
                    break;
                case CIRCLE:
                default:
                    g.strokeOval(-bounds.getWidth() / 2d, -bounds.getHeight() / 2d, bounds.getWidth(), bounds.getHeight());
            }
        }

        g.setTransform(new Affine());

        final Image icon = this.renderIcon(group, g, camera, element, this.getWidth(), this.getHeight());
        if (icon != null)
        {
            final double iconX = pos.getX() - (icon.getWidth() / 2d);
            final double iconY = pos.getY() - (icon.getHeight() / 2d);
            g.drawImage(icon, iconX, iconY);
        }
    }
}
