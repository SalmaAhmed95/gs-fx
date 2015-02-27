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
import org.graphstream.ui.javafx.util.Approximations;
import org.graphstream.ui.javafx.util.IconManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public class EdgeRenderer extends ElementRenderer
{
    private double lineWidth = 1;

    private double arrowLength = 0;

    private double arrowWidth = 0;

    private final Set<String> renderedEdges = new TreeSet<>();


    @Override
    public void clear()
    {
        super.clear();
        this.renderedEdges.clear();
    }


    @Override
    protected void pushDynStyle(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicElement element)
    {
        super.pushDynStyle(group, g, camera, element);

        if (SizeMode.DYN_SIZE.equals(group.getSizeMode()))
        {
            this.lineWidth = camera.getMetrics().lengthToGu(StyleConstants.convertValue(element.getAttribute("ui.size")));
            g.setLineWidth(this.lineWidth);
            g.setLineCap(StrokeLineCap.BUTT);
            g.setLineJoin(StrokeLineJoin.BEVEL);
        }
    }


    @Override
    protected void pushStyle(final StyleGroup group, final GraphicsContext g, final FxCamera camera)
    {
        super.pushStyle(group, g, camera);

        this.lineWidth = group.getStrokeWidth().doubleValue();
        this.arrowLength = group.getArrowSize().get(0);
        this.arrowWidth = group.getArrowSize().get(0);
    }


    @Override
    protected ElementContext computeElement(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicElement element)
    {
        // no context, no ability to click on edge for now
        return null;
    }


    @Override
    protected void elementInvisible(StyleGroup group, GraphicsContext g, FxCamera camera, GraphicElement element)
    {

    }


    @Override
    protected void renderElement(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final GraphicElement element)
    {
        // grab edge, node positions
        final GraphicEdge edge = (GraphicEdge) element;
        final ElementContext node0 = camera.getElement(edge.getNode0().getId());
        final ElementContext node1 = camera.getElement(edge.getNode1().getId());
        if (null == node0 || null == node1)
        {
            return;
        }

        final GraphicEdge.EdgeGroup edgeGroup = edge.getGroup();
        final String id = edge.getId();
        if (edgeGroup != null && this.renderedEdges.contains(id))
        {
            return;
        }

        // render line        
        final Point2D pos0 = node0.getPosition();
        final Point2D pos1 = node1.getPosition();
        Point2D start0 = node0.intersects(pos0.getX(), pos0.getY(), pos1.getX(), pos1.getY());
        if (null == start0)
        {
            start0 = pos0;
        }
        Point2D start1 = node1.intersects(pos1.getX(), pos1.getY(), pos0.getX(), pos0.getY());
        if (null == start1)
        {
            start1 = pos1;
        }
        g.strokeLine(start0.getX(), start0.getY(), start1.getX(), start1.getY());

        // render arrow
        if (edge.isDirected())
        {
            renderArrow(group, g, camera, edge);
        }
        if (edgeGroup != null)
        {
            // render other side arrow (if applicable)
            for (final GraphicEdge otherEdge : edgeGroup.getEdges())
            {
                if (otherEdge.isDirected() && !this.renderedEdges.contains(otherEdge.getId()) && edge.getNode0() != otherEdge.getNode0())
                {
                    this.renderArrow(group, g, camera, otherEdge);
                    break;
                }
            }
        }

        // render icon at midpoint
        final Point2D midpoint = new Point2D((pos0.getX() + pos1.getX()) / 2d, (pos0.getY() + pos1.getY()) / 2d);
        if (edgeGroup != null)
        {
            this.renderIcon(group, g, camera, midpoint, edgeGroup.getEdges());
        }
        else
        {
            this.renderIcon(group, g, camera, midpoint, Arrays.asList(edge));
        }

        // render text
        this.renderText(group, g, camera, element);

        // keep track of rendered edges when we have edge groups
        if (edgeGroup != null)
        {
            for (final GraphicEdge otherEdge : edgeGroup.getEdges())
            {
                this.renderedEdges.add(otherEdge.getId());
            }
            this.renderedEdges.add(id);
        }
    }


    private Rectangle2D renderIcon(final StyleGroup group, final GraphicsContext g, final FxCamera camera, final Point2D midpoint, final Collection<GraphicEdge> edges)
    {
        if (null == edges || edges.isEmpty())
        {
            return null;
        }

        // find all unique icons
        final Map<String, Image> icons = new TreeMap<>();
        int num = 0;
        for (final GraphicEdge edge : edges)
        {
            final String iconName;
            if ("dyn-icon".equalsIgnoreCase(group.getIcon()) || "dynamic".equalsIgnoreCase(group.getIcon()))
            {
                iconName = edge.getAttribute("ui.icon");
            }
            else
            {
                iconName = group.getIcon();
            }

            if (iconName != null)
            {
                final Image icon = this.renderIcon(group, g, camera, edge, this.getWidth(), this.getHeight());
                if (icon != null)
                {
                    icons.put(UUID.randomUUID().toString(), icon);
                    num++;
                }
                if (num >= 15)
                {
                    icons.put("zzzzzzz", IconManager.getInstance().get("right-arrow-next", (int) this.getWidth(), (int) this.getHeight()));
                    break;
                }
            }
        }
        if (icons.isEmpty())
        {
            return null;
        }

        // compute overall size
        final int numIcons = icons.size();
        final double sqrt = Math.sqrt(numIcons);
        int columns = (int) Math.floor(sqrt + 0.0001d);
        int rows = columns;
        if (numIcons < 4)
        {
            columns = numIcons;
            rows = 1;
        }
        else if (columns <= 0)
        {
            columns = numIcons;
        }
        else
        {
            final int leftoverItems = numIcons - (columns * columns);
            if (leftoverItems > 0)
            {
                rows += leftoverItems / columns;
                if (leftoverItems % columns != 0)
                {
                    rows++;
                }
            }
        }
        final double width = (columns * this.getWidth()) + ((columns + 1) * this.getPaddingX());
        final double height = (rows * this.getHeight()) + ((rows + 1) * this.getPaddingY());

        // set translate transform
        final Affine transform = new Affine();
        transform.appendTranslation(midpoint.getX(), midpoint.getY());
        g.setTransform(transform);

        // render shape
        if (!StyleConstants.FillMode.NONE.equals(group.getFillMode()))
        {
            g.fillRoundRect(-width / 2d, -height / 2d, width, height, 4, 4);
        }
        if (!StyleConstants.StrokeMode.NONE.equals(group.getStrokeMode()))
        {
            g.strokeRoundRect(-width / 2d, -height / 2d, width, height, 4, 4);
        }

        // render all icons
        g.setTransform(new Affine());
        final Rectangle2D bounds = new Rectangle2D(midpoint.getX() - (width / 2d), midpoint.getY() - (height / 2d), width, height);
        double x = this.getPaddingX() + bounds.getMinX();
        double y = this.getPaddingY() + bounds.getMinY();
        int row = 0;
        int col = 0;
        for (final Image icon : icons.values())
        {
            if (icon != null)
            {
                g.drawImage(icon, x, y);
            }
            col++;
            if (rows > 1 && col >= columns)
            {
                col = 0;
                row++;
                x = this.getPaddingX() + bounds.getMinX();
                y += this.getHeight() + this.getPaddingY();
            }
            else
            {
                x += this.getWidth() + this.getPaddingX();
            }
        }
        return bounds;
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
        transform.appendTranslation(arrowCenter.getX(), arrowCenter.getY() + this.getPadding());
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
