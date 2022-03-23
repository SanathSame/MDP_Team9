package mdp.group9;

import java.util.*;

public class PathPlanner {

    public static final double UNREACHABLE_PENALTY = 1000.0;
    private final AStar aStar;
    private final List<List<AStar.Cell>> path;
    private int[][] grid;
    private int[][] aStarValues; // store aStar cost values for all pairs of src/dest
    private int WIDTH;
    private int HEIGHT;

    private boolean[] skip;
    public boolean[] bestSkip;

    private int startX = 1;
    private int startY = 1;
    private Direction startDir = Direction.NORTH;

    public PathPlanner() {
        aStar = new AStar();
        path = new ArrayList<>();
        skip = new boolean[Arena.NUM_OBSTACLES];
    }

    public void setGrid(int[][] grid) {
        this.grid = grid;
        WIDTH = grid[0].length;
        HEIGHT = grid.length;
    }

    public void setStartingPos(int x, int y, Direction dir) {
        startX = x;
        startY = y;
        startDir = dir;
    }

    public List<List<AStar.Cell>> planPath(Obstacle[] obstacles) throws Exception {
        path.clear();

        // get image reading positions
        Position[] imagesPos = new Position[obstacles.length];
        for (int i = 0; i < obstacles.length; i++) {
            imagesPos[i] = getTargetPosition(obstacles[i]);
        }

        int[] order = findExplorationOrder(imagesPos, true);
        System.out.print("Order of exploration: ");
        for (int imagePosIndex : order) {
            System.out.print("[" + imagesPos[imagePosIndex].getX() + "," + imagesPos[imagePosIndex].getY() + "] ");
        }
        System.out.println();

        List<AStar.Cell> localPath;
        Position currPos = new Position(startX, startY, startDir); // start position
        for (int obstacleIndex : order) {
            if (bestSkip[obstacleIndex])
            {
                continue;
            }
            Position obstaclePos = imagesPos[obstacleIndex];
            localPath = aStar.modifiedSearch(grid, currPos, obstaclePos, false);
            if (localPath == null) {
                throw new Exception("No valid path found!");
            }
            path.add(new ArrayList<>(localPath)); // add to overall path

            currPos = obstaclePos;
        }

        // print out overall path
        System.out.println("\nOverall path:");
        String pathStr = "";
        double overallCost = 0;
        for (List<AStar.Cell> currPath : path) {
            for (AStar.Cell cell : currPath) {
                pathStr += "[" + cell.x + "," + cell.y + ", " + cell.dir + ", " + cell.carState  + "] => ";
            }
            pathStr = pathStr.substring(0, pathStr.length() - 4);
            pathStr += "\n";
            overallCost += currPath.get(currPath.size()-1).g;
        }
        System.out.println(pathStr);
        System.out.println("Overall Cost: " + overallCost);

        return path;
    }

    private void preComputeAstar(Position[] imagesPos) {
        Position from, to;
        for (int r = 0; r <= Arena.NUM_OBSTACLES; r++)
        {
            for (int c = 0; c <= Arena.NUM_OBSTACLES; c++)
            {
                if (r != c)
                {
                    if (r == Arena.NUM_OBSTACLES)
                        from = new Position(startX, startY, startDir);
                    else
                        from = imagesPos[r];
                    if (c == Arena.NUM_OBSTACLES)
                        to = new Position(startX, startY, startDir);
                    else
                        to = imagesPos[c];
                    List<AStar.Cell> temp = aStar.modifiedSearch(grid, from, to, true);
                    if (temp != null)
                        aStarValues[r][c] = temp.get(0).g;
                    else
                        aStarValues[r][c] = -1;
                }
            }
        }
    }

    /**
     * Note that starting position of robot is always at bottom left corner.
     * @param exhaustive true if use exhaustive search, false if use greedy search
     * @return array of indexes indicating order of images to visit
     */
    public int[] findExplorationOrder(Position[] imagesPos, boolean exhaustive) {
        int[] explorationOrder = new int[imagesPos.length];
        double minDistance = Double.MAX_VALUE;
        double orderDistance;
        //Position from, to;

        aStarValues = new int[Arena.NUM_OBSTACLES + 1][Arena.NUM_OBSTACLES + 1];
        preComputeAstar(imagesPos); // precompute a-star values for every pair of positions

        if (exhaustive) {
            Set<Integer> indexes = new HashSet<>();
            for (int i = 0; i < imagesPos.length; i++) {
                indexes.add(i);
            }
            List<int[]> allPermutations = new ArrayList<>();
            permutate(indexes, new Stack<Integer>(), imagesPos.length, allPermutations);
            bestSkip = new boolean[Arena.NUM_OBSTACLES]; // skip array corresponding to best exploration order
            // test all possible permutations
            for (int[] order : allPermutations) {
                // origin to first obstacle, index 5 in aStarValues represents start point
                skip = new boolean[Arena.NUM_OBSTACLES]; // reset skip array for every permutation
                if (aStarValues[Arena.NUM_OBSTACLES][order[0]] == -1)
                {
                    skip[order[0]] = true;
                    orderDistance = UNREACHABLE_PENALTY;
                }
                else
                    orderDistance = aStarValues[Arena.NUM_OBSTACLES][order[0]];

                // from an obstacle to another
                for (int i = 0; i < order.length-1; i++) {

                    if (skip[order[i + 1]]) // if destination is impossible, skip
                    {
                        orderDistance += UNREACHABLE_PENALTY;
                        continue;
                    }

                    int j = i, fromIndex;
                    while (j >= 0 && skip[order[j]]) // if source is impossible, find last possible source
                        j--;
                    if (j < 0) // if no last source, set starting point as source
                    {
                        fromIndex = Arena.NUM_OBSTACLES;
                    }
                    else
                    {
                        fromIndex = order[j];
                    }

                    if (aStarValues[fromIndex][order[i+1]] == -1)
                    {
                        skip[order[i+1]] = true;
                        orderDistance += UNREACHABLE_PENALTY;
                    }
                    else
                        orderDistance += aStarValues[fromIndex][order[i+1]];
                }
                if (orderDistance < minDistance) {
                    explorationOrder = order.clone();
                    minDistance = orderDistance;
                    bestSkip = skip.clone();
                }
            }
            System.out.println("Min overall distance: " + minDistance);
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
                    double neighbourDistance = aStar.modifiedSearch(grid, curr, neighbour, true).get(0).g;
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

        for (int r = 0; r < Arena.WIDTH; r++)
        {
            for (int c = 0; c < Arena.HEIGHT; c++)
            {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }

        pathPlanner.setGrid(grid);
        List<List<AStar.Cell>> path = pathPlanner.planPath(obstacles);

        MotionPlanner motionPlanner = new MotionPlanner();
        //motionPlanner.pathToMotionCommands(path);
    }
}
