package mdp.group9.ReedsShepp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReedsShepp {

    public double pathLength(List<PathElement> path) {
        double sum = 0;
        for (PathElement e : path) {
            sum += e.param;
        }
        return sum;
    }

    /**
     * Return the shortest path from start to end among those that exist
     * @param start
     * @param end
     * @return
     */
    public List<PathElement> getOptimalPath(Point start, Point end) {
        List<List<PathElement>> paths = getAllPaths(start, end);

        Comparator<List<PathElement>> sumComparator = new Comparator<List<PathElement>>() {
            @Override
            public int compare(List<PathElement> o1, List<PathElement> o2) {
                return Double.compare(pathLength(o1), pathLength(o2));
            }
        };

        return Collections.min(paths, sumComparator);
    }

    /**
     * Return a list of all the paths from start to end generated by the
     * 12 functions and their variants
     * @param start
     * @param end
     * @return
     */
    public List<List<PathElement>> getAllPaths(Point start, Point end) {
        List<List<PathElement>> paths = new ArrayList<>();

        Point point = Utils.changeOfBasis(start, end);
        double x = point.x;
        double y = point.y;
        double theta = point.theta;

        /* get the four variants for each path type, cf article */
        // path 1
        paths.add(path1(x, y, theta));
        paths.add(timeflip(path1(-x, y, -theta)));
        paths.add(reflect(path1(x, -y, -theta)));
        paths.add(reflect(timeflip(path1(-x, -y, theta))));

        // path 2
        paths.add(path2(x, y, theta));
        paths.add(timeflip(path2(-x, y, -theta)));
        paths.add(reflect(path2(x, -y, -theta)));
        paths.add(reflect(timeflip(path2(-x, -y, theta))));

        // path 3
        paths.add(path3(x, y, theta));
        paths.add(timeflip(path3(-x, y, -theta)));
        paths.add(reflect(path3(x, -y, -theta)));
        paths.add(reflect(timeflip(path3(-x, -y, theta))));

        // path 4
        paths.add(path4(x, y, theta));
        paths.add(timeflip(path4(-x, y, -theta)));
        paths.add(reflect(path4(x, -y, -theta)));
        paths.add(reflect(timeflip(path4(-x, -y, theta))));

        // path 5
        paths.add(path5(x, y, theta));
        paths.add(timeflip(path5(-x, y, -theta)));
        paths.add(reflect(path5(x, -y, -theta)));
        paths.add(reflect(timeflip(path5(-x, -y, theta))));

        // path 6
        paths.add(path6(x, y, theta));
        paths.add(timeflip(path6(-x, y, -theta)));
        paths.add(reflect(path6(x, -y, -theta)));
        paths.add(reflect(timeflip(path6(-x, -y, theta))));

        // path 7
        paths.add(path7(x, y, theta));
        paths.add(timeflip(path7(-x, y, -theta)));
        paths.add(reflect(path7(x, -y, -theta)));
        paths.add(reflect(timeflip(path7(-x, -y, theta))));

        // path 8
        paths.add(path8(x, y, theta));
        paths.add(timeflip(path8(-x, y, -theta)));
        paths.add(reflect(path8(x, -y, -theta)));
        paths.add(reflect(timeflip(path8(-x, -y, theta))));

        // path 9
        paths.add(path9(x, y, theta));
        paths.add(timeflip(path9(-x, y, -theta)));
        paths.add(reflect(path9(x, -y, -theta)));
        paths.add(reflect(timeflip(path9(-x, -y, theta))));

        // path 10
        paths.add(path10(x, y, theta));
        paths.add(timeflip(path10(-x, y, -theta)));
        paths.add(reflect(path10(x, -y, -theta)));
        paths.add(reflect(timeflip(path10(-x, -y, theta))));

        // path 11
        paths.add(path11(x, y, theta));
        paths.add(timeflip(path11(-x, y, -theta)));
        paths.add(reflect(path11(x, -y, -theta)));
        paths.add(reflect(timeflip(path11(-x, -y, theta))));

        // path 12
        paths.add(path12(x, y, theta));
        paths.add(timeflip(path12(-x, y, -theta)));
        paths.add(reflect(path12(x, -y, -theta)));
        paths.add(reflect(timeflip(path12(-x, -y, theta))));

        // remove path elements that have parameter 0
        for (List<PathElement> path : paths) {
            path.removeIf(e -> e.param == 0);
        }

        // remove empty paths
        paths.removeIf(List::isEmpty);

        return paths;
    }

    /**
     * timeflip transform described around the end of the article
     * @param path
     * @return
     */
    public List<PathElement> timeflip(List<PathElement> path) {
//        List<PathElement> flipped = new ArrayList<>(path);
//        for (PathElement e : flipped) {
//            e.reverseGear();
//        }
//        return flipped;
        for (PathElement e : path) {
            e.reverseGear();
        }
        return path;
    }

    /**
     * reflect transform described around the end of the article
     * @param path
     */
    public List<PathElement> reflect(List<PathElement> path) {
//        List<PathElement> reversed = new ArrayList<>(path);
//        for (PathElement e : reversed) {
//            e.reverseSteering();
//        }
//        return reversed;
        for (PathElement e : path) {
            e.reverseSteering();
        }
        return path;
    }

    /**
     * Formula 8.1: CSC (same turns)
     * @return
     */
    public List<PathElement> path1(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double[] R_result = Utils.R(x - Math.sin(phi), y - 1 + Math.cos(phi));
        double u = R_result[0];
        double t = R_result[1];
        double v = Utils.M(phi - t);

        path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
        path.add(new PathElement(u, Steering.STRAIGHT, Gear.FORWARD));
        path.add(new PathElement(v, Steering.LEFT, Gear.FORWARD));

        return path;
    }

    /**
     * Formula 8.2: CSC (opposite turns)
     * @return
     */
    public List<PathElement> path2(double x, double y, double phi) {
        phi = Utils.M(Utils.deg2rad(phi));
        List<PathElement> path = new ArrayList<>();

        double[] R_result = Utils.R(x + Math.sin(phi), y - 1 - Math.cos(phi));
        double rho = R_result[0];
        double t1 = R_result[1];

        if (rho * rho >= 4) {
            double u = Math.sqrt(rho * rho - 4);
            double t = Utils.M(t1 + Math.atan2(2, u));
            double v = Utils.M(t - phi);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.STRAIGHT, Gear.FORWARD));
            path.add(new PathElement(v, Steering.RIGHT, Gear.FORWARD));
        }

        return path;
    }

    /**
     * Formula 8.3: C|C|C
     * @return
     */
    public List<PathElement> path3(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x - Math.sin(phi);
        double eta = y - 1 + Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho <= 4) {
            double A = Math.acos(rho / 4);
            double u = Utils.M(Math.PI - 2*A);
            double t = Utils.M(theta + Math.PI/2 + A);
            double v = Utils.M(phi - t - u);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.RIGHT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.LEFT, Gear.FORWARD));
        }

        return path;
    }

    /**
     * Formula 8.4 (1): C|CC
     * @return
     */
    public List<PathElement> path4(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x - Math.sin(phi);
        double eta = y - 1 + Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho <= 4) {
            double A = Math.acos(rho / 4);
            double u = Utils.M(Math.PI - 2*A);
            double t = Utils.M(theta + Math.PI/2 + A);
            double v = Utils.M(t + u - phi);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.RIGHT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.LEFT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.4 (2): CC|C
     * @return
     */
    public List<PathElement> path5(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x - Math.sin(phi);
        double eta = y - 1 + Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho <= 4) {
            double u = Math.acos(1 - rho * rho / 8);
            double A = Math.asin(2 * Math.sin(u) / rho);
            double t = Utils.M(theta + Math.PI/2 - A);
            double v = Utils.M(t - u - phi);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.RIGHT, Gear.FORWARD));
            path.add(new PathElement(v, Steering.LEFT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.7: CCu|CuC
     * @return
     */
    public List<PathElement> path6(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x + Math.sin(phi);
        double eta = y - 1 - Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho <= 4) {
            double A, u, t, v;
            if (rho <= 2) {
                A = Math.acos((rho + 2) / 4);
                u = Utils.M(A);
                t = Utils.M(theta + Math.PI / 2 + A);
                v = Utils.M(phi - t + 2 * u);
            } else {
                A = Math.acos((rho - 2) / 4);
                u = Utils.M(Math.PI - A);
                t = Utils.M(theta + Math.PI / 2 - A);
                v = Utils.M(phi - t + 2 * u);
            }

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.RIGHT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.LEFT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.RIGHT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.8: C|CuCu|C
     * @return
     */
    public List<PathElement> path7(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x + Math.sin(phi);
        double eta = y - 1 - Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];
        double u1 = (20 - rho * rho) / 16;

        if (rho <= 6 && 0 <= u1 && u1 <= 1) {
            double u = Math.acos(u1);
            double A = Math.asin(2 * Math.sin(u) / rho);
            double t = Utils.M(theta + Math.PI / 2 + A);
            double v = Utils.M(t - phi);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.RIGHT, Gear.BACKWARD));
            path.add(new PathElement(u, Steering.LEFT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.RIGHT, Gear.FORWARD));
        }

        return path;
    }

    /**
     * Formula 8.9 (1): C|C[pi/2]SC
     * @return
     */
    public List<PathElement> path8(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x - Math.sin(phi);
        double eta = y - 1 + Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho >= 2) {
            double u = Math.sqrt(rho * rho - 4) - 2;
            double A = Math.atan2(2, u + 2);
            double t = Utils.M(theta + Math.PI / 2 + A);
            double v = Utils.M(t - phi + Math.PI / 2);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(Math.PI/2, Steering.RIGHT, Gear.BACKWARD));
            path.add(new PathElement(u, Steering.STRAIGHT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.LEFT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.9 (2): CSC[pi/2]|C
     * @return
     */
    public List<PathElement> path9(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x - Math.sin(phi);
        double eta = y - 1 + Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho >= 2) {
            double u = Math.sqrt(rho * rho - 4) - 2;
            double A = Math.atan2(u + 2, 2);
            double t = Utils.M(theta + Math.PI / 2 - A);
            double v = Utils.M(t - phi - Math.PI / 2);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.STRAIGHT, Gear.FORWARD));
            path.add(new PathElement(Math.PI/2, Steering.RIGHT, Gear.FORWARD));
            path.add(new PathElement(v, Steering.LEFT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.10 (1): C|C[pi/2]SC
     * @return
     */
    public List<PathElement> path10(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x + Math.sin(phi);
        double eta = y - 1 - Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho >= 2) {
            double t = Utils.M(theta + Math.PI / 2);
            double u = rho - 2;
            double v = Utils.M(phi - t - Math.PI / 2);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(Math.PI/2, Steering.RIGHT, Gear.BACKWARD));
            path.add(new PathElement(u, Steering.STRAIGHT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.RIGHT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.10 (2): CSC[pi/2]|C
     * @return
     */
    public List<PathElement> path11(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x + Math.sin(phi);
        double eta = y - 1 - Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho >= 2) {
            double t = Utils.M(theta);
            double u = rho - 2;
            double v = Utils.M(phi - t - Math.PI / 2);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(u, Steering.STRAIGHT, Gear.FORWARD));
            path.add(new PathElement(Math.PI/2, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(v, Steering.RIGHT, Gear.BACKWARD));
        }

        return path;
    }

    /**
     * Formula 8.11: C|C[pi/2]SC[pi/2]|C
     * @return
     */
    public List<PathElement> path12(double x, double y, double phi) {
        phi = Utils.deg2rad(phi);
        List<PathElement> path = new ArrayList<>();

        double xi = x + Math.sin(phi);
        double eta = y - 1 - Math.cos(phi);
        double[] R_result = Utils.R(xi, eta);
        double rho = R_result[0];
        double theta = R_result[1];

        if (rho >= 4) {
            double u = Math.sqrt(rho * rho - 4) - 4;
            double A = Math.atan2(2, u + 4);
            double t = Utils.M(theta + Math.PI / 2 + A);
            double v = Utils.M(t - phi);

            path.add(new PathElement(t, Steering.LEFT, Gear.FORWARD));
            path.add(new PathElement(Math.PI/2, Steering.RIGHT, Gear.BACKWARD));
            path.add(new PathElement(u, Steering.STRAIGHT, Gear.BACKWARD));
            path.add(new PathElement(Math.PI/2, Steering.LEFT, Gear.BACKWARD));
            path.add(new PathElement(v, Steering.RIGHT, Gear.FORWARD));
        }

        return path;
    }
}
