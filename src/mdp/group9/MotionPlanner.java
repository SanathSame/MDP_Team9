package mdp.group9;

import java.util.ArrayList;
import java.util.List;

public class MotionPlanner {
    /*
    W S - control motor: drive rear wheel, Motor pwm <= 4000 but 0-1000 super slow on ground
    A D - servo (front): left 60, right 86, center 73
    Stop motor using C or 0 pwm value

    format:
    letter(space)value
    */

    /**
     * Converts path from PathPlanner to a set of commands (in String)
     * directing how robot should move
     * @param path
     */
    public void pathToMotionCommands(List<AStar.Cell> path) {
        List<String> commands = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            AStar.Cell cell = path.get(i); // next cell to move to

            String command = "";
            switch (cell.dir) { // indicates direction car faces at this cell
                case NORTH -> command += "W";
                case SOUTH -> command += "S";
                case EAST -> command += "D";
                case WEST -> command += "A";
            }

            if (i > 0 && path.get(i-1).dir != cell.dir) {
                // rotation needed

            }

        }
    }

    private List<AStar.Cell> combinePaths(List<List<AStar.Cell>> path) {
        List<AStar.Cell> overallPath = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) { // assume starting position always at (1,1)
            List<AStar.Cell> localPath = path.get(i);
            localPath.remove(0);
            overallPath.addAll(localPath);
        }

        // for visualization
        System.out.println("\nOverall path:");
        String pathStr = "";
        for (AStar.Cell cell : overallPath) {
            pathStr += "[" + cell.x + "," + cell.y + "," + cell.dir + "]" + " => ";
        }
        pathStr = pathStr.substring(0, pathStr.length() - 4);
        System.out.println(pathStr);

        return overallPath;
    }

    public static void main(String[] args) {

    }
}
