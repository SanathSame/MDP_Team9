package mdp.group9.motion;

import java.util.ArrayList;
import java.util.List;

public class HybridAStar {

    public static final int TURN_PENALTY = 2, REVERSE_PENALTY = 10;

    private final int MAP_LENGTH = 200;
    private final List<Node> open = new ArrayList<>();
    private final List<Node> closed = new ArrayList<>();
    private final List<Node> path = new ArrayList<>();


}
