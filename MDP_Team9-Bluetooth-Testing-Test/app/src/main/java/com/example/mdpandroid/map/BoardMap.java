package com.example.mdpandroid.map;

import static com.example.mdpandroid.map.Robot.ROBOT_FACE_NORTH;
import static com.example.mdpandroid.map.Target.TARGET_IMG_NULL;

import java.util.ArrayList;

public class BoardMap {
    Robot robo;
    ArrayList<Target> targets = new ArrayList<Target>();
    Target lastTouched;

    private int rows = 21;
    private int cols = 21;

    int[][] board = new int[rows][cols];

    public static final int EMPTY_CELL_CODE = 0;
    public static final int CAR_CELL_CODE = 1;
    public static final int TARGET_CELL_CODE = 3;
    public static final int EXPLORE_CELL_CODE = 4;
    public static final int EXPLORE_HEAD_CELL_CODE = 5;
    public static final int FINAL_PATH_CELL_CODE = 6;

    public BoardMap() {
        super();
        robo = new Robot(this);
        board[robo.getX()][robo.getY()] = CAR_CELL_CODE;

        // targets.add(new Target(10, 21-10, targets.size())); // 10, 10
        // targets.add(new Target(15, 21-15, targets.size())); // 15, 15
        // targets.add(new Target(15, 21-5, targets.size())); // 15, 5
        // targets.add(new Target(20, 21-20, targets.size())); // 20, 20
        // targets.add(new Target(20, 21-1, targets.size())); // 20, 1

        // int n = 0;
        // while (n < targets.size()) {
            // board[targets.get(n).getX()][targets.get(n).getY()] = TARGET_CELL_CODE;
            // n++;
        // }
    }

    public final void resetGrid() {
        for(int i = 1; i <= 19; ++i)
            for(int j = 1; j <= 19; ++j)
                this.board[i][j] = 0;

        this.getRobo().setX(1);
        this.getRobo().setY(19);
        this.getRobo().setFacing(ROBOT_FACE_NORTH);
        targets.clear();
        this.board[getRobo().getX()][getRobo().getY()] = CAR_CELL_CODE;
    }

    public Robot getRobo() { return robo;}

    public ArrayList<Target> getTargets() {
        return targets;
    }

    public boolean hasReceivedAllTargets() {
        int targetReceived = 0;
        for(int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getImg() > TARGET_IMG_NULL) {
                targetReceived++;
            }
        }
        return targetReceived == targets.size();
    }

    public void defaceTargets() {
        for(int i = 0; i < targets.size(); i++) {
            targets.get(i).setImg(TARGET_IMG_NULL);
        }
    }

    public void dequeueTarget(Target t) {
        int delTargetIdx = t.getN();

        targets.remove(t.getN());

        while (delTargetIdx < targets.size()) {
            targets.get(delTargetIdx).setN(delTargetIdx);
            delTargetIdx++;
        }
    }

    public Target getLastTouchedTarget() {
        return lastTouched;
    }

    public void setLastTouchedTarget(Target lastT) {
        this.lastTouched = lastT;
    }

    public Target findTarget(int x, int y) {
        int n = 0;
        while (n < targets.size()) {
            if (targets.get(n).getX() == x && targets.get(n).getY() == y)
                return targets.get(n);

            n++;
        }
        return null;
    }

    public int[][] getBoard() {
        return board;
    }
}
