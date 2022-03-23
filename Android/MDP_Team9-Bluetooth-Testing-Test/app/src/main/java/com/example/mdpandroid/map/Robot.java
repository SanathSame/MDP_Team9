package com.example.mdpandroid.map;

import static com.example.mdpandroid.map.BoardMap.TARGET_CELL_CODE;

import java.io.Serializable;

public class Robot implements Serializable {
    private int x, y, s, m, f;

    private BoardMap _map;

    public static final int ROBOT_MOTOR_FORWARD = 1;
    public static final int ROBOT_MOTOR_STOP = 0;
    public static final int ROBOT_MOTOR_REVERSE = -1;

    public static final int ROBOT_SERVO_LEFT = -1;
    public static final int ROBOT_SERVO_CENTRE = 0;
    public static final int ROBOT_SERVO_RIGHT = 1;

    public static final int ROBOT_FACE_NORTH = 0;
    public static final int ROBOT_FACE_EAST = 1;
    public static final int ROBOT_FACE_SOUTH = 2;
    public static final int ROBOT_FACE_WEST = 3;

    public static final String ROBOT_COMMAND_POS = "ROBOT";
    public static final String STM_COMMAND_FORWARD = "F 10"; //mod
    public static final String STM_COMMAND_REVERSE = "B 10"; //mod
    public static final String STM_COMMAND_STOP = "x";
    public static final String STM_COMMAND_LEFT = "l";
    public static final String STM_COMMAND_RIGHT = "r";
    public static final String STM_COMMAND_CENTRE = "c";

    public Robot(BoardMap map) {
        this.x = 1;
        this.y = 19;
        this.s = ROBOT_SERVO_CENTRE;
        this.m = ROBOT_MOTOR_STOP;
        this.f = ROBOT_FACE_NORTH;
        this._map = map;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getServo() { return s; }
    public void setServo(int s) { this.s = s; }

    public int getMotor() { return m; }
    public void setMotor(int m) { this.m = m; }

    public int getFacing() { return f; }
    public void setFacing(int f) { this.f = f; }
    public String getFacingText() {
        switch (this.f) {
            case 0: return "NORTH";
            case 1: return "EAST";
            case 2: return "SOUTH";
            case 3: return "WEST";
        }
        return "N";
    }

    public void motorRotate(int direction) {

        boolean forwardInGrid = false;
        boolean reverseInGrid = false;
        boolean forwardAvoidTarget = false;
        boolean reverseAvoidTarget = false;
        int FORWARD = 0;
        int REVERSE = 0;

        switch(f) {
            case ROBOT_FACE_NORTH:
                forwardInGrid = y-1 >= 1;
                reverseInGrid = y+1 < 20;
                forwardAvoidTarget = (this._map.getBoard()[x][y - 1] != TARGET_CELL_CODE) && (this._map.getBoard()[x + 1][y - 1] != TARGET_CELL_CODE);
                reverseAvoidTarget = false;
                FORWARD = -1;
                REVERSE = 1;
                if (reverseInGrid && direction == ROBOT_MOTOR_REVERSE)
                    reverseAvoidTarget = (this._map.getBoard()[x][y + 2] != TARGET_CELL_CODE) && (this._map.getBoard()[x + 1][y + 2] != TARGET_CELL_CODE);
                break;
            case ROBOT_FACE_EAST:
                forwardInGrid = x+1 < 20;
                reverseInGrid = x-1 >= 1;
                forwardAvoidTarget = false;
                reverseAvoidTarget = (this._map.getBoard()[x - 1][y] != TARGET_CELL_CODE) && (this._map.getBoard()[x - 1][y + 1] != TARGET_CELL_CODE);
                FORWARD = 1;
                REVERSE = -1;
                if (forwardInGrid && direction == ROBOT_MOTOR_FORWARD)
                    forwardAvoidTarget = (this._map.getBoard()[x + 2][y] != TARGET_CELL_CODE) && (this._map.getBoard()[x + 2][y+1] != TARGET_CELL_CODE);
                break;
            case ROBOT_FACE_SOUTH:
                forwardInGrid = y+1 < 20;
                reverseInGrid = y-1 >= 1;
                forwardAvoidTarget = false;
                reverseAvoidTarget = (this._map.getBoard()[x][y - 1] != TARGET_CELL_CODE) && (this._map.getBoard()[x + 1][y - 1] != TARGET_CELL_CODE);
                FORWARD = 1;
                REVERSE = -1;
                if (forwardInGrid && direction == ROBOT_MOTOR_FORWARD)
                    forwardAvoidTarget = (this._map.getBoard()[x][y + 2] != TARGET_CELL_CODE) && (this._map.getBoard()[x + 1][y + 2] != TARGET_CELL_CODE);
                break;
            case ROBOT_FACE_WEST:
                forwardInGrid = x-1 >= 1;
                reverseInGrid = x+1 < 20;
                forwardAvoidTarget = (this._map.getBoard()[x - 1][y] != TARGET_CELL_CODE) && (this._map.getBoard()[x - 1][y + 1] != TARGET_CELL_CODE);
                reverseAvoidTarget = false;
                FORWARD = -1;
                REVERSE = 1;
                if (reverseInGrid && direction == ROBOT_MOTOR_REVERSE)
                    reverseAvoidTarget = (this._map.getBoard()[x + 2][y] != TARGET_CELL_CODE) && (this._map.getBoard()[x + 2][y + 1] != TARGET_CELL_CODE);
                break;
        }

        if ((direction == ROBOT_MOTOR_FORWARD && forwardInGrid && forwardAvoidTarget)
            || (direction == ROBOT_MOTOR_REVERSE && reverseInGrid && reverseAvoidTarget)) {
            this.m = direction == ROBOT_MOTOR_FORWARD ? ROBOT_MOTOR_FORWARD : ROBOT_MOTOR_REVERSE;
            switch(this.f) {
                case ROBOT_FACE_NORTH:
                case ROBOT_FACE_SOUTH:
                    this.y += direction == ROBOT_MOTOR_FORWARD ? FORWARD : REVERSE;
                    break;
                case ROBOT_FACE_EAST:
                case ROBOT_FACE_WEST:
                    this.x += direction == ROBOT_MOTOR_FORWARD ? FORWARD : REVERSE;
                    break;
            }
        }
    }

    public void servoTurn(int direction) {
        this.s = direction == ROBOT_SERVO_LEFT ? ROBOT_SERVO_LEFT : ROBOT_SERVO_RIGHT;
    }

    public void cycleFace(boolean clockwise) {
        if (clockwise)
            this.f = this.f == ROBOT_FACE_WEST ? ROBOT_FACE_NORTH : this.f + 1;
        else
            this.f = this.f == ROBOT_FACE_NORTH ? ROBOT_FACE_WEST : this.f - 1;
    }

    @Override
    public String toString() {
        return "Robot{" +
                "x=" + x +
                ", y=" + y +
                ", s=" + s +
                ", m=" + m +
                ", f=" + f +
                '}';
    }
}
