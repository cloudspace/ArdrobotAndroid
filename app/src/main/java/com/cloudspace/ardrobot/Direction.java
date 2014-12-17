package com.cloudspace.ardrobot;

/**
 * Created by cloudspace on 12/10/14.
 */
public enum Direction {

    LEFT((byte) 0x1, "Left"), RIGHT((byte) 0x2, "Right"), FORWARD((byte) 0x3, "Forward"), BACK((byte) 0x4, "Back"), STOP((byte) 0x5, "Stop");

    public byte directionByte;
    public String directionCommand;
    private int speed = -1;

    public int getSpeed() {
        return speed;
    }

    Direction(byte directionByte, String directionCommand) {
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

    public Direction withSpeed(double speed) {
        this.speed = Double.valueOf(Math.abs(speed * 100)).intValue();
        return this;
    }
}
