package com.cloudspace.ardrobot.imu;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import com.cloudspace.ardrobot.util.StateConsciousTouchListener;

import org.ros.message.Time;
import org.ros.node.topic.Publisher;

import sensor_msgs.Imu;

public class SensorListener implements SensorEventListener {

    private static final long SAMPLE_RATE = 10;
    private Publisher<Imu> publisher;

    private boolean hasAccel;
    private boolean hasGyro;
    private boolean hasQuat;

    private long accelTime;
    private long gyroTime;
    private long quatTime;

    private Imu imu;

    StateConsciousTouchListener touchListener;

    long lastEventTime = -1;

    public SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat) {
        this.publisher = publisher;
        this.hasAccel = hasAccel;
        this.hasGyro = hasGyro;
        this.hasQuat = hasQuat;
        this.accelTime = 0;
        this.gyroTime = 0;
        this.quatTime = 0;
        this.imu = this.publisher.newMessage();
    }

    public SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat,
                          StateConsciousTouchListener touchListener) {
        this(publisher, hasAccel, hasGyro, hasQuat);
        this.touchListener = touchListener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (touchListener == null || touchListener.isDown) {
            if (lastEventTime == -1) {
                lastEventTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - lastEventTime > SAMPLE_RATE) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        this.imu.getLinearAcceleration().setX(event.values[0]);
                        this.imu.getLinearAcceleration().setY(event.values[1]);
                        this.imu.getLinearAcceleration().setZ(event.values[2]);
                        double[] tmpCov = {0, 0, 0, 0, 0, 0, 0, 0, 0};// TODO Make Parameter
                        this.imu.setLinearAccelerationCovariance(tmpCov);
                        this.accelTime = event.timestamp;
                    } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        this.imu.getAngularVelocity().setX(event.values[0]);
                        this.imu.getAngularVelocity().setY(event.values[1]);
                        this.imu.getAngularVelocity().setZ(event.values[2]);
                        double[] tmpCov = {0, 0, 0, 0, 0, 0, 0, 0, 0};// TODO Make Parameter
                        this.imu.setAngularVelocityCovariance(tmpCov);
                        this.gyroTime = event.timestamp;
                    } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                        float[] quaternion = new float[4];
                        SensorManager.getQuaternionFromVector(quaternion, event.values);
                        this.imu.getOrientation().setW(quaternion[0]);
                        this.imu.getOrientation().setX(quaternion[1]);
                        this.imu.getOrientation().setY(quaternion[2]);
                        this.imu.getOrientation().setZ(quaternion[3]);
                        double[] tmpCov = {0, 0, 0, 0, 0, 0, 0, 0, 0};// TODO Make Parameter
                        this.imu.setOrientationCovariance(tmpCov);
                        this.quatTime = event.timestamp;
                    }

                    // Currently storing event times in case I filter them in the future.  Otherwise they are used to determine if all sensors have reported.
                    if ((this.accelTime != 0 || !this.hasAccel) &&
                            (this.gyroTime != 0 || !this.hasGyro) &&
                            (this.quatTime != 0 || !this.hasQuat)) {
                        // Convert event.timestamp (nanoseconds uptime) into system time, use that as the header stamp
                        long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
                        this.imu.getHeader().setStamp(Time.fromMillis(time_delta_millis + event.timestamp / 1000000));
                        this.imu.getHeader().setFrameId("/imu");// TODO Make parameter

                        publisher.publish(this.imu);

                        // Create a new message
                        this.imu = this.publisher.newMessage();

                        // Reset times
                        this.accelTime = 0;
                        this.gyroTime = 0;
                        this.quatTime = 0;
                        lastEventTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }
}