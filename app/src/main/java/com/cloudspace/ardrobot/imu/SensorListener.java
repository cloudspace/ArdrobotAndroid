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

    private Publisher<Imu> publisher;

    private boolean hasAccel;
    private boolean hasGyro;
    private boolean hasQuat;

    private long accelTime;
    private long gyroTime;
    private long quatTime;

    public Imu getImu() {
        return imu;
    }

    private Imu imu;
    double imuMod = 0;

    StateConsciousTouchListener touchListener;

    long lastEventTime = -1;
    int sensorDelay;

    /**
     * A listener used to translate sensor data into ROS compatible messages.
     *
     * @param publisher Publisher used to publish Imu messages
     * @param hasAccel Whether the accelerometer is available on the device
     * @param hasGyro Whether the gyroscope is available on the device
     * @param hasQuat Whether the quaternion sensor is available on the device
     * @param sensorDelay delay in millis between publishing sensor data
     */
    public SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat, int sensorDelay) {
        this.publisher = publisher;
        this.hasAccel = hasAccel;
        this.hasGyro = hasGyro;
        this.hasQuat = hasQuat;
        this.accelTime = 0;
        this.gyroTime = 0;
        this.quatTime = 0;
        this.imu = this.publisher.newMessage();
        this.sensorDelay = sensorDelay;
    }

    /**
     * A listener used to translate sensor data into ROS compatible messages.
     *
     * @param publisher Publisher used to publish Imu messages
     * @param hasAccel Whether the accelerometer is available on the device
     * @param hasGyro Whether the gyroscope is available on the device
     * @param hasQuat Whether the quaternion sensor is available on the device
     * @param touchListener StateConsciousTouchListener used as a trigger to enable/disable sensor data publishing
     * @param sensorDelay delay in millis between publishing sensor data
     */
    public SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat,
                          StateConsciousTouchListener touchListener, int sensorDelay) {
        this(publisher, hasAccel, hasGyro, hasQuat, sensorDelay);
        if (touchListener != null) {
            this.touchListener = touchListener;
            touchListener.setOnDownListener(downListener);
        } else {
            //vertical needs to be straight up.
            imuMod = 15;
        }
    }

    StateConsciousTouchListener.OnDownListener downListener = new StateConsciousTouchListener.OnDownListener() {
        @Override
        public void onDown() {
            imuMod = imu.getLinearAcceleration().getX();
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (touchListener == null || touchListener.isDown) {
            if (lastEventTime == -1) {
                lastEventTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - lastEventTime > sensorDelay) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        //compare linear.x to nuetral and send delta
                        this.imu.getLinearAcceleration().setX(event.values[0] - imuMod);
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
                        this.imu.getHeader().setFrameId(Imu._TYPE.split("/")[1]);

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
