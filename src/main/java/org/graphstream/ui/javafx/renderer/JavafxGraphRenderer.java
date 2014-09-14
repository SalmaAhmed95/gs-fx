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

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import org.graphstream.graph.Element;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.StyleGroupListener;
import org.graphstream.ui.graphicGraph.StyleGroupSet;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.FillMode;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.javafx.util.DefaultCamera;
import org.graphstream.ui.javafx.util.SwingUtils;
import org.graphstream.ui.swingViewer.GraphRenderer;
import org.graphstream.ui.swingViewer.LayerRenderer;
import org.graphstream.ui.swingViewer.util.GraphMetrics;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.Selection;
import org.jfree.fx.FXGraphics2D;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base graph renderer for JavaFX.
 */
public class JavafxGraphRenderer implements GraphRenderer, StyleGroupListener
{
    private static final Logger logger = Logger.getLogger(JavafxGraphRenderer.class.getName());

    private GraphicGraph graph;

    private Selection selection = null;

    private DefaultCamera camera = null;

    private NodeRenderer nodeRenderer = new NodeRenderer();

    private EdgeRenderer edgeRenderer = new EdgeRenderer();

    private SpriteRenderer spriteRenderer = new SpriteRenderer();

    private LayerRenderer backRenderer = null;

    private LayerRenderer foreRenderer = null;

    private PrintStream fpsLog = null;

    private long T1 = 0;

    private long steps = 0;

    private double sumFps = 0;


    public JavafxGraphRenderer()
    {

    }


    @Override
    public void open(final GraphicGraph graph, Container swing)
    {
        if (this.graph != null)
        {
            throw new IllegalStateException("Renderer already open, cannot open twice.");
        }
        this.graph = graph;
        this.graph.getStyleGroups().addListener(this);
        this.camera = new DefaultCamera(graph);
    }


    @Override
    public void close()
    {
        if (this.fpsLog != null)
        {
            this.fpsLog.flush();
            this.fpsLog.close();
            this.fpsLog = null;
        }

        this.camera = null;

        if (this.graph != null)
        {
            this.graph.getStyleGroups().removeListener(this);
            this.graph = null;
        }
    }


    @Override
    public void beginSelectionAt(double x1, double y1)
    {
        this.selection = new Selection();
        this.selection.x1 = x1;
        this.selection.y1 = y1;
        this.selection.x2 = x1;
        this.selection.y2 = y1;
    }


    @Override
    public void selectionGrowsAt(double x, double y)
    {
        if (this.selection != null)
        {
            this.selection.x2 = x;
            this.selection.y2 = y;
        }
    }


    @Override
    public void endSelectionAt(double x2, double y2)
    {
        this.selection = null;
    }


    @Override
    public Camera getCamera()
    {
        return this.camera;
    }


    @Override
    public Collection<GraphicElement> allNodesOrSpritesIn(double x1, double y1, double x2, double y2)
    {
        return camera.allNodesOrSpritesIn(graph, x1, y1, x2, y2);
    }


    public GraphicElement findNodeOrSpriteAt(double x, double y)
    {
        return camera.findNodeOrSpriteAt(graph, x, y);
    }


    public void render(Graphics2D g, int x, int y, int width, int height)
    {
        throw new UnsupportedOperationException();
    }


    public void render(GraphicsContext g, double x, double y, double width, double height)
    {
        if (null == this.graph)
        {
            return;
        }
        if (null == this.camera)
        {
            return;
        }

        this.beginFrame();

        if (camera.getGraphViewport() == null && camera.getMetrics().diagonal == 0 && (graph.getNodeCount() == 0 && graph.getSpriteCount() == 0))
        {
            displayNothingToDo(g, width, height);
        }
        else
        {
            camera.setPadding(graph);
            camera.setViewport(x, y, width, height);
            renderGraph(g);
            renderSelection(g);
        }

        endFrame();
    }


    @Override
    public void moveElementAtPx(GraphicElement element, double x, double y)
    {
        Point3 p = camera.transformPxToGu(camera.getMetrics().viewport[0] + x, camera.getMetrics().viewport[1] + y);
        element.move(p.x, p.y, element.getZ());
    }


    private void beginFrame()
    {
        if (!this.graph.hasLabel("ui.log"))
        {
            return;
        }

        if (null == this.fpsLog)
        {
            try
            {
                final CharSequence log = this.graph.getLabel("ui.log");
                if (log != null)
                {
                    this.fpsLog = new PrintStream(log.toString());
                }
            }
            catch (final Exception e)
            {
                this.fpsLog = null;
                logger.log(Level.WARNING, "Unable to construct print stream for fps log.", e);
            }
        }

        if (this.fpsLog != null)
        {
            this.T1 = System.currentTimeMillis();
        }
    }


    private void endFrame()
    {
        if (null == this.fpsLog)
        {
            return;
        }
        this.steps += 1;
        long T2 = System.currentTimeMillis();
        long time = T2 - T1;
        double fps = 1000.0 / time;
        this.sumFps += fps;
        this.fpsLog.printf("%.3f   %d   %.3f%n", fps, time, (this.sumFps / this.steps));
    }


    private void renderGraph(final GraphicsContext g)
    {
        g.setTransform(new Affine());
        final Graphics2D delegate = new FXGraphics2D(g);
        setupGraphics(delegate);
        renderGraphBackground(g);
        renderBackLayer(new FXGraphics2D(g));
        try
        {
            this.camera.pushView(this.graph, g);
            renderGraphElements(g);
            final StyleGroup style = this.graph.getStyle();
            if (style.getStrokeMode() != StyleConstants.StrokeMode.NONE && style.getStrokeWidth().value > 0)
            {
                GraphMetrics metrics = this.camera.getMetrics();
                double px1 = metrics.px1;
                Value stroke = style.getShadowWidth();
                g.setStroke(SwingUtils.fromAwt(this.graph.getStyle().getStrokeColor(0)));
                g.setLineWidth(metrics.lengthToGu(stroke));
                g.strokeRect(metrics.lo.x, metrics.lo.y + px1, metrics.size.data[0] - px1, metrics.size.data[1] - px1);
            }
        }
        catch (final Exception e)
        {
            logger.log(Level.WARNING, "Unexpected error during graph render.", e);
        }
        renderForeLayer(new FXGraphics2D(g));
    }


    protected void setupGraphics(final Graphics2D g)
    {
        if (graph.hasAttribute("ui.antialias"))
        {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        }
        else
        {
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_DEFAULT);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        if (graph.hasAttribute("ui.quality"))
        {
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        }
        else
        {
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        }
    }


    protected void renderGraphBackground(final GraphicsContext g)
    {
        final StyleGroup group = graph.getStyle();
        final double w = camera.getMetrics().viewport[2] + 1;
        final double h = camera.getMetrics().viewport[3] + 1;
        if (!FillMode.NONE.equals(group.getFillMode()))
        {
            g.setFill(SwingUtils.fromAwt(group.getFillColor(0)));
            g.fillRect(0, 0, w, h);
        }
        else
        {
            g.clearRect(0, 0, w, h);
        }
    }


    protected void renderGraphElements(final GraphicsContext g)
    {
        final StyleGroupSet sgs = graph.getStyleGroups();
        if (null == sgs)
        {
            return;
        }
        for (final Collection<StyleGroup> groups : sgs.zIndex())
        {
            for (final StyleGroup group : groups)
            {
                renderGroup(g, group);
            }
        }
    }


    private void renderGroup(final GraphicsContext g, final StyleGroup group)
    {
        switch (group.getType())
        {
            case NODE:
                nodeRenderer.render(group, g, camera);
                break;
            case EDGE:
                edgeRenderer.render(group, g, camera);
                break;
            case SPRITE:
                spriteRenderer.render(group, g, camera);
                break;
            default:
                break;
        }
    }


    private void renderSelection(final GraphicsContext g)
    {
        if (null == this.selection)
        {
            return;
        }

        double x1 = selection.x1;
        double y1 = selection.y1;
        double x2 = selection.x2;
        double y2 = selection.y2;

//      double w = camera.getMetrics().getSize().data[0];
//      double h = camera.getMetrics().getSize().data[1];

        if (x1 > x2)
        {
            double t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y1 > y2)
        {
            double t = y1;
            y1 = y2;
            y2 = t;
        }

        g.setLineWidth(1);
        g.setFill(Color.rgb(50, 50, 200, .5d));
        g.fillRect(x1, y1, x2 - x1, y2 - y1);
        g.setStroke(Color.rgb(0, 0, 0, .5d));
        g.strokeRect(x1, y1, x2 - x1, y2 - y1);
    }


    private void renderBackLayer(final Graphics2D g)
    {
        if (null == this.backRenderer)
        {
            return;
        }
        this.renderLayer(this.backRenderer, g);
    }


    private void renderForeLayer(final Graphics2D g)
    {
        if (null == this.foreRenderer)
        {
            return;
        }
        this.renderLayer(this.foreRenderer, g);
    }


    private void renderLayer(final LayerRenderer layer, final Graphics2D g)
    {
        final GraphMetrics metrics = camera.getMetrics();
        this.backRenderer.render(g, graph, metrics.ratioPx2Gu,
            (int) metrics.viewport[2], (int) metrics.viewport[3],
            metrics.loVisible.x, metrics.loVisible.y, metrics.hiVisible.x,
            metrics.hiVisible.y);
    }


    @Override
    public void screenshot(String filename, int width, int height)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void setBackLayerRenderer(LayerRenderer renderer)
    {
        backRenderer = renderer;
    }


    @Override
    public void setForeLayoutRenderer(LayerRenderer renderer)
    {
        foreRenderer = renderer;
    }


    @Override
    public void elementStyleChanged(Element element, StyleGroup oldStyle, StyleGroup style)
    {

    }


    protected void displayNothingToDo(final GraphicsContext g, final double w, final double h)
    {
        String msg1 = "Graph width/height/depth is zero !!";
        String msg2 = "Place components using the 'xyz' attribute.";

        g.setStroke(Color.RED);
        g.strokeLine(0, 0, w, h);
        g.strokeLine(0, h, w, 0);

        double x = w / 2;
        double y = h / 2;

        g.setStroke(Color.BLACK);
        g.strokeText(msg1, x, y);
    }
}