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
import javafx.scene.canvas.GraphicsContext;
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

    @Override
    protected void pushDynStyle(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        super.pushDynStyle(group, g, camera, element);
        this.configureSize(group, element);
    }

    @Override
    protected void pushStyle(StyleGroup group, GraphicsContext g, FxCamera camera)
    {
        this.configureSize(group, null);
        this.pushFillStyle(group, g);
        this.pushStrokeStyle(group, g);
    }

    @Override
    protected ElementContext computeElement(StyleGroup group, FxCamera camera, GraphicElement element)
    {
        GraphicNode node = (GraphicNode) element;
        this.configureSize(group, element);
        Point2D pos = camera.graphToScreen(new Point2D(node.x, node.y));
        return new CircleContext(node, pos, this.width / 2d, this.height / 2d);
    }

    @Override
    protected void elementInvisible(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {

    }

    @Override
    protected void renderElement(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        GraphicNode node = (GraphicNode) element;
        Point2D pos = camera.graphToScreen(new Point2D(node.x, node.y));
        double x = pos.getX() - (this.width / 2d);
        double y = pos.getY() - (this.height / 2d);

        if (!StyleConstants.FillMode.NONE.equals(group.getFillMode()))
        {
            switch (group.getShape())
            {
                case BOX:
                    g.fillRect(x, y, this.width, this.height);
                    break;
                case ROUNDED_BOX:
                    g.fillRoundRect(x, y, this.width, this.height, 4, 4);
                    break;
                case CIRCLE:
                default:
                    g.fillOval(x, y, this.width, this.height);
            }
        }
        if (!StyleConstants.StrokeMode.NONE.equals(group.getStrokeMode()))
        {
            switch (group.getShape())
            {
                case BOX:
                    g.strokeRect(x, y, this.width, this.height);
                    break;
                case ROUNDED_BOX:
                    g.strokeRoundRect(x, y, this.width, this.height, 4, 4);
                    break;
                case CIRCLE:
                default:
                    g.strokeOval(x, y, this.width, this.height);
            }
        }

        this.renderText(group, g, camera, element);
    }

    private void configureSize(final StyleGroup group, final GraphicElement element)
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
            }
        }
        else
        {
            this.size = group.getSize();
            this.width = this.size.get(0);
            this.height = this.size.size() > 1 ? this.size.get(1) : this.width;
        }
    }
}
