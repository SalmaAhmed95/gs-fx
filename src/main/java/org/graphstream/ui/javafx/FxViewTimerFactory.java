/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphstream.ui.javafx;

import javafx.animation.AnimationTimer;
import org.graphstream.ui.view.ViewTimer;
import org.graphstream.ui.view.ViewTimerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a javafx timer factory
 *
 * @author trajar
 */
public class FxViewTimerFactory implements ViewTimerFactory
{
    private static final Logger logger = LoggerFactory.getLogger(FxViewTimerFactory.class);

    @Override
    public ViewTimer create(int delayMs, Runnable worker)
    {
        final TimerImpl timer = new TimerImpl(delayMs, worker);
        timer.start();
        return timer;
    }

    @Override
    public void close()
    {
        // nothing to do
    }

    private static class TimerImpl extends AnimationTimer implements ViewTimer
    {
        private final Runnable worker;

        private final int delayMs;

        private long lastTimeMs = 0;

        public TimerImpl(final int delayMs, final Runnable r)
        {
            if (null == r)
            {
                throw new IllegalArgumentException("Runnable worker cannot be null.");
            }
            this.delayMs = delayMs;
            this.worker = r;
        }

        @Override
        public void handle(final long l)
        {
            final long nowMs = System.currentTimeMillis();
            final long sinceMs = nowMs - this.lastTimeMs;
            if (sinceMs < this.delayMs)
            {
                return;
            }

            try
            {
                this.worker.run();
                this.lastTimeMs = System.currentTimeMillis();
            }
            catch (final Exception e)
            {
                logger.warn("Unable to update graph frame.", e);
            }
        }
    }
}
