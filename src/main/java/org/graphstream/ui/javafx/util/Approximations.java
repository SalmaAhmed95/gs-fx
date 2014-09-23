package org.graphstream.ui.javafx.util;

/**
 * collection of approximation utilities for floating-point numbers
 * <p>
 * User: bowen
 * Date: 9/21/14
 */
public class Approximations
{
    public static boolean approximatelyEquals(final double left, final double right)
    {
        return approximatelyEquals(left, right, .0001d);
    }


    public static boolean approximatelyEquals(final double left, final double right, final double within)
    {
        if (left == right)
        {
            return true;
        }
        final double delta = Math.abs(left - right);
        return delta <= within;
    }


    private Approximations()
    {

    }
}
