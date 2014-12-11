package com.cloudspace.ardrobot;

/**
 * Created by cloudspace on 12/10/14.
 */
public enum Direction {

    LEFT(0x1, "Left"), RIGHT(0x2, "Right"), FORWARD(0x3, "Forward"), BACK(0x4, "Back"), STOP(0x5, "Stop");

    public int directionByte;
    public String directionCommand;

    Direction(int directionByte, String directionCommand) {
        this.directionByte = directionByte;
        this.directionCommand = directionCommand;
    }

    public static Direction parseCommandToDirection(String command) {
        for (Direction d : values()) {
            if (d.directionCommand.equals(command)) {
                return d;
            }
        }
        return STOP;
    }
}
