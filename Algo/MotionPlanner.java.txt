package mdp.group9;

import java.util.ArrayList;
import java.util.List;

public class MotionPlanner {

    public static final int CM_PER_CELL = 10;
    public static final int TURN_RADIUS = 20; // need to test
    public static final int TURN_RADIUS2 = 40; // need to test
    // corridor - lounge
    public static int LF_ANGLE = 86; // 90 - 81
    public static int RF_ANGLE = 91; // 93 - 88
    public static int LB_ANGLE = 86; // 85 - 90
    public static int RB_ANGLE = 85; // 79 - 83

    public List<String> toCommands(List<AStar.Cell> path) {
        return toCommands(path, -1);
    }

    public List<String> toCommands(List<AStar.Cell> path, int obstacleID) {
        if (path == null || path.size() == 0)
            return null;
        int cellCount = 0;
        int correctionGap = AStar.TURN_RADIUS * CM_PER_CELL - TURN_RADIUS;
        int correctionGap2 = AStar.TURN_RADIUS2 * CM_PER_CELL - TURN_RADIUS2;
        int correction;
        boolean turnedPreviously = false;
        String currCmd;
        List<String> commands = new ArrayList<>();
        AStar.Cell curr = path.get(0), prev;
        for (int i = 0; i < path.size(); i++) // loop through the path
        {
            curr = path.get(i);
            prev = i == 0 ? null : path.get(i - 1); // get previous cell

            if (prev != null) // not starting point
            {
                boolean justReversed = (curr.carState != prev.carState) && (prev.carState != State.STATIONARY);
                boolean justTurned = curr.dir != prev.dir;
                if (justReversed || justTurned) // state reversal or turn detected
                {
                    boolean isForward; // to store state of last straight-line motion
                    if (prev.carState == State.STATIONARY)
                        isForward = curr.carState == State.FORWARD;
                    else
                        isForward = prev.carState == State.FORWARD;
                    //correction = turnedPreviously ? correctionGap : 0;
                    //currCmd = move(cellCount, isForward, justReversed ? correction : correctionGap + correction);
                    currCmd = move(cellCount, isForward, 0);
                    if (currCmd != null)
                        commands.add(currCmd);

                    if (justTurned) // turn detected as well
                    {
                        isForward = curr.carState == State.FORWARD; // get turning state
                        boolean isLeft;
                        int dirDiff = curr.dir.ordinal() - prev.dir.ordinal();
                        if (dirDiff == 1 || dirDiff == -3) // indicates right turn (if forward)
                            isLeft = !isForward;
                        else // left turn (if forward)
                            isLeft = isForward;

/*                        if (justReversed) // if reversing dir and turning at the same time
                        {
                            currCmd = move(0, isForward, correctionGap); // add correction first before turn
                            if (currCmd != null)
                            {
                                commands.add(currCmd);
                            }
                        }*/
                        currCmd = move(0, isForward, isForward ? correctionGap : correctionGap2);
                        if (currCmd != null)
                        {
                            commands.add(currCmd);
                        }
                        commands.add(turn(isLeft, isForward));
                        currCmd = move(0, isForward, isForward ? correctionGap2 : correctionGap);
                        if (currCmd != null)
                        {
                            commands.add(currCmd);
                        }
                        turnedPreviously = true;
                        cellCount = 0;
                    }
                    else
                    {
                        turnedPreviously = false;
                        cellCount = 1;
                    }

                }
                else
                {
                    cellCount++;
                }
            }
        }
        //correction = turnedPreviously ? correctionGap : 0;
        //currCmd = move(cellCount, curr.carState == State.FORWARD, correction);
        currCmd = move(cellCount, curr.carState == State.FORWARD, 0);
        if (currCmd != null)
        {
            commands.add(currCmd);
        }
        simplifyCommands(commands);
        adhereToLimits(commands);
        return commands;
    }

    private void simplifyCommands(List<String> commands) {
        int i = 1;
        char prev = commands.get(0).charAt(0), cur;
        while (i < commands.size())
        {
            cur = commands.get(i).charAt(0);
            if (prev != 'F' && prev != 'B' || cur != 'F' && cur != 'B')
            {
                prev = cur;
                i++;
            }
            else
            {
                String combinedCmd = combineCommands(commands.get(i - 1), commands.get(i));
                commands.set(i - 1, combinedCmd);
                commands.remove(i);
                prev = cur;
            }
        }
    }

    private void adhereToLimits(List<String> commands) {
        int i = 0;
        char cur;
        while (i < commands.size())
        {
            cur = commands.get(i).charAt(0);
            if (cur == 'F' || cur == 'B')
            {
                boolean forwardFirst;
                if (i == commands.size() - 1)
                    forwardFirst = false;
                else
                {
                    forwardFirst = (commands.get(i + 1).charAt(1) == 'F');
                }
                String[] splitCommands = splitCommand(commands.get(i), forwardFirst);
                if (splitCommands.length > 1)
                {
                    commands.set(i, splitCommands[0]);
                    commands.add(i + 1, splitCommands[1]);
                    i += 2;
                }
                else
                    i++;
            }
            else
                i++;
        }
    }

    /**
     * Combines two consecutive forward/backward commands into one.
     * @param command1 The first command
     * @param command2 The second command
     * @return The combined command
     */
    public String combineCommands(String command1, String command2) {
        boolean wasForward = command1.charAt(0) == 'F';
        boolean forward = command2.charAt(0) == 'F';
        int prevDist = customParseInt(command1.substring(2));
        int dist = customParseInt(command2.substring(2));

        if (wasForward == forward)
        {
            dist += prevDist;
        }
        else
        {
            dist = prevDist - dist;
            if (dist == 0)
                return "";
            if (dist < 0)
            {
                dist = -dist;
                wasForward = !wasForward;
            }
        }
        return String.format(wasForward ? "F %-4d" : "B %-4d", dist);
    }

    private int customParseInt(String s) {
        int res = 0;
        for (int i = 0; i < s.length() && s.charAt(i) != ' '; i++)
        {
            res = res * 10 + Character.getNumericValue(s.charAt(i));
        }
        return res;
    }

    /**
     * Splits a forward/reverse command into 2 (1 forward 1 backward) if the distance is too small
     * @param command The command to be split
     * @param forwardFirst This decides whether the car should go forward or backward first
     * @return The split commands, or the original command if no split is required
     */
    private String[] splitCommand(String command, boolean forwardFirst) {
        int prevDist = customParseInt(command.substring(2));
        String[] commands;
        if (prevDist >= 10)
            return new String[] {command};
        boolean wasForward = command.charAt(0) == 'F';
        commands = new String[2];
        if (wasForward)
        {
            commands[0] = forwardFirst ? String.format("F %-4d", prevDist + 10) : "B 10  ";
            commands[1] = forwardFirst ? "B 10  " : String.format("F %-4d", prevDist + 10);
        }
        else
        {
            commands[0] = forwardFirst ? "F 10  " : String.format("B %-4d", prevDist + 10);
            commands[1] = forwardFirst ? String.format("B %-4d", prevDist + 10) : "F 10  ";
        }

        return commands;
    }

    public String move(int cells, boolean forward, int correctionGap) {
        int dist = CM_PER_CELL * cells + correctionGap;
        if (dist == 0)
            return null;
        if (dist < 0) // if negative distance
        {
            dist = -dist; // make it positive in other direction
            forward = !forward;
        }
        return String.format(forward ? "F %-4d" : "B %-4d", dist);
    }

    public String turn(boolean left, boolean forward) {
        String command = "";
        if (left && forward) {
            command = "LF " + LF_ANGLE + " ";
        } else if (left && !forward) {
            command = "LB " + LB_ANGLE + " ";
        } else if (!left && forward) {
            command = "RF " + RF_ANGLE + " ";
        } else {
            command = "RB " + RB_ANGLE + " ";
        }

        return command;
    }

    public static void main(String[] args) {
        //System.out.println(moveForward(25));
    }
}
