package mdp.group9;

import java.util.Objects;

public class Position {
    int x, y;
    Direction dir;

    public Position(int x, int y, Direction d) {
        this.x = x;
        this.y = y;
        dir = d;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Direction getDir() {
        return dir;
    }

    public void setDir(Direction d) {
        dir = d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y && dir == position.dir;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, dir);
    }
}
