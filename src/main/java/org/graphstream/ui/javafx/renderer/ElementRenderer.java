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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import org.graphstream.graph.Element;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.StyleGroup.ElementEvents;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.javafx.util.DefaultCamera;
import org.graphstream.ui.javafx.util.FontCache;
import org.graphstream.ui.javafx.util.SwingUtils;
import org.graphstream.ui.view.Camera;

import java.awt.geom.PathIterator;


public abstract class ElementRenderer
{
    private Font textFont;

    private Color textColor;

    private int textSize;


    public ElementRenderer()
    {

    }


    public final void render(StyleGroup group, GraphicsContext g, Camera camera)
    {
        setupRenderingPass(group, g, camera);
        pushStyle(group, g, camera);

        for (Element e : group.bulkElements())
        {
            GraphicElement ge = (GraphicElement) e;

            if (camera.isVisible(ge))
            {
                renderElement(group, g, camera, ge);
            }
            else
            {
                elementInvisible(group, g, camera, ge);
            }
        }

        if (group.hasDynamicElements())
        {
            for (Element e : group.dynamicElements())
            {
                GraphicElement ge = (GraphicElement) e;

                if (camera.isVisible(ge))
                {
                    if (!group.elementHasEvents(ge))
                    {
                        pushDynStyle(group, g, camera, ge);
                        renderElement(group, g, camera, ge);
                    }
                }
                else
                {
                    elementInvisible(group, g, camera, ge);
                }
            }
        }

        if (group.hasEventElements())
        {
            for (ElementEvents event : group.elementsEvents())
            {
                GraphicElement ge = (GraphicElement) event.getElement();

                if (camera.isVisible(ge))
                {
                    event.activate();
                    pushStyle(group, g, camera);
                    renderElement(group, g, camera, ge);
                    event.deactivate();
                }
                else
                {
                    elementInvisible(group, g, camera, ge);
                }
            }
        }
    }


    protected abstract void pushStyle(StyleGroup group, GraphicsContext g, Camera camera);


    protected abstract void renderElement(StyleGroup group, GraphicsContext g, Camera camera, GraphicElement element);


    protected abstract void elementInvisible(StyleGroup group, GraphicsContext g, Camera camera, GraphicElement element);


    protected void setupRenderingPass(StyleGroup group, GraphicsContext g, Camera camera)
    {
        this.pushTextStyle(group, g);
        this.pushFillStyle(group, g);
        this.pushStrokeStyle(group, g);
    }


    protected void pushDynStyle(StyleGroup group, GraphicsContext g, Camera camera, GraphicElement element)
    {
        Color fill = SwingUtils.fromAwt(group.getFillColor(0));
        if (element != null && StyleConstants.FillMode.DYN_PLAIN.equals(group.getFillMode()))
        {
            fill = interpolateColor(group, element);
        }
        if (null == fill)
        {
            fill = Color.BLACK;
        }
        g.setFill(fill);
    }


    protected final void pushTextStyle(final StyleGroup group, final GraphicsContext g)
    {
        String fontName = group.getTextFont();
        StyleConstants.TextStyle textStyle = group.getTextStyle();
        if (null == textStyle)
        {
            textStyle = StyleConstants.TextStyle.NORMAL;
        }

        this.textSize = group.getTextSize().intValue();
        this.textColor = SwingUtils.fromAwt(group.getTextColor(0));
        if (null == this.textColor)
        {
            this.textColor = Color.BLACK;
        }

        this.textFont = FontCache.defaultFontCache().getFont(fontName, textStyle, this.textSize);
        if (null == this.textFont)
        {
            this.textFont = FontCache.defaultFontCache().getDefaultFont(textStyle, this.textSize);
        }

        g.setFont(this.textFont);
    }


    protected final void pushStrokeStyle(final StyleGroup group, final GraphicsContext g)
    {
        Color stroke = SwingUtils.fromAwt(group.getStrokeColor(0));
        if (null == stroke)
        {
            stroke = Color.BLACK;
        }
        g.setStroke(stroke);

        Value value = group.getStrokeWidth();
        if (null == value)
        {
            g.setLineWidth(1d);
        }
        else
        {
            g.setLineWidth(value.doubleValue());
        }
    }


    protected final void strokeShake(final java.awt.Shape s, final GraphicsContext g)
    {
        final double[] coords = new double[6];
        g.beginPath();
        PathIterator iterator = s.getPathIterator(null);
        while (!iterator.isDone())
        {
            int segType = iterator.currentSegment(coords);
            switch (segType)
            {
                case PathIterator.SEG_MOVETO:
                    g.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    g.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    g.quadraticCurveTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    g.bezierCurveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    g.closePath();
                    break;
                default:
                    throw new IllegalStateException("Unrecognised segment type " + segType + ".");
            }
            iterator.next();
        }
    }


    protected final void pushFillStyle(final StyleGroup group, final GraphicsContext g)
    {
        Color color = SwingUtils.fromAwt(group.getFillColor(0));
        if (null == color)
        {
            color = Color.BLACK;
        }
        g.setFill(color);
    }


    protected final void renderText(final StyleGroup group, final GraphicsContext g, final Camera camera, final GraphicElement element)
    {
        if (StyleConstants.TextMode.HIDDEN.equals(group.getTextMode()))
        {
            return;
        }
        if (StyleConstants.TextVisibilityMode.HIDDEN.equals(group.getTextVisibilityMode()))
        {
            return;
        }

        final String label = element.getLabel();
        if (null == label || label.isEmpty())
        {
            return;
        }

        final Point3 p;
        Point2D pos = null;
        GraphicSprite s = null;

        if (element instanceof GraphicSprite && camera instanceof DefaultCamera)
        {
            s = (GraphicSprite) element;
            pos = ((DefaultCamera) camera).getSpritePosition(s, Units.GU);
        }

        if (pos != null && s.getUnits() == Units.PX)
        {
            double w = camera.getMetrics().lengthToPx(group.getSize(), 0);
            p = camera.transformGuToPx(pos.getX(), pos.getY(), 0);
            p.x += w / 2;
        }
        else if (pos != null && s != null && s.getUnits() == Units.PERCENTS)
        {
            double w = camera.getMetrics().lengthToPx(group.getSize(), 0);
            p = camera.transformGuToPx(camera.getMetrics().viewport[2] * pos.getX(), camera.getMetrics().viewport[3] * pos.getY(), 0);
            p.x += (w / 2);
        }
        else
        {
            double w = camera.getMetrics().lengthToGu(group.getSize(), 0);
            p = camera.transformGuToPx(element.getX() + (w / 2), element.getY(), 0);
        }

        Affine pop = g.getTransform();
        Paint c = g.getStroke();

        g.setStroke(this.textColor);
        g.setFont(this.textFont);
        g.setTransform(new Affine());
        g.strokeText(label, p.x, p.y + this.textSize / 3d);

        g.setTransform(pop);
        g.setStroke(c);
    }


    private Color interpolateColor(StyleGroup group, GraphicElement element)
    {
        final int n = group.getFillColorCount();

        if (n <= 0)
        {
            if (element.hasAttribute("ui.color", Color.class))
            {
                return SwingUtils.fromAwt(element.getAttribute("ui.color"));
            }
            else
            {
                return SwingUtils.fromAwt(group.getFillColor(0));
            }
        }

        if (element.hasNumber("ui.color") && n > 1)
        {
            float value = element.getFloat("ui.color");

            if (value < 0)
            {
                value = 0;
            }
            else if (value > 1)
            {
                value = 1;
            }

            if (value == 1)
            {
                return SwingUtils.fromAwt(group.getFillColor(n - 1));
            }
            if (value <= 0)
            {
                return SwingUtils.fromAwt(group.getFillColor(0));
            }

            double div = 1d / (n - 1);
            int col = (int) (value / div);
            div = (value - (div * col)) / div;
            Color color0 = SwingUtils.fromAwt(group.getFillColor(col));
            Color color1 = SwingUtils.fromAwt(group.getFillColor(col + 1));

            double red = ((color0.getRed() * (1 - div)) + (color1.getRed() * div));
            double green = ((color0.getGreen() * (1 - div)) + (color1.getGreen() * div));
            double blue = ((color0.getBlue() * (1 - div)) + (color1.getBlue() * div));
            double alpha = ((color0.getOpacity() * (1 - div)) + (color1.getOpacity() * div));

            return new Color(red, green, blue, alpha);
        }
        else if (element.hasAttribute("ui.color", Color.class))
        {
            return SwingUtils.fromAwt(element.getAttribute("ui.color"));
        }
        else
        {
            return SwingUtils.fromAwt(group.getFillColor(0));
        }
    }
}