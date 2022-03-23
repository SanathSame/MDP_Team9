package mdp.group9;

import java.io.FileWriter;
import java.util.*;

public class AStar {
    /**
     * Implements A* Search. To be called consecutively from one image obstacle to another
     */
    public static final int TURN_PENALTY = 8;
    public static final int REVERSE_PENALTY = 2, START_PENALTY = 2;
    public static final int TURN_RADIUS = 2; // in terms of cells
    public static final int TURN_RADIUS2 = 4; // in terms of cells

    public static boolean SAFE_TURN = true;
    public static boolean ALLOW_EDGE_MOVEMENT = false;
    private final int MAX_G = 150; // to set threshold for finding path

    // "Node" class for building tree
    public class Cell implements Comparable<Cell> {
        public int x;
        public int y;
        public int f;
        public int g;
        public int h;
        public Cell parent;
        public State carState;
        public Direction dir; // Added for modified A Star search

        public Cell(int x, int y, int g, int h, Cell parent, Direction d) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.parent = parent;
            this.dir = d;
            this.carState = State.FORWARD;
        }

        public Cell(int x, int y, int g, int h, Cell parent, Direction d, State state) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.parent = parent;
            this.dir = d;
            this.carState = state;
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
            return x == cell.x && y == cell.y && dir == cell.dir && carState == cell.carState;
        }

        @Override
        public String toString() {
            return "Cell{" +
                    "x=" + x +
                    ", y=" + y +
                    ", f=" + f +
                    ", g=" + g +
                    ", h=" + h +
                    ", carState=" + carState +
                    ", dir=" + dir +
                    '}';
        }
    }

    public AStar() {}

    // checks if given cell is blocked
    private boolean isBlocked(int[][] grid, int x, int y) {
        // 1 == blocked, 2 == virtual boundary around arena
        return ALLOW_EDGE_MOVEMENT ? grid[getRowNum(y)][x] == 1 : grid[getRowNum(y)][x] == 1 || grid[getRowNum(y)][x] == 2;
    }

    // checks if car, with its 3x3 footprint, is blocked
    private boolean carIsBlocked(int[][] grid, int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (x+j > 0 && x+j < Arena.WIDTH-1 && y+i > 0 && y+i < Arena.HEIGHT-1) {
                    if (ALLOW_EDGE_MOVEMENT && grid[getRowNum(y+i)][x+j] == 1) {
                        return true;
                    } else if (!ALLOW_EDGE_MOVEMENT && (grid[getRowNum(y+i)][x+j] == 1 || grid[getRowNum(y+i)][x+j] == 2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean reachedDst(int srcX, int srcY, Direction srcD, int dstX, int dstY, Direction dstD) {
        return srcX == dstX && srcY == dstY && srcD == dstD;
    }

    public int heuristic(int srcX, int srcY, int dstX, int dstY, Direction srcD, Direction dstD) {
        return Math.abs(dstX - srcX) + Math.abs(dstY - srcY); // Return Manhattan heuristic distance (for now)
    }

    public int heuristic(Position src, Position dst) {
        return Math.abs(dst.x - src.x) + Math.abs(dst.y - src.y); // Return Manhattan heuristic distance (for now)
    }

    private List<Cell> getNeighbours(int[][] grid, Cell cell, Position goal) {
        List<Cell> neighbours = new ArrayList<>();
        Cell temp;
        int xOffset, yOffset; // for finding back wheel location before turn
        int carBoundary = ALLOW_EDGE_MOVEMENT ? 1 : 2; // for checking if car body should be outside arena

        // Note: turns must consider from back wheel
        switch (cell.dir) {
            case NORTH:
                /* move forward */
                if (cell.y < Arena.HEIGHT - 1)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + 1;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x, cell.y + 1, newG, 0, cell, Direction.NORTH, State.FORWARD);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* reverse */
                if (cell.y > 0)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + 1;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x, cell.y - 1, newG, 0, cell, Direction.NORTH, State.REVERSE);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.NORTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* turn right */
                xOffset = 1; yOffset = -1;
                // forward right
                if (cell.x + xOffset + TURN_RADIUS + carBoundary < Arena.WIDTH && cell.y + yOffset + TURN_RADIUS + carBoundary < Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.EAST, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, 1, 1)) {
                        temp.x += 1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse right
                if (cell.x + xOffset + TURN_RADIUS < Arena.WIDTH && cell.y + yOffset - TURN_RADIUS - carBoundary >= 0)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.WEST, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, -1, -1)) {
                        temp.x += -1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                /* turn left */
                xOffset = -1; yOffset = -1;
                // forward left
                if (cell.x + xOffset - TURN_RADIUS - carBoundary >= 0 && cell.y + yOffset + TURN_RADIUS + carBoundary < Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.WEST, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, -1, 1)) {
                        temp.x += -1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse left
                if (cell.x + xOffset - TURN_RADIUS >= 0 && cell.y + yOffset - TURN_RADIUS - carBoundary >= 0)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.EAST, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, 1, -1)) {
                        temp.x += 1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                break;

            case SOUTH:
                /* forward */
                if (cell.y > 0)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + 1;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x, cell.y - 1, newG, 0, cell, Direction.SOUTH, State.FORWARD);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* reverse */
                if (cell.y < Arena.HEIGHT - 1)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + 1;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x, cell.y + 1, newG, 0, cell, Direction.SOUTH, State.REVERSE);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.SOUTH, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* turn left */
                xOffset = 1; yOffset = 1;
                // forward left
                if (cell.x + xOffset + TURN_RADIUS + carBoundary < Arena.WIDTH && cell.y + yOffset - TURN_RADIUS - carBoundary >= 0)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.EAST, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, 1, -1)) {
                        temp.x += 1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse left
                if (cell.x + xOffset + TURN_RADIUS < Arena.HEIGHT && cell.y + yOffset + TURN_RADIUS + carBoundary < Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.WEST, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, -1, 1)) {
                        temp.x += -1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                /* turn right */
                xOffset = -1; yOffset = 1;
                // forward right
                if (cell.x + xOffset - TURN_RADIUS - carBoundary >= 0 && cell.y + yOffset - TURN_RADIUS - carBoundary >= 0)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.WEST, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, -1, -1)) {
                        temp.x += -1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse right
                if (cell.x + xOffset - TURN_RADIUS >= 0 && cell.y + yOffset + TURN_RADIUS + carBoundary < Arena.HEIGHT)
                { // reverse right
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.EAST, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, 1, 1)) {
                        temp.x += 1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                break;

            case EAST:
                /* forward */
                if (cell.x < Arena.WIDTH - 1)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + 1;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x + 1, cell.y, newG, 0, cell, Direction.EAST, State.FORWARD);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* reverse */
                if (cell.x > 0)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + 1;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x - 1, cell.y, newG, 0, cell, Direction.EAST, State.REVERSE);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.EAST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* turn right */
                xOffset = -1; yOffset = -1;
                // forward right
                if (cell.x + xOffset + TURN_RADIUS + carBoundary < Arena.WIDTH && cell.y + yOffset - TURN_RADIUS - carBoundary >= 0)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.SOUTH, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, 1, -1)) {
                        temp.x += 1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse right
                if (cell.x + xOffset - TURN_RADIUS - carBoundary >= 0 && cell.y + yOffset - TURN_RADIUS >= 0)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.NORTH, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, -1, 1)) {
                        temp.x += -1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                /* turn left */
                xOffset = -1; yOffset = 1;
                // forward left
                if (cell.x + xOffset + TURN_RADIUS + carBoundary < Arena.WIDTH && cell.y + yOffset + TURN_RADIUS + carBoundary < Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.NORTH, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, 1, 1)) {
                        temp.x += 1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse left
                if (cell.x + xOffset - TURN_RADIUS - carBoundary >= 0 && cell.y + yOffset + TURN_RADIUS < Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.SOUTH, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, -1, -1)) {
                        temp.x += -1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                break;

            case WEST:
                /* forward */
                if (cell.x > 0)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + 1;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x - 1, cell.y, newG, 0, cell, Direction.WEST, State.FORWARD);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }

                /* reverse */
                if (cell.x < Arena.WIDTH - 1)
                {
                    int newG;
                    if (cell.carState == State.FORWARD)
                    {
                        newG = cell.g + REVERSE_PENALTY;
                    }
                    else if (cell.carState == State.REVERSE)
                    {
                        newG = cell.g + 1;
                    }
                    else
                    {
                        newG = cell.g + START_PENALTY;
                    }
                    temp = new Cell(cell.x + 1, cell.y, newG, 0, cell, Direction.WEST, State.REVERSE);
                    temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, Direction.WEST, goal.dir);
                    temp.f = temp.g + temp.h;
                    neighbours.add(temp);
                }


                /* turn left */
                xOffset = 1; yOffset = -1;
                // forward left
                if (cell.x + xOffset - TURN_RADIUS - carBoundary >= 0 && cell.y + yOffset - TURN_RADIUS - carBoundary >= 0)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.SOUTH, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, -1, -1)) {
                        temp.x += -1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse left
                if (cell.x + xOffset + TURN_RADIUS + carBoundary < Arena.WIDTH && cell.y + yOffset - TURN_RADIUS >= 0)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset - TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.NORTH, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, 1, 1)) {
                        temp.x += 1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                /* turn right */
                xOffset = 1; yOffset = 1;
                // forward right
                if (cell.x + xOffset - TURN_RADIUS - carBoundary >= 0 && cell.y + yOffset + TURN_RADIUS + carBoundary< Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset - TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.NORTH, State.FORWARD);
                    if (isValidTurn(grid, cell, temp, -1, 1)) {
                        temp.x += -1;
                        temp.y += 1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }
                // reverse right
                if (cell.x + xOffset + TURN_RADIUS + carBoundary < Arena.WIDTH && cell.y + yOffset + TURN_RADIUS < Arena.HEIGHT)
                {
                    temp = new Cell(cell.x + xOffset + TURN_RADIUS, cell.y + yOffset + TURN_RADIUS,
                            cell.g + TURN_PENALTY, 0, cell, Direction.SOUTH, State.REVERSE);
                    if (isValidTurn(grid, cell, temp, 1, -1)) {
                        temp.x += 1;
                        temp.y += -1;
                        temp.h = heuristic(temp.x, temp.y, goal.x, goal.y, temp.dir, goal.dir);
                        temp.f = temp.g + temp.h;
                        neighbours.add(temp);
                    }
                }

                break;
        }
        return neighbours;
    }

    /** helper method for 3x3 turns
     * @param grid
     * @param start start Cell (based on car's center)
     * @param end end Cell (based on car's back wheel)
     * @param xOffset for finding car's center of end position
     * @param yOffset for finding car's center of end position
     * @return
     */
    private boolean isValidTurn(int[][] grid, Cell start, Cell end, int xOffset, int yOffset) {
        // reject if end position will enter obstacle boundary
        if (carIsBlocked(grid, end.x + xOffset, end.y + yOffset)) {
            return false;
        }

        // check based on back wheel
        if (end.x - start.x > 0 && end.y - start.y > 0) { // up right
            switch (start.dir) {
                case NORTH, SOUTH -> {
                    // check left of "/" diagonal
                    for (int y = 1; y <= end.y - start.y; y++) {
                        for (int x = 0; x <= y; x++) {
                            if (isBlocked(grid, start.x + x, start.y + y)) return false;
                        }
                    }
                }
                case EAST, WEST -> {
                    // check right of "/" diagonal
                    for (int x = 1; x < end.x - start.x; x++) {
                        for (int y = 0; y <= x; y++) {
                            if (isBlocked(grid, start.x + x, start.y + y)) return false;
                        }
                    }
                }
            }
        } else if (end.x - start.x < 0 && end.y - start.y > 0) { // up left
            switch (start.dir) {
                case NORTH, SOUTH -> {
                    // check right of "\" diagonal
                    for (int y = 1; y < end.y - start.y; y++) {
                        for (int x = 0; x <= y; x++) {
                            if (isBlocked(grid, start.x - x, start.y + y)) return false;
                        }
                    }
                }
                case EAST, WEST -> {
                    // check left of "\" diagonal
                    for (int x = 1; x < start.x - end.x; x++) {
                        for (int y = 0; y <= x; y++) {
                            if (isBlocked(grid, start.x - x, start.y + y)) return false;
                        }
                    }
                }
            }
        } else if (end.x - start.x > 0 && end.y - start.y < 0) { // down right
            switch (start.dir) {
                case NORTH, SOUTH -> {
                    // check left of "\" diagonal
                    for (int x = 1; x < start.x - end.x; x++) {
                        for (int y = 0; y <= x; y++) {
                            if (isBlocked(grid, start.x - x, start.y + y)) return false;
                        }
                    }
                }
                case EAST, WEST -> {
                    // check right of "\" diagonal
                    for (int y = 1; y < end.y - start.y; y++) {
                        for (int x = 0; x <= y; x++) {
                            if (isBlocked(grid, start.x - x, start.y + y)) return false;
                        }
                    }
                }
            }
        } else if (end.x - start.x < 0 && end.y - start.y < 0) { // down left
            switch (start.dir) {
                case NORTH, SOUTH -> {
                    // check right of "/" diagonal
                    for (int x = 1; x < end.x - start.x; x++) {
                        for (int y = 0; y <= x; y++) {
                            if (isBlocked(grid, start.x + x, start.y + y)) return false;
                        }
                    }
                }
                case EAST, WEST -> {
                    // check left of "/" diagonal
                    for (int y = 1; y <= end.y - start.y; y++) {
                        for (int x = 0; x <= y; x++) {
                            if (isBlocked(grid, start.x + x, start.y + y)) return false;
                        }
                    }
                }
            }
        }

        // if true, will check more cells for turning
        if (SAFE_TURN) {
            if (carIsBlocked(grid, start.x, start.y)) {
                return false;
            }
            // (conservatively) check car's body during turn
            switch (start.dir) {
                case NORTH, SOUTH -> {
                    // check top left corner of turn
                    if (carIsBlocked(grid, start.x, end.y + yOffset)) {
                        return false;
                    }
                }
                case EAST, WEST -> {
                    if (carIsBlocked(grid, end.x + xOffset, start.y)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<Cell> modifiedSearch(int[][] grid, Position start, Position goal, boolean costOnly) {
        PriorityQueue<Cell> q = new PriorityQueue<>(); // Open
        Set<Cell> explored = new HashSet<>(); // Closed
        Map<CarPosition, Integer> g_scores = new HashMap<>(); // Hash table of g scores

        if (isBlocked(grid, start.getX(), start.getY())) {
            System.out.println("Start is blocked!");
            return null;
        }
        if (isBlocked(grid, goal.getX(), goal.getY())) {
            System.out.println("Goal is blocked!");
            return null;
        }

        Cell startCell = new Cell(start.x, start.y, 0, heuristic(start, goal), null, start.dir, State.STATIONARY);
        q.add(startCell);
        g_scores.put(new CarPosition(start.x, start.y, start.dir, State.STATIONARY), startCell.g);
        while (!q.isEmpty())
        {
            Cell curr = q.poll(); // dequeue
            if (curr.g >= MAX_G) {
                System.out.format("No path found: [%d, %d, %s] to [%d, %d, %s]\n", start.x, start.y, start.dir, goal.x, goal.y, goal.dir);
                return null;
            }
            explored.add(curr); // mark as visited
            if (reachedDst(curr.x, curr.y, curr.dir, goal.x, goal.y, goal.dir)) // reached goal
            {
                List<Cell> solutionPath = new ArrayList<>();
                if (costOnly) {
                    solutionPath.add(curr);
                } else {
                    while (curr != null) {
                        solutionPath.add(0, curr);
                        curr = curr.parent;
                    }
                }
                return solutionPath;
            }

            for (Cell neighbour : getNeighbours(grid, curr, goal)) // loop through neighbours
            {
                if (!isBlocked(grid, neighbour.x, neighbour.y) && !explored.contains(neighbour)) // ignore obstructed neighbours
                {
                    CarPosition neighbourPos = cellToPos(neighbour);
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

    public CarPosition cellToPos(Cell cell) {
        return new CarPosition(cell.x, cell.y, cell.dir, cell.carState);
    }

    /**
     * Helper method to convert y-position to row-number
     * @return
     */
    private int getRowNum(int y) {
        return Arena.HEIGHT - 1 - y;
    }
}
