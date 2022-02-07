package mdp.group9;

public class RobotCar {
    private Position pos;

    public RobotCar() {
        pos = new Position(1, 1, Direction.NORTH);
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
