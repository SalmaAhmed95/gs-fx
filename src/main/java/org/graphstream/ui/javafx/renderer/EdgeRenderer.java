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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.ArrowShape;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.SizeMode;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.javafx.util.Approximations;

public class EdgeRenderer extends ElementRenderer
{
    private double width = 1;

    private double arrowLength = 0;

    private double arrowWidth = 0;

    private double padding;


    @Override
    protected void pushDynStyle(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {
        super.pushDynStyle(group, g, camera, element);
        this.configurePadding(group);
        if (SizeMode.DYN_SIZE.equals(group.getSizeMode()))
        {
            this.width = camera.getMetrics().lengthToGu(StyleConstants.convertValue(element.getAttribute("ui.size")));
            g.setLineWidth(this.width);
            g.setLineCap(StrokeLineCap.BUTT);
            g.setLineJoin(StrokeLineJoin.BEVEL);
        }
    }


    @Override
    protected void pushStyle(StyleGroup group, GraphicsContext g, FxCamera camera)
    {
        this.configurePadding(group);
        this.pushFillStyle(group, g);
        this.pushStrokeStyle(group, g);
        this.width = group.getSize().get(0);
        this.arrowLength = group.getArrowSize().get(0);
        this.arrowWidth = group.getArrowSize().get(0);
    }


    @Override
    protected ElementContext computeElement(StyleGroup group, FxCamera camera, GraphicElement element)
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
        GraphicEdge edge = (GraphicEdge) element;
        ElementContext node0 = camera.getElement(edge.getNode0().getId());
        ElementContext node1 = camera.getElement(edge.getNode1().getId());
        if (null == node0 || null == node1)
        {
            return;
        }
        Point2D pos0 = node0.getPosition();
        Point2D pos1 = node1.getPosition();
        g.strokeLine(pos0.getX(), pos0.getY(), pos1.getX(), pos1.getY());
        renderArrow(group, g, camera, edge);
        renderText(group, g, camera, element);
    }


    private void configurePadding(final StyleGroup group)
    {
        Values value = group.getPadding();
        if (null == value)
        {
            this.padding = 0d;
        }
        else
        {
            this.padding = value.get(0);
        }
    }


    private void renderArrow(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicEdge edge)
    {
        if (!edge.isDirected())
        {
            return;
        }
        if (ArrowShape.NONE.equals(group.getArrowShape()))
        {
            return;
        }
        if (this.arrowLength <= 0 || this.arrowWidth <= 0)
        {
            return;
        }

        GraphicNode node0 = edge.getNode0();
        GraphicNode node1 = edge.getNode1();
        ElementContext ctx0 = camera.getElement(node0.getId());
        ElementContext ctx1 = camera.getElement(node1.getId());
        if (null == ctx0 || null == ctx1)
        {
            return;
        }

        Point2D pos0 = camera.graphToScreen(new Point2D(node0.getX(), node0.getY()));
        Point2D pos1 = camera.graphToScreen(new Point2D(node1.getX(), node1.getY()));
        Point2D arrowCenter = ctx1.intersects(pos0.getX(), pos0.getY(), pos1.getX(), pos1.getY());
        if (null == arrowCenter)
        {
            return;
        }

        Affine transform = new Affine();
        double deltax = pos1.getX() - pos0.getX();
        double deltay = pos1.getY() - pos0.getY();
        transform.appendTranslation(arrowCenter.getX(), arrowCenter.getY() + this.padding);
        if (Approximations.approximatelyEquals(deltay, 0d, 0.00001d))
        {
            transform.appendRotation(deltax > 0 ? 90d : 270d);
        }
        else
        {
            transform.appendRotation(90 + Math.toDegrees(Math.atan2(deltay, deltax)));
        }
        double halfWidth = this.arrowWidth / 2d;
        Point2D arrowLeft = transform.transform(new Point2D(-halfWidth, this.arrowLength));
        Point2D arrowRight = transform.transform(new Point2D(halfWidth, this.arrowLength));

        g.beginPath();
        g.moveTo(arrowLeft.getX(), arrowLeft.getY());
        g.lineTo(arrowCenter.getX(), arrowCenter.getY());
        g.lineTo(arrowRight.getX(), arrowRight.getY());
        g.lineTo(arrowLeft.getX(), arrowLeft.getY());
        g.closePath();
        if (!StyleConstants.FillMode.NONE.equals(group.getFillMode()))
        {
            g.fill();
        }
        if (!StyleConstants.StrokeMode.NONE.equals(group.getStrokeMode()))
        {
            g.stroke();
        }
    }
}