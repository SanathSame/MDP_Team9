package mdp.group9;

import javafx.geometry.Pos;

import java.util.*;
public class Main {

    public static void main(String[] args) {
	// write your code here
        testArena();

    }

    private static void testArena() {
        Obstacle[] obstacles = new Obstacle[Obstacle.NUM_OBSTACLES];
        obstacles[0] = new Obstacle(2, 17, Direction.SOUTH);
        obstacles[1] = new Obstacle(6, 13, Direction.EAST);
        obstacles[2] = new Obstacle(9, 10, Direction.WEST);
        obstacles[3] = new Obstacle(12, 15, Direction.EAST);
        obstacles[4] = new Obstacle(14, 7, Direction.NORTH);

        Arena arena = new Arena(new RobotCar(), obstacles);
        int[][] grid = arena.getGrid();

        AStar aStar = new AStar();
        List<AStar.Cell> slnPath;
        Position testStart = new Position(2, 13, Direction.NORTH);
        //Position testEnd = new Position(16, 15, Direction.WEST);
        Position testEnd = new Position(10, 13, Direction.WEST);
        slnPath = aStar.modifiedSearch(grid, testStart, testEnd);
        int r, c;
        for (AStar.Cell cell : slnPath)
        {
            r = Arena.HEIGHT - 1 - cell.y;
            c = cell.x;
            grid[r][c] = 9;

        }

        for (r = 0; r < Arena.WIDTH; r++)
        {
            for (c = 0; c < Arena.HEIGHT; c++)
            {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }

        for (AStar.Cell cell : slnPath)
        {
            System.out.printf("[x: %d, y: %d, dir: %s]\n", cell.x, cell.y, cell.dir);

        }

    }
}
