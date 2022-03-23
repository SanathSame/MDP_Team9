package mdp.group9.tasks;

import mdp.group9.*;
import mdp.group9.connection.RPIConnector;
import mdp.group9.simulator.SimulatorController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageRecTask {
    /*
    Command formats:
    - ROBOT XX YY D (x-coord, y-coord, direction [0-3 NSEW]
    - OBS 1 XX YY D || OBS 2 X Y D || ...
    - IMG TAKEPICTURE 1
    - STM ZZZ F XXXX (ZZZ - counter) [14 characters!]
    */

    /* COMMAND HEADERS */
    public static final String START_IMG_TASK = "START_IMG";
    public static final String START_PARKING_TASK = "START_FASTEST";
    public static final String NEXT_PATH = "NEXT";
    public static final String OBSTACLE = "OBS";
    public static final String COMPLETE = "COMPLETE";
    public static final String TAKE_PIC = "TAKEPICTURE";
    public static final String IMG = "IMG";
    public static final String STM = "STM";
    public static final String ANDROID = "AND";
    public static final String NO_IMG = "NOIMAGE";

    private RPIConnector connector;
    private PathPlanner pathPlanner;
    private AStar aStar;
    private MotionPlanner motionPlanner;
    private Obstacle[] obstacles;
    private Position[] imageReadingPositions;
    private int[] orderOfExploration;
    private int[][] grid;
    private int stmCmdCounter; // each stm command needs a unique id
    private int obstaclesVisited;
    private int lastObs = 0;
    private int numBacks = 0;
    private boolean taskComplete = false;

    public static boolean CORRECTION = false;

    public ImageRecTask() {
        pathPlanner = new PathPlanner();
        aStar = new AStar();
        motionPlanner = new MotionPlanner();
        connector = new RPIConnector();
        stmCmdCounter = 0;
        obstaclesVisited = 0;
    }

    public void start() {
        if (connector.connect()) {
            System.out.println("Connection successful");
        } else {
            System.out.println("Connection failed");
            return;
        }

        while (!taskComplete) {
            try {
                String msg = connector.receive().split("\u0000")[0].strip();
                if (msg == null) {
                    continue;
                }

                switch (msg.split(" ")[0]) { // command header
                    case START_IMG_TASK -> startImgTask();
                    case OBSTACLE -> prepareArena(msg);
                    case NEXT_PATH -> nextPath();
                    case NO_IMG -> handleNoImg();
                    case COMPLETE -> {
                        connector.disconnect();
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception occurred - disconnecting");
                connector.disconnect();
                return;
            }
        }
        System.out.println("Task ended");
    }

    private void startImgTask() {
        Position start = new Position(1, 1, Direction.NORTH);
        while (pathPlanner.bestSkip[orderOfExploration[obstaclesVisited]]) {
            obstaclesVisited++; // skip this obstacle
        }
        sendPath(start, imageReadingPositions[orderOfExploration[obstaclesVisited]],
                obstacles[orderOfExploration[obstaclesVisited]].getId());
    }

    private void nextPath(String msg) {
        if (CORRECTION) {
            // assume msg sent is "NEXT X" where X = region where image is
            // 0-3 (left-right)
            if (obstaclesVisited != obstacles.length) {
                int imgRegion = Integer.parseInt(msg.split(" ")[1]);
                Position start = getProbableEndPosition(imgRegion);
                sendPath(start, imageReadingPositions[orderOfExploration[obstaclesVisited]],
                        obstacles[orderOfExploration[obstaclesVisited]].getId());
            }
        }
        else {
            nextPath();
        }
    }

    private void nextPath() {
        if (obstaclesVisited != obstacles.length) {
            while (pathPlanner.bestSkip[orderOfExploration[obstaclesVisited]]) {
                obstaclesVisited++; // skip this obstacle
            }
            int lastVisited = obstaclesVisited - 1;
            while (lastVisited >= 0 && pathPlanner.bestSkip[orderOfExploration[lastVisited]]) {
                lastVisited--;
            }
            sendPath(imageReadingPositions[orderOfExploration[lastVisited]],
                    imageReadingPositions[orderOfExploration[obstaclesVisited]],
                    obstacles[orderOfExploration[obstaclesVisited]].getId());
        } else {
            connector.send(String.format("%s %-3d %s\n", STM, stmCmdCounter, "S     "));
            taskComplete = true;
        }
    }

    private Position getProbableEndPosition(int imgRegion) {
        Position idealEndPosition = imageReadingPositions[orderOfExploration[obstaclesVisited]];
        // 0-4 (left-right)
        int offset;
        switch (imgRegion) {
            case 0 -> offset = 1;
            case 4 -> offset = -1;
            default -> {
                return idealEndPosition;
            }
        }

        switch (idealEndPosition.getDir()) {
            case NORTH -> {
                return new Position(idealEndPosition.getX() + offset, idealEndPosition.getY(), idealEndPosition.getDir());
            }
            case SOUTH -> {
                return new Position(idealEndPosition.getX() - offset, idealEndPosition.getY(), idealEndPosition.getDir());
            }
            case EAST -> {
                return new Position(idealEndPosition.getX(), idealEndPosition.getY() + offset, idealEndPosition.getDir());
            }
            case WEST -> {
                return new Position(idealEndPosition.getX(), idealEndPosition.getY() - offset, idealEndPosition.getDir());
            }
        }

        return idealEndPosition;
    }

    private void prepareArena(String msg) {
        // create obstacles
        List<Obstacle> obstaclesList = new ArrayList<>();
        String[] obstaclesReceived = msg.split(" \\|\\| ");
        for (String obstacle : obstaclesReceived) {
            String[] obstacleParts = obstacle.split(" ");
            System.out.println("obstacleParts: " + Arrays.toString(obstacleParts));
            Direction dir = Direction.NORTH;
            switch (obstacleParts[4]) {
                case "0" -> dir = Direction.NORTH;
                case "1" -> dir = Direction.EAST;
                case "2" -> dir = Direction.SOUTH;
                case "3" -> dir = Direction.WEST;
            }
            obstaclesList.add(new Obstacle(
                    Integer.parseInt(obstacleParts[1]), // id
                    Integer.parseInt(obstacleParts[2]) - 1, // x
                    Integer.parseInt(obstacleParts[3]) - 1, // y
                    dir
            ));
        }
        obstacles = obstaclesList.toArray(new Obstacle[0]);

        // set grid for A*
        grid = new Arena(new RobotCar(), obstacles).getGrid();
        pathPlanner.setGrid(grid);

        // get image reading positions and find exploration order
        imageReadingPositions = new Position[obstacles.length];
        for (int i = 0; i < obstacles.length; i++) {
            imageReadingPositions[i] = pathPlanner.getTargetPosition(obstacles[i]);
        }
        orderOfExploration = pathPlanner.findExplorationOrder(imageReadingPositions, true); // returns indexes
        System.out.print("Order of exploration: ");
        for (int imagePosIndex : orderOfExploration) {
            System.out.print("[" + imageReadingPositions[imagePosIndex].getX() + "," + imageReadingPositions[imagePosIndex].getY() + "] ");
        }
        System.out.println();
    }

    private void sendPath(Position start, Position end, int obsId) {
        System.out.println("Going to Obstacle " + obsId);
        if (numBacks >= 2) {
            int distanceToBack = (numBacks - 1) * 10;
            connector.send(String.format("%s %-3d F %-4d\n", STM, stmCmdCounter, distanceToBack));
            stmCmdCounter++;
        }
        numBacks = 0;

        List<AStar.Cell> path = aStar.modifiedSearch(grid, start, end, false);
        List<String> commands = motionPlanner.toCommands(path);
        for (int i = 0; i < commands.size(); i++) {
            connector.send(String.format("%s %-3d %s\n", STM, stmCmdCounter, commands.get(i)));
            stmCmdCounter++;
        }

        // wait for RPI to report robot stopped
        while (true) {
            String message = connector.receive().strip();
            if (message.equals("Done " + (stmCmdCounter - 1))) {
                System.out.println("Robot stopped, send take picture");
                connector.send(String.format("%s %s %s\n", ImageRecTask.IMG,
                        ImageRecTask.TAKE_PIC, obsId));
                break;
            }
        }

        obstaclesVisited++;
    }

    public void handleNoImg() {
        connector.send(String.format("%s %-3d %s\n", STM, stmCmdCounter, "B 10  "));
        stmCmdCounter++;
        while (true) {
            String message = connector.receive().strip();
            if (message.equals("Done " + (stmCmdCounter - 1))) {
                System.out.println("Robot stopped, send take picture");
                connector.send(String.format("%s %s %s\n", ImageRecTask.IMG,
                        ImageRecTask.TAKE_PIC, obstacles[orderOfExploration[obstaclesVisited-1]].getId()));
                break;
            }
        }
        if (numBacks >= 3) { // max perform 3 backs only
            nextPath();
        } else {
            numBacks++;
        }
    }

    public void disconnect() {
        connector.disconnect();
    }

    public static void main(String[] args) throws Exception {
        ImageRecTask task = new ImageRecTask();
        task.start();
    }
}
