package mdp.group9.ReedsShepp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReedsSheppTest {

    public static void main(String[] args) {
        List<Point> points = new ArrayList<>();
        points.add(new Point(1,1,90));
        points.add(new Point(3,14,90));
        points.add(new Point(11,14,180));
        points.add(new Point(5,11,0));
        points.add(new Point(15,12,270));
        points.add(new Point(17,16,180));


        List<List<PathElement>> fullPath = new ArrayList<>();
        double totalLength = 0;

        ReedsShepp rs = new ReedsShepp();
        for (int i = 0; i < points.size() - 1; i++) {
            List<PathElement> path = rs.getOptimalPath(points.get(i), points.get(i+1));
            fullPath.add(path);
            totalLength += rs.pathLength(path);
        }

        System.out.println("Shortest path length: " + totalLength);

        for (List<PathElement> path : fullPath) {
            for (PathElement e : path) {
                System.out.println(e);
            }
            System.out.println();
        }
    }
}
