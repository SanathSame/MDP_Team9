package mdp.group9;

import java.util.ArrayList;
import java.util.List;

public class Arena {
    public static final int WIDTH = 20, HEIGHT = 20;
    private Obstacle[] obstacles;
    private RobotCar car;
    private List<Position> imageReadingPositions;
    private int[][] grid; // 0 -> empty, 1 -> blocked, 2 -> car, 3 -> target position
    public static int NUM_OBSTACLES = 5; // assume default is 5, but can vary

    public Arena(RobotCar car, Obstacle[] obs) {
        int row, col;

        grid = new int[WIDTH][HEIGHT];
        this.car = car;
        obstacles = obs;
        imageReadingPositions = new ArrayList<Position>();

//        grid[HEIGHT - 1 - car.getPos().getY()][car.getPos().getX()] = 2;

        for (col = 0; col < WIDTH; col++) // put virtual wall on top and bottom rows
        {
            grid[0][col] = 2;
            grid[HEIGHT - 1][col] = 2;
        }
        for (row = 1; row < HEIGHT - 1; row++) // put virtual wall on left and right columns
        {
            grid[row][0] = 2;
            grid[row][WIDTH - 1] = 2;
        }

        for (Obstacle ob : obs)
        {
            row = HEIGHT - 1 - ob.getPos().getY();
            col = ob.getPos().getX();

            int maxRow = Math.min(HEIGHT - 1, row + 1); // to prevent out-of-bounds
            int maxCol = Math.min(WIDTH - 1, col + 1); // to prevent out-of-bounds
            for (int r = Math.max(0, row - 1); r <= maxRow; r++) // mark virtual obstacle around actual obstacle
            {
                for (int c = Math.max(0, col - 1); c <= maxCol; c++)
                {
                    grid[r][c] = 1; // put obstacle or virtual obstacle
                }
            }
            Position targetPos = getTargetPosition(ob);
//            if (grid[HEIGHT - 1 - targetPos.getY()][targetPos.getX()] == 1) {
//                System.out.println("Image reading position is blocked / at boundary!");
//            }
            imageReadingPositions.add(targetPos); // add target position for current obstacle
            grid[HEIGHT - 1 - targetPos.getY()][targetPos.getX()] = 3;
        }

        NUM_OBSTACLES = obs.length;
    }

    /**
     * Gets the corresponding target position to read a particular obstacle's image
     * @param ob The obstacle whose image is to be read
     * @return The target position
     */
    private Position getTargetPosition(Obstacle ob) {
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

    public RobotCar getCar() {
        return car;
    }

    public Obstacle[] getObstacles() {
        return obstacles;
    }

    public int[][] getGrid() {
        return grid;
    }

    public List<Position> getImageReadingPositions() {
        return imageReadingPositions;
    }
}
