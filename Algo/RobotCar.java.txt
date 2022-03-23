package mdp.group9;

public class RobotCar {
    Position pos;
    State state;

    public RobotCar() {
        pos = new Position(1, 1, Direction.NORTH);
        state = State.STATIONARY;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(int x, int y, Direction d) {
        pos.setX(x);
        pos.setY(y);
        pos.setDir(d);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
