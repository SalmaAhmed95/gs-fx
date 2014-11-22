package org.graphstream.ui.javafx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;

/**
 * a javafx view animation timer
 * <p>
 * User: bowen Date: 8/10/14
 */
public class ViewTimer extends AnimationTimer
{
    private static final Logger logger = Logger.getLogger(ViewTimer.class.getSimpleName());

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private final AtomicBoolean active = new AtomicBoolean(false);

    private final ViewRenderer renderer;

    ViewTimer(final ViewRenderer renderer)
    {
        if (null == renderer)
        {
            throw new IllegalArgumentException("Renderer cannot be null.");
        }
        this.renderer = renderer;
    }

    public void repaint()
    {
        this.dirty.getAndSet(true);
        this.ensureActive();
    }

    private void ensureActive()
    {
        if (this.active.get())
        {
            return;
        }
        this.start();
    }

    @Override
    public void start()
    {
        this.active.getAndSet(true);
        super.start();
    }

    @Override
    public void stop()
    {
        this.active.getAndSet(false);
        super.stop();
    }

    @Override
    public void handle(final long l)
    {
        try
        {
            this.render();
        }
        catch (final Exception e)
        {
            logger.log(Level.WARNING, "Unable to render graph frame.", e);
        }
        if (logger.isLoggable(Level.FINE))
        {
            logger.fine("Rendered frame in " + (System.nanoTime() - l) / 1000f + " msecs.");
        }
    }

    private void render() throws Exception
    {
        // ensure we are active
        final Canvas view = this.renderer.getCanvas();
        if (null == view)
        {
            return;
        }
        if (!this.active.get())
        {
            return;
        }

        // clear dirty flag
        this.dirty.getAndSet(false);

        // render graph
        final double x = view.getLayoutX();
        final double y = view.getLayoutY();
        final double w = Math.max(0, view.getWidth());
        final double h = Math.max(0, view.getHeight());
        this.renderer.render(view.getGraphicsContext2D(), x, y, w, h);

        // if we haven't been set to repaint we can stop
        if (!this.dirty.get())
        {
            this.stop();
        }
    }
}
