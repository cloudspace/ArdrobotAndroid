package com.cloudspace.ardrobot;

/**
 * Created by cloudspace on 12/10/14.
 */
public enum ServoCommand {

    FRONT((byte) 0x1), REAR((byte) 0x2), STOP((byte) 0x3);
    public byte targetServoByte;
    private int speed = 0;


    public int getSpeed() {
        return speed;
    }

    ServoCommand(byte targetServoByte) {
        this.targetServoByte = targetServoByte;
    }

    public static ServoCommand parseCommandToDirection(byte targetServoByte) {
        for (ServoCommand d : values()) {
            if (d.targetServoByte == targetServoByte) {
                return d;
            }
        }
        return STOP;
    }

    public ServoCommand withSpeed(double speed) {
        this.speed = Double.valueOf(Math.abs(speed * 100)).intValue();
        return this;
    }
}
