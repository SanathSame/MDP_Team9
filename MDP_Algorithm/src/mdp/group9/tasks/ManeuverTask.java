package mdp.group9.tasks;

import mdp.group9.*;
import mdp.group9.connection.RPIConnector;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Not used
 */
public class ManeuverTask {
    public static int START_X = 10;
    public static int START_Y = 6;
    public static int OBS_X = 10;
    public static int OBS_Y = 10;

    public static void commandsMethod() {
        RPIConnector connector = new RPIConnector();
        if (connector.connect()) {
            System.out.println("Connection successful");
        } else {
            System.out.println("Connection failed");
            return;
        }

        int obsCount = 1;
        boolean foundImg = false;

        int stmCmdCounter = 0;
        String[] singleTurn = new String[]{
                "B 25  ", "RF 45 ", "F 12  ", "LF 50 ", "F 58  ", "RB 101", "F 10  " //"F 35  "  // F 15 TR | F 25 Tiles
        };

        // wait for STM to reach obstacle
        while (true) {
            String message = connector.receive().split("\u0000")[0];
            System.out.println("[Server]: " + message);
            if (message.equals("CONTINUE")) {
                break;
            }
        }

        for (int i = 0; i < 3; i++) {
            if (foundImg) break;

            for (String s : singleTurn) {
                connector.send(String.format("%s %-3d %s\n", ImageRecTask.STM, stmCmdCounter, s));
                System.out.print("  [Algo]: " + String.format("%s %-3d %s\n", ImageRecTask.STM, stmCmdCounter, s));
                stmCmdCounter++;
            }

            // wait for RPI to report robot stopped
            while (true) {
                String message = connector.receive().split("\u0000")[0];
                System.out.println("[Server]: " + message);
                if (message.equals("Done " + (stmCmdCounter - 1))) {
                    System.out.println("Robot stopped, send next take picture");
                    connector.send(String.format("%s %s %s\n", ImageRecTask.IMG,
                            ImageRecTask.TAKE_PIC, obsCount));
                    System.out.print("  [Algo]: " + String.format("%s %s %s\n", ImageRecTask.IMG,
                            ImageRecTask.TAKE_PIC, obsCount));
                    obsCount++;
                    break;
                }
            }

            // wait for IMG to finish
            while (true) {
                String message = connector.receive();
                System.out.println("[Server]: " + message);
                if (message.equals("CONTINUE")) { // bullseye found
                    break;
                } else if (message.equals("STOP")) { // found image
                    foundImg = true;
                    connector.send(String.format("%s %-3d %s\n", ImageRecTask.STM, stmCmdCounter, "C     "));
                    System.out.println("  [Algo]: " + String.format("%s %-3d %s\n", ImageRecTask.STM, stmCmdCounter, "C     "));
                    break;
                }
            }
        }

        System.out.println("Function completed");
        connector.disconnect();
    }

    public static void aStarMethodNoRPI() throws Exception {
        MotionPlanner motionPlanner = new MotionPlanner();

        Obstacle[] obstacles = new Obstacle[3];
        obstacles[0] = new Obstacle(OBS_X - 1, OBS_Y - 1, Direction.EAST);
        obstacles[1] = new Obstacle(OBS_X - 1, OBS_Y - 1, Direction.NORTH);
        obstacles[2] = new Obstacle(OBS_X - 1, OBS_Y - 1, Direction.WEST);

        Arena arena = new Arena(new RobotCar(), obstacles);
        int[][] grid = arena.getGrid();

        PathPlanner pathPlanner = new PathPlanner();
        pathPlanner.setGrid(grid);
        pathPlanner.setStartingPos(START_X - 1, START_Y - 1, Direction.NORTH);

        int stmCmdCounter = 0;

        List<List<AStar.Cell>> paths = pathPlanner.planPath(obstacles);
        for (List<AStar.Cell> path : paths) {

            String pathStr = "";
            for (AStar.Cell cell : path) {
                pathStr += "[" + cell.x + "," + cell.y + ", " + cell.dir + "]" + " => ";
            }
            pathStr = pathStr.substring(0, pathStr.length() - 4);
            System.out.println(pathStr);

            // send to STM
            List<String> commands = motionPlanner.toCommands(path);
            for (String command : commands) {
                System.out.print(String.format("%s %-3d %s", ImageRecTask.STM, stmCmdCounter, command));
                System.out.println("|");
                stmCmdCounter++;
            }
        }
    }

    public static void aStarMethod() throws Exception {
        RPIConnector connector = new RPIConnector();
        if (connector.connect()) {
            System.out.println("Connection successful");
        } else {
            System.out.println("Connection failed");
            return;
        }
        MotionPlanner motionPlanner = new MotionPlanner();

        Obstacle[] obstacles = new Obstacle[3];
        obstacles[0] = new Obstacle(OBS_X - 1, OBS_Y - 1, Direction.EAST);
        obstacles[1] = new Obstacle(OBS_X - 1, OBS_Y - 1, Direction.NORTH);
        obstacles[2] = new Obstacle(OBS_X - 1, OBS_Y - 1, Direction.WEST);

        Arena arena = new Arena(new RobotCar(), obstacles);
        int[][] grid = arena.getGrid();

        PathPlanner pathPlanner = new PathPlanner();
        pathPlanner.setGrid(grid);
        pathPlanner.setStartingPos(START_X - 1, START_Y - 1, Direction.NORTH);
//        AStar aStar = new AStar();
//
//        Position start = new Position(START_X - 1, START_Y - 1, Direction.NORTH);
//        Position end = null;
        int stmCmdCounter = 0;
        int obsCount = 1; // just for sending to IMG
        boolean foundImg = false;

        // wait for STM to reach obstacle
        while (true) {
            String message = connector.receive().split("\u0000")[0];
            System.out.println("[Server]: " + message);
            if (message.equals("CONTINUE")) {
                System.out.println("Robot reached obstacle");
                break;
            }
        }
        System.out.println("Did it break?");
        List<List<AStar.Cell>> paths = pathPlanner.planPath(obstacles);
        System.out.println("Paths: " + paths.size());
        for (List<AStar.Cell> path : paths) {
            if (foundImg) break;

            String pathStr = "";
            for (AStar.Cell cell : path) {
                pathStr += "[" + cell.x + "," + cell.y + ", " + cell.dir + "]" + " => ";
            }
            pathStr = pathStr.substring(0, pathStr.length() - 4);
            System.out.println(pathStr);

            // send to STM
            List<String> commands = motionPlanner.toCommands(path);
            for (String command : commands) {
                connector.send(String.format("%s %-3d %s\n", ImageRecTask.STM, stmCmdCounter, command));
                System.out.print(String.format("%s %-3d %s", ImageRecTask.STM, stmCmdCounter, command));
                System.out.println("|");
                stmCmdCounter++;

            }

            // wait for RPI to report robot stopped
            while (true) {
                String message = connector.receive().split("\u0000")[0];
                System.out.println(message);
                if (message.equals("Done " + (stmCmdCounter - 1))) {
                    System.out.println("Robot stopped, send next take picture");
                    connector.send(String.format("%s %s %s\n", ImageRecTask.IMG,
                            ImageRecTask.TAKE_PIC, obsCount));
                    obsCount++;
                    break;
                }
            }

            // wait for IMG to finish
            while (true) {
                String message = connector.receive();
                System.out.println("[Server]: " + message);
                if (message.equals("CONTINUE")) { // bullseye found
                    break;
                } else if (message.equals("STOP")) { // found image
                    foundImg = true;
                    break;
                }
            }
        }

//        for (Obstacle obs : obstacles) {
//            if (foundImg) break;
//
//            end = pathPlanner.getTargetPosition(obs);
//            List<AStar.Cell> path = aStar.modifiedSearch(grid, start, end);
//            start = end;
//
//            String pathStr = "";
//            for (AStar.Cell cell : path) {
//                pathStr += "[" + cell.x + "," + cell.y + ", " + cell.dir + "]" + " => ";
//            }
//            pathStr = pathStr.substring(0, pathStr.length() - 4);
//            System.out.println(pathStr);
//
//            // send to STM
//            List<String> commands = motionPlanner.toCommands(path);
//            for (String command : commands) {
//                connector.send(String.format("%s %-3d %s\n", ImageRecTask.STM, stmCmdCounter, command));
//                System.out.print(String.format("%s %-3d %s", ImageRecTask.STM, stmCmdCounter, command));
//                System.out.println("|");
//                stmCmdCounter++;
//
//            }
//
//            // wait for RPI to report robot stopped
//            while (true) {
//                String message = connector.receive().split("\u0000")[0];
//                System.out.println(message);
//                if (message.equals("Done " + (stmCmdCounter - 1))) {
//                    System.out.println("Robot stopped, send next take picture");
//                    connector.send(String.format("%s %s %s\n", ImageRecTask.IMG,
//                            ImageRecTask.TAKE_PIC, obsCount));
//                    obsCount++;
//                    break;
//                }
//            }
//
//            // wait for IMG to finish
//            while (true) {
//                String message = connector.receive();
//                System.out.println("[Server]: " + message);
//                if (message.equals("CONTINUE")) { // bullseye found
//                    break;
//                } else if (message.equals("STOP")) { // found image
//                    foundImg = true;
//                    break;
//                }
//            }
//        }

        connector.disconnect();
    }

    public static void main(String[] args) throws Exception {
        commandsMethod();
    }
}
