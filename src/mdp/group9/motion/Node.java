package mdp.group9.motion;

enum Gear {
    FORWARD, BACKWARD
}

enum Steering {
    LEFT, STRAIGHT, RIGHT
}

public class Node {
    public int x;
    public int y;
    public int f;
    public int g;
    public int h;
    public Gear gear;
    public Steering steering;

    public Node(int x, int y, int g, int h, Gear gear, Steering steering) {
        this.x = x;
        this.y = y;
        this.g = g;
        this.h = h;
        this.f = g + h;
        this.gear = gear;
        this.steering = steering;
    }

    public void reverseSteering() {
        if (steering == Steering.LEFT) {
            steering = Steering.RIGHT;
        } else if (steering == Steering.RIGHT) {
            steering = Steering.LEFT;
        }
    }

    public void reverseGear() {
        gear = gear == Gear.FORWARD ? Gear.BACKWARD : Gear.FORWARD;
    }

//    @Override
//    public int compareTo(Node that) {
//        // compares one Cell with another based on f value
//        // needed to sort priority queue
//        return this.f - that.f;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        // used to check if two Cells are the same
//        // the same Cell may appear multiple times with different g and f value
//        // used to update a Cell in the open list with a lower f value
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
////        AStar.Cell cell = (AStar.Cell) o;
////        return x == cell.x && y == cell.y && dir == cell.dir;
//    }
}
