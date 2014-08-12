package org.graphstream.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.javafx.util.SwingUtils;
import org.graphstream.ui.swingViewer.GraphRenderer;
import org.graphstream.ui.view.Camera;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.util.DefaultMouseManager;
import org.graphstream.ui.view.util.DefaultShortcutManager;
import org.graphstream.ui.view.util.MouseManager;
import org.graphstream.ui.view.util.ShortcutManager;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * a javafx graph view
 * <p/>
 * User: bowen
 * Date: 8/2/14
 */
public class ViewNode extends Canvas implements View
{
    private static final int defaultWidth = 300;

    private static final int defaultHeight = 250;

    private final Collection<KeyListener> keyListeners = new CopyOnWriteArraySet<>();

    private final Collection<MouseListener> mouseListeners = new CopyOnWriteArraySet<>();

    private final Collection<MouseMotionListener> motionListeners = new CopyOnWriteArraySet<>();

    private final ViewRenderer renderer;

    private final GraphicGraph graph;

    private ShortcutManager shortcuts;

    private MouseManager mouseClicks;


    public ViewNode(final Viewer viewer)
    {
        this(viewer, Viewer.newGraphRenderer());
    }


    public ViewNode(final Viewer viewer, final GraphRenderer delegate)
    {
        this(UUID.randomUUID().toString(), viewer, delegate);
    }


    public ViewNode(final String identifier, final Viewer viewer, final GraphRenderer delegate)
    {
        super(defaultWidth, defaultHeight);
        if (null == delegate)
        {
            throw new IllegalArgumentException("Renderer cannot be null.");
        }
        if (null == viewer)
        {
            throw new IllegalArgumentException("Viewer cannot be null.");
        }
        if (null == identifier || identifier.isEmpty())
        {
            throw new IllegalArgumentException("Id cannot be null/empty.");
        }
        this.setId(identifier);
        this.graph = viewer.getGraphicGraph();
        this.renderer = new ViewRenderer(delegate);
        this.renderer.open(graph, this);
        this.wireEvents();
        if (null == this.mouseClicks)
        {
            this.setMouseManager(new DefaultMouseManager());
        }
        if (null == this.shortcuts)
        {
            this.setShortcutManager(new DefaultShortcutManager());
        }
    }


    public Application openInApplication()
    {
        return new ApplicationLauncher(this);
    }


    @Override
    public Camera getCamera()
    {
        return this.renderer.getCamera();
    }


    @Override
    public void display(final GraphicGraph graph, final boolean graphChanged)
    {
        this.renderer.repaint();
    }


    @Override
    public void close(final GraphicGraph graph)
    {
        this.renderer.close();

        if (this.shortcuts != null)
        {
            this.removeKeyListener(this.shortcuts);
            this.shortcuts.release();
            this.shortcuts = null;
        }

        if (this.mouseClicks != null)
        {
            this.removeMouseListener(this.mouseClicks);
            this.removeMouseMotionListener(this.mouseClicks);
            this.mouseClicks.release();
            this.mouseClicks = null;
        }
    }


    @Override
    public void beginSelectionAt(double x1, double y1)
    {
        this.renderer.beginSelectionAt(x1, y1);
        this.renderer.repaint();
    }


    @Override
    public void selectionGrowsAt(double x, double y)
    {
        this.renderer.selectionGrowsAt(x, y);
        this.renderer.repaint();
    }


    @Override
    public void endSelectionAt(double x2, double y2)
    {
        this.renderer.endSelectionAt(x2, y2);
        this.renderer.repaint();
    }


    @Override
    public Collection<GraphicElement> allNodesOrSpritesIn(double x1, double y1, double x2, double y2)
    {
        return this.renderer.allNodesOrSpritesIn(x1, y1, x2, y2);
    }


    @Override
    public GraphicElement findNodeOrSpriteAt(double x, double y)
    {
        return this.renderer.findNodeOrSpriteAt(x, y);
    }


    @Override
    public void moveElementAtPx(final GraphicElement element, final double x, final double y)
    {
        // The feedback on the node positions is often off since not needed
        // and generating lots of events. We activate it here since the
        // movement of the node is decided by the viewer. This is one of the
        // only moment when the viewer really moves a node.
        final boolean on = this.graph.feedbackXYZ();
        this.graph.feedbackXYZ(true);
        this.renderer.moveElementAtPx(element, x, y);
        this.graph.feedbackXYZ(on);
    }


    @Override
    public void freezeElement(final GraphicElement element, final boolean frozen)
    {
        if (frozen)
        {
            element.addAttribute("layout.frozen");
        }
        else
        {
            element.removeAttribute("layout.frozen");
        }
    }


    @Override
    public void setMouseManager(final MouseManager manager)
    {
        if (null == manager)
        {
            throw new IllegalArgumentException("Manager cannot be null.");
        }
        if (this.mouseClicks != null)
        {
            this.mouseClicks.release();
            this.mouseClicks = null;
        }
        manager.init(this.graph, this);
        this.mouseClicks = manager;
    }


    @Override
    public void setShortcutManager(ShortcutManager manager)
    {
        if (null == manager)
        {
            throw new IllegalArgumentException("Manager cannot be null.");
        }
        if (this.shortcuts != null)
        {
            this.shortcuts.release();
        }
        manager.init(this.graph, this);
        this.shortcuts = manager;
    }


    @Override
    public void addKeyListener(KeyListener l)
    {
        if (null == l)
        {
            return;
        }
        this.keyListeners.add(l);
    }


    @Override
    public void removeKeyListener(KeyListener l)
    {
        if (null == l)
        {
            return;
        }
        this.keyListeners.remove(l);
    }


    @Override
    public void addMouseListener(MouseListener l)
    {
        if (null == l)
        {
            return;
        }
        this.mouseListeners.add(l);
    }


    @Override
    public void removeMouseListener(MouseListener l)
    {
        if (null == l)
        {
            return;
        }
        this.mouseListeners.remove(l);
    }


    @Override
    public void addMouseMotionListener(MouseMotionListener l)
    {
        if (null == l)
        {
            return;
        }
        this.motionListeners.add(l);
    }


    @Override
    public void removeMouseMotionListener(MouseMotionListener l)
    {
        if (null == l)
        {
            return;
        }
        this.motionListeners.remove(l);
    }


    private void wireEvents()
    {
        this.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            final java.awt.event.KeyEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                keyListeners.forEach(l -> l.keyPressed(awt));
            }
        });
        this.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            final java.awt.event.KeyEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                keyListeners.forEach(l -> l.keyReleased(awt));
            }
        });
        this.addEventHandler(KeyEvent.KEY_TYPED, event -> {
            final java.awt.event.KeyEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                keyListeners.forEach(l -> l.keyTyped(awt));
            }
        });

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                mouseListeners.forEach(l -> l.mouseClicked(awt));
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                mouseListeners.forEach(l -> l.mousePressed(awt));
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                mouseListeners.forEach(l -> l.mouseReleased(awt));
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                mouseListeners.forEach(l -> l.mouseEntered(awt));
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                mouseListeners.forEach(l -> l.mouseExited(awt));
            }
        });

        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                motionListeners.forEach(l -> l.mouseDragged(awt));
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            final java.awt.event.MouseEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                motionListeners.forEach(l -> l.mouseMoved(awt));
            }
        });

        this.addEventFilter(ScrollEvent.SCROLL, event -> {
            final java.awt.event.KeyEvent awt = SwingUtils.toAwt(event);
            if (awt != null)
            {
                keyListeners.forEach(l -> l.keyPressed(awt));
            }
        });
    }


    private static class ApplicationLauncher extends Application
    {
        private ViewNode canvas;


        private ApplicationLauncher(ViewNode canvas)
        {
            super();
            this.canvas = canvas;
        }


        @Override
        public void start(final Stage stage) throws Exception
        {
            Group root = new Group();
            root.getChildren().add(this.canvas);
            stage.setTitle(this.canvas.getId());
            stage.setScene(new Scene(root, defaultWidth, defaultHeight));
            stage.show();
        }


        @Override
        public void stop()
        {
            Platform.exit();
            System.exit(1);
        }
    }
}
