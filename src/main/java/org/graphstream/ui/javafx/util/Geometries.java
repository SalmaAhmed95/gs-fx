package org.graphstream.ui.javafx.util;


import javafx.geometry.Point2D;

import java.awt.geom.Line2D;

/**
 * common geometry helpers
 * <p>
 * User: bowen
 * Date: 9/22/14
 */
public class Geometries
{
    public static Point2D intersection(final Line2D l1, final Line2D l2)
    {
        return intersection(
            new Point2D(l1.getX1(), l1.getY1()), new Point2D(l1.getX2(), l1.getY2()),
            new Point2D(l2.getX1(), l2.getY1()), new Point2D(l2.getX2(), l2.getY2()));
    }


    public static Point2D intersection(final Point2D p1, final Point2D p2, final Point2D p3, final Point2D p4)
    {
        // calculate differences
        double xD1 = p2.getX() - p1.getX();
        double xD2 = p4.getX() - p3.getX();
        double yD1 = p2.getY() - p1.getY();
        double yD2 = p4.getY() - p3.getY();
        double xD3 = p1.getX() - p3.getX();
        double yD3 = p1.getY() - p3.getY();

        // calculate the lengths of the two lines
        double len1 = Math.sqrt(xD1 * xD1 + yD1 * yD1);
        double len2 = Math.sqrt(xD2 * xD2 + yD2 * yD2);

        // calculate angle between the two lines.
        double dot = (xD1 * xD2 + yD1 * yD2); // dot product
        double deg = dot / (len1 * len2);

        // if abs(angle)==1 then the lines are parallel so no intersection is possible
        if (Approximations.approximatelyEquals(Math.abs(deg), 1d, .00000001d))
        {
            return null;
        }

        // find intersection Pt between two lines
        double div = yD2 * xD1 - xD2 * yD1;
        double ua = (xD2 * yD3 - yD2 * xD3) / div;
        double ub = (xD1 * yD3 - yD1 * xD3) / div;
        Point2D pt = new Point2D(p1.getX() + ua * xD1, p1.getY() + ua * yD1);

        // calculate the combined length of the two segments
        // between Pt-p1 and Pt-p2
        xD1 = pt.getX() - p1.getX();
        xD2 = pt.getX() - p2.getX();
        yD1 = pt.getY() - p1.getY();
        yD2 = pt.getY() - p2.getY();
        double segmentLen1 = Math.sqrt(xD1 * xD1 + yD1 * yD1) + Math.sqrt(xD2 * xD2 + yD2 * yD2);

        // calculate the combined length of the two segments
        // between Pt-p3 and Pt-p4
        xD1 = pt.getX() - p3.getX();
        xD2 = pt.getX() - p4.getX();
        yD1 = pt.getY() - p3.getY();
        yD2 = pt.getY() - p4.getY();
        double segmentLen2 = Math.sqrt(xD1 * xD1 + yD1 * yD1) + Math.sqrt(xD2 * xD2 + yD2 * yD2);

        // if the lengths of both sets of segments are the same as
        // the lengths of the two lines the point is actually on the line segment

        // if the point isn’t on the line, return null
        if (Math.abs(len1 - segmentLen1) > 0.01 || Math.abs(len2 - segmentLen2) > 0.01)
        {
            return null;
        }

        // return the valid intersection
        return pt;
    }


    private Geometries()
    {

    }
}
