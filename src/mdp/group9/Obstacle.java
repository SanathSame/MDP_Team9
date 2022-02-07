package mdp.group9;

public class Obstacle {
    private Position pos;
    public static final int NUM_OBSTACLES = 5;

    public Obstacle(int x, int y, Direction d) {
        pos = new Position(x, y, d);
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(int x, int y, Direction d) {
        pos.setX(x);
        pos.setY(y);
        pos.setDir(d);
    }
}
