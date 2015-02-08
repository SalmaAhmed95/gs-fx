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
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.Style;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.SizeMode;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.graphicGraph.stylesheet.Values;


public class NodeRenderer extends ElementRenderer
{
    private Values size;

    private double width = 0;

    private double height = 0;

    private double paddingX = 0;

    private double paddingY = 0;


    @Override
    protected void pushDynStyle(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        super.pushDynStyle(group, g, camera, element);
        this.configureSize(group, g, camera, element);
    }


    @Override
    protected void pushStyle(StyleGroup group, GraphicsContext g, FxCamera camera)
    {
        this.configureSize(group, g, camera, null);
        this.pushFillStyle(group, g);
        this.pushStrokeStyle(group, g);
    }


    @Override
    protected ElementContext computeElement(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicElement element)
    {
        final GraphicNode node = (GraphicNode) element;
        this.configureSize(group, g, camera, element);
        final Point2D pos = camera.graphToScreen(new Point2D(node.x, node.y));
        final Image icon = this.renderIcon(group, g, camera, element);
        if (icon != null)
        {
            final double minx = pos.getX() - (this.width / 2d) - this.paddingX;
            final double miny = pos.getY() - (this.height / 2d) - this.paddingX;
            return new SquareContext(node, pos, new Rectangle2D(minx, miny, this.width + this.paddingX * 2d, this.height + this.paddingY * 2));
        }
        else
        {
            final double radiusx = (this.width / 2d) + this.paddingX;
            final double radiusy = (this.height / 2d) + this.paddingY;
            return new CircleContext(node, pos, radiusx, radiusy);
        }
    }


    @Override
    protected void elementInvisible(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {

    }


    @Override
    protected void renderElement(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        GraphicNode node = (GraphicNode) element;
        Rectangle2D bounds = camera.getElement(element.getId()).getBounds();
        Point2D pos = camera.graphToScreen(new Point2D(node.x, node.y));

        if (!StyleConstants.FillMode.NONE.equals(group.getFillMode()))
        {
            switch (group.getShape())
            {
                case BOX:
                    g.fillRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
                    break;
                case ROUNDED_BOX:
                    g.fillRoundRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight(), 4, 4);
                    break;
                case CIRCLE:
                default:
                    g.fillOval(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            }
        }

        if (!StyleConstants.StrokeMode.NONE.equals(group.getStrokeMode()))
        {
            switch (group.getShape())
            {
                case BOX:
                    g.strokeRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
                    break;
                case ROUNDED_BOX:
                    g.strokeRoundRect(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight(), 4, 4);
                    break;
                case CIRCLE:
                default:
                    g.strokeOval(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
            }
        }

        final Image icon = this.renderIcon(group, g, camera, element, this.width, this.height);
        if (icon != null)
        {
            final double iconX = pos.getX() - (icon.getWidth() / 2d);
            final double iconY = pos.getY() - (icon.getHeight() / 2d);
            g.drawImage(icon, iconX, iconY);
        }
    }


    private void configureSize(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicElement element)
    {
        if (SizeMode.DYN_SIZE.equals(group.getSizeMode()))
        {
            final Object s = element != null ? element.getAttribute("ui.size") : null;
            if (s != null)
            {
                final Value length = StyleConstants.convertValue(s);
                final double lengthPx = length != null ? length.doubleValue() : 1d;
                this.size = new Values(Style.Units.PX, lengthPx);
                this.width = lengthPx;
                this.height = lengthPx;
            }
            else
            {
                this.size = group.getSize();
                this.width = this.size.get(0);
                this.height = this.size.size() > 1 ? this.size.get(1) : this.width;
                if (StyleConstants.Units.GU.equals(this.size.getUnits()))
                {
                    final Point2D pos = camera.graphToScreen(new Point2D(this.width, this.height));
                    if (pos != null && Double.isFinite(pos.getX()) && Double.isFinite(pos.getY()))
                    {
                        this.width = pos.getX();
                        this.height = pos.getY();
                    }
                }
            }
        }
        else
        {
            this.size = group.getSize();
            this.width = this.size.get(0);
            this.height = this.size.size() > 1 ? this.size.get(1) : this.width;
            if (StyleConstants.Units.GU.equals(this.size.getUnits()))
            {
                final Point2D pos = camera.graphToScreen(new Point2D(this.width, this.height));
                if (pos != null && Double.isFinite(pos.getX()) && Double.isFinite(pos.getY()))
                {
                    this.width = pos.getX();
                    this.height = pos.getY();
                }
            }
        }

        if (SizeMode.FIT.equals(group.getSizeMode()))
        {
            final Image icon = this.renderIcon(group, g, camera, element);
            if (icon != null)
            {
                this.width = Math.max(this.width, icon.getWidth());
                this.height = Math.max(this.height, icon.getHeight());
            }
        }

        if (this.isAutoScale())
        {
            if (camera.getViewPercent() <= 1)
            {
                final double maxWidth = this.width * 1.5d;
                final double maxHeight = this.height * 1.5d;
                final double scaledWidth = this.width + (1d - camera.getViewPercent()) * this.width;
                final double scaledHeight = this.height + (1d - camera.getViewPercent()) * this.height;
                this.width = (int) Math.round(Math.min(maxWidth, scaledWidth));
                this.height = (int) Math.round(Math.min(maxHeight, scaledHeight));
            }
            else
            {
                final double minWidth = this.width * 0.5d;
                final double minHeight = this.height * 0.5d;
                final double scaledWidth = this.width - (camera.getViewPercent() - 1d) * this.width;
                final double scaledHeight = this.height - (camera.getViewPercent() - 1d) * this.height;
                this.width = (int) Math.round(Math.max(minWidth, scaledWidth));
                this.height = (int) Math.round(Math.max(minHeight, scaledHeight));
            }
        }

        final Values padding = group.getPadding();
        if (padding != null && padding.getValueCount() > 0)
        {
            this.paddingX = padding.get(0);
            this.paddingY = padding.getValueCount() > 1 ? padding.get(1) : this.paddingX;
        }
        else
        {
            this.paddingX = 0d;
            this.paddingY = 0d;
        }
    }
}
