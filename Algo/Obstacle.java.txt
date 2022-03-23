package mdp.group9;

public class Obstacle {
    private Position pos;
    private int id;

    public Obstacle(int x, int y, Direction d) {
        pos = new Position(x, y, d);
    }

    public Obstacle(int id, int x, int y, Direction d) {
        pos = new Position(x, y, d);
        this.id = id;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(int x, int y, Direction d) {
        pos.setX(x);
        pos.setY(y);
        pos.setDir(d);
    }

    public int getId() {
        return id;
    }
}
