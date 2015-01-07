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
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.SizeMode;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.swingViewer.util.GraphMetrics;


public class SpriteRenderer extends ElementRenderer
{
    private GraphMetrics metrics;

    private Values size;

    private double width;

    private double height;

    private double halfWidth;

    private double halfHeight;


    @Override
    protected void setupRenderingPass(StyleGroup group, GraphicsContext g, FxCamera camera)
    {
        super.setupRenderingPass(group, g, camera);
        this.metrics = camera.getMetrics();
    }


    @Override
    protected void pushDynStyle(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        super.pushDynStyle(group, g, camera, element);

        if (SizeMode.DYN_SIZE.equals(group.getSizeMode()))
        {
            final double length = metrics.lengthToGu(StyleConstants.convertValue(element.getAttribute("ui.size")));
            this.width = length;
            this.height = length;
            this.halfWidth = this.width / 2d;
            this.halfHeight = this.height / 2d;
        }
    }


    @Override
    protected void pushStyle(StyleGroup group, GraphicsContext g, FxCamera camera)
    {
        this.size = group.getSize();
        this.width = this.metrics.lengthToGu(this.size, 0);
        this.height = this.size.size() > 1 ? this.metrics.lengthToGu(this.size, 1) : this.width;
        this.halfWidth = width / 2d;
        this.halfHeight = height / 2d;
    }


    @Override
    protected ElementContext computeElement(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        return null;
    }


    @Override
    protected void elementInvisible(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {

    }


    @Override
    protected void renderElement(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        final GraphicSprite sprite = (GraphicSprite) element;

        Point2D pos = null;
        if (camera instanceof FxCamera)
        {
            pos = ((FxCamera) camera).getSpritePosition(sprite, StyleConstants.Units.GU);
        }
        if (null == pos)
        {
            return;
        }

        final double x = pos.getX() - this.halfWidth;
        final double y = pos.getY() - this.halfHeight;
        if (!StyleConstants.FillMode.NONE.equals(group.getFillMode()))
        {
            g.fillOval(x, y, this.width, this.halfHeight);
        }
        if (!StyleConstants.StrokeMode.NONE.equals(group.getStrokeMode()))
        {
            g.strokeOval(x, y, this.width, this.halfHeight);
        }
        this.renderText(group, g, camera, element);
        renderText(group, g, camera, element);
    }
}