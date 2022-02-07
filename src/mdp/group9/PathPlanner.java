package mdp.group9;

import java.util.*;

public class PathPlanner {

    private final AStar aStar;
    private final List<List<AStar.Cell>> path;
    private int[][] grid;
    private int WIDTH;
    private int HEIGHT;

    public PathPlanner() {
        aStar = new AStar();
        path = new ArrayList<>();
    }

    public void setGrid(int[][] grid) {
        this.grid = grid;
        WIDTH = grid[0].length;
        HEIGHT = grid.length;
    }

    public List<List<AStar.Cell>> planPath(Obstacle[] obstacles) throws Exception {
        path.clear();

        // get image reading positions
        Position[] imagesPos = new Position[obstacles.length];
        for (int i = 0; i < obstacles.length; i++) {
            Position targetPos = getTargetPosition(obstacles[i]);
            imagesPos[i] = targetPos; // add target position for current obstacle
        }

        int[] order = findExplorationOrder(imagesPos, true);
//        System.out.print("Order of exploration: ");
//        for (int imagePosIndex : order) {
//            System.out.print("[" + imagesPos[imagePosIndex].getX() + "," + imagesPos[imagePosIndex].getY() + "] ");
//        }
//        System.out.println();

        List<AStar.Cell> localPath;
        Position currPos = new Position(1, 1, Direction.NORTH); // start position
        for (int obstacleIndex : order) {
            Position obstaclePos = imagesPos[obstacleIndex];
//            System.out.println("\nCurr Pos: [" + currPos.getY() + ", " + currPos.getY() + "]");
//            System.out.println("Goal Pos: [" + obstaclePos.getY() + ", " + obstaclePos.getY() + "]");
            //localPath = aStar.search(grid, currPos, obstaclePos);
            localPath = aStar.modifiedSearch(grid, currPos, obstaclePos);
            path.add(new ArrayList<>(localPath)); // add to overall path

            currPos = obstaclePos;
        }

        // print out overall path
        System.out.println("\nOverall path:");
        String pathStr = "";
        int overallCost = 0;
        for (List<AStar.Cell> currPath : path) {
            for (AStar.Cell cell : currPath) {
                pathStr += "[" + cell.x + "," + cell.y + "]" + " => ";
            }
            pathStr = pathStr.substring(0, pathStr.length() - 4);
            pathStr += "\n";
            overallCost += currPath.size();
        }
        System.out.println(pathStr);
        System.out.println("Overall Cost: " + overallCost);

        return path;
    }

    /**
     * Note that starting position of robot is always at bottom left corner.
     * @param exhaustive true if use exhaustive search, false if use greedy search
     * @return array of indexes indicating order of images to visit
     */
    private int[] findExplorationOrder(Position[] imagesPos, boolean exhaustive) {
        int[] explorationOrder = new int[imagesPos.length];
        double minDistance = Double.MAX_VALUE;

        if (exhaustive) {
            // test all possible permutations
            Set<Integer> indexes = new HashSet<>();
            for (int i = 0; i < imagesPos.length; i++) {
                indexes.add(i);
            }
            List<int[]> allPermutations = new ArrayList<>();
            permutate(indexes, new Stack<Integer>(), imagesPos.length, allPermutations);

            // find permutation with least (approx) distance covered
            for (int[] order : allPermutations) {
                // origin to first obstacle
                double orderDistance = aStar.calculateH(0, 0, imagesPos[order[0]].getX(), imagesPos[order[0]].getY());
                // from an obstacle to another
                for (int i = 0; i < order.length-1; i++) {
                    Position from = imagesPos[order[i]];
                    Position to = imagesPos[order[i+1]];
                    orderDistance += aStar.calculateH(from.getX(), from.getY(), to.getX(), to.getY());
                }
                if (orderDistance < minDistance) {
                    explorationOrder = order;
                    minDistance = orderDistance;
                }
            }
        } else {
            // do greedy search (nearest neighbour)
            boolean[] visited = new boolean[imagesPos.length]; // tracks index of imagesPos that we have visited
            Position curr = new Position(0,0, Direction.NORTH); // start position
            for (int i = 0; i < imagesPos.length; i++) { // i: used to fill explorationOrder array
                // find nearest neighbour from curr position
                double nnDistance = Double.MAX_VALUE;
                int nnIndex = -1; // nearest neighbour
                for (int index = 0; index < imagesPos.length; index++) { // calculate distance to every neighbour
                    if (visited[index]) { continue; }
                    Position neighbour = imagesPos[index];
                    double neighbourDistance = aStar.calculateH(curr.getX(), curr.getY(), neighbour.getX(), neighbour.getY());
                    if (neighbourDistance < nnDistance) {
                        nnDistance = neighbourDistance;
                        nnIndex = index;
                    }
                }
                // found nearest neighbour
                visited[nnIndex] = true;
                explorationOrder[i] = nnIndex;

                // find this neighbour's nearest neighbour
                curr = imagesPos[nnIndex];
            }
        }
        return explorationOrder;
    }

    private void permutate(Set<Integer> items, Stack<Integer> permutation, int size, List<int[]> allPermutations) {
        /**
         * Helper function to find all possible order of exploration for exhaustive search
         */
        if (permutation.size() == size) {
            allPermutations.add(permutation.stream().mapToInt(i -> i).toArray());
        }

        // items available for permutation
        Integer[] availableItems = items.toArray(new Integer[0]);
        for(Integer item : availableItems) {
            // add to current permutation
            permutation.push(item);
            items.remove(item);

            // recursive call to add to current permutation
            permutate(items, permutation, size, allPermutations);

            // return from recursive call, go on to next possible permutation
            items.add(permutation.pop());
        }
    }

    /**
     * Gets the corresponding target position to read a particular obstacle's image
     * @param ob The obstacle whose image is to be read
     * @return The target position
     */
    public Position getTargetPosition(Obstacle ob) {
        switch (ob.getPos().getDir())
        {
            case NORTH:
                return new Position(ob.getPos().getX(), ob.getPos().getY() + 4, Direction.SOUTH);
            case SOUTH:
                return new Position(ob.getPos().getX(), ob.getPos().getY() - 4, Direction.NORTH);
            case EAST:
                return new Position(ob.getPos().getX() + 4, ob.getPos().getY(), Direction.WEST);
            case WEST:
                return new Position(ob.getPos().getX() - 4, ob.getPos().getY(), Direction.EAST);

        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        PathPlanner pathPlanner = new PathPlanner();

        Obstacle[] obstacles = new Obstacle[5];
        obstacles[0] = new Obstacle(2, 17, Direction.SOUTH);
        obstacles[1] = new Obstacle(6, 13, Direction.EAST);
        obstacles[2] = new Obstacle(9, 10, Direction.WEST);
        obstacles[3] = new Obstacle(12, 15, Direction.EAST);
        obstacles[4] = new Obstacle(14, 7, Direction.NORTH);

        Arena arena = new Arena(new RobotCar(), obstacles);
        int[][] grid = arena.getGrid();

//        for (int r = 0; r < Arena.WIDTH; r++)
//        {
//            for (int c = 0; c < Arena.HEIGHT; c++)
//            {
//                System.out.print(grid[r][c] + " ");
//            }
//            System.out.println();
//        }

        pathPlanner.setGrid(grid);
        List<List<AStar.Cell>> path = pathPlanner.planPath(obstacles);

        MotionPlanner motionPlanner = new MotionPlanner();
//        motionPlanner.pathToMotionCommands(path);
    }
}
