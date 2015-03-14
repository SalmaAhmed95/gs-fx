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
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point2;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.geom.Vector2;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.javafx.util.Approximations;
import org.graphstream.ui.swingViewer.util.GraphMetrics;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.util.CubicCurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Define how the graph is viewed.
 * <p>
 * <p>
 * The camera is in charge of projecting the graph spaces in graph units (GU)
 * into user spaces (often in pixels). It defines the transformation (an affine
 * matrix) to passe from the first to the second. It also contains the graph
 * metrics, a set of values that give the overall dimensions of the graph in
 * graph units, as well as the view port, the area on the screen (or any
 * rendering surface) that will receive the results in pixels (or rendering
 * units).
 * </p>
 * <p>
 * <p>
 * The camera defines a centre at which it always points. It can zoom on the
 * graph, pan in any direction and rotate along two axes.
 * </p>
 * <p>
 * <p>
 * Knowing the transformation also allows to provide services like "what element
 * is not invisible ?" (not in the camera view) or "on what element is the mouse
 * cursor actually ?".
 * </p>
 */
public class FxCamera implements Camera
{
    private static final Logger logger = LoggerFactory.getLogger(FxCamera.class);

    private GraphicGraph graph = null;

    private GraphMetrics metrics = new GraphMetrics();

    private boolean autoFit = true;

    private Point3 center = new Point3();

    private double zoom;

    private Affine Tx = new Affine();

    private Affine xT;

    private double rotation;

    private Values padding = new Values(Units.GU, 0, 0, 0);

    private double gviewport[] = null;

    private double gviewportDiagonal = 0;

    private final Map<String, ElementContext> elements = new TreeMap<>();

    public FxCamera(GraphicGraph graph)
    {
        this.graph = graph;
    }

    @Override
    public Point3 getViewCenter()
    {
        return center;
    }

    @Override
    public void setViewCenter(double x, double y, double z)
    {
        this.setAutoFitView(false);
        this.center.set(x, y, z);
        this.graph.graphChanged = true;
    }

    public void setViewCenter(double x, double y)
    {
        setViewCenter(x, y, 0d);
    }

    @Override
    public double getViewPercent()
    {
        return zoom;
    }

    @Override
    public void setViewPercent(double percent)
    {
        setAutoFitView(false);
        setZoom(percent);
        graph.graphChanged = true;
    }

    @Override
    public double getViewRotation()
    {
        return rotation;
    }

    @Override
    public GraphMetrics getMetrics()
    {
        return metrics;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder(String.format("Camera :%n"));
        builder.append(String.format(", autoFit  = %b%n", autoFit));
        builder.append(String.format(", center   = %s%n", center));
        builder.append(String.format(", rotation = %f%n", rotation));
        builder.append(String.format(", zoom     = %f%n", zoom));
        builder.append(String.format(", padding  = %s%n", padding));
        builder.append(String.format(", metrics  = %s%n", metrics));
        return builder.toString();
    }

    @Override
    public void resetView()
    {
        setAutoFitView(true);
        setViewRotation(0);
    }

    @Override
    public void setBounds(double minx, double miny, double minz, double maxx, double maxy, double maxz)
    {
        metrics.setBounds(minx, miny, minz, maxx, maxy, maxz);
    }

    @Override
    public double getGraphDimension()
    {
        if (this.gviewport != null)
        {
            return this.gviewportDiagonal;
        }

        return this.metrics.diagonal;
    }

    @Override
    public boolean isVisible(GraphicElement element)
    {
        if (null == element)
        {
            return false;
        }

        if (element.hidden)
        {
            return false;
        }

        final StyleGroup style = element.getStyle();
        if (null == style)
        {
            return false;
        }
        if (StyleConstants.VisibilityMode.HIDDEN.equals(style.getVisibilityMode()))
        {
            return false;
        }

        if (this.autoFit)
        {
            return true;
        }

        switch (element.getSelectorType())
        {
            case NODE:
                return isNodeVisible((GraphicNode) element);
            case EDGE:
                return isEdgeVisible((GraphicEdge) element);
            case SPRITE:
                return isSpriteVisible((GraphicSprite) element);
            default:
                return false;
        }
    }

    @Override
    public Point3 transformPxToGu(final double x, final double y)
    {
        final javafx.geometry.Point2D p = xT.transform(x, y);
        if (null == p)
        {
            return null;
        }
        return new Point3(p.getX(), p.getY(), 0d);
    }

    @Override
    public Point3 transformGuToPx(double x, double y, double z)
    {
        final javafx.geometry.Point2D p = Tx.transform(x, y);
        if (null == p)
        {
            return null;
        }
        return new Point3(p.getX(), p.getY(), z);
    }

    public void pushView(final GraphicGraph graph, final GraphicsContext g)
    {
        if (this.autoFit)
        {
            autoFitView();
        }
        else
        {
            userView();
        }
    }

    public boolean putElement(final ElementContext context)
    {
        if (null == context)
        {
            return false;
        }
        final String id = context.getId();
        if (null == id || id.isEmpty())
        {
            return false;
        }
        if (context.getBounds() == null || context.getBounds().getWidth() <= 0 || context.getBounds().getHeight() <= 0)
        {
            return this.elements.remove(id) != null;
        }
        else
        {
            this.elements.put(id, context);
            return true;
        }
    }

    public boolean removeElement(final String id)
    {
        if (null == id)
        {
            return false;
        }
        return this.elements.remove(id) != null;
    }

    public ElementContext getElement(final String id)
    {
        if (null == id)
        {
            return null;
        }
        return this.elements.get(id);
    }

    public GraphicElement findNodeOrSpriteAt(final GraphicGraph graph, final double x, final double y)
    {
        for (Node n : graph)
        {
            GraphicNode node = (GraphicNode) n;

            if (nodeContains(node, x, y))
            {
                return node;
            }
        }

        for (GraphicSprite sprite : graph.spriteSet())
        {
            if (spriteContains(sprite, x, y))
            {
                return sprite;
            }
        }

        return null;
    }

    public Collection<GraphicElement> allNodesOrSpritesIn(GraphicGraph graph, double x1, double y1, double x2, double y2)
    {
        List<GraphicElement> elts = new ArrayList<>();

        for (Node node : graph)
        {
            if (isNodeIn((GraphicNode) node, x1, y1, x2, y2))
            {
                elts.add((GraphicNode) node);
            }
        }

        for (GraphicSprite sprite : graph.spriteSet())
        {
            if (isSpriteIn(sprite, x1, y1, x2, y2))
            {
                elts.add(sprite);
            }
        }

        return Collections.unmodifiableList(elts);
    }

    public Point2D getSpritePosition(GraphicSprite sprite, Units units)
    {
        if (sprite.isAttachedToNode())
        {
            return getSpritePositionNode(sprite, units);
        }
        else if (sprite.isAttachedToEdge())
        {
            return getSpritePositionEdge(sprite, units);
        }
        else
        {
            return getSpritePositionFree(sprite, units);
        }
    }

    public double[] getGraphViewport()
    {
        return gviewport;
    }

    @Override
    public void setGraphViewport(double minx, double miny, double maxx, double maxy)
    {
        setAutoFitView(false);
        setViewCenter(minx + (maxx - minx) / 2.0, miny + (maxy - miny) / 2.0);

        gviewport = new double[4];
        gviewport[0] = minx;
        gviewport[1] = miny;
        gviewport[2] = maxx;
        gviewport[3] = maxy;

        gviewportDiagonal = Math.sqrt((maxx - minx) * (maxx - minx) + (maxy - miny) * (maxy - miny));

        setZoom(1);
    }

    @Override
    public void removeGraphViewport()
    {
        logger.debug("Graph viewport removed for [" + this + "].");
        this.gviewport = null;
        this.resetView();
    }

    private void autoFitView()
    {
        double sx = (metrics.getViewportWidth() - getPaddingXpx() * 2d) / (metrics.size.data[0] + getPaddingXgu() * 2d);
        double sy = (metrics.getViewportHeight() - getPaddingYpx() * 2d) / (metrics.size.data[1] + getPaddingYgu() * 2d);
        double tx = metrics.lo.x + (metrics.size.data[0] / 2d);
        double ty = metrics.lo.y + (metrics.size.data[1] / 2d);
        double centerx = metrics.getViewportX() + metrics.getViewportWidth() / 2d;
        double centery = metrics.getViewportY() + metrics.getViewportHeight() / 2d;
        double scale = Math.min(sx, sy);

        this.Tx = new Affine();
        this.Tx.appendTranslation(centerx, centery);
        if (this.rotation != 0)
        {
            this.Tx.appendRotation(this.rotation / (180d / Math.PI));
        }
        this.Tx.appendScale(scale, -scale);
        this.Tx.appendTranslation(-tx, -ty);

        this.xT = new Affine(Tx);
        try
        {
            this.xT.invert();
        }
        catch (final NonInvertibleTransformException e)
        {
            logger.debug("Cannot inverse gu2px matrix.", e);
        }

        this.zoom = 1;
        this.center.set(tx, ty, 0);
        if (scale > 0 && !Approximations.approximatelyEquals(0, scale, .000001))
        {
            this.metrics.setRatioPx2Gu(scale);
        }
        this.metrics.loVisible.copy(this.metrics.lo);
        this.metrics.hiVisible.copy(this.metrics.hi);
    }

    private void userView()
    {
        double padXgu = getPaddingXgu() * 2;
        double padYgu = getPaddingYgu() * 2;
        double padXpx = getPaddingXpx() * 2;
        double padYpx = getPaddingYpx() * 2;
        double gw = gviewport != null ? gviewport[2] - gviewport[0] : metrics.size.data[0];
        double gh = gviewport != null ? gviewport[3] - gviewport[1] : metrics.size.data[1];

        double sx = (metrics.viewport[2] - padXpx) / ((gw + padXgu) * zoom);
        double sy = (metrics.viewport[3] - padYpx) / ((gh + padYgu) * zoom);
        double tx = center.x;
        double ty = center.y;
        double scale = Math.min(sx, sy);

        this.Tx = new Affine();
        this.Tx.appendTranslation(metrics.viewport[2] / 2d, (metrics.viewport[3] / 2d));
        if (this.rotation != 0)
        {
            this.Tx.appendRotation(this.rotation / 180d / Math.PI);
        }
        this.Tx.appendScale(scale, -scale);
        this.Tx.appendTranslation(-tx, -ty);

        this.xT = new Affine(Tx);
        try
        {
            this.xT.invert();
        }
        catch (final NonInvertibleTransformException e)
        {
            logger.info("Cannot inverse gu2px matrix.", e);
        }

        if (!Approximations.approximatelyEquals(0, scale, .000001))
        {
            this.metrics.setRatioPx2Gu(scale);
        }

        double w2 = (this.metrics.viewport[2] / sx) / 2;
        double h2 = (this.metrics.viewport[3] / sx) / 2;

        this.metrics.loVisible.set(center.x - w2, center.y - h2);
        this.metrics.hiVisible.set(center.x + w2, center.y + h2);
    }

    @Override
    public void setAutoFitView(boolean on)
    {
        if (this.autoFit && (!on))
        {
            // ensure the current centre is at the middle of the graph, and the zoom is at one
            this.zoom = 1;
            this.center.set(metrics.lo.x + (metrics.size.data[0] / 2), metrics.lo.y + (metrics.size.data[1] / 2), 0);
        }
        this.autoFit = on;
    }

    public void setZoom(double z)
    {
        this.zoom = z;
        this.graph.graphChanged = true;
        logger.debug("Zoom value updated to [" + z + "].");
    }

    @Override
    public void setViewRotation(double theta)
    {
        this.rotation = theta;
        this.graph.graphChanged = true;
    }

    public void setViewport(double viewportX, double viewportY, double viewportWidth, double viewportHeight)
    {
        this.metrics.setViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    public void setPadding(GraphicGraph graph)
    {
        this.padding.copy(graph.getStyle().getPadding());
    }

    public Point2D screenToGraph(final Point2D pos)
    {
        if (null == pos)
        {
            return null;
        }
        if (null == this.xT)
        {
            return new Point2D(pos.getX(), pos.getY());
        }
        else
        {
            return this.xT.transform(pos);
        }
    }

    public Point2D graphToScreen(final Point2D pos)
    {
        if (null == pos)
        {
            return null;
        }
        if (null == this.Tx)
        {
            return new Point2D(pos.getX(), pos.getY());
        }
        else
        {
            return this.Tx.transform(pos);
        }
    }

    private double getPaddingXgu()
    {
        if (padding.units == Units.GU && padding.size() > 0)
        {
            return padding.get(0);
        }
        return 0;
    }

    private double getPaddingYgu()
    {
        if (padding.units == Units.GU && padding.size() > 1)
        {
            return padding.get(1);
        }
        return getPaddingXgu();
    }

    private double getPaddingXpx()
    {
        if (Units.PX.equals(this.padding.units) && this.padding.size() > 0)
        {
            return this.padding.get(0);
        }
        else
        {
            return 0;
        }
    }

    private double getPaddingYpx()
    {
        if (Units.PX.equals(this.padding.units) && this.padding.size() > 0)
        {
            return this.padding.get(0);
        }
        else
        {
            return 0;
        }
    }

    private boolean isSpriteVisible(GraphicSprite sprite)
    {
        return isSpriteIn(sprite,
                this.metrics.getViewportX(), this.metrics.getViewportY(),
                this.metrics.getViewportX() + this.metrics.getViewportWidth(),
                this.metrics.getViewportY() + this.metrics.getViewportHeight());
    }

    private boolean isNodeVisible(GraphicNode node)
    {
        return isNodeIn(node,
                this.metrics.getViewportX(), this.metrics.getViewportY(),
                this.metrics.getViewportX() + this.metrics.getViewportWidth(),
                this.metrics.getViewportY() + this.metrics.getViewportHeight());
    }

    private boolean isEdgeVisible(GraphicEdge edge)
    {
        GraphicNode node0 = edge.getNode0();
        GraphicNode node1 = edge.getNode1();

        if (edge.hidden)
        {
            return false;
        }

        if (!node0.positionned)
        {
            return false;
        }

        if (!node1.positionned)
        {
            return false;
        }

        return true;
    }

    private boolean isNodeIn(GraphicNode node, double X1, double Y1, double X2, double Y2)
    {
        Values size = node.getStyle().getSize();
        double w2 = metrics.lengthToPx(size, 0) / 2;
        double h2 = size.size() > 1 ? metrics.lengthToPx(size, 1) / 2 : w2;
        boolean vis = true;

        final Point2D p = Tx.transform(node.getX(), node.getY());

        double x1 = p.getX() - w2;
        double x2 = p.getX() + w2;
        double y1 = p.getY() - h2;
        double y2 = p.getY() + h2;

        if (x2 < X1)
        {
            vis = false;
        }
        else if (y2 < Y1)
        {
            vis = false;
        }
        else if (x1 > X2)
        {
            vis = false;
        }
        else if (y1 > Y2)
        {
            vis = false;
        }

        return vis;
    }

    private boolean isSpriteIn(final GraphicSprite sprite, final double X1, final double Y1, final double X2, final double Y2)
    {
        if (sprite.isAttachedToNode())
        {
            return false;
        }
        if (sprite.isAttachedToEdge() && !isEdgeVisible(sprite.getEdgeAttachment()))
        {
            return false;
        }

        Values size = sprite.getStyle().getSize();
        double w2 = metrics.lengthToPx(size, 0) / 2;
        double h2 = size.size() > 1 ? metrics.lengthToPx(size, 1) / 2 : w2;

        Point2D src = spritePositionPx(sprite);
        if (null == src)
        {
            return false;
        }

        double x1 = src.getX() - w2;
        double x2 = src.getX() + w2;
        double y1 = src.getY() - h2;
        double y2 = src.getY() + h2;

        if (x2 < X1)
        {
            return false;
        }
        if (y2 < Y1)
        {
            return false;
        }
        if (x1 > X2)
        {
            return false;
        }
        if (y1 > Y2)
        {
            return false;
        }

        return true;
    }

    private Point2D spritePositionPx(GraphicSprite sprite)
    {
        return getSpritePosition(sprite, Units.PX);
    }

    private boolean nodeContains(GraphicElement elt, double x, double y)
    {
        Values size = elt.getStyle().getSize();
        double w2 = metrics.lengthToPx(size, 0) / 2;
        double h2 = size.size() > 1 ? metrics.lengthToPx(size, 1) / 2 : w2;

        javafx.geometry.Point2D dst = Tx.transform(elt.getX(), elt.getY());

        dst.subtract(this.metrics.getViewportX(), this.metrics.getViewportY());

        double x1 = dst.getX() - w2;
        double x2 = dst.getX() + w2;
        double y1 = dst.getY() - h2;
        double y2 = dst.getY() + h2;

        if (x < x1)
        {
            return false;
        }
        if (y < y1)
        {
            return false;
        }
        if (x > x2)
        {
            return false;
        }
        if (y > y2)
        {
            return false;
        }

        return true;
    }

    private boolean edgeContains(GraphicElement elt, double x, double y)
    {
        return false;
    }

    private boolean spriteContains(final GraphicElement elt, final double x, final double y)
    {
        final Values size = elt.getStyle().getSize();
        final double w2 = metrics.lengthToPx(size, 0) / 2;
        final double h2 = size.size() > 1 ? metrics.lengthToPx(size, 1) / 2 : w2;
        final Point2D dst = spritePositionPx((GraphicSprite) elt);
        if (null == dst)
        {
            return false;
        }

        final double x1 = dst.getX() - this.metrics.getViewportX() - w2;
        final double x2 = dst.getX() - this.metrics.getViewportX() + w2;
        final double y1 = dst.getY() - this.metrics.getViewportY() - h2;
        final double y2 = dst.getY() - this.metrics.getViewportY() + h2;

        if (x < x1)
        {
            return false;
        }
        if (y < y1)
        {
            return false;
        }
        if (x > x2)
        {
            return false;
        }
        if (y > y2)
        {
            return false;
        }

        return true;
    }

    /**
     * Compute the position of a sprite if it is not attached.
     *
     * @param sprite The sprite.
     * @param units The units the computed position must be given into.
     * @return The same instance as pos, or a new one if pos was null.
     */
    private Point2D getSpritePositionFree(GraphicSprite sprite, Units units)
    {
        if (sprite.getUnits() == units)
        {
            return new Point2D(sprite.getX(), sprite.getY());
        }

        if (Units.GU.equals(units) && Units.PX.equals(sprite.getUnits()))
        {
            return xT.transform(sprite.getX(), sprite.getY());
        }

        if (Units.PX.equals(units) && Units.GU.equals(sprite.getUnits()))
        {
            return xT.transform(sprite.getX(), sprite.getY());
        }

        if (Units.GU.equals(units) && Units.PERCENTS.equals(sprite.getUnits()))
        {
            final double x = metrics.lo.x + (sprite.getX() / 100d) * metrics.graphWidthGU();
            final double y = metrics.lo.y + (sprite.getY() / 100d) * metrics.graphHeightGU();
            return new Point2D(x, y);
        }

        if (Units.PX.equals(units) && Units.PERCENTS.equals(sprite.getUnits()))
        {
            final double x = (sprite.getX() / 100d) * this.metrics.getViewportWidth();
            final double y = (sprite.getY() / 100d) * this.metrics.getViewportHeight();
            return new Point2D(x, y);
        }

        return null;
    }

    /**
     * Compute the position of a sprite if attached to a node.
     *
     * @param sprite The sprite.
     * @param units The units the computed position must be given into.
     * @return The same instance as pos, or a new one if pos was null.
     */
    private Point2D getSpritePositionNode(GraphicSprite sprite, Units units)
    {
        GraphicNode node = sprite.getNodeAttachment();

        double radius = metrics.lengthToGu(sprite.getX(), sprite.getUnits());
        double z = sprite.getZ() * Math.PI / 180d;

        double x = node.x + (Math.cos(z) * radius);
        double y = node.y + (Math.sin(z) * radius);

        if (Units.PX.equals(units))
        {
            return Tx.transform(x, y);
        }
        else
        {
            return new Point2D(x, y);
        }
    }

    /**
     * Compute the position of a sprite if attached to an edge.
     *
     * @param sprite The sprite.
     * @param units The units the computed position must be given into.
     * @return The same instance as pos, or a new one if pos was null.
     */
    private Point2D getSpritePositionEdge(GraphicSprite sprite, Units units)
    {
        GraphicEdge edge = sprite.getEdgeAttachment();
        if (null == edge)
        {
            return null;
        }

        if (edge.isCurve())
        {
            double ctrl[] = edge.getControlPoints();
            Point2 p0 = new Point2(edge.from.getX(), edge.from.getY());
            Point2 p1 = new Point2(ctrl[0], ctrl[1]);
            Point2 p2 = new Point2(ctrl[1], ctrl[2]);
            Point2 p3 = new Point2(edge.to.getX(), edge.to.getY());
            Vector2 perp = CubicCurve.perpendicular(p0, p1, p2, p3, sprite.getX());
            double length = metrics.lengthToGu(sprite.getY(), sprite.getUnits());

            perp.normalize();
            perp.scalarMult(length);

            double x = CubicCurve.eval(p0.x, p1.x, p2.x, p3.x, sprite.getX()) - perp.data[0];
            double y = CubicCurve.eval(p0.y, p1.y, p2.y, p3.y, sprite.getX()) - perp.data[1];
            return new Point2D(x, y);
        }
        else
        {
            double x = ((GraphicNode) edge.getSourceNode()).x;
            double y = ((GraphicNode) edge.getSourceNode()).y;
            double dx = ((GraphicNode) edge.getTargetNode()).x - x;
            double dy = ((GraphicNode) edge.getTargetNode()).y - y;
            double d = sprite.getX();
            double o = metrics.lengthToGu(sprite.getY(), sprite.getUnits());

            d = d > 1 ? 1 : d;
            d = d < 0 ? 0 : d;

            x += dx * d;
            y += dy * d;

            d = Math.sqrt(dx * dx + dy * dy);
            dx /= d;
            dy /= d;

            x += -dy * o;
            y += dx * o;

            if (units == Units.PX)
            {
                return Tx.transform(x, y);
            }
            else
            {
                return new Point2D(x, y);
            }
        }
    }
}
