package org.graphstream.ui.javafx;

import java.util.Collection;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import org.graphstream.graph.Element;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.StyleGroupListener;
import org.graphstream.ui.javafx.renderer.FxGraphRenderer;
import org.graphstream.ui.swingViewer.GraphRenderer;
import org.graphstream.ui.swingViewer.LayerRenderer;
import org.graphstream.ui.view.Camera;
import org.jfree.fx.FXGraphics2D;

/**
 * a javafx graph renderer
 * <p>
 * User: bowen Date: 7/29/14
 */
public class ViewRenderer implements StyleGroupListener
{
    private final GraphRenderer delegate;
    
    private GraphicGraph graph;
    
    private Canvas canvas;
    
    private final ViewTimer timer;
    
    public ViewRenderer(final GraphRenderer delegate)
    {
        if (null == delegate)
        {
            throw new IllegalArgumentException("Delegate cannot be null.");
        }
        this.delegate = delegate;
        this.timer = new ViewTimer(this);
    }
    
    public Canvas getCanvas()
    {
        return this.canvas;
    }
    
    public void open(final GraphicGraph graph, final Canvas canvas)
    {
        if (null == graph)
        {
            throw new IllegalArgumentException("Graph cannot be null.");
        }
        if (null == canvas)
        {
            throw new IllegalArgumentException("Canvas cannot be null.");
        }
        this.graph = graph;
        this.graph.getStyleGroups().addListener(this);
        this.delegate.open(graph, null);
        this.canvas = canvas;
    }
    
    public void close()
    {
        if (this.graph != null)
        {
            this.graph.getStyleGroups().removeListener(this);
            this.graph = null;
        }
        this.delegate.close();
        this.timer.stop();
        this.canvas = null;
    }
    
    public void repaint()
    {
        this.timer.repaint();
    }
    
    public Camera getCamera()
    {
        return this.delegate.getCamera();
    }
    
    public Collection<GraphicElement> allNodesOrSpritesIn(double x1, double y1, double x2, double y2)
    {
        return this.delegate.allNodesOrSpritesIn(x1, y1, x2, y2);
    }
    
    public GraphicElement findNodeOrSpriteAt(double x, double y)
    {
        return this.delegate.findNodeOrSpriteAt(x, y);
    }
    
    public void moveElementAtPx(GraphicElement element, double x, double y)
    {
        this.delegate.moveElementAtPx(element, x, y);
    }
    
    public void render(final GraphicsContext ctx, final double x, final double y, final double width, final double height)
    {
        if (null == this.graph)
        {
            return;
        }
        if (this.graph.getNodeCount() <= 0)
        {
            return;
        }
        if (this.delegate instanceof FxGraphRenderer)
        {
            final FxGraphRenderer fx = (FxGraphRenderer) this.delegate;
            fx.render(ctx, x, y, width, height);
        }
        else
        {
            this.delegate.render(new FXGraphics2D(this.canvas.getGraphicsContext2D()), (int) x, (int) y, (int) width, (int) height);
        }
    }
    
    public void beginSelectionAt(double x1, double y1)
    {
        this.delegate.beginSelectionAt(x1, y1);
    }
    
    public void selectionGrowsAt(double x, double y)
    {
        this.delegate.selectionGrowsAt(x, y);
    }
    
    public void endSelectionAt(double x2, double y2)
    {
        this.delegate.endSelectionAt(x2, y2);
    }
    
    public void setBackLayerRenderer(final LayerRenderer renderer)
    {
        this.delegate.setBackLayerRenderer(renderer);
        this.repaint();
    }
    
    public void setForeLayoutRenderer(final LayerRenderer renderer)
    {
        this.delegate.setForeLayoutRenderer(renderer);
        this.repaint();
    }
    
    @Override
    public void elementStyleChanged(Element element, StyleGroup oldStyle, StyleGroup style)
    {
        this.repaint();
    }
}
