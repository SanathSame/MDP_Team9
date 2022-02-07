package mdp.group9.ReedsShepp;

/**
 * Source: https://github.com/nathanlct/reeds-shepp-curves/blob/master/utils.py
 */
public class Utils {

    /**
     * Returns the angle phi = theta mod (2pi) such that -pi <= theta < pi
     * @param theta
     * @return
     */
    public static double M(double theta) {
        theta = theta % (2 * Math.PI);
        if (theta < -Math.PI) {
            return theta + 2 * Math.PI;
        }
        if (theta >= Math.PI) {
            return theta - 2 * Math.PI;
        }
        return theta;
    }

    /**
     * Return the polar coordinates (r, theta) of the point (x, y).
     * @param x
     * @param y
     * @return
     */
    public static double[] R(double x, double y) {
        double r = Math.sqrt(x*x + y*y);
        double theta = Math.atan2(y, x);
        return new double[]{r, theta};
    }

    /**
     * Given p1 = (x1, y1, theta1) and p2 = (x2, y2, theta2) represented in a
     * coordinate system with origin (0, 0) and rotation 0 (in degrees), return
     * the position and rotation of p2 in the coordinate system which origin
     * (x1, y1) and rotation theta1.
     * @param p1
     * @param p2
     * @return
     */
    public static Point changeOfBasis(Point p1, Point p2) {
        double theta1 = deg2rad(p1.theta);
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double new_x = dx * Math.cos(theta1) + dy * Math.sin(theta1);
        double new_y = -dx * Math.sin(theta1) + dy * Math.cos(theta1);
        double new_theta = p2.theta - p1.theta;
        return new Point(new_x, new_y, new_theta);
    }

    public static double rad2deg(double rad) {
        return 180 * rad / Math.PI;
    }

    public static double deg2rad(double deg) {
        return Math.PI * deg / 180;
    }

    public static int sign(double x) {
        return x >= 0 ? 1 : -1;
    }
}
