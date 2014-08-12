package org.graphstream.ui.javafx;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a javafx view animation timer
 * <p/>
 * User: bowen
 * Date: 8/10/14
 */
public class ViewTimer extends AnimationTimer
{
    private static final Logger logger = Logger.getLogger(ViewTimer.class.getSimpleName());

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private final AtomicBoolean active = new AtomicBoolean(false);

    private final Object sync = new Object();

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
        final Canvas view = this.renderer.getCanvas();
        if (null == view)
        {
            return;
        }

        // clear dirty flag
        this.dirty.getAndSet(false);

        // render graph
        synchronized (this.sync)
        {
            final double x = view.getLayoutX();
            final double y = view.getLayoutY();
            final double w = Math.max(0, view.getWidth());
            final double h = Math.max(0, view.getHeight());
            this.renderer.render(view.getGraphicsContext2D(), x, y, w, h);
            if (logger.isLoggable(Level.FINE))
            {
                logger.fine("Rendered frame in " + (System.nanoTime() - l) / 1000f + " msecs.");
            }
        }

        // if we haven't been set to repaint we can stop
        if (!this.dirty.get())
        {
            this.stop();
        }
    }
}
