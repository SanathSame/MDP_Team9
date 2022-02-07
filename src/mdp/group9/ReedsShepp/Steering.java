package mdp.group9.ReedsShepp;

public enum Steering {
    LEFT(-1), RIGHT(1), STRAIGHT(0);

    private final int steering;

    Steering(int steering) {
        this.steering = steering;
    }

    public int getSteering() {
        return steering;
    }
}
