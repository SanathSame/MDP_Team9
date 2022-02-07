package mdp.group9.ReedsShepp;

public class PathElement {

    double param;
    Steering steering;
    Gear gear;

    public PathElement(double param, Steering steering, Gear gear) {
        this.param = param;
        this.steering = steering;
        this.gear = gear;

        if (param < 0) {
            this.param = -param;
            this.reverseGear();
        }
    }

    public void reverseSteering() {
        if (steering == Steering.LEFT) {
            steering = Steering.RIGHT;
        } else if (steering == Steering.RIGHT) {
            steering = Steering.LEFT;
        }
    }

    public void reverseGear() {
        if (gear == Gear.FORWARD) {
            gear = Gear.BACKWARD;
        } else {
            gear = Gear.FORWARD;
        }
    }

    @Override
    public String toString() {
        return String.format("{ Steering: %10s\tGear: %10s\tDistance: %.2f", steering, gear, param);
    }
}
