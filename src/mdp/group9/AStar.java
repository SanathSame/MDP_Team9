package mdp.group9;

import java.util.*;

public class AStar {
    /**
     * Implements A* Search. To be called consecutively from one image obstacle to another
     */
    public static final int TURN_PENALTY = 2, REVERSE_PENALTY = 10;
    private int HEIGHT;
    private int WIDTH;
    private final List<Cell> open;
    private final List<Cell> closed;
    private final List<Cell> path;

    // "Node" class for building tree
    public class Cell implements Comparable<Cell> {
        public int x;
        public int y;
        public int f;
        public int g;
        public int h;
        public Cell parent;
        public Direction dir; // Added for modified A Star search

        public Cell(int x, int y, int g, int h, Cell parent, Direction d) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.parent = parent;
            this.dir = d;
        }

        @Override
        public int compareTo(Cell that) {
            // compares one Cell with another based on f value
            // needed to sort priority queue
            return this.f - that.f; // EDITED
        }

        @Override
        public boolean equals(Object o) {
            // used to check if two Cells are the same
            // the same Cell may appear multiple times with different g and f value
            // used to update a Cell in the open list with a lower f value
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cell cell = (Cell) o;
            return x == cell.x && y == cell.y && dir == cell.dir;
        }
    }

    public AStar() {
        this.open = new ArrayList<>(); // tracks unexplored nodes
        this.closed = new ArrayList<>(); // tracks explored nodes
        this.path = new ArrayList<>(); // tracks path for a given search call
    }

    // checks if given cell is a valid cell (within grid limits)
    private boolean isInvalid(int x, int y) {
        return x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT;
    }

    // checks if given cell is blocked
    private boolean isBlocked(int[][] grid, int x, int y) {
        return grid[getRowNum(y)][x] == 1; // 1 == blocked
    }

    // checks if destination is reached
    private boolean reachedDst(int srcX, int srcY, int dstX, int dstY) {
        return srcX == dstX && srcY == dstY;
    }

    private boolean reachedDst(int srcX, int srcY, Direction srcD, int dstX, int dstY, Direction dstD) {
        return srcX == dstX && srcY == dstY && srcD == dstD;
    }

    // function to calculate H function, based on Euclidean distance
    public double calculateH(int srcX, int srcY, int dstX, int dstY) {
        double x_diff = srcX - dstX;
        double y_diff = srcY - dstY;
        return Math.sqrt(x_diff * x_diff + y_diff * y_diff);
    }

    public int heuristic(int srcX, int srcY, int dstX, int dstY, Direction srcD, Direction dstD) {
        return Math.abs(dstX - srcX) + Math.abs(dstY - srcY); // Return Manhattan heuristic distance (for now)
    }

    public int heuristic(Position src, Position dst) {
        return Math.abs(dst.x - src.x) + Math.abs(dst.y - src.y); // Return Manhattan heuristic distance (for now)
    }

    private List<Cell> getNeighbours(Cell cell, Position goal) {
        List<Cell> neighbours = new ArrayList<>();
        Cell temp;

        switch (cell.dir) {
            case NORTH:
                if (cell.y < Arena.HEIGHT - 1) // forward
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + 1, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1) // turn right
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + TURN_PENALTY, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // turn left
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0) // reverse
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + REVERSE_PENALTY, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT && cell.x < Arena.WIDTH - 1) // turn forward-right
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT && cell.x > 0) // turn forward-left
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0 && cell.x < Arena.WIDTH - 1) // reverse right
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0 && cell.x > 0) // reverse left
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case SOUTH:
                if (cell.y > 0) // forward
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + 1, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // reverse
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + REVERSE_PENALTY, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1) // turn left
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + TURN_PENALTY, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // turn right
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0 && cell.x > 0) // forward right
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0 && cell.x < Arena.WIDTH - 1) // forward left
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1 && cell.x > 0) // reverse right
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1 && cell.x < Arena.WIDTH - 1) // reverse left
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case EAST:
                if (cell.x < Arena.WIDTH - 1) // forward
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + 1, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // reverse
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + REVERSE_PENALTY, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                if (cell.y > 0) // turn right
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + TURN_PENALTY, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // turn left
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + TURN_PENALTY, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y > 0) // forward right
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y < Arena.HEIGHT - 1) // forward left
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y > 0) // reverse right
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y < Arena.HEIGHT - 1) // reverse left
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case WEST:
                if (cell.x < Arena.WIDTH - 1) // reverse
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + REVERSE_PENALTY, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // forward
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + 1, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0) // turn left
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + TURN_PENALTY, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // turn right
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + TURN_PENALTY, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y < Arena.HEIGHT - 1) // forward right
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y > 0) // forward left
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y < Arena.HEIGHT - 1) // reverse right
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y > 0) // reverse left
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case NORTHEAST:
                if (cell.x < Arena.WIDTH - 1 && cell.y < Arena.HEIGHT - 1) // forward
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + 1, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y > 0) // reverse
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + REVERSE_PENALTY, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y < Arena.HEIGHT - 1) // turn left
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + TURN_PENALTY, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y > 0) // turn right
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + TURN_PENALTY, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1) // forward right
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + TURN_PENALTY/2, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // forward left
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0) // reverse right
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // reverse left
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case NORTHWEST:
                if (cell.x > 0 && cell.y < Arena.HEIGHT - 1) // forward
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + 1, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y > 0) // reverse
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + REVERSE_PENALTY, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y < Arena.HEIGHT - 1) // right
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + TURN_PENALTY, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y > 0) // left
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // forward right
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + TURN_PENALTY / 2, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // forward left
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY / 2, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1) // reverse right
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + TURN_PENALTY / 2 + REVERSE_PENALTY, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0) // reverse left
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + TURN_PENALTY / 2 + REVERSE_PENALTY, 0, cell, Direction.NORTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case SOUTHEAST:
                if (cell.x < Arena.WIDTH - 1 && cell.y > 0) // forward
                {
                    temp = new Cell(cell.x + 1, cell.y - 1, cell.g + 1, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y < Arena.HEIGHT - 1) // reverse
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + REVERSE_PENALTY, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y > 0) // right
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y < Arena.HEIGHT - 1) // left
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + TURN_PENALTY, 0, cell, Direction.NORTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0) // forward right
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + TURN_PENALTY/2, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1) // forward left
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + TURN_PENALTY/2, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // reverse right
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.EAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // reverse left
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                break;
            case SOUTHWEST:
                if (cell.x > 0 && cell.y > 0) // forward
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + 1, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1 && cell.y < Arena.HEIGHT - 1) // reverse
                {
                    temp = new Cell(cell.x + 1, cell.y + 1, cell.g + REVERSE_PENALTY, 0, cell, Direction.SOUTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y < Arena.HEIGHT - 1) // right
                {
                    temp = new Cell(cell.x - 1, cell.y + 1, cell.g + TURN_PENALTY, 0, cell, Direction.NORTHWEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTHWEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0 && cell.y > 0) // left
                {
                    temp = new Cell(cell.x - 1, cell.y - 1, cell.g + TURN_PENALTY, 0, cell, Direction.SOUTHEAST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTHEAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x > 0) // forward right
                {
                    temp = new Cell(cell.x - 1, cell.y, cell.g + TURN_PENALTY/2, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y > 0) // forward left
                {
                    temp = new Cell(cell.x, cell.y - 1, cell.g + 1, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.y < Arena.HEIGHT - 1) // reverse right
                {
                    temp = new Cell(cell.x, cell.y + 1, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.SOUTH);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
                if (cell.x < Arena.WIDTH - 1) // reverse left
                {
                    temp = new Cell(cell.x + 1, cell.y, cell.g + TURN_PENALTY/2 + REVERSE_PENALTY, 0, cell, Direction.WEST);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }
        }
        return neighbours;
    }

    public List<Cell> modifiedSearch(int[][] grid, Position start, Position goal) {
        PriorityQueue<Cell> q = new PriorityQueue<>(); // Open
        Set<Cell> explored = new HashSet<>(); // Closed
        Map<Position, Integer> g_scores = new HashMap<>(); // Hash table of g scores
        List<Cell> solutionPath = new ArrayList<>();

        Cell startCell = new Cell(start.x, start.y, 0, heuristic(start, goal), null, start.dir);
        q.add(startCell);
        g_scores.put(start, startCell.g);
        while (!q.isEmpty())
        {
            Cell curr = q.poll(); // dequeue
            explored.add(curr); // mark as visited
            if (reachedDst(curr.x, curr.y, curr.dir, goal.x, goal.y, goal.dir)) // reached goal
            {
                while (curr != null)
                {
                    solutionPath.add(0, curr);
                    curr = curr.parent;
                }
                return solutionPath;
            }


            for (Cell neighbour : getNeighbours(curr, goal)) // loop through neighbours
            {
                //System.out.println("TEST");
                //System.out.printf("xx: %d, yy: %d dir: %s", neighbour.x, neighbour.y, neighbour.dir);
                if (!isBlocked(grid, neighbour.x, neighbour.y) && !explored.contains(neighbour)) // ignore obstructed neighbours
                {
                    Position neighbourPos = cellToPos(neighbour);
                    if (q.contains(neighbour)) // neighbour already in queue
                    {
                        int old_g = g_scores.get(neighbourPos);
                        if (neighbour.g < old_g) // if new cost is lower, update cost
                        {
                            q.remove(neighbour);
                            g_scores.put(neighbourPos, neighbour.g);
                            q.add(neighbour);
                        }
                    }
                    else
                    {
                        g_scores.put(neighbourPos, neighbour.g);
                        q.add(neighbour);
                    }
                }
            }
        }
        return null; // no solution found
    }

    public Position cellToPos(Cell cell) {
        return new Position(cell.x, cell.y, cell.dir);
    }

    // method to call by parent function
    public List<Cell> search(int[][] grid, Position start, Position goal) throws Exception {
        /**
         * @param grid 2D int array. Require 0 to be open spaces and 1 be obstacle
         */
        HEIGHT = grid.length;
        WIDTH = grid[0].length;
        open.clear();
        closed.clear();
        path.clear();

        /* validity checks */
        if (isInvalid(start.getX(), start.getY())) {
            throw new Exception("Start is out of range!");
        }
        if (isInvalid(goal.getX(), goal.getY())) {
            throw new Exception("Goal is out of range!");
        }
        if (isBlocked(grid, start.getX(), start.getY())) {
            throw new Exception("Start is blocked!");
        }
        if (isBlocked(grid, goal.getX(), goal.getY())) {
            throw new Exception("Goal is blocked!");
        }
        if (reachedDst(start.getX(), start.getY(), goal.getX(), goal.getY())) {
            throw new Exception("Goal is already reached!");
        }

        /* start of search */
        Cell startCell = new Cell(start.x, start.y, 0, heuristic(start, goal), null, start.dir);
        open.add(startCell);
        while (!open.isEmpty()) {
            Cell curr = open.remove(0);
            closed.add(curr);

            if (reachedDst(curr.x, curr.y, goal.getX(), goal.getY())) {
                System.out.println("Destination reached!");
                System.out.println("Total cost: " + curr.g);

                // add to path
                int[][] gridCopy = new int[grid.length][];
                for (int i = 0; i < grid.length; i++) {
                    gridCopy[i] = grid[i].clone();
                }

                Cell parent = curr;
                while (parent != null) { // backtrack path by looking at parent (and grandparents etc)
                    path.add(0, parent);
                    gridCopy[getRowNum(parent.y)][parent.x] = -1; // for drawing path
                    parent = parent.parent;
                }
                gridCopy[getRowNum(curr.y)][curr.x] = 2; // mark end position

                // print path on grid
                for (int[] grid_row : gridCopy) {
                    for (int cell : grid_row) {
                        switch (cell) {
                            case -1 -> System.out.print("*"); // path
                            case 0 -> System.out.print("_"); // walkable space
                            case 1 -> System.out.print("#"); // obstacle
                            case 2 -> System.out.print("^"); // end of path
                            case 3 -> System.out.print("@"); // image reading position
                            default -> System.out.print("?"); // unknown
                        }
                    }
                    System.out.println();
                }
                // print path in terms of cells visited
                String pathStr = "";
                for (Cell cell : path) {
                    pathStr += "[" + cell.x + "," + cell.y + "]" + " => ";
                }
                pathStr = pathStr.substring(0, pathStr.length() - 4);
                System.out.println(pathStr);
                return path;
            }

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if ( // don't allow diagonal movements
                            x == 0 && y == -1 // north
                            || x == 0 && y == 1 // south
                            || x == -1 && y == 0 // west
                            || x == 1 && y == 0 // east
                    ) {
                        int neighbourH = heuristic(curr.x + x, curr.y + y, goal.getX(), goal.getY(), curr.dir, goal.dir);
                        Cell neighbour = new Cell(curr.x + x, curr.y + y, curr.g + 1, neighbourH, curr, curr.dir);

                        if (isInvalid(neighbour.x, neighbour.y)) {
                            continue;
                        }
                        if (isBlocked(grid, neighbour.x, neighbour.y)) {
                            continue;
                        }
                        if (closed.contains(neighbour)) {
                            continue;
                        }

                        if (open.contains(neighbour)) { // neighbour was alr in open
                            Cell prev = open.get(open.indexOf(neighbour));
                            if (prev.f > neighbour.f) {
                                // this neighbour cell was alr in open, but has a higher f value
                                // so we update it with this neighbour's details
                                prev.f = neighbour.f;
                                prev.parent = neighbour.parent;
                            }
                        } else {
                            open.add(neighbour);
                        }
                    }
                }
            }

            Collections.sort(open); // sort according to ascending f values
        }

        throw new Exception("Cannot find path!");
    }

    /**
     * Helper method to convert y-position to row-number
     * @return
     */
    private int getRowNum(int y) {
        return Arena.HEIGHT - 1 - y;
    }
}
