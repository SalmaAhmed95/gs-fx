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
import org.graphstream.ui.geom.Vector2;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.ArrowShape;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.SizeMode;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.javafx.util.DefaultCamera;
import org.graphstream.ui.view.Camera;

import java.awt.geom.Path2D;

public class EdgeRenderer extends ElementRenderer
{
    private double width = 1;

    private double arrowLength = 0;

    private double arrowWidth = 0;


    @Override
    protected void pushDynStyle(StyleGroup group, GraphicsContext g, Camera camera, GraphicElement element)
    {
        super.pushDynStyle(group, g, camera, element);

        if (SizeMode.DYN_SIZE.equals(group.getSizeMode()))
        {
            this.width = camera.getMetrics().lengthToGu(StyleConstants.convertValue(element.getAttribute("ui.size")));
            g.setLineWidth(this.width);
            g.setLineCap(StrokeLineCap.BUTT);
            g.setLineJoin(StrokeLineJoin.BEVEL);
        }
    }


    @Override
    protected void pushStyle(StyleGroup group, GraphicsContext g, Camera camera)
    {
        this.pushFillStyle(group, g);
        this.pushStrokeStyle(group, g);
        this.width = group.getSize().get(0);
        this.arrowLength = group.getArrowSize().get(0);
        this.arrowWidth = group.getArrowSize().get(0);
    }


    @Override
    protected void elementInvisible(StyleGroup group, GraphicsContext g, Camera camera, GraphicElement element)
    {

    }


    @Override
    protected void renderElement(StyleGroup group, GraphicsContext g, Camera camera, GraphicElement element)
    {
        GraphicEdge edge = (GraphicEdge) element;
        DefaultCamera camerafx = (DefaultCamera) camera;
        GraphicNode node0 = edge.getNode0();
        GraphicNode node1 = edge.getNode1();
        Point2D pos0 = camerafx.graphToScreen(new Point2D(node0.x, node0.y));
        Point2D pos1 = camerafx.graphToScreen(new Point2D(node1.x, node1.y));
        g.strokeLine(pos0.getX(), pos0.getY(), pos1.getX(), pos1.getY());
        renderArrow(group, g, camera, edge);
        renderText(group, g, camera, element);
    }


    private void renderArrow(StyleGroup group, GraphicsContext g, Camera camera, GraphicEdge edge)
    {
        if (edge.isDirected() && arrowWidth > 0 && arrowLength > 0)
        {
            if (group.getArrowShape() != ArrowShape.NONE)
            {
                Path2D shape = new Path2D.Double();
                GraphicNode node0 = edge.getNode0();
                GraphicNode node1 = edge.getNode1();
                double off = evalEllipseRadius(edge, node0, node1, camera);
                Vector2 theDirection = new Vector2(node1.getX() - node0.getX(), node1.getY() - node0.getY());

                theDirection.normalize();

                double x = node1.x - (theDirection.data[0] * off);
                double y = node1.y - (theDirection.data[1] * off);
                Vector2 perp = new Vector2(theDirection.data[1], -theDirection.data[0]);

                perp.normalize();
                theDirection.scalarMult(arrowLength);
                perp.scalarMult(arrowWidth);

                shape.reset();
                shape.moveTo(x, y);
                shape.lineTo(x - theDirection.data[0] + perp.data[0], y - theDirection.data[1] + perp.data[1]);
                shape.lineTo(x - theDirection.data[0] - perp.data[0], y - theDirection.data[1] - perp.data[1]);
                shape.closePath();

                this.strokeShake(shape, g);
            }
        }
    }


    private double evalEllipseRadius(GraphicEdge edge, GraphicNode node0, GraphicNode node1, Camera camera)
    {
        Values size = node1.getStyle().getSize();
        double w = camera.getMetrics().lengthToGu(size.get(0), size.getUnits());
        double h = size.size() > 1 ? camera.getMetrics().lengthToGu(
            size.get(1), size.getUnits()) : w;

        w /= 2;
        h /= 2;

        if (w == h)
        {
            return w; // Welcome simplification for circles ...
        }

        // Vector of the entering edge.
        double dx = node1.getX() - node0.getX();
        double dy = node1.getY() - node0.getY();

        // The entering edge must be deformed by the ellipse ratio to find the
        // correct angle.

        dy *= (w / h);

        // Find the angle of the entering vector with (1,0).

        double d = Math.sqrt(dx * dx + dy * dy);
        double a = dx / d;

        // Compute the coordinates at which the entering vector and the ellipse
        // cross.

        a = Math.acos(a);
        dx = Math.cos(a) * w;
        dy = Math.sin(a) * h;

        // The distance from the ellipse center to the crossing point of the
        // ellipse and vector:

        return Math.sqrt(dx * dx + dy * dy);
    }
}