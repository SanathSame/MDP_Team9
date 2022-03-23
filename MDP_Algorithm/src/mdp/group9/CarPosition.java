package mdp.group9;

public class CarPosition extends Position{
    State state;
    public CarPosition (int x, int y, Direction d, State state) {
        super(x,y,d);
        this.state = state;
    }

    public CarPosition (int x, int y, Direction d) {
        super(x,y,d);
        this.state = State.STATIONARY;
    }
}
