package mdp.group9.ReedsShepp;

public enum Gear {
    FORWARD(1), BACKWARD(-1);

    private final int gear;

    Gear(int gear) {
        this.gear = gear;
    }

    public int getDirection() {
        return gear;
    }
}
